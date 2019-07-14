package com.billy.android.swipe.consumer;

import com.billy.android.swipe.SwipeConsumer;

/**
 * Swipe to do something, the contentView do not move,
 *  when swipe released, determine whether the velocity and direction is enough, do some business if success
 * @author billy.qi
 */
public class StayConsumer extends SwipeConsumer {
    private int mMinVelocity = 1000;

    public StayConsumer() {
        setOpenDistance(Integer.MAX_VALUE)
                .setMaxSettleDuration(0);
    }

    @Override
    protected void onDisplayDistanceChanged(int distanceXToDisplay, int distanceYToDisplay, int dx, int dy) {
        //do nothing
    }

    @Override
    public void onSwipeReleased(float xVelocity, float yVelocity) {
        if (Math.abs(xVelocity) > Math.abs(yVelocity)) {
            if (mDirection == DIRECTION_LEFT && xVelocity >= mMinVelocity || (mDirection == DIRECTION_RIGHT && xVelocity <= -mMinVelocity)) {
                mCurSwipeDistanceX = getSwipeOpenDistance();
                mProgress = 1;
            }
        } else {
            if (mDirection == DIRECTION_TOP && yVelocity >= mMinVelocity || (mDirection == DIRECTION_BOTTOM && yVelocity <= -mMinVelocity)) {
                mCurSwipeDistanceY = getSwipeOpenDistance();
                mProgress = 1;
            }
        }
        super.onSwipeReleased(xVelocity, yVelocity);
    }

    public int getMinVelocity() {
        return mMinVelocity;
    }

    public StayConsumer setMinVelocity(int minVelocity) {
        if (minVelocity > 0) {
            this.mMinVelocity = minVelocity;
        }
        return this;
    }
}
