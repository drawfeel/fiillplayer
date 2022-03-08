package com.fiill.fiillplayer.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.fiill.fiillplayer.R;

import java.util.Locale;

import com.fiill.fiillplayer.activities.PlayActivity;
import com.fiill.fiillplayer.application.FiillSettings;
import com.fiill.fiillplayer.fragments.AspectRatioFragment;
import com.fiill.fiillplayer.fragments.SpeedFragment;
import com.fiill.fiillplayer.fragments.TrackSelectorFragment;
import com.fiill.fiillplayer.widgets.ScalableTextureView;
import com.fiill.fiillplayer.widgets.ViewQuery;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;

/**
 * media controller for video view when float mode
 */

public class StreamMediaController implements IPlayerListener,Handler.Callback {

    protected static final int STATUS_ERROR = -1;
    //protected static final int STATUS_IDLE = 0;
    protected static final int STATUS_LOADING = 1;
    protected static final int STATUS_PLAYING = 2;
    protected static final int STATUS_PAUSE = 3;
    protected static final int STATUS_COMPLETED = 4;
    protected final boolean INSTANT_SEEKING = false;
    protected static final int MESSAGE_SHOW_PROGRESS = 1;
    protected static final int MESSAGE_FADE_OUT = 2;
    protected static final int MESSAGE_SEEK_NEW_POSITION = 3;
    protected static final int MESSAGE_HIDE_CENTER_BOX = 4;
    protected static final int MESSAGE_RESTART_PLAY = 5;
    protected final int DEFAULT_TIME_OUT = 3 * 1000;

    protected long mNewPosition = -1;

    protected boolean mIsShowing;
    protected boolean mIsDragging;
    protected SeekBar mSeekBar;
    protected int mVolume = -1;
    protected final int mMaxVolume;

    protected float mBrightness;
    private int mDisplayModel = FiillPlayer.DISPLAY_NORMAL;

    protected final Context mContext;
    protected final AudioManager mAudioManager;
    protected ViewQuery mViewQuery;

    protected Handler mHandler;
    protected VideoView mVideoView;
    protected View mControllerView;
    protected int mLastRotation;

    @Override
    public void bind(VideoView videoView) {
        this.mVideoView = videoView;
        mControllerView = makeControllerView();
        mViewQuery = new ViewQuery(mControllerView);
        initView(mControllerView);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.mVideoView.getContainer().addView(mControllerView, layoutParams);
    }

    private String generateTime(long time) {
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return hours > 0 ?
                String.format(Locale.CHINA,"%02d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.CHINA,"%02d:%02d", minutes, seconds);
    }

    protected final SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser)
                return;
            if (!mVideoView.isCurrentActivePlayer()) {
                return;
            }
            mViewQuery.id(R.id.app_video_status).gone();//hide image when moving
            FiillPlayer player = mVideoView.getPlayer();
            int newPosition = (int) (player.getDuration() * (progress * 1.0 / 1000));
            String time = generateTime(newPosition);
            if (INSTANT_SEEKING) {
                player.seekTo(newPosition);

            }
            mViewQuery.id(R.id.app_video_currentTime).text(time);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mIsDragging = true;
            show(3600000);
            mHandler.removeMessages(MESSAGE_SHOW_PROGRESS);
            if (INSTANT_SEEKING) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (!mVideoView.isCurrentActivePlayer()) {
                return;
            }
            FiillPlayer player = mVideoView.getPlayer();
            if (!INSTANT_SEEKING) {
                player.seekTo((int) (player.getDuration() * (seekBar.getProgress() * 1.0 / 1000)));
            }
            show(DEFAULT_TIME_OUT);
            mHandler.removeMessages(MESSAGE_SHOW_PROGRESS);
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            mIsDragging = false;
            mHandler.sendEmptyMessageDelayed(MESSAGE_SHOW_PROGRESS, 1000);
        }
    };

    protected void updatePausePlay() {
        if (mVideoView.isCurrentActivePlayer()) {
            boolean playing = mVideoView.getPlayer().isPlaying();
            if (playing) {
                mViewQuery.id(R.id.app_video_play).image(R.drawable.ic_stop_white_24dp);
            } else {
                mViewQuery.id(R.id.app_video_play).image(R.drawable.ic_play_arrow_white_24dp);
            }
        } else {
            mViewQuery.id(R.id.app_video_play).image(R.drawable.ic_play_arrow_white_24dp);
            mViewQuery.id(R.id.app_video_currentTime).text("");
            mViewQuery.id(R.id.app_video_endTime).text("");
        }
    }


    protected void setProgress() {
        if (mIsDragging) {
            return;
        }
        //check player is active
        boolean currentPlayer = mVideoView.isCurrentActivePlayer();
        if (!currentPlayer) {
            mSeekBar.setProgress(0);
            return;
        }

        //check player is ready
        FiillPlayer player = mVideoView.getPlayer();
        int currentState = player.getCurrentState();
        if (currentState == FiillPlayer.STATE_IDLE ||
                currentState == FiillPlayer.STATE_PREPARING ||
                currentState == FiillPlayer.STATE_ERROR) {
            return;
        }

        long position = player.getCurrentPosition();
        int duration = player.getDuration();

        if (mSeekBar != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                mSeekBar.setProgress((int) pos);
            }
            int percent = player.getBufferPercentage();
            mSeekBar.setSecondaryProgress(percent * 10);
        }

        mViewQuery.id(R.id.app_video_currentTime).text(generateTime(position));
        if (duration == 0) {//live stream
            mViewQuery.id(R.id.app_video_endTime).text(R.string.fiill_player_live);
        } else {
            mViewQuery.id(R.id.app_video_endTime).text(generateTime(duration));
        }
    }

    protected void show(int timeout) {
        if (!mIsShowing) {
            if (mVideoView.getVideoInfo().isShowTopBar() || mDisplayModel == FiillPlayer.DISPLAY_FULL_WINDOW) {
                mViewQuery.id(R.id.app_video_top_box).visible();
                mViewQuery.id(R.id.app_video_title).text(mVideoView.getVideoInfo().getTitle());
            } else {
                mViewQuery.id(R.id.app_video_top_box).gone();
            }
            showBottomControl(true);
            mIsShowing = true;
        }
        updatePausePlay();
        mHandler.sendEmptyMessage(MESSAGE_SHOW_PROGRESS);
        mHandler.removeMessages(MESSAGE_FADE_OUT);
        if (timeout != 0) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_FADE_OUT), timeout);
        }
    }


    protected void showBottomControl(boolean show) {
        if (mDisplayModel == FiillPlayer.DISPLAY_FLOAT) {
            show = false;
        }
        mViewQuery.id(R.id.app_video_bottom_box).visibility(show ? View.VISIBLE : View.GONE);
    }

    protected void hide(boolean force) {
        if (force || mIsShowing) {
            mHandler.removeMessages(MESSAGE_SHOW_PROGRESS);
            showBottomControl(false);
            mViewQuery.id(R.id.app_video_top_box).gone();
            mIsShowing = false;
        }
    }

    public StreamMediaController(Context ctx) {
        mContext = ctx;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler(Looper.getMainLooper(),this);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    protected View makeControllerView() {
        return LayoutInflater.from(mContext).inflate(R.layout.fiill_media_controller, mVideoView, false);
    }

    protected final View.OnClickListener mControllerOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FiillPlayer player;
            try {
                player = mVideoView.getPlayer();
            }catch (RuntimeException e) {
                return;
            }
            Activity activity = (Activity) mVideoView.getContext();
            if (v.getId() == R.id.app_video_fullscreen) {
                player.toggleFullScreen();
            } else if (v.getId() == R.id.app_video_play) {
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.start();
                }
            } else if (v.getId() == R.id.app_video_replay_icon) {
                player.seekTo(0);
                player.start();
            } else if (v.getId() == R.id.app_video_finish) {
                //if (!player.onBackPressed())
                    ((Activity) mVideoView.getContext()).finish();
            } else if (v.getId() == R.id.app_video_float_close) {
                player.setDisplayModel(FiillPlayer.DISPLAY_IDLE);
            } else if (v.getId() == R.id.app_video_float_full) {
                if(player.getFloatWindowsManager().isFloating()) {
                    player.getActivity().startActivity(new Intent(player.getActivity(),
                            PlayActivity.class));
                }
            } else if (v.getId() == R.id.app_video_clarity) {
                if (activity instanceof AppCompatActivity) {
                    TrackSelectorFragment trackSelectorFragment = new TrackSelectorFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("fingerprint", mVideoView.getVideoInfo().getFingerprint());
                    trackSelectorFragment.setArguments(bundle);
                    FragmentManager supportFragmentManager = ((AppCompatActivity) activity).getSupportFragmentManager();
                    trackSelectorFragment.show(supportFragmentManager, "player_track");
                }

            } else if (v.getId() == R.id.app_button_info) {
                mVideoView.showMediaInfo();
            } else if (v.getId() == R.id.app_button_ratio) {
                if (activity instanceof AppCompatActivity) {
                    AspectRatioFragment aspectRatioFragment = new AspectRatioFragment(
                            mVideoView.getPlayer(), "Aspect Ratio");
                    Bundle bundle = new Bundle();
                    String[] aspectRatios = {
                            "4_3_FIT_PARENT",
                            "16_9_FIT_PARENT",
                            //"ASPECT_FILL_PARENT", "ASPECT_WRAP_CONTENT", "ASPECT_MATCH_PARENT",
                            "ASPECT_FIT_PARENT",
                            "3_4_FIT_PARENT",
                            "9_16_FIT_PARENT",
                    };
                    bundle.putStringArray("DATA", aspectRatios);
                    aspectRatioFragment.setArguments(bundle);
                    FragmentManager supportFragmentManager = ((AppCompatActivity) activity).getSupportFragmentManager();
                    aspectRatioFragment.show(supportFragmentManager, "aspect_ratio");
                }
            } else if (v.getId() == R.id.app_button_float) {
                if (!Settings.canDrawOverlays(activity)) {
                    Toast.makeText(activity, "Request overlay permission", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, 10086);
                } else {
                    mVideoView.getPlayer().setDisplayModel(FiillPlayer.DISPLAY_FLOAT);
                }
            } else if (v.getId() == R.id.app_button_speed) {

                if (activity instanceof AppCompatActivity) {
                    SpeedFragment speedFragment = new SpeedFragment(
                            mVideoView.getPlayer(), "Play Speed");
                    Bundle bundle = new Bundle();
                    String[] aspectRatios = {
                            "0.25x",
                            "0.5x",
                            "1x",
                            "2x",
                            "4x",
                            "8x",
                    };
                    bundle.putStringArray("DATA", aspectRatios);
                    speedFragment.setArguments(bundle);
                    FragmentManager supportFragmentManager = ((AppCompatActivity) activity).getSupportFragmentManager();
                    speedFragment.show(supportFragmentManager, "play_speed");
                }
            } else if (v.getId() == R.id.app_button_rotation) {
                mLastRotation += 90;
                mLastRotation %= 360;
                player.setRotation(mLastRotation);
            }

        }
    };



    protected void initView(View view) {
        mSeekBar = mViewQuery.id(R.id.app_video_seekBar).view();
        mSeekBar.setMax(1000);
        mSeekBar.setOnSeekBarChangeListener(seekListener);
        mViewQuery.id(R.id.app_video_play).clicked(mControllerOnClickListener).imageView().setRotation(isRtl()?180:0);
        mViewQuery.id(R.id.app_video_fullscreen).clicked(mControllerOnClickListener);
        mViewQuery.id(R.id.app_video_finish).clicked(mControllerOnClickListener).imageView().setRotation(isRtl()?180:0);
        mViewQuery.id(R.id.app_video_replay_icon).clicked(mControllerOnClickListener).imageView().setRotation(isRtl()?180:0);
        mViewQuery.id(R.id.app_video_clarity).clicked(mControllerOnClickListener);
        mViewQuery.id(R.id.app_video_float_close).clicked(mControllerOnClickListener);
        mViewQuery.id(R.id.app_video_float_full).clicked(mControllerOnClickListener);

        mViewQuery.id(R.id.app_button_info).clicked(mControllerOnClickListener);
        mViewQuery.id(R.id.app_button_ratio).clicked(mControllerOnClickListener);
        mViewQuery.id(R.id.app_button_float).clicked(mControllerOnClickListener);
        mViewQuery.id(R.id.app_button_rotation).clicked(mControllerOnClickListener);

        FiillSettings settings = new FiillSettings(view.getContext());
        if(settings.getPreferredPlayerType() == FiillSettings.PREFER_PLAYER_ANDROID_MEDIA_PLAYER) {
            mViewQuery.id(R.id.app_button_speed).invisible();
        } else {
            mViewQuery.id(R.id.app_button_speed).clicked(mControllerOnClickListener);
        }

        final GestureDetector gestureDetector = new GestureDetector(mContext, createGestureListener());
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnTouchListener((v, event) -> {

            if (mDisplayModel == FiillPlayer.DISPLAY_FLOAT) {
                return false;
            }

            if (gestureDetector.onTouchEvent(event)) {
                v.performClick();
                return true;
            }

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                    endGesture();
                    break;
            }
            v.performClick();
            return true;
        });
    }

    protected GestureDetector.OnGestureListener createGestureListener() {
        return new PlayerGestureListener();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_FADE_OUT:
                hide(false);
                break;
            case MESSAGE_HIDE_CENTER_BOX:
                mViewQuery.id(R.id.app_video_volume_box).gone();
                mViewQuery.id(R.id.app_video_brightness_box).gone();
                mViewQuery.id(R.id.app_video_fastForward_box).gone();
                break;
            case MESSAGE_SEEK_NEW_POSITION:
                if (mNewPosition >= 0) {
                    mVideoView.getPlayer().seekTo((int) mNewPosition);
                    mNewPosition = -1;
                }
                break;
            case MESSAGE_SHOW_PROGRESS:
                setProgress();
                if (!mIsDragging && mIsShowing) {
                    msg = mHandler.obtainMessage(MESSAGE_SHOW_PROGRESS);
                    mHandler.sendMessageDelayed(msg, 300);
                    updatePausePlay();
                }
                break;
            case MESSAGE_RESTART_PLAY:
//                        play(url);
                break;
        }
        return true;
    }

    @Override
    public void onCompletion(FiillPlayer fiillPlayer) {
        statusChange(STATUS_COMPLETED);
    }

    @Override
    public void onRelease(FiillPlayer fiillplayer) {
        mHandler.removeCallbacksAndMessages(null);

        mViewQuery.id(R.id.app_video_play).image(R.drawable.ic_play_arrow_white_24dp);
        mViewQuery.id(R.id.app_video_currentTime).text("");
        mViewQuery.id(R.id.app_video_endTime).text("");

        //1.set the cover view visible
        mViewQuery.id(R.id.app_video_cover).visible();
        //2.set current view as cover
        VideoInfo videoInfo = mVideoView.getVideoInfo();
        if (videoInfo.isCurrentVideoAsCover()) {
            if (fiillplayer.getCurrentState() != FiillPlayer.STATE_ERROR) {
                ScalableTextureView currentDisplay = fiillplayer.getCurrentDisplay();
                if (currentDisplay != null) {
                    ImageView imageView = mViewQuery.id(R.id.app_video_cover).imageView();
                    if (imageView != null) {
                        int aspectRatio = videoInfo.getAspectRatio();
                        if (aspectRatio == VideoInfo.AR_ASPECT_FILL_PARENT) {
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        } else if (aspectRatio == VideoInfo.AR_MATCH_PARENT) {
                            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                        } else if (aspectRatio == VideoInfo.AR_ASPECT_WRAP_CONTENT) {
                            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        } else {
                            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        }
                        imageView.setImageBitmap(currentDisplay.getBitmap());
                    }
                }
            }
        }

    }

    @Override
    public void onStart(FiillPlayer fiillPlayer) {
        mViewQuery.id(R.id.app_video_replay).gone();
        show(DEFAULT_TIME_OUT);
        statusChange(STATUS_PLAYING);
    }


    protected void endGesture() {
        mVolume = -1;
        mBrightness = -1f;
        if (mNewPosition >= 0) {
            mHandler.removeMessages(MESSAGE_SEEK_NEW_POSITION);
            mHandler.sendEmptyMessage(MESSAGE_SEEK_NEW_POSITION);
        }
        mHandler.removeMessages(MESSAGE_HIDE_CENTER_BOX);
        mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_CENTER_BOX, 500);
    }

    public class PlayerGestureListener extends GestureDetector.SimpleOnGestureListener {
        private boolean firstTouch;
        private boolean volumeControl;
        private boolean toSeek;

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Toast.makeText(context, "onDoubleTap", Toast.LENGTH_SHORT).show();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            firstTouch = true;
            return true;

        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //1. if not the active player,ignore
            boolean currentPlayer = mVideoView.isCurrentActivePlayer();
            if (!currentPlayer) {
                return true;
            }

            float oldX = e1.getX(), oldY = e1.getY();
            float deltaY = oldY - e2.getY();
            float deltaX = oldX - e2.getX();
            if (firstTouch) {
                toSeek = Math.abs(distanceX) >= Math.abs(distanceY);
                volumeControl = oldX > mVideoView.getWidth() * 0.5f;
                firstTouch = false;
            }
            FiillPlayer player = mVideoView.getPlayer();
            if (toSeek) {
                if (player.canSeekForward()) {
                    onProgressSlide(-deltaX / mVideoView.getWidth());
                }
            } else {
                //if player in list controllerView,ignore
                if (mDisplayModel == FiillPlayer.DISPLAY_NORMAL && mVideoView.inListView()) {
                    return true;
                }
                float percent = deltaY / mVideoView.getHeight();
                if (volumeControl) {
                    onVolumeSlide(percent);
                } else {
                    onBrightnessSlide(percent);
                }
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mIsShowing) {
                hide(false);
            } else {
                show(DEFAULT_TIME_OUT);
            }
            return true;
        }
    }

    private void onVolumeSlide(float percent) {
        if (mVolume == -1) {
            mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (mVolume < 0)
                mVolume = 0;
        }
        hide(true);

        int index = (int) (percent * mMaxVolume) + mVolume;
        if (index > mMaxVolume)
            index = mMaxVolume;
        else if (index < 0)
            index = 0;

        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

        int i = (int) (index * 1.0 / mMaxVolume * 100);
        String s = i + "%";
        if (i == 0) {
            s = "off";
        }

        mViewQuery.id(R.id.app_video_volume_icon).image(i == 0 ? R.drawable.ic_volume_off_white_36dp : R.drawable.ic_volume_up_white_36dp);
        mViewQuery.id(R.id.app_video_brightness_box).gone();
        mViewQuery.id(R.id.app_video_volume_box).visible();
        mViewQuery.id(R.id.app_video_volume_box).visible();
        mViewQuery.id(R.id.app_video_volume).text(s).visible();
    }

    private void onProgressSlide(float percent) {
        FiillPlayer player = mVideoView.getPlayer();
        long position = player.getCurrentPosition();
        long duration = player.getDuration();
        long deltaMax = Math.min(100 * 1000, duration - position);
        long delta = (long) (deltaMax * percent);
        if (isRtl()) {
            delta = -1 * delta;
        }

        mNewPosition = delta + position;
        if (mNewPosition > duration) {
            mNewPosition = duration;
        } else if (mNewPosition <= 0) {
            mNewPosition = 0;
            delta = -position;
        }
        int showDelta = (int) delta / 1000;
        if (showDelta != 0) {
            mViewQuery.id(R.id.app_video_fastForward_box).visible();
            String text = showDelta > 0 ? ("+" + showDelta) : "" + showDelta;
            mViewQuery.id(R.id.app_video_fastForward).text(text + "s");
            mViewQuery.id(R.id.app_video_fastForward_target).text(generateTime(mNewPosition) + "/");
            mViewQuery.id(R.id.app_video_fastForward_all).text(generateTime(duration));
        }
        mHandler.sendEmptyMessage(MESSAGE_SEEK_NEW_POSITION);
    }

    private void onBrightnessSlide(float percent) {
        Window window = ((Activity) mContext).getWindow();
        if (mBrightness < 0) {
            mBrightness = window.getAttributes().screenBrightness;
            if (mBrightness <= 0.00f) {
                mBrightness = 0.50f;
            } else if (mBrightness < 0.01f) {
                mBrightness = 0.01f;
            }
        }
        Log.d(this.getClass().getSimpleName(), "brightness:" + mBrightness + ",percent:" + percent);
        mViewQuery.id(R.id.app_video_brightness_box).visible();
        WindowManager.LayoutParams lpa = window.getAttributes();
        lpa.screenBrightness = mBrightness + percent;
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f;
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f;
        }
        mViewQuery.id(R.id.app_video_brightness).text(((int) (lpa.screenBrightness * 100)) + "%");
        window.setAttributes(lpa);

    }

    @Override
    public boolean onInfo(FiillPlayer fiillplayer, int what, int extra) {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                statusChange(STATUS_LOADING);
                break;
            case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                //Toaster.show("download rate:" + extra);
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                statusChange(STATUS_PLAYING);
                break;

            default:
        }

        return true;
    }

    @Override
    public void onCurrentStateChange(int oldState, int newState) {
        if (mContext instanceof Activity) {
            if (newState == FiillPlayer.STATE_PLAYING) {
                //set SCREEN_ON
                ((Activity) mContext).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                ((Activity) mContext).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    protected void statusChange(int status) {
        //this.status = status;
        switch (status) {
            case STATUS_LOADING:
                mViewQuery.id(R.id.app_video_loading).visible();
                mViewQuery.id(R.id.app_video_status).gone();
                break;
            case STATUS_PLAYING:
                mViewQuery.id(R.id.app_video_loading).gone();
                mViewQuery.id(R.id.app_video_status).gone();
                break;
            case STATUS_COMPLETED:
                mHandler.removeMessages(MESSAGE_SHOW_PROGRESS);
                showBottomControl(false);
                mViewQuery.id(R.id.app_video_replay).visible();
                mViewQuery.id(R.id.app_video_loading).gone();
                mViewQuery.id(R.id.app_video_status).gone();
                break;
            case STATUS_ERROR:
                mViewQuery.id(R.id.app_video_status).visible().id(R.id.app_video_status_text).text(R.string.small_problem);
                mHandler.removeMessages(MESSAGE_SHOW_PROGRESS);
                mViewQuery.id(R.id.app_video_loading).gone();
                break;
            default:
        }
    }

    @Override
    public boolean onError(FiillPlayer fiillplayer, int what, int extra) {
        statusChange(STATUS_ERROR);
        return true;
    }

    @Override
    public void onPrepared(FiillPlayer fiillplayer) {
        boolean live = fiillplayer.getDuration() == 0;
        mViewQuery.id(R.id.app_video_seekBar).enabled(!live);
        if (fiillplayer.getTrackInfo().length > 0) {
            mViewQuery.id(R.id.app_video_clarity).visible();
        } else {
            mViewQuery.id(R.id.app_video_clarity).gone();
        }
    }

    @Override
    public void onPreparing(FiillPlayer fiillPlayer) {
        statusChange(STATUS_LOADING);
    }

    @Override
    public void onDisplayModelChange(int oldModel, int newModel) {
        this.mDisplayModel = newModel;
        if (mDisplayModel == FiillPlayer.DISPLAY_FLOAT) {
            mViewQuery.id(R.id.app_video_float_close).visible();
            mViewQuery.id(R.id.app_video_float_full).visible();
            mViewQuery.id(R.id.app_video_bottom_box).gone();

        } else {
            mViewQuery.id(R.id.app_video_float_close).gone();
            mViewQuery.id(R.id.app_video_float_full).gone();
            mViewQuery.id(R.id.app_video_bottom_box).visible();

        }
    }

    @Override
    public void onTargetStateChange(int oldState, int newState) {
        if (newState != FiillPlayer.STATE_IDLE) {
            mViewQuery.id(R.id.app_video_cover).gone();
        }
    }


    @Override
    public void onTimedText(FiillPlayer fiillPlayer, IjkTimedText text) {
        if (text == null) {
            mViewQuery.id(R.id.app_video_subtitle).gone();
        } else {
            mViewQuery.id(R.id.app_video_subtitle).visible().text(text.getText());
        }
    }

    @Override
    public void onPause(FiillPlayer fiillPlayer) {
        statusChange(STATUS_PAUSE);
    }

    private boolean isRtl() {
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;
    }

    @Override
    public void onBufferingUpdate(FiillPlayer fiillplayer, int percent) {
    }

    @Override
    public void onSeekComplete(FiillPlayer fiillplayer) {

    }
}
