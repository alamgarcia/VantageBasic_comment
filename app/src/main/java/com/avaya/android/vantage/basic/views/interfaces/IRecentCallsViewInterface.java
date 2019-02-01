package com.avaya.android.vantage.basic.views.interfaces;

import android.support.v4.graphics.drawable.RoundedBitmapDrawable;

import com.avaya.android.vantage.basic.model.CallData;

import java.util.List;

/**
 * Recent call view interface responsible for connecting {@link com.avaya.android.vantage.basic.views.adapters.MyRecentsRecyclerViewAdapter}
 * and {@link com.avaya.android.vantage.basic.adaptors.UIHistoryViewAdaptor}
 */

public interface IRecentCallsViewInterface {

    void recreateData(List<CallData> items);

    void updateItem(CallData item);

    void removeItem(int position);

    void removeItem(CallData contact);

    void addItem(CallData item);

    void cacheContactDrawable(String mUUID, RoundedBitmapDrawable circularBitmapDrawable);

    boolean isPhotoCached(String uuid);

    int getIndexOf(CallData item);
}
