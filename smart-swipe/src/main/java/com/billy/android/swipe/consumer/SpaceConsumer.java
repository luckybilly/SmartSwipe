package com.billy.android.swipe.consumer;

import android.view.View;
import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.calculator.ScaledCalculator;

/**
 * @author billy.qi
 */
public class SpaceConsumer extends SwipeConsumer {

    public SpaceConsumer() {
        setSwipeDistanceCalculator(new ScaledCalculator(0.5F));
    }

    @Override
    public void onDetachFromWrapper() {
        super.onDetachFromWrapper();
        View contentView = mWrapper.getContentView();
        if (contentView != null) {
            contentView.setTranslationX(0);
            contentView.setTranslationY(0);
        }
    }

    @Override
    public void onDisplayDistanceChanged(int distanceXToDisplay, int distanceYToDisplay, int dx, int dy) {
        View contentView = mWrapper.getContentView();
        if (contentView != null) {
            if (distanceXToDisplay >= 0 && isLeftEnable() || distanceXToDisplay <= 0 && isRightEnable()) {
                contentView.setTranslationX(distanceXToDisplay);
            }
            if (distanceYToDisplay >= 0 && isTopEnable() || distanceYToDisplay <= 0 && isBottomEnable()) {
                contentView.setTranslationY(distanceYToDisplay);
            }
        }
    }

}
