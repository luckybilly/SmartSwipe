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
        //add swipe translucent back performance for all activities
        // (default direction: left, previous activity related factor:0.5F)
//        SmartSwipeBack.activitySlidingBack(this, activitySwipeBackFilter);

        //add swipe back like mobile QQ (activity keep stay and finish activity with release velocity)
        //SmartSwipeBack.activityStayBack(this, activitySwipeBackFilter);

        //add bezier swipe back like XiaoMi (swipe with bezier back consumer at edge of screen)
        //SmartSwipeBack.activityBezierBack(this, activitySwipeBackFilter);

        //add swipe back looks like open a door
        //SmartSwipeBack.activityDoorBack(this, activitySwipeBackFilter);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //use bezier back before LOLLIPOP
            SmartSwipeBack.activityBezierBack(this, activitySwipeBackFilter);
        } else {
            //add swipe back looks like open shutters
            SmartSwipeBack.activityShuttersBack(this, activitySwipeBackFilter);
        }
    }

    private SmartSwipeBack.ActivitySwipeBackFilter activitySwipeBackFilter = new SmartSwipeBack.ActivitySwipeBackFilter() {
        @Override
        public boolean onFilter(Activity activity) {
            return !(activity instanceof MainActivity);
        }
    };
}
