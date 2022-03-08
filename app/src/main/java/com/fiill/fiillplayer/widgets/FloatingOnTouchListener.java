package com.fiill.fiillplayer.widgets;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingOnTouchListener implements View.OnTouchListener {
    private int x;
    private int y;
    private float mFirstDistance;
    WindowManager.LayoutParams mLastLayoutParams;
    float mDefaultAspectRatio;
    WindowManager mWindowsManager;

    public FloatingOnTouchListener(WindowManager.LayoutParams layoutParams, float defaultAspectRatio, WindowManager wm) {
        super();
        this.mLastLayoutParams = layoutParams;
        this.mDefaultAspectRatio = defaultAspectRatio;
        mWindowsManager = wm;
    }
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int fingerCount = event.getPointerCount();
        if (1 == fingerCount) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    mLastLayoutParams.x = mLastLayoutParams.x + movedX;
                    mLastLayoutParams.y = mLastLayoutParams.y + movedY;

                    mWindowsManager.updateViewLayout(view, mLastLayoutParams);
                    break;
                default:
                    break;
            }
            view.performClick();
        } else if (2 == fingerCount) {
            switch (event.getAction()&MotionEvent.ACTION_MASK){
                case MotionEvent.ACTION_POINTER_DOWN:
                    mFirstDistance = getDistance(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float distance = getDistance(event);
                    float scale = distance / mFirstDistance;
                    mLastLayoutParams.width = (int)(mLastLayoutParams.width * scale);
                    /*Use default Aspect Ratio to calculate the height to avoid precision loss
                    after moving multiple times.*/
                    mLastLayoutParams.height = (int)(mLastLayoutParams.width * mDefaultAspectRatio);
                    mWindowsManager.updateViewLayout(view, mLastLayoutParams);
                    mFirstDistance = distance;
                    break;
                case MotionEvent.ACTION_UP:
                default:
                    break;
            }
        }
        return false;
    }

    private float getDistance(MotionEvent event) {
        float x1 = event.getX();
        float y1 = event.getY();
        float x2 = event.getX(1);
        float y2 = event.getY(1);

        return (float) Math.sqrt((x1 - x2)*(x1 - x2)+(y1 - y2)*(y1- y2));
    }
}