package com.avaya.android.vantage.basic.csdk;

import android.util.Log;

import com.avaya.clientservices.voicemessaging.VoiceMessagingService;
import com.avaya.clientservices.voicemessaging.VoiceMessagingServiceListener;
import com.avaya.clientservices.voicemessaging.VoiceMessagingStatusParameters;

import java.lang.ref.WeakReference;

import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_IPOFFICE;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PSTN_VM_NUM;

public class VoiceMessageAdaptor implements VoiceMessagingServiceListener {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private WeakReference<VoiceMessageAdaptorListener> mUiObj;
    private String mVoicemailNumber;
    private boolean mState;

    /**
     * Register listener for {@link VoiceMessageAdaptor}
     *
     * @param uiObj {@link VoiceMessageAdaptorListener}
     */
    public void registerListener(VoiceMessageAdaptorListener uiObj) {
        mUiObj = new WeakReference<VoiceMessageAdaptorListener>(uiObj);
        if (mUiObj.get() == null) {
            Log.d(LOG_TAG, "reference to VoiceMessageAdaptor is null");
        } else {
            Log.d(LOG_TAG, "reference to VoiceMessageAdaptor is NOT null");
        }
    }

    /**
     * Return String representation of voicemail number
     *
     * @return String voicemail number
     */
    public String getVoicemailNumber() {

        if ((mVoicemailNumber==null) &&
                SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ENABLE_IPOFFICE) &&
                (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParam(PSTN_VM_NUM) != null)) {
            mVoicemailNumber = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParam(PSTN_VM_NUM);
        }
        return mVoicemailNumber;
    }

    /**
     * Return boolean representing voicemail state
     *
     * @return boolean
     */
    public boolean getVoiceState() {
        return mState;
    }

    /**
     * Check if voice messaging service is available
     *
     * @param voiceMessagingService {@link VoiceMessagingService}
     */
    @Override
    public void onVoiceMessagingServiceAvailable(VoiceMessagingService voiceMessagingService) {
        Log.d(LOG_TAG, "onVoiceMessagingServiceAvailable voiceNumber = " + voiceMessagingService.getVoicemailNumber());
    }

    @Override
    public void onVoiceMessagingServiceUnavailable(VoiceMessagingService voiceMessagingService) {
        Log.d(LOG_TAG, "onVoiceMessagingServiceUnavailable");
    }

    @Override
    public void onMessageWaitingStatusChanged(VoiceMessagingService voiceMessagingService, VoiceMessagingStatusParameters voiceMessagingStatus) {

        Log.d(LOG_TAG, "onMessageWaitingStatusChanged: new status is " + voiceMessagingStatus.getMessageWaiting());
        mState = voiceMessagingStatus.getMessageWaiting();
        if (mUiObj != null && mUiObj.get() != null) {
            mUiObj.get().onMessageWaitingStatusChanged(voiceMessagingStatus.getMessageWaiting());
        }
    }

    @Override
    public void onVoicemailNumberChanged(VoiceMessagingService voiceMessagingService, String voicemailNumber) {
        Log.d(LOG_TAG, "onVoicemailNumberChanged: new number is " + voicemailNumber);
        this.mVoicemailNumber = voicemailNumber;
        if (mUiObj != null && mUiObj.get() != null) {
            mUiObj.get().onVoicemailNumberChanged(voicemailNumber);
        }
    }
}
