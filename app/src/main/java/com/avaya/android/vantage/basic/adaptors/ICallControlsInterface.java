package com.avaya.android.vantage.basic.adaptors;

import com.avaya.android.vantage.basic.model.UICall;

/**
 * Interface responsible for providing communication from {@link UICallViewAdaptor}
 * and {@link com.avaya.android.vantage.basic.activities.MainActivity}
 */
public interface ICallControlsInterface {

    void onVideoMuted(UICall uiCall, boolean muting);

    void onCallMissed();
}
