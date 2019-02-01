package com.avaya.android.vantage.basic.csdk;

import android.content.Context;

import com.avaya.android.vantage.basic.model.UICall;
import com.avaya.clientservices.call.Call;

/**
 * Interface connecting and providing communication between {@link CallAdaptor}
 * and {@link com.avaya.android.vantage.basic.adaptors.UICallViewAdaptor}
 */

public interface CallAdaptorListener {

    void onCallRemoteAlerting();

    void onCallEstablished(UICall call);

    void onCallHeld(UICall call);

    void onCallUnheld(UICall call);

    void onCallRemoved(UICall call);

    void onCallTransferSuccessful(final int callId, final int callIdToReplace);

    void onCallTransferSuccessful(final int callId, final String remoteAddress);

    void onCallTransferFailed();

    void onAddParticipantSuccessful(int callId, int callId2BeAdded);

    void onAddParticipantFailed(Context context, boolean maxParticipantsReached);

    void onIncomingCallReceived(UICall call);

    void onCallStarted(UICall uiCall);

    void onActiveCallChanged(UICall uiCall);

    void transferCall(int callId, String target);

    void conferenceCall(int callId, String target);

    void onCallFailed(UICall uiCall);

    void onCallEscalatedToVideoSuccessful(UICall uiCall);

    void onCallEscalatedToVideoFailed(UICall uiCall);

    void onCallServiceUnavailable(UICall uiCall);

    void onCallDeescalatedToAudio(UICall uiCall);

    void onCallDeescalatedToAudioFailed(UICall uiCall);

    void onCallRemoteAddressChanged(UICall call, String newDisplayName);

    void onCallDigitCollectionPlayDialTone(UICall call);

    int onCallDigitCollectionCompleted(UICall uiCall);

    boolean isActive();

    void onCallDenied(UICall uiCall);

    void onVideoMuted(UICall uiCall, boolean muting);

    void onCallHoldUnholdSuccessful(int callId, boolean shouldHold);

    void onCallHoldFailed(int callId);

    void onCallConferenceStatusChanged(Call call, boolean isConference);

    void onCallCreated(UICall uiCall);
}
