package com.fiill.fiillplayer.fragments;

import android.view.View;
import android.widget.TextView;

import com.fiill.fiillplayer.controller.FiillPlayer;

public class SpeedFragment extends SelectDialogFragment {
    FiillPlayer mPlayer;

    public SpeedFragment(FiillPlayer player, String dialogName) {
        super(dialogName);
        mPlayer = player;
    }

    float getSpeedValue(String speedString) {
        float speed = -1;
        if (speedString.startsWith("0.25")) {
            speed = 0.25f;
        } else if (speedString.startsWith("0.5")) {
            speed = 0.5f;
        } else if (speedString.startsWith("1")) {
            speed = 1;
        } else if (speedString.startsWith("2")) {
            speed = 2;
        } else if (speedString.startsWith("4")) {
            speed = 4;
        } else if(speedString.startsWith("8")) {
            speed = 8;
        }
        return speed;
    }

    @Override
    View.OnClickListener setItemOnClickListener() {
        return view -> {
            float speed = getSpeedValue(((TextView)view).getText().toString());
            if (speed < 0) return;
            try {
                mPlayer.setSpeed(speed);
            }catch (RuntimeException ignored) {
            } finally {
                dismissAllowingStateLoss();
            }
        };
    }

    @Override
    boolean isDefaultItem(String item) {
        float abs = Math.abs(mPlayer.getSpeed() - getSpeedValue(item));
        return (abs < 0.001);
    }
}
