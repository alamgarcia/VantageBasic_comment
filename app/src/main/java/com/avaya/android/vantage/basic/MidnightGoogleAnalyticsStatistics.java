package com.avaya.android.vantage.basic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import static com.avaya.android.vantage.basic.Constants.MIDNIGHT_GOOGLE_ANALYTICS;

/**
 * This is a Broadcast Receiver class that is called everyday on
 * the Midnight to log the relevant events in the Google Analytics.
 *
 * @author lutsenko
 * @version 31/10/2017
 */

public class MidnightGoogleAnalyticsStatistics extends BroadcastReceiver {

    private static final String TAG = MidnightGoogleAnalyticsStatistics.class.getSimpleName();
    private static int mCallsCounter;

    static public void resetCallsCounterPreference(Context context) {
        Log.d(TAG, "Reset CallsCounter value: to 0");
        SharedPreferences.Editor editor = getCallsCounterPreference(context).edit();
        editor.putInt("callsCounter", 0);
        editor.apply();
    }

    static public SharedPreferences getCallsCounterPreference(Context context) {
        SharedPreferences callsCounterPreference = context.getSharedPreferences("callsCounter", Context.MODE_PRIVATE);
        return callsCounterPreference;
    }

    static private void saveCallsCounterPreference(Context context, int callsCounter) {
        Log.d(TAG, "saveCallsCounterPreference with value: " + callsCounter);
        SharedPreferences.Editor editor = getCallsCounterPreference(context).edit();
        editor.putInt("callsCounter", callsCounter);
        editor.apply();
    }

    static public int getValueOfCallsCounterPreference(Context context) {
        mCallsCounter = getCallsCounterPreference(context).getInt("callsCounter", 0);
        return mCallsCounter;
    }

    static public void increaseCallsCounterPreference(Context context) {
        mCallsCounter = getValueOfCallsCounterPreference(context);
        mCallsCounter++;
        saveCallsCounterPreference(context, mCallsCounter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction() == null) {
            Log.d(TAG, "intent.getAction == null");
            return;
        }
        if (!intent.getAction().equalsIgnoreCase(MIDNIGHT_GOOGLE_ANALYTICS)) {
            return;
        }

        mCallsCounter = getValueOfCallsCounterPreference(context);
        Log.d(TAG, "CallsCounter value: " + mCallsCounter);
        GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALLS_PER_DAY_EVENT, Integer.toString(mCallsCounter));

        if (mCallsCounter == 0) {
            GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALLS_PER_DAY_0_EVENT);
            Log.d(TAG, "CALLS_PER_DAY_0_EVENT");
        }
        if (mCallsCounter > 0 && mCallsCounter <= 10) {
            GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALLS_PER_DAY_UP_10_EVENT);
            Log.d(TAG, "CALLS_PER_DAY_UP_10_EVENT");
        }
        if (mCallsCounter > 10 && mCallsCounter <= 25) {
            GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALLS_PER_DAY_UP_25_EVENT);
            Log.d(TAG, "CALLS_PER_DAY_UP_25_EVENT");
        }
        if (mCallsCounter > 25 && mCallsCounter <= 50) {
            GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALLS_PER_DAY_UP_50_EVENT);
            Log.d(TAG, "CALLS_PER_DAY_UP_50_EVENT");
        }
        if (mCallsCounter > 50 && mCallsCounter <= 100) {
            GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALLS_PER_DAY_UP_100_EVENT);
            Log.d(TAG, "CALLS_PER_DAY_UP_100_EVENT");
        }
        if (mCallsCounter > 100) {
            GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALLS_PER_DAY_MORE_THAN_100_EVENT);
            Log.d(TAG, "CALLS_PER_DAY_MORE_THAN_100_EVENT");
        }

        //Reset the value of the callsCounter after google event sent at midnight
        resetCallsCounterPreference(context);
    }
}
