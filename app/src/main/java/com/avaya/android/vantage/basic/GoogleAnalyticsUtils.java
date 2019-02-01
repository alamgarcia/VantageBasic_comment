package com.avaya.android.vantage.basic;

/**
 * Created by lutsenko on 16/10/2017.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.avaya.android.vantage.basic.csdk.SDKManager;

import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ANALYTICSENABLED;
import static com.avaya.clientservices.collaboration.DrawingView.TAG;

import com.avaya.android.vantage.basic.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.basic.model.UIAudioDevice;
import com.avaya.android.vantage.basic.model.UICall;
import com.avaya.clientservices.call.Call;

import java.util.Calendar;
import java.util.Random;


public class GoogleAnalyticsUtils {
    public enum Event {
        AUDIO_CALL_EVENT("EstablishedAudioCall"),
        VIDEO_CALL_EVENT("EstablishedVideoCall"),
        CALL_LENGTH_UP_1MIN_EVENT("CallLenghtUp1Min"),
        CALL_LENGTH_UP_5MIN_EVENT("CallLenghtUp5Min"),
        CALL_LENGTH_UP_15MIN_EVENT("CallLenghtUp15Min"),
        CALL_LENGTH_UP_30MIN_EVENT("CallLenghtUp30Min"),
        CALL_LENGTH_UP_1HOUR_EVENT("CallLenghtUp1Hour"),
        CALL_LENGTH_MORE_1HOUR_EVENT("CallLenghtMoreThan1Hour"),
        HEADSET_EVENT("HeadsetTransducer"),
        HANDSET_EVENT("HandsetTransducer"),
        SPEAKER_EVENT("SpeakerTransducer"),
        FEATURE_TRANSFER_EVENT("FeatureTransfer"),
        FEATURE_CONFERENCE_EVENT("FeatureConference"),
        CALL_FROM_FAVORITES_EVENT("CallFromFavorites"),
        CALL_FROM_CONTACTS_EVENT("CallFromContacts"),
        CALL_FROM_HISTORY_EVENT("CallFromHistory"),
        CALL_FROM_DIALER_EVENT("CallFromDialer"),
        SEARCH_CONTACTS_EVENT("SearchContacts"),

        //Midnight statistics
        CALLS_PER_DAY_EVENT("CallsPerDay"),
        CALLS_PER_DAY_0_EVENT("CallsPerDay0"),
        CALLS_PER_DAY_UP_10_EVENT("CallsPerDayUp10"),
        CALLS_PER_DAY_UP_25_EVENT("CallsPerDayUp25"),
        CALLS_PER_DAY_UP_50_EVENT("CallsPerDayUp50"),
        CALLS_PER_DAY_UP_100_EVENT("CallsPerDayUp100"),
        CALLS_PER_DAY_MORE_THAN_100_EVENT("CallsPerDayMoreThan100"),

        K155_EULA_ACCEPT_EVENT("K155EulaAccept"),
        K175_EULA_ACCEPT_EVENT("K175EulaAcceptEvent");

        private String mEventName;

        Event(String eventName) {
            mEventName = eventName;
        }

        public String getEventName() {
            return mEventName;
        }
    }


    //for Google Analytics midnight statistics
    private static SharedPreferences mCallsCounterPreference;
    static private PendingIntent mPendingIntent;

    /**
     * Sets Firebase Analytics Enabled and initializes CallsCounter Shared Preferences
     * @param context Application context
     */
    static private void init(Context context) {
        // TO DO Init logging system
        mCallsCounterPreference = MidnightGoogleAnalyticsStatistics.getCallsCounterPreference(context);
    }

    /**
     * Sets Firebase Analytics Disabled
     * @param context Application context
     */
    static private void stop(Context context) {
        // TO DO Stop logging system
    }

    /**
     * Logs an event to the Firebase Analytics
     * @param event {@link Event} to be logged
     * @param arguments Argumnets related to the Event type
     */
    static public void logEvent(Event event, String... arguments) {
        // TO DO implement your own logging
    }

    /**
     * Init or stop Firebase Analytics based on the configuration param ANALYTICSENABLED
     * @param context Application Context
     */
    static public void setOperationalMode(Context context) {
        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ANALYTICSENABLED)) {
            init(context);
            Log.d(TAG, "ANALYTICSENABLED settings parameter is 1 (enabled)");
        } else {
            stop(context);
            Log.d(TAG, "ANALYTICSENABLED settings parameter is 0 (disabled)");
        }
    }

    /**
     * Logs events when a Call is established.
     * @param call instance of the {@link UICall} object that referes to the call that is established.
     */
    static public void googleAnalyticsOnCallEstablished(UICall call) {
        if (call.isVideo()) {
            logEvent(Event.VIDEO_CALL_EVENT);
        } else {
            logEvent(Event.AUDIO_CALL_EVENT);
        }

        UIAudioDevice device = SDKManager.getInstance().getAudioDeviceAdaptor().getActiveAudioDevice();
        if (device == UIAudioDevice.SPEAKER) {
            logEvent(Event.SPEAKER_EVENT);
        }
        if (device == UIAudioDevice.BLUETOOTH_HEADSET || device == UIAudioDevice.WIRED_HEADSET || device == UIAudioDevice.RJ9_HEADSET) {
            logEvent(Event.HEADSET_EVENT);
        }
        if (device == UIAudioDevice.HANDSET || device == UIAudioDevice.WIRELESS_HANDSET) {
            logEvent(Event.HANDSET_EVENT);
        }
    }

    /**
     * Logs events when the call has ended.
     * @param call {@link Call} object that refers to the Call which the events are being logged for.
     */
    static public void googleAnaliticsOnCallEnd(Call call) {
        if (call.isAnswered()) {
            int callDuration = (int) (((System.currentTimeMillis() - call.getEstablishedTimeMillis())) / (60 * 1000));

            if (callDuration <= 1) {
                logEvent(Event.CALL_LENGTH_UP_1MIN_EVENT);
            }
            if (callDuration > 1 && callDuration <= 5) {
                logEvent(Event.CALL_LENGTH_UP_5MIN_EVENT);
            }
            if (callDuration > 5 && callDuration <= 15) {
                logEvent(Event.CALL_LENGTH_UP_15MIN_EVENT);
            }
            if (callDuration > 15 && callDuration <= 30) {
                logEvent(Event.CALL_LENGTH_UP_30MIN_EVENT);
            }
            if (callDuration > 30 && callDuration <= 60) {
                logEvent(Event.CALL_LENGTH_UP_1HOUR_EVENT);
            }
            if (callDuration > 60) {
                logEvent(Event.CALL_LENGTH_MORE_1HOUR_EVENT);
            }
        }
    }


    /**
     * Call from Faforites, Contacts or History  > contact details
     */
    public static void googleAnalyticsCallFromContactsDetailes(OnContactInteractionListener listener) {
        String selectedTab = listener.getContactCapableTab();
        if (selectedTab == null) {
            Log.e(TAG, "Unknown selected TAB");
            return;
        }
        switch (selectedTab) {
            case "Favorites":
                logEvent(Event.CALL_FROM_FAVORITES_EVENT);
                break;
            case "Contacts":
                logEvent(Event.CALL_FROM_CONTACTS_EVENT);
                break;
            case "History":
                logEvent(Event.CALL_FROM_HISTORY_EVENT);
                break;
        }

    }

    /**
     * Creates WakeUp alarm set to Midnight that runs {@link MidnightGoogleAnalyticsStatistics}
     * @param context Application context
     */
    static public void googleAnalyticsMidnightStatistics(Context context) {
        //Create calendar to the specific time = midnight
        Calendar midnightCalendar = getMidnightCalendar();
        //Create AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Retrieve a PendingIntent that will perform a broadcast
        //then create a pending intent MIDNIGHT_GOOGLE_ANALYTICS to be called at midnight
        Intent alarmIntent = new Intent(context, MidnightGoogleAnalyticsStatistics.class);
        alarmIntent.setAction(Constants.MIDNIGHT_GOOGLE_ANALYTICS);
        mPendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, midnightCalendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, mPendingIntent);


          //For debugging only - works with 3 minutes interval
//        Calendar midnightCalendar = Calendar.getInstance();
//        midnightCalendar.set(Calendar.SECOND, 60);
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        Intent alarmIntent = new Intent(context, MidnightGoogleAnalyticsStatistics.class);
//        alarmIntent.setAction(Constants.MIDNIGHT_GOOGLE_ANALYTICS);
//        mPendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, midnightCalendar.getTimeInMillis(), 3 * 60 * 1000, mPendingIntent);

    }

    @NonNull
    private static Calendar getMidnightCalendar() {
        Random generator = new Random();
        int randomMin = generator.nextInt(6); //random min 0-5
        int randomSec = generator.nextInt(60); //random sec 0-59

        //Create new calendar instance
        Calendar midnightCalendar = Calendar.getInstance();

        //Set the time to midnight + around 5 min random time
        midnightCalendar.set(Calendar.HOUR_OF_DAY, 0);
        midnightCalendar.set(Calendar.MINUTE, 0 + randomMin);
        midnightCalendar.set(Calendar.SECOND, 0 + randomSec);
        return midnightCalendar;
    }

}
