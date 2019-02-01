package com.avaya.android.vantage.basic.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.csdk.ErrorManager;
import com.avaya.android.vantage.basic.model.ErrorNotificationAlert;
import com.avaya.android.vantage.basic.views.adapters.ErrorNotificationListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity used for Error overlay message. Messages which are shown by this activity are connected
 * to SIP login failure, unavailability of Aura server or unified login failure.
 */
public class ErrorMessageActivity extends AppCompatActivity {

    RecyclerView mErrorRecyclerList;
    List<ErrorNotificationAlert> mErrorNotificationList;
    ErrorNotificationListAdapter mErrorAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.error_overlay_notification_layout);
        mErrorRecyclerList = (RecyclerView) findViewById(R.id.error_list);
        mErrorNotificationList = new ArrayList<>();
        boolean[] activeErrorList = ErrorManager.getInstance().getErrorList();
        for (int i = 0; i < activeErrorList.length; i++) {
            if (activeErrorList[i]) {
                addErrorNotificationToList(i);
            }
        }
        mErrorAdapter = new ErrorNotificationListAdapter(mErrorNotificationList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mErrorRecyclerList.setLayoutManager(mLayoutManager);
        mErrorRecyclerList.setItemAnimator(new DefaultItemAnimator());
        mErrorRecyclerList.setAdapter(mErrorAdapter);
    }

    /**
     * Add error notification to RecyclerView.
     *
     * @param errorCode Code number of error.
     */
    private void addErrorNotificationToList(int errorCode) {
        String title = ErrorManager.getErrorTitle(getApplicationContext(), errorCode);
        String message = ErrorManager.getErrorMessage(getApplicationContext(), errorCode);

        mErrorNotificationList.add(new ErrorNotificationAlert(title, message));
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

    /**
     * Touch outside of list will close error Activity.
     *
     * @param view RecyclerView background.
     */
    public void closeErrorNotification(View view) {
        //TODO test deleting of errors
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }
}
