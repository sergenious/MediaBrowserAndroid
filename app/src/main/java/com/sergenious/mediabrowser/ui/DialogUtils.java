package com.sergenious.mediabrowser.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sergenious.mediabrowser.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class DialogUtils {
	public static void showErrorDialog(Context ctx, String title, String message,
		final DialogInterface.OnClickListener clickListener) {

		showDialog(ctx, android.R.drawable.ic_dialog_alert, title, message, clickListener);
	}

	public static void showDialog(final Context ctx, final int iconResId,
		final String title, final String message, final DialogInterface.OnClickListener clickListener) {

		new Handler(Looper.getMainLooper()).post(() -> new AlertDialog.Builder(ctx)
			.setTitle(title)
			.setMessage(message)
			.setPositiveButton(android.R.string.ok, (dialog, which) -> {
				if (clickListener != null) {
					clickListener.onClick(dialog, which);
				}
			})
			.setIcon(iconResId)
			.show());
	}

	public static void showProgressDialog(Activity activity, String title, double maxProgress,
		BiFunction<BiConsumer<String, Double>, Supplier<Boolean>, Runnable> taskRunnable) {

		Handler handler = new Handler(Looper.getMainLooper());
		ExecutorService executor = Executors.newSingleThreadExecutor();
		AtomicBoolean dialogCanceled = new AtomicBoolean(false);

		Dialog progressDialog = new AlertDialog.Builder(activity)
			.setView(R.layout.progress)
			.setTitle(title)
			.create();

		progressDialog.setCanceledOnTouchOutside(true);
		progressDialog.setOnDismissListener(dlg -> dialogCanceled.set(true));
		progressDialog.setOnCancelListener(dlg -> dialogCanceled.set(true));

		Runnable showDialogTask = () -> activity.runOnUiThread(() -> {
			progressDialog.show();
			ProgressBar progressBar = progressDialog.findViewById(R.id.progressValue);
			progressBar.setMax(1000);
		});

		handler.postDelayed(showDialogTask, 200);

		BiConsumer<String, Double> progressUpdater = (text, progress) -> activity.runOnUiThread(() -> {
			if (progressDialog.isShowing()) {
				TextView progressContent = progressDialog.findViewById(R.id.progressContent);
				progressContent.setText(text);
				ProgressBar progressBar = progressDialog.findViewById(R.id.progressValue);
				progressBar.setProgress((int) (1000 * progress / maxProgress));
			}
		});

		executor.execute(() -> {
			taskRunnable.apply(progressUpdater, dialogCanceled::get).run();
			handler.removeCallbacks(showDialogTask);
			activity.runOnUiThread(progressDialog::hide);
		});
	}
}


