package com.sergenious.mediabrowser.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import java.util.function.Consumer;

public class CanvasView extends View {
    private final Consumer<Canvas> onDrawFunc;

    public CanvasView(Context context, Consumer<Canvas> onDrawFunc) {
        super(context);
        this.onDrawFunc = onDrawFunc;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        onDrawFunc.accept(canvas);
    }
}
