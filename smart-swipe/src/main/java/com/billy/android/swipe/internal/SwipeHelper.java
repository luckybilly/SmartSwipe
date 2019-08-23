package com.billy.android.swipe.internal;

import android.content.Context;
import android.util.Log;
import android.view.*;
import android.view.animation.Interpolator;
import android.widget.OverScroller;
import com.billy.android.swipe.SwipeConsumer;

import java.util.Arrays;


/**
 * This class is copy and modified from ViewDragHelper
 *  1. mCapturedView removed. use mClampedDistanceX and mClampedDistanceY instead
 *  2. Callback removed. use {@link SwipeConsumer} to consume the motion event
 * @author billy.qi
 */
public class SwipeHelper {
    private static final String TAG = "SwipeHelper";

    /**
     * A null/invalid pointer ID.
     */
    public static final int INVALID_POINTER = -1;
    public static final int POINTER_NESTED_SCROLL = -2;
    public static final int POINTER_NESTED_FLY = -3;

    /**
     * A view is not currently being dragged or animating as a result of a fling/snap.
     */
    public static final int STATE_IDLE = 0;

    /**
     * A view is currently being dragged. The position is currently changing as a result
     * of user input or simulated user input.
     */
    public static final int STATE_DRAGGING = 1;

    /**
     * A view is currently settling into place as a result of a fling or
     * predefined non-interactive motion.
     */
    public static final int STATE_SETTLING = 2;
    public static final int STATE_NONE_TOUCH = 3;
    private final ViewConfiguration viewConfiguration;

    private int maxSettleDuration = 600; // ms

    // Current drag state; idle, dragging or settling
    private int mDragState;

    // Distance to travel before a drag may begin
    private int mTouchSlop;

    // Last known position/pointer tracking
    private int mActivePointerId = INVALID_POINTER;
    private float[] mInitialMotionX;
    private float[] mInitialMotionY;
    private float[] mLastMotionX;
    private float[] mLastMotionY;
    private int mPointersDown;

    private VelocityTracker mVelocityTracker;
    private float mMaxVelocity;
    private float mMinVelocity;

    private OverScroller mScroller;

    private final SwipeConsumer mSwipeConsumer;

    //    private View mCapturedView;
    private boolean mReleaseInProgress;

    private final ViewGroup mParentView;

    private int mClampedDistanceX;
    private int mClampedDistanceY;

    /**
     * Default interpolator defining the animation curve for mScroller
     */
    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    /**
     * Factory method to create a new SwipeHelper.
     *
     * @param forParent Parent view to monitor
     * @param consumer Callback to provide information and receive events
     * @param interpolator interpolator for animation
     * @return a new SwipeHelper instance
     */
    public static SwipeHelper create(ViewGroup forParent, SwipeConsumer consumer, Interpolator interpolator) {
        return new SwipeHelper(forParent.getContext(), forParent, consumer, interpolator);
    }
    public static SwipeHelper create(ViewGroup forParent, SwipeConsumer consumer) {
        return create(forParent, consumer, null);
    }

    /**
     * Factory method to create a new SwipeHelper.
     *
     * @param forParent Parent view to monitor
     * @param sensitivity Multiplier for how sensitive the helper should be about detecting
     *                    the start of a drag. Larger values are more sensitive. 1.0f is normal.
     * @param consumer Callback to provide information and receive events
     * @param interpolator interpolator for animation
     * @return a new SwipeHelper instance
     */
    public static SwipeHelper create(ViewGroup forParent, float sensitivity, SwipeConsumer consumer, Interpolator interpolator) {
        final SwipeHelper helper = create(forParent, consumer, interpolator);
        helper.mTouchSlop = (int) (helper.mTouchSlop * (1 / sensitivity));
        return helper;
    }

    public static SwipeHelper create(ViewGroup forParent, float sensitivity, SwipeConsumer cb) {
        return create(forParent, sensitivity, cb, null);
    }

    public void setSensitivity(float sensitivity) {
        mTouchSlop = (int) (viewConfiguration.getScaledTouchSlop() * (1 / sensitivity));
    }

    /**
     * Apps should use SwipeHelper.create() to get a new instance.
     * This will allow VDH to use internal compatibility implementations for different
     * platform versions.
     *
     * @param context Context to initialize config-dependent params from
     * @param forParent Parent view to monitor
     * @param interpolator interpolator for animation
     */
    private SwipeHelper(Context context, ViewGroup forParent, SwipeConsumer cb, Interpolator interpolator) {
        if (forParent == null) {
            throw new IllegalArgumentException("Parent view may not be null");
        }
        if (cb == null) {
            throw new IllegalArgumentException("Callback may not be null");
        }

        mParentView = forParent;
        mSwipeConsumer = cb;

        viewConfiguration = ViewConfiguration.get(context);

        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mMaxVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mMinVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        setInterpolator(context, interpolator);
    }

    public void setInterpolator(Context context, Interpolator interpolator) {
        if (interpolator == null) {
            interpolator = sInterpolator;
        }
        if (mScroller != null) {
            abort();
            mScroller = null;
        }
        mScroller = new OverScroller(context, interpolator);
    }

    /**
     * Set the minimum velocity that will be detected as having a magnitude greater than zero
     * in pixels per second. Callback methods accepting a velocity will be clamped appropriately.
     *
     * @param minVel Minimum velocity to detect
     * @return this
     */
    public SwipeHelper setMinVelocity(float minVel) {
        mMinVelocity = minVel;
        return this;
    }

    /**
     * Return the currently configured minimum velocity. Any flings with a magnitude less
     * than this value in pixels per second. Callback methods accepting a velocity will receive
     * zero as a velocity value if the real detected velocity was below this threshold.
     *
     * @return the minimum velocity that will be detected
     */
    public float getMinVelocity() {
        return mMinVelocity;
    }

    /**
     * Retrieve the current drag state of this helper. This will return one of
     * {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link #STATE_SETTLING} or {@link #STATE_NONE_TOUCH}.
     * @return The current drag state
     */
    public int getDragState() {
        return mDragState;
    }

    /**
     * @return The ID of the pointer currently dragging
     *         or {@link #INVALID_POINTER}.
     */
    public int getActivePointerId() {
        return mActivePointerId;
    }

    /**
     * @return The minimum distance in pixels that the user must travel to initiate a drag
     */

    public int getTouchSlop() {
        return mTouchSlop;
    }

    /**
     * The result of a call to this method is equivalent to
     * {@link #processTouchEvent(android.view.MotionEvent)} receiving an ACTION_CANCEL event.
     */
    public void cancel() {
        mActivePointerId = INVALID_POINTER;
        clearMotionHistory();

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * {@link #cancel()}, but also abort all motion in progress and snap to the end of any
     * animation.
     */
    public void abort() {
        cancel();
        if (mDragState == STATE_SETTLING || mDragState == STATE_NONE_TOUCH) {
            final int oldX = mScroller.getCurrX();
            final int oldY = mScroller.getCurrY();
            mScroller.abortAnimation();
            final int newX = mScroller.getCurrX();
            final int newY = mScroller.getCurrY();
            mSwipeConsumer.onSwipeDistanceChanged(newX, newY, newX - oldX, newY - oldY);
        }
        setDragState(STATE_IDLE);
    }

    /**
     * Animate the view <code>child</code> to the given (left, top) position.
     * If this method returns true, the caller should invoke {@link #continueSettling()}
     * on each subsequent frame to continue the motion until it returns false. If this method
     * returns false there is no further work to do to complete the movement.
     *
     * @param startX start x position
     * @param startY start y position
     * @param finalX Final x position
     * @param finalY Final y position
     * @return true if animation should continue through {@link #continueSettling()} calls
     */
    public boolean smoothSlideTo(int startX, int startY, int finalX, int finalY) {
        mClampedDistanceX = startX;
        mClampedDistanceY = startY;
        return smoothSlideTo(finalX, finalY);
    }

    public boolean smoothSlideTo(int finalX, int finalY) {
        boolean continueSliding;
        if (mVelocityTracker != null) {
            continueSliding = smoothSettleCapturedViewTo(finalX, finalY,
                    (int) mVelocityTracker.getXVelocity(mActivePointerId),
                    (int) mVelocityTracker.getYVelocity(mActivePointerId));
        } else {
            continueSliding = smoothSettleCapturedViewTo(finalX, finalY, 0, 0);
        }
        mActivePointerId = INVALID_POINTER;
        return continueSliding;
    }

    /**
     * Settle the captured view at the given (left, top) position.
     * The appropriate velocity from prior motion will be taken into account.
     * If this method returns true, the caller should invoke {@link #continueSettling()}
     * on each subsequent frame to continue the motion until it returns false. If this method
     * returns false there is no further work to do to complete the movement.
     *
     * @param finalX Settled left edge position for the captured view
     * @param finalY Settled top edge position for the captured view
     * @return true if animation should continue through {@link #continueSettling()} calls
     */
    public boolean settleCapturedViewAt(int finalX, int finalY) {
        if (!mReleaseInProgress) {
            throw new IllegalStateException("Cannot settleCapturedViewAt outside of a call to "
                    + "Callback#onViewReleased");
        }

        return smoothSettleCapturedViewTo(finalX, finalY,
                (int) mVelocityTracker.getXVelocity(mActivePointerId),
                (int) mVelocityTracker.getYVelocity(mActivePointerId));
    }

    /**
     * Settle the captured view at the given (left, top) position.
     *
     * @param finalX Target left position for the captured view
     * @param finalY Target top position for the captured view
     * @param xvel Horizontal velocity
     * @param yvel Vertical velocity
     * @return true if animation should continue through {@link #continueSettling()} calls
     */
    private boolean smoothSettleCapturedViewTo(int finalX, int finalY, int xvel, int yvel) {
        final int startX = mClampedDistanceX;
        final int startTop = mClampedDistanceY;
        final int dx = finalX - startX;
        final int dy = finalY - startTop;

        mScroller.abortAnimation();
        if (dx == 0 && dy == 0) {
            setDragState(STATE_SETTLING);
            mSwipeConsumer.onSwipeDistanceChanged(finalX, finalY, dx, dy);
            setDragState(STATE_IDLE);
            return false;
        }

        final int duration = computeSettleDuration(dx, dy, xvel, yvel);
        mScroller.startScroll(startX, startTop, dx, dy, duration);

        setDragState(STATE_SETTLING);
        return true;
    }

    private int computeSettleDuration(int dx, int dy, int xvel, int yvel) {
        xvel = clampMag(xvel, (int) mMinVelocity, (int) mMaxVelocity);
        yvel = clampMag(yvel, (int) mMinVelocity, (int) mMaxVelocity);
        final int absDx = Math.abs(dx);
        final int absDy = Math.abs(dy);
        final int absXVel = Math.abs(xvel);
        final int absYVel = Math.abs(yvel);
        final int addedVel = absXVel + absYVel;
        final int addedDistance = absDx + absDy;

        final float xweight = xvel != 0 ? (float) absXVel / addedVel :
                (float) absDx / addedDistance;
        final float yweight = yvel != 0 ? (float) absYVel / addedVel :
                (float) absDy / addedDistance;

        int xduration = computeAxisDuration(dx, xvel, mSwipeConsumer.getHorizontalRange(dx, dy));
        int yduration = computeAxisDuration(dy, yvel, mSwipeConsumer.getVerticalRange(dx, dy));

        return (int) (xduration * xweight + yduration * yweight);
    }

    private int computeAxisDuration(int delta, int velocity, int motionRange) {
        if (delta == 0) {
            return 0;
        }

        final int width = mParentView.getWidth();
        final int halfWidth = width >> 1;
        final float distanceRatio = Math.min(1f, (float) Math.abs(delta) / width);
        final float distance = halfWidth + halfWidth
                * distanceInfluenceForSnapDuration(distanceRatio);

        int duration;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            final float range = (float) Math.abs(delta) / motionRange;
            duration = (int) (range * maxSettleDuration);
        }
        return Math.min(duration, maxSettleDuration);
    }

    /**
     * Clamp the magnitude of value for absMin and absMax.
     * If the value is below the minimum, it will be clamped to zero.
     * If the value is above the maximum, it will be clamped to the maximum.
     *
     * @param value Value to clamp
     * @param absMin Absolute value of the minimum significant value to return
     * @param absMax Absolute value of the maximum value to return
     * @return The clamped value with the same sign as <code>value</code>
     */
    private int clampMag(int value, int absMin, int absMax) {
        final int absValue = Math.abs(value);
        if (absValue < absMin) {
            return 0;
        }
        if (absValue > absMax) {
            return value > 0 ? absMax : -absMax;
        }
        return value;
    }

    /**
     * Clamp the magnitude of value for absMin and absMax.
     * If the value is below the minimum, it will be clamped to zero.
     * If the value is above the maximum, it will be clamped to the maximum.
     *
     * @param value Value to clamp
     * @param absMin Absolute value of the minimum significant value to return
     * @param absMax Absolute value of the maximum value to return
     * @return The clamped value with the same sign as <code>value</code>
     */
    private float clampMag(float value, float absMin, float absMax) {
        final float absValue = Math.abs(value);
        if (absValue < absMin) {
            return 0;
        }
        if (absValue > absMax) {
            return value > 0 ? absMax : -absMax;
        }
        return value;
    }

    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * (float) Math.PI / 2.0f;
        return (float) Math.sin(f);
    }


    public boolean continueSettling() {
        if (mDragState == STATE_SETTLING) {
            boolean keepGoing = mScroller.computeScrollOffset();
            final int x = mScroller.getCurrX();
            final int y = mScroller.getCurrY();
            final int dx = x - mClampedDistanceX;
            final int dy = y - mClampedDistanceY;

            if (dx != 0) {
                mClampedDistanceX = x;
            }
            if (dy != 0) {
                mClampedDistanceY = y;
            }

            if (dx != 0 || dy != 0) {
                mSwipeConsumer.onSwipeDistanceChanged(x, y, dx, dy);
            }

            if (keepGoing && x == mScroller.getFinalX() && y == mScroller.getFinalY()) {
                // Close enough. The interpolator/scroller might think we're still moving
                // but the user sure doesn't.
                mScroller.abortAnimation();
                keepGoing = false;
            }

            if (!keepGoing) {
                setDragState(STATE_IDLE);
            }
        }

        return mDragState == STATE_SETTLING;
    }

    /**
     * Like all callback events this must happen on the UI thread, but release
     * involves some extra semantics. During a release (mReleaseInProgress)
     * is the only time it is valid to call {@link #settleCapturedViewAt(int, int)}
     * @param xvel x velocity
     * @param yvel y velocity
     */
    public void dispatchViewReleased(float xvel, float yvel) {
        mReleaseInProgress = true;
        mSwipeConsumer.onSwipeReleased(xvel, yvel);
        mReleaseInProgress = false;

        if (mDragState == STATE_DRAGGING) {
            // onViewReleased didn't call a method that would have changed this. Go idle.
            setDragState(STATE_IDLE);
        }
    }

    private void clearMotionHistory() {
        if (mInitialMotionX == null) {
            return;
        }
        Arrays.fill(mInitialMotionX, 0);
        Arrays.fill(mInitialMotionY, 0);
        Arrays.fill(mLastMotionX, 0);
        Arrays.fill(mLastMotionY, 0);
        mPointersDown = 0;
    }

    private void clearMotionHistory(int pointerId) {
        if (mInitialMotionX == null || !isPointerDown(pointerId)) {
            return;
        }
        mInitialMotionX[pointerId] = 0;
        mInitialMotionY[pointerId] = 0;
        mLastMotionX[pointerId] = 0;
        mLastMotionY[pointerId] = 0;
        mPointersDown &= ~(1 << pointerId);
    }

    private void ensureMotionHistorySizeForId(int pointerId) {
        if (mInitialMotionX == null || mInitialMotionX.length <= pointerId) {
            float[] imx = new float[pointerId + 1];
            float[] imy = new float[pointerId + 1];
            float[] lmx = new float[pointerId + 1];
            float[] lmy = new float[pointerId + 1];

            if (mInitialMotionX != null) {
                System.arraycopy(mInitialMotionX, 0, imx, 0, mInitialMotionX.length);
                System.arraycopy(mInitialMotionY, 0, imy, 0, mInitialMotionY.length);
                System.arraycopy(mLastMotionX, 0, lmx, 0, mLastMotionX.length);
                System.arraycopy(mLastMotionY, 0, lmy, 0, mLastMotionY.length);
            }

            mInitialMotionX = imx;
            mInitialMotionY = imy;
            mLastMotionX = lmx;
            mLastMotionY = lmy;
        }
    }

    private void saveInitialMotion(float x, float y, int pointerId) {
        ensureMotionHistorySizeForId(pointerId);
        mInitialMotionX[pointerId] = mLastMotionX[pointerId] = x;
        mInitialMotionY[pointerId] = mLastMotionY[pointerId] = y;
        mPointersDown |= 1 << pointerId;
    }

    private void saveLastMotion(MotionEvent ev) {
        final int pointerCount = ev.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            final int pointerId = ev.getPointerId(i);
            // If pointer is invalid then skip saving on ACTION_MOVE.
            if (!isValidPointerForActionMove(pointerId)) {
                continue;
            }
            final float x = ev.getX(i);
            final float y = ev.getY(i);
            mLastMotionX[pointerId] = x;
            mLastMotionY[pointerId] = y;
        }
    }

    /**
     * Check if the given pointer ID represents a pointer that is currently down (to the best
     * of the SwipeHelper's knowledge).
     *
     * <p>The state used to report this information is populated by the methods
     * {@link #shouldInterceptTouchEvent(android.view.MotionEvent)} or
     * {@link #processTouchEvent(android.view.MotionEvent)}. If one of these methods has not
     * been called for all relevant MotionEvents to track, the information reported
     * by this method may be stale or incorrect.</p>
     *
     * @param pointerId pointer ID to check; corresponds to IDs provided by MotionEvent
     * @return true if the pointer with the given ID is still down
     */
    public boolean isPointerDown(int pointerId) {
        return (mPointersDown & 1 << pointerId) != 0;
    }

    void setDragState(int state) {
        if (mDragState != state) {
            mDragState = state;
            mSwipeConsumer.onStateChanged(state);
//            if (mDragState == STATE_IDLE) {
//                mClampedDistanceX = mClampedDistanceY = 0;
//            }
        }
    }

    /**
     * Attempt to capture the view with the given pointer ID. The callback will be involved.
     * This will put us into the "dragging" state. If we've already captured this view with
     * this pointer this method will immediately return true without consulting the callback.
     *
     * @param pointerId Pointer to capture with
     * @return true if capture was successful
     */
    private boolean trySwipe(int pointerId, boolean settling, float downX, float downY, float dx, float dy) {

        return trySwipe(pointerId, settling, downX, downY, dx, dy, true);
    }

    private boolean trySwipe(int pointerId, boolean settling, float downX, float downY, float dx, float dy, boolean touchMode) {
        if (mActivePointerId == pointerId) {
            // Already done!
            return true;
        }
        boolean swipe;
        if (settling || mDragState == STATE_SETTLING) {
            swipe = mSwipeConsumer.tryAcceptSettling(pointerId, downX, downY);
        } else {
            swipe = mSwipeConsumer.tryAcceptMoving(pointerId, downX, downY, dx, dy);
        }
        if (swipe) {
            mActivePointerId = pointerId;
            float initX = 0;
            float initY = 0;
            if (pointerId >= 0 && pointerId < mInitialMotionX.length && pointerId < mInitialMotionY.length) {
                initX = mInitialMotionX[pointerId];
                initY = mInitialMotionY[pointerId];
            }
            mSwipeConsumer.onSwipeAccepted(pointerId, settling, initX, initY);
            mClampedDistanceX = mSwipeConsumer.clampDistanceHorizontal(0, 0);
            mClampedDistanceY = mSwipeConsumer.clampDistanceVertical(0, 0);
            setDragState(touchMode ? STATE_DRAGGING : STATE_NONE_TOUCH);
            return true;
        }
        return false;
    }

    /**
     * Check if this event as provided to the parent view's onInterceptTouchEvent should
     * cause the parent to intercept the touch event stream.
     *
     * @param ev MotionEvent provided to onInterceptTouchEvent
     * @return true if the parent view should return true from onInterceptTouchEvent
     */
    public boolean shouldInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        final int actionIndex = ev.getActionIndex();

        if (action == MotionEvent.ACTION_DOWN) {
            // Reset things for a new event stream, just in case we didn't get
            // the whole previous stream.
            cancel();
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                final int pointerId = ev.getPointerId(0);
                saveInitialMotion(x, y, pointerId);

                // Catch a settling view if possible.
                if (mDragState == STATE_SETTLING || mDragState == STATE_NONE_TOUCH) {
                    trySwipe(pointerId, true, x, y, 0, 0);
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                final int pointerId = ev.getPointerId(actionIndex);
                final float x = ev.getX(actionIndex);
                final float y = ev.getY(actionIndex);

                saveInitialMotion(x, y, pointerId);

                // A SwipeHelper can only manipulate one view at a time.
                if (mDragState == STATE_SETTLING || mDragState == STATE_NONE_TOUCH) {
                    // Catch a settling view if possible.
                    trySwipe(pointerId, true, x, y, 0, 0);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mInitialMotionX == null || mInitialMotionY == null) {
                    break;
                }

                // First to cross a touch slop over a draggable view wins. Also report edge drags.
                final int pointerCount = ev.getPointerCount();
                for (int i = 0; i < pointerCount; i++) {
                    final int pointerId = ev.getPointerId(i);

                    // If pointer is invalid then skip the ACTION_MOVE.
                    if (!isValidPointerForActionMove(pointerId)) {
                        continue;
                    }

                    final float x = ev.getX(i);
                    final float y = ev.getY(i);
                    float downX = mInitialMotionX[pointerId];
                    float downY = mInitialMotionY[pointerId];
                    final float dx = x - downX;
                    final float dy = y - downY;

                    final boolean pastSlop = checkTouchSlop(dx, dy);
                    if (pastSlop) {
                        final int hDragRange = mSwipeConsumer.getHorizontalRange(dx, dy);
                        final int vDragRange = mSwipeConsumer.getVerticalRange(dx, dy);
                        if (hDragRange == 0 && vDragRange == 0) {
                            continue;
                        }
                    }
                    if (pastSlop && trySwipe(pointerId, false, downX, downY, dx, dy)) {
                        break;
                    }
                }
                saveLastMotion(ev);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerId = ev.getPointerId(actionIndex);
                clearMotionHistory(pointerId);
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                cancel();
                break;
            }
            default:
        }

        return mDragState == STATE_DRAGGING;
    }

    /**
     * Process a touch event received by the parent view. This method will dispatch callback events
     * as needed before returning. The parent view's onTouchEvent implementation should call this.
     *
     * @param ev The touch event received by the parent view
     */
    public void processTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        final int actionIndex = ev.getActionIndex();

        if (action == MotionEvent.ACTION_DOWN && mDragState != STATE_DRAGGING) {
            // Reset things for a new event stream, just in case we didn't get
            // the whole previous stream.
            cancel();
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                final int pointerId = ev.getPointerId(0);

                saveInitialMotion(x, y, pointerId);

                // Since the parent is already directly processing this touch event,
                // there is no reason to delay for a slop before dragging.
                // Start immediately if possible.
                if (mDragState != STATE_DRAGGING) {
                    trySwipe(pointerId, mDragState == STATE_SETTLING || mDragState == STATE_NONE_TOUCH, x, y, 0, 0);
                }

                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                final int pointerId = ev.getPointerId(actionIndex);
                final float x = ev.getX(actionIndex);
                final float y = ev.getY(actionIndex);
                saveInitialMotion(x, y, pointerId);
                if (mDragState == STATE_DRAGGING) {
                    trySwipe(pointerId, true, x, y, 0, 0);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mDragState == STATE_DRAGGING) {
                    // If pointer is invalid then skip the ACTION_MOVE.
                    if (!isValidPointerForActionMove(mActivePointerId)) {
                        break;
                    }

                    final int index = ev.findPointerIndex(mActivePointerId);
                    if (index < 0) {
                        break;
                    }
                    final float x = ev.getX(index);
                    final float y = ev.getY(index);
                    final int idx = (int) (x - mLastMotionX[mActivePointerId]);
                    final int idy = (int) (y - mLastMotionY[mActivePointerId]);

                    dragTo(mClampedDistanceX + idx, mClampedDistanceY + idy, idx, idy);

                    saveLastMotion(ev);
                } else {
                    // Check to see if any pointer is now over a draggable view.
                    final int pointerCount = ev.getPointerCount();
                    for (int i = 0; i < pointerCount; i++) {
                        final int pointerId = ev.getPointerId(i);

                        // If pointer is invalid then skip the ACTION_MOVE.
                        if (!isValidPointerForActionMove(pointerId)) {
                            continue;
                        }

                        final float x = ev.getX(i);
                        final float y = ev.getY(i);
                        float downX = mInitialMotionX[pointerId];
                        float downY = mInitialMotionY[pointerId];
                        final float dx = x - downX;
                        final float dy = y - downY;

                        if (checkTouchSlop(dx, dy) && trySwipe(pointerId, false, downX, downY, dx, dy)) {
                            break;
                        }
                    }
                    saveLastMotion(ev);
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerId = ev.getPointerId(actionIndex);
                if (mDragState == STATE_DRAGGING && pointerId == mActivePointerId) {
                    // Try to find another pointer that's still holding on to the captured view.
                    int newActivePointer = INVALID_POINTER;
                    final int pointerCount = ev.getPointerCount();
                    for (int i = 0; i < pointerCount; i++) {
                        final int id = ev.getPointerId(i);
                        if (id == mActivePointerId) {
                            // This one's going away, skip.
                            continue;
                        }
                        if (!isValidPointerForActionMove(id)) {
                            continue;
                        }

                        if (trySwipe(id, true, mInitialMotionX[id], mInitialMotionX[id], 0, 0)) {
                            newActivePointer = mActivePointerId;
                            break;
                        }
                    }

                    if (newActivePointer == INVALID_POINTER) {
                        // We didn't find another pointer still touching the view, release it.
                        releaseViewForPointerUp();
                    }
                }
                clearMotionHistory(pointerId);
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mDragState == STATE_DRAGGING) {
                    releaseViewForPointerUp();
                }
                cancel();
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mDragState == STATE_DRAGGING) {
                    dispatchViewReleased(0, 0);
                }
                cancel();
                break;
            }
            default:
        }
    }

    /**
     * Check if we've crossed a reasonable touch slop for the given child view.
     * If the child cannot be dragged along the horizontal or vertical axis, motion
     * along that axis will not count toward the slop check.
     *
     * @param dx Motion since initial position along X axis
     * @param dy Motion since initial position along Y axis
     * @return true if the touch slop has been crossed
     */
    private boolean checkTouchSlop(float dx, float dy) {
        final boolean checkHorizontal = mSwipeConsumer.getHorizontalRange(dx, dy) > 0;
        final boolean checkVertical = mSwipeConsumer.getVerticalRange(dx, dy) > 0;

        if (checkHorizontal && checkVertical) {
            return dx * dx + dy * dy > mTouchSlop * mTouchSlop;
        } else if (checkHorizontal) {
            return Math.abs(dx) > mTouchSlop;
        } else if (checkVertical) {
            return Math.abs(dy) > mTouchSlop;
        }
        return false;
    }

    private void releaseViewForPointerUp() {
        mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
        final float xvel = clampMag(
                mVelocityTracker.getXVelocity(mActivePointerId),
                mMinVelocity, mMaxVelocity);
        final float yvel = clampMag(
                mVelocityTracker.getYVelocity(mActivePointerId),
                mMinVelocity, mMaxVelocity);
        dispatchViewReleased(xvel, yvel);
    }

    public boolean nestedScrollingDrag(int dx, int dy, int[] consumed, boolean fly) {
        if (mDragState == STATE_IDLE && !trySwipe(fly ? POINTER_NESTED_FLY : POINTER_NESTED_SCROLL, false, 0, 0, dx, dy, false)) {
            return false;
        }
        int clampedX = 0, clampedY = 0;
        if (mClampedDistanceX != 0 || dx != 0) {
            clampedX = mSwipeConsumer.clampDistanceHorizontal(mClampedDistanceX + dx, dx);
            consumed[0] = mClampedDistanceX - clampedX;
        }
        if (mClampedDistanceY != 0 || dy != 0) {
            clampedY = mSwipeConsumer.clampDistanceVertical(mClampedDistanceY + dy, dy);
            consumed[1] = mClampedDistanceY - clampedY;
        }
        if (mClampedDistanceX == 0 && mClampedDistanceY == 0 && consumed[0] == 0 && consumed[1] == 0) {
            mActivePointerId = INVALID_POINTER;
            setDragState(STATE_IDLE);
            return false;
        } else {
            dragTo(clampedX, clampedY, -consumed[0], -consumed[1]);
            return true;
        }
    }

    public void nestedScrollingRelease() {
        if (mDragState == STATE_NONE_TOUCH) {
            dispatchViewReleased(0, 0);
        }
    }

    private void dragTo(int x, int y, int dx, int dy) {
        int clampedX = x;
        int clampedY = y;
        final int oldX = mClampedDistanceX;
        final int oldY = mClampedDistanceY;
        if (dx != 0) {
            clampedX = mSwipeConsumer.clampDistanceHorizontal(x, dx);
            mClampedDistanceX = clampedX;
        }
        if (dy != 0) {
            clampedY = mSwipeConsumer.clampDistanceVertical(y, dy);
            mClampedDistanceY = clampedY;
        }

        if (dx != 0 || dy != 0) {
            final int clampedDx = clampedX - oldX;
            final int clampedDy = clampedY - oldY;
            mSwipeConsumer.onSwipeDistanceChanged(clampedX, clampedY, clampedDx, clampedDy);
        }
    }

    public SwipeConsumer getSwipeConsumer() {
        return mSwipeConsumer;
    }

    private boolean isValidPointerForActionMove(int pointerId) {
        if (!isPointerDown(pointerId)) {
            Log.e(TAG, "Ignoring pointerId=" + pointerId + " because ACTION_DOWN was not received "
                    + "for this pointer before ACTION_MOVE. It likely happened because "
                    + " SwipeHelper did not receive all the events in the event stream.");
            return false;
        }
        return true;
    }

    public int getMaxSettleDuration() {
        return maxSettleDuration;
    }

    public void setMaxSettleDuration(int maxSettleDuration) {
        this.maxSettleDuration = maxSettleDuration;
    }
}

