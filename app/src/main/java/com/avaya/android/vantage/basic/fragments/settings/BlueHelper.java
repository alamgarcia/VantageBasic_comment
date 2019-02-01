package com.avaya.android.vantage.basic.fragments.settings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.text.format.DateUtils;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.VantageDBHelper;
import com.avaya.android.vantage.basic.services.BluetoothStateService;


import static com.avaya.android.vantage.basic.fragments.settings.BlueHelper.BluetoothSharingType.CONTACT;

public class BlueHelper {

    private static final String TAG = BlueHelper.class.getSimpleName();
    /* arbitrary approximate time of bluetooth synchronization mechanism duration */
    public static final int CONTACT_SYNC_DURATION = (int)(8 * DateUtils.SECOND_IN_MILLIS);


    /**
     * Exposed platform API BEGIN
     * Constants below are used for Bluetooth switch access via exposed platform API
     */

    /* from class com.avaya.endpoint.pbap.service.PbapConstants */
    static final String INTENT_MODIFY_PBAP_SETTINGS = "avaya.intent.action.MODIFY_PBAP_SETTINGS";
    static final String EXTRA_KEY_CONTACTS = "CONTACT";
    static final String EXTRA_KEY_CALL_LOGS = "CALLHISTORY";
    static final String EXTRA_VALUE_ENABLED = "ENABLED";
    static final String EXTRA_VALUE_DISABLED = "DISABLED";

    /* from class com.android.settings.ConfigParametersNames */
    private static final String ENABLE_BT_CONTACTS_SYNC = "EnableBtContactsSync";
    private static final String ENABLE_BT_CALLLOG_SYNC = "EnableBtCalllogSync";

    /* related to {@link BlueHelper#ENABLE_BT_CONTACTS_SYNC & BlueHelper#ENABLE_BT_CALLLOG_SYNC} */
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED";

    /* Represents ON state for either bluetooth: Contact Sharing or Call History Sharing */
    private static String SHARING_ON = "1";
    private static String SHARING_OFF = "0";

    /**
     * Exposed platform API END
     */


    private SharedPreferences mUserPreference;
    private SharedPreferences mConnectionPref;


    private BlueHelper() {
    }

    private static BlueHelper instance;

    /**
     * Creates new instance and initializes SharedPreferences.
     * Application context is taken from the {@link ElanApplication#getContext()}
     *
     * @return instance of the {@link BlueHelper}
     */
    public static synchronized BlueHelper instance() {
        if (instance == null) {
            instance = new BlueHelper();
            instance.mUserPreference = getApplicationContext()
                    .getSharedPreferences(Constants.USER_PREFERENCE, Context.MODE_PRIVATE);
            instance.mConnectionPref = getApplicationContext()
                    .getSharedPreferences(Constants.CONNECTION_PREFS, Context.MODE_PRIVATE);
        }
        return instance;
    }

    /**
     * Retrieves Application Context form {@link ElanApplication#getContext()}
     *
     * @return Context of the application
     */
    public static Context getApplicationContext() {
        return ElanApplication.getContext();
    }

    /**
     * Turn on/off Bluetooth Contact Sharing or Call History Sharing.<br>
     * State of the corresponding Bluetooth switch is changed by sending Intent via the<br>
     * exposed platform API see {@link ContactSync#broadcast()} and {@Link ContactSync#createIntent}<br>
     * also in platform com.avaya.endpoint.pbap.service.PbapModifySettingsHelper<br>
     * This is done simply by passing the desired {@link ContactSync} one of:<br>
     * - {@link ContactSync#ContactSharingOn}<br>
     * - {@link ContactSync#ContactSharingOff}<br>
     * - {@link ContactSync#CallHistorySharingOn}<br>
     * - {@link ContactSync#CallHistorySharingOff}<br>
     *
     * @param sharingState
     */
    public void setContactSharing(ContactSync sharingState) {
        sharingState.broadcast();
    }

    /**
     * Method resolves whether to check for Contact Sharing or Call History Sharing<br>
     * by using passed {@link BluetoothSharingType}
     *
     * @param sharingType one of:<br>
     *                    - {@link BluetoothSharingType#CONTACT} or<br>
     *                    - {@link BluetoothSharingType#CALL_HISTORY}
     * @return true if Sharing is one for the given type, false otherwise
     */
    public boolean isSharingEnabled(BluetoothSharingType sharingType) {
        return sharingType == CONTACT ? isContactSharingEnabled() : isCallHistorySharingEnabled();
    }

    /**
     * Check if Contact Sharing is turned on.
     *
     * @return true if Contact Sharing is on, false otherwise
     */
    public boolean isContactSharingEnabled() {
        return SHARING_ON.equals(VantageDBHelper.getParameter(getApplicationContext()
                .getContentResolver(), ENABLE_BT_CONTACTS_SYNC));
    }

    /**
     * Checks if Call History Sharing is turned on.
     *
     * @return true if Call History Sharing is on, false otherwise
     */
    public boolean isCallHistorySharingEnabled() {
        return SHARING_ON.equals(VantageDBHelper.getParameter(getApplicationContext()
                .getContentResolver(), ENABLE_BT_CALLLOG_SYNC));
    }

    /**
     * Retrieves the value of the Bluetooth connection state, from SharedPreferences
     *
     * @return true if SharedPreferences has persisted true, false otherwise
     */
    private boolean isBluetoothConnectedFromPreferences() {
        return mConnectionPref.getBoolean(Constants.BLUETOOTH_CONNECTED, false);
    }

    /**
     * Saves connection state to SharedPreferences.<br>
     * However, state is preserved as boolean so it is true only for connected state.<br>
     * No particular intermittent state is saved.
     *
     * @param connectionState connection state such as {@link BluetoothProfile#STATE_CONNECTED}
     */
    public void putConnectionStateToPreferences(int connectionState) {
        if (mConnectionPref == null) return;

        final boolean isConnected = connectionState == BluetoothProfile.STATE_CONNECTED;
        SharedPreferences.Editor editor = mConnectionPref.edit();
        editor.putBoolean(Constants.BLUETOOTH_CONNECTED, isConnected);
        editor.apply();
    }

    /**
     * Sends broadcast to trigger bluetooth intent in order to refresh connection state.<br>
     * Since at the time of writing there is no direct way to check the state of the<br>
     * bluetooth paired device, probing is used.<br>
     * Once method is executed, the change transmitted will result in a bluetooth intent<br>
     * carrying connection state to be consumed by a {@link android.content.BroadcastReceiver}.<br>
     * One should listen to action: {@link #ACTION_CONNECTION_STATE_CHANGED}
     *
     * @param sharingType one of:<br>
     *                    - {@link BluetoothSharingType#CONTACT} or<br>
     *                    - {@link BluetoothSharingType#CONTACT}
     */
    public void probeConnectionState(BluetoothSharingType sharingType) {
        if (isBluetoothOn()) {
            final boolean isSharing = sharingType == CONTACT
                    ? isContactSharingEnabled() : isCallHistorySharingEnabled();
            ContactSync syncTypeOriginal;
            ContactSync syncTypeTrigger;
            if (sharingType == CONTACT) {
                if (isSharing) {
                    syncTypeOriginal = ContactSync.ContactSharingOn;
                    syncTypeTrigger = ContactSync.ContactSharingOff;
                } else {
                    syncTypeOriginal = ContactSync.ContactSharingOff;
                    syncTypeTrigger = ContactSync.ContactSharingOn;
                }
            } else {
                if (isSharing) {
                    syncTypeOriginal = ContactSync.CallHistorySharingOn;
                    syncTypeTrigger = ContactSync.CallHistorySharingOff;
                } else {
                    syncTypeOriginal = ContactSync.CallHistorySharingOff;
                    syncTypeTrigger = ContactSync.CallHistorySharingOn;
                }
            }
            setContactSharing(syncTypeTrigger);
            setContactSharing(syncTypeOriginal);
        }
    }

    /**
     * Saves Bluetooth device bond state to SharedPreferences.<br>
     * This is to be done through BroadcastReceiver listening to Bluetooth intent.<br>
     * see {@link BluetoothStateService}
     *
     * @param bondState should be one of:<br>
     *                  - {@link BluetoothDevice#BOND_BONDED}
     *                  - {@link BluetoothDevice#BOND_BONDING}
     *                  - {@link BluetoothDevice#BOND_NONE}
     */
    public void putBondStateToPreferences(int bondState) {
        if (mConnectionPref == null) return;

        final boolean isBonded = bondState == BluetoothDevice.BOND_BONDED;
        SharedPreferences.Editor editor = mConnectionPref.edit();
        editor.putBoolean(Constants.BLUETOOTH_BOUNDED, isBonded);
        editor.apply();
    }

    /**
     * Does same as {@link BlueHelper#putBondStateToPreferences(int)}, only this method<br>
     * takes bond state from (@link {@link BluetoothDevice#getBondState()})
     *
     * @param bondedDevice {@link BluetoothDevice} who's bond state is saved to preferences
     */
    public void putBondStateToPreferences(BluetoothDevice bondedDevice) {
        if (bondedDevice == null) return;

        putBondStateToPreferences(bondedDevice.getBondState());
    }

    /**
     * Utility method for getting the adapter
     *
     * @return instance of the {@link BluetoothAdapter}
     */
    private BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Wrapper for {@link BluetoothAdapter#isEnabled()}
     *
     * @return true if bluetooth is enabled
     */
    private boolean isBluetoothOn() {
        return getBluetoothAdapter().isEnabled();
    }

    /**
     * Method checks if the bluetooth is on and if there is a bonded, connected device.
     *
     * @return true if bluetooth is on and if there is a bonded, connected device
     */
    public boolean isBluetoothLinkEstablished() {
        return isBluetoothOn() && isDeviceBonded();
    }

    /**
     * We use SharedPreference populated broadcast receiver<br>
     * as a point of reference that connected bluetooth device
     *
     * @return true there is a bluetooth bonded device, false otherwise
     */
    private boolean isDeviceBonded() {
        return isBluetoothConnectedFromPreferences()
                && !getBluetoothAdapter().getBondedDevices().isEmpty();
    }

    /**
     * Creates an {@link IntentFilter} with following actions:<br>
     * - {@link BluetoothDevice#ACTION_BOND_STATE_CHANGED}<br>
     * - {@link #ACTION_CONNECTION_STATE_CHANGED}
     *
     * @return {@link IntentFilter} with the above mentioned actions
     */
    public IntentFilter getBluetoothIntentFilter() {
        final IntentFilter bluetoothIntentFilter = new IntentFilter();
        bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bluetoothIntentFilter.addAction(ACTION_CONNECTION_STATE_CHANGED);
        return bluetoothIntentFilter;
    }

    /**
     * Model processing for the available variation on the exposed platform bluetooth API<br>
     * for changing the bluetooth Contact Sharing & Call History, respectively.
     */
    public enum ContactSync {

        ContactSharingOn(EXTRA_KEY_CONTACTS, EXTRA_VALUE_ENABLED),
        ContactSharingOff(EXTRA_KEY_CONTACTS, EXTRA_VALUE_DISABLED),
        CallHistorySharingOn(EXTRA_KEY_CALL_LOGS, EXTRA_VALUE_ENABLED),
        CallHistorySharingOff(EXTRA_KEY_CALL_LOGS, EXTRA_VALUE_DISABLED);

        /*key for the Intent extra*/
        public final String sharingStateKey;

        /*value associated with the key for the Intent extra*/
        public final String sharingStateValue;


        ContactSync(String sharingStateKey, String sharingStateValue) {
            this.sharingStateKey = sharingStateKey;
            this.sharingStateValue = sharingStateValue;
        }

        /**
         * Broadcast intent of the current desired state for the bluetooth Contact Sharing or<br>
         * Call History Sharing to platform, using the exposed API.
         *
         */
        public void broadcast() {
            getApplicationContext().sendBroadcast(createIntent());
        }

        /**
         * Create Intent for the current ContactSync/CallHistorySync state
         *
         * @return {@link Intent} to be sent to platform
         */
        Intent createIntent() {
            Intent syncIntent = new Intent(INTENT_MODIFY_PBAP_SETTINGS);
            syncIntent.putExtra(sharingStateKey, sharingStateValue);
            /** Note:
             * If you're using this API on a platform version below 2.0 under BRIO-9750,<br>
             * uncomment the statement below, otherwise, contact sync will not work:<br>
             * syncIntent.putExtra(CallHistorySharingOn.sharingStateKey, sharingStateValue);
             */
            return syncIntent;
        }


    }

    /**
     * Distinguish between Bluetooth [Contact Sharing & Call History Sharing]<br>
     * see {@link BluetoothSyncState#fromHelper(BlueHelper, BluetoothSharingType)}
     */
    public enum BluetoothSharingType {
        CONTACT, CALL_HISTORY;
    }

    /**
     * Keeping the Bluetooth link state clear. Established through broadcast receiver,<br>
     * listening to Bluetooth intent actions:<br>
     * - "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED" and<br>
     * - {@link BluetoothDevice#ACTION_BOND_STATE_CHANGED}
     */
    public enum BluetoothSyncState {
        ENABLED, /*Bluetooth is on, has bonded device and sharing is enabled*/
        DISABLED, /* Bluetooth is on but Sharing is disabled */
        UNPAIRED; /* Device is not paired or disconnected from the other side*/

        public static BluetoothSyncState fromHelper(BlueHelper blueHelper, BluetoothSharingType sharingType) {
            if (blueHelper.isBluetoothLinkEstablished()) {
                return blueHelper.isSharingEnabled(sharingType) ? ENABLED : DISABLED;
            } else {
                return UNPAIRED;
            }
        }
    }
}
