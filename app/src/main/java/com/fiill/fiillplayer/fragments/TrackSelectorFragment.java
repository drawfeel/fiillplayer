package com.fiill.fiillplayer.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.fiill.fiillplayer.R;
import com.fiill.fiillplayer.trackselector.TracksAdapter;
import com.fiill.fiillplayer.utils.Utils;
import com.fiill.fiillplayer.widgets.ViewQuery;

/**
 * This Fragment will show when user press settings key in video view
 * to choose audio/video/text track
 */

public class TrackSelectorFragment extends DialogFragment {
    private ViewQuery mViewQuery;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        View view = inflater.inflate(R.layout.dialog_fragment_selector, container, false);
        mViewQuery = new ViewQuery(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Dialog d = getDialog();
        if(d != null) {
            Utils.hideSystemUI(d.getWindow());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ExpandableListView list = mViewQuery.id(R.id.fragment_selector_list).view();

        mViewQuery.id(R.id.app_video_track_close).clicked(v -> dismissAllowingStateLoss());

        final TracksAdapter tracksAdapter = new TracksAdapter();
        list.setGroupIndicator(null);
        list.setOnGroupClickListener((parent, v, groupPosition, id) -> true);
        list.setAdapter(tracksAdapter);
        assert getArguments() != null;
        tracksAdapter.load(getArguments().getString("fingerprint"));
        int count = tracksAdapter.getGroupCount();
        for ( int i = 0; i < count; i++ ) {
            list.expandGroup(i);
        }

        Dialog d = getDialog();
        if(d != null) {
            View decorView = d.getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(
                    visibility -> {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // TODO: The system bars are visible. Make any desired
                            Utils.hideSystemUI(d.getWindow());
                        }
                    });
        }
    }
}
