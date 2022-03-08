package com.fiill.fiillplayer.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * class to set the video view's size after scale the aspect ratio
 */

public class ScalableTextureView extends TextureView implements IScalableDisplay {
    private MeasureHelper mMeasureHelper;

    public ScalableTextureView(Context context) {
        super(context);
        init();
    }

    public ScalableTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public void setAspectRatio(int aspectRatio) {
        mMeasureHelper.setAspectRatio(aspectRatio);
        requestLayout();
    }

    @Override
    public void setVideoSize(int videoWidth, int videoHeight) {
        if (videoWidth > 0 && videoHeight > 0) {
            mMeasureHelper.setVideoSize(videoWidth, videoHeight);
            requestLayout();
        }
    }

    public ScalableTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mMeasureHelper = new MeasureHelper(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMeasureHelper.doMeasure(widthMeasureSpec,heightMeasureSpec);
        setMeasuredDimension(mMeasureHelper.getMeasuredWidth(), mMeasureHelper.getMeasuredHeight());
    }
}
