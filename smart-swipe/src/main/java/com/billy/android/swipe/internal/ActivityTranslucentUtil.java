package com.billy.android.swipe.internal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeWrapper;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author billy.qi
 */
@SuppressLint("PrivateApi")
public class ActivityTranslucentUtil {
    private static Class mTranslucentConversionListenerClass;
    private static Method mMethodConvertFromTranslucent;
    private static Method mMethodConvertToTranslucent;
    private static Method mMethodGetActivityOptions;
    private static boolean mInitialedConvertToTranslucent;
    private static boolean mInitialedConvertFromTranslucent;


    private Activity mActivity;
    private boolean mIsTranslucent;

    public ActivityTranslucentUtil(Activity activity) {
        this.mActivity = activity;
    }

    public static void convertWindowToTranslucent(Activity activity) {
        if (activity != null) {
            View contentView = activity.findViewById(android.R.id.content);
            Drawable background = contentView.getBackground();
            if (background == null) {
                TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
                int windowBg = a.getResourceId(0, 0);
                a.recycle();
                if (windowBg != 0) {
                    contentView.setBackgroundResource(windowBg);
                }
            }
            Window window = activity.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.getDecorView().setBackgroundDrawable(null);
            SmartSwipeWrapper wrapper = SmartSwipe.peekWrapperFor(activity);
            if (wrapper != null) {
                wrapper.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    private MessageQueue.IdleHandler convertActivityToTranslucentIdleHandler = new MessageQueue.IdleHandler() {
        @Override
        public boolean queueIdle() {
            convertActivityToTranslucent();
            return false;
        }
    };
    private long convertTranslucentTimeStamp;

    public void convertActivityToTranslucent() {
        convertActivityToTranslucent(true);
    }

    public void convertActivityToTranslucent(final boolean retry) {
        if (mIsTranslucent || mActivity == null) {
            return;
        }
        if (convertingActivity != null) {
            Looper.myQueue().addIdleHandler(convertActivityToTranslucentIdleHandler);
            return;
        }
        convertTranslucentTimeStamp = SystemClock.elapsedRealtime();
        final long callbackTimeStamp = convertTranslucentTimeStamp;
        convertActivityToTranslucent(mActivity, new ActivityTranslucentUtil.TranslucentCallback() {
            @Override
            public void onTranslucentCallback(boolean translucent) {
                if (callbackTimeStamp == convertTranslucentTimeStamp) {
                    if (retry && !translucent) {
                        convertActivityToTranslucent(false);
                    } else {
                        setTranslucent(translucent);
                    }
                }
            }
        });
    }

    public void convertActivityFromTranslucent() {
        convertTranslucentTimeStamp = SystemClock.elapsedRealtime();
        convertActivityFromTranslucent(mActivity);
        setTranslucent(false);
    }

    private void setTranslucent(boolean translucent) {
        this.mIsTranslucent = translucent;
    }

    public boolean isTranslucent() {
        return mIsTranslucent;
    }

    /** record the converting activity, resolve more than 1 xxUIs add onto the same activity */
    private static WeakReference<Activity> convertingActivity;
    /**
     * Reflect call Activity.convertToTranslucent(...)
     * @param activity activity
     * @param callback callback
     */
    public static void convertActivityToTranslucent(Activity activity, final TranslucentCallback callback) {
        convertingActivity = new WeakReference<>(activity);
        Object mTranslucentConversionListener = null;
        try {
            if (mTranslucentConversionListenerClass == null) {
                Class[] clazzArray = Activity.class.getDeclaredClasses();
                for (Class clazz : clazzArray) {
                    if (clazz.getSimpleName().contains("TranslucentConversionListener")) {
                        mTranslucentConversionListenerClass = clazz;
                    }
                }
            }
            //resolve black flash at the beginning:
            // Activity.convertToTranslucent(...) will takes tens of milliseconds
            //thanks: https://github.com/Simon-Leeeeeeeee/SLWidget/blob/master/swipeback/src/main/java/cn/simonlee/widget/swipeback/SwipeBackHelper.java
            if (mTranslucentConversionListenerClass != null) {
                InvocationHandler invocationHandler = new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        boolean translucent = false;
                        if (args != null && args.length == 1) {
                            translucent = (Boolean) args[0];
                        }
                        convertCallback(callback, translucent);
                        return null;
                    }
                };
                mTranslucentConversionListener = Proxy.newProxyInstance(mTranslucentConversionListenerClass.getClassLoader(), new Class[]{mTranslucentConversionListenerClass}, invocationHandler);
            }
            if (mMethodConvertToTranslucent == null && mInitialedConvertToTranslucent) {
                convertCallback(callback, false);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mMethodConvertToTranslucent == null) {
                    mInitialedConvertToTranslucent = true;
                    Method getActivityOptions = Activity.class.getDeclaredMethod("getActivityOptions");
                    getActivityOptions.setAccessible(true);
                    mMethodGetActivityOptions = getActivityOptions;
                    Method method = Activity.class.getDeclaredMethod("convertToTranslucent", mTranslucentConversionListenerClass, ActivityOptions.class);
                    method.setAccessible(true);
                    mMethodConvertToTranslucent = method;
                }
                Object options = mMethodGetActivityOptions.invoke(activity);
                mMethodConvertToTranslucent.invoke(activity, mTranslucentConversionListener, options);
            } else {
                if (mMethodConvertToTranslucent == null) {
                    mInitialedConvertToTranslucent = true;
                    Method method = Activity.class.getDeclaredMethod("convertToTranslucent", mTranslucentConversionListenerClass);
                    method.setAccessible(true);
                    mMethodConvertToTranslucent = method;
                }
                mMethodConvertToTranslucent.invoke(activity, mTranslucentConversionListener);
            }
            if (mTranslucentConversionListener == null) {
                convertCallback(callback, false);
            }
        } catch (Throwable ignored) {
            convertCallback(callback, false);
        }
    }

    private static void convertCallback(TranslucentCallback callback, boolean translucent) {
        if (callback != null) {
            callback.onTranslucentCallback(translucent);
        }
        convertingActivity = null;
    }

    public static void convertActivityFromTranslucent(Activity activity) {
        if (activity == null) {
            return;
        }
        if (convertingActivity != null && convertingActivity.get() == activity) {
            convertingActivity = null;
        }
        try {
            if (mMethodConvertFromTranslucent == null) {
                if (mInitialedConvertFromTranslucent) {
                    return;
                }
                mInitialedConvertFromTranslucent = true;
                Method method = Activity.class.getDeclaredMethod("convertFromTranslucent");
                method.setAccessible(true);
                mMethodConvertFromTranslucent = method;
            }
            mMethodConvertFromTranslucent.invoke(activity);
        } catch (Throwable ignored) {
        }
    }

    public interface TranslucentCallback {
        void onTranslucentCallback(boolean translucent);
    }
}
