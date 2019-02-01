package com.avaya.android.vantage.basic.csdk;

import com.avaya.android.vantage.basic.model.ContactData;

import java.util.List;

/**
 * Interface responsible for communication between {@link ContactsAdaptor},
 * {@link com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor} and
 * {@link com.avaya.android.vantage.basic.adaptors.UIHistoryViewAdaptor}
 */

public interface ContactsAdaptorListener {
    List<ContactData> getEnterpriseContacts();

    List<ContactData> getLocalContacts();

    List<ContactData> getAllContacts();

    List<ContactData> getLocalFavorites();

    List<ContactData> getIpoContacts();

    void search(String query);

    enum ChangeType {ADD, REMOVE, UPDATE}

    void onDataRetrievalProgress(List<ContactData> contactDataList, boolean determinate, int numRetrieved, int total, ContactData.Category contactCategory);

    void onDataRetrievalComplete(List<ContactData> contactDataList, ContactData.Category contactCategory);

    void onDataRetrievalFailed(Exception failure);

    void onDataSetChanged(ChangeType type, List<Integer> changedIndices);

    void onDataSetInvalidated(List<ContactData> contactDataList);

    void onContactPhotoReady(ContactData contactData);
}
