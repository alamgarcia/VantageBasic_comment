package com.avaya.android.vantage.basic.csdk;

import android.content.Context;

import com.avaya.android.vantage.basic.model.CallData;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface providing communication for {@link HistoryAdaptor} and
 * {@link com.avaya.android.vantage.basic.adaptors.UIHistoryViewAdaptor}
 */

public interface HistoryAdaptorListener {

    List<CallData> getContacts(Context applicationContext);

    void notifyDataSetChanged(ArrayList<CallData> callDataArray);

    void onDataRetrievalProgress(List<CallData> contactDataList);

    void onDataRetrievalComplete(List<CallData> contactDataList);

    void onDataRetrievalFailed(Exception failure);

    void onDataSetChanged(ContactsAdaptorListener.ChangeType type, List<Integer> changedIndices);

    void onRemoveStarted(String name, CallData.CallCategory category);

    void onRemoveFailed(String name, CallData.CallCategory category);

    void refresh();
}
