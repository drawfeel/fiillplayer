package com.fiill.fiillplayer.trackselector;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import com.fiill.fiillplayer.R;
import com.fiill.fiillplayer.widgets.ViewQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fiill.fiillplayer.controller.FiillPlayer;
import com.fiill.fiillplayer.controller.PlayerManager;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

/**
 * The adapter for track selector Expandable listview.
 */

public class TracksAdapter extends BaseExpandableListAdapter {
    private final Map<Integer, TrackGroup> mDataIndex = new HashMap<>();
    private final List<TrackGroup> mData = new ArrayList<>();

    @Override

    public int getGroupCount() {
        return mData.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mData.get(groupPosition).getTracks().size();
    }

    @Override
    public TrackGroup getGroup(int groupPosition) {
        return mData.get(groupPosition);
    }

    @Override
    public TrackInfoWrapper getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition).getTracks().get(childPosition);
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
        TrackGroup group = getGroup(groupPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_fragment_selector_group, parent, false);
        }
        ViewQuery vq = new ViewQuery(convertView);
        vq.id(R.id.selector_group_name).text(group.getTrackTypeName());
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        TrackGroup group = getGroup(groupPosition);
        TrackInfoWrapper child = getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_fragment_selector_child, parent, false);
            convertView.findViewById(R.id.selector_group_child).setOnClickListener(v -> {
                TrackInfoWrapper track = (TrackInfoWrapper) v.getTag();
                TrackGroup trackGroup = mDataIndex.get(track.getTrackType());
                assert trackGroup != null;
                if (trackGroup.getSelectedTrackIndex() != track.getIndex()) {
                    trackGroup.setSelectedTrackIndex(track.getIndex());
                    notifyDataSetChanged();

                    FiillPlayer player = PlayerManager.getInstance().getPlayerByFingerprint(track.getFingerprint());
                    if (player != null) {
                        if (track.getIndex() >= 0) {
                            player.selectTrack(track.getIndex());
                        } else {
                            player.deselectTrack(player.getSelectedTrack(track.getTrackType()));
                        }
                    }
                }
            });
        }
        ViewQuery vq = new ViewQuery(convertView);
        vq.id(R.id.selector_group_child).text(child.getInfo()).checked(group.getSelectedTrackIndex() == child.getIndex()).view().setTag(child);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void load(String fingerprint) {
        if (TextUtils.isEmpty(fingerprint)) {
            return;
        }
        FiillPlayer player = PlayerManager.getInstance().getPlayerByFingerprint(fingerprint);
        if (player == null) {
            return;
        }
        mDataIndex.clear();
        mData.clear();

        ITrackInfo[] tracks = player.getTrackInfo();
        for (int i = 0; i < tracks.length; i++) {
            ITrackInfo track = tracks[i];
            int trackType = track.getTrackType();
            if (trackType == ITrackInfo.MEDIA_TRACK_TYPE_AUDIO ||
                    trackType == ITrackInfo.MEDIA_TRACK_TYPE_VIDEO ||
                    trackType == ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE ||
                    trackType == ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
                TrackGroup trackGroup = mDataIndex.get(trackType);
                if (trackGroup == null) {
                    int selectedTrack = player.getSelectedTrack(trackType);
                    trackGroup = new TrackGroup(trackType, selectedTrack);
                    //if(trackType == ITrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                    //trackGroup.getTracks().add(TrackInfoWrapper.OFF(fingerprint,trackType));}
                    mDataIndex.put(trackType, trackGroup);
                    mData.add(trackGroup);
                }
                TrackInfoWrapper e = new TrackInfoWrapper(fingerprint, track, i, trackType);
                trackGroup.getTracks().add(e);
            }
        }
        notifyDataSetChanged();
    }
}
