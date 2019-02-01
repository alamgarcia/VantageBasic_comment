package com.avaya.android.vantage.basic.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor;
import com.avaya.android.vantage.basic.csdk.ConfigParametersNames;
import com.avaya.android.vantage.basic.csdk.FavoritesAdaptorListener;
import com.avaya.android.vantage.basic.csdk.ContactsLoader;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.views.EmptyRecyclerView;
import com.avaya.android.vantage.basic.views.adapters.MyFavoritesRecyclerViewAdapter;

import java.util.List;

/**
 * Fragment that shows list of favorite user contacts.
 */
public class FavoritesFragment extends android.support.v4.app.Fragment implements FavoritesAdaptorListener {

    private static final String TAG = "FavoritesFragment";
    private static final int FIRST_NAME_FIRST = Constants.FIRST_NAME_FIRST;
    private static final int LAST_NAME_FIRST = Constants.LAST_NAME_FIRST;
    private static final String NAME_DISPLAY_PREFERENCE = Constants.NAME_DISPLAY_PREFERENCE;
    private static final String REFRESH_FAVORITES = "refreshFavorites";
    private static final String USER_PREFERENCE = Constants.USER_PREFERENCE;
    private static final String NAME_SORT_PREFERENCE = Constants.NAME_SORT_PREFERENCE;
    private static final String ADD_PARTICIPANT = "addParticipant";
    private String ADMIN_NAME_DISPLAY_ORDER = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_DISPLAY_ORDER);
    private String ADMIN_NAME_SORT_ORDER = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_SORT_ORDER);

    private MyFavoritesRecyclerViewAdapter mFavoritesRecyclerViewAdapter;

    private EmptyRecyclerView mFavoriteList;
    private TextView mFavoritesTitle;
    private OnContactInteractionListener mListener;
    private SharedPreferences mUserPreference;
    private ContactsLoader mContactsLoader;
    private boolean addParticipant;
    private int mNameDisplayType;
    private int mNameSortType = Constants.LAST_NAME_FIRST;

    private UIContactsViewAdaptor mUIContactsAdaptor;
    private ContactViewAdaptorInterface mAdapterInstance;

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static FavoritesFragment newInstance(boolean addParticipant) {
        FavoritesFragment fragment = new FavoritesFragment();
        Bundle args = new Bundle();
        args.putBoolean(ADD_PARTICIPANT, addParticipant);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FavoritesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            addParticipant = getArguments().getBoolean(ADD_PARTICIPANT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_favorites_list, container, false);
        mFavoriteList = (EmptyRecyclerView) root.findViewById(R.id.favorite_recycler_list);
        mFavoritesTitle = (TextView) root.findViewById(R.id.favorites_title);
        if(isAdded()&&getResources().getBoolean(R.bool.is_landscape) == true){
            mFavoritesTitle.setVisibility(View.GONE);
        }else{
            mFavoritesTitle.setVisibility(View.VISIBLE);
        }
        TextView emptyView = (TextView) root.findViewById(R.id.empty_favorites);

        // View with message when list is empty.
        mFavoriteList.setEmptyView(emptyView);

        final SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) root.findViewById(R.id.favorite_swipe_layout);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mUIContactsAdaptor != null) {
                    mUIContactsAdaptor.refresh(swipeRefresh);
                }
                if (mContactsLoader != null) {
                    getActivity().getLoaderManager().restartLoader(Constants.LOCAL_ADDRESS_LOADER, null, mContactsLoader);
                }
            }
        });
        mUserPreference = getActivity().getSharedPreferences(USER_PREFERENCE, Context.MODE_PRIVATE);
        initRecyclerView();
        setFavoritesCount();

        return root;
    }

    /**
     * Initiate Favorite contacts list.
     */
    private void initRecyclerView() {

        if (mUIContactsAdaptor == null && mAdapterInstance != null) {
            mUIContactsAdaptor = mAdapterInstance.getContactViewAdapter();
        }
        if (mUIContactsAdaptor != null) {
            mFavoritesRecyclerViewAdapter = new MyFavoritesRecyclerViewAdapter(mUIContactsAdaptor.getAllContacts(), mListener, this, getContext());
            mFavoritesRecyclerViewAdapter.setAddParticipant(addParticipant);
            nameDisplaySet();
            mUIContactsAdaptor.setFavoritesViewInterface(mFavoritesRecyclerViewAdapter);
            mFavoriteList.setAdapter(mFavoritesRecyclerViewAdapter);
            mFavoriteList.setHasFixedSize(true);
            Log.i(TAG, "Recycler view initialized");
        } else {
            Log.e(TAG, "UI contact adaptor is empty");
        }

    }


    /**
     * Set a number of favorite items text.
     */
    private void setFavoritesCount() {
        // TODO if permissions are not accepted, adapter wont be made.
        if (getActivity() != null) {
            if (mFavoritesRecyclerViewAdapter != null) {
                int numberOfFavorites = mFavoritesRecyclerViewAdapter.getItemCount();
                if (mFavoritesTitle != null) {
                    mFavoritesTitle.setText(getText(R.string.favorites_title) + " (" + numberOfFavorites + ")");
                }
                Log.i(TAG, "HeaderView initialized");
            } else {
                Log.e(TAG, "HeaderView cannot be initialized, adapter missing");
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
     * Managing name display method (first name first, last name first)
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
        if (nameDisplay != mNameDisplayType && mFavoritesRecyclerViewAdapter != null) {
            mFavoritesRecyclerViewAdapter.firstNameFirst(nameDisplay == FIRST_NAME_FIRST);
            mFavoritesRecyclerViewAdapter.notifyDataSetChanged();
            reloadFavorites();
            mUIContactsAdaptor.refresh();
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

        if (ADMIN_NAME_SORT_ORDER != null){
            // doing this to prevent bug in case someone entered a value that is different from 1 or 0.
            if (ADMIN_NAME_SORT_ORDER.equals("first,last")){
                defaultValue = FIRST_NAME_FIRST;
            } else {
                defaultValue = LAST_NAME_FIRST;
            }
        }

        int nameSort = mUserPreference.getInt(NAME_SORT_PREFERENCE, defaultValue);

        if (mNameSortType != nameSort) {
            mUIContactsAdaptor.removeContacts();
            SDKManager.getInstance().displayFirstNameFirst(nameSort != LAST_NAME_FIRST);
            mUIContactsAdaptor.refresh();
            mNameSortType = nameSort;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        boolean settingsChanged = mUserPreference.getBoolean(REFRESH_FAVORITES, false);
        // making sure we call methods only if there is a change in the settings
        if (settingsChanged) {
            nameSortSet();
            SharedPreferences.Editor editor = mUserPreference.edit();
            editor.putBoolean(REFRESH_FAVORITES, false);
            editor.apply();
        }
        nameDisplaySet();
    }

    /**
     * Saving user settings
     *
     * @param prefKey   preference key
     * @param prefValue selected value
     */
    private void saveData(String prefKey, boolean prefValue) {
        if (prefKey != null) {
            SharedPreferences.Editor editor = mUserPreference.edit();
            editor.putBoolean(prefKey, prefValue);
            editor.apply();
        } else {
            Log.d(TAG, "pref is null");
        }
    }


    /**
     * Setting up {@link UIContactsViewAdaptor} for favorites fragment
     *
     * @param UIContactsAdaptor
     */
    public void setUIContactsAdaptor(UIContactsViewAdaptor UIContactsAdaptor) {
        this.mUIContactsAdaptor = UIContactsAdaptor;
    }

    /**
     * Get list position and return it in {@link Parcelable}
     *
     * @return {@link Parcelable}
     */
    public Parcelable getListPosition() {
        if (mFavoriteList != null) {
            return mFavoriteList.getLayoutManager().onSaveInstanceState();
        } else {
            return null;
        }
    }

    /**
     * Restoring list position from {@link Parcelable}
     *
     * @param position {@link Parcelable}
     */
    public void restoreListPosition(Parcelable position) {
        if (position != null) {
            mFavoriteList.getLayoutManager().onRestoreInstanceState(position);
        }
    }

//    /**
//     * This method will enable video
//     */
//    public void enableVideo() {
//        if(mFavoritesRecyclerViewAdapter != null)
//            mFavoritesRecyclerViewAdapter.notifyDataSetChanged();
//    }

    /**
     * Setting up mContactsLoader for this fragment to be able to refresh Loader
     *
     * @param mContactsLoader getting this info from Main Activity
     */
    public void setmContactsLoader(ContactsLoader mContactsLoader) {
        this.mContactsLoader = mContactsLoader;
    }

    /**
     * Processing notification of favorites changed
     */
    @Override
    public void notifyFavoritesChanged() {
        setFavoritesCount();
    }

    /**
     * Reload favorites list
     */
    private void reloadFavorites() {
        List<ContactData> listLocal = mUIContactsAdaptor.getLocalFavorites();
        if (listLocal.size() > 0) {
            mFavoritesRecyclerViewAdapter.recreateData(listLocal, ContactData.Category.LOCAL);
        }
        mUIContactsAdaptor.refresh();
    }

    /**
     *
     * This method will be called every time Favorites fragment is active.
     * Can be called even when application is being recreated so we need to
     * check if activity still exist.
     */
    public void fragmentSelected() {
        if (mFavoritesRecyclerViewAdapter != null){
            mFavoritesRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Setting up is view open for adding participant from join conference call
     *
     * @param add
     */
    public void setAddParticipantData(boolean add) {
        addParticipant = add;
        if (mFavoritesRecyclerViewAdapter != null && mFavoriteList != null) {
            mFavoritesRecyclerViewAdapter.setAddParticipant(addParticipant);
            mFavoritesRecyclerViewAdapter.notifyDataSetChanged();
            mFavoriteList.setAdapter(mFavoritesRecyclerViewAdapter);
        }
    }
}