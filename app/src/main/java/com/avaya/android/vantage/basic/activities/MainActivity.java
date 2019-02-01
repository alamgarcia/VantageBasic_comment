package com.avaya.android.vantage.basic.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.avaya.android.vantage.basic.BuildConfig;
import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.GoogleAnalyticsUtils;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.VantageDBHelper;
import com.avaya.android.vantage.basic.VantageDBHelper.VantageDBObserver;
import com.avaya.android.vantage.basic.adaptors.ICallControlsInterface;
import com.avaya.android.vantage.basic.adaptors.IHookListener;
import com.avaya.android.vantage.basic.adaptors.INameExtensionVisibilityInterface;
import com.avaya.android.vantage.basic.adaptors.UIAudioDeviceViewAdaptor;
import com.avaya.android.vantage.basic.adaptors.UICallViewAdaptor;
import com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor;
import com.avaya.android.vantage.basic.adaptors.UIDeskPhoneServiceAdaptor;
import com.avaya.android.vantage.basic.adaptors.UIVoiceMessageAdaptor;
import com.avaya.android.vantage.basic.csdk.CallAdaptor;
import com.avaya.android.vantage.basic.csdk.ConfigParametersNames;
import com.avaya.android.vantage.basic.csdk.ContactsLoader;
import com.avaya.android.vantage.basic.csdk.ErrorManager;
import com.avaya.android.vantage.basic.csdk.LocalContactInfo;
import com.avaya.android.vantage.basic.csdk.LocalContactsManager;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.ActiveCallFragment;
import com.avaya.android.vantage.basic.fragments.CallStatusFragment;
import com.avaya.android.vantage.basic.fragments.ContactDetailsFragment;
import com.avaya.android.vantage.basic.fragments.ContactEditFragment;
import com.avaya.android.vantage.basic.fragments.ContactViewAdaptorInterface;
import com.avaya.android.vantage.basic.fragments.ContactsFragment;
import com.avaya.android.vantage.basic.fragments.DialerFragment;
import com.avaya.android.vantage.basic.fragments.FavoritesFragment;
import com.avaya.android.vantage.basic.fragments.IncomingCallFragment;
import com.avaya.android.vantage.basic.fragments.OffHookTransduceButtonInterface;
import com.avaya.android.vantage.basic.fragments.OnActiveCallInteractionListener;
import com.avaya.android.vantage.basic.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.basic.fragments.RecentCallsFragment;
import com.avaya.android.vantage.basic.fragments.VideoCallFragment;
import com.avaya.android.vantage.basic.fragments.settings.ConfigChangeApplier;
import com.avaya.android.vantage.basic.model.CallData;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.model.UIAudioDevice;
import com.avaya.android.vantage.basic.model.UICall;
import com.avaya.android.vantage.basic.model.UICallState;
import com.avaya.android.vantage.basic.notifications.CallNotificationFactory;
import com.avaya.android.vantage.basic.notifications.NotificationService;
import com.avaya.android.vantage.basic.receiver.FinishCallDialerActivityReciver;
import com.avaya.android.vantage.basic.services.BluetoothStateService;
import com.avaya.android.vantage.basic.views.SlideAnimation;
import com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler;
import com.avaya.android.vantage.basic.views.adapters.NumberPickerAdapter;
import com.avaya.android.vantage.basic.views.adapters.SectionsPagerAdapter;
import com.avaya.android.vantage.basic.views.interfaces.IDeviceViewInterface;
import com.avaya.android.vantage.basic.views.interfaces.IHardButtonListener;
import com.avaya.android.vantage.basic.views.interfaces.ILoginListener;
import com.avaya.clientservices.contact.EditableContact;
import com.avaya.deskphoneservices.HardButtonType;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.slf4j.helpers.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.app.ActivityManager.LOCK_TASK_MODE_LOCKED;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
import static com.avaya.android.vantage.basic.Constants.ALL;
import static com.avaya.android.vantage.basic.Constants.CALL_ID;
import static com.avaya.android.vantage.basic.Constants.CONFERENCE_REQUEST_CODE;
import static com.avaya.android.vantage.basic.Constants.CONTACT_EDITING;
import static com.avaya.android.vantage.basic.Constants.DigitKeys;
import static com.avaya.android.vantage.basic.Constants.INCOMING_CALL_ACCEPT;
import static com.avaya.android.vantage.basic.Constants.TARGET;
import static com.avaya.android.vantage.basic.Constants.TRANSFER_REQUEST_CODE;
import static com.avaya.android.vantage.basic.UriUtil.getPhoneNumberFromTelURI;
import static com.avaya.android.vantage.basic.model.UICallState.ESTABLISHED;
import static com.avaya.android.vantage.basic.model.UICallState.FAILED;
import static com.avaya.android.vantage.basic.model.UICallState.REMOTE_ALERTING;
import static com.avaya.android.vantage.basic.model.UICallState.TRANSFERRING;

/**
 * Activity contains logic required for communication between mentioned fragments and
 * between fragments and CSDK.
 * MainActivity is base of Vantage Basic application which contains {@link DialerFragment},
 * {@link FavoritesFragment}, {@link ContactsFragment}, {@link ActiveCallFragment},
 * {@link RecentCallsFragment} and {@link ContactDetailsFragment}.
 */

public class MainActivity extends AppCompatActivity implements DialerFragment.OnDialerInteractionListener, OnContactInteractionListener,
        ContactDetailsFragment.OnContactDetailsInteractionListener, ContactEditFragment.OnContactEditInteractionListener,
        ContactViewAdaptorInterface, OnActiveCallInteractionListener, ICallControlsInterface,
        IncomingCallFragment.IncomingCallInteraction, View.OnClickListener, ILoginListener, IDeviceViewInterface, IHookListener,INameExtensionVisibilityInterface,
        RecentCallsFragment.OnFilterCallsInteractionListener,OffHookTransduceButtonInterface, IHardButtonListener {

    public static String DIALER_FRAGMENT = "DialerFragment";
    public static String FAVORITES_FRAGMENT = "FavoritesFragment";
    public static String CONTACTS_FRAGMENT = "ContactsFragment";
    public static String HISTORY_FRAGMENT = "HistoryFragment";
    public static String CONTACTS_DETAILS_FRAGMENT = "DetailsFragment";
    public static String CONTACTS_EDIT_FRAGMENT = "EditFragment";
    public static String ACTIVE_CALL_FRAGMENT = "ActiveCallFragment";
    public static String ACTIVE_VIDEO_CALL_FRAGMENT = "ActiveVideoCallFragment";

    private static final String TAG = "MainActivity";
    private static final int LAST_NAME_FIRST = Constants.LAST_NAME_FIRST;
    private static final String NAME_SORT_PREFERENCE = Constants.NAME_SORT_PREFERENCE;
    private static final String USER_PREFERENCE = Constants.USER_PREFERENCE;
    public static final String SERVICE_IMPACTING_CHANGE = "com.avaya.endpoint.action.SERVICE_IMPACTING_CHANGE";
    public static final String NON_SERVICE_IMPACTING_CHANGE = "com.avaya.endpoint.action.NON_SERVICE_IMPACTING_CHANGE";
    public static final String INCOMING_CALL_INTENT = "intent_incoming_call";
    public static final String BRING_TO_FOREGROUND_INTENT = "action.BRING_TO_FORGROUND_INTENT";
    public static final String EMERGENCY_CALL_INTENT = "android.intent.action.CALL";
    private static final String TAB_POSITION = "mainActivityTabPosition";
    public static final String BRAND_PREF = "brand";
    //Declare what is minimal amount of tabs which can be shown and selected
    private static final int MINIMAL_AMOUNT_OF_TABS = 2;
    public static final String SHOW_CALL_INTENT = "com.avaya.android.vantage.action.SHOW_CALL";
    private static final String CONNECTION_STATE_CHANGED ="android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED";
    private static final String ACTION_USB_ATTACHED  = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED  = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_FRONT = 1;
    public static final String HARD_BUTTON = "HARD_BUTTON";
    private boolean mFirstLoad = true;
    private boolean mFullyInitd = false;
    PowerManager.WakeLock mScreenLock = null;

    // handle config changes
    private Locale mLocale;
    private float mFontScale;
    private boolean isConfigChanged;
    //private boolean isCallInProgress;
    private boolean callWhileCallInProgress;

    // directory search variables
    private ContactsLoader mContactsLoader;

    private String mAdminNameDisplayOrder;
    private String mAdminNameSortOrder;
    private String mAdminChoiceRingtone;
    private String mPreviousAdminNameDisplayOrder;
    private String mPreviousAdminNameSortOrder;
    private String mPreviousAadminChoiceRingtone;

    public TextView mClosePicker;
    public TextView mPickContactTitle;

    private UICall mCall;

    private ImageView mBrandView;
    private boolean isAccessibility = false;

    private boolean mLoginGuardOneshot = false;

    private Tabs mContactCapableTab;


    private RelativeLayout tabOne;
    private ImageView tabImage;
    public ImageView tabSelector;
    public boolean showingFirst = false;
    public boolean isFilterOnContactTabEnabled = false;
    public boolean showingFirstRecent = true;
    private RelativeLayout tabSelectorWrapper;

    public ImageView searchButton;
    public ImageView addcontactButton;
    public ImageView filterButton;

    private long mLastClickTime = 0;

    private boolean isToBlockBakcPress = false;
    private boolean isOnKeyDownHappened = false;

    private FinishCallDialerActivityReciver finishCallDialerActivityReciver;
    private IntentFilter intentFilter;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    public SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    public ElanCustomViewPager mViewPager;
    private TextView mOptionUserSettings, mOptionUserAbout, mNumberPickerContactName;
    private UICallViewAdaptor mCallViewAdaptor;
    private CallStateEventHandler mCallStateEventHandler;
    public LinearLayout mToggleAudioMenu, mOptionBTHeadset,
            mOptionHeadset, mOptionHandset, mOptionSpeaker, mOption35Headset, mListPreferences, mSelectPhoneNumber;
    private ToggleButton mSelectAudio, mAudioMute, mVideoMute, mToggleAudioButton;
    public FrameLayout mFrameAll, mUser, mActiveCall, mEditContact, mPersistentControl;

    private SharedPreferences mSharedPref;
    private SharedPreferences mSharedPrefs;
    private SharedPreferences mCallPreference;
    private SharedPreferences mConnectionPref;
    private TextView mLoggedUserExtension;
    private TextView mLoggedUserNumber;
    private TabLayout mTabLayout;
    private ImageView mOpenUser;
    private ImageView mErrorStatus;
    private String mContactNumber;
    public LinearLayout mPickContacts;
    private LinearLayout mBlureFrame;
    private ListView mNumberPickerList;


    private LinearLayout mStatusLayout; //ELAN-1058
    private int mCallActiveCallID = -1;
    private boolean isConferenceCall;
    private String mRequestName;
    private int mActiveCallRequestCode;
    private boolean mIsOffHook = false;

    private Parcelable position;

    private String zeroOrPlus = "0";
    private static final String DIAL_ACTION = "android.intent.action.DIAL";
    /**
     * key up event received from platform via MEDIA_BUTTON intent
     *
     * @param hardButton
     */
    @Override
    public void onKeyUp(@NonNull HardButtonType hardButton) {
        //test for K155 special buttons:
        try {

            UIAudioDevice activeAudioDevice = mAudioDeviceViewAdaptor.getActiveAudioDevice();
            int activeCallId = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
            ToggleButton dialerOffHook = mSectionsPagerAdapter.getDialerFragment().offHook;

            KeyguardManager kgMgr = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            boolean isLocked = (kgMgr != null) && kgMgr.isDeviceLocked() && !ElanApplication.isPinAppLock;

            switch (hardButton) {

                case AUDIO_MUTE://video mute
                    if (mAudioMute.isEnabled()) {
                        Log.d(TAG, "PHYSICAL KEY AUDIO MUTE");
                        mAudioMute.performClick();
                    }
                    break;
                case VIDEO_MUTE://video mute
                    if (mVideoMute.isEnabled()) {
                        Log.d(TAG, "PHYSICAL KEY VIDEO MUTE");
                        mVideoMute.performClick();
                    }
                    break;
                case SPEAKER://speaker

                    Log.d(TAG, "PHYSICAL KEY SPEAKER HOOK");

                    if (isLocked && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0)) {
                        Log.v(TAG, "cancel off hook on lock");
                        return;
                    }

                    updateAudioSelectionUI(UIAudioDevice.SPEAKER);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.SPEAKER.toString());

                    if (!dialerOffHook.isChecked() && (activeCallId == 0)) {
                        if (SDKManager.getInstance().getCallAdaptor().isAlertingCall() == 0) {
                            prepareOffHook();
                        }
                        try {
                            if (isFragmentVisible(DIALER_FRAGMENT)) {
                                ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).offHook.performClick();
                                changeUiForFullScreenInLandscape(false);
                                if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                                    ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else if ((activeAudioDevice == UIAudioDevice.SPEAKER)) {


                        if (activeCallId > 0) {
                            SDKManager.getInstance().getCallAdaptor().endCall(activeCallId);
                            try {
                                if (isFragmentVisible(DIALER_FRAGMENT) && !isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)) {
                                    changeUiForFullScreenInLandscape(false);
                                    if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                                        ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                            try {
                                if (isFragmentVisible(DIALER_FRAGMENT))
                                    ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).setMode(DialerFragment.DialMode.EDIT);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        } else if (dialerOffHook.isChecked()) {
                            resetDialer();
                            mSectionsPagerAdapter.getDialerFragment().offHook.performClick();
                        }
                    } else {
                        Log.w(TAG, "onKeyUp SPEAKER: unexpected state activeCallId=" + activeCallId + " activeAudioDevice=" + activeAudioDevice + " dialerOffHook.isChecked()=" + dialerOffHook.isChecked());
                    }
                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.SPEAKER);

                    break;
                case HEADSET://transducer

                    Log.d(TAG, "PHYSICAL KEY TRANSDUCER SELECTION");

                    if (isLocked && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0)) {
                        Log.v(TAG, "cancel off hook on lock");
                        return;
                    }

                    int device = getHeadsetByPriority();
                    String prefValue = mSharedPref.getString(Constants.AUDIO_PREF_KEY, (UIAudioDevice.SPEAKER).toString());
                    UIAudioDevice savedDevice = UIAudioDevice.valueOf(prefValue.toUpperCase());
                    List<UIAudioDevice> devices = Arrays.asList(UIAudioDevice.BLUETOOTH_HEADSET, UIAudioDevice.RJ9_HEADSET, UIAudioDevice.WIRED_HEADSET);
                    if (devices.contains(savedDevice)) {
                        device = savedDevice.getUIId();
                    }
                    if (!dialerOffHook.isChecked() && (activeCallId == 0)) {
                        prepareOffHook();
                        this.onClick(findViewById(device));
                        try {
                            if (isFragmentVisible(DIALER_FRAGMENT)) {
                                ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).offHook.performClick();
                                changeUiForFullScreenInLandscape(false);
                                if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                                    ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else if (dialerOffHook.isChecked() && (activeAudioDevice != UIAudioDevice.SPEAKER) && (activeAudioDevice != UIAudioDevice.HANDSET) && (activeAudioDevice != UIAudioDevice.WIRELESS_HANDSET)) { //there is active call via Headset
                        if (activeCallId > 0) {
                            SDKManager.getInstance().getCallAdaptor().endCall(activeCallId);
                            try {
                                if (isFragmentVisible(DIALER_FRAGMENT)) {
                                    changeUiForFullScreenInLandscape(false);
                                    if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                                        ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        } else {
                            resetDialer();
                            try {
                                if (isFragmentVisible(DIALER_FRAGMENT)) {
                                    ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).offHook.performClick();
                                    changeUiForFullScreenInLandscape(false);
                                    if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                                        ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (activeCallId > 0 && (activeAudioDevice == UIAudioDevice.SPEAKER || activeAudioDevice == UIAudioDevice.HANDSET || activeAudioDevice == UIAudioDevice.WIRELESS_HANDSET)) { // there is an active call on speaker and headset was pressed
                        View transientView = new View(this);
                        transientView.setId(device);
                        onClick(transientView);
                    } else if (activeCallId > 0 && SDKManager.getInstance().getCallAdaptor().getCall(activeCallId).getState() == UICallState.NOT_RELEVANT) {
                        SDKManager.getInstance().getCallAdaptor().endCall(activeCallId);
                    } else {
                        Log.w(TAG, "onKeyUp HEADSET: unexpected state activeCallId=" + activeCallId + " activeAudioDevice=" + activeAudioDevice + " dialerOffHook.isChecked()=" + dialerOffHook.isChecked());
                    }

                    break;
            }
        }catch (NullPointerException e){
            Log.e(TAG, "NPE in onKeyUp(@NonNull HardButtonType hardButton):", e);
        }
    }
    @NonNull
    private int getHeadsetByPriority() {
        /*Bluetooth headset (if paired and connected)
        3.5mm wired headset (if connected)
        RJ9 headset (connection state is a don't care)*/
        int device = R.id.containerHeadset;
        for (UIAudioDevice uiAudioDevice : mAudioDeviceViewAdaptor.getAudioDeviceList()) {
            /*if (mIsOffHook && mOptionHandset.isEnabled()) {
                device = R.id.containerHandset;
                break;
            }*/
            if (uiAudioDevice == UIAudioDevice.BLUETOOTH_HEADSET) {
                device = R.id.containerBTHeadset;
                break;
            }
            if (uiAudioDevice == UIAudioDevice.WIRED_HEADSET) {
                device = R.id.container35Headset;
                break;
            }
        }

        return device;
    }

    /**
     * key down event received from platform via MEDIA_BUTTON intent
     *
     * @param hardButton
     */
    @Override
    public void onKeyDown(@NonNull HardButtonType hardButton) {
        if (!ElanApplication.isMainActivityVisible()) {

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(MainActivity.BRING_TO_FOREGROUND_INTENT);
            intent.putExtra(HARD_BUTTON, hardButton);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "failed to activate MainActivity from pending intent while it was not visible");
            }
            setIntent(null);
        }
    }

    private enum Tabs {Dialer, Favorites, Contacts, History}

    private HashMap<Tabs, Integer> mTabIndexMap = new HashMap<>();

    // Rect used for canceling keyboard on contact search.
    private Rect mSearchArea;
    // Shows status of fullscreen
    private boolean isSearchInProgress;

    /**
     * loading slide class
     * Animation cannot be reused, so we have to create one for every slide we want to create
     */
    private SlideAnimation mSlideSelecAudioDevice;
    private SlideAnimation mSlideUserPreferences;
    public SlideAnimation mSlideSelectPhoneNumber;


    private SharedPreferences mBrandPref;
    private UIContactsViewAdaptor mUIContactsViewAdaptor;
    private UIAudioDeviceViewAdaptor mAudioDeviceViewAdaptor;
    private UIDeskPhoneServiceAdaptor mUIDeskphoneServiceAdaptor;
    private CoordinatorLayout mCoordinatorLayout;
    private RelativeLayout mActivityLayout;

    private VantageDBObserver mSipUserDisplayObserver;
    private VantageDBObserver mSipUserNumberDisplayObserver;

    private Handler mHandler;
    private Runnable mLayoutCloseRunnable;

    private String mActivePhoneApp;

    private boolean mEmergencyWithoutLogin = false;

    CallNotificationFactory mNotifFactory;
    RecentCallsAndContactObserver mRecentCallAndContactObserver;

    private AppBarLayout appBar;

    private ImageView dialerView;
    public CallData.CallCategory mSelectedCallCategory = CallData.CallCategory.ALL;;

    boolean isToLockPressButton = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        finishCallDialerActivityReciver = new FinishCallDialerActivityReciver();

        intentFilter = new IntentFilter("com.avaya.endpoint.FINISH_CALL_ACTIVITY");

        if (!firstActivationCheck(getIntent())) {
            return;
        }
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        UiChangeListener();
        handleFirstLoadParams();

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        Utils.setDeviceMode(getApplicationContext());

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mLoggedUserExtension = (TextView) findViewById(R.id.extension);
        mLoggedUserNumber = (TextView) findViewById(R.id.number);
        mListPreferences = (LinearLayout) findViewById(R.id.preferences);
        mSelectAudio = (ToggleButton) findViewById(R.id.off_hook);
        mAudioMute = (ToggleButton) findViewById(R.id.audio_mute);
        mVideoMute = (ToggleButton) findViewById(R.id.video_mute);
        mOptionHandset = (LinearLayout) findViewById(R.id.containerHandset);
        mOptionBTHeadset = (LinearLayout) findViewById(R.id.containerBTHeadset);
        mOptionHeadset = (LinearLayout) findViewById(R.id.containerHeadset);
        mOption35Headset = (LinearLayout) findViewById(R.id.container35Headset);
        mOptionSpeaker = (LinearLayout) findViewById(R.id.containerSpeaker);
        mToggleAudioMenu = (LinearLayout) findViewById(R.id.selectAudioMenu);
        mOptionUserAbout = (TextView) findViewById(R.id.containerAbout);
        mOptionUserSettings = (TextView) findViewById(R.id.containerUserSettings);
        mFrameAll = (FrameLayout) findViewById(R.id.frameAll);

        mClosePicker = (TextView) findViewById(R.id.pick_cancel);
        mPickContactTitle = (TextView)  findViewById(R.id.pick_contact_title);

        mUser = (FrameLayout) findViewById(R.id.user);
        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) mUser.getLayoutParams();
        mUser.setVisibility(View.VISIBLE);

        mActiveCall = (FrameLayout) findViewById(R.id.active_call);
        mEditContact = (FrameLayout) findViewById(R.id.edit_contact_frame);

        mOpenUser = (ImageView) findViewById(R.id.open);
        mErrorStatus = (ImageView) findViewById(R.id.topBarError);
        mPickContacts = (LinearLayout) findViewById(R.id.pick_contacts);


        mToggleAudioButton = (ToggleButton) findViewById(R.id.transducer_button);

        mPersistentControl = (FrameLayout) findViewById(R.id.persistent_contrls_container);

        mBlureFrame = (LinearLayout) findViewById(R.id.blur_frame);

        mSelectPhoneNumber = (LinearLayout) findViewById(R.id.selectPhoneNumberContainer);
        mNumberPickerContactName = (TextView) findViewById(R.id.pickerContactName);
        mNumberPickerList = (ListView) findViewById(R.id.pickerContactList);

        mStatusLayout = (LinearLayout) findViewById(R.id.status_layout);//ELAN-1058

        appBar = (AppBarLayout) findViewById(R.id.appbar);

        dialerView = (ImageView) findViewById(R.id.dialer_tab);

        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            tabOne = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        }

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ElanCustomViewPager) findViewById(R.id.container);
        if (mViewPager != null) {
            mViewPager.setOffscreenPageLimit(4);
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.main_content);
        mActivityLayout = (RelativeLayout) findViewById(R.id.main_activity_layout);
        mContactsLoader = new ContactsLoader(this);

        if(getResources().getBoolean(R.bool.is_landscape) == true){
            searchButton = (ImageView) findViewById(R.id.search_button);
            addcontactButton = (ImageView) findViewById(R.id.addcontact_button);
            filterButton = (ImageView) findViewById(R.id.filterRecent);
            filterButton.setImageResource(R.drawable.ic_expand_more);
        }

        // setting up slide animations
        mSlideSelecAudioDevice = new SlideAnimation();
        mSlideUserPreferences = new SlideAnimation();
        mSlideSelectPhoneNumber = new SlideAnimation();
        mSlideUserPreferences.reDrawListener(mListPreferences);
        mSlideSelecAudioDevice.reDrawListener(mToggleAudioMenu);
        mSlideSelectPhoneNumber.reDrawListener(mSelectPhoneNumber);


        mSharedPref = getSharedPreferences("selectedAudioOption", MODE_PRIVATE);
        mCallPreference = getSharedPreferences(Constants.CALL_PREFS, MODE_PRIVATE);
        mSharedPrefs = getSharedPreferences(Constants.USER_PREFERENCE, Context.MODE_PRIVATE);
        mBrandPref = getSharedPreferences(BRAND_PREF, MODE_PRIVATE);
        mConnectionPref = getSharedPreferences(Constants.CONNECTION_PREFS, MODE_PRIVATE);

        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
            mVideoMute.setEnabled(true);
        } else {
            mVideoMute.setEnabled(false);
        }

        mTabLayout = (TabLayout) findViewById(R.id.tabs);

        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    Log.e("TabTest", "onTabSelected");
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    Log.e("TabTest", "onTabUnselected");
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    contactsTabFilterMenuListnerSetup(tabSelectorWrapper);
                }

            });
        }

        if (mTabLayout != null) {
            mTabLayout.setupWithViewPager(mViewPager);
        }

        setupOnClickListeners();
        // Refresh tab layout according to configuration
        mSectionsPagerAdapter.configureTabLayout();
        setTabIcons();
        loadBrand(getBrand());
        configureUserPreferenceAccess();
        // initialize the notification service
        initNotifications();
        initBluetoothChangeListener();
        initCSDK();
        loadAudioSelection();

        mAdminNameDisplayOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_DISPLAY_ORDER);
        mAdminNameSortOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_SORT_ORDER);
        mAdminChoiceRingtone = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.ADMIN_CHOICE_RINGTONE);

        mSectionsPagerAdapter.setLocalContacts(mContactsLoader);

        initLocalContactsDownload();
        mSectionsPagerAdapter.getLocalContacts().setUIContactsViewAdaptor(mUIContactsViewAdaptor);

        initViewPager();

        if (getResources().getBoolean(R.bool.is_landscape) == false) {
            if (false == Utils.isCameraSupported()) {
                mVideoMute.setVisibility(View.GONE);
            } else {
                mVideoMute.setVisibility(View.VISIBLE);
            }
        }else{
            mVideoMute.setVisibility(View.INVISIBLE);
        }

        mSipUserDisplayObserver = new VantageDBObserver(new Handler(), new ParameterUpdateRunnable(mLoggedUserExtension, VantageDBHelper.SIPUSERNAME), VantageDBHelper.SIPUSERNAME);
        getContentResolver().registerContentObserver(mSipUserDisplayObserver.getUri(), true, mSipUserDisplayObserver);

        mSipUserNumberDisplayObserver = new VantageDBObserver(new Handler(), new ParameterUpdateRunnable(mLoggedUserNumber, VantageDBHelper.SIPUSERNUMBER), VantageDBHelper.SIPUSERNUMBER);
        getContentResolver().registerContentObserver(mSipUserNumberDisplayObserver.getUri(), true, mSipUserNumberDisplayObserver);

        // check if accessibility is on and if is enable fullscreen dimensions.
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            isAccessibility = true;
            fullScreenViewResize(1000);
        } else {
            isAccessibility = false;
            fullScreenViewResize(1056);
        }
        // check if accessibility state is changed.
        accessibilityManager.addAccessibilityStateChangeListener(new AccessibilityManager.AccessibilityStateChangeListener() {
            @Override
            public void onAccessibilityStateChanged(boolean b) {
                isAccessibility = b;
                UiChangeListener();
            }
        });

        mHandler = new Handler();
        mLayoutCloseRunnable = new Runnable() {
            @Override
            public void run() {
                hideMenus();
            }
        };

        handleSpecialInitCases();

        mFullyInitd = true;

        mRecentCallAndContactObserver = new RecentCallsAndContactObserver(new Handler());

    }

    @Override
    protected void onDestroy() {
        if (mNotifFactory != null) mNotifFactory.unbindNotificationService();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(finishCallDialerActivityReciver!=null && intentFilter!=null)
            registerReceiver(finishCallDialerActivityReciver, intentFilter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_ATTACHED);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mBroadcastReceiver, filter);
        applyLockSetting();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);

        if(finishCallDialerActivityReciver!=null)
            unregisterReceiver(finishCallDialerActivityReciver);
    }

    public boolean isAccessibilityEnabled;
    public boolean isExploreByTouchEnabled;

    public void backToFullScreen(){
        if (!isAccessibility) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            fullScreenViewResize(1056);
        } else if (!ActiveCallFragment.IS_ACTIVE) {
            fullScreenViewResize(1000);
        }
    }

    @Override
    protected void onResume() {
        if (!isAccessibility) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            fullScreenViewResize(1056);
        } else if (!ActiveCallFragment.IS_ACTIVE) {
            fullScreenViewResize(1000);
        }

        super.onResume();

        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        isAccessibilityEnabled = am.isEnabled();
        isExploreByTouchEnabled = am.isTouchExplorationEnabled();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.LOCAL_CONFIG_CHANGE);
        intentFilter.addAction(Constants.SNACKBAR_SHOW);
        LocalBroadcastManager.getInstance(this).registerReceiver(mConfigChangeAndSnackbarReceiver, intentFilter);
        checkForErrors();
        ElanApplication.setIsMainActivityVisible(true);

        IntentFilter filterBluetooth = new IntentFilter();
        filterBluetooth.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filterBluetooth.addAction(CONNECTION_STATE_CHANGED);

        if (ElanApplication.isConfigChange()) {
            applyConfigChange();
            //reset config change flag
            ElanApplication.setApplyConfigChange(false);
            Log.v(TAG, "done applying postponed configchanges in onResume");
        }

        if (mCallStateEventHandler != null) {
            mCallStateEventHandler.onActivityResume();
        }
        if (mSipUserDisplayObserver != null) {
            getContentResolver().unregisterContentObserver(mSipUserDisplayObserver);
            getContentResolver().registerContentObserver(mSipUserDisplayObserver.getUri(), true, mSipUserDisplayObserver);
        }
        String name = VantageDBHelper.getParameter(getContentResolver(), VantageDBHelper.SIPUSERNAME);
        if ((name != null) && (mLoggedUserExtension != null)) {
            mLoggedUserExtension.setText(name);
        }

        if (mSipUserNumberDisplayObserver != null) {
            getContentResolver().unregisterContentObserver(mSipUserNumberDisplayObserver);
            getContentResolver().registerContentObserver(mSipUserNumberDisplayObserver.getUri(), true, mSipUserNumberDisplayObserver);
        }
        String number = VantageDBHelper.getParameter(getContentResolver(), VantageDBHelper.SIPUSERNUMBER);
        if ((number != null) && (mLoggedUserNumber != null)) {
            mLoggedUserNumber.setText(number);
        }

        mActivePhoneApp = VantageDBHelper.getParameter(getContentResolver(), VantageDBHelper.ACTIVE_CSDK_BASED_PHONE_APP);
        if (mActivePhoneApp != null && !mActivePhoneApp.equals(BuildConfig.APPLICATION_ID)) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.default_app_closing))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }

                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .show();
        }

        if(mRecentCallAndContactObserver != null) {
            getContentResolver().registerContentObserver(android.provider.CallLog.Calls.CONTENT_URI, true, mRecentCallAndContactObserver);
            getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, mRecentCallAndContactObserver);
        }

        restoreIncomingCalls();

    }

    @Override
    public void setNameExtensionVisibility(int extensionNameDisplayOption){

        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) mUser.getLayoutParams();
        mUser.setVisibility(View.VISIBLE);

        int extensionOption = extensionNameDisplayOption;
        Log.d(TAG, "setNameExtensionVisibility to " + extensionOption);
        switch (extensionOption) {
            case 0:
                mLoggedUserExtension.setVisibility(View.VISIBLE);
                mLoggedUserNumber.setVisibility(View.VISIBLE);
                break;
            case 1:
                mLoggedUserExtension.setVisibility(View.VISIBLE);
                mLoggedUserNumber.setVisibility(View.GONE);
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mUser.setLayoutParams(params);
                break;
            case 2:
                mLoggedUserExtension.setVisibility(View.GONE);
                mLoggedUserNumber.setVisibility(View.VISIBLE);
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mUser.setLayoutParams(params);
                break;
            case 3:
                mStatusLayout.setVisibility(View.GONE);
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mUser.setLayoutParams(params);
                break;
        }

    }

    /**
     * Modifies the UI to adopt screen orientation
     * @param show
     */
    public void changeUiForFullScreenInLandscape(boolean show){
        try {
            if (getResources().getBoolean(R.bool.is_landscape) == true) {
                if (show) {
                    mBrandView.setVisibility(View.GONE);
                    mUser.setVisibility(View.GONE);
                    mStatusLayout.setVisibility(View.GONE);
                    mOpenUser.setVisibility(View.GONE);
                    mErrorStatus.setVisibility(View.GONE);
                    appBar.setVisibility(View.INVISIBLE);
                    appBar.getLayoutParams().height = 0;
                    mTabLayout.setVisibility(View.GONE);
                    mTabLayout.getLayoutParams().height = 0;
                    dialerView.setVisibility(View.GONE);
                    mViewPager.getLayoutParams().height = 675;
                } else {
                    mBrandView.setVisibility(View.VISIBLE);
                    mUser.setVisibility(View.VISIBLE);
                    mStatusLayout.setVisibility(View.VISIBLE);
                    mOpenUser.setVisibility(View.VISIBLE);
                    mErrorStatus.setVisibility(View.VISIBLE);
                    appBar.setVisibility(View.VISIBLE);
                    appBar.getLayoutParams().height = 100;
                    mTabLayout.setVisibility(View.VISIBLE);
                    mTabLayout.getLayoutParams().height = 100;
                    dialerView.setVisibility(View.GONE);
                    mViewPager.getLayoutParams().height = 430;

                }
            }
            checkForErrors();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void changeCallStatusVisibility(int visibility){
        CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
        if(visibility==View.VISIBLE) {
            callStatusFragment.showCallStatus();
        }else{
            callStatusFragment.hideCallStatus();
            Utils.hideKeyboard(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mConfigChangeAndSnackbarReceiver);

        if (mSipUserDisplayObserver != null) {
            getContentResolver().unregisterContentObserver(mSipUserDisplayObserver);
        }
        if (mSipUserNumberDisplayObserver != null) {
            getContentResolver().unregisterContentObserver(mSipUserNumberDisplayObserver);
        }
        ElanApplication.setIsMainActivityVisible(false);

        if (mCallStateEventHandler != null) {
            mCallStateEventHandler.onActivityPause();
        }
        if(getContentResolver() != null && mRecentCallAndContactObserver != null)
            getContentResolver().unregisterContentObserver(mRecentCallAndContactObserver);
    }

    /**
     * Load contacts in case it is first run of application or in case it was already
     * started perform reloading of data
     */
    private void initLocalContactsDownload() {
        if (mFirstLoad) {
            getLoaderManager().initLoader(Constants.LOCAL_ADDRESS_LOADER, null, mSectionsPagerAdapter.getLocalContacts());
            mFirstLoad = false;
        } else {
            getLoaderManager().restartLoader(Constants.LOCAL_CONTACTS_LOADER, null, mSectionsPagerAdapter.getLocalContacts());
            getLoaderManager().restartLoader(Constants.LOCAL_ADDRESS_LOADER, null, mSectionsPagerAdapter.getLocalContacts());
            getLoaderManager().restartLoader(Constants.LOCAL_NAME_LOADER, null, mSectionsPagerAdapter.getLocalContacts());
        }
    }

    /**
     * Check if mUser has accepted EULA and if application is supported on device.
     * Same Preference is reused since mUser can't move to application without accepting EULA.
     *
     * @return Continue with application loading.
     * Application will be activated if mUser have accepted EULA.
     */
    private boolean firstActivationCheck(Intent intent) {
        SharedPreferences preferences = getSharedPreferences(Constants.EULA_PREFS_NAME, MODE_PRIVATE);
        boolean eulaAccepted = preferences.getBoolean(Constants.KEY_EULA_ACCEPTED, false);
        if (!eulaAccepted) {
            if (!isDeviceSupported()) {
                showDeviceNotSupportedAlert();
            } else {

                // check whether this is first time activation, but the emergency logic has to be activated
                if ((intent != null) && (intent.getAction() != null) && intent.getAction().equalsIgnoreCase(EMERGENCY_CALL_INTENT)) {
                    final Uri telData = intent.getData();
                    final String toNum = (telData == null) ? "" : PhoneNumberUtils
                            .stripSeparators(getPhoneNumberFromTelURI(Uri.decode(telData.toString())));
                    mEmergencyWithoutLogin = SDKManager.getInstance().getDeskPhoneServiceAdaptor().isEmergencyNumber(toNum);
                    if (mEmergencyWithoutLogin) {
                        // this is first time activation, but it is emergency case - proceed with the application without Legal part
                        return true;
                    }
                }

                checkForLegal();
            }
            return false;
        }
        // If application is running for the first time, shortcut will be made.
        return true;
    }

    /**
     * Show dialog alert if device is not supported in Client SDK.
     */
    private void showDeviceNotSupportedAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle(getString(R.string.app_name));
        alertDialogBuilder
                .setMessage(R.string.device_not_supported)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Disable application usage on non supported devices
     * Only K165 and K175 models are supported atm.
     *
     * @return Device is supported.
     */
    private boolean isDeviceSupported() {
        //noinspection RedundantIfStatement
        if (Build.MODEL.equals("K165") || Build.MODEL.equals("K175")
                || Build.MODEL.equals("Vantage")
                || Build.MODEL.startsWith("Avaya Vantage")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Close MainActivity and open EULA screen if mUser hasn't accepted EULA.
     */
    private void checkForLegal() {
        //if MainLegalActivity is already started, there is not need to start it again
        ActivityManager activityManager = getSystemService(ActivityManager.class);
        if (activityManager.getAppTasks().size() != 0 && activityManager.getAppTasks().get(0).getTaskInfo().baseActivity.getClassName().equals(MainLegalActivity.class.getName())) {
            finish();
            return;
        }
        Intent legalIntent = new Intent(this, MainLegalActivity.class);
        legalIntent.putExtra("startFromSettings", false);
        startActivity(legalIntent);
        finish();
    }

    /**
     * Checking which fragment is active
     */
    private void initViewPager() {
        mUIDeskphoneServiceAdaptor.setHookListener(this);
        mUIDeskphoneServiceAdaptor.setHardButtonListener(this);
        Intent intent = getIntent();
        int oldPosition = intent.getIntExtra(TAB_POSITION, 0);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if(getResources().getBoolean(R.bool.is_landscape) == true && ( isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) || isFragmentVisible(CONTACTS_EDIT_FRAGMENT) )){
                    mViewPager.setEnabledSwipe(false);
                }else{
                    mViewPager.setEnabledSwipe(true);
                }
            }

            @Override
            public void onPageSelected(int position) {
                Utils.hideKeyboard(MainActivity.this);
                Tabs selectedTab = Tabs.Dialer;
                for (Tabs t : mTabIndexMap.keySet()) {
                    if (position == mTabIndexMap.get(t)) {
                        selectedTab = t;
                    }
                }
                if(selectedTab!= Tabs.Dialer){
                    mContactCapableTab = selectedTab;
                }

                if(getResources().getBoolean(R.bool.is_landscape) && mSectionsPagerAdapter.getFragmentContacts()!=null && mSectionsPagerAdapter.getFragmentContacts().mSearchView!=null && mSectionsPagerAdapter.getFragmentContacts().mSearchView.getVisibility() == View.VISIBLE){
                    if(isFragmentVisible(CONTACTS_FRAGMENT)&& !isFragmentVisible(CONTACTS_EDIT_FRAGMENT))
                        mSectionsPagerAdapter.getFragmentContacts().removeSearchResults();
                }

                switch (selectedTab) {
                    case Dialer:
                        if (mSectionsPagerAdapter.getDialerFragment() != null) {
                            mSectionsPagerAdapter.getDialerFragment().fragmentSelected(mSectionsPagerAdapter.getVoiceMessageAdaptor().voiceMsgsState());
                        }
                        if(getResources().getBoolean(R.bool.is_landscape)){
                            searchButton.setVisibility(View.INVISIBLE);
                            addcontactButton.setVisibility(View.INVISIBLE);
                            filterButton.setVisibility(View.INVISIBLE);
                        }

                        break;
                    case Favorites:
                        if (mSectionsPagerAdapter.getFragmentFavorites() == null) {
                            mSectionsPagerAdapter.setFragmentFavorites(FavoritesFragment.newInstance(mSectionsPagerAdapter.isCallAddParticipant()));
                            tabLayoutReset();
                        }
                        mSectionsPagerAdapter.getFragmentFavorites().fragmentSelected();
                        if(getResources().getBoolean(R.bool.is_landscape)){
                            searchButton.setVisibility(View.INVISIBLE);
                            addcontactButton.setVisibility(View.INVISIBLE);
                            filterButton.setVisibility(View.INVISIBLE);
                        }

                        break;
                    case Contacts:
                        if (mSectionsPagerAdapter.getFragmentContacts() == null) {
                            mSectionsPagerAdapter.setFragmentContacts(ContactsFragment.newInstance(1, mSectionsPagerAdapter.isCallAddParticipant(),false));
                            tabLayoutReset();
                        }
                        if(mSectionsPagerAdapter.getFragmentContacts().mUIContactsAdaptor !=null && mSectionsPagerAdapter.getFragmentContacts().prevFilterSelection != 0) {

                            mSectionsPagerAdapter.getFragmentContacts().mUIContactsAdaptor.setContactDisplaySelection(mSectionsPagerAdapter.getFragmentContacts().prevFilterSelection);
                        }
                        mSectionsPagerAdapter.getFragmentContacts().fragmentSelected();
                        if(getResources().getBoolean(R.bool.is_landscape)){
                            searchButton.setVisibility(View.VISIBLE);
                            addcontactButton.setVisibility(View.VISIBLE);
                            filterButton.setVisibility(View.INVISIBLE);
                        }

                        break;
                    case History:
                        if (mSectionsPagerAdapter.getFragmentRecent() == null) {
                            mSectionsPagerAdapter.setFragmentRecent(RecentCallsFragment.newInstance(1, mSectionsPagerAdapter.isCallAddParticipant()));
                            tabLayoutReset();
                        }
                        mSectionsPagerAdapter.getFragmentRecent().fragmentSelected(mSelectedCallCategory);
                        if (mFullyInitd)
                            resetMissedCalls();
                        if(getResources().getBoolean(R.bool.is_landscape)){
                            searchButton.setVisibility(View.INVISIBLE);
                            addcontactButton.setVisibility(View.INVISIBLE);
                            filterButton.setVisibility(View.VISIBLE);
                        }

                        break;
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //Make sure placeholder fragments are created
        for (int i = 0; i < mTabIndexMap.size(); i++) {
            mViewPager.setCurrentItem(i, false);
        }

        mViewPager.setCurrentItem(oldPosition, false);

    }

    /**
     * Reset TabLayout in case our fragment is null
     * due to unpredicted reasons.
     */
    public void tabLayoutReset() {
        try {
            DialerFragment dialerFragment = mSectionsPagerAdapter.getDialerFragment();
            mTabLayout.removeAllTabs();
            if (mSectionsPagerAdapter.getCount() != 0) {
                mSectionsPagerAdapter.setAllowReconfiguration(true);
                mSectionsPagerAdapter.notifyDataSetChanged();
                mSectionsPagerAdapter.setAllowReconfiguration(false);
            } else {
                mSectionsPagerAdapter.configureTabLayout();
            }
            mViewPager.setAdapter(mSectionsPagerAdapter);
            mTabLayout.setupWithViewPager(mViewPager);
            setTabIcons();

            if ( mSectionsPagerAdapter.getDialerFragment() != null && dialerFragment != null) {
                mSectionsPagerAdapter.getDialerFragment().setMode(dialerFragment.getMode());
            }
            if(isFragmentVisible(CONTACTS_EDIT_FRAGMENT))
                ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)).cancelOnClickListener();
        }catch (Exception e){
                e.printStackTrace();
        }

    }

    /**
     * Change missed call counter to 0 and refresh tab icon.
     */
    private void resetMissedCalls() {
        if(mNotifFactory != null) {
            mNotifFactory.removeAll();
        }
        if (mCallPreference != null && mCallPreference.getInt(Constants.KEY_UNSEEN_MISSED_CALLS, 0) > 0) {
            SharedPreferences.Editor editor = mCallPreference.edit();
            editor.putInt(Constants.KEY_UNSEEN_MISSED_CALLS, 0);
            editor.apply();
            setTabIcons();
        }
    }

    /**
     * this method is used to setup onClickListener for objects
     */
    private void setupOnClickListeners() {

        mToggleAudioMenu.setOnClickListener(this);
        mSelectAudio.setOnClickListener(this);
        mOptionHandset.setOnClickListener(this);
        mOptionBTHeadset.setOnClickListener(this);
        mOptionHeadset.setOnClickListener(this);
        mOption35Headset.setOnClickListener(this);
        mOptionSpeaker.setOnClickListener(this);
        mOptionUserAbout.setOnClickListener(this);
        mOptionUserSettings.setOnClickListener(this);
        mFrameAll.setOnClickListener(this);
        mUser.setOnClickListener(this);
        mAudioMute.setOnClickListener(this);
        mVideoMute.setOnClickListener(this);
        mVideoMute.setClickable(false);
        mClosePicker.setOnClickListener(this);

        mToggleAudioButton.setOnClickListener(this);
        mErrorStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ErrorMessageActivity.class);
                startActivity(intent);
            }
        });

        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            addcontactButton.setOnClickListener(this);
            filterButton.setOnClickListener(this);
            searchButton.setOnClickListener(this);
        }

    }

    /**
     * Preparing and showing list of available audio devices
     *
     * @param view {@link View} to be shown
     */
    public void showAudioList(View view) {
        List<UIAudioDevice> deviceList = new ArrayList<>();
        boolean shouldDisplayHanset = false;
        boolean shouldDisplay35Headset = false;
        boolean shouldDisplayHeadset = false;
        boolean shouldDisplayBTHeadset = false;
        if(mAudioDeviceViewAdaptor.getAudioDeviceList()!=null){
            deviceList = mAudioDeviceViewAdaptor.getAudioDeviceList();
            shouldDisplayHanset = (deviceList.contains(UIAudioDevice.HANDSET) || deviceList.contains(UIAudioDevice.WIRELESS_HANDSET)) && mAudioDeviceViewAdaptor.isDeviceOffHook();
            shouldDisplay35Headset = deviceList.contains(UIAudioDevice.WIRED_HEADSET);
            shouldDisplayHeadset = deviceList.contains(UIAudioDevice.RJ9_HEADSET);
            shouldDisplayBTHeadset = deviceList.contains(UIAudioDevice.BLUETOOTH_HEADSET);

        }


        view.findViewById(R.id.containerHandset).setEnabled(shouldDisplayHanset);
        view.findViewById(R.id.handset_image_view).setEnabled(shouldDisplayHanset);
        view.findViewById(R.id.handset_text_view).setEnabled(shouldDisplayHanset);

        view.findViewById(R.id.container35Headset).setEnabled(shouldDisplay35Headset);
        view.findViewById(R.id.headset35_image_view).setEnabled(shouldDisplay35Headset);
        view.findViewById(R.id.headset35_text_view).setEnabled(shouldDisplay35Headset);

        view.findViewById(R.id.containerHeadset).setEnabled(shouldDisplayHeadset);
        view.findViewById(R.id.headset_image_view).setEnabled(shouldDisplayHeadset);
        view.findViewById(R.id.headset_text_view).setEnabled(shouldDisplayHeadset);

        view.findViewById(R.id.containerBTHeadset).setEnabled(shouldDisplayBTHeadset);
        view.findViewById(R.id.bt_headset_image_view).setEnabled(shouldDisplayBTHeadset);
        view.findViewById(R.id.bt_headset_text_view).setEnabled(shouldDisplayBTHeadset);
    }

    /**
     * Initializing and pre-loading all necessary CSDK listeners,
     * preparing adaptors and state handlers.
     */
    private void initCSDK() {

        Log.d(TAG, "initCSDK");

        // loading name display preference
        SharedPreferences mSortPreference = getSharedPreferences(USER_PREFERENCE, MODE_PRIVATE);
        int nameSortPreference = mSortPreference.getInt(NAME_SORT_PREFERENCE, LAST_NAME_FIRST);

        if (nameSortPreference == LAST_NAME_FIRST) {
            SDKManager.getInstance().displayFirstNameFirst(false);
        } else {
            SDKManager.getInstance().displayFirstNameFirst(true);
        }

        mUIContactsViewAdaptor = new UIContactsViewAdaptor();
        mSectionsPagerAdapter.setContactsViewAdaptor(mUIContactsViewAdaptor);
        SDKManager.getInstance().getContactsAdaptor().registerListener(mUIContactsViewAdaptor);

        mCallViewAdaptor = new UICallViewAdaptor();
        SDKManager.getInstance().getCallAdaptor().registerListener(mCallViewAdaptor);
        mCallViewAdaptor.setCallControlsInterface(this);
        mCallStateEventHandler = new CallStateEventHandler(getSupportFragmentManager(), mCallViewAdaptor, this);

        mUIDeskphoneServiceAdaptor = new UIDeskPhoneServiceAdaptor(getApplicationContext(), this,this);
        SDKManager.getInstance().getDeskPhoneServiceAdaptor().registerListener(mUIDeskphoneServiceAdaptor);

        mAudioDeviceViewAdaptor = new UIAudioDeviceViewAdaptor();
        SDKManager.getInstance().getAudioDeviceAdaptor().registerListener(mAudioDeviceViewAdaptor);
        mAudioDeviceViewAdaptor.setDeviceViewInterface(this);

        SDKManager.getInstance().getHistoryAdaptor().registerListener(mSectionsPagerAdapter.getHistoryViewAdaptor());
        SDKManager.getInstance().getHistoryAdaptor().setCallNotificationFactory();//ELAN-1000

        mSectionsPagerAdapter.setVoiceMessageAdaptor(new UIVoiceMessageAdaptor(mSectionsPagerAdapter.getDialerFragment()));
        SDKManager.getInstance().getVoiceMessageAdaptor().registerListener(mSectionsPagerAdapter.getVoiceMessageAdaptor());

        // TODO used for testing. Need to run check for errors from adapter on error appearance.
//        ErrorManager.getInstance().addErrorToList(3);
//        ErrorManager.getInstance().addErrorToList(8);
        checkForErrors();

    }

    /**
     * Setup starting params.
     * Used for config changes in runtime.
     */
    private void handleFirstLoadParams() {
        mLocale = getResources().getConfiguration().locale;
        mFontScale = getResources().getConfiguration().fontScale;
    }

    /**
     * Handle runtime config changes.
     * Restart all views by restarting activity.
     */
    private void handleConfigChanges() {
        if (mViewPager!=null && isConfigChanged && SDKManager.getInstance().getCallAdaptor().getActiveCallId() == 0) {
            Intent intent = getIntent();
            intent.putExtra(TAB_POSITION, mViewPager.getCurrentItem());
            finish();
            startActivity(intent);
        }
    }

    /**
     * Set up Fullscreen mode of applications
     * Change dimensions of view
     */
    public void UiChangeListener() {
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                } else {
                    if (!isAccessibility) {
                        fullScreenViewResize(1056);
                    }
                }
            }
        });
    }

    /**
     * Change views dimension when fullscreen mode is setup
     *
     * @param startDimension start layout dimension in pixel
     */
    private void fullScreenViewResize(int startDimension) {
        if(mViewPager == null) {
            Log.w(TAG, "fullScreenViewResize was called before activity views were created");
            return;
        }
        if(getResources().getBoolean(R.bool.is_landscape) == false) {
            if(mViewPager!=null && mViewPager.getLayoutParams()!=null)
                mViewPager.getLayoutParams().height = startDimension - 136;
        }else{

            boolean isToResize = false;
            FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            if(fragments != null){
                for(Fragment fragment : fragments){
                    if(fragment != null && fragment.isVisible() && fragment instanceof ContactDetailsFragment ) {
                        isToResize = true;
                        break;
                    }else if ( fragment != null && fragment.isVisible() && fragment instanceof ContactsFragment  ){
                        if( ((ContactsFragment) fragment).seacrhLayout.getVisibility() == View.VISIBLE ){
                            isToResize = true;
                            break;
                        }
                    }
                }
            }
            if(!isToResize) {
                if(mViewPager!=null && mViewPager.getLayoutParams()!=null)
                    mViewPager.getLayoutParams().height = 430;
            }
        }
        mCoordinatorLayout.getLayoutParams().height = startDimension - 48;
        mActiveCall.getLayoutParams().height = startDimension - 48;
        mFrameAll.getLayoutParams().height = startDimension - 48;
        mEditContact.getLayoutParams().height = startDimension - 28;

    }

    /**
     * Checks if any SDK error code is active.
     * If true, show error notification image on top.
     */
    private void checkForErrors() {
        boolean[] errorList = ErrorManager.getInstance().getErrorList();

        if (mErrorStatus == null) {
            return;
        }
        mErrorStatus.setVisibility(View.GONE);
        for (int errorCode = 0; errorCode < errorList.length; errorCode++) {
            if (errorList[errorCode]) {
                mErrorStatus.setVisibility(View.VISIBLE);
                mNotifFactory.showOnLine(ErrorManager.getErrorMessage(getApplicationContext(), errorCode));
                return;
            }
        }
    }

    /**
     * Provide us with brand logo URL in form of String from Config Parameters
     *
     * @return String with logo URL in case is available or empty String if not available
     */
    @NonNull
    private String getBrand() {
        //TODO: get brand url from settings
        String brandUrl = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.BRAND_URL);
        if (TextUtils.isEmpty(brandUrl)) {
            return "";
        } else {
            return brandUrl;
        }
    }

    /**
     * Perform loading of brand logo image in brand logo ImageView
     *
     * @param url of logo image
     */
    private void loadBrand(String url) {
        String cachedUrl;
        mBrandView = (ImageView) findViewById(R.id.brand);
        if (mBrandPref.contains(ConfigParametersNames.BRAND_URL.getName())) {
            cachedUrl = mBrandPref.getString(ConfigParametersNames.BRAND_URL.getName(), "");
            if (TextUtils.isEmpty(url)) {
                url = cachedUrl;
            }
        }
        if (!TextUtils.isEmpty(url) && url.startsWith("http")) {
            if (mBrandView != null) {
                Glide.clear(mBrandView);
                if (url.endsWith("gif")) {
                    Glide.with(this).load(url).asGif().fitCenter().error(R.drawable.ic_branding_avaya).diskCacheStrategy(DiskCacheStrategy.SOURCE).crossFade().into(mBrandView);
                } else {
                    Glide.with(this).load(url).asBitmap().error(R.drawable.ic_branding_avaya).fitCenter().into(mBrandView);
                }
            }

            mBrandPref.edit().putString(ConfigParametersNames.BRAND_URL.getName(), url).apply();
        } else {
            if (mBrandView != null) {
                mBrandView.setImageResource(R.drawable.ic_branding_avaya);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        isOnKeyDownHappened = true;
        if (getResources().getBoolean(R.bool.is_landscape) == false && mViewPager!=null && mViewPager.getCurrentItem() == 0) {
            try {
                int keyunicode = event.getUnicodeChar(event.getMetaState());
                char character = (char) keyunicode;
                String digit = "" + character;
                if (isFragmentVisible(DIALER_FRAGMENT) && mSectionsPagerAdapter != null && mSectionsPagerAdapter.getDialerFragment() != null) {
                    if (digit.matches("[\\d]") || digit.matches("\\#") || digit.matches("\\*")) {
                        mSectionsPagerAdapter.getDialerFragment().dialFromKeyboard(digit);
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        mSectionsPagerAdapter.getDialerFragment().deleteDigit();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }else if (getResources().getBoolean(R.bool.is_landscape) == true  && !isToLockPressButton) {
            try {
                if(isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)||isFragmentVisible(CONTACTS_EDIT_FRAGMENT)){
                    isToBlockBakcPress = true;
                }

                isToLockPressButton = true;
                int keyunicode = event.getUnicodeChar(event.getMetaState());
                char character = (char) keyunicode;
                String digit = "" + character;

                if(keyCode == KeyEvent.KEYCODE_BACK && !isLockState(this)) {


                    if (mSelectPhoneNumber.getVisibility() == View.VISIBLE) {
                        if(getResources().getBoolean(R.bool.is_landscape) == true) {
                            mSlideSelectPhoneNumber.collapse(mSelectPhoneNumber, true);
                        }else {
                            mSlideSelectPhoneNumber.collapse(mSelectPhoneNumber, false);
                        }
                    }

                    try {
                        if (((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mMoreCallFeatures.getVisibility() == View.VISIBLE) {
                            ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mMoreCallFeaturesClick();
                            return true;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    mViewPager.setEnabledSwipe(true);
                    if (!isFragmentVisible(ACTIVE_CALL_FRAGMENT) && !isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)) {
                        changeUiForFullScreenInLandscape(false);


                        if (isFragmentVisible(CONTACTS_EDIT_FRAGMENT)) {
                            ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)).cancelOnClickListener();
                            //changeUiForFullScreenInLandscape(true);
                        } else if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)) {
                            ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)).mDeletePopUp.setVisibility(View.GONE);
                            ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)).mBackListener.back();
                            changeUiForFullScreenInLandscape(false);

                            CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                            int isActiveCall = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
                            if( (callStatusFragment.getView().getVisibility()==View.INVISIBLE || callStatusFragment.getView().getVisibility()==View.GONE) && isActiveCall!=0 ) {
                                callStatusFragment.showCallStatus();
                            }
                        }

                        if(isFragmentVisible(CONTACTS_FRAGMENT) == true && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout.getVisibility() == View.VISIBLE){

                            ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout.setVisibility(View.GONE);
                            ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).removeSearchResults();

                            CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                            if( (callStatusFragment.getView().getVisibility()==View.INVISIBLE || callStatusFragment.getView().getVisibility()==View.GONE) && SDKManager.getInstance().getCallAdaptor().hasActiveHeldOrInitiatingCall() ) {
                                callStatusFragment.showCallStatus();
                            }
                            return false;
                        }
                    }else if (mCallViewAdaptor.getNumOfCalls() < CallAdaptor.MAX_NUM_CALLS){
                        ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mBackArrowOnClickListener();
                        changeUiForFullScreenInLandscape(false);
                        return true;
                    }
                }


                if(isFragmentVisible(DIALER_FRAGMENT) && mSectionsPagerAdapter!=null && mSectionsPagerAdapter.getDialerFragment()!=null ) {
                    if (digit.matches("[\\d]") || digit.matches("\\#") || digit.matches("\\*")) {
                        if(isFragmentVisible(CONTACTS_EDIT_FRAGMENT)==false)
                            mSectionsPagerAdapter.getDialerFragment().dialFromKeyboard(digit);
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        mSectionsPagerAdapter.getDialerFragment().deleteDigit();
                    }
                }

                if (DigitKeys.contains(event.getKeyCode()) &&
                        (   ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT))==null || ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT))!=null &&((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout.getVisibility()!=View.VISIBLE   ) ) {

                    if (keyCode != KeyEvent.KEYCODE_0) {
                       // mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(digit);
                        if( ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)) !=null && !((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mHoldCallButton.isChecked()) {
                            ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).sendDTMF(digit.charAt(0));
                        }else{
                            if(isFragmentVisible(CONTACTS_EDIT_FRAGMENT)==false)
                                mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(digit);
                        }
                    }else if (keyCode == KeyEvent.KEYCODE_0){
                        zeroOrPlus = "0";
                    }

                        mViewPager.setCurrentItem(0, false);
                    searchButton.setVisibility(View.INVISIBLE);
                    addcontactButton.setVisibility(View.INVISIBLE);

                    try {
                        if (isFragmentVisible(DIALER_FRAGMENT) && isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) ) {
                            changeUiForFullScreenInLandscape(false);
                            ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                    if (isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT) || isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) || isFragmentVisible(CONTACTS_EDIT_FRAGMENT)) {
                        changeUiForFullScreenInLandscape(true);
                    }

                    event.startTracking();
                    return true;
                }else if (((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT))!=null && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout !=null && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout.getVisibility()==View.VISIBLE  ) {
                    ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).mSearchView.requestFocus();
                    Utils.openKeyboard(this);
                }


            }catch (Exception e){
                e.printStackTrace();
            }

        }
        if(DigitKeys.contains(event.getKeyCode()))
            return true;

        if ((event.getKeyCode() == KeyEvent.KEYCODE_MUTE)) {
            return false;
        }

        return super.onKeyDown(keyCode, event);

    }

    /**
     * @param fragmentName name of the fragment
     * @return instance of the fragment if it is visible
     */
    private Fragment getVisibleFragment(String fragmentName){
        try {
            FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            if (fragments != null) {
                for (Fragment fragment : fragments) {
                    if (fragment != null && fragment.isVisible()) {
                        if (fragmentName.equalsIgnoreCase(DIALER_FRAGMENT)) {
                            if (fragment instanceof DialerFragment) {
                                return fragment;
                            }
                        }else if (fragmentName.equalsIgnoreCase(FAVORITES_FRAGMENT)) {
                            if (fragment instanceof FavoritesFragment) {
                                return fragment;
                            }
                        }else if (fragmentName.equalsIgnoreCase(CONTACTS_FRAGMENT)) {
                            if (fragment instanceof ContactsFragment) {
                                return fragment;
                            }
                        }else if (fragmentName.equalsIgnoreCase(HISTORY_FRAGMENT)) {
                            if (fragment instanceof RecentCallsFragment) {
                                return fragment;
                            }
                        }else if (fragmentName.equalsIgnoreCase(CONTACTS_DETAILS_FRAGMENT)) {
                            if (fragment instanceof ContactDetailsFragment) {
                                return fragment;
                            }
                        }else if (fragmentName.equalsIgnoreCase(CONTACTS_EDIT_FRAGMENT)) {
                            if (fragment instanceof ContactEditFragment) {
                                return fragment;
                            }
                        }else if (fragmentName.equalsIgnoreCase(ACTIVE_CALL_FRAGMENT)) {
                            if (fragment instanceof ActiveCallFragment) {
                                return fragment;
                            }
                        }else if (fragmentName.equalsIgnoreCase(ACTIVE_VIDEO_CALL_FRAGMENT)) {
                            if (fragment instanceof VideoCallFragment) {
                                return fragment;
                            }
                        }
                    }

                }
            }
        }catch (Exception e){
            return null;
        }
        return null;
    }

    /**
     * @param fragmentName name of the fragment whose visibility is tested
     * @return true if the fragment is visible
     */
    public boolean isFragmentVisible(String fragmentName){
        try {
            FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            if (fragments != null) {
                for (Fragment fragment : fragments) {
                    if (fragment != null && fragment.isVisible()) {
                        if (fragmentName.equalsIgnoreCase(DIALER_FRAGMENT)) {
                            if (fragment instanceof DialerFragment) {
                                return true;
                            }
                        }else if (fragmentName.equalsIgnoreCase(FAVORITES_FRAGMENT)) {
                            if (fragment instanceof FavoritesFragment) {
                                return true;
                            }
                        }else if (fragmentName.equalsIgnoreCase(CONTACTS_FRAGMENT)) {
                            if (fragment instanceof ContactsFragment) {
                                return true;
                            }
                        }else if (fragmentName.equalsIgnoreCase(HISTORY_FRAGMENT)) {
                            if (fragment instanceof RecentCallsFragment) {
                                return true;
                            }
                        }else if (fragmentName.equalsIgnoreCase(CONTACTS_DETAILS_FRAGMENT)) {
                            if (fragment instanceof ContactDetailsFragment) {
                                return true;
                            }
                        }else if (fragmentName.equalsIgnoreCase(CONTACTS_EDIT_FRAGMENT)) {
                            if (fragment instanceof ContactEditFragment) {
                                return true;
                            }
                        }else if (fragmentName.equalsIgnoreCase(ACTIVE_CALL_FRAGMENT)) {
                            if (fragment instanceof ActiveCallFragment) {
                                return true;
                            }
                        }else if (fragmentName.equalsIgnoreCase(ACTIVE_VIDEO_CALL_FRAGMENT)) {
                            if (fragment instanceof VideoCallFragment) {
                                return true;
                            }
                        }
                    }

                }
            }
        }catch (Exception e){
            return false;
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            try {
                if (keyCode == KeyEvent.KEYCODE_0) {
                    if (isFragmentVisible(DIALER_FRAGMENT) && mSectionsPagerAdapter != null && mSectionsPagerAdapter.getDialerFragment() != null) {
                        mSectionsPagerAdapter.getDialerFragment().dialFromKeyboard("+");
                        zeroOrPlus = "+";
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(DigitKeys.contains(event.getKeyCode())) {
            return true;
        }else
            return super.onKeyLongPress(keyCode, event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            isToLockPressButton = false;
            String key = KeyEvent.keyCodeToString(event.getKeyCode()).replace("KEYCODE_", "");
            sendAccessibilityEvent(key, findViewById(R.id.dialer_display));

            if (keyCode == KeyEvent.KEYCODE_0) {
                if(mSectionsPagerAdapter!=null && mSectionsPagerAdapter.getDialerFragment()!=null ) {
                    //mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(zeroOrPlus);
                    if(((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)) !=null && !((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mHoldCallButton.isChecked()) {
                        ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).sendDTMF(zeroOrPlus.charAt(0));
                    }else{
                        mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(zeroOrPlus);
                    }
                }
            }
        }
        if(DigitKeys.contains(event.getKeyCode())) {
            if(isOnKeyDownHappened == false) {
                int keyunicode = event.getUnicodeChar(event.getMetaState());
                char character = (char) keyunicode;
                String digit = "" + character;

                if (isFragmentVisible(DIALER_FRAGMENT) && mSectionsPagerAdapter != null && mSectionsPagerAdapter.getDialerFragment() != null) {
                    if (digit.matches("[\\d]") || digit.matches("\\#") || digit.matches("\\*")) {
                        if( ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)) ==null && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout !=null && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout.getVisibility() != View.VISIBLE ) {
                            mSectionsPagerAdapter.getDialerFragment().dialFromKeyboard(digit);
                        }
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        mSectionsPagerAdapter.getDialerFragment().deleteDigit();
                    }
                }

                if (DigitKeys.contains(event.getKeyCode()) && (ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)!=null && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout !=null && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout.getVisibility() != View.VISIBLE) {

                    if (keyCode != KeyEvent.KEYCODE_0) {
                        //mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(digit);
                        if(((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)) !=null && !((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mHoldCallButton.isChecked()) {
                            ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).sendDTMF(digit.charAt(0));
                        }else {
                            mSectionsPagerAdapter.getDialerFragment().onHardKeyClick(digit);
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_0) {
                        zeroOrPlus = "0";
                    }

                    mViewPager.setCurrentItem(0, false);
                    searchButton.setVisibility(View.INVISIBLE);
                    addcontactButton.setVisibility(View.INVISIBLE);

                    try {
                        if (isFragmentVisible(DIALER_FRAGMENT) && isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)) {
                            changeUiForFullScreenInLandscape(false);
                            ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)).mBackListener.back();
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                    event.startTracking();
                }
            }
            isOnKeyDownHappened = false;
            return true;
        }else {
            return super.onKeyUp(keyCode, event);
        }
    }

    public void sendAccessibilityEvent(String speech, View source) {
        AccessibilityManager manager = (AccessibilityManager)this.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(manager.isEnabled()){
            AccessibilityEvent aevent = AccessibilityEvent.obtain();
            aevent.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            aevent.setClassName(getClass().getName());
            aevent.getText().add(speech);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                aevent.setSource(source);
            }
            manager.sendAccessibilityEvent(aevent);

        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && !isAccessibility) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            fullScreenViewResize(1056);
        } else if (!ActiveCallFragment.IS_ACTIVE) {
            fullScreenViewResize(1000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Processing interaction witj {@link DialerFragment}
     *
     * @param number String representation of number entered
     * @param action {@link DialerFragment.ACTION} on {@link DialerFragment}
     *
     * @return true if the number shall be kept and presented to the user after it was used; false - otherwise
     */
    @Override
    public boolean onDialerInteraction(String number, DialerFragment.ACTION action) {

        boolean keepNumberAfterUsage = true;

        try {
            if (action == DialerFragment.ACTION.DIGIT) {
                if (getResources().getBoolean(R.bool.is_landscape) == true) {
                    FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
                    List<Fragment> fragments = fragmentManager.getFragments();
                    if (fragments != null) {
                        for (Fragment fragment : fragments) {
                            if (fragment != null && fragment.isVisible())
                                if (fragment instanceof DialerFragment) {
                                    mCallViewAdaptor.addDigitToOffHookDialCall(number.charAt(0));
                                } else if (fragment instanceof ActiveCallFragment && (((ActiveCallFragment) fragment).getCallState() == UICallState.ESTABLISHED)) {
                                    ((ActiveCallFragment) fragment).sendDTMF(number.charAt(0));
                                    keepNumberAfterUsage = false;
                                }
                        }
                    }
                } else {
                    mCallViewAdaptor.addDigitToOffHookDialCall(number.charAt(0));
                }
            } else {
                boolean isVideo = (action == DialerFragment.ACTION.VIDEO);
                mCallViewAdaptor.createCall(number, isVideo);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return keepNumberAfterUsage;
    }

    /**
     * Handle onNewIntent() to inform the fragment manager that the
     * state is not saved.  If you are handling new intents and may be
     * making changes to the fragment state, you want to be sure to call
     * through to the super-class here first.  Otherwise, if your state
     * is saved but the activity is not stopped, you could get an
     * onNewIntent() call which happens before onResume() and trying to
     * perform fragment operations at that point will throw IllegalStateException
     * because the fragment manager thinks the state is still saved.
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Method responsible for handling received Intents
     * Intents which are processed are
     *
     * @param intent
     * @INCOMING_CALL_INTENT
     * @NON_SERVICE_IMPACTING_CHANGE
     * @NON_SERVICE_IMPACTING_CHANGE
     */
    private void handleIntent(Intent intent) {

        if(getIntent().getExtras()!=null && getIntent().getExtras().getBoolean(INCOMING_CALL_INTENT) == true){
            tabLayoutReset();
        }

        if (intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
            case Intent.ACTION_SEARCH:
                String query = intent.getStringExtra(SearchManager.QUERY);
                mSectionsPagerAdapter.getFragmentContacts().setQuery(query);
                break;
            case SERVICE_IMPACTING_CHANGE:
                SDKManager.getInstance().getDeskPhoneServiceAdaptor().applyConfigChanges(true);
                break;
            case NON_SERVICE_IMPACTING_CHANGE:
                applyConfigChange();
                break;
            case EMERGENCY_CALL_INTENT:
                final Uri telData = intent.getData();
                final String toNum = (telData == null) ? "" : PhoneNumberUtils
                        .stripSeparators(getPhoneNumberFromTelURI(Uri.decode(telData.toString())));
                if (!TextUtils.isEmpty(toNum)) {
                    Log.d(TAG, "Emergency call to " + toNum);
                    onDialerInteraction(toNum, DialerFragment.ACTION.AUDIO);
                }
                break;
            case SHOW_CALL_INTENT:
                int id = intent.getIntExtra(Constants.CALL_ID, 0);
                if (mCallActiveCallID != id) {
                    CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                    if (callStatusFragment != null && callStatusFragment.getCallStatusClickListener() != null) {
                        callStatusFragment.getCallStatusClickListener().onClick(null);
                    }
                }
                break;
            case Intent.ACTION_VIEW:
                if ("tel".equals(intent.getScheme())) {
                    String[] telParts = intent.getData().getSchemeSpecificPart().split(";");
                    //TODO: decide if to dial right away or just load the dialer
                    //boolean isVideo = intent.getDataString().contains("video");
                    //onDialerInteraction(PhoneNumberUtils.stripSeparators(telParts[0]), isVideo ? DialerFragment.ACTION.VIDEO : DialerFragment.ACTION.AUDIO);
                    if (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getDialerFragment() != null) {
                        mSectionsPagerAdapter.getDialerFragment().setDialer(PhoneNumberUtils.stripSeparators(telParts[0]));
                    }
                }
                //ELAN-1000
                try {
                    if (intent.hasExtra(Constants.GO_TO_FRAGMENT) && Constants.GO_TO_FRAGMENT_MISSED_CALLS.equalsIgnoreCase(intent.getStringExtra(Constants.GO_TO_FRAGMENT))) {

                        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CONTACTS) == true) {

                            if(isFragmentVisible(ACTIVE_CALL_FRAGMENT) || isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)){
                                ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mBackArrowOnClickListener();
                            }
                            mViewPager.setCurrentItem(3, false);

                        } else {

                            if(isFragmentVisible(ACTIVE_CALL_FRAGMENT) || isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)){
                                ((ActiveCallFragment) getVisibleFragment(ACTIVE_CALL_FRAGMENT)).mBackArrowOnClickListener();
                            }
                            mViewPager.setCurrentItem(2, false);

                        }

                        if(isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)) {
                            ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)).mBackListener.back();
                            changeUiForFullScreenInLandscape(false);
                        }

                        if(isFragmentVisible(CONTACTS_EDIT_FRAGMENT)) {
                            ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)).cancelOnClickListener();
                            changeUiForFullScreenInLandscape(false);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;

            case DIAL_ACTION:
                if ("tel".equals(intent.getScheme())) {
                    String[] telParts = intent.getData().getSchemeSpecificPart().split(";");
                    //TODO: decide if to dial right away or just load the dialer
                    //boolean isVideo = intent.getDataString().contains("video");
                    //onDialerInteraction(PhoneNumberUtils.stripSeparators(telParts[0]), isVideo ? DialerFragment.ACTION.VIDEO : DialerFragment.ACTION.AUDIO);
                    if (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getDialerFragment() != null) {
                        mSectionsPagerAdapter.getDialerFragment().setDialer(PhoneNumberUtils.stripSeparators(telParts[0]));
                    }
                }
                break;


        }

    }

    /**
     * Processing interaction with {@link ContactsFragment}
     *
     * @param item {@link ContactData} to be presented in fragment
     */
    @Override
    public void onContactsFragmentInteraction(final ContactData item) {
        if (!mSectionsPagerAdapter.isCallAddParticipant()) {
            Log.d(TAG, "contact selected - show contact detail");
            mSectionsPagerAdapter.setContactDetails(item);
            setTabIcons();
        } else {
            try {
                final List<ContactData.PhoneNumber> contactPhones;
                // since local contacts ContactData does not contain all phone numbers, we get them using
                // LocalContactInfo class. As for Enterprise contacts, we go the old way

                if (item.mCategory == ContactData.Category.LOCAL || item.mCategory == ContactData.Category.IPO) {
                    contactPhones = LocalContactInfo.getPhoneNumbers(Uri.parse(item.mURI), this);
                } else {
                    contactPhones = item.mPhones;
                }

                // we assign appropriate adapter to number picker list
                NumberPickerAdapter mNumberPickerAdapter = new NumberPickerAdapter(this, item, contactPhones);
                mNumberPickerList.setAdapter(mNumberPickerAdapter);

                // since we have list of phone numbers here, we can handle list click directly from main activity
                mNumberPickerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        //after phone number is selected, we just hide invisible frame and list of phone numbers
                        mSelectPhoneNumber.setVisibility(View.INVISIBLE);
                        mFrameAll.setVisibility(View.GONE);

                        if(getResources().getBoolean(R.bool.is_landscape) == true){
                            changeUiForFullScreenInLandscape(false);
                        }

                        // since method, that is used to add participant or transfer a call, accepts only contact data item,
                        // we create new contact data item that contains only one contact number (the one we selected)
                        String selectedPhone = contactPhones.get(position).Number.replaceAll("\\D+", ""); // extracting digits from the phone number
                        ContactData.PhoneType selectedPhoneType = contactPhones.get(position).Type;
                        ContactData.PhoneNumber selectedPhoneNumber = new ContactData.PhoneNumber(selectedPhone, selectedPhoneType, false, "");
                        List<ContactData.PhoneNumber> newList = new ArrayList<>();
                        newList.add(selectedPhoneNumber);
                        ContactData newItem = item.createNew(null, newList, item.mAccountType, "", "");
                        onCallAddParticipant(newItem);
                    }
                });

                // displaying contact name
                mNumberPickerContactName.setText(item.mName);
                // showing popup menu with contact phone numbers
                if(getResources().getBoolean(R.bool.is_landscape) == true) {
                    mSlideSelectPhoneNumber.expand(mSelectPhoneNumber,true);
                }else{
                    mSlideSelectPhoneNumber.expand(mSelectPhoneNumber,false);
                }

                mFrameAll.setVisibility(View.VISIBLE);
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Setting up icons and description for tabs
     */
    public void setTabIcons() {

        Log.d(TAG, "setTabIcons");
        int idx = 0;
        mTabIndexMap.clear();
        dialerView = (ImageView) findViewById(R.id.dialer_tab);

        if (!mSectionsPagerAdapter.isCallAddParticipant()) {
            mTabIndexMap.put(Tabs.Dialer, idx);
            //noinspection ConstantConditions,ConstantConditions,ConstantConditions
            ((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(idx).setContentDescription(getString(R.string.dialer_content_description));
            TabLayout.Tab tab = mTabLayout.getTabAt(idx++);
            if (tab != null) {
                tab.setIcon(R.drawable.ic_dialpad);
            }
        }

        if (mSectionsPagerAdapter.isFavoriteTabPresent()) {
            Log.d(TAG, "setTabIcons favorites for " + idx);
            mTabIndexMap.put(Tabs.Favorites, idx);
            //noinspection ConstantConditions
            ((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(idx).setContentDescription(getString(R.string.favorites_content_description));
            TabLayout.Tab tab = mTabLayout.getTabAt(idx++);
            if (tab != null) {
                tab.setIcon(R.drawable.ic_favorites);
            }
        }

        if (mSectionsPagerAdapter.isContactsTabPresent()) {
            Log.d(TAG, "setTabIcons contacts for " + idx);
            mTabIndexMap.put(Tabs.Contacts, idx);
            //noinspection ConstantConditions

            ((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(idx).setContentDescription(getString(R.string.contacts_content_description));
            TabLayout.Tab tab = mTabLayout.getTabAt(idx++);
            if (tab != null) {
                tab.setIcon(R.drawable.ic_contacts);
            }

            if(getResources().getBoolean(R.bool.is_landscape) == true) {

                tabImage = (ImageView)  tabOne.findViewById(R.id.tab_image);
                tabImage.setImageResource(R.drawable.ic_contacts);

                tabSelector = (ImageView)  tabOne.findViewById(R.id.tab_selector);

                tabSelector.setImageResource(R.drawable.triangle_copy);
                showingFirst = false;

                tabSelectorWrapper = (RelativeLayout)  tabOne.findViewById(R.id.filter);


                if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) == true  && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CALL_LOG) == true){
                    if ( (mTabLayout.getTabAt(2).getCustomView() == null && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0) || !mSectionsPagerAdapter.isCallAddParticipant()))
                        mTabLayout.getTabAt(2).setCustomView(tabOne);
                    else if (mTabLayout.getTabAt(1).getCustomView() == null && SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0 )
                        mTabLayout.getTabAt(1).setCustomView(tabOne);
                }else if( SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) == true  && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CALL_LOG) != true ) {

                    if ( ( mTabLayout.getTabAt(2)!=null && mTabLayout.getTabAt(2).getCustomView() == null && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0) || !mSectionsPagerAdapter.isCallAddParticipant()))
                        mTabLayout.getTabAt(2).setCustomView(tabOne);
                    else if ( mTabLayout.getTabAt(1)!=null && mTabLayout.getTabAt(1).getCustomView() == null && SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0 )
                        mTabLayout.getTabAt(1).setCustomView(tabOne);

                }else if( SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) != true  && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CALL_LOG) == true ) {
                    if ( ( mTabLayout.getTabAt(1)!=null && mTabLayout.getTabAt(1).getCustomView() == null && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0) || !mSectionsPagerAdapter.isCallAddParticipant()))
                        mTabLayout.getTabAt(1).setCustomView(tabOne);
                    else if ( mTabLayout.getTabAt(0)!=null && mTabLayout.getTabAt(0).getCustomView() == null && SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0 )
                        mTabLayout.getTabAt(0).setCustomView(tabOne);
                }else if( SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) != true  && SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CALL_LOG) != true ) {
                    if ( ( mTabLayout.getTabAt(1)!=null && mTabLayout.getTabAt(1).getCustomView() == null && (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0) || !mSectionsPagerAdapter.isCallAddParticipant()))
                        mTabLayout.getTabAt(1).setCustomView(tabOne);
                    else if ( mTabLayout.getTabAt(0)!=null && mTabLayout.getTabAt(0).getCustomView() == null && SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0 )
                        mTabLayout.getTabAt(0).setCustomView(tabOne);
                }
            }



        }
        else if (getResources().getBoolean(R.bool.is_landscape)) {

                searchButton.setVisibility(View.INVISIBLE);
                addcontactButton.setVisibility(View.INVISIBLE);
                filterButton.setVisibility(View.INVISIBLE);
        }
        if (mSectionsPagerAdapter.isRecentTabPresent()) {
            Log.d(TAG, "setTabIcons recents for " + idx);
            mTabIndexMap.put(Tabs.History, idx);
            ((ViewGroup) mTabLayout.getChildAt(0)).getChildAt(idx).setContentDescription(getString(R.string.recent_calls_content_description));
            setHistoryIcon(idx);
        }


        if (dialerView != null) {
            if (mTabLayout.getTabCount() < MINIMAL_AMOUNT_OF_TABS) {
                mTabLayout.setVisibility(View.GONE);
                dialerView.setVisibility(View.VISIBLE);
            } else {
                dialerView.setVisibility(View.GONE);
                mTabLayout.setVisibility(View.VISIBLE);
            }
        }

        ColorStateList colors;
        if (Build.VERSION.SDK_INT >= 23) {
            colors = getResources().getColorStateList(R.color.tab_tint, getTheme());
        } else {
            colors = this.getColorStateList(R.color.tab_tint);
        }

        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            Drawable icon = null;
            if (tab != null) {
                icon = tab.getIcon();
            }
            if (icon != null) {
                icon = DrawableCompat.wrap(icon);
                DrawableCompat.setTintList(icon, colors);
            }
        }
    }

    private void contactsTabFilterMenuListnerSetup(View v){
        try{
            if(showingFirst == true){


                if(getResources().getBoolean(R.bool.is_landscape) == true){
                    FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
                    List<Fragment> fragments = fragmentManager.getFragments();
                    if(fragments != null){
                        for(Fragment fragment : fragments){
                            if(fragment != null && fragment.isVisible())
                                if (fragment instanceof ContactsFragment && ((ContactsFragment) fragment).mUserVisibleHint) {
                                    ((ContactsFragment) fragment).hideMenus();
                                    tabSelector.setImageResource(R.drawable.triangle_copy);
                                    showingFirst = false;
                                    break;
                                }
                        }
                    }

                }

            }else{

                if(getResources().getBoolean(R.bool.is_landscape) == true){
                    try {
                        FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
                        List<Fragment> fragments = fragmentManager.getFragments();
                        if (fragments != null) {
                            for (Fragment fragment : fragments) {
                                if (fragment != null && fragment.isVisible())
                                    if (fragment instanceof ContactsFragment && ((ContactsFragment) fragment).mUserVisibleHint && !fragment.isDetached()) {
                                        ((ContactsFragment) fragment).onClick(v);
                                        tabSelector.setImageResource(R.drawable.triangle);
                                        showingFirst = true;
                                        break;
                                    }
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Updating number of missed calls for History tab in tab view
     *
     * @param pageNumber number of tab where icon have to be placed
     */
    @SuppressLint("InflateParams")
    public void setHistoryIcon(int pageNumber) {
        //noinspection ConstantConditions
        try {
            int numberOfMissedCalls = mCallPreference.getInt(Constants.KEY_UNSEEN_MISSED_CALLS, 0);
            if (numberOfMissedCalls > 0) {
                View tabVIew = null;
                ImageView missedCallBackground = null;
                if (mTabLayout.getTabAt(pageNumber) != null) {
                    //noinspection ConstantConditions
                    tabVIew = mTabLayout.getTabAt(pageNumber).getCustomView();
                }
                if (tabVIew == null) {
                    tabVIew = getLayoutInflater().inflate(R.layout.recent_tab_view, null);
                }
                if (numberOfMissedCalls > 99) {
//                    missedCallBackground = (ImageView) tabVIew.findViewById(R.id.recent_tab_number_background);
//                    if (missedCallBackground != null) {
//                        ViewGroup.LayoutParams params = missedCallBackground.getLayoutParams();
//                        params.width = getResources().getInteger(R.integer.recent_tab_big_width);
//                        missedCallBackground.setLayoutParams(params);
//                    }
                }
                try {

                    TextView numberView = (TextView) tabVIew.findViewById(R.id.recent_tab_number);
                    numberView.setText(String.valueOf(numberOfMissedCalls));

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                if (mTabLayout.getTabAt(pageNumber) != null) {
                    //noinspection ConstantConditions
                    mTabLayout.getTabAt(pageNumber).setCustomView(tabVIew);
                }
            } else {
                if (mTabLayout.getTabAt(pageNumber) != null) {
                    //noinspection ConstantConditions
                    mTabLayout.getTabAt(pageNumber).setCustomView(null);
                    //noinspection ConstantConditions
                    mTabLayout.getTabAt(pageNumber).setIcon(R.drawable.ic_history);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Processing audio call for {@link ContactData}
     *
     * @param item        {@link ContactData} for which audio call is started
     * @param phoneNumber String representation of number which is called
     */
    @Override
    public void onCallContactAudio(ContactData item, String phoneNumber) {
        // scroll back to the first page to be prepared to any other operations
        // that might be activated via the active call interface
        makeCall(item, false, phoneNumber);
    }

    /**
     * Processing event of adding participant to call
     *
     * @param item {@link ContactData} to be added to call
     */
    @Override
    public void onCallAddParticipant(ContactData item) {
        callAddParticipant(item);
    }

    /**
     * This method prepares data for Conference or Transfer and creates Intent
     * for {@link #onActivityResult(int, int, Intent)} method
     *
     * @param contactData The {@link ContactData} item which should be add to the existing call
     */
    public void callAddParticipant(ContactData contactData) {

        if (isConferenceCall) {
            mRequestName = getResources().getString(R.string.merge_complete);
        } else {
            mRequestName = getResources().getString(R.string.trasfer_complete);
        }

        mContactNumber = setPhoneNumberPriority(contactData);

        if (mContactNumber.length() > 0) {
            Intent data = new Intent(getPackageName() + Constants.ACTION_TRANSFER);
            data.putExtra(Constants.CALL_ID, mCallActiveCallID);
            data.putExtra(Constants.TARGET, mContactNumber);
            setResult(RESULT_OK, data);
            onActivityResult(mActiveCallRequestCode, RESULT_OK, data);
        } else {
            Snackbar.make(mActivityLayout, getResources().getString(R.string.operation_failed), Snackbar.LENGTH_SHORT).show();
        }
        setAddParticipant(false);
        tabLayoutReset();
    }

    /**
     * Processing video call of contact
     *
     * @param item        {@link ContactData} which is called
     * @param phoneNumber String representation of phone number
     */
    @Override
    public void onCallContactVideo(ContactData item, String phoneNumber) {
        // scroll back to the first page to be prepared to any other operations
        // that might be activated via the active call interface
        makeCall(item, true, phoneNumber);
    }

    /**
     * Processing new contact creation with opening {@link ContactEditFragment}
     */
    @Override
    public void onCreateNewContact() {
        ContactEditFragment mContactEditFragment = ContactEditFragment.newInstance();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.edit_contact_frame, mContactEditFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public void checkFilterVisibility() {
        if (mSectionsPagerAdapter.getFragmentContacts() != null){
            mSectionsPagerAdapter.getFragmentContacts().checkFilterVisibility();
        }
    }

    /**
     * Processing search start event
     *
     * @param mSearchLayout {@link Rect}
     */
    @Override
    public void onStartSearching(Rect mSearchLayout) {
        mSearchArea = mSearchLayout;
        isSearchInProgress = true;
    }

    /**
     * Perform preparation for performing call.
     * Inform CSDK to perform call
     *
     * @param contactData {@link ContactData} for which call have to be done
     * @param isVideo     boolean is call video call
     * @param phoneNumber String phone number which have to be called
     */
    private void makeCall(ContactData contactData, boolean isVideo, String phoneNumber) {
        String number;

        if (phoneNumber == null || phoneNumber.equals("")) {
            if (contactData.mPhones.size() == 0) {
                Log.d(TAG, "makeCall: no phone numbers associated with this contact");
                return;
            } else {
                number = setPhoneNumberPriority(contactData);
            }
        } else {
            number = phoneNumber;
        }
        Log.d(TAG, "contact audio call to Contact Name: " + contactData.mName + " Contact number: " + number);
        if (number!=null && number.length() > 0) {
            SDKManager.getInstance().getCallAdaptor().createCall(number, isVideo);
        }
    }

    /**
     * Setting up a priority for numbers if contact have more than one
     *
     * @param contactData {@link ContactData} for which we are setting priority phone number
     * @return Phone number according to priority
     */
    private String setPhoneNumberPriority(ContactData contactData) {

        String number;
        String type;
        final String PRIORITY_WORK = "1";
        final String PRIORITY_MOBILE = "2";
        final String PRIORITY_CUSTOM = "3";
        final String PRIORITY_OTHER = "4";
        final String PRIORITY_HOME = "5";
        final String PRIORITY_DEFAULT = "6";

        List<String[]> priorityList = new ArrayList<>();

        for (ContactData.PhoneNumber phone : contactData.mPhones) {
            number = phone.Number;
            type = phone.Type.toString();

            // if we find primary number, just return that number
            if (phone.Primary) {
                return number;
            }

            // setting up priorities for phone numbers
            switch (type) {
                case "WORK":
                    priorityList.add(new String[]{PRIORITY_WORK, number});
                    break;
                case "MOBILE":
                    priorityList.add(new String[]{PRIORITY_MOBILE, number});
                    break;
                case "HOME":
                    priorityList.add(new String[]{PRIORITY_HOME, number});
                    break;
                case "OTHER":
                    priorityList.add(new String[]{PRIORITY_OTHER, number});
                    break;
                case "CUSTOM":
                    priorityList.add(new String[]{PRIORITY_CUSTOM, number});
                    break;
                default:
                    priorityList.add(new String[]{PRIORITY_DEFAULT, number});
            }
        }
        // sorting out the list according to priority
        Collections.sort(priorityList, new Comparator<String[]>() {
            @Override
            public int compare(String[] lhs, String[] rhs) {
                return lhs[0].compareTo(rhs[0]);
            }
        });

        // returning first phone number
        return priorityList.get(0)[1];
    }

    @Override
    public void back() {
        mSectionsPagerAdapter.clearContactDetails();
        setTabIcons();
        Tabs selectedTab = Tabs.Favorites;
        for (Tabs t : mTabIndexMap.keySet()) {
            if (mViewPager!=null && mViewPager.getCurrentItem() == mTabIndexMap.get(t)) {
                selectedTab = t;
            }
        }
        switch (selectedTab) {
            case Favorites:
                mSectionsPagerAdapter.getFragmentFavorites().restoreListPosition(mSectionsPagerAdapter.getFavoritesListPosition());
                break;
            case Contacts:
                mSectionsPagerAdapter.getFragmentContacts().restoreListPosition(mSectionsPagerAdapter.getContactsListPosition());
                break;
            case History:
                mSectionsPagerAdapter.getFragmentRecent().restoreListPosition(mSectionsPagerAdapter.getRecentCallsListPosition());
                mSectionsPagerAdapter.getFragmentRecent().performSelectionByCategory(mSelectedCallCategory);
                mSectionsPagerAdapter.getFragmentRecent().setLastVisibleItem(position);
                break;
            default:
                // default statement
        }

        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
            int mNumActiveCalls = mCallStateEventHandler.mCalls.size();
            if( callStatusFragment.getView().getVisibility()!=View.VISIBLE  && mNumActiveCalls>0) {
                callStatusFragment.showCallStatus();
            }
        }
    }

    /**
     * Processing edit request for {@link ContactData} and starting new {@link ContactEditFragment}
     *
     * @param contactData  {@link ContactData} to be edited if exist
     * @param isNewContact boolean giving us information do we create new contact or we are editing
     *                     existing one
     */
    @Override
    public void edit(ContactData contactData, boolean isNewContact) {
        ContactEditFragment mContactEditFragment = ContactEditFragment.newInstance(contactData, isNewContact);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.edit_contact_frame, mContactEditFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    /**
     * Processing successful login made by user
     *
     * @param name      name of user
     * @param extension extension number of user
     */
    @Override
    public void onSuccessfulLogin(String name, String extension) {
        Log.d(TAG, "onSuccessfulLogin: name="+name + " extension="+extension);
        mLoginGuardOneshot = true;
        if (isDestroyed()) {
            Log.e(TAG, "Activity is destroyed returning");
            return;
        }
        mLoggedUserNumber.setText(extension);
        mLoggedUserExtension.setText(name);
        loadBrand(getBrand(), true);

        applyLockSetting();

        if (mUIContactsViewAdaptor != null) {
            mUIContactsViewAdaptor.refresh();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isSearchInProgress && mSearchArea != null && ev.getAction() == MotionEvent.ACTION_DOWN && !mSearchArea.contains((int) ev.getX(), (int) ev.getY())) {
            Utils.hideKeyboard(this);
        }
        try {
            return super.dispatchTouchEvent(ev);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Handling on clicks in simpler way
     *
     * @param clickView {@link View} of a clicked item
     */
    @Override
    public void onClick(View clickView) {
        try {
            switch (clickView.getId()) {
                case R.id.off_hook: // select audio button
                    if (clickView instanceof ToggleButton) {
                        if (((ToggleButton) clickView).isChecked()) {
                            int callId = SDKManager.getInstance().getCallAdaptor().isAlertingCall();
                            if (callId != 0) {
                                Intent intent = new Intent(INCOMING_CALL_ACCEPT);
                                intent.putExtra(Constants.CALL_ID, callId);
                                sendBroadcast(intent);
                            } else if (SDKManager.getInstance().getCallAdaptor().getActiveCallId() == 0) {
                                 if( isLockState(this) == true){
                                     setOffhookButtosChecked(false);
                                 }
                                 else {
                                     prepareOffHook();
                                     mCallViewAdaptor.createCall(false);
                                 }
                            }
                        } else {
                            int callId = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
                            if (callId == 0 && mCallStateEventHandler != null)
                                callId = mCallStateEventHandler.getCurretCallId();
                            mCallViewAdaptor.endCall(callId);
                            try {
                                if (isFragmentVisible(DIALER_FRAGMENT))
                                    ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).setMode(DialerFragment.DialMode.EDIT);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return;
                case R.id.audio_mute:
                    mCallViewAdaptor.audioMuteStateToggled();
                    break;
                case R.id.video_mute:
                    Log.d(TAG, "onClick: video_mute is " + mVideoMute.isChecked());
                    mVideoMute.setEnabled(false);
                    mVideoMute.setChecked(!((ToggleButton) clickView).isChecked());
                    mCallViewAdaptor.videoMuteStateToggled();
                    break;
                case R.id.containerBTHeadset:
                    Log.d(TAG, "onClick: BT Headset");
                    mSelectAudio.setBackgroundResource(R.drawable.pc_bluetooth);
                    setBackgroundResourceForDeviceId(clickView.getId());

                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.BLUETOOTH_HEADSET);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.BLUETOOTH_HEADSET.toString());
                    break;
                case R.id.containerHeadset:
                    Log.d(TAG, "onClick: headset");
                    mSelectAudio.setBackgroundResource(R.drawable.pc_headset);

                    setBackgroundResourceForDeviceId(clickView.getId());
                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.RJ9_HEADSET);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.RJ9_HEADSET.toString());
                    break;
                case R.id.container35Headset:
                    Log.d(TAG, "onClick: 3.5 headset");
                    mSelectAudio.setBackgroundResource(R.drawable.pc_35mm);

                    setBackgroundResourceForDeviceId(clickView.getId());
                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.WIRED_HEADSET);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.WIRED_HEADSET.toString());
                    break;
                case R.id.containerHandset:
                    Log.d(TAG, "onClick: handset");
                    UIAudioDevice device = UIAudioDevice.HANDSET;
                    if (mAudioDeviceViewAdaptor.getAudioDeviceList() != null && mAudioDeviceViewAdaptor.getAudioDeviceList().contains(UIAudioDevice.WIRELESS_HANDSET)) {
                        device = UIAudioDevice.WIRELESS_HANDSET;
                    }
                    mSelectAudio.setBackgroundResource(R.drawable.pc_handset);

                    setBackgroundResourceForDeviceId(clickView.getId());
                    mAudioDeviceViewAdaptor.setUserRequestedDevice(device);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, device.toString());
                    break;
                case R.id.containerSpeaker:
                    Log.d(TAG, "onClick: speaker");
                    mSelectAudio.setBackgroundResource(R.drawable.pc_off_hook);
                    setBackgroundResourceForDeviceId(clickView.getId());
                    mAudioDeviceViewAdaptor.setUserRequestedDevice(UIAudioDevice.SPEAKER);
                    saveAudioSelection(Constants.AUDIO_PREF_KEY, UIAudioDevice.SPEAKER.toString());
                    break;
                case R.id.user:
                    if ( (SystemClock.elapsedRealtime() - mLastClickTime < 1000) ) {
                        return;
                    }
                    if( getResources().getBoolean(R.bool.is_landscape) == false && mPickContacts.getVisibility()==View.VISIBLE) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    mUser.setContentDescription(mLoggedUserExtension.getText().toString() + " " + mLoggedUserNumber.getText().toString() + " " + getString(R.string.user_content_description));
                    if (getResources().getBoolean(R.bool.is_landscape) == true) {
                        mSlideSelecAudioDevice.collapse(mToggleAudioMenu, true);
                    } else {
                        mSlideSelecAudioDevice.collapse(mToggleAudioMenu, false);
                    }
                    if (mListPreferences.getVisibility() == View.GONE || mListPreferences.getVisibility() == View.INVISIBLE) {
                        if (getResources().getBoolean(R.bool.is_landscape) == true) {
                            mSlideUserPreferences.expand(mListPreferences, true);
                        } else {
                            mSlideUserPreferences.expand(mListPreferences, false);
                        }

                        mFrameAll.setVisibility(View.VISIBLE);
                        mHandler.postDelayed(mLayoutCloseRunnable, Constants.LAYOUT_DISAPPEAR_TIME);
                        mOpenUser.setImageDrawable(getDrawable(R.drawable.ic_expand_less));
                    } else {
                        mOpenUser.setImageDrawable(getDrawable(R.drawable.ic_expand_more));
                        if (getResources().getBoolean(R.bool.is_landscape) == true) {
                            mSlideUserPreferences.collapse(mListPreferences, true);
                        } else {
                            mSlideUserPreferences.collapse(mListPreferences, false);
                        }

                        mHandler.removeCallbacks(mLayoutCloseRunnable);
                    }
                    return;
                case R.id.containerUserSettings:
                    Intent i = new Intent(MainActivity.this, UserPreferencesActivity.class);
                    //i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(i);
                    break;
                case R.id.containerAbout:
                    Intent s = new Intent(MainActivity.this, SupportActivity.class);
                    startActivity(s);
                    break;
                case R.id.pick_cancel:
                    cancelAddSomeOneScreen();

                    CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                    int isActiveCall = SDKManager.getInstance().getCallAdaptor().getActiveCallId();
                    int numOfCalls = SDKManager.getInstance().getCallAdaptor().getNumOfCalls();
                    int mNumCalls = mCallStateEventHandler.mCalls.size();
                    if( (callStatusFragment.getView().getVisibility()==View.INVISIBLE || callStatusFragment.getView().getVisibility()==View.GONE) && isActiveCall!=0 && numOfCalls >= 1 && mNumCalls>0) {
                        callStatusFragment.showCallStatus();
                    }


                    break;
                case R.id.transducer_button:
                    showAudioList(mToggleAudioMenu);
                    if (getResources().getBoolean(R.bool.is_landscape) == true) {
                        mSlideUserPreferences.collapse(mListPreferences, true);
                    } else {
                        mSlideUserPreferences.collapse(mListPreferences, false);
                    }

                    if (mToggleAudioMenu.getVisibility() == View.VISIBLE) {
                        if (getResources().getBoolean(R.bool.is_landscape) == true)
                            mSlideSelecAudioDevice.collapse(mToggleAudioMenu, true);
                        else
                            mSlideSelecAudioDevice.collapse(mToggleAudioMenu, false);
                        mHandler.removeCallbacks(mLayoutCloseRunnable);
                    } else {
                        if (getResources().getBoolean(R.bool.is_landscape) == true)
                            mSlideSelecAudioDevice.expand(mToggleAudioMenu, true);
                        else
                            mSlideSelecAudioDevice.expand(mToggleAudioMenu, false);
                        mFrameAll.setVisibility(View.VISIBLE);
                        mHandler.postDelayed(mLayoutCloseRunnable, Constants.LAYOUT_DISAPPEAR_TIME);
                    }
                    // "return" must be used here to show the menus
                    return;

                case R.id.addcontact_button:
                    if (addcontactButton.getVisibility() == View.VISIBLE && !isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)) {

                        onCreateNewContact();

                        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("addcontact_button", true);
                        editor.commit();

                        return;
                    }

                case R.id.filterRecent:
                    if (filterButton.getVisibility() == View.VISIBLE) {
                        Log.e("Test", "filter_button");
                        FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
                        List<Fragment> fragments = fragmentManager.getFragments();
                        if (fragments != null) {
                            for (Fragment fragment : fragments) {
                                if (fragment != null && fragment.isVisible())
                                    if (fragment instanceof RecentCallsFragment && !fragment.isDetached()) {

                                        if (showingFirstRecent) {
                                            filterButton.setImageResource(R.drawable.ic_expand_less);
                                            //showingFirstRecent = false;
                                        } else {
                                            filterButton.setImageResource(R.drawable.ic_expand_more);
                                            //showingFirstRecent = true;
                                        }

                                        ((RecentCallsFragment) fragment).onClick(clickView);
                                    }
                            }
                        }
                        return;
                    }
                case R.id.search_button:

                    if (getResources().getBoolean(R.bool.is_landscape) == true && (mToggleAudioMenu.getVisibility() == View.INVISIBLE || mToggleAudioMenu.getVisibility() == View.GONE && mListPreferences.getVisibility() == View.GONE)) {
                        changeUiForFullScreenInLandscape(true);
                        mSectionsPagerAdapter.getFragmentContacts().seacrhLayout.setVisibility(View.VISIBLE);
                        mSectionsPagerAdapter.getFragmentContacts().mAdd.setVisibility(View.INVISIBLE);

                        mSectionsPagerAdapter.getFragmentContacts().mSearchView.setQuery("", false);
                        mSectionsPagerAdapter.getFragmentContacts().mSearchView.requestFocus();
                        Utils.openKeyboard(this);

                        mSectionsPagerAdapter.getFragmentContacts().hideMenus();
                        tabSelector.setImageResource(R.drawable.triangle_copy);
                        showingFirst = false;

                        CallStatusFragment callStatusFragment_search_button = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                        if (callStatusFragment_search_button.getView().getVisibility() == View.VISIBLE) {
                            callStatusFragment_search_button.hideCallStatus();
                            Utils.hideKeyboard(this);
                        }

                         mViewPager.setEnabledSwipe(false);

                    }
                    break;
                default:
                    hideMenus();
                    return;
            }
            hideMenus();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }


    public void cancelAddSomeOneScreen(){

        if( SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 0  ) {
            CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
            if (callStatusFragment != null) {
                callStatusFragment.hideCallStatus();
                Utils.hideKeyboard(this);
            }
            ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
            if (activeCallFragment != null) {
                cancelContactPicker();
                activeCallFragment.setVisible();
            }
        }
        setAddParticipant(false);

        tabLayoutReset();

        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            if (fragments != null) {
                for (Fragment fragment : fragments) {
                    if (fragment != null && fragment.isVisible())
                        if (fragment instanceof VideoCallFragment && !fragment.isDetached()) {
                            changeUiForFullScreenInLandscape(true);
                            break;
                        } else {
                            changeUiForFullScreenInLandscape(false);
                        }
                }
            }
        }
    }
    /**
     * Hiding all drop down and popup menus
     */
    private void hideMenus() {
        mHandler.removeCallbacks(mLayoutCloseRunnable);
        if (mToggleAudioMenu.getVisibility() == View.VISIBLE) {
            if(getResources().getBoolean(R.bool.is_landscape) == true)
                mSlideSelecAudioDevice.collapse(mToggleAudioMenu,true);
            else
                mSlideSelecAudioDevice.collapse(mToggleAudioMenu,false);
        }
        if (mListPreferences.getVisibility() == View.VISIBLE) {
            if(getResources().getBoolean(R.bool.is_landscape) == true) {
                mSlideUserPreferences.collapse(mListPreferences, true);
            }else {
                mSlideUserPreferences.collapse(mListPreferences, false);
            }
            mOpenUser.setImageDrawable(getDrawable(R.drawable.ic_expand_more));
        }

        if (mSelectPhoneNumber.getVisibility() == View.VISIBLE) {
            if(getResources().getBoolean(R.bool.is_landscape) == true) {
                mSlideSelectPhoneNumber.collapse(mSelectPhoneNumber, true);
            }else {
                mSlideSelectPhoneNumber.collapse(mSelectPhoneNumber, false);
            }
        }
        mFrameAll.setVisibility(View.GONE);
        mToggleAudioButton.setChecked(false);

        if(getResources().getBoolean(R.bool.is_landscape) == true){
            FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            if(fragments != null){
                for(Fragment fragment : fragments){
                    if(fragment != null && fragment.isVisible())
                        if (fragment instanceof DialerFragment) {
                            ((DialerFragment) fragment).transducerButton.setChecked(false);
                        }else if (fragment instanceof ActiveCallFragment){
                            ((ActiveCallFragment) fragment).transducerButton.setChecked(false);
                        }
                }
            }

        }
    }


    /**
     * Saving selected audio device
     *
     * @param audioKey   static value declared as AUDIO_PREF_KEY. Value "audioDevice"
     * @param audioValue value that determines what option is selected
     */

    private void saveAudioSelection(String audioKey, String audioValue) {

        //get the current value from shared preference and compare it with request value
        String prefValue = mSharedPref.getString(Constants.AUDIO_PREF_KEY, (UIAudioDevice.SPEAKER).toString());
       if (audioValue.equals(prefValue)){
           //audio device has not been changed  - do nothing
           return;
       }

        if (audioKey != null) {
            //((AudioManager) getSystemService(AUDIO_SERVICE)).setSpeakerphoneOn(SPEAKER.equals(audioValue));
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString(audioKey, audioValue);
            editor.apply();

            mAudioMute.setChecked(false);
            mCallViewAdaptor.changeAudioMuteState(false);
        } else {
            Log.d(TAG, "audioKey is null");
        }
    }


    /**
     * Loading selected audio device based on {@link SharedPreferences} audio
     * preferences key
     */

    private void updateAudioSelectionUI(UIAudioDevice device) {
        int resId = getResourceIdForDevice(device);
        mSelectAudio.setBackgroundResource(resId);
        setBackgroundResource(resId);
    }

    private int getResourceIdForDevice(UIAudioDevice device) {

        switch (device) {
            case SPEAKER:
               return R.drawable.pc_off_hook;

            case BLUETOOTH_HEADSET:
                return R.drawable.pc_bluetooth;

            case WIRED_HEADSET:
                return R.drawable.pc_35mm;

            case RJ9_HEADSET:
                return R.drawable.pc_headset;

            case HANDSET:
            case WIRELESS_HANDSET:
                return R.drawable.pc_handset;

            default:
                return R.drawable.pc_off_hook;
        }
    }


    public int getResourceIdForDeviceId(int id) {

        switch (id) {
            case R.id.containerSpeaker:
                return R.drawable.pc_off_hook;

            case  R.id.containerBTHeadset:
                return R.drawable.pc_bluetooth;

            case R.id.container35Headset:
                return R.drawable.pc_35mm;

            case R.id.containerHeadset:
                return R.drawable.pc_headset;

            case R.id.containerHandset:
                return R.drawable.pc_handset;

            default:
                return R.drawable.pc_off_hook;
        }
    }

    private void setBackgroundResourceForDeviceId(int id) {
        int resId = getResourceIdForDeviceId(id);
        setBackgroundResource(resId);
    }


    private void setBackgroundResource(int resId){

        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            try {
                if (isFragmentVisible(DIALER_FRAGMENT)) {
                    ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).offHook.setBackgroundResource(resId);
                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }
            ActiveCallFragment fragment = (ActiveCallFragment) (getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG));
            if (fragment != null) {
                fragment.offHook.setBackgroundResource(resId);
            }
        }
    }



    /**
     * Loading last selected {@link UIAudioDevice} and updating selection for it
     */
    public void loadAudioSelection() {
        int resId = getDeviceResIdFromSharedPref();
        mSelectAudio.setBackgroundResource(resId);
    }

    /**
     * @return Resource ID of the Audio Device
     */
    public int getDeviceResIdFromSharedPref() {
        String prefValue = mSharedPref.getString(Constants.AUDIO_PREF_KEY, (UIAudioDevice.SPEAKER).toString());
        UIAudioDevice device = UIAudioDevice.valueOf(prefValue.toUpperCase());
        if (device == UIAudioDevice.HANDSET && SDKManager.getInstance().getAudioDeviceAdaptor().isWirelessHandset()) {
            device = UIAudioDevice.WIRELESS_HANDSET;
            mSelectAudio.setChecked(true);
        }
        if (mAudioDeviceViewAdaptor.getAudioDeviceList()!=null && mAudioDeviceViewAdaptor.getAudioDeviceList().contains(device)) {
            if (mAudioDeviceViewAdaptor.getActiveAudioDevice() != device && !isLockState(this))
                mAudioDeviceViewAdaptor.setUserRequestedDevice(device);
        } else {
            device = mAudioDeviceViewAdaptor.getUserRequestedDevice();
        }
        int resId = getResourceIdForDevice(device);

        return resId;
    }


    /**
     * Processing cancellation of editing contact
     */
    @Override
    public void cancelEdit() {
        Utils.hideKeyboard(this);
        getSupportFragmentManager().popBackStack();
        if (getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame) != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame)).commit();
        }
    }

    /**
     * Processing confirmation for local {@link ContactData} editing
     *
     * @param contactData  {@link ContactData}
     * @param imageUri     {@link Uri} for image which represent contact to be shown with contact
     * @param isNewContact boolean giving us information if this is new contact
     */
    @Override
    public void confirmLocalContactEdit(ContactData contactData, Uri imageUri, boolean isNewContact, String accountType, String accountName) {

        Utils.hideKeyboard(this);
        getSupportFragmentManager().popBackStack();
        if (getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame) != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame)).commit();
        }

        LocalContactsManager localContactsManager = new LocalContactsManager(this);
        if (!isNewContact) {
            localContactsManager.updateContact(contactData, imageUri);
        } else {
            localContactsManager.createContact(contactData, imageUri, accountType, accountName);
            if (isFragmentVisible(CONTACTS_FRAGMENT) && mSectionsPagerAdapter != null && mSectionsPagerAdapter.getFragmentContacts() != null){
                mSectionsPagerAdapter.getFragmentContacts().contactCreated();
            }
        }

        refreshSectionPager();
    }

    /**
     * Processing confirmation of enterprise {@link ContactData} editing.
     *
     * @param contactData     {@link ContactData}
     * @param editableContact {@link EditableContact}
     */
    @Override
    public void confirmEnterpriseEdit(ContactData contactData, EditableContact editableContact, boolean isNewContact) {
        Utils.hideKeyboard(this);
        getSupportFragmentManager().popBackStack();
        if (getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame) != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(getSupportFragmentManager().findFragmentById(R.id.edit_contact_frame)).commit();
        }

        if (!isNewContact){
            SDKManager.getInstance().getContactsAdaptor().startEnterpriseEditing(contactData, editableContact, true);
        } else {
            SDKManager.getInstance().getContactsAdaptor().createEnterpriseContact(contactData);
        }

        refreshSectionPager();
    }

    /**
     * After done with contact edit, return all fragments to previous stage
     */
    private void refreshSectionPager() {

        if (mSectionsPagerAdapter.getContactDetailsFragment() != null) {
            mSectionsPagerAdapter.clearContactDetails();
            setTabIcons();
            Tabs selectedTab = Tabs.Favorites;
            for (Tabs t : mTabIndexMap.keySet()) {
                if ( mViewPager!=null && mViewPager.getCurrentItem() == mTabIndexMap.get(t)) {
                    selectedTab = t;
                }
            }
            switch (selectedTab) {
                case Favorites:
                    mSectionsPagerAdapter.getFragmentFavorites().restoreListPosition(mSectionsPagerAdapter.getFavoritesListPosition());
                    break;
                case Contacts:
                    mSectionsPagerAdapter.getFragmentContacts().restoreListPosition(mSectionsPagerAdapter.getContactsListPosition());
                    break;
                case History:
                    mSectionsPagerAdapter.getFragmentRecent().restoreListPosition(mSectionsPagerAdapter.getRecentCallsListPosition());
                    break;
                default:
                    // default statement
            }
        }
    }

    /**
     * Starting contact picker for call transfer process
     *
     * @param mCallId of active call to which we want to transfer picked contact
     */
    @Override
    public void startContactPickerForCallTransfer(int mCallId) {
        isConferenceCall = false;
        mCallActiveCallID = mCallId;
        setAddParticipant(true);
        mActiveCallRequestCode = Constants.TRANSFER_REQUEST_CODE;
        tabLayoutReset();
    }

    /**
     * Starting contact picker for call merge process
     *
     * @param mCallId of active call which we want to merge to conference with picked contact
     */
    @Override
    public void startContactPickerForConference(int mCallId) {
        isConferenceCall = true;
        mCallActiveCallID = mCallId;
        setAddParticipant(true);
        mActiveCallRequestCode = Constants.CONFERENCE_REQUEST_CODE;
        tabLayoutReset();
    }

    /**
     * Canceling contact picker we opened for {@link #startContactPickerForCallTransfer(int)}
     * or {@link #startContactPickerForConference(int)}
     */
    @Override
    public void cancelContactPicker() {
        isConferenceCall = false;
        mCallActiveCallID = 0;
        setAddParticipant(false);
        mActiveCallRequestCode = 0;
        tabLayoutReset();
    }


    /**
     * Preparing views when call is started. Based on parameter isVideo it will prepare audio only
     * or video call
     *
     * @param isVideo is call audio only or video call
     */
    @Override
    public void onCallStarted(boolean isVideo) {
        Utils.hideKeyboard(this);

        if(getResources().getBoolean(R.bool.is_landscape) == true){
            mSlideSelectPhoneNumber.collapse(mSelectPhoneNumber,true);
        }else{
            mSlideSelectPhoneNumber.collapse(mSelectPhoneNumber,false);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        boolean isMuteEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().isMuteEnabled();
        boolean isVideoEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled();

        mAudioMute.setEnabled(isMuteEnabled);

        if(getResources().getBoolean(R.bool.is_landscape) == false) {
            if (isMuteEnabled)
                mAudioMute.setVisibility(View.VISIBLE);
            else
                mAudioMute.setVisibility(View.INVISIBLE);
        }

        mVideoMute.setEnabled(isMuteEnabled & isVideoEnabled);

        if(getResources().getBoolean(R.bool.is_landscape) == false) {
            if (hasVideoCamera() && isMuteEnabled & isVideoEnabled)
                mVideoMute.setVisibility(View.VISIBLE);
            else
                mVideoMute.setVisibility(View.GONE);
        }

        mVideoMute.setClickable(isMuteEnabled & isVideoEnabled);

        boolean isVideoMuted = SDKManager.getInstance().getCallAdaptor().ismVideoMuted();
        mVideoMute.setChecked(isVideoMuted);

        mSelectAudio.setChecked(true);

    }

    /**
     * Proces of preparation of  {@link MainActivity} where we are hiding unnecessary views, enabling
     * {@link ToggleButton} for video and audio mute. {@link UICallViewAdaptor} audio and video mute
     * state is set to false
     */
    @Override
    public void onCallEnded() {

        ActiveCallFragment activeCallFragment = (ActiveCallFragment) (getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG));
        if(activeCallFragment!=null && activeCallFragment.alertDialog!=null )
            activeCallFragment.alertDialog.dismiss();

        Intent intentKillCallDialerActivity = new Intent("com.avaya.endpoint.FINISH_CALL_ACTIVITY");
        sendBroadcast(intentKillCallDialerActivity);


        if (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getFragmentContacts() != null){
            mSectionsPagerAdapter.getFragmentContacts().unblockListClick();
        }
        if (mEmergencyWithoutLogin) {
            // back to the login screen
            Log.d(TAG, "End emergency call during first time activation.");
            mEmergencyWithoutLogin = false;
            final Intent intent = new Intent();
            intent.setAction(Constants.SERVICE_STATE_CHANGE);
            intent.putExtra(Constants.KEY_SERVICE_TYPE_EXTRA, Constants.SIP_SERVICE_TYPE);
            intent.putExtra(Constants.KEY_SERVICE_STATUS_EXTRA, Constants.FAIL_STATUS);
            intent.putExtra(Constants.KEY_SERVICE_RETRY_EXTRA, 0);
            sendBroadcast(intent);
            finish();
            return;
        }

        if (SDKManager.getInstance().getCallAdaptor().hasActiveCalls() == false) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            mAudioMute.setEnabled(false);
            mAudioMute.setChecked(false);
            mCallViewAdaptor.changeAudioMuteState(false);

            mVideoMute.setEnabled(false);
            mVideoMute.setChecked(false);
            mCallViewAdaptor.changeVideoMuteState(false);

            mSelectAudio.setChecked(false);

            if(getResources().getBoolean(R.bool.is_landscape) == true){
                try {
                    if (isFragmentVisible(DIALER_FRAGMENT)) {
                        ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).offHook.setChecked(false);
                    }
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
                searchAddFilterIconViewController();
                FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
                List<Fragment> fragments = fragmentManager.getFragments();
                if(fragments != null){
                    for(Fragment fragment : fragments){
                        if(fragment != null && fragment.isVisible())
                            if (fragment instanceof ContactsFragment) {
                                ((ContactsFragment) fragment).removeSearchResults();
                            } else if (fragment instanceof ContactDetailsFragment || fragment instanceof ContactEditFragment) {
                                changeUiForFullScreenInLandscape(true);
                                break;
                            }else {
                                changeUiForFullScreenInLandscape(false);
                            }
                    }
                }
            }

            // by some reason configuration that effects DialerFragment UI (like ENABLE_REDIAL) is not
            // always properly restored after ActiveCallFragment destroy
            if (mSectionsPagerAdapter.getDialerFragment() != null) {
                mSectionsPagerAdapter.getDialerFragment().applyConfigChange();
                mSectionsPagerAdapter.getDialerFragment().onMessageWaitingStatusChanged(SDKManager.getInstance().getVoiceMessageAdaptor().getVoiceState());
            }
            setFeatureMenuOpen(false);

            if(getResources().getBoolean(R.bool.is_landscape) == true) {
                hideMenus();
                showingFirstRecent = true;
                filterButton.setImageResource(R.drawable.ic_expand_more);
            }
        }
        if(mActiveCall!=null)
            mActiveCall.setClickable(false);


    }

    /**
     * In case that feature menu is opened in {@link ActiveCallFragment} we have to blur {@link MainActivity}
     * frame as it is not possible to do that directly from {@link ActiveCallFragment}
     *
     * @param isOpen boolean
     */
    @Override
    public void setFeatureMenuOpen(boolean isOpen) {
        if (mBlureFrame != null) {
            if (isOpen) {
                mBlureFrame.setVisibility(View.VISIBLE);
            } else {
                mBlureFrame.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Setting {@link ToggleButton} for audio selection on or off based on parameters provided
     *
     * @param isOn boolean based on which {@link ToggleButton} is set
     */
    @Override
    public void setOffhookButtosChecked(boolean isOn) {
        mSelectAudio.setChecked(isOn);

        if(getResources().getBoolean(R.bool.is_landscape) == true){
            if (mSectionsPagerAdapter.getDialerFragment() != null && mSectionsPagerAdapter.getDialerFragment().offHook != null){
                mSectionsPagerAdapter.getDialerFragment().offHook.setChecked(isOn);
            }

            ActiveCallFragment fragment = (ActiveCallFragment) (getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG));
            if (fragment != null && fragment.offHook != null) {
                fragment.offHook.setChecked(isOn);
            }
        }
    }

    public boolean isOffhookChecked(){
        return mSelectAudio.isChecked();
    }

    public void setOffHookButtonsBasedCallState(int callId, UICallState state){
        boolean isChecked=false;
        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            int offhookCallId = SDKManager.getInstance().getCallAdaptor().getOffhookCallId();
            if (offhookCallId != 0 && offhookCallId != callId)
                isChecked=true;
            else if (state == ESTABLISHED || state == REMOTE_ALERTING || state ==FAILED || state==TRANSFERRING) {
                isChecked=true;
            }

            setOffhookButtosChecked(isChecked);

        }

    }

    /**
     * handling result of transfer/conference action
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case TRANSFER_REQUEST_CODE:
                Log.d(TAG, "transfer request arrived");
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        int callId = data.getIntExtra(CALL_ID, 0);
                        String target = data.getStringExtra(TARGET);
                        Log.d(TAG, "Call transfered: " + callId + " Target: " + target);
                        mCallViewAdaptor.transferCall(callId, target);

                        GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.FEATURE_TRANSFER_EVENT);

                    } else {
                        Log.e(TAG, "not enough data to perform transfer");
                    }
                    ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
                    if (activeCallFragment != null) {
                        activeCallFragment.dismissMenu();
                    }
                }
                break;
            case CONFERENCE_REQUEST_CODE:
                Log.d(TAG, "conference request arrived");
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        int callId = data.getIntExtra(CALL_ID, 0);
                        String target = data.getStringExtra(TARGET);
                        mCallViewAdaptor.conferenceCall(callId, target);

                        GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.FEATURE_CONFERENCE_EVENT);

                    } else {
                        Log.e(TAG, "not enough data to perform conference");
                    }
                }
                ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
                if (activeCallFragment != null) {
                    activeCallFragment.dismissMenu();
                }
                if (mSectionsPagerAdapter.isCallAddParticipant()) {
                    //noinspection ConstantConditions
                    if (activeCallFragment != null) {
                        cancelContactPicker();
                        activeCallFragment.setVisible();
                    }
                    CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
                    if (callStatusFragment != null) {
                        callStatusFragment.hideCallStatus();
                        Utils.hideKeyboard(this);
                    }
                }

                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }


    /**
     *
     * Returns the Tab name (Favorites, Contacts, History) used for the access to the specific Contact details
     */
    @Override
    public String getContactCapableTab() {
        switch (mContactCapableTab) {
            case Favorites:
                return "Favorites";
            case Contacts:
                return "Contacts";
            case History:
                return "History";
            default:
                return null;

        }
    }

    @Override
    public void saveSelectedCallCategory(CallData.CallCategory callCategory) {
        mSelectedCallCategory = callCategory;
    }

    /**
     * Store this value in main activity because of the recent
     * fragment recreation.
     *
     * @param position last position of the visible items
     */
    @Override
    public void onPositionToBeSaved(Parcelable position) {
        this.position = position;
    }

    /**
     * Prepare configuration changes and inform fragments about it
     */
    private void applyConfigChange() {
        // check which tabs shall be present in the section selection bar
        mSectionsPagerAdapter.configureTabLayout();
        tabLayoutReset();

        // inform all fragment that some change in UI representation might happen
        List<Fragment> allFragments = getSupportFragmentManager().getFragments();
        for (Fragment f : allFragments) {
            if (f instanceof ConfigChangeApplier) {
                ((ConfigChangeApplier) f).applyConfigChange();
                break;
            }
        }

        if (mSectionsPagerAdapter.getDialerFragment() != null) {
            mSectionsPagerAdapter.getDialerFragment().applyConfigChange();
        }

        loadBrand(getBrand(), true);

        configureUserPreferenceAccess();

        // working on refreshing fragments with new information regarding name display and name sort
        mAdminNameSortOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_SORT_ORDER);
        mAdminNameDisplayOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_DISPLAY_ORDER);
        mAdminChoiceRingtone = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.ADMIN_CHOICE_RINGTONE);

        // managing name sort preference
        if (mAdminNameSortOrder != null && !mAdminNameSortOrder.equals(mPreviousAdminNameSortOrder)) {
            getLoaderManager().restartLoader(Constants.LOCAL_ADDRESS_LOADER, null, mSectionsPagerAdapter.getLocalContacts());
            SharedPreferences.Editor editor = mSharedPrefs.edit();
            editor.putBoolean(Constants.REFRESH_FAVORITES, true);
            editor.putBoolean(Constants.REFRESH_CONTACTS, true);
            editor.putBoolean(Constants.REFRESH_RECENTS, true);
            editor.apply();
            mPreviousAdminNameSortOrder = mAdminNameSortOrder;
        }

        // managing admin choice ringtone
        SharedPreferences adminRingPreference = getSharedPreferences(Constants.ADMIN_RINGTONE_PREFERENCES, MODE_PRIVATE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        //checking if user has already selected some ringtone
        boolean ringtoneSettingsExist = prefs.contains(Constants.CUSTOM_RINGTONE_PREFERENCES);
        // if there is a change to admin settings, but user has set his ringtone preference, we just
        // make sure admin ringtone is default ringtone
        if (mAdminChoiceRingtone != null && !mAdminChoiceRingtone.equals(mPreviousAadminChoiceRingtone) && ringtoneSettingsExist) {
            // settings exist, going back to default
            SharedPreferences.Editor editor = adminRingPreference.edit();
            editor.putString(Constants.ADMIN_RINGTONE_PREFERENCES, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
            editor.apply();
        }

        // if there is a change to admin settings and use did not change his ringtone settings,
        // we will use ringtone set by admin
        if (mAdminChoiceRingtone != null && !mAdminChoiceRingtone.equals(mPreviousAadminChoiceRingtone) && !ringtoneSettingsExist) {
            String ringtoneFound = "";
            RingtoneManager ringtoneMgr = new RingtoneManager(this);
            ringtoneMgr.setType(RingtoneManager.TYPE_RINGTONE);
            Cursor ringToneCursor = ringtoneMgr.getCursor();
            while (ringToneCursor.moveToNext()) {
                int currentPosition = ringToneCursor.getPosition();
                Uri ringtoneUri = ringtoneMgr.getRingtoneUri(currentPosition);
                if (ringtoneUri != null) {
                    Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                    if (ringtone != null && mAdminChoiceRingtone.toLowerCase().equals(ringtone.getTitle(this).toLowerCase())) {
                        SharedPreferences.Editor editor = adminRingPreference.edit();
                        editor.putString(Constants.ADMIN_RINGTONE_PREFERENCES, ringtoneUri.toString());
                        ringtoneFound = ringtoneUri.toString();
                        editor.apply();
                    }
                }
            }
            // if no matching found, we use default system ringtone
            if (ringtoneFound.equals("")) {
                SharedPreferences.Editor editor = adminRingPreference.edit();
                editor.putString(Constants.ADMIN_RINGTONE_PREFERENCES, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
                editor.apply();
            }
            mPreviousAadminChoiceRingtone = mAdminChoiceRingtone;
        }

        applyLockSetting();

    }

    /**
     * Starts or stops the LockTask depending on the current settings
     */
    private void applyLockSetting() {

        DevicePolicyManager devicePolicyManager = getSystemService(DevicePolicyManager.class);
        ActivityManager activityManager = getSystemService(ActivityManager.class);
        if (TextUtils.equals(getApplicationInfo().packageName, SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue("PIN_APP")) &&
                devicePolicyManager.isLockTaskPermitted(getApplicationInfo().packageName)) {
            SharedPreferences p = getSharedPreferences(getApplicationInfo().packageName + "_preferences", MODE_PRIVATE);
            String pinApp = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.PIN_APP);
            if (Arrays.asList(pinApp.split(",")).contains(getPackageName())) {
                boolean isPinned = p.getBoolean(Constants.EXIT_PIN, true);
                Log.d(TAG, String.format("applyLockSetting : isPinned=%b isRegistered=%b isAnonymous=%b",
                        isPinned, (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getRegisteredUser() != null), SDKManager.getInstance().getDeskPhoneServiceAdaptor().isAnonymous()));
                if (isPinned && (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getRegisteredUser() != null) && !SDKManager.getInstance().getDeskPhoneServiceAdaptor().isAnonymous()) {
                    p.edit().putBoolean(Constants.EXIT_PIN, true).apply();
                    if (activityManager.getLockTaskModeState() != LOCK_TASK_MODE_LOCKED) {
                        if (!isForeground()) {
                            Intent intentHome = new Intent(getApplicationContext(), MainActivity.class);
                            intentHome.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentHome);
                        }
                        else {
                            startLockTask();
                            ElanApplication.isPinAppLock = true;
                        }
                    }
                } else if (activityManager.getLockTaskModeState() == LOCK_TASK_MODE_LOCKED) {
                    stopLockTask();
                    ElanApplication.isPinAppLock = false;
                }
            }
        }

    }

    /**
     * @return true if the Application is in the foreground
     */
    private boolean isForeground() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
    }


    /**
     * Load logo of company for which application have to be branded
     *
     * @param brand
     * @param overrideCache should we override cache
     */
    private void loadBrand(String brand, boolean overrideCache) {
        if (overrideCache) {
            mBrandPref.edit().putString(ConfigParametersNames.BRAND_URL.getName(), brand).apply();
        }
        loadBrand(getBrand());
    }

    /**
     * Configure the accessibility of "User Preferences" menu
     */
    private void configureUserPreferenceAccess() {
        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor()
                .getConfigBooleanParam(ConfigParametersNames.PROVIDE_OPTIONS_SCREEN)) {
            mOptionUserSettings.setVisibility(View.VISIBLE);
        } else {
            mOptionUserSettings.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * @param addParticipant Display or remove header for Picking contact depending on this param
     */
    private void setAddParticipant(boolean addParticipant) {
        if (mSectionsPagerAdapter == null) {
            Log.e(TAG, "error: setAddParticipant was called before mSectionsPagerAdapter was created");
            return;

        }

        if(mSectionsPagerAdapter!=null)
            mSectionsPagerAdapter.setCallAddParticipant(addParticipant);

        if (addParticipant) {
            mPickContacts.setVisibility(View.VISIBLE);

        } else {
            mPickContacts.setVisibility(View.GONE);
            // hiding contact picker in case phone call is ended during the action of picking a number to transfer to.
            if(getResources().getBoolean(R.bool.is_landscape) == true){
                mSlideSelectPhoneNumber.collapse(mSelectPhoneNumber,true);
            }else{
                mSlideSelectPhoneNumber.collapse(mSelectPhoneNumber,false);
            }

            mFrameAll.setVisibility(View.GONE);
        }
        if (mSectionsPagerAdapter.getFragmentContacts() != null && mSectionsPagerAdapter.isContactsTabPresent()) {
            mSectionsPagerAdapter.getFragmentContacts().setAddParticipantData(addParticipant);
        }
        if (mSectionsPagerAdapter.getFragmentFavorites() != null && mSectionsPagerAdapter.isFavoriteTabPresent()) {
            mSectionsPagerAdapter.getFragmentFavorites().setAddParticipantData(addParticipant);
        }
        if (mSectionsPagerAdapter.getFragmentRecent() != null && mSectionsPagerAdapter.isRecentTabPresent()) {
            mSectionsPagerAdapter.getFragmentRecent().setAddParticipantData(addParticipant);
        }
    }

    /**
     * Local broadcast receiver from SDK connectivity changes and Snackebar showing.
     */
    private BroadcastReceiver mConfigChangeAndSnackbarReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Constants.LOCAL_CONFIG_CHANGE)) {
                boolean loginSuccessful = intent.getBooleanExtra(Constants.CONFIG_CHANGE_STATUS, false);
                if (mErrorStatus == null
                        || loginSuccessful ^ mErrorStatus.getVisibility() == View.VISIBLE) {
                    return;
                }

                checkForErrors();
            } else if (intent.getAction().equalsIgnoreCase(Constants.SNACKBAR_SHOW)) {
                String message = intent.getStringExtra(Constants.SNACKBAR_MESSAGE);
                boolean msgLength = intent.getBooleanExtra(Constants.SNACKBAR_LENGTH, false);
                if (message != null && !message.isEmpty()) {
                    Snackbar.make(mActivityLayout, message, msgLength ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG).show();
                }
            }
        }
    };

    /**
     * Local broadcast receiver for phone's unlock event.
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_USER_PRESENT)) {
                Log.d(TAG, "Phone was unlocked");
                ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
                if (activeCallFragment != null) {
                    activeCallFragment.setEmergencyAndLockFeature();
                }
            }
            else if (intent.getAction().equalsIgnoreCase(ACTION_USB_ATTACHED)){
                Log.d(TAG, "USB attached");
                //check if the usb device attached is camera
                if ((((UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)).getDeviceClass() & UsbConstants.USB_CLASS_VIDEO) != 0){
                    //usb device is camera - so video button should be visible
                    mSectionsPagerAdapter.getDialerFragment().setVideoButtonVisibility(View.VISIBLE);
                    mVideoMute.setVisibility(View.VISIBLE);
                }
            }
            else if (intent.getAction().equalsIgnoreCase(ACTION_USB_DETACHED)){
                Log.d(TAG, "USB detached");
                //check if the usb device detached is camera
                if (intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) != null && (((UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)).getDeviceClass() & UsbConstants.USB_CLASS_VIDEO) != 0){
                    //check if we have internal camera
                    if (hasVideoCamera() == false)
                        //we don't have any camera (not usb and not internal) - so video button should not be visible
                        mSectionsPagerAdapter.getDialerFragment().setVideoButtonVisibility(View.GONE);
                        mVideoMute.setVisibility(View.GONE);
                }
            }
        }
    };

    /**
     * @return true if the video camera is available
     */
    public boolean hasVideoCamera()
    {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        try
        {
            int cameraCount = Camera.getNumberOfCameras();
            for (int cameraId = 0; cameraId < cameraCount; cameraId++)
            {
                Camera.getCameraInfo(cameraId, cameraInfo);
                if (cameraInfo.facing == CAMERA_FACING_FRONT || cameraInfo.facing == CAMERA_FACING_BACK )
                {
                    return true;
                }
            }
        }
        catch (Throwable e)
        {
            Log.e("VideoCaptureController", "failed to get camera info", e);
        }
        return false;
    }

    /**
     * Processing device chnage logic based of {@link UIAudioDevice} provided in parameters and
     * boolean value which represent is device active
     *
     * @param device {@link UIAudioDevice}
     * @param active boolean representing is device active
     */
    @Override
    public void onDeviceChanged(UIAudioDevice device, boolean active) {
        Log.d(TAG, "onDeviceChanged. device=" + device + " active=" + active);
        saveAudioSelection(Constants.AUDIO_PREF_KEY, device.toString());


        int resId = getResourceIdForDevice(device);
        mSelectAudio.setBackgroundResource(resId);

        mSelectAudio.setChecked(active);
        if(getResources().getBoolean(R.bool.is_landscape) == true) {

            if (mSectionsPagerAdapter.getDialerFragment() != null && mSectionsPagerAdapter.getDialerFragment().offHook != null){
                if(isFragmentVisible(CONTACTS_DETAILS_FRAGMENT)==true) {
                    ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)).isBackORDeletePressed = true;
                }

                mSectionsPagerAdapter.getDialerFragment().offHook.setChecked(active);
                mSectionsPagerAdapter.getDialerFragment().offHook.setBackgroundResource(resId);
            }
            ActiveCallFragment fragment = (ActiveCallFragment) (getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG));
            if (fragment != null) {
                fragment.offHook.setChecked(active);
                fragment.offHook.setBackgroundResource(resId);
            }
        }





        if (mToggleAudioMenu.getVisibility() == View.VISIBLE) {
            if(getResources().getBoolean(R.bool.is_landscape) == true)
                mSlideSelecAudioDevice.collapse(mToggleAudioMenu,true);
            else
                mSlideSelecAudioDevice.collapse(mToggleAudioMenu,false);
        }
    }

    /**
     * Obtaining {@link UIContactsViewAdaptor}
     *
     * @return {@link UIContactsViewAdaptor} in use
     */
    @Override
    public UIContactsViewAdaptor getContactViewAdapter() {
        return mUIContactsViewAdaptor;
    }

    /**
     * Processing onHook event for provided handset type
     *
     * @param handsetType type of handset which changed state to on hook
     */
    @Override
    public void onOnHook(HandSetType handsetType) {
        mIsOffHook = false;
        resetDialer();
    }

    /**
     * Resets the DialerFragment
     */
    public void resetDialer() {
        DialerFragment dialerFragment = mSectionsPagerAdapter.getDialerFragment();
        if (dialerFragment != null&&  SDKManager.getInstance().getCallAdaptor().getOffhookCallId() ==0) {
            dialerFragment.setMode(DialerFragment.DialMode.EDIT);
        }
        if (mScreenLock != null && mScreenLock.isHeld()) {
            mScreenLock.release();
        }
    }

    /**
     * Processing off hook event of provided handset type
     *
     * @param handsetType type of handset which changed state to off hook
     */
    @Override
    public void onOffHook(HandSetType handsetType) {
        if (!mCallStateEventHandler.hasIncomingCall()) {
            mIsOffHook = true;
            prepareOffHook();
        }
    }

    /**
     * Prepares app state for the off hook
     */
    public void prepareOffHook() {
        if (mCallStateEventHandler.hasIncomingCall()) {
            Log.d(TAG, "prepareOffHook() is called for incomig call -> retrun");
            return;
        }
        if ((SDKManager.getInstance().getCallAdaptor().getCall(SDKManager.getInstance().getCallAdaptor().getActiveCallIdWithoutOffhook()) == null) && !isLockState(this)) {
            if (mViewPager != null) {
                mViewPager.setCurrentItem(0, false);//jump to dialer
                Fragment f = getVisibleFragment(CONTACTS_EDIT_FRAGMENT);
                if (f != null && f.getView() != null ) {
                    View cancel = f.getView().findViewById(R.id.contact_edit_cancel);
                    if (cancel != null) {
                        cancel.performClick();
                    }
                }
            }
            try {
                if ( mSectionsPagerAdapter.getDialerFragment() != null) {
                    mSectionsPagerAdapter.getDialerFragment().setMode(DialerFragment.DialMode.OFF_HOOK);
                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }
            ActiveCallFragment activeCallFragment = (ActiveCallFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.ACTIVE_CALL_TAG);
            if (activeCallFragment != null && activeCallFragment.getView() != null && activeCallFragment.isVisible()) {
                ImageView back = (ImageView) activeCallFragment.getView().findViewById(R.id.back);
                back.performClick();
            }

            CallStatusFragment callStatusFragment = (CallStatusFragment) getSupportFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);

            final int heldCallId = SDKManager.getInstance().getCallAdaptor().getHeldCallId();
            if(callStatusFragment != null && heldCallId > 0 && callStatusFragment.getCallId() == heldCallId && !callStatusFragment.isVisible())
                callStatusFragment.showCallStatus();

        }

        PowerManager pm = ((PowerManager) getApplicationContext().getSystemService(POWER_SERVICE));
        if (!pm.isInteractive()) {
            mScreenLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
            mScreenLock.acquire();
        }

        if(getResources().getBoolean(R.bool.is_landscape) == true){
            if(!isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT) && isFragmentVisible(CONTACTS_FRAGMENT) == false && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT))!=null && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout!=null && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).seacrhLayout.getVisibility() !=  View.VISIBLE)
                changeUiForFullScreenInLandscape(false);
            if (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT))
                ((ContactDetailsFragment) getVisibleFragment(CONTACTS_DETAILS_FRAGMENT)). mBackListener.back();
        }
    }

    /**
     * @param context Activity context
     * @return true if the device is locked
     */
    private boolean isLockState(Context context) {
        KeyguardManager kgMgr = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return (kgMgr != null) && kgMgr.isDeviceLocked() && !ElanApplication.isPinAppLock;
    }

    /**
     * Processing configuration change
     *
     * @param newConfig {@link Configuration} to be applied
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mLocale != null && mLocale != newConfig.locale || mFontScale != newConfig.fontScale) {
            isConfigChanged = true;
            handleConfigChanges();
        }
    }

    /**
     * Processing incoming call end notification
     */
    @Override
    public void onIncomingCallEnded() {

        if (mSectionsPagerAdapter != null) {
            if (mViewPager != null && mTabIndexMap.get(Tabs.History) != null
                    && mSectionsPagerAdapter.getFragmentRecent() == null) {
                mViewPager.setCurrentItem(mTabIndexMap.get(Tabs.History), false);
            }
            try {
                if (isFragmentVisible(DIALER_FRAGMENT)) {
                    ((DialerFragment) getVisibleFragment(DIALER_FRAGMENT)).setMode(DialerFragment.DialMode.EDIT);
                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }
        if (mTabIndexMap.get(Tabs.History) != null) {
            setHistoryIcon(mTabIndexMap.get(Tabs.History));
        }

        if (mCallViewAdaptor != null && mCallViewAdaptor.getNumOfCalls() == 0) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if(getResources().getBoolean(R.bool.is_landscape) == true){

            searchAddFilterIconViewController();

            if( (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) || isFragmentVisible(CONTACTS_EDIT_FRAGMENT) || isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)) && !isFragmentVisible(DIALER_FRAGMENT) ){
                changeUiForFullScreenInLandscape(true);
                if( isFragmentVisible(ACTIVE_CALL_FRAGMENT) && !isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT))
                    changeUiForFullScreenInLandscape(false);
            }else  if( (isFragmentVisible(CONTACTS_DETAILS_FRAGMENT) || isFragmentVisible(CONTACTS_EDIT_FRAGMENT) || isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT)) ) {
                changeUiForFullScreenInLandscape(true);

            }else if (  mSectionsPagerAdapter.getFragmentContacts() != null && mSectionsPagerAdapter.getFragmentContacts().seacrhLayout.getVisibility() == View.VISIBLE   ){
                changeUiForFullScreenInLandscape(true);
            }else{
                changeUiForFullScreenInLandscape(false);
            }
        }
        if(mActiveCall!=null)
           mActiveCall.setClickable(false);
    }

    /**
     * Controls visibility of the search, addContact and filter buttons.
0     * based on the state of the viewPager.
     */
    public void searchAddFilterIconViewController(){
        try {
            if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CONTACTS) == true &&
                    SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) == true && mViewPager!=null) {

                if(mSectionsPagerAdapter.getCount() == 4){
                    if (mViewPager.getCurrentItem() == 0) {
                        searchButton.setVisibility(View.INVISIBLE);
                        addcontactButton.setVisibility(View.INVISIBLE);
                        filterButton.setVisibility(View.INVISIBLE);
                    } else if (mViewPager.getCurrentItem() == 1) {
                        searchButton.setVisibility(View.INVISIBLE);
                        addcontactButton.setVisibility(View.INVISIBLE);
                        filterButton.setVisibility(View.INVISIBLE);
                    } else if (mViewPager.getCurrentItem() == 2) {
                        searchButton.setVisibility(View.VISIBLE);
                        addcontactButton.setVisibility(View.VISIBLE);
                        filterButton.setVisibility(View.INVISIBLE);
                    } else if (mViewPager.getCurrentItem() == 3) {
                        searchButton.setVisibility(View.INVISIBLE);
                        addcontactButton.setVisibility(View.INVISIBLE);
                        filterButton.setVisibility(View.VISIBLE);
                    }
                }else if(mSectionsPagerAdapter.getCount() == 3){
                    if (mViewPager.getCurrentItem() == 0) {
                        searchButton.setVisibility(View.INVISIBLE);
                        addcontactButton.setVisibility(View.INVISIBLE);
                        filterButton.setVisibility(View.INVISIBLE);
                    } else if (mViewPager.getCurrentItem() == 1) {
                        searchButton.setVisibility(View.VISIBLE);
                        addcontactButton.setVisibility(View.VISIBLE);
                        filterButton.setVisibility(View.INVISIBLE);
                    } else if (mViewPager.getCurrentItem() == 2) {
                        searchButton.setVisibility(View.INVISIBLE);
                        addcontactButton.setVisibility(View.INVISIBLE);
                        filterButton.setVisibility(View.VISIBLE);
                    }
                }



            } else if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CONTACTS) == true &&
                    SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) == false) {
                if (mViewPager.getCurrentItem() == 0) {
                    searchButton.setVisibility(View.INVISIBLE);
                    addcontactButton.setVisibility(View.INVISIBLE);
                    filterButton.setVisibility(View.INVISIBLE);
                } else if (mViewPager.getCurrentItem() == 1) {
                    searchButton.setVisibility(View.VISIBLE);
                    addcontactButton.setVisibility(View.VISIBLE);
                    filterButton.setVisibility(View.INVISIBLE);
                } else if (mViewPager.getCurrentItem() == 2) {
                    searchButton.setVisibility(View.INVISIBLE);
                    addcontactButton.setVisibility(View.INVISIBLE);
                    filterButton.setVisibility(View.VISIBLE);
                }
            } else if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CONTACTS) == false &&
                    SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) == true) {
                if (mViewPager.getCurrentItem() == 0) {
                    searchButton.setVisibility(View.INVISIBLE);
                    addcontactButton.setVisibility(View.INVISIBLE);
                    filterButton.setVisibility(View.INVISIBLE);
                } else if (mViewPager.getCurrentItem() == 1) {
                    searchButton.setVisibility(View.INVISIBLE);
                    addcontactButton.setVisibility(View.INVISIBLE);
                    filterButton.setVisibility(View.INVISIBLE);
                } else if (mViewPager.getCurrentItem() == 2) {
                    searchButton.setVisibility(View.INVISIBLE);
                    addcontactButton.setVisibility(View.INVISIBLE);
                    filterButton.setVisibility(View.VISIBLE);
                }
            } else if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_CONTACTS) == false &&
                    SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_FAVORITES) == false) {
                if (mViewPager.getCurrentItem() == 0) {
                    searchButton.setVisibility(View.INVISIBLE);
                    addcontactButton.setVisibility(View.INVISIBLE);
                    filterButton.setVisibility(View.INVISIBLE);
                } else if (mViewPager.getCurrentItem() == 1) {
                    searchButton.setVisibility(View.INVISIBLE);
                    addcontactButton.setVisibility(View.VISIBLE);
                    filterButton.setVisibility(View.INVISIBLE);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Processing incoming call start notification
     */
    @Override
    public void onIncomingCallStarted() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(isFragmentVisible(CONTACTS_EDIT_FRAGMENT)==true &&  ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)).alertDialog!=null)
             ((ContactEditFragment) getVisibleFragment(CONTACTS_EDIT_FRAGMENT)).alertDialog.dismiss();

        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            hideMenus();
            checkFilterButtonState();

            if(isFragmentVisible(CONTACTS_FRAGMENT)==true && ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).alertDialog!=null){
                ((ContactsFragment) getVisibleFragment(CONTACTS_FRAGMENT)).alertDialog.dismiss();
            }

            if(isFragmentVisible(HISTORY_FRAGMENT)==true && ((RecentCallsFragment) getVisibleFragment(HISTORY_FRAGMENT)).alertDialog!=null){
                ((RecentCallsFragment) getVisibleFragment(HISTORY_FRAGMENT)).alertDialog.dismiss();
            }
        }
    }

    /**
     * If showing filter screen is already present,
     * we shouldn't set flag showingFirstRecent to true again.
     */
    private void checkFilterButtonState() {
        if (!showingFirstRecent) return;
        showingFirstRecent =  true;
        filterButton.setImageResource(R.drawable.ic_expand_more);
    }

    /**
     * Processing on video muted event and setting adequate value for video mute {@link ToggleButton}
     *
     * @param uiCall {@link UICall}
     * @param muting boolean based on which {@link ToggleButton} is set
     */
    @Override
    public void onVideoMuted(UICall uiCall, boolean muting) {
        Log.d(TAG, "onVideoMuted: muting=" + muting);
        mVideoMute.setEnabled(true);
        mVideoMute.setChecked(muting);
    }

    /**
     * Process missed call and refresh history icon after onCallRemoved in {@link CallAdaptor}
     * is finished executing
     */
    @Override
    public void onCallMissed() {
        onIncomingCallEnded();
    }

    /**
     * Runnable responsible for obtaining parameters from parameter store
     */
    private static class ParameterUpdateRunnable implements Runnable {
        private final TextView mBoundView;
        private final String mBoundParameter;

        ParameterUpdateRunnable(TextView username, String boundParameter) {
            mBoundView = username;
            mBoundParameter = boundParameter;
        }

        @Override
        public void run() {
            if (mBoundView != null) {
                String name = VantageDBHelper.getParameter(mBoundView.getContext().getContentResolver(), mBoundParameter);
                if (name != null) {
                    mBoundView.setText(name);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {

        if(getResources().getBoolean(R.bool.is_landscape) == true && isToBlockBakcPress == true) {
            isToBlockBakcPress = false;
            return;
        }

        Log.d(TAG, "onBackPressed Called");
        ActivityManager am = getSystemService(ActivityManager.class);
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (am.getLockTaskModeState() != LOCK_TASK_MODE_LOCKED) {
            startActivity(setIntent);
        }
        else {
            String pinApp = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.PIN_APP);
            if(!TextUtils.isEmpty(pinApp)) {
                String KIOSK = "com.avaya.endpoint.avayakiosk";
                Intent launchIntentForPackage;
                if (Arrays.asList(pinApp.split(",")).contains(KIOSK)) {
                    launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(KIOSK);
                    if(launchIntentForPackage !=null) {
                        startActivity(launchIntentForPackage);
                    }
                    else {
                        startActivity(setIntent);
                    }
                }
                else {
                    String firstApp = pinApp.split(",")[0];
                    launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(firstApp);
                    if (launchIntentForPackage != null) {
                        startActivity(launchIntentForPackage);
                    } else {
                        startActivity(setIntent);
                    }
                }
            }

        }
    }

    @Override
    public void onSaveSelectedCategoryRecentFragment(CallData.CallCategory callCategory) {
        mSelectedCallCategory = callCategory;
    }

    /**
     * We should refresh history icon in case recent calls tab is shown
     * and missed call is logged. When user clears all calls from recent call tab,
     * we reset value and update icon.
     */
    @Override
    public void refreshHistoryIcon() {
        resetMissedCalls();
    }

    /**
     * Starts {@link NotificationService} and creates mNotifFactory.
     */
    private void initNotifications() {

        // fist check whether notifcation mechanism was started already
        if (mNotifFactory != null) {
            return;
        }
        // start the notification mechanism
        startService(new Intent(this, NotificationService.class));
        mNotifFactory = new CallNotificationFactory(getApplicationContext());
    }

    /**
     * Starts {@link BluetoothBackgroundService}
     */
    private void initBluetoothChangeListener() {
        startService(new Intent(this, BluetoothStateService.class));
    }

    /**
     * Initialize the app after crash or manual launch.
     */
    private void handleSpecialInitCases() {

        // special handle for manual application launch after crash
        if ((getIntent() != null) && (getIntent().getAction() == null) && !getIntent().getBooleanExtra(Constants.CONFIG_RECEIVER, false)) {
            Log.d(TAG, "Start application from Launcher.");
            SDKManager.getInstance().getDeskPhoneServiceAdaptor().initAfterCrash();
        }

        // special handle for emergency during first activation
        if (mEmergencyWithoutLogin) {
            Log.i(TAG, "This is emergency during first time activation");
            handleIntent(getIntent());
        }


        // restore the calls if any ongoing calls exists
        if (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 0) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    SDKManager.getInstance().getCallAdaptor().restoreCalls();
                }
            }, 1000);
        }

        //ELAN-1000
        if ((getIntent() != null) && (getIntent().getAction() == Intent.ACTION_VIEW) && getIntent().hasExtra(Constants.GO_TO_FRAGMENT) && Constants.GO_TO_FRAGMENT_MISSED_CALLS.equalsIgnoreCase(getIntent().getStringExtra(Constants.GO_TO_FRAGMENT))){
            handleIntent(getIntent());
        }

    }


    /**
     * Restores incoming calls
     */
    private void restoreIncomingCalls(){
        if (SDKManager.getInstance().getCallAdaptor().isAlertingCall() != 0){
            SDKManager.getInstance().getCallAdaptor().restoreIncomingCalls();
        }
    }

    /**
     * Receives call backs for changes to Recent calls and contacts
     */
    class RecentCallsAndContactObserver extends ContentObserver {

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        public RecentCallsAndContactObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            try {
                if (mSectionsPagerAdapter != null && mSectionsPagerAdapter.getFragmentRecent() != null
                        && mSectionsPagerAdapter.getFragmentRecent() != null &&
                        mSectionsPagerAdapter.getFragmentContacts() != null) {
                    if (uri.toString().contains("call_log/calls")) {
                        mSectionsPagerAdapter.getFragmentRecent().callTableUpdated();
                    } else {
                        /**
                         * When bluetooth connection state changes, we get update for
                         * "com.android.contacts" uri either way, but for the "call_log"
                         * when Call History is off & bluetooth state changes we still need to update
                         * RecentCallsFragment bluetooth sync icon state
                         */
                        mSectionsPagerAdapter.getFragmentRecent().updateSyncIconState();
                    }
                    if (uri.toString().contains("com.android.contacts")) {
                        mSectionsPagerAdapter.getFragmentContacts().contactTableUpdated();
                    }

                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    @Override
    public void triggerOffHookButton(View v) {
        onClick(v);
    }

    @Override
    public void triggerTransducerButton(View v) {
        onClick(v);
    }
}
