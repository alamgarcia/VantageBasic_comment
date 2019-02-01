package com.avaya.android.vantage.basic.adaptors;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.ViewGroup;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.OnCallDigitCollectionCompletedListener;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.csdk.CallAdaptorListener;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.model.UIAudioDevice;
import com.avaya.android.vantage.basic.model.UICall;
import com.avaya.android.vantage.basic.views.interfaces.ICallViewInterface;
import com.avaya.clientservices.call.Call;

import static android.content.Context.MODE_PRIVATE;

/**
 * {@link UICallViewAdaptor} is responsible for listening and processing events connected to
 * user calls. With {@link ICallViewInterface} it is connected to
 * {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler}
 */

public class UICallViewAdaptor implements CallAdaptorListener {

    private ICallViewInterface mCallViewInterface;
    //private IIncomingCallViewInterface mIncomingCallInterface;
    private ICallControlsInterface mCallControlsInterface;
    private final String LOG_TAG = this.getClass().getSimpleName();

    private OnCallDigitCollectionCompletedListener mDigitCollectionListener;
    private SharedPreferences mAudioSelectionPreference;

    /**
     * Alerting {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler} with
     * {@link ICallViewInterface} method on remote call
     */
    @Override
    public void onCallRemoteAlerting() {
        Log.d(LOG_TAG, "onCallRemoteAlerting");
        if (mCallViewInterface != null) {
            mCallViewInterface.onCallRemoteAlert();
        }
    }

    /**
     * Alerting {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler} with
     * {@link ICallViewInterface} method for remote address change
     * @param uiCall {@link UICall} for which remote address is changed
     * @param newDisplayName String new name to be shown for call name
     */
    @Override
    public void onCallRemoteAddressChanged(UICall uiCall, String newDisplayName) {
        Log.d(LOG_TAG, "onCallRemoteAddressChanged");
        if (mCallViewInterface != null) {
            mCallViewInterface.onCallRemoteAddressChanged(uiCall, newDisplayName);
        }
    }


    @Override
    public void onCallDigitCollectionPlayDialTone(UICall call) {
    }

    /**
     *
     * @param call {@link UICall}
     * @return int
     */
    @Override
    public int onCallDigitCollectionCompleted(UICall call) {
        if(mDigitCollectionListener != null) {
            mDigitCollectionListener.onCallDigitCollectionCompleted(call);
        }
        return 0;
    }

    /**
     * Check if call is active in {@link SDKManager}
     * @return boolean
     */
    @Override
    public boolean isActive() {
        return SDKManager.getInstance().getCallAdaptor().getActiveCallId() == 0;
    }

    /**
     * Check if call is denied
     * @param uiCall {@link UICall} for which to check if call is denied
     */
    @Override
    public void onCallDenied(UICall uiCall) {
        Log.d(LOG_TAG, "onCallDenied");
    }

    /**
     * Sending information to {@link ICallControlsInterface} which then provide that data to
     * {@link com.avaya.android.vantage.basic.activities.MainActivity}
     * @param uiCall {@link UICall}for which video muting is performed
     * @param muting boolean which tell to us if video is to be muted
     */
    @Override
    public void onVideoMuted(UICall uiCall, boolean muting) {
        if (mCallControlsInterface != null) {
            mCallControlsInterface.onVideoMuted(uiCall, muting);
        }
    }

    @Override
    public void onCallHoldUnholdSuccessful(int callId, boolean checked){
        Log.d(LOG_TAG, "onCallHoldUnholdSuccessful: checked " + checked);
        mCallViewInterface.onCallHoldUnholdSuccessful(callId, checked);
    }

    @Override
    public void onCallHoldFailed(int callId) {
        Log.d(LOG_TAG, "onCallHoldFailed");
        mCallViewInterface.onCallHoldFailed(callId);
    }

    /**
     * Alerting {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler} with
     * {@link ICallViewInterface} method called to report a change in conference status of the call.
     * @param call The call that the callback is associated with.
     * @param isConference True if the call has become a conference call; false, if the call
     */
    @Override
    public void onCallConferenceStatusChanged(Call call, boolean isConference) {
        Log.d(LOG_TAG, "onCallConferenceStatusChanged: is conferences call " + isConference);
        if (mCallViewInterface != null) {
            mCallViewInterface.onCallConferenceStatusChanged(call, isConference);
        }
    }

    @Override
    public void onCallCreated(UICall uiCall) {
        Log.d(LOG_TAG, "onCallCreated");
        mCallViewInterface.onCallCreated(uiCall);
    }

    /**
     * {@link ICallViewInterface} method called when {@link UICall} is established
     * @param call {@link UICall} which is established
     */
    @Override
    public void onCallEstablished(UICall call) {
        if (call != null){
            Log.d(LOG_TAG, "onCallEstablished: " + call.getRemoteDisplayName());
            mCallViewInterface.onCallEstablished(call);
        }
    }

    /**
     * {@link ICallViewInterface} method called when {@link UICall} is held
     * @param call {@link UICall} which is held
     */
    @Override
    public void onCallHeld(UICall call) {
        Log.d(LOG_TAG, "onCallHeld");
        if (mCallViewInterface!=null && call != null)
            mCallViewInterface.setCallStateChanged(call);
    }

    /**
     * {@link ICallViewInterface} method call when {@link UICall} is not held
     * due to unknown reason
     * @param call {@link UICall} which is not held
     */
    @Override
    public void onCallUnheld(UICall call) {
        Log.d(LOG_TAG, "onCallUnheld");
        if (mCallViewInterface!=null && call != null)
            mCallViewInterface.setCallStateChanged(call);
    }

    /**
     * Removing call from {@link ICallViewInterface}
     * interfaces. Also refresh history icon from {@link com.avaya.android.vantage.basic.activities.MainActivity}
     * when call is removed
     * @param call {@link UICall} which is removed
     */
    @Override
    public void onCallRemoved(UICall call) {
        Log.d(LOG_TAG, "onCallRemoved");
        if (call != null) {
            if (mCallViewInterface != null)
                mCallViewInterface.onCallRemoved(call);
//            if (mIncomingCallInterface != null)
//                mIncomingCallInterface.onCallRemoved(call);
            if (mCallControlsInterface != null && call.isMissedCall()) {
                mCallControlsInterface.onCallMissed();
            }
        }
    }

    /**
     * {@link CallAdaptorListener} method override and send {@link UICall} forward to
     * {@link ICallViewInterface}
     * @param call {@link UICall} which is removed
     */
    @Override
    public void onIncomingCallReceived(UICall call) {
        if (call != null && mCallViewInterface != null) {
            Log.d(LOG_TAG, "onIncomingCallReceived. Name: " + call.getRemoteDisplayName() + ", Number: " + call.getRemoteNumber());
            mCallViewInterface.onIncomingCallReceived(call);
        }
    }

    @Override
    public void onActiveCallChanged(UICall call) {
        Log.d(LOG_TAG, "onActiveCallChanged");
    }

    /**
     * Transfer call with provided call id to provided target
     * @param callId of call to be transfer
     * @param target to which call have to be transfer
     */
    @Override
    public void transferCall(int callId, String target) {
        SDKManager.getInstance().getCallAdaptor().transferCall(callId, target);
    }

    /**
     * Add party to call to create conference call or to existing conference call
     * @param callId of call to be added
     * @param target call or conference call to which call have to be added
     */
    @Override
    public void conferenceCall(int callId, String target) {
        SDKManager.getInstance().getCallAdaptor().addPartyToCall(callId, target);
    }

    /**
     * In case of failed call we are informing over {@link ICallViewInterface} interface
     * {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler}
     * @param uiCall {@link UICall}  for which call failed
     */
    @Override
    public void onCallFailed(UICall uiCall) {
        mCallViewInterface.onCallFailed(uiCall);
    }

    /**
     * In case of call escalated to video call we are informing
     * {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler} over
     * {@link ICallViewInterface} interface
     * @param uiCall for which video is escalated
     */
    @Override
    public void onCallEscalatedToVideoSuccessful(UICall uiCall) {
        mCallViewInterface.onCallEscalatedToVideoSuccessful(uiCall);
    }

    /**
     * Called to report that the call's signaling path has failed.
     * @param uiCall The call that the callback is associated with.
     */
    @Override
    public void onCallServiceUnavailable(UICall uiCall) {
        mCallViewInterface.onCallServiceUnavailable(uiCall);
    }

    /**
     * In case of call escalation to video call failed we are informing
     * {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler} over
     * {@link ICallViewInterface} interface
     * @param uiCall for which video failed to escalate
     */
    @Override
    public void onCallEscalatedToVideoFailed(UICall uiCall) {
        mCallViewInterface.onCallEscalatedToVideoFailed(uiCall);
    }

    /**
     * In case of call deescalation to audio call we are informing
     * {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler} over
     * {@link ICallViewInterface} interface
     * @param uiCall for which video deescalated to audio
     */
    @Override
    public void onCallDeescalatedToAudio(UICall uiCall) {
        mCallViewInterface.onCallDeescalatedToAudio(uiCall);
    }

    /**
     * In case of call deescalation to audio call failed we are informing
     * {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler} over
     * {@link ICallViewInterface} interface
     * @param uiCall for which video failed to escalate
     */
    @Override
    public void onCallDeescalatedToAudioFailed(UICall uiCall) {
        mCallViewInterface.onCallDeescalatedToAudioFailed(uiCall);
    }

    /**
     * In case we have incoming call we are informing about it
     * {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler} over
     * {@link ICallViewInterface} interface
     * @param call incoming call which is received and we have to inform
     * {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler}
     */
    @Override
    public void onCallStarted(UICall call) {
        if (call != null && mCallViewInterface != null) {
            Log.d(LOG_TAG, "onCallStarted. Number: " + call.getRemoteNumber() + ", Name: " + call.getRemoteDisplayName() );
            mCallViewInterface.onCallStarted(call);
        }
    }

    /**
     * Set view interface for {@link UIContactsViewAdaptor}
     * @param viewInterface {@link ICallViewInterface} which have to be set
     */
    public void setViewInterface(ICallViewInterface viewInterface) {
        this.mCallViewInterface = viewInterface;
    }

    /**
     * Set {@link ICallControlsInterface}
     * @param viewInterface
     */
    public void setCallControlsInterface(ICallControlsInterface viewInterface) {
        this.mCallControlsInterface = viewInterface;
    }

//    /**
//     * Set incoming call interface for {@link UIContactsViewAdaptor}
//     * @param viewInterface {@link IIncomingCallViewInterface} which have to be set
//     */
//    public void setIncomingCallInterface(IIncomingCallViewInterface viewInterface) {
//        this.mIncomingCallInterface = viewInterface;
//    }

    /**
     * Creating call based on #number value. In case #isVideoCall is true we will attempt to create
     * video call
     * @param number for which call have to be created
     * @param isVideoCall is true in case that we are creating video call for provided number
     * @return int
     */
    public int createCall(String number, boolean isVideoCall) {
        return SDKManager.getInstance().getCallAdaptor().createCall(number, isVideoCall);
    }

    /**
     * Creating video call and setting it to video call in case #isVideoCall is true
     * @param isVideoCall
     * @return call id which have to be created
     */
    public int createCall(boolean isVideoCall) {
        int callId = SDKManager.getInstance().getCallAdaptor().createCall(isVideoCall);
        return callId;

    }

    /**
     * Ending call with #callId provided as parameter. {@link SDKManager} is called to end provided
     * call.
     * @param callId which have to be ended
     */
    public void endCall(int callId) {
        SDKManager.getInstance().getCallAdaptor().endCall(callId);
    }

    /**
     * Perform call hold for call with #callId provided as parameter. {@link SDKManager} is called
     * to perform hold of call
     * @param callId of call to be set on hold
     */
    public void holdCall(int callId) {
        SDKManager.getInstance().getCallAdaptor().holdCall(callId);
    }

    /**
     * Remove call from hold status and return to state of active call.
     *
     * @param callId of call which have to be removed from hold state
     */
    public void unholdCall(int callId) {
        SDKManager.getInstance().getCallAdaptor().unholdCall(callId);
    }

    /**
     * Accept call from user #callId with possibility to create escalate to video call in case
     * that #userRequestedVideo value is true. {@link SDKManager} is informed that we are accepting
     * specified call.
     * @param callId of incoming call
     * @param userRequestedVideo is true if user which call requested video call
     */
    public void acceptCall(int callId, boolean userRequestedVideo) {
        KeyguardManager kgMgr = ElanApplication.getContext().getSystemService(KeyguardManager.class);
        boolean isLocked = (kgMgr != null) && kgMgr.isDeviceLocked() && !ElanApplication.isPinAppLock;


        if (isLocked) {
            if (mAudioSelectionPreference == null) {
                mAudioSelectionPreference = ElanApplication.getContext().getSharedPreferences("selectedAudioOption", MODE_PRIVATE);
            }
            String prefValue = mAudioSelectionPreference.getString(Constants.AUDIO_PREF_KEY, (UIAudioDevice.SPEAKER).toString());
            UIAudioDevice device = UIAudioDevice.SPEAKER;
            try {
                device = UIAudioDevice.valueOf(prefValue.toUpperCase());
                if (device != SDKManager.getInstance().getAudioDeviceAdaptor().getActiveAudioDevice()) {
                    SDKManager.getInstance().getAudioDeviceAdaptor().setUserRequestedDevice(device);
                }
            }
            catch (IllegalArgumentException e) {
               Log.e(LOG_TAG, "could not set audio device in locked state: prefValue '" + prefValue + "' is not a properdevice");
            }

        }
        SDKManager.getInstance().getCallAdaptor().acceptCall(callId, userRequestedVideo);
    }

    /**
     * Ignoring call with provided #callId. {@link SDKManager} is informed about ignoring of
     * specific call.
     * @param callId of ignored call
     */
    public void ignoreCall(int callId) {
        SDKManager.getInstance().getCallAdaptor().ignoreCall(callId);
    }

    /**
     * Denny call from #callId . {@link SDKManager} is informed about denying call to provided
     * #callId
     * @param callId of user which have to be denied
     */
    public void denyCall(int callId) {
        SDKManager.getInstance().getCallAdaptor().denyCall(callId);
    }

    /**
     * Sending DTMF #digit for provided #callId. {@link SDKManager} is informed to which call is
     * sendt #digit
     * @param callId to which #digit is sent
     * @param digit which is sent to #callId
     */
    public void sendDTMF(int callId, char digit) {
        SDKManager.getInstance().getCallAdaptor().sendDTMF(callId, digit);
    }

    /**
     * Obtaining display name from {@link SDKManager} for provided #callId
     * @param callId for which remote display name is requested
     * @return display name for provided #callId
     */
    public String getRemoteDisplayName(int callId) {
        String name = null;

        UICall call = SDKManager.getInstance().getCallAdaptor().getCall(callId);

        if (call != null) {
            name = call.getRemoteDisplayName();
        }
        return name;
    }

    /**
     * Start video in provided viewgroup. Call for whihc video have to be started is provided
     * as parameter #callId
     * @param viewGroup in which video have to be shown
     * @param callId of call for which video is started
     */
    public void startVideo(ViewGroup viewGroup, int callId) {
        SDKManager.getInstance().getCallAdaptor().startVideo(viewGroup, callId);
    }

    /**
     * Successful transferred call
     * @param callId to which to transfer
     * @param callIdToReplace from which to transfer
     */
    public void onCallTransferSuccessful(final int callId, final int callIdToReplace) {
        Log.d(LOG_TAG, "onCallTransferSuccessful " + callId + " " + callIdToReplace);
    }

    /**
     * Successful transferred call
     * @param callId to which to transfer
     * @param remoteAddress from which to transfer
     */
    public void onCallTransferSuccessful(final int callId, final String remoteAddress) {
        Log.d(LOG_TAG, "onCallTransferSuccessful " + callId + " " + remoteAddress);
    }

    /**
     * In case transfer call failed
     */
    public void onCallTransferFailed() {
        Log.d(LOG_TAG, "onCallTransferFailed");
        mCallViewInterface.onCallTransferFailed();
    }

    /**
     * Initialize video call for provided callId. {@link SDKManager} is informed about
     * initialization of video for provided callId
     * @param context {@link Context} in which video call is initialized
     * @param callId of call for which video is initialized
     */
    public void initVideoCall(Context context, int callId) {
        SDKManager.getInstance().getCallAdaptor().initVideoCallManager(context, callId);
    }

    /**
     * Adding participant was successful.
     * @param callId of original call
     * @param callId2BeAdded of call which is added
     */
    public void onAddParticipantSuccessful(int callId, int callId2BeAdded) {
        Log.d(LOG_TAG, "onAddParticipantSuccessful " + callId + " " + callId2BeAdded);
    }

    /**
     * Adding participant to call failed
     */
    public void onAddParticipantFailed(Context context, boolean maxParticipantsReached) {
        Log.d(LOG_TAG, "onAddParticipantFailed");
        if (maxParticipantsReached)
            Utils.sendSnackBarDataWithDelay(context, context.getResources().getString(R.string.max_participants_in_conference), Utils.SNACKBAR_SHORT);
        else
            Utils.sendSnackBarData(context, context.getResources().getString(R.string.operation_failed), Utils.SNACKBAR_SHORT);
    }

    /**
     * Informing {@link SDKManager} that audio mute state is toggled
     */
    public void audioMuteStateToggled() {
        Log.d(LOG_TAG, "Audio Mute was called in UICallViewAdaptor");
        SDKManager.getInstance().getCallAdaptor().audioMuteStateToggled();
    }

    /**
     * Informing {@link SDKManager} that video mute state is toggled
     */
    public void videoMuteStateToggled() {
        Log.d(LOG_TAG, "Video Mute was called in UICallViewAdaptor");
        SDKManager.getInstance().getCallAdaptor().videoMuteStateToggled();
    }

    /**
     * Changing state of audio mute based on parameters provided. {@link SDKManager} is
     * informed about this change
     * @param mute boolean parameter noticing about audio mute state
     */
    public void changeAudioMuteState(boolean mute) {
        Log.d(LOG_TAG, "Mute was called in UICallViewAdaptor");
        SDKManager.getInstance().getCallAdaptor().changeAudioMuteState(mute);
    }

    /**
     * Changing state of video mute based on parameters provided. {@link SDKManager} is
     * informed about this change
     * @param mute boolean parameter noticing about video mute state
     */
    public void changeVideoMuteState(boolean mute) {
        Log.d(LOG_TAG, "Mute was called in UICallViewAdaptor");
        SDKManager.getInstance().getCallAdaptor().changeVideoMuteState(mute);
    }

    /**
     * Call video stop in {@link SDKManager} for provided call id
     * @param callId for video have to be stopped
     */
    public void stopVideo(int callId) {
        SDKManager.getInstance().getCallAdaptor().stopVideo(callId);
    }


    /**
     * Sending information to {@link SDKManager} that we are destroying video
     * view fot call specified in parameters
     * @param callId for which video view have to be destroyed
     */
    public void onDestroyVideoView(int callId) {
        SDKManager.getInstance().getCallAdaptor().onDestroyVideoView(callId);
    }

    /**
     * Obtaining total number of calls from {@link SDKManager}
     * @return number of calls obtained from {@link SDKManager}
     */
    public int getNumOfCalls() {
        return SDKManager.getInstance().getCallAdaptor().getNumOfCalls();
    }

    /**
     * Obtaining data from {@link SDKManager} is call for which we provided call id CM conference
     * @param callId of call for which we are performing check in {@link SDKManager}
     * @return boolean result of check
     */
    public boolean isCMConferenceCall(int callId) {
        return SDKManager.getInstance().getCallAdaptor().isCMConferenceCall(callId);
    }

    /**
     * Obtaining data from {@link SDKManager} if call for which we provided call id is conference
     * @param callId of call for which we are performing check in {@link SDKManager}
     * @return boolean result of check
     */
    public boolean isConferenceCall(int callId) {
        return SDKManager.getInstance().getCallAdaptor().isConferenceCall(callId);
    }

    /**
     * Obtaining data from {@link SDKManager} is call for which we provided call id IPO conference
     * @param callId of call for which we are performing check in {@link SDKManager}
     * @return boolean result of check
     */
    public boolean isIPOConferenceCall(int callId) {
        return SDKManager.getInstance().getCallAdaptor().isIPOConference(callId);
    }

    /**
     * Escalate call from provided call id to video call
     * @param callId of call which have to be escalated
     */
    public void escalateToVideoCall(int callId) {
        SDKManager.getInstance().getCallAdaptor().escalateToVideoCall(callId);
    }

    /**
     * Deescalate call to audio call
     * @param callId of call which have to be deescalated
     */
    public void deescalateToAudioCall(int callId) {
        SDKManager.getInstance().getCallAdaptor().deescalateToAudioCall(callId);
    }

    /**
     * Adding digits to off hook dial call and informing {@link SDKManager} about it
     * @param digit about which {@link SDKManager} is to be informed about
     */
    public void addDigitToOffHookDialCall(char digit){
        SDKManager.getInstance().getCallAdaptor().addDigit(digit);
    }

    /**
     * Returning current {@link OnCallDigitCollectionCompletedListener}
     * @return {@link OnCallDigitCollectionCompletedListener}
     */
    public OnCallDigitCollectionCompletedListener getDigitCollectionListener() {
        return mDigitCollectionListener;
    }

    /**
     * Setting {@link OnCallDigitCollectionCompletedListener}
     * @param mDigitCollectionListener {@link OnCallDigitCollectionCompletedListener} to be set
     */
    public void setDigitCollectionListener(OnCallDigitCollectionCompletedListener mDigitCollectionListener) {
        this.mDigitCollectionListener = mDigitCollectionListener;
    }
}
