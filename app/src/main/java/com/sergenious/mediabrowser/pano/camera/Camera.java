package com.sergenious.mediabrowser.pano.camera;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;

import com.sergenious.mediabrowser.pano.matrix.Matrix;
import com.sergenious.mediabrowser.pano.matrix.Matrix.MatrixMode;
import com.sergenious.mediabrowser.pano.model.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Smooth dynamic camera, controlled by mouse events.
 * Directly modifies the WebGL matrices.
 */
public class Camera {
	private final static float CAMERA_INERTIA = 0.05f;
	private final static float ROTATE_INERTIA = 0.05f;

	protected final Vector3D position = new Vector3D(); // real current viewing position
	protected final Vector3D positionTarget = new Vector3D(); // target position, to which the real position is persistently moving. When diff < threshold, the updating stops
	protected final Vector3D positionSpeed = new Vector3D(); // extra speed to integrate the target
	protected final Vector3D positionSpeedInternal = new Vector3D(); // extra speed to integrate the target
	protected float rotationHorz, rotationVert; // real current viewing angles
	protected float rotationHorzTarget, rotationVertTarget; // target angles, to which the real viewing angles are persistently moving. When diff < threshold, the updating stops
	protected boolean isPanoramicMode = false;

	private final Vector3D scaleFactor = new Vector3D(1, 1, 1); // constant factors scale the speed in X and Y direction, and to scale the normal.Z coordinate
	private double rotateFactor = 1.0f;
	private float minRotationVert, maxRotationVert;
	private float cameraZMax;
	private final CameraEventHandler eventHandler;
	private final float screenDensityDpi;

	public Camera(float screenDensityDpi, float minRotationVert, float maxRotationVert) {
		this.screenDensityDpi = screenDensityDpi;
		this.minRotationVert = minRotationVert;
		this.maxRotationVert = maxRotationVert;

		eventHandler = new CameraEventHandler();
		cameraZMax = Float.NaN;
		setPosition(Float.NaN, Float.NaN, Float.NaN);
	}

	public void enableDeviceRotation(boolean enabled) {
		if (enabled) {
			rotationHorzTarget = rotationVertTarget = 0;
		}
	}

	public CameraEventHandler getEventHandler() {
		return eventHandler;
	}

	public boolean onTimer(float deltaTime) {
		return updateDynamicValues(deltaTime, true);
	}

	public void setProjectionMatrix(boolean mirror, float nearPlane, float farPlane, float widthRatio, float heightRatio) {
		Matrix.setMatrixMode(MatrixMode.PROJECTION);
		Matrix.loadIdentity();
		Matrix.frustum(-widthRatio * nearPlane * (mirror ? -1 : +1), widthRatio * nearPlane * (mirror ? -1 : +1),
			-heightRatio * nearPlane, heightRatio * nearPlane, nearPlane, farPlane);
	}

	protected void setNormalMatrix(float[] normalMatrix) {
		android.opengl.Matrix.transposeM(Matrix.get(MatrixMode.NORMAL), 0, normalMatrix, 0);
	}

	public boolean isPositionSet() {
		return !Double.isNaN(positionTarget.z);
	}

	public float getNearPlane() {
		return (float) position.z * 0.1f;
	}

	public float getFarPlane() {
		float nearPlane = getNearPlane();
		float farPlane = (float) position.z * 100;
		if (farPlane < nearPlane) {
			farPlane = nearPlane + 1;
		}
		return farPlane;
	}

	public float getRotationHorz() {
		return this.rotationHorz;
	}

	public float getRotationVert() {
		return this.rotationVert;
	}

	public void setCameraMaxZ(float maxZ) {
		cameraZMax = maxZ;
	}

	public Vector3D getPosition() {
		return position;
	}

	public void setPosition(double x, double y, double z) {
		this.position.x = x;
		this.position.y = y;
		this.position.z = z;
		setPositionTarget(this.position);
	}

	public void setPositionTarget(Vector3D position) {
		if (position != null) {
			this.positionTarget.x = position.x;
			this.positionTarget.y = position.y;
			setPositionZTarget(position.z);
		}
	}

	public void setPositionZ(double z) {
		position.z = z;
		if (!Float.isNaN(cameraZMax) && (position.z > cameraZMax)) {
			position.z = cameraZMax;
		}
		setPositionZTarget(z);
	}

	public void setPositionZTarget(double z) {
		positionTarget.z = z;
		//if (positionTarget.z < 10) positionTarget.z = 10;
		if (!Float.isNaN(cameraZMax) && (positionTarget.z > cameraZMax)) {
			positionTarget.z = cameraZMax;
		}
	}

	public void setRotation(float rotH, float rotV) {
		rotationHorz = rotH;
		rotationVert = rotV;
		if (rotationVert < minRotationVert) {
			rotationVert = minRotationVert;
		}
		if (rotationVert > maxRotationVert) {
			rotationVert = maxRotationVert;
		}
		setRotationTarget(rotH, rotV);
	}

	public void setRotationTarget(float rotH, float rotV) {
		rotationHorzTarget = rotH;
		rotationVertTarget = rotV;
		if (rotationVertTarget < minRotationVert) {
			rotationVertTarget = minRotationVert;
		}
		if (rotationVertTarget > maxRotationVert) {
			rotationVertTarget = maxRotationVert;
		}
	}

	public void setRotateFactor(double rotateFactor) {
		this.rotateFactor = rotateFactor;
	}

	public void setMinRotationVert(float degrees) {
		this.minRotationVert = degrees;
	}

	public void setMaxRotationVert(float degrees) {
		this.maxRotationVert = degrees;
	}

	public boolean updateDynamicValues(float deltaTime, boolean allowHumanControl) {
		if (!isPositionSet()) {
			return false;
		}
		boolean isContinuous = false;

		float rotateFactor = Math.min(1, deltaTime / ROTATE_INERTIA);
		float moveFactor = Math.min(1, deltaTime / CAMERA_INERTIA);

		if (allowHumanControl) {
			float speedX = (float) (positionSpeed.x + positionSpeedInternal.x);
			float speedY = (float) (positionSpeed.y + positionSpeedInternal.y);
			float speedZ = (float) (positionSpeed.z + positionSpeedInternal.z);

			float sin = (float) Math.sin(rotationHorz * Math.PI / 180);
			float cos = (float) Math.cos(rotationHorz * Math.PI / 180);
			positionTarget.x += deltaTime * scaleFactor.x * positionTarget.z * (cos * speedX + sin * speedY);
			positionTarget.y += deltaTime * scaleFactor.y * positionTarget.z * (cos * speedY - sin * speedX);
			positionTarget.z += deltaTime * scaleFactor.z * speedZ;
			if (!Float.isNaN(cameraZMax) && (positionTarget.z > cameraZMax)) {
				positionTarget.z = cameraZMax;
			}
		}

		if (rotationVertTarget < minRotationVert) {
			rotationVertTarget = minRotationVert;
		}
		if (rotationVertTarget > maxRotationVert) {
			rotationVertTarget = maxRotationVert;
		}

		if ((Math.abs(positionTarget.x - position.x) > 0.01f)
			|| (Math.abs(positionTarget.y - position.y) > 0.01f)
			|| (Math.abs(positionTarget.z - position.z) > 0.01f)) {

			position.x += moveFactor * (positionTarget.x - position.x);
			position.y += moveFactor * (positionTarget.y - position.y);
			position.z += rotateFactor * (positionTarget.z - position.z);
			isContinuous = true;
		}
		else {
			position.x = positionTarget.x;
			position.y = positionTarget.y;
			position.z = positionTarget.z;
		}

		if ((Math.abs(rotationHorz - rotationHorzTarget) > 0.01f) 
			|| (Math.abs(rotationVert - rotationVertTarget) > 0.01f)) {
			
			rotationHorz = fixAngle(rotationHorz, rotationHorzTarget);
			rotationVert = fixAngle(rotationVert, rotationVertTarget);
			
			rotationHorz += rotateFactor * (rotationHorzTarget - rotationHorz);
			rotationVert += rotateFactor * (rotationVertTarget - rotationVert);
			isContinuous = true;
		}
		else {
			rotationHorz = rotationHorzTarget;
			rotationVert = rotationVertTarget;
		}
		
		return isContinuous;
	}

	private static float fixAngle(float angle, float refAngle) {
		while (angle - refAngle > 180) {
			angle -= 360;
		}
		while (angle - refAngle < -180) {
			angle += 360;
		}
		return angle;
	}

	public class CameraEventHandler {
		private final Map<Integer, PointerCoords> startMultiCoords = new HashMap<>();
		private float currCenterX, currCenterY;
		private float maxMoveDistance;
		private Consumer<PointF> onClickListener;

		public void setOnClickListener(Consumer<PointF> onClickListener) {
			this.onClickListener = onClickListener;
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

		public boolean onTouchStart(MotionEvent event) {
			for (int i = 0; i < event.getPointerCount(); i++) {
				PointerCoords coords = new PointerCoords();
				event.getPointerCoords(i, coords);
				startMultiCoords.put(event.getPointerId(i), coords);
			}
			currCenterX = currCenterY = 0;
			maxMoveDistance = 0;
			return true;
		}

		public boolean onTouchMove(MotionEvent event) {
			int numCoords = event.getPointerCount();
			List<PointerCoords> newCoordsList = new ArrayList<>(numCoords);
			List<PointerCoords> prevCoordsList = new ArrayList<>(numCoords);

			currCenterX = currCenterY = 0;
			float prevCenterX = 0, prevCenterY = 0;
			for (int i = 0; i < numCoords; i++) {
				PointerCoords coords = new PointerCoords();
				event.getPointerCoords(i, coords);
				newCoordsList.add(coords);
				currCenterX += coords.x;
				currCenterY += coords.y;
				PointerCoords prevCoords = startMultiCoords.get(event.getPointerId(i));
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
			maxMoveDistance = Math.max(maxMoveDistance,
				(float) Math.hypot(currCenterX - prevCenterX, currCenterY - prevCenterY));

			if (numCoords < 2) {
				rotationHorzTarget += (50 / screenDensityDpi) * rotateFactor * (currCenterX - prevCenterX);
				rotationVertTarget -= (50 / screenDensityDpi) * rotateFactor * (currCenterY - prevCenterY);
			}
			else { // 2 or more
				float currDeltaX = (newCoordsList.get(0).x - newCoordsList.get(1).x) * 0.5f;
				float currDeltaY = (newCoordsList.get(0).y - newCoordsList.get(1).y) * 0.5f;
				float prevDeltaX = (prevCoordsList.get(0).x - prevCoordsList.get(1).x) * 0.5f;
				float prevDeltaY = (prevCoordsList.get(0).y - prevCoordsList.get(1).y) * 0.5f;
				float currDistance = (float) Math.sqrt(currDeltaX * currDeltaX + currDeltaY * currDeltaY);
				float prevDistance = (float) Math.sqrt(prevDeltaX * prevDeltaX + prevDeltaY * prevDeltaY);
				float currAngle = (float) (Math.atan2(currDeltaY, currDeltaX) * 180 / Math.PI);
				float prevAngle = (float) (Math.atan2(prevDeltaY, prevDeltaX) * 180 / Math.PI);
				if (prevAngle < currAngle - 180) {
					prevAngle += 360;
				}
				if (prevAngle > currAngle + 180) {
					prevAngle -= 360;
				}

				positionTarget.z *= prevDistance / currDistance;

				if (isPanoramicMode) {
					rotationHorzTarget += (50 / screenDensityDpi) * rotateFactor * (currCenterX - prevCenterX);
					rotationVertTarget -= (50 / screenDensityDpi) * rotateFactor * (currCenterY - prevCenterY);
				}
				else {
					rotationHorzTarget -= currAngle - prevAngle;
					rotationVertTarget += 0.2 * rotateFactor * (currCenterY - prevCenterY);
				}
				rotationVertTarget = Math.min(maxRotationVert, Math.max(minRotationVert, rotationVertTarget));
			}

			startMultiCoords.clear();
			for (int i = 0; i < numCoords; i++) {
				PointerCoords coords = new PointerCoords();
				event.getPointerCoords(i, coords);
				startMultiCoords.put(event.getPointerId(i), coords);
			}
			return true;
		}

		public boolean onTouchEnd(MotionEvent ignoredEvent) {
			startMultiCoords.clear();
			if ((onClickListener != null) && (maxMoveDistance < 5)) { // not moved (much), toggle the full screen
				onClickListener.accept(new PointF(currCenterX, currCenterY));
			}
			return true;
		}
	}
}
