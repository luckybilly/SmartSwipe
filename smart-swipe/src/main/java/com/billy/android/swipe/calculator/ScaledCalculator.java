package com.billy.android.swipe.calculator;

import com.billy.android.swipe.SwipeConsumer;

/**
 * The damping effect with scaled swipe distance
 * {@link SwipeConsumer} move distance = pointer move distance * scale
 * @author billy.qi
 */
public class ScaledCalculator implements SwipeDistanceCalculator {

    private float mScale;

    public ScaledCalculator(float scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("scale must be positive");
        }
        this.mScale = scale;
    }

    @Override
    public int calculateSwipeDistance(int swipeDistance, float progress) {
        return (int) (swipeDistance * mScale);
    }

    @Override
    public int calculateSwipeOpenDistance(int openDistance) {
        return (int) (openDistance / mScale);
    }
}
