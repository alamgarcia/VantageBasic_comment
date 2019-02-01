package com.avaya.android.vantage.basic.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.adaptors.UIHistoryViewAdaptor;
import com.avaya.android.vantage.basic.csdk.ConfigParametersNames;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.settings.BlueHelper;
import com.avaya.android.vantage.basic.model.CallData;
import com.avaya.android.vantage.basic.views.SlideAnimation;
import com.avaya.android.vantage.basic.views.adapters.MyRecentsRecyclerViewAdapter;

import java.util.List;

import static com.avaya.android.vantage.basic.fragments.settings.BlueHelper.CONTACT_SYNC_DURATION;
import static com.avaya.android.vantage.basic.fragments.settings.BlueHelper.ContactSync.CallHistorySharingOff;
import static com.avaya.android.vantage.basic.fragments.settings.BlueHelper.ContactSync.CallHistorySharingOn;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnContactInteractionListener}
 * interface.
 */

public class RecentCallsFragment extends android.support.v4.app.Fragment implements View.OnClickListener,
        MyRecentsRecyclerViewAdapter.IRecentsCallback, MyRecentsRecyclerViewAdapter.ItemRemovedListener {

    private static final String TAG = "RecentCallsFragment";

    private MyRecentsRecyclerViewAdapter mRecentCallsAdapter;
    private OnContactInteractionListener mListener;


    private LinearLayout mHistoryFilter, mContainerAllHistory, mContainerMissedCalls, mContainerOutCalls, mContainerIncomingCalls, fragmentRecentCallsHeader,containerHistoryBT;

    private OnFilterCallsInteractionListener mFilterCallsInteractionListener;
    private LinearLayout mSyncDialog;
    private RelativeLayout mDeleteDialog;
    private TextView mRecentFilter, mContainerDeleteAllHistory, mNoRecentCalls, mSyncPopupText, mSyncPopupTitle;
    private TextView mCancelSyncPopUp, mOpenBluetoothSettings, mOpenMoreSettings, mOKSyncPopUp;
    private TextView mConfirmDelete, mCancelDelete;
    private ImageView mAllCheck, mMissedCallsCheck, mOutCheck, mInCheck, mSyncContacts, mExitDeleteDialog, mExitButton;
    private FrameLayout mRrecentCallFilterContainer;
    private RecyclerView mRecentList;
    private static final String NAME_DISPLAY_PREFERENCE = Constants.NAME_DISPLAY_PREFERENCE;
    private static final String USER_PREFERENCE = Constants.USER_PREFERENCE;
    private static final String SYNC_HISTORY = Constants.SYNC_HISTORY;
    private static final String ADD_PARTICIPANT = "addParticipant";
    private static final int FIRST_NAME_FIRST = Constants.FIRST_NAME_FIRST;
    private static final int LAST_NAME_FIRST = Constants.LAST_NAME_FIRST;
    private SharedPreferences mUserPreference;
    private SharedPreferences mConnectionPref;
    private boolean addParticipant;
    private int mNameDisplayType;
    private int mPositionToBeDeleted;
    private CallData itemToDelete;
    private Parcelable position;

    private String ADMIN_NAME_DISPLAY_ORDER = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_DISPLAY_ORDER);


    private List<CallData> mCallList;
    private UIHistoryViewAdaptor mUIHistoryContactsAdaptor;

    SlideAnimation mFilterSlider;
    SlideAnimation mSyncUpSlider;
    SlideAnimation mDeleteCallHistory;

    private Handler mHandler;
    private Runnable mLayoutCloseRunnable;

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";

    private BlueHelper blueHelper;

    private ImageView contactsBluetoothSyncIcon;
    public AlertDialog alertDialog;

    private Snackbar mSnackBar;

    private View.OnClickListener mCancelSnackbarAction = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mSnackBar.dismiss();
            if (blueHelper != null) {
                final boolean isSharingEnabled = blueHelper.isCallHistorySharingEnabled();
                blueHelper.setContactSharing(isSharingEnabled
                        ? CallHistorySharingOff : CallHistorySharingOn);
            }
        }
    };

    public boolean mUserVisibleHint = false;

    private CallData.CallCategory previousCallCategory = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            addParticipant = getArguments().getBoolean(ADD_PARTICIPANT);
        }
        blueHelper = BlueHelper.instance();
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        if(menuVisible) {
            mUserVisibleHint = true;
        }else {
            mUserVisibleHint = false;
        }
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static RecentCallsFragment newInstance(int columnCount, boolean addParticipant) {
        RecentCallsFragment fragment = new RecentCallsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putBoolean(ADD_PARTICIPANT, addParticipant);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_recent_call_list, container, false);

        mRecentList = (RecyclerView) root.findViewById(R.id.list);
        mRecentList.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                checkListCount();
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {

            }
        });


        mFilterSlider = new SlideAnimation();
        mSyncUpSlider = new SlideAnimation();
        mDeleteCallHistory = new SlideAnimation();
        mHistoryFilter = (LinearLayout) root.findViewById(R.id.select_history_filter);
        mSyncDialog = (LinearLayout) root.findViewById(R.id.sync_pop_up);
        mSyncDialog.setVisibility(View.GONE);
        mDeleteDialog = (RelativeLayout) root.findViewById(R.id.delete_call_history);
        mDeleteDialog.setVisibility(View.GONE);
        mSyncPopupText = (TextView) root.findViewById(R.id.sync_dialog_description);
        mSyncPopupTitle = (TextView) root.findViewById(R.id.sync_dialog_title);
        mCancelSyncPopUp = (TextView) mSyncDialog.findViewById(R.id.sync_dialog_cancel);
        mOKSyncPopUp = (TextView) mSyncDialog.findViewById(R.id.sync_dialog_ok);

        mCancelDelete = (TextView) mDeleteDialog.findViewById(R.id.delete_callhistory_dialog_cancel);
        mConfirmDelete = (TextView) mDeleteDialog.findViewById(R.id.delete_callhistory_dialog_ok);
        mRecentFilter = (TextView) root.findViewById(R.id.filterRecent);
        if (isAdded() && Utils.isLandScape()) {
            mRecentFilter.setVisibility(View.GONE);
        } else {
            mRecentFilter.setVisibility(View.VISIBLE);
        }
        mRrecentCallFilterContainer = (FrameLayout) root.findViewById(R.id.frameRecentCallFilter);
        mContainerAllHistory = (LinearLayout) root.findViewById(R.id.containerAllHistory);
        mContainerMissedCalls = (LinearLayout) root.findViewById(R.id.containerMissedCalls);
        containerHistoryBT = (LinearLayout) root.findViewById(R.id.containerHistoryBT);
        mContainerOutCalls = (LinearLayout) root.findViewById(R.id.containerOugtoingCalls);
        mContainerIncomingCalls = (LinearLayout) root.findViewById(R.id.containerIncomingCalls);
        mContainerDeleteAllHistory = (TextView) root.findViewById(R.id.containerDeleteAllHistory);
        mAllCheck = (ImageView) root.findViewById(R.id.recentAllCheck);
        mMissedCallsCheck = (ImageView) root.findViewById(R.id.recentMissedCheck);
        mOutCheck = (ImageView) root.findViewById(R.id.recentOutCheck);
        mInCheck = (ImageView) root.findViewById(R.id.recentIncomingCheck);
        mNoRecentCalls = (TextView) root.findViewById(R.id.noRecentCalls);
        mSyncContacts = (ImageView) root.findViewById(R.id.sync_contacts);
        if (isAdded() && Utils.isLandScape()) {
            mSyncContacts.setVisibility(View.GONE);
        } else {
            mSyncContacts.setVisibility(View.VISIBLE);
        }
        mExitButton = (ImageView) mSyncDialog.findViewById(R.id.call_features_close);
        mExitDeleteDialog = (ImageView) mDeleteDialog.findViewById(R.id.call_features_close);

        fragmentRecentCallsHeader = (LinearLayout) root.findViewById(R.id.fragment_recent_calls_header);
        if (isAdded() && Utils.isLandScape()) {
            fragmentRecentCallsHeader.setVisibility(View.GONE);
        } else {
            fragmentRecentCallsHeader.setVisibility(View.VISIBLE);
        }

        contactsBluetoothSyncIcon = (ImageView) root.findViewById(R.id.recent_call_image_view_bt);

        // setting on click listeners
        mRecentFilter.setOnClickListener(this);
        mContainerAllHistory.setOnClickListener(this);
        mContainerMissedCalls.setOnClickListener(this);

        if (Utils.isLandScape())
            containerHistoryBT.setOnClickListener(this);

        mContainerOutCalls.setOnClickListener(this);
        mContainerIncomingCalls.setOnClickListener(this);
        mContainerDeleteAllHistory.setOnClickListener(this);
        mRrecentCallFilterContainer.setOnClickListener(this);
        setClickListenersForSyncDialog();
        setClickListenerForDeleteCallLog();

        mUserPreference = getActivity().getSharedPreferences(USER_PREFERENCE, Context.MODE_PRIVATE);
        mConnectionPref = getActivity().getSharedPreferences(Constants.CONNECTION_PREFS, Context.MODE_PRIVATE);

        //setting up slider animation
        mFilterSlider.reDrawListener(mHistoryFilter);
        mSyncUpSlider.reDrawListener(mSyncDialog);
        mDeleteCallHistory.reDrawListener(mDeleteDialog);

        initRecyclerView();
        initSnackBar();
        initFilter();

        mHandler = new Handler();
        mLayoutCloseRunnable = new Runnable() {
            @Override
            public void run() {
                hideMenus();
                if (isAdded() && Utils.isLandScape()  && (MainActivity) getActivity() != null) {
                    ((MainActivity)getActivity()).filterButton.setImageResource(R.drawable.ic_expand_more);
                    ((MainActivity)getActivity()).showingFirstRecent = true;
                }
            }
        };

        if(mRecentCallsAdapter != null)
            mRecentCallsAdapter.setOnItemClickListener(this);

        return root;
    }

    private void initSnackBar() {
        mSnackBar = Snackbar.make(getActivity().findViewById(android.R.id.content),
                R.string.waitBluetoothCallHistorySharing, Snackbar.LENGTH_INDEFINITE);
    }

    private void initFilter() {
        if (Utils.isLandScape() && isAdded() && getActivity() != null) {
            ((MainActivity) getActivity()).filterButton.setImageResource(R.drawable.ic_expand_more);
            ((MainActivity) getActivity()).showingFirstRecent = true;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mUserVisibleHint) {
            if (Utils.isLandScape() && isAdded() && (MainActivity) getActivity() != null)
                ((MainActivity) getActivity()).filterButton.setVisibility(View.VISIBLE);
        } else {
            if (Utils.isLandScape() && isAdded() && (MainActivity) getActivity() != null)
                ((MainActivity)getActivity()).filterButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(alertDialog!=null)
            alertDialog.dismiss();
    }

    /**
     * Does the following:<br>
     * - Updates state of the Call History bluetooth sync icon,<br>
     * - Sets the {@link MyRecentsRecyclerViewAdapter#setmFilteringPaired(boolean)}<br>
     * - Notifies recent calls adapter of the changed data set<br>
     */
    public void prepareSyncIcon() {
        boolean isFilteringPaired = updateSyncIconState();
        if (mRecentCallsAdapter != null && isVisible()) {
            mRecentCallsAdapter.setmFilteringPaired(isFilteringPaired);
            mRecentCallsAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Update bluetooth Call History Sharing sync icon state.
     *
     * @return true if we should filter out paired Call Log items, false otherwise
     */
    public boolean updateSyncIconState() {
        boolean isFilteringPaired;
        if (blueHelper.isBluetoothLinkEstablished()) {
            if (blueHelper.isCallHistorySharingEnabled()) { // show blue icon
                setSyncContactImage(R.drawable.ic_sync_paired_on);
                isFilteringPaired = false;
            } else { // show grey icon
                setSyncContactImage(R.drawable.ic_sync_paired_off_grey);
                isFilteringPaired = true;
            }
        } else { // show dialog confirm -> go to device Bluetooth Settings
            setSyncContactImage(R.drawable.ic_sync_unpaired_grey);
            isFilteringPaired = false;
        }
        return isFilteringPaired;
    }

    /**
     * Collapse Filter Menu, Sync Pop and Delete dialogs if opened.
     */
    private void hideMenus() {
        try {
            mHandler.removeCallbacks(mLayoutCloseRunnable);
            if (Utils.isLandScape()) {
                mFilterSlider.collapse(mHistoryFilter, true);
                mSyncUpSlider.collapse(mSyncDialog, false);
                mDeleteCallHistory.collapse(mDeleteDialog, true);
            } else {
                mFilterSlider.collapse(mHistoryFilter, false);
                mSyncUpSlider.collapse(mSyncDialog, false);
                mDeleteCallHistory.collapse(mDeleteDialog, false);
            }
            mRrecentCallFilterContainer.setVisibility(View.GONE);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Initialize recycler view used for {@link RecentCallsFragment}
     *
     */
    private void initRecyclerView() {
        // Set the mRecentSpinnerAdapter
        if (mUIHistoryContactsAdaptor != null) {
            // Revert refresh data in order not to cause potential additional bugs
            // We should manually refresh data in case updateCallLogs() is not triggered immediately
            // by the service
            mUIHistoryContactsAdaptor.refresh();
            mCallList = mUIHistoryContactsAdaptor.getContacts(getActivity().getApplicationContext());

            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            mRecentList.setLayoutManager(layoutManager);
            mRecentCallsAdapter = null;
            mRecentCallsAdapter = new MyRecentsRecyclerViewAdapter(mCallList, mListener, this);
            mRecentCallsAdapter.setmFilteringPaired(isFilteringPaired());
            mRecentCallsAdapter.setAddParticipant(addParticipant);
            nameDisplaySet();
            mUIHistoryContactsAdaptor.setViewInterface(mRecentCallsAdapter);
            mRecentList.setAdapter(mRecentCallsAdapter);
            checkListCount();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnContactInteractionListener) {
            mListener = (OnContactInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnContactsFragmentInteractionListener");
        }

        if (context instanceof OnFilterCallsInteractionListener) {
            mFilterCallsInteractionListener = (OnFilterCallsInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFilterCallsInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        nameDisplaySet();
//      mRecentCallsAdapter.recreateData(mCallList);
        checkListCount();
        prepareSyncIcon();
    }

    /**
     * Managing name display method (first name first, last name first).
     */
    private void nameDisplaySet() {
        int defaultValue = Constants.FIRST_NAME_FIRST;

        if (ADMIN_NAME_DISPLAY_ORDER != null){
            // doing this to prevent bug in case someone entered a value that is different from 1 or 0.
            if (ADMIN_NAME_DISPLAY_ORDER.equals("last,first")){
                defaultValue = LAST_NAME_FIRST;
            } else {
                defaultValue = FIRST_NAME_FIRST;
            }
        }

        int nameDisplay = mUserPreference.getInt(NAME_DISPLAY_PREFERENCE, defaultValue);

        if (nameDisplay != mNameDisplayType && mRecentCallsAdapter != null) {
            mRecentCallsAdapter.firstNameFirst(nameDisplay != LAST_NAME_FIRST);
            mRecentCallsAdapter.notifyDataSetChanged();
            mNameDisplayType = nameDisplay;
        }

    }

    /**
     * Checking if there is any data in the list. If not,
     * showing a message there there are no calls.
     */
    private void checkListCount() {
        if (mRecentCallsAdapter != null && mNoRecentCalls != null) {
            if ((MainActivity)getActivity() !=null && mRecentCallsAdapter.getItemCountWithFilter(((MainActivity)getActivity()).mSelectedCallCategory) == 0) {
                mNoRecentCalls.setVisibility(View.VISIBLE);
            } else {
                mNoRecentCalls.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.filterRecent:
                if(((MainActivity)getActivity()).showingFirstRecent) {
                    mRrecentCallFilterContainer.setVisibility(View.VISIBLE);
                    if (Utils.isLandScape()) {
                        mFilterSlider.expand(mHistoryFilter,true);
                    } else {
                        mFilterSlider.expand(mHistoryFilter,false);
                    }

                    if (Utils.isLandScape()) {
                        mSyncUpSlider.collapse(mSyncDialog, true);
                        mDeleteCallHistory.collapse(mDeleteDialog, true);
                    } else {
                        mSyncUpSlider.collapse(mSyncDialog, false);
                        mDeleteCallHistory.collapse(mDeleteDialog, false);
                    }
                    mHandler.postDelayed(mLayoutCloseRunnable, Constants.LAYOUT_DISAPPEAR_TIME);
                    ((MainActivity)getActivity()).showingFirstRecent = false;
                }else {
                    hideMenus();
                    ((MainActivity)getActivity()).showingFirstRecent = true;
                }
                return;
            case R.id.containerAllHistory:
                performSelectionByCategory(CallData.CallCategory.values()[0]);
                ((MainActivity)getActivity()).showingFirstRecent = true;
                break;
            case R.id.containerMissedCalls:
                performSelectionByCategory(CallData.CallCategory.values()[1]);
                ((MainActivity)getActivity()).showingFirstRecent = true;
                break;
            case R.id.containerOugtoingCalls:
                performSelectionByCategory(CallData.CallCategory.values()[2]);
                ((MainActivity)getActivity()).showingFirstRecent = true;
                break;
            case R.id.containerIncomingCalls:
                performSelectionByCategory(CallData.CallCategory.values()[3]);
                ((MainActivity)getActivity()).showingFirstRecent = true;
                break;
            case R.id.containerDeleteAllHistory:
                performSelectionByCategory(CallData.CallCategory.values()[4]);
                ((MainActivity)getActivity()).showingFirstRecent = true;
                break;
            case R.id.containerHistoryBT:
                syncContactsClick();
                ((MainActivity)getActivity()).showingFirstRecent = true;
                break;
            default:
                hideMenus();
                ((MainActivity)getActivity()).showingFirstRecent = true;
                break;
        }

        if (Utils.isLandScape() && isAdded() && (MainActivity) getActivity() != null) {
            ((MainActivity)getActivity()).filterButton.setImageResource(R.drawable.ic_expand_more);
            ((MainActivity)getActivity()).showingFirstRecent = true;
        }

        hideMenus();
        checkListCount();
        scrollToFirstPosition();
    }

    /**
     * Set recent call log list based on current call category
     *
     * @param callDataCategory Current call data category
     */
    public void performSelectionByCategory(CallData.CallCategory callDataCategory) {
        if (callDataCategory == null || mRecentCallsAdapter == null) {
            return;
        }

        if (callDataCategory.equals(CallData.CallCategory.ALL)) {
            mRecentCallsAdapter.clearFilter();
            uncheck();
            mRecentFilter.setText(getText(R.string.recent_call_all));
            mAllCheck.setVisibility(View.VISIBLE);
            //mFilterCallsInteractionListener.onSaveSelectedCategoryRecentFragment(CallData.CallCategory.ALL);
            previousCallCategory = CallData.CallCategory.ALL;
            ((MainActivity)getActivity()).mSelectedCallCategory = CallData.CallCategory.ALL;

        } else if (callDataCategory.equals(CallData.CallCategory.MISSED)) {
            mRecentCallsAdapter.setFilter(CallData.CallCategory.values()[1]);
            uncheck();
            mRecentFilter.setText(getText(R.string.recent_call_missed));
            mMissedCallsCheck.setVisibility(View.VISIBLE);
           // mFilterCallsInteractionListener.onSaveSelectedCategoryRecentFragment(CallData.CallCategory.MISSED);
            previousCallCategory = CallData.CallCategory.MISSED;
            ((MainActivity)getActivity()).mSelectedCallCategory = CallData.CallCategory.MISSED;

        } else if (callDataCategory.equals(CallData.CallCategory.OUTGOING)) {
            mRecentCallsAdapter.setFilter(CallData.CallCategory.values()[2]);
            uncheck();
            mRecentFilter.setText(getText(R.string.recent_call_outgoing));
            mOutCheck.setVisibility(View.VISIBLE);
           // mFilterCallsInteractionListener.onSaveSelectedCategoryRecentFragment(CallData.CallCategory.OUTGOING);
            previousCallCategory = CallData.CallCategory.OUTGOING;
            ((MainActivity)getActivity()).mSelectedCallCategory = CallData.CallCategory.OUTGOING;
        } else if (callDataCategory.equals(CallData.CallCategory.INCOMING)) {
            mRecentCallsAdapter.setFilter(CallData.CallCategory.values()[3]);
            uncheck();
            mRecentFilter.setText(getText(R.string.recent_call_incoming));
            mInCheck.setVisibility(View.VISIBLE);
           // mFilterCallsInteractionListener.onSaveSelectedCategoryRecentFragment(CallData.CallCategory.INCOMING);
            previousCallCategory = CallData.CallCategory.INCOMING;
            ((MainActivity)getActivity()).mSelectedCallCategory = CallData.CallCategory.INCOMING;
        } else if (callDataCategory.equals(CallData.CallCategory.DELETE)) {
            mRecentCallsAdapter.setFilter(CallData.CallCategory.values()[4]);
            mUIHistoryContactsAdaptor.deleteAllCallLogs();
            mCallList.clear();
            checkListCount();
            mRecentCallsAdapter.clearFilter();
            mFilterCallsInteractionListener.refreshHistoryIcon();
           // mFilterCallsInteractionListener.onSaveSelectedCategoryRecentFragment(CallData.CallCategory.DELETE);
            if(previousCallCategory!=null) {
                switch (previousCallCategory) {
                    case DELETE:
                        performSelectionByCategory(CallData.CallCategory.DELETE);
                        break;
                    case MISSED:
                        performSelectionByCategory(CallData.CallCategory.MISSED);
                        break;
                    case INCOMING:
                        performSelectionByCategory(CallData.CallCategory.INCOMING);
                        break;
                    case OUTGOING:
                        performSelectionByCategory(CallData.CallCategory.OUTGOING);
                        break;
                    case ALL:
                        performSelectionByCategory(CallData.CallCategory.ALL);
                        break;
                }
            }
        }
    }

    /**
     * Set all check views to INVISIBLE
     */
    private void uncheck() {
        mInCheck.setVisibility(View.INVISIBLE);
        mAllCheck.setVisibility(View.INVISIBLE);
        mMissedCallsCheck.setVisibility(View.INVISIBLE);
        mOutCheck.setVisibility(View.INVISIBLE);

    }

    /**
     * Setting up {@link UIHistoryViewAdaptor}
     *
     * @param UIHistoryAdaptor
     */
    public void setUIHistoryContactsAdaptor(UIHistoryViewAdaptor UIHistoryAdaptor) {
        this.mUIHistoryContactsAdaptor = UIHistoryAdaptor;
    }

    /**
     * Get list position for {@link RecentCallsFragment} in {@link Parcelable}
     *
     * @return {@link Parcelable}
     */
    public Parcelable getListPosition() {
        if (mRecentList != null) {
            return mRecentList.getLayoutManager().onSaveInstanceState();
        } else {
            return null;
        }
    }

    /**
     * Restore list position from {@link Parcelable}
     *
     * @param position
     */
    public void restoreListPosition(Parcelable position) {
        if (mRecentList != null && position != null) {
            mRecentList.getLayoutManager().onRestoreInstanceState(position);
        }
    }

    /**
     * This method will be called every time RecentCalls fragment is active.
     * Can be called even when application is being recreated so we need to
     * check if activity still exist.
     */
    public void fragmentSelected(CallData.CallCategory callCategory) {
        Log.e(TAG, "fragmentSelected: Recent");
        if (getActivity() != null) {
            nameDisplaySet();
            prepareSyncIcon();
            performSelectionByCategory(callCategory);
        }
    }

    /**
     * Scroll to the first element in the recent calls list.
     * This method will be called either when user clicked from tabs history tab or when
     * changing category filter in the history tab.
     */
    private void scrollToFirstPosition() {
        if (mRecentList != null && mRecentCallsAdapter !=null && mRecentCallsAdapter.getItemCount() > 0) {
            mRecentList.scrollToPosition(0);
        }
    }

    /**
     * Save last visible item in the case when bluetooth loader
     * is restarted. Fragment necessary recreation causes that we
     * lose state when call logs items are fetched async from
     * framework database.
     *
     * TODO Check this with the new call history architecture
     *
     * @param position
     */
    public void setLastVisibleItem(Parcelable position) {
        this.position = position;
    }

    /**
     * Setting up is view open for adding participant from join conference call
     *
     * @param add
     */
    public void setAddParticipantData(boolean add) {
        addParticipant = add;
        if (mRecentCallsAdapter != null && mRecentList != null) {
            mRecentCallsAdapter.setAddParticipant(addParticipant);
            mRecentCallsAdapter.notifyDataSetChanged();
            mRecentList.setAdapter(mRecentCallsAdapter);
        }
    }

    /**
     * Click listeners for sync feature.
     */
    private void setClickListenersForSyncDialog () {
        mSyncContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncContactsClick();
            }
        });
        mCancelSyncPopUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isLandScape()) {
                    mSyncUpSlider.collapse(mSyncDialog,true);
                } else {
                    mSyncUpSlider.collapse(mSyncDialog,false);
                }

            }
        });
        mOKSyncPopUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isLandScape()) {
                    mSyncUpSlider.collapse(mSyncDialog,false);
                }else{
                    mSyncUpSlider.collapse(mSyncDialog,false);
                }

                Intent openBluetoothIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(openBluetoothIntent);
            }
        });
    }

    /**
     * Handles click on the Recent Calls bluetooth sync icon.<br>
     * Method uses {@link BlueHelper} instance to determine:<br>
     *     - state of the bluetooth<br>
     *     - state of potential connected device and <br>
     *     - is CallHistorySharing enabled.<br>
     * Based on these states, bluetooth sync icon is updated as well as the Recent Calls list.
     */
    public void syncContactsClick() {
        if (mSnackBar.isShown() || mSyncDialog.getVisibility() == View.VISIBLE) return;
        if (blueHelper.isBluetoothLinkEstablished()) {
            boolean isFilteringPaired;
            if (blueHelper.isCallHistorySharingEnabled()) {
                // turn Off Call History Sharing
                mSnackBar.setText(R.string.turnOffBluetoothCallHistorySharing)
                        .setDuration(Snackbar.LENGTH_SHORT).show();
                blueHelper.setContactSharing(CallHistorySharingOff);
                setSyncContactImage(R.drawable.ic_sync_paired_off_grey);
                isFilteringPaired = true;
            } else { // turn On Call History Sharing
                mSnackBar.setText(R.string.waitBluetoothCallHistorySharing)
                        .setAction(R.string.cancel, mCancelSnackbarAction)
                        .setDuration(CONTACT_SYNC_DURATION).show();
                blueHelper.setContactSharing(CallHistorySharingOn);
                setSyncContactImage(R.drawable.ic_sync_paired_on);
                isFilteringPaired = false;
            }
            if (mRecentCallsAdapter != null) {
                mRecentCallsAdapter.setmFilteringPaired(isFilteringPaired);
                mRecentCallsAdapter.notifyDataSetChanged();
            }
        } else { // show Dialog confirm -> go to Device Bluetooth Settings
            mSyncPopupText.setText(R.string.sync_call_history_text);
            mSyncPopupTitle.setText(R.string.sync_call_history_title);
            if (Utils.isLandScape()) {
                mSyncUpSlider.expand(mSyncDialog,true);
            } else {
                mSyncUpSlider.expand(mSyncDialog,false);
            }

            if (Utils.isLandScape()) {
                syncPopupAlertDialog();
            }
        }
    }

    /**
     * Sets tge image resource for the Recent Calls bluetooth sync icon.<br>
     *
     * @param syncIconRes Drawable resource to set
     */
    private void setSyncContactImage(@DrawableRes int syncIconRes) {
        if(mSyncContacts != null) {
            if (!Utils.isLandScape()){
                mSyncContacts.setImageResource(syncIconRes);
            } else{
                contactsBluetoothSyncIcon.setImageResource(syncIconRes);
            }
        }
    }

    /**
     * Implement content and the functions of the elements of the Sync Popup Dialog
     */
    private void syncPopupAlertDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        // ...Irrelevant code for customizing the buttons and title
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.sync_pop_up_layout, null);
        dialogBuilder.setView(dialogView);


        mCancelSyncPopUp = (TextView) dialogView.findViewById(R.id.sync_dialog_cancel);
        mSyncPopupText = (TextView) dialogView.findViewById(R.id.sync_dialog_description);
        mSyncPopupTitle = (TextView) dialogView.findViewById(R.id.sync_dialog_title);
        mOKSyncPopUp = (TextView) dialogView.findViewById(R.id.sync_dialog_ok);

        mOKSyncPopUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isLandScape()) {
                    mSyncUpSlider.collapse(mSyncDialog,true);
                }else{
                    mSyncUpSlider.collapse(mSyncDialog,false);
                }

                Intent openBluetoothIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(openBluetoothIntent);
                alertDialog.dismiss();
            }
        });
        mCancelSyncPopUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        mSyncPopupText.setText(R.string.sync_call_history_text);
        mSyncPopupTitle.setText(R.string.sync_call_history_title);


        alertDialog = dialogBuilder.create();
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        alertDialog.show();
    }

    /**
     * Click listener for Delete feature
     */
    private void setClickListenerForDeleteCallLog() {
        mExitDeleteDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isLandScape()) {
                    mDeleteCallHistory.collapse(mDeleteDialog,true);
                } else {
                    mDeleteCallHistory.collapse(mDeleteDialog,false);
                }

            }
        });
        mConfirmDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecentCallsAdapter != null) {
                    mRecentCallsAdapter.deleteItem(mPositionToBeDeleted);
                }
                mUIHistoryContactsAdaptor.deleteCallLog(itemToDelete);
                if (Utils.isLandScape()) {
                    mDeleteCallHistory.collapse(mDeleteDialog,true);
                }else{
                    mDeleteCallHistory.collapse(mDeleteDialog,false);
                }

                checkListCount();
            }
        });
        mCancelDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isLandScape()) {
                    mDeleteCallHistory.collapse(mDeleteDialog,true);
                } else {
                    mDeleteCallHistory.collapse(mDeleteDialog,false);
                }

            }
        });
    }

    private boolean isFilteringPaired() {
        return !blueHelper.isBluetoothLinkEstablished() || !blueHelper.isCallHistorySharingEnabled();
    }

    @Override
    public void refreshBluetoothData() {
        prepareSyncIcon();
        checkListCount();
        restoreListPosition(position);
    }

    /**
     * Refreshes Call list view and the sync icon.
     */
    public void callTableUpdated() {
        final boolean isFilteringPaired = updateSyncIconState();
        if(mUIHistoryContactsAdaptor != null && mRecentCallsAdapter != null && getActivity()!=null) {
            mUIHistoryContactsAdaptor.refresh();
            mRecentCallsAdapter.setCallHistoryList(mUIHistoryContactsAdaptor.getContacts(getActivity().getApplicationContext()));
            mRecentCallsAdapter.setmFilteringPaired(isFilteringPaired);
            mRecentCallsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(int position, CallData item) {
        itemToDelete = item;
        mPositionToBeDeleted = position;
        if (Utils.isLandScape()) {
            mDeleteCallHistory.expand(mDeleteDialog,true);
        } else {
            mDeleteCallHistory.expand(mDeleteDialog,false);
        }

    }

    /**
     * {@link OnFilterCallsInteractionListener} is interface responsible for communication of {@link RecentCallsFragment}
     * and {@link com.avaya.android.vantage.basic.activities.MainActivity}.
     *
     */
    public interface OnFilterCallsInteractionListener {
        void onSaveSelectedCategoryRecentFragment(CallData.CallCategory callCategory);
        void refreshHistoryIcon();
    }
}
