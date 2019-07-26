package com.billy.android.swipe.ext.refresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import com.billy.android.swipe.SmartSwipeRefresh;
import com.wuyr.arrowdrawable.ArrowDrawable;

/**
 *
 * @since v1.0.3
 * @author billy.qi
 */
public class ArrowHeader extends View implements SmartSwipeRefresh.SmartSwipeRefreshHeader {

    private ArrowDrawable mDrawable;
    private IArrowInitializer mInitializer;
    private int bowColor;
    private int arrowColor;
    private int stringColor;
    private int lineColor;
    private int bowLength;

    public static interface IArrowInitializer {
        void onArrowInit(ArrowHeader arrowHeader, ArrowDrawable arrowDrawable);
    }

    public ArrowHeader(Context context) {
        this(context, null);
    }

    public ArrowHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArrowHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ArrowHeader, defStyleAttr, 0);
        bowColor = a.getColor(R.styleable.ArrowHeader_bowColor, Color.GRAY);
        arrowColor = a.getColor(R.styleable.ArrowHeader_arrowColor, Color.GRAY);
        stringColor = a.getColor(R.styleable.ArrowHeader_stringColor, Color.GRAY);
        lineColor = a.getColor(R.styleable.ArrowHeader_lineColor, Color.GRAY);
        bowLength = a.getDimensionPixelSize(R.styleable.ArrowHeader_bowLength, 0);
        a.recycle();
    }

    @Override
    public View getView() {
        return this;
    }

    boolean measured;
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!measured) {
            measured = true;
            mDrawable = ArrowDrawable.create(this, getMeasuredWidth(), getMeasuredHeight(),
                    (int) (bowLength > 0 ? bowLength : getMeasuredWidth() * .3F));
            if (bowColor != 0) {
                mDrawable.setBowColor(bowColor);
            }
            if (arrowColor != 0) {
                mDrawable.setArrowColor(arrowColor);
            }
            if (stringColor != 0) {
                mDrawable.setStringColor(stringColor);
            }
            if (lineColor != 0) {
                mDrawable.setLineColor(lineColor);
            }
            mDrawable.reset();
            if (mInitializer != null) {
                mInitializer.onArrowInit(this, mDrawable);
            }
        }
    }

    @Override
    public void onInit(boolean horizontal) {
    }

    @Override
    public void onStartDragging() {
    }

    @Override
    public void onReset() {
        if (mDrawable != null) {
            mDrawable.reset();
        }
    }

    @Override
    public void onProgress(boolean dragging, float progress) {
        float value;
        if (progress <= .5F) {
            value = progress / 2;
        } else if (progress <= .75F) {
            value = progress - .25F;
        } else {
            value = (progress - .5F) * 2;
        }
        mDrawable.setProgress(value);
        invalidate();
    }

    @Override
    public long onFinish(boolean success) {
        if (success) {
            mDrawable.hit();
            return getHitAnimationDuration();
        } else {
            mDrawable.miss();
            return getMissAnimationDuration();
        }
    }

    private int getHitAnimationDuration() {
        return (int) (mDrawable.getHitDuration() + (mDrawable.getSkewDuration() * 8) + 400);
    }

    private int getMissAnimationDuration() {
        return ((int) mDrawable.getMissDuration()) + 100;
    }

    @Override
    public void onDataLoading() {
        mDrawable.fire();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawable != null) {
            mDrawable.draw(canvas);
        }
        invalidate();
    }

    public IArrowInitializer getInitializer() {
        return mInitializer;
    }

    public void setInitializer(IArrowInitializer initializer) {
        this.mInitializer = initializer;
    }

    public ArrowDrawable getDrawable() {
        return mDrawable;
    }
}
