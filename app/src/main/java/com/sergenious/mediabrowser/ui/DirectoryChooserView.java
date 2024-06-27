package com.sergenious.mediabrowser.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.sergenious.mediabrowser.R;
import com.sergenious.mediabrowser.ui.adapter.GridAdapter;
import com.sergenious.mediabrowser.utils.FileUtils;
import com.sergenious.mediabrowser.utils.FileUtils.FileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressLint("ViewConstructor")
public class DirectoryChooserView extends FrameLayout {
    private final LayoutInflater inflater;
    private final TextView currDirNameView;
    private final GridView gridView;
    private final Map<File, FileUtils.FileInfo> rootDirectories;
    private File currentRootDir;
    private File currentDirectory;

    public static AlertDialog createDialog(Context context, String title, File startDirectory,
        Consumer<File> selectedDirectoryListener) {

        final DirectoryChooserView chooser = new DirectoryChooserView(context, startDirectory);

        return new AlertDialog.Builder(context, R.style.MediaBrowserTheme_AlertDialog)
            .setTitle(title)
            .setView(chooser)
            .setPositiveButton(android.R.string.ok, (d, which) -> {
                File dir = chooser.getSelectedDirectory();
                if (dir != null) {
                    selectedDirectoryListener.accept(chooser.getSelectedDirectory());
                }
            })
            .setNegativeButton(android.R.string.cancel, (d, which) -> {})
            .show();
    }

    public DirectoryChooserView(Context context, File startDirectory) {
        super(context);

        inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.directory_chooser, this, true);
        currDirNameView = findViewById(R.id.directoryChooserCurrent);
        gridView = findViewById(R.id.directoryChooserList);

        gridView.setOnItemClickListener((adapter, v, position, id) -> {
            FileInfo fileInfo = ((DirectoriesGridAdapter) gridView.getAdapter()).getItem(position);
            if (fileInfo.file == null) { // up clicked
                if (currentDirectory.equals(currentRootDir)) {
                    currentDirectory = currentRootDir = null;
                }
                else {
                    currentDirectory = currentDirectory.getParentFile();
                }
            }
            else {
                if (currentDirectory == null) {
                    currentRootDir = fileInfo.file;
                }
                currentDirectory = fileInfo.file;
            }
            initDirectoryList();
        });

        rootDirectories = FileUtils.getRootDirectories(context);
        initDirectoryList();
    }

    public File getSelectedDirectory() {
        return (currentRootDir != null) ? currentDirectory : null;
    }

    private void initDirectoryList() {
        if (currentDirectory == null) {
            currDirNameView.setText(R.string.root_folders);
            gridView.setAdapter(new DirectoriesGridAdapter(rootDirectories.values()));
        }
        else {
            currDirNameView.setText(currentDirectory.getAbsolutePath());

            List<FileInfo> directoryList = new ArrayList<>();
            directoryList.add(new FileInfo(null, false, getContext().getString(R.string.directory_up),
                R.drawable.ic_up, 0));

            File[] files = (currentDirectory != null) ? currentDirectory.listFiles() : null;
            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File file: files) {
                    if (file.isDirectory()) {
                        directoryList.add(new FileInfo(file, false, file.getName(), R.drawable.ic_folder, 0));
                    }
                }
            }

            gridView.setAdapter(new DirectoriesGridAdapter(directoryList));
        }
    }

    private class DirectoriesGridAdapter extends GridAdapter<FileInfo> {
        public DirectoriesGridAdapter(Collection<FileInfo> directoryList) {
            super(directoryList);
        }

        @Override
        protected View createRow() {
            return inflater.inflate(R.layout.directory_chooser_item, gridView, false);
        }

        @Override
        protected void prepareRow(int position, View row, FileInfo fileInfo) {
            ((TextView) row.findViewById(R.id.directoryName)).setText(fileInfo.name);
            ((ImageView) row.findViewById(R.id.directoryIcon)).setImageResource(fileInfo.iconResId);
        }
    }
}
