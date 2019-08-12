package com.billy.android.swipe.consumer;

import android.app.Activity;
import com.billy.android.swipe.R;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.internal.ActivityTranslucentUtil;
import com.billy.android.swipe.internal.SwipeHelper;

/**
 * @author billy.qi
 */
public class ActivityShuttersBackConsumer extends ShuttersConsumer {
    protected ActivityTranslucentUtil mActivityTranslucentUtil;
    protected Activity mActivity;

    public ActivityShuttersBackConsumer(Activity activity) {
        this.mActivity = activity;
        this.mActivityTranslucentUtil = new ActivityTranslucentUtil(activity);
    }

    @Override
    public void onAttachToWrapper(SmartSwipeWrapper wrapper, SwipeHelper swipeHelper) {
        super.onAttachToWrapper(wrapper, swipeHelper);
        ActivityTranslucentUtil.convertWindowToTranslucent(mActivity);
    }

    @Override
    public void onSwipeAccepted(int activePointerId, boolean settling, float initialMotionX, float initialMotionY) {
        if (!mActivityTranslucentUtil.isTranslucent()) {
            mActivityTranslucentUtil.convertActivityToTranslucent();
        }
        super.onSwipeAccepted(activePointerId, settling, initialMotionX, initialMotionY);
    }

    @Override
    protected void onDisplayDistanceChanged(int distanceXToDisplay, int distanceYToDisplay, int dx, int dy) {
        if (mActivityTranslucentUtil.isTranslucent()) {
            super.onDisplayDistanceChanged(distanceXToDisplay, distanceYToDisplay, dx, dy);
        }
    }

    @Override
    public void onDetachFromWrapper() {
        super.onDetachFromWrapper();
        mActivityTranslucentUtil.convertActivityFromTranslucent();
    }

    @Override
    protected void onOpened() {
        super.onOpened();
        if (mListeners == null || mListeners.isEmpty()) {
            if (mActivity != null) {
                mActivity.finish();
                mActivity.overridePendingTransition(R.anim.anim_none, R.anim.anim_none);
            }
        }
    }

    @Override
    protected void onClosed() {
        super.onClosed();
        mActivityTranslucentUtil.convertActivityFromTranslucent();
    }

    @Override
    public int clampDistanceVertical(int distanceY, int dy) {
        //resolve smooth problem while convert to transparent
        if (mActivityTranslucentUtil.isTranslucent()) {
            return super.clampDistanceVertical(distanceY, dy);
        }
        return 0;
    }

    @Override
    public int clampDistanceHorizontal(int distanceX, int dx) {
        //resolve smooth problem while convert to transparent
        if (mActivityTranslucentUtil.isTranslucent()) {
            return super.clampDistanceHorizontal(distanceX, dx);
        }
        return 0;
    }

}
