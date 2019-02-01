package com.avaya.android.vantage.basic.csdk;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseArray;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor;
import com.avaya.android.vantage.basic.model.ContactData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.avaya.android.vantage.basic.Constants.IPO_CONTACT_TYPE;

/**
 * This class is used to get all local contacts from android device. All contacts are added in
 * ContactData List using Loader. First we load contact address information, store it by contact ID in
 * sparse array, than we load contacts firs and last name and store it in Sparse array, and at the end,
 * we load other information. We do this because all those information is held in different databases
 * and we cannot load them with one cursor.
 */

public class ContactsLoader implements LoaderManager.LoaderCallbacks<Cursor> {

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case Constants.LOCAL_CONTACTS_LOADER: // loading contact basic information
                return new CursorLoader(mActivity, ContactsContract.Contacts.CONTENT_URI,
                        null, null, null, null);
            case Constants.LOCAL_NAME_LOADER: // loading first and last name
                String whereName = ContactsContract.Data.MIMETYPE + " = ?";
                String[] whereNameParams = new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};
                return new CursorLoader(mActivity, ContactsContract.Data.CONTENT_URI, null, whereName, whereNameParams, null);
            case Constants.LOCAL_ADDRESS_LOADER: // loading addresses
                return new CursorLoader(mActivity, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                        null, null, null, null);
        }

        return null;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor cursor) {
        Log.d(TAG, "ContactLoad Start. Loader ID: " + loader.getId());
        if(cursor == null) {
            Log.e(TAG, "null cursor - probably no contacts db exist");
            return;
        }
        switch (loader.getId()) {
            case Constants.LOCAL_CONTACTS_LOADER:
                mUIContactsViewAdaptor.setLocalContacts(getContactData(cursor));
                mUIContactsViewAdaptor.setIpoContacts(mIpoDataList);
                mUIContactsViewAdaptor.setLocalFavorites(mFavoriteDataList);
                break;
            case Constants.LOCAL_NAME_LOADER:
                getContactName(cursor);
                break;
            case Constants.LOCAL_ADDRESS_LOADER:
                getContactsAddresses(cursor);
        }
        Log.d(TAG, "ContactLoad: END " + loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset: ");
    }

    // constants
    private static final String TAG = "ContactsLoader";
    private static final String NAME_SORT_PREFERENCE = Constants.NAME_SORT_PREFERENCE;
    private static final String USER_PREFERENCE = Constants.USER_PREFERENCE;
    private static final int FIRST_NAME_FIRST = Constants.FIRST_NAME_FIRST;
    private static final int LAST_NAME_FIRST = Constants.LAST_NAME_FIRST;
    // activity we will use for weak reference
    private Activity mActivity;
    private WeakReference<Activity> weakContext;
    // other objects
    private List<ContactData> mContactDataList = new ArrayList<>();
    private List<ContactData> mFavoriteDataList = new ArrayList<>();
    private List<ContactData> mIpoDataList = new ArrayList<>();
    private String mFirstName = "";
    private String mLastName = "";
    private SharedPreferences mUserPreference;
    private UIContactsViewAdaptor mUIContactsViewAdaptor;
    // sparse arrays used to store contact data by contact ID
    private SparseArray<String> mapAddr = new SparseArray<>();
    private SparseArray<String[]> mapName = new SparseArray<>();

    /**
     * Constructor
     *
     * @param activity - Loader needs activity, so we pass it here
     */
    public ContactsLoader(Activity activity) {
        weakContext = new WeakReference<>(activity);
        this.mActivity = activity;
        mUserPreference = activity.getSharedPreferences(USER_PREFERENCE, MODE_PRIVATE);
    }

    /**
     * Setting up uiContactsViewAdaptor for this class. We need this to push contact information to recyclerViews
     *
     * @param uiContactsViewAdaptor adaptor received from main Activity
     */
    public void setUIContactsViewAdaptor(UIContactsViewAdaptor uiContactsViewAdaptor) {
        this.mUIContactsViewAdaptor = uiContactsViewAdaptor;
    }

    /**
     * this method will get all information from the local contacts and add it to mContactDataList
     */
    private List<ContactData> getContactData(Cursor cur) {
        int defaultValue = Constants.LAST_NAME_FIRST;
        String adminSortOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_SORT_ORDER);
        if (adminSortOrder != null) {
            // doing this to prevent bug in case someone entered a value that is different from required values.
            if (adminSortOrder.equals("first,last")) {
                defaultValue = FIRST_NAME_FIRST;
            } else {
                defaultValue = LAST_NAME_FIRST;
            }
        }

        // making sure we take admin settings in consideration
        if (mUserPreference.contains(NAME_SORT_PREFERENCE)) {
            Log.d(TAG, "User has already set name sort preference, so we will not change it");
        } else {
            Log.d(TAG, "No settings found for name sort, so we take admin settings in consideration. "
                    + "New admin settings are " + adminSortOrder);
        }
        int nameSort = mUserPreference.getInt(NAME_SORT_PREFERENCE, defaultValue);


        mContactDataList.clear();
        mFavoriteDataList.clear();
        mIpoDataList.clear();
        // making sure our activity is alive. Otherwise, no need to download anything
        if (weakContext.get() == null) {
            return mContactDataList;
        }

        if (cur != null && cur.getCount() > 0) {
            while (cur.moveToNext()) {
                // getting display name
                String displayName = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                // getting photo
                String mPhotoThumbnailURI = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
                String mPhotoURI = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));

                // getting ID
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));

                // getting URI
                Uri uri = Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_URI, String.valueOf(id));
                String mURI = uri.toString();

                // making sure we add only contacts who have phone numbers
                if (Integer.parseInt(cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                    // setting up first, last name and account type
                    String accountTupe = "";
                    String[] fullName = mapName.get(Integer.parseInt(id));
                    if (fullName != null) {
                        mFirstName = fullName[0].trim();
                        mLastName = fullName[1].trim();
                        accountTupe = fullName[3];
                    }else{
                        accountTupe = "com.android.bluetooth.pbapsink";
                    }

                    // in case we have first and last name empty, we will use nickname to generate first and last name
                    if (mFirstName.trim().length() == 0 && mLastName.trim().length() == 0 && displayName.trim().length() > 0) {
                        String nameArray[] = displayName.split(" ", 2);
                        // adding first name
                        if (nameArray.length > 0) {
                            mFirstName = nameArray[0];
                        }
                        // adding last name
                        if (nameArray.length > 1) {
                            mLastName = nameArray[1];
                        }
                    }

                    // getting name sort preference
                    String mName;
                    if (nameSort == FIRST_NAME_FIRST) {
                        mName = Utils.combinedName(weakContext.get(), mFirstName, mLastName);
                    } else {
                        mName = Utils.combinedName(weakContext.get(), mLastName, mFirstName);
                    }

                    // getting is favorite (1 is yes, 0 is no)
                    boolean mIsFavorite = Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.STARRED))) != 0;

                    // getting phone numbers
                    List<ContactData.PhoneNumber> phoneNumbers = new LinkedList<>();

                    // getting address
                    String mLocation = mapAddr.get(Integer.parseInt(id));
                    if (mLocation == null || mLocation.trim().length() < 1) {
                        mLocation = "";
                    }

                    // creating contact data
                    String mCity = "";
                    String mCompany = "";
                    String mPosition = "";
                    ContactData cdata = new ContactData(
                            mName,
                            mFirstName,
                            mLastName,
                            null,
                            mIsFavorite,
                            mLocation,
                            mCity,
                            mPosition,
                            mCompany,
                            phoneNumbers,
                            accountTupe.equals(IPO_CONTACT_TYPE) ? ContactData.Category.IPO : ContactData.Category.LOCAL,
                            id,
                            mURI,
                            mPhotoThumbnailURI,
                            true,
                            "",
                            mPhotoURI,
                            accountTupe,
                            "",
                            "");
                    // clearing up first and last name to prevent bug in case next contact does not have one of those
                    mFirstName = "";
                    mLastName = "";

                    if (cdata.mCategory == ContactData.Category.IPO){
                        mIpoDataList.add(cdata);
                    } else {
                        mContactDataList.add(cdata);
                        if (mIsFavorite){
                            mFavoriteDataList.add(cdata);
                        }
                    }

                } else {
                    Log.d(TAG, "getContactData: " + displayName + " does not have a phone number.");
                }
            }
        }
        return mContactDataList;
    }

    /**
     * Getting contact first, last, middle name and account type
     *
     * @param nameCur cursor we get from cursor loader
     */
    private void getContactName(Cursor nameCur) {
        mapName.clear();
        String firstName, lastName, middleName, accountType;
        int contactId;
        while (nameCur.moveToNext()) {
            contactId = nameCur.getInt(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID));
            firstName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
            if (firstName == null) firstName = "";
            lastName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
            if (lastName == null) lastName = "";
            middleName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
            if (middleName == null) middleName = "";
            accountType = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.ACCOUNT_TYPE_AND_DATA_SET));
            if(accountType == null) accountType = "";
            mapName.put(contactId, new String[]{firstName, lastName, middleName, accountType});
        }
        // names are loaded, now we need to load other contact information
        mActivity.getLoaderManager().restartLoader(Constants.LOCAL_CONTACTS_LOADER, null, this);
    }

    /**
     * getting all contacts addresses
     *
     * @param cursor we get this from loader
     */
    private void getContactsAddresses(Cursor cursor) {
        mapAddr.clear();
        while (cursor.moveToNext()) {
            Integer contactID = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID));
            String address = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
            mapAddr.put(contactID, address);
        }
        // addresses are loaded, now we need to load names
        mActivity.getLoaderManager().restartLoader(Constants.LOCAL_NAME_LOADER, null, this);
    }

}
