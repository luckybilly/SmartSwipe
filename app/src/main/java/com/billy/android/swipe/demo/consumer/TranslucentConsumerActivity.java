package com.billy.android.swipe.demo.consumer;

import android.os.Bundle;
import android.view.View;

import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeRefresh;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.consumer.SlidingConsumer;
import com.billy.android.swipe.consumer.StretchConsumer;
import com.billy.android.swipe.consumer.TranslucentSlidingConsumer;
import com.billy.android.swipe.demo.BaseRecyclerViewActivity;
import com.billy.android.swipe.demo.R;
import com.billy.android.swipe.listener.SimpleSwipeListener;


/**
 * demo:
 * @author billy.qi
 */
public class TranslucentConsumerActivity extends BaseRecyclerViewActivity {

    @Override
    public int getTitleResId() {
        return R.string.demo_ui_TranslucentSlidingConsumer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //enable recyclerView top & bottom swipe
        SmartSwipeRefresh.behindMode(findViewById(R.id.container), false)
                .setDataLoader(dataLoader)
                .getSwipeConsumer()
                .as(SlidingConsumer.class)
                .setEdgeAffinity(true)
                //disable nested non-touch scroll
                .enableNestedFlyVertical(false)
                .addConsumer(new StretchConsumer())
                .enableVertical()
        ;
    }

    @Override
    protected ViewHolder createRecyclerViewHolder(View view) {
        SwipeConsumer consumer = SmartSwipe.wrap(view)
                .addConsumer(new TranslucentSlidingConsumer())
                .enableHorizontal()
                ;
        return new ViewHolder(consumer.getWrapper(), consumer);
    }

    class ViewHolder extends BaseRecyclerViewActivity.ViewHolder {

        ViewHolder(SmartSwipeWrapper wrapper, SwipeConsumer consumer) {
            super(wrapper);
            consumer.addListener(new SimpleSwipeListener(){
                @Override
                public void onSwipeOpened(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                    adapter.removeItem(getAdapterPosition());
                }
            });
        }

    }
}
