package com.fiill.fiillplayer.widgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import com.fiill.fiillplayer.R;

import java.util.ArrayList;
import java.util.Arrays;

public abstract  class SelectDialogAdapter extends BaseExpandableListAdapter {
    String mDialogName;
    private final View.OnClickListener mItemOnClickListener;

    public abstract boolean isDefaultItem(String item);

    public SelectDialogAdapter(String dialogName, View.OnClickListener listener) {
        this.mDialogName = dialogName;
        this.mItemOnClickListener = listener;
    }

    private final ArrayList<String> data = new ArrayList<>();
    @Override
    public int getGroupCount() {return 1;}

    @Override
    public int getChildrenCount(int groupPosition) {return data.size();}

    @Override
    public ArrayList<String> getGroup(int groupPosition) { return data;}

    @Override
    public String getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return getChild(groupPosition, childPosition).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_fragment_selector_group, parent, false);
        }
        ViewQuery vq = new ViewQuery(convertView);
        vq.id(R.id.selector_group_name).text(mDialogName);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String child = getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_fragment_selector_child, parent, false);
            ViewQuery vq = new ViewQuery(convertView);
            boolean isSelected = isDefaultItem(child);
            vq.id(R.id.selector_group_child).text(child).checked(isSelected).view().setTag(child);
            convertView.findViewById(R.id.selector_group_child).setOnClickListener(mItemOnClickListener);
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void load(String[] items) {
        data.clear();
        data.addAll(Arrays.asList(items));
        notifyDataSetChanged();
    }
}