package com.billy.android.swipe.demo.consumer;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.consumer.ActivitySlidingBackConsumer;
import com.billy.android.swipe.demo.BaseActivity;
import com.billy.android.swipe.demo.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;


/**
 * demo:
 * @author billy.qi
 */
public class SwipeBackTranslucentConsumerActivity extends BaseActivity {

    private NumberFormat formatter = new DecimalFormat("0.00");

    private TextView numberTextView;
    private ActivitySlidingBackConsumer consumer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_description);
        TextView textView = findViewById(R.id.description);
        textView.setText(R.string.demo_description_SwipeBackTranslucentConsumer);
        consumer = SmartSwipe.wrap(this)
                .removeAllConsumers()
                .addConsumer(new ActivitySlidingBackConsumer(this))
                .setRelativeMoveFactor(0.5F)
                .enableAllDirections()
                .as(ActivitySlidingBackConsumer.class)
                ;
        LinearLayout container = findViewById(R.id.container);
        TextView label = new TextView(this);
        label.setText(R.string.demo_main_group_main_ui_relative_move_factor);
        label.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(label);
        numberTextView = new TextView(this);
        numberTextView.setTextColor(getResources().getColor(R.color.colorPrimary));
        numberTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(numberTextView);
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float moveFactor = consumer.setRelativeMoveFactor(progress * 0.01F).getRelativeMoveFactor();
                numberTextView.setText(formatter.format(moveFactor));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBar.setProgress((int) (consumer.getRelativeMoveFactor() * 100));
        container.addView(seekBar);
    }

    @Override
    public int getTitleResId() {
        return R.string.demo_ui_ActivitySlidingBackConsumer;
    }
}
