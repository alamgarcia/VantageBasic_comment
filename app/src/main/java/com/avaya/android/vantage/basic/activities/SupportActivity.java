package com.avaya.android.vantage.basic.activities;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.fragments.settings.AboutFragment;
import com.avaya.android.vantage.basic.fragments.settings.SupportFragment;

/**
 * {@link SupportActivity} is main hub for launching support activities and fragment which helps
 * user get additional information about application or legal information about Vantage Basic application.
 *
 * From {@link SupportActivity} you have access to {@link TutorialActivity},
 * {@link MainLegalActivity} ,{@link AboutFragment} and {@link SupportFragment}
 */

public class SupportActivity extends AppCompatActivity implements SupportFragment.SupportItemSelected {

    private static final String TAG = "SupportActivity";

    // active fragment tracker
    private int active_fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hideSystemUI(); // enabling immersive mode
        setContentView(R.layout.activity_support);
        fragmentSwitcher("support"); // switch to default fragment

        // setting up toolbar
        Toolbar prefToolbar = (Toolbar) findViewById(R.id.support_toolbar);
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

    /* handling navigation bar back button press */
    @Override
    public void onBackPressed() {
        if (active_fragment == 0) { // if initial fragment is active, close activity
            finish();
        } else {
            fragmentSwitcher("support");
        }
    }


    /* this function is used to control fragments */
    @Override
    public void fragmentSwitcher(String fragmentKey) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        switch (fragmentKey) {
            case "support":
                SupportFragment supportFragment = new SupportFragment();
                fragmentTransaction.replace(R.id.activity_support_container, supportFragment, "support");
                fragmentTransaction.commit();
                setTitle(getText(R.string.support_activity_support));
                active_fragment = 0;
                break;
            case "about":
                /* starting about fragment */
                AboutFragment aboutFragment = new AboutFragment();
                fragmentTransaction.replace(R.id.activity_support_container, aboutFragment, "about");
                fragmentTransaction.commit();
                setTitle(getText(R.string.support_activity_about));
                active_fragment = 1;
                break;
            case "tutorial":
                /* starting tutorial activity */
                Intent intent = new Intent(getApplicationContext(),
                        TutorialActivityK155.resolveTutorialActivity(this));
                startActivity(intent);
                break;
            case "legal":
                /* starting legal activity */
                Intent legalIntent = new Intent(this, MainLegalActivity.class);
                legalIntent.putExtra("startFromSettings", true);
                startActivity(legalIntent);
                break;
            default: /* starting initial fragment */
                Log.e(TAG, "fragmentSwitcher, no fragment recognized");
                break;
        }

    }


    /**
     * Set window to immersive mode.
     */
    private void hideSystemUI() {
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
