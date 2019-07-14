package com.billy.android.swipe.demo.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

/**
 * @author billy.qi
 */
public class Util {

    public static boolean setStatusBarTransparent(Activity activity, boolean darkStatusBar) {
        View decorView = activity.getWindow().getDecorView();
        boolean isInMultiWindowMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode();
        if (isInMultiWindowMode || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return false;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        } else {
            int systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (darkStatusBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(systemUiVisibility);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        return true;
    }
}
