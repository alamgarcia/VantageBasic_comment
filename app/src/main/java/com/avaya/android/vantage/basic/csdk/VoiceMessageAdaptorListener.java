package com.avaya.android.vantage.basic.csdk;

/**
 * Interface used for communication between {@link com.avaya.android.vantage.basic.fragments.DialerFragment}
 * , {@link com.avaya.android.vantage.basic.adaptors.UIVoiceMessageAdaptor} and {@link VoiceMessageAdaptor}
 */

public interface VoiceMessageAdaptorListener {

    void onMessageWaitingStatusChanged(boolean voiceMsgsAreWaiting);

    void onVoicemailNumberChanged(String voicemailNumber);
}
