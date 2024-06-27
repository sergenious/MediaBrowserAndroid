package com.sergenious.mediabrowser;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.MediaController;
import android.widget.VideoView;

import com.sergenious.mediabrowser.utils.UiUtils;

public class VideoActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        UiUtils.requestFullScreen(this, true);
        boolean hasNoPermission = UiUtils.requestReadMediaPermissions(this);

        setContentView(R.layout.video);

        if (!hasNoPermission) {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        init();
    }

    private void init() {
        VideoView videoView = findViewById(R.id.videoView);
        videoView.setOnPreparedListener(mp -> {
            MediaController mediaController = new MediaController(this) {
                @Override
                public boolean dispatchKeyEvent(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                        mp.release();
                        super.hide();
                        finish();
                        return true;
                    }
                    return super.dispatchKeyEvent(event);
                }
            };
            mediaController.setAnchorView(videoView);
            mediaController.addOnAttachStateChangeListener(new MediaControllerFullScreenSwitcher());
            videoView.setMediaController(mediaController);
        });

        videoView.setVideoURI(getIntent().getData());
        videoView.requestFocus();
        videoView.start();
    }

    private class MediaControllerFullScreenSwitcher implements View.OnAttachStateChangeListener {
        @Override
        public void onViewAttachedToWindow(View v) {
            UiUtils.requestFullScreen(VideoActivity.this, false);
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            UiUtils.requestFullScreen(VideoActivity.this, true);
        }
    }
}
