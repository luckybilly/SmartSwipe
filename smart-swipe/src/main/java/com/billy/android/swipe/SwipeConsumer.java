package com.billy.android.swipe;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.AbsSeekBar;

import com.billy.android.swipe.calculator.ScaledCalculator;
import com.billy.android.swipe.calculator.SwipeDistanceCalculator;
import com.billy.android.swipe.consumer.ActivitySlidingBackConsumer;
import com.billy.android.swipe.consumer.DrawerConsumer;
import com.billy.android.swipe.consumer.SlidingConsumer;
import com.billy.android.swipe.internal.ScrimView;
import com.billy.android.swipe.internal.SwipeHelper;
import com.billy.android.swipe.internal.ViewCompat;
import com.billy.android.swipe.listener.SimpleSwipeListener;
import com.billy.android.swipe.listener.SwipeListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base class to consume swipe motion event,
 * all subclasses should manage the following 4 direction swipes:
 * <pre>
 *  1. {@link #DIRECTION_LEFT}
 *  2. {@link #DIRECTION_RIGHT}
 *  3. {@link #DIRECTION_TOP}
 *  4. {@link #DIRECTION_BOTTOM}
 * </pre>
 * <pre>
 * To consume Motion Event via:
 *  when contentView is idle, SwipeConsumer hold the swipe motion event via {@link #tryAcceptMoving(int, float, float, float, float)}
 *  In other cases, when contentView is settling, SwipeConsumer hold the swipe motion event via {@link #tryAcceptSettling(int, float, float)}
 * </pre>
 * @author billy.qi
 */
public abstract class SwipeConsumer {
    public static int DEFAULT_OPEN_DISTANCE_IN_DP = 150;

    public static final int DIRECTION_NONE      = 0;
    public static final int DIRECTION_LEFT      = 1;
    public static final int DIRECTION_RIGHT     = 1 << 1;
    public static final int DIRECTION_TOP       = 1 << 2;
    public static final int DIRECTION_BOTTOM    = 1 << 3;
    public static final int DIRECTION_HORIZONTAL    = DIRECTION_LEFT | DIRECTION_RIGHT;
    public static final int DIRECTION_VERTICAL      = DIRECTION_TOP | DIRECTION_BOTTOM;
    public static final int DIRECTION_ALL           = DIRECTION_HORIZONTAL | DIRECTION_VERTICAL;

    public static final int RELEASE_MODE_NONE = 0;
    public static final int RELEASE_MODE_AUTO_CLOSE = 1;
    public static final int RELEASE_MODE_AUTO_OPEN = 2;
    public static final int RELEASE_MODE_AUTO_OPEN_CLOSE = RELEASE_MODE_AUTO_OPEN | RELEASE_MODE_AUTO_CLOSE;
    public static final int RELEASE_MODE_HOLE_OPEN = 4;


    /** The wrapper which this SwipeConsumer attached to, it would not be null */
    protected SmartSwipeWrapper mWrapper;

    public static final float PROGRESS_CLOSE = 0F;
    public static final float PROGRESS_OPEN = 1F;
    /** current swipe direction */
    protected int mDirection;
    /** cached distance by last swipe */
    protected int mCachedSwipeDistanceX, mCachedSwipeDistanceY;
    /** distance by current swipe */
    protected int mCurSwipeDistanceX, mCurSwipeDistanceY;
    protected int mSwipeOpenDistance;
    protected boolean mOpenDistanceSpecified;
    protected int mSwipeMaxDistance;
    /**
     * distance to display for subclass to show UI
     * these value is the calculate result by {@link SwipeDistanceCalculator}
     * @see SwipeDistanceCalculator
     */
    protected int mCurDisplayDistanceX, mCurDisplayDistanceY;
    protected float mProgress;
    protected volatile boolean mSwiping;
    protected SwipeHelper mSwipeHelper;


    /////////////////////////////////////////////
    //
    //  common settings  ↓↓↓↓↓
    //
    /////////////////////////////////////////////

    /** default enabled direction: none */
    private int mEnableDirection = DIRECTION_NONE;

    private int mLockDirection = DIRECTION_NONE;
    /** The interpolator when touch released, it would be used by {@link SwipeHelper#setInterpolator(Context, Interpolator)} */
    protected Interpolator mInterpolator;
    /**
     * The edge pixel size for SwipeConsumer to consume touch event
     * @see #tryAcceptMoving(int, float, float, float, float)
     */
    protected int mEdgeSize;
    private float mSensitivity = 1F;
    protected int mReleaseMode = RELEASE_MODE_AUTO_CLOSE;
    protected final List<SwipeListener> mListeners = new CopyOnWriteArrayList<>();
    /** to open this SwipeConsumer, distance should not less than this value */
    protected int mOpenDistance;
    protected float mOverSwipeFactor = 0F;
    protected SwipeDistanceCalculator mSwipeDistanceCalculator;
    protected boolean mDisableSwipeOnSettling;

    protected Object mTag;
    protected Integer mMaxSettleDuration;
    /** by default: enable nested scroll and nested fly for all direction */
    protected int mEnableNested = (DIRECTION_ALL << 4) | DIRECTION_ALL;

    /** the wrapper width, it`s value assigned via {@link #onMeasure(int, int)}  */
    protected int mWidth;
    /** the wrapper height, it`s value assigned via {@link #onMeasure(int, int)}  */
    protected int mHeight;

    /**
     * set auto close or not when SmartSwipeWrapper.onDetachedFromWindow() has been called.
     * default is true excepted Activity???BackConsumer(such as {@link ActivitySlidingBackConsumer})
     */
    protected boolean mAutoCloseOnWrapperDetachedFromWindow = true;

    /**
     * always swipe by default when settling
     * @param pointerId pointer id
     * @param downX motion event x for pointerId
     * @param downY motion event y for pointerId
     * @return swipe or not
     */
    public boolean tryAcceptSettling(int pointerId, float downX, float downY) {
        if (isNestedAndDisabled(pointerId, mDirection)) {
            return false;
        }
        if (mDisableSwipeOnSettling && getDragState() == SwipeHelper.STATE_SETTLING) {
            return false;
        }
        return isDirectionEnable(mDirection) && !isDirectionLocked(mDirection);
    }

    public boolean tryAcceptMoving(int pointerId, float downX, float downY, float dx, float dy) {
        int dir = calSwipeDirection(pointerId, downX, downY, dx, dy);
        boolean handle = dir != DIRECTION_NONE;
        if (handle) {
            mDirection = dir;
        }
        return handle;
    }
    
    public int calSwipeDirection(int pointerId, float downX, float downY, float dx, float dy) {
        if (mDirection == DIRECTION_NONE) {
            if (pointerId == SwipeHelper.POINTER_NESTED_SCROLL && ((mEnableNested) & DIRECTION_ALL) == 0
                || pointerId == SwipeHelper.POINTER_NESTED_FLY && ((mEnableNested >> 4) & DIRECTION_ALL) == 0) {
                //nested scrolling or fling, but all direction disabled for nested scrolling & fling
                return DIRECTION_NONE;
            }
        }
        float absX = Math.abs(dx);
        float absY = Math.abs(dy);
        if ((mCurSwipeDistanceX != 0 || mCurSwipeDistanceY != 0)) {
            if (dx == 0 && dy == 0) {
                return DIRECTION_NONE;
            }
            //already swiped, checkout whether the swipe direction as same as last one
            if ((mDirection & DIRECTION_HORIZONTAL) > 0 && absX > absY || (mDirection & DIRECTION_VERTICAL) > 0 && absX < absY) {
                if (!isDirectionLocked(mDirection) && !isNestedAndDisabled(pointerId, mDirection)) {
                    //it seams like it wants to continue current swiping, now, check whether any child can scroll
                    boolean canChildScroll = canChildScroll(mWrapper, mDirection, pointerId, downX, downY, dx, dy);
                    return canChildScroll ? DIRECTION_NONE : mDirection;
                }
            }
            return DIRECTION_NONE;
        }
        int dir = DIRECTION_NONE;
        boolean handle = false;
        if (absX == 0 && absY == 0) {
            if (mEdgeSize > 0) {
                if (isLeftEnable() && downX <= mEdgeSize) {
                    dir = DIRECTION_LEFT;
                    handle = true;
                } else if (isRightEnable() && downX >= mWidth - mEdgeSize) {
                    dir = DIRECTION_RIGHT;
                    handle = true;
                } else if (isTopEnable() && downY <= mEdgeSize) {
                    dir = DIRECTION_TOP;
                    handle = true;
                } else if (isBottomEnable() && downY >= mHeight - mEdgeSize) {
                    dir = DIRECTION_BOTTOM;
                    handle = true;
                }
            }
        } else {
            if (absX > absY) {
                if (dx > 0 && isLeftEnable()) {
                    dir = DIRECTION_LEFT;
                    handle = true;
                } else if (dx < 0 && isRightEnable()) {
                    dir = DIRECTION_RIGHT;
                    handle = true;
                }
            } else {
                if (dy > 0 && isTopEnable()) {
                    dir = DIRECTION_TOP;
                    handle = true;
                } else if (dy < 0 && isBottomEnable()) {
                    dir = DIRECTION_BOTTOM;
                    handle = true;
                }
            }
            if (handle) {
                if (mEdgeSize > 0) {
                    //edge size has set, just check it, ignore all child views` scroll ability (also include child Wrappers) inside this Wrapper
                    switch (dir) {
                        case DIRECTION_LEFT:    handle = downX <= mEdgeSize;break;
                        case DIRECTION_RIGHT:   handle = downX >= mWidth - mEdgeSize;break;
                        case DIRECTION_TOP:     handle = downY <= mEdgeSize;break;
                        case DIRECTION_BOTTOM:  handle = downY >= mHeight - mEdgeSize;break;
                        default:
                    }
                } else {
                    //no edge size set, check any child can scroll on this direction
                    // (absolutely, also check whether child Wrapper can consume this swipe motion event)
                    handle = !canChildScroll(mWrapper, dir, pointerId, downX, downY, dx, dy);
                }
            }
        }
        if (handle) {
            // nested fling and enabled
            if (pointerId == SwipeHelper.POINTER_NESTED_FLY && isNestedFlyEnable(dir)) {
                return dir;
            }
            if (isDirectionLocked(dir)) {
                return DIRECTION_NONE;
            }
            if (isNestedAndDisabled(pointerId, dir)) {
                return DIRECTION_NONE;
            }
            return dir;
        }
        return DIRECTION_NONE;
    }

    protected boolean isNestedAndDisabled(int pointerId, int direction) {
        return pointerId == SwipeHelper.POINTER_NESTED_SCROLL && !isNestedScrollEnable(direction)
                || pointerId == SwipeHelper.POINTER_NESTED_FLY && !isNestedFlyEnable(direction);
    }

    protected boolean canChildScroll(ViewGroup parentView, int direction, int pointerId, float downX, float downY, float dx, float dy) {
        boolean canScroll = false;
        View topChild = findTopChildUnder(parentView, (int)downX, (int)downY);
        if (topChild instanceof SmartSwipeWrapper) {
            SmartSwipeWrapper wrapper = (SmartSwipeWrapper) topChild;
            SwipeHelper swipeHelper = wrapper.mHelper;
            SwipeConsumer consumer;
            if (swipeHelper != null && (consumer = swipeHelper.getSwipeConsumer()) != null ) {
                int dir = consumer.calSwipeDirection(pointerId, downX, downY, dx, dy);
                canScroll = dir != DIRECTION_NONE && consumer.getProgress() < PROGRESS_OPEN;
            } else {
                List<SwipeConsumer> allConsumers = wrapper.getAllConsumers();
                for (SwipeConsumer sc : allConsumers) {
                    if (sc != null && sc.calSwipeDirection(pointerId, downX, downY, dx, dy) != DIRECTION_NONE) {
                        canScroll = true;
                        break;
                    }
                }
            }
        } else if (topChild != null) {
            switch (direction) {
                case DIRECTION_LEFT:
                case DIRECTION_RIGHT:
                    if (topChild instanceof AbsSeekBar) {
                        AbsSeekBar seekBar = (AbsSeekBar) topChild;
                        int progress = seekBar.getProgress();
                        int min = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            min = seekBar.getMin();
                        }
                        int max = seekBar.getMax();
                        canScroll = dx > 0 && progress < max || dx < 0 && progress > min;
                    } else {
                        canScroll = topChild.canScrollHorizontally(dx > 0 ? -1 : 1);
                    }
                    break;
                case DIRECTION_TOP:
                case DIRECTION_BOTTOM:
                    int dir = dy > 0 ? -1 : 1;
                    if (topChild instanceof AbsListView) {
                        canScroll = ViewCompat.canListViewScrollVertical((AbsListView) topChild, dir);
                    } else {
                        canScroll = topChild.canScrollVertically(dir);
                    }
                    break;
                default:
            }
        }
        if (!canScroll && topChild instanceof ViewGroup) {
            return canChildScroll((ViewGroup) topChild, direction, pointerId, downX - topChild.getLeft(), downY - topChild.getTop(), dx, dy);
        }
        return canScroll;
    }

    public void onSwipeAccepted(int activePointerId, boolean settling, float initialMotionX, float initialMotionY) {
        mSwiping = true;
        ViewParent parent = mWrapper.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        if ((mCurSwipeDistanceX != 0 || mCurSwipeDistanceY != 0)) {
            mCachedSwipeDistanceX = mCurSwipeDistanceX;
            mCachedSwipeDistanceY = mCurSwipeDistanceY;
        }
        mSwipeOpenDistance = getSwipeOpenDistance();
        if (mOverSwipeFactor > 0) {
            mSwipeMaxDistance = (int) (mSwipeOpenDistance * (1 + mOverSwipeFactor));
        } else {
            mSwipeMaxDistance = mSwipeOpenDistance;
        }
        notifySwipeStart();
    }

    public void onSwipeReleased(float xVelocity, float yVelocity) {
        ViewParent parent = mWrapper.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(false);
        }
        notifySwipeRelease(xVelocity, yVelocity);
        if (mProgress >= PROGRESS_OPEN ) {
            if ((mReleaseMode & RELEASE_MODE_HOLE_OPEN) == RELEASE_MODE_HOLE_OPEN) {
                smoothSlideTo(PROGRESS_OPEN);
                return;
            }
        }
        switch (mReleaseMode & RELEASE_MODE_AUTO_OPEN_CLOSE) {
            default:
            case RELEASE_MODE_NONE: break;
            case RELEASE_MODE_AUTO_CLOSE:
                if (mProgress >= PROGRESS_OPEN ) {
                    onOpened();
                }
                smoothSlideTo(PROGRESS_CLOSE);
                break;
            case RELEASE_MODE_AUTO_OPEN:         smoothSlideTo(PROGRESS_OPEN); break;
            case RELEASE_MODE_AUTO_OPEN_CLOSE:   smoothOpenOrClose(xVelocity, yVelocity); break;
        }
    }

    protected void smoothOpenOrClose(float xVelocity, float yVelocity) {
        boolean open = false;
        switch (mDirection) {
            case DIRECTION_LEFT:    open = xVelocity > 0 || xVelocity == 0 && mProgress > 0.5F; break;
            case DIRECTION_RIGHT:   open = xVelocity < 0 || xVelocity == 0 && mProgress > 0.5F; break;
            case DIRECTION_TOP:     open = yVelocity > 0 || yVelocity == 0 && mProgress > 0.5F; break;
            case DIRECTION_BOTTOM:  open = yVelocity < 0 || yVelocity == 0 && mProgress > 0.5F; break;
            default: break;
        }
        smoothSlideTo(open ? PROGRESS_OPEN : PROGRESS_CLOSE);
    }

    public void setCurrentStateAsClosed() {
        onClosed();
        reset();
    }

    protected void notifySwipeOpened() {
        for (SwipeListener listener : mListeners) {
            if (listener != null) {
                listener.onSwipeOpened(mWrapper, this, mDirection);
            }
        }
    }

    protected void notifySwipeClosed() {
        for (SwipeListener listener : mListeners) {
            if (listener != null) {
                listener.onSwipeClosed(mWrapper, this, mDirection);
            }
        }
    }

    protected void notifyAttachToWrapper() {
        for (SwipeListener listener : mListeners) {
            if (listener != null) {
                listener.onConsumerAttachedToWrapper(mWrapper, this);
            }
        }
    }

    protected void notifyDetachFromWrapper() {
        for (SwipeListener listener : mListeners) {
            if (listener != null) {
                listener.onConsumerDetachedFromWrapper(mWrapper, this);
            }
        }
    }

    protected void notifySwipeStateChanged(int state) {
        for (SwipeListener listener : mListeners) {
            if (listener != null) {
                listener.onSwipeStateChanged(mWrapper, this, state, mDirection, mProgress);
            }
        }
    }

    protected void notifySwipeStart() {
        for (SwipeListener listener : mListeners) {
            if (listener != null) {
                listener.onSwipeStart(mWrapper, this, mDirection);
            }
        }
    }

    protected void notifySwipeRelease(float xVelocity, float yVelocity) {
        for (SwipeListener listener : mListeners) {
            if (listener != null) {
                listener.onSwipeRelease(mWrapper, this, mDirection, mProgress, xVelocity, yVelocity);
            }
        }
    }

    protected void notifySwipeProgress(boolean settling) {
        for (SwipeListener listener : mListeners) {
            if (listener != null) {
                listener.onSwipeProcess(mWrapper, this, mDirection, settling, mProgress);
            }
        }
    }

    public int getHorizontalRange(float dx, float dy) {
        if (mCurSwipeDistanceX != 0
                || dx > 0 && isLeftEnable() && !isLeftLocked()
                || dx < 0 && isRightEnable() && !isRightLocked()) {
            return getSwipeOpenDistance();
        }
        return 0;
    }

    public int getVerticalRange(float dx, float dy) {
        if (mCurSwipeDistanceY != 0
                || dy > 0 && isTopEnable() && !isTopLocked()
                || dy < 0 && isBottomEnable() && !isBottomLocked()) {
            return getSwipeOpenDistance();
        }
        return 0;
    }

    public int clampDistanceHorizontal(int distanceX, int dx) {
        if (mCachedSwipeDistanceX != 0) {
            distanceX += mCachedSwipeDistanceX;
            mCachedSwipeDistanceX = 0;
        }
        if ((mDirection & DIRECTION_LEFT) > 0 && isLeftEnable()) {
            return SmartSwipe.ensureBetween(distanceX, 0, mSwipeMaxDistance);
        }
        if ((mDirection & DIRECTION_RIGHT) > 0 && isRightEnable()) {
            return SmartSwipe.ensureBetween(distanceX, -mSwipeMaxDistance, 0);
        }
        return 0;
    }

    public int clampDistanceVertical(int distanceY, int dy) {
        if (mCachedSwipeDistanceY != 0) {
            distanceY += mCachedSwipeDistanceY;
            mCachedSwipeDistanceY = 0;
        }
        if ((mDirection & DIRECTION_TOP) > 0 && isTopEnable()) {
            return SmartSwipe.ensureBetween(distanceY, 0, mSwipeMaxDistance);
        }
        if ((mDirection & DIRECTION_BOTTOM) > 0 && isBottomEnable()) {
            return SmartSwipe.ensureBetween(distanceY, -mSwipeMaxDistance, 0);
        }
        return 0;
    }

    /**
     * The core function to change layouts
     * @param clampedDistanceX swipe horizontal distance clamped via {@link #clampDistanceHorizontal(int, int)}
     * @param clampedDistanceY swipe vertical distance clamped via {@link #clampDistanceVertical(int, int)}
     * @param dx delta x distance from last call
     * @param dy delta y distance from last call
     * @see #clampDistanceHorizontal(int, int)
     * @see #clampDistanceVertical(int, int)
     */
    public void onSwipeDistanceChanged(int clampedDistanceX, int clampedDistanceY, int dx, int dy) {
        int maxDistance = getOpenDistance();
        if (maxDistance <= 0) {
            return;
        }
        float lastProgress = this.mProgress;
        if (clampedDistanceX != mCurSwipeDistanceX || clampedDistanceY != mCurSwipeDistanceY) {
            mCurSwipeDistanceX = clampedDistanceX;
            mCurSwipeDistanceY = clampedDistanceY;

            if (mSwipeOpenDistance <= 0) {
                mProgress = 0;
            } else {
                switch (mDirection) {
                    case DIRECTION_LEFT: case DIRECTION_RIGHT:
                        mProgress = Math.abs((float) mCurSwipeDistanceX / mSwipeOpenDistance);
                        break;
                    case DIRECTION_TOP: case DIRECTION_BOTTOM:
                        mProgress = Math.abs((float) mCurSwipeDistanceY / mSwipeOpenDistance);
                        break;
                    default:
                }
            }
            if ((mDirection & DIRECTION_HORIZONTAL) > 0) {
                int realDistanceX = clampedDistanceX;
                if (mSwipeDistanceCalculator != null) {
                    realDistanceX = mSwipeDistanceCalculator.calculateSwipeDistance(clampedDistanceX, mProgress);
                }
                dx = realDistanceX - mCurDisplayDistanceX;
                dy = 0;
                mCurDisplayDistanceX = realDistanceX;
            } else if ((mDirection & DIRECTION_VERTICAL) > 0) {
                int realDistanceY = clampedDistanceY;
                if (mSwipeDistanceCalculator != null) {
                    realDistanceY = mSwipeDistanceCalculator.calculateSwipeDistance(clampedDistanceY, mProgress);
                }
                dx = 0;
                dy = realDistanceY - mCurDisplayDistanceY;
                mCurDisplayDistanceY = realDistanceY;
            }
            onDisplayDistanceChanged(mCurDisplayDistanceX, mCurDisplayDistanceY, dx, dy);
        }
        if (this.mProgress != lastProgress) {
            boolean settling = getDragState() == SwipeHelper.STATE_SETTLING;
            notifySwipeProgress(settling);
        }
    }

    /**
     * Called to refresh UI when swipe distance is changed. final value has clamped and modified by Resistor(if it not null)
     * @param distanceXToDisplay distance changed in pixels along the X axis to show UI
     * @param distanceYToDisplay distance changed in pixels along the Y axis to show UI
     * @param dx Change in X position from the last call
     * @param dy Change in Y position from the last call
     * @see SwipeDistanceCalculator
     */
    protected abstract void onDisplayDistanceChanged(int distanceXToDisplay, int distanceYToDisplay, int dx, int dy);

    /**
     * Called when SwipeConsumer add to Wrapper
     * @param wrapper Wrapper to add to
     * @param swipeHelper SwipeHelper bind to this SwipeConsumer
     */
    public void onAttachToWrapper(SmartSwipeWrapper wrapper, SwipeHelper swipeHelper) {
        this.mWrapper = wrapper;
        if (this.mOpenDistance == 0) {
            this.mOpenDistance = SmartSwipe.dp2px(DEFAULT_OPEN_DISTANCE_IN_DP, wrapper.getContext());
        }
        this.mSwipeHelper = swipeHelper;
        if (mMaxSettleDuration != null) {
            mSwipeHelper.setMaxSettleDuration(mMaxSettleDuration);
        }
        //compat for xml usage
        if (mWrapper.isInflateFromXml()) {
            initChildrenFormXml();
        }
        notifyAttachToWrapper();
    }

    /**
     * init children via xml usage of {@link SmartSwipeWrapper}
     * @see DrawerConsumer
     * @see SlidingConsumer
     */
    protected void initChildrenFormXml() {

    }

    /**
     * Called when removed from Wrapper
     */
    public void onDetachFromWrapper() {
        notifyDetachFromWrapper();
        reset();
    }

    /**
     * Called when Wrapper#dispatchDraw(Canvas) called
     * @param canvas canvas
     */
    public void dispatchDraw(Canvas canvas) {

    }

    /**
     * Called when Wrapper#onDraw(Canvas) called
     * @param canvas canvas
     */
    public void onDraw(Canvas canvas) {

    }

    /**
     * Called when Wrapper#onMeasure(int, int) called
     * @param widthMeasureSpec widthMeasureSpec
     * @param heightMeasureSpec heightMeasureSpec
     */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = mWrapper.getMeasuredWidth();
        mHeight = mWrapper.getMeasuredHeight();
    }

    /**
     * Called when Wrapper#onLayout(boolean, int, int, int, int) called
     * @param changed changed or not
     * @param left left position
     * @param top top position
     * @param right right position
     * @param bottom bottom position
     * @return true:    Wrapper will not call super.onLayout(...),
     *         false:   Wrapper will call super.onLayout(...) to layout children
     */
    public boolean onLayout(boolean changed, int left, int top, int right, int bottom) {
        return false;
    }


    /**
     * Determine if the supplied view is under the given point in the
     * parent view's coordinate system.
     *
     * @param view Child view of the parent to hit test
     * @param x X position to test in the parent's coordinate system
     * @param y Y position to test in the parent's coordinate system
     * @return true if the supplied view is under the given point, false otherwise
     */
    public boolean isViewUnder(View view, int x, int y) {
        if (view == null) {
            return false;
        }
        return x >= view.getLeft()
                && x < view.getRight()
                && y >= view.getTop()
                && y < view.getBottom();
    }

    /**
     * Find the topmost child under the given point within the parent view's coordinate system.
     *
     * @param parentView the parent view
     * @param x X position to test in the parent's coordinate system
     * @param y Y position to test in the parent's coordinate system
     * @return The topmost child view under (x, y) or null if none found.
     */
    public View findTopChildUnder(ViewGroup parentView, int x, int y) {
        final int childCount = parentView.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = parentView.getChildAt(i);
            if (x >= child.getLeft() && x < child.getRight()
                    && y >= child.getTop() && y < child.getBottom()
                    && child.getVisibility() == View.VISIBLE) {
                if (child instanceof ScrimView && !child.isFocusable() && !child.isClickable()) {
                    continue;
                }
                return child;
            }
        }
        return null;
    }

    public void onStateChanged(int state) {
        notifySwipeStateChanged(state);
        if (state == SwipeHelper.STATE_IDLE) {
            mSwiping = false;
            if (mProgress >= 1F) {
                onOpened();
            } else if (mProgress <= 0F) {
                onClosed();
            }
        }
    }

    protected void onOpened() {
        notifySwipeOpened();
    }

    protected void onClosed() {
        notifySwipeClosed();
        mDirection = DIRECTION_NONE;
    }

    protected void reset() {
        mDirection = DIRECTION_NONE;
        mProgress = PROGRESS_CLOSE;
        mCachedSwipeDistanceX = mCurSwipeDistanceX = mCurDisplayDistanceX = 0;
        mCachedSwipeDistanceY = mCurSwipeDistanceY = mCurDisplayDistanceY = 0;
    }

    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    public SwipeConsumer setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
        if (mSwipeHelper != null && mWrapper != null) {
            mSwipeHelper.setInterpolator(mWrapper.getContext(), interpolator);
        }
        return this;
    }

    public float getSensitivity() {
        return mSensitivity;
    }

    /**
     * set the sensitivity of swipe touch event. it should be positive.
     * touchSlope /= sensitivity
     * @param sensitivity more bigger more sensitivity
     * @return this
     */
    public SwipeConsumer setSensitivity(float sensitivity) {
        if (sensitivity > 0) {
            this.mSensitivity = sensitivity;
            if (mSwipeHelper != null) {
                mSwipeHelper.setSensitivity(sensitivity);
            }
        }
        return this;
    }

    public int getReleaseMode() {
        return mReleaseMode;
    }

    /**
     * set the mode when released (default value: {@link #RELEASE_MODE_AUTO_CLOSE})
     * @param releaseMode {@link #RELEASE_MODE_NONE}
     *                  / {@link #RELEASE_MODE_AUTO_CLOSE}
     *                  / {@link #RELEASE_MODE_AUTO_OPEN}
     *                  / {@link #RELEASE_MODE_AUTO_OPEN_CLOSE}
     *                  / {@link #RELEASE_MODE_HOLE_OPEN}
     * @return this
     */
    public SwipeConsumer setReleaseMode(int releaseMode) {
        this.mReleaseMode = releaseMode;
        return this;
    }

    public int getEdgeSize() {
        return mEdgeSize;
    }

    public SwipeConsumer setEdgeSize(int edgeSize) {
        this.mEdgeSize = edgeSize;
        return this;
    }

    public SmartSwipeWrapper getWrapper() {
        return mWrapper;
    }

    public SwipeHelper getSwipeHelper() {
        return mSwipeHelper;
    }

    public int getDragState() {
        return mSwipeHelper.getDragState();
    }

    public float getProgress() {
        return mProgress;
    }

    public boolean isSwiping() {
        return mSwiping;
    }

    public int getSwipeOpenDistance() {
        if (mSwipeDistanceCalculator != null) {
            return mSwipeDistanceCalculator.calculateSwipeOpenDistance(mOpenDistance);
        } else {
            return mOpenDistance;
        }
    }
    public int getOpenDistance() {
        return mOpenDistance;
    }

    public SwipeConsumer setOpenDistance(int openDistance) {
        this.mOpenDistance = openDistance;
        this.mOpenDistanceSpecified = true;
        return this;
    }

    /**
     * remove all {@link SwipeListener} added via {@link #addListener(SwipeListener)}
     * @return this
     * @see #addListener(SwipeListener)
     */
    public SwipeConsumer removeAllListeners() {
        mListeners.clear();
        return this;
    }

    public SwipeConsumer removeListener(SwipeListener listener) {
        mListeners.remove(listener);
        return this;
    }

    /**
     * add a {@link SwipeListener} as an observer of swipe details
     * @param listener will be called when swipe event happens
     * @return this
     * @see SwipeListener
     * @see SimpleSwipeListener
     */
    public SwipeConsumer addListener(SwipeListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            this.mListeners.add(listener);
            if (mWrapper != null) {
                listener.onConsumerAttachedToWrapper(mWrapper, this);
            }
        }
        return this;
    }

    /**
     * set a calculator of swipe distance.
     * by default: swipe distance is the same as drag distance, calculator can change this role
     * @param calculator calculator
     * @return this
     * @see SwipeDistanceCalculator
     * @see ScaledCalculator
     */
    public SwipeConsumer setSwipeDistanceCalculator(SwipeDistanceCalculator calculator) {
        this.mSwipeDistanceCalculator = calculator;
        return this;
    }

    public SwipeDistanceCalculator getSwipeDistanceCalculator() {
        return mSwipeDistanceCalculator;
    }

    public boolean isDisableSwipeOnSetting() {
        return mDisableSwipeOnSettling;
    }

    /**
     * disable to handle swipe event via user touch when automatically swipe is processing
     * @param disable disable or not
     * @return this
     */
    public SwipeConsumer setDisableSwipeOnSettling(boolean disable) {
        this.mDisableSwipeOnSettling = disable;
        return this;
    }

    public float getOverSwipeFactor() {
        return mOverSwipeFactor;
    }

    /**
     * set over swipe factor.
     * max swipe distance = {@link #getSwipeOpenDistance()} * (1 + overSwipeFactor)
     * @param overSwipeFactor over swipe factor
     * @return this
     */
    public SwipeConsumer setOverSwipeFactor(float overSwipeFactor) {
        if (overSwipeFactor >= 0) {
            this.mOverSwipeFactor = overSwipeFactor;
        }
        return this;
    }

    /**
     * Get the binding extension object
     * @return extension object
     */
    public Object getTag() {
        return mTag;
    }

    /**
     * Bind an extension object if necessary
     * @param tag The extension object
     * @return this
     */
    public SwipeConsumer setTag(Object tag) {
        this.mTag = tag;
        return this;
    }

    public Integer getMaxSettleDuration() {
        if (mSwipeHelper != null) {
            return mSwipeHelper.getMaxSettleDuration();
        }
        return mMaxSettleDuration;
    }

    public SwipeConsumer setMaxSettleDuration(int maxSettleDuration) {
        this.mMaxSettleDuration = maxSettleDuration;
        if (mSwipeHelper != null) {
            mSwipeHelper.setMaxSettleDuration(maxSettleDuration);
        }
        return this;
    }

    public int getWidth() {
        return mWidth;
    }

    /**
     * set width to the ui, it works before SmartSwipeWrapper.onMeasure(int, int) invoked.
     *
     * this value will be reset by {@link #onMeasure(int, int)}
     * @param width the width of SmartSwipeWrapper
     * @return this
     */
    public SwipeConsumer setWidth(int width) {
        this.mWidth = width;
        return this;
    }

    public int getHeight() {
        return mHeight;
    }

    /**
     * set height to the ui, it works before SmartSwipeWrapper.onMeasure(int, int) invoked.
     *
     * this value will be reset by {@link #onMeasure(int, int)}
     * @param height the height of SmartSwipeWrapper
     * @return this
     */
    public SwipeConsumer setHeight(int height) {
        this.mHeight = height;
        return this;
    }

    /**
     * if set true, {@link #close()} function will be called when {@link SmartSwipeWrapper} detached from window
     * @param autoClose auto close or not
     * @return this
     */
    public SwipeConsumer setAutoCloseOnWrapperDetachedFromWindow(boolean autoClose) {
        this.mAutoCloseOnWrapperDetachedFromWindow = autoClose;
        return this;
    }

    public boolean isAutoCloseOnWrapperDetachedFromWindow() {
        return mAutoCloseOnWrapperDetachedFromWindow;
    }

    public SwipeConsumer setLeftOpen() {
        return open(false, DIRECTION_LEFT);
    }
    public SwipeConsumer setRightOpen() {
        return open(false, DIRECTION_RIGHT);
    }
    public SwipeConsumer setTopOpen() {
        return open(false, DIRECTION_TOP);
    }
    public SwipeConsumer setBottomOpen() {
        return open(false, DIRECTION_BOTTOM);
    }

    public SwipeConsumer smoothLeftOpen() {
        return open(true, DIRECTION_LEFT);
    }
    public SwipeConsumer smoothRightOpen() {
        return open(true, DIRECTION_RIGHT);
    }
    public SwipeConsumer smoothTopOpen() {
        return open(true, DIRECTION_TOP);
    }
    public SwipeConsumer smoothBottomOpen() {
        return open(true, DIRECTION_BOTTOM);
    }

    public SwipeConsumer open(final boolean smooth, final int direction) {
        if (mDirection == DIRECTION_NONE) {
            if (isDirectionEnable(direction)) {
                mDirection = direction;
                onSwipeAccepted(0, true, 0, 0);
            } else {
                return this;
            }
        } else if (mDirection != direction || mProgress == 1F) {
            return this;
        }
        boolean isLocked = isDirectionLocked(mDirection);
        if (!isLocked) {
            final int curDirection = mDirection;
            lockDirection(curDirection);
            addListener(new SimpleSwipeListener() {
                @Override
                public void onSwipeOpened(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                    unlockDirection(curDirection);
                    removeListener(this);
                }
            });
        }
        slideTo(smooth, 1F);
        return this;
    }

    public SwipeConsumer close() {
        return close(false);
    }

    public SwipeConsumer smoothClose() {
        return close(true);
    }

    public SwipeConsumer close(final boolean smooth) {
        if (mDirection != DIRECTION_NONE && mProgress != 0) {
            onSwipeAccepted(0, true, 0, 0);
            mCachedSwipeDistanceX = 0;
            mCachedSwipeDistanceY = 0;
            boolean isLocked = isDirectionLocked(mDirection);
            if (!isLocked) {
                lockDirection(mDirection);
                addListener(new SimpleSwipeListener() {
                    @Override
                    public void onSwipeClosed(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                        unlockDirection(direction);
                        removeListener(this);
                    }
                });
            }
            if (smooth) {
                smoothSlideTo(0, 0);
            } else {
                smoothSlideTo(0, 0, 0, 0);
            }
        }
        return this;
    }

    public SwipeConsumer smoothSlideTo(float progress) {
        slideTo(true, progress);
        return this;
    }

    public SwipeConsumer slideTo(boolean smooth, float progress) {
        progress = SmartSwipe.ensureBetween(progress, 0F, 1F);
        int finalX = 0, finalY = 0;
        int distance = (int) (mSwipeOpenDistance * progress);
        switch (mDirection) {
            case DIRECTION_LEFT:    finalX = distance; break;
            case DIRECTION_RIGHT:   finalX = -distance; break;
            case DIRECTION_TOP:     finalY = distance; break;
            case DIRECTION_BOTTOM:  finalY = -distance; break;
            default: break;
        }
        if (smooth) {
            smoothSlideTo(finalX, finalY);
        } else {
            smoothSlideTo(finalX, finalY, finalX, finalY);
        }
        return this;
    }

    public SwipeConsumer addToExclusiveGroup(SwipeConsumerExclusiveGroup group) {
        if (group != null) {
            group.add(this);
        }
        return this;
    }

    public int getDirection() {
        return mDirection;
    }

    public SwipeConsumer enableDirection(int direction, boolean enable) {
        if (enable) {
            return enableDirection(direction);
        } else {
            return disableDirection(direction);
        }
    }

    public SwipeConsumer enableLeft() {
        return enableDirection(DIRECTION_LEFT);
    }
    public SwipeConsumer enableRight() {
        return enableDirection(DIRECTION_RIGHT);
    }
    public SwipeConsumer enableTop() {
        return enableDirection(DIRECTION_TOP);
    }
    public SwipeConsumer enableBottom() {
        return enableDirection(DIRECTION_BOTTOM);
    }
    public SwipeConsumer enableHorizontal() {
        return enableDirection(DIRECTION_HORIZONTAL);
    }
    public SwipeConsumer enableVertical() {
        return enableDirection(DIRECTION_VERTICAL);
    }
    public SwipeConsumer enableAllDirections() {
        return enableDirection(DIRECTION_ALL);
    }

    public SwipeConsumer disableLeft() {
        return disableDirection(DIRECTION_LEFT);
    }
    public SwipeConsumer disableRight() {
        return disableDirection(DIRECTION_RIGHT);
    }
    public SwipeConsumer disableTop() {
        return disableDirection(DIRECTION_TOP);
    }
    public SwipeConsumer disableBottom() {
        return disableDirection(DIRECTION_BOTTOM);
    }
    public SwipeConsumer disableHorizontal() {
        return disableDirection(DIRECTION_HORIZONTAL);
    }
    public SwipeConsumer disableVertical() {
        return disableDirection(DIRECTION_VERTICAL);
    }
    public SwipeConsumer disableAllDirections() {
        return disableDirection(DIRECTION_ALL);
    }

    public SwipeConsumer enableDirection(int direction) {
        mEnableDirection |= direction;
        return this;
    }

    public SwipeConsumer disableDirection(int direction) {
        if ((mDirection & direction) != 0) {
            close();
        }
        mEnableDirection &= ~direction;
        return this;
    }

    public boolean isDirectionEnable(int direction) {
        return direction != DIRECTION_NONE && (mEnableDirection & direction) == direction;
    }
    public boolean isAllDirectionsEnable() {
        return (mEnableDirection & DIRECTION_ALL) == DIRECTION_ALL;
    }
    public boolean isVerticalEnable() {
        return (mEnableDirection & DIRECTION_VERTICAL) == DIRECTION_VERTICAL;
    }
    public boolean isHorizontalEnable() {
        return (mEnableDirection & DIRECTION_HORIZONTAL) == DIRECTION_HORIZONTAL;
    }
    public boolean isLeftEnable() {
        return (mEnableDirection & DIRECTION_LEFT) != 0;
    }
    public boolean isRightEnable() {
        return (mEnableDirection & DIRECTION_RIGHT) != 0;
    }
    public boolean isTopEnable() {
        return (mEnableDirection & DIRECTION_TOP) != 0;
    }
    public boolean isBottomEnable() {
        return (mEnableDirection & DIRECTION_BOTTOM) != 0;
    }

    public SwipeConsumer lockDirection(int direction, boolean lock) {
        if (lock) {
            return lockDirection(direction);
        } else {
            return unlockDirection(direction);
        }
    }

    public SwipeConsumer lockLeft() {
        return lockDirection(DIRECTION_LEFT);
    }
    public SwipeConsumer lockRight() {
        return lockDirection(DIRECTION_RIGHT);
    }
    public SwipeConsumer lockTop() {
        return lockDirection(DIRECTION_TOP);
    }
    public SwipeConsumer lockBottom() {
        return lockDirection(DIRECTION_BOTTOM);
    }
    public SwipeConsumer lockHorizontal() {
        return lockDirection(DIRECTION_HORIZONTAL);
    }
    public SwipeConsumer lockVertical() {
        return lockDirection(DIRECTION_VERTICAL);
    }
    public SwipeConsumer lockAllDirections() {
        return lockDirection(DIRECTION_ALL);
    }

    public SwipeConsumer unlockLeft() {
        return unlockDirection(DIRECTION_LEFT);
    }
    public SwipeConsumer unlockRight() {
        return unlockDirection(DIRECTION_RIGHT);
    }
    public SwipeConsumer unlockTop() {
        return unlockDirection(DIRECTION_TOP);
    }
    public SwipeConsumer unlockBottom() {
        return unlockDirection(DIRECTION_BOTTOM);
    }
    public SwipeConsumer unlockHorizontal() {
        return unlockDirection(DIRECTION_HORIZONTAL);
    }
    public SwipeConsumer unlockVertical() {
        return unlockDirection(DIRECTION_VERTICAL);
    }
    public SwipeConsumer unlockAllDirections() {
        return unlockDirection(DIRECTION_ALL);
    }


    public SwipeConsumer lockDirection(int direction) {
        mLockDirection |= direction;
        return this;
    }

    public SwipeConsumer unlockDirection(int direction) {
        mLockDirection &= ~direction;
        return this;
    }
    public boolean isDirectionLocked(int direction) {
        return direction != DIRECTION_NONE && (mLockDirection & direction) == direction;
    }
    public boolean isAllDirectionsLocked() {
        return (mLockDirection & DIRECTION_ALL) == DIRECTION_ALL;
    }
    public boolean isVerticalLocked() {
        return (mLockDirection & DIRECTION_VERTICAL) == DIRECTION_VERTICAL;
    }
    public boolean isHorizontalLocked() {
        return (mLockDirection & DIRECTION_HORIZONTAL) == DIRECTION_HORIZONTAL;
    }
    public boolean isLeftLocked() {
        return (mLockDirection & DIRECTION_LEFT) != 0;
    }
    public boolean isRightLocked() {
        return (mLockDirection & DIRECTION_RIGHT) != 0;
    }
    public boolean isTopLocked() {
        return (mLockDirection & DIRECTION_TOP) != 0;
    }
    public boolean isBottomLocked() {
        return (mLockDirection & DIRECTION_BOTTOM) != 0;
    }

    public SwipeConsumer enableNestedScrollLeft(boolean enable) {
        return enableNestedScroll(DIRECTION_LEFT, enable);
    }
    public SwipeConsumer enableNestedScrollRight(boolean enable) {
        return enableNestedScroll(DIRECTION_RIGHT, enable);
    }
    public SwipeConsumer enableNestedScrollTop(boolean enable) {
        return enableNestedScroll(DIRECTION_TOP, enable);
    }
    public SwipeConsumer enableNestedScrollBottom(boolean enable) {
        return enableNestedScroll(DIRECTION_BOTTOM, enable);
    }
    public SwipeConsumer enableNestedScrollHorizontal(boolean enable) {
        return enableNestedScroll(DIRECTION_HORIZONTAL, enable);
    }
    public SwipeConsumer enableNestedScrollVertical(boolean enable) {
        return enableNestedScroll(DIRECTION_VERTICAL, enable);
    }
    public SwipeConsumer enableNestedScrollAllDirections(boolean enable) {
        return enableNestedScroll(DIRECTION_ALL, enable);
    }

    private SwipeConsumer enableNestedScroll(int direction, boolean enable) {
        if (enable) {
            mEnableNested |= direction;
        } else {
            mEnableNested &= ~direction;
        }
        return this;
    }

    public boolean isNestedScrollEnable(int direction) {
        return (mEnableNested & direction) == direction;
    }

    public SwipeConsumer enableNestedFlyLeft(boolean enable) {
        return enableNestedFly(DIRECTION_LEFT, enable);
    }
    public SwipeConsumer enableNestedFlyRight(boolean enable) {
        return enableNestedFly(DIRECTION_RIGHT, enable);
    }
    public SwipeConsumer enableNestedFlyTop(boolean enable) {
        return enableNestedFly(DIRECTION_TOP, enable);
    }
    public SwipeConsumer enableNestedFlyBottom(boolean enable) {
        return enableNestedFly(DIRECTION_BOTTOM, enable);
    }
    public SwipeConsumer enableNestedFlyHorizontal(boolean enable) {
        return enableNestedFly(DIRECTION_HORIZONTAL, enable);
    }
    public SwipeConsumer enableNestedFlyVertical(boolean enable) {
        return enableNestedFly(DIRECTION_VERTICAL, enable);
    }
    public SwipeConsumer enableNestedFlyAllDirections(boolean enable) {
        return enableNestedFly(DIRECTION_ALL, enable);
    }

    private SwipeConsumer enableNestedFly(int direction, boolean enable) {
        if (enable) {
            mEnableNested |= direction << 4;
        } else {
            mEnableNested &= ~(direction << 4);
        }
        return this;
    }

    public boolean isNestedFlyEnable(int direction) {
        return ((mEnableNested >> 4) & direction) == direction;
    }

    public boolean isVerticalDirection() {
        return (mDirection & DIRECTION_VERTICAL) > 0;
    }

    public boolean isHorizontalDirection() {
        return (mDirection & DIRECTION_HORIZONTAL) > 0;
    }

    public void smoothSlideTo(int startX, int startY, int finalX, int finalY) {
        if (mSwipeHelper != null && mWrapper != null) {
            mSwipeHelper.smoothSlideTo(startX, startY, finalX, finalY);
            ViewCompat.postInvalidateOnAnimation(mWrapper);
        }
    }

    public void smoothSlideTo(int finalX, int finalY) {
        if (mSwipeHelper != null && mWrapper != null) {
            mSwipeHelper.smoothSlideTo(finalX, finalY);
            ViewCompat.postInvalidateOnAnimation(mWrapper);
        }
    }

    public boolean isOpened() {
        return getDragState() == SwipeHelper.STATE_IDLE && mProgress >= PROGRESS_OPEN;
    }

    public boolean isClosed() {
        return getDragState() == SwipeHelper.STATE_IDLE && mProgress <= PROGRESS_CLOSE;
    }

    public <T extends SwipeConsumer> T as(Class<T> clazz) {
        return (T)this;
    }

    public <T extends SwipeConsumer> T addConsumer(T consumer) {
        if (mWrapper != null) {
            return mWrapper.addConsumer(consumer);
        }
        return consumer;
    }
}
