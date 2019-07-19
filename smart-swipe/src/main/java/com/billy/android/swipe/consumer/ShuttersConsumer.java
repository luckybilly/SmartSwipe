package com.billy.android.swipe.consumer;

import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.internal.SwipeUtil;

import java.util.concurrent.CountDownLatch;

/**
 * looks like shutters open and close
 * @author billy.qi
 */
public class ShuttersConsumer extends SwipeConsumer {
    protected final Camera mCamera;
    protected Paint mPaint;
    protected boolean mHorizontalSwiping;
    protected int mBaseAlpha;
    protected int lastScreenDirection = DIRECTION_NONE;
    /** internal mark: refreshBitmapRunnable is working or not */
    protected volatile boolean mRefreshing;
    protected volatile Bitmap[] mScreenshots;

    protected int mScrimColor;
    protected int mLeavesCount = 5;
    protected volatile boolean mRefreshable = true;
    protected boolean mWaitForScreenshot = true;
    /** refresh rate: 30 frames per second */
    protected int refreshDelay = 1000 / 30;


    public ShuttersConsumer() {
        setReleaseMode(RELEASE_MODE_AUTO_OPEN_CLOSE);
        mCamera = new Camera();
        mCamera.setLocation(0,0, -20);
        mPaint = new Paint();
    }

    @Override
    public void onDetachFromWrapper() {
        super.onDetachFromWrapper();
        setRefreshing(false);
        recycleScreenshots();
        layoutChildren();
    }

    @Override
    public void onSwipeAccepted(int activePointerId, boolean settling, float initialMotionX, float initialMotionY) {
        if (lastScreenDirection != mDirection) {
            recycleScreenshots();
        }
        lastScreenDirection = mDirection;
        lastRefreshTime = 0;
        if ((mCurSwipeDistanceX == 0 && mCurSwipeDistanceY == 0)) {
            int halfWidth = mWidth >> 1;
            int halfHeight = mHeight >> 1;
            mHorizontalSwiping = isHorizontalDirection();
            if (!mOpenDistanceSpecified) {
                if (mHorizontalSwiping) {
                    mOpenDistance = halfWidth;
                } else {
                    mOpenDistance = halfHeight;
                }
            }
        }
        super.onSwipeAccepted(activePointerId, settling, initialMotionX, initialMotionY);
        layoutChildren();
        if(!mRefreshing) {
            setRefreshing(true);
            SwipeUtil.runInThreadPool(refreshBitmapRunnable);
        }
    }

    private Runnable refreshWrapperRunnable = new Runnable() {
        @Override
        public void run() {
            layoutChildren();
            mWrapper.postInvalidate();
        }
    };
    private Runnable refreshBitmapRunnable = new Runnable(){
        @Override
        public void run() {
            refreshBitmap();
        }
    };

    private class ScreenshotCreateRunnable implements Runnable {
        int width, height;
        int index;
        Bitmap[] array;
        CountDownLatch latch;
        View srcView;
        int scrollX, scrollY;

        ScreenshotCreateRunnable(int width, int height, int index, Bitmap[] array, CountDownLatch latch, View srcView, int scrollX, int scrollY) {
            this.width = width;
            this.height = height;
            this.index = index;
            this.array = array;
            this.latch = latch;
            this.srcView = srcView;
            this.scrollX = scrollX;
            this.scrollY = scrollY;
        }
        @Override
        public void run() {
            boolean switchToMainThread = false;
            try {
                Bitmap screenshot = array[index];
                if (screenshot == null || screenshot.isRecycled() || screenshot.getWidth() != width || screenshot.getHeight() != height) {
                    screenshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                }
                Canvas canvas = new Canvas(screenshot);
                canvas.translate(-srcView.getScrollX() - scrollX, -srcView.getScrollY() - scrollY);

                // Draw background
                Drawable bgDrawable = srcView.getBackground();
                if (bgDrawable != null) {
                    bgDrawable.draw(canvas);
                }
                try {
                    srcView.draw(canvas);
                    array[index] = screenshot;
                } catch(Exception e) {
                    if (Looper.myLooper() != Looper.getMainLooper()) {
                        switchToMainThread = true;
                        srcView.post(this);
                    }
                }
            } catch(Throwable e) {
                e.printStackTrace();
            } finally {
                if (!switchToMainThread) {
                    latch.countDown();
                }
            }
        }
    }

    private long lastRefreshTime;
    protected void refreshBitmap() {
        if (lastRefreshTime == 0) {
            lastRefreshTime = SystemClock.elapsedRealtime();
        }
        View v = mWrapper.getContentView();
        final int leavesCount = this.mLeavesCount;
        int width = (int) (mWidth * 1F / (mHorizontalSwiping ? leavesCount : 1) + 0.5F);
        int height = (int) (mHeight * 1F / (mHorizontalSwiping ? 1 : leavesCount) + 0.5F);
        int lastWidth = mHorizontalSwiping ? (mWidth - width * (leavesCount - 1)) : width;
        int lastHeight = mHorizontalSwiping ? height : (mHeight - height * (leavesCount - 1));
        //TODO reuse bitmap array. Tried to use buffer bitmap array, but a blink bug happens when refreshable enabled
        Bitmap[] array = new Bitmap[leavesCount];
        CountDownLatch latch = new CountDownLatch(leavesCount);
        int scrollX = 0, scrollY = 0;
        for (int i = 0; i < leavesCount; i++) {
            if (mHorizontalSwiping) {
                scrollX = width * i;
            } else {
                scrollY = height * i;
            }
            if (i == leavesCount - 1) {
                if (lastWidth <= 0 || lastHeight <= 0) {
                    latch.countDown();
                } else {
                    SwipeUtil.runInThreadPool(new ScreenshotCreateRunnable(lastWidth, lastHeight, i, array, latch, v, scrollX, scrollY));
                }
            } else {
                SwipeUtil.runInThreadPool(new ScreenshotCreateRunnable(width, height, i, array, latch, v, scrollX, scrollY));
            }
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!mSwiping && (mProgress <= 0 || mProgress >= 1)) {
            setRefreshing(false);
        }
        if (!mRefreshing) {
            return;
        }
        boolean hasNull = false;
        for (Bitmap bitmap : array) {
            if (bitmap == null) {
                hasNull = true;
                break;
            }
        }
        if (!hasNull) {
            this.mScreenshots = array;
        }
        v.post(refreshWrapperRunnable);
        if (mRefreshable) {
            long timePass = SystemClock.elapsedRealtime() - lastRefreshTime;
            lastRefreshTime = SystemClock.elapsedRealtime();
            if (timePass < refreshDelay) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SwipeUtil.runInThreadPool(refreshBitmapRunnable);
                    }
                }, refreshDelay - timePass);
            } else {
                SwipeUtil.runInThreadPool(refreshBitmapRunnable);
            }
        } else {
            setRefreshing(false);
        }
    }

    private void setRefreshing(boolean refreshing) {
        mRefreshing = refreshing;
    }

    private void layoutChildren() {
        View contentView = mWrapper.getContentView();
        if (mDirection == DIRECTION_NONE || mScreenshots == null && mWaitForScreenshot) {
            contentView.layout(0, 0, mWidth, mHeight);
            contentView.setVisibility(View.VISIBLE);
        } else {
            if (mRefreshable) {
                contentView.layout(-9999, -9999, mWidth - 9999, mHeight - 9999);
            } else {
                contentView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        final Bitmap[] screenshots = this.mScreenshots;
        if (mDirection == DIRECTION_NONE || screenshots == null || screenshots.length == 0) {
            return;
        }
        if (mScrimColor != 0 && mBaseAlpha != 0) {
            canvas.drawRect(0, 0, mWidth, mHeight, mPaint);
        }
        int halfW = mWidth >> 1;
        int halfH = mHeight >> 1;
        int leafSize = (int) ((mHorizontalSwiping ? mWidth : mHeight) * 1F / screenshots.length + 0.5F);
        int halfLeafSize = leafSize >> 1;
        int dir = (mDirection == DIRECTION_LEFT || mDirection == DIRECTION_BOTTOM) ? 1 : -1;
        for (int i = 0; i < screenshots.length; i++) {
            Bitmap screenshot = screenshots[i];
            if (screenshot == null || screenshot.isRecycled()) {
                continue;
            }
            canvas.save();
            mCamera.save();
            if (mHorizontalSwiping) {
                canvas.translate(leafSize * i + halfLeafSize, halfH);
                mCamera.rotateY(dir * 90 * mProgress);
                mCamera.applyToCanvas(canvas);
                canvas.translate(-halfLeafSize, 0);
                canvas.drawBitmap(screenshot, 0, -halfH, null);
            } else {
                canvas.translate(halfW, leafSize * i + halfLeafSize);
                mCamera.rotateX(dir * 90 * mProgress);
                mCamera.applyToCanvas(canvas);
                canvas.translate(0, -halfLeafSize);
                canvas.drawBitmap(screenshot, -halfW, 0, null);
            }
            mCamera.restore();
            canvas.restore();
        }
    }

    @Override
    public boolean onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutChildren();
        return true;
    }

    @Override
    protected void onClosed() {
        super.onClosed();
        recycleScreenshots();
        setRefreshing(false);
        layoutChildren();
    }

    protected void recycleScreenshots() {
        lastScreenDirection = DIRECTION_NONE;
        mScreenshots = null;
    }

    @Override
    protected void onDisplayDistanceChanged(int distanceXToDisplay, int distanceYToDisplay, int dx, int dy) {
        if (mScrimColor != 0 && mBaseAlpha != 0) {
            float progress = SmartSwipe.ensureBetween(mProgress, 0, 1F);
            int alpha = (int) (mBaseAlpha * (1 - progress));
            mPaint.setAlpha(alpha);
        }
        if (!mRefreshable) {
            mWrapper.postInvalidate();
        }
    }

    @Override
    public int clampDistanceHorizontal(int distanceX, int dx) {
        if (mScreenshots != null || !mWaitForScreenshot) {
            return super.clampDistanceHorizontal(distanceX, dx);
        }
        return 0;
    }

    @Override
    public int clampDistanceVertical(int distanceY, int dy) {
        if (mScreenshots != null || !mWaitForScreenshot) {
            return super.clampDistanceVertical(distanceY, dy);
        }
        return 0;
    }

    public int getScrimColor() {
        return mScrimColor;
    }

    public ShuttersConsumer setScrimColor(int scrimColor) {
        this.mScrimColor = scrimColor;
        this.mPaint.setColor(scrimColor);
        mBaseAlpha = (mScrimColor & 0xFF000000) >>> 24;
        return this;
    }

    public int getLeavesCount() {
        return mLeavesCount;
    }

    public ShuttersConsumer setLeavesCount(int leavesCount) {
        int newCount = SmartSwipe.ensureBetween(leavesCount, 1, 100);
        if (newCount != mLeavesCount) {
            this.mLeavesCount = newCount;
            recycleScreenshots();
        }
        return this;
    }

    public boolean isRefreshable() {
        return mRefreshable;
    }

    public ShuttersConsumer setRefreshable(boolean refreshable) {
        this.mRefreshable = refreshable;
        return this;
    }

    public boolean isWaitForScreenshot() {
        return mWaitForScreenshot;
    }

    /**
     * if content view draw coat too much time, wait for screenshot is useful
     * @param wait default is false, if set as true, do real swipe after screenshot has been done
     * @return this
     */
    public ShuttersConsumer setWaitForScreenshot(boolean wait) {
        this.mWaitForScreenshot = wait;
        return this;
    }

    public ShuttersConsumer setRefreshFrameRate(int frameRate) {
        frameRate = SmartSwipe.ensureBetween(frameRate, 1, 60);
        refreshDelay = 1000 / frameRate;
        return this;
    }

}
