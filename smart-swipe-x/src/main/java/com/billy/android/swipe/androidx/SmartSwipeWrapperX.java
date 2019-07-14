package com.billy.android.swipe.androidx;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.NestedScrollingParentHelper;
import com.billy.android.swipe.SmartSwipeWrapper;

/**
 * Use this class to enable nested scroll swiping within androidX library environment
 * @author billy.qi
 */
public class SmartSwipeWrapperX extends SmartSwipeWrapper implements NestedScrollingParent2 {
    NestedScrollingParentHelper parentHelper = new NestedScrollingParentHelper(this);

    public SmartSwipeWrapperX(Context context) {
        super(context);
    }

    public SmartSwipeWrapperX(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmartSwipeWrapperX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SmartSwipeWrapperX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
