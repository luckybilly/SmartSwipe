package com.billy.android.swipe.internal;

import com.billy.android.swipe.SwipeConsumer;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.billy.android.swipe.SwipeConsumer.DIRECTION_HORIZONTAL;
import static com.billy.android.swipe.SwipeConsumer.DIRECTION_VERTICAL;

/**
 * utils
 * @author billy.qi
 */
public class SwipeUtil {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, CPU_COUNT - 1);
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT + 1;
    private static final int KEEP_ALIVE = 10;


    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "SmartSwipe #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(128);

    private static final Executor THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);

    public static void runInThreadPool(Runnable runnable) {
        THREAD_POOL_EXECUTOR.execute(runnable);
    }


    /**
     * return the reverse direction for the given direction
     * @param direction the given direction, must be one of: {@link SwipeConsumer#DIRECTION_LEFT}
     *                                                      /{@link SwipeConsumer#DIRECTION_RIGHT}
     *                                                      /{@link SwipeConsumer#DIRECTION_TOP}
     *                                                      /{@link SwipeConsumer#DIRECTION_BOTTOM}
     * @return the reverse direction
     */
    public static int getReverseDirection(int direction) {
        if ((direction & DIRECTION_HORIZONTAL) != 0) {
            return (direction ^ DIRECTION_HORIZONTAL) & DIRECTION_HORIZONTAL;
        } else {
            return (direction ^ DIRECTION_VERTICAL) & DIRECTION_VERTICAL;
        }
    }
}
