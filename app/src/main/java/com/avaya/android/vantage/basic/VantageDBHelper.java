package com.avaya.android.vantage.basic;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

/**
 * Helper class to access the vantage platform DB
 */
public class VantageDBHelper {

    public static final String TAG = VantageDBHelper.class.getSimpleName();
    private static final String AUTHORITY =
            "com.avaya.endpoint.providers.configurationproxy";
    private static final Uri CONTENT_URI = Uri.parse("content://" +
            AUTHORITY + "/spark");
    private static final boolean DEBUG_DB = false;
    public static final String SIPUSERNAME = "SipUserDisplayname";
    public static final String SIPUSERNUMBER = "Sipusername";
    public static final String ACTIVE_CSDK_BASED_PHONE_APP = "ActiveCsdkBasedPhoneApp";
    public static final String ENABLE_BT_CALLLOG_SYNC = "EnableBtCalllogSync";
    public static final String ENABLE_BT_CONTACTS_SYNC = "EnableBtContactsSync";

    /**
     * get URI for a given name
     *
     * @param name name of parameter
     * @return Uri for parameter
     */
    private static Uri getUriFor(String name) {
        return getUriFor(CONTENT_URI, name);
    }

    private static Uri getUriFor(Uri uri, String name) {
        return Uri.withAppendedPath(uri, name);
    }

    /**
     * get a value from Vantage DB
     *
     * @param resolver content resolver needed for database
     * @param name     parameter name
     * @return parameter value in String form
     */
    public static String getParameter(ContentResolver resolver, String
            name) {

        if (resolver == null) {
            Log.w(TAG, "getParameter(" + name + "): resolver is null");
            return null;
        }
        if (name == null) {
            Log.w(TAG, "getParameter: paramater name = null");
            return null;
        }
        String[] projection = new String[]{
                "current_value"
        };
        String selection = "name" + "=?";
        String[] selectionArgs = new String[]{
                name
        };
        Cursor cursor = null;
        try {
            if (DEBUG_DB) {
                dumpDB(resolver);
            }
            cursor = resolver.query(CONTENT_URI, projection, selection, selectionArgs, null);
        } catch (Exception e) { // todo: generic exception is discouraged
            Log.e(TAG, "getParameter(" + name + "): exception thrown in resolver.query()");
        }
        if (cursor == null) {
            Log.w(TAG, "getParameter(" + name + "): query failed");
            return null;
        }

        if (cursor.getCount() < 1) {
            Log.e(TAG, "getParameter(" + name + "): parameter does not exist in database.");
            cursor.close();
            return null;
        }

        if (cursor.getCount() > 1) {
            Log.w(TAG, "getParameter(" + name + "): multiple entries found");
        }

        // the database schema should only result in a single row of results
        if (!cursor.moveToFirst()) {
            Log.w(TAG, "unable to get data for " + name);
            cursor.close();
            return null;
        }

        int valueColumnIndex = cursor.getColumnIndex("current_value");
        if (valueColumnIndex == -1) {
            Log.w(TAG, "No value exists for " + name);
            cursor.close();
            return null;
        }

        String value = cursor.getString(valueColumnIndex);

        Log.v(TAG, "getParameter(" + name + ")=" + value);
        cursor.close();

        return value;

    }

    /**
     * Method used to dump database
     *
     * @param resolver content resolver needed for database
     */
    private static void dumpDB(ContentResolver resolver) {
        Cursor cursor = null;

        try {
            cursor = resolver.query(CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                Log.d(TAG, cursor.getColumnNames().toString());
            }
            StringBuilder row = new StringBuilder();
            while (cursor != null && cursor.moveToNext()) {
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    row.append(cursor.getColumnName(i)).append("=").append(cursor.getString(i)).append("|");
                }
                row.append("\r\n");
                Log.v(TAG, row.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Dump database fail: ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static class VantageDBObserver extends ContentObserver {

        private final Runnable mCallback;
        private final Handler mHandler;
        private final Uri mUri;

        /**
         * Creates a content observer.
         *
         * @param handler   The handler to run {@link #onChange} on, or null if none.
         * @param callback  callback to run when change in parameter occur
         * @param parameter the DB parameter to observe
         */
        public VantageDBObserver(Handler handler, Runnable callback, String parameter) {
            super(handler);
            mHandler = handler;
            mCallback = callback;
            mUri = getUriFor(parameter);
        }


        /**
         * This method is called when a content change occurs.
         * <p>
         * Subclasses should override this method to handle content changes.
         * </p>
         *
         * @param selfChange True if this is a self-change notification.
         */
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        /**
         * This method is called when a content change occurs.
         * Includes the changed content Uri when available.
         * <p>
         * Subclasses should override this method to handle content changes.
         * To ensure correct operation on older versions of the framework that
         * did not provide a Uri argument, applications should also implement
         * the {@link #onChange(boolean)} overload of this method whenever they
         * implement the {@link #onChange(boolean, Uri)} overload.
         * </p><p>
         * Example implementation:
         * <pre><code>
         * // Implement the onChange(boolean) method to delegate the change notification to
         * // the onChange(boolean, Uri) method to ensure correct operation on older versions
         * // of the framework that did not have the onChange(boolean, Uri) method.
         * {@literal @Override}
         * public void onChange(boolean selfChange) {
         *     onChange(selfChange, null);
         * }
         *
         * // Implement the onChange(boolean, Uri) method to take advantage of the new Uri argument.
         * {@literal @Override}
         * public void onChange(boolean selfChange, Uri uri) {
         *     // Handle change.
         * }
         * </code></pre>
         * </p>
         *
         * @param selfChange True if this is a self-change notification.
         * @param uri        The Uri of the changed content, or null if unknown.
         */
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "uri is " + (uri == null ? "NULL" : uri.toString()));
            if (mCallback != null && mHandler != null) {
                mHandler.post(mCallback);
            }
        }

        public Uri getUri() {
            return mUri;
        }
    }
}
