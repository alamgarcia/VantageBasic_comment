package com.avaya.android.vantage.basic.csdk;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.widget.Toast;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.GoogleAnalyticsUtils;
import com.avaya.android.vantage.basic.MidnightGoogleAnalyticsStatistics;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.ToneManager;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.model.UIAudioDevice;
import com.avaya.android.vantage.basic.model.UICall;
import com.avaya.android.vantage.basic.model.UICallState;
import com.avaya.android.vantage.basic.notifications.CallNotificationFactory;
import com.avaya.clientservices.call.AllowedVideoDirection;
import com.avaya.clientservices.call.Call;
import com.avaya.clientservices.call.CallCompletionHandler;
import com.avaya.clientservices.call.CallCreationInfo;
import com.avaya.clientservices.call.CallEndReason;
import com.avaya.clientservices.call.CallError;
import com.avaya.clientservices.call.CallException;
import com.avaya.clientservices.call.CallListener;
import com.avaya.clientservices.call.CallPrecedenceLevel;
import com.avaya.clientservices.call.CallPreemptionReason;
import com.avaya.clientservices.call.CallService;
import com.avaya.clientservices.call.CallServiceListener;
import com.avaya.clientservices.call.CallState;
import com.avaya.clientservices.call.DTMFType;
import com.avaya.clientservices.call.MediaDirection;
import com.avaya.clientservices.call.TransferCompletionHandler;
import com.avaya.clientservices.call.TransferProgressCode;
import com.avaya.clientservices.call.VideoChannel;
import com.avaya.clientservices.call.VideoDisabledReason;
import com.avaya.clientservices.call.VideoMode;
import com.avaya.clientservices.call.feature.BusyIndicator;
import com.avaya.clientservices.call.feature.CallFeatureService;
import com.avaya.clientservices.call.feature.CallFeatureServiceListener;
import com.avaya.clientservices.call.feature.CallPickupAlertParameters;
import com.avaya.clientservices.call.feature.EnhancedCallForwardingStatus;
import com.avaya.clientservices.call.feature.FeatureStatusParameters;
import com.avaya.clientservices.call.feature.FeatureType;
import com.avaya.clientservices.call.feature.TeamButton;
import com.avaya.clientservices.call.feature.TeamButtonIncomingCall;
import com.avaya.clientservices.media.AudioTone;
import com.avaya.clientservices.media.gui.PlaneViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.text.TextUtils.isEmpty;
import static com.avaya.android.vantage.basic.Constants.MAX_NUM_PARTICIPANTS_IN_CM_CONFERENCE;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PSTN_VM_NUM;
import static com.avaya.clientservices.call.CallState.ALERTING;
import static com.avaya.clientservices.call.CallState.ESTABLISHED;
import static com.avaya.clientservices.call.CallState.FAILED;
import static com.avaya.clientservices.call.CallState.HELD;
import static com.avaya.clientservices.call.CallState.REMOTE_ALERTING;
import static com.avaya.clientservices.call.VideoMode.DISABLE;

/**
 * {@link CallAdaptor} used in {@link com.avaya.android.vantage.basic.fragments.ActiveCallFragment}
 * and {@link SDKManager}
 */
public class CallAdaptor implements CallListener, CallServiceListener, CallFeatureServiceListener {

    public static final int MAX_NUM_CALLS = 2;
    public static final int DTMF_LENGTH = 700;
    public static final int DIGIT_TIME = 200;
    private final String LOG_TAG = this.getClass().getSimpleName();
    private Context mContext;
    private WeakReference<CallAdaptorListener> mUiObj;
    private SharedPreferences mCallPreference;
    private static final String REDIAL_NUMBER = "redialNumber";

    private final ToneManager mToneManager;

    private Queue<Runnable> mDelayedIncomingCalls = new ArrayDeque<>();

    private HashMap mVideoMap = new HashMap<Integer, VideoManager>();

    private boolean mAudioMuted = false;
    private boolean mVideoMuted = false;

    private int mConferenceCallId = -1;
    private int mAddPartyCallId = -1;
    private AudioManager audioManager;
    private final Handler mHandler;
    private Call mOffHookDialCall;
    private boolean mIsIncomingCallAccpetedAsVideo = false;
    private PlaneViewGroup mVideoPlaneViewGroup;

    private static final int CALL_CANNOT_BE_ADDED = -1;
    private static final int CONFIG_ADAPTER_NOT_SET = -2;
    private static final int CALL_SERVICE_NOT_READY = -3;
    private static final int CALL_CANNOT_BE_CREATED = -4;
    private static final int CALL_ALREADY_IN_PROGRESS = -5;

    private CallNotificationFactory mCallNotificationFactory;

    /**
     * Public constructor for {@link CallAdaptor}
     *
     * @param context
     */
    public CallAdaptor(Context context) {
        mHandler = new Handler(Looper.getMainLooper());
        this.mContext = context;
        mCallPreference = context.getSharedPreferences(Constants.CALL_PREFS, MODE_PRIVATE);
        mToneManager = new ToneManager(context);
        mVideoPlaneViewGroup = new PlaneViewGroup(context);
        mCallNotificationFactory = new CallNotificationFactory(context);
    }


    private List<Call> getCallList(){
        List<Call> callList = new ArrayList<Call>();
        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor() != null && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getCallService() != null)
            callList = (List)SDKManager.getInstance().getDeskPhoneServiceAdaptor().getCallService().getCalls();

        // ignore remote calls
        List<Call> localCallList = new ArrayList<Call>();
        for (Iterator<Call> iterator = callList.iterator(); iterator.hasNext();) {
            Call call = iterator.next();
            if ((call != null) && !call.isRemote()) {
                // Remove the current element from the iterator and the list.
                localCallList.add(call);
            }
        }

        return localCallList;
    }

    /**
     * Register listeners for {@link CallAdaptor}
     *
     * @param uiObj {@link CallAdaptorListener} object
     */
    public void registerListener(CallAdaptorListener uiObj) {

        mUiObj = new WeakReference<CallAdaptorListener>(uiObj);

        try {
            if (mUiObj.get() == null) {
                Log.d(LOG_TAG, "reference to CallAdaptor is null");
            } else {
                Log.d(LOG_TAG, "reference to CallAdaptor is NOT null");
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        while (mDelayedIncomingCalls.peek() != null) {
            mHandler.post(mDelayedIncomingCalls.poll());
        }
    }

    /**
     * Return the call with specified call id if it is not removed yet from {@link SparseArray}
     *
     * @param callId int id of specific call
     * @return {@link Call} object which is obtained from calls map
     */
    private Call getCallByCallId(int callId) {
        for (Call call : getCallList()){
            if (call.getCallId() == callId)
                return call;
        }
        return null;
    }

    /**
     * Obtain call data and provide it in for of {@link UICall}
     *
     * @param callId call id
     * @return {@link UICall} based on callId
     */
    public UICall getCall(int callId) {

        Call call = getCallByCallId(callId);
        if (call == null) {
            return null;
        }

        return convertCall(call);
    }


    /**
     * Obtain number of stored {@link Call} objects in {@link SparseArray}
     *
     * @return int with number of objects
     */
    public int getNumOfCalls() {
        return  getCallList().size();
    }

    /**
     * Obtain the ID of the held call {@link Call} objects in {@link SparseArray} if exists
     *
     * @return int the id of the held call if exists; -1 otherwise
     */
    public int getHeldCallId() {

        for (Call call :  getCallList()) {
            if (call == null) {
                continue;
            }
            if (call.getState() == HELD) {
                return call.getCallId();
            }
        }

        return -1;
    }

    /**
     * Returns any call different than the one specified as diffFromCallId
     * @param diffFromCallId Call ID
     * @return Call ID
     */
    public int getOtherCallId(int diffFromCallId) {

        for (Call call :  getCallList()) {
            if (call == null) {
                continue;
            }
            if (call.getCallId() != diffFromCallId) {
                return call.getCallId();
            }
        }

        return -1;
    }

    /**
     * Create call, set the number and return call id of created call
     *
     * @param calledParty string representing address of who have to be called
     * @param isVideoCall boolean which determinate is this call video call
     * @return int which represent call id. In case of error it will return -1
     */
    public int createCall(String calledParty, boolean isVideoCall) {
        // Create call
        if (mOffHookDialCall != null) {
            mOffHookDialCall.end();
            mToneManager.stop();
            mOffHookDialCall = null;
        }

        if (getNumOfCalls() == MAX_NUM_CALLS) {
            Log.i(LOG_TAG, "Exceeded max call number. Can not add another call");
            return -1;
        }

        // special handling for voice-mail numbers in IPO
        boolean bPstnVmNum = false;
        if (isIPO() && (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParam(PSTN_VM_NUM) != null)) {
            String pstnNumStr = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParam(PSTN_VM_NUM);
            if (calledParty!=null&&pstnNumStr.equals(calledParty)) {
                bPstnVmNum = true;
            }

            // if the number to call is the user part of the voice-mail number, reconstructure the whole voice-mail number
            if (calledParty!=null&&pstnNumStr.startsWith(calledParty) && (pstnNumStr.length() > calledParty.length()) && (pstnNumStr.charAt(calledParty.length()) == '@')) {
                bPstnVmNum = true;
                calledParty = pstnNumStr;
            }
        }

        // extracting numbers from a string and saving redial number
        if (!bPstnVmNum && calledParty!=null) {
            calledParty = calledParty.split("\\@")[0];
            SharedPreferences mSharedPref = mContext.getSharedPreferences(REDIAL_NUMBER, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString(REDIAL_NUMBER, calledParty);
            editor.apply();
        }

        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor() == null) {
            Log.e(LOG_TAG, "createCall: LoginConfigAdaptor is not set");
            return -1;
        }
        CallService callService = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getCallService();
        if (callService == null) {
            Log.e(LOG_TAG, "createCall: Call service is not ready");
            return -1;
        }

        Call call = callService.createCall();
        if (call == null) {
            Log.e(LOG_TAG, "callCreate: call can not be created");
            return -1;
        }

        call.addListener(this);
        // Set far-end's number
        call.setRemoteAddress(calledParty);

        if (getNumOfCalls() == 0) {
            mVideoMuted = false;
        }

        startCall(call, isVideoCall);

        // Get unique call id specified for created call
        int callId = call.getCallId();

        AudioFocusManager.getInstance(mContext).requestAudioFocusIfNeeded(AudioManager.STREAM_VOICE_CALL, getNumOfCalls());

        connectBluetoothIfNeeded();

        return callId;
    }

    /**
     * Creating call
     *
     * @param isVideoCall boolean
     * @return state of call or error
     */
    public int createCall(boolean isVideoCall) {

        if (mOffHookDialCall != null) {
            Log.e(LOG_TAG, "mOffHookDialCall is already in Progress");
            return CALL_ALREADY_IN_PROGRESS;
        }

        if (getNumOfCalls() == MAX_NUM_CALLS) {
            Log.i(LOG_TAG, "Exceeded max call number. Can not add another call");
            return CALL_CANNOT_BE_ADDED;
        }

        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor() == null) {
            Log.e(LOG_TAG, "createCall: LoginConfigAdaptor is not set");
            return CONFIG_ADAPTER_NOT_SET;
        }
        CallService callService = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getCallService();
        if (callService == null) {
            Log.e(LOG_TAG, "createCall: Call service is not ready");
            return CALL_SERVICE_NOT_READY;
        }

        CallCreationInfo callCreationInfo = new CallCreationInfo();
        callCreationInfo.setOffHookDialingEnabled(true);
        Call call = callService.createCall(callCreationInfo);
        if (call == null) {
            Log.e(LOG_TAG, "callCreate: call can not be created");
            return CALL_CANNOT_BE_CREATED;
        }

        call.addListener(this);

        if (getNumOfCalls() == 0) {
            mVideoMuted = false;
        }

        if (isVideoCall) {
            addVideo(call);
        }

        call.start();

        mOffHookDialCall = call;
        Log.i(LOG_TAG, "createCall: created a call in off hook dialing mode");


//        if (mContext != null) {
//            String cachedDevice = mContext.getSharedPreferences("selectedAudioOption", Context.MODE_PRIVATE).getString(Constants.AUDIO_PREF_KEY, (UIAudioDevice.SPEAKER).toString());
//            Log.d(LOG_TAG, "createCall cachedDevice = " + cachedDevice);
//            SDKManager.getInstance().getAudioDeviceAdaptor().setUserRequestedDevice(UIAudioDevice.valueOf(cachedDevice));
//        }

        AudioFocusManager.getInstance(mContext).requestAudioFocusIfNeeded(AudioManager.STREAM_VOICE_CALL, getNumOfCalls());

        connectBluetoothIfNeeded();

        mToneManager.play(AudioTone.DIAL);

        return call.getCallId();
    }

    private void connectBluetoothIfNeeded(){
        UIAudioDevice device = SDKManager.getInstance().getAudioDeviceAdaptor().getUserRequestedDevice();
        if (device.equals(UIAudioDevice.BLUETOOTH_HEADSET) || device.equals(UIAudioDevice.WIRELESS_HANDSET)) {
            //if device is BT handset/headset, we need to connect BT
            SDKManager.getInstance().getAudioDeviceAdaptor().connectBluetoothSCO(mContext);
        }
    }

    /**
     * Start call. If isVideoCall is set to true we will start video call
     *
     * @param call        {@link Call} object which have to be started
     * @param isVideoCall boolean representing is call should be video
     */
    private void startCall(Call call, boolean isVideoCall) {

        if (isVideoCall) {
            // Add video to the call
            addVideo(call);
        }
        call.start();
    }

    /**
     * End call with provided callId
     *
     * @param callId of {@link Call} to be ended
     */
    public void endCall(int callId) {

        if (mOffHookDialCall != null && mOffHookDialCall.getCallId() == callId) {
            mOffHookDialCall.end();
            mOffHookDialCall = null;
            mToneManager.stop();
        } else {
            Call call = getCallByCallId(callId);
            if (call != null) {
                if (call.getState() != CallState.HELD) {
                    mToneManager.stop();
                }
                call.end();
            }
        }

    }

    /**
     * Hold {@link Call} with provided call id
     *
     * @param callId int id of call to be set on hold
     */
    public void holdCall(final int callId) {
        Call call = getCallByCallId(callId);
        if (call != null) {
            call.hold( new CallCompletionHandler() {
                @Override
                public void onSuccess() {
                    // Do nothing - handled in onCallAudioMuteStatusChanged
                    Log.d(LOG_TAG, "Call " + callId + "call hold succesfully");
                    onCallHoldUnholdSuccessful(callId, true);
                }

                @Override
                public void onError(@NonNull CallException error) {
                    Log.e(LOG_TAG, "Call " + callId + " failed to hold call: reason " + getCallExceptionReason(error));
                    onCallHoldFailed(callId);
                }
            });
        }
    }

    /**
     * Set {@link Call} from hold state to active call state
     *
     * @param callId int id of call which have to be moved to active state
     */
    public void unholdCall(final int callId) {
        Call call = getCallByCallId(callId);
        if (call != null) {
            call.unhold( new CallCompletionHandler() {
                @Override
                public void onSuccess() {
                    // Do nothing - handled in onCallAudioMuteStatusChanged
                    Log.d(LOG_TAG, "Call " + callId + "call unhold succesfully");
                    onCallHoldUnholdSuccessful(callId, false);
                }

                @Override
                public void onError(@NonNull CallException error) {
                    Log.e(LOG_TAG, "Call " + callId + " failed to unhold call: reason " + getCallExceptionReason(error));
                }
            });
        }
    }

    /**
     * Changing state of Audio Mute state
     */
    public void audioMuteStateToggled() {
        Log.d(LOG_TAG, "Audio mute button pressed, setting mute status to " + !mAudioMuted);
        changeAudioMuteState(!mAudioMuted);
    }

    /**
     * Changing state of Video Mute state
     */
    public void videoMuteStateToggled() {
        Log.d(LOG_TAG, "Video mute button pressed, setting mute status to " + !mVideoMuted);
        changeVideoMuteState(!mVideoMuted);
    }

    /**
     * Performing attended transfer
     *
     * @param callId          of {@link Call} current call
     * @param callIdToReplace id of {@link Call} to which call have to be transfer
     */
    public void transferCall(final int callId, final int callIdToReplace) {
        Call call = getCallByCallId(callId);
        Call callToReplace = getCallByCallId(callIdToReplace);
        if ((call == null) || (callToReplace == null)) {
            Log.e(LOG_TAG, "One of the calls does not exist.");
            if (mUiObj!=null && mUiObj.get() != null) {
                mUiObj.get().onCallTransferFailed();
            }
            return;
        }

        final TransferCompletionHandler completionHandler = new TransferCompletionHandler() {
            @Override
            public void onSuccess() {
                if (mUiObj!=null && mUiObj.get() != null) {
                    mUiObj.get().onCallTransferSuccessful(callId, callIdToReplace);
                }
            }

            @Override
            public void onProgressUpdate(TransferProgressCode statusCode) {
            }

            @Override
            public void onError(Exception error) {
                if(error instanceof CallException) {
                    Log.w(LOG_TAG, "Call transfer failed with error " + getCallExceptionReason((CallException) error));
                } else {
                    Log.w(LOG_TAG, "Call transfer failed");
                }
                if (mUiObj!=null && mUiObj.get() != null) {
                    mUiObj.get().onCallTransferFailed();
                }
            }
        };
        call.transfer(callToReplace, completionHandler);
    }

    /**
     * Perform unattended call
     *
     * @param callId        of {@link Call} from which transfer to be performed
     * @param remoteAddress to which {@link Call} have to be transfer
     */
    public void transferCall(final int callId, final String remoteAddress) {
        Call call = getCallByCallId(callId);
        if (call == null) {
            Log.e(LOG_TAG, "Call does not exist.");
            if (mUiObj!=null && mUiObj.get() != null) {
                mUiObj.get().onCallTransferFailed();
            } else {
                Log.e(LOG_TAG, "Weak ref is null");
            }
            return;
        }
        final TransferCompletionHandler completionHandler = new TransferCompletionHandler() {
            @Override
            public void onSuccess() {
                if (mUiObj!=null && mUiObj.get() != null) {
                    mUiObj.get().onCallTransferSuccessful(callId, remoteAddress);
                }
            }

            @Override
            public void onProgressUpdate(TransferProgressCode statusCode) {
            }

            @Override
            public void onError(Exception error) {
                if(error instanceof CallException) {
                    Log.w(LOG_TAG, "Call transfer failed with error " + getCallExceptionReason((CallException) error));
                } else {
                    Log.w(LOG_TAG, "Call transfer failed");
                }
                if (mUiObj!=null && mUiObj.get() != null) {
                    mUiObj.get().onCallTransferFailed();
                }
            }
        };
        call.transfer(remoteAddress, completionHandler);
    }

    /**
     * Sending DTMF character on specific {@link Call} based on call id
     *
     * @param callId    of {@link Call} to which we have to send specific character
     * @param charDigit to be sent for specific {@link Call}
     */
    public void sendDTMF(int callId, final char charDigit) {
        Call call = getCallByCallId(callId);
        if (call != null) {
            DTMFType DTMFDigit = parseToDTMF(charDigit);
            if(DTMFDigit != null)
                call.sendDTMF(DTMFDigit);
        }

    }

    /**
     * Parse received character to {@link DTMFType}
     *
     * @param digit to be parsed to {@link DTMFType}
     * @return {@link DTMFType}
     */
    private static DTMFType parseToDTMF(char digit) {
        switch (digit) {
            case '1':
                return DTMFType.ONE;
            case '2':
                return DTMFType.TWO;
            case '3':
                return DTMFType.THREE;
            case '4':
                return DTMFType.FOUR;
            case '5':
                return DTMFType.FIVE;
            case '6':
                return DTMFType.SIX;
            case '7':
                return DTMFType.SEVEN;
            case '8':
                return DTMFType.EIGHT;
            case '9':
                return DTMFType.NINE;
            case '0':
                return DTMFType.ZERO;
            case '#':
                return DTMFType.POUND;
            case '*':
                return DTMFType.STAR;
            default:
                return null;
        }
    }

    /**
     * Accept {@link Call} of specific call id
     *
     * @param callId of {@link Call} which have to be accepted
     */
    public void acceptCall(int callId, boolean userRequestedVideo) {
        Call call = getCallByCallId(callId);
        mIsIncomingCallAccpetedAsVideo = false;

        for (Call c : // end all failed calls.
                SDKManager.getInstance().getDeskPhoneServiceAdaptor().getCallService().getCalls()) {
            if (c != null && c != call && !c.isRemote() && (c.getState() == CallState.FAILED || isMediaPreservedCall(c))) {
                if (isMediaPreservedCall(c)) {
                    Log.d(LOG_TAG, "Media preserved call " + c.getCallId() + " is ended due to incoming call");
                }
                c.end();
            }
        }
        if (call != null) {
            if (call.getState() == CallState.FAILED) {
                call.end();
                return;
            }
            //boolean isVideo = (call.getIncomingVideoStatus() == Call.IncomingVideoStatus.SUPPORTED);
            if (userRequestedVideo) {
                mIsIncomingCallAccpetedAsVideo = true;
                mVideoMuted=false;
                addVideo(call);
            }
            call.accept();
            AudioFocusManager.getInstance(mContext).requestAudioFocusIfNeeded(AudioManager.STREAM_VOICE_CALL, getNumOfCalls());

        }

    }

    /**
     * Ignore {@link Call} with call id provided in parameters
     *
     * @param callId of {@link Call} to be ignored
     */
    public void ignoreCall(int callId) {
        Call call = getCallByCallId(callId);
        if (call != null) {
            if (call.getState() == CallState.FAILED) {
                call.end();
                return;
            }
            call.ignore();
        }
    }

    /**
     * Deny {@link Call} for specific call id
     *
     * @param callId of {@link Call} to be denied
     */
    public void denyCall(int callId) {
        CallCompletionHandler callCompletionHandler = new CallCompletionHandler() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "Video channel added");
            }

            @Override
            public void onError(CallException e) {
                Log.e(LOG_TAG, "Video channel add failed. Exception: " + e.getError());
            }
        };

        Call call = getCallByCallId(callId);
        if (call != null) {
            if (call.getState() == CallState.FAILED) {
                call.end();
                return;
            }
            call.deny(callCompletionHandler);

        }
    }

    /**
     * Adding party to {@link Call} with remoteNumber
     *
     * @param callId       of {@link Call} to which party have to be added
     * @param remoteNumber to be added to party
     */
    public void addPartyToCall(int callId, final String remoteNumber) {
        Log.d(LOG_TAG, "addPartyToCall " + callId + " " + remoteNumber);
        Call call = getCallByCallId(callId);
        if (call != null) {
            addPartyToCall(call, remoteNumber);
        }
    }

    /**
     * Add additional party to {@link Call} with remoteNumber
     *
     * @param call         {@link Call} to which additional party have to be added
     * @param remoteNumber to be added to {@link Call}
     */
    public void addPartyToCall(Call call, final String remoteNumber) {

        Log.d(LOG_TAG, "addPartyToCall " + remoteNumber);
        if ((call == null) || (call.getConference() == null)) {
            Log.e(LOG_TAG, "Can not create conference.");
            return;
        }

        // check whether maximal number of participants has reached for CM conference
        if (maxNumOfParticipantsReached(call)) {
            Log.d(LOG_TAG, "Maximal number of participants reached.");
            if (mUiObj!=null && mUiObj.get() != null) {
                mUiObj.get().onAddParticipantFailed(mContext, true);
            }
            return;
        }

        // add the participant via dialout if it is supported
        if (call.getConference().getAddParticipantViaDialoutCapability().isAllowed()) {
            addPartyToCallViaDialout(call, remoteNumber);
            return;
        }

        // add participant by first calling to it
        addPartyToCallViaCallMerging(call, remoteNumber);
    }

    /**
     * This is for adding participant through dialout option (for AAC and Scopia, not for CM).
     *
     * @param call         {@link Call} to which party is added
     * @param remoteNumber of party to be added
     */
    private void addPartyToCallViaDialout(Call call, final String remoteNumber) {

        call.getConference().addParticipant(remoteNumber, new CallCompletionHandler() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "Adding a party to the call is successful");
            }

            @Override
            public void onError(CallException error) {
                Log.d(LOG_TAG, "Add party " + remoteNumber + " to call failed: " + getCallExceptionReason((error)));
            }
        });
    }

    /**
     * This is for adding participant through two calls merging (for CM only).
     *
     * @param call         {@link Call} to which party have to be added
     * @param remoteNumber to be added to {@link Call}
     */
    private void addPartyToCallViaCallMerging(Call call, final String remoteNumber) {

        if ((call == null) || (call.getConference() == null)) {
            Log.d(LOG_TAG, "Can not create conference via merging.");
            return;
        }

        mConferenceCallId = call.getCallId();
        mAddPartyCallId = createCall(remoteNumber, false);
    }

    /**
     * Add participant to {@link Call} based on call id of current call and callId2BeAdded
     *
     * @param callId         of {@link Call} to which another participant have to be added
     * @param callId2BeAdded of participant to be added
     */
    public void addParticipant(final int callId, final int callId2BeAdded) {
        Log.d(LOG_TAG, "addParticipant: " + callId + " + " + callId2BeAdded);
        Call call = getCallByCallId(callId);
        final Call callToAdd = getCallByCallId(callId2BeAdded);
        if ((call == null) || (callToAdd == null)) {
            Log.e(LOG_TAG, "One of the calls does not exist.");
            if (mUiObj!=null && mUiObj.get() != null) {
                mUiObj.get().onAddParticipantFailed(mContext, false);
            }
            return;
        }

        final CallCompletionHandler completionHandler = new CallCompletionHandler() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "addParticipant succeeded");
                if (mUiObj!=null && mUiObj.get() != null) {
                    mUiObj.get().onAddParticipantSuccessful(callId, callId2BeAdded);
                }
            }

            @Override
            public void onError(CallException error) {
                Log.w(LOG_TAG, "addParticipant failed with error " + getCallExceptionReason(error));
                if (mUiObj!=null && mUiObj.get() != null) {
                    mUiObj.get().onAddParticipantFailed(mContext, false);
                }
                // if this is dial-out trial to add a new participant - end the dialed-out call
                if (mConferenceCallId == callId) {
                    callToAdd.end();
                }
            }
        };
        if (call.isConference()) {
            // check whether maximal number of participants has reached for CM conference
            if (maxNumOfParticipantsReached(call)) {
                Log.d(LOG_TAG, "Maximal number of participants reached.");
                if (mUiObj!=null && mUiObj.get() != null) {
                    mUiObj.get().onAddParticipantFailed(mContext, true);
                }
                return;
            }
        }

        callToAdd.getConference().addParticipantFromCall(call, completionHandler);
    }

    /**
     * @param callId ID of Call
     * @return true if this is a conference , not advanced call
     */
    public boolean isCMConferenceCall(int callId) {
        Call call = getCallByCallId(callId);
        return call != null && call.isConference() && !isAdvancedConference(call) && !isIPO();
    }

    /**
     * @param callId ID of Call
     * @return true if this is a conference 
     */
    public boolean isConferenceCall(int callId) {
        Call call = getCallByCallId(callId);
        return call != null && call.isConference();
    }

    /**
     * @param call reference to Call object
     * @return true if this is Advanced conference call
     */
    private boolean isAdvancedConference(Call call) {
        return call.isConference() && call.getConference().getRetrieveParticipantListCapability().isAllowed();
    }

    /**
     * @param callId ID of Call
     * @return true if this is an IPO conference call
     */
    public boolean isIPOConference(int callId) {
        Call call = getCallByCallId(callId);
        return call != null && call.isConference() && isIPO();
    }

    /**
     * @param callId ID of Call
     * @return true if this is a conference call
     */
    public boolean isConference(int callId) {

        Call call = getCallByCallId(callId);
        return call != null && call.isConference();
    }


    /**
     * Removes last participant from the conference call
     * @param callId ID of the conference call
     */
    public void dropLastParticipant(final int callId) {

        Call call = getCallByCallId(callId);
        if (call == null) {
            Log.e(LOG_TAG, "No call found for callId " + callId + ". Cannot drop participant.");
            return;
        }

        CallCompletionHandler callCompletionHandler = new CallCompletionHandler() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "Last participant was removed from conference " + callId);
            }

            @Override
            public void onError(CallException e) {
                Log.e(LOG_TAG, "Cannot remove last participant from conference " + callId + ": " + e.getError());
            }
        };
        call.getConference().removeLastParticipant(callCompletionHandler);
    }

    /**
     * Check whether user has drop last capability.
     * We enable or disable this feature regarding returned boolean value.
     *
     * @param callId
     * @return true if drop last participant is allowed, otherwise false
     */
    public boolean isDropLastParticipantEnabled(final int callId) {
        Call call = getCallByCallId(callId);
        if (call == null || call.getConference() == null) {
            return false;
        }

        return call.getConference().getRemoveLastParticipantCapability().isAllowed();
    }

    /**
     * Create video chanel and set it for the call. In case video is not supported
     * for specific {@link Call} false will be returned.
     *
     * @param call {@link Call} to which we need to add video
     * @return boolean
     */
    public boolean addVideo(Call call) {
        // Check if video supported
        if (!call.getUpdateVideoModeCapability().isAllowed()) {
            Log.d(LOG_TAG, "Don't add video. Video isn't supported");
            return false;
        }

        VideoManager videoManager = (VideoManager) mVideoMap.get(call.getCallId());
        if (videoManager == null) {
            videoManager = new VideoManager(mVideoPlaneViewGroup);
        }
        //noinspection unchecked
        mVideoMap.put(call.getCallId(), videoManager);

        // Set video mode for the call depending on camera device
        videoManager.setVideoMode(call);
        return true;
    }

    /**
     * Change audio mute state for all {@link Call} in mCallsMap
     *
     * @param muting boolean
     */
    public void changeAudioMuteState(final boolean muting) {

        for (Call call :  getCallList()) {
            if (call == null || call == mOffHookDialCall) {
                continue;
            }
            changeAudioMuteState(call, muting);
        }
        mAudioMuted = muting;
    }

    /**
     * Changing video mute state for all {@link Call} in mCallsMap
     *
     * @param muting boolean
     */
    public void changeVideoMuteState(final boolean muting) {

        for (Call call :  getCallList()) {
            if (call == null) {
                continue;
            }
            changeVideoMuteState(call, muting);
        }
    }

    /**
     * Change audio mute state for specific {@link Call} based on muting
     *
     * @param call   {@link Call} which have to be muted
     * @param muting boolean
     */
    private void changeAudioMuteState(final Call call, final boolean muting) {

        call.muteAudio(muting, new CallCompletionHandler() {
            @Override
            public void onSuccess() {
                // Do nothing - handled in onCallAudioMuteStatusChanged
                Log.d(LOG_TAG, "Call " + call.getCallId() + "mute state was changed to " + muting);
            }

            @Override
            public void onError(@NonNull CallException error) {
                Log.d(LOG_TAG, "Call " + call.getCallId() + " failed to " + (muting ? "mute" : "unmute") + ": " + getCallExceptionReason(error));
            }
        });
    }

    /**
     * Force mute for video for {@link Call}.
     *
     * @param call   {@link Call} on which we want to mute video
     * @param muting boolean
     */
    public void changeVideoMuteState(final Call call, final boolean muting) {

        if ((call.getVideoChannels() == null) || call.getVideoChannels().isEmpty()) {
            Log.d(LOG_TAG, "This is an audio call. Cannot mute/unmute video.");
            onVideoMuted(call, mVideoMuted);
            return;
        }

        if (call.getState() == HELD){
            Log.d(LOG_TAG,  call.getCallId() + " video call is on hold. can't not mute video");
            onVideoMuted(call, mVideoMuted);
            return;
        }

        VideoManager videoManager = (VideoManager) mVideoMap.get(call.getCallId());
        if (videoManager != null) {
            VideoMode curVideoMode = call.getVideoMode();
            if (curVideoMode != VideoMode.RECEIVE_ONLY && curVideoMode != VideoMode.SEND_RECEIVE) {
                Log.d(LOG_TAG,  call.getCallId() + " video mode is " + curVideoMode + ". mute state is not change");
                onVideoMuted(call, mVideoMuted);
                return;
            } else {
                VideoMode newVideoMode = muting ? VideoMode.RECEIVE_ONLY : VideoMode.SEND_RECEIVE;

                if (newVideoMode == curVideoMode) {
                    Log.d(LOG_TAG,  call.getCallId() + " video mode didn't change. mute state is not change");
                    onVideoMuted(call, mVideoMuted);
                    return;
                }
                call.setVideoMode(newVideoMode, new CallCompletionHandler() {
                    @Override
                    public void onSuccess() {
                        Log.d(LOG_TAG, "Video mode has been set to " + muting + " for call id " + call.getCallId());
                        mVideoMuted = muting;
                        onVideoMuted(call, mVideoMuted);

                    }

                    @Override
                    public void onError(CallException e) {
                        Log.e(LOG_TAG, "Video mode can't be set to " + muting +  ". call state is  " + call.getState() + ". Exception: " + e.getError() + " call id=" + call.getCallId());
                        mVideoMuted = !muting;
                        onVideoMuted(call, mVideoMuted);
                    }
                });
            }
        }
    }

    /**
     * Notifies {@CallAdaptorListener} that the video call is muting or unmuting
     * @param call reference to Call object
     * @param muting true if Video call is muting
     */
    private void onVideoMuted(Call call, boolean muting) {
        UICall UiCall = convertCall(call);
        if (mUiObj != null && mUiObj.get() != null) {
            mUiObj.get().onVideoMuted(UiCall, muting);
        }
    }

    /**
     * Notifies {@CallAdaptorListener} about hold/unhold being successful
     * @param callId ID of Call
     * @param shouldHold true if the call should be tagged as on hold
     */
    private void onCallHoldUnholdSuccessful(int callId, boolean shouldHold) {
        if (mUiObj != null && mUiObj.get() != null) {
            mUiObj.get().onCallHoldUnholdSuccessful(callId, shouldHold);
        }
    }

    /**
     * Notifies {@CallAdaptorListener} about hold action failed
     * @param callId ID of Call
     */
    private void onCallHoldFailed(int callId) {
        if (mUiObj != null && mUiObj.get() != null) {
            mUiObj.get().onCallHoldFailed(callId);
        }
    }

    public boolean ismVideoMuted() {
        return mVideoMuted;
    }


    /**
     * Convert from {@link com.avaya.clientservices.call.CallState} to {@link UICallState}
     * for provided {@link Call}
     *
     * @param call {@link Call} from which we have to get {@link com.avaya.clientservices.call.CallState}
     * @return {@link UICallState}
     */
    private UICallState convertCallState(Call call) {
        if (call == null) {
            return null;
        }
        UICallState state;
        switch (call.getState()) {
            case IDLE:
                state = UICallState.IDLE;
                break;
            case ESTABLISHED:
                state = UICallState.ESTABLISHED;
                break;
            case REMOTE_ALERTING:
                state = UICallState.REMOTE_ALERTING;
                break;
            case HELD:
                // check whether onCallHeld was ever called for this call.
                // If not, the hold operation actually failed (although the CSDK left the state of the call as HELD) and
                // call shall be handled as if it is in ESTABLISHED state
                if (call.getHeldTimeMillis() != 0)
                    state = UICallState.HELD;
                else
                    state = UICallState.ESTABLISHED;
                break;
            case ENDED:
                state = UICallState.ENDED;
                break;
            case FAILED:
                state = UICallState.FAILED;
                break;
            case TRANSFERRING:
                state = UICallState.TRANSFERRING;
                break;
            default:
                state = UICallState.NOT_RELEVANT;
        }
        return state;
    }

    /**
     * Convert current {@link Call} to {@link UICall}
     *
     * @param call    {@link Call} to be converted
     * @param isVideo boolean is {@link Call} video call
     * @return {@link UICall} to be returned
     */
    private UICall convertCall(Call call, boolean isVideo) {
        if (call != null) {
            return new UICall(call.getCallId(), convertCallState(call), call.getRemoteDisplayName(), call.getRemoteNumber(), isVideo, call.isEmergencyCall(), call.getEstablishedTimeMillis(), call.getHeldTimeMillis());
        } else {
            return new UICall(-1, UICallState.NOT_RELEVANT, "", "", false, false, 0, 0);
        }
    }

    /**
     * Convert current {@link Call} to {@link UICall}
     *
     * @param call {@link Call} to be converted
     * @return {@link UICall} to be returned
     */
    private UICall convertCall(Call call) {
        if (call == null) {
            return null;
        }

        boolean isVideo;
        isVideo = !(call.getVideoChannels() == null || call.getVideoChannels().isEmpty());

        if((call.getState().equals(ALERTING) && call.getIncomingVideoStatus().equals(Call.IncomingVideoStatus.SUPPORTED))){
            isVideo = true;
        }
        else if (!isVideo && call.isIncoming()) {
            isVideo = mIsIncomingCallAccpetedAsVideo;
        }



        return new UICall(call.getCallId(), convertCallState(call), call.getRemoteDisplayName(), call.getRemoteNumber(), isVideo, call.isEmergencyCall(), call.getEstablishedTimeMillis(), call.getHeldTimeMillis());
    }

    public boolean isIncomingCall(int callId) {
        Call call = getCallByCallId(callId);
        return call != null && call.isIncoming();
    }

    @Override
    public void onCallStarted(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());

        UICall UiCall = convertCall(call);
        if (mUiObj!=null && mUiObj.get() != null) {
            mUiObj.get().onCallStarted(UiCall);
        }
    }

    @Override
    public void onCallRemoteAlerting(Call call, boolean hasEarlyMedia) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
        if (mUiObj!=null && mUiObj.get() != null) {
            mUiObj.get().onCallRemoteAlerting();
        }
        if (!hasEarlyMedia) {
            mToneManager.play(AudioTone.RING_BACK);
        }
    }

    @Override
    public void onCallRedirected(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallQueued(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallEstablished(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());

        connectBluetoothIfNeeded();

        if (call.getCallId() == mAddPartyCallId) {
            Log.d(LOG_TAG, "Merge calls " + mConferenceCallId + " and " + mAddPartyCallId);
            addParticipant(mConferenceCallId, mAddPartyCallId);
        }

        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        mToneManager.stop();

        UICall UiCall = convertCall(call);

        if (mAudioMuted) {
            changeAudioMuteState(call, mAudioMuted);
        }

        if (mVideoMuted) {
            changeVideoMuteState(call, mVideoMuted);
        }

        if (mUiObj!=null && mUiObj.get() != null) {
            mUiObj.get().onCallEstablished(UiCall);
        }

        GoogleAnalyticsUtils.googleAnalyticsOnCallEstablished(UiCall);
        MidnightGoogleAnalyticsStatistics.increaseCallsCounterPreference(mContext);
    }

    @Override
    public void onCallRemoteAddressChanged(Call call, String newAddress, String newDisplayName) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
        if (mUiObj!=null && mUiObj.get() != null) {
            UICall uiCall = convertCall(call);
            mUiObj.get().onCallRemoteAddressChanged(uiCall, newDisplayName);
        }
    }

    @Override
    public void onCallHeld(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
        UICall UiCall = convertCall(call);
        if (mUiObj!=null && mUiObj.get() != null) {
            mUiObj.get().onCallHeld(UiCall);
        }
    }

    @Override
    public void onCallUnheld(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
        mToneManager.stop();
        UICall UiCall = convertCall(call);
        if (UiCall.isVideo()) {
            Log.d(LOG_TAG, getCurrentMethodName() + "calling changeVideoMuteState for call id=" + call.getCallId() + ". call state = " + call.getState());
            changeVideoMuteState(call, mVideoMuted);
        }
        if (mUiObj!=null && mUiObj.get() != null) {
            mUiObj.get().onCallUnheld(UiCall);
        }
    }

    @Override
    public void onCallHeldRemotely(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallUnheldRemotely(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallJoined(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallEnded(Call call, CallEndReason endReason) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId() + " " + endReason.toString());
        GoogleAnalyticsUtils.googleAnaliticsOnCallEnd(call);
        mIsIncomingCallAccpetedAsVideo = false;
    }

    @Override
    public void onCallFailed(Call call, CallException exception) {
        Log.d(LOG_TAG, getCurrentMethodName() + " state:" + call.getState() +" id:" + call.getCallId() + " reason:" + getCallExceptionReason(exception));

        mToneManager.stop();
        mToneManager.play(AudioTone.ERROR_BEEP);

        /*if (call.equals(mOffHookDialCall)) {
            mOffHookDialCall.end();
            mOffHookDialCall = null;
        }*/
        boolean callNotStarted = false;
        if (call.equals(mOffHookDialCall)) {
            mHandler.postDelayed(new EndFailedCallRunnable(call.getCallId()), Constants.END_FAILED_CALL_DELAY_MILLIS);
        }
        else if ((exception.getError()== CallError.USER_NOT_FOUND) && (call.getRemoteNumber()==null || call.getRemoteNumber().equals(""))) {
            callNotStarted = true;
        }

        UICall UiCall = convertCall(call);
        if (mUiObj!=null && mUiObj.get() != null) {
            if (callNotStarted)
                mUiObj.get().onCallStarted(UiCall);
            mUiObj.get().onCallFailed(UiCall);
        }
        else {
            Toast.makeText(mContext, R.string.operation_failed, Toast.LENGTH_SHORT).show();
            call.end();
        }
    }

    @Override
    public void onCallDenied(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName());
        UICall UiCall = convertCall(call);
        if (mUiObj!=null && mUiObj.get() != null) {
            mUiObj.get().onCallDenied(UiCall);
        }
    }

    @Override
    public void onCallIgnored(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallAudioMuteStatusChanged(Call call, boolean muted) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
    }

    /**
     * Called when speaker is silenced or unsilenced for the call.
     *
     * @param call     The call associated with the callback.
     * @param silenced The new silence state.
     */
    @Override
    public void onCallSpeakerSilenceStatusChanged(Call call, boolean silenced) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
    }

    @Override
    public void onCallVideoChannelsUpdated(Call call, List<VideoChannel> videoChannels) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());

        VideoManager videoManager = (VideoManager) mVideoMap.get(call.getCallId());
        if (videoManager != null) {
            if (!videoChannels.isEmpty()) {
                if (!videoChannels.get(0).isEnabled() && videoChannels.get(0).getDisabledReason() == VideoDisabledReason.REMOTE_USER) {
                    // video was disabled by the remote user. set our video mode to disabled as well so that csdk does
                    // not automatically start streaming our video if the far end escalates
                    deescalateToAudioCall(call);
                }
            }
            MediaDirection mediaDirection = videoChannels.get(0).getNegotiatedDirection();
            if (mediaDirection == MediaDirection.SEND_RECEIVE)
                mVideoMuted=false;
            else if (mediaDirection == MediaDirection.RECEIVE_ONLY)
                mVideoMuted=true;
            videoManager.onCallVideoChannelsUpdated(videoChannels);

        }
    }

    @Override
    public void onCallIncomingVideoAddRequestReceived(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());

        VideoManager videoManager = (VideoManager) mVideoMap.get(call.getCallId());
        if (videoManager == null) {
            videoManager = new VideoManager(mVideoPlaneViewGroup);
        }
        //noinspection unchecked
        mVideoMap.put(call.getCallId(), videoManager);
        // Set video mode for the call depending on camera device
        videoManager.acceptVideo(call);
    }

    @Override
    public void onCallIncomingVideoAddRequestAccepted(Call call, VideoChannel videoChannel) {
        //when the othder party escalate to video
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());

        UICall UiCall = convertCall(call, true);
        if (mUiObj!=null && mUiObj.get() != null) {
            mUiObj.get().onCallEscalatedToVideoSuccessful(UiCall);
        }

        mVideoMuted=true;

        VideoManager videoManager = (VideoManager) mVideoMap.get(call.getCallId());
        if (videoManager != null) {
            VideoMode videoMode = mVideoMuted ? VideoMode.RECEIVE_ONLY : VideoMode.SEND_RECEIVE;
            videoManager.setVideoMode(call, videoMode);
        }
    }

    @Override
    public void onCallIncomingVideoAddRequestDenied(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
    }

    @Override
    public void onCallIncomingVideoAddRequestTimedOut(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
    }

    @Override
    public void onCallConferenceStatusChanged(Call call, boolean isConference) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
        VideoManager videoManager = (VideoManager) mVideoMap.get(call.getCallId());
        if (isConference && videoManager != null && !call.getVideoChannels().isEmpty() &&
                call.getVideoChannels().get(0).getNegotiatedDirection() == MediaDirection.INACTIVE) {
            //the video call has escalated to conference, but video is INACTIVE, sothe call need to be de-escalated to audio
            deescalateToAudioCall(call);
        }

        if (mUiObj!=null && mUiObj.get() != null) {
            mUiObj.get().onCallConferenceStatusChanged(call, isConference);
        }
    }

    @Override
    public void onCallCapabilitiesChanged(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
    }

    @Override
    public void onCallServiceAvailable(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
    }

    @Override
    public void onCallServiceUnavailable(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
        Log.d(LOG_TAG, "is hold allowed =" + call.getHoldCapability().isAllowed());

        if (call.getState() == HELD) {
            Log.d(LOG_TAG, "Held call " + call.getCallId() + " is ended due to FO");
            call.end();
            return;
        }

        UICall UiCall = convertCall(call);
        if (mUiObj!=null && mUiObj.get() != null) {
            mUiObj.get().onCallServiceUnavailable(UiCall);
        }
    }

    @Override
    public void onCallParticipantMatchedContactsChanged(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
    }

    @Override
    public void onCallDigitCollectionPlayDialTone(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName());

        mToneManager.play(AudioTone.DIAL);
        if (mUiObj!=null && mUiObj.get() != null) {
            UICall uiCall = convertCall(call, mVideoMap.get(call.getCallId()) != null);
            (mUiObj.get()).onCallDigitCollectionPlayDialTone(uiCall);
        }
    }

    @Override
    public void onCallDigitCollectionCompleted(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName());
        mOffHookDialCall = null;
        mToneManager.stop();

        AudioFocusManager.getInstance(mContext).requestAudioFocusIfNeeded(AudioManager.STREAM_VOICE_CALL, getNumOfCalls());


        if (mUiObj!=null && mUiObj.get() != null) {
            UICall uiCall = convertCall(call, mVideoMap.get(call.getCallId()) != null);
            (mUiObj.get()).onCallDigitCollectionCompleted(uiCall);
        }

    }

    @Override
    public void onCallPrecedenceLevelChanged(Call call, CallPrecedenceLevel newPrecedenceLevel) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallPreempted(Call call, CallPreemptionReason preemptionReason, boolean isPreemptionCompletionRequiredByClient) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallAllowedVideoDirectionChanged(Call call, AllowedVideoDirection allowedVideoDirection) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallExtraPropertiesChanged(Call call, Map<String, String> extraProperties) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallFeatureServiceAvailable(CallFeatureService callFeatureService) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallFeatureServiceUnavailable(CallFeatureService callFeatureService) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onFeatureListChanged(CallFeatureService callFeatureService) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onFeatureCapabilityChanged(CallFeatureService callFeatureService, FeatureType featureType) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onAvailableFeatures(CallFeatureService callFeatureService, List<FeatureType> features) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onFeatureStatus(CallFeatureService callFeatureService, List<FeatureStatusParameters> featureStatusList) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onFeatureStatusChanged(CallFeatureService callFeatureService, FeatureStatusParameters statusInfo) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onSendAllCallsStatusChanged(CallFeatureService callFeatureService, boolean enabled, String extension) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallForwardingStatusChanged(CallFeatureService callFeatureService, boolean enabled, String extension, String destination) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallForwardingBusyNoAnswerStatusChanged(CallFeatureService callFeatureService, boolean enabled, String extension, String destination) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onEnhancedCallForwardingStatusChanged(CallFeatureService callFeatureService, String extension, EnhancedCallForwardingStatus enhancedCallForwardingStatus) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onCallPickupAlertStatusChanged(CallFeatureService callFeatureService, CallPickupAlertParameters callPickupAlertParameters) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onEC500StatusChanged(CallFeatureService callFeatureService, boolean enabled) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onAutoCallbackStatusChanged(CallFeatureService callFeatureService, boolean enabled) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onBusyIndicatorChanged(CallFeatureService callFeatureService, BusyIndicator busyIndicator) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onTeamButtonsChanged(CallFeatureService callFeatureService) {

    }

    /**
     * Occurs if the Team Button busy status is changed when the monitored station
     * eiher dials or has an active call.
     * A Team Button is considered Idle if all call appearances on the monitored station
     * are either in the Idle or Ringing states. If there are bridged call appearances on
     * the monitored station, the states "Alerting" and "In-Use" are interpreted by the CM
     * as equivalent to Idle.
     *
     * @param callFeatureService The call feature service instance reporting the callback.
     * @param teamButton         The team button with busy status updated.
     */
    @Override
    public void onTeamButtonBusyStatusChanged(CallFeatureService callFeatureService, TeamButton teamButton) {

    }

    /**
     * Occurs when the monitored station has an incoming ringing call.
     * The Team Button notification alert status depends on {@link TeamButton#getPickupRingType}.
     * <p>
     * {@link TeamButtonIncomingCall} started ringing at the monitored station is provided.
     *
     * @param callFeatureService          The call feature service instance reporting the callback.
     * @param teamButton                  The team button that has alerting calls.
     * @param teamButtonIncomingCallAdded The team button call that started alerting.
     * @see TeamButton#getPickupCapability
     * @see TeamButton#getSpeedDialCapability
     */
    @Override
    public void onIncomingTeamButtonCallAdded(CallFeatureService callFeatureService, TeamButton teamButton, TeamButtonIncomingCall teamButtonIncomingCallAdded) {

    }

    /**
     * Occurs when the incoming call to monitored station stops ringing.
     * <p>
     * {@link TeamButtonIncomingCall} stopped ringing at the monitored station is provided.
     *
     * @param callFeatureService            The call feature service instance reporting the callback.
     * @param teamButton                    The team button that has alerting calls.
     * @param teamButtonIncomingCallRemoved The team button call that stopped alerting.
     * @see TeamButton#getPickupCapability
     * @see TeamButton#getSpeedDialCapability
     */
    @Override
    public void onIncomingTeamButtonCallRemoved(CallFeatureService callFeatureService, TeamButton teamButton, TeamButtonIncomingCall teamButtonIncomingCallRemoved) {

    }


    /**
     * Occurs when the forwarding status of Team Button monitored station is changed.
     *
     * @param callFeatureService
     * @param teamButton
     * @see TeamButton#isForwardingEnabled
     * @see TeamButton#getForwardingDestination
     */
    @Override
    public void onTeamButtonForwardingStatusChanged(CallFeatureService callFeatureService, TeamButton teamButton) {

    }

    @Override
    public void onIncomingCallReceived(CallService callService, final Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId() + ", Name: " + call.getRemoteDisplayName() + ", Number: " + call.getRemoteNumber());
        if (call.isRemote()) {
            if(!handleBridgeIncomingCall(callService, call))
                return;
        }
        // if all line appearances are occupied, deny the incoming call
        if (getNumOfCalls() > MAX_NUM_CALLS) {
            Log.i(LOG_TAG, "Exceeded max call number. Can not add another call");
            call.deny(null);
            return;
        }

        if (mUiObj == null) {
            //if EULA has not beed accepted, incoming call can't be received
            SharedPreferences preferences = mContext.getSharedPreferences(Constants.EULA_PREFS_NAME, MODE_PRIVATE);
            boolean eulaAccepted = preferences.getBoolean(Constants.KEY_EULA_ACCEPTED, false);
            if (!eulaAccepted) {
                call.deny(null);
                return;
            }

            mContext.startActivity(new Intent(mContext, MainActivity.class).addFlags(FLAG_ACTIVITY_NEW_TASK));
            mDelayedIncomingCalls.add(new Runnable() {
                @Override
                public void run() {
                    processIncomingCall(call);
                }
            });
        } else {
            processIncomingCall(call);
        }

        connectBluetoothIfNeeded();
    }

    private boolean handleBridgeIncomingCall(CallService callService, Call call) {
        Log.w(LOG_TAG, "bridge calls are not implemented yet");
        return false;
    }

    /**
     * Processing incoming {@link Call}
     *
     * @param call {@link Call} object to be processed
     */
    private void processIncomingCall(Call call) {
        if (getNumOfCalls() > MAX_NUM_CALLS) {
            Log.i(LOG_TAG, "Exceeded max call number. Can not add another call");
            return;
        }

        call.addListener(this);

        boolean isVideo = (call.getIncomingVideoStatus() == Call.IncomingVideoStatus.SUPPORTED);
        UICall UiCall = convertCall(call, isVideo);
        if (mUiObj!=null && mUiObj.get() != null) {
            if(call.canAutoAnswer())
                acceptCall(call.getCallId(), isVideo);
            else
                mUiObj.get().onIncomingCallReceived(UiCall);
        }

        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getMode() != AudioManager.MODE_IN_COMMUNICATION)
            audioManager.setMode(AudioManager.MODE_RINGTONE);

        AudioFocusManager.getInstance(mContext).requestAudioFocusIfNeeded(AudioManager.STREAM_RING, getNumOfCalls());

        //Terminate ongoing offhook call
        if (mOffHookDialCall != null) {
            mOffHookDialCall.end();
            mOffHookDialCall = null;
        }
    }


    @Override
    public void onCallCreated(CallService callService, Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());

        UICall uiCall = convertCall(call);
        if (mUiObj != null && mUiObj.get() != null) {
            mUiObj.get().onCallCreated(uiCall);
        }
    }


    @Override
    public void onIncomingCallUndelivered(CallService callService, Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
    }

    @Override
    public void onCallRemoved(CallService callService, Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
        UICall uiCall = convertCall(call);

        saveTimestampOfLastCall();

        if (call.isMissed()) {
            int numberOfMissedCalls = mCallPreference.getInt(Constants.KEY_UNSEEN_MISSED_CALLS, 0) + 1;
            SharedPreferences.Editor editor = mCallPreference.edit();
            editor.putInt(Constants.KEY_UNSEEN_MISSED_CALLS, numberOfMissedCalls);
            editor.apply();

            mCallNotificationFactory.showMissedCallsNotification(numberOfMissedCalls);
        }

        if (getNumOfCalls() == 0) {
            mCallNotificationFactory.showOnLine(mContext.getString(R.string.notification_online));
        }

        if (callService != null && callService.getCalls().size() == 0) {
            mToneManager.stop();
        }

        if (mUiObj != null && mUiObj.get() != null) {
            mUiObj.get().onCallRemoved(uiCall);
        }

        AudioFocusManager.getInstance(mContext).releaseAudioFocus(getNumOfCalls());

        if (getNumOfCalls() == 0) {
            audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_NORMAL);
            SDKManager.getInstance().getAudioDeviceAdaptor().disconnectBluetoothSCO(mContext);
            mVideoMuted = false;
            mOffHookDialCall = null;
        }

    }

    @Override
    public void onCallServiceCapabilityChanged(CallService callService) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onActiveCallChanged(CallService callService, Call call) {
        if (call == null) {
            Log.d(LOG_TAG, getCurrentMethodName() + " for null call");
            return;
        } else {
            Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
        }

        // if this the call to be merged into conference, do nothing
        if (call.getCallId() == mAddPartyCallId) {
            return;
        }

        // outgoing calls are audio-muted in onCallEstablished function
        if (mAudioMuted && call.isIncoming() && !call.isAudioMuted()) {
            changeAudioMuteState(call, mAudioMuted);
        }

        // outgoing calls are video-muted in onCallEstablished function
        if (mVideoMuted && call.isIncoming()) {
            changeVideoMuteState(call, mVideoMuted);
        }

        UICall UiCall = convertCall(call);
        if (mUiObj!=null && mUiObj.get() != null) {
            mUiObj.get().onActiveCallChanged(UiCall);
        }
    }



    /**
     * Obtain all reasons for exceptions in {@link Call}
     *
     * @param exception {@link CallException} to be processed
     * @return String with explanation of {@link CallException}
     */
    private static String getCallExceptionReason(@NonNull CallException exception) {
        final StringBuilder buf = new StringBuilder(128);
        buf.append(exception.getError());
        final String detailMessage = exception.getMessage();
        if (!isEmpty(detailMessage)) {
            buf.append(' ').append(detailMessage);
        }
        final int protocolErrorCode = exception.getProtocolErrorCode();
        if (protocolErrorCode > 0) {
            buf.append(' ').append(protocolErrorCode);
        }
        final String reason = exception.getProtocolErrorReason();
        if (!isEmpty(reason)) {
            buf.append(' ').append(reason);
        }
        return buf.toString();
    }

    /**
     * Obtain current method name for Logs
     *
     * @return String with name of current method name
     */
    private static String getCurrentMethodName() {
        final StackTraceElement e = Thread.currentThread().getStackTrace()[3];
        final String s = e.getClassName();
        return s.substring(s.lastIndexOf('.') + 1, s.length()) + "." + e.getMethodName();
    }

    /**
     * Initializing {@link VideoManager}
     *
     * @param context {@link Context}
     * @param callId  id of {@link Call} for which {@link VideoManager} have to be initialized
     */
    public void initVideoCallManager(Context context, int callId) {
        Call call = getCallByCallId(callId);
        if (call != null) {
            VideoManager videoManager = (VideoManager) mVideoMap.get(call.getCallId());
            //noinspection ConstantConditions
            if (videoManager != null) {
                videoManager.init(context);
            }
        }
    }

    /**
     * Start video for {@link Call} based on call id provided
     *
     * @param videoViewGroup {@link ViewGroup}
     * @param callId         int representation of call id
     */
    public void startVideo(ViewGroup videoViewGroup, int callId) {
        VideoManager videoManager = (VideoManager) mVideoMap.get(callId);
        Call call = getCallByCallId(callId);
        List<VideoChannel> videoChannels=null;
        if (call!= null)
            videoChannels = call.getVideoChannels();
        if (videoManager != null) {
            videoManager.startVideo(videoViewGroup, videoChannels);
        }
    }

    /**
     * Removing video view for specific call id
     *
     * @param callId of {@link Call} for which video view have to be removed
     */
    public void onDestroyVideoView(int callId) {
        VideoManager videoManager = (VideoManager) mVideoMap.get(callId);
        if (videoManager != null) {
            videoManager.onDestroyVideoView();

            if (getCallByCallId(callId) == null) { //call has been removed
                if (videoManager != null)
                    videoManager.onDestroy();
                mVideoMap.remove(callId);
            }
        }
    }

    /**
     * Stopping video for {@link Call} with provided call id
     *
     * @param callId for which video have to be stopped
     */
    public void stopVideo(int callId) {
        VideoManager videoManager = (VideoManager) mVideoMap.get(callId);
        if (videoManager != null) {
            videoManager.stopVideo();
        }
    }


    /**
     * Escalate audio call to video call for {@link Call} with provided
     * callId
     *
     * @param callId of {@link Call} to be escalated
     */
    public void escalateToVideoCall(final int callId) {
        final Call call = getCallByCallId(callId);
        final UICall UiCall = convertCall(call, true);

        if (call != null) {
            VideoManager videoManager = (VideoManager) mVideoMap.get(call.getCallId());
            if (videoManager == null) {
                videoManager = new VideoManager(mVideoPlaneViewGroup);
            }
            //noinspection unchecked
            mVideoMap.put(call.getCallId(), videoManager);

            call.setVideoMode(videoManager.setupCamera(), new CallCompletionHandler() {
                @Override
                public void onSuccess() {
                    Log.d(LOG_TAG, "Video escalation request accepted");

                    if (mUiObj!=null && mUiObj.get() != null) {
                        mUiObj.get().onCallEscalatedToVideoSuccessful(UiCall);
                        mVideoMuted=false;
                    }
                }

                @Override
                public void onError(CallException e) {
                    Log.e(LOG_TAG, "Video escalation request failed. Exception: " + e.getError());
                    if (mUiObj!=null && mUiObj.get() != null) {
                        mUiObj.get().onCallEscalatedToVideoFailed(UiCall);
                    }
                    mVideoMap.remove(callId);
                }
            });
        }

    }

    /**
     * Deescalate from video call to audio call only for {@link Call}
     * with specific callId
     *
     * @param callId for {@link Call} to be deescalated
     */
    public void deescalateToAudioCall(int callId) {
        Call call = getCallByCallId(callId);

        deescalateToAudioCall(call);
    }

    public void deescalateToAudioCall(Call call) {
        Log.d(LOG_TAG, getCurrentMethodName() + " " + call.getCallId());
        VideoManager videoManager = (VideoManager) mVideoMap.get(call.getCallId());
        if (videoManager != null) {

            if (null != call && call.getVideoMode() != DISABLE) {
                final UICall UiCall = convertCall(call, false);
                call.setVideoMode(DISABLE, new CallCompletionHandler() {
                    @Override
                    public void onSuccess() {
                        Log.d(LOG_TAG, "Video mode has been set to DISABLE");
                        if (mUiObj!=null && mUiObj.get() != null) {
                            mUiObj.get().onCallDeescalatedToAudio(UiCall);
                        }
                    }

                    @Override
                    public void onError(CallException e) {
                        Log.e(LOG_TAG, "Video mode can't be set. Exception: " + e.getError());
                        if (mUiObj!=null && mUiObj.get() != null) {
                            mUiObj.get().onCallDeescalatedToAudioFailed(UiCall);
                        }
                    }
                });
            } else {
                Log.e(LOG_TAG, "SetVideoMode, active call is null");
            }
            mVideoMuted=false;
        }
    }

    /**
     * End all {@link Call} in list of calls
     */
    public void endAllCalls() {
        Log.d(LOG_TAG, "endAllCals: numOfCall()="+getNumOfCalls());
        for (Call call :  getCallList()) {
            if (call == null) {
                continue;
            }
            call.end();
        }
    }

    /**
     * @return call id of alerting call
     */
    public int isAlertingCall() {
        for (Call call :  getCallList()) {
            if (call != null && call.getState() == ALERTING) {
                return call.getCallId();
            }
        }
        return 0;
    }

    /**
     * @return the alerting call object
     */
    public UICall getAlertingCall() {
        int callId = isAlertingCall();
        if (callId != 0) {
            Call call = getCallByCallId(callId);
            boolean isVideo = (call.getIncomingVideoStatus() == Call.IncomingVideoStatus.SUPPORTED);
            return convertCall(call, isVideo);
        }
        return null;
    }

    /**
     * @return the call id of the active call (might be off hook call in mid dial state)
     */
    public int getActiveCallId() {

        int callId = getActiveCallIdWithoutOffhook();
        if (callId ==0)
            return getOffhookCallId();

        return callId;
    }

    public int getActiveCallIdWithoutOffhook() {
        for (Call call :  getCallList()) {
            if (call != null && (call.getState() == ESTABLISHED
                    || call.getState() == REMOTE_ALERTING) || call.getState() == FAILED) {
                return call.getCallId();
            }
        }
        return 0;
    }

    public int getOffhookCallId(){
        return mOffHookDialCall == null ? 0 : mOffHookDialCall.getCallId();
    }

    public boolean hasActiveCalls() {
        for (Call call :  getCallList()) {
            if (call != null && (call.getState() == ESTABLISHED || call.getState() == FAILED)) {
                return true;
            }
        }
        return mOffHookDialCall == null ? false : true;
    }


    /**
     * add a digit to the off hook call
     *
     * @param digit
     */
    public void addDigit(char digit) {
        try {
            Log.d(LOG_TAG, "adding digit " + digit + " call state is:" + (mOffHookDialCall == null ? "N/A" : mOffHookDialCall.getState().toString()));
            if (mOffHookDialCall != null && mOffHookDialCall.getState() == CallState.IDLE) {
                final int seqNum = mToneManager.play(digit);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (seqNum == mToneManager.getSequnceNumber())
                            mToneManager.stop();
                    }
                }, DIGIT_TIME);
                mOffHookDialCall.addRemoteAddressDigit(parseToDTMF(digit));
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    public ToneManager getToneManager() {
        return mToneManager;
    }

    private class EndFailedCallRunnable implements Runnable {

        private final int mCallId;

        private EndFailedCallRunnable(int id) {
            this.mCallId = id;
        }

        /**
         * Starts executing the active part of the class' code. This method is
         * called when a thread is started that has been created with a class which
         * implements {@code Runnable}.
         */
        @Override
        public void run() {
            if (mOffHookDialCall != null && mCallId == mOffHookDialCall.getCallId()) {
                mOffHookDialCall.end();
                mOffHookDialCall = null;
            }
        }
    }

    /**
     * Checks whether the specified call is CM conference and it has the maximal number of participants
     *
     * @param call
     * @return true if this is CM conference that has maximal number of participants.
     */
    private boolean maxNumOfParticipantsReached(Call call) {

        if (!isCMConferenceCall(call.getCallId())) {
            return false;
        }

        return Utils.containsConferenceString(call.getRemoteDisplayName()) && call.getRemoteDisplayName().indexOf(MAX_NUM_PARTICIPANTS_IN_CM_CONFERENCE) != -1;

    }

    /**
     * @return true if this IPO environment.
     */
    public boolean isIPO() {
        return SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_IPOFFICE);
    }

    /**
     * Restore the UI representation of the ongoing calls
     */
    public void restoreCalls() {

        // if no user is login - no sense to any existing calls
       if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().isAnonymous()) {
            return;
        }

        for (Call call :  getCallList()) {
            if (call == null) {
                continue;
            }
            UICall UiCall = convertCall(call);
            if (UiCall == null) {
                continue;
            }

            if (getOffhookCallId() == UiCall.getCallId() )
                return;

            Log.d(LOG_TAG, "Restore call " + call.getCallId() + " state is " + call.getState());
            if ((mUiObj != null) && (mUiObj.get() != null)) {
                if (call.getState() != ALERTING) {
                    mUiObj.get().onCallStarted(UiCall);
                    if (call.getState() == HELD) {
                        mUiObj.get().onCallHeld(UiCall);
                    }
                }
            }

        }
    }

    /**
     * For all active calls in the Alerting state,
     * notifies {@CallAdaptorListener}  onIncomingCallReceived
     */
    public void restoreIncomingCalls() {
        for (Call call :  getCallList()) {
            if (call == null) {
                continue;
            }
            UICall UiCall = convertCall(call);
            if (UiCall == null) {
                continue;
            }
            Log.d(LOG_TAG, "Restore call " + call.getCallId() + " state is " + call.getState());
            if ((mUiObj != null) && (mUiObj.get() != null)) {
                if (call.getState() == ALERTING) {
                    mUiObj.get().onIncomingCallReceived(UiCall);
                }
            }
        }
    }

    /**
     * We should store time of the last call in order to compare with it when doing sync.
     *
     */
    private void saveTimestampOfLastCall() {
        SharedPreferences.Editor editorTimestampLastMissedCall = mCallPreference.edit();
        editorTimestampLastMissedCall.putLong(Constants.KEY_CALL_TIMESTAMP, System.currentTimeMillis());
        editorTimestampLastMissedCall.apply();
    }

    private boolean isMediaPreservedCall(Call call) {
        return !call.isServiceAvailable();
    }


    public boolean hasActiveHeldOrInitiatingCall() {
        if(SDKManager.getInstance().getDeskPhoneServiceAdaptor() != null && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getCallService() != null) {
            CallService cs = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getCallService();
            boolean hasActiveHeldOrInitiatingCalls = false;
            final List<CallState> callStates = new ArrayList<>();
            callStates.add(CallState.FAILED);
            callStates.add(CallState.ESTABLISHED);
            callStates.add(CallState.HELD);
            callStates.add(CallState.REMOTE_ALERTING);
            for (Call call : cs.getCalls()) {
                if (!call.isRemote()) {
                    if (callStates.contains(call.getState())) {
                        hasActiveHeldOrInitiatingCalls = true;
                        break;
                    }
                }
            }
            return hasActiveHeldOrInitiatingCalls;
        }
        return false;
    }
}
