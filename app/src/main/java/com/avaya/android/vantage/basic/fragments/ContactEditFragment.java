package com.avaya.android.vantage.basic.fragments;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.PhotoLoadUtility;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.csdk.EnterpriseContactManager;
import com.avaya.android.vantage.basic.csdk.LocalContactInfo;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.model.EditablePhoneNumber;
import com.avaya.android.vantage.basic.views.SlideAnimation;
import com.avaya.android.vantage.basic.views.adapters.ContactEditPhoneListAdapter;
import com.avaya.clientservices.contact.EditableContact;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static com.avaya.android.vantage.basic.Constants.DIRECTORY_ACCOUNT_NAME_LOADER;
import static com.avaya.android.vantage.basic.Constants.FIRST_NAME_FIRST;
import static com.avaya.android.vantage.basic.Constants.IPO_CONTACT_NAME_LIMIT;
import static com.avaya.android.vantage.basic.Constants.IPO_CONTACT_TYPE;
import static com.avaya.android.vantage.basic.Constants.LAST_NAME_FIRST;
import static com.avaya.android.vantage.basic.Constants.NAME_DISPLAY_PREFERENCE;
import static com.avaya.android.vantage.basic.Constants.NEW_CONTACT_PREF;
import static com.avaya.android.vantage.basic.Constants.REFRESH_CONTACTS;
import static com.avaya.android.vantage.basic.Constants.USER_PREFERENCE;
import static com.avaya.android.vantage.basic.activities.MainActivity.ACTIVE_CALL_FRAGMENT;
import static com.avaya.android.vantage.basic.activities.MainActivity.ACTIVE_VIDEO_CALL_FRAGMENT;
import static com.avaya.android.vantage.basic.activities.MainActivity.CONTACTS_DETAILS_FRAGMENT;
import static com.avaya.android.vantage.basic.activities.MainActivity.DIALER_FRAGMENT;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_IPOFFICE;

/**
 * {@link ContactEditFragment} is responsible for process of editing contact data.
 */
public class ContactEditFragment extends Fragment implements ContactEditPhoneListAdapter.OnContactEditPhoneChangeListener, EnterpriseContactManager.EnterpriseContactListener, View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = "ContactEditFragment";

    private static final int imageWidth = 105;
    private static final int imageHeigth = 105;

    private static final int REQUEST_CAMERA_PHOTO = 501;
    private static final int PICK_PHOTO = 502;
    private static final String TAG = "ContactEditFragment";
    private static final String REFRESH_FAVORITES = "refreshFavorites";

    private static final int LOCAL_CONTACT = 0;
    private static final int ENTERPRISE_CONTACT = 1;
    private static final int IPO_CONTACT = 2;

    private static final String LOCAL_CONTACT_PREF = "0";
    private static final String ENTERPRISE_CONTACT_PREF = "1";

    private ContactData mContactData;
    private EditableContact mEditableEnterpriseContact;
    private Uri mImageUri;
    private String mTempPhotoPath;
    private TextView mCancel, mDone, mContactImage, mEditTitle, mNewContactLocal, mNewContactEnterprise;
    private EditText mContactFirstName, mContactLastName, mContactJobTitle, mContactCompany, mContactAddress;
    private ImageView mFavoriteImage, mContactFirstNameClear, mContactLastNameClear, mContactJobTitleClear, mContactCompanyClear, mContactAddressClear;
    private ListView mContactPhoneList;
    private LinearLayout mAddContactNumber;
    private LinearLayout mPhoneTypeMenuHolder;
    private LinearLayout mContactTypeHolder;
    private FrameLayout contact_edit_holder;
    private SlideAnimation menuSlide = new SlideAnimation();
    private ContactEditPhoneListAdapter mAdapter;
    private List<EditablePhoneNumber> mPhoneNumbers;
    private boolean mFavoriteSelected, isAddingNewContact, phoneNumberValid, disallowFavoriteStatusChange, saveContactClicked;
    private int currentPhoneListPosition = 0;
    //phone type selection textViews
    private TextView typeWork, typeMobile, typeHome, typeFax, typePager, typeOther;
    public OnContactEditInteractionListener mEditListener;
    private Map<String, Drawable> mContactPhotoCache = new HashMap<>();
    private EditText mTextToFocus;
    private EnterpriseContactManager mEnterpriseContactManager;
    private SharedPreferences mUserPreference;
    private Handler mHandler;
    private Runnable mLayoutCloseRunnable;
    private int mContactType = 0;
    private String mAccountType = null;
    private String mAccountName = null;
    private boolean mIpoEnabled;
    private boolean mAllowNewIpoContact;
    private String mIpoAccountName = "";

    private AlertDialog.Builder builder;
    public AlertDialog alertDialog;

    public static ContactEditFragment newInstance(ContactData contactData, boolean isNewContact) {
        ContactEditFragment fragment = new ContactEditFragment();
        Bundle args = new Bundle();
        args.putParcelable(Constants.CONTACT_DATA, contactData);
        args.putBoolean(Constants.CONTACT_EDITING, isNewContact);
        fragment.setArguments(args);
        return fragment;
    }

    public static ContactEditFragment newInstance() {
        ContactEditFragment fragment = new ContactEditFragment();
        Bundle args = new Bundle();
        args.putBoolean(Constants.CONTACT_EDITING, true);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Empty constructor
     */
    public ContactEditFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnContactEditInteractionListener) {
            mEditListener = (ContactEditFragment.OnContactEditInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnContactDetailsInteractionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIpoEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ENABLE_IPOFFICE);
        Account[] accounts = AccountManager.get(getActivity()).getAccountsByType(IPO_CONTACT_TYPE);
        // no need to run the loader in case we don't have IPO enabled
        if (mIpoEnabled){
            getActivity().getLoaderManager().restartLoader(DIRECTORY_ACCOUNT_NAME_LOADER, null,this);
        }
        mAllowNewIpoContact = accounts.length > 0;
        mContactData = getArguments().getParcelable(Constants.CONTACT_DATA);
        isAddingNewContact = getArguments().getBoolean(Constants.CONTACT_EDITING);
        mPhoneNumbers = new ArrayList<>();
        if (mContactData != null) {
            if (mContactData.mPhones != null && mContactData.mPhones.size() > 0 && mContactData.mPhones.get(0).Number != null) {
                if (mContactData.mCategory == ContactData.Category.ENTERPRISE) {
                    findEnterpriseEditableContact();
                }
                phoneNumberValid = true;
            }
            if (mContactData.mPhones != null) {
                for (int i = 0; i < mContactData.mPhones.size(); i++) {
                    mPhoneNumbers.add(new EditablePhoneNumber(mContactData.mPhones.get(i).Number,
                            mContactData.mPhones.get(i).Type, mContactData.mPhones.get(i).Primary,
                            mContactData.mPhones.get(i).phoneNumberId));
                }
            }
            mFavoriteSelected = mContactData.isFavorite();
        } else {
            mContactData = ContactData.getEmptyContactData();
        }
        if (mContactData.mCategory == ContactData.Category.LOCAL){
            mContactType = LocalContactInfo.getAccountInfo(getContext(), mContactData.mUUID);
        } else if (mContactData.mCategory == ContactData.Category.ENTERPRISE) {
            mContactType = ENTERPRISE_CONTACT;
        } else if (mContactData.mCategory == ContactData.Category.IPO){
            mContactType = IPO_CONTACT;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_contact_edit, container, false);
        mCancel = (TextView) root.findViewById(R.id.contact_edit_cancel);
        mDone = (TextView) root.findViewById(R.id.contact_edit_done);
        mEditTitle = (TextView) root.findViewById(R.id.contact_edit_title);
        mContactPhoneList = (ListView) root.findViewById(R.id.contact_edit_phone_list);
        contact_edit_holder = (FrameLayout) root.findViewById(R.id.contact_edit_holder);
        mUserPreference = getActivity().getSharedPreferences(USER_PREFERENCE, MODE_PRIVATE);
        mContactTypeHolder = (LinearLayout) root.findViewById(R.id.contact_type_holder);
        if (mContactData.mCategory == ContactData.Category.ENTERPRISE) {
            mPhoneTypeMenuHolder = (LinearLayout) root.findViewById(R.id.phone_enterprise_type_menu_holder);
        } else {
            mPhoneTypeMenuHolder = (LinearLayout) root.findViewById(R.id.phone_type_menu_holder);
            typeOther = (TextView) mPhoneTypeMenuHolder.findViewById(R.id.type_other);
        }
        menuSlide.reDrawListener(mPhoneTypeMenuHolder);

        // phone number type
        typeWork = (TextView) mPhoneTypeMenuHolder.findViewById(R.id.type_work);
        typeMobile = (TextView) mPhoneTypeMenuHolder.findViewById(R.id.type_mobile);
        typeHome = (TextView) mPhoneTypeMenuHolder.findViewById(R.id.type_home);
        typeFax = (TextView) mPhoneTypeMenuHolder.findViewById(R.id.type_fax);
        typePager = (TextView) mPhoneTypeMenuHolder.findViewById(R.id.type_pager);

        // new contact type
        mNewContactLocal = (TextView) mContactTypeHolder.findViewById(R.id.contact_type_local);
        mNewContactEnterprise = (TextView) mContactTypeHolder.findViewById(R.id.contact_type_enterprise);

        final View header = inflater.inflate(R.layout.contact_edit_header, mContactPhoneList, false);
        mContactFirstName = (EditText) header.findViewById(R.id.contact_edit_first_name);
        mContactLastName = (EditText) header.findViewById(R.id.contact_edit_last_name);
        mContactJobTitle = (EditText) header.findViewById(R.id.contact_edit_job_title);
        mContactCompany = (EditText) header.findViewById(R.id.contact_edit_company);
        mContactAddress = (EditText) header.findViewById(R.id.contact_edit_address);
        mContactImage = (TextView) header.findViewById(R.id.contact_edit_contact_image);
        mFavoriteImage = (ImageView) header.findViewById(R.id.contact_edit_contact_favorite);
        mContactFirstNameClear = (ImageView) header.findViewById(R.id.contact_edit_first_name_clear);
        mContactLastNameClear = (ImageView) header.findViewById(R.id.contact_edit_last_name_clear);
        mContactJobTitleClear = (ImageView) header.findViewById(R.id.contact_edit_job_title_clear);
        mContactCompanyClear = (ImageView) header.findViewById(R.id.contact_edit_company_clear);
        mContactAddressClear = (ImageView) header.findViewById(R.id.contact_edit_address_clear);
        mAddContactNumber = (LinearLayout) header.findViewById(R.id.add_phone_number);

        mContactPhoneList.addHeaderView(header);
        mHandler = new Handler();
        mLayoutCloseRunnable = new Runnable() {
            @Override
            public void run() {
                hideMenus();
            }
        };

        if (mIpoEnabled) {
            mNewContactEnterprise.setText(R.string.contact_details_add_contact_personal_directory);
        } else {
            mNewContactEnterprise.setText(R.string.contact_details_add_contact_enterprise);
        }

        setListeners();
        setData();
        loadContactImage();
        if (isAddingNewContact){
            loadNewContactTypeSettings();
        }
        if (mIpoEnabled && !mAllowNewIpoContact){
            setNewContactType(LOCAL_CONTACT);
        } else {
            setNewContactType(mContactType);
        }

        if( getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null)
            ((MainActivity)getActivity()).changeUiForFullScreenInLandscape(true);

        return root;
    }

    /**
     * Load settings from shared preferences and set new contact type according to settings
     */
    private void loadNewContactTypeSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String defaultNewContact = null;//
        try{
            defaultNewContact = prefs.getString(NEW_CONTACT_PREF, ENTERPRISE_CONTACT_PREF);
        }catch (ClassCastException e){
            e.printStackTrace();
            defaultNewContact = String.valueOf(prefs.getLong(NEW_CONTACT_PREF,Long.valueOf(ENTERPRISE_CONTACT_PREF) ) );
        }
        switch (defaultNewContact){
            case LOCAL_CONTACT_PREF:
                setNewContactType(LOCAL_CONTACT);
                break;
            case ENTERPRISE_CONTACT_PREF:
                // if IPO is enabled, Enterprise is disabled. That is why we switch between these two
                if (mIpoEnabled){
                    setNewContactType(IPO_CONTACT);
                } else {
                    setNewContactType(ENTERPRISE_CONTACT);
                }
                break;
            default:
                setNewContactType(LOCAL_CONTACT);
        }
    }

    /**
     * Hiding menus and setting them non clickable
     */
    private void hideMenus() {
        mHandler.removeCallbacks(mLayoutCloseRunnable);
        mPhoneTypeMenuHolder.setVisibility(View.INVISIBLE);
        mContactTypeHolder.setVisibility(View.INVISIBLE);
        mEditTitle.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_expand_more, 0, 0, 0);
        contact_edit_holder.setClickable(false);
        contact_edit_holder.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mEditListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && getActivity() != null) {
            switch (requestCode) {
                case PICK_PHOTO:
                    mImageUri = data.getData();
                    if (mImageUri != null) {
                        setContactPhoto(mImageUri);
                    }
                    break;
                case REQUEST_CAMERA_PHOTO:
                    try {
                        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
                        File f = new File(mTempPhotoPath);
                        if(getResources().getBoolean(R.bool.is_landscape) == false) {
                            //Do not rotate in case of K155
                            rotateImage(mTempPhotoPath);
                        }
                        mImageUri = Uri.fromFile(f);
                        if (mImageUri != null) {
                            setContactPhoto(mImageUri);
                            mediaScanIntent.setData(mImageUri);
                            getActivity().sendBroadcast(mediaScanIntent);
                        }
                        mTempPhotoPath = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * Load image from local contact or from CSDK
     */

    private void loadContactImage() {
        //preventing crash in case mPhotoURI is null and setting up contact picture
        if (mContactData.mPhotoURI != null && mContactData.mPhotoURI.trim().length() > 0) {
            Uri mPhotoURI = Uri.parse(mContactData.mPhotoURI);
            setContactPhoto(mPhotoURI);
        } else {
            setInitials(mContactImage, mContactData);
        }
    }

    /**
     * Update on stat of favorite based on click
     */
    private void updateFavorites() {
        mFavoriteSelected = !mFavoriteSelected;
        setFavoriteColor();
    }

    /**
     * Setting color of favorites star properly for contact
     */
    private void setFavoriteColor() {
        if (mFavoriteImage != null) {
            if (mFavoriteSelected) {
                mFavoriteImage.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent));
                mFavoriteImage.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_selected48));
            } else {
                mFavoriteImage.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                mFavoriteImage.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorites_selected));
            }
        }
    }

    /**
     * Set listeners on all views.
     */
    private void setListeners() {

        typeWork.setOnClickListener(this);
        typeHome.setOnClickListener(this);
        typeMobile.setOnClickListener(this);
        typeFax.setOnClickListener(this);
        typePager.setOnClickListener(this);

        // no need for click listeners if we are editing existing contact
        if (isAddingNewContact) {
            mNewContactLocal.setOnClickListener(this);
            mNewContactEnterprise.setOnClickListener(this);

            if (!(mIpoEnabled && !mAllowNewIpoContact)) {
                mEditTitle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        contact_edit_holder.setClickable(true);
                        contact_edit_holder.setVisibility(View.VISIBLE);
                        mContactTypeHolder.setVisibility(View.VISIBLE);
                        mEditTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_expand_less, 0, 0, 0);
                    }
                });
            } else {
                Log.e(TAG, "IPO is Enabled and Allow New IPO is not");
            }

            mNewContactEnterprise.setEnabled(true);
            mNewContactEnterprise.setOnClickListener(this);
            mNewContactEnterprise.setTextColor(getActivity().getColor(R.color.primary));
        } else {
            mEditTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        if (mContactData.mCategory != ContactData.Category.ENTERPRISE) {
            typeOther.setOnClickListener(this);
        }

        mContactPhoneList.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (mTextToFocus != null && getActivity() != null) {
                    mTextToFocus.setFocusableInTouchMode(true);
                    mTextToFocus.requestFocus();
                    if (getResources().getBoolean(R.bool.is_landscape) == false) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(mTextToFocus, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }
        });

        contact_edit_holder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenus();
            }
        });


        mContactFirstNameClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContactFirstName.setText("");
            }
        });
        mContactLastNameClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContactLastName.setText("");
            }
        });
        mContactJobTitleClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContactJobTitle.setText("");
            }
        });
        mContactCompanyClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContactCompany.setText("");
            }
        });
        mContactAddressClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContactAddress.setText("");
            }
        });
        mContactFirstName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    mContactFirstNameClear.setVisibility(View.INVISIBLE);
                } else {
                    mContactFirstNameClear.setVisibility(View.VISIBLE);
                }
            }
        });
        mContactLastName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    mContactLastNameClear.setVisibility(View.INVISIBLE);
                } else {
                    mContactLastNameClear.setVisibility(View.VISIBLE);
                }
            }
        });
        mContactJobTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    mContactJobTitleClear.setVisibility(View.INVISIBLE);
                } else {
                    mContactJobTitleClear.setVisibility(View.VISIBLE);
                }
            }
        });
        mContactCompany.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    mContactCompanyClear.setVisibility(View.INVISIBLE);
                } else {
                    mContactCompanyClear.setVisibility(View.VISIBLE);
                }
            }
        });
        mContactAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    mContactAddressClear.setVisibility(View.INVISIBLE);
                } else {
                    mContactAddressClear.setVisibility(View.VISIBLE);
                }
            }
        });
        mFavoriteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!disallowFavoriteStatusChange) {
                    updateFavorites();
                } else {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_non_edit_favorite), Utils.SNACKBAR_LONG);
                }
            }
        });
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                cancelOnClickListener();
            }
        });
        mDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mAdapter.primaryPhoneExist()) {
                    // hiding the keyboard so that SnackBar can be visible
                    Utils.hideKeyboard(getActivity());
                    Utils.sendSnackBarData(getContext(), getString(R.string.must_set_primary), Utils.SNACKBAR_LONG);

                    return;
                }

                if (saveContactClicked) {
                    return;
                }
                saveContactClicked = true;

                if (!checkIfValid()) {
                    saveContactClicked = false;
                    return;
                }


                if (mContactType == IPO_CONTACT){
                    if (!TextUtils.isEmpty(mIpoAccountName)) {
                        mAccountType = IPO_CONTACT_TYPE;
                        mAccountName = mIpoAccountName;
                    } else {
                        Utils.hideKeyboard(getActivity());
                        Utils.sendSnackBarDataWithDelay(getActivity(), getString(R.string.not_enterprise_found), true);
                        saveContactClicked = false;
                        return;
                    }
                } else {
                    mAccountName = null;
                    mAccountType = null;
                }
                saveContact();

                if( getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
                    ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(false);
                    ((MainActivity) getActivity()).mViewPager.setEnabledSwipe(true);
                }
            }
        });
        mAddContactNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdapter != null && allowAddPhoneNumber()) {
                    ContactData.PhoneType phoneType;
                    if (mContactType == IPO_CONTACT){
                        phoneType = ContactData.PhoneType.WORK;
                    } else {
                        phoneType = ContactData.PhoneType.MOBILE;
                    }

                    EditablePhoneNumber newNumber = new EditablePhoneNumber("", phoneType, false, null);
                    mPhoneNumbers.add(newNumber);
                    mAdapter.notifyDataSetChanged();
                    mContactPhoneList.setSelection(mAdapter.getCount() - 1);
                    validateEdit();
                }
            }
        });
        mContactImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mContactData.mCategory == ContactData.Category.LOCAL || mContactType == LOCAL_CONTACT) {
                    showImagePicker();
                }
            }
        });
        mContactFirstName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                validateEdit();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        mContactLastName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                validateEdit();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    public void cancelOnClickListener(){
        mEditListener.cancelEdit();

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        boolean value = sharedPref.getBoolean("addcontact_button", false);
        if( getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
            if(((MainActivity) getActivity()).isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)){

                ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(true);
            }else{
                if( ((MainActivity) getActivity()).isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)   )
                    ((MainActivity)getActivity()).changeUiForFullScreenInLandscape(true);
                else
                    ((MainActivity)getActivity()).changeUiForFullScreenInLandscape(false);
            }
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("addcontact_button", false);
        editor.commit();

    }
    /**
     * Saves contact data.
     * Called upon pressing "Done" button.
     */
    private void saveContact() {
        ContactData.Category category;

        if (isAddingNewContact) {
            switch (mContactType) {
                case LOCAL_CONTACT:
                    category = ContactData.Category.LOCAL;
                    break;
                case IPO_CONTACT:
                    category = ContactData.Category.IPO;
                    break;
                case ENTERPRISE_CONTACT:
                    category = ContactData.Category.ENTERPRISE;
                    break;
                default:
                    category = ContactData.Category.LOCAL;

            }
        } else {
            category = mContactData.mCategory;
        }
        List<ContactData.PhoneNumber> newPhoneNumbers = addNewPhoneNumbersToContact();

        final String firstName = mContactFirstName.getText().toString().trim();
        final String lastName = (category == ContactData.Category.IPO) ? "" : mContactLastName.getText().toString().trim();
        final String name = (category == ContactData.Category.IPO) ? firstName : Utils.combinedName(getContext(), firstName, lastName);
        ContactData newContactData = new ContactData(name, firstName, lastName,
                mContactData.mPhoto,
                mFavoriteSelected,
                mContactAddress.getText().toString(),
                mContactData.mCity,
                mContactJobTitle.getText().toString(),
                mContactCompany.getText().toString(),
                newPhoneNumbers,
                category,
                mContactData.mUUID,
                mContactData.mURI,
                mContactData.mPhotoThumbnailURI,
                mContactData.mHasPhone,
                mContactData.mEmail,
                mContactData.mPhotoURI,
                mContactData.mAccountType, "", "");
        dataSaved();
        if (newContactData.mCategory == ContactData.Category.ENTERPRISE){
            if (!isAddingNewContact && mEditableEnterpriseContact == null) {
                Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_enterprise_contact_not_found), Utils.SNACKBAR_LONG);
                findEnterpriseEditableContact();
            } else {
                mEditListener.confirmEnterpriseEdit(newContactData, mEditableEnterpriseContact, isAddingNewContact);
            }
        } else {
            mEditListener.confirmLocalContactEdit(newContactData, mImageUri, isAddingNewContact, mAccountType, mAccountName);
        }

        saveContactClicked = false;
    }

    /**
     * Set edited contact phones.
     *
     * @return List of newly set contact phone numbers.
     */
    private List<ContactData.PhoneNumber> addNewPhoneNumbersToContact() {
        List<ContactData.PhoneNumber> newPhoneNumbers = new ArrayList<>();

        // Focused phone position. Used to fix bug with phone number not being saved
        // if EditText is focused in time of saving.
        int focusedPhonePosition = mAdapter.getViewPositionWithFocus();

        boolean phoneEditInProgress = focusedPhonePosition >= 0;

        for (int i = 0; i < mPhoneNumbers.size(); i++) {
            if (!TextUtils.isEmpty(mPhoneNumbers.get(i).getNumber()) || focusedPhonePosition == i) {
                String retrievedPhoneNumber = getPhoneNumber(i, phoneEditInProgress);
                if (!TextUtils.isEmpty(retrievedPhoneNumber)) {
                    newPhoneNumbers.add(new ContactData.PhoneNumber(retrievedPhoneNumber,
                            mPhoneNumbers.get(i).getType(), mPhoneNumbers.get(i).isPrimary(),
                            mPhoneNumbers.get(i).getPhoneNumberId()));
                }
            }
        }

        return newPhoneNumbers;
    }

    /**
     * Used to fix problem with phone number edit field that is in focus.
     * Field in focus wont update mPhoneNumbers list.
     *
     * @param phonePosition        Position of phone number in list.
     * @param phoneNumberIsFocused Edit text with phone number is focused.
     * @return phone number
     */
    private String getPhoneNumber(int phonePosition, boolean phoneNumberIsFocused) {
        String phoneNumber = mPhoneNumbers.get(phonePosition).getNumber();
        if (mContactPhoneList != null) {
            if (phoneNumberIsFocused) {
                View focusedPhoneNumberItem = mContactPhoneList.getChildAt(phonePosition + 1);
                if (focusedPhoneNumberItem != null) {
                    EditText phoneNumberEditText = (EditText) focusedPhoneNumberItem.findViewById(R.id.contact_edit_phone_number);
                    if (phoneNumberEditText != null) {
                        phoneNumber = phoneNumberEditText.getText().toString();
                    }
                }
            }
        }
        return phoneNumber;
    }

    /**
     * Method is used to check if all phone number texts have phone numbers in them
     *
     * @return if some phone numbers are empty, do not allow adding of another phone number
     */
    private boolean allowAddPhoneNumber() {
        boolean allowAdd = true;
        if (mContactPhoneList != null) {
            // IPO supports only one phone number, just making sure we notify user about that
            if (mContactType == IPO_CONTACT && mContactPhoneList.getAdapter().getCount() > 1){
                Utils.hideKeyboard(getActivity()); // hiding keyboard to make sure user can see error message
                Utils.sendSnackBarData(getActivity(), getString(R.string.contact_details_ipo_only_one_number), true);
                return false;
            }

            for (int i = 0; i < mContactPhoneList.getAdapter().getCount(); i++) {
                View phoneNumberItem = mContactPhoneList.getChildAt(i);
                if (phoneNumberItem != null) {
                    EditText phoneNumberText = (EditText) phoneNumberItem.findViewById(R.id.contact_edit_phone_number);
                    if (phoneNumberText != null && phoneNumberText.getText().toString().trim().length() > 0) {
                        allowAdd = true;
                    }
                    if (phoneNumberText != null && phoneNumberText.getText().toString().trim().length() == 0) {
                        allowAdd = false;
                        break;
                    }
                }
            }
        }
        return allowAdd;
    }

    /**
     * This method will go through all phone numbers and check if any phone number is valid
     *
     * @return if any phone number is valid, allow save
     */
    private boolean allowSavingPhones() {
        boolean allowSave = false;
        if (mContactPhoneList != null) {
            for (int i = 0; i < mContactPhoneList.getAdapter().getCount(); i++) {
                View phoneNumberItem = mContactPhoneList.getChildAt(i);
                if (phoneNumberItem != null) {
                    EditText phoneNumberText = (EditText) phoneNumberItem.findViewById(R.id.contact_edit_phone_number);
                    if (phoneNumberText != null && phoneNumberText.getText().toString().trim().length() > 0) {
                        allowSave = true;
                    }
                }
            }
        }
        return allowSave;
    }

    /**
     * Validation of contact edit.
     */
    private void validateEdit() {

        if (mContactType == IPO_CONTACT && mContactFirstName.getText().length() > IPO_CONTACT_NAME_LIMIT){
            mDone.setTextColor(getResources().getColor(R.color.midGray, getActivity().getTheme()));
            mDone.setEnabled(false);
            return;
        }

        if (mPhoneNumbers.size() >= 1 && phoneNumberValid && validateName()) {
            mDone.setTextColor(getResources().getColor(R.color.midOrange, getActivity().getTheme()));
            mDone.setEnabled(true);
        } else {
            mDone.setTextColor(getResources().getColor(R.color.midGray, getActivity().getTheme()));
            mDone.setEnabled(false);
        }
    }

    /**
     * Validation of required name.
     *
     * @return Is data valid.
     */
    private boolean validateName() {
        return !mContactFirstName.getText().toString().trim().equals("") || !mContactLastName.getText().toString().trim().equals("");
    }

    /**
     * Conditions needed to edit/add contact.
     *
     * @return allow/disallow contact changes.
     */
    private boolean checkIfValid() {

        if (mContactFirstName.getText().toString().trim().matches("") && mContactLastName.getText().toString().trim().matches("")) {
            Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_name_required), Utils.SNACKBAR_LONG);
            mContactFirstName.requestFocus();
            return false;
        } else if (mAdapter.getCount() == 0) {
            Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_number_required), Utils.SNACKBAR_LONG);
            return false;
        }
        return true;
    }

    /**
     * Present user information to user.
     */
    private void setData() {
        mContactFirstName.setText(mContactData.mFirstName);
        mContactLastName.setText(mContactData.mLastName);
        mContactJobTitle.setText(mContactData.mPosition);
        mContactCompany.setText(mContactData.mCompany);
        mContactAddress.setText(mContactData.mLocation);
        if (isAddingNewContact) {
            mEditTitle.setText(getString(R.string.contact_details_add_contact));
        }
        setFavoriteColor();

        mAdapter = new ContactEditPhoneListAdapter(getContext(), mPhoneNumbers, this);
        mContactPhoneList.setAdapter(mAdapter);
        validateEdit();
        mContactFirstName.requestFocus();
    }

    /**
     * Displaying contact photo
     *
     * @param photoURI contact photo URI
     */
    private void setContactPhoto(Uri photoURI) {
        try {
            Bitmap uriImage = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), photoURI);
            //Perform check if we have to check for image rotation or not. If image is provided by CSDK
            //result of getRealPathFromURI will be empty string
            uriImage = Utils.checkAndPerformBitmapRotation(uriImage, getActivity().getContentResolver(), photoURI);
            RoundedBitmapDrawable contactPhoto =
                    RoundedBitmapDrawableFactory.create(getResources(), uriImage);
            contactPhoto.setCircular(true);
            mContactImage.setText("");
            mContactImage.setBackground(contactPhoto);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set initials for contact. In case {@link ContactData} have photo url we we use it.
     * If there is no url we will create initial view from name of contact.
     *
     * @param photo       {@link TextView} in which we are showing initials.
     * @param contactData {@link ContactData} from which we are taking data.
     */
    private void setInitials(final TextView photo, ContactData contactData) {
        String name;
        String lastName = contactData.mLastName;
        String firstName = contactData.mFirstName;

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
            if (lastName != null && lastName.length() > 0){
                firstLetter = String.valueOf(lastName.toUpperCase().charAt(0));
            }
            if (firstName != null && firstName.length() > 0){
                secondLetter = String.valueOf(firstName.toUpperCase().charAt(0));
            }
        } else {
            if (lastName != null && lastName.length() > 0){
                secondLetter = String.valueOf(lastName.toUpperCase().charAt(0));
            }

            if (firstName != null && firstName.length() > 0){
                firstLetter = String.valueOf(firstName.toUpperCase().charAt(0));
            }
        }

        String initials = firstLetter + secondLetter;
        photo.setText(initials);

    }

    /**
     * Get EditableEnterpriseContact from CSDK since ContactData is immutable
     */
    private void findEnterpriseEditableContact() {
        if (mEnterpriseContactManager == null) {
            mEnterpriseContactManager = new EnterpriseContactManager(this);
        }
        if (!TextUtils.isEmpty(mContactData.mEmail)) {
            mEnterpriseContactManager.findEnterpriseContact(mContactData.mEmail, false);
        } else {
            mEnterpriseContactManager.findEnterpriseContact(mContactData.mPhones.get(0).Number, false);
        }
    }


    /**
     * Prepare and show {@link android.app.AlertDialog} for choosing new image with options for
     * taking photo, choosing photo from device, deleting it or just cancel selection.
     */
    private void showImagePicker() {
        final CharSequence[] items = {getString(R.string.contact_edit_take_photo), getString(R.string.contact_edit_choose_photo), getString(R.string.contact_edit_delete_photo)};
        builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.contact_edit_change_photo));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (items[item].equals(getString(R.string.contact_edit_take_photo))) {
                    dispatchTakePictureIntent();
                } else if (items[item].equals(getString(R.string.contact_edit_choose_photo))) {
                    Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, PICK_PHOTO);
                } else if (items[item].equals(getString(R.string.contact_edit_delete_photo))){
                    // just adding empty string so that we can tell LocalContactManager to delete the image for this contact
                    mImageUri = Uri.parse("");
                    setInitials(mContactImage, mContactData);
                }
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Processing removing of item and calling method to check if edit is valid
     */
    @Override
    public void itemRemoved() {

        // bellow code resolves a bug that when you click to delete phone number it deletes one bellow also
        View lastPhoneNumberItem = mContactPhoneList.getChildAt(mAdapter.getCount());
        if (lastPhoneNumberItem != null) {
            EditText phoneNumberEditText = (EditText) lastPhoneNumberItem.findViewById(R.id.contact_edit_phone_number);
            if (phoneNumberEditText != null) {
                String phoneNumber = phoneNumberEditText.getText().toString();
                mAdapter.getmContactPhones().get(mAdapter.getmContactPhones().size() - 1).setNumber(phoneNumber);
            }
        }

        // validating if edit is allowed
        validateEdit();
    }

    /**
     * Processing information on phone number valid event and calling method to check if edit is valid
     *
     * @param phoneValid boolean
     */
    @Override
    public void phoneNumberValid(boolean phoneValid) {
        phoneNumberValid = phoneValid;

        // in case we get response from EditText that it is not valid, we just do general check
        // and see if any phone numbers are valid. If they are, we allo saving of contact
        if (!phoneValid) {
            phoneNumberValid = allowSavingPhones();
        }
        validateEdit();
    }

    /**
     * Processing click on phone type
     *
     * @param itemPosition     int position of item
     * @param listItemPosition int position in list of items
     */
    @Override
    public void phoneTypeClicked(int itemPosition, int listItemPosition) {
        Utils.hideKeyboard(getActivity());
        this.currentPhoneListPosition = listItemPosition;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        int position = (itemPosition - 450);
        if (position < 0) position = 0;
        params.setMargins(70, position, 0, 0);
        params.gravity = Gravity.TOP;
        mPhoneTypeMenuHolder.setLayoutParams(params);
        contact_edit_holder.setClickable(true);
        contact_edit_holder.setVisibility(View.VISIBLE);
        mPhoneTypeMenuHolder.setVisibility(View.VISIBLE);
        mHandler.postDelayed(mLayoutCloseRunnable, Constants.LAYOUT_DISAPPEAR_TIME);
    }

    /**
     * Requesting focus for {@link EditText}
     *
     * @param editText {@link EditText}
     */
    @Override
    public void requestFocus(final EditText editText) {
        mTextToFocus = editText;
        if(getResources().getBoolean(R.bool.is_landscape) == true){
            mTextToFocus.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        }
    }

    @Override
    public void retrieveEditableContact(EditableContact editableContact) {
        if (editableContact != null) {
            Log.d(TAG, "EditableContact retrieved" + editableContact.getNativeDisplayName());
            mEditableEnterpriseContact = editableContact;
            setupEnterpriseFields(mEditableEnterpriseContact);
        }
    }

    @Override
    public void contactDeleted() {
        //None
    }

    /**
     * Showing appropriate error in form of {@link android.support.design.widget.Snackbar}
     */
    @Override
    public void reportError() {
        if (getContext() != null) {
            Utils.sendSnackBarData(getContext(), getString(R.string.contact_uneditable_error), Utils.SNACKBAR_LONG);
        }
    }

    @Override
    public void reportDeleteError() {
        //None
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.type_work:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.WORK);
                break;
            case R.id.type_mobile:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.MOBILE);
                break;
            case R.id.type_home:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.HOME);
                break;
            case R.id.type_fax:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.FAX);
                break;
            case R.id.type_pager:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.PAGER);
                break;
            case R.id.type_other:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.OTHER);
                break;
            case R.id.contact_type_local:
                setNewContactType(LOCAL_CONTACT);
                break;
            case R.id.contact_type_enterprise:
                // if IPO is enabled, we create IPO, if not, we create Enterprise
                if (mIpoEnabled) {
                    setNewContactType(IPO_CONTACT);
                } else {
                    setNewContactType(ENTERPRISE_CONTACT);
                }
                break;
            default:
                mAdapter.setPhoneType(currentPhoneListPosition, ContactData.PhoneType.OTHER);
        }
        hideMenus();

    }

    /**
     * Change new contact type
     * @param newContactType type of new contact
     */
    private void setNewContactType(int newContactType){

        switch (newContactType){
            case LOCAL_CONTACT:
                if (isAddingNewContact){
                    mEditTitle.setText(getContext().getString(R.string.new_contact_header_text, getContext().getString(R.string.contact_details_add_contact_local).substring(0,1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_local).substring(1).toLowerCase()  ));
                } else {
                    mEditTitle.setText(getContext().getString(R.string.edit_contact_header_text, getContext().getString(R.string.contact_details_add_contact_local).substring(0,1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_local).substring(1).toLowerCase()  ));
                }

                if(mContactType != IPO_CONTACT) {
                    mContactLastName.setVisibility(View.VISIBLE);
                }else{
                    mContactLastName.setVisibility(View.VISIBLE);
                    mContactLastName.getText().clear();
                }

                mContactType = LOCAL_CONTACT;

                changeEditTexts(true, mContactLastName, mContactAddress, mContactCompany, mContactJobTitle);
                mFavoriteImage.setVisibility(View.VISIBLE);



                break;
            case ENTERPRISE_CONTACT:
                if (isAddingNewContact){
                    mEditTitle.setText(getContext().getString(R.string.new_contact_header_text, getContext().getString(R.string.contact_details_add_contact_enterprise).substring(0,1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_enterprise).substring(1).toLowerCase()  ));
                } else {
                    mEditTitle.setText(getContext().getString(R.string.edit_contact_header_text, getContext().getString(R.string.contact_details_add_contact_enterprise).substring(0,1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_enterprise).substring(1).toLowerCase()  ));
                }
                mContactType = ENTERPRISE_CONTACT;
                mFavoriteImage.setVisibility(View.VISIBLE);
                changeEditTexts(true, mContactLastName, mContactAddress, mContactCompany, mContactJobTitle);
                clearPhoto();
                mContactLastName.setVisibility(View.VISIBLE);
                break;
            case IPO_CONTACT:

                if (isAddingNewContact){
                    mEditTitle.setText(getContext().getString(R.string.new_contact_header_text, getContext().getString(R.string.contact_details_add_contact_personal_directory).substring(0,1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_personal_directory).substring(1).toLowerCase()  ));
                } else {
                    mEditTitle.setText(getContext().getString(R.string.edit_contact_header_text, getContext().getString(R.string.contact_details_add_contact_personal_directory).substring(0,1).toLowerCase() + getContext().getString(R.string.contact_details_add_contact_personal_directory).substring(1).toLowerCase()  ));
                }
                mContactType = IPO_CONTACT;
                changeEditTexts(false, mContactLastName, mContactAddress, mContactCompany, mContactJobTitle);

                clearPhoto();
                mContactFirstName.setHint(R.string.contact_edit_ipo_name_hint);
                mContactFirstName.setText(Utils.combinedName(getContext(), mContactData.mFirstName, mContactData.mLastName));
                mContactLastName.setVisibility(View.GONE);
                mContactCompany.getText().clear();
                mContactJobTitle.getText().clear();
                mContactAddress.getText().clear();
                mFavoriteSelected = false;
                setFavoriteColor();
                mFavoriteImage.setVisibility(View.INVISIBLE);
                if (mContactFirstName.getText().length() > IPO_CONTACT_NAME_LIMIT){
                    Utils.sendSnackBarData(getActivity(), String.format(getActivity().getString(R.string.contact_name_char_limit), String.valueOf(IPO_CONTACT_NAME_LIMIT)), true);
                    Utils.hideKeyboard(getActivity());
                }
                break;
            default:
                mEditTitle.setText(getContext().getString(R.string.new_contact_header_text, getContext().getString(R.string.contact_details_add_contact_local)));
                mContactType = LOCAL_CONTACT;
                changeEditTexts(true, mContactLastName, mContactAddress, mContactCompany, mContactJobTitle);
                mFavoriteImage.setVisibility(View.VISIBLE);
                mContactLastName.setVisibility(View.VISIBLE);
        }
        validateEdit();
    }

    /**
     * If user selects Enterprise contact, we clear the photo and notify the photo it is not supported
     */
    private void clearPhoto(){
        if (mImageUri != null && mImageUri.toString().trim().length() > 0){
            mContactImage.setText("");
            mContactImage.setBackground(getActivity().getDrawable(R.drawable.ic_avatar_generic105));
            Utils.sendSnackBarData(getActivity(), getString(R.string.photo_not_supported), true);
        }

    }

    /**
     * Since IPO supports only display name (no first or last name) we will disable all non supported
     * fields in case user is creating IPO contact
     * @param enabled should we enable or disable edit text's
     * @param editTexts array of edit texts to change
     */
    private void changeEditTexts(boolean enabled, EditText... editTexts){
        for (EditText editText : editTexts){
            editText.setEnabled(enabled);
            editText.setFocusableInTouchMode(enabled);
            if (!enabled) {
                editText.setTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
                editText.setHintTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            } else {
                editText.setTextColor(getResources().getColor(R.color.colorBlack, getActivity().getTheme()));
                editText.setHintTextColor(getResources().getColor(R.color.colorDefaultHint, getActivity().getTheme()));
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final String[] PROJECTION = {ContactsContract.Directory.ACCOUNT_NAME};
        final String SELECTION = ContactsContract.Directory.ACCOUNT_TYPE + " = ?";
        final String[] SELECTION_ARGS = {IPO_CONTACT_TYPE};
        if (id == DIRECTORY_ACCOUNT_NAME_LOADER){
            return new CursorLoader(getActivity(), ContactsContract.Directory.CONTENT_URI,
                    PROJECTION, SELECTION, SELECTION_ARGS, ContactsContract.Directory.DISPLAY_NAME);
        }
        return null;
    }

    /**
     * For optimization purpose, we only get ACCOUNT_NAME
     * if we ever need to get anything else, we must change String[] PROJECTION in {@link #onCreateLoader}
     * @param loader loader returned from loader manager
     * @param cursor data received from loader in {@link #onCreateLoader}
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == DIRECTORY_ACCOUNT_NAME_LOADER){
            while (cursor != null && cursor.moveToNext()){
                mIpoAccountName = cursor.getString(cursor.getColumnIndex(ContactsContract.Directory.ACCOUNT_NAME));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * To be implemented in {@link com.avaya.android.vantage.basic.activities.MainActivity} to
     * provide communication with {@link ContactsFragment}
     */
    public interface OnContactEditInteractionListener {
        void cancelEdit();

        void confirmLocalContactEdit(ContactData contactData, Uri imageUri, boolean isNewContact, String accountType, String accountName);

        void confirmEnterpriseEdit(ContactData contactData, EditableContact contact, boolean isNewContact);
    }

    /**
     * Inform application that data has been changed.
     * Store that info in preference.
     */
    private void dataSaved() {
        SharedPreferences mSharedPref = getActivity().getSharedPreferences(USER_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(REFRESH_CONTACTS, true);
        editor.putBoolean(REFRESH_FAVORITES, true);
        editor.apply();
        Log.d(TAG, "Data saved");
    }


    /**
     * Prepare Enterprise fields for editing process based on provided {@link EditableContact}
     *
     * @param editableContact {@link EditableContact}
     */
    private void setupEnterpriseFields(EditableContact editableContact) {

        if (!editableContact.getNativeFirstName().getCapability().isAllowed()) {
            Log.d(LOG_TAG, "FirstName not editable");
            mContactFirstName.setFocusable(false);
            mContactFirstName.setTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            mContactFirstName.setHintTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            mContactFirstName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_non_edit_first_name), Utils.SNACKBAR_LONG);
                }
            });
        }
        if (!editableContact.getNativeLastName().getCapability().isAllowed()) {
            Log.d(LOG_TAG, "LastName not editable");
            mContactLastName.setFocusable(false);
            mContactLastName.setTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            mContactLastName.setHintTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            mContactLastName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_non_edit_last_name), Utils.SNACKBAR_LONG);
                }
            });
        }

        if (!editableContact.getCompany().getCapability().isAllowed()) {
            Log.d(LOG_TAG, "mContactCompany name not editable");
            mContactCompany.setFocusable(false);
            mContactCompany.setTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            mContactCompany.setHintTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            mContactCompany.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_non_edit_company), Utils.SNACKBAR_LONG);
                }
            });
        }

        if (!editableContact.getTitle().getCapability().isAllowed()) {
            Log.d(LOG_TAG, "mEditTitle name not editable");
            mContactJobTitle.setFocusable(false);
            mContactJobTitle.setTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            mContactJobTitle.setHintTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            mContactJobTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_non_edit_job), Utils.SNACKBAR_LONG);
                }
            });
        }

        if (!editableContact.getStreetAddress().getCapability().isAllowed()) {
            Log.d(LOG_TAG, "mContactAddress name not editable");
            mContactAddress.setFocusable(
                    false);
            mContactAddress.setTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            mContactAddress.setHintTextColor(getResources().getColor(R.color.lightGray, getActivity().getTheme()));
            mContactAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.sendSnackBarData(getContext(), getString(R.string.contact_edit_non_edit_address), Utils.SNACKBAR_LONG);
                }
            });
        }

        //noinspection RedundantIfStatement
        if (!editableContact.isFavorite().getCapability().isAllowed()) {
            disallowFavoriteStatusChange = true;
        } else {
            disallowFavoriteStatusChange = false;
        }
    }

    /**
     * Create file for camera image
     *
     * @return Empty file
     * @throws IOException IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mTempPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Sending intent for taking picture
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "Error occurred while creating the File");
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getActivity(),
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CAMERA_PHOTO);
            }
        }
    }

    /**
     * Rotate image since camera default mode is landscape and we image needs to be in portrait mode
     *
     * @param file Image Uri
     * @throws IOException IOException
     */
    public void rotateImage(String file) throws IOException {

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, bounds);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bm = BitmapFactory.decodeFile(file, opts);

        // on Avaya Vantage, camera is by default in landscape mode so we need to rotate image
        int rotationAngle = 90;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);
        FileOutputStream fos = new FileOutputStream(file);
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
        fos.close();
    }

}
