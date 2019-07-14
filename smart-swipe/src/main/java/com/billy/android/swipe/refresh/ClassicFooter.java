package com.billy.android.swipe.refresh;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import com.billy.android.swipe.R;
import com.billy.android.swipe.SmartSwipeRefresh;

/**
 * classic footer for {@link SmartSwipeRefresh}
 * @author billy.qi
 */
public class ClassicFooter extends ClassicHeader implements SmartSwipeRefresh.SmartSwipeRefreshFooter {

    public boolean mNoMoreData;

    public ClassicFooter(Context context) {
        super(context);
    }

    public ClassicFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClassicFooter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ClassicFooter(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onProgress(boolean dragging, float progress) {
        if (mNoMoreData) {
            cancelAnimation();
            return;
        }
        if (dragging) {
            setText(progress >= 1 ? R.string.ssr_footer_release : R.string.ssr_footer_pulling);
        } else if (progress <= 0) {
            cancelAnimation();
        }
    }

    @Override
    public long onFinish(boolean success) {
        cancelAnimation();
        if (!mNoMoreData) {
            setText(success ? R.string.ssr_footer_finish : R.string.ssr_footer_failed);
        }
        return 500;
    }

    @Override
    public void onDataLoading() {
        if (!mNoMoreData) {
            showAnimation();
            setText(R.string.ssr_footer_refreshing);
        }
    }


    @Override
    public void setNoMoreData(boolean noMoreData) {
        this.mNoMoreData = noMoreData;
        setText(R.string.ssr_footer_no_more_data);
    }
}
