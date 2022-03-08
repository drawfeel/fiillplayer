package com.fiill.fiillplayer.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fiill.fiillplayer.R;
import com.fiill.fiillplayer.controller.FiillPlayer;
import com.fiill.fiillplayer.controller.VideoInfo;
import com.fiill.fiillplayer.controller.PlayerManager;
import com.fiill.fiillplayer.controller.VideoView;

//import tv.danmaku.ijk.media.player.IjkMediaPlayer;


/**
 * This activity is  a container for fiillplayer
 */

public class PlayActivity extends AppCompatActivity {

    FiillPlayer mPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fiill_player_activity);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        //IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        //hide virtual bottom keys
        Window _window = getWindow();
        WindowManager.LayoutParams params = _window.getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        _window.setAttributes(params);

        VideoInfo videoInfo = intent.getParcelableExtra("__video_info__");
        if (videoInfo == null) {
            finish();
            return;
        }
        if (videoInfo.isFullScreenOnly()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        PlayerManager.getInstance().releaseByFingerprint(videoInfo.getFingerprint());
        VideoView videoView = findViewById(R.id.video_view);

        videoView.videoInfo(videoInfo);
        mPlayer = PlayerManager.getInstance().getPlayer(videoView);
        mPlayer.start();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        PlayerManager.getInstance().onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (PlayerManager.getInstance().onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPlayer.getFloatWindowsManager().isFloating()) {
            mPlayer.getFloatWindowsManager().removeFloatView();
            mPlayer.setDisplayModel(FiillPlayer.DISPLAY_FULL_WINDOW);
        }
    }
    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //IjkMediaPlayer.native_profileEnd();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10086) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Request permission failed!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission granted. Retry again!", Toast.LENGTH_SHORT).show();
            }

        }
    }
}
