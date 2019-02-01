package com.avaya.android.vantage.basic.fragments;

/**
 * {@link OnActiveCallInteractionListener} is interface responsible for communication of {@link ActiveCallFragment}
 * and {@link com.avaya.android.vantage.basic.activities.MainActivity}
 */

public interface OnActiveCallInteractionListener {

    /**
     * Starting contact picker for call transfer process
     *
     * @param CallId of active call to which we want to transfer
     *               picked contact
     */
    void startContactPickerForCallTransfer(int CallId);

    /**
     * Starting contact picker for call merge process
     *
     * @param CallId of active call which we want to merge to conference
     *               with picked contact
     */
    void startContactPickerForConference(int CallId);

    /**
     * Canceling contact picker we opened for {@link #startContactPickerForCallTransfer(int)}
     * or {@link #startContactPickerForConference(int)}
     */
    void cancelContactPicker();

    /**
     * Preparing views when call is started. Based on parameter isVideo it will prepare audio only
     * or video call
     *
     * @param isVideo is call audio only or video call
     */
    void onCallStarted(boolean isVideo);

    /**
     * Process of preparation of activity for post call
     */
    void onCallEnded();

    /**
     * Setting for audio selection on or off based on parameters provided
     * @param isOn boolean based on which selection
     */
    void setOffhookButtosChecked(boolean isOn);

    /**
     * Setting parameter which marks if feature or any other menu which should
     * shade background
     * @param isOpen boolean
     */
    void setFeatureMenuOpen(boolean isOpen);
}
