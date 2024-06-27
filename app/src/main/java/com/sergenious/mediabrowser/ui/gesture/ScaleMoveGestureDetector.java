package com.sergenious.mediabrowser.ui.gesture;

import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScaleMoveGestureDetector {
    private final Map<Integer, MotionEvent.PointerCoords> startMultiCoords = new HashMap<>();
    private final OnScaleMoveGestureListener listener;

    public interface OnScaleMoveGestureListener {
        void onGestureStart(float x, float y);
        void onGestureScale(float centerX, float centerY, float prevCenterX, float prevCenterY,
            float scaleFactor, float scaleFactorX, float scaleFactorY);
        void onGestureMove(float deltaX, float deltaY);
        void onGestureEnd(float x, float y);
    }

    public ScaleMoveGestureDetector(OnScaleMoveGestureListener listener) {
        this.listener = listener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if ((event.getAction() == MotionEvent.ACTION_DOWN)
            || (event.getAction() == MotionEvent.ACTION_POINTER_DOWN)) {

            return onTouchStart(event);
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return onTouchMove(event);
        }
        if ((event.getAction() == MotionEvent.ACTION_UP)
            || (event.getAction() == MotionEvent.ACTION_POINTER_UP)) {

            return onTouchEnd(event);
        }
        return false;
    }

    private boolean onTouchStart(MotionEvent event) {
        for (int i = 0; i < event.getPointerCount(); i++) {
            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            event.getPointerCoords(i, coords);
            startMultiCoords.put(event.getPointerId(i), coords);
        }
        listener.onGestureStart(event.getX(), event.getY());
        return true;
    }

    private boolean onTouchMove(MotionEvent event) {
        int numCoords = event.getPointerCount();
        List<MotionEvent.PointerCoords> newCoordsList = new ArrayList<>(numCoords);
        List<MotionEvent.PointerCoords> prevCoordsList = new ArrayList<>(numCoords);

        float currCenterX = 0, currCenterY = 0, prevCenterX = 0, prevCenterY = 0;
        for (int i = 0; i < numCoords; i++) {
            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            event.getPointerCoords(i, coords);
            newCoordsList.add(coords);
            currCenterX += coords.x;
            currCenterY += coords.y;
            MotionEvent.PointerCoords prevCoords = startMultiCoords.get(event.getPointerId(i));
            if (prevCoords == null) {
                prevCoords = coords;
            }
            prevCenterX += prevCoords.x;
            prevCenterY += prevCoords.y;
            prevCoordsList.add(prevCoords);
        }

        if (numCoords > 0) {
            currCenterX /= numCoords;
            currCenterY /= numCoords;
            prevCenterX /= numCoords;
            prevCenterY /= numCoords;
        }

        if (numCoords >= 2) {
            float currDeltaX = (newCoordsList.get(0).x - newCoordsList.get(1).x) * 0.5f;
            float currDeltaY = (newCoordsList.get(0).y - newCoordsList.get(1).y) * 0.5f;
            float prevDeltaX = (prevCoordsList.get(0).x - prevCoordsList.get(1).x) * 0.5f;
            float prevDeltaY = (prevCoordsList.get(0).y - prevCoordsList.get(1).y) * 0.5f;
            float currDistance = (float) Math.sqrt(currDeltaX * currDeltaX + currDeltaY * currDeltaY);
            float prevDistance = (float) Math.sqrt(prevDeltaX * prevDeltaX + prevDeltaY * prevDeltaY);

            listener.onGestureScale(currCenterX, currCenterY, prevCenterX, prevCenterY,
                (prevDistance != 0) ? currDistance / prevDistance : 1,
                (Math.abs(prevDeltaX) > 50) && (Math.signum(currDeltaX) == Math.signum(prevDeltaX)) ? currDeltaX / prevDeltaX : 1,
                (Math.abs(prevDeltaY) > 50) && (Math.signum(currDeltaY) == Math.signum(prevDeltaY)) ? currDeltaY / prevDeltaY : 1);
        }
        else {
            listener.onGestureMove(currCenterX - prevCenterX, currCenterY - prevCenterY);
        }

        startMultiCoords.clear();
        for (int i = 0; i < numCoords; i++) {
            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            event.getPointerCoords(i, coords);
            startMultiCoords.put(event.getPointerId(i), coords);
        }
        return true;
    }

    private boolean onTouchEnd(MotionEvent event) {
        startMultiCoords.clear();
        listener.onGestureEnd(event.getX(), event.getY());
        return true;
    }
}
