package com.sergenious.mediabrowser.ui.adapter;

import java.util.*;

import android.view.*;
import android.widget.BaseAdapter;

public abstract class GridAdapter<T> extends BaseAdapter {
    private final List<RowData> gridRowList;

    private class RowData {
        public View rowView;
        public T rowObject;
    }
    
    public GridAdapter(Collection<T> gridRowObjectList) {
        gridRowList = (gridRowObjectList != null) ? new ArrayList<>(gridRowObjectList.size()) : null;
        if (gridRowObjectList != null) {
            for (T rowObject: gridRowObjectList) {
                RowData rowData = new RowData();
                rowData.rowObject = rowObject;
                rowData.rowView = null;
                gridRowList.add(rowData);
            }
        }
    }

    public void invalidate() {
        for (RowData rowData: gridRowList) {
            rowData.rowView = null;
        }
    }

    @Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
        View row = gridRowList.get(position).rowView;
        if (row == null) {
            row = createRow();
            if (row != null) {
            	final RowData rowdata = gridRowList.get(position);
            	rowdata.rowView = row;
                prepareRow(position, row, getItem(position));
            }
        }

        return row;
    }
    
    @Override
	public int getCount() {
        if (gridRowList == null) {
        	return 0;
        }
        return gridRowList.size();
    }

    @Override
	public T getItem(int position) {
        if ((gridRowList == null) || (position < 0) || (position >= gridRowList.size())) {
        	return null;
        }
        return gridRowList.get(position).rowObject;
    }

    @Override
	public long getItemId(int position) {
        return position;
    }
    
    @Override
	public final int getViewTypeCount() {
        return 1;
    }
    
    @Override
	public final boolean hasStableIds() {
        return true;
    }

    // overridable
    public boolean isItemCheckable(int position) {
        return true;
    }

    protected abstract View createRow();
    protected abstract void prepareRow(int position, View row, T rowObject);
}
