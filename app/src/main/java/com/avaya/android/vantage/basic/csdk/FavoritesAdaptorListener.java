package com.avaya.android.vantage.basic.csdk;

/**
 * Interface used to notify favorites fragment list about favorites changes. Provide communication to
 * {@link com.avaya.android.vantage.basic.fragments.FavoritesFragment} and
 * {@link com.avaya.android.vantage.basic.views.adapters.MyFavoritesRecyclerViewAdapter}
 */
public interface FavoritesAdaptorListener {

    void notifyFavoritesChanged();
}
