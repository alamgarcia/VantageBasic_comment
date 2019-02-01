package com.avaya.android.vantage.basic.adaptors;

import android.util.Log;

import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.csdk.VoiceMessageAdaptorListener;

/**
 * Adaptor extending {@link VoiceMessageAdaptorListener} used in {@link com.avaya.android.vantage.basic.views.adapters.SectionsPagerAdapter}
 */

public class UIVoiceMessageAdaptor implements VoiceMessageAdaptorListener {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private boolean message;
    private VoiceMessageAdaptorListener mViewInterface;

    /**
     * Public {@link UIVoiceMessageAdaptor} constructor
     * @param viewInterface {@link VoiceMessageAdaptorListener} to be set as {@link #mViewInterface}
     */
    public UIVoiceMessageAdaptor(VoiceMessageAdaptorListener viewInterface) {
        mViewInterface = viewInterface;
    }

    /**
     * Setting view interface {@link VoiceMessageAdaptorListener}
     * @param viewInterface set {@link #mViewInterface}
     */
    public void setViewInterface(VoiceMessageAdaptorListener viewInterface) {
        mViewInterface = viewInterface;
    }

    /**
     * Processing message waiting status change. {@link VoiceMessageAdaptorListener} is informed about
     * message waiting status change.
     * @param voiceMsgsAreWaiting boolean
     */
    @Override
    public void onMessageWaitingStatusChanged(boolean voiceMsgsAreWaiting) {
        Log.d(LOG_TAG, "onMessageWaitingStatusChanged: Current voice number is " + SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber());
        message = voiceMsgsAreWaiting;
        Log.e(LOG_TAG, "boolean "+voiceMsgsAreWaiting + "mViewInterface=" + mViewInterface);
        if (mViewInterface != null) {
            mViewInterface.onMessageWaitingStatusChanged(voiceMsgsAreWaiting);
        }
    }

    /**
     * Processing voice mail number change. {@link VoiceMessageAdaptorListener} is informed about
     * voice mail number
     * @param voicemailNumber voicemail number.
     */
    @Override
    public void onVoicemailNumberChanged(String voicemailNumber) {
        Log.d(LOG_TAG, "onVoicemailNumberChanged");
        if (mViewInterface != null) {
            mViewInterface.onVoicemailNumberChanged(voicemailNumber);
        }
    }

    /**
     * Returning state of message
     * @return boolean
     */
    public boolean voiceMsgsState() {
        return message;
    }
}
