package com.billy.android.swipe.consumer;

import android.view.View;
import com.billy.android.swipe.SwipeConsumer;

/**
 * Sliding with no drawer view, the view below will gradually show up as the contentView moves
 * <pre>
 * Note:
 *      {@link TranslucentSlidingConsumer} works similar to {@link SpaceConsumer}, differences are:
 *      1. {@link SpaceConsumer} is simple, just make contentView movable
 *      2. {@link TranslucentSlidingConsumer} can do something like {@link SlidingConsumer}, such as:
 *              2.1 show shadow while swiping;
 *              2.2 delete from data list with ListView/RecyclerView... on fully swiped
 *      3. {@link TranslucentSlidingConsumer} is the base class of {@link ActivitySlidingBackConsumer}
 * </pre>
 * @author billy.qi
 */
public class TranslucentSlidingConsumer extends SlidingConsumer {

    public TranslucentSlidingConsumer() {
        //set drawer view not required
        setDrawerViewRequired(false);
    }

    @Override
    protected void initChildrenFormXml() {
        //do nothing
    }

    @Override
    public View getDrawerView(int direction) {
        //no drawer view
        return null;
    }

    @Override
    protected void layoutDrawerView() {
        //no drawer to layout
    }

    @Override
    public TranslucentSlidingConsumer setDrawerView(int direction, View drawerView) {
        // add no drawer view
        return this;
    }

    @Override
    protected void changeDrawerViewVisibility(int visibility) {
        // no drawer view to show
    }

    @Override
    public SwipeConsumer setOverSwipeFactor(float overSwipeFactor) {
        // no over scale
        return this;
    }
}
