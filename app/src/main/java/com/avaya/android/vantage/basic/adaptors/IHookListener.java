package com.avaya.android.vantage.basic.adaptors;

import com.avaya.deskphoneservices.HandsetType;

/**
 * Interface which connects {@link com.avaya.android.vantage.basic.activities.MainActivity} and
 * {@link UIDeskPhoneServiceAdaptor}. Main purpose is to inform
 * {@link com.avaya.android.vantage.basic.activities.MainActivity} are we on hook or off hook for
 * {@link HandSetType}
 */

public interface IHookListener {
    enum HandSetType{CORDLESS_HANDSET, WIRED_HANDSET, WIRED_HEADSET;

        static IHookListener.HandSetType valueOf(HandsetType type){
            switch (type){
                case CORDLESS_HANDSET:
                    return HandSetType.CORDLESS_HANDSET;
                case WIRED_HANDSET:
                    return HandSetType.WIRED_HANDSET;
                case WIRED_HEADSET:
                    return HandSetType.WIRED_HEADSET;
            }
            return HandSetType.WIRED_HANDSET;
        }
    }

    /**
     * Provided handset type is on hook
     * @param handsetType type of handset which changed state to on hook
     */
    void onOnHook(IHookListener.HandSetType handsetType);

    /**
     * Provided handset type is off hook
     * @param handsetType type of handset which changed state to off hook
     */
    void onOffHook(IHookListener.HandSetType handsetType);
}
