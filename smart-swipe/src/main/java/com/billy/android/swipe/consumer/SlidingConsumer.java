package com.billy.android.swipe.consumer;

import android.view.View;
import com.billy.android.swipe.SmartSwipe;

import static android.view.View.VISIBLE;

/**
 * @author billy.qi
 */
public class SlidingConsumer extends DrawerConsumer {
    /** contentView moves above the drawer view, drawer view show below contentView */
    public static final float FACTOR_COVER = 0F;

    /** Default factor, contentView moves and drawer view followed pixel by pixel */
    public static final float FACTOR_FOLLOW = 1F;

    protected float mRelativeMoveFactor = 0.5F;
    protected boolean mEdgeAffinity;
    protected boolean mDrawerExpandable;
    protected int mDrawerW, mDrawerH;

    @Override
    public void onDetachFromWrapper() {
        super.onDetachFromWrapper();
        for (View drawerView : mDrawerViews) {
            if (drawerView != null) {
                drawerView.scrollTo(0, 0);
            }
        }
        View contentView = mWrapper.getContentView();
        if (contentView != null) {
            contentView.layout(0, 0, mWidth, mHeight);
        }
    }

    @Override
    protected void calculateDrawerDirectionInitPosition(int direction, int w, int h) {
        mDrawerW = w;
        mDrawerH = h;
        int initDistance = (int) (mOpenDistance * (1 - mRelativeMoveFactor) + 0.5F);
        switch (direction) {
            case DIRECTION_LEFT:
                l = -w + initDistance;
                if (mEdgeAffinity && l > 0) {
                    l = 0;
                }
                r = l + w;  t = 0;      b = h;
                break;
            case DIRECTION_RIGHT:
                l = mWidth - initDistance;
                r = l + w;  t = 0;      b = h;
                if (mEdgeAffinity && r < mWidth) {
                    r = mWidth;
                    l = r - w;
                }
                break;
            case DIRECTION_TOP:
                l = 0;      r = mWidth; t = -h + initDistance;
                if (mEdgeAffinity && t > 0) {
                    t = 0;
                }
                b = t + h; break;
            case DIRECTION_BOTTOM:
                l = 0;      r = mWidth; t = mHeight - initDistance;b = t + h;
                if (mEdgeAffinity && b < mHeight) {
                    b = mHeight;
                    t = b - h;
                }
                break;
            default: break;
        }
    }

    @Override
    protected void onDisplayDistanceChanged(int distanceXToDisplay, int distanceYToDisplay, int dx, int dy) {
        layoutChildren();
    }

    @Override
    protected void orderChildren() {
        View contentView = mWrapper.getContentView();
        if (contentView != null) {
            contentView.bringToFront();
        }
        if (mScrimView != null) {
            mScrimView.bringToFront();
        }
    }

    @Override
    protected void layoutContentView(View contentView) {
        if (contentView != null) {
            contentView.layout(mCurDisplayDistanceX, mCurDisplayDistanceY, mWidth + mCurDisplayDistanceX, mHeight + mCurDisplayDistanceY);
        }
    }

    @Override
    protected void layoutDrawerView() {
        View contentView = mWrapper.getContentView();
        final View drawerView = mCurDrawerView;
        if (contentView == null || drawerView == null) {
            return;
        }
        if (drawerView.getVisibility() == VISIBLE) {
            //layout drawer view
            int rdx = (int) (mCurDisplayDistanceX * mRelativeMoveFactor + (mCurDisplayDistanceX > 0 ? 0.5f : -0.5f));
            int rdy = (int) (mCurDisplayDistanceY * mRelativeMoveFactor + (mCurDisplayDistanceY > 0 ? 0.5f : -0.5f));
            int scrollX = 0, scrollY = 0;
            int left = l, top = t, right = r, bottom = b;
            switch (mDirection) {
                case DIRECTION_LEFT:
                    left = l + rdx;
                    right = contentView.getLeft();
                    if (!mDrawerExpandable) {
                        if (!mEdgeAffinity && right > mDrawerW) {
                            left = right - mDrawerW;
                        } else if (mEdgeAffinity && left > 0) {
                            left = 0;
                        }
                        if (right - left > mDrawerW) {
                            right = left + mDrawerW;
                        }
                    } else if (left > 0) {
                        left = 0;
                    }
                    break;
                case DIRECTION_RIGHT:
                    left = contentView.getRight();
                    right = r + rdx;
                    if (!mDrawerExpandable) {
                        if (!mEdgeAffinity && left + mDrawerW < mWidth) {
                            right = left + mDrawerW;
                        } else if (mEdgeAffinity && right < mWidth) {
                            right = mWidth;
                        }
                        if (right - left > mDrawerW) {
                            left = right - mDrawerW;
                        }
                    } else if (right < mWidth) {
                        right = mWidth;
                    }
                    scrollX = (int) ((mOpenDistance + mCurDisplayDistanceX) * (1 - mRelativeMoveFactor));
                    scrollX = Math.max(scrollX, 0);
                    break;
                case DIRECTION_TOP:
                    top = t + rdy;
                    bottom = contentView.getTop();
                    if (!mDrawerExpandable) {
                        if (!mEdgeAffinity && bottom > mDrawerH) {
                            top = bottom - mDrawerH;
                        } else if (mEdgeAffinity && top > 0) {
                            top = 0;
                        }
                        if (bottom - top > mDrawerH) {
                            bottom = top + mDrawerH;
                        }
                    } else if (top > 0) {
                        top = 0;
                    }
                    break;
                case DIRECTION_BOTTOM:
                    top = contentView.getBottom();
                    bottom = b + rdy;
                    if (!mDrawerExpandable) {
                        if (!mEdgeAffinity && top + mDrawerH < mHeight) {
                            bottom = top + mDrawerH;
                        } else if (mEdgeAffinity && bottom < mHeight) {
                            bottom = mHeight;
                        }
                        if (bottom - top > mDrawerH) {
                            top = bottom - mDrawerH;
                        }
                    } else if (bottom < mHeight) {
                        bottom = mHeight;
                    }
                    scrollY = (int) ((mOpenDistance + mCurDisplayDistanceY) * (1 - mRelativeMoveFactor));
                    scrollY = Math.max(scrollY, 0);
                    break;
                default: break;
            }
            drawerView.layout(left, top, right, bottom);
            //compat for contentView background is transparent
            // drawer view don`t overlap with contentView
            drawerView.scrollTo(scrollX, scrollY);
        }
    }

    public float getRelativeMoveFactor() {
        return mRelativeMoveFactor;
    }

    /**
     * Set the movement factor of drawer view relative to content view
     * @param factor Multiplier of mCurDrawerView relative movement to mWrapper.getContentView().
     *               this value must to be between 0F and 1F.
     *               0: drawer view not move
     *               0~1:
     *               1: drawer view followed contentView pixel by pixel
     * @return this
     */
    public SlidingConsumer setRelativeMoveFactor(float factor) {
        this.mRelativeMoveFactor = SmartSwipe.ensureBetween(factor, FACTOR_COVER, FACTOR_FOLLOW);
        return this;
    }

    public boolean isEdgeAffinity() {
        return mEdgeAffinity;
    }

    /**
     * set drawer is affinity to the edge or not
     * <pre>
     * this property make effect when {@link #mDrawerExpandable} is false and {@link #mSwipeMaxDistance} is bigger than drawerView`s size(width or height).
     * it may be cause by:
     *  1. {@link #mSwipeOpenDistance} is bigger than drawerView`s size via {@link #setOpenDistance(int)}
     *  2. {@link #mOverSwipeFactor} is bigger than 0F via {@link #setOverSwipeFactor(float)}
     * </pre>
     * if set true, drawer view adsorbs to {@link com.billy.android.swipe.SmartSwipeWrapper}`s edge
     * @param affinity adsorbs to edge or not
     * @return this
     * @see #setOpenDistance(int)
     * @see #setOverSwipeFactor(float)
     * @see #setDrawerExpandable(boolean)
     */
    public SlidingConsumer setEdgeAffinity(boolean affinity) {
        this.mEdgeAffinity = affinity;
        return this;
    }

    public boolean isDrawerExpandable() {
        return mDrawerExpandable;
    }

    /**
     * determine whether drawers expandable.
     * <pre>
     * this property make effect when {@link #mSwipeMaxDistance} is bigger than drawerView`s size(width or height).
     * it may be cause by:
     *  1. {@link #mSwipeOpenDistance} is bigger than drawerView`s size via {@link #setOpenDistance(int)}
     *  2. {@link #mOverSwipeFactor} is bigger than 0F via {@link #setOverSwipeFactor(float)}
     * </pre>
     * if set true, {@link #setEdgeAffinity(boolean)} will be ignored.
     * @param expandable expandable or not
     * @return this
     * @see #setEdgeAffinity(boolean)
     * @see #setOverSwipeFactor(float)
     */
    public SlidingConsumer setDrawerExpandable(boolean expandable) {
        this.mDrawerExpandable = expandable;
        return this;
    }
}
