package com.fiill.fiillplayer.trackselector;

import tv.danmaku.ijk.media.player.misc.ITrackInfo;

/**
 * Track information Wrapper
 */

public class TrackInfoWrapper {
    private final ITrackInfo mInnerTrack;
    private final int mIndex;
    private final int mTrackType;

    public int getIndex() {
        return mIndex;
    }

    public int getTrackType() {
        return mTrackType;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    private final String fingerprint;

    public TrackInfoWrapper(String fingerprint,ITrackInfo track,int index,int trackType) {
        this.fingerprint = fingerprint;
        this.mInnerTrack = track;
        this.mIndex = index;
        this.mTrackType = trackType;
    }

    public String getInfo() {
        return mInnerTrack == null ? "OFF" : mInnerTrack.getInfoInline();
    }

    public static TrackInfoWrapper OFF(String fingerprint, int trackType) {
        return new TrackInfoWrapper(fingerprint,null,-1,trackType);
    }
}
