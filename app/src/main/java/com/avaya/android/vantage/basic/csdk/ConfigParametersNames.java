package com.avaya.android.vantage.basic.csdk;

import java.util.ArrayList;

/**
 * Enumerated type for all of the possible configuration parameters.
 */
public enum ConfigParametersNames {
    //                         name of parameter                default value                   has impact on service        has impact on UI
    SIP_CONTROLLER_LIST   ("SIP_CONTROLLER_LIST",                   "",                            true,                            false),
    SIPDOMAIN             ("SIPDOMAIN",                             "",                            true,                            false),
    ENABLE_REDIAL         ("ENABLE_REDIAL",                         "1",                           false,                           true),
    SIPUSERNAME           ("SIPUSERNAME",                           "anonymous",                   true,                            false),
    SIPPASSWORD           ("SIPPASSWORD",                           "",                            true,                            false),
    SIPHA1                ("SIPHA1",                                "",                            true,                            false),
    SSOUSERID             ("SSOUSERID",                             "",                            true,                            false),
    SSOPASSWORD           ("SSOPASSWORD",                           "",                            true,                            false),
    ACSENABLED            ("ACSENABLED",                            "0",                           true,                            false),
    ACSSRVR               ("ACSSRVR",                               "",                            true,                            false),
    ACSPORT               ("ACSPORT",                               "",                            true,                            false),
    ACSSECURE             ("ACSSECURE",                             "0",                           true,                            false),
    MEDIAENCRYPTION       ("MEDIAENCRYPTION",                       "",                            true,                            false),
    ENCRYPT_SRTCP         ("ENCRYPT_SRTCP",                         "0",                           true,                            false),
    ENABLE_VIDEO          ("ENABLE_VIDEO",                          "1",                           true,                            true),
    HOLDSTAT              ("HOLDSTAT",                              "1",                           false,                           true),
    CCBTNSTAT             ("CCBTNSTAT",                             "1",                           false,                           true),
    MUTESTAT              ("MUTESTAT",                              "1",                           false,                           true),
    ENABLE_CALL_LOG       ("ENABLE_CALL_LOG",                       "1",                           false,                           true),
    TLSSRVRID             ("TLSSRVRID",                             "1",                           true,                            false),
    ENABLE_FAVORITES      ("ENABLE_FAVORITES",                      "1",                           false,                           true),
    ENABLE_CONTACTS       ("ENABLE_CONTACTS",                       "1",                           false,                           true),
    ENABLE_MODIFY_CONTACTS("ENABLE_MODIFY_CONTACTS",                "1",                           false,                           true),
    XFERSTAT              ("XFERSTAT",                              "1",                           false,                           true),
    BRAND_URL             ("BRANDING_FILE",                         "",                            false,                           true),
    CONFSTAT              ("CONFSTAT",                              "1",                           false,                           true),
    PROVIDE_OPTIONS_SCREEN("PROVIDE_OPTIONS_SCREEN",                "1",                           false,                           true),
    ENHDIALSTAT           ("ENHDIALSTAT",                           "0",                           true,                           false),
    EC500ENABLED          ("EC500ENABLED",                          "0",                           false,                          false),
    DSCPAUD               ("DSCPAUD",                               null,                          false,                          false),
    DSCPVID               ("DSCPVID",                               null,                          false,                          false),
    DSCPSIG               ("DSCPSIG",                               null,                          false,                          false),
    PHNOL                 ("PHNOL",                                 null,                          true,                           false),
    PHNCC                 ("PHNCC",                                 null,                          true,                           false),
    PHNIC                 ("PHNIC",                                 null,                          true,                           false),
    PHNDPLENGTH           ("PHNDPLENGTH",                           null,                          true,                           false),
    PHNLDLENGTH           ("PHNLDLENGTH",                           null,                          true,                           false),
    PHNLD                 ("PHNLD",                                 null,                          true,                           false),
    SIP_USER_DISPLAY_NAME ("SIP_USER_DISPLAY_NAME",                 null,                          false,                          false),
    EXTENSION_NAME_DISPLAY_OPTIONS  ("EXTENSION_NAME_DISPLAY_OPTIONS",                 "0",        false,                          true),
    RTP_PORT_LOW          ("RTP_PORT_LOW",                          null,                          true,                           false),
    RTP_PORT_RANGE        ("RTP_PORT_RANGE",                        null,                          true,                           false),
    VIDEO_MAX_BANDWIDTH_ANY_NETWORK ("VIDEO_MAX_BANDWIDTH_ANY_NETWORK", null,                      true,                           false),
    ADMIN_CHOICE_RINGTONE ("ADMIN_CHOICE_RINGTONE",                 null,                          false,                          false),
    NAME_SORT_ORDER       ("NAME_SORT_ORDER",                       null,                          false,                          false),
    NAME_DISPLAY_ORDER    ("NAME_DISPLAY_ORDER",                    null,                          false,                          false),
    ENABLE_OPUS           ("ENABLE_OPUS",                           "0",                           true,                           false),
    LOG_VERBOSITY         ("LOG_VERBOSITY",                         "0",                           false,                          false),
    LOGLOCAL              ("LOGLOCAL",                              "1",                           false,                          true),
    APPLY_DIALING_RULES_TO_PLUS_NUMBERS ("APPLY_DIALINGRULES_TO_PLUS_NUMBERS",  "0",               true,                           false),
    AUTO_APPLY_ARS_TO_SHORT_NUMBERS("AUTOAPPLY_ARS_TO_SHORTNUMBERS",            "1",               true,                           false),
    PHN_REMOVE_AREA_CODE  ("PHNREMOVEAREACODE",                     "0",                           true,                           false),
    DIALPLANLOCALCALLPREFIX("DIALPLANLOCALCALLPREFIX",              "0",                           true,                           false),
    SP_AC                 ("SP_AC",                                  null,                         true,                           false),
    DIALPLANAREACODE      ("DIALPLANAREACODE",                       null,                         true,                           false),
    PHN_PBX_MAIN_PREFIX   ("PHNPBXMAINPREFIX",                       null,                         true,                           false),
    DIAL_PLAN_PBX_PREFIX  ("DIALPLANPBXPREFIX",                     null,                         true,                           false),
    DIALPLANNATIONALPHONENUMLENGTHLIST("DIALPLANNATIONALPHONENUMLENGTHLIST", null,                 true,                           false),
    DIALPLANEXTENSIONLENGTHLIST("DIALPLANEXTENSIONLENGTHLIST",       null,                         true,                           false),
    PIN_APP               ("PIN_APP",                                null,                         false,                          false),
    ANALYTICSENABLED      ("ANALYTICSENABLED",                       "1",                          false,                          true),
    ENABLE_PPM_CALL_JOURNALING  ("ENABLE_PPM_CALL_JOURNALING",       "1",                          true,                           true),
    DIALPLAN              ("DIALPLAN",                               "",                           true,                           false),
    INTER_DIGIT_TIMEOUT  ("INTER_DIGIT_TIMEOUT",                     "5",                          true,                           false),
    NO_DIGIT_TIMEOUT     ("NO_DIGIT_TIMEOUT",                        "20",                         true,                           false),
    ENABLE_IPOFFICE      ("ENABLE_IPOFFICE",                         "0",                          true,                           true),
    PSTN_VM_NUM           ("PSTN_VM_NUM",                            null,                         false,                          false),
    SUBSCRIBE_LIST_NON_AVAYA ("SUBSCRIBE_LIST_NON_AVAYA",            "0",                          true,                           true),
    SIMULTANEOUS_REGISTRATIONS ("SIMULTANEOUS_REGISTRATIONS",        "3",                          true,                           true),
    REGISTERWAIT ("REGISTERWAIT",                                    "300",                        true,                           true),
    CONFERENCE_FACTORY_URI ("CONFERENCE_FACTORY_URI",                null,                         true,                           false),
    HUNDRED_REL_SUPPORT  ("100REL_SUPPORT ",                         "1",                          true,                           false),
    POUND_KEY_AS_CALL_TRIGGER ("POUND_KEY_AS_CALL_TRIGGER",          "1",                          true,                           false);

    private final String mName;
    private final String mDefaultValue;
    private final boolean mIsServiceImpacting;
    private final boolean mIsUIImpacting;

    /**
     * Public constructor for {@link ConfigParametersNames}
     * @param name
     * @param defaultValue
     * @param isServiceImpacting
     * @param isUIImpacting
     */
    ConfigParametersNames(String name, String defaultValue, boolean isServiceImpacting, boolean isUIImpacting) {
        mName = name;
        mDefaultValue = defaultValue;
        mIsServiceImpacting = isServiceImpacting;
        mIsUIImpacting = isUIImpacting;
    }

    private static ArrayList<String> mServiceImpactingParamArray = null;
    private static ArrayList<String> mUIImpactingParamArray = null;
    private static ArrayList<String> mClientImpactingParamArray = null;

    /**
     * Return data if configuration change is impacting active service
     *
     * @param param to be checked for
     * @return boolean
     */
    static boolean isServiceImpacting(String param) {

        if (mServiceImpactingParamArray == null) {
            buildServiceImpactingArray();
        }
        return mServiceImpactingParamArray.contains(param);
    }

    /**
     * Return data if configuration change is impacting UI
     *
     * @param param to be checked for
     * @return boolean
     */
    static boolean isUIImpacting(String param) {

        if (mUIImpactingParamArray == null) {
            buildUIImpactingArray();
        }
        return mUIImpactingParamArray.contains(param);
    }

    /**
     * Prepare array of service impacting configuration parameters
     */
    static void buildServiceImpactingArray() {
        mServiceImpactingParamArray = new ArrayList<String>();
        for (ConfigParametersNames param : ConfigParametersNames.values()) {
            if (param.mIsServiceImpacting) {
                mServiceImpactingParamArray.add(param.mName);
            }
        }
    }

    /**
     * Prepare array of UI impacting configuration parameter
     */
    static void buildUIImpactingArray() {
        mUIImpactingParamArray = new ArrayList<String>();
        for (ConfigParametersNames param : ConfigParametersNames.values()) {
            if (param.mIsUIImpacting) {
                mUIImpactingParamArray.add(param.mName);
            }
        }
    }

    /**
     * Return current configuration name
     *
     * @return String name
     */
    public String getName() {
        return mName;
    }

    /**
     * Return default value for configuration
     *
     * @return String default value
     */
    public String getDefaultValue() {
        return mDefaultValue;
    }
}
