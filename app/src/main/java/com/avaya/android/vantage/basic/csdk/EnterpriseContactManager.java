package com.avaya.android.vantage.basic.csdk;

import android.util.Log;

import com.avaya.clientservices.common.Capability;
import com.avaya.clientservices.common.DataCollectionChangeType;
import com.avaya.clientservices.common.DataRetrievalWatcher;
import com.avaya.clientservices.common.DataRetrievalWatcherListener;
import com.avaya.clientservices.contact.Contact;
import com.avaya.clientservices.contact.ContactCompletionHandler;
import com.avaya.clientservices.contact.ContactException;
import com.avaya.clientservices.contact.ContactSearchLocationType;
import com.avaya.clientservices.contact.ContactSearchScopeType;
import com.avaya.clientservices.contact.EditableContact;
import com.avaya.clientservices.user.User;

import java.util.List;

/**
 * Contact manager for enterprise contacts
 */
public class EnterpriseContactManager {

    private static final String TAG = "EnterpriseContactMgr";

    private DataRetrievalWatcher<Contact> mSearchWatcher;
    private EnterpriseContactListener mListener;
    private boolean mIsDeleting;

    /**
     * {@link EnterpriseContactManager} public constructor. Constructor have to be provided with
     * {@link EnterpriseContactListener}
     * @param mListener {@link EnterpriseContactListener}
     */
    public EnterpriseContactManager(EnterpriseContactListener mListener) {
        this.mListener = mListener;
        mSearchWatcher = new DataRetrievalWatcher<>();
        setupWatcherListener();
    }

    /**
     * Setting up {@link DataRetrievalWatcherListener}
     */
    private void setupWatcherListener() {
        DataRetrievalWatcherListener<Contact> mSearchDataRetrievalWatcherListener = new DataRetrievalWatcherListener<Contact>() {
            @Override
            public void onRetrievalProgress(DataRetrievalWatcher<Contact> watcher, boolean determinate, int numRetrieved, int total) {
                Log.d(TAG, "Find Enterprise contact retrieval progress");
            }

            @Override
            public void onRetrievalCompleted(DataRetrievalWatcher<Contact> watcher) {
                Log.d(TAG, "Find Enterprise contact retrieval completed");
            }

            @Override
            public void onRetrievalFailed(DataRetrievalWatcher<Contact> watcher, Exception failure) {
                Log.d(TAG, "Find Enterprise contact retrieval failed");
            }

            @Override
            public void onCollectionChanged(DataRetrievalWatcher<Contact> watcher, DataCollectionChangeType changeType, List<Contact> changedItems) {

                if (changedItems.size() > 0) {
                    if (mIsDeleting) {
//                        for (Contact contact: changedItems) {
//                         IF WE DELETE ALL CONTACTS BACKEND WILL DIE
//                        }
                        Contact foundContact = changedItems.get(0);
                        Log.d(TAG, "found item: " + foundContact.getNativeDisplayName());
                        boolean deleteContactCapability = foundContact.getDeleteContactCapability().isAllowed();
                        Log.d(TAG, "Contact is available to be deleted:"+deleteContactCapability);
                        if (deleteContactCapability) {
                            Log.d(TAG, "Contact deleted");
                            SDKManager.getInstance().getContactsAdaptor().getUser().getContactService().deleteContact(foundContact, new ContactCompletionHandler() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "Contact deleted");
                                        SDKManager.getInstance().getContactsAdaptor().retrieveContacts();
                                        if (mListener != null) {
                                            mListener.contactDeleted();
                                        }
                                }

                                @Override
                                public void onError(ContactException error) {
                                    Log.d(TAG, "Contact not deleted");
                                        if (mListener != null) {
                                            mListener.reportDeleteError();
                                        }
                                }
                            });
                        } else {
                            Log.d(TAG, "Enterprise contact deleting not available");
                        }
                    } else {
                        Log.d(TAG, "found item: " + changedItems.get(0).getNativeDisplayName());
                        Contact foundContact = changedItems.get(0);
                        boolean editContactCapability = foundContact.getUpdateContactCapability().isAllowed();
                        if (!editContactCapability) {
                            //disable edit if not allowed
                            if (mListener != null) {
                                mListener.reportError();
                            }
                        }
                        EditableContact editableEnterpriseContact = SDKManager.getInstance().getContactsAdaptor().getUser().getContactService().createEditableContactFromContact(foundContact);
                        if (editableEnterpriseContact == null) {
                            //disable edit if not found
                            if (mListener != null) {
                                mListener.reportError();
                            }
                        } else {
                            if (mListener != null) {
                                mListener.retrieveEditableContact(editableEnterpriseContact);
                            }
                        }
                    }
                } else {
                    if (mListener != null) {
                        mListener.reportError();
                    }
                    Log.d(TAG, "Enterprise contact not found");
                }
            }
        };

        // implement listener of DataRetrievalWatcherListener interface to get notifications (onRetrievalProgress, onRetrievalCompleted etc)
        mSearchWatcher.addListener(mSearchDataRetrievalWatcherListener);
    }

    /**
     * Used for editing Enterprise contact.
     *
     * @param searchQuery unique identifier for contact. Can be email or phone number.
     */
    public void findEnterpriseContact(String searchQuery , boolean toDelete) {

        User user = SDKManager.getInstance().getContactsAdaptor().getUser();
        if (user == null) {
            Log.d(TAG, "user is null");
            return;
        }

        mIsDeleting = toDelete;
        ContactSearchLocationType location = user.getContactService().getNetworkSearchContactCapability().isAllowed() ? ContactSearchLocationType.ALL : ContactSearchLocationType.LOCAL_CACHE;
        user.getContactService().searchContacts(mSearchWatcher, searchQuery, ContactSearchScopeType.ALL, location, 200, 10);
    }

    /**
     * {@link EnterpriseContactListener} responsible for communication between
     * {@link EnterpriseContactManager} and {@link com.avaya.android.vantage.basic.fragments.ContactDetailsFragment}
     * also it provides communication with {@link com.avaya.android.vantage.basic.fragments.ContactEditFragment}
     */
    public interface EnterpriseContactListener {
        void retrieveEditableContact(EditableContact editableContact);

        void contactDeleted();

        void reportError();

        void reportDeleteError();
    }
}
