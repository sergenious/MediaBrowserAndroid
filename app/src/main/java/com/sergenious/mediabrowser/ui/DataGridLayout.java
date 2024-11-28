package com.sergenious.mediabrowser.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.sergenious.mediabrowser.utils.UiUtils;

import java.util.List;

@SuppressLint("ViewConstructor")
public class DataGridLayout extends ScrollView {
    public DataGridLayout(Context context, List<List<Object>> rows,
        List<Integer> columnWidths, List<Integer> columnStyles) {

        super(context);

        TableLayout tableLayout = new TableLayout(getContext());
        tableLayout.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        addView(tableLayout);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (List<Object> rowData: rows) {
            TableRow rowView = new TableRow(getContext());
            rowView.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
            tableLayout.addView(rowView);

            for (int columnIndex = 0; columnIndex < rowData.size(); columnIndex++) {
                TextView cellView = new TextView(getContext());
                cellView.setTextIsSelectable(true);
                int padding = UiUtils.dpToPx(getContext(), 8);
                cellView.setPadding(padding, padding / 2, padding, padding / 2);

                Integer columnWidth = (columnIndex < columnWidths.size()) ? columnWidths.get(columnIndex) : null;
                cellView.setLayoutParams(new TableRow.LayoutParams(
                    (columnWidth != null) ? UiUtils.dpToPx(getContext(), columnWidth) : 0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    (columnWidth != null) ? 0 : 1
                ));

                Integer style = (columnIndex < columnStyles.size()) ? columnStyles.get(columnIndex) : null;
                if (style != null) {
                    cellView.setTextAppearance(style);
                }

                setText(cellView, rowData.get(columnIndex));
                rowView.addView(cellView);
            }
        }
    }

    public static void setText(TextView textView, Object value) {
        if (value instanceof Spanned) {
            textView.setClickable(true);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText((Spanned) value);
        }
        else {
            String content = value.toString();
            content = content.replace("\r\n", System.getProperty("line.separator"));
            content = content.replace("\n", System.getProperty("line.separator"));
            content = content.replace("\r", System.getProperty("line.separator"));
            textView.setText(content);
        }
    }
}
