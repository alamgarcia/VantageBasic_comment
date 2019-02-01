package com.avaya.android.vantage.basic.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import static android.app.Notification.FLAG_ONGOING_EVENT;

/**
 * Persistent foreground service for raising notifications to the status bar.
 */
public class NotificationService extends Service {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private NotificationManager notificationManager;

    /**
     * Have we already set ourselves in the foreground?
     */
    private boolean foregroundWasSet;

    @Override
    public NotificationServiceBinder onBind(Intent intent) {
       Log.d(LOG_TAG, "Binding to NotificationService");
        //noinspection ReturnOfInnerClass
        return new NotificationServiceBinder();
    }

    public class NotificationServiceBinder extends Binder {

        public NotificationService getNotificationService() {
            return NotificationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        foregroundWasSet = false;
        return START_STICKY;
    }

    /**
     * Cancels previously shown notification
     * @param id an unique notification identifier
     */
    public void cancel(int id) {
        notificationManager.cancel(id);
    }

    /**
     * Cancels previously shown notification
     * @param id an unique notification identifier
     */
    public void cancel(String tag, int id) {
        notificationManager.cancel(tag, id);
    }

    public void cancelAll() {
        notificationManager.cancelAll();
    }

    /**
     * Posts notification to be shown. Makes sure that the service runs in the
     * foreground for ongoing notifications.
     *
     * @param id an unique notification identifier
     * @param notification A Notification object
     */
    public synchronized void notify(int id, Notification notification) {
        // notifications that are not "ongoing" cannot be raised with
        // the startForeground API.
        final boolean ongoingEvent = (notification.flags & FLAG_ONGOING_EVENT) != 0;

        if (foregroundWasSet || !ongoingEvent) {
            notificationManager.notify(id, notification);
        } else {
            foregroundWasSet = true;
            startForeground(id, notification);
        }
    }

    // copy content of notification under id into notification under otherId
    public synchronized void copy(int id, int otherId) {

        Log.d(LOG_TAG, "Move from " + id + " to " + otherId);

        if (notificationManager == null)
            return;

        // get list of all notifications
        StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
        if (activeNotifications == null)
            return;

        // find the notifications associated with the specified ids
        int idIdx=-1;
        int otherIdIdx=-1;
        for (int i=0; i<activeNotifications.length && (idIdx==-1 || otherIdIdx==-1); i++) {
            if (activeNotifications[i].getId() == id)
                idIdx = i;
            else if (activeNotifications[i].getId() == otherId)
                otherIdIdx = i;
        }
        // make sure both notifications exists
        if (idIdx==-1 || otherIdIdx==-1) {
            Log.e(LOG_TAG, "One of the notifications does not exist: idIdx=" + idIdx + " otherIdIdx=" + otherIdIdx);
            return;
        }

        // copy the content of the notification to appear under other id
        notify(otherId, activeNotifications[idIdx].getNotification());
    }
}
