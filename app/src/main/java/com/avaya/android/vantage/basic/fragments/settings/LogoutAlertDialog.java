package com.avaya.android.vantage.basic.fragments.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.csdk.SDKManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * This is a dialog fragment that is popped up whenever some service effective configuration parameter has changed.
 * The user is proposed option to perform silent logout/login in order the new value of the parameter will take effect.
 * User can also postpone the logout/login.
 */

public class LogoutAlertDialog extends DialogFragment {

    private final static String LOG_TAG = "LogoutAlertDialog";
    private static final String LOGOUT_ALERT_TAG = "LogoutAlertDialog";
    private static final int LOGOUT_ALERT_ID = 1;

    private boolean mRefreshNeeded;

    /**
     * Obtaining isntance of {@link LogoutAlertDialog}
     * @param UIRefreshNeeded boolean which tell us should we refresh UI
     * @return {@link LogoutAlertDialog}
     */
    public static LogoutAlertDialog newInstance(boolean UIRefreshNeeded) {
        LogoutAlertDialog frag = new LogoutAlertDialog();
        frag.setCancelable(false);
        frag.setUIRefresh(UIRefreshNeeded);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final String title = getActivity().getString(R.string.logout_to_apply_changes_title);
        final String text = getActivity().getString(R.string.logout_to_apply_changes_text);
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton(R.string.apply_changes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                logoutAndApplyChanges();
                            }
                        }
                )
                .setNegativeButton(R.string.postpone_logout,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                postponeLogout();
                            }
                        }
                )
                .create();
    }

    /**
     * This fucntion creates the alert dialog and adds it to the
     * current ongoing activity as a fragment.
     *
     * @param withUIRefresh determines if we will refresh UI
     */
    public static void showLogoutToApplyChangesDialog(boolean withUIRefresh) {

        DialogFragment newFragment = LogoutAlertDialog.newInstance(withUIRefresh);
        Activity curActivity = LogoutAlertDialog.getCurrentActivity();
        if (curActivity != null) {
            newFragment.show(curActivity.getFragmentManager(), "dialog");
        } else {
            Log.d(LOG_TAG, "Can not ask for logout since no current activity.");
        }
    }

    /**
     * Set refreshing of UI if needed
     *
     * @param refreshNeeded determines if we will refresh UI
     */
    private void setUIRefresh(boolean refreshNeeded) {
        mRefreshNeeded = refreshNeeded;
    }

    /**
     * This function finds the current ongoing activity
     *
     * @return current Activity
     */
    private static Activity getCurrentActivity() {

        Field activitiesField;
        Object activityThread;
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            //noinspection unchecked
            activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            activitiesField = activityThreadClass.getDeclaredField("mActivities");
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            Log.d(LOG_TAG, "Can not find current activity due to " + e);
            return null;
        }
        activitiesField.setAccessible(true);

        Map<Object, Object> activities;
        try {
            //noinspection unchecked
            activities = (Map<Object, Object>) activitiesField.get(activityThread);
        } catch (IllegalAccessException e) {
            Log.d(LOG_TAG, "Can not find current activity due to " + e);
            return null;
        }
        if (activities == null) {
            Log.d(LOG_TAG, "Can not find activities.");
            return null;
        }

        for (Object activityRecord : activities.values()) {
            Class activityRecordClass = activityRecord.getClass();
            try {
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    return (Activity) activityField.get(activityRecord);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Log.d(LOG_TAG, "Can not find current activity due to " + e);
                return null;
            }
        }

        return null;
    }

    /**
     * Start the process of the CSDK reconfiguration
     */
    private void logoutAndApplyChanges() {
        Log.d(LOG_TAG, "logoutAndApplyChanges mRefreshNeeded=" + mRefreshNeeded);
        SDKManager.getInstance().getDeskPhoneServiceAdaptor().applyConfigChanges(mRefreshNeeded);
    }

    /**
     * Use notification in order to postpone the CSDK reconfiguration
     */
    private void postponeLogout() {
        Log.d(LOG_TAG, "postponeLogout");

        Notification.Builder notification = new Notification.Builder(getActivity());
        notification.setSmallIcon(android.R.drawable.stat_sys_warning);
        notification.setContentTitle(getActivity().getResources().getString(R.string.logout_to_apply_changes_title));
        notification.setAutoCancel(true);

        Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
        String longLine = getActivity().getResources().getString(R.string.logout_to_apply_changes_text);
        int middleIdx = longLine.indexOf(' ', longLine.length() / 2);
        String firstLine = longLine.substring(0, middleIdx);
        String secondLine = longLine.substring(middleIdx + 1);
        inboxStyle.addLine(firstLine);
        inboxStyle.addLine(secondLine);
        notification.setStyle(inboxStyle);

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setAction(MainActivity.SERVICE_IMPACTING_CHANGE);
        notification.setContentIntent(PendingIntent.getActivity(getActivity(), 0, intent, 0));

        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(LOGOUT_ALERT_TAG, LOGOUT_ALERT_ID, notification.build());
    }
}
