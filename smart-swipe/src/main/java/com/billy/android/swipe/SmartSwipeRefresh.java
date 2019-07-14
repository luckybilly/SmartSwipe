package com.billy.android.swipe;

import android.content.Context;
import android.view.View;
import com.billy.android.swipe.calculator.ScaledCalculator;
import com.billy.android.swipe.listener.SimpleSwipeListener;
import com.billy.android.swipe.refresh.ClassicFooter;
import com.billy.android.swipe.refresh.ClassicHeader;
import com.billy.android.swipe.consumer.DrawerConsumer;
import com.billy.android.swipe.consumer.SlidingConsumer;

import static com.billy.android.swipe.SwipeConsumer.*;


/**
 * A wrapper of DrawerConsumer(or SlidingConsumer) to build Refresh util for View(s)
 *
 * Supports for: View/ViewGroup/LinearLayout/RelativeLayout/ListView/RecyclerView/WebView/etc...
 * <pre>
 *     Usage:
 *      1. with global default header and footer via SmartSwipeRefresh.setDefaultRefreshViewCreator(creator)
 *          1). SmartSwipeRefresh.drawerMode(view, false).setDataLoader(loader);
 *          2). SmartSwipeRefresh.behindMode(view, false).setDataLoader(loader);
 *          3). SmartSwipeRefresh.translateMode(view, false).setDataLoader(loader);
 *          4). SmartSwipeRefresh.scaleMode(view, false).setDataLoader(loader);
 *      2. with specified header and footer
 *          1). SmartSwipeRefresh.drawerMode(view, false, false).setDataLoader(loader).setHeader(header).setFooter(footer);
 *          2). SmartSwipeRefresh.behindMode(view, false, false).setDataLoader(loader).setHeader(header).setFooter(footer);
 *          3). SmartSwipeRefresh.translateMode(view, false, false).setDataLoader(loader).setHeader(header).setFooter(footer);
 *          4). SmartSwipeRefresh.scaleMode(view, false, false).setDataLoader(loader).setHeader(header).setFooter(footer);
 *
 *      3. more DrawerConsumer(SlidingConsumer extends DrawerConsumer) features
 *          DrawerConsumer consumer = smartSwipeRefresh.getSwipeConsumer();
 *          //behindMode or translateMode or scaleMode
 *          SlidingConsumer consumer = smartSwipeRefresh.getSwipeConsumer().as(SlidingConsumer.class);
 * </pre>
 * if
 * @see SmartSwipeRefreshViewCreator
 * @see SmartSwipeRefreshDataLoader
 * @see SmartSwipeRefreshHeader
 * @see SmartSwipeRefreshFooter
 * @see ClassicHeader
 * @see ClassicFooter
 * @author billy.qi
 */
public class SmartSwipeRefresh {

    private static SmartSwipeRefreshViewCreator mCreator;

    private DrawerConsumer mConsumer;
    private SmartSwipeRefreshHeader mHeader;
    private SmartSwipeRefreshFooter mFooter;
    private RefreshView mActiveRefreshView;
    private boolean mHorizontal;
    private SmartSwipeRefreshDataLoader mDataLoader;
    private boolean mNoMoreData;

    public static void setDefaultRefreshViewCreator(SmartSwipeRefreshViewCreator creator) {
        mCreator = creator;
    }


    /**
     * looks like drawer: header and footer show above the contentView
     * @param contentView any view which needs to refresh and load more
     * @param horizontal works horizontally or not
     * @return this
     */
    public static SmartSwipeRefresh drawerMode(View contentView, boolean horizontal) {
        return drawerMode(contentView, horizontal, true);
    }

    /**
     * looks like drawer: header and footer show above the contentView
     * @param contentView any view which needs to refresh and load more
     * @param horizontal works horizontally or not
     * @param withDefaultHeaderAndFooter use default header and footer via {@link #setDefaultRefreshViewCreator(SmartSwipeRefreshViewCreator)}
     * @return this
     * @see #drawerMode(View, boolean)
     */
    public static SmartSwipeRefresh drawerMode(View contentView, boolean horizontal, boolean withDefaultHeaderAndFooter) {
        return create(contentView, new DrawerConsumer(), horizontal, withDefaultHeaderAndFooter);
    }

    /**
     * header and footer show behind the contentView, and stay at its position while contentView is moving
     * @param contentView any view which needs to refresh and load more
     * @param horizontal works horizontally or not
     * @return this
     */
    public static SmartSwipeRefresh behindMode(View contentView, boolean horizontal) {
        return behindMode(contentView, horizontal, true);
    }

    /**
     * header and footer show behind the contentView, and stay at its position while contentView is moving
     * @param contentView any view which needs to refresh and load more
     * @param horizontal works horizontally or not
     * @param withDefaultHeaderAndFooter use default header and footer via {@link #setDefaultRefreshViewCreator(SmartSwipeRefreshViewCreator)}
     * @return this
     * @see #behindMode(View, boolean)
     */
    public static SmartSwipeRefresh behindMode(View contentView, boolean horizontal, boolean withDefaultHeaderAndFooter) {
        return slideMode(contentView, SlidingConsumer.FACTOR_COVER, horizontal, withDefaultHeaderAndFooter);
    }

    /**
     * header and footer show followed the contentView (moves pixel by pixel with contentView)
     * @param contentView any view which needs to refresh and load more
     * @param horizontal works horizontally or not
     * @return this
     */
    public static SmartSwipeRefresh translateMode(View contentView, boolean horizontal) {
        return translateMode(contentView, horizontal, true);
    }

    /**
     * header and footer show followed the contentView (moves pixel by pixel with contentView)
     * @param contentView any view which needs to refresh and load more
     * @param horizontal works horizontally or not
     * @param withDefaultHeaderAndFooter use default header and footer via {@link #setDefaultRefreshViewCreator(SmartSwipeRefreshViewCreator)}
     * @return this
     * @see #translateMode(View, boolean)
     */
    public static SmartSwipeRefresh translateMode(View contentView, boolean horizontal, boolean withDefaultHeaderAndFooter) {
        return slideMode(contentView, SlidingConsumer.FACTOR_FOLLOW, horizontal, withDefaultHeaderAndFooter);
    }

    /**
     * header and footer show behind the contentView
     * Always in the middle of the space left after the main view has moved
     * @param contentView any view which needs to refresh and load more
     * @param horizontal works horizontally or not
     * @return this
     */
    public static SmartSwipeRefresh scaleMode(View contentView, boolean horizontal) {
        return scaleMode(contentView, horizontal, true);
    }

    /**
     * header and footer show behind the contentView
     * Always in the middle of the space left after the main view has moved
     * @param contentView any view which needs to refresh and load more
     * @param horizontal works horizontally or not
     * @param withDefaultHeaderAndFooter use default header and footer via {@link #setDefaultRefreshViewCreator(SmartSwipeRefreshViewCreator)}
     * @return this
     * @see #scaleMode(View, boolean)
     */
    public static SmartSwipeRefresh scaleMode(View contentView, boolean horizontal, boolean withDefaultHeaderAndFooter) {
        return slideMode(contentView, 0.5F, horizontal, withDefaultHeaderAndFooter);
    }

    /**
     * header and footer show behind the contentView, and there relative movement specified by relativeMoveFactor
     * @param contentView contentView to refresh
     * @param relativeMoveFactor the factor header and footer moves relative to contentView
     * @param horizontal is refresh horizontally(true) or vertically(false)
     * @param withDefaultHeaderAndFooter use default header and footer by {@link SmartSwipeRefreshViewCreator} (if set)
     *                                   or {@link ClassicHeader} / {@link ClassicFooter} (if creator not set)
     * @return this
     * @see SmartSwipeRefreshViewCreator
     * @see ClassicHeader
     * @see ClassicFooter
     * @see SlidingConsumer#setRelativeMoveFactor(float)
     */
    public static SmartSwipeRefresh slideMode(View contentView, float relativeMoveFactor, boolean horizontal, boolean withDefaultHeaderAndFooter) {
        return create(contentView, new SlidingConsumer().setRelativeMoveFactor(relativeMoveFactor), horizontal, withDefaultHeaderAndFooter);
    }

    public static SmartSwipeRefresh create(View contentView, DrawerConsumer consumer, boolean horizontal, boolean withDefaultHeaderAndFooter) {
        SmartSwipeRefresh ssr = new SmartSwipeRefresh();
        ssr.mConsumer = SmartSwipe.wrap(contentView)
                .addConsumer(consumer)
                //disable motion event to handle refresh view after drag released
                .setDisableSwipeOnSettling(true)
                //add listener to support refresh and load more events
                .addListener(ssr.swipeListener)
                //set distance calculator for current DrawerConsumer instance
                .setSwipeDistanceCalculator(new ScaledCalculator(0.4F))
                //hold on if swipe opened when release, otherwise, auto close it
                .setReleaseMode(SwipeConsumer.RELEASE_MODE_AUTO_CLOSE | SwipeConsumer.RELEASE_MODE_HOLE_OPEN)
                //set default rebound factor
                .setOverSwipeFactor(0.5F)
                //enable nested scroll fling to auto refresh or load more by default
                // If need to disable, like this: smartSwipeRefresh.getSwipeConsumer().setDisableNestedFly(true)
                .setDisableNestedFly(false)
                .as(DrawerConsumer.class);
        ssr.mHorizontal = horizontal;
        if (withDefaultHeaderAndFooter) {
            if (mCreator != null) {
                ssr.setHeader(mCreator.createRefreshHeader(contentView.getContext()));
                ssr.setFooter(mCreator.createRefreshFooter(contentView.getContext()));
            } else {
                ssr.setHeader(new ClassicHeader(contentView.getContext()));
                ssr.setFooter(new ClassicFooter(contentView.getContext()));
            }
        }
        return ssr;
    }
    
    private SimpleSwipeListener swipeListener = new SimpleSwipeListener() {
        @Override
        public void onSwipeStart(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
            mActiveRefreshView = null;
            switch (direction) {
                case DIRECTION_LEFT:
                case DIRECTION_TOP:
                    mActiveRefreshView = mHeader;
                    break;
                case DIRECTION_RIGHT:
                case DIRECTION_BOTTOM:
                    mActiveRefreshView = mFooter;
                    break;
                default:
            }
            if (mActiveRefreshView != null) {
                mActiveRefreshView.onStartDragging();
            }
        }

        @Override
        public void onSwipeOpened(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
            if (mDataLoader == null) {
                //no data loader, close it
                finished(false);
                return;
            }
            if (mActiveRefreshView == mHeader) {
                consumer.lockAllDirections();
                mActiveRefreshView.onDataLoading();
                mDataLoader.onRefresh(SmartSwipeRefresh.this);
            } else if (mActiveRefreshView == mFooter) {
                consumer.lockAllDirections();
                mActiveRefreshView.onDataLoading();
                if (mNoMoreData) {
                    finished(true);
                } else {
                    mDataLoader.onLoadMore(SmartSwipeRefresh.this);
                }
            }
        }

        @Override
        public void onSwipeClosed(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
            consumer.unlockAllDirections();
//            if (mActiveRefreshView == mFooter) {
//                mFooter.setNoMoreData(mNoMoreData);
//            }
            mActiveRefreshView = null;
        }

        @Override
        public void onSwipeProcess(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction, boolean settling, float progress) {
            if (mActiveRefreshView != null) {
                mActiveRefreshView.onProgress(!settling, progress);
            }
        }
    };

    public SmartSwipeRefresh disableRefresh() {
        int direction = mHorizontal ? DIRECTION_LEFT : DIRECTION_TOP;
        mConsumer.disableDirection(direction);
        return this;
    }

    public SmartSwipeRefresh disableLoadMore() {
        int direction = mHorizontal ? DIRECTION_RIGHT : DIRECTION_BOTTOM;
        mConsumer.disableDirection(direction);
        return this;
    }

    /**
     * fake to refresh without swipe motion event
     * @return this
     */
    public SmartSwipeRefresh startRefresh() {
        int direction = mHorizontal ? DIRECTION_LEFT : DIRECTION_TOP;
        openDirection(direction);
        return this;
    }

    /**
     * fake to load more without swipe motion event
     * @return this
     */
    public SmartSwipeRefresh startLoadMore() {
        int direction = mHorizontal ? DIRECTION_RIGHT : DIRECTION_BOTTOM;
        openDirection(direction);
        return this;
    }

    private void openDirection(final int direction) {
        mConsumer.getWrapper().post(new Runnable() {
            @Override
            public void run() {
                mConsumer.open(true, direction);
            }
        });
    }

    /**
     * finish current drawer( header or footer)
     * @param success data load success or not
     * @return this
     */
    public SmartSwipeRefresh finished(boolean success) {
        if (mActiveRefreshView != null) {
            if (success && mActiveRefreshView == mHeader) {
                //auto set mNoMoreData as false when refresh success
                setNoMoreData(false);
            }
            long animationDuration = mActiveRefreshView.onFinish(success);
            if (animationDuration > 0) {
                mConsumer.getWrapper().postDelayed(mResetRunnable, animationDuration);
                return null;
            }
        }
        mConsumer.smoothClose();
        return this;
    }

    public SmartSwipeRefreshDataLoader getDataLoader() {
        return mDataLoader;
    }

    /**
     * set the data loader to do refresh and load more business
     * @param dataLoader data loader
     * @return this
     * @see SmartSwipeRefreshDataLoader
     */
    public SmartSwipeRefresh setDataLoader(SmartSwipeRefreshDataLoader dataLoader) {
        this.mDataLoader = dataLoader;
        return this;
    }

    public boolean isNoMoreData() {
        return mNoMoreData;
    }

    /**
     * mark footer there is no more data to load
     * @param noMoreData no more data or not
     * @return this
     */
    public SmartSwipeRefresh setNoMoreData(boolean noMoreData) {
        this.mNoMoreData = noMoreData;
        if (mFooter != null) {
            mFooter.setNoMoreData(noMoreData);
        }
        return this;
    }

    public SmartSwipeRefreshHeader getHeader() {
        return mHeader;
    }

    /**
     * set the refresh header
     * @param header refresh header
     * @return this
     */
    public SmartSwipeRefresh setHeader(SmartSwipeRefreshHeader header) {
        this.mHeader = header;
        if (header != null) {
            header.onInit(mHorizontal);
        }
        mConsumer.setDrawerView(mHorizontal ? DIRECTION_LEFT : DIRECTION_TOP, header == null ? null : header.getView());
        return this;
    }

    public SmartSwipeRefreshFooter getFooter() {
        return mFooter;
    }

    /**
     * set the refresh footer
     * @param footer refresh footer
     * @return this
     */
    public SmartSwipeRefresh setFooter(SmartSwipeRefreshFooter footer) {
        this.mFooter = footer;
        if (footer != null) {
            footer.onInit(mHorizontal);
        }
        mConsumer.setDrawerView(mHorizontal ? DIRECTION_RIGHT : DIRECTION_BOTTOM, footer == null ? null : footer.getView());
        return this;
    }

    private Runnable mResetRunnable = new Runnable() {
        @Override
        public void run() {
            mConsumer.smoothClose();
            mConsumer.unlockAllDirections();
        }
    };

    public boolean isHorizontal() {
        return mHorizontal;
    }

    public DrawerConsumer getSwipeConsumer() {
        return mConsumer;
    }

    private interface RefreshView {
        /**
         * get view to display
         * @return View
         */
        View getView();

        /**
         * Called before RefreshView add to {@link DrawerConsumer} or {@link SlidingConsumer}
         * @param horizontal true: will be layout at left(/right) if this is a SmartSwipeRefreshHeader(/SmartSwipeRefreshFooter) instance.
         *                   false: will be layout at top(/bottom) if this is a SmartSwipeRefreshHeader(/SmartSwipeRefreshFooter) instance
         */
        void onInit(boolean horizontal);

        /**
         * Called when dragging state determined
         */
        void onStartDragging();

        /**
         * Call while swipe distance changes
         * @param dragging user dragging event or not
         * @param progress [0F, 1F + overSwipeFactor]
         */
        void onProgress(boolean dragging, float progress);

        /**
         * Call when {@link SmartSwipeRefresh#finished(boolean)} called
         * @param success is data load success or not
         * @return time delay for finish animation plays before header or footer close
         */
        long onFinish(boolean success);

        /**
         * Called when header or footer fully swiped and animate rebound to the fully distance
         */
        void onDataLoading();
    }

    public interface SmartSwipeRefreshHeader extends RefreshView {

    }

    public interface SmartSwipeRefreshFooter extends RefreshView {
        /**
         * mark footer that there is no more data to load
         * @param noMoreData true: no more data, false: maybe has more data to load
         */
        void setNoMoreData(boolean noMoreData);
    }

    /**
     * The refresh data loader
     * When refresh or load more event emits, its methods will be called
     */
    public interface SmartSwipeRefreshDataLoader {
        /**
         * Called when {@link SmartSwipeRefreshHeader} swipe released and it has been fully swiped
         * @param ssr {@link SmartSwipeRefresh}
         */
        void onRefresh(SmartSwipeRefresh ssr);
        /**
         * Called when {@link SmartSwipeRefreshFooter} swipe released and it has been fully swiped
         * @param ssr {@link SmartSwipeRefresh}
         */
        void onLoadMore(SmartSwipeRefresh ssr);
    }

    /**
     * creator of {@link SmartSwipeRefreshHeader} and {@link SmartSwipeRefreshFooter}
     * @see #setDefaultRefreshViewCreator(SmartSwipeRefreshViewCreator)
     */
    public interface SmartSwipeRefreshViewCreator {

        /**
         * create the refresh header view
         * @param context context
         * @return header
         */
        SmartSwipeRefreshHeader createRefreshHeader(Context context);

        /**
         * create the refresh footer view
         * @param context context
         * @return footer
         */
        SmartSwipeRefreshFooter createRefreshFooter(Context context);
    }
}
