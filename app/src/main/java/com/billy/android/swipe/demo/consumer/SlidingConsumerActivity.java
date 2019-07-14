package com.billy.android.swipe.demo.consumer;

import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.*;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.billy.android.swipe.*;
import com.billy.android.swipe.consumer.SlidingConsumer;
import com.billy.android.swipe.demo.BaseRecyclerViewActivity;
import com.billy.android.swipe.demo.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.view.View.OnClickListener;


/**
 * demo:
 * @author billy.qi
 */
public class SlidingConsumerActivity extends BaseRecyclerViewActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SmartSwipeRefresh.behindMode(findViewById(R.id.container), false)
                .setDataLoader(dataLoader).startRefresh()
        ;
    }

    SwipeConsumerExclusiveGroup exclusiveGroup = new SwipeConsumerExclusiveGroup();
    @Override
    protected ViewHolder createRecyclerViewHolder(View view) {
        TextView textView = new TextView(SlidingConsumerActivity.this);
        textView.setBackgroundColor(0xFFAA0000);
        textView.setTextColor(0xFFFFFFFF);
        textView.setTextSize(14);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT));
        SwipeConsumer consumer = SmartSwipe.wrap(view)
                .addConsumer(new SlidingConsumer())
                .setHorizontalDrawerView(textView)
                .setScrimColor(0x2F000000)
                .addToExclusiveGroup(exclusiveGroup)
                ;
        return new ViewHolder(consumer.getWrapper(), textView, consumer);
    }

    static Interpolator[] INTERPOLATORS = null;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            INTERPOLATORS = Arrays.asList(
                    new AccelerateInterpolator()
                    , new AccelerateDecelerateInterpolator()
                    , new DecelerateInterpolator()
                    , new LinearInterpolator()
                    , new AnticipateInterpolator()
                    , new AnticipateOvershootInterpolator()
                    , new OvershootInterpolator()
                    , new BounceInterpolator()
            ).toArray(new Interpolator[0]);
        }
    }
    class ViewHolder extends BaseRecyclerViewActivity.ViewHolder {
        private final TextView textView;
        private int position;
        private SwipeConsumer mConsumer;

        ViewHolder(SmartSwipeWrapper wrapper, TextView textView, SwipeConsumer consumer) {
            super(wrapper);
            this.textView = textView;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                textView.setText(R.string.demo_drawer_ui_item_text_delete);
            }
            textView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        adapter.removeItem(getAdapterPosition());
                    } else {
                        Toast.makeText(SlidingConsumerActivity.this, "position:(" + position + ") drawer clicked!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            this.mConsumer = consumer;
        }

        @Override
        protected void onBindData(Data data, int position) {
            super.onBindData(data, position);
            this.position = position;
            String str = "";
            if (mConsumer instanceof SlidingConsumer) {
                SlidingConsumer consumer = (SlidingConsumer) mConsumer;
                consumer.setRelativeMoveFactor((position % 5) / 4F);
                str = getString(R.string.demo_main_group_main_ui_relative_move_factor) + consumer.getRelativeMoveFactor() + "\n\n";
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && INTERPOLATORS != null) {
                Interpolator interpolator = INTERPOLATORS[position % INTERPOLATORS.length];
                mConsumer.setInterpolator(interpolator);
                textView.setText(str + getString(R.string.demo_drawer_ui_item_text, interpolator == null ? "default" : interpolator.getClass().getSimpleName()));
            } else {
                textView.setText(getString(R.string.demo_drawer_ui_item_text_delete) + "\n" + str);
            }
        }

    }

    @Override
    protected List<Data> getInitData() {
        return new ArrayList<>();
    }

    @Override
    public int getTitleResId() {
        return R.string.demo_ui_SlidingConsumer;
    }

    @Override
    protected int getMessageArray() {
        return R.array.demo_sliding_ui_messages;
    }
}
