package com.avaya.android.vantage.basic.activities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.tutorial.PageListener;
import com.avaya.android.vantage.basic.tutorial.Screens;
import com.avaya.android.vantage.basic.tutorial.TutorialPagerAdapter;


/**
 * This activity is made for K155 device tutorial.<br>
 * New tutorial for the K155 device is arranged into paging swipe-able screens,<br>
 * with smooth transitions.
 * <p>
 * If possible, there should be a separate task, to refactor the old {@link TutorialActivity}<br>
 * made for K175, to combine both into a single tutorial activity with this new mechanism,<br>
 * to provide smooth transitions and swiping for the old K175 portrait tutorial as well.<br>
 * Estimated time for that task is roughly day or two. *
 */
public class TutorialActivityK155 extends AppCompatActivity implements View.OnClickListener {

    private ViewPager viewPager;
    private TutorialPagerAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_activity_layout_land);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        final TextView leftButton = (TextView) findViewById(R.id.previous);
        final TextView rightButton = (TextView) findViewById(R.id.next);
        leftButton.setOnClickListener(this);
        rightButton.setOnClickListener(this);

        viewPager = (ViewPager) findViewById(R.id.tutorialViewPager);
        viewPager.setOffscreenPageLimit(Screens.lastScreen().ordinal());
        adapter = new TutorialPagerAdapter(this);
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new PageListener(this, leftButton, rightButton));
    }

    @Override
    public void onClick(View view) {
        if(viewPager!=null) {
            int current = viewPager.getCurrentItem();

            switch (view.getId()) {
                case R.id.previous:
                    if (current == 0) finish();
                    viewPager.setCurrentItem(current - 1);
                    break;
                case R.id.next:
                    if (current == adapter.getCount() - 1) finish();
                    viewPager.setCurrentItem(current + 1);
                    break;
            }
        }
    }

    /**
     * Returns different Tutorial Activity class for portrait or landscape orientation
     * @param context Activity
     * @return Tutorial Activity class
     */
    public static Class<?> resolveTutorialActivity(AppCompatActivity context) {
        return isPort(context) ? TutorialActivity.class : TutorialActivityK155.class;
    }

    /**
     * @param context Activity
     * @return true if {@link DisplayMetrics} height is bigger than the width
     */
    private static boolean isPort(AppCompatActivity context) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.heightPixels > metrics.widthPixels;
    }

}
