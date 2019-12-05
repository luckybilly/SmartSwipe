package com.billy.android.swipe.support;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingChild2;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent2;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.billy.android.swipe.SmartSwipeWrapper;

/**
 * Use this class to enable nested scroll swiping within android support library environment
 * @author billy.qi
 */
public class SmartSwipeWrapperSupport extends SmartSwipeWrapper implements NestedScrollingParent2, NestedScrollingChild2 {
    NestedScrollingParentHelper mNestedParent = new NestedScrollingParentHelper(this);
    NestedScrollingChildHelper mChildHelper = new NestedScrollingChildHelper(this);

    public SmartSwipeWrapperSupport(Context context) {
        super(context);
    }

    public SmartSwipeWrapperSupport(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmartSwipeWrapperSupport(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SmartSwipeWrapperSupport(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mChildHelper.onDetachedFromWindow();
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mIsNestedScrollingEnabled = enabled;
        if (mChildHelper != null) {
            mChildHelper.setNestedScrollingEnabled(enabled);
        }
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    protected void helperOnNestedScrollAccepted(View child, View target, int axes, int type) {
        mNestedParent.onNestedScrollAccepted(child, target, axes, type);
        mChildHelper.startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL, type);
    }

    @Override
    protected void helperOnStopNestedScroll(View target, int type) {
        mNestedParent.onStopNestedScroll(target, type);
        mChildHelper.stopNestedScroll(type);
    }

    @Override
    protected void helperOnNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
        mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, mParentOffsetInWindow, type);
    }

    @Override
    protected void helperOnNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow, type);
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }
}
