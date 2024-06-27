package com.sergenious.mediabrowser.filebrowser;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.GridView;

import com.sergenious.mediabrowser.R;
import com.sergenious.mediabrowser.ui.adapter.GridAdapter;
import com.sergenious.mediabrowser.utils.FileUtils.FileInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class FileMultiChoiceListener implements AbsListView.MultiChoiceModeListener {
    private final GridView fileGridView;
    private final Consumer<Boolean> onActionModeChange;
    private final BiFunction<Integer, List<FileInfo>, Boolean> onActionVisibilityProvider;
    private final BiConsumer<Integer, List<FileInfo>> onActionListener;

    public FileMultiChoiceListener(GridView fileGridView,
        Consumer<Boolean> onActionModeChange, BiFunction<Integer,
        List<FileInfo>, Boolean> onActionVisibilityProvider,
        BiConsumer<Integer, List<FileInfo>> onActionListener) {

        this.fileGridView = fileGridView;
        this.onActionModeChange = onActionModeChange;
        this.onActionVisibilityProvider = onActionVisibilityProvider;
        this.onActionListener = onActionListener;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        List<FileInfo> selectedFiles = getSelectedFileList();
        mode.getMenuInflater().inflate(R.menu.file_browser_multiselection_menu, menu);
        for (int itemIndex = 0; itemIndex < menu.size(); itemIndex++) {
            MenuItem item = menu.getItem(itemIndex);
            item.setVisible(onActionVisibilityProvider.apply(item.getItemId(), selectedFiles));
        }
        onActionModeChange.accept(true);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (onActionListener != null) {
            onActionListener.accept(item.getItemId(), getSelectedFileList());
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        onActionModeChange.accept(false);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
        // process uncheckable items
        GridAdapter<?> adapter = (GridAdapter<?>) fileGridView.getAdapter();
        if (checked && fileGridView.isItemChecked(position) && !adapter.isItemCheckable(position)) {
            fileGridView.setItemChecked(position, false);
        }

        List<FileInfo> selectedFiles = getSelectedFileList();
        Menu menu = actionMode.getMenu();
        for (int itemIndex = 0; itemIndex < menu.size(); itemIndex++) {
            MenuItem item = menu.getItem(itemIndex);
            item.setVisible(onActionVisibilityProvider.apply(item.getItemId(), selectedFiles));
        }

        final int checkedCount = fileGridView.getCheckedItemCount();
        actionMode.setTitle(checkedCount + " / " + fileGridView.getAdapter().getCount());
    }

    private List<FileInfo> getSelectedFileList() {
        List<FileInfo> fileList = new ArrayList<>();
        for (int i = 0; i < fileGridView.getAdapter().getCount(); i++) {
            if (fileGridView.isItemChecked(i)) {
                FileInfo fileInfo = (FileInfo) fileGridView.getAdapter().getItem(i);
                if (fileInfo != null) {
                    fileList.add(fileInfo);
                }
            }
        }
        return fileList;
    }
}
