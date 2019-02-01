package com.avaya.android.vantage.basic.csdk;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.clientservices.common.DataCollectionChangeType;
import com.avaya.clientservices.common.DataRetrievalWatcher;
import com.avaya.clientservices.common.DataRetrievalWatcherListener;
import com.avaya.clientservices.contact.AddContactCompletionHandler;
import com.avaya.clientservices.contact.Contact;
import com.avaya.clientservices.contact.ContactCompletionHandler;
import com.avaya.clientservices.contact.ContactError;
import com.avaya.clientservices.contact.ContactException;
import com.avaya.clientservices.contact.ContactProviderSourceType;
import com.avaya.clientservices.contact.ContactSearchLocationType;
import com.avaya.clientservices.contact.ContactSearchScopeType;
import com.avaya.clientservices.contact.ContactService;
import com.avaya.clientservices.contact.ContactServiceListener;
import com.avaya.clientservices.contact.ContactSourceType;
import com.avaya.clientservices.contact.EditableContact;
import com.avaya.clientservices.contact.ExtraFieldKeys;
import com.avaya.clientservices.contact.UpdateContactCompletionHandler;
import com.avaya.clientservices.contact.fields.ContactEmailAddressFieldList;
import com.avaya.clientservices.contact.fields.ContactPhoneField;
import com.avaya.clientservices.contact.fields.ContactPhoneFieldList;
import com.avaya.clientservices.contact.fields.ContactPhoneNumberType;
import com.avaya.clientservices.contact.fields.ContactStringField;
import com.avaya.clientservices.contact.fields.EditableContactPhoneField;
import com.avaya.clientservices.user.User;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.avaya.android.vantage.basic.csdk.ErrorManager.AADS_GENERAL_ERROR;
import static com.avaya.android.vantage.basic.model.ContactData.Category;
import static com.avaya.android.vantage.basic.model.ContactData.PhoneType;

/**
 *
 */

public class ContactsAdaptor implements ContactServiceListener {

    private ContactDataRetrievalWatcherListener mContactDataRetrievalWatcherListener;

    public ContactsAdaptor(Context context) {
        this.context = context;
    }


    private static Map<String, UIContactsViewAdaptor.OnPhotoInCache> sPhotoListenersQueue = new HashMap<>();
    private static Map<String, byte[]> sPhotoCache = new HashMap<>();
    private static final String TAG = "Contacts adaptor";
    private final DataRetrievalWatcher<Contact> mDataRetrivalWatcher = new DataRetrievalWatcher<>();
    private Context context;

    private WeakReference<UIContactsViewAdaptor> mUiObj;
    private User mUser;
    private boolean mIsListeningToContactService = false;
    private boolean mIsFirstNameFirst = true;

    // handle contact update
    private UpdateContactCompletionHandler mUpdateContactCompletionHandler;
    private EditableContact mEditableContact;
    private boolean showSnackbarMessage = false;
    // retry update once (fix for fast changing favorite status)
    private boolean retryUpdate = false;


    /**
     * Register {@link UIContactsViewAdaptor} for {@link ContactsAdaptor}
     *
     * @param uiObj {@link UIContactsViewAdaptor}
     */
    public void registerListener(UIContactsViewAdaptor uiObj) {
        mUiObj = new WeakReference<>(uiObj);
        if (mContactDataRetrievalWatcherListener != null) {
            mDataRetrivalWatcher.removeListener(mContactDataRetrievalWatcherListener);
        }
        mContactDataRetrievalWatcherListener = new ContactDataRetrievalWatcherListener(mUiObj.get().getEnterpriseContacts());
        mDataRetrivalWatcher.addListener(mContactDataRetrievalWatcherListener);
        retrieveContacts();
    }

    /**
     * Set mFirstNameFirst boolean which represent do we set on first place
     * last name or first name when we are showing full name.
     *
     * @param showFirstNameFirst boolean
     */
    void firstNameFirst(Boolean showFirstNameFirst) {
        mIsFirstNameFirst = showFirstNameFirst;
    }

    /**
     * Create {@link ContactData} from provided {@link Contact}
     *
     * @param contact {@link Contact}
     * @return {@link ContactData}
     */
    private ContactData createContactData(final Contact contact) {

        // in case we ned to use CSDK to load contacts, we can use this code to get native URI and photo thumbnail URI
        ContactStringField rawURI = (ContactStringField) contact.getExtraFields().get(ExtraFieldKeys.CONTACT_LOOK_UP_URI_STRING);
        String nativeURI;
        String photoURI = "";
        String photoThumbnailURI = "";
        if (rawURI != null) {
            nativeURI = rawURI.getValue();
            String[] photoData = LocalContactInfo.getContactPhotoURI(Uri.parse(nativeURI), context);
            photoThumbnailURI = photoData[0];
            photoURI = photoData[1];
        } else {
            nativeURI = "";
        }

        String email = "";
        ContactEmailAddressFieldList contactEmailList = contact.getEmailAddresses();
        for (int i = 0; i < contactEmailList.getValues().size(); i++) {
            email = contactEmailList.getValues().get(i).getAddress();
        }

        ContactPhoneFieldList phones = contact.getPhoneNumbers();

        //debug information
        String dbgName = contact.getNativeDisplayName().getValue();
        Log.d(TAG, dbgName + " - Phone numbers: " + contact.getPhoneNumbers().getValues().size() );
        // debung information end

        Category category = (phones.getContactProviderSourceType() == ContactProviderSourceType.LOCAL) ? Category.LOCAL : Category.ENTERPRISE;
        List<ContactData.PhoneNumber> uiphones = new ArrayList<>();
        for (ContactPhoneField phone : phones.getValues()) {
            PhoneType type = PhoneType.HOME;
            switch (phone.getType()) {

                case WORK:
                    type = PhoneType.WORK;
                    break;
                case HOME:
                    type = PhoneType.HOME;
                    break;
                case MOBILE:
                    type = PhoneType.MOBILE;
                    break;
                case HANDLE:
                    type = PhoneType.HANDLE;
                    break;
                case FAX:
                    type = PhoneType.FAX;
                    break;
                case PAGER:
                    type = PhoneType.PAGER;
                    break;
                case ASSISTANT:
                    type = PhoneType.ASSISTANT;
                    break;
                case OTHER:
                    type = PhoneType.OTHER;
                    break;
            }

            String phoneNumber = phone.getPhoneNumber();
            uiphones.add(new ContactData.PhoneNumber(phoneNumber, type, phone.isDefault(), null));
        }

        String displayName;
        if (!mIsFirstNameFirst) {
            displayName = contact.getNativeLastName().getValue() + " " + contact.getNativeFirstName().getValue();
        } else {
            displayName = contact.getNativeFirstName().getValue() + " " + contact.getNativeLastName().getValue();
        }

        final ContactData ui_contact = new ContactData(displayName,
                contact.getNativeFirstName().getValue(),
                contact.getNativeLastName().getValue(),
                contact.hasPicture() ? contact.getPictureData() : null,
                contact.isFavorite().getValue(),
                contact.getLocation().getValue(),
                contact.getCity().getValue(),
                contact.getTitle().getValue(),
                contact.getCompany().getValue(),
                uiphones,
                category,
                contact.getUniqueAddressForMatching(),
                nativeURI, photoThumbnailURI, true, email, photoURI, "", "", "");
        if (contact.hasPicture()) {
            contact.retrievePicture(new ContactCompletionHandler() {
                ContactsAdaptorListener cal = ContactsAdaptor.this.mUiObj.get();

                @Override
                public void onSuccess() {
                    sPhotoCache.put(ui_contact.mUUID, contact.getPictureData());
                    if (sPhotoListenersQueue.containsKey(ui_contact.mUUID)) {
                        UIContactsViewAdaptor.OnPhotoInCache listener = sPhotoListenersQueue.get(ui_contact.mUUID);
                        listener.setPhoto(ui_contact);
                        sPhotoListenersQueue.remove(contact.getUniqueAddressForMatching());
                    }
                    ui_contact.mPhoto = contact.getPictureData();
                    cal.onContactPhotoReady(ui_contact);

                }

                @Override
                public void onError(ContactException error) {
                    Log.e(TAG, "Failed To retrieve photo data");
                }
            });

        }
        return ui_contact;
    }

    /**
     * Get current method name for purpose of logging
     *
     * @return String with name of method
     */
    private static String getCurrentMethodName() {
        final StackTraceElement e = Thread.currentThread().getStackTrace()[3];
        final String s = e.getClassName();
        return s.substring(s.lastIndexOf('.') + 1, s.length()) + "." + e.getMethodName();
    }


    /**
     * Retrieve contacts
     */
    public void retrieveContacts() {
        if (mUser != null) {
            ContactService contactService = mUser.getContactService();
            if (contactService != null && contactService.isServiceAvailable()) {
                if(mContactDataRetrievalWatcherListener != null) {
                    mContactDataRetrievalWatcherListener.clear();
                }
                contactService.getContacts(mDataRetrivalWatcher,
                        ContactSourceType.ALL);
            }
        } else {
            Log.e(TAG, "can not retrieve contacts, user is not set ...");
        }
    }

    /**
     * Set {@link User}
     *
     * @param user {@link User}
     */
    public void setUser(User user) {
        if (user == null && mUser != null && mIsListeningToContactService) {
            mUser.getContactService().removeListener(this);
            mIsListeningToContactService = false;
        }
        this.mUser = user;
        if (mUser != null && !mIsListeningToContactService) {
            mUser.getContactService().addListener(this);
            mIsListeningToContactService = true;
        }

    }

    /**
     * Used in Enterprise contact edit to retrieve SDK Contact object.
     *
     * @return User object
     */
    public User getUser() {
        return mUser;
    }

    /**
     * Called when the contact service becomes available.
     *
     * @param contactService The contact service associated with the callback.
     */
    @Override
    public void onContactServiceAvailable(ContactService contactService) {
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * Called when the contact service becomes unavailable.
     *
     * @param contactService The contact service associated with the callback.
     */
    @Override
    public void onContactServiceUnavailable(ContactService contactService) {
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * Called when a contact provider failed.
     *
     * @param contactService The contact service associated with the callback.
     * @param sourceType     The contact source type provided by this provider.
     * @param providerError  The error code for the failure.
     */
    @Override
    public void onContactServiceProviderFailed(ContactService contactService, ContactSourceType sourceType, ContactError providerError) {
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * Called when contact available provider list changes.
     */
    @Override
    public void onContactServiceAvailableProviderListChanged() {
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * Called when contact services add or search capability changed.
     */
    @Override
    public void onContactServiceCapabilitiesChanged() {
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * Called when loading of contacts for a contact provider has been completed successfully.
     *
     * @param contactService         The contact service associated with the callback.
     * @param contactSourceType      The contact source type provided by this provider.
     * @param contactLoadingComplete is contact loading completed
     */
    @Override
    public void onContactServiceLoadingComplete(ContactService contactService, ContactSourceType contactSourceType, boolean contactLoadingComplete) {
        Log.d(TAG, getCurrentMethodName());
        ErrorManager.getInstance().removeErrorFromList(AADS_GENERAL_ERROR);
        retrieveContacts();
    }

    /**
     * Called when loading of contacts for a contact provider has been completed successfully.
     *
     * @param contactService         The contact service associated with the callback.
     * @param contactSourceType      The contact source type provided by this provider.
     * @param contactLoadingComplete is contact loading complete
     * @param providerError          The error code for the failure.
     */
    @Override
    public void onContactServiceLoadingFailed(ContactService contactService, ContactSourceType contactSourceType, boolean contactLoadingComplete, ContactError providerError) {
        Log.d(TAG, getCurrentMethodName());
        Log.d(TAG, "AADS contact retrieval failed with error code: " + providerError.toString());
        Utils.sendSnackBarData(context, context.getResources().getString(R.string.error_message_service_not_available), Utils.SNACKBAR_LONG);
        ErrorManager.getInstance().addErrorToList(AADS_GENERAL_ERROR);
    }

    /**
     * Perform search base od provided query string
     *
     * @param query String based on which search is performed
     */
    public void search(String query) {
        if (mUser != null && mUser.getContactService().isServiceAvailable()) {
            if (mContactDataRetrievalWatcherListener != null) {
                mContactDataRetrievalWatcherListener.clear();
            }
            ContactSearchLocationType location = mUser.getContactService().getNetworkSearchContactCapability().isAllowed() ? ContactSearchLocationType.ALL : ContactSearchLocationType.LOCAL_CACHE;
            mUser.getContactService().searchContacts(mDataRetrivalWatcher, query, ContactSearchScopeType.ALL, location, 200, 10);
        }
    }

    /**
     * Start editing of enterprise contacts
     *
     * @param contactData     {@link ContactData}
     * @param editableContact {@link EditableContact}
     * @param isContactEdit   false - only favorites tag is being changed, true - contact is being edited
     */
    public void startEnterpriseEditing(ContactData contactData, EditableContact editableContact, boolean isContactEdit) {
        Log.d(TAG, "Changes from ContactEditFragment " + isContactEdit);
        showSnackbarMessage = isContactEdit;
        mEditableContact = editableContact;

        if (context == null) {
            return;
        }

        if (mUser == null) {
            if (showSnackbarMessage) {
                Utils.sendSnackBarData(context, context.getResources().getString(R.string.contact_edit_user_not_found), Utils.SNACKBAR_LONG);
            }
            return;
        }

        if (mEditableContact == null) {
            if (showSnackbarMessage) {
                Utils.sendSnackBarData(context, context.getResources().getString(R.string.contact_edit_server_not_available), Utils.SNACKBAR_LONG);
            }
            Log.e(TAG, "editable contact is null");
            return;
        }

        if (mEditableContact.getNativeFirstName().getCapability().isAllowed()) {
            mEditableContact.getNativeFirstName().setValue(contactData.mFirstName);
        } else {
            Log.d(TAG, "First name cannot be changed");
        }
        if (mEditableContact.getNativeLastName().getCapability().isAllowed()) {
            mEditableContact.getNativeLastName().setValue(contactData.mLastName);
        } else {
            Log.d(TAG, "Last name cannot be changed");
        }

        if (mEditableContact.getNativeDisplayName().getCapability().isAllowed()) {
            mEditableContact.getNativeDisplayName().setValue(contactData.mName);
        } else {
            Log.d(TAG, "Nickname cannot be changed");
        }

        if (mEditableContact.getCompany().getCapability().isAllowed()) {
            mEditableContact.getCompany().setValue(contactData.mCompany);
        } else {
            Log.d(TAG, "Company name cannot be changed");
        }

        if (mEditableContact.getTitle().getCapability().isAllowed()) {
            mEditableContact.getTitle().setValue(contactData.mPosition);
        } else {
            Log.d(TAG, "Job title cannot be changed");
        }

        if (mEditableContact.getStreetAddress().getCapability().isAllowed()) {
            mEditableContact.getStreetAddress().setValue(contactData.mLocation);
        } else {
            Log.d(TAG, "Street address cannot be changed");
        }

        // Add created phone item to array and set it for editable contact
        ArrayList<EditableContactPhoneField> phoneNumbers = new ArrayList<>();

        // Create phone item
        for (int i = 0; i < contactData.mPhones.size(); i++) {
            EditableContactPhoneField contactPhoneField = new EditableContactPhoneField();
            contactPhoneField.setPhoneNumber(contactData.mPhones.get(i).Number);
            contactPhoneField.setType(getConvertedPhoneType(contactData.mPhones.get(i).Type));
            contactPhoneField.setDefault(contactData.mPhones.get(i).Primary);
            phoneNumbers.add(contactPhoneField);
        }

        if (mEditableContact.getPhoneNumbers().getCapability().isAllowed()) {
            mEditableContact.getPhoneNumbers().setValues(phoneNumbers);
        } else {
            Log.d(TAG, "Contact phones cannot be changed");
        }

        mEditableContact.isFavorite().setValue(contactData.isFavorite());

        boolean contactSavable = mEditableContact.isContactSavable();

        if (!contactSavable) {
            if (showSnackbarMessage) {
                Utils.sendSnackBarData(context, context.getResources().getString(R.string.contact_edit_not_savable_toast), Utils.SNACKBAR_LONG);
            }
            Log.e(TAG, "Contact not savable");
            return;
        }

        retryUpdate = false;
        mUser.getContactService().updateContact(mEditableContact, getUpdateContactCompletionHandler());
    }

    /**
     * Used to convert regular contact data to editable contact
     * @param contactData regular contact data
     * @return editable contact with all added information
     */
    private EditableContact convertContactDataToEditable(ContactData contactData){
        EditableContact editableContact = mUser.getContactService().createEditableContact();
        editableContact.getNativeDisplayName().setValue(contactData.mName);
        editableContact.getNativeFirstName().setValue(contactData.mFirstName);
        editableContact.getNativeLastName().setValue(contactData.mLastName);
        editableContact.getTitle().setValue(contactData.mPosition);
        editableContact.getCompany().setValue(contactData.mCompany);
        editableContact.getStreetAddress().setValue(contactData.mLocation);
        editableContact.isFavorite().setValue(contactData.isFavorite());

        for (ContactData.PhoneNumber phoneNumber : contactData.mPhones){
            EditableContactPhoneField number = new EditableContactPhoneField();
            number.setPhoneNumber(phoneNumber.Number);
            number.setDefault(phoneNumber.Primary);
            number.setType(getConvertedPhoneType(phoneNumber.Type));
            editableContact.getPhoneNumbers().getValues().add(number);
        }
        return editableContact;
    }

    /**
     * Method we use to create new enterprise cotnact
     * @param contactData standard contact data object
     */
    public void createEnterpriseContact(final ContactData contactData){
        boolean addContactCapability = mUser.getContactService().getAddContactCapability()
                .isAllowed();
        if (!addContactCapability) {
            Log.d(TAG, "Contact cannot be added");
            Utils.sendSnackBarData(context, context.getString(R.string.enterprise_contact_create_error), Utils.SNACKBAR_LONG);
        } else {
            mUser.getContactService().addContact(convertContactDataToEditable(contactData), new AddContactCompletionHandler() {
                @Override
                public void onSuccess(Contact contact, boolean b) {
                    Log.d(TAG, "Contact has been added");
                    String message = context.getString(R.string.contact_create_with_name);
                    String name = contactData.mName;
                    String displayMessage = message + name;
                    Utils.sendSnackBarData(context, displayMessage, Utils.SNACKBAR_LONG);
                    if (mUiObj != null && mUiObj.get() != null){
                        mUiObj.get().refresh();
                    } else {
                        Log.e(TAG, "UI Adapter is null");
                    }
                }

                @Override
                public void onError(ContactException e) {
                    Log.e(TAG, "Failed to add contact. ", e);
                    Utils.sendSnackBarData(context, context.getString(R.string.enterprise_contact_create_error), Utils.SNACKBAR_LONG);
                }
            });
        }
    }

    /**
     * Reuse singleton instance of completion handler
     *
     * @return {@link UpdateContactCompletionHandler}
     */
    private UpdateContactCompletionHandler getUpdateContactCompletionHandler() {
        if (mUpdateContactCompletionHandler == null) {
            mUpdateContactCompletionHandler = new UpdateContactCompletionHandler() {

                @Override
                public void onSuccess(Contact contact) {
                    if (showSnackbarMessage) {
                        Utils.sendSnackBarData(context, context.getResources().getString(R.string.contact_edit_success), Utils.SNACKBAR_LONG);
                    }
                    if (mUiObj != null && mUiObj.get() != null) {
                        mUiObj.get().refresh();
                    } else {
                        Log.e(TAG, "UIAdaptor is null");
                    }
                    if (mEditableContact != null && mEditableContact.isFavorite().getValue() != contact.isFavorite().getValue() && !retryUpdate) {
                        retryUpdate = true;
                        mUser.getContactService().updateContact(mEditableContact, this);
                    }
                }

                @Override
                public void onError(ContactException error) {
                    if (showSnackbarMessage) {
                        Utils.sendSnackBarData(context, context.getResources().getString(R.string.contact_edit_general_error), Utils.SNACKBAR_LONG);
                    }
                    Log.e(TAG, "Failed to update contact", error);
                }
            };
        }
        return mUpdateContactCompletionHandler;
    }

    /**
     * From provided {@link PhoneType} obtain {@link ContactPhoneNumberType}
     *
     * @param phoneType {@link PhoneType}
     * @return {@link ContactPhoneNumberType}
     */
    private ContactPhoneNumberType getConvertedPhoneType(PhoneType phoneType) {
        if (ContactData.PhoneType.WORK.equals(phoneType)) {
            return ContactPhoneNumberType.WORK;
        } else if (ContactData.PhoneType.MOBILE.equals(phoneType)) {
            return ContactPhoneNumberType.MOBILE;
        } else if (ContactData.PhoneType.HOME.equals(phoneType)) {
            return ContactPhoneNumberType.HOME;
        } else if (ContactData.PhoneType.HANDLE.equals(phoneType)) {
            return ContactPhoneNumberType.HANDLE;
        } else if (ContactData.PhoneType.FAX.equals(phoneType)) {
            return ContactPhoneNumberType.FAX;
        } else if (ContactData.PhoneType.PAGER.equals(phoneType)) {
            return ContactPhoneNumberType.PAGER;
        } else if (ContactData.PhoneType.ASSISTANT.equals(phoneType)) {
            return ContactPhoneNumberType.ASSISTANT;
        } else if (ContactData.PhoneType.OTHER.equals(phoneType)) {
            return ContactPhoneNumberType.OTHER;
        }
        return ContactPhoneNumberType.WORK;
    }

    /**
     * Helper method in order to access local contacts in {@link ContactsAdaptor}
     * with the SDK manager.
     *
     * @return returns the list of local contacts
     */
    public List<ContactData> getLocalContacts() {
        return mUiObj.get() != null ? mUiObj.get().getLocalContacts() : null;
    }

    /**
     * Implementation of {@link DataRetrievalWatcherListener<Contact>}
     */
    private class ContactDataRetrievalWatcherListener implements DataRetrievalWatcherListener<Contact> {
        private final List<ContactData> mContacts;

        private ContactDataRetrievalWatcherListener(List<ContactData> contacts) {
            mContacts = contacts;
        }

        @Override
        public void onRetrievalProgress(DataRetrievalWatcher<Contact> watcher, boolean determinate, int numRetrieved, int total) {
            Log.d(TAG, getCurrentMethodName());

            //TODO: this implementation waits till all contacts are retrieved and only then populate the contact list. we might want to consider lazy loading for performance improvement
            if (mUiObj.get() != null) {
                if (numRetrieved == total) {
                    mContacts.clear();
                }
                if (numRetrieved == total) {
                    List<Contact> contacts = watcher.getSnapshot();
                    for (int i = mContacts.size(); i < numRetrieved; i++) {
                        if (TextUtils.isEmpty(contacts.get(i).getNativeDisplayName().getValue()) &&
                                TextUtils.isEmpty(contacts.get(i).getNativeFirstName().getValue()) &&
                                TextUtils.isEmpty(contacts.get(i).getNativeLastName().getValue()) &&
                                contacts.get(i).getPhoneNumbers().getValues().size() == 0)
                            continue;
                        if (!contacts.get(i).getPhoneNumbers().getContactProviderSourceType().equals(ContactProviderSourceType.LOCAL)) {
                            mContacts.add(createContactData(contacts.get(i)));
                            Log.d(TAG, "Enterprise contact received: " + contacts.get(i).getNativeDisplayName().getValue() + ", ID: " + contacts.get(i).getUniqueAddressForMatching());
                        }
                    }
                    mUiObj.get().onDataRetrievalProgress(mContacts, determinate, numRetrieved, total, Category.ENTERPRISE);
                }

            }

        }

        @Override
        public void onRetrievalCompleted(DataRetrievalWatcher<Contact> watcher) {
            Log.d(TAG, getCurrentMethodName());
            if (mUiObj.get() != null) {
                mUiObj.get().onDataRetrievalComplete(mUiObj.get().getEnterpriseContacts(), Category.ENTERPRISE);
            }
        }

        @Override
        public void onRetrievalFailed(DataRetrievalWatcher<Contact> watcher, Exception failure) {
            Log.d(TAG, getCurrentMethodName());
            if (mUiObj.get() != null)
                mUiObj.get().onDataRetrievalFailed(failure);
        }

        @Override
        public void onCollectionChanged(DataRetrievalWatcher<Contact> watcher, DataCollectionChangeType changeType, List<Contact> changedItems) {
            Log.d(TAG, getCurrentMethodName());
            if (mUiObj.get() != null) {
                ContactsAdaptorListener.ChangeType uichangeType = ContactsAdaptorListener.ChangeType.ADD;
                switch (changeType) {
                    case ITEMS_ADDED:
                        uichangeType = ContactsAdaptorListener.ChangeType.ADD;
                        break;
                    case ITEMS_DELETED:
                        uichangeType = ContactsAdaptorListener.ChangeType.REMOVE;
                        break;
                    case ITEMS_UPDATED:
                        uichangeType = ContactsAdaptorListener.ChangeType.UPDATE;
                        break;
                    case COLLECTION_CLEARED:
                        onDataSetInvalidated();
                        return;
                }

                List<Integer> changedIndices = new ArrayList<>();
                for (Contact c : changedItems) {
                    for (int cd = 0; cd < mContacts.size(); cd++)
                        if (mContacts.get(cd).mUUID.equals(c.getUniqueAddressForMatching()))
                            changedIndices.add(cd);
                }
                mUiObj.get().onDataSetChanged(uichangeType, changedIndices);
            }
        }

        public void clear() {
            if (mContacts != null)
                mContacts.clear();
        }

        private void onDataSetInvalidated() {
            Log.d(TAG, getCurrentMethodName());
            if (mUiObj.get() != null) {
                mUiObj.get().getEnterpriseContacts().clear();
                mUiObj.get().onDataSetInvalidated(mUiObj.get().getEnterpriseContacts());
            }
        }
    }
}