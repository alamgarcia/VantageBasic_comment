package com.avaya.android.vantage.basic.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.PhotoLoadUtility;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor;
import com.avaya.android.vantage.basic.csdk.ConfigParametersNames;
import com.avaya.android.vantage.basic.csdk.EnterpriseContactManager;
import com.avaya.android.vantage.basic.csdk.LocalContactInfo;
import com.avaya.android.vantage.basic.csdk.LocalContactsManager;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.views.adapters.ContactDetailsPhoneListAdapter;
import com.avaya.android.vantage.basic.views.interfaces.IContactDetailesViewInterface;
import com.avaya.clientservices.contact.EditableContact;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static com.avaya.android.vantage.basic.activities.MainActivity.ACTIVE_CALL_FRAGMENT;
import static com.avaya.android.vantage.basic.activities.MainActivity.ACTIVE_VIDEO_CALL_FRAGMENT;
import static com.avaya.android.vantage.basic.activities.MainActivity.CONTACTS_EDIT_FRAGMENT;
import static com.avaya.android.vantage.basic.activities.MainActivity.CONTACTS_FRAGMENT;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_IPOFFICE;

/**
 * Contact Details Fragment which show to user all relevant data regarding selected contact
 */
public class ContactDetailsFragment extends Fragment implements IContactDetailesViewInterface, View.OnClickListener, EnterpriseContactManager.EnterpriseContactListener {

    private static final String TAG = "ContactDetailsFragment";

    private static final int imageWidth = 105;
    private static final int imageHeigth = 105;
    private static final String NAME_DISPLAY_PREFERENCE = Constants.NAME_DISPLAY_PREFERENCE;
    private static final String USER_PREFERENCE = Constants.USER_PREFERENCE;
    private static final int FIRST_NAME_FIRST = Constants.FIRST_NAME_FIRST;
    private static final int LAST_NAME_FIRST = Constants.LAST_NAME_FIRST;
    private String ADMIN_NAME_DISPLAY_ORDER = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_DISPLAY_ORDER);

    private TextView mContactNameText;
    private TextView mContactImage;
    private TextView mBackText;
    private TextView mCompanyText;
    private TextView mContactType;
    private TextView mContactLocation;
    private TextView mContactPosition;
    private ImageView mFavoriteImage;
    private TextView mContactEdit;
    private TextView mContactDelete;
    private ListView mContactPhoneList;
    public LinearLayout mDeletePopUp;
    private TextView mDeleteConfirme;
    private TextView mDeleteCancel;
    private ContactViewAdaptorInterface mAdapterInstance;
    public OnContactDetailsInteractionListener mBackListener;
    private OnContactInteractionListener mListener;
    private ContactDetailsPhoneListAdapter mAdapter;
    private String mFirstName;
    private String mLastName;
    private LocalContactsManager mLocalContactsManager;
    private Uri mContactUri;
    private boolean isNewContact;
    private boolean searchOnce = false;
    private boolean mIpoEnabled;
    private String enableContactEdit = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.ENABLE_MODIFY_CONTACTS);

    private Map<String, Drawable> mContactPhotoCache = new HashMap<>();

    private EnterpriseContactManager mEnterpriseContactManager;
    private EditableContact mEditableEnterpriseContact;

    private ContactData mContactData;
    private SharedPreferences mUserPreference;

    private LinearLayout nameInfo;
    private ImageView openCloseNameInfo;
    private boolean isContactInfo = false;
    private String isFromLandSearch = "0";
    public boolean isBackORDeletePressed = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (for example upon screen orientation changes).
     */
    public ContactDetailsFragment() {
    }


    public static ContactDetailsFragment newInstance(ContactData contactData) {
        ContactDetailsFragment fragment = new ContactDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(Constants.CONTACT_DATA, contactData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_contact_details, container, false);
        mBackText = (TextView) root.findViewById(R.id.contact_details_back);
        mContactImage = (TextView) root.findViewById(R.id.contact_details_contact_image);
        mFavoriteImage = (ImageView) root.findViewById(R.id.contact_details_contact_favorite);
        mContactPhoneList = (ListView) root.findViewById(R.id.contact_details_phone_list);
        mCompanyText = (TextView) root.findViewById(R.id.contact_details_contact_company);
        mContactEdit = (TextView) root.findViewById(R.id.contact_details_edit);
        mContactDelete = (TextView) root.findViewById(R.id.contact_details_delete);
        mUserPreference = getActivity().getSharedPreferences(USER_PREFERENCE, MODE_PRIVATE);
        mContactType = (TextView) root.findViewById(R.id.contact_type);
        mContactData = getArguments().getParcelable(Constants.CONTACT_DATA);
        mContactNameText = (TextView) root.findViewById(R.id.contact_details_contact_name);
        mContactLocation = (TextView) root.findViewById(R.id.contact_details_contact_location);
        mContactPosition = (TextView) root.findViewById(R.id.contact_details_contact_position);
        mDeletePopUp = (LinearLayout) root.findViewById(R.id.contact_delete_confirmation);
        mDeleteConfirme = (TextView) root.findViewById(R.id.contact_delete_yes);
        mDeleteCancel = (TextView) root.findViewById(R.id.contact_delete_no);


        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            nameInfo = (LinearLayout) root.findViewById(R.id.name_info);
            openCloseNameInfo = (ImageView) root.findViewById(R.id.open_close_name_info);
            openCloseNameInfo.setImageResource(R.drawable.ic_expand_more);
            openCloseNameInfo.setOnClickListener(this);

        }


        mIpoEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ENABLE_IPOFFICE);

        setupContactData();

        if( getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null)
            ((MainActivity)getActivity()).changeUiForFullScreenInLandscape(true);

        return root;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
            if ( (!((MainActivity) getActivity()).isFragmentVisible(ACTIVE_CALL_FRAGMENT)  || ((MainActivity) getActivity()).isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT) ) && isBackORDeletePressed == false) {
                ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(true);
            }else {
                ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(false);
            }
        }
    }


    /**
     * Fill up data that is presented to the user.
     */
    private void setupContactData() {
        if (mContactData != null && getContext() != null) {
            mFirstName = mContactData.mFirstName;
            mLastName = (mContactData.mCategory == ContactData.Category.IPO) ? "" :mContactData.mLastName;

            //preventing crash in case mURI is null
            if (mContactData.mURI != null) {
                mContactUri = Uri.parse(mContactData.mURI);
            }

            //preventing crash in case mPhoto is null and setting up contact picture
            if (mContactData.mPhotoURI != null && mContactData.mPhotoURI.trim().length() > 0) {
                Uri mPhotoURI = Uri.parse(mContactData.mPhotoURI);
                setContactPhoto(mPhotoURI);
            } else {
                setInitials(mContactImage, mContactData);
            }

            // if local contact is loaded, get all the additional info from the local contact manager
            if ((mContactData.mCategory == ContactData.Category.LOCAL || mContactData.mCategory == ContactData.Category.IPO) && mContactUri != null && mContactUri.toString().trim().length() > 0) {
                String location = "";
                String position = "";
                String company = "";
                List<ContactData.PhoneNumber> mPhoneNumbers = LocalContactInfo.getPhoneNumbers(mContactUri, getContext());
                String contactID = LocalContactInfo.getContactID(mContactUri, getContext());
                mContactData.setmIsFavorite(LocalContactInfo.getFavoriteStatus(mContactUri, getContext()));
                if (!TextUtils.isEmpty(contactID)) {
                    String[] companyInfo = LocalContactInfo.getCompanyInfo(contactID, getContext());
                    mCompanyText.setText(companyInfo[0]);
                    if (mContactData.mCategory == ContactData.Category.IPO){
                        mContactType.setText(R.string.display_personal_directory_contact_information);
                    } else {
                        mContactType.setText(R.string.display_local_contact_information);
                    }
                    mContactPosition.setText(companyInfo[1]);
                    location = LocalContactInfo.getContactAddress(LocalContactInfo.getContactID(mContactUri, getContext()), getContext());
                    if (location != null && location.trim().length() > 0) {
                        mContactLocation.setText(location);
                        mContactLocation.setVisibility(View.VISIBLE);
                    }
                    company = companyInfo[0];
                    position = companyInfo[1];
                }

                mContactData = new ContactData(
                        mContactData.mName,
                        mContactData.mFirstName,
                        mContactData.mLastName,
                        null,
                        mContactData.isFavorite(),
                        location,
                        mContactData.mCity,
                        position,
                        company,
                        mPhoneNumbers,
                        mContactData.mCategory,
                        mContactData.mUUID,
                        mContactData.mURI,
                        mContactData.mPhotoThumbnailURI,
                        mContactData.mHasPhone,
                        mContactData.mEmail,
                        mContactData.mPhotoURI,
                        mContactData.mAccountType,
                        "", "");
            } else if (mContactData.mCategory == ContactData.Category.ENTERPRISE) {
                if (mContactData.mPhones != null && mContactData.mPhones.size() > 0 && mContactData.mPhones.get(0).Number != null) {
                    findEnterpriseEditableContact();
                }
                mContactType.setText(R.string.display_enterprise_contact_information);
            } else if (mContactData.mCategory == ContactData.Category.DIRECTORY){
                mContactType.setText(R.string.display_directory_contact_information);
            } else {
                mContactType.setText("");
                if (mContactData.mPhones != null && mContactData.mPhones.size() > 0) {
                    searchForLocalEnterpriseContact(mContactData.mPhones.get(0).Number);
                }
            }


            mBackText.setOnClickListener(this);
            mFavoriteImage.setOnClickListener(this);
            mContactEdit.setOnClickListener(this);
            mContactDelete.setOnClickListener(this);
            mDeleteConfirme.setOnClickListener(this);
            mDeleteCancel.setOnClickListener(this);

            if (!mContactData.mCompany.equals("")) {
                mCompanyText.setVisibility(View.VISIBLE);
                mCompanyText.setText(mContactData.mCompany);
            }
            if (!mContactData.mPosition.equals("")) {
                mContactPosition.setVisibility(View.VISIBLE);
                mContactPosition.setText(mContactData.mPosition);
            }
            if (!mContactData.mLocation.equals("")) {
                mContactLocation.setVisibility(View.VISIBLE);
                mContactLocation.setText(mContactData.mLocation);
            }

            if (mContactData.mPhones != null) {
                mAdapter = new ContactDetailsPhoneListAdapter(getContext(), mContactData, mListener);
                mContactPhoneList.setAdapter(mAdapter);
            }

            mLocalContactsManager = new LocalContactsManager(getActivity());

            if (enableContactEdit != null && enableContactEdit.equals("0")) {
                mContactEdit.setVisibility(View.INVISIBLE);
            } else {
                mContactEdit.setVisibility(View.VISIBLE);
            }
            if (mContactData.mCategory == ContactData.Category.IPO){
                mFavoriteImage.setVisibility(View.INVISIBLE);
            } else {
                mFavoriteImage.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Search for enterprise contact related to user.
     *
     * @param phoneNumber Search query data. Phone number is used to compared current contact with
     *                    enterprise contacts saved in user's contacts.
     */
    private void searchForLocalEnterpriseContact(String phoneNumber) {
        if (mAdapterInstance != null && !searchOnce) {
            searchOnce = true; // security flag used to prevent stack overflow with future changes.
            UIContactsViewAdaptor mUIContactsAdaptor = mAdapterInstance.getContactViewAdapter();
            if (mUIContactsAdaptor != null) {
                ContactData localEnterpriseContact = mUIContactsAdaptor.searchForEnterpriseContact(phoneNumber);
                if (!TextUtils.isEmpty(localEnterpriseContact.mName)
                        && localEnterpriseContact.mCategory == ContactData.Category.ENTERPRISE) {
                    mContactData = localEnterpriseContact;
                    setupContactData();
                }
            }
        }
    }

    /**
     * Get EditableContact from CSDK since ContactData is immutable
     */
    private void findEnterpriseEditableContact() {
        Log.d(TAG, "Contact without category presented. Check if its enterprise");
        if (mEnterpriseContactManager == null) {
            mEnterpriseContactManager = new EnterpriseContactManager(this);
        }
        if (!TextUtils.isEmpty(mContactData.mEmail)) {
            mEnterpriseContactManager.findEnterpriseContact(mContactData.mEmail, false);
        } else {
            mEnterpriseContactManager.findEnterpriseContact(mContactData.mPhones.get(0).Number, false);
        }
    }

    private void deleteContact() {
        if(mContactData.mCategory == ContactData.Category.ENTERPRISE) {
            Log.d(TAG, "Contact without category presented. Check if its enterprise");
            if (mEnterpriseContactManager == null) {
                mEnterpriseContactManager = new EnterpriseContactManager(this);
            }
            if (!TextUtils.isEmpty(mContactData.mEmail)) {
                mEnterpriseContactManager.findEnterpriseContact(mContactData.mEmail, true);
            } else {
                mEnterpriseContactManager.findEnterpriseContact(mContactData.mPhones.get(0).Number, true);
            }
        } else if (mContactData.mCategory == ContactData.Category.LOCAL || mContactData.mCategory == ContactData.Category.IPO){
            mLocalContactsManager.deleteLocalContact(mContactData.mUUID);
        } else {
            Utils.sendSnackBarData(getActivity(), getString(R.string.contact_uneditable_error), true);
        }

        mBackListener.back();
    }

    /**
     * Displaying contact photo
     *
     * @param photoURI contact  URI
     */
    private void setContactPhoto(Uri photoURI) {
        try {
            Bitmap uriImage = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), photoURI);
            RoundedBitmapDrawable contactPhoto =
                    RoundedBitmapDrawableFactory.create(getResources(), uriImage);
            contactPhoto.setCircular(true);
            mContactImage.setBackground(contactPhoto);
        } catch (IOException e) {
            e.printStackTrace();
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

        int nameDisplay = mUserPreference.getInt(NAME_DISPLAY_PREFERENCE, defaultValue);
        String mDisplayName;
        if (nameDisplay == LAST_NAME_FIRST) {
            mDisplayName = mLastName + " " + mFirstName;
        } else {
            mDisplayName = mFirstName + " " + mLastName;
        }
        mContactNameText.setText(mDisplayName);

        // if this is non existing contact, we will change "edit" text to "Create contact". Also hiding favorites icon
        if (TextUtils.isEmpty(mContactNameText.getText().toString().trim())
                || mContactData.mCategory == ContactData.Category.ALL
                || mContactData.mCategory == ContactData.Category.DIRECTORY
                || (mContactData.mPhones != null && mContactData.mPhones.size() == 0)) {
            Log.d(TAG, "Contact doesn't exist. Create new contact.");
            mContactEdit.setText(R.string.contact_add_label);
            mContactEdit.setContentDescription(getString(R.string.contact_add_label));

            if(getResources().getBoolean(R.bool.is_landscape) == true) {
                mContactDelete.setVisibility(View.GONE);
            }else{
                mContactDelete.setVisibility(View.INVISIBLE);
            }

            mFavoriteImage.setVisibility(View.INVISIBLE);
            isNewContact = true;
        } else {
            mContactEdit.setText(getString(R.string.contact_details_edit));
            mContactEdit.setContentDescription(getString(R.string.contact_details_edit));

            if(getResources().getBoolean(R.bool.is_landscape) == true) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mContactEdit.getLayoutParams();
                params.setMargins(0, 0, 200, 0); //substitute parameters for left, top, right, bottom
                mContactEdit.setLayoutParams(params);
            }

            mContactDelete.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Update on stat of favorite based on click
     */
    private void updateFavorites() {
        if (mContactData != null) {
            mContactData.setmIsFavorite(!mContactData.isFavorite());
            if (mContactData.mCategory == ContactData.Category.LOCAL && mContactUri.toString().length() > 1) {
                String id = getContactID(mContactUri);
                if (id != null) {
                    mLocalContactsManager.setAsFavorite(getContactID(mContactUri), mContactData.isFavorite());
                } else {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_not_found), Utils.SNACKBAR_LONG);
                    mBackListener.back();
                    return;
                }
            } else {
                if (mEditableEnterpriseContact != null) {
                    SDKManager.getInstance().getContactsAdaptor().startEnterpriseEditing(mContactData, mEditableEnterpriseContact, false);
                } else {
                    Log.e(TAG, "updateFavorites not possible. EditableContact missing");
                    findEnterpriseEditableContact();
                }
            }
            setFavoriteColor();
        }
    }

    /**
     * Set color of favorite star icon properly
     */
    private void setFavoriteColor() {
        if (mFavoriteImage != null && ElanApplication.getContext() != null) {
            if (mContactData.isFavorite()) {
                mFavoriteImage.setColorFilter(ElanApplication.getContext().getColor(R.color.colorAccent));
                mFavoriteImage.setImageDrawable(ElanApplication.getContext().getDrawable(R.drawable.ic_favorite_selected48));
            } else {
                mFavoriteImage.setColorFilter(ElanApplication.getContext().getColor(R.color.colorPrimary));
                mFavoriteImage.setImageDrawable(ElanApplication.getContext().getDrawable(R.drawable.ic_favorites_selected));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setFavoriteColor();
        nameDisplaySet();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnContactDetailsInteractionListener) {
            mBackListener = (OnContactDetailsInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnContactDetailsInteractionListener");
        }
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
        mBackListener = null;
        mListener = null;
        mAdapterInstance = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.contact_details_back:
                isBackORDeletePressed = true;
                mDeletePopUp.setVisibility(View.GONE);
                mBackListener.back();
                break;
            case R.id.contact_details_contact_favorite:
                mDeletePopUp.setVisibility(View.GONE);
                updateFavorites();
                break;
            case R.id.contact_details_edit:
                mDeletePopUp.setVisibility(View.GONE);
                mBackListener.edit(mContactData, isNewContact);
                break;
            case R.id.contact_details_delete:
                showDeleteConfirmation();
                break;
            case R.id.contact_delete_yes:
                isBackORDeletePressed = true;
                deleteContact();
                break;
            case R.id.contact_delete_no:
                mDeletePopUp.setVisibility(View.GONE);
                break;
            case R.id.open_close_name_info:

               if(isContactInfo == false){
                   openContactInfo();
               }else{
                   closeContactInfo();
               }
            default:
        }
    }

    /**
     * Changes UI state to show contact name info
     */
    private void openContactInfo(){
        isContactInfo = true;
        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) nameInfo.getLayoutParams();
        params.height = 300;
        nameInfo.setLayoutParams(params);
        openCloseNameInfo.setImageResource(R.drawable.ic_expand_less);
    }

    /**
     * Changes UI state to hide contact name info
     */
    private void closeContactInfo(){
        isContactInfo = false;
        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) nameInfo.getLayoutParams();
        params.height = 120;
        nameInfo.setLayoutParams(params);
        openCloseNameInfo.setImageResource(R.drawable.ic_expand_more);
    }

    /**
     * Show Delete Popup menu
     */
    private void showDeleteConfirmation() {
        mDeletePopUp.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBackListener = null;
    }

    @Override
    public void retrieveEditableContact(EditableContact editableContact) {
        Log.d(TAG, "EditableContact retrieved");
        mEditableEnterpriseContact = editableContact;
        mContactData.setmIsFavorite(editableContact.isFavorite().getValue());
        setFavoriteColor();
    }

    @Override
    public void contactDeleted() {
        Log.d(TAG, "Contact Deleted");
    }

    @Override
    public void reportError() {
        //do nothing
    }

    @Override
    public void reportDeleteError() {}

    /**
     * Interface responsible for comunication between {@link ContactDetailsFragment} and
     * {@link com.avaya.android.vantage.basic.activities.MainActivity}. We are giving information is
     * contact edited or we just pressed back in {@link ContactDetailsFragment}
     */
    public interface OnContactDetailsInteractionListener {
        void back();

        void edit(ContactData contactData, boolean isNewContact);
    }

//    /**
//     * This method will enable video
//     */
//    public void enableVideo() {
//        if (mAdapter != null)
//            mAdapter.notifyDataSetChanged();
//    }

    /**
     * Set initials for contact. In case {@link ContactData} have photo url we we use it.
     * If there is no url we will create initial view from name of contact
     *
     * @param photo       {@link TextView} in which we are showing initials
     * @param contactData {@link ContactData} from which we are taking data
     */
    private void setInitials(final TextView photo, ContactData contactData) {
        String name;
        if (contactData.mPhoto != null && contactData.mPhoto.length > 0) {
            photo.setText("");
            Drawable drawable = mContactPhotoCache.get(contactData.mUUID);
            if (drawable != null) {
                photo.setBackground(drawable);
            } else {
                PhotoLoadUtility.setPhoto(mContactData, mContactImage, imageWidth, imageHeigth);
            }
            return;
        }
        mContactPhotoCache.remove(contactData.mUUID);
        name = contactData.mName;
        int colors[] = photo.getResources().getIntArray(R.array.material_colors);
        photo.setBackgroundResource(R.drawable.empty_circle);
        ((GradientDrawable) photo.getBackground().mutate()).setColor(colors[Math.abs(name.hashCode() % colors.length)]);


        int nameDisplay = mUserPreference.getInt(NAME_DISPLAY_PREFERENCE, FIRST_NAME_FIRST);
        @SuppressWarnings("UnusedAssignment") String firstLetter = "";
        String secondLetter = "";

        if (nameDisplay == LAST_NAME_FIRST) {
            if (mLastName != null && mLastName.length() > 0) {
                firstLetter = String.valueOf(mLastName.toUpperCase().charAt(0));
            }
            if (mFirstName != null && mFirstName.length() > 0) {
                secondLetter = String.valueOf(mFirstName.toUpperCase().charAt(0));
            }
        } else {
            if (mLastName != null && mLastName.length() > 0) {
                secondLetter = String.valueOf(mLastName.toUpperCase().charAt(0));
            }

            if (mFirstName != null && mFirstName.length() > 0) {
                firstLetter = String.valueOf(mFirstName.toUpperCase().charAt(0));
            }
        }

        String initials = firstLetter + secondLetter;
        photo.setText(initials);
    }

    /**
     * getting contact ID from URI.
     *
     * @param contactUri Android contact URI.
     * @return Contact ID.
     */
    private String getContactID(Uri contactUri) {
        Cursor cursor = getActivity().getContentResolver().query(contactUri, null, null, null, null);
        int idx;
        String id = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                idx = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                id = cursor.getString(idx);
            }
            cursor.close();
        }
        return id;
    }


    /**
     * Adding {@link RoundedBitmapDrawable} to our contact photo cash for future use
     *
     * @param mUUID                  String of cached contact
     * @param circularBitmapDrawable {@link RoundedBitmapDrawable} to be cached
     */
    @Override
    public void cacheContactDrawable(String mUUID, RoundedBitmapDrawable circularBitmapDrawable) {
        mContactPhotoCache.put(mUUID, circularBitmapDrawable);
    }

    /**
     * Performing check if photo for contact with specific UUID is cached
     *
     * @param uuid of contact for which we are performing check
     * @return boolean
     */
    @Override
    public boolean isPhotoCached(String uuid) {
        return mContactPhotoCache.containsKey(uuid);
    }
}
