package com.avaya.android.vantage.basic.csdk;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.model.UIAudioDevice;
import com.avaya.clientservices.media.AudioDevice;
import com.avaya.clientservices.media.AudioDeviceError;
import com.avaya.clientservices.media.AudioDeviceListener;
import com.avaya.clientservices.media.AudioInterface;
import com.avaya.clientservices.media.AudioTone;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.avaya.android.vantage.basic.Constants.INCOMING_CALL_ACCEPT;
import static com.avaya.android.vantage.basic.model.UIAudioDevice.BLUETOOTH_HEADSET;
import static com.avaya.android.vantage.basic.model.UIAudioDevice.WIRED_HEADSET;
import static com.avaya.android.vantage.basic.model.UIAudioDevice.WIRELESS_HANDSET;

/**
 * {@link AudioDeviceAdaptor} implements {@link AudioDeviceListener} and fallow state of audio device
 */

public class AudioDeviceAdaptor implements AudioDeviceListener {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private static final String HEADSET_3_5_MM = "3.5mm Headset";
    private WeakReference<AudioDeviceAdaptorListener> mUiObj;
    private List<UIAudioDevice> mAudioDeviceList = new ArrayList<UIAudioDevice>();
    private boolean mIsOffHook = false;
    private int mOffHookCallId = -1;

    /**
     * Processing change on audio device list
     * @param newDeviceList list of {@link AudioDevice}
     * @param activeDeviceChanged boolean is active device changed
     */
    @Override
    public void onAudioDeviceListChanged(List<AudioDevice> newDeviceList, boolean activeDeviceChanged) {
        Log.d(LOG_TAG, "onAudioDeviceListChanged");
        int callId = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
        boolean isActive = callId!=0;
        //if (callId != 0) {//there is an active call

            if (activeDeviceChanged) {
                //active device was removed
                UIAudioDevice device=getUserRequestedDevice();
                if (device.equals(UIAudioDevice.BLUETOOTH_HEADSET) && mAudioDeviceList.contains(WIRED_HEADSET))
                    onDeviceChanged(UIAudioDevice.WIRED_HEADSET, isActive);
                else {
                    onDeviceChanged(UIAudioDevice.SPEAKER, isActive);
                }
            }
            else{
                if (callId != 0) {

                    if (getUserRequestedDevice() != UIAudioDevice.HANDSET && getUserRequestedDevice() != UIAudioDevice.WIRELESS_HANDSET) {
                        if (isDeviceIncluded(newDeviceList, AudioDevice.Type.WIRED_HEADSET, HEADSET_3_5_MM) && !mAudioDeviceList.contains(WIRED_HEADSET)) {
                            onDeviceChanged(WIRED_HEADSET, isActive);
                        }//3.5 mm device insert
                        else if (isDeviceIncluded(newDeviceList, AudioDevice.Type.WIRELESS_HANDSET, null) && !mAudioDeviceList.contains(WIRELESS_HANDSET) /*&& callId != 0*/ && SDKManager.getInstance().getDeskPhoneServiceAdaptor().isOffHookBTHandset()) {
                            onDeviceChanged(WIRELESS_HANDSET, isActive);
                        }// BT handset connected
                    }
                    if (isDeviceIncluded(newDeviceList, AudioDevice.Type.BLUETOOTH_HEADSET, null) && !mAudioDeviceList.contains(BLUETOOTH_HEADSET)) {
                        onDeviceChanged(BLUETOOTH_HEADSET, isActive);
                    }// BT headset connected
                }
                else { //if callId==0, meaning there is no active call
                        if (isDeviceDisconnectedOffline(newDeviceList)){
                            onDeviceChanged(UIAudioDevice.SPEAKER, isActive);
                    }
                }
            }

        setAvailableDevices(newDeviceList);
    }

    /**
     * Perform check if requested audio device type is included
     * @param list of {@link AudioDevice}
     * @param type of {@link AudioDevice}
     * @param name of {@link AudioDevice} to be searched for
     * @return boolean if we found requested device
     */
    private boolean isDeviceIncluded(List<AudioDevice> list, AudioDevice.Type type, String name) {
        if(list!=null) {
            for (AudioDevice device : list) {
                if (device.getType() == (type) && (name == null || device.getName().equals(name))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param newDeviceList {@link AudioDevice}
     * @return true if requested device in not in the list
     */
    private boolean isDeviceDisconnectedOffline(List<AudioDevice> newDeviceList){
        if(newDeviceList!=null && mAudioDeviceList!=null){
            for (UIAudioDevice uidevice : mAudioDeviceList){
                AudioDevice device = convertToAudioDevice(uidevice);
                if (!isDeviceIncluded(newDeviceList, device.getType(), device.getName()) && getUserRequestedDevice() == uidevice)
                    return true;
            }
        }
        return false;
    }

    /**
     * Called in case we have {@link AudioDevice} error
     * @param error {@link AudioDeviceError}
     */
    @Override
    public void onAudioDeviceError(AudioDeviceError error) {

    }

    private AudioInterface mAudioInterface;

    /**
     * Initializing {@link AudioDeviceAdaptor} and setting {@link #mAudioInterface} from {@link SDKManager}
     * and setting audio device listener
     */
    public void init() {
        mAudioInterface = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getAudioInterface();
        if (mAudioInterface != null) {
            mAudioInterface.addAudioDeviceListener(this);
        }
        //setUserRequestedDevice(UIAudioDevice.SPEAKER);

    }

    /**
     * Obtain active {@link UIAudioDevice}
     * @return {@link UIAudioDevice}
     */
    public UIAudioDevice getActiveAudioDevice() {
        return convertToUIAudioDevice(mAudioInterface.getActiveDevice());
    }

    /**
     * Obtain user requested {@link UIAudioDevice}
     * @return {@link UIAudioDevice}
     */
    public UIAudioDevice getUserRequestedDevice() {
        return convertToUIAudioDevice(mAudioInterface.getUserRequestedDevice());
    }

    /**
     * Obtaining list of {@link UIAudioDevice}
     * @return list of {@link UIAudioDevice}
     */
    public List<UIAudioDevice> getAudioDeviceList() {
        if(getAudioInterface()!=null)
            return convertToUIAudioDevice( getAudioInterface().getDevices());
        else
            return null;
    }

    /**
     * @return {@link AudioInterface}
     */
    private AudioInterface getAudioInterface() {
        return mAudioInterface;
    }

    /**
     * Registering {@link AudioDeviceAdaptorListener}
     * @param uiObj {@link AudioDeviceAdaptorListener}
     */
    public void registerListener(AudioDeviceAdaptorListener uiObj) {

        mUiObj = new WeakReference<AudioDeviceAdaptorListener>(uiObj);

        if (mUiObj.get() == null) {
            Log.e(LOG_TAG, "reference to AudioDeviceAdaptor is null");
        } else {
            Log.d(LOG_TAG, "reference to AudioDeviceAdaptor is NOT null");
        }
    }

    /**
     * Setting {@link UIAudioDevice} in {@link AudioInterface}
     * @param device {@link UIAudioDevice}
     */
    public void setUserRequestedDevice(UIAudioDevice device) {
        mAudioInterface.setUserRequestedDevice(convertToAudioDevice(device));
        if (SDKManager.getInstance().getCallAdaptor().getToneManager().isDialTone()) {
            SDKManager.getInstance().getCallAdaptor().getToneManager().stop();
            SDKManager.getInstance().getCallAdaptor().getToneManager().play(AudioTone.DIAL);
        }
        Log.v(LOG_TAG, "selected audio device is " + device.toString());
    }

    /**
     * Converting {@link AudioDevice} to {@link UIAudioDevice}
     * @param audioDevice {@link AudioDevice} to be converted to {@link UIAudioDevice}
     * @return {@link UIAudioDevice}
     */
    private UIAudioDevice convertToUIAudioDevice(AudioDevice audioDevice) {

        if (audioDevice == null)
            return UIAudioDevice.SPEAKER;

        if (audioDevice == null)
            return UIAudioDevice.SPEAKER;

        switch (audioDevice.getType()) {
            case WIRED_HEADSET:
                return WIRED_HEADSET;
            case HANDSET:
                return UIAudioDevice.HANDSET;
            case SPEAKER:
                return UIAudioDevice.SPEAKER;
            case BLUETOOTH_HEADSET:
                return BLUETOOTH_HEADSET;
            case WIRED_SPEAKER:
                return UIAudioDevice.WIRED_SPEAKER;
            case RJ9_HEADSET:
                return UIAudioDevice.RJ9_HEADSET;
            case WIRELESS_HANDSET:
                return UIAudioDevice.WIRELESS_HANDSET;
            default:
                return UIAudioDevice.SPEAKER;
        }
    }

    /**
     * @return true if Wireless Handset is available
     */
    public boolean isWirelessHandset() {
        List<AudioDevice> list = SDKManager.getInstance().getAudioDeviceAdaptor().mAudioInterface.getDevices();
        if(list!=null) {
            for (AudioDevice d : list) {
                if (d.getType() == AudioDevice.Type.WIRELESS_HANDSET) return true;
            }
        }
        return false;
    }

    /**
     * Converting {@link UIAudioDevice} to {@link AudioDevice}
     * @param uiAudioDevice {@link UIAudioDevice} to be converted
     * @return {@link AudioDevice}
     */
    private AudioDevice convertToAudioDevice(UIAudioDevice uiAudioDevice) {

        //AudioDevice.Type deviceType = AudioDevice.Type.values()[uiAudioDevice.ordinal()];

        AudioDevice.Type deviceType;
        switch (uiAudioDevice) {
            case WIRED_HEADSET:
                deviceType= AudioDevice.Type.WIRED_HEADSET;
                break;
            case HANDSET:
                deviceType= isWirelessHandset() ? AudioDevice.Type.WIRELESS_HANDSET : AudioDevice.Type.HANDSET;
                break;
            case SPEAKER:
                deviceType= AudioDevice.Type.SPEAKER;
                break;
            case BLUETOOTH_HEADSET:
                deviceType= AudioDevice.Type.BLUETOOTH_HEADSET;
                break;
            case WIRED_SPEAKER:
                deviceType= AudioDevice.Type.WIRED_SPEAKER;
                break;
            case RJ9_HEADSET:
                deviceType= AudioDevice.Type.RJ9_HEADSET;
                break;
            case WIRELESS_HANDSET:
                deviceType= AudioDevice.Type.WIRELESS_HANDSET;
                break;
            default:
                deviceType= AudioDevice.Type.SPEAKER;
        }

        //set device to be handset by default in case of some crash
        @SuppressWarnings("deprecation") AudioDevice audioDevice = AudioDevice.handset;
        List<AudioDevice> list = mAudioInterface.getDevices();
        if(list!=null) {
            for (AudioDevice device : list) {
                if (device.getType() == deviceType) {
                    audioDevice = device;
                }
            }
        }
        Log.d(LOG_TAG, "csdk audio device " + audioDevice.toString());
        return audioDevice;
    }

    /**
     * Sets audio devices
     * @param newDeviceList {@link AudioDevice}
     */
    void setAvailableDevices(List<AudioDevice> newDeviceList) {
        mAudioDeviceList.clear();
        if(newDeviceList!=null) {
            for (AudioDevice device : newDeviceList) {
                mAudioDeviceList.add(convertToUIAudioDevice(device));
            }
        }
    }

    /**
     * Converting list of {@link AudioDevice} to list of {@link UIAudioDevice}
     * @param audioDeviceList list of {@link AudioDevice}
     * @return list of {@link UIAudioDevice}
     */
    private List<UIAudioDevice> convertToUIAudioDevice(List<AudioDevice> audioDeviceList) {

        if(audioDeviceList == null)
            return null;

        //mAudioDeviceList.clear();
        List<UIAudioDevice> deviceList = new ArrayList<UIAudioDevice>();
        for (AudioDevice device : audioDeviceList) {
            deviceList.add(convertToUIAudioDevice(device));
        }

        return deviceList;
    }

    /**
     * Is device of hook
     * @return boolean is off hook
     */
    public boolean isDeviceOffHook() {
        return mIsOffHook;
    }

    /**
     * Processing off hook event for {@link UIAudioDevice}
     * @param context {@link Context}
     * @param audioDevice {@link UIAudioDevice} for which off hook event is processed
     */
    public void handleOffHook(Context context, UIAudioDevice audioDevice) {

        int isCallAlerting = SDKManager.getInstance().getCallAdaptor().isAlertingCall();
        int isActiveCall = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
        if (isCallAlerting ==0 && isActiveCall==0 && (isLockState(context))){
            Log.w(LOG_TAG, "handleOffHook : trying to do off hook while locked");
            return;
        }

        onDeviceChanged(audioDevice, true);
        mIsOffHook = true;

        if (isCallAlerting != 0) {
            Intent intent = new Intent(INCOMING_CALL_ACCEPT);
            intent.putExtra(Constants.CALL_ID, isCallAlerting);
            context.sendBroadcast(intent);
        } else if (isActiveCall == 0) {
            mOffHookCallId = SDKManager.getInstance().getCallAdaptor().createCall(false);
        }
    }

    /**
     * @param context Activity
     * @return true if the device is locked
     */
    private boolean isLockState(Context context) {
        KeyguardManager kgMgr = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean fullyLocked = context.getSystemService(ActivityManager.class).getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_LOCKED;
        Log.d(LOG_TAG, "isLockState : fullyLocked=" + fullyLocked);
        return !fullyLocked && ((kgMgr != null) && kgMgr.isDeviceLocked() && !ElanApplication.isPinAppLock);
    }

    /**
     * Processing on hook event for {@link UIAudioDevice}
     * @param audioDevice {@link UIAudioDevice} for which on hook event is processed
     */
    public void handleOnHook(UIAudioDevice audioDevice) {

        mIsOffHook = false;
        if (getUserRequestedDevice() != audioDevice) {
            //if the audio device is not handset, there is no meaning for onHook
            return;
        }

        int callId = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
        if (callId != 0) {
            SDKManager.getInstance().getCallAdaptor().endCall(callId);
        } else if (mOffHookCallId != -1) {
            SDKManager.getInstance().getCallAdaptor().endCall(mOffHookCallId);
        }
        if (mOffHookCallId != -1) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { // allow time for dialtone to end so it won't leak to speaker
                @Override
                public void run() {
                    onDeviceChanged(UIAudioDevice.SPEAKER, false);
                }
            }, 100);
        }
        else {
            onDeviceChanged(UIAudioDevice.SPEAKER, false);
        }

        mOffHookCallId = -1;


    }

    /**
     * Processing changing {@link UIAudioDevice} event
     * @param device {@link UIAudioDevice} which is changed
     * @param active boolean is device active
     */
    private void onDeviceChanged(UIAudioDevice device, boolean active) {
        Log.d(LOG_TAG, "onDeviceChanged to " + device.toString());
        setUserRequestedDevice(device);
        if (mUiObj != null && mUiObj.get() != null) {
            if (device == UIAudioDevice.WIRELESS_HANDSET)
                device = UIAudioDevice.HANDSET;
            mUiObj.get().onDeviceChanged(device, active);
        }
    }

    /**
     * Disconnects Bluetooth SCO link
     * @param context Activity
     */
    public void disconnectBluetoothSCO(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isBluetoothScoOn()) {
            Log.d(LOG_TAG, "disconnectBluetoothSco");
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
    }

    /**
     * Disconnects Bluetooth SCO link
     * @param context Activity
     */
    public void connectBluetoothSCO(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Log.d(LOG_TAG, "connectBluetoothSco");
        if (audioManager.isBluetoothScoOn() == false) {
            audioManager.setBluetoothScoOn(true);
            audioManager.startBluetoothSco();
        }
        else{
            Log.d(LOG_TAG, "BluetoothSco already on");
        }
    }


    /**
     * Stops listening AudioInterface
     */
    public void shutdown(){
        try {
            if (mAudioInterface != null) {
                synchronized (mAudioInterface) {
                    mAudioInterface.removeAudioDeviceListener(this);

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
