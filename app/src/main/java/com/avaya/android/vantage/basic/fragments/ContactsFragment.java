package com.avaya.android.vantage.basic.fragments;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.adaptors.RemoveSearchResultsContactsFragmentInterface;
import com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor;
import com.avaya.android.vantage.basic.csdk.ConfigParametersNames;
import com.avaya.android.vantage.basic.csdk.ContactsLoader;
import com.avaya.android.vantage.basic.csdk.EnterpriseContactManager;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.settings.BlueHelper;
import com.avaya.android.vantage.basic.views.FastScrollRecyclerView;
import com.avaya.android.vantage.basic.views.SlideAnimation;
import com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler;
import com.avaya.android.vantage.basic.views.adapters.MyContactsRecyclerViewAdapter;
import com.avaya.clientservices.contact.EditableContact;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.avaya.android.vantage.basic.Constants.ALL_CONTACTS;
import static com.avaya.android.vantage.basic.Constants.ENTERPRISE_ONLY;
import static com.avaya.android.vantage.basic.Constants.IPO_ONLY;
import static com.avaya.android.vantage.basic.Constants.LOCAL_ONLY;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_IPOFFICE;
import static com.avaya.android.vantage.basic.fragments.settings.BlueHelper.CONTACT_SYNC_DURATION;
import static com.avaya.android.vantage.basic.fragments.settings.BlueHelper.ContactSync.ContactSharingOff;
import static com.avaya.android.vantage.basic.fragments.settings.BlueHelper.ContactSync.ContactSharingOn;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnContactInteractionListener}
 * interface.
 */
public class ContactsFragment extends android.support.v4.app.Fragment implements View.OnClickListener, EnterpriseContactManager.EnterpriseContactListener,RemoveSearchResultsContactsFragmentInterface {

    public static final int ALL = 1;
    public static final int ENTERPRISE = 2;
    public static final int LOCAL = 3;
    public static final int IPO = 4;
    public static boolean isToShowSearch;

    // TODO: Customize parameters
    private int mColumnCount = 1;
    private static final String TAG = "ContactsFragment";
    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final String ADD_PARTICIPANT = "addParticipant";
    private static final String SYNC_CONTACTS = Constants.SYNC_CONTACTS;

    private static final int FIRST_NAME_FIRST = Constants.FIRST_NAME_FIRST;
    private static final int LAST_NAME_FIRST = Constants.LAST_NAME_FIRST;
    private static final int SEARCH_DELAY = 200;


    private String ADMIN_NAME_DISPLAY_ORDER = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_DISPLAY_ORDER);
    private String ADMIN_NAME_SORT_ORDER = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_SORT_ORDER);

    private String enableContactEdit = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.ENABLE_MODIFY_CONTACTS);

    private OnContactInteractionListener mListener;
    private MyContactsRecyclerViewAdapter mContactsRecyclerViewAdapter;
    public UIContactsViewAdaptor mUIContactsAdaptor;

    private LinearLayout mFilterLayout;
    private TextView mFilterView;
    private ImageView mSelectedAllImage;
    private LinearLayout mSelectedAllLinear;
    private ImageView mSelectedEnterpriseImage;
    private LinearLayout mSelectedEnterpriseLinear;
    private LinearLayout mContactsBluetoothSyncLinear;
    private ImageView mSelectedLocalImage;
    private LinearLayout mSelectedLocalLinear;
    private FrameLayout frameContactsAll;
    public ImageView mAdd;
    private ImageView back;
    private FastScrollRecyclerView mRecycleView;
    private SwipeRefreshLayout mSwipeRefresh;
    private TextView mSelectedAllText, mSelectedEnterpriseText, mSelectedLocalText;
    private LinearLayout mSyncDialog;
    private TextView mOKSyncPopUp;
    private TextView mSyncPopupText;
    private TextView mSyncPopupTitle;
    private TextView mCancelSyncPopUp;
    private ContactsLoader mContactsLoader;
    private boolean addParticipant;
    private int mNameDisplayType;
    private int mNameSortType = Constants.LAST_NAME_FIRST;
    private int mEnterpriseTextColor;

    private SlideAnimation mSlideAnimation;
    private SlideAnimation mSyncUpSlider;
    private SharedPreferences mUserPreference;
    private SharedPreferences mConnectionPref;
    public LinearLayout seacrhLayout;
    public SearchView mSearchView;
    private TextView mEmptyView;

    public int filterSelection;
    public int prevFilterSelection = 0;

    private BlueHelper blueHelper;


    private Handler mHandler = new Handler();
    private Runnable mLayoutCloseRunnable = new Runnable() {
        @Override
        public void run() {
            hideMenus();
        }
    };
    private Runnable mSearchRunnable = new Runnable() {
        @Override
        public void run() {
            if (mContactsRecyclerViewAdapter != null) {
                mContactsRecyclerViewAdapter.searchFilter(sSearchQuery, sSearchQuery.matches("[0-9]+"));
            }
        }
    };

    /* Field below is made static to facilitate least-code-changes search term preservation.
     * One of the issues that needs to be addressed separately is extraordinary frequent
     * fragment instance creation, which requires task in itself.
     */
    private static String sSearchQuery = "";
    private boolean mIpoEnabled;

    private ContactViewAdaptorInterface mAdapterInstance;
    private ImageView mSyncContacts;
    private ImageView contactsBluetoothSyncIcon;

    public AlertDialog alertDialog;

    private Snackbar mSnackBar;

    public boolean mUserVisibleHint = false;

    private View.OnClickListener mCancelSnackbarAction = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mSnackBar.dismiss();
            if (blueHelper != null) {
                final boolean isSharingEnabled = blueHelper.isContactSharingEnabled();
                blueHelper.setContactSharing(isSharingEnabled ? ContactSharingOff : ContactSharingOn);
            }
        }
    };

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static ContactsFragment newInstance(int columnCount, boolean addParticipant, boolean showSearch) {
        ContactsFragment fragment = new ContactsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putBoolean(ADD_PARTICIPANT, addParticipant);
        fragment.setArguments(args);
        isToShowSearch = showSearch;
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ContactsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_contacts_list, container, false);
        mSwipeRefresh = (SwipeRefreshLayout) root.findViewById(R.id.swipe_refresh);
        mRecycleView = (FastScrollRecyclerView) root.findViewById(R.id.list);
        mRecycleView.setmSwipeRefreshLayout(mSwipeRefresh);
        mEmptyView = (TextView) root.findViewById(R.id.empty_contacts);
        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            mRecycleView.setIndexBarTextColor("#FFFFFF");
            mRecycleView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
            mRecycleView.setIndexbarWidth(30);
        }else{
            mRecycleView.setIndexBarTextColor("#304050");
            mRecycleView.setIndexbarWidth(40);
        }
        mRecycleView.setIndexBarColor("#FFFFFF");


        mRecycleView.setAlphaBarEnabled(true);// use this to show alpha bar if needed
        mUserPreference = getActivity().getSharedPreferences(Constants.USER_PREFERENCE, MODE_PRIVATE);
        mConnectionPref = getActivity().getSharedPreferences(Constants.CONNECTION_PREFS, MODE_PRIVATE);
        mIpoEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ENABLE_IPOFFICE);
        initSnackBar();
        initRecyclerView();
        initSearch(root);
        initSwipeToRefresh();
        initFilter(root);
        setClickListenersForSyncDialog();

        return root;
    }

    private void initSnackBar() {
        mSnackBar = Snackbar.make(getActivity().findViewById(android.R.id.content),
                R.string.waitBluetoothContactSharing, Snackbar.LENGTH_INDEFINITE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mContactsRecyclerViewAdapter != null){
            mContactsRecyclerViewAdapter.disableBlockClick();
        }
        boolean settingsChanged = mUserPreference.getBoolean(Constants.REFRESH_CONTACTS, false);
        // making sure we call methods only if there is a change in the settings
        if (settingsChanged) {
            SharedPreferences.Editor editor = mUserPreference.edit();
            editor.putBoolean(Constants.REFRESH_CONTACTS, false);
            editor.apply();
            nameSortSet();
        }
        nameDisplaySet();
        if(TextUtils.isEmpty(sSearchQuery)){
            performSelection();
        } else {
            reQueryIfInSearch();
        }

        mSelectedEnterpriseLinear.setEnabled(true);
        mSelectedEnterpriseText.setEnabled(true);
        mEnterpriseTextColor = getActivity().getColor(R.color.primary);
        mSelectedEnterpriseText.setTextColor(mEnterpriseTextColor);

        prepareSyncIcon();

    }

    @Override
    public void onDestroyView() {
        clearSearch();
        super.onDestroyView();

        if(alertDialog!=null)
            alertDialog.dismiss();
    }

    @Override
    public void onPause() {
        clearUiCallbacks();
        super.onPause();

        if(alertDialog!=null)
            alertDialog.dismiss();
    }

    private void clearSearch() {
        sSearchQuery = "";
        if (mSearchView != null && mFilterView != null) {
            mSearchView.setQuery(sSearchQuery, false);
            mSearchView.clearFocus();
        }
    }

    /**
     * Initialize filter and logic for it
     * Also including logic for selection
     *
     * @param root {@link View}
     */
    private void initFilter(final View root) {
        mFilterLayout = (LinearLayout) root.findViewById(R.id.select_contacts_filter);
        mFilterView = (TextView) root.findViewById(R.id.filter);
        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            mFilterView.setVisibility(View.GONE);
        }
        mSelectedAllImage = (ImageView) root.findViewById(R.id.contacts_all_icon);
        mSelectedAllLinear = (LinearLayout) root.findViewById(R.id.contacts_all_linear);
        mSelectedEnterpriseImage = (ImageView) root.findViewById(R.id.contacts_enterprise_icon);
        mSelectedEnterpriseLinear = (LinearLayout) root.findViewById(R.id.contacts_enterprise_linear);

        if (getResources().getBoolean(R.bool.is_landscape) == true)
            mContactsBluetoothSyncLinear = (LinearLayout) root.findViewById(R.id.contacts_bluetooth_sync_linear);

        mSelectedLocalImage = (ImageView) root.findViewById(R.id.contacts_local_icon);
        mSelectedLocalLinear = (LinearLayout) root.findViewById(R.id.contacts_local_linear);
        frameContactsAll = (FrameLayout) root.findViewById(R.id.frameContactsAll);
        mAdd = (ImageView) root.findViewById(R.id.add);

        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            back = (ImageView) root.findViewById(R.id.back);
        }
        mSelectedAllText = (TextView) root.findViewById(R.id.contacts_all);
        mSelectedEnterpriseText = (TextView) root.findViewById(R.id.contacts_enterprise);
        mSelectedLocalText = (TextView) root.findViewById(R.id.contacts_local);
        mSyncContacts = (ImageView) root.findViewById(R.id.sync_contacts);
        if(isAdded()&&getResources().getBoolean(R.bool.is_landscape) == true){
            mSyncContacts.setVisibility(View.GONE);
        }else{
            mSyncContacts.setVisibility(View.VISIBLE);
        }
        mSyncDialog = (LinearLayout) root.findViewById(R.id.sync_pop_up);
        mSyncDialog.setVisibility(View.GONE);
        mCancelSyncPopUp = (TextView) mSyncDialog.findViewById(R.id.sync_dialog_cancel);
        mSyncPopupText = (TextView) mSyncDialog.findViewById(R.id.sync_dialog_description);
        mSyncPopupTitle = (TextView) mSyncDialog.findViewById(R.id.sync_dialog_title);
        mOKSyncPopUp = (TextView) mSyncDialog.findViewById(R.id.sync_dialog_ok);

        contactsBluetoothSyncIcon = (ImageView) root.findViewById(R.id.contacts_bluetooth_sync_icon);

        // setting up slide animations
        mSlideAnimation = new SlideAnimation();
        mSyncUpSlider = new SlideAnimation();
        mSlideAnimation.reDrawListener(mFilterLayout);
        mSyncUpSlider.reDrawListener(mSyncDialog);

        //Add click listeners to proper views
        mFilterView.setOnClickListener(this);
        mSelectedAllLinear.setOnClickListener(this);
        mSelectedEnterpriseLinear.setOnClickListener(this);

        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            mContactsBluetoothSyncLinear.setOnClickListener(this);
            back.setOnClickListener(this);
        }

        mSelectedLocalLinear.setOnClickListener(this);
        frameContactsAll.setOnClickListener(this);
        mAdd.setOnClickListener(this);

        if (enableContactEdit != null && enableContactEdit.equals("0") || getResources().getBoolean(R.bool.is_landscape) == true) {
            mAdd.setVisibility(View.INVISIBLE);
        } else {
            mAdd.setVisibility(View.VISIBLE);
        }

        if (mIpoEnabled){
            mSelectedEnterpriseText.setText(getText(R.string.personal_directory_contacts));
        } else {
            mSelectedEnterpriseText.setText(getText(R.string.enterprise_contacts));
        }

    }

    /**
     * Hiding contacts filter in case there are no contacts
     */
    public void checkFilterVisibility() {
        if (mFilterView != null
                && mUIContactsAdaptor.getAllContacts().isEmpty()
                && mUIContactsAdaptor.getLocalContacts().isEmpty()
                && mUIContactsAdaptor.getEnterpriseContacts().isEmpty()) {
            if(isAdded()&&getResources().getBoolean(R.bool.is_landscape) == true){
                mFilterView.setVisibility(View.GONE);
            }else{
                mFilterView.setVisibility(View.INVISIBLE);
            }

        } else {
            if (mFilterView != null && sSearchQuery.isEmpty()){
                if(isAdded()&&getResources().getBoolean(R.bool.is_landscape) == true){
                    mFilterView.setVisibility(View.GONE);
                }else {
                    mFilterView.setVisibility(View.VISIBLE);
                }

            }
        }
    }

    /**
     * Performing search
     *
     * @param root {@link View}
     */
    private void initSearch(View root) {
        seacrhLayout = (LinearLayout) root.findViewById(R.id.seacrh_layout);
        if(isAdded()&&getResources().getBoolean(R.bool.is_landscape) == true){
            if(isToShowSearch)
                seacrhLayout.setVisibility(View.VISIBLE);
            else
                seacrhLayout.setVisibility(View.GONE);
        }else{
            seacrhLayout.setVisibility(View.VISIBLE);
        }

        mSearchView = (SearchView) root.findViewById(R.id.search);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) mSearchView.getContext().getSystemService(Context.SEARCH_SERVICE);
        // Assumes current activity is the searchable activity
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        // Do not iconify the widget; expand it by default
        mSearchView.setIconifiedByDefault(false);
        EditText searchEditText = (EditText) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);

        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            searchEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            searchEditText.setPrivateImeOptions("nm");
        }

        searchEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    mListener.onStartSearching(getLocationOnScreen());
                }
            }
        });
        searchEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onStartSearching(getLocationOnScreen());
            }
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdd.setVisibility(View.GONE);
                mFilterView.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    if(getResources().getBoolean(R.bool.is_landscape) == true) {
                        mFilterView.setVisibility(View.GONE);
                    }else {
                        mFilterView.setVisibility(View.VISIBLE);
                    }
                    performSelection();
                    if (enableContactEdit != null && enableContactEdit.equals("0") || getResources().getBoolean(R.bool.is_landscape) == true) {
                        mAdd.setVisibility(View.INVISIBLE);
                    } else {
                        mAdd.setVisibility(View.VISIBLE);
                    }
                } else {
                    mAdd.setVisibility(View.GONE);
                    mFilterView.setVisibility(View.GONE);
                }
                if (newText != null) {
                    // do a search only if we have 200ms delay between keys entered. This is used to prevent doing a search in case user is still typing
                    search(newText);
                }
                return false;
            }
        });
        reQueryIfInSearch();
    }

    /**
     * Setup swipe to refresh layout.
     */
    private void initSwipeToRefresh() {

        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mUIContactsAdaptor != null && mSearchView != null && sSearchQuery.isEmpty()) {
                    performSelection();
                    if (mSearchView.getQuery().toString().equalsIgnoreCase("")) {
                        mUIContactsAdaptor.refresh(mSwipeRefresh);
                        if (mContactsLoader != null) {
                            getActivity().getLoaderManager().restartLoader(Constants.LOCAL_ADDRESS_LOADER, null, mContactsLoader);
                        }
                        mSearchView.clearFocus();
                        //refresh list of all contacts if query is not set.
                    } else {
                        mSwipeRefresh.setRefreshing(false);
                        //if query exist do not refresh
                    }
                } else {
                    mSwipeRefresh.setRefreshing(false);
                }
            }
        });
    }

    /**
     * Setup list.
     */
    private void initRecyclerView() {
        // Set the adapter
        if (mColumnCount <= 1) {
            mRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            mRecycleView.setLayoutManager(new GridLayoutManager(getContext(), mColumnCount));
        }

        mRecycleView.setIndexBarTransparentValue((float) 0.2);
        if (mUIContactsAdaptor == null && mAdapterInstance != null) {
            mUIContactsAdaptor = mAdapterInstance.getContactViewAdapter();
        }
        if (mUIContactsAdaptor != null) {
            mContactsRecyclerViewAdapter = new MyContactsRecyclerViewAdapter(
                    mUIContactsAdaptor.getAllContacts(),
                    mUIContactsAdaptor.getLocalContacts(),
                    mUIContactsAdaptor.getEnterpriseContacts(),
                    mUIContactsAdaptor.getIpoContacts(),
                    mListener, getActivity(),
                    mEmptyView, !showPaired(),getContext(),this);
            mContactsRecyclerViewAdapter.setAddParticipant(addParticipant);
            nameDisplaySet();
            mUIContactsAdaptor.setContactsViewInterface(mContactsRecyclerViewAdapter);
            mRecycleView.setAdapter(mContactsRecyclerViewAdapter);
            mRecycleView.setHasFixedSize(true);
        }
    }

    /**
     * Updates Sync Contacts icon and Sync contact state based on the
     * current BT connection status and Connection shared preferences.
     */
    private void prepareSyncIcon() {Log.e("TestTest","prepareSyncIcon");
        if (blueHelper == null) return;
        boolean isFilteringPaired;
        if (blueHelper.isBluetoothLinkEstablished()) {
            if (blueHelper.isContactSharingEnabled()) { // show blue  icon
                setSyncContactImage(R.drawable.ic_sync_paired_on);
                isFilteringPaired = false;
            } else { // show grey icon
                setSyncContactImage(R.drawable.ic_sync_paired_off_grey);
                isFilteringPaired = true;
            }
        } else { // show dashed grey icon
            setSyncContactImage(R.drawable.ic_sync_unpaired_grey);
            isFilteringPaired = true;
        }
        if (mContactsRecyclerViewAdapter != null) {
            mContactsRecyclerViewAdapter.setmFilteringPaired(isFilteringPaired);
            performSelection();
        }
    }

    private void setSyncContactImage(@DrawableRes int syncIconRes) {
        if(mSyncContacts != null) {
            if(getResources().getBoolean(R.bool.is_landscape) == true){
                contactsBluetoothSyncIcon.setImageResource(syncIconRes);
            } else {
                mSyncContacts.setImageResource(syncIconRes);
            }
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
        if (context instanceof ContactViewAdaptorInterface) {
            mAdapterInstance = (ContactViewAdaptorInterface) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnContactsFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mAdapterInstance = null;
    }

    /**
     * Perform search based on provided query
     *
     * @param query String based on which we perform search
     */
    public void search(String query) {
        sSearchQuery = query;
        if (mHandler != null) {
            mHandler.removeCallbacks(mSearchRunnable);
            mHandler.postDelayed(mSearchRunnable, SEARCH_DELAY);
        }
    }

    /**
     * Sets a search string of the {@link SearchView} which will then <br>
     * in OnQueryTextListener trigger actual async search.
     * This method is meant to be called as a response to Intent.ACTION_SEARCH<br>
     * parameter should be set to true in order to trigger<br>
     * {@link android.support.v7.widget.SearchView.OnQueryTextListener#onQueryTextChange(String)}
     *
     * @param query String set to in {@link SearchView} to search for in Contacts
     */
    public void setQuery(String query) {
        if (mSearchView != null) {Log.e("TestTest","setQuery");
            mSearchView.requestFocus();
            mSearchView.setQuery(query, false);
        }
    }

    /**
     * Called to re-trigger search<br>
     * If sSearchQuery has text {@link #search(String)} is called.
     */
    public void reQueryIfInSearch() {
        if (!TextUtils.isEmpty(sSearchQuery)) {
            search(sSearchQuery);
        }
    }

    /**
     * Setting up {@link UIContactsViewAdaptor}
     *
     * @param UIContactsAdaptor {@link UIContactsViewAdaptor}
     */
    public void setUIContactsAdaptor(UIContactsViewAdaptor UIContactsAdaptor) {
        this.mUIContactsAdaptor = UIContactsAdaptor;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.filter:
                if (mFilterLayout.getVisibility() == View.VISIBLE) {
                    if(getResources().getBoolean(R.bool.is_landscape) == true){
                        mSlideAnimation.collapse(mFilterLayout,true);
                    }else{
                        mSlideAnimation.collapse(mFilterLayout,false);
                    }

                    mHandler.removeCallbacks(mLayoutCloseRunnable);
                } else {
                    if(getResources().getBoolean(R.bool.is_landscape) == true) {
                        mSlideAnimation.expand(mFilterLayout,true);
                    }else{
                        mSlideAnimation.expand(mFilterLayout,false);
                    }

                    frameContactsAll.setVisibility(View.VISIBLE);
                    mHandler.postDelayed(mLayoutCloseRunnable, Constants.LAYOUT_DISAPPEAR_TIME);
                }

                if(getResources().getBoolean(R.bool.is_landscape) == true){
                    mSyncUpSlider.collapse(mSyncDialog,false);
                }else{
                    mSyncUpSlider.collapse(mSyncDialog,false);
                }

                return;
            case R.id.contacts_all_linear:
                mUIContactsAdaptor.setContactDisplaySelection(ALL);
                performSelection();

                if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
                    ((MainActivity) getActivity()).tabSelector.setImageResource(R.drawable.triangle_copy);
                    ((MainActivity) getActivity()).showingFirst = false;
                }
                break;
            case R.id.contacts_enterprise_linear:
                mUIContactsAdaptor.setContactDisplaySelection(ENTERPRISE);
                performSelection();

                if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
                    ((MainActivity) getActivity()).tabSelector.setImageResource(R.drawable.triangle_copy);
                    ((MainActivity) getActivity()).showingFirst = false;
                }


                break;

            case R.id.contacts_bluetooth_sync_linear:
                syncContactsClick();

                if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
                    ((MainActivity) getActivity()).tabSelector.setImageResource(R.drawable.triangle_copy);
                    ((MainActivity) getActivity()).showingFirst = false;
                }

                break;

            case R.id.contacts_local_linear:
                mUIContactsAdaptor.setContactDisplaySelection(LOCAL);
                performSelection();

                if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
                    ((MainActivity) getActivity()).tabSelector.setImageResource(R.drawable.triangle_copy);
                    ((MainActivity) getActivity()).showingFirst = false;
                }

                break;
            case R.id.add:
                mListener.onCreateNewContact();
                break;

            case R.id.back:
                removeSearchResults();

                CallStatusFragment callStatusFragment = (CallStatusFragment) ((MainActivity) getActivity()).getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);

                if((callStatusFragment.getView().getVisibility()==View.INVISIBLE))
                    callStatusFragment.showCallStatus();


                int isActiveCall = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
                int heldCallId = SDKManager.getInstance().getCallAdaptor().getHeldCallId();

                if(isActiveCall!=0 || heldCallId != -1)
                    callStatusFragment.showCallStatus();

                ((MainActivity) getActivity()).mViewPager.setEnabledSwipe(true);
                break;
            default:
                hideMenus();
                break;
        }
        hideMenus();
    }

    @Override
    public void removeSearchResults(){
        if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
            //((MainActivity) getActivity()).searchAddFilterIconViewController();
            ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(false);
            seacrhLayout.setVisibility(View.GONE);
            search("");
            mUIContactsAdaptor.setContactDisplaySelection(prevFilterSelection);
            performSelection();

        }
    }

    /**
     * Get on screen location of {@link Rect}
     *
     * @return {@link Rect}
     */
    private Rect getLocationOnScreen() {
        Rect mRect = new Rect();
        int[] location = new int[2];

        mSearchView.getLocationOnScreen(location);

        mRect.left = location[0];
        mRect.top = location[1];
        mRect.right = location[0] + mSearchView.getWidth();
        mRect.bottom = location[1] + mSearchView.getHeight();

        return mRect;
    }

    /**
     * Return list position as {@link Parcelable}
     *
     * @return {@link Parcelable}
     */
    public Parcelable getListPosition() {
        if (mRecycleView != null) {
            return mRecycleView.getLayoutManager().onSaveInstanceState();
        } else {
            return null;
        }
    }

    /**
     * Once we created new contact, we need to make sure to refresh contact fragment and display
     * new content
     */
    public void contactCreated(){
        boolean settingsChanged = mUserPreference.getBoolean(Constants.REFRESH_CONTACTS, false);
        prepareSyncIcon();
        // making sure we call methods only if there is a change in the settings
        if (settingsChanged) {
            SharedPreferences.Editor editor = mUserPreference.edit();
            editor.putBoolean(Constants.REFRESH_CONTACTS, false);
            editor.apply();
            nameSortSet();
        }
        nameDisplaySet();
        performSelection();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {

            SharedPreferences sharedPref = getActivity().getPreferences(MODE_PRIVATE);
            boolean value = sharedPref.getBoolean("is_return_ro_search_land", false);
            if(value){
                mAdd.setVisibility(View.INVISIBLE);
            }

            if(mUserVisibleHint) {
                if(getResources().getBoolean(R.bool.is_landscape) == true && isAdded() && (MainActivity)getActivity()!=null) {
                    ((MainActivity) getActivity()).addcontactButton.setVisibility(View.VISIBLE);
                    ((MainActivity) getActivity()).searchButton.setVisibility(View.VISIBLE);
                }
            }else {
                if(getResources().getBoolean(R.bool.is_landscape) == true && isAdded() && (MainActivity)getActivity()!=null) {
                    ((MainActivity) getActivity()).addcontactButton.setVisibility(View.INVISIBLE);
                    ((MainActivity) getActivity()).searchButton.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    /**
     * Restoring list position from {@link Parcelable}
     *
     * @param position {@link Parcelable}
     */
    public void restoreListPosition(Parcelable position) {
        prepareSyncIcon();
        if (position != null) {
            mRecycleView.getLayoutManager().onRestoreInstanceState(position);
        }
    }


    /**
     * Update selection in list and update text for filter
     *
     */
    private void performSelection() {
        // prevent crash in case fragment is not yet attached to activity
        if (getActivity() == null){
            return;
        }
        filterSelection = mUIContactsAdaptor.getContactDisplaySelection();

        if(filterSelection==0){
            SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences("prevFilterSelection", MODE_PRIVATE);
            int prevSelection = prefs.getInt("prevFilterSelection", 0);
            if (prevSelection != 0) {
                filterSelection = prevSelection;
            }
        }


        if (filterSelection == ALL) {
            mFilterView.setText(getText(R.string.all_contacts));
            mContactsRecyclerViewAdapter.loadContactData(ALL_CONTACTS);
            mContactsRecyclerViewAdapter.setContactDisplaySelection(ALL);
            mSelectedAllImage.setVisibility(View.VISIBLE);
            mSelectedAllText.setTextColor(getActivity().getColor(R.color.midBlue));
            mSelectedEnterpriseImage.setVisibility(View.INVISIBLE);
            mSelectedEnterpriseText.setTextColor(mEnterpriseTextColor);
            mSelectedLocalImage.setVisibility(View.INVISIBLE);
            mSelectedLocalText.setTextColor(getActivity().getColor(R.color.primary));
            prevFilterSelection = ALL;

            SharedPreferences.Editor editor = getActivity().getApplicationContext().getSharedPreferences("prevFilterSelection", MODE_PRIVATE).edit();
            editor.putInt("prevFilterSelection", ALL);
            editor.apply();

        } else if (filterSelection == ENTERPRISE) {
            if (mIpoEnabled){
                mFilterView.setText(getText(R.string.personal_directory_contacts));
            } else {
                mFilterView.setText(getText(R.string.enterprise_contacts));
            }
            mSelectedAllImage.setVisibility(View.INVISIBLE);
            mSelectedAllText.setTextColor(getActivity().getColor(R.color.primary));
            mSelectedEnterpriseImage.setVisibility(View.VISIBLE);
            mSelectedEnterpriseText.setTextColor(getActivity().getColor(R.color.midBlue));
            mSelectedLocalImage.setVisibility(View.INVISIBLE);
            mSelectedLocalText.setTextColor(getActivity().getColor(R.color.primary));
            if (mIpoEnabled){
                mContactsRecyclerViewAdapter.loadContactData(IPO_ONLY);
                mContactsRecyclerViewAdapter.setContactDisplaySelection(IPO);
            } else {
                mContactsRecyclerViewAdapter.loadContactData(ENTERPRISE_ONLY);
                mContactsRecyclerViewAdapter.setContactDisplaySelection(ENTERPRISE);
            }
            prevFilterSelection = ENTERPRISE;
            mUIContactsAdaptor.setContactDisplaySelection(prevFilterSelection);

            SharedPreferences.Editor editor = getActivity().getApplicationContext().getSharedPreferences("prevFilterSelection", MODE_PRIVATE).edit();
            editor.putInt("prevFilterSelection", ENTERPRISE);
            editor.apply();

        } else if (filterSelection == LOCAL) {
            mFilterView.setText(getText(R.string.local_contacts));
            mSelectedAllImage.setVisibility(View.INVISIBLE);
            mSelectedAllText.setTextColor(getActivity().getColor(R.color.primary));
            mSelectedEnterpriseImage.setVisibility(View.INVISIBLE);
            mSelectedEnterpriseText.setTextColor(mEnterpriseTextColor);
            mSelectedLocalImage.setVisibility(View.VISIBLE);
            mSelectedLocalText.setTextColor(getActivity().getColor(R.color.midBlue));
            mContactsRecyclerViewAdapter.loadContactData(LOCAL_ONLY);
            mContactsRecyclerViewAdapter.setContactDisplaySelection(LOCAL);
            prevFilterSelection = LOCAL;
            mUIContactsAdaptor.setContactDisplaySelection(prevFilterSelection);

            SharedPreferences.Editor editor = getActivity().getApplicationContext().getSharedPreferences("prevFilterSelection", MODE_PRIVATE).edit();
            editor.putInt("prevFilterSelection", LOCAL);
            editor.apply();
        }
    }

    /**
     * Managing name display method (first name first, last name first)
     */
    private void nameDisplaySet() {
        int defaultValue = Constants.FIRST_NAME_FIRST;
        if (ADMIN_NAME_DISPLAY_ORDER != null) {
            // doing this to prevent bug in case someone entered a value that is different from 1 or 0.
            if (ADMIN_NAME_DISPLAY_ORDER.equals("last,first")) {
                defaultValue = LAST_NAME_FIRST;
            } else {
                defaultValue = FIRST_NAME_FIRST;
            }
        }

        int nameDisplay = mUserPreference.getInt(Constants.NAME_DISPLAY_PREFERENCE, defaultValue);

        if (mNameDisplayType != nameDisplay && mContactsRecyclerViewAdapter != null) {
            mContactsRecyclerViewAdapter.firstNameFirst(nameDisplay == Constants.FIRST_NAME_FIRST);
            mContactsRecyclerViewAdapter.notifyDataSetChanged();
            mNameDisplayType = nameDisplay;
        }
    }

    /**
     * Managing name sort method (first name first, last name first)
     */
    private void nameSortSet() {
        int defaultValue = Constants.LAST_NAME_FIRST;
        if (mUIContactsAdaptor == null) {
            return;
        }

        performSelection();
        if (ADMIN_NAME_SORT_ORDER != null) {
            // doing this to prevent bug in case someone entered a value that is different from 1 or 0.
            if (ADMIN_NAME_SORT_ORDER.equals("first,last")) {
                defaultValue = FIRST_NAME_FIRST;
            } else {
                defaultValue = LAST_NAME_FIRST;
            }
        }

        int nameSort = mUserPreference.getInt(Constants.NAME_SORT_PREFERENCE, defaultValue);

        if (mNameSortType != nameSort) {
            mUIContactsAdaptor.removeContacts();
            SDKManager.getInstance().displayFirstNameFirst(nameSort != Constants.LAST_NAME_FIRST);
            mUIContactsAdaptor.refresh();
            mNameSortType = nameSort;
            if (getActivity() != null){
                getActivity().getLoaderManager().restartLoader(Constants.LOCAL_ADDRESS_LOADER, null, mContactsLoader);
            } else {
                Log.e(TAG, "Activity is null");
            }
        }
    }

    /**
     * Setting up mContactsLoader for this fragment to be able to refresh Loader
     *
     * @param mContactsLoader getting this info from Main Activity
     */
    public void setmContactsLoader(ContactsLoader mContactsLoader) {
        this.mContactsLoader = mContactsLoader;
    }

//    /**
//     * This method will enable video
//     */
//    public void enableVideo() {
//        if (mContactsRecyclerViewAdapter != null)
//            mContactsRecyclerViewAdapter.notifyDataSetChanged();
//    }

    /**
     * This method will be called every time Contacts fragment is active
     */
    public void fragmentSelected() {
        /**
         * When activity is null, there is no sense in trying to execute other UI operations
         * as they are most, if not all, dependent on the Activity context.
         * Explanation:
         * Root cause is fragments creation and their methods called (e.g.: fragmentSelected())
         * from the ViewPager, before they are attached to the parent activity.
         * That should be a separate refactoring task.
         */
        if (getActivity() == null) return;

        Log.e(TAG, "fragmentSelected: Contacts");
        unblockListClick();
        prepareSyncIcon();
        if (mContactsRecyclerViewAdapter != null){
            mContactsRecyclerViewAdapter.reloadDirectories();
        }
        reQueryIfInSearch();
    }

    /**
     * Enables click on contact list
     */
    public void unblockListClick(){Log.e("TestTest","unblockListClick");
        if (mContactsRecyclerViewAdapter != null){
            mContactsRecyclerViewAdapter.disableBlockClick();
        }
    }
    /**
     * Prepare {@link ContactsFragment} for adding to existing call
     *
     * @param add boolean which mark if we are preparing for adding participants in current call
     */
    public void setAddParticipantData(boolean add) {
        addParticipant = add;
        if (mContactsRecyclerViewAdapter != null && mRecycleView != null) {
            mContactsRecyclerViewAdapter.setAddParticipant(addParticipant);
            mContactsRecyclerViewAdapter.notifyDataSetChanged();
            if (mRecycleView.getAdapter() == null) {
                mRecycleView.setAdapter(mContactsRecyclerViewAdapter);
            }
        }
    }

    public void clearUiCallbacks() {
        Utils.hideKeyboard(getActivity());
        if (mHandler != null) {
            mHandler.removeCallbacks(mSearchRunnable);
            mHandler.removeCallbacks(mLayoutCloseRunnable);
        }
    }

    /**
     * Collapse contact filter menu
     */
    public void hideMenus() {
        mHandler.removeCallbacks(mLayoutCloseRunnable);
        if (mFilterLayout.getVisibility() == View.VISIBLE) {
            if(getResources().getBoolean(R.bool.is_landscape) == true){
                mSlideAnimation.collapse(mFilterLayout,true);
            }else{
                mSlideAnimation.collapse(mFilterLayout,false);
            }

            frameContactsAll.setVisibility(View.GONE);

            if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
                ((MainActivity) getActivity()).tabSelector.setImageResource(R.drawable.triangle_copy);
                ((MainActivity) getActivity()).showingFirst = false;
            }
        } if (mSyncDialog.getVisibility() == View.VISIBLE) {
            if(getResources().getBoolean(R.bool.is_landscape) == true){
                mSyncUpSlider.collapse(mSyncDialog, false);
            }else {
                mSyncUpSlider.collapse(mSyncDialog, false);
            }
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
        mOKSyncPopUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getResources().getBoolean(R.bool.is_landscape) == true){
                    mSyncUpSlider.collapse(mSyncDialog,false);
                }else{
                    mSyncUpSlider.collapse(mSyncDialog,false);
                }

                Intent openBluetoothIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(openBluetoothIntent);
            }
        });
        mCancelSyncPopUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getResources().getBoolean(R.bool.is_landscape) == true){
                    mSyncUpSlider.collapse(mSyncDialog,false);
                }else{
                    mSyncUpSlider.collapse(mSyncDialog,false);
                }

            }
        });
    }


    /**
     * This is triggered when sync bluetooth icon is clicked.<br>
     * Checks the state of the bluetooth link and turns on/off bluetooth Contact Sharing.<br>
     * If Bluetooth is off or disconnected a dialog leading to device Bluetooth Settings is shown.
     */
    public void syncContactsClick() {
        if (mSnackBar.isShown()) return;

        if (mSyncDialog.getVisibility() == View.GONE) {
            if (blueHelper.isBluetoothLinkEstablished()) {
                boolean isFilteringPaired;
                if (blueHelper.isContactSharingEnabled()) {
                    // turn Off Contact Sharing
                    mSnackBar.setText(R.string.turnOffBluetoothContactSharing)
                            .setDuration(Snackbar.LENGTH_SHORT).show();
                    blueHelper.setContactSharing(ContactSharingOff);
                    setSyncContactImage(R.drawable.ic_sync_paired_off_grey);
                    isFilteringPaired = true;
                } else { // turn On Contact Sharing
                    mSnackBar.setText(R.string.waitBluetoothContactSharing)
                            .setAction(R.string.cancel, mCancelSnackbarAction)
                            .setDuration(CONTACT_SYNC_DURATION).show();
                    blueHelper.setContactSharing(ContactSharingOn);
                    setSyncContactImage(R.drawable.ic_sync_paired_on);
                    isFilteringPaired = false;
                }
                mContactsRecyclerViewAdapter.setmFilteringPaired(isFilteringPaired);
                performSelection();
            } else { // show Dialog confirm -> go to Device Bluetooth Settings
                mSyncPopupText.setText(R.string.sync_contact_text);
                mSyncPopupTitle.setText(R.string.sync_contact_title);
                mSyncUpSlider.expand(mSyncDialog,false);

                if(getResources().getBoolean(R.bool.is_landscape) == true){
                    syncPopupAlertDialog();
                }
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
                if(getResources().getBoolean(R.bool.is_landscape) == true){
                    mSyncUpSlider.collapse(mSyncDialog,false);
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

        mSyncPopupText.setText(R.string.sync_contact_text);
        mSyncPopupTitle.setText(R.string.sync_contact_title);


        alertDialog = dialogBuilder.create();
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        alertDialog.show();
    }

    /**
     * Determines whether bluetooth paired Contact Sharing is enabled.
     *
     * @return true if bluetooth paired Contact Sharing is enabled, false otherwise
     */
    private boolean showPaired() {
        return blueHelper.isBluetoothLinkEstablished()
                && blueHelper.isContactSharingEnabled();
    }

    /**
     * Method to be called within ContentObserver change for Contacts
     */
    public void contactTableUpdated() {
        prepareSyncIcon();
    }

    @Override
    public void retrieveEditableContact(EditableContact editableContact) {

    }

    /**
     * Notifies user that the contact is deleted
     */
    @Override
    public void contactDeleted() {
        if(getContext() != null) {
            Utils.sendSnackBarData(getContext(), getString(R.string.contact_deleted), Utils.SNACKBAR_LONG);
        }
    }

    @Override
    public void reportError() {

    }

    @Override
    public void reportDeleteError() {
        if(getContext() != null) {
            Utils.sendSnackBarData(getContext(), getString(R.string.contact_undeletable_error), Utils.SNACKBAR_LONG);
        }
    }
}
