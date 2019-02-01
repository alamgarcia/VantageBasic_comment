package com.avaya.android.vantage.basic.csdk;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.avaya.clientservices.media.OpusCodecProfileMode;

/**
 * Enumerated type for the possible values in the ENABLE_OPUS setting
  */
public enum OpusValue {

    // According to ELAN SRAD, those are values that can appear in the settings file
    OFF(0),
    ENABLE_OPUS_WIDEBAND_20K(1),
    ENABLE_OPUS_NARROWBAND_16K(2),
    ENABLE_OPUS_NARROWBAND_12K(3);

    private final int value;

   OpusValue(int value) {
        this.value = value;
    }

    /**
     * Return {@link OpusValue} Enum
     * @return
     */
    public int getValue() {
        return value;
    }

    private final static String LOG_TAG = "OpusValue";

    @Nullable
    public OpusCodecProfileMode getOpusMode() {
        switch (this) {
            case OFF:
                return OpusCodecProfileMode.OFF;
            case ENABLE_OPUS_WIDEBAND_20K:
                return OpusCodecProfileMode.WIDE_BAND;
            case ENABLE_OPUS_NARROWBAND_16K:
                return OpusCodecProfileMode.NARROW_BAND;
            case ENABLE_OPUS_NARROWBAND_12K:
                return OpusCodecProfileMode.CONSTRAINED_NARROW_BAND;
            default:
                throw new IllegalArgumentException("No OpusMode for " + getValue());
        }
    }

    /**
     * Get the {@code OpusValue} instance with given numeric value.
     *
     * @param value The numeric value.
     * @return The corresponding {@code OpusValue} instance.
     * @exception IllegalArgumentException if the numeric value doesn't match
     *            the known instances.
     */
    @NonNull
    public static OpusValue getOpusValue(int value) {
        for (final OpusValue instance : values()) {
            if (value == instance.value) {
                return instance;
            }
        }
        throw new IllegalArgumentException("No OpusValue for " + value);
    }
}
