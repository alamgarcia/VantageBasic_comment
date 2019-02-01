package com.avaya.android.vantage.basic.views.interfaces;

import com.avaya.android.vantage.basic.model.UIAudioDevice;

/**
 * Audio device change interface responsible for connecting {@link com.avaya.android.vantage.basic.activities.MainActivity}
 *  and {@link com.avaya.android.vantage.basic.adaptors.UIAudioDeviceViewAdaptor}
 */
public interface IDeviceViewInterface {

    void onDeviceChanged(UIAudioDevice device, boolean active);
}
