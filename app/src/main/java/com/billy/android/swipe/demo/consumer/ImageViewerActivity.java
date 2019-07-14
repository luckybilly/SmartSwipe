package com.billy.android.swipe.demo.consumer;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.consumer.TranslucentSlidingConsumer;
import com.billy.android.swipe.demo.BaseActivity;
import com.billy.android.swipe.demo.R;
import com.billy.android.swipe.internal.ActivityTranslucentUtil;
import com.billy.android.swipe.listener.SimpleSwipeListener;

/**
 * image viewer (swipe down to finish this activity)
 * @author billy.qi
 */
public class ImageViewerActivity extends BaseActivity {
    public static final String IMAGE_EXTRA = "extra_image";
    private View activityContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int avatarResId = getIntent().getIntExtra(IMAGE_EXTRA, 0);
        ImageView imageView = new ImageView(this);
        if (avatarResId != 0) {
            imageView.setImageResource(avatarResId);
        }
        setContentView(imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
        //ActivityTranslucentUtil compat for android api level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activityContentView = findViewById(android.R.id.content);
            activityContentView.setBackgroundColor(0xFF000000);
            //first: copy window background to android.R.id.content(if it not set background)
            //secondly: set window background to transparent
            ActivityTranslucentUtil.convertWindowToTranslucent(this);
            SmartSwipe.wrap(imageView)
                    .addConsumer(new TranslucentSlidingConsumer())
                    .enableTop()
                    .addListener(new SimpleSwipeListener(){
                        @Override
                        public void onSwipeStart(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                            ActivityTranslucentUtil.convertActivityToTranslucent(ImageViewerActivity.this, null);
                        }

                        @Override
                        public void onSwipeProcess(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction, boolean settling, float progress) {
                            View contentView = wrapper.getContentView();
                            //set android.R.id.content alpha from opaque to translucent
                            activityContentView.setAlpha(1 - progress);
                            //set scale from 1 to 0
                            contentView.setScaleX(1 - progress);
                            contentView.setScaleY(1 - progress);
                        }

                        @Override
                        public void onSwipeOpened(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                            //finish this activity with no animation when swipe opened
                            finish();
                            overridePendingTransition(R.anim.anim_none, R.anim.anim_none);
                        }
                    })
            ;
        }
    }
}
