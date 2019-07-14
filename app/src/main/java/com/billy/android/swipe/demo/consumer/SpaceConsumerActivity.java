package com.billy.android.swipe.demo.consumer;

import android.os.Bundle;
import android.view.View;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.consumer.SpaceConsumer;
import com.billy.android.swipe.demo.BaseRecyclerViewActivity;
import com.billy.android.swipe.demo.R;


/**
 * demo:
 * @author billy.qi
 */
public class SpaceConsumerActivity extends BaseRecyclerViewActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //enable recyclerView top & bottom swipe
        SmartSwipe.wrap(recyclerView).addConsumer(new SpaceConsumer()).enableVertical();
    }

    @Override
    public int getTitleResId() {
        return R.string.demo_ui_SpaceConsumer;
    }

    @Override
    protected ViewHolder createRecyclerViewHolder(View view) {
        return new ViewHolder(SmartSwipe.wrap(view).addConsumer(new SpaceConsumer()).enableHorizontal().getWrapper());
    }
}
