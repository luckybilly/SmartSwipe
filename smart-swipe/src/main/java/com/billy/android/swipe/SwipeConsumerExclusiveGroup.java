package com.billy.android.swipe;

import com.billy.android.swipe.listener.SimpleSwipeListener;

import java.util.LinkedList;
import java.util.List;

/**
 * manage a group of SwipeConsumer(s), only single one SwipeConsumer can be mark as the current one, the original SwipeConsumer will be close
 * @author billy.qi
 */
public class SwipeConsumerExclusiveGroup {
    private List<SwipeConsumer> list = new LinkedList<>();
    private SwipeConsumer curSwipeConsumer;
    private boolean smooth;
    private boolean lockOther = false;

    public SwipeConsumerExclusiveGroup() {
        this.smooth = true;
    }

    /**
     * create a group, specific close mode smoothly or not
     * @param smooth specific close mode smoothly or not
     */
    public SwipeConsumerExclusiveGroup(boolean smooth) {
        this.smooth = smooth;
    }

    public void markNoCurrent() {
        if (curSwipeConsumer != null) {
            curSwipeConsumer.close(smooth);
            curSwipeConsumer = null;
        }
        if (lockOther) {
            for (SwipeConsumer consumer : list) {
                if (consumer.isAllDirectionsLocked()) {
                    consumer.unlockAllDirections();
                }
            }
        }
    }

    public void markAsCurrent(SwipeConsumer consumer) {
        markAsCurrent(consumer, smooth);
    }

    public void markAsCurrent(SwipeConsumer current, boolean smoothResetOrigin) {
        if (this.curSwipeConsumer == current) {
            return;
        }
        this.curSwipeConsumer = current;
        for(SwipeConsumer consumer : list) {
            if (consumer != curSwipeConsumer) {
                if (lockOther && !consumer.isAllDirectionsLocked()) {
                    consumer.lockAllDirections();
                }
                consumer.close(smoothResetOrigin);
            }
        }
    }

    public void add(SwipeConsumer consumer) {
        if (!list.contains(consumer)) {
            list.add(consumer);
            consumer.addListener(singleListener);
        }
    }

    public void remove(SwipeConsumer consumer) {
        if (consumer != null) {
            list.remove(consumer);
            consumer.removeListener(singleListener);
        }
    }

    public void clear() {
        while(!list.isEmpty()) {
            SwipeConsumer consumer = list.remove(0);
            if (consumer != null) {
                consumer.removeListener(singleListener);
            }
        }
    }

    private SimpleSwipeListener singleListener = new SimpleSwipeListener() {

        @Override
        public void onSwipeOpened(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
            markAsCurrent(consumer);
        }

        @Override
        public void onSwipeClosed(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
            if (consumer == curSwipeConsumer) {
                markNoCurrent();
            }
        }
    };

    public boolean isLockOther() {
        return lockOther;
    }

    public void setLockOther(boolean lockOther) {
        this.lockOther = lockOther;
    }

    public SwipeConsumer getCurSwipeConsumer() {
        return curSwipeConsumer;
    }

    public boolean isSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }
}
