package com.avaya.android.vantage.basic.activities;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.fragments.settings.UserPreferencesFragment;
import com.avaya.android.vantage.basic.fragments.settings.UserSettingsApplicationsFragment;
import com.avaya.android.vantage.basic.fragments.settings.UserSettingsAudioVideoFragment;

/**
 * {@link UserPreferencesActivity} creates in activity preference screen where user can change
 * some basic changes in way Vantage Basic application work. Activity contains of  {@link UserPreferencesFragment},
 * {@link UserSettingsApplicationsFragment} and {@link UserSettingsAudioVideoFragment}.
 */

public class UserPreferencesActivity extends AppCompatActivity implements UserPreferencesFragment.SettingsItemSelected {

    private static final String TAG = "SupportActivity";

    // active fragment tracker
    private int active_fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_preferences);
        fragmentSwitcher("initial"); // switch to default fragment

        // setting up toolbar
        Toolbar prefToolbar = (Toolbar) findViewById(R.id.preferences_toolbar);
        setSupportActionBar(prefToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    /* handling bottom navigation bar back button press */
    @Override
    public void onBackPressed() {
        if (active_fragment == 0) {
            // if initial fragment is active, close activity
            finish();
        } else {
            fragmentSwitcher("initial");
        }
    }

    /* this function is used to control fragments */
    @Override
    public void fragmentSwitcher(String fragmentKey) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        switch (fragmentKey) {
            case "initial":
                UserPreferencesFragment preferenceFragment = new UserPreferencesFragment();
                fragmentTransaction.replace(R.id.activity_user_preferences_container, preferenceFragment, "default");
                fragmentTransaction.commit();
                setTitle(getText(R.string.user_settings));
                active_fragment = 0;
                break;
            case "application": /* starting application settings fragment */
                UserSettingsApplicationsFragment userSettingsApplicationsFragment = new UserSettingsApplicationsFragment();
                fragmentTransaction.replace(R.id.activity_user_preferences_container, userSettingsApplicationsFragment, "application");
                fragmentTransaction.commit();
                setTitle(getText(R.string.application_settings));
                active_fragment = 1;
                break;
            case "audioVideo": /* starting audio/video settings fragment */
                UserSettingsAudioVideoFragment userSettingsAudioVideoFragment = new UserSettingsAudioVideoFragment();
                fragmentTransaction.replace(R.id.activity_user_preferences_container, userSettingsAudioVideoFragment, "audioVideo");
                fragmentTransaction.commit();
                if (!Utils.isCameraSupported()) {
                    setTitle(getText(R.string.audio_settings));
                } else {
                    setTitle(getText(R.string.audio_video_settings));
                }
                active_fragment = 2;
                break;
            default:
                Log.e(TAG, "fragmentSwitcher, no fragment recognized");
                break;

        }

    }

    /**
     * Set window to immersive mode.
     */
    public void hideSystemUI() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

}
