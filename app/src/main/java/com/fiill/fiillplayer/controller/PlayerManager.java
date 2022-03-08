package com.fiill.fiillplayer.controller;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    public static final String TAG = "FiillPlayerManager";
    private volatile String mCurrentPlayerFingerprint;
    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks;
    private WeakReference<Activity> mTopActivityRef;

    /**
     * default config for all
     */
    private final VideoInfo mDefaultVideoInfo = new VideoInfo();

    public VideoInfo getDefaultVideoInfo() {
        return mDefaultVideoInfo;
    }

    public PlayerManager.MediaControllerGenerator getMediaControllerGenerator() {
        return MediaControllerGenerator;
    }

    //reserved. for setting a new controller with new views & style
    public void setMediaControllerGenerator(PlayerManager.MediaControllerGenerator mediaControllerGenerator) {
        MediaControllerGenerator = mediaControllerGenerator;
    }

    private MediaControllerGenerator MediaControllerGenerator = (context, videoInfo) -> new StreamMediaController(context);

    private final WeakHashMap<String, VideoView> videoViewsRef = new WeakHashMap<>();
    private final Map<String, FiillPlayer> playersRef = new ConcurrentHashMap<>();
    private final WeakHashMap<Context, String> activity2playersRef = new WeakHashMap<>();


    private static final PlayerManager instance = new PlayerManager();


    public static PlayerManager getInstance() {
        return instance;
    }

    public FiillPlayer getCurrentPlayer() {
        return mCurrentPlayerFingerprint == null ? null : playersRef.get(mCurrentPlayerFingerprint);
    }

    private FiillPlayer createPlayer(VideoView videoView) {
        VideoInfo videoInfo = videoView.getVideoInfo();
        log(videoInfo.getFingerprint(), "createPlayer");
        videoViewsRef.put(videoInfo.getFingerprint(), videoView);
        registerActivityLifecycleCallbacks(((Activity) videoView.getContext()).getApplication());
        FiillPlayer player = FiillPlayer.createPlayer(videoView.getContext(), videoInfo);
        playersRef.put(videoInfo.getFingerprint(), player);
        activity2playersRef.put(videoView.getContext(), videoInfo.getFingerprint());
        if (mTopActivityRef == null || mTopActivityRef.get() == null) {
            mTopActivityRef = new WeakReference<>((Activity) videoView.getContext());
        }
        return player;
    }

    private synchronized void registerActivityLifecycleCallbacks(Application context) {
        if (mActivityLifecycleCallbacks != null) {
            return;
        }
        mActivityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
//                System.out.println("======onActivityStarted============"+activity);

            }

            @Override
            public void onActivityResumed(Activity activity) {
                FiillPlayer currentPlayer = getPlayerByFingerprint(activity2playersRef.get(activity));
                if (currentPlayer != null) {
                    currentPlayer.onActivityResumed();
                }
                mTopActivityRef = new WeakReference<>(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                FiillPlayer currentPlayer = getPlayerByFingerprint(activity2playersRef.get(activity));
                if (currentPlayer == null || currentPlayer.isBackgroundPlayEnabled())return;
                if(currentPlayer.getFloatWindowsManager().isFloating()) return;
                currentPlayer.onActivityPaused();
                if (mTopActivityRef != null && mTopActivityRef.get() == activity) {
                    mTopActivityRef.clear();
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                FiillPlayer currentPlayer = getPlayerByFingerprint(activity2playersRef.get(activity));
                if(currentPlayer == null || getCurrentPlayer().getFloatWindowsManager().isFloating()) return;
                if ( !currentPlayer.isBackgroundPlayEnabled()) {
                    currentPlayer.release(true);
                    currentPlayer.stopBackgroundPlay();
                } else {
                    currentPlayer.enterBackground();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                FiillPlayer currentPlayer = getPlayerByFingerprint(activity2playersRef.get(activity));
                if (currentPlayer != null) {
                    currentPlayer.onActivityDestroyed();
                }
                activity2playersRef.remove(activity);
            }
        };
        context.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }

    public void releaseCurrent() {
        log(mCurrentPlayerFingerprint, "releaseCurrent");
        FiillPlayer currentPlayer = getCurrentPlayer();
        if (currentPlayer != null) {
            if (currentPlayer.getProxyPlayerListener() != null) {
                currentPlayer.getProxyPlayerListener().onCompletion(currentPlayer);
            }
            currentPlayer.release();
        }
        mCurrentPlayerFingerprint = null;
    }


    public boolean isCurrentPlayer(String fingerprint) {
        return fingerprint != null && fingerprint.equals(this.mCurrentPlayerFingerprint);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        FiillPlayer currentPlayer = getCurrentPlayer();
        if (currentPlayer != null) {
            currentPlayer.onConfigurationChanged(newConfig);
        }
    }

    public boolean onBackPressed() {
        FiillPlayer currentPlayer = getCurrentPlayer();
        if (currentPlayer != null) {
            return currentPlayer.onBackPressed();
        }
        return false;
    }

    public VideoView getVideoView(VideoInfo videoInfo) {
        return videoViewsRef.get(videoInfo.getFingerprint());
    }


    public void setCurrentPlayer(FiillPlayer fiillPlayer) {
        VideoInfo videoInfo = fiillPlayer.getVideoInfo();
        log(videoInfo.getFingerprint(), "setCurrentPlayer");

        //if choose a new playerRef
        String fingerprint = videoInfo.getFingerprint();
        if (!isCurrentPlayer(fingerprint)) {
            try {
                log(videoInfo.getFingerprint(), "not same release before one:" + mCurrentPlayerFingerprint);
                releaseCurrent();
                mCurrentPlayerFingerprint = fingerprint;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            log(videoInfo.getFingerprint(), "is currentPlayer");
        }
    }

    public FiillPlayer getPlayer(VideoView videoView) {
        VideoInfo videoInfo = videoView.getVideoInfo();
        FiillPlayer player = playersRef.get(videoInfo.getFingerprint());
        if (player == null) {
            player = createPlayer(videoView);
        }
        return player;
    }

    public FiillPlayer getPlayerByFingerprint(String fingerprint) {
        if (fingerprint == null) {
            return null;
        }
        return playersRef.get(fingerprint);
    }

    public void releaseByFingerprint(String fingerprint) {
        FiillPlayer player = playersRef.get(fingerprint);
        if (player != null) {
            player.release();
        }
    }

    public void removePlayer(String fingerprint) {
        playersRef.remove(fingerprint);
    }

    private void log(String fingerprint, String msg) {
        if (FiillPlayer.debug) {
            Log.d(TAG, String.format("[setFingerprint:%s] %s", fingerprint, msg));
        }
    }

    /**
     * to create a custom MediaController
     */
    public interface MediaControllerGenerator {
        /**
         * called when VideoView need a MediaController
         * @param context activity context
         * @param videoInfo video information, options
         * @return A playerListener/controller instance
         */
        IPlayerListener create(Context context, VideoInfo videoInfo);
    }
}
