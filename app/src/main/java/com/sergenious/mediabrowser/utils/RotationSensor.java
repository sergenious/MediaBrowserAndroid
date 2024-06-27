package com.sergenious.mediabrowser.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class RotationSensor implements SensorEventListener {
	private final float[] rotationMatrix = new float[4 * 4];
	private final OnDeviceRotationChanged changeHandler;
		
	public interface OnDeviceRotationChanged {
		void onDeviceRotationChanged(float[] matrix4x4);
	}
	
	public RotationSensor(OnDeviceRotationChanged changeHandler) {
		this.changeHandler = changeHandler;
	}
	
	public void setEnabled(Context ctx, boolean isEnabled) {
		SensorManager sensorMan = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		if (isEnabled) {
			sensorMan.registerListener(this, sensorMan.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_FASTEST);
		}
		else {
			sensorMan.unregisterListener(this);
		}
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
			changeHandler.onDeviceRotationChanged(rotationMatrix);
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}	
}
