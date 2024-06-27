package com.sergenious.mediabrowser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.sergenious.mediabrowser.pano.PanoramicRenderer;
import com.sergenious.mediabrowser.utils.FileUtils;
import com.sergenious.mediabrowser.utils.RotationSensor;
import com.sergenious.mediabrowser.utils.RotationSensor.OnDeviceRotationChanged;
import com.sergenious.mediabrowser.utils.UiUtils;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class PanoramicActivity extends Activity implements OnDeviceRotationChanged {
	private PanoramicRenderer panoRenderer;
	private GLSurfaceView panoViewer;
	private ToggleButton btnDeviceRotationEnabled;
	private RotationSensor rotationSensor;
	private Timer timer = null;
	private long prevTime = 0;
	private boolean isFullScreen = true;

	@SuppressLint({"RtlHardcoded", "ClickableViewAccessibility"})
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		UiUtils.requestFullScreen(this, isFullScreen);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setBackgroundDrawable(new ColorDrawable(0x80000000));

		UiUtils.requestReadMediaPermissions(this);

		FrameLayout container = new FrameLayout(this);
		
		rotationSensor = new RotationSensor(this);

		panoRenderer = new PanoramicRenderer(this,
			() -> panoViewer.requestRender(),
			clickedPoint -> {
				isFullScreen = !isFullScreen;
				UiUtils.requestFullScreen(this, isFullScreen);
			});

		panoViewer = new PanoramicView(this, panoRenderer);
		panoViewer.setLayoutParams(new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
		panoViewer.setOnTouchListener((view, event) ->
			panoRenderer.onTouchEvent(event) || super.onTouchEvent(event));
		container.addView(panoViewer);

		btnDeviceRotationEnabled = UiUtils.createToggleButton(this,
			R.drawable.ic_move, R.drawable.ic_compass,
			0, 0, UiUtils.mmToPx(this, 6), UiUtils.mmToPx(this, 6), UiUtils.mmToPx(this, 2),
			Gravity.BOTTOM | Gravity.LEFT,
			(btn, isChecked) -> {
				panoRenderer.enableDeviceRotation(isChecked);
				rotationSensor.setEnabled(PanoramicActivity.this, isChecked);
			});

		ToggleButton btnLinearMode = UiUtils.createToggleButton(this,
			R.drawable.ic_spherical, R.drawable.ic_linear,
			UiUtils.mmToPx(this, 8), 0, UiUtils.mmToPx(this, 6),
			UiUtils.mmToPx(this, 6), UiUtils.mmToPx(this, 2),
			Gravity.BOTTOM | Gravity.LEFT,
			(btn, isChecked) -> panoRenderer.enableLinearMode(isChecked));
		
		container.addView(btnDeviceRotationEnabled);
		container.addView(btnLinearMode);
		
		setContentView(container);
		
		Uri uri = getIntent().getData();
		File file = (uri != null) ? FileUtils.getFileFromUri(this, uri) : null;
		if (file != null) {
			setTitle(file.getName());
			panoRenderer.setImage(file);
		}
		else {
			Toast.makeText(this, R.string.no_file, Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		rotationSensor.setEnabled(this, false);
		timer.cancel();
		timer = null;
		prevTime = 0;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		rotationSensor.setEnabled(this, btnDeviceRotationEnabled.isChecked());
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				long currTime = System.currentTimeMillis();
				if (prevTime == 0) {
					prevTime = currTime;
				}
				float deltaTime = (currTime - prevTime) / 1000.0f;
				prevTime = currTime;

				panoRenderer.onTimer(deltaTime);
			}
		}, 0, Constants.PANORAMIC_TIMER_INTERVAL);
	}
	
	@Override
	protected void onDestroy() {
		panoRenderer.destroy();
		super.onDestroy();
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
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		int orientation = 90 * getWindowManager().getDefaultDisplay().getRotation();
		panoRenderer.onDeviceOrientationChanged(orientation);
	}
	
	@Override
	public void onDeviceRotationChanged(float[] matrix4x4) {
		panoRenderer.onDeviceRotationChanged(matrix4x4);
	}

	private static class PanoramicView extends GLSurfaceView {
		public PanoramicView(Context context, GLSurfaceView.Renderer renderer) {
			super(context);
			setPreserveEGLContextOnPause(true);
			setEGLContextClientVersion(2);
			setRenderer(renderer);
			setRenderMode(RENDERMODE_WHEN_DIRTY);
		}
	}
}
