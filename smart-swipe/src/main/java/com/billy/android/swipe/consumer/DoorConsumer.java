package com.billy.android.swipe.consumer;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * looks like open the door from the middle to the sides
 * @author billy.qi
 */
public class DoorConsumer extends ShuttersConsumer {
    private static final int LEAVES_COUNT = 2;

    public DoorConsumer() {
        mLeavesCount = LEAVES_COUNT;
        setMaxSettleDuration(1000);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        final Bitmap[] screenshots = this.mScreenshots;
        if (mDirection == DIRECTION_NONE || screenshots == null || screenshots.length != LEAVES_COUNT) {
            return;
        }
        if (screenshots[0] == null || screenshots[0].isRecycled() || screenshots[1] == null || screenshots[1].isRecycled()) {
            return;
        }
        int halfW = mWidth >> 1;
        int halfH = mHeight >> 1;
        if (mScrimColor != 0 && mBaseAlpha != 0) {
            if (mHorizontalSwiping) {
                canvas.drawRect(halfW * (1 - mProgress), 0, halfW * (1 + mProgress), mHeight, mPaint);
            } else {
                canvas.drawRect(0, halfH * (1 - mProgress), mWidth, halfH * (1 + mProgress), mPaint);
            }
        }

        canvas.save();
        if (mHorizontalSwiping) {
            canvas.translate(-halfW * mProgress, 0);
            canvas.drawBitmap(screenshots[0], 0, 0, null);
            canvas.restore();
            canvas.save();
            canvas.translate(halfW * (1 + mProgress), 0);
            canvas.drawBitmap(screenshots[1], 0, 0, null);
        } else {
            canvas.translate(0, -halfH * mProgress);
            canvas.drawBitmap(screenshots[0], 0, 0, null);
            canvas.restore();
            canvas.save();
            canvas.translate(0, halfH * (1 + mProgress));
            canvas.drawBitmap(screenshots[1], 0, 0, null);
        }
        canvas.restore();
    }

    @Override
    public ShuttersConsumer setLeavesCount(int leavesCount) {
        //leaves count always 2 (constant: LEAVES_COUNT)
        return this;
    }
}
