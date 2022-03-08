package com.fiill.fiillplayer.controller;

import tv.danmaku.ijk.media.player.IjkTimedText;

/**
 *  Interface to adapter ijkplayer or android media player.
 *  either defaultPlayerListener or streamMediaController will implement it.
 */

public interface IPlayerListener {

    void onPrepared(FiillPlayer fiillPlayer);

    /**
     * Called to update status in buffering a media stream received through progressive HTTP download.
     * @param fiillplayer the listening player
     * @param percent nt: the percentage (0-100) of the content that has been buffered or played thus far
     */
    void onBufferingUpdate(FiillPlayer fiillplayer, int percent);

    boolean onInfo(FiillPlayer fiillplayer, int what, int extra);

    void onCompletion(FiillPlayer fiillplayer);

    void onSeekComplete(FiillPlayer fiillplayer);

    boolean onError(FiillPlayer fiillplayer,int what, int extra);

    void onPause(FiillPlayer fiillplayer);

    void onRelease(FiillPlayer fiillplayer);

    void onStart(FiillPlayer fiillplayer);

    void onTargetStateChange(int oldState, int newState);

    void onCurrentStateChange(int oldState, int newState);

    void onDisplayModelChange(int oldModel, int newModel);

    void onPreparing(FiillPlayer fiillPlayer);

    /**
     * render subtitle
     * @param fiillplayer the listening player
     * @param text timed text string
     */
    void onTimedText(FiillPlayer fiillplayer,IjkTimedText text);

    void bind(VideoView videoView);
}
