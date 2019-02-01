package com.avaya.android.vantage.basic.views.adapters;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.SparseArray;
import android.view.WindowManager;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.adaptors.UICallViewAdaptor;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.ActiveCallFragment;
import com.avaya.android.vantage.basic.fragments.CallStatusFragment;
import com.avaya.android.vantage.basic.fragments.DialerFragment;
import com.avaya.android.vantage.basic.fragments.IncomingCallFragment;
import com.avaya.android.vantage.basic.fragments.VideoCallFragment;
import com.avaya.android.vantage.basic.model.UICall;
import com.avaya.android.vantage.basic.model.UICallState;
import com.avaya.android.vantage.basic.notifications.CallNotificationFactory;
import com.avaya.android.vantage.basic.views.interfaces.ICallViewInterface;
import com.avaya.clientservices.call.Call;

import java.util.List;

/**
 * Class used to handle call states
 */

public class CallStateEventHandler implements ICallViewInterface {

    final static private String TAG = "CallStateEventHandler";
    public static final String ACTIVE_CALL_TAG = "active_call";
    public static final String CALL_STATUS_TAG = "call_status";
    private FragmentManager mFm = null;
    private UICallViewAdaptor mCallViewAdaptor = null;
    private int mCurrentCallId = -1;
    public SparseArray mCalls = new SparseArray();
    private CallNotificationFactory mCallNotificationFactory;
    private Context mContext;
    private boolean mIsIncomingReceiverRegistered = false;

    /**
     * Constructor
     *
     * @param fm              fragment manager we use to handle fragments
     * @param callViewAdaptor current call view adaptor
     * @param context         active context
     */
    public CallStateEventHandler(FragmentManager fm, UICallViewAdaptor callViewAdaptor, Context context) {
        mFm = fm;
        mCallViewAdaptor = callViewAdaptor;
        mCallViewAdaptor.setViewInterface(this);
        mContext = context;

        mCallNotificationFactory = new CallNotificationFactory(context);


        CallStatusFragment callStatusFragment = (CallStatusFragment) mFm.findFragmentById(R.id.call_status);
        if (callStatusFragment != null) {
            callStatusFragment.init(this);
        } else {
            callStatusFragment = new CallStatusFragment();
            callStatusFragment.init(this);
        }

        FragmentTransaction ft = mFm.beginTransaction();
        ft.replace(R.id.call_status, callStatusFragment, CALL_STATUS_TAG);
        ft.hide(callStatusFragment);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onCallStarted(UICall call) {
        //outgoing call
        createCallFragment(call, call.isVideo());
        if (mCallNotificationFactory != null) {
            mCallNotificationFactory.show(call);
        }
        DialerFragment dialer = null;
        if (mFm.getFragments() != null) {
            for (Fragment f : mFm.getFragments()) {
                if (f instanceof DialerFragment) {
                    dialer = (DialerFragment) f;
                    break;
                }
            }
        }
        if (dialer != null) {
            dialer.clear();
            dialer.setMode(DialerFragment.DialMode.EDIT);
        }


    }

    /**
     * Create proper call fragment based on isVideo parameter.
     * We will create video call fragment or just audio call fragment
     *
     * @param call    Call object for which we are creating call
     * @param isVideo boolean based on which we are creating video or
     *                audio call fragment
     */
    private ActiveCallFragment createCallFragment(UICall call, boolean isVideo) {

        int oldCurrentCallId = mCurrentCallId;
        mCurrentCallId = call.getCallId();
        //Create proper fragment based on isVideo
        ActiveCallFragment fragment = isVideo ? new VideoCallFragment() : new ActiveCallFragment();

        //noinspection unchecked
        mCalls.put(mCurrentCallId, fragment);
        fragment.init(call, mCallViewAdaptor);
        FragmentTransaction ft = mFm.beginTransaction();
        ft.replace(R.id.active_call, fragment, ACTIVE_CALL_TAG);
        ft.commitAllowingStateLoss();

        if (mCalls.size() > Constants.CALL_SIZE_1) {
            ActiveCallFragment activeCallFragment = (ActiveCallFragment) mCalls.get(oldCurrentCallId);
            if (activeCallFragment != null) {
                showAndUpdateCallStatusFragment(activeCallFragment.getContactName(), activeCallFragment.getCallState(), activeCallFragment.getCallId());
            }
        }

        return fragment;
    }

    @Override
    public void onCallEscalatedToVideoSuccessful(UICall uiCall) {
        replaceAndRemoveFragment(new VideoCallFragment(), uiCall);
    }

    @Override
    public void onCallServiceUnavailable(UICall uiCall) {
        ActiveCallFragment activeCallFragment = (ActiveCallFragment) mCalls.get(uiCall.getCallId());
        if (activeCallFragment != null) {
            activeCallFragment.onCallServiceUnavailable();
        }
    }

    @Override
    public void onCallEscalatedToVideoFailed(UICall uiCall) {
        // Escalating from audio to video call failed
        if (mContext != null) {
            Utils.sendSnackBarData(mContext, mContext.getString(R.string.escalation_failed), Utils.SNACKBAR_LONG);
        }
    }

    @Override
    public void onCallDeescalatedToAudio(UICall uiCall) {
        replaceAndRemoveFragment(new ActiveCallFragment(), uiCall);
    }

    @Override
    public void onCallDeescalatedToAudioFailed(UICall uiCall) {
        // Deescalating from video to audio call failed
        if (mContext != null) {
            Utils.sendSnackBarData(mContext, mContext.getString(R.string.deescalation_failed), Utils.SNACKBAR_LONG);
        }
    }

    /**
     * Replacing current call fragment with provided call fragment
     * which can be ActiveCallFragment or VideoCallFragment type
     *
     * @param fragment ActiveCallFragment or VideoCallFragment which have to
     *                 be set in place of currently shown fragment
     * @param uiCall   call object for which we are performing change
     */
    private void replaceAndRemoveFragment(ActiveCallFragment fragment, UICall uiCall) {
        ActiveCallFragment activeCallFragment = (ActiveCallFragment) mCalls.get(uiCall.getCallId());
        fragment.initForReplaced(uiCall, mCallViewAdaptor);

        //noinspection unchecked
        mCalls.put(mCurrentCallId, fragment);

        FragmentTransaction ft = mFm.beginTransaction();
        ft.replace(R.id.active_call, fragment, ACTIVE_CALL_TAG);
        ft.remove(activeCallFragment);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onCallEstablished(UICall call) {
        //if (mCurrentCallId != call.getCallStatusCallId()) {
        ActiveCallFragment activeCallFragment = (ActiveCallFragment) mCalls.get(call.getCallId());
        if (activeCallFragment == null) {
            //incoming call
            //mOnCallEndedListener.callStarted();
            activeCallFragment = createCallFragment(call, call.isVideo());
        }
        if (mCallNotificationFactory != null) {
            mCallNotificationFactory.show(call);
        } else {
            mCallNotificationFactory = new CallNotificationFactory(mContext);
            mCallNotificationFactory.removeAll();
            mCallNotificationFactory.show(call);
        }
        activeCallFragment.onCallEstablished(call);

        CallStatusFragment callStatusFragment = (CallStatusFragment) mFm.findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
        if (callStatusFragment != null && (callStatusFragment.getCallId()==call.getCallId())) {
            callStatusFragment.setCallStateChanged(call.getState());
        }
    }

    @Override
    public void setCallStateChanged(UICall call) {
        ActiveCallFragment activeCallFragment = (ActiveCallFragment) mCalls.get(call.getCallId());
        if (activeCallFragment != null) {
            if (mCurrentCallId == call.getCallId()) {
                activeCallFragment.setCallStateChanged(call.getState());
            } else {//state changed for the non-active call. so need to update CallStatus
                CallStatusFragment callStatusFragment = (CallStatusFragment) mFm.findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                if (callStatusFragment != null) {
                    callStatusFragment.setCallStateChanged(call.getState());
                }
            }
        }
        if (mCallNotificationFactory != null) {
            mCallNotificationFactory.show(call);
        } else {
            mCallNotificationFactory = new CallNotificationFactory(mContext);
            mCallNotificationFactory.removeAll();
            mCallNotificationFactory.show(call);
        }
    }

    @Override
    public void onCallRemoteAlert() {
        ActiveCallFragment activeCallFragment = (ActiveCallFragment) mCalls.get(mCurrentCallId);
        if (activeCallFragment != null) {
            activeCallFragment.onCallRemoteAlert();
        }
    }

    @Override
    public void onCallRemoteAddressChanged(UICall uiCall, String newDisplayName) {
        ActiveCallFragment activeCallFragment = (ActiveCallFragment) mCalls.get(uiCall.getCallId());
        if (activeCallFragment != null) {
            activeCallFragment.onCallRemoteAddressChanged(uiCall.getRemoteNumber(), newDisplayName, uiCall);
        }

        CallStatusFragment callStatusFragment = (CallStatusFragment) mFm.findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
        if (callStatusFragment != null && uiCall.getCallId() == callStatusFragment.getCallId()){
            callStatusFragment.updateCallStatusName(uiCall.getRemoteNumber(), newDisplayName);
        }

        IncomingCallFragment fragment = (IncomingCallFragment) mFm.findFragmentByTag(INCOMING_CALL_TAG);
        if (fragment != null) {
            fragment.setNewRemoteName(uiCall, newDisplayName);
        }

        if (mCallNotificationFactory != null) {
            mCallNotificationFactory.show(uiCall);
        }  else {
            mCallNotificationFactory = new CallNotificationFactory(mContext);
            mCallNotificationFactory.removeAll();
            mCallNotificationFactory.show(uiCall);
        }
    }

    @Override
    public void onCallRemoved(UICall call) {
        FragmentTransaction ft = mFm.beginTransaction();
        if (call != null) {
            boolean switched = false;
            ActiveCallFragment activeCallFragment = (ActiveCallFragment) mCalls.get(call.getCallId());
            if (activeCallFragment == null){
                if (call.getState() == UICallState.IDLE)
                    ((MainActivity)mContext).setOffhookButtosChecked(false);
            }
            else {
                activeCallFragment.onCallRemoved();
            }
            mCalls.remove(call.getCallId());
            CallStatusFragment callStatusFragment = (CallStatusFragment) mFm.findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
            if (callStatusFragment != null) {
                callStatusFragment.stopTimerUpdate();
            }

            if (activeCallFragment != null && activeCallFragment.getActivity() != null && !activeCallFragment.getActivity().isDestroyed()) {
                ft.hide(activeCallFragment);
                ft.remove(activeCallFragment);
                ft.commitAllowingStateLoss();
                if (mCalls.size() == 1) {
                    switched = true;
                    setFragmentAsActiveCallFragment(mCalls.keyAt(0));
                }
            } else if (mCalls.size() == 1) {
                int callId = mCalls.keyAt(0);
                ActiveCallFragment curentActiveCallFragment = (ActiveCallFragment) mCalls.get(callId);
                if(curentActiveCallFragment!=null)
                    curentActiveCallFragment.setBackArrowColor();
            }
            callStatusFragment = (CallStatusFragment) mFm.findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
            if (callStatusFragment != null) {
                if ( switched || (activeCallFragment != null
                        && callStatusFragment.getCallId() == activeCallFragment.getCallId())
                        || SDKManager.getInstance().getCallAdaptor().getCall(callStatusFragment.getCallId()) == null  ) {
                    hideCallStatusFragment();
                }
                if (mCallNotificationFactory != null) {
                    mCallNotificationFactory.remove(call.getCallId());
                }

                removeIncomingCall(call);
            }

            if (mCallViewAdaptor != null && mCallViewAdaptor.getNumOfCalls() == 0) {
                ((Activity)mContext).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
        }
    }


    @Override
    public void onCallFailed(UICall uiCall) {
        ActiveCallFragment activeCallFragment = (ActiveCallFragment) mCalls.get(uiCall.getCallId());
        if (activeCallFragment != null) {
            CallStatusFragment callStatusFragment = (CallStatusFragment) mFm.findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
            if(callStatusFragment != null && callStatusFragment.getCallId() == uiCall.getCallId()) {
                //1.replace fragments
                ActiveCallFragment currentActiveCallFragment = (ActiveCallFragment) mCalls.get(getCurretCallId());
                setFragmentAsActiveCallFragment(callStatusFragment.getCallId());
                //2.update calls status
                if(getNumActiveCallFragments()>1 && currentActiveCallFragment != null) {
                    showAndUpdateCallStatusFragment(currentActiveCallFragment.getContactName(), currentActiveCallFragment.getCallState(), currentActiveCallFragment.getCallId());
                }
                else {
                    hideCallStatusFragment();
                    setFragmentAsActiveCallFragment(activeCallFragment.getCallId());
                    activeCallFragment.setVisible();
                }

            }

            activeCallFragment.onCallFailed();
        }
        else if (mContext != null){
            Utils.sendSnackBarData(mContext, mContext.getString(R.string.operation_failed), Utils.SNACKBAR_LONG);
        }
    }


    /**
     * Handle click on CallStatusFragment. Depending on number of calls
     * in array of calls we are hiding status fragment or replacing
     * currently active call
     */
    public void onCallStateClicked() {
        if (mCalls == null || mCalls.size() == Constants.CALL_SIZE_0) {
            return;
        }
        if (mCalls.size() == Constants.CALL_SIZE_1) {
            ActiveCallFragment activeCallFragment = ((ActiveCallFragment) mCalls.get(mCurrentCallId));
            activeCallFragment.setVisible();
            hideCallStatusFragment();

            int callId= SDKManager.getInstance().getCallAdaptor().getActiveCallId();
            if (callId !=0 && callId!=mCurrentCallId) {
                //there is antoher call with only dialtone (without a active call fragment)
                mCallViewAdaptor.endCall(callId);
                activeCallFragment.setOffhookButtonChecked(false);
            }
        } else if (mCalls.size() == Constants.CALL_SIZE_2) {
            //There are 2 calls. Need to replace them

            ActiveCallFragment activeCallFragment = ((ActiveCallFragment) mCalls.get(mCurrentCallId));

            //1.replace fragments
            setFragmentAsActiveCallFragment(getCallStatusCallId());

            //2.update calls status (un-hold will automatically make the previously active call on-hold)
            if (activeCallFragment != null) {
                showAndUpdateCallStatusFragment(activeCallFragment.getContactName(), activeCallFragment.getCallState(), activeCallFragment.getCallId());
            }
        }
    }

    @Override
    public void onCallTransferFailed() {
        ActiveCallFragment activeCallFragment = ((ActiveCallFragment) mCalls.get(mCurrentCallId));
        if (activeCallFragment != null) {
            activeCallFragment.onTransferFailed();
        }
        if (mCalls.size() == Constants.CALL_SIZE_1) {
            hideCallStatusFragment();
        }
    }

    /**
     * Return ID of call from list of calls based
     * on current call ID
     *
     * @return Int representation of calls id
     */
    private int getCallStatusCallId() {
        int callId = 0;
        for (int i = 0; i < mCalls.size(); i++) {
            if (mCurrentCallId != mCalls.keyAt(i)) {
                callId = mCalls.keyAt(i);
            }
        }
        return callId;
    }

    /**
     * Setting ActiveCallFragment for specified callId
     *
     * @param callId ID of call for which we have to set up fragment
     */
    private void setFragmentAsActiveCallFragment(int callId) {

        ActiveCallFragment fragment = (ActiveCallFragment) mCalls.get(callId);
        mCurrentCallId = callId;
        FragmentTransaction ft = mFm.beginTransaction();
        ft.replace(R.id.active_call, fragment, ACTIVE_CALL_TAG);
        ft.commitAllowingStateLoss();
        mFm.executePendingTransactions();

    }

    /**
     * Prepare and show CallStatusFragment for specific call name
     *
     * @param callName  String from which name have to be extracted
     * @param callState call state enum
     */
    private void showAndUpdateCallStatusFragment(String callName, UICallState callState, int callId) {
        CallStatusFragment callStatusFragment = (CallStatusFragment) mFm.findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
        if (callStatusFragment != null && callName != null && callState != null) {
            callStatusFragment.setCallId(callId);
            callStatusFragment.updateCallStatusName(callName);
            callStatusFragment.setCallStateChanged(callState);
            callStatusFragment.showCallStatus();
        }
    }

    /**
     * Hide CallStatusFragment
     */
    private void hideCallStatusFragment() {
        CallStatusFragment callStatusFragment = (CallStatusFragment) mFm.findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
        if (callStatusFragment != null && callStatusFragment.getActivity() != null && !callStatusFragment.getActivity().isDestroyed()) {
            callStatusFragment.hideCallStatus();
            Utils.hideKeyboard(callStatusFragment.getActivity());
            ((MainActivity) callStatusFragment.getActivity()).backToFullScreen();
        }

        if(mContext!=null && (MainActivity) mContext!=null &&
                mContext.getResources().getBoolean(R.bool.is_landscape) == true ) {


            FragmentManager fragmentManager = ((MainActivity) mContext).getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            if(fragments != null){
                for(Fragment fragment : fragments){
                    if(fragment != null && fragment.isVisible())
                        if (fragment instanceof VideoCallFragment) {
                            ((MainActivity) mContext).changeUiForFullScreenInLandscape(true);
                            break;
                        }else if(fragment instanceof ActiveCallFragment) {
                            ((MainActivity) mContext).changeUiForFullScreenInLandscape(false);
                        }
                }
            }

        }
    }

    private Handler mHandler = new Handler(Looper.myLooper());
    private static final String INCOMING_CALL_TAG = "IncomingCallTag";

    @Override
    public void onIncomingCallReceived(final UICall call) {
        Log.d(TAG, "onIncomingCallReceived: Name: " + call.getRemoteDisplayName() + ", Number: " + call.getRemoteNumber());

        if (!ElanApplication.isMainActivityVisible()) {
                if (mContext == null) {
                    Log.e(TAG, "mContext is NULL - can not proceed");
                    return;
                }
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(MainActivity.INCOMING_CALL_INTENT,true);
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
                try {
                    pendingIntent.send();
                    Utils.wakeDevice(mContext);
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "failed to activate MainActivity from pending intent while it was not visible");
                }
        }
        else {
            showIncomingCallUI(call);
        }

    }

    @Override
    public void onCallHoldUnholdSuccessful(int callId, boolean shouldHold) {
        ActiveCallFragment fragment = (ActiveCallFragment) mCalls.get(callId);
        if (fragment != null)
            fragment.setHoldButtonChecked(shouldHold);

    }

    @Override
    public void onCallHoldFailed(int callId) {
        ActiveCallFragment fragment = (ActiveCallFragment) mCalls.get(callId);
        if (fragment != null ) {
            fragment.onCallHoldFailed(callId);
        }
    }

    @Override
    public void onCallConferenceStatusChanged(Call call, boolean isConference) {
        ActiveCallFragment activeCallFragment = (ActiveCallFragment) mCalls.get(call.getCallId());
        if (activeCallFragment != null) {
            activeCallFragment.onCallConferenceStatusChanged();
        }
    }

    @Override
    public void onCallCreated(UICall uiCall) {
        if (mContext != null && ((Activity)mContext).getWindow() != null) {
            ((Activity) mContext).getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    public void showIncomingCallUI(final UICall call) {
        IncomingCallFragment incomingCallFragment = (IncomingCallFragment) mFm.findFragmentByTag(INCOMING_CALL_TAG);
        if( (incomingCallFragment != null) && (incomingCallFragment.isDismissed())) {
            Log.d(TAG, "IncomingCallFragment was dismissed.");
            FragmentTransaction ft = mFm.beginTransaction();
            ft.remove(incomingCallFragment);
            ft.commitNow();
            incomingCallFragment = (IncomingCallFragment) mFm.findFragmentByTag(INCOMING_CALL_TAG);
        }
        if (incomingCallFragment == null) {
            Log.d(TAG, "showIncomingCallUI new IncomingCallFragment");
            incomingCallFragment = new IncomingCallFragment();
            incomingCallFragment.init(mCallViewAdaptor);
            FragmentTransaction ft = mFm.beginTransaction();
            ft.add(incomingCallFragment, INCOMING_CALL_TAG);
            ft.commitAllowingStateLoss();

            mHandler.post(new Runnable() {
                IncomingCallFragment mFragment;

                Runnable setParam(IncomingCallFragment incomingCallFragment) {
                    mFragment = incomingCallFragment;
                    return this;
                }

                @Override
                public void run() {
                    mFragment.addCall(call);
                }
            }.setParam(incomingCallFragment));

        } else {
            Log.d(TAG, "showIncomingCallUI: existing IncomingCallFragment");
            incomingCallFragment.addCall(call);
        }

        if (mCallNotificationFactory != null) {
            mCallNotificationFactory.show(call);
        } else {
            mCallNotificationFactory = new CallNotificationFactory(mContext);
            mCallNotificationFactory.removeAll();
            mCallNotificationFactory.show(call);
        }
    }

    /**
     * Removing incoming call fragment
     *
     * @param call current call
     */
    private void removeIncomingCall(UICall call) {
        IncomingCallFragment incomingCallFragment = (IncomingCallFragment) mFm.findFragmentByTag(INCOMING_CALL_TAG);
        if (incomingCallFragment != null) {
            incomingCallFragment.removeCall(call.getCallId());
        }
    }

    /**
     * Broadcast Receiver
     */
    private BroadcastReceiver mIncomingReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(Constants.INCOMING_CALL_ACCEPT)) {
                int callId = intent.getIntExtra(Constants.CALL_ID, 0);
                IncomingCallFragment fragment = (IncomingCallFragment) mFm.findFragmentByTag(INCOMING_CALL_TAG);
                if (fragment != null) {
                    fragment.acceptAudioCall(callId);
                }
            }
        }
    };

    /**
     * Pause activity
     */
    public void onActivityPause() {
        if (mContext != null && mIsIncomingReceiverRegistered) {
            mContext.unregisterReceiver(mIncomingReceiver);
            mIsIncomingReceiverRegistered = false;
        }
    }

    /**
     * Resume Activity
     */
    public void onActivityResume() {
        if (mContext != null) {
            mContext.registerReceiver(mIncomingReceiver, new IntentFilter(Constants.INCOMING_CALL_ACCEPT));
            mIsIncomingReceiverRegistered = true;
        }
    }

    /**
     * Get active calls size
     *
     * @return size
     */
    public int getNumActiveCallFragments() {
        return mCalls.size();
    }

    public int getCurretCallId(){
        return mCurrentCallId;
    }

    public boolean hasIncomingCall() {
        return SDKManager.getInstance().getCallAdaptor().isAlertingCall() != 0;
    }
}
