package com.avaya.android.vantage.basic.csdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.TimestampComparator;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.model.CallData;
import com.avaya.android.vantage.basic.model.CallDataContact;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.notifications.CallNotificationFactory;
import com.avaya.android.vantage.basic.views.adapters.MyRecentsRecyclerViewAdapter;
import com.avaya.clientservices.calllog.CallLogActionType;
import com.avaya.clientservices.calllog.CallLogCompletionHandler;
import com.avaya.clientservices.calllog.CallLogItem;
import com.avaya.clientservices.calllog.CallLogParticipant;
import com.avaya.clientservices.calllog.CallLogService;
import com.avaya.clientservices.calllog.CallLogServiceListener;
import com.avaya.clientservices.common.DataCollectionChangeType;
import com.avaya.clientservices.common.DataRetrievalWatcher;
import com.avaya.clientservices.common.DataRetrievalWatcherListener;
import com.avaya.clientservices.contact.Contact;
import com.avaya.clientservices.contact.ContactProviderSourceType;
import com.avaya.clientservices.contact.ExtraFieldKeys;
import com.avaya.clientservices.contact.fields.ContactEmailAddressFieldList;
import com.avaya.clientservices.contact.fields.ContactPhoneField;
import com.avaya.clientservices.contact.fields.ContactPhoneFieldList;
import com.avaya.clientservices.contact.fields.ContactStringField;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * {@link HistoryAdaptor} is used for data retrieval and processing
 */

public class HistoryAdaptor extends DataRetrievalWatcher<CallData>
        implements DataRetrievalWatcherListener<CallData>, CallLogServiceListener {

    private static final String TAG = "HistoryAdaptor";
    private static final String MISSED_CALL_ACTION = "MISSED";
    private List<CallLogItem> mCallLogItems = new ArrayList<CallLogItem>();
    private CallLogService mCallLogService;
    private WeakReference<HistoryAdaptorListener> mUiObj;
    private SharedPreferences mCallPreference;

    //ELAN-1000
    private CallNotificationFactory mNotificationFactory;

    private Context mContext;

    private boolean mIsFirstTimeLoggedUser;
    private long mLastTimeStampMissedCall;
    private int mNumberOfflineMissedCalls;
    private boolean mIsOfflineMissedCallsChanged;

    public HistoryAdaptor(Context context) {
        mContext = context;
        mCallPreference = context.getSharedPreferences(Constants.CALL_PREFS, MODE_PRIVATE);
    }

    public void setCallNotificationFactory() {
        mNotificationFactory = new CallNotificationFactory(ElanApplication.getContext());
    }

    public void missedCallsNotification(int missedCallsNotification) {//ELAN-1000
        mNotificationFactory.showMissedCallsNotification(missedCallsNotification);
    }

    public void removeMissedCallsNotification() {//ELAN-1000
        mNotificationFactory.removeMissedCallsNotification();
    }

    /**
     * Registering new {@link HistoryAdaptorListener}
     *
     * @param uiObj
     */
    public void registerListener(HistoryAdaptorListener uiObj) {
        mUiObj = new WeakReference<HistoryAdaptorListener>(uiObj);
    }

    /**
     * Set {@link CallLogService}
     *
     * @param callLogService {@link CallLogService}
     */
    public void setLogService(CallLogService callLogService) {
        Log.d(TAG, "setLogService");

        mCallLogService = callLogService;

        if (mCallLogService != null) {
            callLogService.addListener(this);
        }
        updateCallLogs();
        checkForOfflineMissedCalls();
    }

    /**
     * Delete specific {@link CallLogItem} from call log item list
     *
     * @param callData call data item from the list which is going to be removed
     */
    public void deleteCallLog(final CallData callData) {

        CallLogItem callLogItem = findLogItemByTimestamp(callData.mCallDateTimestamp);
        if (callLogItem == null) {
            Log.d(TAG, "No call log to delete was found.");
            if (mUiObj.get() != null) {
                mUiObj.get().onRemoveFailed(callData.mName, callData.mCategory);
            }
            return;
        }
        ArrayList<CallLogItem> array = new ArrayList<>();
        array.add(callLogItem);
        mCallLogService.removeCallLogs(array, new CallLogCompletionHandler() {
            // local implementation of the CallLogCompletionHandler
            @Override
            public void onSuccess() {
                Log.d(TAG, "Call log item deleting started");
                if (mUiObj.get() != null) {
                    mUiObj.get().onRemoveStarted(callData.mName, callData.mCategory);
                }
            }

            @Override
            public void onError() {
                Log.e(TAG, "Call log item cannot be deleted. ");
                if (mUiObj.get() != null) {
                    mUiObj.get().onRemoveFailed(callData.mName, callData.mCategory);
                }
            }
        });
    }

    /**
     * Delete all data from call log history
     */
    public void deleteAllCallLogs() {
        if (mCallLogService == null) {
            Log.d(TAG, "No calls in history ");
            return;
        }
        mCallLogService.removeAllCallLogs(new CallLogCompletionHandler() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Call log empty " + getCallLogs().size());
                updateCallLogs();
            }

            @Override
            public void onError() {
                Log.d(TAG, "Call log error while removing " + getCallLogs().size());
                updateCallLogs();
            }
        });
    }

    /**
     * Processing call and call for {@link #updateCallLogs()}
     *
     * @param callLogService {@link CallLogService}
     * @param callLogItems   {@link List}
     */
    @Override
    public void onCallLogServiceCallLogItemsAdded(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceCallLogItemsAdded");
        updateCallLogs();
    }

    /**
     * Processing call and call for {@link #updateCallLogs()}
     *
     * @param callLogService {@link CallLogService}
     * @param callLogItems   {@link List}
     */
    @Override
    public void onCallLogServiceCallLogItemsResynchronized(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceCallLogItemsResynchronized");
        updateCallLogs();
    }

    /**
     * Processing call and call for {@link #updateCallLogs()}
     *
     * @param callLogService {@link CallLogService}
     * @param callLogItems   {@link List}
     */
    @Override
    public void onCallLogServiceCallLogItemsRemoved(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceCallLogItemsRemoved " + mCallLogItems.size());
        updateCallLogs();
    }

    /**
     * Processing call and call for {@link #updateCallLogs()}
     *
     * @param callLogService {@link CallLogService}
     * @param callLogItems   {@link List}
     */
    @Override
    public void onCallLogServiceCallLogItemsUpdated(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceCallLogItemsUpdated");
        callLogService.resynchronizeCallLogs(new CallLogCompletionHandler() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Resynchronizing complete");
                updateCallLogs();
            }

            @Override
            public void onError() {
                Log.e(TAG, "Error while resynchronizing call logs ");
            }
        });
    }

    /**
     * Processing call and call for {@link #updateCallLogs()}
     *
     * @param callLogService {@link CallLogService}
     * @param callLogItems   {@link List}
     */
    @Override
    public void onCallLogServiceLoaded(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceLoaded");
        updateCallLogs();
    }

    @Override
    public void onCallLogServiceLoadFailed(CallLogService callLogService, List<CallLogItem> callLogItems) {
        Log.d(TAG, "onCallLogServiceLoadFailed");
    }

    @Override
    public void onCallLogServiceCapabilitiesChanged(CallLogService callLogService) {
        Log.d(TAG, "onCallLogServiceCapabilitiesChanged");
    }

    /**
     * Get full list of call logs {@link CallData}
     *
     * @return {@link CallData}
     */
    public ArrayList<CallData> getCallLogs() {
        if (mCallLogService == null) {
            Log.d(TAG, "No log calls found.");
            mCallLogItems.clear();
        } else {
            // getting the call log from service and notify the adapter about it to update the view.
            Log.d(TAG, "Call logs found. Updating list");
            mCallLogItems = mCallLogService.getCallLogs();
        }

        ArrayList<CallData> callDataArray = new ArrayList<CallData>();
        for (CallLogItem callLogItem : mCallLogItems) {
            CallData callData = convertCallData(callLogItem);
            if (callData != null) {
                callDataArray.add(callData);
            }
        }

        Collections.sort(callDataArray, new TimestampComparator());

        return callDataArray;
    }

    /**
     * If extension has enabled offline call log from SMGR, we should check if there are any
     * new missed calls. We should save the last timestamp for the new missed call and
     * update number of missed calls.
     */
    private void checkForOfflineMissedCalls() {
        mIsFirstTimeLoggedUser = mCallPreference.getBoolean(Constants.KEY_CHECK_NEW_CALL, false);
        // We should save last missed call only once after log in
        if (mIsFirstTimeLoggedUser) {
            saveOfflineMissedCalls(mCallLogService.getCallLogs());
        }
    }

    /**
     * Update history call logs
     */
    private void updateCallLogs() {
        ArrayList<CallData> callDataArrayList = getCallLogs();

        if (mUiObj == null || mUiObj.get() == null) {
            return;
        }
        
        mUiObj.get().notifyDataSetChanged(callDataArrayList);
    }

    /**
     * Convert from {@link CallLogItem} to {@link CallData}
     *
     * @param callLogItem {@link CallLogItem}
     * @return {@link CallData}
     */
    private CallData convertCallData(CallLogItem callLogItem) {

        if (callLogItem == null) {
            return null;
        }

        CallData.CallCategory callCategory = (callLogItem.getCallLogAction() == CallLogActionType.MISSED) ? CallData.CallCategory.MISSED :
                (callLogItem.getCallLogAction() == CallLogActionType.OUTGOING) ? CallData.CallCategory.OUTGOING : CallData.CallCategory.INCOMING;

        String name = callLogItem.getRemoteNumber();
        if (!callLogItem.isConference() && callLogItem.getRemoteParticipants() != null && callLogItem.getRemoteParticipants().size() > 0) {
            String displayName = callLogItem.getRemoteParticipants().get(0).getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                name = displayName;
            }
        }
        boolean nonCallableConference = false;
        // for ad-hoc IPO conference - put the hard-coded name CONFERENCE (note, that for ad-hoc conference number of the events on the call
        // will be greater than 0 since there is transfer event during ad-hoc conference creation, while for meet-me there is no events
        if (callLogItem.isConference() && (callLogItem.getCallEvents().size() > 0) && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_IPOFFICE)) {
            name = ElanApplication.getContext().getResources().getString(R.string.conference);
            nonCallableConference = true;
        }

        if (name.equalsIgnoreCase("WITHHELD")) {
            if (callCategory == CallData.CallCategory.INCOMING) {
                name = ElanApplication.getContext().getResources().getString(R.string.private_address);
                nonCallableConference = true;
            } else if (callLogItem.getRemoteNumber() != null) {
                name = callLogItem.getRemoteNumber();
            }
        }

        return new CallData(name,
                callCategory,
                callLogItem.getStartTime().toString(),
                callLogItem.getStartTime().getTime(),
                callLogItem.getStartTime().toString(),
                Long.toString(callLogItem.getDurationInSeconds()), callLogItem.getRemoteNumber(),
                "", "", getCallDataContact(callLogItem), callLogItem.getRemoteNumber(), false, nonCallableConference);
    }


    /**
     * Prepare {@link CallDataContact} dor {@link CallData}
     *
     * @param callLogItem to be used for creation of {@link CallDataContact}
     * @return prepared {@link CallDataContact}
     */
    private CallDataContact getCallDataContact(CallLogItem callLogItem) {
        CallDataContact callData = new CallDataContact();
        if (callLogItem != null && callLogItem.getRemoteParticipants() != null
                && callLogItem.getRemoteParticipants().get(0) != null) {
            Contact logItemContact = callLogItem.getRemoteParticipants().get(0).getMatchingContact();
            if (logItemContact != null) {
                callData.setmCity(logItemContact.getCity().getValue());
                callData.setmCompany(logItemContact.getCompany().getValue());

                String email = "";
                ContactEmailAddressFieldList contactEmailList = logItemContact.getEmailAddresses();
                for (int i = 0; i < contactEmailList.getValues().size(); i++) {
                    email = contactEmailList.getValues().get(i).getAddress();
                }
                callData.setmEmail(email);

                ContactStringField field = (ContactStringField) logItemContact.getExtraFields().get(ExtraFieldKeys.CONTACT_LOOK_UP_URI_STRING);
                if (field != null) {
                    callData.setmExtraField(field.getValue());
                } else {
                    callData.setmExtraField("");
                }
                callData.setmHawPicture(logItemContact.hasPicture());
                callData.setmIsFavorite(logItemContact.isFavorite().getValue());
                callData.setmLocation(logItemContact.getLocation().getValue());
                callData.setmPictureData(logItemContact.getPictureData());

                callData.setmNativeDisplayName(logItemContact.getNativeDisplayName().getValue());
                callData.setmNativeFirstName(logItemContact.getNativeFirstName().getValue());
                callData.setmNativeLastName(logItemContact.getNativeLastName().getValue());

                callData.setmTitle(logItemContact.getTitle().getValue());
                callData.setmUniqueAddressForMatching(logItemContact.getUniqueAddressForMatching());
                callData.setmPhones(getListOfPhones(logItemContact));
                callData.setmCategory(getCategory(logItemContact));
                callData.setmListOfParticipants(getParticipantsList(callLogItem));
            }
        }
        return callData;
    }

    /**
     * Obtaining list of participants and saving only display names for future use
     *
     * @param logItemContact {@link CallLogItem} from which we are obtaining data for list
     * @return ArrayList of Strings filled with display names of participants
     */
    private ArrayList<String> getParticipantsList(CallLogItem logItemContact) {
        ArrayList<String> participantNames = new ArrayList<>();
        List<CallLogParticipant> participants = logItemContact.getRemoteParticipants();
        if (participants != null && !participants.isEmpty()) {
            for (int i = 0; i < participants.size(); i++) {
                @SuppressWarnings("UnusedAssignment") String name = "";
                if (!TextUtils.isEmpty(participants.get(i).getDisplayName())) {
                    name = participants.get(i).getDisplayName();
                } else {
                    name = logItemContact.getRemoteNumber();
                }
                participantNames.add(name);
            }
        }
        return participantNames;
    }

    /**
     * Obtaining {@link ContactData.Category} for provided {@link Contact}
     *
     * @param logItemContact {@link Contact}
     * @return {@link ContactData.Category}
     */
    private ContactData.Category getCategory(Contact logItemContact) {
        return ContactData.Category.fromContactSourceType(logItemContact.getPhoneNumbers()
                .getContactProviderSourceType());
    }

    /**
     * Obtaining list of phones for provided {@link Contact} in parameter
     *
     * @param contact {@link Contact}
     * @return List of {@link ContactData.PhoneNumber}
     */
    private List<ContactData.PhoneNumber> getListOfPhones(Contact contact) {
        ContactPhoneFieldList phones = contact.getPhoneNumbers();
        List<ContactData.PhoneNumber> uiPhones = new ArrayList<>();
        for (ContactPhoneField phone : phones.getValues()) {
            ContactData.PhoneType type = ContactData.PhoneType.HOME;
            switch (phone.getType()) {

                case WORK:
                    type = ContactData.PhoneType.WORK;
                    break;
                case HOME:
                    type = ContactData.PhoneType.HOME;
                    break;
                case MOBILE:
                    type = ContactData.PhoneType.MOBILE;
                    break;
                case HANDLE:
                    type = ContactData.PhoneType.HANDLE;
                    break;
                case FAX:
                    type = ContactData.PhoneType.FAX;
                    break;
                case PAGER:
                    type = ContactData.PhoneType.PAGER;
                    break;
                case ASSISTANT:
                    type = ContactData.PhoneType.ASSISTANT;
                    break;
                case OTHER:
                    type = ContactData.PhoneType.OTHER;
                    break;
            }
            uiPhones.add(new ContactData.PhoneNumber(phone.getPhoneNumber(), type, phone.isDefault(), null));
        }
        return uiPhones;
    }

    /**
     * Find {@link CallLogItem} from list of items. In case there is no item found returning null
     *
     * @param callDataTimestamp timestamp of the call log item
     * @return {@link CallLogItem} found
     */
    private CallLogItem findLogItemByTimestamp(final long callDataTimestamp) {
        if (mCallLogItems.isEmpty()) {
            return null;
        }

        for (int i = 0; i < mCallLogItems.size(); i++) {
            if (mCallLogItems.get(i).getStartTime().getTime() == callDataTimestamp) {
                return mCallLogItems.get(i);
            }

        }

        return null;
    }

    @Override
    public void onRetrievalProgress(DataRetrievalWatcher<CallData> watcher, boolean determinate, int numRetrieved, int total) {

    }

    @Override
    public void onRetrievalCompleted(DataRetrievalWatcher<CallData> watcher) {

    }

    @Override
    public void onRetrievalFailed(DataRetrievalWatcher<CallData> watcher, Exception failure) {

    }

    @Override
    public void onCollectionChanged(DataRetrievalWatcher<CallData> watcher, DataCollectionChangeType changeType, List<CallData> changedItems) {

    }

    /**
     * We should store number of new offline missed calls and time of the last missed call.
     *
     * @param callLogItems List of synced call logs
     */
    private void saveOfflineMissedCalls(List<CallLogItem> callLogItems) {
        if (callLogItems == null || callLogItems.isEmpty()) {
            return;
        }

        // Call logs time should be unique for the returned list of the call log
        HashSet<Long> callLogsItemSet = new HashSet<>();
        for (CallLogItem callLogItem : callLogItems) {
            if (callLogItem.getCallLogAction().equals(CallLogActionType.MISSED)) {
                callLogsItemSet.add(callLogItem.getStartTime().getTime());
            }
        }

        mIsOfflineMissedCallsChanged = false;
        mNumberOfflineMissedCalls = mCallPreference.getInt(Constants.KEY_UNSEEN_MISSED_CALLS, 0);
        mLastTimeStampMissedCall = mCallPreference.getLong(Constants.KEY_CALL_TIMESTAMP, 0);

        for (Long timestamp : callLogsItemSet) {
            if (timestamp > mLastTimeStampMissedCall) {
                mNumberOfflineMissedCalls++;
                mIsOfflineMissedCallsChanged = true;
            }
        }

        // We should set maximum timestamp of all missed calls
        if(!callLogsItemSet.isEmpty())
            mLastTimeStampMissedCall = Collections.max(callLogsItemSet);

        SharedPreferences.Editor editor = mCallPreference.edit();
        editor.putBoolean(Constants.KEY_CHECK_NEW_CALL, false);
        if (mIsOfflineMissedCallsChanged) {
            editor.putInt(Constants.KEY_UNSEEN_MISSED_CALLS, mNumberOfflineMissedCalls);
            editor.putLong(Constants.KEY_CALL_TIMESTAMP, mLastTimeStampMissedCall);

            // We should send broadcast only in case there are new offline missed calls
            Utils.refreshHistoryIcon(mContext, mNumberOfflineMissedCalls);
        }
        editor.apply();
    }
}
