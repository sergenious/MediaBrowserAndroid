package com.sergenious.mediabrowser.media;

import java.io.File;

public interface AbstractMediaView {
    void destroy();
    File getFile();
    void setGlobalOfsX(float ofsX);
    void setVisible(boolean visible);
    void processTransients(double deltaTime);
    void loadFully();
    boolean isLeftmost();
    boolean isRightmost();
    boolean isScaled();
    void scale(float centerX, float centerY, float prevCenterX, float prevCenterY, float scaleFactor);
    void move(float deltaX, float deltaY);
    void doubleClick(float x, float y);
    void endRepositioning();
}
