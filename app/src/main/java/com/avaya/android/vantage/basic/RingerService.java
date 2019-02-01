package com.avaya.android.vantage.basic;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;

public class RingerService extends Service {
    private Ringtone mRingtone;
    private static final int POS_UNKNOWN = -1;

    public RingerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Called by the system every time a client explicitly starts the service by calling
     * {@link Context#startService}, providing the arguments it supplied and a
     * unique integer token representing the start request.  Do not call this method directly.
     * <p>
     * <p>For backwards compatibility, the default implementation calls
     * {@link #onStart} and returns either {@link #START_STICKY}
     * or {@link #START_STICKY_COMPATIBILITY}.
     * <p>
     * <p>If you need your application to run on platform versions prior to API
     * level 5, you can use the following model to handle the older {@link #onStart}
     * callback in that case.  The <code>handleCommand</code> method is implemented by
     * you as appropriate:
     * <p>
     * {@sample development/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.java
     * start_compatibility}
     * <p>
     * <p class="caution">Note that the system calls this on your
     * service's main thread.  A service's main thread is the same
     * thread where UI operations take place for Activities running in the
     * same process.  You should always avoid stalling the main
     * thread's event loop.  When doing long-running operations,
     * network calls, or heavy disk I/O, you should kick off a new
     * thread, or use {@link AsyncTask}.</p>
     *
     * @param intent  The Intent supplied to {@link Context#startService},
     *                as given.  This may be null if the service is being restarted after
     *                its process has gone away, and it had previously returned anything
     *                except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags   Additional data about this start request.  Currently either
     *                0, {@link #START_FLAG_REDELIVERY}, or {@link #START_FLAG_RETRY}.
     * @param startId A unique integer representing this specific request to
     *                start.  Use with {@link #stopSelfResult(int)}.
     * @return The return value indicates what semantics the system should
     * use for the service's current started state.  It may be one of the
     * constants associated with the {@link #START_CONTINUATION_MASK} bits.
     * @see #stopSelfResult(int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sharedPref = getSharedPreferences(Constants.ADMIN_RINGTONE_PREFERENCES, Context.MODE_PRIVATE);
        String adminChoiceRingtone =  sharedPref.getString(Constants.ADMIN_RINGTONE_PREFERENCES, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
        String defaultRingtone;

        // if user has already set his ringtone settings, we just use default system ringtone for
        // default value. Otherwise, we use admin setting as default value
        if (!sharedPref.contains(Constants.CUSTOM_RINGTONE_PREFERENCES)){
            defaultRingtone = adminChoiceRingtone;
        } else {
            defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString();
        }


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String strRingtonePreference = prefs.getString(Constants.CUSTOM_RINGTONE_PREFERENCES, defaultRingtone);
        Uri currentRingtoneUri = Uri.parse(strRingtonePreference);
        if(mRingtone != null) {
            mRingtone.stop();
        }

        // checking if the ringtone actually exists. If not, using the Default Ringtone Uri.
        RingtoneManager rm = new RingtoneManager(getApplicationContext());
        int ringtonePosition = rm.getRingtonePosition(currentRingtoneUri);
        if (ringtonePosition == POS_UNKNOWN){
            if (!(currentRingtoneUri.toString().equals(""))){
                currentRingtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
            }
        }

        mRingtone = RingtoneManager.getRingtone(this, currentRingtoneUri);
        if(mRingtone != null) {
            mRingtone.play();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        if (mRingtone != null) {
            mRingtone.stop();
        }
    }
}
