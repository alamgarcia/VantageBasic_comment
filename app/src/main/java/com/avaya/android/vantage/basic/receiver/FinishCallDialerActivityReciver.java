package com.avaya.android.vantage.basic.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.CallDialerActivity;
import com.avaya.android.vantage.basic.views.interfaces.FinishCallDialerActivityInterface;

/**
 * Created by dshar on 01/07/2018.
 */

public class FinishCallDialerActivityReciver extends BroadcastReceiver {

    private FinishCallDialerActivityInterface mOnEventListener;


    @Override
    public void onReceive(Context context, Intent intent) {

        if(Utils.callDialerActivity!=null && intent.getAction().equalsIgnoreCase("com.avaya.endpoint.FINISH_CALL_ACTIVITY")) {
            mOnEventListener = Utils.callDialerActivity;
            Log.e("TEST", "FinishCallDialerActivityReciver - onReceive");
            mOnEventListener.killCallDialerActivity();
            Utils.callDialerActivity = null;
        }
    }
}
