package com.billy.android.swipe.demo.consumer;

import android.os.Bundle;
import android.widget.TextView;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.demo.BaseActivity;
import com.billy.android.swipe.demo.R;
import com.billy.android.swipe.consumer.ActivityShuttersBackConsumer;
import com.billy.android.swipe.SwipeConsumer;


/**
 * demo:
 * @author billy.qi
 */
public class SwipeBackShuttersConsumerActivity extends BaseActivity {

    private SwipeConsumer swipeConsumer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_description);
        TextView textView = findViewById(R.id.description);
        textView.setText(R.string.demo_description_ActivityShuttersBackConsumer);
        swipeConsumer = SmartSwipe.wrap(this)
                .removeAllConsumers()
                .addConsumer(new ActivityShuttersBackConsumer(this))
                .setScrimColor(0x7F000000)
                .enableAllDirections();
    }

    @Override
    public void onBackPressed() {
        if (swipeConsumer != null) {
            swipeConsumer.smoothLeftOpen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onLeftTitleBtnClick() {
        if (swipeConsumer != null) {
            swipeConsumer.smoothLeftOpen();
        } else {
            super.onLeftTitleBtnClick();
        }
    }

    @Override
    public int getTitleResId() {
        return R.string.demo_ui_ActivityShuttersBackConsumer;
    }
}
