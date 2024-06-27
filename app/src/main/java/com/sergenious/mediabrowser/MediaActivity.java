package com.sergenious.mediabrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sergenious.mediabrowser.media.AbstractMediaView;
import com.sergenious.mediabrowser.media.MediaImageView;
import com.sergenious.mediabrowser.ui.DataGridLayout;
import com.sergenious.mediabrowser.ui.DialogUtils;
import com.sergenious.mediabrowser.ui.gesture.ScaleMoveGestureDetector;
import com.sergenious.mediabrowser.utils.FileUtils;
import com.sergenious.mediabrowser.utils.MediaUtils;
import com.sergenious.mediabrowser.utils.UiUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MediaActivity extends Activity implements ScaleMoveGestureDetector.OnScaleMoveGestureListener {
    public static final String SLIDESHOW_FILES_PARAM = "slideshowFiles";
    private static final float TRANSIENT_FACTOR = 0.2f;
    private static final float SWIPE_SPEED_THRESHOLD = 10; // speed for moving to the prev/next image, although less than half screen swiped

    private final Handler handler = new Handler(Looper.getMainLooper());
    private RelativeLayout rootLayout;
    private ImageButton btnAction;
    private AbstractMediaView currentMediaView;
    private AbstractMediaView prevMediaView, nextMediaView;
    private ScaleMoveGestureDetector gestureDetector;
    private ActionType currentActionType = ActionType.NONE;
    private final List<File> mediaFileList = new ArrayList<>();
    private int currentMediaFileIndex;
    private boolean isFullScreen = true;
    private boolean canSwipeLeft, canSwipeRight;
    private float maxMoveDistance;
    private float currentSwipeOffsetX;
    private float lastSwipeSpeed;
    private long lastClickTime;
    private Runnable clickRunnable;
    private Timer transientTimer;
    private boolean isTransientTimer;

    private static final RelativeLayout.LayoutParams MATCH_PARENT = new RelativeLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    private enum ActionType {
        NONE,
        PANO,
        VIDEO,
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        UiUtils.requestFullScreen(this, isFullScreen);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setBackgroundDrawable(new ColorDrawable(0x80000000));

        boolean hasNoPermission = UiUtils.requestReadMediaPermissions(this);

        setContentView(R.layout.media);
        rootLayout = findViewById(R.id.media_container);
        gestureDetector = new ScaleMoveGestureDetector(this);
        btnAction = createActionButton(file -> launchAction(currentActionType, file));

        if (!hasNoPermission) {
            init();
        }

        transientTimer = new Timer("MediaTransient");
        transientTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                double deltaTime = 1000.0 / Constants.TRANSIENT_TIMER_INTERVAL;
                if (isTransientTimer) {
                    if (!handler.post(() -> updateTransient())) {
                        Log.e("MediaActivity", "Cannot post updateTransient runnable");
                    }
                }
                if (currentMediaView != null) {
                    currentMediaView.processTransients(deltaTime);
                }
                if (prevMediaView != null) {
                    prevMediaView.processTransients(deltaTime);
                }
                if (nextMediaView != null) {
                    nextMediaView.processTransients(deltaTime);
                }
            }
        }, Constants.TRANSIENT_TIMER_INTERVAL, Constants.TRANSIENT_TIMER_INTERVAL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        init();
    }

    @Override
    protected void onDestroy() {
        if (transientTimer != null) {
            transientTimer.cancel();
        }
        if (currentMediaView != null) {
            currentMediaView.destroy();
        }
        if (prevMediaView != null) {
            prevMediaView.destroy();
        }
        if (nextMediaView != null) {
            nextMediaView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.media_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if ((id == R.id.btnInfo) && (currentMediaView != null)) {
            try {
                List<List<Object>> metadata = MediaUtils.getMetadata(this, currentMediaView.getFile())
                    .entrySet().stream()
                    .map(entry -> Arrays.asList(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

                AlertDialog dialog = new AlertDialog.Builder(this, R.style.MediaBrowserTheme_AlertDialog)
                    .setTitle(R.string.details)
                    .setView(new DataGridLayout(this, metadata,
                        Collections.singletonList(150),
                        Arrays.asList(R.style.MediaBrowserTheme_MediaDetailsLabel,
                            R.style.MediaBrowserTheme_MediaDetailsValue)))
                    .show();

                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                return true;
            }
            catch (Exception e) {
                Log.e(Constants.appNameInternal, "Error during file information", e);
            }
        }
        if ((id == R.id.btnShare) && (currentMediaView != null)) {
            MediaUtils.startShareIntent(this, currentMediaView.getFile(), null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public void onGestureStart(float x, float y) {
        isTransientTimer = false;
        canSwipeLeft = (currentMediaView != null) && currentMediaView.isLeftmost();
        canSwipeRight = (currentMediaView != null) && currentMediaView.isRightmost();
        maxMoveDistance = 0;
        lastSwipeSpeed = 0;
    }

    @Override
    public void onGestureScale(float centerX, float centerY, float prevCenterX, float prevCenterY,
        float scaleFactor, float scaleFactorX, float scaleFactorY) {

        if (currentMediaView != null) {
            currentMediaView.scale(centerX, centerY, prevCenterX, prevCenterY, scaleFactor);
            btnAction.setVisibility(!currentMediaView.isScaled()
                && (currentActionType != ActionType.NONE) ? View.VISIBLE : View.GONE);
        }
        maxMoveDistance = Math.max(maxMoveDistance, (float) Math.hypot(centerX - prevCenterX, centerY - prevCenterY));
    }

    @Override
    public void onGestureMove(float deltaX, float deltaY) {
        isTransientTimer = false;

        if (currentMediaView != null) {
            btnAction.setVisibility(!currentMediaView.isScaled()
                && (currentActionType != ActionType.NONE) ? View.VISIBLE : View.GONE);
        }

        boolean isSwiping = false;
        if (canSwipeLeft || canSwipeRight) {
            isSwiping = updateMediaViewsPositions((int) (currentSwipeOffsetX + deltaX));
            if (isSwiping) {
                currentSwipeOffsetX += deltaX;
                lastSwipeSpeed += 0.1 * (deltaX - lastSwipeSpeed);
            }
            else {
                lastSwipeSpeed = 0;
            }
        }
        if (currentMediaView != null) {
            currentMediaView.move(!isSwiping ? deltaX : 0, deltaY);
        }
        maxMoveDistance = Math.max(maxMoveDistance, (float) Math.hypot(deltaX, deltaY));
    }

    @Override
    public void onGestureEnd(float x, float y) {
        if (currentMediaView != null) {
            currentMediaView.endRepositioning();
        }

        if (maxMoveDistance < 5) { // not moved (much), toggle the full screen
            onClick(x, y);
        }

        if (canSwipeLeft || canSwipeRight) {
            isTransientTimer = true;
        }
    }

    private void init() {
        mediaFileList.clear();

        if (getIntent().hasExtra(SLIDESHOW_FILES_PARAM)) {
            @SuppressWarnings("unchecked")
            List<File> slideshowFiles = (List<File>) getIntent().getSerializableExtra(SLIDESHOW_FILES_PARAM);
            collectSlideshowFiles(slideshowFiles);
            return;
        }

        Uri uri = getIntent().getData();
        File currentFile = (uri != null) ? FileUtils.getFileFromUri(this, uri) : null;
        if (currentFile != null) {
            FileUtils.FileSortMode fileSortMode = FileUtils.FileSortMode.valueOf(UiUtils.getSharedPreference(this,
                "fileSortMode", FileUtils.FileSortMode.PATH_DIRS_FILES.name()));

            mediaFileList.addAll(FileUtils.getFileList(currentFile.getParentFile(),
                    MediaUtils.getAllMediaExtensions(), false, fileSortMode)
                .stream().map(f -> f.file).collect(Collectors.toList()));
        }

        if (!mediaFileList.isEmpty()) {
            initMediaViews(currentFile);
        }
        else {
            finish(); // terminate the media activity
        }
    }

    private void collectSlideshowFiles(List<File> files) {
        DialogUtils.showProgressDialog(this, getString(R.string.collecting_files) + "...", 1.0f,
            (progressUpdater, canceled) -> () -> {
                mediaFileList.clear();
                float progressFactor = 1.0f / files.size();

                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);

                    if (file.isDirectory()) {
                        mediaFileList.addAll(FileUtils.searchFiles(null, file,
                                MediaUtils.getAllMediaExtensions(), FileUtils.FileSortMode.PATH,
                                i * progressFactor, progressFactor, progressUpdater, canceled)
                            .stream()
                            .filter(f -> !f.file.isDirectory())
                            .map(f -> f.file)
                            .collect(Collectors.toList()));
                    }
                    else {
                        mediaFileList.add(file);
                    }
                }

                if (!canceled.get() && !mediaFileList.isEmpty()) {
                    runOnUiThread(() -> initMediaViews(null));
                }
                else {
                    finish(); // canceled, go back
                }
            }
        );
    }

    private void initMediaViews(File currentFile) {
        if (currentFile == null) {
            currentFile = mediaFileList.get(0);
        }

        currentMediaFileIndex = mediaFileList.indexOf(currentFile);
        if (currentMediaFileIndex < 0) { // should not happen in a normal situation
            finish();
            return;
        }

        currentMediaView = addMediaView(mediaFileList.get(currentMediaFileIndex), true, false);
        updateFileState();

        if (currentMediaFileIndex > 0) {
            prevMediaView = addMediaView(mediaFileList.get(currentMediaFileIndex - 1), false, true);
        }
        if (currentMediaFileIndex < mediaFileList.size() - 1) {
            nextMediaView = addMediaView(mediaFileList.get(currentMediaFileIndex + 1), false, true);
        }
    }

    private ImageButton createActionButton(Consumer<File> clickListener) {
        int size = UiUtils.mmToPx(this, Constants.ACTION_BUTTON_SIZE_MM);
        RelativeLayout.LayoutParams actionLayoutParams = new RelativeLayout.LayoutParams(size, size);
        actionLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        actionLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        actionLayoutParams.setMargins(0, 0, 0, 0);
        ImageButton btn = new ImageButton(this);
        btn.setPadding(0, 0, 0, 0);
        btn.setBackground(null);
        btn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        btn.setLayoutParams(actionLayoutParams);
        btn.setVisibility(View.GONE);
        btn.setOnClickListener(view -> {
            File file = (File) view.getTag();
            if (file != null) {
                clickListener.accept(file);
            }
        });
        rootLayout.addView(btn);
        return btn;
    }

    private void updateFileState() {
        File file = currentMediaView.getFile();
        String title = file.getName();
        if ((mediaFileList != null) && (mediaFileList.size() > 1)) {
            title += " [" + (currentMediaFileIndex + 1) + "/" + mediaFileList.size() + "]";
        }
        setTitle(title);

        String extension = FileUtils.getFileExtension(file);
        currentActionType = ActionType.NONE;
        if (MediaUtils.isImageExtension(extension) && MediaUtils.isImagePano(file)) {
            currentActionType = ActionType.PANO;
        }
        if (MediaUtils.isVideoExtension(extension)) {
            currentActionType = ActionType.VIDEO;
        }

        btnAction.setTag(file);
        btnAction.setVisibility((currentActionType != ActionType.NONE) ? View.VISIBLE : View.GONE);
        btnAction.bringToFront();

        switch (currentActionType) {
            case PANO: btnAction.setImageResource(R.drawable.ic_360); break;
            case VIDEO: btnAction.setImageResource(R.drawable.ic_video); break;
        }
    }

    private void launchAction(ActionType actionType, File file) {
        switch (actionType) {
            case PANO: {
                Intent intent = new Intent(this, PanoramicActivity.class);
                intent.setData(Uri.fromFile(file));
                this.startActivity(intent);
                break;
            }
            case VIDEO: {
                Intent intent = new Intent(this, VideoActivity.class);
                intent.setData(Uri.fromFile(file));
                this.startActivity(intent);
                break;
            }
        }
    }

    private AbstractMediaView addMediaView(File file, boolean visible, boolean previewOnly) {
        AbstractMediaView mediaView;

        // TODO!!! other types of the view
        mediaView = new MediaImageView(this, rootLayout, file, previewOnly);

        mediaView.setVisible(visible);
        ((View) mediaView).setLayoutParams(MATCH_PARENT);
        rootLayout.addView((View) mediaView);
        return mediaView;
    }

    private void onClick(float x, float y) {
        if (currentMediaView != null) {
            // check double click
            if (lastClickTime + Constants.IMAGE_DOUBLE_CLICK_INTERVAL > System.currentTimeMillis()) {
                if (clickRunnable != null) {
                    handler.removeCallbacks(clickRunnable);
                    clickRunnable = null;
                }
                currentMediaView.doubleClick(x, y);
                btnAction.setVisibility(!currentMediaView.isScaled()
                    && (currentActionType != ActionType.NONE) ? View.VISIBLE : View.GONE);
            }
            else {
                clickRunnable = () -> {
                    isFullScreen = !isFullScreen;
                    UiUtils.requestFullScreen(this, isFullScreen);
                    clickRunnable = null;
                };
                // delay the single click action, so the double click can cancel it
                long delay = (int) (1.1 * Constants.IMAGE_DOUBLE_CLICK_INTERVAL);
                if (!handler.postDelayed(clickRunnable, currentMediaView.isScaled() ? 0 : delay)) {
                    Log.e("MediaActivity", "OnClick handler not posted!");
                }
            }

            lastClickTime = System.currentTimeMillis();
        }
    }

    private void updateTransient() {
        boolean isEnd;

        if ((currentMediaFileIndex > 0) && (prevMediaView != null) && canSwipeLeft
            && ((currentSwipeOffsetX > rootLayout.getWidth() / 2.0f) || (lastSwipeSpeed > SWIPE_SPEED_THRESHOLD))) {

            isEnd = updateTransientSwipeOffsetX(rootLayout.getWidth());
        }
        else if ((currentMediaFileIndex < mediaFileList.size() - 1) && (nextMediaView != null) && canSwipeRight
            && ((currentSwipeOffsetX < -rootLayout.getWidth() / 2.0f) || (lastSwipeSpeed < -SWIPE_SPEED_THRESHOLD))) {

            isEnd = updateTransientSwipeOffsetX(-rootLayout.getWidth());
        }
        else { // cancel swipe, return to the current image
            isEnd = updateTransientSwipeOffsetX(0);
        }

        if (isEnd) {
            isTransientTimer = false;
        }

        updateMediaViewsPositions((int) currentSwipeOffsetX);

        if (isEnd) {
            if ((currentMediaFileIndex > 0) && (prevMediaView != null)
                && (currentSwipeOffsetX > rootLayout.getWidth() / 2.0f)) {

                moveToOtherMediaView(prevMediaView, -1); // swipe right, prev image display
            }
            else if ((currentMediaFileIndex < mediaFileList.size() - 1) && (nextMediaView != null)
                && (currentSwipeOffsetX < -rootLayout.getWidth() / 2.0f)) {

                moveToOtherMediaView(nextMediaView, +1); // swipe left, next image display
            }
        }
    }

    private boolean updateTransientSwipeOffsetX(int targetOffset) {
        currentSwipeOffsetX += TRANSIENT_FACTOR * (targetOffset - currentSwipeOffsetX);
        if (Math.abs(targetOffset - currentSwipeOffsetX) < 0.25) {
            currentSwipeOffsetX = targetOffset;
            return true;
        }
        return false;
    }

    private boolean updateMediaViewsPositions(int posX) {
        boolean isOk = (canSwipeLeft && (currentMediaFileIndex > 0) && (prevMediaView != null)
            && (posX >= 0) && currentMediaView.isLeftmost())
            || (canSwipeRight && (currentMediaFileIndex < mediaFileList.size() - 1) && (nextMediaView != null)
            && (posX <= 0) && currentMediaView.isRightmost());

        if (!isOk) {
            posX = 0;
        }
        if (prevMediaView != null) {
            prevMediaView.setGlobalOfsX(posX - rootLayout.getWidth());
            prevMediaView.setVisible(posX > 0);
        }
        if (nextMediaView != null) {
            nextMediaView.setGlobalOfsX(posX + rootLayout.getWidth());
            nextMediaView.setVisible(posX < 0);
        }
        currentMediaView.setGlobalOfsX(posX);

        return isOk;
    }

    private void moveToOtherMediaView(AbstractMediaView otherMediaView, int direction) {
        AbstractMediaView savedMediaView = currentMediaView;
        currentMediaView = otherMediaView;
        currentMediaView.setGlobalOfsX(0);
        savedMediaView.destroy();
        rootLayout.removeView((View) savedMediaView);
        currentMediaFileIndex += direction;
        currentSwipeOffsetX = 0;

        if ((otherMediaView != prevMediaView) && (prevMediaView != null)) {
            prevMediaView.destroy();
            rootLayout.removeView((View) prevMediaView);
        }
        if ((otherMediaView != nextMediaView) && (nextMediaView != null)) {
            nextMediaView.destroy();
            rootLayout.removeView((View) nextMediaView);
        }
        prevMediaView = nextMediaView = null;
        updateFileState();

        handler.post(() -> {
            currentMediaView.setVisible(true);
            currentMediaView.loadFully();
            if (currentMediaFileIndex > 0) {
                prevMediaView = addMediaView(mediaFileList.get(currentMediaFileIndex - 1), false, true);
            }
            if (currentMediaFileIndex < mediaFileList.size() - 1) {
                nextMediaView = addMediaView(mediaFileList.get(currentMediaFileIndex + 1), false, true);
            }
        });
    }
}
