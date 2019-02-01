package com.avaya.android.vantage.basic.model;

import java.util.Calendar;

/**
 * {@link UICall} skeleton class with encapsulating methods
 */

public class UICall {

    final private int mCallId;
    private UICallState mCallState;
    final private String mRemoteDisplayName, mRemoteNumber;
    private final boolean mIsVideo;
    private final boolean mIsEmergency;
    private long mEstablishedTimeMillis, mHeldTimeMillis;

    public UICall(
            int callId,
            UICallState state,
            String remoteDisplayName,
            String remoteNumber,
            boolean isVideo,
            boolean isEmergency,
            long establishedTimeMillis,
            long heldTimeMillis) {

        mCallId = callId;
        mCallState = state;
        mRemoteDisplayName = remoteDisplayName;
        mRemoteNumber = remoteNumber;
        mIsVideo = isVideo;
        mIsEmergency = isEmergency;
        mEstablishedTimeMillis = establishedTimeMillis;
        mHeldTimeMillis = heldTimeMillis;
    }

    public boolean isVideo() {
        return mIsVideo;
    }

    public UICallState getState() {
        return mCallState;
    }

    public int getCallId() {
        return mCallId;
    }

    public String getRemoteDisplayName() {
        return mRemoteDisplayName;
    }

    public String getRemoteNumber() {
        return mRemoteNumber;
    }

    public boolean isEmergency() {
        return mIsEmergency;
    }

    public long getStateStartTime() {
        if (mCallState == UICallState.ESTABLISHED)
            return mEstablishedTimeMillis;
        else if (mCallState == UICallState.HELD)
            return mHeldTimeMillis;
        else if(mEstablishedTimeMillis>0 && (mEstablishedTimeMillis < Calendar.getInstance().getTimeInMillis())) {
            // sometimes established call moves into failed state if it loses connection with the SIP Proxy. The media continues and the timer shall show
            // the time from the moment the call was established
            return mEstablishedTimeMillis;
        }
        return Calendar.getInstance().getTimeInMillis();
    }

    public boolean isMissedCall() {
        return mEstablishedTimeMillis == 0;
    }
}
