package com.billy.android.swipe.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.billy.android.swipe.SmartSwipe;
import com.billy.android.swipe.SmartSwipeWrapper;
import com.billy.android.swipe.SwipeConsumer;
import com.billy.android.swipe.consumer.DoorConsumer;
import com.billy.android.swipe.consumer.DrawerConsumer;
import com.billy.android.swipe.consumer.ShuttersConsumer;
import com.billy.android.swipe.consumer.SlidingConsumer;
import com.billy.android.swipe.consumer.SpaceConsumer;
import com.billy.android.swipe.consumer.StretchConsumer;
import com.billy.android.swipe.demo.consumer.DoorConsumerActivity;
import com.billy.android.swipe.demo.consumer.DrawerConsumerActivity;
import com.billy.android.swipe.demo.consumer.ShuttersConsumerActivity;
import com.billy.android.swipe.demo.consumer.SlidingConsumerActivity;
import com.billy.android.swipe.demo.consumer.SpaceConsumerActivity;
import com.billy.android.swipe.demo.consumer.StretchConsumerActivity;
import com.billy.android.swipe.demo.consumer.SwipeBackBezierConsumerActivity;
import com.billy.android.swipe.demo.consumer.SwipeBackDoorConsumerActivity;
import com.billy.android.swipe.demo.consumer.SwipeBackShuttersConsumerActivity;
import com.billy.android.swipe.demo.consumer.SwipeBackStayConsumerActivity;
import com.billy.android.swipe.demo.consumer.SwipeBackTranslucentConsumerActivity;
import com.billy.android.swipe.demo.consumer.TranslucentConsumerActivity;
import com.billy.android.swipe.listener.SimpleSwipeListener;

import java.text.DecimalFormat;
import java.text.NumberFormat;



/**
 * @author billy.qi
 */
public class MainActivity extends BaseActivity {
    private NumberFormat formatter = new DecimalFormat("0.00");
    private SwipeConsumer mCurrentDrawerConsumer;
    private DrawerConsumer mDrawerConsumer;
    private SlidingConsumer mSlidingConsumer;
    private TextView mTvMainConsumerName;
    private View mMainConsumerSlidePanel;
    private TextView mTvMainConsumerSlideFactor;
    private SeekBar mSlideFactorSeekBar;
    private TextView mTvMainConsumerEdgeSize;
    private SeekBar mEdgeSizeSeekBar;

    TextView shuttersLeavesCountTextView;
    CheckBox shuttersRefreshableCheckBox;
    SeekBar shuttersSeekBar;
    CheckBox doorRefreshableCheckBox;

    private boolean mIsMenuMode;
    private CoverManager mCoverManager;
    private FallingView fallingView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        initCover();

        initSideMenu();

        initListeners();

        toggleConsumer();

    }

    private void initCover() {
        fallingView = findViewById(R.id.falling_view);
        fallingView.addFallObject(100, new FallingView.FallObject.Builder(getResources().getDrawable(R.drawable.icon_snow))
                .setSpeed(100,true)
                .setSize(50,50,true)
                .setWind(5,true,true)
                )
        ;

        //manage the app cover
        View mCover = findViewById(R.id.cover);
        mCoverManager = CoverManager.manage(mCover)
                .setWidth(screenWidth)
                .setHeight(screenHeight)
                .setCoverListener(new CoverManager.CoverListener() {
                    @Override
                    public void onOpened() {
                        if (mCurrentDrawerConsumer != null) {
                            mCurrentDrawerConsumer.unlockAllDirections();
                        }
                        fallingView.refresh();
                        fallingView.stopFalling();
                    }

                    @Override
                    public void onClosed() {
                        if (mCurrentDrawerConsumer != null) {
                            mCurrentDrawerConsumer.lockAllDirections();
                        }
                        fallingView.startFalling();
                    }
                })
        ;
    }

    private void initSideMenu() {
        //demo to specified size of drawer
        int size = SmartSwipe.dp2px(300, this);

        //create horizontal menu view: stretch top & bottom
        View horizontalMenu = LayoutInflater.from(this).inflate(R.layout.layout_main_menu, null);
        horizontalMenu.setLayoutParams(new ViewGroup.LayoutParams(size, ViewGroup.LayoutParams.MATCH_PARENT));
        SmartSwipeWrapper horizontalMenuWrapper = SmartSwipe.wrap(horizontalMenu).addConsumer(new StretchConsumer()).enableVertical().getWrapper();

        //create top menu view: Space on top drag
        View topMenu = LayoutInflater.from(this).inflate(R.layout.layout_main_menu, null);
        topMenu.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, size));
//        SmartSwipeWrapper topMenuWrapper = SmartSwipe.wrap(topMenu).addConsumer(new SpaceConsumer()).enableTop().getWrapper();
        SmartSwipeWrapper topMenuWrapper = SmartSwipe.wrap(topMenu)
                .addConsumer(new SpaceConsumer())
                .enableTop()
                //enable & lock bottom, and enable bottom nested fly,
                // let it to consume bottom nested fly event
                // try it with demo apk!
                .enableBottom().enableNestedScrollBottom(false).enableNestedFlyBottom(true)
                .addListener(new SimpleSwipeListener() {

                    @Override
                    public void onSwipeStart(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                        if (direction == SwipeConsumer.DIRECTION_BOTTOM) {
                            wrapper.setNestedScrollingEnabled(false);
                        }
                    }

                    @Override
                    public void onSwipeClosed(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                        wrapper.setNestedScrollingEnabled(true);
                    }
                })
                .getWrapper();

        //create bottom menu view: Space on bottom drag
        View bottomMenu = LayoutInflater.from(this).inflate(R.layout.layout_main_menu, null);
        bottomMenu.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, size));
        SmartSwipeWrapper bottomMenuWrapper = SmartSwipe.wrap(bottomMenu).addConsumer(new SpaceConsumer()).enableBottom().getWrapper();

        SimpleSwipeListener listener = new SimpleSwipeListener() {
            @Override
            public void onSwipeOpened(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                super.onSwipeOpened(wrapper, consumer, direction);
                setLeftBtnImage(false);
            }

            @Override
            public void onSwipeClosed(SmartSwipeWrapper wrapper, SwipeConsumer consumer, int direction) {
                super.onSwipeClosed(wrapper, consumer, direction);
                setLeftBtnImage(true);
            }
        };

        mDrawerConsumer = new DrawerConsumer()
                //horizontal menu
                .setHorizontalDrawerView(horizontalMenuWrapper)
                //top menu
                .setTopDrawerView(topMenuWrapper)
                //bottom menu
                .setBottomDrawerView(bottomMenuWrapper)
                //set the translucent color of scrim (default is 0:transparent)
                .setScrimColor(0x7F000000)
                //set the shadow color follow the drawer while swiping (default is 0:transparent)
                .setShadowColor(0x80000000)
                .setShadowSize(SmartSwipe.dp2px(10, this))
                .addListener(listener)
                //set edge size to swipe to 20dp (default is 0: whole range of the contentView bounds)
                .setEdgeSize(SmartSwipe.dp2px(20, this))
                .as(DrawerConsumer.class);

        mSlidingConsumer = new SlidingConsumer()
                .setDrawerExpandable(true)
                //horizontal menu
                .setHorizontalDrawerView(horizontalMenuWrapper)
                //top menu
                .setTopDrawerView(topMenuWrapper)
                //bottom menu
                .setBottomDrawerView(bottomMenuWrapper)
                .showScrimAndShadowOutsideContentView()
                //set the translucent color of scrim (default is 0:transparent)
                .setScrimColor(0x7F000000)
                .setShadowSize(SmartSwipe.dp2px(10, this))
                .setShadowColor(0x80000000)
                .addListener(listener)
                //set edge size to swipe to 20dp (default is 0: whole range of the contentView bounds)
                .setEdgeSize(SmartSwipe.dp2px(20, this))
                .as(SlidingConsumer.class);
    }

    @Override
    protected void onDestroy() {
        if (fallingView != null) {
            fallingView.stopFalling();
        }
        super.onDestroy();
    }

    private void setLeftBtnImage(boolean menuMode) {
        if (menuMode ^ mIsMenuMode) {
            mIsMenuMode = menuMode;
            setLeftBtnImage(menuMode ? R.drawable.icon_menu : R.drawable.icon_back);
        }
    }

    private void initViews() {
        mTvMainConsumerName = findViewById(R.id.main_ui_name);
        mMainConsumerSlidePanel = findViewById(R.id.main_ui_slide_panel);
        mTvMainConsumerSlideFactor = findViewById(R.id.main_ui_slide_factor);
        mSlideFactorSeekBar = findViewById(R.id.main_ui_slide_factor_seek_bar);
        mTvMainConsumerEdgeSize = findViewById(R.id.main_ui_edge_size);
        mEdgeSizeSeekBar = findViewById(R.id.main_ui_edge_size_seek_bar);

        //demo to use SmartSwipe within xml layout
        SmartSwipeWrapper wrapper = findViewById(R.id.main_ui_wrap_view);
        wrapper.addConsumer(new SlidingConsumer()).setRelativeMoveFactor(SlidingConsumer.FACTOR_FOLLOW);


        shuttersLeavesCountTextView = findViewById(R.id.cover_shutters_leaves_count_number);
        shuttersRefreshableCheckBox = findViewById(R.id.cover_shutters_refreshable);
        shuttersSeekBar = findViewById(R.id.cover_shutters_leaves_count_seek_bar);
        doorRefreshableCheckBox = findViewById(R.id.cover_door_refreshable);

        setLeftBtnImage(true);
    }

    private void initListeners() {
        mSlideFactorSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSlidingConsumer.setRelativeMoveFactor(progress * 0.01F);
                float result = mSlidingConsumer.getRelativeMoveFactor();
                mTvMainConsumerSlideFactor.setText(formatter.format(result));
            }
        });
        mEdgeSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCurrentDrawerConsumer.setEdgeSize(progress);
                if (progress == 0) {
                    mTvMainConsumerEdgeSize.setText(R.string.demo_main_group_ui_edge_full);
                } else {
                    mTvMainConsumerEdgeSize.setText(progress + "px");
                }
            }
        });
        findViewById(R.id.main_ui_toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleConsumer();
            }
        });
    }

    private void toggleConsumer() {
        SwipeConsumer consumer = mCurrentDrawerConsumer == mSlidingConsumer ? mDrawerConsumer : mSlidingConsumer;
        //add swipe consumer to this activity
        mCurrentDrawerConsumer = SmartSwipe.wrap(this)
                //remove current consumer
                .removeConsumer(mCurrentDrawerConsumer)
                //add new consumer to this activity wrapper
                .addConsumer(consumer);
        mTvMainConsumerName.setText(mCurrentDrawerConsumer.getClass() == SlidingConsumer.class ? R.string.demo_ui_SlidingConsumer : R.string.demo_ui_DrawerConsumer);
        mMainConsumerSlidePanel.setVisibility(mCurrentDrawerConsumer == mSlidingConsumer ? View.VISIBLE : View.GONE);
        mEdgeSizeSeekBar.setProgress(mCurrentDrawerConsumer.getEdgeSize());
        View avatarView = findViewById(R.id.avatar);
        if (avatarView != null) {
            avatarView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    forwardToActivity(SwipeBackTranslucentConsumerActivity.class);
                }
            });
        }
        if (mCurrentDrawerConsumer == mSlidingConsumer) {
            float factor = mSlidingConsumer.getRelativeMoveFactor();
            mTvMainConsumerSlideFactor.setText(String.valueOf(factor));
            mSlideFactorSeekBar.setProgress((int) (factor * 100));
        }
    }

    @Override
    public int getTitleResId() {
        return R.string.app_name;
    }

    @Override
    protected void onLeftTitleBtnClick() {
        if (mIsMenuMode) {
            mCurrentDrawerConsumer.smoothLeftOpen();
        } else {
            mCurrentDrawerConsumer.smoothClose();
        }
    }

    public void showShuttersCover(View view) {
        if (mCoverManager != null) {
            mCoverManager.showShuttersMode();
            refreshCoverOptionPanel();
        }
    }

    public void showDoorCover(View view) {
        if (mCoverManager != null) {
            mCoverManager.doorMode();
            refreshCoverOptionPanel();
        }
    }

    public void showDrawerCover(View view) {
        if (mCoverManager != null) {
            mCoverManager.drawerMode();
            refreshCoverOptionPanel();
        }
    }

    private void refreshCoverOptionPanel() {
        if (mCurrentDrawerConsumer != null) {
            mCurrentDrawerConsumer.lockAllDirections();
        }
        Class<? extends SwipeConsumer> clazz = mCoverManager.consumer.getClass();
        findViewById(R.id.panel_shutters).setVisibility(clazz == ShuttersConsumer.class ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.panel_door).setVisibility(clazz == DoorConsumer.class ? View.VISIBLE : View.INVISIBLE);
        if (clazz == ShuttersConsumer.class) {
            ShuttersConsumer consumer = (ShuttersConsumer) mCoverManager.consumer;
            shuttersLeavesCountTextView.setText(String.valueOf(consumer.getLeavesCount()));
            shuttersRefreshableCheckBox.setChecked(consumer.isRefreshable());
            shuttersSeekBar.setProgress(consumer.getLeavesCount());
            shuttersSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    ShuttersConsumer consumer = (ShuttersConsumer) mCoverManager.consumer;
                    consumer.setLeavesCount(progress);
                    shuttersLeavesCountTextView.setText(String.valueOf(consumer.getLeavesCount()));
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        } else if (clazz == DoorConsumer.class) {
            DoorConsumer consumer = (DoorConsumer) mCoverManager.consumer;
            doorRefreshableCheckBox.setChecked(consumer.isRefreshable());;
        }
    }

    public void toggleShuttersRefresh(View view) {
        if (view instanceof CheckBox && mCoverManager.consumer instanceof ShuttersConsumer) {
            ((ShuttersConsumer)mCoverManager.consumer).setRefreshable(((CheckBox) view).isChecked());
        }
    }

    @Override
    public void onBackPressed() {
        if (mCoverManager != null && !mCoverManager.isOpened()) {
            //open the cover automatically
            mCoverManager.open();
        } else if (mCurrentDrawerConsumer != null && !mCurrentDrawerConsumer.isClosed()) {
            //close the drawer automatically
            mCurrentDrawerConsumer.close(true);
        } else {
            //finish activity
            finish();
        }
    }

    private void forwardToActivity(Class<? extends Activity> clazz) {
        startActivity(new Intent(this, clazz));
        //auto close drawer view
//        mCurrentDrawerConsumer.smoothClose();
    }

    public void toSpaceConsumerActivity(View view) {
        forwardToActivity(SpaceConsumerActivity.class);
    }

    public void toStretchConsumerActivity(View view) {
        forwardToActivity(StretchConsumerActivity.class);
    }

    public void toDrawerConsumerActivity(View view) {
        forwardToActivity(DrawerConsumerActivity.class);
    }

    public void toBezierConsumerActivity(View view) {
        forwardToActivity(SwipeBackBezierConsumerActivity.class);
    }

    public void toStayConsumerActivity(View view) {
        forwardToActivity(SwipeBackStayConsumerActivity.class);
    }

    public void toSwipeBackTranslucentConsumerActivity(View view) {
        forwardToActivity(SwipeBackTranslucentConsumerActivity.class);
    }

    public void toSlideConsumerActivity(View view) {
        forwardToActivity(SlidingConsumerActivity.class);
    }

    public void toTranslucentConsumerActivity(View view) {
        forwardToActivity(TranslucentConsumerActivity.class);
    }

    public void toDoorConsumerActivity(View view) {
        forwardToActivity(DoorConsumerActivity.class);
    }

    public void toActivityDoorBackConsumerActivity(View view) {
        forwardToActivity(SwipeBackDoorConsumerActivity.class);
    }

    public void toShuttersConsumerActivity(View view) {
        forwardToActivity(ShuttersConsumerActivity.class);
    }

    public void toActivityShuttersBackConsumerActivity(View view) {
        forwardToActivity(SwipeBackShuttersConsumerActivity.class);
    }
}
