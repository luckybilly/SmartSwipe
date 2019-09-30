package com.billy.android.swipe;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

/**
 * A smart swipe util to wrap a view and consume swipe event to do some business via {@link SwipeConsumer}
 * classic usage:
 * <pre>
 *     SmartSwipe.wrap(view)    //specific the view to wrap
 *          .addConsumer(new StretchConsumer()) // add consumer to consume swipe event
 *          .enableVertical(); //enable consumer`s direction(s)
 * </pre>
 * @author billy.qi
 */
public class SmartSwipe {

    /**
     * wrap an activity
     * the content view is: android.R.id.content
     * @param activity activity
     * @return the wrapper
     */
    public static SmartSwipeWrapper wrap(Activity activity) {
        SmartSwipeWrapper wrapper = peekWrapperFor(activity);
        if (wrapper != null) {
            return wrapper;
        }
        View decorView = activity.getWindow().getDecorView();
        if (decorView instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) decorView;
            int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = group.getChildAt(i);
                if (child.findViewById(android.R.id.content) != null) {
                    return wrap(child);
                }
            }
        }
        View contentView = decorView.findViewById(android.R.id.content);
        return wrap(contentView);
    }

    /**
     * peek wrapper for the specific activity, return the origin {@link SmartSwipeWrapper} if exists, else return null
     * @param activity activity
     * @return the wrapper if exists, otherwise returns null
     */
    public static SmartSwipeWrapper peekWrapperFor(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        View contentView = decorView.findViewById(android.R.id.content);
        while (contentView != null && contentView != decorView) {
            if (contentView.getParent() instanceof SmartSwipeWrapper) {
                return (SmartSwipeWrapper) contentView.getParent();
            }
            contentView = (View) contentView.getParent();
        }
        return null;
    }

    /**
     * wrap a view in activity, view id is specified
     * if already wrapped, returns the original wrapper
     * @param activity activity
     * @param viewId the id of view to be wrapped
     * @return the original wrapper or create a new wrapper to wrap the view and replace its place into parent
     */
    public static SmartSwipeWrapper wrap(Activity activity, int viewId) {
        if (activity != null) {
            View view = activity.findViewById(viewId);
            if (view != null) {
                return wrap(view);
            }
        }
        return null;
    }

    /**
     * wrap a view
     * @param view the view to be wrapped
     * @return the original wrapper or create a new wrapper to wrap the view and replace its place into parent
     */
    public static SmartSwipeWrapper wrap(View view) {
        SmartSwipeWrapper wrapper = peekWrapperFor(view);
        if (wrapper != null) {
            return wrapper;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (view.getParent() != null) {
            ViewGroup viewParent = (ViewGroup) view.getParent();
            wrapper = createNewWrapper(view.getContext());
            int index = viewParent.indexOfChild(view);
            viewParent.removeView(view);
            viewParent.addView(wrapper, index, layoutParams);
        } else {
            wrapper = createNewWrapper(view.getContext());
            wrapper.setLayoutParams(layoutParams);
        }
        wrapper.setContentView(view);
        return wrapper;
    }

    /**
     * get wrapper of the specific view
     * @param view view to find wrapper
     * @return the original wrapper of the specific view
     */
    public static SmartSwipeWrapper peekWrapperFor(View view) {
        if (view.getParent() instanceof SmartSwipeWrapper) {
            return (SmartSwipeWrapper) view.getParent();
        }
        return null;
    }

    /**
     * switch direction enable for all {@link SwipeConsumer} that already added to the wrapper
     * @param view the view which be wrapped
     * @param enable true: to enable, false: to disable
     * @param direction direction to enable or disable
     */
    public static void switchDirectionEnable(View view, boolean enable, int direction) {
        enableOrDisableFor(peekWrapperFor(view), enable, direction);
    }

    public static void switchDirectionEnable(Activity activity, boolean enable, int direction) {
        enableOrDisableFor(peekWrapperFor(activity), enable, direction);
    }

    private static void enableOrDisableFor(SmartSwipeWrapper wrapper, boolean enable, int direction) {
        if (wrapper != null) {
            wrapper.enableDirection(direction, enable);
        }
    }

    public static int dp2px(int dp, Context context){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int ensureBetween(int origin, int min, int max) {
        return Math.max(min, Math.min(origin, max));
    }

    public static float ensureBetween(float origin, float min, float max) {
        return Math.max(min, Math.min(origin, max));
    }

    public static double ensureBetween(double origin, double min, double max) {
        return Math.max(min, Math.min(origin, max));
    }


    private static IWrapperFactory factory;

    private static SmartSwipeWrapper createNewWrapper(Context context) {
        final IWrapperFactory factory = SmartSwipe.factory;
        if (factory != null) {
            SmartSwipeWrapper wrapper = factory.createWrapper(context);
            if (wrapper != null) {
                return wrapper;
            }
        }
        return new SmartSwipeWrapper(context);
    }

    /**
     * set the factory of {@link SmartSwipeWrapper}
     * by default, create the {@link SmartSwipeWrapper} instance to wrap if {@link #factory} is null
     * @param factory the factory
     * @see #createNewWrapper(Context)
     */
    public static void setFactory(IWrapperFactory factory) {
        SmartSwipe.factory = factory;
    }

    public interface IWrapperFactory {
        SmartSwipeWrapper createWrapper(Context context);
    }

    static {
        //set wrapper factory automatically
        try {
            //android x
            boolean success = initFactoryByClassName("com.billy.android.swipe.androidx.WrapperFactory");
            if (!success) {
                //android support
                initFactoryByClassName( "com.billy.android.swipe.support.WrapperFactory");
            }
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    private static boolean initFactoryByClassName(String factoryClassName)  {
        Class<?> clazz;
        try {
            clazz = Class.forName(factoryClassName);
            if (clazz != null) {
                Object o = clazz.getConstructor().newInstance();
                if (o instanceof IWrapperFactory) {
                    setFactory((IWrapperFactory) o);
                }
            }
            return true;
        } catch(Exception ignored) {
        }
        return false;
    }
}
