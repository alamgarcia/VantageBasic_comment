package com.avaya.android.vantage.basic;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Build;

import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.deskphoneservices.DeskPhoneEventsBroadcastReceiver;

import java.lang.ref.WeakReference;

/**
 * Main Vantage Basic Application class
 */
public class ElanApplication extends Application {

    private static boolean sIsMainActivityVisble = false;
    private static WeakReference<ElanApplication> mInstanceRef;
    private static boolean sShouldApplyConfigChange;
    private PendingIntent mDeskphoneEventsPendingIntent;
    private AudioManager mAudioManager;
    private boolean mIsMediaButtonReceiverRegistered = false;

    /**
     * Return boolean which tell us is main activity visible
     *
     * @return boolean
     */
    public static boolean isMainActivityVisible() {
        return sIsMainActivityVisble;
    }

    /**
     * Setting boolean which tell us is main activity visible
     *
     * @param visible boolean
     */
    public static void setIsMainActivityVisible(boolean visible) {
        sIsMainActivityVisble = visible;
    }

    /**
     * Setting boolean which tell us if configuration is changed
     *
     * @param applyConfigChange
     */
    public static void setApplyConfigChange(boolean applyConfigChange) {
        ElanApplication.sShouldApplyConfigChange = applyConfigChange;
    }

    /**
     * Returning boolean which tell us is configuration changed
     *
     * @return boolean
     */
    public static boolean isConfigChange() {
        return sShouldApplyConfigChange;
    }

    public static boolean isPinAppLock = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstanceRef = new WeakReference<>(this);
        // clear old notifications
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
        initCSDK();
        GoogleAnalyticsUtils.setOperationalMode(this);
        GoogleAnalyticsUtils.googleAnalyticsMidnightStatistics(this);
    }

    /**
     * Sets Pending Intent to the {@link DeskPhoneEventsBroadcastReceiver}
     * to allow restarting playback after the session has been stopped.
     */
    public void registerMediaButtonReceiver() {
        if(mDeskphoneEventsPendingIntent == null) {
            mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            Intent intent = new Intent(this, DeskPhoneEventsBroadcastReceiver.class);

            mDeskphoneEventsPendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        if (!mIsMediaButtonReceiverRegistered) {
            mAudioManager.registerMediaButtonEventReceiver(mDeskphoneEventsPendingIntent);
            mIsMediaButtonReceiverRegistered = true;
        }
    }

    /**
     * Unregister {@link DeskPhoneEventsBroadcastReceiver} for receiving
     * MEDIA_BUTTON intents.
     */
    public void unRegisterMediaButtonReceiver() {
        if (mAudioManager != null && mDeskphoneEventsPendingIntent != null && mIsMediaButtonReceiverRegistered) {
            mAudioManager.unregisterMediaButtonEventReceiver(mDeskphoneEventsPendingIntent);
            mIsMediaButtonReceiverRegistered = false;
        }
    }

    /**
     * Obtaining base context from application
     *
     * @return Context
     */
    public static Context getContext() {
        if (mInstanceRef.get() == null) {
            return null;
        }
        return mInstanceRef.get().getBaseContext();
    }

    /**
     * Start initialisation of CSDK in {@link SDKManager}
     */
    private void initCSDK() {
        SDKManager.getInstance().initializeSDK(this);
    }


}
