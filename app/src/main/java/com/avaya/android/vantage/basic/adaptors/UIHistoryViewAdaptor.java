package com.avaya.android.vantage.basic.adaptors;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.csdk.ContactsAdaptorListener;
import com.avaya.android.vantage.basic.csdk.HistoryAdaptorListener;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.model.CallData;
import com.avaya.android.vantage.basic.views.interfaces.IRecentCallsViewInterface;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

/**
 * Adapter for showing properly formated list in {@link com.avaya.android.vantage.basic.fragments.RecentCallsFragment}
 * {@link UIContactsViewAdaptor} implements {@link HistoryAdaptorListener} and overrides
 * some of its methods
 */

public class UIHistoryViewAdaptor implements HistoryAdaptorListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private static final String SYNC_HISTORY = Constants.SYNC_HISTORY;

    private SwipeRefreshLayout mSwipeRefresh;
    private IRecentCallsViewInterface mViewInterface;
    private final CallData mPending;

    List<CallData> mContacts = new LinkedList<>();

    /**
     * Public constructor for {@link UIHistoryViewAdaptor}
     */
    public UIHistoryViewAdaptor(){
        mPending = CallData.getDummyContactForPendingUpdate();
    }

    /**
     * Obtain list of {@link CallData}
     * @return list of {@link CallData}
     * @param applicationContext
     */
    @Override
    public List<CallData> getContacts(Context applicationContext) {
        return mContacts;
    }

    /**
     * Provide logic for update of data set and call for recreating data in {@link #mViewInterface}
     * @param callDataArray array list of {@link CallData}
     */
    @Override
    public void notifyDataSetChanged(ArrayList<CallData> callDataArray) {

        Log.d(LOG_TAG, "notifyDataSetChanged");

        for (CallData callData : callDataArray) {
            Log.d (LOG_TAG, "Call: " + callData + "\n");
        }
        mContacts.clear();
        mContacts = callDataArray;
        if(mViewInterface!=null) {
            mViewInterface.recreateData(callDataArray);
        }
    }

    /**
     * On retrieval of data completed we will stop refreshing {@link #mSwipeRefresh} and remove
     * {@link #mPending} from {@link #mViewInterface}
     * @param contactDataList to be set to {@link #mContacts}
     */
    @Override
    public void onDataRetrievalComplete(List<CallData> contactDataList) {
        Log.d(LOG_TAG,"onDataRetrievalComplete");
        mContacts = contactDataList;
        if(mViewInterface!=null) {
            mViewInterface.removeItem(mPending);
        }
        if(mSwipeRefresh!=null)
            mSwipeRefresh.setRefreshing(false);
    }

    /**
     * While data retrieval is in progress we will in {@link #mViewInterface} start recreating data
     * with provided list of {@link CallData} in parameter. Also we will add to {@link #mViewInterface}
     * item {@link #mPending}
     * @param contacts list of {@link CallData} to be recreated
     */
    @Override
    public void onDataRetrievalProgress(List<CallData> contacts) {
        Log.d(LOG_TAG,"onDataRetrievalProgress");
        if(mViewInterface!=null) {
            mViewInterface.recreateData(contacts);
            mViewInterface.addItem(mPending);
        }
    }

    /**
     * In case data retrieval failed we are getting failure {@link Exception} as parameter
     * @param failure {@link Exception}
     */
    @Override
    public void onDataRetrievalFailed(Exception failure) {
        if(mSwipeRefresh!=null) {
            mSwipeRefresh.setRefreshing(false);
        }
    }

    /**
     * Updating data
     * @param type
     * @param changedIndices
     */
    @Override
    public void onDataSetChanged(ContactsAdaptorListener.ChangeType type, List<Integer> changedIndices) {

    }

    @Override
    public void onRemoveStarted(String name, CallData.CallCategory category) {
        Log.d(LOG_TAG, "onRemoveStarted");
    }

    @Override
    public void onRemoveFailed(String name, CallData.CallCategory category) {
        Log.d(LOG_TAG, "onRemoveFailed");
    }

    /**
     * Refreshing {@link #mContacts} with data obtained from {@link SDKManager}
     */
    @Override
    public void refresh() {
        mContacts = SDKManager.getInstance().getHistoryAdaptor().getCallLogs();
    }

    /**
     * Deleting call logs from {@link SDKManager} based on name and {@link CallData.CallCategory}
     *
     * @param callData of call log to be deleted
     */
    public void deleteCallLog(final CallData callData) {
        SDKManager.getInstance().getHistoryAdaptor().deleteCallLog(callData);
    }

    /**
     * Removing all call logs and obtaining list of call logs after removal
     */
    public void deleteAllCallLogs(){
        SDKManager.getInstance().getHistoryAdaptor().deleteAllCallLogs();
        mContacts = SDKManager.getInstance().getHistoryAdaptor().getCallLogs();
    }

    /**
     * Performing refresh of {@link SwipeRefreshLayout} and obtaining list of
     * {@link CallData} from {@link SDKManager}
     * @param mSwipeLayout {@link SwipeRefreshLayout} to be refreshed
     */
    public void refresh(SwipeRefreshLayout mSwipeLayout) {
        mSwipeRefresh = mSwipeLayout;
        mContacts = SDKManager.getInstance().getHistoryAdaptor().getCallLogs();
    }

    /**
     * Setting {@link IRecentCallsViewInterface} as view interface for {@link #mViewInterface}
     * @param viewInterface to be set in {@link #mViewInterface}
     */
    public void setViewInterface(IRecentCallsViewInterface viewInterface) {
        this.mViewInterface = viewInterface;
    }
}
