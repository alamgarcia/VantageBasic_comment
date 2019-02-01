package com.avaya.android.vantage.basic.csdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.avaya.clientservices.call.MediaEncryptionType;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import static com.avaya.clientservices.call.MediaEncryptionType.AES_128_HMAC_SHA1_32;
import static com.avaya.clientservices.call.MediaEncryptionType.AES_128_HMAC_SHA1_80;
import static com.avaya.clientservices.call.MediaEncryptionType.AES_256_HMAC_SHA1_32;
import static com.avaya.clientservices.call.MediaEncryptionType.AES_256_HMAC_SHA1_80;

/**
 * Enumerated type for the possible values in the MEDIAENCRYPTION list setting,
 * as defined in UCAPPSSE-601.
 */
public enum MediaEncryptionValue {

    // According to UCAPPSSE-601, the 3.0 client only supports 1, 2, 10, 11, and 9.
    AESCM128_HMAC80(1),
    AESCM128_HMAC32(2),
    AESCM128_HMAC80_UNAUTH(3),
    AESCM128_HMAC32_UNAUTH(4),
    AESCM128_HMAC80_UNENC(5),
    AESCM128_HMAC32_UNENC(6),
    AESCM128_HMAC80_UNENC_UNAUTH(7),
    AESCM128_HMAC32_UNENC_UNAUTH(8),
    NONE(9),
    AESCM256_HMAC80(10),
    AESCM256_HMAC32(11);

    private final int value;

    MediaEncryptionValue(int value) {
        this.value = value;
    }

    /**
     * Return {@link MediaEncryptionValue} Enum
     * @return
     */
    public int getValue() {
        return value;
    }

    private final static String LOG_TAG = "MediaEncryptionValue";

    @Nullable
    public MediaEncryptionType getMediaEncryptionType() {
        switch (this) {
            case NONE:
                return MediaEncryptionType.NONE;
            case AESCM128_HMAC32:
                return AES_128_HMAC_SHA1_32;
            case AESCM128_HMAC80:
                return AES_128_HMAC_SHA1_80;
            case AESCM256_HMAC32:
                return AES_256_HMAC_SHA1_32;
            case AESCM256_HMAC80:
                return AES_256_HMAC_SHA1_80;
            default:
                return null;
        }
    }

    /**
     * Get the {@code MediaEncryptionValue} instance with given numeric value.
     *
     * @param value The numeric value.
     * @return The corresponding {@code MediaEncryptionValue} instance.
     * @exception IllegalArgumentException if the numeric value doesn't match
     *            the known instances.
     */
    @NonNull
    public static MediaEncryptionValue getMediaEncryptionValue(int value) {
        for (final MediaEncryptionValue instance : values()) {
            if (value == instance.value) {
                return instance;
            }
        }
        throw new IllegalArgumentException("No MediaEncryptionValue for " + value);
    }

    /**
     * Converts a list of integers into a list of the corresponding enum
     * instances.
     * <p>
     * Any integer values that don't have a corresponding enum instance will be
     * silently dropped.
     *
     * @param integerList The list of integer values.
     * @return The list of corresponding {@code MediaEncryptionValue} instances.
     */
    @NonNull
    public static List<MediaEncryptionValue> convertMediaEncryptionList(
            @NonNull List<Integer> integerList) {
        final List<MediaEncryptionValue> result = new ArrayList<>(integerList.size());
        for (final int i : integerList) {
            try {
                result.add(getMediaEncryptionValue(i));
            } catch (IllegalArgumentException ignored) {
                Log.w(LOG_TAG, "Found unexpected MediaEncryptionValue " + i);
            }
        }
        return result;
    }

    /**
     * Returns list of {@link MediaEncryptionType} for each {@link MediaEncryptionValue}
     * specified in the mediaEncryptionValues
     * @param mediaEncryptionValues {@link MediaEncryptionValue}
     * @return {@link List<MediaEncryptionType>}
     */
    @NonNull
    public static List<MediaEncryptionType> mediaEncryptionTypeList(
            @NonNull List<MediaEncryptionValue> mediaEncryptionValues) {
        final List<MediaEncryptionType> mediaEncryptionTypes = new ArrayList<>(mediaEncryptionValues.size());
        for (final MediaEncryptionValue value : mediaEncryptionValues) {
            final MediaEncryptionType type = value.getMediaEncryptionType();
            if (type != null) {
                mediaEncryptionTypes.add(type);
            }
        }
        return mediaEncryptionTypes;
    }
}
