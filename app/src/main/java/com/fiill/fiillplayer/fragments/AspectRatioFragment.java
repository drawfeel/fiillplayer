package com.fiill.fiillplayer.fragments;

import android.view.View;
import android.widget.TextView;

import com.fiill.fiillplayer.controller.FiillPlayer;
import com.fiill.fiillplayer.controller.VideoInfo;

public class AspectRatioFragment extends SelectDialogFragment {
    FiillPlayer mPlayer;

    public AspectRatioFragment(FiillPlayer player, String dialogName) {
        super(dialogName);
        mPlayer = player;
    }

    int getAspectRatio(String c) {
        int aspectRatio = -1;
        if (c.endsWith("4_3_FIT_PARENT")) {
            aspectRatio = VideoInfo.AR_4_3_FIT_PARENT;
        } else if (c.endsWith("16_9_FIT_PARENT")) {
            aspectRatio = VideoInfo.AR_16_9_FIT_PARENT;
        } else if (c.endsWith("ASPECT_FILL_PARENT")) {
            aspectRatio = VideoInfo.AR_ASPECT_FILL_PARENT;
        } else if (c.endsWith("ASPECT_WRAP_CONTENT")) {
            aspectRatio = VideoInfo.AR_ASPECT_WRAP_CONTENT;
        } else if (c.endsWith("ASPECT_MATCH_PARENT")) {
            aspectRatio = VideoInfo.AR_MATCH_PARENT;
        } else if(c.endsWith("ASPECT_FIT_PARENT")) {
            aspectRatio = VideoInfo.AR_ASPECT_FIT_PARENT;
        } else if(c.endsWith("3_4_FIT_PARENT")) {
            aspectRatio = VideoInfo.AR_3_4_FIT_PARENT;
        } else if(c.endsWith("9_16_FIT_PARENT")) {
            aspectRatio = VideoInfo.AR_9_16_FIT_PARENT;
        }
        return aspectRatio;
    }

    View.OnClickListener setItemOnClickListener() {
        return view -> {
            int aspectRatio = getAspectRatio(((TextView)view).getText().toString());
            try {
                mPlayer.aspectRatio(aspectRatio);
            }catch (RuntimeException ignored) {
            } finally {
                dismissAllowingStateLoss();
            }
        };
    }

    @Override
    boolean isDefaultItem(String item) {
        return (mPlayer.getVideoInfo().getAspectRatio() == getAspectRatio(item));
    }
}
