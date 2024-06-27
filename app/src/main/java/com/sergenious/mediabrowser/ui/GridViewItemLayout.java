package com.sergenious.mediabrowser.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sergenious.mediabrowser.R;

public class GridViewItemLayout extends RelativeLayout implements Checkable {
    private boolean isChecked;

    public GridViewItemLayout(Context context) {
        super(context);
    }

    public GridViewItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridViewItemLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Twice the width, to force a square size. Height ignored
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    public void setChecked(boolean checked) {
        isChecked = checked;
        setSelected(isChecked);
        ImageView selectedImage = findViewById(R.id.itemSelectedIcon);
        if (selectedImage != null) {
            selectedImage.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        setChecked(!isChecked);
    }
}
