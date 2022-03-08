package com.fiill.fiillplayer.fragments;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.fiill.fiillplayer.R;
import com.fiill.fiillplayer.content.PathCursor;
import com.fiill.fiillplayer.content.PathCursorLoader;
import com.fiill.fiillplayer.application.Option;
import com.fiill.fiillplayer.controller.FiillPlayer;
import com.fiill.fiillplayer.controller.VideoInfo;

import java.io.File;
import java.io.IOException;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class FileListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARG_PATH = "path";

    private TextView mPathView;
    private ListView mFileListView;
    private VideoAdapter mAdapter;
    private String mPath;
    private Button mEnter;
    private final FragmentActivity mActivity;

    public FileListFragment(FragmentActivity activity){
        mActivity = activity;
    }

    public FileListFragment(){
        super();
        mActivity = getActivity();
    }

    public static FileListFragment newInstance(FragmentActivity activity, File path) {
        FileListFragment f = new FileListFragment(activity);

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putString(ARG_PATH, String.valueOf(path));
        f.setArguments(args);

        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_file_list, container, false);
        mPathView = viewGroup.findViewById(R.id.path_view);
        mFileListView = viewGroup.findViewById(R.id.file_list_view);

        mPathView.setVisibility(View.VISIBLE);
        mEnter = viewGroup.findViewById(R.id.path_enter);
        mEnter.setVisibility(View.VISIBLE);
        return viewGroup;
    }

    public void doOpenDirectory(File path, boolean addToBackStack) {
        Fragment newFragment = FileListFragment.newInstance(mActivity, path);
        FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.body, newFragment);

        if (addToBackStack)
            transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onClickFile(String path) {
        File f = new File(path);
        try {
            f = f.getAbsoluteFile();
            f = f.getCanonicalFile();
            if (TextUtils.isEmpty(f.toString()))
                f = new File("/");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (f.isDirectory()) {
            doOpenDirectory(f, true);
        } else if (f.exists()) {
            VideoInfo videoInfo = new VideoInfo(f.getPath())
                    .setTitle(f.getName())
                    .setAspectRatio(VideoInfo.AR_ASPECT_FIT_PARENT)
                    .addOption(Option.create(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1L))
                    .addOption(Option.create(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "multiple_requests", 1L))

                    .setShowTopBar(true);
            FiillPlayer.debug = true;//show java logs
            FiillPlayer.play(mActivity, videoInfo);
            mActivity.overridePendingTransition(0, 0);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final Activity activity = getActivity();
        Bundle bundle = getArguments();
        if (bundle != null) {
            mPath = bundle.getString(ARG_PATH);
            mPath = new File(mPath).getAbsolutePath();
            mPathView.setText(mPath);
        }

        mAdapter = new VideoAdapter(activity);
        mFileListView.setAdapter(mAdapter);
        mFileListView.setOnItemClickListener((parent, v, position, id) -> {
            String path = mAdapter.getFilePath(position);
            if (TextUtils.isEmpty(path)) return;
            onClickFile(path);
        });

        LoaderManager.getInstance(this).initLoader(1, null, this);

        mEnter.setOnClickListener(v -> {
            String text = mPathView.getText().toString();
            Uri uri = Uri.parse(text);
            boolean isUri = ((null != uri) && (null != uri.getScheme()));
            File f = new File(text);
            if (f.isDirectory()) {
                System.out.println(text);
            } else if (f.exists() || isUri) {
                VideoInfo videoInfo = new VideoInfo(text)
                        .setTitle(f.getName())
                        .setAspectRatio(VideoInfo.AR_ASPECT_FIT_PARENT)
                        .addOption(Option.create(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1L))
                        .addOption(Option.create(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "multiple_requests", 1L))

                        .setShowTopBar(true);
                FiillPlayer.debug = true;//show java logs
                FiillPlayer.play(getContext(), videoInfo);
                if(null != activity)activity.overridePendingTransition(0, 0);
            }
        });
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new PathCursorLoader(getActivity(), mPath);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }

    static final class VideoAdapter extends SimpleCursorAdapter {
        static final class ViewHolder {
            public ImageView iconImageView;
            public TextView nameTextView;
        }

        public VideoAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2, null,
                    new String[]{PathCursor.CN_FILE_NAME, PathCursor.CN_FILE_PATH},
                    new int[]{android.R.id.text1, android.R.id.text2}, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                view = inflater.inflate(R.layout.fragment_file_list_item, parent, false);
            }

            ViewHolder viewHolder = (ViewHolder) view.getTag();
            if (viewHolder == null) {
                viewHolder = new ViewHolder();
                viewHolder.iconImageView = view.findViewById(R.id.icon);
                viewHolder.nameTextView = view.findViewById(R.id.name);
            }

            if (isDirectory(position)) {
                viewHolder.iconImageView.setImageResource(R.drawable.ic_theme_folder);
            } else if (isVideo(position)) {
                viewHolder.iconImageView.setImageResource(R.drawable.ic_theme_play_arrow);
            } else {
                viewHolder.iconImageView.setImageResource(R.drawable.ic_theme_description);
            }
            viewHolder.nameTextView.setText(getFileName(position));

            return view;
        }

        @Override
        public long getItemId(int position) {
            final Cursor cursor = moveToPosition(position);
            if (cursor == null)
                return 0;

            return cursor.getLong(PathCursor.CI_ID);
        }

        Cursor moveToPosition(int position) {
            final Cursor cursor = getCursor();
            if (cursor.getCount() == 0 || position >= cursor.getCount()) {
                return null;
            }
            cursor.moveToPosition(position);
            return cursor;
        }

        public boolean isDirectory(int position) {
            final Cursor cursor = moveToPosition(position);
            if (cursor == null)
                return true;

            return cursor.getInt(PathCursor.CI_IS_DIRECTORY) != 0;
        }

        public boolean isVideo(int position) {
            final Cursor cursor = moveToPosition(position);
            if (cursor == null)
                return true;

            return cursor.getInt(PathCursor.CI_IS_VIDEO) != 0;
        }

        public String getFileName(int position) {
            final Cursor cursor = moveToPosition(position);
            if (cursor == null)
                return "";

            return cursor.getString(PathCursor.CI_FILE_NAME);
        }

        public String getFilePath(int position) {
            final Cursor cursor = moveToPosition(position);
            if (cursor == null)
                return "";

            return cursor.getString(PathCursor.CI_FILE_PATH);
        }
    }
}
