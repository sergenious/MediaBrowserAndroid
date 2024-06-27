package com.sergenious.mediabrowser.pano;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;

import com.sergenious.mediabrowser.Constants;
import com.sergenious.mediabrowser.R;
import com.sergenious.mediabrowser.pano.camera.Camera;
import com.sergenious.mediabrowser.pano.matrix.Matrix;
import com.sergenious.mediabrowser.pano.matrix.Matrix.MatrixMode;
import com.sergenious.mediabrowser.pano.mesh.AbstractVertexMesh;
import com.sergenious.mediabrowser.pano.mesh.SphereMesh;
import com.sergenious.mediabrowser.pano.shader.ShaderList;
import com.sergenious.mediabrowser.ui.DialogUtils;
import com.sergenious.mediabrowser.utils.MediaUtils;

import java.io.File;
import java.util.function.Consumer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PanoramicRenderer implements GLSurfaceView.Renderer {
	private static final float MIN_Z = 20;

	private final Context ctx;
	private final Runnable requestRenderFunc;
	private final Consumer<PointF> onClickListener;
	private Camera camera;
	private ShaderList shaderList;
	private float frustumWidthRatio, frustumHeightRatio;
	private boolean canUseDeviceRotation = false;
	private boolean hasGlContext = false;
	private SphereMesh sphereMesh;
	private File imageFile = null;
	private int deviceOrientation = 0;
	private float[] rotationMatrix = null;
	private boolean isLinearMode = false;
	
	public PanoramicRenderer(Context ctx, Runnable requestRenderFunc, Consumer<PointF> onClickListener) {
		this.ctx = ctx;
		this.requestRenderFunc = requestRenderFunc;
		this.onClickListener = onClickListener;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		hasGlContext = true;
		init(ctx);
		checkGLError();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		setViewportSize(width, height);
		requestRenderFunc.run();
		checkGLError();
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if (shaderList == null) {
			return;
		}

		checkGLError();
		Matrix.resetAll();
		shaderList.useNoShader();
		GLES20.glClearColor(0, 0, 0, 1.0f);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		checkGLError();

		renderInternal();

		GLES20.glFinish();
		GLES20.glFlush();
		checkGLError();
	}

	/** Enables or disabled 3D camera rotation binding to the device's orientation sensors.
	 * If disabled, camera rotation is done manually via mouse or touch events. Default: disabled. */
	public void enableDeviceRotation(boolean enabled) {
		this.canUseDeviceRotation = enabled;
		if (camera != null) {
			camera.enableDeviceRotation(enabled);
		}
		requestRenderFunc.run();
	}

	public void onTimer(float deltaTime) {
		if (camera != null) {
			if (camera.onTimer(deltaTime)) {
				requestRenderFunc.run();
			}
		}
	}

	public boolean onTouchEvent(MotionEvent event) {
		return (camera != null) && camera.getEventHandler().onTouchEvent(event);
	}

	protected void init(Context ctx) {
		shaderList = new ShaderList(ctx);

		camera = new PanoCamera(ctx.getResources().getDisplayMetrics().densityDpi);
		camera.setCameraMaxZ(1000);
		camera.setPosition(0, 0, 500);
		camera.setRotation(0, 0);
		camera.setMinRotationVert(-90);
		camera.setMaxRotationVert(+90);
		camera.getEventHandler().setOnClickListener(onClickListener);
		
		if (imageFile != null) {
			setImage(imageFile);
		}
	}

	public void destroy() {
		try {
			if (sphereMesh != null) {
				sphereMesh.destroy();
			}
			if (shaderList != null) {
				shaderList.clear();
			}
		}
		catch (Exception e) {
			Log.e(Constants.appNameInternal, "Exception while closing", e);
		}

		sphereMesh = null;
		camera = null;
		shaderList = null;
	}
	
	public void enableLinearMode(boolean enabled) {
		this.isLinearMode = enabled;
		requestRenderFunc.run();
	}
	
	public void onDeviceRotationChanged(float[] rotationMatrix) {
		this.rotationMatrix = rotationMatrix;
		requestRenderFunc.run();
	}
	
	public void onDeviceOrientationChanged(int orientation) {
		this.deviceOrientation = orientation;
		requestRenderFunc.run();
	}
	
	public void setImage(File file) {
		if (!hasGlContext) {
			this.imageFile = file;
		}
		else {
			try {
				int maxTextureSize = getMaxTextureSize();
				Pair<RectF, Bitmap> imageData = MediaUtils.loadImageWithPano(ctx,
					file, maxTextureSize, maxTextureSize, false, false);

				if (imageData != null) {
					setImage(imageData.second, imageData.first);
				}
				else {
					DialogUtils.showErrorDialog(ctx, ctx.getString(R.string.error),
						ctx.getString(R.string.error_loading_image), null);
				}
			}
			catch (Throwable e) {
				Log.e(Constants.appNameInternal, "Error loading image", e);
				DialogUtils.showErrorDialog(ctx, ctx.getString(R.string.error),
					ctx.getString(R.string.error_loading_image) + ":\n" + e.getMessage(), null);
			}
		}
	}
	
	public void setImage(Bitmap bitmap, RectF panoRect) {
		if (sphereMesh != null) {
			sphereMesh.destroy();
			sphereMesh = null;
		}
		
		if (bitmap != null) {
			int numXSegments = (int) Math.max(1, 128 * (panoRect.right - panoRect.left) / 360);
			int numYSegments = (int) Math.max(1, 32 * (panoRect.bottom - panoRect.top) / 180);
	
			if (camera != null) {
				camera.setMinRotationVert(panoRect.top);
				camera.setMaxRotationVert(panoRect.bottom);
			}

			sphereMesh = new SphereMesh(1000, panoRect, numXSegments, numYSegments);
			sphereMesh.loadImage(bitmap);
			checkGLError();
		}
	}

	private void renderInternal() {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_FRONT);
		resetTextures();

		if (camera.isPositionSet() && (camera.getPosition().z < MIN_Z)) {
			camera.setPositionZ(MIN_Z);
		}
		((PanoCamera) camera).setMatrices(false);

		if (sphereMesh != null) {
			sphereMesh.render(null, shaderList, 0, 1);
		}
	}

	private static float getFrustumWidthRatio(float width, float height) {
		return (width > height) ? width / height : 1.0f;
	}

	private static float getFrustumHeightRatio(float width, float height) {
		return (width < height) ? height / width : 1.0f;
	}

	private void setViewportSize(int width, int height) {
		int screenWidth = Math.min(width, height * 4);
		int screenHeight = Math.min(height, screenWidth * 4);
		int screenOfsX = (width - screenWidth) >> 1;
		int screenOfsY = (height - screenHeight) >> 1;
		if ((screenWidth <= 1) || (screenHeight <= 1)) {
			return; // invalid window size
		}

		frustumWidthRatio = getFrustumWidthRatio(screenWidth, screenHeight);
		frustumHeightRatio = getFrustumHeightRatio(screenWidth, screenHeight);
		GLES20.glViewport(screenOfsX, screenOfsY, screenWidth, screenHeight);
	}

	private void checkGLError() {
		int error = GLES20.glGetError();
		if (error != GLES20.GL_NO_ERROR) {
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			String info = " at " + ((stackTrace.length > 3) ? stackTrace[3].getFileName()
				+ " (" + stackTrace[3].getLineNumber() + ")" : "");
			Log.e(Constants.appNameInternal, "OpenGL error" + info + ":"
				+ GLU.gluErrorString(error) + " (" + error + ")");
		}
	}

	private int getMaxTextureSize() {
		int[] maxTextureSize = new int[1];
		GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
		return maxTextureSize[0];
	}

	private void resetTextures() {
		for (int texIndex = AbstractVertexMesh.MAX_TEX_UNITS - 1; texIndex >= 0; texIndex--) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + texIndex);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		}
	}

	private class PanoCamera extends Camera {
		public PanoCamera(float screenDensityDpi) {
			super(screenDensityDpi, -90, 90);
			isPanoramicMode = true;
		}

		public void setMatrices(boolean mirror) {
			setRotateFactor(camera.getPosition().z / 500);

			float maxFovAngle = isLinearMode ? 150 : 120;
			float fovAngleTangens = (float) Math.tan(maxFovAngle * camera.getPosition().z * Math.PI / (1000 * 360));
			setProjectionMatrix(mirror, getNearPlane() * 0.8f, getFarPlane(),
				frustumWidthRatio * fovAngleTangens, frustumHeightRatio * fovAngleTangens);

			Matrix.setMatrixMode(MatrixMode.MODELVIEW);
			Matrix.loadIdentity();

			if (!isLinearMode) {
				// spherical distortion
				Matrix.translate(0, 0, -camera.getPosition().z);
			}
			
			if (canUseDeviceRotation && (rotationMatrix != null)) {
				Matrix.rotate(deviceOrientation, 0, 0, 1);
				Matrix.multiply(rotationMatrix);
				Matrix.rotate(180, 0, 1, 0);
			}
			else {
				Matrix.rotate(camera.getRotationVert() + 90, 1, 0, 0);
			}
			Matrix.rotate(-camera.getRotationHorz() + 90, 0, 0, -1);

			float[] inverseModelViewMatrix = new float[16];
			android.opengl.Matrix.invertM(inverseModelViewMatrix, 0, Matrix.get(MatrixMode.MODELVIEW), 0);
			setNormalMatrix(inverseModelViewMatrix);
		}
	}
}
