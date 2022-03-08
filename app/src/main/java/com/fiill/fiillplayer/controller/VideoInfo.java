package com.fiill.fiillplayer.controller;

import android.graphics.Color;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.fiill.fiillplayer.application.Option;
import com.fiill.fiillplayer.widgets.MeasureHelper;

import java.util.Collection;
import java.util.HashSet;

/**
 *  video information for create video view or setting properties.
 */

public class VideoInfo implements Parcelable {
    public static final int AR_ASPECT_FIT_PARENT = MeasureHelper.AR_ASPECT.AR_ASPECT_FIT_PARENT; // without clip
    public static final int AR_ASPECT_FILL_PARENT = MeasureHelper.AR_ASPECT.AR_ASPECT_FILL_PARENT; // may clip
    public static final int AR_ASPECT_WRAP_CONTENT = MeasureHelper.AR_ASPECT.AR_ASPECT_WRAP_CONTENT;
    public static final int AR_MATCH_PARENT = MeasureHelper.AR_ASPECT.AR_MATCH_PARENT;
    public static final int AR_16_9_FIT_PARENT = MeasureHelper.AR_ASPECT.AR_16_9_FIT_PARENT;
    public static final int AR_4_3_FIT_PARENT = MeasureHelper.AR_ASPECT.AR_4_3_FIT_PARENT;
    public static final int AR_9_16_FIT_PARENT = MeasureHelper.AR_ASPECT.AR_9_16_FIT_PARENT;
    public static final int AR_3_4_FIT_PARENT = MeasureHelper.AR_ASPECT.AR_3_4_FIT_PARENT;

    private HashSet<Option> mOptions = new HashSet<>();
    private boolean mIsShowTopBar = false;
    private Uri mUri;
    private String mFingerprint = Integer.toHexString(hashCode());
    private boolean mPortraitWhenFullScreen = true;
    private String mTitle;
    private int mAspectRatio = AR_ASPECT_FIT_PARENT;
    private String mLastFingerprint;
    private Uri mLastUri;
    private int mRetryInterval=0;
    private int mBackgroundColor = Color.DKGRAY;
    private boolean mIsFullScreenAnimation = true;
    private boolean mIsLooping = false;
    private boolean mIsCurrentVideoAsCover = true; //set current video as cover image when player released
    private boolean mIsFullScreenOnly = false;

    public VideoInfo() {
        mFingerprint = Integer.toHexString(hashCode());
    }

    public VideoInfo(VideoInfo defaultVideoInfo) {
        mFingerprint = Integer.toHexString(hashCode());
        this.mTitle = defaultVideoInfo.mTitle;
        mPortraitWhenFullScreen = defaultVideoInfo.mPortraitWhenFullScreen;
        mAspectRatio = defaultVideoInfo.mAspectRatio;
        for (Option op : defaultVideoInfo.mOptions) {
            try {
                mOptions.add(op.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        mIsShowTopBar = defaultVideoInfo.mIsShowTopBar;
        mRetryInterval = defaultVideoInfo.mRetryInterval;
        mBackgroundColor = defaultVideoInfo.mBackgroundColor;
        mIsFullScreenAnimation = defaultVideoInfo.mIsFullScreenAnimation;
        mIsLooping = defaultVideoInfo.mIsLooping;
        mIsCurrentVideoAsCover = defaultVideoInfo.mIsCurrentVideoAsCover;
        mIsFullScreenOnly = defaultVideoInfo.mIsFullScreenOnly;
        mLastFingerprint = defaultVideoInfo.mLastFingerprint;
        mUri = defaultVideoInfo.mUri;
        mLastUri = defaultVideoInfo.mLastUri;

    }

    public boolean isFullScreenOnly() {
        return mIsFullScreenOnly;
    }

    public VideoInfo setFullScreenOnly(boolean fullScreenOnly) {
        this.mIsFullScreenOnly = fullScreenOnly;
        return this;
    }

    public boolean isFullScreenAnimation() {
        return mIsFullScreenAnimation;
    }

    public VideoInfo setFullScreenAnimation(boolean fullScreenAnimation) {
        this.mIsFullScreenAnimation = fullScreenAnimation;
        return this;
    }

    public int getBgColor() {
        return mBackgroundColor;
    }

    public int getRetryInterval() {
        return mRetryInterval;
    }

    /**
     * retry to play again interval (in second)
     * @param retryInterval interval in second <=0 will disable retry
     * @return VideoInfo
     */
    public VideoInfo setRetryInterval(int retryInterval) {
        this.mRetryInterval = retryInterval;
        return this;
    }


    public HashSet<Option> getOptions() {
        return mOptions;
    }

    /**
     * add player init option
     * @param option option
     * @return VideoInfo
     */
    public VideoInfo addOption(Option option) {
        this.mOptions.add(option);
        return this;
    }

    /**
     * add player init option
     * @param options option
     * @return VideoInfo
     */
    public VideoInfo addOptions(Collection<Option> options) {
        this.mOptions.addAll(options);
        return this;
    }

    public boolean isShowTopBar() {
        return mIsShowTopBar;
    }

    /**
     * show top bar(back arrow and title) when user tap the view
     * @param showTopBar true to show
     * @return VideoInfo
     */
    public VideoInfo setShowTopBar(boolean showTopBar) {
        this.mIsShowTopBar = showTopBar;
        return this;
    }

    public boolean isPortraitWhenFullScreen() {
        return mPortraitWhenFullScreen;
    }

    public String getTitle() {
        return mTitle;
    }

    /**
     * video title
     * @param title title
     * @return VideoInfo
     */
    public VideoInfo setTitle(String title) {
        this.mTitle = title;
        return this;
    }

    public int getAspectRatio() {
        return mAspectRatio;
    }

    public float getAspectRatioValue(float mediaDefaultAspectRatio) {
        switch (mAspectRatio){
            case AR_ASPECT_FIT_PARENT:
                return mediaDefaultAspectRatio;
            case AR_3_4_FIT_PARENT:
                return 4.0f/3.0f;
            case AR_9_16_FIT_PARENT:
                return 16.0f/9.0f;
            case AR_16_9_FIT_PARENT:
                return 9.0f/16.0f;
            case AR_4_3_FIT_PARENT:
                return 3.0f/4.0f;
        }
        return mediaDefaultAspectRatio;
    }

    public VideoInfo setAspectRatio(int aspectRatio) {
        this.mAspectRatio = aspectRatio;
        return this;
    }

    public VideoInfo(String uri) {
        this.mUri = Uri.parse(uri);
    }

    protected VideoInfo(Parcel in) {
        mFingerprint = in.readString();
        mUri = in.readParcelable(Uri.class.getClassLoader());
        mTitle = in.readString();
        mPortraitWhenFullScreen = in.readByte() != 0;
        mAspectRatio = in.readInt();
        mLastFingerprint = in.readString();
        mLastUri = in.readParcelable(Uri.class.getClassLoader());
        mOptions = (HashSet<Option>) in.readSerializable();
        mIsShowTopBar = in.readByte() != 0;
        mRetryInterval = in.readInt();
        mBackgroundColor = in.readInt();
        mIsFullScreenAnimation = in.readByte() != 0;
        mIsLooping = in.readByte() != 0;
        mIsCurrentVideoAsCover = in.readByte() != 0;
        mIsFullScreenOnly = in.readByte() != 0;

    }

    public static final Creator<VideoInfo> CREATOR = new Creator<VideoInfo>() {
        @Override
        public VideoInfo createFromParcel(Parcel in) {
            return new VideoInfo(in);
        }

        @Override
        public VideoInfo[] newArray(int size) {
            return new VideoInfo[size];
        }
    };

    /*
    public VideoInfo setFingerprint(Object fingerprint) {
        String fp = "" + fingerprint;//to string first
        if (lastFingerprint!=null && !lastFingerprint.equals(fp)) {
            //different from last setFingerprint, release last
            PlayerManager.getInstance().releaseByFingerprint(lastFingerprint);
        }
        this.fingerprint = fp;
        lastFingerprint = this.fingerprint;
        return this;
    }
    */

    /**
     * A Fingerprint represent a player
     * @return setFingerprint
     */
    public String getFingerprint() {
        return mFingerprint;
    }

    public Uri getUri() {
        return mUri;
    }

    /**
     * set video uri
     * @param uri uri
     * @return VideoInfo
     */
    public VideoInfo setUri(Uri uri) {
        if (mLastUri!=null && !mLastUri.equals(uri)) {
            //different from last uri, release last
            PlayerManager.getInstance().releaseByFingerprint(mFingerprint);
        }
        this.mUri = uri;
        this.mLastUri = this.mUri;
        return this;
    }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mFingerprint);
        dest.writeParcelable(mUri, flags);
        dest.writeString(mTitle);
        dest.writeByte((byte) (mPortraitWhenFullScreen ? 1 : 0));
        dest.writeInt(mAspectRatio);
        dest.writeString(mLastFingerprint);
        dest.writeParcelable(mLastUri, flags);
        dest.writeSerializable(mOptions);
        dest.writeByte((byte) (mIsShowTopBar ? 1 : 0));
        dest.writeInt(mRetryInterval);
        dest.writeInt(mBackgroundColor);
        dest.writeByte((byte) (mIsFullScreenAnimation ? 1 : 0));
        dest.writeByte((byte) (mIsLooping ? 1 : 0));
        dest.writeByte((byte) (mIsCurrentVideoAsCover ? 1 : 0));
        dest.writeByte((byte) (mIsFullScreenOnly ? 1 : 0));
    }

    public static VideoInfo createFromDefault(){
        return new VideoInfo(PlayerManager.getInstance().getDefaultVideoInfo());
    }

    public boolean isLooping() {
        return mIsLooping;
    }

    public boolean isCurrentVideoAsCover() {
        return mIsCurrentVideoAsCover;
    }
}
