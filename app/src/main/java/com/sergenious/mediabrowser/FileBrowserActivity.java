package com.sergenious.mediabrowser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.sergenious.mediabrowser.filebrowser.FileMultiChoiceListener;
import com.sergenious.mediabrowser.ui.DialogUtils;
import com.sergenious.mediabrowser.ui.DirectoryChooserView;
import com.sergenious.mediabrowser.ui.adapter.GridAdapter;
import com.sergenious.mediabrowser.ui.gesture.SimpleScaleGestureDetector;
import com.sergenious.mediabrowser.utils.FileUtils;
import com.sergenious.mediabrowser.utils.FileUtils.FileInfo;
import com.sergenious.mediabrowser.utils.MediaUtils;
import com.sergenious.mediabrowser.utils.ThumbnailsDatabase;
import com.sergenious.mediabrowser.utils.UiUtils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FileBrowserActivity extends Activity {
	public static final String START_DIRECTORY = "START_DIR";
	public static final String ROOT_DIRECTORY = "ROOT_DIR";
	private static final String PREF_COLUMN_WIDTH = "columnWidth";
	private static final String PREF_FILE_SORT_MODE = "fileSortMode";
	private static final String PREF_SHOW_THUMBNAIL_NAMES = "showThumbnailNames";
	private static final String PREF_SHORTCUTS = "shortcuts";

	private File currentDirectory;
	private File currentRootDir;
	private Map<File, FileInfo> rootDirectories;
	private Set<File> shortcuts;
	private FileUtils.FileSortMode fileSortMode = FileUtils.FileSortMode.PATH_DIRS_FILES;
	private GridView fileGridView;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private SimpleScaleGestureDetector scaleGestureDetector;
	private LayoutInflater inflater;
	private ThumbnailsDatabase thumbnailsDatabase;
	private ScheduledExecutorService thumbnailLoadingExecutor;
	private final Map<View, ScheduledFuture<?>> thumbnailLoadingTasks = new ConcurrentHashMap<>();
	private boolean showThumbnailNames;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_browser);
		setTitle(R.string.files);

		thumbnailsDatabase = ThumbnailsDatabase.getInstance(this.getApplicationContext());
		thumbnailLoadingExecutor = new ScheduledThreadPoolExecutor(Constants.NUM_IMAGE_LOADING_THREADS);

		getActionBar().setDisplayHomeAsUpEnabled(false);
		getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.gradient_appbar, getTheme()));

		boolean hasNoPermission = UiUtils.requestReadMediaPermissions(this);

		fileSortMode = FileUtils.FileSortMode.valueOf(UiUtils.getSharedPreference(this, PREF_FILE_SORT_MODE,
			FileUtils.FileSortMode.PATH_DIRS_FILES.name()));

		String currDirPath = getIntent().getStringExtra(START_DIRECTORY);
		currentDirectory = (currDirPath != null) ? new File(currDirPath) : null;

		String rootDirPath = getIntent().getStringExtra(ROOT_DIRECTORY);
		currentRootDir = (rootDirPath != null) ? new File(rootDirPath) : null;

		rootDirectories = FileUtils.getRootDirectories(this);

		shortcuts = UiUtils.getSharedPreference(this, PREF_SHORTCUTS, Collections.<String>emptySet())
			.stream()
			.map(File::new)
			.collect(Collectors.toCollection(TreeSet::new));

		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		fileGridView = findViewById(R.id.fileBrowserList);
		fileGridView.setEmptyView(findViewById(R.id.fileBrowserEmpty));
		fileGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		fileGridView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
			checkFileItemsVisibility());
		fileGridView.setOnItemClickListener((adapter, v, position, id) ->
			onGridItemClick((FileInfo) adapter.getItemAtPosition(position)));
		fileGridView.setMultiChoiceModeListener(new FileMultiChoiceListener(fileGridView,
			isActionMode -> {}, this::getFileMultiChoiceActionVisibility, this::onFileMultiChoiceAction));

		scaleGestureDetector = new SimpleScaleGestureDetector(this, 0, 150, 1000, columnWidth -> {
			@SuppressWarnings("unchecked")
			GridAdapter<FileInfo> adapter = (GridAdapter<FileInfo>) fileGridView.getAdapter();
			adapter.invalidate();
			fileGridView.setColumnWidth(columnWidth);
			UiUtils.setSharedPreference(this, PREF_COLUMN_WIDTH, columnWidth);
		});
		fileGridView.setOnTouchListener(scaleGestureDetector);

		ImageView loadingIcon = findViewById(R.id.fileBrowserEmptyIcon);
		RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		rotate.setDuration(2000);
		rotate.setRepeatCount(RotateAnimation.INFINITE);
		rotate.setInterpolator(new LinearInterpolator());
		loadingIcon.startAnimation(rotate);

		showThumbnailNames = UiUtils.getSharedPreference(this, PREF_SHOW_THUMBNAIL_NAMES, true);

		if (!hasNoPermission) {
			refreshFileList(currentRootDir, currentDirectory);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		refreshFileList(currentRootDir, currentDirectory);
	}

	@Override
	protected void onResume() {
		super.onResume();
		int fileGridColumnWidth = UiUtils.getSharedPreference(this, PREF_COLUMN_WIDTH, 300);
		// set the scale factor, if changed in another activity meanwhile
		if (fileGridColumnWidth != scaleGestureDetector.getScaleFactor()) {
			scaleGestureDetector.setScaleFactor(fileGridColumnWidth);
			fileGridView.setNumColumns(GridView.AUTO_FIT);
			fileGridView.setColumnWidth(fileGridColumnWidth);
			if (fileGridView.getAdapter() != null) {
				((FilesGridAdapter) fileGridView.getAdapter()).invalidate();
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			// cancel all pending thumbnail loading tasks
			Iterator<Map.Entry<View, ScheduledFuture<?>>> iterator = thumbnailLoadingTasks.entrySet().iterator();
			while (iterator.hasNext()) {
				ScheduledFuture<?> thumbnailLoadingFuture = iterator.next().getValue();
				thumbnailLoadingFuture.cancel(false);
				iterator.remove();
			}
		}
		catch (Exception e) {
			Log.e(Constants.appNameInternal, "Error canceling thumbnail loading tasks", e);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (thumbnailLoadingExecutor != null) {
			thumbnailLoadingExecutor.shutdownNow();
			thumbnailLoadingExecutor = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.file_browser_menu, menu);
		menu.findItem(R.id.btnSearch).setVisible(currentDirectory != null);
		menu.findItem(R.id.btnAddShortcut).setVisible(currentDirectory == null);
		menu.findItem(R.id.btnSlideshow).setVisible(currentDirectory != null);
		menu.findItem(R.id.btnSort).setVisible(currentDirectory != null);
		menu.findItem(R.id.btnShowLabels).setVisible(currentDirectory != null);

		SearchView searchView = (SearchView) menu.findItem(R.id.btnSearch).getActionView();
		searchView.setOnCloseListener(() -> {
			refreshFileList(currentRootDir, currentDirectory);
			return false;
		});
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				searchForFiles(query);
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				return true;
			}
		});
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			finish();
			return true;
		}
		if (id == R.id.btnSlideshow) {
			Intent intent = new Intent(this, MediaActivity.class);
			intent.putExtra(MediaActivity.SLIDESHOW_FILES_PARAM,
				(Serializable) Collections.singletonList(currentDirectory));
			this.startActivity(intent);
		}
		if (id == R.id.btnShowLabels) {
			showThumbnailNames = !showThumbnailNames;
			updateThumbnailNamesVisibility(showThumbnailNames);
			UiUtils.setSharedPreference(this, PREF_SHOW_THUMBNAIL_NAMES, showThumbnailNames);
		}
		if (id == R.id.btnSort) {
			openFileSortModeSettings();
		}
		if (id == R.id.btnAddShortcut) {
			DirectoryChooserView.createDialog(this, getString(R.string.add_shortcut_directory),
				null, this::addShortcut);
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean getFileMultiChoiceActionVisibility(int itemId, List<FileInfo> files) {
		final AtomicInteger numMediaSelected = new AtomicInteger(0);
		final AtomicInteger numDirectoriesSelected = new AtomicInteger(0);

		files.forEach(f -> {
			if ((f.file != null) && f.file.isDirectory()) {
				numDirectoriesSelected.incrementAndGet();
			}
			else {
				numMediaSelected.incrementAndGet();
			}
		});

		if (itemId == R.id.btnDeleteShortcut) {
			return (currentDirectory == null) && (files.size() == 1) && !files.get(0).isRootDir;
		}
		if (itemId == R.id.btnShare) {
			return (numMediaSelected.get() > 0) && (numDirectoriesSelected.get() == 0);
		}
		if (itemId == R.id.btnSlideshow) {
			return (numMediaSelected.get() > 0) || (numDirectoriesSelected.get() > 0);
		}
		if (itemId == R.id.btnFileSizes) {
			return (files.size() == 1) && files.get(0).file.isDirectory();
		}
		return true;
	}

	private void onFileMultiChoiceAction(int itemId, List<FileInfo> files) {
		if (itemId == R.id.btnShare) {
			MediaUtils.startShareIntent(this, FileUtils.toFileList(files), null);
		}
		else if (itemId == R.id.btnSlideshow) {
			startSlideshow(FileUtils.toFileList(files));
		}
		else if (itemId == R.id.btnDeleteShortcut) {
			deleteShortcuts(files);
		}
		else if ((itemId == R.id.btnFileSizes) && (files.size() == 1)) {
			Intent intent = new Intent(this, FileSizesActivity.class);
			intent.putExtra(FileSizesActivity.ROOT_DIR_PARAM, files.get(0).file);
			this.startActivity(intent);
		}
	}

	private void startSlideshow(List<File> files) {
		Intent intent = new Intent(this, MediaActivity.class);
		intent.putExtra(MediaActivity.SLIDESHOW_FILES_PARAM, (Serializable) files);
		this.startActivity(intent);
	}

	private void addShortcut(File shortcut) {
		shortcuts.add(shortcut);
		refreshFileList(currentRootDir, currentDirectory);
		UiUtils.setSharedPreference(this, PREF_SHORTCUTS,
			shortcuts.stream().map(File::getAbsolutePath).collect(Collectors.toList()));
	}

	private void deleteShortcuts(List<FileInfo> deletedShortcuts) {
		for (FileInfo deletedShortcut: deletedShortcuts) {
			shortcuts.remove(deletedShortcut.file);
		}
		refreshFileList(currentRootDir, currentDirectory);
		UiUtils.setSharedPreference(this, PREF_SHORTCUTS, shortcuts
			.stream().map(File::getAbsolutePath).collect(Collectors.toList()));
	}

	private void onGridItemClick(FileInfo fileInfo) {
		if (!fileInfo.file.exists()) {
			Toast.makeText(this, R.string.file_not_exists, Toast.LENGTH_SHORT).show();
			return;
		}
		if (fileInfo.file.isDirectory()) {
			File newRootDir = (currentRootDir == null) ? fileInfo.file : currentRootDir;
			File newCurrDir = fileInfo.file;

			Intent intent = new Intent(this, FileBrowserActivity.class);
			intent.putExtra(START_DIRECTORY, newCurrDir.getAbsolutePath());
			intent.putExtra(ROOT_DIRECTORY, newRootDir.getAbsolutePath());
			this.startActivity(intent);
		}
		else {
			Intent intent = new Intent(this, MediaActivity.class);
			intent.setData(Uri.fromFile(fileInfo.file));
			this.startActivity(intent);
		}
	}

	private void searchForFiles(String query) {
		DialogUtils.showProgressDialog(this, getString(R.string.searching_for) + " \"" + query + "\"...", 1.0f,
			(progressUpdater, canceled) -> () -> {
				List<FileInfo> fileInfoList = FileUtils.searchFiles(query, currentDirectory,
					MediaUtils.getAllMediaExtensions(), FileUtils.FileSortMode.PATH, progressUpdater, canceled);

				if (!canceled.get()) {
					runOnUiThread(() -> fileGridView.setAdapter(new FilesGridAdapter(fileInfoList)));
				}
			}
		);
	}

	private void openFileSortModeSettings() {
		List<FileUtils.FileSortMode> sortModeItems = Arrays.asList(FileUtils.FileSortMode.values());
		int selectedModeIndex = sortModeItems.indexOf(fileSortMode);

		String[] sortModeItemNames = sortModeItems.stream()
			.map(sortMode -> getString(sortMode.getLabelResId()))
			.toArray(String[]::new);

		new AlertDialog.Builder(this)
			.setTitle(R.string.sorting)
			.setSingleChoiceItems(sortModeItemNames, selectedModeIndex, (dialog, which) -> {
				fileSortMode = FileUtils.FileSortMode.values()[which];
				UiUtils.setSharedPreference(this, PREF_FILE_SORT_MODE, fileSortMode.name());
				refreshFileList(currentRootDir, currentDirectory);
				dialog.dismiss();
			})
			.show();
	}

	private void updateThumbnailNamesVisibility(boolean visible) {
		for (int i = 0; i < fileGridView.getAdapter().getCount(); i++) {
			FileInfo fileInfo = (FileInfo) fileGridView.getAdapter().getItem(i);
			if ((fileInfo != null) && fileInfo.file.isFile()) {
				View row = fileGridView.getAdapter().getView(i, null, null);
				TextView fileNameView = row.findViewById(R.id.itemFileName);
				ImageView imageView = row.findViewById(R.id.itemIcon);
				fileNameView.setVisibility(visible || (imageView.getDrawable() == null) ? View.VISIBLE : View.INVISIBLE);
			}
		}
	}

	private void refreshFileList(File currRootDir, File currDir) {
		((TextView) findViewById(R.id.fileBrowserEmptyLabel)).setText(R.string.loading);
		findViewById(R.id.fileBrowserEmptyIcon).setVisibility(View.VISIBLE);
		handler.post(() -> refreshFileListInternal(currRootDir, currDir));
	}

	private void refreshFileListInternal(File currRootDir, File currDir) {
		getActionBar().setDisplayHomeAsUpEnabled(currDir != null);

		if (currDir == null) { // list root folders
			setTitle(getResources().getString(R.string.root_folders));
			List<FileInfo> rootItems = new ArrayList<>(rootDirectories.values());
			for (File shortcut: shortcuts) {
				rootItems.add(new FileInfo(shortcut, false, shortcut.getName(), R.drawable.ic_shortcut, 0));
			}
			fileGridView.setAdapter(new FilesGridAdapter(rootItems));
		}
		else { // list directories and files
			List<FileInfo> fileInfoList = FileUtils.getFileList(currDir,
				MediaUtils.getAllMediaExtensions(), true, fileSortMode);
			fileGridView.setAdapter(new FilesGridAdapter(fileInfoList));

			FileInfo currRootInfo = rootDirectories.get(currRootDir);
			String title = currDir.equals(currRootDir) && (currRootInfo != null) ? currRootInfo.name : currDir.getName();
			setTitle(title + " [" + fileInfoList.size() + "]");
		}

		((TextView) findViewById(R.id.fileBrowserEmptyLabel)).setText(R.string.no_media_files);
		findViewById(R.id.fileBrowserEmptyIcon).setVisibility(View.GONE);
	}

	private void loadAndSetThumbnail(final ImageView imgView, final TextView fileNameView, String fileName) {
		try {
			long fileSize = new File(fileName).length();
			Bitmap thumbnail = thumbnailsDatabase.loadThumbnail(fileName, fileSize);
			if (thumbnail == null) {
				final Pair<Pair<Size, Integer>, Bitmap> imageData = MediaUtils.loadThumbnailImage(this, new File(fileName), true);

				if ((imageData != null) && (imageData.second != null)) {
					thumbnail = imageData.second;
					thumbnailsDatabase.saveThumbnail(fileName, fileSize, thumbnail);
				}
			}

			if (thumbnail != null) {
				final Bitmap thumbnailFinal = thumbnail;
				runOnUiThread(() -> {
					if (!isDestroyed()) {
						imgView.setImageBitmap(thumbnailFinal);
						fileNameView.setVisibility(showThumbnailNames ? View.VISIBLE : View.INVISIBLE);
					}
				});
			}
		}
		catch (Exception e) {
			Log.e(Constants.appNameInternal, "Error loading thumbnail", e);
		}
	}

	private void scheduleFileThumbnailLoad(View row) {
		ImageView imgView = row.findViewById(R.id.itemIcon);
		TextView fileNameView = row.findViewById(R.id.itemFileName);
		File file = (File) row.getTag();

		ScheduledFuture<?> thumbnailLoadingFuture = thumbnailLoadingExecutor.schedule(() -> {
			loadAndSetThumbnail(imgView, fileNameView, file.getAbsolutePath());
			thumbnailLoadingTasks.remove(row);
		}, Constants.THUMBNAIL_LOADING_DELAY, TimeUnit.MILLISECONDS);

		thumbnailLoadingTasks.put(row, thumbnailLoadingFuture);
	}

	private void checkFileItemsVisibility() {
		if (fileGridView.getAdapter() == null) {
			return;
		}

		for (int i = 0; i < fileGridView.getAdapter().getCount(); i++) {
			View row = fileGridView.getAdapter().getView(i, null, null);
			if (row != null) {
				File file = (File) row.getTag();
				ImageView imgView = row.findViewById(R.id.itemIcon);
				if ((i >= fileGridView.getFirstVisiblePosition()) && (i <= fileGridView.getLastVisiblePosition())) {
					if ((file != null) && !file.isDirectory() && (imgView.getDrawable() == null)
						&& !thumbnailLoadingTasks.containsKey(row)) {

						// file item becomes visible, while not yet loaded or scheduled
						scheduleFileThumbnailLoad(row);
					}
				}
				else {
					ScheduledFuture<?> thumbnailLoadingFuture = thumbnailLoadingTasks.remove(row);
					if (thumbnailLoadingFuture != null) {
						// file item becomes invisible, so cancel the scheduled task
						thumbnailLoadingFuture.cancel(false);
					}
				}
			}
		}
	}

	private class FilesGridAdapter extends GridAdapter<FileInfo> {
		public FilesGridAdapter(Collection<FileInfo> fileInfoList) {
			super(fileInfoList);
        }

		@Override
        protected View createRow() {
			return inflater.inflate(R.layout.file_browser_item, fileGridView, false);
        }

		@Override
        protected void prepareRow(int position, View row, FileInfo fileInfo) {
			TextView fileNameView = row.findViewById(R.id.itemFileName);
			ImageView imgView = row.findViewById(R.id.itemIcon);
			fileNameView.setText((fileInfo.name != null) ? fileInfo.name : fileInfo.file.getName());
			row.setTag(fileInfo.file);

			try {
				if (fileInfo.iconResId > 0) {
					imgView.setBackgroundResource(fileInfo.iconResId);
				}
				if (fileInfo.file.isFile()) {
					scheduleFileThumbnailLoad(row);
				}
			}
			catch (Exception e) {
				Log.e(Constants.appNameInternal, "Error loading and setting image", e);
			}
        }
	}
}
