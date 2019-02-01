package com.avaya.android.vantage.basic.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;

import com.avaya.android.vantage.basic.GoogleAnalyticsUtils;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;

/**
 * EULA Screen. Displayed when the app starts for the first time or if picked from Settings.
 * Activity should be shown only once per user login application launch.
 */
public class MainLegalActivity extends AppCompatActivity {

    private static final String EULA_URL = "file:///android_asset/EULA_V2.htm";

    private static final String KEY_EULA_ACCEPTED = "eula_accepted";
    private static final String EULA_PREFS_NAME = "eula_preferences";

    public static final String ACTION_REQUEST_LOGOUT = "com.avaya.endpoint.action.REQUEST_LOGOUT";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.legal_webview_layout);
        //hideSystemUI();
        boolean startedFromSettings = getIntent().getBooleanExtra("startFromSettings", false);
        // if application is download from Google Play don't create shortcut, it will be created automatically.
        // ELAN-705 - if application is installed by push application Shortcut will be installed by push manager (install via adb will not create shortcut)
        /*if(!DummyContent.isStoreVersion(this)) {
            addShortcutIcon();
        }*/
        setupUI(startedFromSettings);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
        }
        return true;
    }

    /**
     * Load Webview with EULA.
     *
     * @param startedFromSettings show accept/decline buttons to user.
     */
    private void setupUI(boolean startedFromSettings) {

        LinearLayout containerLayout = (LinearLayout) findViewById(R.id.eula_container);

        if (startedFromSettings) {
            showToolbar();
            LinearLayout mButtonContainerLayout = (LinearLayout) findViewById(R.id.button_container);
            if (mButtonContainerLayout != null) {
                mButtonContainerLayout.setVisibility(View.GONE);
            }
            if (containerLayout != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) containerLayout.getLayoutParams();
                params.bottomMargin = 0;
                containerLayout.setLayoutParams(params);
            }
        } else {
            Button mAcceptLegalButton = (Button) findViewById(R.id.accept_legal);
            if (mAcceptLegalButton != null) {
                mAcceptLegalButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SharedPreferences preferences = getSharedPreferences(EULA_PREFS_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(KEY_EULA_ACCEPTED, true);
                        editor.apply();
                        startMainActivity();
                        GoogleAnalyticsUtils.logEvent(Utils.isK155() ? GoogleAnalyticsUtils.Event.K155_EULA_ACCEPT_EVENT : GoogleAnalyticsUtils.Event.K175_EULA_ACCEPT_EVENT);
                    }
                });
            }
            Button mDeclineLegalButton = (Button) findViewById(R.id.decline_legal);
            if (mDeclineLegalButton != null) {
                mDeclineLegalButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showLogoutDialog();
                    }
                });
            }
        }

        WebView webView = new WebView(this);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

        if (containerLayout != null) {
            containerLayout.addView(webView);
            webView.loadUrl(EULA_URL);
        }
    }


    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getApplicationContext().getString(R.string.logout));
        builder.setMessage(getApplicationContext().getString(R.string.logout_msg));
        // Set up the buttons
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendBroadcast(new Intent(ACTION_REQUEST_LOGOUT));
                finish();
            }
        });
        builder.show();

    }



    /**
     * This method makes icon shortcut on homescreen
     * It will be call only when mUser first time run application, after he accept terms of use.
     * After application installation it will be visible on home screen and in application list.
     */
    private void addShortcutIcon() {
        Intent shortcutIntent = new Intent(getApplicationContext(),
                SplashActivity.class);

        shortcutIntent.setAction(Intent.ACTION_MAIN);
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.mipmap.ic_launcher));

        removeShortcut();

        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);

    }

    /**
     * Remove Home screen shortcut
     */
    private void removeShortcut() {
        //Deleting shortcut for MainActivity
        //on Home screen
        Intent shortcutIntent = new Intent(getApplicationContext(),
                SplashActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.mipmap.ic_launcher));

        addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);
    }

    /**
     * Show toolbar with action bar on top of the screen
     */
    private void showToolbar() {

        // setting up toolbar
        Toolbar prefToolbar = (Toolbar) findViewById(R.id.eula_toolbar);
        if (prefToolbar != null) {
            prefToolbar.setVisibility(View.VISIBLE);
        }
        setSupportActionBar(prefToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    /**
     * Lunch {@link MainActivity} on user agreement with EULA
     */
    private void startMainActivity() {
        final Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
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