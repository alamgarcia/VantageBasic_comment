package com.avaya.android.vantage.basic.csdk;

import android.content.Context;
import android.util.Log;

import com.avaya.android.vantage.basic.R;

/**
 * List of possible error with CSDK.
 *
 * {number of error code in application}, {number of error code in requirement}, {error case}
 * FUTURE - not yet supported
 *
 * 0. (1) Wrong SIP extension and/or password as result of changing password by administrator in SMGR.
 * 1. (2) General error case reported by CSDK.
 * 2. (3) Invalid domain reported by CSDK.
 * 3. (4) Connection error reported by CSDK.
 * 4. (5) Server error reported by CSDK.
 * 5. (6) Server error reported by CSDK.
 * 6. (7) Server error reported by CSDK.
 * 7. (8) Wrong AADS/user enterprise credentials  username and/or password when SIP login is used.
 * 7. (9) Wrong AADS/user enterprise credentials  username and/or password when UL login is used.
 * 8. (10) SIP extension (SIPUSERNAME), SIPHA1 or SIPPASSWORD are empty (not retrieved) from AADS/User Authenticated File Server.
 * 0. (11) SIP login failed as result of wrong SIP extension and/or password while UL login was used.
 * 9. (15) General error accessing AADS contacts services by simple and customized phone applications.
 */

public class ErrorManager {

    private final String LOG_TAG = this.getClass().getSimpleName();

    public static final int AUTHENTICATION_ERROR = 0;
    public static final int GENERAL_ERROR = 1;
    public static final int DOMAIN_ERROR = 2;
    public static final int CONNECTION_ERROR = 3;
    public static final int SERVER_ERROR = 4;
    public static final int CERTIFICATE_ERROR = 5;
    public static final int MAX_REGISTRATIONS_EXCEEDED_ERROR = 6;
    public static final int UL_MISSING_CREDENTIALS_ERROR = 7;
    static final int EMPTY_CREDENTIALS_ERROR = 8;
    static final int AADS_GENERAL_ERROR = 9;

    // Singleton instance of ErrorManager
    private static volatile ErrorManager instance = null;

    private boolean[] mErrorList;

    public static ErrorManager getInstance() {
        if (instance == null) {
            synchronized (ErrorManager.class) {
                if (instance == null) {
                    instance = new ErrorManager();
                }
            }
        }
        return instance;
    }

    private ErrorManager() {
        Log.d(LOG_TAG, "Error manager created");
    }

    /**
     * List of errors that are active.
     *
     * @return full error list.
     */
    public boolean[] getErrorList() {
        if (mErrorList == null) {
            mErrorList = new boolean[10];
        }
        return mErrorList;
    }

    /**
     * Add CSDK error for notification list.
     *
     * @param errorNumber Error code number (see description on top).
     */
    public void addErrorToList(int errorNumber) {
        Log.d(LOG_TAG, "add error " + errorNumber);
        getErrorList()[errorNumber] = true;
    }

    /**
     * Remove error from notification list.
     *
     * @param errorNumber Error code number (see description on top).
     */
    void removeErrorFromList(int errorNumber) {
        getErrorList()[errorNumber] = false;
    }

    /**
     * Remove all error notification in case of successful login.
     */
    public void removeAllErrors() {
        mErrorList = new boolean[10];
    }

    /**
     * @param context Activity
     * @param errorCode number
     * @return Literal description for the error code
     */
    public static String getErrorTitle(Context context, int errorCode) {

        switch (errorCode) {
            case AUTHENTICATION_ERROR:
                return context.getResources().getString(R.string.error_title_sip_extension);
            case GENERAL_ERROR:
                return context.getResources().getString(R.string.error_title_general);
            case DOMAIN_ERROR:
                return context.getResources().getString(R.string.error_title_invalid_domain);
            case CONNECTION_ERROR:
                return context.getResources().getString(R.string.error_title_voip);
            case SERVER_ERROR:
                return context.getResources().getString(R.string.error_title_phone_service);
            case CERTIFICATE_ERROR:
                return context.getResources().getString(R.string.error_title_certificate);
            case MAX_REGISTRATIONS_EXCEEDED_ERROR:
                return context.getResources().getString(R.string.error_title_device_limit);
            case UL_MISSING_CREDENTIALS_ERROR:
                return context.getResources().getString(R.string.error_title_sip_credentials);
            case EMPTY_CREDENTIALS_ERROR:
                return context.getResources().getString(R.string.error_title_sip_extension);
            case AADS_GENERAL_ERROR:
                return context.getResources().getString(R.string.error_message_service_not_available);
            default:
                return context.getResources().getString(R.string.error_unknown);
        }
    }

    /**
     * @param context Activity
     * @param errorCode number
     * @return Message that correspond to the error code
     */
    public static String getErrorMessage(Context context, int errorCode) {

        switch (errorCode) {
            case AUTHENTICATION_ERROR:
                return context.getResources().getString(R.string.error_message_sip_extension);
            case GENERAL_ERROR:
                if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_IPOFFICE))
                    return context.getResources().getString(R.string.error_message_general_ipo);
                else
                    return context.getResources().getString(R.string.error_message_general);
            case DOMAIN_ERROR:
                return context.getResources().getString(R.string.error_message_invalid_domain);
            case CONNECTION_ERROR:
               return context.getResources().getString(R.string.error_message_voip);
            case SERVER_ERROR:
                return context.getResources().getString(R.string.error_message_phone_service);
            case CERTIFICATE_ERROR:
                return context.getResources().getString(R.string.error_message_certificate);
            case MAX_REGISTRATIONS_EXCEEDED_ERROR:
                return context.getResources().getString(R.string.error_message_device_limit);
            case UL_MISSING_CREDENTIALS_ERROR:
                return context.getResources().getString(R.string.error_message_ul_credentials);
            case EMPTY_CREDENTIALS_ERROR:
                return context.getResources().getString(R.string.error_message_empty);
            case AADS_GENERAL_ERROR:
                return "";
            default:
                return context.getResources().getString(R.string.error_unknown);
        }
    }
}
