package com.avaya.android.vantage.basic.adaptors;

import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import com.avaya.android.vantage.basic.csdk.ContactsAdaptor;
import com.avaya.android.vantage.basic.csdk.ContactsAdaptorListener;
import com.avaya.android.vantage.basic.csdk.ContactsLoader;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.ContactsFragment;
import com.avaya.android.vantage.basic.fragments.FavoritesFragment;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.views.interfaces.IContactsViewInterface;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_IPOFFICE;

/**
 * Contact view adaptor responsible for preparing and showing contact data
 */

public class UIContactsViewAdaptor implements ContactsAdaptorListener {
    private static final String TAG = "UIContactsViewAdaptor";
    private final String LOG_TAG = this.getClass().getSimpleName();
    private List<ContactData> mLocalContacts = new LinkedList<>();
    private List<ContactData> mIpoContacts = new LinkedList<>();
    private List<ContactData> mEnterpriseContacts = new ArrayList<>();
    private List<ContactData> mAllContacts = new LinkedList<>();
    private List<ContactData> mLocalFavorites = new LinkedList<>();
    private List<ContactData> mEnterpriseFavorites = new LinkedList<>();

    private IContactsViewInterface mFavoritesViewInterface, mContactsViewInterface;
    private final ContactData mPendingEnterprise;
    private SwipeRefreshLayout mSwipeRefresh;
    private int mContactDisplaySelection = ContactsFragment.ALL; // represents users selection of contacts to display

    /**
     * {@link UIContactsViewAdaptor} public constructor
     */
    public UIContactsViewAdaptor() {
        mPendingEnterprise = ContactData.getDummyContactForPendingUpdate(ContactData.Category.ENTERPRISE);
    }

    /**
     * While data retrieval is in progress we are setting and adding headers.
     *
     * @param contacts        list of {@link ContactData} to be presented
     * @param determinate     currently not in use
     * @param numRetrieved    currently not in use
     * @param total           currently not in use
     * @param contactCategory {@link com.avaya.android.vantage.basic.model.ContactData.Category} which
     *                        we will be showing
     */
    @Override
    public void onDataRetrievalProgress(List<ContactData> contacts, boolean determinate, int numRetrieved, int total, ContactData.Category contactCategory) {
        Log.d(LOG_TAG, "onDataRetrievalProgress");

        if (mFavoritesViewInterface != null) {
            mEnterpriseContacts = contacts;
            mFavoritesViewInterface.recreateData(mEnterpriseContacts, ContactData.Category.ENTERPRISE);
            mFavoritesViewInterface.addItem(mPendingEnterprise);
        }
        if (mContactsViewInterface != null) {
            mEnterpriseContacts = contacts;
            mContactsViewInterface.recreateData(mEnterpriseContacts, ContactData.Category.ENTERPRISE);
            mContactsViewInterface.addItem(mPendingEnterprise);
        }
    }

    /**
     * Refreshing swipe view and removing {@link ContactData}
     *
     * @param contactDataList list of {@link ContactData} to be set as {@link #mEnterpriseContacts}
     * @param contactCategory not in use
     */
    @Override
    public void onDataRetrievalComplete(List<ContactData> contactDataList, ContactData.Category contactCategory) {
        mEnterpriseContacts = contactDataList;

        if (mFavoritesViewInterface != null) {
            mFavoritesViewInterface.removeItem(mPendingEnterprise);
        }
        if (mContactsViewInterface != null) {
            mContactsViewInterface.removeItem(mPendingEnterprise);
        }

        if (mSwipeRefresh != null) {
            mSwipeRefresh.setRefreshing(false);
        }
    }

    /**
     * In case {@link ContactsAdaptorListener} returns data retrieval failure this method
     * is triggered
     *
     * @param failure {@link Exception} which is received from {@link ContactsAdaptorListener}
     */
    @Override
    public void onDataRetrievalFailed(Exception failure) {
        Log.d(LOG_TAG, "onDataRetrievalFailed");
        failure.printStackTrace();
        if (mSwipeRefresh != null) {
            mSwipeRefresh.setRefreshing(false);
        }
    }

    /**
     * Triggering in case data set is changed
     *
     * @param type           {@link com.avaya.android.vantage.basic.csdk.ContactsAdaptorListener.ChangeType}
     *                       which have triggered this change
     * @param changedIndices Changed indices
     */
    @Override
    public void onDataSetChanged(ChangeType type, List<Integer> changedIndices) {
        Log.d(LOG_TAG, "onDataSetChanged");
    }

    /**
     * In case data set is invalidated we are replacing {@link #mEnterpriseContacts} with provided
     * {@link ContactData} list
     *
     * @param contactDataList list of {@link ContactData} to replace {@link #mEnterpriseContacts}
     */
    @Override
    public void onDataSetInvalidated(List<ContactData> contactDataList) {
        mEnterpriseContacts = contactDataList;
        Log.d(LOG_TAG, "onDataSetInvalidated");
    }

    /**
     * Updating {@link ContactData} item when contact photo is ready
     *
     * @param contactData {@link ContactData} for which photo is updated
     */
    @Override
    public void onContactPhotoReady(ContactData contactData) {
        if (mFavoritesViewInterface != null) {
            mFavoritesViewInterface.updateItem(contactData);
        }
        if (mContactsViewInterface != null) {
            mContactsViewInterface.updateItem(contactData);
        }
    }

    /**
     * Obtaining list of {@link ContactData}
     *
     * @return list of {@link ContactData}
     */
    @Override
    public List<ContactData> getEnterpriseContacts() {
        return mEnterpriseContacts;
    }

    /**
     * Obtaining list of local {@link ContactData}
     *
     * @return list of local {@link ContactData}
     */
    @Override
    public List<ContactData> getLocalContacts() {
        return mLocalContacts;
    }

    /**
     * Obtaining list of all {@link ContactData}. Clearing {@link #mAllContacts} and replacing
     * content with {@link #mEnterpriseContacts} and {@link #mLocalContacts}
     *
     * @return list of {@link ContactData}
     */
    @Override
    public List<ContactData> getAllContacts() {
        mAllContacts.clear();
        mAllContacts.addAll(mLocalContacts);
        mAllContacts.addAll(mEnterpriseContacts);
        mAllContacts.addAll(mIpoContacts);
        return mAllContacts;
    }

    /**
     * Used by {@link FavoritesFragment} to load favorites
     *
     * @return list of all favorites
     */
    @Override
    public List<ContactData> getLocalFavorites() {
        return mLocalFavorites;
    }

    @Override
    public List<ContactData> getIpoContacts() {
        return mIpoContacts;
    }

    /**
     * Sending search query which is provided as parameter to {@link SDKManager}
     *
     * @param query which is sent to {@link SDKManager} to perform search
     */
    @Override
    public void search(String query) {
        SDKManager.getInstance().getContactsAdaptor().search(query);
    }

    /**
     * Search for enterprise contact saved in local contact list.
     *
     * @param phoneNumber Search query.
     * @return Contact if its found, empty contact if not.
     */
    public ContactData searchForEnterpriseContact(String phoneNumber) {
        if (mEnterpriseContacts.size() > 0) {
            List<ContactData.PhoneNumber> listOfPhones;
            for (int i = 0; i < mEnterpriseContacts.size(); i++) {
                listOfPhones = mEnterpriseContacts.get(i).mPhones;
                if (listOfPhones != null && listOfPhones.size() > 0) {
                    for (int j = 0; j < listOfPhones.size(); j++) {
                        String numberToCompare = listOfPhones.get(j).Number;
                        if (numberToCompare.contains("@")) {
                            numberToCompare = numberToCompare.substring(0, numberToCompare.indexOf("@"));
                        }
                        if (phoneNumber.equalsIgnoreCase(numberToCompare)) {
                            return mEnterpriseContacts.get(i);
                        }
                    }
                }
            }
        }
        return ContactData.getEmptyContactData();
    }

    /**
     * Removing all contacts from {@link ContactData} list which represent list of all contacts
     * {@link #mAllContacts}
     */
    public void removeContacts() {
        mAllContacts.clear();
    }

    /**
     * Setting favorites view interface
     *
     * @param favoritesViewInterface {@link IContactsViewInterface}
     */
    public void setFavoritesViewInterface(IContactsViewInterface favoritesViewInterface) {
        this.mFavoritesViewInterface = favoritesViewInterface;
    }

    /**
     * Setting contact view interface {@link IContactsViewInterface}
     *
     * @param favoritesViewInterface {@link IContactsViewInterface}
     */
    public void setContactsViewInterface(IContactsViewInterface favoritesViewInterface) {
        this.mContactsViewInterface = favoritesViewInterface;
    }

    /**
     * Refreshing and retrieving contacts. {@link SwipeRefreshLayout} is provided as parameter and
     * set to {@link #mSwipeRefresh}
     *
     * @param swipeRefresh {@link SwipeRefreshLayout} to be set to {@link #mSwipeRefresh}
     */
    public void refresh(SwipeRefreshLayout swipeRefresh) {
        mSwipeRefresh = swipeRefresh;
        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ENABLE_IPOFFICE)){
            if(mSwipeRefresh!=null)
                mSwipeRefresh.setRefreshing(false);
        } else {
            SDKManager.getInstance().getContactsAdaptor().retrieveContacts();
        }
    }

    /**
     * Refreshing and retrieving contacts.
     */
    public void refresh() {
        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ENABLE_IPOFFICE) && mSwipeRefresh != null){
            if(mSwipeRefresh!=null)
                mSwipeRefresh.setRefreshing(false);
        } else {
            SDKManager.getInstance().getContactsAdaptor().retrieveContacts();
        }
    }

    /**
     * Interface which provide method for setting photos. It is used in
     * {@link com.avaya.android.vantage.basic.csdk.ContactsAdaptor}
     */
    public interface OnPhotoInCache {

        /**
         * Setting photo to contact {@link ContactData} from parameters provided
         *
         * @param contactData {@link ContactData}
         */
        void setPhoto(ContactData contactData);
    }

    /**
     * Setting local contacts list
     *
     * @param contactData list containing {@link ContactData}
     */
    public void setLocalContacts(List<ContactData> contactData) {
        this.mLocalContacts = contactData;
        if (mContactsViewInterface != null) {
            mContactsViewInterface.recreateData(contactData, ContactData.Category.LOCAL);
        }
        if (contactData != null) {
            Log.e(TAG, "LocalContactsReceived. Number of contacts: " + contactData.size());
        }
    }

    public void setIpoContacts(List<ContactData> ipoContacts){
        this.mIpoContacts = ipoContacts;
        if (mContactsViewInterface != null){
            mContactsViewInterface.recreateData(ipoContacts, ContactData.Category.IPO);
        }
    }

    /**
     * Called by {@link ContactsLoader} to add all local contact favorites to appropriate list
     *
     * @param mLocalFavorites When all contacts are loaded, we pass list of favorite contacts
     */
    public void setLocalFavorites(List<ContactData> mLocalFavorites) {
        this.mLocalFavorites = mLocalFavorites;
        Log.d(TAG, "Favorite contacts set. Number of favorite contacts: " + mLocalFavorites.size());
        if (mFavoritesViewInterface != null) {
            mFavoritesViewInterface.recreateData(mLocalFavorites, ContactData.Category.LOCAL);
        }
    }

    /**
     * Called by {@link ContactsAdaptor} to add Enterprise cotnacts favorites to appropriate list
     *
     * @param mEnterpriseFavorites when enterprise contacts are loaded, we pass the list
     */
    public void setEnterpriseFavorites(List<ContactData> mEnterpriseFavorites) {
        this.mEnterpriseFavorites = mEnterpriseFavorites;
        Log.d(TAG, "Enterprise contacts set. Numbe of enterprise contacts: " + mLocalFavorites.size());
    }

    public int getContactDisplaySelection() {
        return mContactDisplaySelection;
    }

    public void setContactDisplaySelection(int contactTypeSelection) {
        this.mContactDisplaySelection = contactTypeSelection;
    }
}
