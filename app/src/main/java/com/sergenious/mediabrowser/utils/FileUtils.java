package com.sergenious.mediabrowser.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.sergenious.mediabrowser.Constants;
import com.sergenious.mediabrowser.R;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FileUtils {
    private static final DecimalFormat fileSizeFormatter = new DecimalFormat("#,###.##");

    public enum FileSortMode {
        PATH_DIRS_FILES(R.string.sort_path_dirs_files, PATH_DIRS_FILES_COMPARATOR),
        PATH(R.string.sort_path, Comparator.comparing(f -> f.file.getAbsolutePath(), String.CASE_INSENSITIVE_ORDER)),
        DATE_ASC(R.string.sort_date_asc, Comparator.comparing(f -> f.fileTime)),
        DATE_DESC(R.string.sort_date_desc, Comparator.<FileInfo, Long>comparing(f -> f.fileTime).reversed());

        private final int labelResId;
        private final Comparator<FileInfo> comparator;

        FileSortMode(int labelResId, Comparator<FileInfo> comparator) {
            this.labelResId = labelResId;
            this.comparator = comparator;
        }

        public int getLabelResId() {
            return labelResId;
        }

        public Comparator<FileInfo> getComparator() {
            return comparator;
        }
    }

    public static class FileInfo {
        public final File file;
        public final boolean isRootDir;
        public final String name;
        public final int iconResId;
        public final long fileTime;

        public FileInfo(File file, boolean isRootDir, String name, int iconResId, long fileTime) {
            this.file = file;
            this.isRootDir = isRootDir;
            this.name = name;
            this.iconResId = iconResId;
            this.fileTime = fileTime;
        }
    }

    public static List<File> toFileList(List<FileInfo> fileInfoList) {
        return fileInfoList.stream().map(f -> f.file).collect(Collectors.toList());
    }

    public static String getFileExtension(File file) {
        int dotPos = file.getName().lastIndexOf('.');
        return (dotPos >= 0) ? file.getName().substring(dotPos + 1).toLowerCase() : "";
    }

    public static String fileSizeToString(long size, boolean withRawSize) {
        if (size < 1024) {
            return fileSizeFormatter.format(size);
        }
        if (size < 1024 * 1024) {
            return fileSizeFormatter.format(size / 1024.0f) + " kB"
                + (withRawSize ? " (" + fileSizeFormatter.format(size) + ")" : "");
        }
        if (size < 1024 * 1024 * 1024) {
            return fileSizeFormatter.format(size / (1024.0f * 1024)) + " MB"
                + (withRawSize ? " (" + fileSizeFormatter.format(size) + ")" : "");
        }
        return fileSizeFormatter.format(size / (1024.0f * 1024 * 1024)) + " GB"
            + (withRawSize ? " (" + fileSizeFormatter.format(size) + ")" : "");
    }

    public static long getFileCreatedTime(File file) {
        try {
            BasicFileAttributes fileAttr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            return fileAttr.creationTime().toMillis();
        }
        catch (IOException e) {
            return 0;
        }
    }

    public static long getFileLastModifiedTime(File file) {
        try {
            BasicFileAttributes fileAttr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            return fileAttr.lastModifiedTime().toMillis();
        }
        catch (IOException e) {
            return 0;
        }
    }

    public static File getFileFromUri(Context context, Uri uri) {
        if (uri.getScheme().equals("file")) {
            return new File(uri.getPath());
        }

        if (uri.getScheme().equals("content")) {
            File file = getFileFromContentUri(context, uri);
            if (file != null) {
                return file;
            }

            return resolveFileByRootDirs(context, uri);
        }

        return null;
    }

    public static File getFileFromContentUri(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    if (columnIndex >= 0) {
                        return new File(cursor.getString(columnIndex));
                    }
                }
            }
        }
        catch (Exception e) {
            Log.e(Constants.appNameInternal, "Error converting URI to file", e);
        }
        return null;
    }

    public static Map<File, FileInfo> getRootDirectories(Context context) {
        Map<File, FileInfo> rootDirectories = new LinkedHashMap<>();

        File[] extFilesDir = context.getExternalFilesDirs(null);
        if (extFilesDir != null) {
            for (File file : extFilesDir) {
                if (file.getParent() != null) {
                    file = new File(file.getParent().replace("/Android/data/", "")
                        .replace(context.getPackageName(), ""));
                    String name = context.getString(R.string.sdcard) + " (" + file.getName() + ")";
                    rootDirectories.put(file, new FileInfo(file, true, name, R.drawable.ic_sdcard, 0));
                }
            }
        }

        File internalStorageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        rootDirectories.put(internalStorageDir, new FileInfo(internalStorageDir, true,
            context.getResources().getString(R.string.internal_storage), R.drawable.ic_phone, 0));

        return rootDirectories;
    }

    public static List<FileInfo> getFileList(File currDir, List<String> extensions, boolean includeDirs, FileSortMode sortMode) {
        List<FileInfo> fileInfoList = new ArrayList<>();
        String fileNameFilterRegEx = getFileFilterRegEx(extensions);

        File[] files = (currDir != null) ? currDir.listFiles() : null;
        if (files != null) {
            for (File file: files) {
                boolean isDirectory = file.isDirectory();
                if (!file.getName().startsWith(".")
                    && (isDirectory || file.getName().toLowerCase().matches(fileNameFilterRegEx))) {

                    if (includeDirs || !isDirectory) {
                        FileInfo fileInfo = new FileInfo(file, false, null,
                            isDirectory ? R.drawable.ic_folder : 0, getFileLastModifiedTime(file));
                        fileInfoList.add(fileInfo);
                    }
                }
            }
        }

        fileInfoList.sort(sortMode.getComparator());

        return fileInfoList;
    }

    public static List<FileInfo> searchFiles(String query, File rootDir, List<String> extensions, FileSortMode sortMode,
        BiConsumer<String, Double> progressUpdater, Supplier<Boolean> canceled) {

        return searchFiles(query, rootDir, extensions, sortMode, 0.0, 1.0, progressUpdater, canceled);
    }

    public static List<FileInfo> searchFiles(String query, File rootDir, List<String> extensions, FileSortMode sortMode,
        double progress, double progressFactor, BiConsumer<String, Double> progressUpdater, Supplier<Boolean> canceled) {

        List<FileInfo> fileInfoList = new ArrayList<>();
        searchFilesInternal((query != null) ? query.toLowerCase() : null, rootDir, rootDir, fileInfoList,
            getFileFilterRegEx(extensions), progressUpdater, canceled, progress, progressFactor);

        fileInfoList.sort(sortMode.getComparator());

        return fileInfoList;
    }

    private static void searchFilesInternal(String query, File currDir, File rootDir,
        List<FileInfo> fileInfoList, String fileNameFilterRegEx,
        BiConsumer<String, Double> progressUpdater, Supplier<Boolean> canceled,
        double progress, double progressFactor) {

        if (canceled.get()) {
            return;
        }
        progressUpdater.accept(currDir.getAbsolutePath().substring(rootDir.getAbsolutePath().length()), progress);

        File[] files = currDir.listFiles();
        if (files != null) {
            progressFactor /= files.length;

            for (int fileIndex = 0; fileIndex < files.length; fileIndex++) {
                File file = files[fileIndex];
                boolean isDirectory = file.isDirectory();

                if (!file.getName().startsWith(".")
                    && (isDirectory || file.getName().toLowerCase().matches(fileNameFilterRegEx))) {

                    int lastDotPos = isDirectory ? -1 : file.getName().lastIndexOf('.');
                    String name = (lastDotPos >= 0) ? file.getName().substring(0, lastDotPos) : file.getName();
                    if ((query == null) || name.toLowerCase().contains(query)) {
                        FileInfo fileInfo = new FileInfo(file, false, null,
                            isDirectory ? R.drawable.ic_folder : 0, getFileLastModifiedTime(file));
                        fileInfoList.add(fileInfo);
                    }
                }
                if (isDirectory) {
                    searchFilesInternal(query, file, rootDir, fileInfoList,
                        fileNameFilterRegEx, progressUpdater, canceled,
                        progress + progressFactor * fileIndex, progressFactor);
                }
            }
        }
    }

    private static String getFileFilterRegEx(List<String> extensions) {
        return extensions.stream().map(e -> "..*?\\." + e).collect(Collectors.joining("|"));
    }

    // This is a little hack, by removing the path parts from the left,
    // and checking if the file exists in a specified root dir
    private static File resolveFileByRootDirs(Context context, Uri uri) {
        Collection<File> rootDirs = getRootDirectories(context).keySet();

        String path = uri.getPath();
        while (path.length() > 0) {
            int slashPos = path.indexOf('/');
            if (slashPos >= 0) {
                path = path.substring(slashPos + 1);
            }

            for (File rootDir: rootDirs) {
                File file = new File(rootDir, path);
                if (file.exists()) {
                    return file;
                }
            }

            if (slashPos < 0) {
                break;
            }
        }
        return null;
    }

    // comparator that displays directories first
    private static final Comparator<FileInfo> PATH_DIRS_FILES_COMPARATOR = (info0, info1) -> {
        if (info0.file.isDirectory() && !info1.file.isDirectory()) {
            return -1;
        }
        if (!info0.file.isDirectory() && info1.file.isDirectory()) {
            return +1;
        }
        return info0.file.getName().compareToIgnoreCase(info1.file.getName());
    };
}
