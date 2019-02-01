package com.avaya.android.vantage.basic.csdk;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.ArraySet;
import android.util.Log;

import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.model.ContactData;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to provide contact information from the URI or ID. All methods are declared
 * static in order to simplify usage. We also need context for content resolver
 */

public class LocalContactInfo {

    // static phone types. These are results we get from google contacts. Used in getType method
    private static final int MOBILE = 2;
    private static final int WORK = 3;
    private static final int HOME = 1;
    private static final int MAIN = 12;
    private static final int WORK_FAX = 4;
    private static final int HOME_FAX = 5;
    private static final int PAGER = 6;
    private static final int OTHER = 7;
    private static final String TAG = "GetLocalContactsInfo";
    private static final String EMPTY_STRING = "";

    /**
     * Method used to get all phone numbers for specific contact. Contact is identified by URI.
     *
     * @param uri     contact URI
     * @param context context needed for contentResolver
     * @return list oh phone numbers
     */
    public static List<ContactData.PhoneNumber> getPhoneNumbers(Uri uri, Context context) {
        List<ContactData.PhoneNumber> phoneNumbersAll = new ArrayList<>();

        if (context != null) {
            Cursor phonesCursor = null;

            try {
                phonesCursor = context.getContentResolver().query(uri, null, null, null, null);
                //noinspection ConstantConditions
                if (uri != null && phonesCursor != null) {

                    if (phonesCursor.moveToNext()) {
                        int columnIndex_ID = phonesCursor.getColumnIndex(ContactsContract.Contacts._ID);
                        String contactID = phonesCursor.getString(columnIndex_ID);

                        int columnIndex_HASPHONENUMBER = phonesCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                        String stringHasPhoneNumber = phonesCursor.getString(columnIndex_HASPHONENUMBER);

                        if (stringHasPhoneNumber.equalsIgnoreCase("1")) {
                            Cursor cursorNum = null;
                            try {
                                cursorNum = context.getContentResolver().query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        null,
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactID,
                                        null,
                                        null);

                                if (cursorNum != null) {
                                    while (cursorNum.moveToNext()) {
                                        int columnIndex_number = cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                                        int columnIndex_type = cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                                        int columnIndex_id = cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);
                                        int columnIndex_primary = cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY);
                                        String phoneNumber = cursorNum.getString(columnIndex_number);
                                        Integer phoneType = cursorNum.getInt(columnIndex_type);
                                        String phoneId = cursorNum.getString(columnIndex_id);
                                        boolean isPrimary = cursorNum.getInt(columnIndex_primary) != 0;
                                        ContactData.PhoneNumber number = new ContactData.PhoneNumber(phoneNumber, getType(phoneType), isPrimary, phoneId);
                                        phoneNumbersAll.add(number);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to retrieve phone numbers: ", e);
                            } finally {
                                if (cursorNum != null) {
                                    cursorNum.close();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve contact phone numbers: ", e);
            } finally {
                if (phonesCursor != null) {
                    phonesCursor.close();
                }
            }
        }
        return phoneNumbersAll;
    }

    /**
     * This method receives information about phone type from google contact and converts it to
     * our phone number type. Static object definitions can be found on top of this class
     *
     * @param type ID of google phone number type
     * @return our phone number type
     */

    public static ContactData.PhoneType getType(int type) {

        switch (type) {
            case HOME:
                return ContactData.PhoneType.HOME;
            case MOBILE:
                return ContactData.PhoneType.MOBILE;
            case WORK:
                return ContactData.PhoneType.WORK;
            case WORK_FAX:
                return ContactData.PhoneType.FAX;
            case HOME_FAX:
                return ContactData.PhoneType.FAX;
            case PAGER:
                return ContactData.PhoneType.PAGER;
            case OTHER:
                return ContactData.PhoneType.OTHER;
            default:
                return ContactData.PhoneType.OTHER;
        }
    }

    /**
     * Method used to get contact information (Company name and Position).
     *
     * @param contactID contact ID
     * @param context   context needed for contentResolver
     * @return two string are returned. String[0] Company Name, String[1] Position. In case those
     * are null, we just return empty string.
     */
    public static String[] getCompanyInfo(String contactID, Context context) {
        String orgName = "";
        String title = "";
        ContentResolver cr = context.getContentResolver();
        String selection = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
        String[] selectionArgs = new String[]{contactID,
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};
        Cursor companyCursor = null;

        try {
            companyCursor = cr.query(ContactsContract.Data.CONTENT_URI,
                    null, selection, selectionArgs, null);
            while (companyCursor != null && companyCursor.moveToNext()) {
                orgName = companyCursor.getString(companyCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DATA));
                title = companyCursor.getString(companyCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get company info: ", e);
        } finally {
            if (companyCursor != null) {
                companyCursor.close();
            }
        }

        if (orgName == null) orgName = "";
        if (title == null) title = "";
        return new String[]{orgName, title};
    }

    /**
     * Method used to get contact address
     *
     * @param id      contact unique ID
     * @param context context needed for content resolved
     * @return contact address
     */
    public static String getContactAddress(String id, Context context) {
        String address = "";
        ContentResolver cr = context.getContentResolver();
        String selection = ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = ? AND "
                + ContactsContract.CommonDataKinds.StructuredPostal.MIMETYPE + " = ?";
        String[] selectionArgs = new String[]{id,
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE};
        Cursor addressCursor = null;

        try {
            addressCursor = cr.query(ContactsContract.Data.CONTENT_URI,
                    null, selection, selectionArgs, null);
            while (addressCursor != null && addressCursor.moveToNext()) {
                address = addressCursor.getString(addressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve contact address: ", e);
        } finally {
            if (addressCursor != null) {
                addressCursor.close();
            }
        }
        return address;
    }

    /**
     * Getting contact ID
     *
     * @param contactUri contact unique URI
     * @param context    context needed for contentResolver
     * @return returning uniqueID
     */
    public static String getContactID(Uri contactUri, Context context) {
        Cursor cursor = null;
        String id = null;

        try {
            cursor = context.getContentResolver().query(contactUri, null, null, null, null);
            int idx;
            if (cursor != null && cursor.moveToFirst()) {
                idx = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                id = cursor.getString(idx);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to get contact ID: ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return id;
    }

    /**
     * Method used to get contact photo URI and photo thumbnail URI
     *
     * @param contactUri contact unique URI
     * @param context    context needed for contentResolver
     * @return returning photo thumbnail and photo URI's
     */
    public static String[] getContactPhotoURI(Uri contactUri, Context context) {
        int idPhotoThumbUri, idPhotoUri;
        String photoThumbnailURI = "";
        String photoURI = "";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(contactUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                idPhotoThumbUri = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
                idPhotoUri = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI);
                photoThumbnailURI = cursor.getString(idPhotoThumbUri);
                photoURI = cursor.getString(idPhotoUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve contact photo URI: ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return new String[]{photoThumbnailURI, photoURI};
    }

    /**
     * Method is used to get contact information by the number. We use it mostly to identify incoming call
     * or to display information in call log.
     *
     * @param number  phone number
     * @param context context needed for content resolved
     * @return name, firstName, lastName, photoThumbnailURI, contactId, contactUri and photoURI
     */
    public static String[] getContactInfoByNumber(String number, Context context) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Uri contactUri = Uri.parse("");
        String name = "";
        String firstName = "";
        String lastName = "";
        String contactId = "";
        String photoThumbnailURI = "";
        String photoURI = "";
        String accountType = "";
        Cursor contactLookup = null;

        try {
            contactLookup = context.getContentResolver().query(uri, new String[]{BaseColumns._ID,
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.Data.PHOTO_THUMBNAIL_URI,
                    ContactsContract.Data.PHOTO_URI
            }, null, null, null);
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                photoThumbnailURI = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.PHOTO_THUMBNAIL_URI));
                photoURI = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.PHOTO_URI));
                contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }
        if (contactId != null && contactId.trim().length() > 0) {
            String selection = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + " = ?";
            String[] selectionArgs = new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, contactId};
            Cursor nameCur = null;

            try {
                nameCur = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, selection, selectionArgs, null);
                if (nameCur != null) {
                    while (nameCur.moveToNext()) {
                        firstName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                        if (firstName == null) firstName = "";
                        lastName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                        if (lastName == null) lastName = "";
                        accountType = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.ACCOUNT_TYPE_AND_DATA_SET));
                        if (accountType == null) accountType = "";

                        if (nameCur.getCount() > 1) {
                            firstName = getFirstNameFromMergedContacts(SDKManager.getInstance().getContactsAdaptor().getLocalContacts());
                            break;
                        }
                    }
                }
                contactUri = Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_URI, String.valueOf(contactId));
            } catch (Exception e) {
                Log.e(TAG, "Failed to get display name: ", e);
            } finally {
                if (nameCur != null) {
                    nameCur.close();
                }
            }

        } else {
            firstName = "";
            lastName = "";
        }

        return new String[]
                {name, firstName, lastName, photoThumbnailURI, contactId, contactUri.toString(), photoURI, accountType};
    }

    /**
     * If contacts were merged Android framework database returns 2 rows for
     * particular contact id. We should show value that is used in contact list
     * after merging.
     *
     * @param mergedContacts
     * @return
     */
    private static String getFirstNameFromMergedContacts(List<ContactData> mergedContacts) {
        if (mergedContacts == null) {
            return EMPTY_STRING;
        }
        return mergedContacts.get(0).mFirstName;
    }

    /**
     * This method gets first and last name of a contact
     *
     * @param contactID contact unique ID
     * @param context   context needed for content resolver
     * @return string array containing first and last name
     */
    private static String[] getFirstAndLastName(String contactID, Context context) {
        String selection = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, contactID};
        String firstName = "";
        String lastName = "";
        Cursor nameCur = null;
        try {
            nameCur = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, selection, selectionArgs, null);
            while (nameCur != null && nameCur.moveToNext()) {
                firstName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                if (firstName == null) firstName = "";
                lastName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                if (lastName == null) lastName = "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get first and last name: ", e);
        } finally {
            if (nameCur != null) {
                nameCur.close();
            }
        }
        return new String[]{firstName, lastName};
    }

    /**
     * Method is used to get favorite status of local contact
     *
     * @param contactUri contact location
     * @param context    context needed for content resolver
     * @return contact favorite status
     */
    public static boolean getFavoriteStatus(Uri contactUri, Context context) {
        Cursor cursor = null;
        boolean isFavorite = false;
        if (context != null) {
            try {
                cursor = context.getContentResolver().query(contactUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    isFavorite = Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.STARRED))) != 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return isFavorite;
    }

    /**
     * Method is used to get phone type by phone number
     *
     * @param phoneNumber search query
     * @param context     context needed for content resolver
     * @return returning contact phoneType
     */
    public static ContactData.PhoneType getPhoneTypeByNumber(String phoneNumber, Context context) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Integer phoneType = 7;

        ContentResolver contentResolver = context.getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[]{
                ContactsContract.PhoneLookup.TYPE
        }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                phoneType = contactLookup.getInt(contactLookup.getColumnIndex(ContactsContract.PhoneLookup.TYPE));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }
        return getType(phoneType);
    }

    /**
     * Method will get phone number search query and return first result matching contact number
     *
     * @param searchQuery phone number search query
     * @return String [] containing phone number and cotnact name
     */
    public static String[] phoneNumberSearch(String searchQuery) {
        if (ElanApplication.getContext() != null) {
            ContentResolver con = ElanApplication.getContext().getContentResolver();
            String selection = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?";
            String number, rawNumber;
            String name;
            String photoURI;
            Integer type;
            String[] selectionArgs = new String[]{ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, "%"+searchQuery+"%"};
            Cursor nameCur = null;

            try {
                nameCur = con.query(ContactsContract.Data.CONTENT_URI, null, selection, selectionArgs, ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (nameCur != null && nameCur.moveToNext()) {
                    rawNumber = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    name = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    photoURI = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                    type = nameCur.getInt(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    number = rawNumber.replaceAll("\\D+", "");
                    if (photoURI == null) {
                        photoURI = "";
                    }
                    if (number != null) {
                        if (number.contains(searchQuery)) {
                            if (name != null && name.trim().length() > 0) {
                                return new String[]{name, rawNumber, getType(type).toString(), photoURI}; // extracting digits
                            } else {
                                nameCur.close();
                                return null;
                            }
                        }
                    }

                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get phone number: ", e);
            } finally {
                if (nameCur != null) {
                    nameCur.close();
                }
            }
        }
        return new String[]{"", ""};
    }

    /**
     * Method used to search contacts database. At the moment we use it to search contacts phone numbers
     * but this can be used for general search
     *
     * @param searchQuery number
     * @param context     application context needed for content resolver
     * @return returning list of contact data with search results
     */
    public static List<ContactData> search(String searchQuery, Context context, ArraySet<String> contactsAlreadyAdded) {
        List<ContactData> searchResults = new ArrayList<>();

        Uri contentUri = Uri.withAppendedPath(ContactsQuery.FILTER_URI, Uri.encode(searchQuery));
        String name;
        String photoURI;
        String photoThumbnailURI;
        String id;
        String uri;
        boolean mIsFavorite;

        ContentResolver contentResolver = context.getContentResolver();
        Cursor contactLookup = null;

        try {

            contactLookup = contentResolver.query(contentUri, ContactsQuery.PROJECTION,
                    ContactsQuery.SELECTION, null, null);

            if (contactLookup != null && contactLookup.getCount() > 0) {
                while (contactLookup.moveToNext()) {

                    if (Integer.parseInt(contactLookup.getString(
                            contactLookup.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                        name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                        photoURI = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                        photoThumbnailURI = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));
                        id = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data._ID));
                        uri = Uri.withAppendedPath(
                                ContactsContract.Contacts.CONTENT_URI, String.valueOf(id)).toString();
                        mIsFavorite = Integer.parseInt(contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Contacts.STARRED))) != 0;
                        String[] fullName = getFirstAndLastName(id, context);
                        if (!contactsAlreadyAdded.contains(id)) {
                            searchResults.add(new ContactData(name, fullName[0], fullName[1], null, mIsFavorite,
                                    getContactAddress(id, context), "", "", "", null,
                                    ContactData.Category.LOCAL, id, uri, photoThumbnailURI, true, "", photoURI, "", "", ""));
                            contactsAlreadyAdded.add(id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get serach query: ", e);
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }
        return searchResults;
    }

    /**
     * Get contact type (Local or IPOffice)
     * @param context context needed for Content Resolver
     * @param contactID unique contact ID
     * @return contact type
     */
    public static int getAccountInfo(Context context, String contactID){
        final int LOCAL_CONTACT = 0;
        final int IPO_CONTACT = 2;
        Uri uri = ContactsContract.RawContacts.CONTENT_URI;
        String[] projection = new String[]{
                ContactsContract.RawContacts.ACCOUNT_NAME,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.CONTACT_ID,
                ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
        };
        String selection = ContactsContract.RawContacts.CONTACT_ID + "=?";
        String[] selectionArgs = new String[]{contactID};

        if (context != null){
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()){
                    String accountname = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY));
                    if (accountname.toLowerCase().contains("ipo")){
                        return IPO_CONTACT;
                    } else {
                        return LOCAL_CONTACT;
                    }
                }
            } catch (Exception e){
                Log.e(TAG, "Could not get account info: ", e);
            } finally {
                if (cursor != null){
                    cursor.close();
                }
            }
        }
        return LOCAL_CONTACT;
    }

    /**
     * interface used for search query
     */
    private interface ContactsQuery {

        Uri FILTER_URI = ContactsContract.Contacts.CONTENT_FILTER_URI;

        @SuppressLint("InlinedApi")
        String SELECTION =
                (ContactsContract.Contacts.DISPLAY_NAME) +
                        "<>''" + " AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1";

        @SuppressLint("InlinedApi")
        String SORT_ORDER =
                ContactsContract.Contacts.DISPLAY_NAME;

        @SuppressLint("InlinedApi")
        String[] PROJECTION = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.PHOTO_URI,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                ContactsContract.Contacts.STARRED,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                SORT_ORDER,
        };
    }

}

