package com.billy.android.swipe.demo.consumer;

import android.os.Bundle;
import android.widget.TextView;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.demo.BaseActivity;
import com.billy.android.swipe.demo.R;
import com.billy.android.swipe.listener.SimpleSwipeListener;
import com.billy.android.swipe.consumer.BezierBackConsumer;
import com.billy.android.swipe.SwipeConsumer;


/**
 * demo:
 * @author billy.qi
 */
public class SwipeBackBezierConsumerActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_description);
        TextView textView = findViewById(R.id.description);
        textView.setText(R.string.demo_description_SwipeBackBezierConsumer);

        SmartSwipe.wrap(this)
                .removeAllConsumers()
                .addConsumer(new BezierBackConsumer())
                .enableAllDirections()
                .addListener(new SimpleSwipeListener() {
                    @Override
                    public void onSwipeOpened(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                        finish();
                    }
                })
        ;
    }

    @Override
    public int getTitleResId() {
        return R.string.demo_ui_BezierBackConsumer;
    }
}
