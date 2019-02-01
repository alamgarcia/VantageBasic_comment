package com.avaya.android.vantage.basic.fragments;

import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.CallDialerActivity;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.adaptors.UICallViewAdaptor;
import com.avaya.android.vantage.basic.csdk.CallAdaptor;
import com.avaya.android.vantage.basic.csdk.LocalContactInfo;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.settings.ConfigChangeApplier;
import com.avaya.android.vantage.basic.model.UICall;
import com.avaya.android.vantage.basic.model.UICallState;
import com.avaya.android.vantage.basic.views.adapters.CallStateEventHandler;
import com.daasuu.bl.BubbleLayout;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.avaya.android.vantage.basic.activities.MainActivity.ACTIVE_VIDEO_CALL_FRAGMENT;
import static com.avaya.android.vantage.basic.activities.MainActivity.CONTACTS_EDIT_FRAGMENT;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_IPOFFICE;
import static com.avaya.android.vantage.basic.model.UICallState.ESTABLISHED;
import static com.avaya.android.vantage.basic.model.UICallState.FAILED;
import static com.avaya.android.vantage.basic.model.UICallState.HELD;
import static com.avaya.android.vantage.basic.model.UICallState.IDLE;
import static com.avaya.android.vantage.basic.model.UICallState.REMOTE_ALERTING;

/**
 * {@link ActiveCallFragment} is responsible for showing current call information and provide user
 * with ability to interact with curent {@link com.avaya.clientservices.call.Call}
 */
public class ActiveCallFragment extends Fragment implements ConfigChangeApplier {

    private static final String TAG = "ActiveCallFragment";
    private static final String REDIAL_NUMBER = "redialNumber";
    private static final String SHOW_CALL_FEATURES_HINT = "ShowCallFeaturesHint";
    public static final String NUM_CALLS_KEY = "NUM_CALLS_KEY";
    public static final String ACTIVE_CALL_KEY = "ACTIVE_CALL_KEY";
    public static boolean IS_ACTIVE = false;

    OnActiveCallInteractionListener mCallback;

    private boolean mIsVideo = false;
    private boolean mIsEmergency = false;
    protected int mCallId = 0;
    private String mDisplayName = null;
    public ToggleButton mHoldCallButton = null;
    private ToggleButton mDialpadCallButton = null;
    private ToggleButton mEndCallButton = null;
    protected TextView mContactName = null, mCallStateView = null, mDigitsView = null;
    private TextView mContactNameFeature;
    private TextView mCallStateFeature;
    private ImageView mContaceImage = null;
    private ImageView mCloseDialpadButton = null;
    private ImageView mBackArrow = null;
    private ImageView mMoreButton = null;
    private ToggleButton mMoreButtonLand = null;
    private TableLayout mButtonsGrid = null;
    private FrameLayout mDialpadView = null;
    public FrameLayout mMoreCallFeatures;
    private FrameLayout mDialogLayout;
    private FrameLayout mMergeDialogLayout;
    private HorizontalScrollView mTextScroll;
    private TextView mDismissCallFeatureDialog = null, mDontShowAgain = null;
    private BubbleLayout mCallFeatureDialog = null;
    private String mPhotoURI;
    private View mRoot;
    private boolean mIsOpen = false;

    CallStatusFragment mCallStatusFragment = null;

    public ToggleButton transducerButton;
    public ToggleButton offHook;
    private OffHookTransduceButtonInterface mCallbackoffHookTransduceButtonInterface;

    private Handler mHandler;
    private Runnable mUpdateDurationTask;
    private volatile long mCurrTimerMillis = 0, mOpenMenuTime = 0;
    private RoundedBitmapDrawable mContactImageCache = null;
    protected int mSavedCallsNumber = -1;

    public UICallState getCallState() {
        return mCallState;
    }

    private UICallState mCallState = IDLE;
    protected UICallViewAdaptor mCallViewAdaptor = null;
    private boolean mVideoCall;

    private static final long mFontNormal = 50;
    private static final long mFontSmall = 40;
    private static final long mFontSmaller = 30;

    public RelativeLayout activeCallRoot;

    private String mCallNumber;
    private String[] mDigits;
    private String[] mLetters;

    private long mLastClickTime = 0;

    //Synchronization lock object
    private final Object lock = new Object();
    private String isFromLandSearch = "0";

    private AlertDialog.Builder builder;
    public AlertDialog alertDialog;

    /**
     * Required empty constructor
     */
    public ActiveCallFragment() {
        // Required empty public constructor
    }

    /**
     * Initialisation of {@link ActiveCallFragment} parameters
     *
     * @param call            {@link UICall} for which {@link ActiveCallFragment} is created
     * @param callViewAdaptor {@link UICallViewAdaptor} used in {@link ActiveCallFragment}
     */
    public void init(UICall call, UICallViewAdaptor callViewAdaptor) {
        mHandler = new Handler(Looper.getMainLooper());
        mUpdateDurationTask = new UpdateDurationRunnable(this);
        mCallId = call.getCallId();
        if (!call.getRemoteDisplayName().equalsIgnoreCase("")) {
            mDisplayName = call.getRemoteDisplayName();
        }
        mCallViewAdaptor = callViewAdaptor;
        mCallState = call.getState();
        mIsVideo = call.isVideo();
        mIsEmergency = call.isEmergency() || SDKManager.getInstance().getDeskPhoneServiceAdaptor().isAnonymous();
        mCallNumber = call.getRemoteNumber();
    }

    public void initForReplaced(UICall call, UICallViewAdaptor callViewAdaptor) {
        init(call, callViewAdaptor);
        updateContactNameAndPhoto(call.getRemoteNumber(), call.getRemoteDisplayName());
    }

    /**
     * Called when all saved state has been restored into the view hierarchy
     * of the fragment.  This can be used to do initialization based on saved
     * state that you are letting the view hierarchy track itself, such as
     * whether check box widgets are currently checked.  This is called
     * after {@link #onActivityCreated(Bundle)} and before
     * {@link #onStart()}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        restoreState(savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Calculate particular call duration
        if (SDKManager.getInstance().getCallAdaptor().getCall(mCallId) != null) {
            mCurrTimerMillis = new Date().getTime() - SDKManager.getInstance().getCallAdaptor().getCall(mCallId).getStateStartTime();
        }

        if((MainActivity)getActivity()!=null && !getActivity().isDestroyed()) {
            if(((MainActivity) getActivity()).mActiveCall!=null)
                ((MainActivity) getActivity()).mActiveCall.setClickable(true);

            if( SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 1 ) {
                ((MainActivity) getActivity()).mPickContacts.setVisibility(View.GONE);
            }

            if( SDKManager.getInstance().getCallAdaptor().getNumOfCalls() == 1 ){
                mCallStatusFragment = (CallStatusFragment) getFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);

                if(mCallStatusFragment!=null) {
                    mCallStatusFragment.hideCallStatus();
                    Utils.hideKeyboard(getActivity());
                }
            }

            ((MainActivity) getActivity()).cancelAddSomeOneScreen();
            ((MainActivity) getActivity()).backToFullScreen();
        }
    }

    protected void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(NUM_CALLS_KEY)) {
                mSavedCallsNumber = savedInstanceState.getInt(NUM_CALLS_KEY);
            } else {
                mSavedCallsNumber = -1;
            }
            if (savedInstanceState.containsKey(ACTIVE_CALL_KEY)) {
                mCallId = savedInstanceState.getInt(ACTIVE_CALL_KEY);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPhotoURI != null) {
            setContactPhoto(Uri.parse(mPhotoURI));
        } else {
            if (TextUtils.isEmpty(mCallNumber)) {
                mContaceImage.setImageResource(R.drawable.ic_avatar_generic);
                return;
            }
            String contactPhotoURI = LocalContactInfo.getContactInfoByNumber(mCallNumber, getContext())[6];
            if (contactPhotoURI != null && contactPhotoURI.length() > 0) {
                setContactPhoto(Uri.parse(contactPhotoURI));
            }

        }
        if (mDialpadView.getVisibility() == View.VISIBLE) {
            mDialpadCallButton.setChecked(true);
        } else {
            mDialpadCallButton.setChecked(false);
        }
        // the state of the call brobabaly was changed
        UICall call = SDKManager.getInstance().getCallAdaptor().getCall(mCallId);
        if (call != null) {
            setCallStateChanged(call.getState());
        }

        setEmergencyAndLockFeature();

        if (getActivity() != null)
            ((MainActivity)getActivity()).setOffHookButtonsBasedCallState(mCallId, mCallState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        IS_ACTIVE = true;

        if (mDisplayName == null && !SDKManager.getInstance().getCallAdaptor().isIncomingCall(mCallId)) {
            SharedPreferences mRedialSharedPrefs = this.getActivity().getSharedPreferences(REDIAL_NUMBER, Context.MODE_PRIVATE);
            String redailNumber = mRedialSharedPrefs.getString(REDIAL_NUMBER, "");
            mPhotoURI = Utils.getPhotoURI(redailNumber);
            mDisplayName = Utils.getFirstContact(redailNumber);
        }

        View view = inflater.inflate(R.layout.active_call, container, false);

        mRoot = view.findViewById(R.id.viewRoot);

        mEndCallButton = (ToggleButton) view.findViewById(R.id.control_endcall);
        mHoldCallButton = (ToggleButton) view.findViewById(R.id.control_hold);
        mDialpadCallButton = (ToggleButton) view.findViewById(R.id.control_dialpad);
        mContactName = (TextView) view.findViewById(R.id.contact_name);
        mCallStateView = (TextView) view.findViewById(R.id.call_state);
        mDigitsView = (TextView) view.findViewById(R.id.active_call_dialer_text);
        mContactNameFeature = (TextView) view.findViewById(R.id.contact_name_feature);
        mCallStateFeature = (TextView) view.findViewById(R.id.call_state_feature);
        mCloseDialpadButton = (ImageView) view.findViewById(R.id.call_features_close);
        mBackArrow = (ImageView) view.findViewById(R.id.back);

        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            mMoreButtonLand = (ToggleButton) view.findViewById(R.id.more);
        }else{
            mMoreButton = (ImageView) view.findViewById(R.id.more);
        }
        mContaceImage = (ImageView) view.findViewById(R.id.contact_image);
        mTextScroll = (HorizontalScrollView) view.findViewById(R.id.scroll_call_dtmf);
        mDialpadView = (FrameLayout) view.findViewById(R.id.dtmf);
        mMoreCallFeatures = (FrameLayout) view.findViewById(R.id.more_call_features);
        mDialogLayout = (FrameLayout) view.findViewById(R.id.transfer_dialog);
        mMergeDialogLayout = (FrameLayout) view.findViewById(R.id.merge_dialog);

        mDismissCallFeatureDialog = (TextView) view.findViewById(R.id.dismiss);
        mDontShowAgain = (TextView) view.findViewById(R.id.dont_show_again);
        mCallFeatureDialog = (BubbleLayout) view.findViewById(R.id.call_features_hint);
        SharedPreferences mRedialSharedPrefs = this.getActivity().getSharedPreferences(SHOW_CALL_FEATURES_HINT, Context.MODE_PRIVATE);
        boolean shouldShowHint = mRedialSharedPrefs.getBoolean(SHOW_CALL_FEATURES_HINT, true);

        if (!shouldShowHint) {
            mCallFeatureDialog.setVisibility(View.GONE);
        }

        if (mDialogLayout.getVisibility() == View.GONE) {
            mOpenMenuTime = 0;
            mIsOpen = false;
        }

        if (mMergeDialogLayout.getVisibility() == View.GONE) {
            mOpenMenuTime = 0;
            mIsOpen = false;
        }

        mDialpadView.setVisibility(View.GONE);
        mButtonsGrid = (TableLayout) mDialpadView.findViewById(R.id.buttons_grid);

        mDigits = getResources().getStringArray(R.array.dialer_numbers);
        mLetters = getResources().getStringArray(R.array.dialer_letters);

        if (mContactImageCache != null) {
            mContaceImage.setBackground(mContactImageCache);
            mContactImageCache = null;
        }

        Log.e(TAG, "onCreateView: " + mDisplayName);
        if (mDisplayName != null) {
            mContactName.setText(mDisplayName);
            mContactNameFeature.setText(mDisplayName);
        }

        if (mCallState != IDLE) {
            setCallStateChanged(mCallState);
        }

        configureHoldButton();
        setOnClickListeners();
        handleActiveCallDialerPad();
        setEmergencyAndLockFeature();



        if(getResources().getBoolean(R.bool.is_landscape) == true) {

            transducerButton = (ToggleButton)  view.findViewById(R.id.transducer_button);
            transducerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallbackoffHookTransduceButtonInterface.triggerTransducerButton(v);
                }
            });

            offHook = (ToggleButton)  view.findViewById(R.id.off_hook);
            offHook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null)
                        ((MainActivity) getActivity()).setOffhookButtosChecked(offHook.isChecked());
                    mCallbackoffHookTransduceButtonInterface.triggerOffHookButton(v);
                }
            });

            if((MainActivity)getActivity()!=null) {
                ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(false);
            }
        }

        activeCallRoot = (RelativeLayout) view.findViewById(R.id.active_call_root);

        hideMenus(true);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCallback.onCallStarted(mIsVideo);

        if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
            int resId = ((MainActivity) getActivity()).getDeviceResIdFromSharedPref();
            offHook.setBackgroundResource(resId);
            offHook.setChecked(true);
        }
    }

    /**
     * Set onClickListeners for views
     */
    private void setOnClickListeners() {



        mRoot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //hide dialog which is open.
                if (mIsOpen && motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    hideMenus(false);
                    return true;
                }
                mCallFeatureDialog.setVisibility(View.GONE);


                if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
                    ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(false);
                    if( ((MainActivity) getActivity()).isFragmentVisible(ACTIVE_VIDEO_CALL_FRAGMENT) ){
                        ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(true);
                    }

                    //after phone number is selected, we just hide invisible frame and list of phone numbers
                    ((MainActivity) getActivity()).mSelectPhoneNumber.setVisibility(View.INVISIBLE);
                    ((MainActivity) getActivity()).mFrameAll.setVisibility(View.GONE);
                }

                return false;
            }
        });

        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            mMoreButtonLand.setOnClickListener(new OnMoreButtonClickListener());
        }else{
            mMoreButton.setOnClickListener(new OnMoreButtonClickListener());
        }

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBackArrowOnClickListener();
            }
        });
        mCloseDialpadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mContaceImage != null) {
                    mContaceImage.setVisibility(View.VISIBLE);
                }
                if (mDialpadView != null) {
                    mDialpadView.setVisibility(View.INVISIBLE);
                    mIsOpen = false;
                }
                if (mDialpadCallButton != null) {
                    mDialpadCallButton.setChecked(false);
                }
            }
        });

        mDismissCallFeatureDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallFeatureDialog.setVisibility(View.GONE);
            }
        });

        mDontShowAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCallFeatureHint(false);
                mCallFeatureDialog.setVisibility(View.GONE);
            }
        });

        mDialpadCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDialpadCallButton.isChecked()) {
                    mContaceImage.setVisibility(View.INVISIBLE);
                    mDialpadView.setVisibility(View.VISIBLE);
                    mOpenMenuTime = 0;
                    mMoreCallFeatures.setVisibility(View.GONE);
                    mCallback.setFeatureMenuOpen(false);
                    hideContactNameAndStatus(false);
                    mIsOpen = true;
                } else {
                    mContaceImage.setVisibility(View.VISIBLE);
                    mDialpadView.setVisibility(View.INVISIBLE);
                    mOpenMenuTime = 0;
                    mIsOpen = false;
                }
            }
        });
        mHoldCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mHoldCallButton.isChecked()) {
                    mHoldCallButton.setChecked(false);
                    mCallViewAdaptor.holdCall(mCallId);
                } else {
                    mHoldCallButton.setChecked(true);
                    mCallViewAdaptor.unholdCall(mCallId);
                }
            }
        });
        mEndCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                mCallViewAdaptor.endCall(mCallId);
                if (mCallback != null) {
                    mCallback.cancelContactPicker();
                }
            }
        });
    }

    public void mBackArrowOnClickListener(){
        if (!mIsOpen) {
            pauseAndShowCallStatus();

            cancelFullScreenMode();
        }
        if(((MainActivity) getActivity()!=null && ((MainActivity) getActivity()).mActiveCall!=null))
            ((MainActivity) getActivity()).mActiveCall.setClickable(false);


        if(getResources().getBoolean(R.bool.is_landscape) == true){
            FragmentManager fragmentManager = ((MainActivity) getActivity()).getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            if(fragments != null){
                for(Fragment fragment : fragments){
                    if(fragment != null && fragment.isVisible())
                        if (fragment instanceof ContactDetailsFragment || fragment instanceof ContactEditFragment || fragment instanceof VideoCallFragment) {
                            ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(true);
                            break;
                        }else {
                            ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(false);
                        }
                }
            }

        }

        ((MainActivity) getActivity()).tabLayoutReset();
    }

    public void setHoldButtonChecked(boolean checked){
        mHoldCallButton.setChecked(checked);
        if(getActivity()!=null) {
            ToggleButton toggle = (ToggleButton) getActivity().findViewById(R.id.off_hook);
            if (checked) {
                CallStatusFragment.setCallStatusVisiblity(true);
                if (getActivity() != null) {
                    ((MainActivity) getActivity()).setOffhookButtosChecked(false);
                }
            } else {
                if (getActivity() != null) {
                    ((MainActivity) getActivity()).setOffhookButtosChecked(true);
                }
                CallStatusFragment.setCallStatusVisiblity(false);
            }
        }
    }

    public void setOffhookButtonChecked(boolean checked){
        ToggleButton toggle = (ToggleButton) getActivity().findViewById(R.id.off_hook);
        toggle.setChecked(checked);
    }

    private void setCallFeatureHint(boolean value) {

        SharedPreferences pref = this.getActivity().getSharedPreferences(SHOW_CALL_FEATURES_HINT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(SHOW_CALL_FEATURES_HINT, value);
        editor.apply();
    }

    /**
     * Hide menus if user click out of screen.
     */
    private void hideMenus(boolean isNewCall) {
        //set up time on 0 and boolean mIsOpen on false;
        mOpenMenuTime = 0;
        mIsOpen = false;
        // hide all menus which are open.
        if (mMoreCallFeatures.getVisibility() == View.VISIBLE || isNewCall) {
            mMoreCallFeatures.setVisibility(View.GONE);
            mCallback.setFeatureMenuOpen(false);
            hideContactNameAndStatus(false);
        }
        if (mDialpadView.getVisibility() == View.VISIBLE) {
            mDialpadCallButton.setChecked(false);
            mDialpadView.setVisibility(View.GONE);
        }
        if (mDialogLayout.getVisibility() == View.VISIBLE) {
            mDialogLayout.setVisibility(View.GONE);
        }
        if (mMergeDialogLayout.getVisibility() == View.VISIBLE) {
            mMergeDialogLayout.setVisibility(View.GONE);
        }
        if (!mIsVideo)
            mContaceImage.setVisibility(View.VISIBLE);
    }

    /**
     * Set up emergency feature for device
     */
    public void setEmergencyAndLockFeature() {
        boolean mIsLockState;
        KeyguardManager kgMgr =
                (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
        mIsLockState = (kgMgr != null) && kgMgr.isDeviceLocked() && !ElanApplication.isPinAppLock;

        Log.d(TAG, "mEmergency = " + mIsEmergency + " mIsLockState = " + mIsLockState);

        // if this is not emergency call and we are not in Lock state, nothing to be set
        ToggleButton videoMute = (ToggleButton) getActivity().findViewById(R.id.video_mute);
        if (videoMute != null) {
            if (!mIsEmergency && !mIsLockState) {
                setBackArrowColor();
                if (getResources().getBoolean(R.bool.is_landscape) == true) {
                    mMoreButtonLand.setVisibility(View.VISIBLE);
                }else {
                    mMoreButton.setVisibility(View.VISIBLE);
                }
                videoMute.setEnabled(true);
            } else {
                // disable some middle call and video features
                mBackArrow.setVisibility(View.INVISIBLE);
                if (getResources().getBoolean(R.bool.is_landscape) == true) {
                    mMoreButtonLand.setVisibility(View.INVISIBLE);
                }else {
                    mMoreButton.setVisibility(View.INVISIBLE);
                }
                mCallFeatureDialog.setVisibility(View.GONE);
                if (mIsEmergency) {
                    videoMute.setEnabled(false);
                }
            }
        }
    }


    /**
     * Called to ask the fragment to save its current dynamic state, so it
     * can later be reconstructed in a new instance of its process is
     * restarted.  If a new instance of the fragment later needs to be
     * created, the data you place in the Bundle here will be available
     * in the Bundle given to {@link #onCreate(Bundle)},
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}, and
     * {@link #onActivityCreated(Bundle)}.
     * <p>
     * <p>This corresponds to {@link Activity#onSaveInstanceState(Bundle)
     * Activity.onSaveInstanceState(Bundle)} and most of the discussion there
     * applies here as well.  Note however: <em>this method may be called
     * at any time before {@link #onDestroy()}</em>.  There are many situations
     * where a fragment may be mostly torn down (such as when placed on the
     * back stack with no UI showing), but its state will not be saved until
     * its owning activity actually needs to save its state.
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mCallViewAdaptor != null)
            outState.putInt(NUM_CALLS_KEY, mCallViewAdaptor.getNumOfCalls());
        outState.putInt(ACTIVE_CALL_KEY, mCallId);

    }

    public void setBackArrowColor() {
        if (mBackArrow == null) {
            Log.e(TAG, "Black arrow is null");
            return;
        }

        int numOfCalls = (mCallViewAdaptor == null) ? mSavedCallsNumber : mCallViewAdaptor.getNumOfCalls();
        if(numOfCalls == CallAdaptor.MAX_NUM_CALLS || isLockState()) {
            mBackArrow.setVisibility(View.INVISIBLE);
        } else {
            mBackArrow.setVisibility(View.VISIBLE);
        }
    }

    private boolean isLockState() {
        if (getContext() == null)
            return false;

        KeyguardManager kgMgr = (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
        return (kgMgr != null) && kgMgr.isDeviceLocked() && !ElanApplication.isPinAppLock;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnActiveCallInteractionListener) context;
            mCallbackoffHookTransduceButtonInterface = (OffHookTransduceButtonInterface) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "OnActiveCallInteractionListener cast failed");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;

    }

    /**
     * Remove call feature menu
     */
    public void dismissMenu() {
        mMoreCallFeatures.findViewById(R.id.call_features_close).callOnClick();
        mCallback.setFeatureMenuOpen(false);
        hideContactNameAndStatus(false);
    }

    @Override
    public void applyConfigChange() {
        configureHoldButton();
    }

    /**
     * Call ending procedure and in case of failure showing Snackbar
     */

    //TODO Check is it proper behaviour
    public void onCallFailed() {
        if (getContext() != null) {
            Snackbar.make(mRoot, R.string.operation_failed, Snackbar.LENGTH_LONG).show();
        }

        if (mCallback != null) {
            mCallback.cancelContactPicker();
        }
    }

    public void onCallRemoved(){
        if (mCallback != null) {
            mCallback.cancelContactPicker();
        }
        if (getActivity() != null) {
            ((ElanApplication) (getActivity().getApplication())).unRegisterMediaButtonReceiver();
        }
    }

    /**
     * Processing transfer failed logic
     */
    public void onTransferFailed() {

        setCallStateChanged(ESTABLISHED);
        if (getView() != null) {
            getView().setVisibility(View.VISIBLE);
        }

        if (getContext() != null) {
            Snackbar.make(mRoot, R.string.operation_failed, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Method changes name and photo according to new call transfer
     *
     * @param newDisplayNumber String new number to be shown
     */
    public void onCallRemoteAddressChanged(String newDisplayNumber, String newDisplayName, UICall call) {
        if(mCallNumber!=null && !mCallNumber.isEmpty() && call != null)
            mCallNumber = call.getRemoteNumber();
        updateContactNameAndPhoto(newDisplayNumber, newDisplayName);
    }

    /**
     * Update photo after call conferece status is changed.
     */
    public void onCallConferenceStatusChanged() {
        updateContactPhoto();
    }

    /**
     * Updating contact name and photo based on provided parameters
     *
     * @param newDisplayNumber String
     * @param newDisplayName   String
     */
    private void updateContactNameAndPhoto(String newDisplayNumber, String newDisplayName) {
        mPhotoURI = Utils.getPhotoURI(newDisplayNumber);

        if (ElanApplication.getContext() == null) return;

        // preventing but where contact that has same name as number, would not be displayed correctly
        String contactName = LocalContactInfo.getContactInfoByNumber(mCallNumber, ElanApplication.getContext())[0];
        if (mCallViewAdaptor.isCMConferenceCall(mCallId) || contactName.trim().length() == 0) {
            mDisplayName = Utils.getContactName(newDisplayNumber, newDisplayName, mCallViewAdaptor.isCMConferenceCall(mCallId));
        } else {
            mDisplayName = contactName;
        }

        if (mContactName != null) {
            mContactName.setText(mDisplayName);
        }
        if (mContactNameFeature != null) {
            mContactNameFeature.setText(mDisplayName);
        }

        Log.d(TAG, "updateContactNameAndPhoto mDisplayName=" + mDisplayName);
        updateContactPhoto();
    }

    /**
     * Updating contant photo.
     */
    public void updateContactPhoto() {
        if (mPhotoURI == null && ElanApplication.getContext() != null) {
            Resources resources = ElanApplication.getContext().getResources();
            mPhotoURI = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(resources.getResourcePackageName(R.drawable.ic_avatar_generic))
                    .appendPath(resources.getResourceTypeName(R.drawable.ic_avatar_generic))
                    .appendPath(resources.getResourceEntryName(R.drawable.ic_avatar_generic))
                    .build().toString();
        }

        setContactPhoto(Uri.parse(mPhotoURI));
    }

    /**
     * Hide ActiveCall Fragment and display CallStatus on the MainActivity
     */
    private void pauseAndShowCallStatus() {
        if (mCallViewAdaptor.getNumOfCalls() < CallAdaptor.MAX_NUM_CALLS || SDKManager.getInstance().getCallAdaptor().getOffhookCallId() != 0) {

            mCallStatusFragment = (CallStatusFragment) getFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
            if (mCallStatusFragment != null) {
                mCallStatusFragment.updateCallStatusInfo(mCallStateView, mContactName, mCallId);
            }

            if (getView() != null) {
                getView().setVisibility(View.INVISIBLE);
            }

            mCallStatusFragment = (CallStatusFragment) getFragmentManager().findFragmentByTag(CallStateEventHandler.CALL_STATUS_TAG);
            if (mCallStatusFragment != null) {
                mCallStatusFragment.showCallStatus();
            }
            if((MainActivity)getActivity()!=null) {
                if(((MainActivity) getActivity()).mActiveCall!=null)
                    ((MainActivity) getActivity()).mActiveCall.setClickable(false);
            }
        }
    }

    /**
     * Handling call dialer pad for active call fragment
     */
    private void handleActiveCallDialerPad() {

        int buttonIds[] = {
                R.id.b1,
                R.id.b2,
                R.id.b3,
                R.id.b4,
                R.id.b5,
                R.id.b6,
                R.id.b7,
                R.id.b8,
                R.id.b9,
                R.id.ba,
                R.id.bz,
                R.id.bp
        };

        mButtonsGrid.setClickable(false);
        for (int i = 0; i < buttonIds.length; i++) {
            configureButton(mButtonsGrid.findViewById(buttonIds[i]), mDigits[i], mLetters[i]);
        }

        mDigitsView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // resizing text in TextView depending of a character mNumber
                int mTextLength = mDigitsView.getText().length();

                if (mTextLength >= 10 && mTextLength < 13) {
                    mDigitsView.setTextSize(mFontSmall);
                } else if (mTextLength >= 13) {
                    mDigitsView.setTextSize(mFontSmaller);
                } else if (mTextLength < 10) {
                    mDigitsView.setTextSize(mFontNormal);
                }
            }
        });
    }

    /**
     * Preparing and configuring buttons onClickListeners
     *
     * @param button       {@link View} for which onClickListener have to be set
     * @param digitString  text to be set on button view
     * @param letterString text to be set under number in button
     */
    private void configureButton(View button, final String digitString, String letterString) {
        TextView digit = (TextView) button.findViewById(R.id.digit);
        digit.setText(digitString);
        TextView letters = (TextView) button.findViewById(R.id.letters);
        letters.setText(letterString);
        digit.setEnabled(false);
        letters.setEnabled(false);
        button.setContentDescription(digitString);
        if (!Character.isDigit(digitString.charAt(0))) {
            letters.setVisibility(View.GONE);
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOpenMenuTime = 0;
                mDigitsView.append(digitString);
                if(!mHoldCallButton.isChecked())
                    mCallViewAdaptor.sendDTMF(mCallId, digitString.toCharArray()[0]);
                scrollRight();

            }
        });
    }

    /**
     * Obtain ContactName String
     *
     * @return String for contact name
     */
    public String getContactName() {
        return mDisplayName;
    }

    /**
     * Returning call id of active call
     *
     * @return int {@link #mCallId}
     */
    public int getCallId() {
        return mCallId;
    }

    //TODO move to separate class
    private class OnMoreButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (mMoreCallFeatures.getVisibility() == View.VISIBLE) {
                mMoreCallFeatures.setVisibility(View.GONE);
                mCallback.setFeatureMenuOpen(false);
                hideContactNameAndStatus(false);
                mOpenMenuTime = 0;
                mIsOpen = false;
            } else {
                mMoreCallFeatures.setVisibility(View.VISIBLE);
                mCallback.setFeatureMenuOpen(true);
                hideContactNameAndStatus(true);
                mOpenMenuTime = 0;
                mDialpadView.setVisibility(View.GONE);
                mDialpadCallButton.setChecked(false);
                mIsOpen = true;
            }
            CallFeaturesVisibilityHandler callFeaturesVisibilityHandler = new CallFeaturesVisibilityHandler().invoke();
            TextView newCall = callFeaturesVisibilityHandler.getNewCall();
            TextView merge = callFeaturesVisibilityHandler.getMerge();
            TextView drop = callFeaturesVisibilityHandler.getDrop();
            TextView transfer = callFeaturesVisibilityHandler.getTransfer();
            TextView startVideo = callFeaturesVisibilityHandler.getStartVideo();
            TextView stopVideo = callFeaturesVisibilityHandler.getStopVideo();
            TextView conference = callFeaturesVisibilityHandler.getConference();


            newCall.setOnClickListener(new NewCallClickListener());

            mMoreCallFeatures.findViewById(R.id.call_features_close).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mMoreCallFeaturesClick();
                }
            });

            mMoreCallFeatures.findViewById(R.id.feature_title).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //TODO video call test
                    mOpenMenuTime = 0;
                    mVideoCall = !mVideoCall;
                    if (mVideoCall) {
                        if (mDialpadCallButton.isChecked()) {
                            mDialpadCallButton.performClick();
                        }
                        mMoreCallFeatures.findViewById(R.id.feature_linear_layout).setBackgroundResource(R.drawable.call_video_features_white_stroke);
                    } else {
                        mMoreCallFeatures.findViewById(R.id.feature_linear_layout).setBackgroundResource(R.drawable.call_features_white_stroke);
                    }
                }
            });

            merge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mergeClickListener();
                }
            });


            drop.setOnClickListener(new DropClickListener());

            transfer.setOnClickListener(new TransferClickListener());

            startVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideMenus(false);
                    mOpenMenuTime = 0;
                    mCallViewAdaptor.escalateToVideoCall(mCallId);
                }
            });

            stopVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //UICall uiCall = new UICall(mCallId, mCallState, mDisplayName,null, false);
                    hideMenus(false);
                    mOpenMenuTime = 0;
                    mCallViewAdaptor.deescalateToAudioCall(mCallId);
                }
            });

            conference.setOnClickListener(new ConferenceClickListener());
        }



        private class NewCallClickListener implements View.OnClickListener {
            @Override
            public void onClick(View v) {
                hideMenus(false);
                if (mCallViewAdaptor.getNumOfCalls() < CallAdaptor.MAX_NUM_CALLS) {
                    if (mCallState == UICallState.ESTABLISHED) {
                        mHoldCallButton.setChecked(true);
                        mHoldCallButton.callOnClick();
                        mCallState = UICallState.HELD;
                    }
                    mBackArrow.callOnClick();
                }
            }
        }

        /**
         * Action on Merge click
         */
        private void mergeClickListener() {
            hideMenus(false);
            mMergeDialogLayout.setVisibility(View.VISIBLE);
            mOpenMenuTime = 0;
            mIsOpen = true;
            final int heldCallId = SDKManager.getInstance().getCallAdaptor().getHeldCallId();
            if (heldCallId == -1) {
                return;
            }

            UICall call = SDKManager.getInstance().getCallAdaptor().getCall(heldCallId);
            String callOnHold = "";

            if (call != null && call.getRemoteNumber() != null) {
                String localContactNameSearch = LocalContactInfo.getContactInfoByNumber(call.getRemoteNumber(), getContext())[0];

                if (TextUtils.isEmpty(localContactNameSearch)) {
                    callOnHold = call.getRemoteDisplayName();
                } else {
                    callOnHold = localContactNameSearch;
                }

            }
            TextView callToMerge = (TextView) mMergeDialogLayout.findViewById(R.id.merge_call_with);

            callToMerge.setText(callOnHold);
            mMergeDialogLayout.findViewById(R.id.cancel_merge).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mMergeDialogLayout.setVisibility(View.GONE);
                    mIsOpen = false;
                    mOpenMenuTime = 0;
                }
            });
            mMergeDialogLayout.findViewById(R.id.merge_call).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SDKManager.getInstance().getCallAdaptor().addParticipant(heldCallId, mCallId);
                    mMergeDialogLayout.setVisibility(View.GONE);
                    mOpenMenuTime = 0;
                    mIsOpen = false;
                }
            });
        }

        private class DropClickListener implements View.OnClickListener {
            @Override
            public void onClick(View view) {
                mOpenMenuTime = 0;
                hideMenus(false);
                SDKManager.getInstance().getCallAdaptor().dropLastParticipant(mCallId);
            }
        }

        /**
         * Preparing feature target list dialog from provided resource id
         *
         * @param title int representing resource id to be shown in dialog
         */
        private void showFeatureTargetListDialog(int title) {
            //create rows map
            mDialogLayout.setVisibility(View.VISIBLE);
            mOpenMenuTime = 0;
            mIsOpen = true;
            TextView dialogTitle = (TextView) mDialogLayout.findViewById(R.id.transfer_call_title);
            mDialogLayout.setBackground(new ColorDrawable(Color.TRANSPARENT));
            dialogTitle.setText(title);
        }

        private class TransferClickListener implements View.OnClickListener {
            @Override
            public void onClick(View view) {
                hideMenus(false);

                if (SDKManager.getInstance().getCallAdaptor().getNumOfCalls() > 1) {

                    final int otherCallId = SDKManager.getInstance().getCallAdaptor().getOtherCallId(mCallId);
                    if (otherCallId == -1) {
                        return;
                    }
                    UICall adaptor = SDKManager.getInstance().getCallAdaptor().getCall(otherCallId);
                    String callOnHold = "";
                    if (adaptor != null) {
                        callOnHold = adaptor.getRemoteDisplayName();
                    }

                    builder = new AlertDialog.Builder(getContext(), R.style.AvayaSimpleAlertDialog);
                    
                    builder.setTitle(getString(R.string.transfer_direct_dialog_title))
                            .setMessage(String.format(getString(R.string.transfer_dialog_body), mContactName.getText(), callOnHold))
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SDKManager.getInstance().getCallAdaptor().transferCall(mCallId, otherCallId);
                                }
                            });


                    alertDialog = builder.create();
                    alertDialog.show();


                } else {
                    showFeatureTargetListDialog(R.string.transfer_dialog_title);
                    transferListClickListener();
                }
            }

            /**
             * Action on Transfer click
             */
            private void transferListClickListener() {
                mDialogLayout.findViewById(R.id.contact_transfer).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.i(TAG, "Pick Contact for transfer");
                        hideMenus(false);
                        pauseAndShowCallStatus();
                        if (mCallback != null) {
                            mCallback.startContactPickerForCallTransfer(mCallId);
                        }
                        if(getResources().getBoolean(R.bool.is_landscape) == true && ((MainActivity) getActivity())!=null){
                            ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(false);
                        }

                    }
                });
                mDialogLayout.findViewById(R.id.dialer_transfer).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), CallDialerActivity.class);
                        hideMenus(false);
                        intent.putExtra(Constants.IS_CONFERENCE, false);
                        intent.putExtra(Constants.CALL_ID, mCallId);
                        getActivity().startActivityForResult(intent, Constants.TRANSFER_REQUEST_CODE);

                    }
                });
            }
        }

        /**
         * Action on Conference click
         */
        private class ConferenceClickListener implements View.OnClickListener {
            @Override
            public void onClick(View view) {
                hideMenus(false);
                showFeatureTargetListDialog(R.string.feature_dialog_add_someone);
                conferenceListClickListener();
            }

            /**
             * Processing click on dialog for creating conference.
             */
            private void conferenceListClickListener() {
                mDialogLayout.findViewById(R.id.contact_transfer).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.i(TAG, getString(R.string.conference_dialog_description));
                        pauseAndShowCallStatus();
                        cancelFullScreenMode();
                        hideMenus(false);
                        if (mCallback != null) {
                            mCallback.startContactPickerForConference(mCallId);
                        }

                        if(getResources().getBoolean(R.bool.is_landscape) == true && ((MainActivity) getActivity())!=null){

                            if(((MainActivity) getActivity()).isFragmentVisible(CONTACTS_EDIT_FRAGMENT)){
                                ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(true);
                            }else{
                                ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(false);
                            }


                        }
                    }
                });
                mDialogLayout.findViewById(R.id.dialer_transfer).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hideMenus(false);
                        Intent intent = new Intent(getActivity(), CallDialerActivity.class);
                        intent.putExtra(Constants.IS_CONFERENCE, true);
                        intent.putExtra(Constants.CALL_ID, mCallId);
                        getActivity().startActivityForResult(intent, Constants.CONFERENCE_REQUEST_CODE);

                    }
                });
            }
        }

    }


    public void mMoreCallFeaturesClick(){
        mMoreCallFeatures.setVisibility(View.GONE);
        mCallback.setFeatureMenuOpen(false);
        hideContactNameAndStatus(false);
        mOpenMenuTime = 0;
        if (!mIsVideo)
            mContaceImage.setVisibility(View.VISIBLE);
        hideMenus(false);
    }

    /**
     * Process performed when {@link UICall} is established
     *
     * @param call {@link UICall} parameter
     */
    public void onCallEstablished(UICall call) {
        Log.d(TAG, "onCallEstablished start");

        if (call==null)
            return;

        if((mCallNumber!=null) && mCallNumber.isEmpty() && (call != null))
            mCallNumber = call.getRemoteNumber();

        updateContactNameAndPhoto(call.getRemoteNumber(), call.getRemoteDisplayName());
        setCallStateChanged(call.getState());

        if (getActivity() != null) {
            ((ElanApplication) (getActivity().getApplication())).registerMediaButtonReceiver();
        }
    }

    /**
     * Setting active call state
     *
     * @param callState UICallState object
     */
    public void setCallStateChanged(UICallState callState) {
        mCallState = callState;
        if (mCallStateView == null || !isAdded()) {
            return;
        }
        if (callState.equals(ESTABLISHED)) {
            updateCallStatusTimer();
            mCallStateView.setTextColor(getResources().getColor(R.color.colorCallStateText, null));
            if (mCallStateFeature != null) {
                mCallStateFeature.setTextColor(getResources().getColor(R.color.colorCallStateText, null));
            }
            setMoreButtonEnabled(true);
            mHoldCallButton.setChecked(false);
            configureHoldButton();
            mCallback.setOffhookButtosChecked(true);
        } else if (callState.equals(HELD)) {
            mCallStateView.setText(getText(R.string.on_hold));
            mCallStateView.setTextColor(getResources().getColor(R.color.colorOnHold, null));
            if (mCallStateFeature != null) {
                mCallStateFeature.setText(getText(R.string.on_hold));
                mCallStateFeature.setTextColor(getResources().getColor(R.color.colorOnHold, null));
            }
            mHoldCallButton.setChecked(true);
            mHoldCallButton.setEnabled(true);
            mCallback.setOffhookButtosChecked(false);
        } else if (callState.equals(REMOTE_ALERTING)) {
            mCallStateView.setText(getText(R.string.calling));
            mCallStateView.setTextColor(getResources().getColor(R.color.colorCallStateText, null));
            if (mCallStateFeature != null) {
                mCallStateFeature.setText(getText(R.string.calling));
                mCallStateFeature.setTextColor(getResources().getColor(R.color.colorCallStateText, null));
            }
            setMoreButtonEnabled(false);
            mHoldCallButton.setEnabled(false);
            mCallback.setOffhookButtosChecked(true);
        } else if (callState.equals(FAILED)) {
            setMoreButtonEnabled(false);
            mHoldCallButton.setEnabled(false);
        }
        new CallFeaturesVisibilityHandler().invoke();
    }

    private void setMoreButtonEnabled(boolean isEnabled) {
        if (getResources().getBoolean(R.bool.is_landscape) == true)
            mMoreButtonLand.setEnabled(isEnabled);
        else
            mMoreButton.setEnabled(isEnabled);
    }

    /**
     * Setting up contact name in case of remote alert
     */
    public void onCallRemoteAlert() {
        setCallStateChanged(REMOTE_ALERTING);
    }

    /**
     * Updating call status timer for active call
     */
    private void updateCallStatusTimer() {
        updateTimer();
    }

    /**
     * Update call length timer
     */
    private void updateTimer() {
        // remove leftovers from Q
        mHandler.removeCallbacks(mUpdateDurationTask);
        // Calculate particular call duration
        if (SDKManager.getInstance().getCallAdaptor().getCall(mCallId) != null) {
            synchronized (lock) {
                mCurrTimerMillis = new Date().getTime() - SDKManager.getInstance().getCallAdaptor().getCall(mCallId).getStateStartTime();
            }
        }

        long minutes;
        long seconds;
        long hours;
        String connectMessage = "";

        // covert time
        hours = TimeUnit.MILLISECONDS.toHours(mCurrTimerMillis); // hours
        minutes = TimeUnit.MILLISECONDS.toMinutes(mCurrTimerMillis) % Constants.MINUTES; // minutes
        seconds = TimeUnit.MILLISECONDS.toSeconds(mCurrTimerMillis) % Constants.SECONDS; // seconds

        // format time
        if (hours != 0) {
            connectMessage = Long.toString(hours) + ":";
        }
        // no leading zero is needed
        if (connectMessage.isEmpty() || minutes > 9) {
            connectMessage += Long.toString(minutes);
        } else {
            connectMessage += "0" + Long.toString(minutes);
        }
        connectMessage += ":";
        if (seconds > 9) {
            connectMessage += Long.toString(seconds);
        } else {
            connectMessage += "0" + Long.toString(seconds);
        }
        if (mCallState.equals(ESTABLISHED)) {
            // set the text and advance the timer
            mCallStateView.setText(connectMessage);
            mCallStateFeature.setText(connectMessage);
            //mInfoContactState.setText(connectMessage);

            if (mCallStatusFragment != null && mCallViewAdaptor.getNumOfCalls() == Constants.CALL_SIZE_1) {
                mCallStatusFragment.updateCallStatusState(mCallStateView, mCallState);
            }
        }else if(mCallState.equals(HELD)) {
            if (mCallStatusFragment != null && mCallViewAdaptor.getNumOfCalls() == Constants.CALL_SIZE_1) {
                mCallStatusFragment.updateCallStatusState(mCallStateView, mCallState);
            }
        }
        synchronized (lock) {
            mCurrTimerMillis += Constants.MILISECONDS; // advance the time in a second.
            mOpenMenuTime += Constants.MILISECONDS;
        }
        // post next update
        mHandler.postDelayed(mUpdateDurationTask, Constants.MILISECONDS);
        if (mOpenMenuTime > Constants.LAYOUT_DISAPPEAR_TIME) {
            hideMenus(false);
        }

        if (mCurrTimerMillis > Constants.CALL_FEATURE_HINT_DISMISS_TIME) {
            mCallFeatureDialog.setVisibility(View.GONE);
        }
    }

    /**
     * Responsible for call timer update
     */
    private static class UpdateDurationRunnable implements Runnable {
        private WeakReference<ActiveCallFragment> mTimerLayout;

        UpdateDurationRunnable(ActiveCallFragment layout) {
            mTimerLayout = new WeakReference<>(layout);
        }

        public void run() {
            if (mTimerLayout != null && mTimerLayout.get() != null) {
                mTimerLayout.get().updateTimer();
            }
        }
    }

    /**
     * Configuring hold button based on CSDK value
     */
    private void configureHoldButton() {

        boolean isHoldEnabled = SDKManager.getInstance().getDeskPhoneServiceAdaptor().isHoldEnabled();
        Log.d(TAG, "isHoldEnabled = " + isHoldEnabled);

        mHoldCallButton.setEnabled(isHoldEnabled);
    }

    /**
     * Scroll mNumber text
     */
    private void scrollRight() {
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mTextScroll != null) {
                    mTextScroll.fullScroll(View.FOCUS_RIGHT);
                }
            }
        }, 100);
    }

    /**
     * Returning handler. In case that handler is null we are creating new one.
     *
     * @return Handler
     */
    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler();
        }
        return mHandler;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        IS_ACTIVE = false;
        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateDurationTask);
        }

        cancelFullScreenMode();
    }

    public void cancelFullScreenMode(){
        try {
            if (getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity) getActivity() != null) {

                FragmentManager fragmentManager = ((MainActivity) getActivity()).getSupportFragmentManager();
                List<Fragment> fragments = fragmentManager.getFragments();
                if (fragments != null) {
                    for (Fragment fragment : fragments) {
                        if (fragment != null)
                            if (fragment instanceof ContactDetailsFragment || fragment instanceof ContactEditFragment) {
                                ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(true);
                            } else if (fragment instanceof ActiveCallFragment) {
                                ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(false);
                            } else if (fragment instanceof DialerFragment) {
                                ((MainActivity) getActivity()).searchButton.setVisibility(View.INVISIBLE);
                                ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(false);
                            }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mCallback.onCallEnded();
    }


    /**
     * Set visibility for active call fragment view
     */
    public void setVisible() {
        if (mCallback != null) {
            mCallback.cancelContactPicker();
        }
        if (getView() != null) {
            getView().setVisibility(View.VISIBLE);
        }

        if((MainActivity)getActivity()!=null) {
            if((((MainActivity) getActivity()).mActiveCall!=null))
                ((MainActivity) getActivity()).mActiveCall.setClickable(true);
        }
    }


    /**
     * Displaying contact photo thumbnail
     *
     * @param mPhotoURI contact Thumbnail URI
     */
    private void setContactPhoto(Uri mPhotoURI) {
        try {
            if (ElanApplication.getContext() != null) {
                if (mCallViewAdaptor.isConferenceCall(mCallId)) {
                    if (mContaceImage == null) {
                        mContactImageCache = RoundedBitmapDrawableFactory.create(ElanApplication.getContext().getResources(),
                                BitmapFactory.decodeResource(ElanApplication.getContext().getResources(), R.drawable.ic_common_avatar_group_124));
                    } else {
                        mContaceImage.setBackgroundResource(R.drawable.ic_common_avatar_group_124);
                    }


                } else {
                    Bitmap uriImage = MediaStore.Images.Media.getBitmap(ElanApplication.getContext().getContentResolver(), mPhotoURI);
                    RoundedBitmapDrawable contactThumbnail =
                            RoundedBitmapDrawableFactory.create(ElanApplication.getContext().getResources(), uriImage);
                    contactThumbnail.setCircular(true);
                    if (mContaceImage == null) {
                        mContactImageCache = contactThumbnail;
                    } else {
                        mContaceImage.setBackground(contactThumbnail);
                    }
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "Error: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Inner class responsible for call feature visibility.
     */
    private class CallFeaturesVisibilityHandler {
        private TextView startVideo;
        private TextView stopVideo;
        private TextView transfer;
        private TextView newCall;
        private TextView conference;
        private TextView merge;
        private TextView drop;

        /**
         * Obtain TextView for video start
         *
         * @return TextView
         */
        TextView getStartVideo() {
            return startVideo;
        }

        /**
         * Obtain TextView for video stop
         *
         * @return TextView
         */
        TextView getStopVideo() {
            return stopVideo;
        }

        /**
         * Obtain TextView for call transfer
         *
         * @return TextView
         */
        TextView getTransfer() {
            return transfer;
        }

        /**
         * Obtain TextView for new call
         *
         * @return TextView
         */
        TextView getNewCall() {
            return newCall;
        }

        /**
         * Obtain TextView for conference call
         *
         * @return TextView
         */
        TextView getConference() {
            return conference;
        }

        /**
         * Obtain TextView for call merge
         *
         * @return TextView
         */
        TextView getMerge() {
            return merge;
        }

        /**
         * Obtain TextView for dropping call
         *
         * @return TextView
         */
        TextView getDrop() {
            return drop;
        }

        /**
         * Enabling and disabling TextViews base on requirement and parameters which are responsible for
         * specified TextView.
         *
         * @return {@link CallFeaturesVisibilityHandler}
         */
        CallFeaturesVisibilityHandler invoke() {
            startVideo = (TextView) mMoreCallFeatures.findViewById(R.id.feature_start_video);
            stopVideo = (TextView) mMoreCallFeatures.findViewById(R.id.feature_stop_video);
            transfer = (TextView) mMoreCallFeatures.findViewById(R.id.feature_transfer);
            newCall = (TextView) mMoreCallFeatures.findViewById(R.id.feature_new_call);
            conference = (TextView) mMoreCallFeatures.findViewById(R.id.feature_conference);
            merge = (TextView) mMoreCallFeatures.findViewById(R.id.feature_merge);
            drop = (TextView) mMoreCallFeatures.findViewById(R.id.feature_remove_last_participant);

            boolean isMaxCall = mCallViewAdaptor.getNumOfCalls() < CallAdaptor.MAX_NUM_CALLS;
            setViewEnabled(newCall, isMaxCall);
            setViewEnabled(merge, !isMaxCall && !mCallState.equals(HELD));
            setViewEnabled(startVideo, !mCallState.equals(HELD));
            setViewEnabled(stopVideo, !mCallState.equals(HELD));
            setViewEnabled(conference, isConferenceEnabled());
            setViewEnabled(transfer, isTransferEnabled() && !SDKManager.getInstance().getCallAdaptor().isConference(mCallId));
            setViewEnabled(drop, SDKManager.getInstance().getCallAdaptor().isDropLastParticipantEnabled(mCallId));

            startVideo.setVisibility((!mIsVideo && isVideoEnabled()) ? View.VISIBLE : View.GONE);
            stopVideo.setVisibility((mIsVideo && isVideoEnabled()) ? View.VISIBLE : View.GONE);
            drop.setVisibility(SDKManager.getInstance().getCallAdaptor().isConference(mCallId) ? View.VISIBLE : View.GONE);

            return this;
        }

        /**
         * Hiding or showing provided {@link View} parameter based on provided parameter boolean
         *
         * @param view      {@link View} on which operation have to be performed
         * @param isEnabled boolean telling us will we show or hide provided view
         */
        private void setViewEnabled(View view, boolean isEnabled) {
            if (isEnabled) {
                view.setAlpha(1);
                view.setEnabled(true);
            } else {
                view.setAlpha(0.4f);
                view.setEnabled(false);
            }
        }

        /**
         * Check if video call is enabled
         *
         * @return call is not part of conference or in "on Hold" state or device is not "K165"
         */
        private boolean isVideoEnabled() {
            //video is not enabled in conference call or when call is "on Hold" state
            return Utils.isCameraSupported() && !mCallViewAdaptor.isCMConferenceCall(mCallId) && !mCallViewAdaptor.isIPOConferenceCall(mCallId);
        }

        /**
         * Check if conference is enabled
         *
         * @return boolean is conference enabled
         */
        private boolean isConferenceEnabled() {
            return (SDKManager.getInstance().getDeskPhoneServiceAdaptor().isConferenceEnabled() && (mCallViewAdaptor.getNumOfCalls() < CallAdaptor.MAX_NUM_CALLS) &&
                     !SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ENABLE_IPOFFICE));
        }

        /**
         * Check if transfer call is enabled
         *
         * @return boolean is transfer enabled
         */
        private boolean isTransferEnabled() {
            return (SDKManager.getInstance().getDeskPhoneServiceAdaptor().isTransferEnabled() && (mCallViewAdaptor.getNumOfCalls() <= CallAdaptor.MAX_NUM_CALLS));
        }
    }

    /**
     * Hiding active call fragment contact name and call status
     *
     * @param isHiden boolean which tell us should we hide contact name and call status
     */
    private void hideContactNameAndStatus(boolean isHiden) {
        if (mCallStateView != null && mContactName != null) {
            if (isHiden) {
                mCallStateView.setVisibility(View.INVISIBLE);
                mContactName.setVisibility(View.INVISIBLE);
            } else {
                mCallStateView.setVisibility(View.VISIBLE);
                mContactName.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Called to report that the call's signaling path has failed.
     */
    public void onCallServiceUnavailable() {
        if (mHoldCallButton != null)
            mHoldCallButton.setEnabled(false);
        if(getResources().getBoolean(R.bool.is_landscape) == true) {
            if (mMoreButtonLand != null)
                mMoreButtonLand.setVisibility(View.INVISIBLE);
        }else {
            if (mMoreButton != null)
                mMoreButton.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Sends DTMF tone to the current call
     * @param digit char value of the digit
     */
    public void sendDTMF(char digit) {
        mCallViewAdaptor.sendDTMF(mCallId, digit);
    }

    /**
     * Update sUI status if the call holding has failed
     * @param callId of the current call
     */
    public void onCallHoldFailed(int callId) {
        if (mCallStatusFragment != null && mCallStatusFragment.getCallId()==callId) {
            // we marked the call as held even before it was really held. Move it back to established
            mCallState = ESTABLISHED;
            mCallStatusFragment.updateCallStatusState(mCallStateView, mCallState);
        }
    }
}
