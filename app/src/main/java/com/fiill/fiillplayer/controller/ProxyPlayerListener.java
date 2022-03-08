package com.fiill.fiillplayer.controller;

import android.util.Log;

import tv.danmaku.ijk.media.player.IjkTimedText;


/**
 * This proxy to choose which PlayerListener to use,
 * either StreamMediaController or DefaultPlayerListener
 */

public class ProxyPlayerListener implements IPlayerListener {
    private static final String TAG = "FiillListener";
    private final VideoInfo mVideoInfo;
    private IPlayerListener mOuterListener;

    public ProxyPlayerListener(VideoInfo videoInfo) {
        this.mVideoInfo = videoInfo;
    }

    public IPlayerListener getOuterListener() {
        return mOuterListener;
    }


    public void setOuterListener(IPlayerListener outerListener) {
        this.mOuterListener = outerListener;
    }

    private IPlayerListener outerListener() {
        if (mOuterListener != null) {
            return mOuterListener;
        }
        VideoView videoView = PlayerManager.getInstance().getVideoView(mVideoInfo);
        if (videoView != null && videoView.getPlayerListener() != null) {
            return videoView.getPlayerListener();
        }
        return DefaultPlayerListener.INSTANCE;
    }

    private IPlayerListener listener() {
        VideoView videoView = PlayerManager.getInstance().getVideoView(mVideoInfo);
        if (videoView != null && videoView.getMediaController() != null) {
            return videoView.getMediaController();
        }
        return DefaultPlayerListener.INSTANCE;
    }

    @Override
    public void onPrepared(FiillPlayer fiillPlayer) {
        log("onPrepared");
        listener().onPrepared(fiillPlayer);
        outerListener().onPrepared(fiillPlayer);
    }

    @Override
    public void onBufferingUpdate(FiillPlayer fiillPlayer, int percent) {
        listener().onBufferingUpdate(fiillPlayer,percent);
        outerListener().onBufferingUpdate(fiillPlayer,percent);
    }

    @Override
    public boolean onInfo(FiillPlayer fiillPlayer, int what, int extra) {
        if (FiillPlayer.debug) {
            log("onInfo:"+what+","+extra);
        }
        listener().onInfo(fiillPlayer,what,extra);
        return outerListener().onInfo(fiillPlayer,what,extra);
    }

    @Override
    public void onCompletion(FiillPlayer fiillPlayer) {
        log("onCompletion");
        listener().onCompletion(fiillPlayer);
        outerListener().onCompletion(fiillPlayer);
    }

    @Override
    public void onSeekComplete(FiillPlayer fiillPlayer) {
        log("onSeekComplete");
        listener().onSeekComplete(fiillPlayer);
        outerListener().onSeekComplete(fiillPlayer);

    }

    @Override
    public boolean onError(FiillPlayer fiillPlayer, int what, int extra) {
        if (FiillPlayer.debug) {
            log("onError:"+what+","+extra);
        }
        listener().onError(fiillPlayer,what,extra);
        return outerListener().onError(fiillPlayer,what,extra);
    }

    @Override
    public void onPause(FiillPlayer fiillPlayer) {
        log("onPause");
        listener().onPause(fiillPlayer);
        outerListener().onPause(fiillPlayer);
    }

    @Override
    public void onRelease(FiillPlayer fiillPlayer) {
        log("onRelease");
        listener().onRelease(fiillPlayer);
        outerListener().onRelease(fiillPlayer);

    }

    @Override
    public void onStart(FiillPlayer fiillPlayer) {
        log("onStart");
        listener().onStart(fiillPlayer);
        outerListener().onStart(fiillPlayer);
    }

    @Override
    public void onTargetStateChange(int oldState, int newState) {
        if (FiillPlayer.debug) {
            log("onTargetStateChange:"+oldState+"->"+newState);
        }
        listener().onTargetStateChange(oldState,newState);
        outerListener().onTargetStateChange(oldState,newState);
    }

    @Override
    public void onCurrentStateChange(int oldState, int newState) {
        if (FiillPlayer.debug) {
            log("onCurrentStateChange:"+oldState+"->"+newState);
        }
        listener().onCurrentStateChange(oldState,newState);
        outerListener().onCurrentStateChange(oldState,newState);
    }

    @Override
    public void onDisplayModelChange(int oldModel, int newModel) {
        if (FiillPlayer.debug) {
            log("onDisplayModelChange:"+oldModel+"->"+newModel);
        }
        listener().onDisplayModelChange(oldModel,newModel);
        outerListener().onDisplayModelChange(oldModel,newModel);
    }

    public void onPreparing(FiillPlayer fiillPlayer) {
        log("onPreparing");
        listener().onPreparing(fiillPlayer);
        outerListener().onPreparing(fiillPlayer);
    }

    @Override
    public void onTimedText(FiillPlayer fiillPlayer, IjkTimedText text) {
        if (FiillPlayer.debug) {
            log("onTimedText:"+(text!=null?text.getText():"null"));
        }
        listener().onTimedText(fiillPlayer,text);
        outerListener().onTimedText(fiillPlayer,text);
    }

    @Override
    public void bind(VideoView videoView) {}

    private void log(String msg) {
        if (FiillPlayer.debug) {
            Log.d(TAG, String.format("[fingerprint:%s] %s", mVideoInfo.getFingerprint(), msg));
        }
    }

    /**
     * default player controller if no MediaController.
     */

    public static class DefaultPlayerListener implements IPlayerListener {

        public static final DefaultPlayerListener INSTANCE = new DefaultPlayerListener();

        @Override
        public void onPrepared(FiillPlayer fiillplayer) { }

        @Override
        public void onBufferingUpdate(FiillPlayer fiillplayer, int percent) { }

        @Override
        public boolean onInfo(FiillPlayer fp, int what, int extra) { return true;}

        @Override
        public void onCompletion(FiillPlayer fiillplayer) { }

        @Override
        public void onSeekComplete(FiillPlayer fiillplayer) { }

        @Override
        public boolean onError(FiillPlayer fiillplayer, int what, int extra) {return true;}

        @Override
        public void onPause(FiillPlayer fiillplayer) { }

        @Override
        public void onRelease(FiillPlayer fiillplayer) { }

        @Override
        public void onStart(FiillPlayer fiillplayer) { }

        @Override
        public void onTargetStateChange(int oldState, int newState) { }

        @Override
        public void onCurrentStateChange(int oldState, int newState) { }

        @Override
        public void onDisplayModelChange(int oldModel, int newModel) { }

        @Override
        public void onPreparing(FiillPlayer fiillplayer) { }

        @Override
        public void onTimedText(FiillPlayer fiillplayer, IjkTimedText text) { }

        @Override
        public void bind(VideoView videoView) { }
    }
}
