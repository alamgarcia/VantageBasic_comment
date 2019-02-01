package com.avaya.android.vantage.basic.csdk;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.model.ContactData;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.provider.ContactsContract.AUTHORITY;

/**
 * This class is used to make changes to local contacts.
 */

public class LocalContactsManager {

    private static final String TAG = "LocalContactsManager";

    private static final int TYPE_HOME = 1;
    private static final int TYPE_MOBILE = 2;
    private static final int TYPE_WORK = 3;
    private static final int TYPE_FAX_WORK = 4;
    private static final int TYPE_PAGER = 6;
    private static final int TYPE_OTHER = 7;
    private static final int TYPE_MAIN = 12;
    private static final int TYPE_ASSISTANT = 19;

    private WeakReference<Context> mContextReference;

    /**
     * constructor
     *
     * @param context context is needed for contentResolver
     */
    public LocalContactsManager(Context context) {
        mContextReference = new WeakReference<>(context);
    }

    /**
     * Updating contacts
     *
     * @param contactData contact information
     */
    public void updateContact(final ContactData contactData, final Uri imageUri) {
        // just checking if URI is OK
        if (contactData.mURI == null || contactData.mURI.trim().length() < 1) {
            return;
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // getting much needed ID and RawId
                String contactID = getContactID(contactData.mURI);

                if (contactID == null) {
                    Log.e(TAG, "Contact ID not found");
                    return;
                }
                int rawId = getContactRawID(contactID);
                String contactRawId = "";
                if (rawId != -1) {
                    contactRawId = Integer.toString(rawId);
                }
                // for IPO, we only support one phone number, so we update only first one... if it exist
                if (contactData.mCategory == ContactData.Category.IPO){
                    if (contactData.mPhones != null && contactData.mPhones.size() > 0) {
                        setContactName(contactID, contactData);
                        updatePhone(contactID, contactData.mPhones.get(0).phoneNumberId, contactData.mPhones.get(0).Number, TYPE_WORK, true);
                    }
                } else {
                    //executing changes
                    updateContactPhones(contactData.mURI, contactID, contactRawId, contactData.mPhones);
                    setContactName(contactID, contactData);
                    setAsFavorite(contactID, contactData.isFavorite());
                    updateCompanyInfo(contactID, contactRawId, contactData.mCompany, contactData.mPosition);
                    updateContactAddress(contactRawId, contactData.mLocation);
                    updateContactImage(contactID, imageUri);
                }
            }
        });

    }

    /**
     * Method used to update contact address
     *
     * @param rawId      contact unique RawID
     * @param newAddress new contact address
     */
    private void updateContactAddress(String rawId, String newAddress) {

        if (getAddressInfo(rawId, newAddress)) {

            try {
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI);

                String orgWhere = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
                String[] orgWhereParams = new String[]{rawId,
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE};

                builder.withSelection(orgWhere, orgWhereParams);
                builder.withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, newAddress);
                ops.add(builder.build());

                if (mContextReference != null && mContextReference.get() != null) {
                    mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
                }
            } catch (Exception e) {
                Log.e(TAG, "Update contact address failed", e);
            }
        }
    }

    /**
     * getting contact address info. If we do not find any info, we add dummy data so we can use
     * update option. Otherwise, changes would not take effect
     *
     * @param rawId      contact unique rawId
     * @param newAddress contact new address
     * @return should we update at all?
     */
    private boolean getAddressInfo(String rawId, String newAddress) {
        String oldAddress = null;
        boolean shouldUpdate = false;
        if (mContextReference != null && mContextReference.get() != null) {
            ContentResolver cr = mContextReference.get().getContentResolver();
            String orgWhere = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] orgWhereParams = new String[]{rawId,
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE};

            Cursor orgCur = cr.query(ContactsContract.Data.CONTENT_URI,
                    null, orgWhere, orgWhereParams, null);
            if (orgCur != null) {
                try {
                    while (orgCur.moveToNext()) {
                        oldAddress = orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getAddressInfo failed", e);
                } finally {
                    orgCur.close();
                }
            }

            if (oldAddress == null && newAddress.trim().length() > 0) {
                addDummyData(rawId, "address");
                oldAddress = "m";
            }
            if (oldAddress != null && !oldAddress.equals(newAddress)) {
                shouldUpdate = true;
            }
        }

        return shouldUpdate;
    }

    /**
     * Method used to add contact to favorites
     *
     * @param id         Contact Unique ID
     * @param isFavorite Contact favorite value (true: add to favorite, false remove from favorite)
     */
    public void setAsFavorite(String id, Boolean isFavorite) {
        String setFav = isFavorite ? "1" : "0";
        ContentValues values = new ContentValues();
        String[] fv = new String[]{id};
        values.put(ContactsContract.Contacts.STARRED, setFav);
        if (mContextReference != null && mContextReference.get() != null) {
            mContextReference.get().getContentResolver().update(ContactsContract.Contacts.CONTENT_URI, values,
                    ContactsContract.Contacts._ID + "= ?", fv);
        }
    }

    /**
     * Changing contact first and last name
     *
     * @param id       contact unique ID
     *
     */
    private void setContactName(String id, ContactData contact) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        // Name
        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI);
        builder.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + "=?",
                new String[]{id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
                /*  Avaya model has [DisplayName, FirstName and LastName] this means that:
                 *  - there is an issue of handling other name parts [NamePrefix, MiddleName and NameSuffix]
                 *    for Google Contacts model has [DisplayName, NamePrefix, FirstName, MiddleName, LastName, NameSuffix]
                 *    and Google Contacts can add/edit/remove those additional fields.
                 */
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.mFirstName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.mLastName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.mName);
        ops.add(builder.build());
        if (mContextReference != null && mContextReference.get() != null) {
            try {
                mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
            } catch (Exception e) {
                Log.e(TAG, "setContactName failed", e);
            }
        }
    }

    /**
     * Setup new contact photo
     *
     * @param contactID Contact unique ID
     * @param imageUri  Image Uri
     */
    private void updateContactImage(String contactID, Uri imageUri) {
        if (imageUri != null && mContextReference != null && mContextReference.get() != null) {

            String selection = ContactsContract.Data.CONTACT_ID + " = ?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] selectionArgs = new String[]{String.valueOf(contactID), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE};

            if (imageUri.toString().trim().length() > 0) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContextReference.get().getContentResolver(), imageUri);
                    //Perform check if we have to check for image rotation or not. If there is no path in database
                    //image is most likely provided directly from CSDK and we can ignore image rotation
                    bitmap = Utils.checkAndPerformBitmapRotation(bitmap, mContextReference.get().getContentResolver(), imageUri);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                    ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(selection, selectionArgs)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, outputStream.toByteArray())
                            .build());
                    // check again for context since compression can be long
                    if (mContextReference != null && mContextReference.get() != null) {
                        mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "failed to update contact image", e);
                }
            } else {
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(selection, selectionArgs)
                        .build());
                try {
                    mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to delete an image: ", e);
                }
            }
        }
    }

    /**
     * Deletes all contact phones from specific contact
     *
     * @param id contact unique URI
     */
    private void deleteContactPhone(String id, String phoneId) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        String selection =
                ContactsContract.Data.CONTACT_ID + "=?" + " AND "
                        + ContactsContract.Data.MIMETYPE + "=?" + " AND "
                        + ContactsContract.CommonDataKinds.Phone._ID + "=?";
        String selectionArgs[] = {
                String.valueOf(id),
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                phoneId};

        ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI);
        builder.withSelection(selection, selectionArgs);
        ops.add(builder.build());

        // Update
        if (mContextReference != null && mContextReference.get() != null) {
            try {
                mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
                Log.d(TAG, "Phone deleted. ID: " + phoneId);
            } catch (Exception e) {
                Log.e(TAG, "deleteContactPhone failed", e);
            }
        }
    }

    /**
     * Adds contact phone to specific contact.
     *
     * @param rawID       contact unique raw ID
     * @param phoneNumber contact phone that needs to be added
     * @param type        Phone number type to be added
     */
    private void addContactPhone(String rawID, String phoneNumber, ContactData.PhoneType type, boolean primary) {
        int primarySet = 0;
        if (primary) {
            primarySet = 1;
        }

        int phoneType = convertPhoneType(type);

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawID)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, primarySet)
                .build());

        // Update
        if (mContextReference != null && mContextReference.get() != null) {
            try {
                mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
                Log.d(TAG, "Phone added. Number: " + phoneNumber);
            } catch (Exception e) {
                Log.e(TAG, "addContactPhone failed", e);
            }
        }
    }

    /**
     * we use this method to conver out phone type to Google phone type
     *
     * @param type our phone type
     * @return google phone type
     */
    private int convertPhoneType(ContactData.PhoneType type) {
        switch (type) {
            case WORK:
                return TYPE_WORK;
            case MOBILE:
                return TYPE_MOBILE;
            case HOME:
                return TYPE_HOME;
            case FAX:
                return TYPE_FAX_WORK;
            case PAGER:
                return TYPE_PAGER;
            case ASSISTANT:
                return TYPE_ASSISTANT;
            default:
                return TYPE_OTHER;
        }
    }

    /**
     * This method gets updates contacts phone numbers. We get contact URI and new list of phone
     * numbers and update contact data on android device. Logic behind this is to get new list of
     * phone numbers and compare list sizes. Then we do a search by phone number ID and update
     * that phone number information.
     * In case list sizes are the same, we just update old information
     * In case lists sizes are different, we have another logic
     * 1. we search through new phone number list and add all phone numbers that do not have ID.
     * By logic, if phone number does not have ID, than it is new phone number.
     * 2. Than we compare old and new list. Search if there is any phone number ID that is removed.
     * If there is, we just delete that phone number
     * 3. Compare old and new list and check what ID's are matching, and just pass new information
     * to the content provider (update phone number information)
     *
     * @param uri           contact unique URI
     * @param contactId     contact unique ID
     * @param newPhonesList new list of phone numbers
     */
    private void updateContactPhones(String uri, String contactId, String rawId, List<ContactData.PhoneNumber> newPhonesList) {

        // just doing necessary checks to avoid crashes
        if (newPhonesList == null) return;

        List<ContactData.PhoneNumber> oldPhoneList = getPhoneNumbers(uri); // we get old phone list so we can update it
        int oldSize = oldPhoneList.size();
        int newSize = newPhonesList.size();

        // easiest option. Lists are the same, so we just update phone IDs with new information
        if (oldSize == newSize) {
            Log.i(TAG, "Lists are the same: ");
            for (int i = 0; i < oldSize; i++) {
                updatePhone(
                        contactId, oldPhoneList.get(i).phoneNumberId,
                        newPhonesList.get(i).Number,
                        convertPhoneType(newPhonesList.get(i).Type),
                        newPhonesList.get(i).Primary);
            }
        }

        // since old phone sizes differ, we need to know what to modify and what to delete
        if (oldSize != newSize) {
            Log.i(TAG, "Lists are not the same");

            // ID lists used to ease up the search
            List<String> newIds = new ArrayList<>();
            List<String> oldIds = new ArrayList<>();


            // we fill in newIds list with the new IDs that we will use to find what phone number has been deleted
            for (int i = 0; i < newSize; i++) {
                newIds.add(newPhonesList.get(i).phoneNumberId);
            }

            // we fill in newIds list with the new IDs that we will use to find what phone number has been deleted
            for (int i = 0; i < oldSize; i++) {
                oldIds.add(oldPhoneList.get(i).phoneNumberId);
            }

            // We add all phone numbers from new list that do not have ID or ID is null
            for (int i = 0; i < newPhonesList.size(); i++) {
                if (newPhonesList.get(i).phoneNumberId == null || newPhonesList.get(i).phoneNumberId.trim().length() < 1) {
                    addContactPhone(rawId, newPhonesList.get(i).Number, newPhonesList.get(i).Type, newPhonesList.get(i).Primary);
                }
            }

            // search through the new list and check what phone number has to be deleted
            for (int i = 0; i < oldSize; i++) {
                String phoneId = oldPhoneList.get(i).phoneNumberId;
                if (!newIds.contains(phoneId)) {
                    deleteContactPhone(contactId, phoneId);
                }
            }

            // finally, we update old phones with new changes
            for (int i = 0; i < newSize; i++) {
                String phoneId = newPhonesList.get(i).phoneNumberId;
                if (oldIds.contains(phoneId)) {
                    updatePhone(contactId, phoneId, newPhonesList.get(i).Number, convertPhoneType(newPhonesList.get(i).Type), newPhonesList.get(i).Primary);
                }
            }
        }
    }

    /**
     * Method used to update phone number.
     *
     * @param id           contact unique ID
     * @param phoneId      phone number unique ID
     * @param newPhone     new phone number we are gonna add instead of the old one
     * @param newPhoneType phone type we need to assign to edited phone number
     */
    private void updatePhone(String id, String phoneId, String newPhone, Object newPhoneType, boolean primary) {
        int primarySet = 0;
        if (primary) {
            primarySet = 1;
        }

        String selection =
                ContactsContract.Data.CONTACT_ID + "=?" + " AND "
                        + ContactsContract.Data.MIMETYPE + "=?" + " AND "
                        + ContactsContract.CommonDataKinds.Phone._ID + "=?";
        String selectionArgs[] = {
                String.valueOf(id), // contact ID
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, // content type
                phoneId}; // ID to search

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI);
        builder.withSelection(selection, selectionArgs);

        builder.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone);
        builder.withValue(ContactsContract.CommonDataKinds.Phone.TYPE, newPhoneType);
        builder.withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, primarySet);

        ops.add(builder.build());

        // Update
        if (mContextReference != null && mContextReference.get() != null) {
            try {
                mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
                Log.d(TAG, "Phone updated. ID: " + phoneId);
            } catch (Exception e) {
                Log.e(TAG, "updatePhone failed", e);
            }
        }
    }


    /**
     * This method will get contact URI and return phone list data for specific contact. We use this
     * to be able to get old list of phones, and update it with new list.
     *
     * @param uri contact URI
     * @return list oh phone numbers
     */
    private List<ContactData.PhoneNumber> getPhoneNumbers(String uri) {
        List<ContactData.PhoneNumber> phoneNumbersAll = new ArrayList<>();

        if (mContextReference != null && mContextReference.get() != null) {
            Cursor phonesCursor = null;
            try {
                phonesCursor = mContextReference.get().getContentResolver().query(Uri.parse(uri), null, null, null, null);

                if (phonesCursor != null && phonesCursor.moveToNext()) {
                    int columnIndex_ID = phonesCursor.getColumnIndex(ContactsContract.Contacts._ID);
                    String contactID = phonesCursor.getString(columnIndex_ID);

                    int columnIndex_HASPHONENUMBER = phonesCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                    String stringHasPhoneNumber = phonesCursor.getString(columnIndex_HASPHONENUMBER);

                    if (stringHasPhoneNumber.equalsIgnoreCase("1")) {
                        Cursor numCursor = null;
                        try {
                            numCursor = mContextReference.get().getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactID,
                                    null,
                                    null);

                            if (numCursor != null) {
                                //Get the first phone number
                                while (numCursor.moveToNext()) {
                                    int columnIndex_number = numCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                                    int columnIndex_Id = numCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);
                                    String phoneNumber = numCursor.getString(columnIndex_number);
                                    String phoneId = numCursor.getString(columnIndex_Id);
                                    ContactData.PhoneNumber phone = new ContactData.PhoneNumber(phoneNumber, ContactData.PhoneType.OTHER, false, phoneId);
                                    phoneNumbersAll.add(phone);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to retrieve phone numbers", e);
                        } finally {
                            if (numCursor != null) {
                                numCursor.close();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve contact phones", e);
            } finally {
                if (phonesCursor != null) {
                    phonesCursor.close();
                }
            }
        }
        return phoneNumbersAll;
    }


    /**
     * getting contact ID from URI.
     *
     * @param contactUri Android contact URI.
     * @return Contact ID.
     */
    private String getContactID(String contactUri) {
        Cursor cursor = null;
        String id = null;
        if (mContextReference != null && mContextReference.get() != null) {
            try {
                cursor = mContextReference.get().getContentResolver().query(Uri.parse(contactUri), null, null, null, null);
                int idx;
                if (cursor != null && cursor.moveToFirst()) {
                    idx = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                    id = cursor.getString(idx);
                }
            } catch (Exception e) {
                Log.e(TAG, "getContactID failed", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return id;
    }

    /**
     * get contact RAW ID to be able to add phones
     *
     * @param contactID contact id
     * @return raw contact ID
     */
    private int getContactRawID(String contactID) {

        String[] projection = new String[]{ContactsContract.RawContacts._ID};
        String selection = ContactsContract.RawContacts.CONTACT_ID + "=?";
        String[] selectionArgs = new String[]{contactID};

        int rawContactId = -1;
        if (mContextReference != null && mContextReference.get() != null) {
            Cursor c = null;
            try {
                c = mContextReference.get().getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs, null);

                if (c != null && c.moveToFirst()) {
                    rawContactId = c.getInt(c.getColumnIndex(ContactsContract.RawContacts._ID));
                }
            } catch (Exception e) {
                Log.e(TAG, "getContactRawID failed", e);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        return rawContactId;
    }


    /**
     * Updating company info. Since update will not work unless we have some data already, we are
     * performing checks to getCompanyInfo method. And if some value is null, we will add our dummy value.
     * After this is done, edit can be performed normally
     *
     * @param id       contact unique id
     * @param rawId    contact unique rawID
     * @param company  company name
     * @param position position
     */
    private void updateCompanyInfo(String id, String rawId, String company, String position) {
        if (getCompanyInfo(id, rawId, company, position)) {

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI);

            String orgWhere = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] orgWhereParams = new String[]{rawId,
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};

            builder.withSelection(orgWhere, orgWhereParams);
            builder.withValue(ContactsContract.CommonDataKinds.Organization.DATA, company);
            builder.withValue(ContactsContract.CommonDataKinds.Organization.TITLE, position);
            ops.add(builder.build());

            if (mContextReference != null && mContextReference.get() != null) {
                try {
                    mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
                } catch (Exception e) {
                    Log.e(TAG, "updateCompanyInfo failed", e);
                }
            }
        }
    }

    /**
     * Getting company information. If something does not exist, we add dummy data
     *
     * @param id              contact unique ID
     * @param rawId           contact unique rawID
     * @param newCompanyName  company name to be added
     * @param newPositionInfo position info to be added
     */
    private boolean getCompanyInfo(String id, String rawId, String newCompanyName, String newPositionInfo) {
        String oldCompanyName = null;
        String oldPositionInfo = null;
        boolean shouldUpdate = false;

        if (mContextReference != null && mContextReference.get() != null) {
            ContentResolver cr = mContextReference.get().getContentResolver();
            String orgWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] orgWhereParams = new String[]{id,
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};

            Cursor orgCur = null;
            try {
                orgCur = cr.query(ContactsContract.Data.CONTENT_URI,
                        null, orgWhere, orgWhereParams, null);
                if (orgCur != null) {
                    while (orgCur.moveToNext()) {
                        oldCompanyName = orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DATA));
                        oldPositionInfo = orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "getCompanyInfo failed", e);
            } finally {
                if (orgCur != null) {
                    orgCur.close();
                }
            }

            if (oldCompanyName == null && newCompanyName.trim().length() > 0) {
                addDummyData(rawId, "orgName");
                oldCompanyName = "m";
            }
            if (oldPositionInfo == null && newPositionInfo.trim().length() > 0) {
                addDummyData(rawId, "title");
                oldPositionInfo = "m";
            }

            if (oldCompanyName != null && !oldCompanyName.equals(newCompanyName)) {
                shouldUpdate = true;
            }
            if (oldPositionInfo != null && !oldPositionInfo.equals(newPositionInfo)) {
                shouldUpdate = true;
            }
        }

        return shouldUpdate;
    }

    /**
     * Adding dummy data to company info so we can use update option. Used only on first company edit
     *
     * @param rawId contact unique raw ID
     * @param data  identifier we use to determine what needs to be added
     */
    private void addDummyData(String rawId, String data) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        switch (data) {
            case "orgName":
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.DATA, "m")
                        .build());
                break;
            case "title":
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, "m")
                        .build());
                break;
            case "address":
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, "m")
                        .build());
                break;
            default:
                // default statement. Do nothing
        }

        if (mContextReference != null && mContextReference.get() != null) {
            try {
                mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
            } catch (Exception e) {
                Log.e(TAG, "addDummyData failed", e);
            }
        }
    }

    /**
     * Create new contact.
     *
     * @param contactData contact information
     */
    public void createContact(final ContactData contactData, final Uri imageUri, final String accountType, final String accountName) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mContextReference == null || mContextReference.get() == null) {
                        Log.e(TAG, "create contact, context is null");
                        return;
                    }
                    String setFav = contactData.isFavorite() ? "1" : "0";

                    ContentResolver cr = mContextReference.get().getContentResolver();

                    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                    if (contactData.mCategory == ContactData.Category.IPO){
                        // Adding IPO contact to the account
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                                .build());

                        // Keep entire name input in the single field as IPO has only one field (duplicate name to both given and family name to handle sort correctly)
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contactData.mName)
                                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contactData.mFirstName)
                                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contactData.mLastName)
                                .build());

                        // Adding only first phone number as IPO supports only one phone number with type of Work
                            if (contactData.mPhones.size() > 0 && contactData.mPhones.get(0).Number != null) {
                                //int phoneType = convertPhoneType(contactData.mPhones.get(0).Type);
                                ops.add(ContentProviderOperation.
                                        newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                        .withValue(ContactsContract.Data.MIMETYPE,
                                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contactData.mPhones.get(0).Number)
                                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, TYPE_WORK)
                                        .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, true)
                                        .build());
                            }
                    } else {
                        // creating contact
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                                .withValue(ContactsContract.RawContacts.STARRED, setFav)
                                .build());

                        // first and last name
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contactData.mFirstName)
                                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contactData.mLastName)
                                .build());

                        // phone numbers
                        for (int i = 0; i < contactData.mPhones.size(); i++) {
                            if (contactData.mPhones.get(i).Number != null) {
                                int phoneType = convertPhoneType(contactData.mPhones.get(i).Type);
                                ops.add(ContentProviderOperation.
                                        newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                        .withValue(ContactsContract.Data.MIMETYPE,
                                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contactData.mPhones.get(i).Number)
                                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                                phoneType)
                                        .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, contactData.mPhones.get(i).Primary)
                                        .build());
                            }
                        }

                        //Organization
                        if (!contactData.mCompany.equals("") || !contactData.mPosition.equals("")) {
                            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                    .withValue(ContactsContract.Data.MIMETYPE,
                                            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, contactData.mCompany)
                                    .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                                    .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, contactData.mPosition)
                                    .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                                    .build());
                        }

                        //Address
                        if (!contactData.mLocation.equals("") && contactData.mLocation.trim().length() > 0) {
                            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.CommonDataKinds.StructuredPostal.RAW_CONTACT_ID, 0)
                                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.MIMETYPE,
                                            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, contactData.mLocation)
                                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
                                    .build());
                        }

                        //Photo
                        if (imageUri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContextReference.get().getContentResolver(), imageUri);
                                //Perform check if we have to check for image rotation or not. If there is no path in database
                                //image is most likely provided directly from CSDK and we can ignore image rotation
                                bitmap = Utils.checkAndPerformBitmapRotation(bitmap, mContextReference.get().getContentResolver(), imageUri);
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
                                outputStream.flush();
                                outputStream.close();
                                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, outputStream.toByteArray())
                                        .build());
                            } catch (Exception e) {
                                Log.e(TAG, "photo uri saving failed", e);
                            }
                        } else {
                            try {
                                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, null)
                                        .build());
                            } catch (Exception e) {
                                Log.e(TAG, "photo uri saving failed", e);
                            }
                        }
                    }
                    cr.applyBatch(AUTHORITY, ops);

                    // long operation, check for context instance again
                    if (mContextReference != null && mContextReference.get() != null) {
                        String message = mContextReference.get().getString(R.string.contact_create_with_name);
                        String name = contactData.mName;
                        String displayMessage = message + name;
                        Utils.sendSnackBarData(mContextReference.get(), displayMessage, Utils.SNACKBAR_LONG);
                    }
                    Log.i(TAG, "Contact successfully created");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create contact", e);
                }
            }
        });
    }

    /**
     * Deletes from the local database the raw contact that correspond contactID
     * @param contactID id of the contact to be delted
     */
    public void deleteLocalContact(String contactID){
        if (contactID != null && contactID.trim().length() > 0) {
            String rawID = String.valueOf(getContactRawID(contactID));
            Log.d(TAG, "Deleting contact with ID: " + contactID);
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection(ContactsContract.RawContacts._ID + " = ?", new String[]{rawID})
                    .build());
            try {
                mContextReference.get().getContentResolver().applyBatch(AUTHORITY, ops);
                Utils.sendSnackBarData(mContextReference.get(), mContextReference.get().getString(R.string.contact_deleted), true);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Contact cannot be deleted. Contact ID: " + contactID);
            Utils.sendSnackBarData(mContextReference.get(), mContextReference.get().getString(R.string.contact_undeletable_error), true);
        }
    }
}
