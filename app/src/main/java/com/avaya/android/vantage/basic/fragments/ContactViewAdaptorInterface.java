package com.avaya.android.vantage.basic.fragments;

import com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor;

/**
 * Interface is used for communication between {@link ContactsFragment}, {@link FavoritesFragment}
 * and {@link com.avaya.android.vantage.basic.activities.MainActivity}
 * Used to get latest version of UIContactViewAdaptor from
 * {@link com.avaya.android.vantage.basic.activities.MainActivity}
 */
public interface ContactViewAdaptorInterface {

    UIContactsViewAdaptor getContactViewAdapter();
}