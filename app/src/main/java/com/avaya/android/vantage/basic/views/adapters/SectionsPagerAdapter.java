package com.avaya.android.vantage.basic.views.adapters;


import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.View;

import com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor;
import com.avaya.android.vantage.basic.adaptors.UIHistoryViewAdaptor;
import com.avaya.android.vantage.basic.adaptors.UIVoiceMessageAdaptor;
import com.avaya.android.vantage.basic.csdk.ConfigParametersNames;
import com.avaya.android.vantage.basic.csdk.ContactsLoader;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.ContactDetailsFragment;
import com.avaya.android.vantage.basic.fragments.ContactsFragment;
import com.avaya.android.vantage.basic.fragments.DialerFragment;
import com.avaya.android.vantage.basic.fragments.FavoritesFragment;
import com.avaya.android.vantage.basic.fragments.PlaceholderFragment;
import com.avaya.android.vantage.basic.fragments.RecentCallsFragment;
import com.avaya.android.vantage.basic.model.ContactData;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private static final String TAG = "SectionsPagerAdapter";

    private ContactData mContactDetails;
    private boolean isFavTabPresent = false;
    private boolean isContactsTabPresent = false;
    private boolean isRecentTabPresent = false;

    private FavoritesFragment mFragmentFavorites;
    private ContactsFragment mFragmentContacts;
    private RecentCallsFragment mFragmentHistory;
    private ContactDetailsFragment mContactDetailsFragment;
    private DialerFragment mDialerFragment;

    private Parcelable mContactsListPosition;
    private Parcelable mFavoritesListPosition;
    private Parcelable mRecentCallsListPosition;

    private boolean isAddingCallParticipant = false;

    private ContactsLoader localContacts;

    private UIHistoryViewAdaptor mHistoryViewAdaptor;
    private UIVoiceMessageAdaptor mVoiceMessageAdaptor;
    private UIContactsViewAdaptor mContactsViewAdaptor;

    // this flag indicates whether the number and content of tabs in the TabLayout shall be changed as result of config-n changes
    private boolean allowReconfiguration = false;

    public SectionsPagerAdapter(FragmentManager fm) {
        super(fm);
        mHistoryViewAdaptor = new UIHistoryViewAdaptor();
    }

    public void setContactDetails(ContactData contactData) {
        mContactDetails = contactData;
        allowReconfiguration = true;
        notifyDataSetChanged();
        allowReconfiguration = false;
    }

    /**
     * Removes ContactDetailsFragment form the Pager Adapter
     */
    public void clearContactDetails() {
        mContactDetails = null;
        allowReconfiguration = true;
        notifyDataSetChanged();
        allowReconfiguration = false;
    }

    /**
     * Called when the host view is attempting to determine if an item's position
     * has changed. Returns {@link #POSITION_UNCHANGED} if the position of the given
     * item has not changed or {@link #POSITION_NONE} if the item is no longer present
     * in the adapter.
     * <p>
     * <p>The default implementation assumes that items will never
     * change position and always returns {@link #POSITION_UNCHANGED}.
     *
     * @param object Object representing an item, previously returned by a call to
     *               {@link #instantiateItem(View, int)}.
     * @return object's new position index from [0, {@link #getCount()}),
     * {@link #POSITION_UNCHANGED} if the object's position has not changed,
     * or {@link #POSITION_NONE} if the item is no longer present.
     */
    @Override
    public int getItemPosition(Object object) {

        // when pressing back, refresh only contact details fragment and not its neighbours.
        if (allowReconfiguration && (mContactDetails != null || object.equals(mContactDetailsFragment))) {
            Log.d(TAG, "getItemPosition : removing " +object.getClass().getSimpleName());
            return POSITION_NONE;
        }

        return super.getItemPosition(object);
    }

    @Override
    public Fragment getItem(int position) {

        // handle user's click for contact's details either from Contact or Favorites or Recent Call tabs
        if (mContactDetails != null) {
            setListPositions(position);
            setContactDetailsFragment(ContactDetailsFragment.newInstance(mContactDetails));
            mContactDetails = null;
            return getContactDetailsFragment();
        }

        if (isCallAddParticipant()) {
            return getItemDuringAddParticipant(position);
        } else {
            // getItem is called to instantiate the fragment for the given page.
            return getItemRegular(position);
        }
    }

    /**
     * Instantiate the fragment for the given page while there IS active
     * "add participant for conference or transfer" process
     *
     * @param position of Fragment which we need
     * @return Fragment
     */
    private Fragment getItemDuringAddParticipant(int position) {

        switch (position) {
            case 0:
                if (isFavTabPresent) {
                    setFragmentFavorites(FavoritesFragment.newInstance(isCallAddParticipant()));
                    getFragmentFavorites().setUIContactsAdaptor(mContactsViewAdaptor);
                    getFragmentFavorites().setmContactsLoader(getLocalContacts());
                    return getFragmentFavorites();
                }
                // intentionally missing break: if favorities tab is not present, another tab at place 0 shall appear
            case 1:
                if (isContactsTabPresent && (position == 0 || isFavTabPresent)) {
                    setFragmentContacts(ContactsFragment.newInstance(1, isCallAddParticipant(),false));
                    getFragmentContacts().setUIContactsAdaptor(mContactsViewAdaptor);
                    getFragmentContacts().setmContactsLoader(getLocalContacts());
                    return getFragmentContacts();
                }
                // intentionally missing break: if contact tab is not present, another tab at place 1 shall appear
            case 2:
                if (isRecentTabPresent) {
                    setFragmentRecent(RecentCallsFragment.newInstance(1, isCallAddParticipant()));
                    getFragmentRecent().setUIHistoryContactsAdaptor(getHistoryViewAdaptor());
                    return getFragmentRecent();
                }
            default:
                return PlaceholderFragment.newInstance(position + 1);
        }
    }

    /**
     * Instantiate the fragment for the given page while there is NO active
     * "add participant for conference or transfer" process
     *
     * @param position of Fragment to be returned
     * @return Fragment on specific position
     */
    private Fragment getItemRegular(int position) {

        switch (position) {
            case 0:
                setmDialerFragment(DialerFragment.newInstance("", "", "", mDialerFragment == null ? DialerFragment.DialMode.EDIT : mDialerFragment.getMode()));
                getDialerFragment().setUIContactsAdaptor(mContactsViewAdaptor);
                getVoiceMessageAdaptor().setViewInterface(getDialerFragment());
                return getDialerFragment();
            case 1:
                if (isFavTabPresent) {
                    setFragmentFavorites(FavoritesFragment.newInstance(isCallAddParticipant()));
                    getFragmentFavorites().setUIContactsAdaptor(mContactsViewAdaptor);
                    getFragmentFavorites().setmContactsLoader(getLocalContacts());
                    return getFragmentFavorites();
                }
                // intentionally missing break: if favorities tab is not present, another tab at place 1 shall appear
            case 2:
                if (isContactsTabPresent && (position == 1 || isFavTabPresent)) {
                    setFragmentContacts(ContactsFragment.newInstance(1, isCallAddParticipant(),false));
                    getFragmentContacts().setUIContactsAdaptor(mContactsViewAdaptor);
                    getFragmentContacts().setmContactsLoader(getLocalContacts());
                    return getFragmentContacts();
                }
                // intentionally missing break: if favorities tab is not present, another tab at place 2 shall appear
            case 3:
                if (isRecentTabPresent) {
                    setFragmentRecent(RecentCallsFragment.newInstance(1, isCallAddParticipant()));
                    getFragmentRecent().setUIHistoryContactsAdaptor(getHistoryViewAdaptor());
                    return getFragmentRecent();
                }
            default:
                return PlaceholderFragment.newInstance(position + 1);
        }
    }

    @Override
    public int getCount() {

        int idx = 0;

        if (!isCallAddParticipant()) {
            idx++;                    // dialer is present
        }
        if (isFavTabPresent) {
            idx++;
        }
        if (isContactsTabPresent) {
            idx++;
        }
        if (isRecentTabPresent) {
            idx++;
        }
        return idx;
    }

    /**
     * Setting list positions based on do we adding participants in currently ongoing call
     * or we are just showing list
     *
     * @param position of list which have to be shown
     */
    private void setListPositions(int position) {
        if (isCallAddParticipant()) {
            switch (position) {
                case 1:
                    if(getFragmentContacts()!=null)
                        setContactsListPosition(getFragmentContacts().getListPosition());
                    break;
                case 2:
                    if(getFragmentRecent()!=null)
                        setRecentCallsListPosition(getFragmentRecent().getListPosition());
                    break;
                default:
                    //default option
            }
        } else {
            switch (position) {
                case 1:
                    if(getFragmentFavorites()!=null)
                        setFavoritesListPosition(getFragmentFavorites().getListPosition());
                    break;
                case 2:
                   if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CONTACTS)==true) {
                       if(getFragmentContacts()!=null)
                           setContactsListPosition(getFragmentContacts().getListPosition());
                    }else{
                       if(getFragmentRecent()!=null)
                            setRecentCallsListPosition(getFragmentRecent().getListPosition());
                   }
                    break;
                case 3:
                    if(getFragmentRecent()!=null)
                        setRecentCallsListPosition(getFragmentRecent().getListPosition());
                    break;
                default:
                    //default option
            }
        }
    }

    /**
     * @return Favorites tab is currently visible
     */
    public boolean isFavoriteTabPresent() {
        return isFavTabPresent;
    }

    /**
     * @return Contacts tab is currently visible
     */
    public boolean isContactsTabPresent() {
        return isContactsTabPresent;
    }

    /**
     * @return Recent tab is currently visible
     */
    public boolean isRecentTabPresent() {
        return isRecentTabPresent;
    }

    /**
     * @param allowReconfiguration is reconfiguration in progress
     */
    public void setAllowReconfiguration(boolean allowReconfiguration) {
        this.allowReconfiguration = allowReconfiguration;
    }

    /**
     * Returning FavoritesFragment
     *
     * @return FavoritesFragment
     */
    public FavoritesFragment getFragmentFavorites() {
        return mFragmentFavorites;
    }

    /**
     * Set FavoritesFragment
     *
     * @param fragmentFavorites Favorites fragment
     */
    public void setFragmentFavorites(FavoritesFragment fragmentFavorites) {
        mFragmentFavorites = fragmentFavorites;
    }

    /**
     * Get ContactsFragment
     *
     * @return ContactsFragment
     */
    public ContactsFragment getFragmentContacts() {
        return mFragmentContacts;
    }

    /**
     * Set ContactsFragment
     *
     * @param fragmentContacts Contacts fragment
     */
    public void setFragmentContacts(ContactsFragment fragmentContacts) {
        mFragmentContacts = fragmentContacts;
    }

    /**
     * Get RecentCallFragment
     *
     * @return RecentCallsFragment
     */
    public RecentCallsFragment getFragmentRecent() {
        return mFragmentHistory;
    }

    /**
     * Set RecentCallsFragment
     *
     * @param fragmentHistory History fragment
     */
    public void setFragmentRecent(RecentCallsFragment fragmentHistory) {
        mFragmentHistory = fragmentHistory;
    }

    /**
     * Get ContactDetailsFragment
     *
     * @return ContactDetailsFragment
     */
    public ContactDetailsFragment getContactDetailsFragment() {
        return mContactDetailsFragment;
    }

    /**
     * Set ContactDetailsFragment
     *
     * @param contactDetailsFragment Contacts fragment
     */
    private void setContactDetailsFragment(ContactDetailsFragment contactDetailsFragment) {
        mContactDetailsFragment = contactDetailsFragment;
    }

    /**
     * Obtain Contact list position in form of Parcelable from which we can restore list position
     *
     * @return Parcelable
     */
    public Parcelable getContactsListPosition() {
        return mContactsListPosition;
    }

    /**
     * Set Parcelable with Contact list position
     *
     * @param contactsListPosition contacts list position
     */
    private void setContactsListPosition(Parcelable contactsListPosition) {
        mContactsListPosition = contactsListPosition;
    }

    /**
     * Obtain Favorites list position in form of Parcelable from which we can restore list position
     *
     * @return Parcelable
     */
    public Parcelable getFavoritesListPosition() {
        return mFavoritesListPosition;
    }

    /**
     * Set Parcelable with Favorites list position
     *
     * @param favoritesListPosition save favorites list position
     */
    private void setFavoritesListPosition(Parcelable favoritesListPosition) {
        mFavoritesListPosition = favoritesListPosition;
    }

    /**
     * Obtain RecentCall list position in form of Parcelable from which we can restore list position
     *
     * @return Parcelable
     */
    public Parcelable getRecentCallsListPosition() {
        return mRecentCallsListPosition;
    }

    /**
     * Set Parcelable with RecentCall list position
     *
     * @param recentCallsListPosition recent calls list position
     */
    private void setRecentCallsListPosition(Parcelable recentCallsListPosition) {
        mRecentCallsListPosition = recentCallsListPosition;
    }

    /**
     * Is call in state when we are adding additional participants
     *
     * @return boolean representing state
     */
    public boolean isCallAddParticipant() {
        return isAddingCallParticipant;
    }

    /**
     * Set call in state where we add additional participant
     *
     * @param callAddParticipant adding participant
     */
    public void setCallAddParticipant(boolean callAddParticipant) {
        isAddingCallParticipant = callAddParticipant;
    }

    /**
     * Get ContactsLoader set of data
     *
     * @return ContactsLoader
     */
    public ContactsLoader getLocalContacts() {
        return localContacts;
    }

    /**
     * Set ContactLoader which loading contacts
     *
     * @param contactsLoader contacts loader
     */
    public void setLocalContacts(ContactsLoader contactsLoader) {
        this.localContacts = contactsLoader;
    }

    /**
     * Get DialerFragment
     *
     * @return DialerFragment
     */
    public DialerFragment getDialerFragment() {
        return mDialerFragment;
    }

    /**
     * Set DialerFragment
     *
     * @param dialerFragment Dialer Fragment
     */
    private void setmDialerFragment(DialerFragment dialerFragment) {
        mDialerFragment = dialerFragment;
    }

    /**
     * Get UIHistoryAdapter which is used for RecentCalls list
     *
     * @return UIHistoryViewAdaptor
     */
    public UIHistoryViewAdaptor getHistoryViewAdaptor() {
        return mHistoryViewAdaptor;
    }

    /**
     * Get UIVoiceMessageAdaptor
     *
     * @return UIVoiceMessageAdaptor
     */
    public UIVoiceMessageAdaptor getVoiceMessageAdaptor() {
        return mVoiceMessageAdaptor;
    }

    /**
     * Set UIVoiceMessageAdaptor
     *
     * @param voiceMessageAdaptor set voice message adapter
     */
    public void setVoiceMessageAdaptor(UIVoiceMessageAdaptor voiceMessageAdaptor) {
        mVoiceMessageAdaptor = voiceMessageAdaptor;
    }

    /**
     * Set UIContactsViewAdaptor
     *
     * @param contactsViewAdaptor UIContactsAdaptor created in {@MainActivity}
     */
    public void setContactsViewAdaptor(UIContactsViewAdaptor contactsViewAdaptor) {
        mContactsViewAdaptor = contactsViewAdaptor;
    }


    /**
     * Configure what tab shall be present in the section bar
     */
    public void configureTabLayout() {

        allowReconfiguration = true;
        boolean tabLayoutChanged = false;
        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor()
                .getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) != isFavTabPresent) {
            Log.d(TAG, "configureTabLayout favoriteTabPresent changed");
            isFavTabPresent = !isFavTabPresent;
            tabLayoutChanged = true;
        }

        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor()
                .getConfigBooleanParam(ConfigParametersNames.ENABLE_CONTACTS) != isContactsTabPresent) {
            Log.d(TAG, "configureTabLayout contactsTabPresent changed");
            isContactsTabPresent = !isContactsTabPresent;
            tabLayoutChanged = true;
        }
        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor()
                .getConfigBooleanParam(ConfigParametersNames.ENABLE_CALL_LOG) != isRecentTabPresent) {
            Log.d(TAG, "configureTabLayout recentTabIsPresent changed");
            isRecentTabPresent = !isRecentTabPresent;
            tabLayoutChanged = true;
        }

        if (tabLayoutChanged) {
            clearContactDetails();
            notifyDataSetChanged();
        }

        allowReconfiguration = false;
    }


    @Override
    public Parcelable saveState() {
        return null;
    }
}
