package com.billy.android.swipe.calculator;

import com.billy.android.swipe.SwipeConsumer;

/**
 * swipe distance is the same as pointer move distance by default, this calculator can change the role
 * @author billy.qi
 */
public interface SwipeDistanceCalculator {
    /**
     * calculate swipe distance
     * @param swipeDistance pointer move distance
     * @param progress current {@link SwipeConsumer} opening progress, value: (from 0F to 1F + {@link SwipeConsumer#getOverSwipeFactor()})
     * @return the distance of calculate result for {@link SwipeConsumer} to do business
     */
    int calculateSwipeDistance(int swipeDistance, float progress);

    /**
     * calculate the open distance by this calculator`s role
     * @param openDistance the original open distance
     * @return calculated open distance
     */
    int calculateSwipeOpenDistance(int openDistance);
}
