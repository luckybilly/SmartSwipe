package com.billy.android.swipe.demo;

import android.view.View;
import android.view.animation.BounceInterpolator;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.internal.SwipeHelper;
import com.billy.android.swipe.listener.SimpleSwipeListener;
import com.billy.android.swipe.consumer.*;
import com.billy.android.swipe.consumer.ShuttersConsumer;
import com.billy.android.swipe.SwipeConsumer;

/**
 * demo to manage an app cover
 * @author billy.qi
 */
public class CoverManager {
    public static final int MAX_SETTLE_DURATION = 1000;

    private final View coverView;
    private CoverListener listener;
    public SwipeConsumer consumer;
    protected int width;
    protected int height;
    private SwipeConsumer doorConsumer, shuttersConsumer;
    private SwipeConsumer translucentSlidingConsumer;

    public boolean isOpened() {
        return consumer != null && consumer.isOpened();
    }

    public CoverManager open() {
        if (consumer != null && consumer.getDragState() != SwipeHelper.STATE_DRAGGING) {
            consumer.smoothTopOpen();
        }
        return this;
    }

    public interface CoverListener {
        void onOpened();
        void onClosed();
    }

    public static CoverManager manage(View view) {
        return new CoverManager(view);
    }

    public CoverManager setCoverListener(CoverListener listener) {
        this.listener = listener;
        return this;
    }

    public CoverManager setWidth(int width) {
        this.width = width;
        return this;
    }

    public CoverManager setHeight(int height) {
        this.height = height;
        return this;
    }

    public CoverManager(View coverView) {
        this.coverView = coverView;
    }

    private SimpleSwipeListener swipeListener = new SimpleSwipeListener() {
        @Override
        public void onSwipeStart(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
            super.onSwipeStart(wrapper, consumer, direction);
            if (wrapper.getVisibility() != View.VISIBLE) {
                wrapper.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onConsumerAttachedToWrapper(SmartSwipeWrapper wrapper, SwipeConsumer consumer) {
            super.onConsumerAttachedToWrapper(wrapper, consumer);
            coverView.setVisibility(View.VISIBLE);
            if (consumer instanceof ShuttersConsumer) {
                //make ShuttersConsumer(/DoorConsumer) could show open status immediately
                consumer.as(ShuttersConsumer.class).setWaitForScreenshot(false);
            }
            consumer.setTopOpen().smoothClose();
        }

        @Override
        public void onSwipeOpened(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
            super.onSwipeOpened(wrapper, consumer, direction);
            wrapper.setVisibility(View.GONE);
            if (CoverManager.this.listener != null) {
                CoverManager.this.listener.onOpened();
            }
        }

        @Override
        public void onSwipeClosed(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
            super.onSwipeClosed(wrapper, consumer, direction);
            if (consumer instanceof ShuttersConsumer) {
                //make ShuttersConsumer(/DoorConsumer) wait for the first screenshot before moves
                ((ShuttersConsumer) consumer).setWaitForScreenshot(true);
            }
            if (CoverManager.this.listener != null) {
                CoverManager.this.listener.onClosed();
            }
        }
    };

    public void doorMode() {
        if (doorConsumer == null) {
            initDoorConsumerBeforeAddToWrapper();
        }
        consumer = SmartSwipe.wrap(coverView)
                .removeAllConsumers()
                .addConsumer(doorConsumer);
    }

    public void showShuttersMode() {
        if (shuttersConsumer == null) {
            initShuttersConsumerBeforeAddToWrapper();
        }
        consumer = SmartSwipe.wrap(coverView)
                .removeAllConsumers()
                .addConsumer(shuttersConsumer);
    }

    public void drawerMode() {
        if (translucentSlidingConsumer == null) {
            initTranslucentSlidingConsumerBeforeAddToWrapper();
        }
        consumer = SmartSwipe.wrap(coverView)
                .removeAllConsumers()
                .addConsumer(translucentSlidingConsumer);

    }

    //demo for usage of SwipeListener#onConsumerAttachedToWrapper
    //create and init SwipeConsumer before using it
    private void initDoorConsumerBeforeAddToWrapper() {
        doorConsumer = new DoorConsumer()
                .setRefreshable(true)
                .setScrimColor(0xAF000000)
                .setMaxSettleDuration(MAX_SETTLE_DURATION)
                .setInterpolator(new BounceInterpolator())
                .enableAllDirections()
                .setWidth(width)
                .setHeight(height)
                .addListener(swipeListener)
        ;
    }
    private void initShuttersConsumerBeforeAddToWrapper() {
        shuttersConsumer = new ShuttersConsumer()
                .setRefreshable(true)
                .setLeavesCount(4)
                .setScrimColor(0xAF000000)
                .setRefreshFrameRate(30)
                .setMaxSettleDuration(MAX_SETTLE_DURATION)
                .setOpenDistance(SmartSwipe.dp2px(100, coverView.getContext()))
                .setOverSwipeFactor(2F)
                .setInterpolator(new BounceInterpolator())
                .enableAllDirections()
                .setWidth(width)
                .setHeight(height)
                .addListener(swipeListener)
        ;
    }
    private void initTranslucentSlidingConsumerBeforeAddToWrapper() {
        translucentSlidingConsumer = new TranslucentSlidingConsumer()
                .showScrimAndShadowOutsideContentView()
                .setScrimColor(0xAF000000)
                .setMaxSettleDuration(MAX_SETTLE_DURATION)
                .setInterpolator(new BounceInterpolator())
                .enableAllDirections()
                .setWidth(width)
                .setHeight(height)
                .addListener(swipeListener)
        ;
    }

}
