package com.avaya.android.vantage.basic.views.interfaces;

/**
 * SIP Login listener responsible for connecting {@link com.avaya.android.vantage.basic.adaptors.UIDeskPhoneServiceAdaptor}
 * and {@link com.avaya.android.vantage.basic.activities.MainActivity}
 */

public interface ILoginListener {
    void onSuccessfulLogin(String name, String extension);
}
