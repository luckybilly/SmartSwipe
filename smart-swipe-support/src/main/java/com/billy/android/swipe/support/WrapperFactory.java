package com.billy.android.swipe.support;

import android.content.Context;
import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeWrapper;

/**
 * @author billy.qi
 */
public class WrapperFactory implements SmartSwipe.IWrapperFactory {
    @Override
    public SmartSwipeWrapper createWrapper(Context context) {
        return new SmartSwipeWrapperSupport(context);
    }
}
