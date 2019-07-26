package com.billy.android.swipe.demo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.ViewGroup;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeBack;
import com.billy.android.swipe.SmartSwipeRefresh;
import com.billy.android.swipe.ext.refresh.ArrowHeader;
import com.billy.android.swipe.refresh.ClassicFooter;
import com.wuyr.arrowdrawable.ArrowDrawable;

/**
 * @author billy.qi
 */
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //add global swipe back for all activities
        // for more details: https://qibilly.com/SmartSwipe-tutorial/pages/SmartSwipeBack.html

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //use bezier back before LOLLIPOP
            SmartSwipeBack.activityBezierBack(this, activitySwipeBackFilter);
        } else {
            //add relative moving slide back
            SmartSwipeBack.activitySlidingBack(this, activitySwipeBackFilter);
        }

        // demo: set the default creator of refresh view with SmartSwipeRefresh for global usage
        SmartSwipeRefresh.setDefaultRefreshViewCreator(new SmartSwipeRefresh.SmartSwipeRefreshViewCreator() {
            @Override
            public SmartSwipeRefresh.SmartSwipeRefreshHeader createRefreshHeader(Context context) {
                ArrowHeader arrowHeader = new ArrowHeader(context);
                int height = SmartSwipe.dp2px(100, context);
                ViewGroup.LayoutParams layoutParams =  new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
                arrowHeader.setLayoutParams(layoutParams);
                arrowHeader.setInitializer(new ArrowHeader.IArrowInitializer() {
                    @Override
                    public void onArrowInit(ArrowHeader arrowHeader, ArrowDrawable arrowDrawable) {
                        arrowDrawable.setBowColor(Color.GRAY);
                        arrowDrawable.setArrowColor(Color.BLACK);
                        arrowDrawable.setStringColor(Color.GRAY);
                        arrowDrawable.setLineColor(Color.GRAY);
                        arrowHeader.setBackgroundColor(Color.LTGRAY);
                    }
                });
                return arrowHeader;
            }

            @Override
            public SmartSwipeRefresh.SmartSwipeRefreshFooter createRefreshFooter(Context context) {
                return new ClassicFooter(context);
            }
        });
    }

    private SmartSwipeBack.ActivitySwipeBackFilter activitySwipeBackFilter = new SmartSwipeBack.ActivitySwipeBackFilter() {
        @Override
        public boolean onFilter(Activity activity) {
            return !(activity instanceof MainActivity);
        }
    };
}
