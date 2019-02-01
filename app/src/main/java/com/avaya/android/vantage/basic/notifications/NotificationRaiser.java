package com.avaya.android.vantage.basic.notifications;

import android.app.Application;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import com.avaya.android.vantage.basic.notifications.NotificationService.NotificationServiceBinder;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Component that raises notifications to Android's standard status bar at the top of the screen.
 */
public class NotificationRaiser {

    private final String LOG_TAG = this.getClass().getSimpleName();
    public static final int IDLE_TAG=Integer.MAX_VALUE-1;
    public static final int MISSED_CALLS_NOTIFICATION_ID=Integer.MAX_VALUE*15;//ELAN-1000

    private Context mContext;

    private Queue<Notification> queuedItems = new LinkedList<>();
    private Queue<Pair<Integer, Integer>> queuedIDs = new LinkedList<>();

    /**
     * To avoid memory leak, use Application context.<br>
     * Throws ClassCastException if context is not instance of Application<br>
     * to force the use of Application context.
     *
     * @param context use Application context to avoid memory leak
     */
    public NotificationRaiser(Context context) {
        if (!(context instanceof Application)) {
            throw new ClassCastException(String.format(Locale.getDefault(),
                    "Only use application context here!\nFound:%s", context.getClass().getName()));
        }

        mContext = context;
    }

    // Starting the NotificationService and setting up the connection to it
    // happens asynchronously, so at startup there's a small period (can be up
    // to a couple of seconds), where this object exists but does not have its
    // connection to the service set yet. To handle this, at startup we enqueue
    // any received items, and push all of those notifications once the service
    // becomes available, at which time the queue is no longer needed.
    //
    // Similarly, there's a race on shutdown where the application may have
    // stopped the service before other code is finished trying to send
    // notifications. In that case, we just ignore the late notifications.
    private NotificationService notificationService;

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            serviceConnected(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // Since this is a local service, should never be called.
            Log.e(LOG_TAG, "Disconnected from NotificationService");
            notificationService = null;
        }
    };

    public void bindNotificationService() {
        final Intent intent = new Intent(mContext, NotificationService.class);
        final boolean rc = mContext.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        Log.d(LOG_TAG, "bindNotificationService() {}" + (rc ? "success" : "failure"));
        assert rc;
    }

    public void unbindNotificationService() {
        mContext.unbindService(serviceConnection);
    }

    protected synchronized void serviceConnected(IBinder ibinder) {
        Log.d(LOG_TAG, "Connected to NotificationService");
        if (ibinder instanceof NotificationServiceBinder) {
            final NotificationServiceBinder binder = (NotificationServiceBinder) ibinder;
            notificationService = binder.getNotificationService();
            notificationService.cancelAll();
        }

        while (queuedItems.peek() != null) {
            raiseNotification(IDLE_TAG, queuedItems.remove());
        }
        queuedItems = null;
        while (queuedIDs.peek() != null) {
            Pair<Integer, Integer> idPair = queuedIDs.remove();
            notificationService.copy(idPair.first, idPair.second);
        }
    }

    /**
     * Cancel a previously shown notification. If it's transient, the view will be hidden. If it's
     * persistent, it will be removed from the status bar.
     */
    public void cancelNotification(int id) {
        if (notificationService != null) {
            notificationService.cancel(id);
        }
    }

    /**
     * Cancel a previously shown notification. If it's transient, the view will be hidden. If it's
     * persistent, it will be removed from the status bar.
     */
    public void cancelNotification(String tag, int id) {
        if (notificationService != null) {
            notificationService.cancel(tag, id);
        }
    }

    /**
     * Cancel all previously shown notifications
     */
    public void cancelNotification() {
        if (notificationService != null) {
            notificationService.cancelAll();
        }
    }

    /**
     * If the {@link NotificationService} runs, Post the notification
     * if not, adds it to the queue.
     * @param id A unique notification identifier
     * @param notification Notification object
     */
    public void raiseNotification(int id, Notification notification) {

        if (notificationService != null) {
            notificationService.notify(id, notification);
        }
        else
        {
            queuedItems.add(notification);
        }
    }

    // copy content of notification under id into notification under otherId
    public void copy(int id, int otherId) {
        if (notificationService != null) {
            notificationService.copy(id, otherId);
        }
        else {
            queuedIDs.add(new Pair<Integer,Integer>(id,otherId));
        }
    }
}
