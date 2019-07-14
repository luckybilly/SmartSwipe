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
import com.billy.android.swipe.consumer.DrawerConsumer;
import com.billy.android.swipe.consumer.SlidingConsumer;
import com.billy.android.swipe.demo.BaseRecyclerViewActivity;
import com.billy.android.swipe.demo.R;

import java.util.Arrays;

import static android.view.View.OnClickListener;


/**
 * demo:
 * @author billy.qi
 */
public class DrawerConsumerActivity extends BaseRecyclerViewActivity {

    @Override
    public int getTitleResId() {
        return R.string.demo_ui_DrawerConsumer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SmartSwipeRefresh.scaleMode(findViewById(R.id.container), false)
                .setDataLoader(dataLoader)
                .getSwipeConsumer().as(SlidingConsumer.class)
                .setDrawerExpandable(false)
                .setEdgeAffinity(true)
        ;
    }


    SwipeConsumerExclusiveGroup exclusiveGroup = new SwipeConsumerExclusiveGroup();
    @Override
    protected ViewHolder createRecyclerViewHolder(View view) {
        TextView textView = new TextView(DrawerConsumerActivity.this);
        textView.setBackgroundColor(0xFFAA0000);
        textView.setTextColor(0xFFFFFFFF);
        textView.setTextSize(14);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT));
        SwipeConsumer consumer = SmartSwipe.wrap(view)
                .addConsumer(new DrawerConsumer())
                .setHorizontalDrawerView(textView)
                .setScrimColor(0x2F000000)
                .setShadowColor(0x80000000)
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
                textView.setPadding(30, 0, 30, 0);
                textView.setText("Delete");
            }
            textView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        adapter.removeItem(getAdapterPosition());
                    } else {
                        Toast.makeText(DrawerConsumerActivity.this, "position:(" + position + ") drawer clicked!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            this.mConsumer = consumer;
        }

        @Override
        protected void onBindData(BaseRecyclerViewActivity.Data data, int position) {
            super.onBindData(data, position);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && INTERPOLATORS != null) {
                Interpolator interpolator = INTERPOLATORS[position % INTERPOLATORS.length];
                mConsumer.setInterpolator(interpolator);
                textView.setText(getString(R.string.demo_drawer_ui_item_text, interpolator.getClass().getSimpleName()));
            }
            this.position = position;
        }

    }

    @Override
    protected int getMessageArray() {
        return R.array.demo_drawer_ui_messages;
    }
}
