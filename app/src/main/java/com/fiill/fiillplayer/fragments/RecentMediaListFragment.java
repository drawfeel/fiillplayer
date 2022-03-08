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

package com.fiill.fiillplayer.fragments;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.fiill.fiillplayer.R;
import com.fiill.fiillplayer.content.RecentMediaStorage;
import com.fiill.fiillplayer.application.Option;
import com.fiill.fiillplayer.controller.FiillPlayer;
import com.fiill.fiillplayer.controller.VideoInfo;


import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RecentMediaListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    static final int DIALOG_MENU_ITEM_REMOVE_SINGLE = 0;
    static final int DIALOG_MENU_ITEM_REMOVE_ALL = 1;

    private ListView mFileListView;
    private RecentMediaAdapter mAdapter;
    RecentMediaStorage.CursorLoader mLoader;
    AlertDialog mAlertDialog;

    public static RecentMediaListFragment newInstance() {
        return new RecentMediaListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_file_list, container, false);
        mFileListView = viewGroup.findViewById(R.id.file_list_view);
        return viewGroup;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final Activity activity = getActivity();

        mAdapter = new RecentMediaAdapter(activity);
        mFileListView.setAdapter(mAdapter);
        mFileListView.setOnItemClickListener((parent, view1, position, id) -> {
            String url = mAdapter.getUrl(position);
            String name = mAdapter.getName(position);
            assert activity != null;
            VideoInfo videoInfo = new VideoInfo(url)
                    .setTitle(name)
                    .setAspectRatio(VideoInfo.AR_ASPECT_FIT_PARENT)
                    .addOption(Option.create(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1L))
                    .addOption(Option.create(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "multiple_requests", 1L))

                    .setShowTopBar(true);
            FiillPlayer.debug = true;//show java logs
            FiillPlayer.play(getContext(), videoInfo);
            requireActivity().overridePendingTransition(0, 0);
        });

        mFileListView.setOnItemLongClickListener((adapterView, view12, position, id) -> {
            long itemId = mAdapter.getItemId(position);
            showLongClickMenu(itemId, getContext());
            return true; // avoid OnItemClick event.
        });

        LoaderManager.getInstance(this).initLoader(2, null, this);
    }

    public void showLongClickMenu(long id, Context context){
        final String[] items = new String[2];
        items[DIALOG_MENU_ITEM_REMOVE_SINGLE] = "Delete Item";
        items[DIALOG_MENU_ITEM_REMOVE_ALL] =  "Delete All";

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        //alertBuilder.setTitle("You want to:");
        alertBuilder.setItems(items, (dialogInterface, i) -> {
            int ret = -1;
            long[] ids = new long[1];
            if(i== DIALOG_MENU_ITEM_REMOVE_SINGLE) {
                ids[0] = id;
            } else if(i == DIALOG_MENU_ITEM_REMOVE_ALL) {
                ids = null;
            }
            try {
                ret = RecentMediaStorage.removeAsync(getContext(), ids);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(ret == 0){
                mLoader.startLoading();
            }
            mAlertDialog.dismiss();
        });
        mAlertDialog = alertBuilder.create();
        mAlertDialog.show();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        mLoader = new RecentMediaStorage.CursorLoader(getActivity());
        return mLoader;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    static final class RecentMediaAdapter extends SimpleCursorAdapter {
        private int mIndex_id = -1;
        private int mIndex_url = -1;
        private int mIndex_name = -1;

        public RecentMediaAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2, null,
                    new String[]{RecentMediaStorage.Entry.COLUMN_NAME_NAME, RecentMediaStorage.Entry.COLUMN_NAME_URL},
                    new int[]{android.R.id.text1, android.R.id.text2}, 0);
        }

        @Override
        public Cursor swapCursor(Cursor c) {
            Cursor res = super.swapCursor(c);

            mIndex_id = c.getColumnIndex(RecentMediaStorage.Entry.COLUMN_NAME_ID);
            mIndex_url = c.getColumnIndex(RecentMediaStorage.Entry.COLUMN_NAME_URL);
            mIndex_name = c.getColumnIndex(RecentMediaStorage.Entry.COLUMN_NAME_NAME);

            return res;
        }

        @Override
        public long getItemId(int position) {
            final Cursor cursor = moveToPosition(position);
            if (cursor == null)
                return 0;

            return cursor.getLong(mIndex_id);
        }

        Cursor moveToPosition(int position) {
            final Cursor cursor = getCursor();
            if (cursor.getCount() == 0 || position >= cursor.getCount()) {
                return null;
            }
            cursor.moveToPosition(position);
            return cursor;
        }

        public String getUrl(int position) {
            final Cursor cursor = moveToPosition(position);
            if (cursor == null)
                return "";

            return cursor.getString(mIndex_url);
        }

        public String getName(int position) {
            final Cursor cursor = moveToPosition(position);
            if (cursor == null)
                return "";

            return cursor.getString(mIndex_name);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLoader.destroy();
    }
}
