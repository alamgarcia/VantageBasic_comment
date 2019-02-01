package com.avaya.android.vantage.basic.csdk;

import com.avaya.deskphoneservices.HandsetType;
import com.avaya.deskphoneservices.HardButtonType;

/**
 * Interface which connects and provide communication between {@link DeskPhoneServiceAdaptor} and
 * {@link com.avaya.android.vantage.basic.adaptors.UIDeskPhoneServiceAdaptor}
 */
public interface DeskPhoneServiceListener {

    void onUserRegistrationSuccessful(String displayName, String userId);

    // value of some service-impacting parameter has changed
    // withUIRefresh indicates whether some non-service-impacting parameter has also changed
    void onServiceImpactingParamChange(boolean withUIRefresh);

    // value of some non-service-impacting parameter has changed
    void onNonServiceImpactingParamChange();

    void onOffHook(HandsetType handsetType);

    void onOnHook(HandsetType handsetType);

    void setNameExtensionVisibility(int extensionNameDisplayOption);

    void finishAndLock();

    void onKeyUp(HardButtonType hardButtonType);

    void onKeyDown(HardButtonType hardButtonType);
}
