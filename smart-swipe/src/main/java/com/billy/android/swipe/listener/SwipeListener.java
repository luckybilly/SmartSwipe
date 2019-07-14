package com.billy.android.swipe.listener;

import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.internal.SwipeHelper;

/**
 * listen swipe state of {@link SwipeConsumer} via {@link SwipeConsumer#addListener(SwipeListener)}
 * @author billy.qi
 * @see SimpleSwipeListener
 */
public interface SwipeListener {
    /**
     * Depending on whether SwipeConsumer has been added to the wrapper through {@link SmartSwipeWrapper#addConsumer(SwipeConsumer)},
     * This method will be called in 2 cases : <br>
     * 1. not added: called when {@link SmartSwipeWrapper#addConsumer(SwipeConsumer)} <br>
     * 2. already added: called when added to SwipeConsumer via {@link SwipeConsumer#addListener(SwipeListener)} <br>
     *
     * This callback method is useful to program auto open or auto close action when SwipeConsumer attached to SmartSwipeWrapper before SwipeConsumer attached to SmartSwipeWrapper
     *
     * @param wrapper SmartSwipeWrapper the SwipeConsumer add to
     * @param consumer the SwipeConsumer this listener add to
     * @see SmartSwipeWrapper#addConsumer(SwipeConsumer)
     * @see SwipeConsumer#onAttachToWrapper(SmartSwipeWrapper, SwipeHelper)
     * @see SwipeConsumer#addListener(SwipeListener)
     */
    void onConsumerAttachedToWrapper(SmartSwipeWrapper wrapper, SwipeConsumer consumer);
    void onConsumerDetachedFromWrapper(SmartSwipeWrapper wrapper, SwipeConsumer consumer);
    void onSwipeStateChanged(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int state, int direction, float progress);
    void onSwipeStart(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction);
    void onSwipeProcess(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction, boolean settling, float progress);
    void onSwipeRelease(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction, float progress, float xVelocity, float yVelocity);
    void onSwipeOpened(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction);
    void onSwipeClosed(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction);
}
