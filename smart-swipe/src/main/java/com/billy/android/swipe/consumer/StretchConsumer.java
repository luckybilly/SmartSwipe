package com.billy.android.swipe.consumer;

import android.view.View;
import com.billy.android.swipe.SwipeConsumer;

/**
 * @author billy.qi
 */
public class StretchConsumer extends SwipeConsumer {

    @Override
    public void onDetachFromWrapper() {
        super.onDetachFromWrapper();
        View contentView = mWrapper.getContentView();
        if (contentView != null) {
            contentView.setScaleX(1);
            contentView.setScaleY(1);
            contentView.setTranslationX(0);
            contentView.setTranslationY(0);
        }
    }

    @Override
    public void onDisplayDistanceChanged(int distanceXToDisplay, int distanceYToDisplay, int dx, int dy) {
        View contentView = mWrapper.getContentView();
        if (contentView != null) {
            if (distanceXToDisplay >= 0 && isLeftEnable() || distanceXToDisplay <= 0 && isRightEnable()) {
                contentView.setScaleX(1 + Math.abs((float) distanceXToDisplay) / mWidth);
                contentView.setTranslationX(distanceXToDisplay / 2F);
            }
            if (distanceYToDisplay >= 0 && isTopEnable() || distanceYToDisplay <= 0 && isBottomEnable()) {
                contentView.setScaleY(1 + Math.abs((float) distanceYToDisplay) / mHeight);
                contentView.setTranslationY(distanceYToDisplay / 2F);
            }
        }
    }

}
