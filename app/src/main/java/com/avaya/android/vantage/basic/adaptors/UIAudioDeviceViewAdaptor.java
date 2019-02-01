package com.avaya.android.vantage.basic.adaptors;

import com.avaya.android.vantage.basic.csdk.AudioDeviceAdaptorListener;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.model.UIAudioDevice;
import com.avaya.android.vantage.basic.views.interfaces.IDeviceViewInterface;

import java.util.List;

/**
 * Adaptor which implements {@link AudioDeviceAdaptorListener} and check for state of audio device.
 * We check if audio device is off hook, is there device change, did we got requested device.
 */

public class UIAudioDeviceViewAdaptor implements AudioDeviceAdaptorListener {

    private IDeviceViewInterface mDeviceViewInterface;

    /**
     * Set view interface for device
     * @param viewInterface view interface to be set
     */
    public void setDeviceViewInterface(IDeviceViewInterface viewInterface) {
        this.mDeviceViewInterface = viewInterface;
    }


    /**
     * Obtaining list of audio devices {@link UIAudioDevice} from {@link SDKManager}
     * @return list of {@link UIAudioDevice}
     */
    public List<UIAudioDevice> getAudioDeviceList(){
        if(SDKManager.getInstance().getAudioDeviceAdaptor().getAudioDeviceList()!=null)
            return SDKManager.getInstance().getAudioDeviceAdaptor().getAudioDeviceList();
        else
            return null;
    }

    /**
     * Obtaining audio device {@link UIAudioDevice} from {@link SDKManager}
     * @return active audio device
     */
    public UIAudioDevice getActiveAudioDevice() {
        return SDKManager.getInstance().getAudioDeviceAdaptor().getActiveAudioDevice();
    }

    /**
     * Return information if device is off hook. Information is obtained from {@link SDKManager}
     * @return boolean
     */
    public boolean isDeviceOffHook() {
        return SDKManager.getInstance().getAudioDeviceAdaptor().isDeviceOffHook();
    }

    /**
     * Return {@link UIAudioDevice} requested by user
     * @return {@link UIAudioDevice}
     */
    public UIAudioDevice getUserRequestedDevice() {
        return SDKManager.getInstance().getAudioDeviceAdaptor().getUserRequestedDevice();
    }


    /**
     * Set {@link UIAudioDevice} which is requested by user
     * @param device {@link UIAudioDevice}
     */
    public void setUserRequestedDevice(UIAudioDevice device){
        SDKManager.getInstance().getAudioDeviceAdaptor().setUserRequestedDevice(device);
    }

    /**
     * Changing {@link UIAudioDevice} for audio device
     * @param device {@link UIAudioDevice} to be set
     * @param active boolean should it be active
     */
    public void onDeviceChanged(UIAudioDevice device, boolean active){
        if (mDeviceViewInterface != null)
            mDeviceViewInterface.onDeviceChanged(device, active);
    }
}
