package com.fiill.fiillplayer.widgets;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.fiill.fiillplayer.R;
import com.fiill.fiillplayer.utils.Utils;

public class FloatWindowsManager {
    WindowManager.LayoutParams mLayoutParams = new WindowManager.LayoutParams();
    WindowManager mWindowManager;
    private final ViewGroup mFloatBox;
    float mDefaultAspectRatio;
    boolean isFloating;
    private final Activity mActivity;
    public static int sFloatBox_Last_X = Integer.MAX_VALUE; //max_value means unset
    public static int sFloatBox_Last_Y = Integer.MAX_VALUE;


    public  FloatWindowsManager (Activity activity){
        mActivity = activity;
        mWindowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        mFloatBox = (ViewGroup) LayoutInflater.from(
                activity.getApplication()).inflate(R.layout.fiill_float_box, null);
    }
    public  ViewGroup getFloatVideoBox(float aspectRatio) {
        fillFloatVideoBox(aspectRatio);
        isFloating = true;
        mWindowManager.addView(mFloatBox, mLayoutParams);
        return mFloatBox;
    }

    public void removeFloatView(){
        if(mFloatBox != null && isFloating) mWindowManager.removeView(mFloatBox);
        isFloating = false;
    }

    public boolean isFloating(){
        return isFloating;
    }

    void fillFloatVideoBox(float aspectRatio) {
        if (aspectRatio < 0.2 || aspectRatio > 5) return;
        mDefaultAspectRatio = aspectRatio;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                             WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        DisplayMetrics displayMetrics=new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int widthPixels = Math.min(displayMetrics.heightPixels, displayMetrics.widthPixels);
        int heightPixels =  (int)(widthPixels * aspectRatio);

        mLayoutParams.width = widthPixels;
        mLayoutParams.height = heightPixels;

        if (sFloatBox_Last_X == Integer.MAX_VALUE || sFloatBox_Last_Y == Integer.MAX_VALUE) {
            mLayoutParams.gravity = Gravity.TOP | Gravity.CENTER;
        } else {
            //set the float view at the last position
            mLayoutParams.gravity = Gravity.TOP;
            mLayoutParams.x = sFloatBox_Last_X;
            mLayoutParams.y = sFloatBox_Last_Y;
        }
        mFloatBox.setOnTouchListener(new FloatingOnTouchListener(mLayoutParams,
                                        mDefaultAspectRatio, mWindowManager));
    }

    public void removeFloatContainer() {
        if (mActivity != null) {
            View floatBox = mActivity.findViewById(R.id.player_display_float_box);
            if (floatBox != null) {
                FloatWindowsManager.sFloatBox_Last_X = (int)floatBox.getX();
                FloatWindowsManager.sFloatBox_Last_Y = (int)floatBox.getY();
            }
            Utils.removeFromParent(floatBox);
        }
    }
}
