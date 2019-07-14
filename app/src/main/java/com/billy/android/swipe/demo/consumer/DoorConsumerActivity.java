package com.billy.android.swipe.demo.consumer;

import android.os.Bundle;
import android.view.View;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeRefresh;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.demo.BaseRecyclerViewActivity;
import com.billy.android.swipe.demo.R;
import com.billy.android.swipe.listener.SimpleSwipeListener;
import com.billy.android.swipe.consumer.*;
import com.billy.android.swipe.consumer.SlidingConsumer;
import com.billy.android.swipe.consumer.StretchConsumer;


/**
 * demo:
 * @author billy.qi
 */
public class DoorConsumerActivity extends BaseRecyclerViewActivity {

    @Override
    public int getTitleResId() {
        return R.string.demo_ui_DoorConsumer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //enable recyclerView top & bottom swipe refersh
        SmartSwipeRefresh.behindMode(findViewById(R.id.container), false)
                .setDataLoader(dataLoader)
                //get SwipeConsumer
                .getSwipeConsumer().as(SlidingConsumer.class)
                .setEdgeAffinity(true)
                //disable nested touch scroll
                .setDisableNestedScroll(true)
                //disable nested non-touch scroll
                .setDisableNestedFly(true)
                //add StretchConsumer as the secondary SwipeConsumer to handle the nested scroll event
                .addConsumer(new StretchConsumer())
                //enable StretchConsumer top & bottom directions
                .enableVertical()
                ;
    }

    @Override
    protected ViewHolder createRecyclerViewHolder(View view) {
        SwipeConsumer consumeri = SmartSwipe.wrap(view)
                .addConsumer(new DoorConsumer())
                .enableHorizontal()
                ;
        return new ViewHolder(consumeri.getWrapper(), consumeri);
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
