package com.billy.android.swipe.demo.consumer;

import android.os.Bundle;
import android.widget.TextView;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.consumer.StayConsumer;
import com.billy.android.swipe.demo.BaseActivity;
import com.billy.android.swipe.demo.R;
import com.billy.android.swipe.listener.SimpleSwipeListener;


/**
 * demo:
 * @author billy.qi
 */
public class SwipeBackStayConsumerActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_description);
        TextView textView = findViewById(R.id.description);
        textView.setText(R.string.demo_description_SwipeBackStayConsumer);
        SmartSwipe.wrap(this)
                .removeAllConsumers()
                .addConsumer(new StayConsumer())
                .enableAllDirections()
                .as(StayConsumer.class)
                .addListener(new SimpleSwipeListener(){
                    @Override
                    public void onSwipeOpened(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                        finish();
                    }
                })
        ;
    }

    @Override
    public int getTitleResId() {
        return R.string.demo_ui_StayConsumer;
    }
}
