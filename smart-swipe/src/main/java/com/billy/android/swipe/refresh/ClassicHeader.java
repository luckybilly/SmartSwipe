package com.billy.android.swipe.refresh;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.billy.android.swipe.R;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeRefresh;

/**
 * classic header for {@link SmartSwipeRefresh}
 * @author billy.qi
 */
public class ClassicHeader extends RelativeLayout implements SmartSwipeRefresh.SmartSwipeRefreshHeader {
    public TextView mTitleTextView;
    public ImageView mProgressImageView;
    public int mStrResId;
    public ObjectAnimator animator;

    public ClassicHeader(Context context) {
        super(context);
        if (isInEditMode()) {
            onInit(false);
        }
    }

    public ClassicHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (isInEditMode()) {
            onInit(false);
        }
    }

    public ClassicHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            onInit(false);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ClassicHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (isInEditMode()) {
            onInit(false);
        }
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onInit(boolean horizontal) {

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (horizontal) {
            LayoutInflater.from(getContext()).inflate(R.layout.ssr_classic_header_footer_horizontal, this);
            if (layoutParams == null) {
                int width = SmartSwipe.dp2px(60, getContext());
                layoutParams = new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        } else {
            LayoutInflater.from(getContext()).inflate(R.layout.ssr_classic_header_footer, this);
            if (layoutParams == null) {
                int height = SmartSwipe.dp2px(60, getContext());
                layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            }
        }
        setLayoutParams(layoutParams);
        Drawable background = getBackground();
        if (background == null) {
            setBackgroundColor(0xFFEEEEEE);
        }
        mProgressImageView = findViewById(R.id.ssr_classics_progress);
        mProgressImageView.setVisibility(GONE);
        mTitleTextView = findViewById(R.id.ssr_classics_title);
        mTitleTextView.setText(R.string.ssr_header_pulling);
        animator = ObjectAnimator.ofFloat(mProgressImageView, "rotation", 0, 3600);
        animator.setDuration(5000);
        animator.setInterpolator(null);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
    }

    public void cancelAnimation() {
        animator.cancel();
        mProgressImageView.setVisibility(GONE);
    }

    public void showAnimation() {
        animator.start();
        mProgressImageView.setVisibility(VISIBLE);
    }

    @Override
    public void onStartDragging() {

    }

    @Override
    public void onProgress(boolean dragging, float progress) {
        if (dragging) {
            setText(progress >= 1 ? R.string.ssr_header_release : R.string.ssr_header_pulling);
        } else if (progress <= 0) {
            cancelAnimation();
        }
    }

    @Override
    public long onFinish(boolean success) {
        cancelAnimation();
        setText(success ? R.string.ssr_header_finish : R.string.ssr_header_failed);
        return 500;
    }

    @Override
    public void onReset() {

    }

    @Override
    public void onDataLoading() {
        showAnimation();
        setText(R.string.ssr_footer_refreshing);
    }


    public void setText(int strResId) {
        if (mStrResId != strResId && mTitleTextView != null) {
            mStrResId = strResId;
            mTitleTextView.setText(strResId);
        }
    }
}
