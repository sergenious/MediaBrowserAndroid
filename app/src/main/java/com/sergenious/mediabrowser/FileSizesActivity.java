package com.sergenious.mediabrowser;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import com.sergenious.mediabrowser.ui.CanvasView;
import com.sergenious.mediabrowser.ui.DialogUtils;
import com.sergenious.mediabrowser.ui.gesture.ScaleMoveGestureDetector;
import com.sergenious.mediabrowser.utils.FileUtils;
import com.sergenious.mediabrowser.utils.UiUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class FileSizesActivity extends Activity implements ScaleMoveGestureDetector.OnScaleMoveGestureListener {
    public static final String ROOT_DIR_PARAM = "rootDir";
    private static final float MIN_SCALE_X = 200;

    private CanvasView fileView;
    private ScaleMoveGestureDetector gestureDetector;
    private final TextPaint fileNamePaint = new TextPaint();
    private final TextPaint fileSizePaint = new TextPaint();
    private final Paint rectOutlinePaint = new Paint();
    private final Paint rectFillPaint = new Paint();
    private FileTreeNode rootTreeNode = null;
    private int maxTreeDepth;
    private float viewScaleX = MIN_SCALE_X * 2.0f;
    private float viewScaleY = 0;
    private float viewOffsetX = 0.0f;
    private float viewOffsetY = 0.0f;

    private static class FileTreeNode {
        public final File file;
        public final List<FileTreeNode> childNodes = new ArrayList<>();
        public long fileSize;

        public FileTreeNode(File file) {
            this.file = file;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setTitle(R.string.file_sizes);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.gradient_appbar, getTheme()));

        fileView = new CanvasView(this, this::onDraw);
        fileView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(fileView);
        gestureDetector = new ScaleMoveGestureDetector(this);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        fileNamePaint.setColor(0xFFFFFFFF);
        fileNamePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 2, dm));
        fileSizePaint.setColor(0xFFC0C0C0);
        fileSizePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 2, dm));
        rectOutlinePaint.setStyle(Paint.Style.STROKE);
        rectOutlinePaint.setColor(0xFF404040);
        rectOutlinePaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 0.25f, dm));
        rectFillPaint.setStyle(Paint.Style.FILL);
        rectFillPaint.setColor(0xFF606060);

        boolean hasNoPermission = UiUtils.requestReadMediaPermissions(this);

        if (!hasNoPermission) {
            refreshFileList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        refreshFileList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
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
    }

    @Override
    public void onGestureScale(float centerX, float centerY, float prevCenterX, float prevCenterY,
        float scaleFactor, float scaleFactorX, float scaleFactorY) {

        float prevScaleX = viewScaleX;
        viewScaleX *= scaleFactorX;
        viewScaleX = Math.max(MIN_SCALE_X, Math.min(MIN_SCALE_X * 4, viewScaleX));

        float prevScaleY = viewScaleY;
        viewScaleY *= scaleFactorY;
        viewScaleY = Math.max(fileView.getHeight(), viewScaleY);

        limitScales();

        if (prevScaleX > 0) {
            viewOffsetX = centerX - (viewScaleX / prevScaleX) * (prevCenterX - viewOffsetX);
        }
        if (prevScaleY > 0) {
            viewOffsetY = centerY - (viewScaleY / prevScaleY) * (prevCenterY - viewOffsetY);
        }

        limitOffsets();
        fileView.invalidate();
    }

    @Override
    public void onGestureMove(float deltaX, float deltaY) {
        viewOffsetX += deltaX;
        viewOffsetY += deltaY;
        limitOffsets();
        fileView.invalidate();
    }

    @Override
    public void onGestureEnd(float x, float y) {
    }

    private void limitScales() {
        viewScaleX = Math.max(MIN_SCALE_X, Math.min(MIN_SCALE_X * 4, viewScaleX));
        viewScaleY = Math.max(fileView.getHeight(), viewScaleY);
    }

    private void limitOffsets() {
        viewOffsetX = Math.min(0, Math.max(fileView.getWidth() - (maxTreeDepth + 1) * viewScaleX, viewOffsetX));
        viewOffsetY = Math.min(0, Math.max(fileView.getHeight() - viewScaleY, viewOffsetY));
    }

    private void refreshFileList() {
        if (getIntent().hasExtra(ROOT_DIR_PARAM)) {
            File rootDir = (File) getIntent().getSerializableExtra(ROOT_DIR_PARAM);

            DialogUtils.showProgressDialog(this, getString(R.string.loading), 1.0f,
                (progressUpdater, canceled) -> () -> {
                    refreshFileList(rootDir, progressUpdater, canceled);
                    if (!canceled.get()) {
                        runOnUiThread(() -> fileView.invalidate());
                    }
                    else {
                        finish();
                    }
                }
            );
        }
    }

    private void refreshFileList(File rootDir, BiConsumer<String, Double> progressUpdater, Supplier<Boolean> canceled) {
        rootTreeNode = new FileTreeNode(rootDir);
        AtomicInteger maxDepth = new AtomicInteger();

        rootTreeNode.fileSize += appendDirectory(rootTreeNode, rootDir, rootDir,
            1, maxDepth, progressUpdater, canceled, 0.0, 1.0);

        maxTreeDepth = maxDepth.get();
    }

    private long appendDirectory(FileTreeNode parentNode, File rootDir, File parentDir,
        int depth, AtomicInteger maxDepth,
        BiConsumer<String, Double> progressUpdater, Supplier<Boolean> canceled,
        double progress, double progressFactor) {

        if (canceled.get()) {
            return 0;
        }
        progressUpdater.accept(parentDir.getAbsolutePath().substring(rootDir.getAbsolutePath().length()), progress);
        if (depth > maxDepth.get()) {
            maxDepth.set(depth);
        }

        long totalFileSize = 0;
        File[] fileList = parentDir.listFiles();
        if (fileList != null) {
            progressFactor /= fileList.length;
            for (int fileIndex = 0; fileIndex < fileList.length; fileIndex++) {
                File file = fileList[fileIndex];

                FileTreeNode node = new FileTreeNode(file);
                parentNode.childNodes.add(node);

                if (file.isDirectory()) {
                    long childSize = appendDirectory(node, rootDir, file, depth + 1, maxDepth,
                        progressUpdater, canceled,
                        progress + progressFactor * fileIndex, progressFactor);
                    totalFileSize += childSize;
                    node.fileSize += childSize;
                }
                else {
                    long fileSize = file.length();
                    totalFileSize += fileSize;
                    node.fileSize = fileSize;
                }
            }

            parentNode.childNodes.sort((n1, n2) -> {
                long diff = n2.fileSize - n1.fileSize;
                return (int) Math.signum(diff);
            });
        }

        return totalFileSize;
    }

    private void onDraw(Canvas canvas) {
        if (rootTreeNode == null) {
            return;
        }

        limitScales();
        limitOffsets();
        drawFile(canvas, viewScaleX, viewScaleY, viewOffsetX, viewOffsetY, rootTreeNode);
    }

    private void drawFile(Canvas canvas, float scaleX, float scaleY, float ofsX, float ofsY, FileTreeNode fileNode) {
        if (scaleY > 4) {
            canvas.drawRect(ofsX + 2, ofsY + 2, ofsX + scaleX - 4, ofsY + scaleY - 4, rectFillPaint);
            canvas.drawRect(ofsX + 2, ofsY + 2, ofsX + scaleX - 4, ofsY + scaleY - 4, rectOutlinePaint);

            if (scaleY > fileNamePaint.getTextSize() + 4) {
                String fileName = fileNode.file.getName();
                if (fileNode.file.isDirectory()) {
                    fileName += " [" + fileNode.childNodes.size() + "]";
                }
                CharSequence fileNameText = TextUtils.ellipsize(fileName,
                    fileNamePaint, scaleX - 4, TextUtils.TruncateAt.MIDDLE);
                canvas.drawText(fileNameText.toString(), ofsX + 2, ofsY + fileNamePaint.getTextSize() + 0, fileNamePaint);
            }
            if (scaleY > fileNamePaint.getTextSize() + fileSizePaint.getTextSize() + 8) {
                CharSequence sizeText = TextUtils.ellipsize(FileUtils.fileSizeToString(fileNode.fileSize, false),
                    fileSizePaint, scaleX - 4, TextUtils.TruncateAt.MIDDLE);
                canvas.drawText(sizeText.toString(), ofsX + 2,
                    ofsY + fileNamePaint.getTextSize() + fileSizePaint.getTextSize() + 6, fileSizePaint);
            }
        }

        if (ofsX + scaleX < canvas.getWidth()) {
            for (FileTreeNode childNode: fileNode.childNodes) {
                float childScaleY = scaleY * childNode.fileSize / (float) fileNode.fileSize;
                if ((ofsY < canvas.getHeight()) && (ofsY + childScaleY > 0)) {
                    drawFile(canvas, scaleX, childScaleY, ofsX + scaleX, ofsY, childNode);
                }
                ofsY += childScaleY;
            }
        }
    }
}
