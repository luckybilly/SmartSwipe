package com.billy.android.swipe.demo;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.demo.util.Util;

import java.util.List;

/**
 * base activity
 * @author billy.qi
 */
public abstract class BaseActivity extends Activity {


    protected int screenWidth;
    protected int screenHeight;
    private TextView mTvTitle;
    protected ImageView mIvLeftBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        if (Build.VERSION.SDK_INT >= 19) {
            Util.setStatusBarTransparent(this, false);
        } else {
            View titleContainer = findViewById(R.id.title_container);
            if (titleContainer != null) {
                titleContainer.setPadding(0, 0, 0, 0);
            }
        }
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        initView();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        initView();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        initView();
    }

    private void initView() {
        mIvLeftBtn = findViewById(R.id.iv_left);
        mTvTitle = findViewById(R.id.tv_title);
        if (mTvTitle == null) {
            return;
        }
        int resId = getTitleResId();
        if (resId != 0) {
            mTvTitle.setText(resId);
        } else {
            mTvTitle.setText(getTitleStr());
        }

        if (mIvLeftBtn != null) {
            mIvLeftBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onLeftTitleBtnClick();
                }
            });
        }

    }

    protected void onLeftTitleBtnClick() {
        finish();
    }

    public String getTitleStr() {
        return "";
    }

    public @StringRes
    int getTitleResId() {
        return 0;
    }

    public void setLeftBtnImage(@DrawableRes int resId) {
        if (mIvLeftBtn != null) {
            mIvLeftBtn.setImageResource(resId);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        SmartSwipeWrapper wrapper = SmartSwipe.peekWrapperFor(this);
        if (wrapper != null) {
            List<SwipeConsumer> consumers = wrapper.getAllConsumers();
            if (!consumers.isEmpty()) {
                for (SwipeConsumer consumer : consumers) {
                    if (consumer != null) {
                        if (consumer.isLeftEnable()) {
                            consumer.smoothLeftOpen();
                            return;
                        } else if (consumer.isTopEnable()) {
                            consumer.smoothTopOpen();
                            return;
                        }
                    }
                }
            }
        }
        super.onBackPressed();
    }
}
