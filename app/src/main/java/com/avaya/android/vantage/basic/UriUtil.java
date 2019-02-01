package com.avaya.android.vantage.basic;

import android.support.annotation.NonNull;

import java.util.regex.Pattern;

/**
 * Miscellaneous utilities class for working with URIs and URLs.
 */
public final class UriUtil {

    private static final String TEL_SCHEME = "tel";

    private static final String TEL_SCHEME_URI_PREFIX = TEL_SCHEME + ':';

    private static final Pattern TEL_SCHEME_PATTERN = Pattern.compile(TEL_SCHEME_URI_PREFIX, Pattern.LITERAL);

    private UriUtil() {
        // Private constructor ensures can not be instantiated.
    }

    /**
     * Given a string of the form "tel:1234567890", returns the phone number
     * without the leading "tel:" prefix.
     *
     * @param uriString The tel: URI string of interest.
     * @return The corresponding phone number from the URI.
     */
    public static String getPhoneNumberFromTelURI(@NonNull CharSequence uriString) {
        return TEL_SCHEME_PATTERN.matcher(uriString).replaceAll("");
    }
}
