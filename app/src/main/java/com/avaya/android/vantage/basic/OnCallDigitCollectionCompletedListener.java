package com.avaya.android.vantage.basic;

import com.avaya.android.vantage.basic.model.UICall;

/**
 * Interface responsible for connecting and providing communication of {@link com.avaya.android.vantage.basic.fragments.DialerFragment}
 * and {@link com.avaya.android.vantage.basic.adaptors.UICallViewAdaptor}
 */
public interface OnCallDigitCollectionCompletedListener {
    void onCallDigitCollectionCompleted(UICall call);
}
