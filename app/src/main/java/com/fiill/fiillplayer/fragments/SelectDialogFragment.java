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
import com.fiill.fiillplayer.utils.Utils;
import com.fiill.fiillplayer.widgets.ViewQuery;
import com.fiill.fiillplayer.widgets.SelectDialogAdapter;

import java.util.Objects;

  /*
  This is the base class of dialog select radio menu
  */

public abstract class SelectDialogFragment extends DialogFragment {
    protected ViewQuery mViewQuery;
    String mDialogName;
    private final View.OnClickListener mItemOnClickListener;

    abstract View.OnClickListener setItemOnClickListener();
    abstract boolean isDefaultItem(String item);

    public SelectDialogFragment(String dialogName) {
        this.mDialogName = dialogName;
        mItemOnClickListener = setItemOnClickListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        Dialog d = getDialog();
        if(d != null) {
            Utils.hideSystemUI(d.getWindow());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Objects.requireNonNull(getDialog()).requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_fragment_selector, container, false);
        mViewQuery = new ViewQuery(view);
        //hide virtual bottom keys
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ExpandableListView list = mViewQuery.id(R.id.fragment_selector_list).view();

        mViewQuery.id(R.id.app_video_track_close).clicked(v -> dismissAllowingStateLoss());

        final SelectDialogAdapter adapter = new SelectDialogAdapter(mDialogName, mItemOnClickListener) {
            @Override
            public boolean isDefaultItem(String item) {
                return SelectDialogFragment.this.isDefaultItem(item);
            }
        };
        list.setGroupIndicator(null);
        list.setOnGroupClickListener((parent, v, groupPosition, id) -> true);
        list.setAdapter(adapter);
        assert getArguments() != null;
        adapter.load(getArguments().getStringArray("DATA"));
        int count = adapter.getGroupCount();
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
