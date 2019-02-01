package com.avaya.android.vantage.basic.tutorial;

import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.avaya.android.vantage.basic.R;

/**
 * A model enum for each screen in the K155 device, landscape tutorial.<br>
 * Each screen has its own resource id for: title, layout and progress indicator circle.
 *
 */
public enum Screens {

    QuickTutorial(R.string.quick_tutorial, R.layout.tutorial_1_quick_settings, R.id.tutorial_dot_1),
    NavigationButtons(R.string.tutorialNavigationButtons, R.layout.tutorial_2_navigation_buttons, R.id.tutorial_dot_2),
    Main(R.string.tutorialMain, R.layout.tutorial_3_main, R.id.tutorial_dot_3),
    ActiveCall(R.string.tutorialActiveCall, R.layout.tutorial_4_active_call, R.id.tutorial_dot_4),
    Contacts(R.string.contacts, R.layout.tutorial_5_contacts, R.id.tutorial_dot_5),
    History(R.string.tutorialHistory, R.layout.tutorial_6_history, R.id.tutorial_dot_6);

    public final @StringRes int titleRes;
    public final @LayoutRes int layoutRes;
    public final @IdRes int indicatorIdRes;

    Screens(@StringRes int titleRes, @LayoutRes int layoutRes, @IdRes int indicatorIdRes) {
        this.titleRes = titleRes;
        this.layoutRes = layoutRes;
        this.indicatorIdRes = indicatorIdRes;
    }

    /**
     * Utility method for creating an array of indicator dots, to be managed per screen swipe.
     *
     * @param container {@link View} containing indicator dots(small circles)
     * @return View[] of selection indicators
     */
    public static View[] indicators(AppCompatActivity container) {
        Screens[] screens = Screens.values();
        final int length = screens.length;
        View[] indicators = new View[length];
        for (int i = 0; i < length; i++) {
            indicators[i] = container.findViewById(screens[i].indicatorIdRes);
            indicators[i].setEnabled(i == 0); // enable first, disable others
        }
        return indicators;
    }

    /**
     * Method enables current screen indicator and disables adjacent if available.
     *
     * @param indicators current screen indicators in the form of small circles, derived from the<br>
     *                   indicatorRes for each Screen e.g.: {@link Screens#QuickTutorial}
     */
    public void enableCurrentIndicator(View[] indicators) {
        final int last = indicators.length - 1;
        final int current = ordinal();
        final int prev = current - 1;
        final int next = current + 1;
        if (current > 0) indicators[prev].setEnabled(false);
        if (current < last) indicators[next].setEnabled(false);
        indicators[current].setEnabled(true);
    }

    /**
     * Utility method for setting title of the current screen
     *
     * @param title {@link TextView} where the selected screen title is displayed
     */
    public void setTitle(TextView title) {
        title.setText(titleRes);
    }

    /**
     * Extracts strings from resources into passed {@link CharSequence}[]
     *
     * @param activity context to resolve string resource from
     * @return titleArray {@link CharSequence}[] filled from corresponding {@link Screens} resources
     */
    public static CharSequence[] fillTitleArray(AppCompatActivity activity) {
        Screens[] screens = Screens.values();
        CharSequence[] titleArray = new CharSequence[screens.length];
        for (int i = 0; i < titleArray.length; i++) {
            titleArray[i] = activity.getResources().getString(screens[i].titleRes);
        }
        return titleArray;
    }

    /**
     * Utility method for the first screen in the enum
     *
     * @return first element of the {@link Screens} enum
     */
    public static Screens firstScreen() {
        return Screens.values()[0];
    }

    /**
     * Utility method for the last screen in the enum
     *
     * @return last element of the {@link Screens} enum
     */
    public static Screens lastScreen() {
        return Screens.values()[Screens.values().length - 1];
    }
}
