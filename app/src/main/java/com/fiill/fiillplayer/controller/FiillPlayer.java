package com.fiill.fiillplayer.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.MediaController;

import com.fiill.fiillplayer.R;
import com.fiill.fiillplayer.activities.PlayActivity;
import com.fiill.fiillplayer.application.Option;
import com.fiill.fiillplayer.application.FiillSettings;
import com.fiill.fiillplayer.content.RecentMediaStorage;
import com.fiill.fiillplayer.services.MediaPlayerService;
import com.fiill.fiillplayer.utils.Utils;
import com.fiill.fiillplayer.widgets.FloatWindowsManager;
import com.fiill.fiillplayer.widgets.IScalableDisplay;
import com.fiill.fiillplayer.widgets.ScalableTextureView;
import com.fiill.fiillplayer.widgets.UIHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;


public class FiillPlayer implements MediaController.MediaPlayerControl {
    public static final String TAG = "FiillPlayer";
    public static boolean debug = false;
    // Internal messages
    private static final int MSG_CTRL_PLAYING = 1;

    private static final int MSG_CTRL_PAUSE = 2;
    private static final int MSG_CTRL_SEEK = 3;
    private static final int MSG_CTRL_RELEASE = 4;
    private static final int MSG_CTRL_RETRY = 5;
    private static final int MSG_CTRL_SELECT_TRACK = 6;
    private static final int MSG_CTRL_DESELECT_TRACK = 7;
    private static final int MSG_CTRL_SET_VOLUME = 8;
    private static final int MSG_SET_DISPLAY = 12;

    // all possible internal states
    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_PLAYBACK_COMPLETED = 5;
    public static final int STATE_RELEASE = 6;
    public static final int DISPLAY_NORMAL = 0;
    public static final int DISPLAY_FULL_WINDOW = 1;
    public static final int DISPLAY_FLOAT = 2;
    public static final int DISPLAY_IDLE = 3;

    private int mCurrentBufferPercentage = 0;
    private boolean mCanSeekBackward = true;
    private boolean mCanSeekForward = true;
    private int mAudioSessionId;

    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;
    private Uri mUri;
    private final Map<String, String> mHeaders = new HashMap<>();
    private final Context mContext;

    private IMediaPlayer mMediaPlayer;
    private volatile boolean mIsReleased;
    private final Handler mPlayHandler;
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private final HandlerThread mInternalPlaybackThread;
    private final ProxyPlayerListener mProxyListener;

    private volatile int mStartPosition = -1;
    private WeakReference<? extends ViewGroup> mDisplayBoxRef;
    private int mIgnoreOrientation = -100;

    private int mDisplayModel = DISPLAY_NORMAL;
    private int mLastDisplayModel = mDisplayModel;
    private final VideoInfo mVideoInfo;
    private final WeakReference<? extends ViewGroup> mBoxContainerRef;
    private final FiillSettings mSettings;
    private final FloatWindowsManager mFloatWindowsManager;

    private FiillPlayer(final Context context, final VideoInfo videoInfo) {
        mContext = context.getApplicationContext();
        mVideoInfo = videoInfo;
        Activity activity = getActivity();
        mSettings = new FiillSettings(activity);
        mFloatWindowsManager = new FloatWindowsManager(activity);
        VideoView videoView = PlayerManager.getInstance().getVideoView(videoInfo);
        mBoxContainerRef = new WeakReference<>(videoView != null ? videoView.getContainer() : null);
        if (mBoxContainerRef.get() != null) {
            mBoxContainerRef.get().setBackgroundColor(videoInfo.getBgColor());
        }
        this.mProxyListener = new ProxyPlayerListener(videoInfo);
        mInternalPlaybackThread = new HandlerThread("FiillPlayerInternal:Handler", Process.THREAD_PRIORITY_AUDIO);
        mInternalPlaybackThread.start();
        init(true);
        mPlayHandler = new Handler(mInternalPlaybackThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                //init mediaPlayer before any actions
                log("handleMessage:" + msg.what);
                if (msg.what == MSG_CTRL_RELEASE) {
                    if (!mIsReleased) {
                        mPlayHandler.removeCallbacks(null);
                        currentState(STATE_RELEASE);
                        doRelease(((String) msg.obj));
                    }
                    return true;
                }
                if (mMediaPlayer == null || mIsReleased) {
                    mPlayHandler.removeCallbacks(null);
                    try {
                        init(true);
                        mPlayHandler.sendMessage(Message.obtain(msg));
                    } catch (UnsatisfiedLinkError e) {
                        log("UnsatisfiedLinkError:" + e);
                    }
                    return true;
                }
                switch (msg.what) {
                    case MSG_CTRL_PLAYING:
                        if (mCurrentState == STATE_ERROR) {
                            Message retry_msg = new Message();
                            retry_msg.what = MSG_CTRL_RETRY;
                            mPlayHandler.sendMessageDelayed(retry_msg, 1000);
                        } else if (isInPlaybackState()) {
                            if (mCanSeekForward) {
                                if (mCurrentState == STATE_PLAYBACK_COMPLETED) {
                                    mStartPosition = 0;
                                }
                                if (mStartPosition >= 0) {
                                    mMediaPlayer.seekTo(mStartPosition);
                                    mStartPosition = -1;
                                }
                            }
                            mMediaPlayer.start();
                            //RecentMediaStorage.saveUrlAsync(context, videoInfo.getUri().toString());
                            currentState(STATE_PLAYING);
                        }
                        break;
                    case MSG_CTRL_PAUSE:
                        mMediaPlayer.pause();
                        currentState(STATE_PAUSED);
                        break;
                    case MSG_CTRL_SEEK:
                        if (!mCanSeekForward) {
                            break;
                        }
                        int position = (int) msg.obj;
                        mMediaPlayer.seekTo(position);
                        break;
                    case MSG_CTRL_SELECT_TRACK:
                        int track = (int) msg.obj;
                        if (mMediaPlayer instanceof IjkMediaPlayer) {
                            ((IjkMediaPlayer) mMediaPlayer).selectTrack(track);
                        } else if (mMediaPlayer instanceof AndroidMediaPlayer) {
                            ((AndroidMediaPlayer) mMediaPlayer).getInternalMediaPlayer().selectTrack(track);
                        }
                        break;
                    case MSG_CTRL_DESELECT_TRACK:
                        int deselectTrack = (int) msg.obj;
                        if (mMediaPlayer instanceof IjkMediaPlayer) {
                            ((IjkMediaPlayer) mMediaPlayer).deselectTrack(deselectTrack);
                        } else if (mMediaPlayer instanceof AndroidMediaPlayer) {
                            ((AndroidMediaPlayer) mMediaPlayer).getInternalMediaPlayer().deselectTrack(deselectTrack);
                        }
                        break;
                    case MSG_SET_DISPLAY:
                        if (msg.obj == null) {
                            mMediaPlayer.setDisplay(null);
                        } else if (msg.obj instanceof SurfaceTexture) {
                            mMediaPlayer.setSurface(new Surface((SurfaceTexture) msg.obj));
                        } else if (msg.obj instanceof SurfaceView) {
                            mMediaPlayer.setDisplay(((SurfaceView) msg.obj).getHolder());
                        }
                        break;
                    case MSG_CTRL_RETRY:
                        init(false);
                        mPlayHandler.sendEmptyMessage(MSG_CTRL_PLAYING);
                        break;
                    case MSG_CTRL_SET_VOLUME:
                        if(msg.obj != null) {
                            Map<String, Float> pram = (Map<String, Float>) msg.obj;
                            mMediaPlayer.setVolume(pram.get("left"), pram.get("right"));
                        }
                        break;

                    default:
                }
                return true;
            }
        });
        PlayerManager.getInstance().setCurrentPlayer(this);
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    @Override
    public void start() {
        if (mCurrentState == STATE_PLAYBACK_COMPLETED && !mCanSeekForward) {
            releaseMediaPlayer();
        }
        targetState(STATE_PLAYING);
        mPlayHandler.sendEmptyMessage(MSG_CTRL_PLAYING);
        mProxyListener.onStart(this);
    }

    private void targetState(final int newState) {
        final int oldTargetState = mTargetState;
        mTargetState = newState;
        if (oldTargetState != newState) {
            mUiHandler.post(()-> mProxyListener.onTargetStateChange(oldTargetState, newState));
        }
    }

    private void currentState(final int newState) {
        final int oldCurrentState = mCurrentState;
        mCurrentState = newState;
        if (oldCurrentState != newState) {
            mUiHandler.post(()-> mProxyListener.onCurrentStateChange(oldCurrentState, newState));
        }
    }

    @Override
    public void pause() {
        targetState(STATE_PAUSED);
        mPlayHandler.sendEmptyMessage(MSG_CTRL_PAUSE);
        mProxyListener.onPause(this);
    }

    @Override
    public int getDuration() {
        if (mMediaPlayer == null) {
            return 0;
        }
        return (int) mMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        if (mMediaPlayer == null) {
            return 0;
        }
        return (int) mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mPlayHandler.obtainMessage(MSG_CTRL_SEEK, pos).sendToTarget();
    }

    @Override
    public boolean isPlaying() {
        return mCurrentState == STATE_PLAYING;
    }

    @Override
    public int getBufferPercentage() {
        return mCurrentBufferPercentage;
    }

    @Override
    public boolean canPause() { return true; }

    @Override
    public boolean canSeekBackward() {
        return mCanSeekBackward;
    }

    @Override
    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    @Override
    public int getAudioSessionId() {
        if (mAudioSessionId == 0) {
            mAudioSessionId = mMediaPlayer.getAudioSessionId();
        }
        return mAudioSessionId;
    }

    /**
     * Sets video URI using specific headers.
     *
     *  uri     the URI of the video.
     *  headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     *

    private FiillPlayer setVideoURI(Uri uri, Map<String, String> headers){
        this.mUri = uri;
        this.mHeaders.clear();
        this.mHeaders.putAll(headers);
        return this;
    }*/

    private void init(boolean createDisplay) {
        log("init createDisplay:" + createDisplay);
        mUiHandler.post(()-> mProxyListener.onPreparing(FiillPlayer.this));
        releaseMediaPlayer();
        mMediaPlayer = createMediaPlayer();
        setOptions();

        mIsReleased = false;
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(mVideoInfo.isLooping());
        initInternalListener();
        if (createDisplay) {
            VideoView videoView = PlayerManager.getInstance().getVideoView(mVideoInfo);
            if (videoView != null && videoView.getContainer() != null) {
                createDisplay(videoView.getContainer());
            }
        }
        try {
            mUri = mVideoInfo.getUri();
            mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
            currentState(STATE_PREPARING);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            currentState(STATE_ERROR);
            e.printStackTrace();
            mUiHandler.post(() -> mProxyListener.onError(FiillPlayer.this, 0, 0));
        }
        initBackground();
    }

    private IMediaPlayer createMediaPlayer() {
        int playerType = mSettings.getPreferredPlayerType();
        if(playerType == FiillSettings.PREFER_PLAYER_ANDROID_MEDIA_PLAYER) {
            return new AndroidMediaPlayer();
        } else { //if(playerType == FiillSettings.PREFER_PLAYER_AUTO || playerType == FiillSettings.PREFER_PLAYER_IJK_PLAYER )
            IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
            IjkMediaPlayer.native_setLogLevel(FiillSettings.Config.isDebug() ?
                            IjkMediaPlayer.IJK_LOG_DEBUG : IjkMediaPlayer.IJK_LOG_ERROR);
            return ijkMediaPlayer;
        }
    }

    private void setOptions() {
        mHeaders.clear();
        if (mMediaPlayer instanceof IjkMediaPlayer) {
            //default options
            IjkMediaPlayer ijkMediaPlayer = (IjkMediaPlayer) mMediaPlayer;
            boolean usingMediaCodec = mSettings.getUsingMediaCodec();
            boolean usingMediaCodecAutoRotate = mSettings.getUsingMediaCodecAutoRotate();
            boolean usingOpenSLES = mSettings.getUsingOpenSLES();
            String pixelFormat = mSettings.getPixelFormat();
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
            IjkMediaPlayer.native_setLogLevel(FiillSettings.Config.isDebug()?
                            IjkMediaPlayer.IJK_LOG_DEBUG:IjkMediaPlayer.IJK_LOG_ERROR);
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);

            if (usingMediaCodec) {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
                if (usingMediaCodecAutoRotate) {
                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
                } else {
                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
                }
            } else {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
            }

            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", usingOpenSLES ? 1 : 0);

            if (TextUtils.isEmpty(pixelFormat)) {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
            } else {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", pixelFormat);
            }
            //support H265
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1);

            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 10000000);
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1);
            //user options
            if (mVideoInfo.getOptions().size() <= 0) {
                return;
            }
            for (Option option : mVideoInfo.getOptions()) {
                try {
                    if (option.getValue() instanceof String) {
                        ijkMediaPlayer.setOption(option.getCategory(), option.getName(), ((String) option.getValue()));
                    } else if (option.getValue() instanceof Long) {
                        ijkMediaPlayer.setOption(option.getCategory(), option.getName(), ((Long) option.getValue()));
                    }
                }catch (IllegalStateException ignored){ }
            }
        } else if (mMediaPlayer instanceof AndroidMediaPlayer) {
            if (mVideoInfo.getOptions().size() <= 0) {
                return;
            }
            for (Option option : mVideoInfo.getOptions()) {
                if (IjkMediaPlayer.OPT_CATEGORY_FORMAT == option.getCategory() && "headers".equals(option.getName())) {
                    String h = "" + option.getValue();
                    String[] hs = h.split("\r\n");
                    for (String hd : hs) {
                        String[] kv = hd.split(":");
                        String v = kv.length >= 2 ? kv[1] : "";
                        mHeaders.put(kv[0], v);
                        log("add header " + kv[0] + ":" + v);
                    }
                    break;
                }
            }
        }
    }

    private void initInternalListener() {
        //mProxyListener fire on main thread
        mMediaPlayer.setOnPreparedListener(iMediaPlayer -> {
            if (!TextUtils.isEmpty(mUri.toString())) {
                RecentMediaStorage.saveUrlAsync(mContext, mUri.toString());
            }
            mUiHandler.post(() -> {
                if(null == mMediaPlayer) return;
                boolean live = mMediaPlayer.getDuration() == 0;
                mCanSeekBackward = !live;
                mCanSeekForward = !live;
                currentState(STATE_PREPARED);
                mProxyListener.onPrepared(FiillPlayer.this);
                if (mTargetState == STATE_PLAYING) {
                    mPlayHandler.sendEmptyMessage(MSG_CTRL_PLAYING);
                }
            });
        });
        mMediaPlayer.setOnBufferingUpdateListener((iMediaPlayer, percent) -> {
            mCurrentBufferPercentage = percent;
            mUiHandler.post(() -> mProxyListener.onBufferingUpdate(FiillPlayer.this, percent));
        });
        mMediaPlayer.setOnInfoListener((iMediaPlayer, what, extra) -> {
            final boolean[] ret = new boolean[1];
            mUiHandler.post(() -> {
                if (what == IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED) {
                    ScalableTextureView currentDisplay = getCurrentDisplay();
                    if (currentDisplay != null) {
                        currentDisplay.setRotation(extra);
                    }
                }
                ret[0] =  mProxyListener.onInfo(FiillPlayer.this, what, extra);
            });
            return ret[0];
        });
        mMediaPlayer.setOnCompletionListener(iMediaPlayer -> mUiHandler.post(() ->  {
            currentState(STATE_PLAYBACK_COMPLETED);
            mProxyListener.onCompletion(FiillPlayer.this);
        }));
        mMediaPlayer.setOnErrorListener((iMediaPlayer, what, extra) -> {
            final boolean[] ret = new boolean[1];
            mUiHandler.post(() ->  {
                currentState(STATE_ERROR);
                ret[0] = mProxyListener.onError(FiillPlayer.this, what, extra);
                int retryInterval = mVideoInfo.getRetryInterval();
                if (retryInterval > 0) {
                    log("replay delay " + retryInterval + " seconds");
                    mPlayHandler.sendEmptyMessageDelayed(MSG_CTRL_RETRY, retryInterval * 1000L);
                }
            });
            return ret[0];
        });
        mMediaPlayer.setOnSeekCompleteListener(
                iMediaPlayer -> mUiHandler.post(() -> mProxyListener.onSeekComplete(FiillPlayer.this)));
        mMediaPlayer.setOnVideoSizeChangedListener((mp, width, height, sarNum, sarDen) -> {
            if (debug) {
                log("onVideoSizeChanged:width:" + width + ",height:" + height);
            }
            mUiHandler.post(() -> {
                    int videoWidth = mp.getVideoWidth();
                    int videoHeight = mp.getVideoHeight();
                    //int videoSarNum = mp.getVideoSarNum(); int videoSarDen = mp.getVideoSarDen();
                    if (videoWidth != 0 && videoHeight != 0) {
                    View currentDisplay = getCurrentDisplay();
                    if (currentDisplay != null) {
                        IScalableDisplay scalableDisplay = (IScalableDisplay) currentDisplay;
                        scalableDisplay.setVideoSize(videoWidth, videoHeight);
                        }
                   }
            });
        });
        mMediaPlayer.setOnTimedTextListener((mp, text) -> mUiHandler.post(() ->
                        mProxyListener.onTimedText(FiillPlayer.this, text)));
    }

    public static FiillPlayer createPlayer(Context context, VideoInfo videoInfo) {
        return new FiillPlayer(context, videoInfo);
    }

    private void bindDisplay(final TextureView textureView) {
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            private SurfaceTexture _surface;
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                log("onSurfaceTextureAvailable");
                if (this._surface == null) {
                    mPlayHandler.obtainMessage(MSG_SET_DISPLAY, surface).sendToTarget();
                    this._surface = surface;
                } else {
                    textureView.setSurfaceTexture(this._surface);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                log("onSurfaceTextureDestroyed");
                 //this callback will be called when view moving if fullscreen.
                // system will destroy the view if set as true, that will make the view unavailable.
                return false;
            }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
        });
    }

    public IPlayerListener getProxyPlayerListener() {
        return this.mProxyListener;
    }

    public void createDisplay(final ViewGroup container) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mUiHandler.post(() -> doCreateDisplay(container));
        } else {
            doCreateDisplay(container);
        }
    }

    private void doCreateDisplay(ViewGroup container) {
        isolateDisplayBox();
        FrameLayout displayBox = new FrameLayout(container.getContext());
        displayBox.setId(R.id.player_display_box);
        displayBox.setBackgroundColor(mVideoInfo.getBgColor());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
        );
        ScalableTextureView textureView = new ScalableTextureView(container.getContext());
        textureView.setAspectRatio(mVideoInfo.getAspectRatio());
        textureView.setId(R.id.player_display);
        displayBox.addView(textureView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        ));
        container.addView(displayBox, 0, lp);
        bindDisplay(textureView);
        mDisplayBoxRef = new WeakReference<>(displayBox);
    }

    private void isolateDisplayBoxContainer() {
        if (mBoxContainerRef != null) {
            ViewGroup box = mBoxContainerRef.get();
            Utils.removeFromParent(box);
        }
    }

    private void isolateDisplayBox() {
        if (mDisplayBoxRef != null) {
            ViewGroup box = mDisplayBoxRef.get();
            Utils.removeFromParent(box);
        }
    }

    private void log(String msg) {
        if (debug) {
            Log.d(TAG, String.format("[fingerprint:%s] %s", mVideoInfo.getFingerprint(), msg));
        }
    }


    private void doRelease(String fingerprint) {
        if (mIsReleased) return;
        PlayerManager.getInstance().removePlayer(fingerprint);
        //1. quit handler thread
        mInternalPlaybackThread.quit();
        //2. remove display group
        releaseDisplayBox();
        releaseMediaPlayer();
        mIsReleased = true;
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            currentState(STATE_IDLE);
            if (cleartargetstate) {
                targetState (STATE_IDLE);
            }
            //(AudioManager) context.getSystemService(Context.AUDIO_SERVICE).abandonAudioFocus(null);
        }
    }

    public IMediaPlayer getMediaPlayer() {
        if (mMediaPlayer != null) {
            return mMediaPlayer;
        }
        return null;
    }

    public void release() {
        String fingerprint = mVideoInfo.getFingerprint();
        PlayerManager.getInstance().removePlayer(fingerprint);
        mProxyListener.onRelease(this);
        mPlayHandler.obtainMessage(MSG_CTRL_RELEASE, fingerprint).sendToTarget();
    }

    private void releaseDisplayBox() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            doReleaseDisplayBox();
        } else {
            mUiHandler.post(this::doReleaseDisplayBox);
        }
    }

    private void doReleaseDisplayBox() {
        ScalableTextureView currentDisplay = getCurrentDisplay();
        if (currentDisplay != null) {
            currentDisplay.setSurfaceTextureListener(null);
        }
        isolateDisplayBox();
    }

    public ScalableTextureView getCurrentDisplay() {
        if (mDisplayBoxRef != null) {
            ViewGroup box = mDisplayBoxRef.get();
            if (box != null) {
                return box.findViewById(R.id.player_display);
            }
        }
        return null;
    }

    public void toggleFullScreen() {
        if (mDisplayModel == DISPLAY_NORMAL) {
            setDisplayModel(DISPLAY_FULL_WINDOW);
        } else {
            setDisplayModel(DISPLAY_NORMAL);
        }
    }

    public void setDisplayModel(int targetDisplayModel) {
        if (targetDisplayModel == mDisplayModel) {
            return ;
        }

        //if no display box container,nothing can do
        if (mBoxContainerRef == null || mBoxContainerRef.get() == null) {
            return ;
        }
        mLastDisplayModel = mDisplayModel;

        if (targetDisplayModel == DISPLAY_IDLE){
            stop();
            mFloatWindowsManager.removeFloatView();
            getActivity().finish();
        } else if (targetDisplayModel == DISPLAY_FULL_WINDOW) {
            Activity activity = getActivity();
            if (activity == null) {
                return ;
            }
            //orientation & action bar
            UIHelper uiHelper = UIHelper.with(activity);
            if (mVideoInfo.isPortraitWhenFullScreen()) {
                uiHelper.requestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                mIgnoreOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            }
            uiHelper.showActionBar(false).fullScreen(true);
            ViewGroup activityBox = activity.findViewById(android.R.id.content);

            animateIntoContainerAndThen(activityBox, new VideoViewAnimationListener() {
                @Override
                public void onStart(ViewGroup src, ViewGroup target) {
                    mFloatWindowsManager.removeFloatContainer();
                }

                @Override
                public void onEnd(ViewGroup src, ViewGroup target) {
                    mProxyListener.onDisplayModelChange(mDisplayModel, DISPLAY_FULL_WINDOW);
                    mDisplayModel = DISPLAY_FULL_WINDOW;
                }
            });

        } else if (targetDisplayModel == DISPLAY_NORMAL) {
            final Activity activity = getActivity();
            if (activity == null) {
                return ;
            }
            final VideoView videoView = PlayerManager.getInstance().getVideoView(mVideoInfo);
            if (videoView == null) {
                return ;
            }
            //change orientation & action bar
            UIHelper uiHelper = UIHelper.with(activity);
            if (mVideoInfo.isPortraitWhenFullScreen()) {
                uiHelper.requestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                mIgnoreOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
            uiHelper.showActionBar(true).fullScreen(false);

            animateIntoContainerAndThen(videoView, new VideoViewAnimationListener() {
                @Override
                public void onStart(ViewGroup src, ViewGroup target) {
                    mFloatWindowsManager.removeFloatContainer();
                }

                @Override
                public void onEnd(ViewGroup src, ViewGroup target) {
                    mProxyListener.onDisplayModelChange(mDisplayModel, DISPLAY_NORMAL);
                    mDisplayModel = DISPLAY_NORMAL;
                }
            });
        } else if (targetDisplayModel == DISPLAY_FLOAT) {
            Activity activity = getActivity();
            if (activity == null) {
                return ;
            }

            //change orientation & action bar
            UIHelper uiHelper = UIHelper.with(activity);
            if (mVideoInfo.isPortraitWhenFullScreen()) {
                uiHelper.requestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                mIgnoreOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
            uiHelper.showActionBar(true).fullScreen(false);

            final ViewGroup floatBox = createFloatBox(
                    mVideoInfo.getAspectRatioValue((float)mMediaPlayer.getVideoHeight() / (float)mMediaPlayer.getVideoWidth()));
            if (null != floatBox) {
                floatBox.setVisibility(View.INVISIBLE);
                animateIntoContainerAndThen(floatBox, new VideoViewAnimationListener() {
                    @Override
                    void onEnd(ViewGroup src, ViewGroup target) {
                        floatBox.setVisibility(View.VISIBLE);
                        mProxyListener.onDisplayModelChange(mDisplayModel, DISPLAY_FLOAT);
                        mDisplayModel = DISPLAY_FLOAT;
                    }
                });
                //press home key
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                getActivity().startActivity(intent);
            }
        }
    }

    static class VideoViewAnimationListener {
        void onStart(ViewGroup src, ViewGroup target) { }
        void onEnd(ViewGroup src, ViewGroup target) { }
    }

    private void animateIntoContainerAndThen(final ViewGroup container, final VideoViewAnimationListener listener) {
        final ViewGroup displayBoxContainer = mBoxContainerRef.get();
        boolean usingAnim = usingAnim();
        if (!usingAnim) { //no animation
            listener.onStart(displayBoxContainer, container);
            if (displayBoxContainer.getParent() != container) {
                isolateDisplayBoxContainer();
                container.addView(displayBoxContainer, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            }
            listener.onEnd(displayBoxContainer, container);
            return;
        }

        final Activity activity = getActivity();
        if (activity == null) return;

        mUiHandler.post(()-> {
            ViewGroup activityBox = activity.findViewById(android.R.id.content);
            int[] targetXY = new int[]{0, 0};
            int[] activityBoxXY = new int[]{0, 0};

            //1. set src LayoutParams
            activityBox.getLocationInWindow(activityBoxXY);
            if (displayBoxContainer.getParent() != activityBox) {
                int[] srcXY = new int[]{0, 0};
                FrameLayout.LayoutParams srcLayoutParams =
                        new FrameLayout.LayoutParams(displayBoxContainer.getWidth(), displayBoxContainer.getHeight());
                displayBoxContainer.getLocationInWindow(srcXY);
                srcLayoutParams.leftMargin = srcXY[0] - activityBoxXY[0];
                srcLayoutParams.topMargin = srcXY[1] - activityBoxXY[1];
                isolateDisplayBoxContainer();
                activityBox.addView(displayBoxContainer, srcLayoutParams);
            }
            //2.set target LayoutParams
            final FrameLayout.LayoutParams targetLayoutParams = new FrameLayout.LayoutParams(container.getLayoutParams());
            container.getLocationInWindow(targetXY);
            targetLayoutParams.leftMargin = targetXY[0] - activityBoxXY[0];
            targetLayoutParams.topMargin = targetXY[1] - activityBoxXY[1];

            final Transition transition = new ChangeBounds();
            transition.setStartDelay(200);
            transition.addListener(new Transition.TransitionListener() {
                private void afterTransition() {
                    //fire listener
                    if (displayBoxContainer.getParent() != container) {
                        isolateDisplayBoxContainer();
                        container.addView(displayBoxContainer, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                    }
                    listener.onEnd(displayBoxContainer, container);
                }
                @Override
                public void onTransitionStart(Transition transition) { }
                @Override
                public void onTransitionEnd(Transition transition) {
                    afterTransition();
                }
                @Override
                public void onTransitionCancel(Transition transition) {
                    afterTransition();
                }
                @Override
                public void onTransitionPause(Transition transition) { }
                @Override
                public void onTransitionResume(Transition transition) { }
            });
            //must put the action to queue so the beginDelayedTransition can take effect
            mUiHandler.post(()-> {
                listener.onStart(displayBoxContainer, container);
                TransitionManager.beginDelayedTransition(displayBoxContainer, transition);
                displayBoxContainer.setLayoutParams(targetLayoutParams);
            });
        });
    }

    private ViewGroup createFloatBox(float defaultAspectRatio) {
        mFloatWindowsManager.removeFloatContainer();
        return mFloatWindowsManager.getFloatVideoBox(defaultAspectRatio);
    }

    private boolean usingAnim() {
        return mVideoInfo.isFullScreenAnimation() && !mVideoInfo.isPortraitWhenFullScreen();
    }

    public VideoInfo getVideoInfo() {return mVideoInfo;}

    Activity getActivity() {
        VideoView videoView = PlayerManager.getInstance().getVideoView(mVideoInfo);
        if (videoView != null) {
            return (Activity) videoView.getContext();
        }
        return null;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        log("onConfigurationChanged");
        if (mIgnoreOrientation == newConfig.orientation) {
            log("onConfigurationChanged ignore");
            return;
        }
        if (mVideoInfo.isPortraitWhenFullScreen()) {
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                setDisplayModel(mLastDisplayModel);
            } else {
                setDisplayModel(DISPLAY_FULL_WINDOW);
            }
        }
    }

    public boolean onBackPressed() {
        if (mVideoInfo.isFullScreenOnly()) {
            return false;
        }
        if (mDisplayModel == DISPLAY_FULL_WINDOW) {
            setDisplayModel(mLastDisplayModel);
            return true;
        }
        return false;
    }

    public void onActivityResumed() {
        if (mTargetState == STATE_PLAYING) {
            start();
        } else if (mTargetState == STATE_PAUSED) {
            if (mCanSeekForward && mStartPosition >= 0) {
                seekTo(mStartPosition);
            }
        }
    }

    public void onActivityPaused() {
        if (mMediaPlayer == null) { return;}
        if (mTargetState == STATE_PLAYING
                || mCurrentState == STATE_PLAYING
                || mTargetState == STATE_PAUSED
                || mCurrentState == STATE_PAUSED) {
            mStartPosition = (int) mMediaPlayer.getCurrentPosition();
            releaseMediaPlayer();
        }
    }

    public void onActivityDestroyed() { release();}

    public void stop() { release();}

    public static void play(Context context, VideoInfo videoInfo) {
        Intent intent = new Intent(context, PlayActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra("__video_info__", videoInfo);
        PlayerManager.getInstance().releaseCurrent();
        context.startActivity(intent);
    }

    public void aspectRatio(int aspectRatio) {
        log("aspectRatio:" + aspectRatio);
        if(aspectRatio < 0) return;
        mVideoInfo.setAspectRatio(aspectRatio);
        IScalableDisplay display = getCurrentDisplay();
        if (display != null) {
            display.setAspectRatio(aspectRatio);
        }
    }

    public ITrackInfo[] getTrackInfo() {
        if (mMediaPlayer == null || mIsReleased) {
            return new ITrackInfo[0];
        }
        return mMediaPlayer.getTrackInfo();
    }

    public int getSelectedTrack(int trackType) {
        if (mMediaPlayer == null || mIsReleased) {
            return -1;
        }
        if (mMediaPlayer instanceof IjkMediaPlayer) {
            return ((IjkMediaPlayer) mMediaPlayer).getSelectedTrack(trackType);
        } else if (mMediaPlayer instanceof AndroidMediaPlayer) {
            return ((AndroidMediaPlayer) mMediaPlayer).getInternalMediaPlayer().getSelectedTrack(trackType);
        }
        return -1;
    }

    public void selectTrack(int track) {
        if (mMediaPlayer == null || mIsReleased) {
            return;
        }
        mPlayHandler.removeMessages(MSG_CTRL_SELECT_TRACK);
        mPlayHandler.obtainMessage(MSG_CTRL_SELECT_TRACK, track).sendToTarget();
    }

    public void deselectTrack(int selectedTrack) {
        if (mMediaPlayer == null || mIsReleased) {
            return;
        }
        mPlayHandler.removeMessages(MSG_CTRL_DESELECT_TRACK);
        mPlayHandler.obtainMessage(MSG_CTRL_DESELECT_TRACK, selectedTrack).sendToTarget();
    }

    public int getCurrentState() { return mCurrentState; }

    private void initBackground() {
        boolean mEnableBackgroundPlay = mSettings.getEnableBackgroundPlay();
        if (mEnableBackgroundPlay) {
            MediaPlayerService.intentToStart(mContext);
        }
    }

    public boolean isBackgroundPlayEnabled() {
        return mSettings.getEnableBackgroundPlay();
    }

    public void enterBackground() {
        MediaPlayerService.setMediaPlayer(mMediaPlayer);
    }

    public void stopBackgroundPlay() {
        MediaPlayerService.setMediaPlayer(null);
    }

    public void setSpeed(float speed) {
        if(mMediaPlayer instanceof IjkMediaPlayer)
            ((IjkMediaPlayer)mMediaPlayer).setSpeed(speed);
    }

    public float getSpeed() {
        if(mMediaPlayer instanceof IjkMediaPlayer)
            return ((IjkMediaPlayer)mMediaPlayer).getSpeed(1);
        return 1;
    }

    public void setRotation(int rotation){
    ScalableTextureView currentDisplay = getCurrentDisplay();
        if (currentDisplay != null) {
            currentDisplay.setRotation(rotation);
        }
    }

    //public void setPlayerListener(IPlayerListener playerListener) { this.mProxyListener.setOuterListener(playerListener); }

    public FloatWindowsManager getFloatWindowsManager(){ return mFloatWindowsManager; }
}