package com.billy.android.swipe.internal;

import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.widget.AbsListView;

import static android.os.Build.VERSION.SDK_INT;

/**
 * @author billy.qi
 * @since 2019-05-23 11:25
 */
public class ViewCompat {

    /**
     * Indicates that the input type for the gesture is from a user touching the screen.
     */
    public static final int TYPE_TOUCH = 0;

    /**
     * Indicates that the input type for the gesture is caused by something which is not a user
     * touching a screen. This is usually from a fling which is settling.
     */
    public static final int TYPE_NON_TOUCH = 1;

    /**
     * Indicates no axis of view scrolling.
     */
    public static final int SCROLL_AXIS_NONE = 0;

    /**
     * Indicates scrolling along the horizontal axis.
     */
    public static final int SCROLL_AXIS_HORIZONTAL = 1 << 0;

    /**
     * Indicates scrolling along the vertical axis.
     */
    public static final int SCROLL_AXIS_VERTICAL = 1 << 1;

    private static ThreadLocal<Rect> sThreadLocalRect;

    private static Rect getEmptyTempRect() {
        if (sThreadLocalRect == null) {
            sThreadLocalRect = new ThreadLocal<>();
        }
        Rect rect = sThreadLocalRect.get();
        if (rect == null) {
            rect = new Rect();
            sThreadLocalRect.set(rect);
        }
        rect.setEmpty();
        return rect;
    }

    /**
     * <p>Cause an invalidate to happen on the next animation time step, typically the
     * next display frame.</p>
     *
     * <p>This method can be invoked from outside of the UI thread
     * only when this View is attached to a window.</p>
     *
     * @param view View to invalidate
     */
    public static void postInvalidateOnAnimation(View view) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.postInvalidateOnAnimation();
        } else {
            view.postInvalidate();
        }
    }
    /**
     * Offset this view's vertical location by the specified number of pixels.
     * @param view view
     * @param offset the number of pixels to offset the view by
     */
    public static void offsetTopAndBottom(View view, int offset) {
        if (view == null || offset == 0) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            view.offsetTopAndBottom(offset);
        } else if (Build.VERSION.SDK_INT >= 21) {
            final Rect parentRect = getEmptyTempRect();
            boolean needInvalidateWorkaround = false;

            final ViewParent parent = view.getParent();
            if (parent instanceof View) {
                final View p = (View) parent;
                parentRect.set(p.getLeft(), p.getTop(), p.getRight(), p.getBottom());
                // If the view currently does not currently intersect the parent (and is therefore
                // not displayed) we may need need to invalidate
                needInvalidateWorkaround = !parentRect.intersects(view.getLeft(), view.getTop(),
                        view.getRight(), view.getBottom());
            }

            // Now offset, invoking the API 14+ implementation (which contains its own workarounds)
            compatOffsetTopAndBottom(view, offset);

            // The view has now been offset, so let's intersect the Rect and invalidate where
            // the View is now displayed
            if (needInvalidateWorkaround && parentRect.intersect(view.getLeft(), view.getTop(),
                    view.getRight(), view.getBottom())) {
                ((View) parent).invalidate(parentRect);
            }
        } else {
            compatOffsetTopAndBottom(view, offset);
        }
    }

    private static void compatOffsetTopAndBottom(View view, int offset) {
        view.offsetTopAndBottom(offset);
        if (view.getVisibility() == View.VISIBLE) {
            tickleInvalidationFlag(view);

            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                tickleInvalidationFlag((View) parent);
            }
        }
    }

    /**
     * Offset this view's horizontal location by the specified amount of pixels.
     *
     * @param view view
     * @param offset the number of pixels to offset the view by
     */
    public static void offsetLeftAndRight(View view, int offset) {
        if (view == null || offset == 0) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            view.offsetLeftAndRight(offset);
        } else if (Build.VERSION.SDK_INT >= 21) {
            final Rect parentRect = getEmptyTempRect();
            boolean needInvalidateWorkaround = false;

            final ViewParent parent = view.getParent();
            if (parent instanceof View) {
                final View p = (View) parent;
                parentRect.set(p.getLeft(), p.getTop(), p.getRight(), p.getBottom());
                // If the view currently does not currently intersect the parent (and is therefore
                // not displayed) we may need need to invalidate
                needInvalidateWorkaround = !parentRect.intersects(view.getLeft(), view.getTop(),
                        view.getRight(), view.getBottom());
            }

            // Now offset, invoking the API 14+ implementation (which contains its own workarounds)
            compatOffsetLeftAndRight(view, offset);

            // The view has now been offset, so let's intersect the Rect and invalidate where
            // the View is now displayed
            if (needInvalidateWorkaround && parentRect.intersect(view.getLeft(), view.getTop(),
                    view.getRight(), view.getBottom())) {
                ((View) parent).invalidate(parentRect);
            }
        } else {
            compatOffsetLeftAndRight(view, offset);
        }
    }

    private static void compatOffsetLeftAndRight(View view, int offset) {
        view.offsetLeftAndRight(offset);
        if (view.getVisibility() == View.VISIBLE) {
            tickleInvalidationFlag(view);

            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                tickleInvalidationFlag((View) parent);
            }
        }
    }

    private static void tickleInvalidationFlag(View view) {
        final float y = view.getTranslationY();
        view.setTranslationY(y + 1);
        view.setTranslationY(y);
    }


    /**
     * <p>Convert script specific gravity to absolute horizontal value.</p>
     *
     * if horizontal direction is LTR, then START will set LEFT and END will set RIGHT.
     * if horizontal direction is RTL, then START will set RIGHT and END will set LEFT.
     *
     *
     * @param gravity The gravity to convert to absolute (horizontal) values.
     * @param layoutDirection The layout direction.
     * @return gravity converted to absolute (horizontal) values.
     */
    public static int getAbsoluteGravity(int gravity, int layoutDirection) {
        if (SDK_INT >= 17) {
            return Gravity.getAbsoluteGravity(gravity, layoutDirection);
        } else {
            // Just strip off the relative bit to get LEFT/RIGHT.
            return gravity & ~Gravity.RELATIVE_LAYOUT_DIRECTION;
        }
    }
    public static int getLayoutDirection(View view) {
        if (Build.VERSION.SDK_INT >= 17) {
            return view.getLayoutDirection();
        }
        return View.LAYOUT_DIRECTION_LTR;
    }


    /**
     * Check if the items in the list can be scrolled in a certain direction.
     *
     * @param listView listView
     * @param direction Negative to check scrolling up, positive to check
     *            scrolling down.
     * @return true if the list can be scrolled in the specified direction,
     *         false otherwise.
     */
    public static boolean canListViewScrollVertical(AbsListView listView, int direction) {
        if (Build.VERSION.SDK_INT >= 19) {
            // Call the framework version directly
            return listView.canScrollList(direction);
        } else {
            // provide backport on earlier versions
            final int childCount = listView.getChildCount();
            if (childCount == 0) {
                return false;
            }

            final int firstPosition = listView.getFirstVisiblePosition();
            if (direction > 0) {
                final int lastBottom = listView.getChildAt(childCount - 1).getBottom();
                final int lastPosition = firstPosition + childCount;
                return lastPosition < listView.getCount()
                        || (lastBottom > listView.getHeight() - listView.getListPaddingBottom());
            } else {
                final int firstTop = listView.getChildAt(0).getTop();
                return firstPosition > 0 || firstTop < listView.getListPaddingTop();
            }
        }
    }
}
