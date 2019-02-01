package com.avaya.android.vantage.basic;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.avaya.android.vantage.basic.activities.CallDialerActivity;
import com.avaya.android.vantage.basic.csdk.DeskPhoneServiceAdaptor;
import com.avaya.android.vantage.basic.csdk.LocalContactInfo;
import com.avaya.android.vantage.basic.csdk.SDKManager;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static android.os.Build.DEVICE;
import static android.os.Build.PRODUCT;
import static com.avaya.android.vantage.basic.Constants.MILISECONDS_IN_SECOND;
import static com.avaya.android.vantage.basic.Constants.UUID_PREFIX;

/**
 * Utility class used for providing contact information and sending Snackbar broadcast
 */

public class Utils {

    public static final boolean SNACKBAR_LONG = true;
    public static final boolean SNACKBAR_SHORT = false;
    public static final String CONTACT = "CONTACT";
    public static final String CALLHISTORY = "CALLHISTORY";
    public static final String ENABLED = "ENABLED";
    public static final String DISABLED = "DISABLED";
    public static final String PBAP_URL = "avaya.intent.action.MODIFY_PBAP_SETTINGS";
    public static final String SYNC_TYPE = "syncType";
    public static final String STATUS = "status";
    private static final String TAG = "AvayaUtils";

    private static boolean sIsLandScape;

    public static CallDialerActivity callDialerActivity;

    /**
     * Method that get display name
     * If call is CM conference - it just returns the display name
     * Otherwise, it searches the contact in contact list using number and returns contact name (if not empty)
     *
     * @param displayNumber      Phone number
     * @param displayName        Phone display name
     * @param isCMConferenceCall boolean that displays if this is a conference call
     * @return Display name
     */
    public static String getContactName(String displayNumber, String displayName, boolean isCMConferenceCall) {

        String contactName;
        if (TextUtils.isEmpty(displayName)) {
            displayName = displayNumber;
        }

        if (isCMConferenceCall) {
            contactName = displayName;
        } else {
            // this line of code will get display name by phone number
            String resolvedDisplayName = getFirstContact(displayNumber);
            //if contact doesn't exist in contact list, use display name
            contactName = TextUtils.equals(displayNumber, resolvedDisplayName) ? displayName : resolvedDisplayName;
        }
        return contactName;
    }

    /**
     * Method for finding contact name for calling number
     *
     * @param number Phone number
     * @return Contact name if it exist in contact list or phone number
     */
    public static String getFirstContact(String number) {
        String[] searchResults = LocalContactInfo.phoneNumberSearch(number);

        if (searchResults != null && searchResults[0].trim().length() > 0 && searchResults[1].trim().length() > 0) {
            if (searchResults[1].replaceAll("[\\D]", "").equalsIgnoreCase(number)) {
                return searchResults[0];
            }
        }
        return number;
    }

    /**
     * Method for finding finding contact photo URI
     *
     * @param number Phone number
     * @return Photo URI if exists
     */
    public static String getPhotoURI(String number) {
        String[] searchResults = LocalContactInfo.phoneNumberSearch(number);

        String photoURI = null;
        if (searchResults != null && searchResults.length > 2 &&
                searchResults[3] != null && searchResults[3].trim().length() > 0) {
            photoURI = searchResults[3];
        }
        return photoURI;
    }


    /**
     * Sending local broadcast with data for Snackbar to be shown.
     * Broadcast is captured in {@link com.avaya.android.vantage.basic.activities.MainActivity}
     *
     * @param context Activity context
     * @param message String to be shown in Snackbar
     * @param length  boolean length of {@link Snackbar}
     *                false Snackbar.LENGTH_SHORT
     *                true Snackbar.LENGTH_LONG
     */
    public static void sendSnackBarData(Context context, String message, boolean length) {
        Intent snackBarShow = new Intent(Constants.SNACKBAR_SHOW);
        snackBarShow.putExtra(Constants.SNACKBAR_MESSAGE, message);
        snackBarShow.putExtra(Constants.SNACKBAR_LENGTH, length);
        LocalBroadcastManager.getInstance(context).sendBroadcast(snackBarShow);
    }

    /**
     * Sending normal broadcast with data for number of missed calls
     * Broadcast is captured in {@link com.avaya.android.vantage.basic.receiver.ConfigReceiver}
     *
     * @param context Activity context
     * @param numberOfMissedCalls Number of missed calls
     */
    public static void refreshHistoryIcon(Context context, int numberOfMissedCalls) {
        Intent refreshHistoryIcon = new Intent(Constants.REFRESH_HISTORY_ICON);
        refreshHistoryIcon.putExtra(Constants.EXTRA_UNSEEN_CALLS, numberOfMissedCalls);
        context.sendBroadcast(refreshHistoryIcon);
    }

    public static void sendSnackBarDataWithDelay(final Context context, final String message, final boolean length) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                sendSnackBarData(context, message, length);
            }
        }, MILISECONDS_IN_SECOND);
    }

    /**
     * Performing check if bitmap is provided by CSDK or we have just chosen image from gallery application
     * In case we are taking bitmap from gallery we have to perform check for rotation before we save such
     * bitmap as it can be incorrectly rotated.
     *
     * @param bitmap   Which have to be saved
     * @param resolver {@link ContentResolver}
     * @param uri      {@link Uri} of bitmap used
     * @return Bitmap
     */
    public static Bitmap checkAndPerformBitmapRotation(Bitmap bitmap, ContentResolver resolver, Uri uri) {
        if (!Utils.getRealPathFromURI(resolver, uri).isEmpty()) {
            try {
                File imageFile = new File(Utils.getRealPathFromURI(resolver, uri));
                ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                bitmap = Utils.prepareRotation(bitmap, orientation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    /**
     * Perform check if Uri provided belong to some of images already saved on device and returning
     * physical patch to that image. In case that we can't find physical patch we are returning
     * empty string
     *
     * @param resolver {@link ContentResolver}
     * @param uri      {@link Uri} for requested image
     * @return String with physical patch to image or in case image is provided by CSDK empty string.
     */
    private static String getRealPathFromURI(ContentResolver resolver, Uri uri) {
        String path = "";
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                path = cursor.getString(idx);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    /**
     * Check in which direction image have to be rotated adn calling image rotating methods with
     * adequate parameters
     *
     * @param source      Bitmap to be rotated
     * @param orientation int representing current rotation of image
     * @return Bitmap which is properly rotated or in case of failure return non changed image
     */
    private static Bitmap prepareRotation(Bitmap source, int orientation) {

        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                source = rotateImage(source, 90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                source = rotateImage(source, 180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                source = rotateImage(source, 270);
                break;

            case ExifInterface.ORIENTATION_NORMAL:

            default:
                break;
        }
        return source;
    }

    /**
     * Rotatin provided bitmap for provided angle
     *
     * @param source Bitmap to be rotated
     * @param angle  float for which provided bitmap have to be rotated
     * @return prepared and rotated bitmap
     */
    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    /**
     * This function was provided by CSDK group. It tests whether word CONFERENCE appears in
     * in the provided string taken into account localization aspects of CM conference.
     * Note that the strings in this function can not be resolved via the device's localization
     * mechanism since CM's localization is not synchronized with the device's localization.
     *
     * @param sRemoteAddress Remote address of the call
     * @return prepared and rotated bitmap
     */
    public static boolean containsConferenceString(String sRemoteAddress) {

        if (sRemoteAddress == null || sRemoteAddress.isEmpty()) return false;

        // english or french
        if (sRemoteAddress.indexOf("CONFERENCE") != -1) {
            return true;
        }

        // italian
        if (sRemoteAddress.indexOf("CONFERENZA") != -1) {
            return true;
        }

        // spanish
        if (sRemoteAddress.indexOf("CONFERENCIA") != -1) {
            return true;
        }

        // german
        if (sRemoteAddress.indexOf("Konferenz") != -1) {
            return true;
        }

        // russian
        if ((sRemoteAddress.indexOf("Конференция") != -1) || (sRemoteAddress.indexOf("КОНФЕРЕНЦИЯ") != -1)) {
            return true;
        }

        // dutch
        if (sRemoteAddress.indexOf("CONFERENTIE") != -1) {
            return true;
        }

        // portuguese
        if (sRemoteAddress.indexOf("CONFERENCIA") != -1) {
            return true;
        }

        // korean
        if (sRemoteAddress.indexOf("회의") != -1) {
            return true;
        }

        // japanese
        if (sRemoteAddress.indexOf("会議") != -1) {
            return true;
        }

        // chinese
        return sRemoteAddress.indexOf("会议") != -1;

    }

    /**
     * @return true if a camera is available
     */
    public static boolean isCameraSupported() {
        return SDKManager.getInstance().isCameraSupported();
    }

    public static void wakeDevice(Context context) {
        Log.d(TAG, "force waking device");
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "WakeUpLock");
        synchronized (context) {
            if (wakeLock.isHeld())
                wakeLock.release();
            wakeLock.acquire();
            wakeLock.release();
        }
    }

    /**
     * enum that defines that states of the contacts or call logs syncing with the
     * paired device.
     */
    public enum SyncState {
        NOT_PAIRED("not paired"), SYNC_OFF("sync off"), SYNC_ON("sync on");

        private String mStateName;

        SyncState(String stateName) {
            mStateName = stateName;
        }

        public String getStateName() {
            return mStateName;
        }
    }

    /**
     * @param context Activity context
     * @return true if at least one BT device is connected.
     */
    public static boolean havePairedDevice(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return false;
        } else {
            // bluetooth is off
            if(!bluetoothAdapter.isEnabled() && bluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) != BluetoothHeadset.STATE_CONNECTED) {
                return false;
            } else {
                // Get bounded devices from BT adapter
               Set<BluetoothDevice> boundedDevices = bluetoothAdapter.getBondedDevices();
                if (boundedDevices.isEmpty()) {
                    return false;
                }
                if(boundedDevices.size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Hides Soft keyboard from the screen
     * @param activity reference to the Activity
     */
    public static void hideKeyboard(Activity activity){
        if (activity == null) return;
        View focus = activity.getCurrentFocus();
        if (focus != null) {
            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    public static void openKeyboard(Activity activity){
        if (activity == null) return;
        View focus = activity.getCurrentFocus();
        if (focus != null) {
            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(focus,0);
        }
    }
    /**
     * Creates UUID object from the MAC address of the devices ethernet interface.
     * @return {@link UUID} object
     */
    public static UUID uniqueDeviceUUID() {

        String mac = getMac();

        try {
            UUID uuid = UUID.fromString(UUID_PREFIX + mac);
            if(uuid!=null && uuid.toString().endsWith("-"))
                return uuid;
            else
                return UUID.randomUUID();
        }
        catch (Exception e) {
            Log.d(TAG, "Could not build UUID basing on MAC due to " + e.getMessage() + " Returning random.");
            return UUID.randomUUID();
        }
    }

    /**
     * @return String: MAC address of the devices ethernet interface.
     */
    public static String getMac(){

        String mac = SystemPropertiesProxy.get(Constants.AVAYA_ETHADDR, null);
        if (mac == null) {
            Log.e(TAG, "Could not get MAC");
            return null;
        }
        Log.d(TAG, "MAC="+mac);
        mac = mac.replace(":", "");

        return mac;
    }

    /**
     * @param context Activity context
     * @param firstName String: first name
     * @param lastName String: last name
     * @return String: last and first name connected based on the configured preferences.
     */
    public static String combinedName(Context context, String firstName, String lastName) {
        return String.format(context.getResources().getConfiguration().locale, "%s %s", firstName, lastName).trim();
    }

    /**
     * Sets the landscape flag indicator from the value from the resources
     * @param context Activity context
     */
    public static void setDeviceMode(Context context) {
        sIsLandScape = context.getResources().getBoolean(R.bool.is_landscape);
    }

    /**
     * @return true if the landscape mode is set.
     */
    public static boolean isLandScape() {
        return sIsLandScape;
    }

    public static boolean isK155() {
        return ((PRODUCT.indexOf("K155") != -1) || (DEVICE.indexOf("K155") != -1));
    }
}
