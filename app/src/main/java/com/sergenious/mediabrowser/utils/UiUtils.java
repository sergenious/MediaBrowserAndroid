package com.sergenious.mediabrowser.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class UiUtils {
	public static final DecimalFormat VALUE_FORMATTER = new DecimalFormat("0.######");

	public static String padStringLeft(String str, int maxLength, char padChar) {
		StringBuilder s = new StringBuilder(str);
		for (int i = s.length(); i < maxLength; i++) {
			s.insert(0, padChar);
		}
		return s.toString();
	}

	public static String valueToString(Object value) {
		if (value == null) {
			return "/";
		}
		else if (value.getClass().isArray()) {
			return arrayToString(value);
		}
		else if (value instanceof Number) {
			return VALUE_FORMATTER.format(value);
		}
		return value.toString();
	}

	public static String arrayToString(Object array) {
		StringBuilder s = new StringBuilder();
		int len = Array.getLength(array);
		for (int i = 0; i < len; i++) {
			if (i > 0) {
				s.append(", ");
			}
			Object item = Array.get(array, i);
			s.append((item != null) ? valueToString(item) : "/");
		}
		return s.toString();
	}

	public static String durationToString(double seconds) {
		int hours = (int) (seconds / 3600);
		seconds -= hours * 3600;
		int minutes = (int) (seconds / 60);
		seconds -= minutes * 60;

		if (hours > 0) {
			return hours + ":" + padStringLeft(Integer.toString(minutes), 2, '0') + ":"
				+ padStringLeft(VALUE_FORMATTER.format(seconds), 2, '0');
		}
		else if (minutes > 0) {
			return minutes + ":" + padStringLeft(VALUE_FORMATTER.format(seconds), 2, '0');
		}
		return VALUE_FORMATTER.format(seconds);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getSharedPreference(Context context, String key, T defaultValue) {
		SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
		if (defaultValue instanceof Collection) {
			return (T) prefs.getStringSet(key, new TreeSet<>((Collection<String>) defaultValue));
		}
		if (defaultValue instanceof String) {
			return (T) prefs.getString(key, (String) defaultValue);
		}
		if (defaultValue instanceof Integer) {
			return (T) (Integer) prefs.getInt(key, (Integer) defaultValue);
		}
		if (defaultValue instanceof Long) {
			return (T) (Long) prefs.getLong(key, (Long) defaultValue);
		}
		if (defaultValue instanceof Float) {
			return (T) (Float) prefs.getFloat(key, (Float) defaultValue);
		}
		if (defaultValue instanceof Boolean) {
			return (T) (Boolean) prefs.getBoolean(key, (Boolean) defaultValue);
		}
		return defaultValue;
	}

	public static <T> void setSharedPreference(Context context, String key, T value) {
		SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		if (value instanceof Collection) {
			@SuppressWarnings("unchecked")
			Collection<String> stringArray = (Collection<String>) value;
			editor.putStringSet(key, new TreeSet<>(stringArray));
		}
		else if (value instanceof String) {
			editor.putString(key, (String) value);
		}
		else if (value instanceof Integer) {
			editor.putInt(key, (Integer) value);
		}
		else if (value instanceof Long) {
			editor.putLong(key, (Long) value);
		}
		else if (value instanceof Float) {
			editor.putFloat(key, (Float) value);
		}
		else if (value instanceof Boolean) {
			editor.putBoolean(key, (Boolean) value);
		}
		editor.apply();
	}

	public static void requestFullScreen(Activity activity, boolean fullScreen) {
		final int flags = fullScreen
			? View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			: View.SYSTEM_UI_FLAG_VISIBLE;

		int layoutFullscreen = fullScreen ? WindowManager.LayoutParams.FLAG_FULLSCREEN : 0;
		activity.getWindow().setFlags(layoutFullscreen, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		activity.getWindow().getDecorView().setSystemUiVisibility(fullScreen ? flags : 0);

		if (activity.getActionBar() != null) {
			if (fullScreen) {
				activity.getActionBar().hide();
			}
			else {
				activity.getActionBar().show();
			}
		}

		final View decorView = activity.getWindow().getDecorView();
		decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
			if (fullScreen && ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)) {
				decorView.setSystemUiVisibility(flags);
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.TIRAMISU)
	public static boolean requestReadMediaPermissions(Activity activity) {
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && !Environment.isExternalStorageManager()) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
				Uri.parse("package:" + activity.getPackageName()));
			activity.startActivity(intent);
			return false;
		}

		return requestPermissions(activity, Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,
			Manifest.permission.READ_MEDIA_AUDIO);
	}

	public static boolean requestPermissions(Activity activity, String... permissions) {
		List<String> notGrantedPermissions = new ArrayList<>();
		for (String permission: permissions) {
			if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				notGrantedPermissions.add(permission);
			}
		}
		if (!notGrantedPermissions.isEmpty()) {
			activity.requestPermissions(notGrantedPermissions.toArray(new String[0]), 1);
			return true;
		}
		return false;
	}

	public static int dpToPx(Context ctx, float amount) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
			amount, ctx.getResources().getDisplayMetrics());
	}

	public static int mmToPx(Context ctx, float amount) {
		return (int) Math.ceil(amount * ctx.getResources().getDisplayMetrics().densityDpi / 25.4);
	}
	
	public static ToggleButton createToggleButton(Context ctx, final int uncheckedResId, final int checkedResId,
		int xOfs, int yOfs, int width, int height, int margin, int gravity,
		final CompoundButton.OnCheckedChangeListener clickHandler) {
		
		ToggleButton btn = new ToggleButton(ctx);
		btn.setText("");
		btn.setTextOn("");
		btn.setTextOff("");
		btn.setBackgroundResource(uncheckedResId);
		FrameLayout.LayoutParams btnLayoutParams = new FrameLayout.LayoutParams(width, height);
		btnLayoutParams.gravity = gravity;
		btnLayoutParams.setMargins(margin + xOfs, margin + yOfs, margin, margin);
		btn.setLayoutParams(btnLayoutParams);
		
		btn.setOnCheckedChangeListener((btn1, isChecked) -> {
			btn1.setBackgroundResource(isChecked ? checkedResId : uncheckedResId);
			if (clickHandler != null) {
				clickHandler.onCheckedChanged(btn1, isChecked);
			}
		});
		
		return btn;
	}
}
