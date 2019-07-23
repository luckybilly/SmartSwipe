package com.billy.android.swipe.consumer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.internal.SwipeHelper;
import com.billy.android.swipe.internal.ScrimView;
import com.billy.android.swipe.internal.ViewCompat;
import com.billy.android.swipe.listener.SwipeListener;

import static android.view.View.VISIBLE;
import static com.billy.android.swipe.SmartSwipeWrapper.*;
import static com.billy.android.swipe.internal.SwipeUtil.getReverseDirection;

/**
 * contains content view and at most 4 drawer view
 * drawer view shows above content view
 * default release mode is {@link #RELEASE_MODE_AUTO_OPEN_CLOSE}
 *
 * @author billy.qi
 */
public class DrawerConsumer extends SwipeConsumer implements OnClickListener {

    protected final View[] mDrawerViews = new View[4];
    protected View mCurDrawerView;
    protected int l, t, r, b;
    protected int mScrimColor = 0;
    protected int mShadowColor = 0;
    protected ScrimView mScrimView;
    protected int mShadowSize;
    protected boolean mDrawerViewRequired = true;
    protected boolean mShowScrimAndShadowOutsideContentView;

    public DrawerConsumer() {
        //set default release mode
        setReleaseMode(SwipeConsumer.RELEASE_MODE_AUTO_OPEN_CLOSE);
    }

    @Override
    public void onAttachToWrapper(SmartSwipeWrapper wrapper, SwipeHelper swipeHelper) {
        super.onAttachToWrapper(wrapper, swipeHelper);
        for (int i = 0; i < mDrawerViews.length; i++) {
            attachDrawerView(i);
        }
        if (mShadowSize == 0) {
            //10dp by default
            mShadowSize = SmartSwipe.dp2px(10, wrapper.getContext());
        }
    }

    @Override
    protected void initChildrenFormXml() {
        final SmartSwipeWrapper wrapper = mWrapper;
        int childCount = wrapper.getChildCount();
        View contentView = wrapper.getContentView();
        for (int i = 0; i < childCount; i++) {
            View child = wrapper.getChildAt(i);
            if (child == contentView || !(child.getLayoutParams() instanceof SmartSwipeWrapper.LayoutParams)) {
                continue;
            }
            final int gravity = ((SmartSwipeWrapper.LayoutParams) child.getLayoutParams()).gravity;
            if (mDrawerViews[0] == null && (gravity & DIRECTION_LEFT) == DIRECTION_LEFT) {
                // This child is a left drawer
                setLeftDrawerView(child);
                mWrapper.consumeInflateFromXml();
            }
            if (mDrawerViews[1] == null && (gravity & DIRECTION_RIGHT) == DIRECTION_RIGHT) {
                // This child is a right drawer
                setRightDrawerView(child);
                mWrapper.consumeInflateFromXml();
            }
            if (mDrawerViews[2] == null && (gravity & DIRECTION_TOP) == DIRECTION_TOP) {
                // This child is a top drawer
                setTopDrawerView(child);
                mWrapper.consumeInflateFromXml();
            }
            if (mDrawerViews[3] == null && (gravity & DIRECTION_BOTTOM) == DIRECTION_BOTTOM) {
                // This child is a bottom drawer
                setBottomDrawerView(child);
                mWrapper.consumeInflateFromXml();
            }
        }
    }

    @Override
    public void onDetachFromWrapper() {
        super.onDetachFromWrapper();
        if (mScrimView != null) {
            mWrapper.removeView(mScrimView);
            mScrimView.setOnClickListener(null);
            mScrimView = null;
        }
        for (View drawerView : mDrawerViews) {
            if (drawerView != null) {
                mWrapper.removeView(drawerView);
            }
        }
        mCurDrawerView = null;
    }

    @Override
    protected void onOpened() {
        super.onOpened();
        if (mScrimView != null && !mShowScrimAndShadowOutsideContentView) {
            mScrimView.setOnClickListener(this);
        }
    }

    @Override
    protected void onClosed() {
        super.onClosed();
        if (mCurDrawerView != null) {
            changeDrawerViewVisibility(INVISIBLE);
        }
        if (mScrimView != null) {
            mScrimView.setOnClickListener(null);
            mScrimView.setClickable(false);
            mScrimView.setFocusable(false);
            mScrimView.setVisibility(GONE);
        }
    }

    @Override
    public boolean tryAcceptMoving(int pointerId, float downX, float downY, float dx, float dy) {
        boolean handle = super.tryAcceptMoving(pointerId, downX, downY, dx, dy);
        if (handle && mCachedSwipeDistanceX == 0 && mCachedSwipeDistanceY == 0) {
            if (mDrawerViewRequired && getDrawerView(mDirection) == null) {
                handle = false;
            }
        }
        return handle;
    }

    @Override
    public void onSwipeAccepted(int activePointerId, boolean settling, float initialMotionX, float initialMotionY) {
        if (mCachedSwipeDistanceX == 0 && mCachedSwipeDistanceY == 0) {
            changeDrawerViewVisibility(INVISIBLE);
            mCurDrawerView = getDrawerView(mDirection);
            changeDrawerViewVisibility(VISIBLE);
        }
        int w = mWidth;
        int h = mHeight;
        if (mCurDrawerView != null) {
            w = mCurDrawerView.getMeasuredWidth();
            h = mCurDrawerView.getMeasuredHeight();
        } else if (mDrawerViewRequired) {
            return;
        }
        if (!mOpenDistanceSpecified) {
            if ((mDirection & DIRECTION_HORIZONTAL) > 0) {
                mOpenDistance = w;
            } else {
                mOpenDistance = h;
            }
        }
        calculateDrawerDirectionInitPosition(mDirection, w, h);
        changeDrawerViewVisibility(VISIBLE);
        initScrimView();
        layoutChildren();
        orderChildren();
        super.onSwipeAccepted(activePointerId, settling, initialMotionX, initialMotionY);
    }

    protected void changeDrawerViewVisibility(int visibility) {
        if (mCurDrawerView != null) {
            mCurDrawerView.setVisibility(visibility);
        }
    }

    @Override
    public void setCurrentStateAsClosed() {
        mCurDrawerView = null;
        super.setCurrentStateAsClosed();
    }

    protected void initScrimView() {
        if (mScrimColor != 0 || mShadowColor != 0 && mShadowSize > 0) {
            if (mScrimView == null) {
                mScrimView = new ScrimView(mWrapper.getContext());
                mWrapper.addView(mScrimView);
            }
            mScrimView.setScrimColor(mScrimColor);
            if (mShadowColor != 0 && mShadowSize > 0) {
                int shadowDirection = this.mDirection;
                if (mShowScrimAndShadowOutsideContentView) {
                    shadowDirection = getReverseDirection(mDirection);
                }
                mScrimView.setDirection(this.mDirection, mShadowColor, shadowDirection, mShadowSize, mWidth, mHeight);
            }
            mScrimView.setVisibility(VISIBLE);
        }
    }

    protected void calculateDrawerDirectionInitPosition(int direction, int w, int h) {
        switch (direction) {
            case DIRECTION_LEFT:    l = -w;     r = l + w;  t = 0;      b = h; break;
            case DIRECTION_RIGHT:   l = mWidth; r = l + w;  t = 0;      b = h; break;
            case DIRECTION_TOP:     l = 0;      r = mWidth; t = -h;     b = t + h; break;
            case DIRECTION_BOTTOM:  l = 0;      r = mWidth; t = mHeight;b = t + h; break;
            default: break;
        }
    }

    @Override
    public boolean onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mWrapper != null) {
            layoutChildren();
            return true;
        }
        return false;
    }

    @Override
    protected void onDisplayDistanceChanged(int distanceXToDisplay, int distanceYToDisplay, int dx, int dy) {
        View drawerView = mCurDrawerView;
        if (drawerView != null && drawerView.getParent() == mWrapper) {
            boolean horizontal = (mDirection & DIRECTION_HORIZONTAL) > 0;
            if (horizontal) {
                ViewCompat.offsetLeftAndRight(drawerView, dx);
            } else {
                ViewCompat.offsetTopAndBottom(drawerView, dy);
            }
            layoutScrimView();
        }
    }

    protected void orderChildren() {
        if (mCurDrawerView != null) {
            mCurDrawerView.bringToFront();
        }
        if (mScrimView != null) {
            mScrimView.bringToFront();
        }
    }

    protected void layoutChildren() {
        layoutContentView(mWrapper.getContentView());
        layoutDrawerView();
        layoutScrimView();
    }

    protected void layoutContentView(View contentView) {
        if (contentView != null) {
            contentView.layout(0, 0, mWidth, mHeight);
        }
    }

    protected void layoutDrawerView() {
        if (mCurDrawerView != null && mCurDrawerView.getVisibility() == VISIBLE) {
            mCurDrawerView.layout(l + mCurDisplayDistanceX, t + mCurDisplayDistanceY, r + mCurDisplayDistanceX, b + mCurDisplayDistanceY);
        }
    }

    protected void layoutScrimView() {
        if (mScrimView != null && mScrimView.getVisibility() == VISIBLE) {
            int l = 0, r = mWidth, t = 0, b = mHeight;
            if (mShowScrimAndShadowOutsideContentView) {
                switch (mDirection) {
                    case DIRECTION_LEFT:    r = mCurDisplayDistanceX;  break;
                    case DIRECTION_RIGHT:   l = r + mCurDisplayDistanceX;  break;
                    case DIRECTION_TOP:     b = mCurDisplayDistanceY;  break;
                    case DIRECTION_BOTTOM:  t = b + mCurDisplayDistanceY;  break;
                    default:
                }
            } else {
                switch (mDirection) {
                    case DIRECTION_LEFT:    l = mCurDisplayDistanceX;  break;
                    case DIRECTION_RIGHT:   r = r + mCurDisplayDistanceX;  break;
                    case DIRECTION_TOP:     t = mCurDisplayDistanceY;  break;
                    case DIRECTION_BOTTOM:  b = b + mCurDisplayDistanceY;  break;
                    default:
                }
            }
            mScrimView.layout(l, t, r, b);
            mScrimView.setProgress(mShowScrimAndShadowOutsideContentView ? (1 - mProgress) : mProgress);
        }
    }

    @Override
    protected void notifySwipeStart() {
        if (mCurDrawerView instanceof SwipeListener) {
            ((SwipeListener)mCurDrawerView).onSwipeStart(mWrapper, this, mDirection);
        }
        super.notifySwipeStart();
    }

    @Override
    protected void notifySwipeProgress(boolean settling) {
        if (mCurDrawerView instanceof SwipeListener) {
            ((SwipeListener) mCurDrawerView).onSwipeProcess(mWrapper, this, mDirection, settling, mProgress);
        }
        super.notifySwipeProgress(settling);
    }

    @Override
    protected void notifySwipeRelease(float xVelocity, float yVelocity) {
        if (mCurDrawerView instanceof SwipeListener) {
            ((SwipeListener) mCurDrawerView).onSwipeRelease(mWrapper, this, mDirection, mProgress, xVelocity, yVelocity);
        }
        super.notifySwipeRelease(xVelocity, yVelocity);
    }

    public View getDrawerView(int direction) {
        int viewIndex = -1;
        switch (direction) {
            default: break;
            case DIRECTION_LEFT:    viewIndex = 0; break;
            case DIRECTION_RIGHT:   viewIndex = 1; break;
            case DIRECTION_TOP:     viewIndex = 2; break;
            case DIRECTION_BOTTOM:  viewIndex = 3; break;
        }
        if (viewIndex < 0) {
            return null;
        }
        return mDrawerViews[viewIndex];
    }

    public DrawerConsumer setLeftDrawerView(View drawerView) {
        return setDrawerView(DIRECTION_LEFT, drawerView);
    }
    public DrawerConsumer setRightDrawerView(View drawerView) {
        return setDrawerView(DIRECTION_RIGHT, drawerView);
    }
    public DrawerConsumer setTopDrawerView(View drawerView) {
        return setDrawerView(DIRECTION_TOP, drawerView);
    }
    public DrawerConsumer setBottomDrawerView(View drawerView) {
        return setDrawerView(DIRECTION_BOTTOM, drawerView);
    }
    public DrawerConsumer setHorizontalDrawerView(View drawerView) {
        return setDrawerView(DIRECTION_HORIZONTAL, drawerView);
    }
    public DrawerConsumer setVerticalDrawerView(View drawerView) {
        return setDrawerView(DIRECTION_VERTICAL, drawerView);
    }
    public DrawerConsumer setAllDirectionDrawerView(View drawerView) {
        return setDrawerView(DIRECTION_ALL, drawerView);
    }

    /**
     * set a extension to the direction, also set direction enable if drawerView is not null(otherwise, disable the direction)
     * direction can be a single direction or mixed direction(eg: DIRECTION_LEFT | DIRECTION_RIGHT)
     * @param direction direction to set
     * @param drawerView view
     * @return this
     */
    public DrawerConsumer setDrawerView(int direction, View drawerView) {
        enableDirection(direction, drawerView != null);
        if ((direction & DIRECTION_LEFT)    > 0) {
            setOrUpdateDrawerView(0, drawerView);
        }
        if ((direction & DIRECTION_RIGHT)   > 0) {
            setOrUpdateDrawerView(1, drawerView);
        }
        if ((direction & DIRECTION_TOP)     > 0) {
            setOrUpdateDrawerView(2, drawerView);
        }
        if ((direction & DIRECTION_BOTTOM)  > 0) {
            setOrUpdateDrawerView(3, drawerView);
        }
        return this;
    }

    private void setOrUpdateDrawerView(int index, View drawerView) {
        View oldView = mDrawerViews[index];
        if (oldView == drawerView) {
            return;
        }
        mDrawerViews[index] = drawerView;
        attachDrawerView(index);
    }

    private void attachDrawerView(final int index) {
        final View drawerView = mDrawerViews[index];
        final SmartSwipeWrapper wrapper = mWrapper;
        if (drawerView != null && wrapper != null && drawerView.getParent() != wrapper) {
            if (drawerView.getParent() != null) {
                ((ViewGroup)drawerView.getParent()).removeView(drawerView);
            }
            int contentViewIndex = wrapper.indexOfChild(wrapper.getContentView());
            if (contentViewIndex >= 0) {
                ViewGroup.LayoutParams lp = drawerView.getLayoutParams();
                if (lp == null) {
                    int w = FrameLayout.LayoutParams.WRAP_CONTENT, h = FrameLayout.LayoutParams.WRAP_CONTENT;
                    switch (index) {
                        default: break;
                        case 0: case 1: h = FrameLayout.LayoutParams.MATCH_PARENT; break;
                        case 2: case 3: w = FrameLayout.LayoutParams.MATCH_PARENT; break;
                    }
                    lp = new FrameLayout.LayoutParams(w, h);
                    drawerView.setLayoutParams(lp);
                }
                wrapper.addView(drawerView, contentViewIndex);
                drawerView.setVisibility(INVISIBLE);
            }
        }
    }

    @Override
    public int getOpenDistance() {
        if (mCurDrawerView == null) {
            return super.getOpenDistance();
        }
        if ((mDirection & DIRECTION_HORIZONTAL) > 0) {
            return mCurDrawerView.getMeasuredWidth();
        }
        return mCurDrawerView.getMeasuredHeight();
    }

    /**
     * Set a color to use for the scrim that obscures primary content while a drawer is open.
     * @param color Color to use in 0xAARRGGBB format.
     * @return this
     */
    public DrawerConsumer setScrimColor(int color) {
        mScrimColor = color;
        return this;
    }

    /**
     * Set a color to use for the shadow at the edge of content view while a drawer is open.
     * @param shadowColor  Color to use in 0xAARRGGBB format.
     * @return this
     */
    public DrawerConsumer setShadowColor(int shadowColor) {
        mShadowColor = shadowColor;
        return this;
    }

    public int getShadowSize() {
        return mShadowSize;
    }

    /**
     * set the size of shadow at the edge of content view while a drawer is open.
     * @param size shadow size in pixel
     * @return this
     */
    public DrawerConsumer setShadowSize(int size) {
        this.mShadowSize = size;
        return this;
    }

    public boolean isDrawerViewRequired() {
        return mDrawerViewRequired;
    }

    /**
     * set the extension view as drawer is required or not
     * it useful inside this sdk framework,
     * developers who use this SDK do not call this function unless you really know what its mean
     * @param required required or not
     * @return this
     */
    public DrawerConsumer setDrawerViewRequired(boolean required) {
        this.mDrawerViewRequired = required;
        return this;
    }

    public boolean isScrimAndShadowOutsideContentView() {
        return mShowScrimAndShadowOutsideContentView;
    }

    public DrawerConsumer showScrimAndShadowOutsideContentView() {
        this.mShowScrimAndShadowOutsideContentView = true;
        return this;
    }
    public DrawerConsumer showScrimAndShadowInsideContentView() {
        this.mShowScrimAndShadowOutsideContentView = false;
        return this;
    }

    @Override
    public void onClick(View v) {
        if (getDragState() == SwipeHelper.STATE_IDLE && !mShowScrimAndShadowOutsideContentView && v == mScrimView) {
            smoothClose();
        }
    }

}
