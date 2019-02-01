package com.avaya.android.vantage.basic;

import android.support.v4.util.ArraySet;
import android.view.KeyEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Static class which contain all constants for our application
 */
public class Constants {
    public static final String CALL_ID = "CallId";
    public static final String IS_VIDEO = "IsVideo";
    public static final String IS_CONFERENCE = "isConference";
    public static final String ACTION_TRANSFER = "ACTION_TRANSFER";
    public static final String TARGET = "target extension number";

    public static final String INCOMING_CALL_ACCEPT = "INCOMING_CALL_ACCEPT";

    // intent constants
    public static final String LOCAL_CONFIG_CHANGE = "com.avaya.android.vantage.basic.LOCAL_CONFIG_CHANGE";
    public static final String SERVICE_STATE_CHANGE = "com.avaya.endpoint.SERVICE_STATE_CHANGE";
    public static final String SNACKBAR_SHOW = "com.avaya.android.vantage.basic.SNACKBAR_SHOW";
    public static final String REFRESH_HISTORY_ICON = "com.avaya.endpoint.REFRESH_HISTORY_ICON";
    public static final String CONFIG_CHANGED = "com.avaya.endpoint.action.CONFIG_CHANGED";
    public static final String LOGIN_SIGNAL = "com.avaya.endpoint.action.LOGIN_SIGNAL";
    public static final String KEY_SERVICE_TYPE_EXTRA = "serviceType";
    public static final String KEY_SERVICE_STATUS_EXTRA = "status";
    public static final String KEY_SERVICE_REASON_EXTRA = "reason";
    public static final String KEY_SERVICE_RETRY_EXTRA = "retry";
    public static final String SIP_SERVICE_TYPE = "SIP";
    public static final String SUCCESS_STATUS = "SUCCESS";
    public static final String FAIL_STATUS = "FAIL";
    public static final String CONFIG_RECEIVER = "ConfigReceiver";
    public static final String MIDNIGHT_GOOGLE_ANALYTICS = "com.avaya.endpoint.MIDNIGHT_GOOGLE_ANALYTICS";

    public static final String CONFIG_CHANGE_STATUS = "isSuccessful";
    public static final String SNACKBAR_MESSAGE = "message";
    public static final String SNACKBAR_LENGTH = "length";

    public static final String ALL = "All";
    public static final String IPO_CONTACT_TYPE = "com.avaya.endpoint.account.ipo";
    public static final int ALL_CONTACTS = 0;
    public static final int LOCAL_ONLY = 1;
    public static final int ENTERPRISE_ONLY = 2;
    public static final int SEARCHED_CONTACTS = 3;
    public static final int IPO_ONLY = 4;

    public static final String CONTACT_DATA = "contactIdentifier";
    public static final String CONTACT_EDITING = "isNewContact";

    public static final int TRANSFER_REQUEST_CODE = 2000 + 'T';
    public static final int CONFERENCE_REQUEST_CODE = 2000 + 'C';
    public static final int SYNC_CALLS_LOADER = 0;
    public static final int LOCAL_CONTACTS_LOADER = 1;
    public static final int LOCAL_NAME_LOADER = 3;
    public static final int LOCAL_ADDRESS_LOADER = 4;
    public static final int DIRECTORY_LOADER = 5;
    public static final int DIRECTORY_ACCOUNT_NAME_LOADER = 6;
    public static final int DIRECTORY_CONTACT_SEARCH_LOADER_BASE = 1000;
    public static final int DIRECTORY_CONTACT_PHONE_SEARCH_LOADER_BASE = 6000;

    //Call sizes
    public static final int CALL_SIZE_0 = 0;
    public static final int CALL_SIZE_1 = 1;
    public static final int CALL_SIZE_2 = 2;

    //Time split
    public static final int MILISECONDS = 1000;
    public static final int SECONDS = 60;
    public static final int MINUTES = 60;

    //name display and name sort preference
    public static final int FIRST_NAME_FIRST = 0;
    public static final int LAST_NAME_FIRST = 1;
    public static final String NAME_DISPLAY_PREFERENCE = "nameDisplayPref";
    public static final String NAME_SORT_PREFERENCE = "nameSortPref";
    public static final String USER_PREFERENCE = "userPref";
    public static final String USER_AGENT_INSTANCE_ID = "prefs_user_agent_instance_id";
    public static final String REFRESH_CONTACTS = "refreshContacts";
    public static final String REFRESH_FAVORITES = "refreshFavorites";
    public static final String REFRESH_RECENTS = "refreshRecents";
    public static final String ADMIN_RINGTONE_PREFERENCES = "adminRingtonePref";
    public static final String CUSTOM_RINGTONE_PREFERENCES = "customRingtonePref";

    public static final String EXIT_PIN = "exit_pin";
    public static final String NEW_CONTACT_PREF = "newContactPref";

    public static final String ANONYMOUS_USER = "anonymous";

    public static final long LAYOUT_DISAPPEAR_TIME = 20000;
    public static final long CALL_FEATURE_HINT_DISMISS_TIME = 10000;
    public static final int END_FAILED_CALL_DELAY_MILLIS = 30000;

    public static final int MILISECONDS_IN_SECOND = 1000;
    // the following string defines maximal number of participants in CM conference without the Moderator
    // so the actual number is 6
    public static final String MAX_NUM_PARTICIPANTS_IN_CM_CONFERENCE = "5";
    public static final String AUDIO_PREF_KEY = "audioDevice";

    //eula
    public static final String KEY_EULA_ACCEPTED = "eula_accepted";
    public static final String EULA_PREFS_NAME = "eula_preferences";

    //Call history sync
    public static final String CONNECTION_PREFS = "connectionprefferences";
    public static final String SYNC_HISTORY = "sync_history";

    public static final String AVAYA_PRODUCT_MODEL = "ro.avaya.product.model";
    public static final String AVAYA_ETHADDR = "ro.avaya.ethaddr";
    public static final String UUID_PREFIX = "0000-0000-1000-8000-";

    public static final String GO_TO_FRAGMENT = "go_to_fragment";
    public static final String GO_TO_FRAGMENT_MISSED_CALLS = "go_to_fragment_missed_calls";

    public static final String SYNC_CONTACTS = "sync_contacts";
    public static final String BLUETOOTH_CONNECTED = "bluetooth_connected";
    public static final String BLUETOOTH_BOUNDED = "bluetooth_bounded";

    //IPO
    public static final int IPO_CONTACT_NAME_LIMIT = 31;

    public static final String KEY_CALL_TIMESTAMP = "call_timestamp";
    public static final String EXTRA_UNSEEN_CALLS = "number_of_missed_calls";
    public static final String KEY_CHECK_NEW_CALL = "check_new_call";
    public static final String KEY_UNSEEN_MISSED_CALLS = "unseen_missed_calls";
    public static final String CALL_PREFS = "CALL_PREFS";
    static ArraySet as = new ArraySet(Arrays.asList(
            KeyEvent.KEYCODE_0,
            KeyEvent.KEYCODE_1,
            KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_7,
            KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9,
            KeyEvent.KEYCODE_POUND,
            KeyEvent.KEYCODE_STAR,
            KeyEvent.KEYCODE_NUMPAD_0,
            KeyEvent.KEYCODE_NUMPAD_1,
            KeyEvent.KEYCODE_NUMPAD_2,
            KeyEvent.KEYCODE_NUMPAD_3,
            KeyEvent.KEYCODE_NUMPAD_4,
            KeyEvent.KEYCODE_NUMPAD_5,
            KeyEvent.KEYCODE_NUMPAD_6,
            KeyEvent.KEYCODE_NUMPAD_7,
            KeyEvent.KEYCODE_NUMPAD_8,
            KeyEvent.KEYCODE_NUMPAD_9,
            KeyEvent.KEYCODE_NUMPAD_MULTIPLY
    ));
    public static final Set<Integer> DigitKeys = Collections.unmodifiableSet(as);

}
