package com.sergenious.mediabrowser.media;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Handler;
import android.util.Pair;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.sergenious.mediabrowser.Constants;
import com.sergenious.mediabrowser.utils.MediaUtils;

import java.io.File;

@SuppressLint("ViewConstructor")
public class MediaImageView extends ImageView implements AbstractMediaView {
    private final ViewGroup rootLayout;
    private final File file;
    private Bitmap currentBitmap;
    private Integer imageExifOrientation;
    private final Matrix matrix;
    private final Handler handler = new Handler();
    private Size imageDimensions = new Size(0, 0);
    private Runnable fullImageLoader;
    private boolean isTransientTimer;
    private float scale;
    private float ofsX;
    private float ofsY;
    private float globalOfsX;
    private float speedX;
    private float speedY;
    private boolean isVisible;
    private boolean isFullyLoaded;
    private boolean isRepositioning;

    public MediaImageView(Context context, ViewGroup rootLayout, File file, boolean previewOnly) {
        super(context);

        this.rootLayout = rootLayout;
        this.file = file;
        this.matrix = new Matrix();

        setScaleType(ImageView.ScaleType.MATRIX);
        setImageMatrix(matrix);
        loadImage(false);

        if (!previewOnly) {
            loadFully();
        }

        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            scale = Math.max(scale, getMinScale());
            updateMatrix();
        });
    }

    @Override
    public void destroy() {
        setImageBitmap(null);
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        super.setImageBitmap(bitmap);
        if (currentBitmap != null) {
            currentBitmap.recycle();
        }
        currentBitmap = bitmap;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void processTransients(double deltaTime) {
        if (isTransientTimer) {
            handler.post(this::updateTransients);
        }
    }

    @Override
    public void loadFully() {
       fullImageLoader = () -> {
           if (isAttachedToWindow()) {
               loadImage(true);
           }
           fullImageLoader = null;
        };
        handler.postDelayed(fullImageLoader, Constants.FULL_IMAGE_LOAD_DELAY);
    }

    @Override
    public void setGlobalOfsX(float ofsX) {
        this.globalOfsX = ofsX;
        updateMatrix();
    }

    @Override
    public void setVisible(boolean visible) {
        setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        isVisible = (visibility == View.VISIBLE);
        updateMatrix();
    }

    @Override
    public boolean isLeftmost() {
        RectF offsetBounds = getOffsetBounds();
        return ofsX >= offsetBounds.right;
    }

    @Override
    public boolean isRightmost() {
        RectF offsetBounds = getOffsetBounds();
        return ofsX <= offsetBounds.left;
    }

    @Override
    public boolean isScaled() {
        return scale > getMinScale();
    }

    @Override
    public void scale(float centerX, float centerY, float prevCenterX, float prevCenterY, float scaleFactor) {
        float prevScale = scale;
        scale *= scaleFactor;
        scale = Math.max(getMinScale(), Math.min(Constants.MAX_IMAGE_SCALE, this.scale));
        if (prevScale > 0) {
            ofsX = centerX - (scale / prevScale) * (prevCenterX - ofsX);
            ofsY = centerY - (scale / prevScale) * (prevCenterY - ofsY);
        }
        speedX = speedY = 0;
        isRepositioning = true;
        updateMatrix();
    }

    @Override
    public void move(float deltaX, float deltaY) {
        ofsX += deltaX;
        ofsY += deltaY;
        if (Math.abs(deltaX) > 1) {
            speedX = deltaX;
        }
        if (Math.abs(deltaY) > 1) {
            speedY = deltaY;
        }
        isRepositioning = true;
        updateMatrix();
    }

    @Override
    public void doubleClick(float x, float y) {
        zoomInNative(x, y);
    }

    @Override
    public void endRepositioning() {
        isRepositioning = false;
        isTransientTimer = true;
    }

    /*@Override
    protected void onDraw(Canvas canvas) {
        if (currentBitmap != null) {
            canvas.drawBitmap(currentBitmap, matrix, null);
        }
    }*/

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (fullImageLoader != null) {
            handler.removeCallbacks(fullImageLoader);
            fullImageLoader = null;
        }
    }

    private void loadImage(boolean isFull) {
        new Thread(() -> {
            int maxImageSize = isFull ? Constants.MAX_IMAGE_SIZE : Constants.PREVIEW_IMAGE_SIZE;
            final Pair<Pair<Size, Integer>, Bitmap> imageData = MediaUtils.loadImage(file, maxImageSize, maxImageSize, isFull, false);
            if (imageData != null) {
                imageExifOrientation = imageData.first.second;

                handler.post(() -> {
                    if (isFull) {
                        isFullyLoaded = true;
                    }
                    Size prevDimensions = imageDimensions;
                    imageDimensions = new Size(imageData.second.getWidth(), imageData.second.getHeight());
                    imageDimensions = MediaUtils.fixImageSizeByExifOrientation(imageDimensions, imageExifOrientation);
                    if ((prevDimensions != null) && (prevDimensions.getWidth() > 0)) {
                        scale *= (float) prevDimensions.getWidth() / imageDimensions.getWidth();
                    }
                    updateMatrix();
                    setImageBitmap(imageData.second);
                });
            }
        }, "ImageLoadingThread").start();
    }

    public void zoomInNative(float clickX, float clickY) {
        float minScale = getMinScale();
        if (scale <= minScale) {
            float prevScale = scale;
            scale = getNativeScale();
            if (prevScale > 0) {
                ofsX = clickX - (scale / prevScale) * (clickX - ofsX);
                ofsY = clickY - (scale / prevScale) * (clickY - ofsY);
            }
        }
        else {
            scale = minScale;
            ofsX = 0;
            ofsY = 0;
        }
        updateMatrix();
    }

    private void updateTransients() {
        speedX /= 1.05f;
        speedY /= 1.05f;
        if ((Math.abs(speedX) > 1) || (Math.abs(speedY) > 1)) {
            if (!isRepositioning) {
                ofsX += speedX;
                ofsY += speedY;
                updateMatrix();
            }
        }
        else {
            isTransientTimer = false;
        }
    }

    private void updateMatrix() {
        scale = isVisible ? Math.max(getMinScale(), Math.min(Constants.MAX_IMAGE_SCALE, this.scale)) : 0;
        RectF offsetBounds = getOffsetBounds();
        ofsX = Math.min(offsetBounds.right, Math.max(offsetBounds.left, ofsX));
        ofsY = Math.min(offsetBounds.bottom, Math.max(offsetBounds.top, ofsY));
        matrix.reset();
        MediaUtils.fixImageMatrixByExifOrientation(matrix, imageDimensions, imageExifOrientation);
        matrix.postScale(scale, scale);
        matrix.postTranslate(ofsX + globalOfsX, ofsY);
        setImageMatrix(matrix);
    }

    private RectF getOffsetBounds() {
        return new RectF(
            rootLayout.getWidth() - imageDimensions.getWidth() * scale,
            rootLayout.getHeight() - imageDimensions.getHeight() * scale,
            Math.max(0, (rootLayout.getWidth() - imageDimensions.getWidth() * scale) / 2),
            Math.max(0, (rootLayout.getHeight() - imageDimensions.getHeight() * scale) / 2));
    }

    private float getMinScale() {
        if ((imageDimensions.getWidth() == 0) || (imageDimensions.getHeight() == 0)) {
            return 0;
        }
        return Math.min((float) rootLayout.getWidth() / imageDimensions.getWidth(),
            (float) rootLayout.getHeight() / imageDimensions.getHeight());
    }

    private float getNativeScale() {
        if ((imageDimensions.getWidth() == 0) || (imageDimensions.getHeight() == 0)) {
            return 0;
        }
        return Math.max((float) rootLayout.getWidth() / imageDimensions.getWidth(),
            (float) rootLayout.getHeight() / imageDimensions.getHeight());
    }
}
