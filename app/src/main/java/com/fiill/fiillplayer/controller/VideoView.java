package com.fiill.fiillplayer.controller;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;

import com.fiill.fiillplayer.R;
import com.fiill.fiillplayer.utils.Utils;
import com.fiill.fiillplayer.widgets.TableLayoutBinder;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * The is the base video view for live stream
 */

public class VideoView extends FrameLayout {

    private IPlayerListener mMediaController;
    private IPlayerListener mPlayerListener;
    private ViewGroup mViewGroupContainer;

    private VideoInfo mVideoInfo = VideoInfo.createFromDefault();

    public IPlayerListener getPlayerListener() {
        return mPlayerListener;
    }

    public void setPlayerListener(IPlayerListener playerListener) { this.mPlayerListener = playerListener; }

    public VideoInfo getVideoInfo() {
        return mVideoInfo;
    }

    public void videoInfo(VideoInfo videoInfo) {
        if (this.mVideoInfo.getUri() != null && !this.mVideoInfo.getUri().equals(videoInfo.getUri())) {
            PlayerManager.getInstance().releaseByFingerprint(this.mVideoInfo.getFingerprint());
        }
        this.mVideoInfo = videoInfo;
    }

    public VideoView(Context context) {
        super(context);
        init(context);
    }

    public VideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mViewGroupContainer = new FrameLayout(context);
        addView(mViewGroupContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        initMediaController();
        setBackgroundColor(mVideoInfo.getBgColor());
    }

    private void initMediaController() {
        mMediaController = PlayerManager.getInstance().getMediaControllerGenerator().create(getContext(), mVideoInfo);
        if (mMediaController != null) {
            mMediaController.bind(this);
        }
    }

    public FiillPlayer getPlayer() {
        if (mVideoInfo.getUri() == null) {
            throw new RuntimeException("player uri is null");
        }
        return PlayerManager.getInstance().getPlayer(this);
    }

    /**
     * is current active player (in list controllerView there are many players)
     *
     * @return boolean
     */
    public boolean isCurrentActivePlayer() {
        return PlayerManager.getInstance().isCurrentPlayer(mVideoInfo.getFingerprint());
    }

    public IPlayerListener getMediaController() {
        return mMediaController;
    }

    /**
     * is video controllerView in 'list' controllerView
     */
    public boolean inListView() {
        for (ViewParent vp = getParent(); vp != null; vp = vp.getParent()) {
            if (vp instanceof AbsListView
                    || vp instanceof ScrollView) {
                return true;
            }
        }
        return false;
    }

    public ViewGroup getContainer() {
        return mViewGroupContainer;
    }

    public ImageView getCoverView() {
        return findViewById(R.id.app_video_cover);
    }

    public void showMediaInfo(){
        IMediaPlayer mp = getPlayer().getMediaPlayer();
        TableLayoutBinder builder = Utils.getMediaInfo(mp, getContext());

        AlertDialog.Builder adBuilder = builder.buildAlertDialogBuilder();
        adBuilder.setTitle(R.string.media_information);
        adBuilder.setNegativeButton(R.string.close, null);
        adBuilder.show();
    }
}
