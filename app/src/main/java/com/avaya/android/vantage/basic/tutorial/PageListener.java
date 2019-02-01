package com.avaya.android.vantage.basic.tutorial;

import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.avaya.android.vantage.basic.R;

public class PageListener implements ViewPager.OnPageChangeListener {

    private View[] indicators;
    private int current = 0;
    private final Screens first;
    private final Screens last;

    private TextView leftButton;
    private TextView rightButton;

    public PageListener(AppCompatActivity activity, TextView leftButton, TextView rightButton) {
        this.indicators = Screens.indicators(activity);
        this.leftButton = leftButton;
        this.rightButton = rightButton;

        first = Screens.firstScreen();
        first.enableCurrentIndicator(indicators);
        last = Screens.lastScreen();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(final int position) {
        final Screens currentScreen = Screens.values()[position];
        if (current != position) {
            currentScreen.enableCurrentIndicator(indicators);
            switch (currentScreen) { // change, screen corresponding, button text, minimum times
                case QuickTutorial:
                    if (current > first.ordinal()) leftButton.setText(R.string.skip_button);
                    break;
                case History:
                    if (current < last.ordinal()) rightButton.setText(R.string.done);
                    break;
                default:
                    if (current == first.ordinal()) leftButton.setText(R.string.back);
                    else if (current == last.ordinal()) rightButton.setText(R.string.next);
            }
            current = position;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
