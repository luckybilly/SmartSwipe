package com.billy.android.swipe.demo.consumer;

import android.os.Bundle;
import android.view.View;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.consumer.StretchConsumer;
import com.billy.android.swipe.demo.BaseRecyclerViewActivity;
import com.billy.android.swipe.demo.R;


/**
 * demo:
 * @author billy.qi
 */
public class StretchConsumerActivity extends BaseRecyclerViewActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //enable recyclerView top & bottom swipe
        SmartSwipe.wrap(recyclerView).addConsumer(new StretchConsumer()).enableVertical();
    }

    @Override
    public int getTitleResId() {
        return R.string.demo_ui_StretchConsumer;
    }

    @Override
    protected ViewHolder createRecyclerViewHolder(View view) {
        return new ViewHolder(SmartSwipe.wrap(view).addConsumer(new StretchConsumer()).enableHorizontal().getWrapper());
    }

}
