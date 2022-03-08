package com.fiill.fiillplayer.widgets;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class ViewQuery {
    private Activity mActivity;
    private View mRootView;
    private View view;
    private final Context mContext;
    private final SparseArray<View> mCache = new SparseArray<>();

    public ViewQuery(Activity a) {
        this.mActivity = a;
        mContext = a.getApplicationContext();
    }

    public ViewQuery(View v) {
        this.mRootView = v;
        this.view = v;
        mContext = v.getContext();
    }

    public ViewQuery id(int id) {
        view = mCache.get(id);
        if (view == null) {
            if (mRootView != null) {
                view = mRootView.findViewById(id);
            } else {
                view = mActivity.findViewById(id);
            }
            mCache.put(id,view);
        }
        return this;
    }

    public void image(int resId) {
        if (view instanceof ImageView) {
            ((ImageView) view).setImageResource(resId);
        }
    }

    public ViewQuery visible() {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        return this;
    }

    public ViewQuery gone() {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
        return this;
    }

    public ViewQuery invisible() {
        if (view != null) {
            view.setVisibility(View.INVISIBLE);
        }
        return this;
    }

    public ViewQuery clicked(View.OnClickListener handler) {
        if (view != null) {
            view.setOnClickListener(handler);
        }
        return this;
    }

    public ViewQuery text(CharSequence text) {
        if (view != null && view instanceof TextView) {
            ((TextView) view).setText(text);
        }
        return this;
    }

    public ViewQuery text(int resid) {
        if (view != null && view instanceof TextView) {
            ((TextView) view).setText(resid);
        }
        return this;
    }

    public ViewQuery visibility(int visible) {
        if (view != null) {
            view.setVisibility(visible);
        }
        return this;
    }


    public <T extends View> T view() {
        return (T) view;
    }

    public <T extends View> T view(Class<T> clazz) {
        return (T) view;
    }


    public String text() {
        if (view != null && view instanceof TextView) {
            return ((TextView) view).getText().toString();
        }
        return null;
    }

    public ViewQuery adapter(BaseAdapter adapter) {
        if (view != null && view instanceof ListView) {
            ((ListView) view).setAdapter(adapter);
        }
        return this;
    }

    public ViewQuery alpha(float alpha) {
        if (view != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                view.setAlpha(alpha);
            }
        }
        return this;
    }

    public ImageView imageView() {
        return (ImageView) view();
    }

    public ViewQuery hint(int stringRes) {
        if (view != null && view instanceof TextView) {
            ((TextView) view).setHint(stringRes);
        }
        return this;
    }

    public EditText edit() {
        return (EditText) view;
    }

    /**
     * request focus
     * @return
     */
    public ViewQuery focus() {
        if (view != null) {
            view.requestFocus();
        }
        return this;
    }

    /**
     * show soft input keyboard
     * @param show
     * @return
     */
    public ViewQuery showInputMethod(boolean show) {
        if (mContext != null) {
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (show && view != null) {
                view.requestFocus();
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            } else {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        return this;
    }

    /**
     * get view height in px
     * @return
     */
    public int height() {
        if (view != null) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            return layoutParams.height;
        }
        return 0;
    }

    /**
     * get view width in px
     * @return
     */
    public int width() {
        if (view != null) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            return layoutParams.width;
        }
        return 0;
    }

    /**
     * set view height in px
     * @param height
     * @return
     */
    public ViewQuery height(int height) {
        return height(height,false);
    }

    /**
     * set view height
     * @param height
     * @param dp true for db else for px
     * @return
     */
    public ViewQuery height(int height,boolean dp) {
        if (view != null) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = dp?dp2px(height):height;
            view.setLayoutParams(layoutParams);
        }
        return this;
    }

    public ViewQuery width(int height) {
        return width(height,false);
    }

    public ViewQuery width(int height,boolean dp) {
        if (view != null) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = dp?dp2px(height):height;
            view.setLayoutParams(layoutParams);
        }
        return this;
    }

    public ViewQuery background(int resId) {
        if (view != null) {
            view.setBackgroundResource(resId);
        }
        return this;
    }

    /**
     * convert dp to px
     * @param dp
     * @return
     */
    public int dp2px(int dp) {
        if (mContext != null) {
            return (int) (mContext.getResources().getDisplayMetrics().density * dp + 0.5f);
        }
        return 0;
    }

    /**
     * set text color
     * @param color
     * @return
     */
    public ViewQuery textColor(int color) {
        if (view != null && view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        }
        return this;
    }

    /**
     * set text color
     * @param color
     * @return
     */
    public ViewQuery textColor(String color) {
        if (view != null && view instanceof TextView) {
            ((TextView) view).setTextColor(Color.parseColor(color));
        }
        return this;
    }

    /**
     * get background drawable
     * @return
     */
    public Drawable background() {
        if (view != null) {
            return view.getBackground();
        }
        return null;
    }

    /**
     * set drawableLeft
     * @param drawableId
     * @return
     */
    public ViewQuery drawableLeft(int drawableId) {
        if (view!=null && view instanceof TextView) {
            if (drawableId <= 0) {
                ((TextView) view).setCompoundDrawables(null,null,null,null);
            } else {
                Drawable drawable = view.getResources().getDrawable(drawableId);
                drawable.setBounds(0,0,drawable.getMinimumWidth(),drawable.getMinimumHeight());
                ((TextView) view).setCompoundDrawables(drawable,null,null,null);
            }
        }
        return this;
    }

    /**
     * set view enable or disable
     * @param enabled
     * @return
     */
    public ViewQuery enabled(boolean enabled) {
        if (view != null) {
            view.setEnabled(enabled);
        }
        return this;
    }

    public static class TextWatcher implements android.text.TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    public boolean checked() {
        if (view != null && view instanceof Checkable) {
            return ((Checkable) view).isChecked();
        }
        return false;
    }

    public ViewQuery checked(boolean checked) {
        if (view != null && view instanceof Checkable) {
            ((Checkable) view).setChecked(checked);
        }
        return this;
    }

    public int screenWidth(){
        if (mContext != null) {
            return mContext.getResources().getDisplayMetrics().widthPixels;
        }
        return 0;
    }

    public int screenHeight(){
        if (mContext != null) {
            return mContext.getResources().getDisplayMetrics().heightPixels;
        }
        return 0;
    }

}