package com.sergenious.mediabrowser.ui.gesture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.function.Consumer;

public class SimpleScaleGestureDetector
    implements View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {

    private final Consumer<Integer> onScaleChanged;
    private final ScaleGestureDetector gestureScale;
    private final int minScale, maxScale;
    private float scaleFactor;

    public SimpleScaleGestureDetector(Context context, int initialScale, int minScale, int maxScale,
        Consumer<Integer> onScaleChanged) {

        this.onScaleChanged = onScaleChanged;
        this.gestureScale = new ScaleGestureDetector(context, this);
        this.scaleFactor = initialScale;
        this.minScale = minScale;
        this.maxScale = maxScale;
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float newScaleFactor = scaleFactor * detector.getScaleFactor();
        newScaleFactor = Math.max(minScale, Math.min(maxScale, Math.round(newScaleFactor)));

        if (scaleFactor != newScaleFactor) {
            scaleFactor = newScaleFactor;
            if (onScaleChanged != null) {
                onScaleChanged.accept((int) scaleFactor);
            }
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureScale.onTouchEvent(event);
        return false; // to allow the default behavior, like scrolling
    }
}
