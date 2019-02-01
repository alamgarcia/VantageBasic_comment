package com.avaya.android.vantage.basic.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.model.UICall;
import com.avaya.android.vantage.basic.model.UICallState;
import com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.avaya.android.vantage.basic.model.UICallState.ESTABLISHED;
import static com.avaya.android.vantage.basic.model.UICallState.HELD;
import static com.avaya.android.vantage.basic.model.UICallState.REMOTE_ALERTING;

/**
 * {@link CallStatusFragment} responsible for showing current status of call
 */

public class CallStatusFragment extends Fragment {

    CallStateEventHandler mCallStateEventHandler = null;
    private TextView mInfoContactName = null;
    private TextView mInfoContactState;
    private static boolean mIsVisible = false;
    private int mCallId = -1;
    private Handler mHandler;
    private UpdateDurationRunnable mUpdateDurationTask;
    private Object lock = new Object();
    private long mCurrTimerMillis = new Date().getTime();

    public CallStatusClickListener getCallStatusClickListener() {
        return mCallStatusClickListener;
    }

    private CallStatusClickListener mCallStatusClickListener;

    public void init(CallStateEventHandler stateEventHandler) {
        mCallStateEventHandler = stateEventHandler;
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(Context)} (Activity)} and before
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * <p>
     * <p>Note that this can be called while the fragment's activity is
     * still in the process of being created.  As such, you can not rely
     * on things like the activity's content view hierarchy being initialized
     * at this point.  If you want to do work once the activity itself is
     * created, see {@link #onActivityCreated(Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mUpdateDurationTask = new UpdateDurationRunnable(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.call_status, container, false);
        mCallStatusClickListener = new CallStatusClickListener();
        view.setOnClickListener(mCallStatusClickListener);

        mInfoContactName = (TextView) view.findViewById(R.id.caller_name);
        mInfoContactState = (TextView) view.findViewById(R.id.state_and_timer);

        return view;
    }

    @Override
    public void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateDurationTask);
        }
        super.onDestroy();
    }

    /**
     * Set data in CallStatusFragment based on data from ActiveCallFragment
     * data
     *
     * @param callState TextView which contains all data required for setting
     *                  proper data in CallStatusFragment call state
     * @param CallName  TextView which contains all data required for setting
     * @param callId    call id
     */
    public void updateCallStatusInfo(TextView callState, TextView CallName, int callId) {
        mInfoContactState.setText(callState.getText());
        mInfoContactState.setTextColor(callState.getTextColors());
        mInfoContactName.setText(CallName.getText());
        mCallId = callId;
    }

    /**
     * Setting call id for current call
     *
     * @param callId int of current call
     */
    public void setCallId(int callId) {
        mCallId = callId;
    }

    /**
     * Update call status state data from provided TextView
     * @param callState TextView containing data required for setting
     *                  proper data in CallStatusFragment call state
     * @param callStateValue {@link UICallState} of call which is shown
     *                  in {@link CallStatusFragment}
     */
    public void updateCallStatusState(TextView callState, UICallState callStateValue) {
        if(callStateValue.equals(HELD)){
            mInfoContactState.setText(getText(R.string.on_hold));
            mInfoContactState.setTextColor(getResources().getColor(R.color.colorOnHold, null));
            mUpdateDurationTask.setCallState(callStateValue);
        } else {
            mInfoContactState.setText(callState.getText());
        }
    }

    /**
     * Update call name state data from provided TextView
     *
     * @param callName TextView containing data required for setting
     *                 proper data in CallStatusFragment call name
     */
    public void updateCallStatusName(String callName) {
        mInfoContactName.setText(callName);
    }

    /**
     * Hide call status fragment
     */
    public void hideCallStatus() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.hide(this);
        ft.commitAllowingStateLoss();
        mIsVisible = false;
    }

    /**
     * Showing call status fragment
     */
    public void showCallStatus() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.show(this);
        ft.commitAllowingStateLoss();
        mIsVisible = true;
    }

    /**
     * Check is call on hold.
     *
     * @return mIsVisible value
     */
    public static boolean isCallStatusVisible() {
        return mIsVisible;
    }

    /**
     * Setting call status visibility by setting {@link #mIsVisible} parameter
     *
     * @param isVisbile boolean
     */
    public static void setCallStatusVisiblity(Boolean isVisbile) {
        mIsVisible = isVisbile;
    }

    /**
     * Setting call state based on provided parameters.
     *
     * @param callState UICallState which can be ESTABLISHED or HELD
     */
    public void setCallStateChanged(UICallState callState) {
        if (mInfoContactState == null)
            return;
        if (mHandler != null && mUpdateDurationTask != null) {
            mUpdateDurationTask.setCallState(callState);
            mHandler.post(mUpdateDurationTask);
        }
    }

    /**
     * Returning call id of current call
     *
     * @return int representing call id
     */
    public int getCallId() {
        return mCallId;
    }

    /**
     * Processing call status name update
     *
     * @param remoteNumber   of call
     * @param newDisplayName of current call
     */
    public void updateCallStatusName(String remoteNumber, String newDisplayName) {
        if (mInfoContactName != null) {
            String contactName = Utils.getContactName(remoteNumber, newDisplayName, SDKManager.getInstance().getCallAdaptor().isCMConferenceCall(mCallId));
            mInfoContactName.setText(contactName);
        }
    }


    /**
     * Update call length timer
     */
    private void updateTimer(UICallState callState) {
        // remove leftovers from Q
        mHandler.removeCallbacks(mUpdateDurationTask);
        // Calculate particular call duration
        if (SDKManager.getInstance().getCallAdaptor().getCall(mCallId) != null) {
            synchronized (lock) {
                mCurrTimerMillis = new Date().getTime() - SDKManager.getInstance().getCallAdaptor().getCall(mCallId).getStateStartTime();
            }
        }

        long minutes;
        long seconds;
        long hours;
        String connectMessage = "";

        // covert time
        hours = TimeUnit.MILLISECONDS.toHours(mCurrTimerMillis); // hours
        minutes = TimeUnit.MILLISECONDS.toMinutes(mCurrTimerMillis) % Constants.MINUTES; // minutes
        seconds = TimeUnit.MILLISECONDS.toSeconds(mCurrTimerMillis) % Constants.SECONDS; // seconds

        // format time
        if (hours != 0) {
            connectMessage = Long.toString(hours) + ":";
        }
        // no leading zero is needed
        if (connectMessage.isEmpty() || minutes > 9) {
            connectMessage += Long.toString(minutes);
        } else {
            connectMessage += "0" + Long.toString(minutes);
        }
        connectMessage += ":";
        if (seconds > 9) {
            connectMessage += Long.toString(seconds);
        } else {
            connectMessage += "0" + Long.toString(seconds);
        }
        if (callState.equals(ESTABLISHED)) {
            // set the text and advance the timer
            mInfoContactState.setTextColor(getResources().getColor(R.color.colorCallStateText, null));
            mInfoContactState.setText(connectMessage);
            // post next update
            mHandler.postDelayed(mUpdateDurationTask, Constants.MILISECONDS);
        } else if (callState.equals(HELD)) {
            mInfoContactState.setText(getText(R.string.on_hold));
            mInfoContactState.setTextColor(getResources().getColor(R.color.colorOnHold, null));
        } else if (callState.equals(REMOTE_ALERTING)) {
            mInfoContactState.setText(getText(R.string.calling));
            mInfoContactState.setTextColor(getResources().getColor(R.color.colorCallStateText, null));
        }
        synchronized (lock) {
            mCurrTimerMillis += Constants.MILISECONDS; // advance the time in a second.
        }

    }

    /**
     * Removes {@link UpdateDurationRunnable} from the Handler
     */
    public void stopTimerUpdate() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateDurationTask);
        }
    }

    /**
     * Responsible for call timer update
     */
    private static class UpdateDurationRunnable implements Runnable {
        private WeakReference<CallStatusFragment> mCallStatusFragmentWeakReference;
        private UICallState mCallState = UICallState.ESTABLISHED;

        UpdateDurationRunnable(CallStatusFragment layout) {
            mCallStatusFragmentWeakReference = new WeakReference<>(layout);
        }

        public void setCallState(UICallState callState) {
            mCallState = callState;
        }

        public void run() {
            if (mCallStatusFragmentWeakReference != null && mCallStatusFragmentWeakReference.get() != null) {
                mCallStatusFragmentWeakReference.get().updateTimer(mCallState);
            }
        }
    }


    /**
     * {@link CallStatusClickListener} for processing clicks on {@link CallStatusFragment}
     */
    public class CallStatusClickListener implements View.OnClickListener {

        private static final int DEBOUNCE_DELAY = 500;
        private final String TAG = CallStatusClickListener.class.getSimpleName();

        @Override
        public void onClick(View v) {
            if (mCallStateEventHandler != null) {
                mCallStateEventHandler.onCallStateClicked();
            }
            View statusFragmentView = CallStatusFragment.this.getView();
            if (statusFragmentView != null) {
                statusFragmentView.setClickable(false);
            }
            Log.d(TAG, "not clickable");
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    CallStatusFragment.this.getView().setClickable(true);
                    Log.d(TAG, "clickable again");
                }
            }, DEBOUNCE_DELAY);
        }
    }
}
