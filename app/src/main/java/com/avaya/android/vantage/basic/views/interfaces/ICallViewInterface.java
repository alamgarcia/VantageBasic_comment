package com.avaya.android.vantage.basic.views.interfaces;

import com.avaya.android.vantage.basic.model.UICall;
import com.avaya.clientservices.call.Call;

/**
 * Interface for providing communication between {@link com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler}
 * and {@link com.avaya.android.vantage.basic.adaptors.UICallViewAdaptor}
 */
public interface ICallViewInterface {

    void onCallStarted(UICall call);

    void onCallEstablished(UICall call);

    void setCallStateChanged(UICall call);

    void onCallRemoteAlert();

    void onCallRemoved(UICall call);

    void onCallFailed(UICall uiCall);

    void onCallEscalatedToVideoSuccessful(UICall uiCall);

    void onCallServiceUnavailable(UICall uiCall);

    void onCallEscalatedToVideoFailed(UICall uiCall);

    void onCallDeescalatedToAudio(UICall uiCall);

    void onCallDeescalatedToAudioFailed(UICall uiCall);

    void onCallRemoteAddressChanged(UICall uiCall, String newDisplayName);

    void onCallTransferFailed();

    void onIncomingCallReceived(UICall call);

    void onCallHoldUnholdSuccessful(int callId, boolean shouldHold);

    void onCallHoldFailed(int callId);

    void onCallConferenceStatusChanged(Call call, boolean isConference);

    void onCallCreated(UICall uiCall);
}
