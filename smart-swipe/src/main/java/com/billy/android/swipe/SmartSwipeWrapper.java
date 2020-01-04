package com.billy.android.swipe;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.billy.android.swipe.internal.SwipeHelper;
import com.billy.android.swipe.internal.ViewCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.billy.android.swipe.SwipeConsumer.DIRECTION_BOTTOM;
import static com.billy.android.swipe.SwipeConsumer.DIRECTION_LEFT;
import static com.billy.android.swipe.SwipeConsumer.DIRECTION_NONE;
import static com.billy.android.swipe.SwipeConsumer.DIRECTION_RIGHT;
import static com.billy.android.swipe.SwipeConsumer.DIRECTION_TOP;
import static com.billy.android.swipe.SwipeConsumer.PROGRESS_OPEN;

/**
 * a wrapper to wrap the content view, handle motion events to do swipe business by {@link SwipeHelper} and {@link SwipeConsumer}
 * @author billy.qi
 */
public class SmartSwipeWrapper extends ViewGroup {

    protected SwipeHelper mHelper;
    protected View mContentView;
    protected final List<SwipeHelper> mHelpers = new LinkedList<>();
    protected final List<SwipeConsumer> mConsumers = new LinkedList<>();
    protected boolean mInflateFromXml;
    protected boolean mNestedInProgress;
    protected boolean mIsNestedScrollingEnabled = true;

    public SmartSwipeWrapper(Context context) {
        this(context, null, 0);
    }

    public SmartSwipeWrapper(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartSwipeWrapper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SmartSwipeWrapper(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    protected void init() {
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mHelper = null;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mNestedInProgress) {
            if (mHelper != null) {
                return mHelper.shouldInterceptTouchEvent(ev);
            } else {
                for (SwipeHelper helper : mHelpers) {
                    if (helper.shouldInterceptTouchEvent(ev)) {
                        mHelper = helper;
                        return true;
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mNestedInProgress) {
            if (mHelper != null) {
                mHelper.processTouchEvent(event);
            } else {
                for (SwipeHelper helper : mHelpers) {
                    helper.processTouchEvent(event);
                    if (helper.getDragState() == SwipeHelper.STATE_DRAGGING) {
                        mHelper = helper;
                        return true;
                    }
                }
            }
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        for (SwipeConsumer consumer : mConsumers) {
            if (consumer != null) {
                consumer.dispatchDraw(canvas);
            }
        }
    }

    public void drawChild(Canvas canvas, View child) {
        drawChild(canvas, child, getDrawingTime());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (SwipeConsumer consumer : mConsumers) {
            if (consumer != null) {
                consumer.onDraw(canvas);
            }
        }
    }

    private final ArrayList<View> mMatchParentChildren = new ArrayList<>(1);
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int count = getChildCount();
        final boolean measureMatchParentChildren =
                MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                        MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
        mMatchParentChildren.clear();

        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final ViewGroup.LayoutParams lp = child.getLayoutParams();
            final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
            final int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
            maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
            childState = combineMeasuredStates(childState, child.getMeasuredState());
            if (measureMatchParentChildren) {
                if (lp.width == LayoutParams.MATCH_PARENT ||
                        lp.height == LayoutParams.MATCH_PARENT) {
                    mMatchParentChildren.add(child);
                }
            }
        }

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));

        count = mMatchParentChildren.size();
        if (count > 1) {
            for (int i = 0; i < count; i++) {
                final View child = mMatchParentChildren.get(i);
                final ViewGroup.LayoutParams lp = child.getLayoutParams();

                final int childWidthMeasureSpec;
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    final int width = Math.max(0, getMeasuredWidth());
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
                }

                final int childHeightMeasureSpec;
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    final int height = Math.max(0, getMeasuredHeight());
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
                }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
        for (SwipeConsumer consumer : mConsumers) {
            if (consumer != null) {
                consumer.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        boolean layoutByConsumer = false;
        if (mHelper != null) {
            layoutByConsumer = mHelper.getSwipeConsumer().onLayout(changed, left, top, right, bottom);
        } else {
            for (SwipeConsumer consumer : mConsumers) {
                if (consumer != null && consumer.onLayout(changed, left, top, right, bottom)) {
                    layoutByConsumer = true;
                }
            }
        }
        if (!layoutByConsumer) {
            if (mContentView != null) {
                mContentView.layout(0, 0, mContentView.getMeasuredWidth(), mContentView.getMeasuredHeight());
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //compat for xml usage
        mInflateFromXml = true;
        int childCount = getChildCount();
        if (childCount > 0 && mContentView == null) {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
                if (layoutParams instanceof LayoutParams) {
                    final int gravity = ((LayoutParams) layoutParams).gravity;
                    if (gravity == LayoutParams.UNSPECIFIED_GRAVITY) {
                        setContentView(child);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void computeScroll() {
        if (!mHelpers.isEmpty() ) {
            boolean shouldContinue = false;
            for (SwipeHelper helper : mHelpers) {
                if (helper.continueSettling()) {
                    shouldContinue = true;
                }
            }
            if (shouldContinue) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    public <T extends SwipeConsumer> T addConsumer(T consumer) {
        if (consumer != null) {
            this.mConsumers.add(consumer);
            SwipeHelper helper = consumer.getSwipeHelper();
            if (helper == null) {
                helper = SwipeHelper.create(this, consumer.getSensitivity(), consumer, consumer.getInterpolator());
            }
            consumer.onAttachToWrapper(this, helper);
            mHelpers.add(helper);
        }
        return consumer;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (SwipeConsumer consumer : mConsumers) {
            if (consumer.isAutoCloseOnWrapperDetachedFromWindow()) {
                consumer.close();
            }
        }
    }

    public SmartSwipeWrapper removeAllConsumers() {
        Iterator<SwipeConsumer> iterator = mConsumers.iterator();
        while (iterator.hasNext()) {
            SwipeConsumer consumer = iterator.next();
            iterator.remove();
            if (consumer != null) {
                consumer.onDetachFromWrapper();
                SwipeHelper swipeHelper = consumer.getSwipeHelper();
                mHelpers.remove(swipeHelper);
                if (mHelper == swipeHelper) {
                    mHelper = null;
                }
            }
        }
        return this;
    }

    public SmartSwipeWrapper removeConsumer(SwipeConsumer consumer) {
        boolean removed = mConsumers.remove(consumer);
        if (removed) {
            consumer.onDetachFromWrapper();
            SwipeHelper swipeHelper = consumer.getSwipeHelper();
            mHelpers.remove(swipeHelper);
            if (mHelper == swipeHelper) {
                mHelper = null;
            }
        }
        return this;
    }

    public SwipeConsumer getConsumerByType(Class<? extends SwipeConsumer> clazz) {
        for (SwipeConsumer consumer : mConsumers) {
            if (consumer != null && consumer.getClass() == clazz) {
                return consumer;
            }
        }
        return null;
    }

    public void setContentView(View contentView) {
        if (contentView == null || this.mContentView == contentView) {
            return;
        }
        this.mContentView = contentView;
        if (contentView.getParent() == null) {
            addView(contentView);
        }
    }

    public View getContentView() {
        return mContentView;
    }

    public List<SwipeConsumer> getAllConsumers() {
        return mConsumers;
    }

    public SmartSwipeWrapper enableDirection(int direction) {
        return enableDirection(direction, true);
    }

    public SmartSwipeWrapper enableDirection(int direction, boolean enable) {
        for (SwipeConsumer consumer : mConsumers) {
            consumer.enableDirection(direction, enable);
        }
        return this;
    }

    public boolean isInflateFromXml() {
        return mInflateFromXml;
    }

    public void consumeInflateFromXml() {
        this.mInflateFromXml = false;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        } else if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    public static class LayoutParams extends MarginLayoutParams {
        /**
         * Value for {@link #gravity} indicating that a gravity has not been
         * explicitly specified.
         */
        public static final int UNSPECIFIED_GRAVITY = 0;

        /**
         * The gravity to apply with the View to which these layout parameters
         * are associated.
         */
        public int gravity = UNSPECIFIED_GRAVITY;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.SmartSwipeWrapper_Layout);
            gravity = a.getInt(R.styleable.SmartSwipeWrapper_Layout_swipe_gravity, UNSPECIFIED_GRAVITY);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * Creates a new set of layout parameters with the specified width, height
         * and weight.
         *
         * @param width the width, either {@link #MATCH_PARENT},
         *              {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param height the height, either {@link #MATCH_PARENT},
         *               {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param gravity the gravity
         *
         * @see android.view.Gravity
         */
        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height, margin values, and
         * gravity of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(LayoutParams source) {
            super(source);
            this.gravity = source.gravity;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setNestedScrollingEnabled(mIsNestedScrollingEnabled);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mIsNestedScrollingEnabled = enabled;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.setNestedScrollingEnabled(enabled);
        }
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.isNestedScrollingEnabled();
        }
        return false;
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return onStartNestedScroll(child, target, nestedScrollAxes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onStopNestedScroll(View child) {
        onStopNestedScroll(child, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH);
    }

    /////////////////////////////////////////
    //
    // support for NestedScrollingParent2
    //
    /////////////////////////////////////////

    public boolean onStartNestedScroll(View child, View target, int axes, int type) {
        boolean vertical = (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        boolean horizontal = (axes & ViewCompat.SCROLL_AXIS_HORIZONTAL) != 0;
        for (SwipeConsumer consumer : mConsumers) {
            int direction = consumer.getDirection();
            boolean handle;
            if (direction != DIRECTION_NONE) {
                //only handle the same axes as current swipe direction
                handle = horizontal && (direction == DIRECTION_LEFT || direction == DIRECTION_RIGHT);
                handle = handle || vertical && (direction == DIRECTION_TOP || direction == DIRECTION_BOTTOM);
            } else {
                handle = horizontal && (consumer.isLeftEnable() || consumer.isRightEnable());
                handle = handle || vertical && (consumer.isTopEnable() || consumer.isBottomEnable());
            }
            if (handle) {
                startNestedScroll(axes, type);
                return true;
            }
        }
        return false;
    }

    private static final int NESTED_TYPE_INVALID = -1;
    protected int mCurNestedType = NESTED_TYPE_INVALID;
    protected boolean mNestedFlyConsumed;

    public void onNestedScrollAccepted(View child, View target, int axes, int type) {
        mNestedInProgress = true;
        mNestedFlyConsumed = false;
        flyToOpen = flyToClose = null;
        mCurNestedType = type;
        helperOnNestedScrollAccepted(child, target, axes, type);
    }

    public void onStopNestedScroll(View target, int type) {
        mNestedInProgress = false;
        helperOnStopNestedScroll(target, type);
        if (type == mCurNestedType) {
            mCurNestedType = NESTED_TYPE_INVALID;
            if (mHelper != null) {
                mHelper.nestedScrollingRelease();
            }
        }
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.onNestedPreFling(target, velocityX, velocityY);
        }
        return false;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.onNestedFling(target, velocityX, velocityY, consumed);
        }
        return false;
    }

    protected int[] mParentOffsetInWindow = new int[2];

    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        helperOnNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
        dxUnconsumed += mParentOffsetInWindow[0];
        dyUnconsumed += mParentOffsetInWindow[1];
        if (dxUnconsumed != 0 || dyUnconsumed != 0) {
            if (type == ViewCompat.TYPE_NON_TOUCH) {
                //fling nested scroll has not been consumed
                requestDisallowInterceptTouchEvent(false);
            }
            int[] consumed = new int[2];
            wrapperNestedScroll(dxUnconsumed, dyUnconsumed, consumed, type);
        }
    }

    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
        int consumedX = 0, consumedY = 0;
        boolean helperConsumed = false;
        if (mHelper == null || mHelper.getSwipeConsumer().getProgress() == 0) {
            helperConsumed = true;
            Arrays.fill(consumed, 0);
            helperOnNestedPreScroll(target, dx, dy, consumed, type);
            consumedX += consumed[0];
            consumedY += consumed[1];
        }
        if (mHelper != null && mHelper.getSwipeConsumer().getDirection() != DIRECTION_NONE) {
            Arrays.fill(consumed, 0);
            wrapperNestedScroll(dx - consumedX, dy - consumedY, consumed, type);
            consumedX -= consumed[0];
            consumedY -= consumed[1];
        }
        if (!helperConsumed) {
            Arrays.fill(consumed, 0);
            helperOnNestedPreScroll(target, dx - consumedX, dy - consumedY, consumed, type);
            consumedX += consumed[0];
            consumedY += consumed[1];
        }
        consumed[0] = consumedX;
        consumed[1] = consumedY;
    }

    private Boolean flyToOpen, flyToClose;


    private void wrapperNestedScroll(int dxUnconsumed, int dyUnconsumed, int[] consumed, int type) {
        if (mCurNestedType == NESTED_TYPE_INVALID) {
            //resolve problem: miss a call of: onStartNestedScroll(type = 1) and onNestedScrollAccepted(type=0)
            // time line like this:
            //  onStartNestedScroll(type=0)
            //  onNestedScrollAccepted(type=0)
            //  some onNestedPreScroll/onNestedScroll(type=0)...
            //  onStopNestedScroll(type=0)
            //  some onNestedPreScroll/onNestedScroll(type=1)...
            //  onStopNestedScroll(type=1)
            mCurNestedType = type;
            mNestedFlyConsumed = false;
            flyToOpen = flyToClose = null;
        }
        boolean fly = type == ViewCompat.TYPE_NON_TOUCH;
        if (mHelper != null) {
            SwipeConsumer consumer = mHelper.getSwipeConsumer();
            float maxProgress = PROGRESS_OPEN + consumer.getOverSwipeFactor();
            if (fly) {
                if (flyToOpen == null) {
                    //scroll trending:
                    // ↓: y < 0, ↑: y > 0
                    // →: x < 0, ←: x > 0
                    switch (consumer.getDirection()) {
                        case DIRECTION_TOP:     flyToOpen = dyUnconsumed < 0; flyToClose = dyUnconsumed > 0; if(dyUnconsumed == 0) return; break;
                        case DIRECTION_BOTTOM:  flyToOpen = dyUnconsumed > 0; flyToClose = dyUnconsumed < 0; if(dyUnconsumed == 0) return; break;
                        case DIRECTION_LEFT:    flyToOpen = dxUnconsumed < 0; flyToClose = dxUnconsumed > 0; if(dxUnconsumed == 0) return; break;
                        case DIRECTION_RIGHT:   flyToOpen = dxUnconsumed > 0; flyToClose = dxUnconsumed < 0; if(dxUnconsumed == 0) return; break;
                        default: flyToOpen = flyToClose = false;
                    }
                }
                if (!mNestedFlyConsumed) {
                    mHelper.nestedScrollingDrag(-dxUnconsumed, -dyUnconsumed, consumed, true);
                    if (flyToOpen && consumer.getProgress() >= maxProgress || flyToClose && consumer.getProgress() <= 0) {
                        mNestedFlyConsumed = true;
                        mHelper.nestedScrollingRelease();
                    }
                }
            } else {
                mHelper.nestedScrollingDrag(-dxUnconsumed, -dyUnconsumed, consumed, false);
                if (consumer.getProgress() >= maxProgress || consumer.getProgress() <= 0) {
                    mHelper = null;
                }
            }
        } else {
            for (SwipeHelper helper : mHelpers) {
                if (helper != null) {
                    //try to determined which SwipeHelper will handle this fake drag via nested scroll
                    if (helper.nestedScrollingTrySwipe(-dxUnconsumed, -dyUnconsumed, type == ViewCompat.TYPE_NON_TOUCH)) {
                        mHelper = helper;
                        break;
                    }
                }
            }
        }
    }

    protected void helperOnNestedScrollAccepted(View child, View target, int axes, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onNestedScrollAccepted(child, target, axes);
        }
    }

    protected void helperOnNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onNestedPreScroll(target, dx, dy, consumed);
        }
    }

    protected void helperOnNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow);
        }
    }

    protected void helperOnStopNestedScroll(View target, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onStopNestedScroll(target);
        }
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return startNestedScroll(axes, ViewCompat.TYPE_TOUCH);
    }


    public boolean startNestedScroll(int axes, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.startNestedScroll(axes);
        }
        return false;
    }

    @Override
    public void stopNestedScroll() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH);
    }

    public void stopNestedScroll(int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.stopNestedScroll();
        }
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
    }

    public boolean hasNestedScrollingParent(int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.hasNestedScrollingParent();
        }
        return false;
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow, ViewCompat.TYPE_TOUCH);
    }

    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.dispatchNestedScroll(dxConsumed, dyConsumed,
                    dxUnconsumed, dyUnconsumed, offsetInWindow);
        }
        return false;
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH);
    }

    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
        }
        return false;
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.dispatchNestedFling(velocityX, velocityY, consumed);
        }
        return false;
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.dispatchNestedPreFling(velocityX, velocityY);
        }
        return false;
    }
}
