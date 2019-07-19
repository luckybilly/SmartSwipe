package com.billy.android.swipe.demo;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import com.billy.android.swipe.SmartSwipeBack;

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
    }

    private SmartSwipeBack.ActivitySwipeBackFilter activitySwipeBackFilter = new SmartSwipeBack.ActivitySwipeBackFilter() {
        @Override
        public boolean onFilter(Activity activity) {
            return !(activity instanceof MainActivity);
        }
    };
}
