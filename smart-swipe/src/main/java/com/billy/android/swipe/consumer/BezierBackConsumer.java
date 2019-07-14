package com.billy.android.swipe.consumer;

import android.content.Context;
import android.graphics.*;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.internal.SwipeHelper;
import com.billy.android.swipe.internal.ViewCompat;

import static com.billy.android.swipe.SmartSwipe.ensureBetween;

/**
 * swipe to back with bezier consumer
 * thanks:
 *  1. https://github.com/qinci/AndroidSlideBack
 *  2. https://github.com/Blankj/SwipePanel
 * @author billy.qi
 */
public class BezierBackConsumer extends SwipeConsumer {
    protected float mThickness, mLastThickness;

    protected final Paint mPaint = new Paint();
    protected final Path mPath = new Path();
    protected final PointF mPathStart = new PointF();
    protected final PointF mPathControl1 = new PointF();
    protected final PointF mPathControl2 = new PointF();
    protected final PointF mPathControl = new PointF();
    protected final PointF mPathControl3 = new PointF();
    protected final PointF mPathControl4 = new PointF();
    protected final PointF mPathEnd = new PointF();

    protected final Paint mPaintArrow = new Paint();
    protected final Path mPathArrow = new Path();
    protected Rect mDrawRect = new Rect();
    protected int mArrowSize;
    protected int mSize;
    protected int mColor;
    protected int mArrowColor = 0xFFF2F2F2;
    protected boolean mCenter;

    public BezierBackConsumer() {
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaintArrow.setAntiAlias(true);
        mPaintArrow.setStyle(Paint.Style.STROKE);
        mPaintArrow.setColor(mArrowColor);
        mPaintArrow.setStrokeWidth(4);
        mPaintArrow.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void onAttachToWrapper(SmartSwipeWrapper wrapper, SwipeHelper swipeHelper) {
        Context context = wrapper.getContext();
        if (mSize == 0) {
            mSize = SmartSwipe.dp2px(200, context);
        }
        if (mArrowSize == 0) {
            mArrowSize = SmartSwipe.dp2px(4, context);
        }
        if (mOpenDistance == 0) {
            mOpenDistance = SmartSwipe.dp2px(30, context);
        }
        super.onAttachToWrapper(wrapper, swipeHelper);
    }

    @Override
    public void onSwipeAccepted(int activePointerId, boolean settling, float initialMotionX, float initialMotionY) {
        super.onSwipeAccepted(activePointerId, settling, initialMotionX, initialMotionY);
        // init bezier positions
        boolean left = mDirection == DIRECTION_LEFT;
        boolean right = mDirection == DIRECTION_RIGHT;
        boolean top = mDirection == DIRECTION_TOP;
        boolean horizontal = left || right;

        int thickness = mOpenDistance;
        boolean isCenter = settling || isCenter();

        int halfSize = Math.min(mSize, horizontal ? mHeight : mWidth) >> 1;
        int quarterSize =  halfSize >> 2;
        float middleX = horizontal ? (left ? 0 : mWidth) : (isCenter ? mWidth >> 1 : ensureBetween(initialMotionX, halfSize, mWidth - halfSize));
        float middleY = !horizontal ? (top ? 0 : mHeight) : (isCenter ? mHeight >> 1 : ensureBetween(initialMotionY, halfSize, mHeight - halfSize));

        mPathStart.set(horizontal ? middleX : middleX - halfSize, horizontal ? middleY - halfSize : middleY);
        mPathControl1.set(horizontal ? middleX : middleX - quarterSize, horizontal ? middleY - quarterSize : middleY);
        mPathControl2.set(mPathControl1.x, mPathControl1.y);
        mPathControl.set(middleX, middleY);
        mPathControl3.set(horizontal ? middleX : middleX + quarterSize, horizontal ? middleY + quarterSize : middleY);
        mPathControl4.set(mPathControl3.x, mPathControl3.y);
        mPathEnd.set(horizontal ? middleX : middleX + halfSize, horizontal ? middleY + halfSize : middleY);

        mDrawRect.left = horizontal ? left ? 0 : mWidth - thickness : (int) mPathStart.x;
        mDrawRect.top = horizontal ? (int) mPathStart.y : top ? 0 : mHeight - thickness;
        mDrawRect.right = horizontal ? left ? thickness : mWidth : (int) mPathEnd.x;
        mDrawRect.bottom = horizontal ? (int) mPathEnd.y : top ? thickness : mHeight;

    }

    @Override
    public void onDisplayDistanceChanged(int distanceXToDisplay, int distanceYToDisplay, int dx, int dy) {
        if ((mDirection & DIRECTION_HORIZONTAL) != 0) {
            mThickness = Math.abs(distanceXToDisplay);
        } else if ((mDirection & DIRECTION_VERTICAL) != 0) {
            mThickness = Math.abs(distanceYToDisplay);
        } else {
            return;
        }
        if (mThickness != mLastThickness) {
            ViewCompat.postInvalidateOnAnimation(mWrapper);
        }
        mLastThickness = mThickness;
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        switch (mDirection) {
            case DIRECTION_LEFT:    mPathControl3.x = mPathControl2.x = mPathControl.x = mThickness; break;
            case DIRECTION_RIGHT:   mPathControl3.x = mPathControl2.x = mPathControl.x = mWidth - mThickness; break;
            case DIRECTION_TOP:     mPathControl3.y = mPathControl2.y = mPathControl.y = mThickness; break;
            case DIRECTION_BOTTOM:  mPathControl3.y = mPathControl2.y = mPathControl.y = mHeight - mThickness; break;
            default: break;
        }
        float percent = getProgress();
        int alpha = (int) (0xFF * ensureBetween(percent, 0.2F, 0.8F));
        mPaint.setAlpha(alpha);
        mPath.reset();
        mPath.moveTo(mPathStart.x, mPathStart.y);
        mPath.cubicTo(mPathControl1.x, mPathControl1.y, mPathControl2.x, mPathControl2.y, mPathControl.x, mPathControl.y);
        mPath.cubicTo(mPathControl3.x, mPathControl3.y, mPathControl4.x, mPathControl4.y, mPathEnd.x, mPathEnd.y);
        canvas.drawPath(mPath, mPaint);
        drawArrow(canvas, mPaintArrow, mThickness, percent);
    }

    /**
     * draw an arrow by default, subclass can draw anything else
     * @param canvas canvas
     * @param paint Paint
     * @param curThickness current thickness of BezierBackConsumer
     * @param percent percent of full swipe
     */
    protected void drawArrow(Canvas canvas, Paint paint, float curThickness, float percent) {
        percent = ensureBetween(percent, 0F, 1F);
        float startX, startY, middleX, middleY, endX, endY;
        float arrowAddition = percent < 0.5 ? 0 : (percent - 0.5f) * mArrowSize * 2;
        float offset = curThickness / 2;
        switch (mDirection) {
            case DIRECTION_LEFT:
            case DIRECTION_RIGHT:
                boolean left = mDirection == DIRECTION_LEFT;
                middleX = left ? offset : mWidth - offset;
                middleY = mPathControl.y;
                startX = endX = middleX + arrowAddition * (left ? 1 : -1);
                startY = mPathControl.y - mArrowSize;
                endY = mPathControl.y + mArrowSize;
                break;
            case DIRECTION_TOP:
            case DIRECTION_BOTTOM:
                boolean top = mDirection == DIRECTION_TOP;
                middleY = top ? offset : mHeight - offset;
                middleX = mPathControl.x;
                startY = endY = middleY + arrowAddition * (top ? 1 : -1);
                startX = mPathControl.x - mArrowSize;
                endX = mPathControl.x + mArrowSize;
                break;
            default:
                return;
        }
        mPaintArrow.setAlpha((int) (0xFF * percent));
        mPathArrow.reset();
        mPathArrow.moveTo(startX, startY);
        mPathArrow.lineTo(middleX, middleY);
        mPathArrow.lineTo(endX, endY);
        canvas.drawPath(mPathArrow, paint);
    }

    public BezierBackConsumer setSize(int size) {
        this.mSize = size;
        return this;
    }

    protected int getSize() {
        return mSize;
    }

    public int getColor() {
        return mColor;
    }

    public BezierBackConsumer setColor(int color) {
        this.mColor = color;
        mPaint.setColor(color);
        return this;
    }

    public int getArrowColor() {
        return mArrowColor;
    }

    public BezierBackConsumer setArrowColor(int color) {
        this.mArrowColor = color;
        mPaintArrow.setColor(color);
        return this;
    }

    public boolean isCenter() {
        return mCenter;
    }

    public BezierBackConsumer setCenter(boolean center) {
        this.mCenter = center;
        return this;
    }
}
