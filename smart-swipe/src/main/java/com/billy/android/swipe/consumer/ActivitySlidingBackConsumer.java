package com.billy.android.swipe.consumer;

import android.app.Activity;
import android.view.View;
import com.billy.android.swipe.R;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeBack;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.internal.ActivityTranslucentUtil;
import com.billy.android.swipe.internal.SwipeHelper;

/**
 * swipe to finish activity. make current activity translucent to show the previous activity
 * thanks:
 *  1. https://github.com/ikew0ng/SwipeBackLayout
 *  2. https://github.com/Simon-Leeeeeeeee/SLWidget
 * @author billy.qi
 */
public class ActivitySlidingBackConsumer extends TranslucentSlidingConsumer {

    protected final ActivityTranslucentUtil mActivityTranslucentUtil;
    protected Activity mActivity;
    protected int initTranslation = 0;
    protected View mPreviousActivityContentView;
    protected boolean mHorizontalSwiping;

    public ActivitySlidingBackConsumer(Activity activity) {
        this.mActivity = activity;
        this.mActivityTranslucentUtil = new ActivityTranslucentUtil(activity);
        showScrimAndShadowOutsideContentView();
        //set default shadow color and shadow size
        setShadowColor(0x80000000);
        setShadowSize(SmartSwipe.dp2px(10, activity));
    }

    @Override
    public boolean tryAcceptMoving(int pointerId, float downX, float downY, float dx, float dy) {
        return super.tryAcceptMoving(pointerId, downX, downY, dx, dy);
    }

    @Override
    public boolean tryAcceptSettling(int pointerId, float downX, float downY) {
        return false;
    }

    @Override
    public void onAttachToWrapper(SmartSwipeWrapper wrapper, SwipeHelper swipeHelper) {
        super.onAttachToWrapper(wrapper, swipeHelper);
        ActivityTranslucentUtil.convertWindowToTranslucent(mActivity);
    }

    @Override
    public void onDetachFromWrapper() {
        super.onDetachFromWrapper();
        mActivityTranslucentUtil.convertActivityFromTranslucent();
        resetPreviousActivityContentView();
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
        resetPreviousActivityContentView();
    }

    @Override
    protected void onClosed() {
        super.onClosed();
        mActivityTranslucentUtil.convertActivityFromTranslucent();
        resetPreviousActivityContentView();
    }

    private void resetPreviousActivityContentView() {
        if (mPreviousActivityContentView != null) {
            mPreviousActivityContentView.setTranslationX(0);
            mPreviousActivityContentView.setTranslationY(0);
            mPreviousActivityContentView = null;
        }
    }

    @Override
    protected void initChildrenFormXml() {
        //do nothing
    }

    @Override
    public void onSwipeAccepted(int activePointerId, boolean settling, float initialMotionX, float initialMotionY) {
        if (!mActivityTranslucentUtil.isTranslucent()) {
            mActivityTranslucentUtil.convertActivityToTranslucent();
        }
        if (mRelativeMoveFactor > 0) {
            mHorizontalSwiping = (mDirection & DIRECTION_HORIZONTAL) > 0;
            Activity previousActivity = SmartSwipeBack.findPreviousActivity(mActivity);
            if (previousActivity != null) {
                mPreviousActivityContentView = previousActivity.getWindow().getDecorView();
                switch (mDirection) {
                    case DIRECTION_LEFT:    initTranslation = -(int) (mWidth * mRelativeMoveFactor); break;
                    case DIRECTION_RIGHT:   initTranslation = (int) (mWidth * mRelativeMoveFactor); break;
                    case DIRECTION_TOP:     initTranslation = -(int) (mHeight * mRelativeMoveFactor); break;
                    case DIRECTION_BOTTOM:  initTranslation = (int) (mHeight * mRelativeMoveFactor); break;
                    default:
                }
                movePreviousActivityContentView(initTranslation);
            }
        }
        super.onSwipeAccepted(activePointerId, settling, initialMotionX, initialMotionY);
    }

    private void movePreviousActivityContentView(int translation) {
        if (mPreviousActivityContentView == null || !mActivityTranslucentUtil.isTranslucent()) {
            return;
        }
        if (mHorizontalSwiping) {
            mPreviousActivityContentView.setTranslationX(translation);
        } else {
            mPreviousActivityContentView.setTranslationY(translation);
        }
    }

    @Override
    protected void onDisplayDistanceChanged(int distanceXToDisplay, int distanceYToDisplay, int dx, int dy) {
        if (!mActivityTranslucentUtil.isTranslucent()) {
            return;
        }
        if (mPreviousActivityContentView != null) {
            int translation = 0;
            switch (mDirection) {
                case DIRECTION_LEFT:    translation = initTranslation + (int) (mWidth * mProgress * mRelativeMoveFactor); break;
                case DIRECTION_RIGHT:   translation = initTranslation - (int) (mWidth * mProgress * mRelativeMoveFactor); break;
                case DIRECTION_TOP:     translation = initTranslation + (int) (mHeight * mProgress * mRelativeMoveFactor); break;
                case DIRECTION_BOTTOM:  translation = initTranslation - (int) (mHeight * mProgress * mRelativeMoveFactor); break;
                default:
            }
            movePreviousActivityContentView(translation);
        }
        boolean horizontal = (mDirection & DIRECTION_HORIZONTAL) > 0;
        View contentView = mWrapper.getContentView();
        if (contentView != null) {
            if (horizontal) {
                contentView.setTranslationX(distanceXToDisplay);
            } else {
                contentView.setTranslationY(distanceYToDisplay);
            }
        }
        layoutScrimView();
    }

    @Override
    protected void layoutContentView(View contentView) {
        if (contentView != null) {
            contentView.layout(0, 0, mWidth, mHeight);
        }
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
