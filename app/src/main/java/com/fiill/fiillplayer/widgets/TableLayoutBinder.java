/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fiill.fiillplayer.widgets;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.fiill.fiillplayer.R;

public class TableLayoutBinder {
    private final Context mContext;
    public ViewGroup mTableView;
    public TableLayout mTableLayout;

    public TableLayoutBinder(Context context) {
        this(context, R.layout.table_media_info);
    }

    public TableLayoutBinder(Context context, int layoutResourceId) {
        mContext = context;
        mTableView = (ViewGroup) LayoutInflater.from(mContext).inflate(layoutResourceId, null);
        mTableLayout = mTableView.findViewById(R.id.table);
    }

    public TableLayoutBinder(Context context, TableLayout tableLayout) {
        mContext = context;
        mTableView = tableLayout;
        mTableLayout = tableLayout;
    }


    public View appendRow(String name, String value) {
        return appendSection(R.layout.table_media_info_row, name, value);
    }

    public View appendRow(int nameId, String value) {
        return appendRow(mContext.getString(nameId), value);
    }

    public void appendSection(String name) {
        appendSection(R.layout.table_media_info_section, name, null);
    }

    public void appendSection(int nameId) {
        appendSection(mContext.getString(nameId));
    }

    public View appendSection(int layoutId, String name, String value) {
        ViewGroup rowView = (ViewGroup) LayoutInflater.from(mContext).inflate(layoutId, mTableLayout, false);
        setNameValueText(rowView, name, value);

        mTableLayout.addView(rowView);
        return rowView;
    }

    public ViewHolder obtainViewHolder(View rowView) {
        ViewHolder viewHolder = (ViewHolder) rowView.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder();
            viewHolder.mNameTextView = rowView.findViewById(R.id.name);
            viewHolder.mValueTextView = rowView.findViewById(R.id.value);
            rowView.setTag(viewHolder);
        }
        return viewHolder;
    }

    public void setNameValueText(View rowView, String name, String value) {
        ViewHolder viewHolder = obtainViewHolder(rowView);
        viewHolder.setName(name);
        viewHolder.setValue(value);
    }

    public void setValueText(View rowView, String value) {
        ViewHolder viewHolder = obtainViewHolder(rowView);
        viewHolder.setValue(value);
    }

    public ViewGroup buildLayout() {
        return mTableView;
    }

    public AlertDialog.Builder buildAlertDialogBuilder() {
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(new ContextThemeWrapper(mContext,
                R.style.AlertDialogMediaInfo));
        dlgBuilder.setView(buildLayout());
        return dlgBuilder;
    }

    private static class ViewHolder {
        public TextView mNameTextView;
        public TextView mValueTextView;

        public void setName(String name) {
            if (mNameTextView != null) {
                mNameTextView.setText(name);
            }
        }

        public void setValue(String value) {
            if (mValueTextView != null) {
                mValueTextView.setText(value);
            }
        }
    }
}
