package com.avaya.android.vantage.basic.views.interfaces;

import android.support.v4.graphics.drawable.RoundedBitmapDrawable;

import com.avaya.android.vantage.basic.model.ContactData;

import java.util.List;

/**
 * Interface used in classes that load contact information. It provides communication between
 * {@link com.avaya.android.vantage.basic.views.adapters.MyContactsRecyclerViewAdapter},
 * {@link com.avaya.android.vantage.basic.views.adapters.MyFavoritesRecyclerViewAdapter} and
 * {@link com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor}
 */
public interface IContactsViewInterface {
    void recreateData(List<ContactData> items, ContactData.Category contactCategory);

    void updateItem(ContactData item);

    void removeItem(int position);

    void removeItem(ContactData contact);

    void addItem(ContactData item);

    void cacheContactDrawable(String mUUID, RoundedBitmapDrawable circularBitmapDrawable);

    boolean isPhotoCached(String uuid);

    int getIndexOf(ContactData item);
}
