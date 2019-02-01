package com.avaya.android.vantage.basic.services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.avaya.android.vantage.basic.fragments.settings.BlueHelper;


public class BluetoothStateService extends Service {

    private BlueHelper mBlueHelper;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        mBlueHelper = BlueHelper.instance();
        mBlueHelper.probeConnectionState(BlueHelper.BluetoothSharingType.CONTACT);
        registerReceiver(mBluetoothBroadcastReceiver, mBlueHelper.getBluetoothIntentFilter());
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBluetoothBroadcastReceiver);
    }

    private final BroadcastReceiver mBluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BlueHelper.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                final int connectionState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                mBlueHelper.putConnectionStateToPreferences(connectionState);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBlueHelper.putBondStateToPreferences(device);
            }
        }
    };

    /**
     * Left as utility for testing state
     *
     * @param device {@link BluetoothDevice}
     * @return String representing bluetooth device bond state
     */
    private String returnBondingState(BluetoothDevice device) {
        String bondState;
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDING:
                bondState = "BluetoothDevice.BOND_BONDING pref:BLUETOOTH_BOUNDED=False";
                break;
            case BluetoothDevice.BOND_BONDED:
                bondState = "BluetoothDevice.BOND_BONDED pref:BLUETOOTH_BOUNDED=True";
                break;
            case BluetoothDevice.BOND_NONE:
                bondState = "BluetoothDevice.BOND_NONE pref:BLUETOOTH_BOUNDED=False";
                break;
            default:
                bondState = "default";
                break;
        }
        return bondState;
    }

    /**
     * Left as utility for testing state
     *
     * @param connectionStateValue bluetooth connection state
     * @return String representing bluetooth connection state
     */
    private String returnConnectionState(int connectionStateValue) {
        String connectionState;
        switch (connectionStateValue) {
            case BluetoothProfile.STATE_CONNECTING:
                connectionState = "BluetoothProfile.STATE_CONNECTING";
                break;
            case BluetoothProfile.STATE_CONNECTED:
                connectionState = "BluetoothProfile.STATE_CONNECTED";
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                connectionState = "BluetoothProfile.STATE_DISCONNECTING";
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                connectionState = "BluetoothProfile.STATE_DISCONNECTED";
                break;
            default:
                connectionState = "default";
                break;
        }
        return connectionState;
    }
}
