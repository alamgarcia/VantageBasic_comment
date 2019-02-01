package com.avaya.android.vantage.basic.views.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.GoogleAnalyticsUtils;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.adaptors.RemoveSearchResultsContactsFragmentInterface;
import com.avaya.android.vantage.basic.csdk.LocalContactInfo;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.ContactsFragment;
import com.avaya.android.vantage.basic.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.model.DirectoryData;
import com.avaya.android.vantage.basic.views.interfaces.IContactsViewInterface;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.signature.StringSignature;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.avaya.android.vantage.basic.Constants.ALL_CONTACTS;
import static com.avaya.android.vantage.basic.Constants.DIRECTORY_CONTACT_PHONE_SEARCH_LOADER_BASE;
import static com.avaya.android.vantage.basic.Constants.DIRECTORY_CONTACT_SEARCH_LOADER_BASE;
import static com.avaya.android.vantage.basic.Constants.DIRECTORY_LOADER;
import static com.avaya.android.vantage.basic.Constants.ENTERPRISE_ONLY;
import static com.avaya.android.vantage.basic.Constants.IPO_ONLY;
import static com.avaya.android.vantage.basic.Constants.LOCAL_ONLY;
import static com.avaya.android.vantage.basic.Constants.SEARCHED_CONTACTS;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ContactData} and makes a call to the
 * specified {@link OnContactInteractionListener}.
 */
public class MyContactsRecyclerViewAdapter extends RecyclerView.Adapter<MyContactsRecyclerViewAdapter.ItemViewHolder> implements IContactsViewInterface, SectionIndexer, LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ContactAdapter";
    private static final String SNIPPET_ARGS_PARAM_KEY = "snippet_args";
    private static final String DEFERRED_SNIPPETING_KEY = "deferred_snippeting";
    private static final char SNIPPET_START_MATCH = '\u0001';
    private static final char SNIPPET_END_MATCH = '\u0001';
    private static final String SNIPPET_ELLIPSIS = "\u2026";
    private static final int SNIPPET_MAX_TOKENS = 5;
    private static final int DIRCTORY_SEARCH_LIMIT = 50;
    private static final String SNIPPET_ARGS = SNIPPET_START_MATCH + ","
            + SNIPPET_END_MATCH + "," + SNIPPET_ELLIPSIS + ","
            + SNIPPET_MAX_TOKENS;
    private static final String BUNDLE_DIRECTORY_ID = "directoryId";
    private static final String BUNDLE_CONTACT_LOOKUP = "dirLookup";
    private static final String BUNDLE_DIRECTORY_SEARCH_TERM = "dirSearchTerm";

    private static final int INTERACTION_DETAILS = 0;
    private static final int INTERACTION_CALL_AUDIO = 1;
    private static final int INTERACTION_CALL_VIDEO = 2;
    private static final int INTERACTION_ADD_PARTICIPANT = 3;

    private static final String[] PROJECTION_PHONE = {ContactsContract.Contacts._ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.CONTACT_PRESENCE, ContactsContract.CommonDataKinds.Phone.CONTACT_STATUS,
            ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER, ContactsContract.CommonDataKinds.Phone.PHOTO_ID, ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_ALTERNATIVE,
            ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE};

    private static final String PBAP_ACCOUNT = "com.android.bluetooth.pbapsink";
    private boolean mFilteringPaired;
    private final OnContactInteractionListener mListener;
    private Map<String, Drawable> mContactPhotoCache = new HashMap<>();
    private boolean mFirstNameFirst = true;
    private LinkedList<ContactData> mAllContactData = new LinkedList<>();
    private LinkedList<ContactData> mLocalContacts = new LinkedList<>();
    private LinkedList<ContactData> mIpoContacts = new LinkedList<>();
    private LinkedList<ContactData> mEnterpriseContacts = new LinkedList<>();
    private List<ContactData> mListOfContactsToShow = new ArrayList<>();
    private LinkedList<ContactData> mSearchList = new LinkedList<>();
    private LinkedList<ContactData> mDirectorySearchList = new LinkedList<>();
    private String mSearchTerm;
    private TextAppearanceSpan mHighlightTextSpan;
    private Activity mActivity;
    private boolean directorySearchLoaderStarted = false;
    private boolean addParticipant = false;
    private int activeTaskID = 0;
    private ArrayList<Integer> mSectionPositions;
    private TextView mEmptyView;
    private List<DirectoryData> mDirectoryList = new ArrayList<>();
    private ArraySet<String> mAlreadyFoundContacts = new ArraySet<>();
    private ArraySet<Integer> mStartedLoaders = new ArraySet<>();
    private ArrayMap<String, ContactData> mDirectoryContactHolder = new ArrayMap<>();
    private ArrayMap<Integer, Boolean> mLoaderHolder = new ArrayMap<>();
    private int mContactDisplaySelection = ContactsFragment.ALL;
    private int mCurrentActiveLoader = 0;
    private ContactData mCurrentDirectoryContact;
    private boolean mBlockClick; // since getting directory contact's phone number is done in the
    // background, we need to be sure we can prevent fast clicks and unwanted change of mCurrentDirectoryContact
    private int mContactInteraction; // when directory contact is tapped, we decide if we want to call or show details


    private RemoveSearchResultsContactsFragmentInterface removeSearchResultsContactsFragmentInterface;

    private Context mContext;
    public MyContactsRecyclerViewAdapter(List<ContactData> allContact, List<ContactData> localContacts,
                                         List<ContactData> enterpriseContacts, List<ContactData> ipoContacts, OnContactInteractionListener listener,
                                         Activity activity, TextView emptyView, boolean filterPaired, Context mContext,
                                         RemoveSearchResultsContactsFragmentInterface removeSearchResultsContactsFragmentInterface) {

        this.mContext = mContext;
        this.mActivity = activity;
        mListener = listener;
        mLocalContacts.addAll(localContacts);
        mEnterpriseContacts.addAll(enterpriseContacts);
        mAllContactData.addAll(allContact);
        mIpoContacts.addAll(ipoContacts);
        this.mEmptyView = emptyView;
        mFilteringPaired = filterPaired;
        prepareCompositeList(allContact, ContactData.Category.ALL);
        mHighlightTextSpan = new TextAppearanceSpan(mActivity, R.style.searchTextHiglight);
        activity.getLoaderManager().restartLoader(Constants.DIRECTORY_LOADER, null, this);
        this.removeSearchResultsContactsFragmentInterface = removeSearchResultsContactsFragmentInterface;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id >= DIRECTORY_CONTACT_SEARCH_LOADER_BASE && id < DIRECTORY_CONTACT_PHONE_SEARCH_LOADER_BASE) {
            mStartedLoaders.add(id);
            CursorLoader loader = null;
            String mSortOrder = ContactsContract.Contacts.SORT_KEY_ALTERNATIVE
                    + " COLLATE LOCALIZED ASC";
            Uri contentsUri;
            contentsUri = ContactsContract.Contacts.CONTENT_FILTER_URI;
            contentsUri = getDirectorySearchUri(contentsUri, args.getString(BUNDLE_DIRECTORY_ID), args.getString(BUNDLE_DIRECTORY_SEARCH_TERM));
            loader = new android.content.CursorLoader(mActivity, contentsUri, PROJECTION_PHONE, "1 == 0", null, null);
            return loader;
        } else if (id == DIRECTORY_LOADER) {
            return new CursorLoader(mActivity, ContactsContract.Directory.CONTENT_URI,
                    null, null, null, ContactsContract.Directory.DISPLAY_NAME);
        } else if (id >= DIRECTORY_CONTACT_PHONE_SEARCH_LOADER_BASE){
            String lookupKey = args.getString(BUNDLE_CONTACT_LOOKUP);
            String idStr = args.getString(BUNDLE_DIRECTORY_ID);
            Uri uri = ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendEncodedPath(lookupKey)
                    .appendPath(ContactsContract.Contacts.Entity.CONTENT_DIRECTORY)
                    .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY, idStr).build();
            CursorLoader loader = new CursorLoader(mActivity);
            loader.setUri(uri);
            loader.setProjection(PhoneQuery.PROJECTION_PRIMARY);
            loader.setSortOrder(ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY);
            return loader;
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() >= DIRECTORY_CONTACT_SEARCH_LOADER_BASE && loader.getId() < DIRECTORY_CONTACT_PHONE_SEARCH_LOADER_BASE) {
            loadDirectoryContacts(cursor, loader.getId());
        } else if (loader.getId() == DIRECTORY_LOADER) {
            getDirectories(cursor);
        } else if (loader.getId() >= DIRECTORY_CONTACT_SEARCH_LOADER_BASE){
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.MIMETYPE));
                    if (mimeType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                        String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        mCurrentDirectoryContact.mPhones.add(new ContactData.PhoneNumber(number, ContactData.PhoneType.WORK, mCurrentDirectoryContact.mPhones.size() == 0, String.valueOf(cursor.getPosition())));
                    }
                }
                switch (mContactInteraction){
                    case INTERACTION_DETAILS:
                        mListener.onContactsFragmentInteraction(mCurrentDirectoryContact);
                        break;
                    case INTERACTION_CALL_AUDIO:
                        mListener.onCallContactAudio(mCurrentDirectoryContact, null);
                        break;
                    case INTERACTION_CALL_VIDEO:
                        mListener.onCallContactVideo(mCurrentDirectoryContact, null);
                        break;
                    case INTERACTION_ADD_PARTICIPANT:
                        mListener.onCallAddParticipant(mCurrentDirectoryContact);
                        setAddParticipant(false);
                        refreshData();
                        break;
                    default:
                        // do  nothing
                        Log.d(TAG, "Contact interaction, unrecognized command");
                }
            } else {
                Log.e(TAG, "Directory contact. No phones found for this one");
            }
            mBlockClick = false;
        }
    }

    /**
     * Disable another click on the list
     */
    public void disableBlockClick() {
        this.mBlockClick = false;
    }

    /**
     * Reload directory list
     */
    public void reloadDirectories(){
        mActivity.getLoaderManager().restartLoader(Constants.DIRECTORY_LOADER, null, this);
    }

    /**
     * Loading directory contacts
     * @param cursor received from loader
     */
    private void loadDirectoryContacts(Cursor cursor, int loaderID) {
        mDirectorySearchList.clear();
        mDirectoryContactHolder.clear();
        int dirListID = loaderID - DIRECTORY_CONTACT_SEARCH_LOADER_BASE;
        boolean isIpoContact = mLoaderHolder.get(loaderID);
        while (cursor != null && cursor.moveToNext()) {
            String firstName = "";
            String lastName = "";
            String name;
            String id;
                name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                id = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY));
                // lookup key starts with phone number and phone number ends with "%" so in order to get phone number,
                // we just get everything before "%"
            String[] parts;

            if (mDirectoryList.size() > dirListID && name!=null && id!=null) {
                if (name.contains(",")) {
                    parts = name.split(",", 2);
                } else {
                    parts = name.split(" ", 2);
                }

                if (parts.length > 1) {
                    firstName = parts[1].trim();
                    lastName = parts[0].trim();
                } else {
                    firstName = name;
                }
                ContactData directoryContact;
                List<ContactData.PhoneNumber> phoneList = new LinkedList<>();
                // IPO stores first and last name differently, so I switch them here

                if (isIpoContact) {
                    directoryContact = new ContactData(
                            name, lastName, firstName, null, false, "", "", "", "", phoneList,
                            ContactData.Category.DIRECTORY, id, "", "", true, "", "", "",
                            mDirectoryList.get(dirListID).directoryName, mDirectoryList.get(dirListID).accountName);
                } else {
                    directoryContact = new ContactData(
                            name, firstName, lastName, null, false, "", "", "", "", phoneList,
                            ContactData.Category.DIRECTORY, id, "", "", true, "", "", "",
                            mDirectoryList.get(dirListID).directoryName, mDirectoryList.get(dirListID).accountName);
                }
                directoryContact.mDirectoryID = String.valueOf(mDirectoryList.get(dirListID).directoryID);
                mDirectoryContactHolder.put(id, directoryContact);
            }
        }

        for (Map.Entry<String, ContactData> entry : mDirectoryContactHolder.entrySet()){
            ContactData contact = entry.getValue();
            mDirectorySearchList.add(contact);
        }

        Collections.sort(mDirectorySearchList, new Comparator<ContactData>() {
            public int compare(ContactData o1, ContactData o2) {
                return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
            }
        });

        if (!mDirectorySearchList.isEmpty()){
            mDirectorySearchList.get(0).setIsHeader(true);
        }

        mSearchList.addAll(mDirectorySearchList);
        loadContactData(SEARCHED_CONTACTS);
        if (mListOfContactsToShow.size() > 0) {
            mEmptyView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
        refreshData();
    }

    /**
     * Loading list of directories
     * @param cursor cursor received from loader
     */
    private void getDirectories(Cursor cursor) {
        mDirectoryList.clear();
            while (cursor != null && cursor.moveToNext()) {
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Directory.DISPLAY_NAME));
                String accountName = cursor.getString(cursor.getColumnIndex(ContactsContract.Directory.ACCOUNT_NAME));
                int id = cursor.getInt(cursor.getColumnIndex(ContactsContract.Directory._ID));
                String uri = Uri.withAppendedPath(ContactsContract.Directory.CONTENT_URI, String.valueOf(id)).toString();
                String type = cursor.getString(cursor.getColumnIndex(ContactsContract.Directory.ACCOUNT_TYPE));
                String packageName = cursor.getString(cursor.getColumnIndex(ContactsContract.Directory.PACKAGE_NAME));

                if (id != ContactsContract.Directory.DEFAULT && id != ContactsContract.Directory.LOCAL_INVISIBLE) {
                    // just making sure we avoid any NPEs
                    if (displayName == null) displayName = "";
                    if (accountName == null) accountName = "";
                    DirectoryData directory = new DirectoryData(displayName, accountName, id, uri, type, packageName);
                    Log.d(TAG, "Adding directory: " + displayName + " account: " + accountName + " id: " + id);
                    mDirectoryList.add(directory);
                } else {
                    Log.d(TAG, "Directory with id: " + id + " and name: " + displayName + " with account name: " + accountName + " not added");
                }
            }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * returns the Uri for directory search query.
     *
     * @param baseUri basic uri we will append with new info
     * @return Uri formatted URI
     */
    private Uri getDirectorySearchUri(Uri baseUri, String directoryId, String searchTerm) {
        baseUri = baseUri
                .buildUpon()
                .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        directoryId).build();
        baseUri = baseUri
                .buildUpon()
                .appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                        String.valueOf(DIRCTORY_SEARCH_LIMIT)).build();
        baseUri = baseUri
                .buildUpon()
                .appendQueryParameter(
                        SNIPPET_ARGS_PARAM_KEY,
                        SNIPPET_ARGS).build();

        baseUri = baseUri
                .buildUpon()
                .appendQueryParameter(
                        DEFERRED_SNIPPETING_KEY, "1")
                .build();

        baseUri = Uri.withAppendedPath(baseUri, searchTerm)
                .buildUpon().build();

        return baseUri;
    }

    /**
     * Prune contacts that are marked as paired by bluetooth, from the given list of contacts
     *
     * @param contacts list of contacts to prune
     */
    private void removePairedContacts(List<ContactData> contacts) {
        for (int i = contacts.size() - 1; i >= 0; i--) {
            if (PBAP_ACCOUNT.equals(contacts.get(i).mAccountType)) {
                contacts.remove(i);
            }
        }
    }

    /**
     * Identifying start of search query
     *
     * @param displayName display name of the contact
     * @return starting position of a search string
     */
    private int indexOfSearchQuery(String displayName) {
        if (!TextUtils.isEmpty(mSearchTerm)) {
            GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.SEARCH_CONTACTS_EVENT);
            return displayName.toLowerCase(Locale.getDefault()).indexOf(
                    mSearchTerm.toLowerCase(Locale.getDefault()));
        }
        return -1;
    }

    /**
     * Preparing list of Enterprise and Local contacts with appropriate headers.
     * Category parameter is used for filtering what type of contacts are added to list.
     *
     * @param items    ContactData to be shown in list
     * @param category {@link ContactData.Category} based on which we are returning specific category
     *                 It can be ALL, LOCAL or ENTERPRISE
     */
    private void prepareCompositeList(List<ContactData> items, ContactData.Category category) {
        mAllContactData.clear();
        if (category == ContactData.Category.ENTERPRISE) {
            mEnterpriseContacts.clear();
            mEnterpriseContacts.addAll(items);
            mAllContactData.addAll(mEnterpriseContacts);
            mAllContactData.addAll(mLocalContacts);
            mAllContactData.addAll(mIpoContacts);
        } else if (category == ContactData.Category.LOCAL) {
            mLocalContacts.clear();
            mLocalContacts.addAll(items);
            mAllContactData.addAll(mEnterpriseContacts);
            mAllContactData.addAll(mLocalContacts);
            mAllContactData.addAll(mIpoContacts);
        } else if (category == ContactData.Category.IPO){
            mIpoContacts.clear();
            mIpoContacts.addAll(items);
            mAllContactData.addAll(mEnterpriseContacts);
            mAllContactData.addAll(mLocalContacts);
            mAllContactData.addAll(mIpoContacts);
        } else {
            mAllContactData.addAll(items);
        }

        if (mFilteringPaired) {
            removePairedContacts(mAllContactData);
        }

        loadContactTypeSelection();
    }

    /**
     * Return the view type of the item at <code>position</code> for the purposes
     * of view recycling.
     * <p>
     * <p>The default implementation of this method returns 0, making the assumption of
     * a single view type for the adapter. Unlike ListView adapters, types need not
     * be contiguous. Consider using id resources to uniquely identify item view types.
     *
     * @param position position to query
     * @return integer value identifying the type of the view needed to represent the item at
     * <code>position</code>. Type codes need not be contiguous.
     */
    @Override
    public int getItemViewType(int position) {
        if (mListOfContactsToShow.get(position).isPendingUpdate()) {
            return R.layout.contact_list_pending_item;
        } else {
            return R.layout.contact_list_item;
        }
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        return new ItemViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * should update the contents of the {ViewHolder # itemView} to reflect the item at
     * the given position.
     * <p>
     * Note that unlike {@link ListView}, RecyclerView will not call this method
     * again if the position of the item changes in the data set unless the item itself is
     * invalidated or the new position cannot be determined. For this reason, you should only
     * use the <code>position</code> parameter while acquiring the related data item inside
     * this method and should not keep a copy of it. If you need the position of an item later
     * on (e.g. in a click listener), use {ViewHolder#getAdapterPosition()} which will
     * have the updated adapter position.
     * <p>
     * Partial bind vs full bind:
     * <p>
     * The payloads parameter is a merge list from {@link #notifyItemChanged(int, Object)} or
     * {@link #notifyItemRangeChanged(int, int, Object)}.  If the payloads list is not empty,
     * the ViewHolder is currently bound to old data and Adapter may run an efficient partial
     * update using the payload info.  If the payload is empty,  Adapter must run a full bind.
     * Adapter should not assume that the payload passed in notify methods will be received by
     * onBindViewHolder().  For example when the view is not attached to the screen, the
     * payload in notifyItemChange() will be simply dropped.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     * @param payloads A non-null list of merged payloads. Can be empty list if requires full
     */
    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position, List<Object> payloads) {
        if (payloads != null && payloads.size() > 0) {
            if (payloads.get(0) instanceof RoundedBitmapDrawable) {
                holder.mPhoto.setBackground((RoundedBitmapDrawable) payloads.get(0));
                holder.mPhoto.setText("");
            }
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {

        if (mListOfContactsToShow == null) {
            return;
        }
        ContactData data = mListOfContactsToShow.get(position);

        String displayName;

        if (holder.mProgress != null) {
            return;
        }

        holder.mItem = data;
        if (data.mCategory == ContactData.Category.IPO) {
            displayName = data.mFirstName;
        } else if (!mFirstNameFirst) {
            displayName = data.mLastName + " " + data.mFirstName;
        } else {
            displayName = data.mFirstName + " " + data.mLastName;
        }

        if (data.isHeader() && mDirectoryList.size() > 0){
            holder.mDirectoryInfo.setText(mActivity.getString(R.string.directory_separator_display, data.mDirectoryName, data.mAccountName));
            holder.mDirectoryInfo.setVisibility(View.VISIBLE);
        } else {
            holder.mDirectoryInfo.setText("");
            holder.mDirectoryInfo.setVisibility(View.GONE);
        }

        // highlighting searched text
        final int startIndex = indexOfSearchQuery(displayName);
        if (startIndex == -1) {
            holder.mName.setText(displayName);
        } else {
            final SpannableString highlightedName = new SpannableString(displayName);
            highlightedName.setSpan(mHighlightTextSpan, startIndex,
                    startIndex + mSearchTerm.length(), 0);
            holder.mName.setText(highlightedName);
        }


        setThumbnails(holder.mPhoto, holder.mItem);
        holder.mFavorite.setVisibility(data.isFavorite() ? View.VISIBLE : View.INVISIBLE);
        holder.mSyncContact.setVisibility(data.mAccountType.equals(PBAP_ACCOUNT) ? View.VISIBLE : View.INVISIBLE);
        holder.mLocation.setText(data.mLocation);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mBlockClick && mListener != null) {
                    if (holder.mItem.mCategory == ContactData.Category.DIRECTORY) {
                        mBlockClick = true;
                    }
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    // using handler to delay showing of a fragment a little bit and displaying ripple effect
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (holder.mItem.mCategory == ContactData.Category.DIRECTORY) {
                                directoryContactInteraction(holder.mItem, INTERACTION_DETAILS);
                            } else {
                                mListener.onContactsFragmentInteraction(holder.mItem);
                            }
                        }
                    }, 100);
                }

            }
        });
        if (!data.mHasPhone) {
            holder.mCallAudio.setAlpha(0.5f);
            holder.mCallVideo.setAlpha(0.5f);
        } else {
            holder.mCallAudio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mContext.getResources().getBoolean(R.bool.is_landscape) == true)
                        removeSearchResultsContactsFragmentInterface.removeSearchResults();
                    if (!mBlockClick && null != mListener) {
                        if (holder.mItem.mCategory == ContactData.Category.DIRECTORY) {
                            directoryContactInteraction(holder.mItem, INTERACTION_CALL_AUDIO);
                            mBlockClick = true;
                        } else {
                            // Notify the active callbacks interface (the activity, if the
                            // fragment is attached to one) that an item has been selected.
                            if (holder.mItem.mCategory == ContactData.Category.LOCAL || holder.mItem.mCategory == ContactData.Category.IPO) {
                                List<ContactData.PhoneNumber> phoneNumbers = LocalContactInfo.getPhoneNumbers(Uri.parse(holder.mItem.mURI), mActivity);
                                ContactData contactItem = holder.mItem.createNew(null, phoneNumbers, "", "", "");
                                mListener.onCallContactAudio(contactItem, null);
                            } else {
                                mListener.onCallContactAudio(holder.mItem, null);
                            }
                            //Call from Contacts main page
                        }
                        GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_CONTACTS_EVENT);

                    }
                }
            });
            if (!Utils.isCameraSupported()) {
                holder.mCallVideo.setVisibility(View.GONE);
            } else {
                if (!SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
                    holder.mCallVideo.setAlpha(0.5f);
                    holder.mCallVideo.setEnabled(false);
                } else {
                    holder.mCallVideo.setEnabled(true);
                    holder.mCallVideo.setAlpha(1f);
                    holder.mCallVideo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(mContext.getResources().getBoolean(R.bool.is_landscape) == true)
                                removeSearchResultsContactsFragmentInterface.removeSearchResults();
                            if (!mBlockClick && mListener != null) {
                                if (holder.mItem.mCategory == ContactData.Category.DIRECTORY) {
                                    directoryContactInteraction(holder.mItem, INTERACTION_CALL_VIDEO);
                                    mBlockClick = true;
                                } else {
                                    // Notify the active callbacks interface (the activity, if the
                                    // fragment is attached to one) that an item has been selected.
                                    if (holder.mItem.mCategory == ContactData.Category.LOCAL || holder.mItem.mCategory == ContactData.Category.IPO) {
                                        List<ContactData.PhoneNumber> phoneNumbers = LocalContactInfo.getPhoneNumbers(Uri.parse(holder.mItem.mURI), mActivity);
                                        ContactData contactItem = holder.mItem.createNew(null, phoneNumbers, "", "", "");
                                        mListener.onCallContactVideo(contactItem, null);
                                    } else {
                                        mListener.onCallContactVideo(holder.mItem, null);
                                    }
                                }
                                //Call from Contacts main page
                                GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_CONTACTS_EVENT);
                            }
                        }
                    });
                }
            }

            holder.mAddParticipant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mContext.getResources().getBoolean(R.bool.is_landscape) == true)
                        removeSearchResultsContactsFragmentInterface.removeSearchResults();
                    if (!mBlockClick && mListener != null) {
                        if (holder.mItem.mCategory == ContactData.Category.DIRECTORY) {
                            directoryContactInteraction(holder.mItem, INTERACTION_ADD_PARTICIPANT);
                            mBlockClick = true;
                        } else {
                            // Notify the active callbacks interface (the activity, if the
                            // fragment is attached to one) that an item has been selected.
                            if (holder.mItem.mCategory == ContactData.Category.LOCAL || holder.mItem.mCategory == ContactData.Category.IPO) {
                                List<ContactData.PhoneNumber> phoneNumbers = LocalContactInfo.getPhoneNumbers(Uri.parse(holder.mItem.mURI), mActivity);
                                ContactData contactItem = holder.mItem.createNew(null, phoneNumbers, "", "", "");
                                mListener.onCallAddParticipant(contactItem);
                            } else {
                                mListener.onCallAddParticipant(holder.mItem);
                            }
                            setAddParticipant(false);
                            refreshData();
                        }

                    }
                }
            });
        }
    }

    /**
     * This method handles click on Directory items
     * @param item ContactData item
     * @param interactionType type of interaction
     */
    private void directoryContactInteraction(ContactData item, int interactionType){
        mContactInteraction = interactionType;
        int baseID = mCurrentActiveLoader + 1;
        mCurrentActiveLoader = baseID;
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_CONTACT_LOOKUP, item.mUUID);
        bundle.putString(BUNDLE_DIRECTORY_ID, item.mDirectoryID);
        mActivity.getLoaderManager().restartLoader(DIRECTORY_CONTACT_PHONE_SEARCH_LOADER_BASE + baseID, bundle, MyContactsRecyclerViewAdapter.this);
        mCurrentDirectoryContact = item;
    }

    /**
     * Adding thumbnail for contact. In case that there is no image associated with provided contact
     * we will create thumbnail from contact name.
     *
     * @param photo       TextView where we have to place thumbnail
     * @param contactData {@link ContactData} of contact for which thumbnail have to be shown
     */
    private void setThumbnails(final TextView photo, ContactData contactData) {
        if (contactData.mPhotoThumbnailURI != null && contactData.mPhotoThumbnailURI.length() > 0) {
            photo.setText("");
            Glide.clear(photo);
            Glide.with(photo.getContext())
                    .load(contactData.mPhotoThumbnailURI)
                    .asBitmap()
                    .signature(new StringSignature(contactData.mPhotoURI))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .into(new ViewTarget<TextView, Bitmap>(photo) {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation anim) {
                            TextView photoView = this.view;
                            if (photoView == null) {
                                return;
                            }
                            // making bitmap roundable
                            RoundedBitmapDrawable circularBitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(photoView.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            photoView.setBackground(circularBitmapDrawable);
                        }
                    });
        } else {
            mContactPhotoCache.remove(contactData.mUUID);
            String name = contactData.mName;
            int colors[] = photo.getResources().getIntArray(R.array.material_colors);
            photo.setBackgroundResource(R.drawable.empty_circle);
            ((GradientDrawable) photo.getBackground().mutate()).setColor(colors[Math.abs(name.hashCode() % colors.length)]);
            String initials;
            String firstNameLetter;
            String lastNameLetter;

            if (contactData.mFirstName != null && contactData.mFirstName.trim().length() > 0) {
                firstNameLetter = String.valueOf(contactData.mFirstName.trim().charAt(0));
            } else {
                firstNameLetter = "";
            }

            if (contactData.mLastName != null && contactData.mLastName.trim().length() > 0) {
                lastNameLetter = String.valueOf(contactData.mLastName.trim().charAt(0));
            } else {
                lastNameLetter = "";
            }

            if (mFirstNameFirst) {
                initials = firstNameLetter + lastNameLetter;
            } else {
                initials = lastNameLetter + firstNameLetter;
            }

            photo.setText(initials.toUpperCase());
        }
    }

    @Override
    public void onViewRecycled(ItemViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.mPhoto != null) {
            Glide.clear(holder.mPhoto);
        }
    }

    @Override
    public int getItemCount() {
        return mListOfContactsToShow.size();
    }


    @Override
    public void recreateData(List<ContactData> items, ContactData.Category contactType) {
        if (contactType.equals(ContactData.Category.ENTERPRISE)) {
            prepareCompositeList(items, ContactData.Category.ENTERPRISE);
        } else if (contactType.equals(ContactData.Category.IPO)){
            prepareCompositeList(items, ContactData.Category.IPO);
        }else if (contactType.equals(ContactData.Category.ALL)) {
            prepareCompositeList(items, ContactData.Category.ALL);
        }else if (contactType.equals(ContactData.Category.LOCAL)){
            prepareCompositeList(items, ContactData.Category.LOCAL);
        }else if (contactType.equals(ContactData.Category.PAIRED)){
            prepareCompositeList(items, ContactData.Category.PAIRED);
        }else if (contactType.equals(ContactData.Category.DIRECTORY)){
            prepareCompositeList(items, ContactData.Category.DIRECTORY);
        }
    }

    @Override
    public int getIndexOf(ContactData item) {
        return mListOfContactsToShow.indexOf(item);
    }

    @Override
    public void updateItem(ContactData item) {
    }

    @Override
    public void removeItem(int position) {
    }

    @Override
    public void removeItem(ContactData contact) {
    }


    @Override
    public void addItem(ContactData item) {
    }

    @Override
    public void cacheContactDrawable(String mUUID, RoundedBitmapDrawable circularBitmapDrawable) {
        mContactPhotoCache.put(mUUID, circularBitmapDrawable);
    }

    @Override
    public boolean isPhotoCached(String uuid) {
        return mContactPhotoCache.containsKey(uuid);
    }

    /**
     * Managing contact display using filters (Enterprise, Local, Search or All)
     *
     * @param contactsToDisplay contact group to display
     */
    public void loadContactData(int contactsToDisplay) {
        mListOfContactsToShow.clear();
        if(contactsToDisplay == ALL_CONTACTS) {
            Set<ContactData> hs = new HashSet<>();
            if (mAllContactData.size() > 0) {
                // mListOfContactsToShow.addAll(mAllContactData);
                hs.addAll(mAllContactData);
            }
            if (mLocalContacts.size() > 0) {
                //mListOfContactsToShow.addAll(mLocalContacts);
                hs.addAll(mLocalContacts);
            }
            if (mEnterpriseContacts.size() > 0) {
                //mListOfContactsToShow.addAll(mEnterpriseContacts);
                hs.addAll(mEnterpriseContacts);
            }
            if (mIpoContacts.size() > 0) {
                //mListOfContactsToShow.addAll(mIpoContacts);
                hs.addAll(mIpoContacts);
            }
            mListOfContactsToShow.addAll(hs);
        }else  if(contactsToDisplay == LOCAL_ONLY) {
            Set<ContactData> hs = new HashSet<>();

            if (mLocalContacts.size() > 0) {
                //mListOfContactsToShow.addAll(mLocalContacts);
                hs.addAll(mLocalContacts);
            }
            mListOfContactsToShow.addAll(hs);
        }else if(contactsToDisplay == ENTERPRISE_ONLY) {
            Set<ContactData> hs = new HashSet<>();

            if (mEnterpriseContacts.size() > 0) {
                //mListOfContactsToShow.addAll(mLocalContacts);
                hs.addAll(mEnterpriseContacts);
            }
            mListOfContactsToShow.addAll(hs);
        }else if(contactsToDisplay == IPO_ONLY) {
            Set<ContactData> hs = new HashSet<>();

            if (mIpoContacts.size() > 0) {
                //mListOfContactsToShow.addAll(mLocalContacts);
                hs.addAll(mIpoContacts);
            }
            mListOfContactsToShow.addAll(hs);

        } else if(contactsToDisplay == SEARCHED_CONTACTS) {
            mListOfContactsToShow.addAll(mSearchList);
        }

        if(mFilteringPaired) {
            removePairedContacts(mListOfContactsToShow);
        }

        if (contactsToDisplay != SEARCHED_CONTACTS) {
            Collections.sort(mListOfContactsToShow, new Comparator<ContactData>() {
                public int compare(ContactData o1, ContactData o2) {
                    return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
                }
            });
        }

        // hiding "No contacts to display" text in case we have some dat ain the list
        if (mListOfContactsToShow.size() > 0) {
            if (mEmptyView != null) {
                mEmptyView.setVisibility(View.INVISIBLE);
            }
        } else {
            if (mEmptyView != null) {
                mEmptyView.setVisibility(View.VISIBLE);
            }
        }
        refreshData();
    }

    private void refreshData(){
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.checkFilterVisibility();
        }
    }

    /**
     * Show only contacts that user previously selected (enterprise, all or local)
     */
    private void loadContactTypeSelection() {
        switch (mContactDisplaySelection){
            case ContactsFragment.LOCAL:
                loadContactData(LOCAL_ONLY);
                break;
            case ContactsFragment.ENTERPRISE:
                loadContactData(ENTERPRISE_ONLY);
                break;
            case ContactsFragment.IPO:
                loadContactData(IPO_ONLY);
                break;
            default:
                loadContactData(ALL_CONTACTS);
                break;
        }
    }

    public void setContactDisplaySelection(int contactDisplaySelection) {
        this.mContactDisplaySelection = contactDisplaySelection;
    }

    /**
     * Method used to search contacts
     *
     * @param text search query
     */
    public void searchFilter(String text, boolean numbersOnly) {
        activeTaskID = 0;
        this.mSearchTerm = text;
        if (TextUtils.isEmpty(text)) {
            loadContactTypeSelection();
        } else {
            activeTaskID++;
            new AsyncSearch(
                    mActivity,
                    text.toLowerCase(),
                    activeTaskID,
                    numbersOnly,
                    mFirstNameFirst,
                    mAlreadyFoundContacts,
                    mAllContactData,
                    mEnterpriseContacts).execute();
        }
    }

    /**
     * Method used to query directory contact search
     * @param searchQuery search query
     */
    private void searchDirectoryContacts(String searchQuery){
        destroyAllLoaders();
        for (int i = 0; i < mDirectoryList.size(); i++) {
            String dirID = String.valueOf(mDirectoryList.get(i).directoryID);
            boolean isIpo = mDirectoryList.get(i).accountName.contains("IPOffice");
            Bundle bundle = new Bundle();
            bundle.putString(BUNDLE_DIRECTORY_ID, dirID);
            bundle.putString(BUNDLE_DIRECTORY_SEARCH_TERM, searchQuery);
            int loaderId = DIRECTORY_CONTACT_SEARCH_LOADER_BASE + i;
            mLoaderHolder.put(loaderId, isIpo);

            if (dirID.trim().length() > 0 && searchQuery.trim().length() > 1) {
                if (!directorySearchLoaderStarted) {
                    mActivity.getLoaderManager().initLoader(loaderId, bundle, this);
                    directorySearchLoaderStarted = true;
                } else {
                    mActivity.getLoaderManager().restartLoader(loaderId, bundle, this);
                }
            }
        }
    }

    /**
     * Since we started new loader, there is no need for others to continue their work.
     * Using this method we kill all previously started loaders and clear list of started loaders
     */
    private void destroyAllLoaders() {
        for (Integer id : mStartedLoaders){
            Loader<?> loader = mActivity.getLoaderManager().getLoader(id);
            if (loader instanceof CursorLoader) {
                ((CursorLoader) loader).cancelLoadInBackground();
                mActivity.getLoaderManager().destroyLoader(id);
                Log.d(TAG, "Destroying and canceling loader with ID: " + id);
            } else {
                mActivity.getLoaderManager().destroyLoader(id);
                Log.d(TAG, "Destroying loader with ID: " + id);
            }
        }
        mStartedLoaders.clear();
    }

    /**
     * Set mFirstNameFirst boolean which represent do we set on first place
     * last name or first name when we are showing full name.
     *
     * @param firstNameFirst boolean
     */
    public void firstNameFirst(boolean firstNameFirst) {
        mFirstNameFirst = firstNameFirst;
    }

    @Override
    public Object[] getSections() {
        List<String> sections = new ArrayList<>(26);
        mSectionPositions = new ArrayList<>(26);
        for (int i = 0, size = mListOfContactsToShow.size(); i < size; i++) {
            String section = String.valueOf(mListOfContactsToShow.get(i).mName.charAt(0)).toUpperCase();
            if (!sections.contains(section)) {
                sections.add(section);
                mSectionPositions.add(i);
            }
        }
        return sections.toArray(new String[0]);
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (mSectionPositions.size() > 0) {
            return mSectionPositions.get(sectionIndex);
        } else {
            return 0;
        }
    }

    @Override
    public int getSectionForPosition(int position) {
        return 0;
    }

    /**
     * ViewHolder for recycler view adapter
     */
    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView mName;
        private final TextView mLocation;
        private final ImageView mFavorite;
        private final ImageView mSyncContact;
        private final TextView mPhoto;
        private final ImageView mCallAudio;
        private final ImageView mCallVideo;
        private final ProgressBar mProgress;
        private ContactData mItem;
        private final ImageView mAddParticipant;
        private final TextView mDirectoryInfo;

        private ItemViewHolder(View view) {
            super(view);
            mView = view;
            mName = (TextView) view.findViewById(R.id.contact_name);
            mLocation = (TextView) view.findViewById(R.id.contact_location);
            mFavorite = (ImageView) view.findViewById(R.id.contact_is_favorite);
            mPhoto = (TextView) view.findViewById(R.id.initials);
            mCallAudio = (ImageView) view.findViewById(R.id.call_audio);
            mCallVideo = (ImageView) view.findViewById(R.id.call_video);
            mAddParticipant = (ImageView) view.findViewById(R.id.add_participant);
            mDirectoryInfo = (TextView) view.findViewById(R.id.directory_info);
            mSyncContact = (ImageView) view.findViewById(R.id.contact_is_sync);
            if (addParticipant) {
                android.view.ViewGroup.LayoutParams params = mLocation.getLayoutParams();
                params.width = 176;
                mLocation.setLayoutParams(params);
                mCallAudio.setVisibility(View.INVISIBLE);
                mCallVideo.setVisibility(View.INVISIBLE);
                mAddParticipant.setVisibility(View.VISIBLE);
            }

            if(mContext.getResources().getBoolean(R.bool.is_landscape) == true){
                android.view.ViewGroup.LayoutParams params = mLocation.getLayoutParams();
                params.width = 332;
                mLocation.setLayoutParams(params);
            }
            mProgress = (ProgressBar) view.findViewById(R.id.progressBar);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mName.getText() + "'";
        }
    }

    /**
     * Setting up is view open for adding participant from join conference call
     *
     * @param add are we adding new participant
     */
    public void setAddParticipant(boolean add) {
        addParticipant = add;
    }

    private void populateSearchedContacts(
            List<ContactData> foundLocalsFromPhoneSearch,
            List<ContactData> foundLocalsFromNameSearch,
            List<ContactData> foundEnterprise, String searchQuery,
            int taskID){
        if (mSearchList != null && taskID >= activeTaskID) {
            mSearchList.clear();
            mSearchList.addAll(foundLocalsFromNameSearch);
            mSearchList.addAll(foundLocalsFromPhoneSearch);
            mSearchList.addAll(foundEnterprise);
            loadContactData(SEARCHED_CONTACTS);
            searchDirectoryContacts(searchQuery);
        } else if (taskID < activeTaskID) {
            Log.d(TAG, "Newer task has been run, no need to update lists" + "\n" +
                    "This task ID " + taskID + " - New Task ID: " + activeTaskID);
        }
    }

    /**
     * class  used to display search results asynchronously
     */
    private class AsyncSearch extends AsyncTask<Void, Void, Void> {
        private String text;
        private List<ContactData> foundLocalsFromPhoneSearch = new ArrayList<>();
        private List<ContactData> foundLocalsFromNameSearch = new ArrayList<>();
        private List<ContactData> foundEnterprise = new ArrayList<>();
        private WeakReference<Context> weakContext;
        private int taskID = 0;
        private boolean numbersOnly = false;
        private boolean maFirstNameFirst;
        private ArraySet<String> maAlreadyFoundContacts;
        private LinkedList<ContactData> maAllContactData;
        private LinkedList<ContactData> maEnterpriseContacts;

        private AsyncSearch(
                Context context,
                String searchQuery,
                int taskID,
                boolean numbersOnly,
                boolean firstNameFirst,
                ArraySet<String> alreadyFoundContacts,
                LinkedList<ContactData> allContactData,
                LinkedList<ContactData> enterpriseContacts) {
            weakContext = new WeakReference<>(context);
            this.text = searchQuery;
            this.taskID = taskID;
            this.numbersOnly = numbersOnly;
            this.maFirstNameFirst = firstNameFirst;
            this.maAlreadyFoundContacts = alreadyFoundContacts;
            this.maAllContactData = allContactData;
            this.maEnterpriseContacts = enterpriseContacts;
        }

        @Override
        protected Void doInBackground(Void... params) {
            maAlreadyFoundContacts.clear();
            // searching local contact list
            for (int i = 0; i < maAllContactData.size(); i++) {
                // using this if statement in case user has name display method ad FNF, but sort at LNF and vice versa
                if (maFirstNameFirst) {
                    String name = maAllContactData.get(i).mFirstName + " " + maAllContactData.get(i).mLastName;
                    if (name.toLowerCase().contains(text) && !maAlreadyFoundContacts.contains(maAllContactData.get(i).mUUID)) {
                        foundLocalsFromNameSearch.add(maAllContactData.get(i));
                        maAlreadyFoundContacts.add(maAllContactData.get(i).mUUID);
                    }
                } else {
                    String name = maAllContactData.get(i).mLastName + " " + maAllContactData.get(i).mFirstName;
                    if (name.toLowerCase().contains(text) && !maAlreadyFoundContacts.contains(maAllContactData.get(i).mUUID)) {
                        foundLocalsFromNameSearch.add(maAllContactData.get(i));
                        maAlreadyFoundContacts.add(maAllContactData.get(i).mUUID);
                    }
                }
            }

            // searching by phone numbers in case user entered only numbers
            if (numbersOnly) {
                foundLocalsFromPhoneSearch.addAll(LocalContactInfo.search(text, weakContext.get(), maAlreadyFoundContacts));
                //searching enterprise contacts TODO need to find better way to search enterprise contacts
                for (int i = 0; i < maEnterpriseContacts.size(); i++) {
                    for (int j = 0; j < maEnterpriseContacts.get(i).mPhones.size(); j++) {
                        if (maEnterpriseContacts.get(i).mPhones.get(j).Number.contains(text)) {
                            foundEnterprise.add(maEnterpriseContacts.get(i));
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (weakContext != null && weakContext.get() != null) {
                populateSearchedContacts(foundLocalsFromPhoneSearch, foundLocalsFromNameSearch, foundEnterprise, text, taskID);
            }
        }
    }

    public boolean ismFilteringPaired() {
        return mFilteringPaired;
    }

    public void setmFilteringPaired(boolean mFilteringPaired) {
        this.mFilteringPaired = mFilteringPaired;
    }

    protected static class PhoneQuery {
        private static final String[] PROJECTION_PRIMARY = new String[] {
                ContactsContract.CommonDataKinds.Phone._ID,                          // 0
                ContactsContract.CommonDataKinds.Phone.TYPE,                         // 1
                ContactsContract.CommonDataKinds.Phone.LABEL,                        // 2
                ContactsContract.CommonDataKinds.Phone.NUMBER,                       // 3
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,                   // 4
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,                   // 5
                ContactsContract.CommonDataKinds.Phone.PHOTO_ID,                     // 6
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,         // 7
                ContactsContract.CommonDataKinds.Phone.MIMETYPE,                     // 8
        };

        private static final String[] PROJECTION_ALTERNATIVE = new String[] {
                ContactsContract.CommonDataKinds.Phone._ID,                          // 0
                ContactsContract.CommonDataKinds.Phone.TYPE,                         // 1
                ContactsContract.CommonDataKinds.Phone.LABEL,                        // 2
                ContactsContract.CommonDataKinds.Phone.NUMBER,                       // 3
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,                   // 4
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,                   // 5
                ContactsContract.CommonDataKinds.Phone.PHOTO_ID,                     // 6
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_ALTERNATIVE,     // 7
                ContactsContract.CommonDataKinds.Phone.MIMETYPE,                     // 8
        };

        public static final int PHONE_ID           = 0;
        public static final int PHONE_TYPE         = 1;
        public static final int PHONE_LABEL        = 2;
        public static final int PHONE_NUMBER       = 3;
        public static final int PHONE_CONTACT_ID   = 4;
        public static final int PHONE_LOOKUP_KEY   = 5;
        public static final int PHONE_PHOTO_ID     = 6;
        public static final int PHONE_DISPLAY_NAME = 7;
    }
}
