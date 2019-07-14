package com.billy.android.swipe.support;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.NestedScrollingParent2;
import android.support.v4.view.NestedScrollingParentHelper;
import android.util.AttributeSet;
import android.view.View;
import com.billy.android.swipe.SmartSwipeWrapper;

/**
 * Use this class to enable nested scroll swiping within android support library environment
 * @author billy.qi
 */
public class SmartSwipeWrapperSupport extends SmartSwipeWrapper implements NestedScrollingParent2 {
    NestedScrollingParentHelper parentHelper = new NestedScrollingParentHelper(this);

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
    protected void helperOnNestedScrollAccepted(View child, View target, int axes, int type) {
        parentHelper.onNestedScrollAccepted(child, target, axes, type);
    }

    @Override
    protected void helperOnStopNestedScroll(View target, int type) {
        parentHelper.onStopNestedScroll(target, type);
    }
}
