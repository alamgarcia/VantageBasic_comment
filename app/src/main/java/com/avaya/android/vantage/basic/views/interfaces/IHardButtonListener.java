package com.avaya.android.vantage.basic.views.interfaces;

import android.support.annotation.NonNull;

import com.avaya.deskphoneservices.HardButtonType;

public interface IHardButtonListener {
    /**
     * key up event received from platform via MEDIA_BUTTON intent
     * @param hardButton
     */
    void onKeyUp(@NonNull HardButtonType hardButton);
    /**
     * key down event received from platform via MEDIA_BUTTON intent
     * @param hardButton
     */
    void onKeyDown(@NonNull HardButtonType hardButton);
}
