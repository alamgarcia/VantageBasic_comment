package com.avaya.android.vantage.basic.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.GoogleAnalyticsUtils;
import com.avaya.android.vantage.basic.OnCallDigitCollectionCompletedListener;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.adaptors.UIContactsViewAdaptor;
import com.avaya.android.vantage.basic.csdk.ConfigParametersNames;
import com.avaya.android.vantage.basic.csdk.LocalContactInfo;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.csdk.VoiceMessageAdaptorListener;
import com.avaya.android.vantage.basic.fragments.settings.ConfigChangeApplier;
import com.avaya.android.vantage.basic.model.CallData;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.model.UICall;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnDialerInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DialerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DialerFragment extends android.support.v4.app.Fragment implements ConfigChangeApplier, VoiceMessageAdaptorListener, OnCallDigitCollectionCompletedListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = DialerFragment.class.getSimpleName();
    private static final String NUMBER = "number";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String MODE = "mode";
    private static final String REDIAL_NUMBER = "redialNumber";

    //    private static final String AURA_AUDIO_DIAL_PATTERN = "[\\*\\+#]?[0-9,-]+[#]?[0-9,-]*";
//    private static final String IPO_AUDIO_DIAL_PATTERN = "[\\*\\+]?[0-9,-,*]+\\#?";
//    private static final String VIDEO_DIAL_PATTERN = "\\+?[0-9,-]+";
    private long fontNormal = 70;  private long fontNormalLand = 54;
    private long fontSmall = 56;    private long fontSmallLand = 44;
    private long fontSmaller = 42;  private long fontSmallerLand = 36;

    public static final int DELAY = 0;
    public static final int PERIOD = 60*1000;
//    private Handler mHandler;
    private ToggleButton mVoicemail;
    private UIContactsViewAdaptor mUIContactsAdaptor;
    private String mFoundNumber = "";
    private String mPhoneType;
    private ImageButton mAudioButton;
    private ImageButton mRedialButton;

    public ToggleButton transducerButton;
    public ToggleButton offHook;
    private OffHookTransduceButtonInterface mCallback;

    // TODO: Rename and change types of parameters
    private String mNumber = "";
    private String mName = "";
    private String mType = "";
    public TextView mDigitsView;
    private TextView mNameView;
    private String mRedialName = "Default Name";
    private ImageView mDelete, mVideoCall;

    private OnDialerInteractionListener mListener;
//    private String mCurrentDate = "";
//    private String mCurrentTime = "";
    private Timer mDisplayTimer;
    private HorizontalScrollView mTextScroll;
    private SharedPreferences mSharedPref;
    private DialMode mMode;

    boolean enableRedial = true;
    private long mLastClickTime = 0;

    private TextView dateUndwerClock;
    private LinearLayout clockWrapper;
    private LinearLayout callControls;

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
        mNumber = getArguments().getString(NUMBER);
        mName = getArguments().getString(NAME);
        mType = getArguments().getString(TYPE);
        mMode = (DialMode) getArguments().getSerializable(MODE);
        View root = getView();
        if (root != null) {
            mDigitsView = (TextView) root.findViewById(R.id.digits);
            mNameView = (TextView) root.findViewById(R.id.name);
            dateUndwerClock = (TextView) root.findViewById(R.id.date);

            if (!mNumber.isEmpty()) {
//                mDigitsView.setText(SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()));
//                mNameView.setText(SimpleDateFormat.getDateInstance(DateFormat.FULL).format(new Date()));
            } else {
                mDigitsView.setText(mNumber);
                mNameView.setText(mName);
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        clockWrapper = (LinearLayout) view.findViewById(R.id.clock_wrapper);

        if (getView() != null) {
            mVoicemail = (ToggleButton) getView().findViewById(R.id.voicemail);
            mVoicemail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e(TAG, "voiceMail number " + SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber());
                    if (mListener != null && (SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber() != null)) {
                        mListener.onDialerInteraction(SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber(), ACTION.AUDIO);
                    }
                }
            });
        }
        onMessageWaitingStatusChanged(SDKManager.getInstance().getVoiceMessageAdaptor().getVoiceState());
        onVoicemailNumberChanged(SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber());

        if(getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
            int resId = ((MainActivity) getActivity()).getDeviceResIdFromSharedPref();
            offHook.setBackgroundResource(resId);
        }

        mSharedPref = this.getActivity().getSharedPreferences(REDIAL_NUMBER, Context.MODE_PRIVATE);
        mTextScroll = (HorizontalScrollView) getView().findViewById(R.id.textScroll);
        mDigitsView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //getFirstContact(mNumber);

                if (mNumber != null && mNumber.length() > 0) {
                    new GetPhoneNumberAsync(mNumber).execute();
                }else{
                    mNameView.setText("");
                }
                if (mNumber != null && mNumber.equals("")) {
                    clockWrapper.setVisibility(View.VISIBLE);
                    if (SDKManager.getInstance().getVoiceMessageAdaptor().getVoicemailNumber() != null)
                        mVoicemail.setVisibility(View.VISIBLE);
                    mDelete.setVisibility(View.INVISIBLE);
                    mNameView.setVisibility(View.INVISIBLE);

                    if(isAdded()&&mRedialButton!=null&&getResources().getBoolean(R.bool.is_landscape) == true) {
                        if(enableRedial)
                            mRedialButton.setVisibility(View.VISIBLE);
                        else
                            mRedialButton.setVisibility(View.INVISIBLE);
                    }
                } else {
                    mVoicemail.setVisibility(View.INVISIBLE);
                    if (mMode == DialMode.OFF_HOOK) {
                        clockWrapper.setVisibility(View.INVISIBLE);
                        mDelete.setVisibility(View.INVISIBLE);
                        mNameView.setVisibility(View.INVISIBLE);
                        if(isAdded()&&mRedialButton!=null&&getResources().getBoolean(R.bool.is_landscape) == true) {
                            mRedialButton.setVisibility(View.VISIBLE);
                        }
                    } else {
                        clockWrapper.setVisibility(View.INVISIBLE);
                        mDelete.setVisibility(View.VISIBLE);
                        mNameView.setVisibility(View.INVISIBLE);
                        if(isAdded()&&mRedialButton!=null&&getResources().getBoolean(R.bool.is_landscape) == true) {
                            mRedialButton.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // resizing text in TextView depending of a character number
                int mTextLength = mDigitsView.getText().length();
                try {
                    if (isAdded()&&getResources().getBoolean(R.bool.is_landscape) == false) {
                        if (mTextLength >= 10 && mTextLength < 14) {
                            mDigitsView.setTextSize(fontSmall);
                        } else if (mTextLength >= 14) {
                            mDigitsView.setTextSize(fontSmaller);
                        } else if (mTextLength < 10) {
                            mDigitsView.setTextSize(fontNormal);
                        }
                    } else {
                        if (mTextLength >= 10 && mTextLength < 14) {
                            mDigitsView.setTextSize(fontSmallLand);
                        } else if (mTextLength >= 14) {
                            mDigitsView.setTextSize(fontSmallerLand);
                        } else if (mTextLength < 10) {
                            mDigitsView.setTextSize(fontNormalLand);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it
     * can later be reconstructed in a new instance of its process is
     * restarted.  If a new instance of the fragment later needs to be
     * created, the data you place in the Bundle here will be available
     * in the Bundle given to {@link #onCreate(Bundle)},
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}, and
     * {@link #onActivityCreated(Bundle)}.
     * <p/>
     * <p>This corresponds to {@link com.avaya.android.vantage.basic.activities.MainActivity#onSaveInstanceState(Bundle)
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
        super.onSaveInstanceState(outState);
        outState.putString(NAME, mName);
        outState.putString(NUMBER, mNumber);
        outState.putString(TYPE, mType);
        outState.putSerializable(MODE, mMode);
    }

    public DialerFragment() {
        // Required empty public constructor
    }

    Handler mHandler = new Handler();

    @Override
    public void onResume() {
        super.onResume();
        //startClockTimer();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                {
                    dateUndwerClock.setText(SimpleDateFormat.getDateInstance(DateFormat.FULL).format(new Date()));

                    mHandler.postDelayed(this, PERIOD);
                }
            }
        };
        mHandler.post(runnable);

        onMessageWaitingStatusChanged(SDKManager.getInstance().getVoiceMessageAdaptor().getVoiceState());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mDisplayTimer != null) {
            mDisplayTimer.cancel();
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param number number to preload the dialer
     * @param name   number to preload the dialer
     * @param type   contact type
     * @param mode   dial mode
     * @return A new instance of fragment DialerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DialerFragment newInstance(String number, String name, String type, DialMode mode) {
        DialerFragment fragment = new DialerFragment();
        Bundle args = new Bundle();
        args.putString(NUMBER, number);
        args.putString(NAME, name);
        args.putString(TYPE, type);
        args.putSerializable(MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNumber = getArguments().getString(NUMBER);
            mName = getArguments().getString(NAME);
            mType = getArguments().getString(TYPE);
            mMode = (DialMode) getArguments().getSerializable(MODE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //int resId = Utils.isCameraSupported() ? R.layout.main_dialer : R.layout.main_dialer_k165;
        int resId = R.layout.main_dialer;
        View root = inflater.inflate(resId, container, false);
        TableLayout dialerGrid = (TableLayout) root.findViewById(R.id.activity_dialer_pad);
        String[] digits = getResources().getStringArray(R.array.dialer_numbers);
        String[] letters = getResources().getStringArray(R.array.dialer_letters);

        // dialpad buttons ID's
        int buttonIds[] = {
                R.id.mb1,
                R.id.mb2,
                R.id.mb3,
                R.id.mb4,
                R.id.mb5,
                R.id.mb6,
                R.id.mb7,
                R.id.mb8,
                R.id.mb9,
                R.id.mba,
                R.id.mbz,
                R.id.mbp
        };

        if (isAdded()&&getResources().getBoolean(R.bool.is_landscape) == false) {
            dialerGrid.setClickable(false);
            for (int i = 0; i < buttonIds.length; i++) {
                configureButton(dialerGrid.findViewById(buttonIds[i]), digits[i], letters[i]);
            }
        }

        mDelete = (ImageView) root.findViewById(R.id.delete);
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDigit();
            }
        });
        mDelete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clear();
                return true;
            }
        });

        root.findViewById(R.id.redialButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String redialNumber = mSharedPref.getString(REDIAL_NUMBER, "");
                if (redialNumber.length() > 0) {
                    mNumber = redialNumber;
                    getFirstContact(mNumber);
                    mDigitsView.setText(mNumber);
                }
            }
        });

        if(isAdded()&&getResources().getBoolean(R.bool.is_landscape) == true) {
            mRedialButton = (ImageButton) root.findViewById(R.id.redialButton);
        }

//        Log.e("TESTTETSTEST",String.valueOf( ((MainActivity) getActivity()).isAccessibilityEnabled));
//        Log.e("TESTTETSTEST",String.valueOf( ((MainActivity) getActivity()).isExploreByTouchEnabled));

        if(isAdded()&&getResources().getBoolean(R.bool.is_landscape) == false) {
            callControls = (LinearLayout) root.findViewById(R.id.call_controls);
            if (((MainActivity) getActivity()) != null && ((MainActivity) getActivity()).isAccessibilityEnabled == true && ((MainActivity) getActivity()).isExploreByTouchEnabled == true) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) callControls.getLayoutParams();
                params.setMargins(0, 26, 10, 0); //substitute parameters for left, top, right, bottom
                callControls.setLayoutParams(params);
            } else {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) callControls.getLayoutParams();
                params.setMargins(0, 66, 10, 0); //substitute parameters for left, top, right, bottom
                callControls.setLayoutParams(params);
            }
        }

        configureRedialButton(root);

        mAudioButton = (ImageButton) root.findViewById(R.id.audioButton);
        mVideoCall = (ImageView) root.findViewById(R.id.contact_item_call_video);

        if(getResources().getBoolean(R.bool.is_landscape) == true) {

            transducerButton = (ToggleButton)  root.findViewById(R.id.transducer_button);
            transducerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLastClickTime = SystemClock.elapsedRealtime();

                    mCallback.triggerTransducerButton(v);
                }
            });

            offHook = (ToggleButton)  root.findViewById(R.id.off_hook);
            offHook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if((MainActivity)getActivity()!=null)
                        ((MainActivity) getActivity()).setOffhookButtosChecked(offHook.isChecked());

                    mLastClickTime = SystemClock.elapsedRealtime();

                    mCallback.triggerOffHookButton(v);
                }
            });

            if((MainActivity)getActivity()!=null) {
                boolean checked = ((MainActivity)getActivity()).isOffhookChecked();
                offHook.setChecked(checked);
            }
        }


            int visibility = Utils.isCameraSupported() ? View.VISIBLE : View.GONE;
        setVideoButtonVisibility(visibility);

        enableVideo(SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled());

        mAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dialPattern;
                /*if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_IPOFFICE))
                /*String dialPattern;
                if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_IPOFFICE))
                    dialPattern = IPO_AUDIO_DIAL_PATTERN;
                else
                    dialPattern = AURA_AUDIO_DIAL_PATTERN;*/
                if (mNumber.length() > 0 /*&& mNumber.matches(dialPattern)*/) {
                    doAction(mNumber, ACTION.AUDIO);
                    GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_DIALER_EVENT);
                } else if (mNumber.length() == 0 && enableRedial) {
                    String redialNumber = mSharedPref.getString(REDIAL_NUMBER, "");
                    if (redialNumber.length() > 0) {
                        mNumber = redialNumber;
                        getFirstContact(mNumber);
                        mDigitsView.setText(mNumber);
                        if (mDelete != null) {
                            clockWrapper.setVisibility(mMode == DialMode.EDIT ? View.INVISIBLE : View.VISIBLE);
                            mDelete.setVisibility(mMode == DialMode.EDIT ? View.VISIBLE : View.INVISIBLE);
                            mNameView.setVisibility(mMode == DialMode.EDIT ? View.VISIBLE : View.INVISIBLE );
                            if(isAdded()&&mRedialButton!=null&&getResources().getBoolean(R.bool.is_landscape) == true) {
                                mRedialButton.setVisibility(mMode == DialMode.EDIT ? View.INVISIBLE : View.VISIBLE);
                            }
                        }
                    }
                }
            }
        });

        setDisplayInternal(root);
//        mCurrentDate = SimpleDateFormat.getDateInstance(DateFormat.FULL).format(new Date());
//        mCurrentTime = DateUtils.formatDateTime(ElanApplication.getContext(), new Date().getTime(), DateUtils.FORMAT_SHOW_TIME);
//        updateDialerDisplay();
        if (mNameView != null) {
            mNameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mNumber != null && mNumber.trim().length() > 0) {
                        String found[] = LocalContactInfo.phoneNumberSearch(mNumber);
                        if (found != null && found[1].length() > 0) {
                            mNumber = found[1].replaceAll("\\D+", "");
                            mDigitsView.setText(mNumber);
                        } else {
                            if (mFoundNumber != null && mFoundNumber.length() > 0) {
                                mNumber = mFoundNumber.replaceAll("\\D+", "");
                                mDigitsView.setText(mNumber);
                            }
                        }
                    }
                    if(mMode != DialMode.OFF_HOOK) {
                        SharedPreferences.Editor editor = mSharedPref.edit();
                        editor.putString(REDIAL_NUMBER, mNumber);
                        editor.apply();
                    }
                }
            });
        }
        return root;
    }


    /**
     * Preparing and configuring buttons onClickListeners
     *
     * @param button       {@link View} for which onClickListener have to be set
     * @param digitString  text to be set on button view
     * @param letterString text to be set under number in button
     */
    private void configureButton(View button, final String digitString, String letterString) {
        final TextView digit = (TextView) button.findViewById(R.id.digit);
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
                mNumber += digitString;
                mDigitsView.setText(mNumber);
                //scrollRight();
                if (mMode == DialMode.OFF_HOOK) {
                    SharedPreferences.Editor editor = mSharedPref.edit();
                    editor.putString(REDIAL_NUMBER, mNumber);
                    editor.apply();
                    if(DialerFragment.this.mListener!=null)
                        DialerFragment.this.mListener.onDialerInteraction(digitString, ACTION.DIGIT);
                }


            }
        });

        // display + if user is holding 0
        if (digitString.equals("0")) {
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mNumber.equalsIgnoreCase("")) {
                        mNumber += "+";
                        mDigitsView.setText(mNumber);
                        //scrollRight();
                        return true;
                    } else {
                        return false;
                    }

                }
            });
        }
    }

    public void onHardKeyClick(String number){
        if (mMode == DialMode.OFF_HOOK) {
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString(REDIAL_NUMBER, mNumber);
            editor.apply();
        }
        if(DialerFragment.this.mListener!=null) {
            boolean keepDigitAfterUsage = DialerFragment.this.mListener.onDialerInteraction(number, ACTION.DIGIT);
            if (!keepDigitAfterUsage) {
                deleteDigit();
            }
        }
    }

    /**
     * Removing digits from dialer
     */
    public void deleteDigit() {
        if (mNumber.length() > 0) {
            mNumber = mNumber.substring(0, mNumber.length() - 1);
            mDigitsView.setText(mNumber);
        }
        if (mNumber.length() == 0){
            isFirstDigitInDial = true;
        }
    }

    /**
     * Refresh clock time.
     */
//    private void startClockTimer() {
//        final Handler handler = new Handler();
//
//        TimerTask timertask = new TimerTask() {
//            @Override
//            public void run() {
//                handler.post(new Runnable() {
//                    public void run() {
//                        mCurrentDate = SimpleDateFormat.getDateInstance(DateFormat.FULL).format(new Date());
//                        mCurrentTime = DateUtils.formatDateTime(ElanApplication.getContext(), new Date().getTime(), DateUtils.FORMAT_SHOW_TIME);
//                        updateDialerDisplay();
//                    }
//                });
//            }
//        };
//        mDisplayTimer = new Timer(); //We need new Timer since it cannot be restarted after cancel
//        mDisplayTimer.schedule(timertask, DELAY, PERIOD); // execute in every 1 sec
//    }

    /**
     * Perform action on dialer
     *
     * @param number for which action is performed
     * @param action which is performed
     */
    private void doAction(String number, ACTION action) {
        if(mListener!=null) {
            mListener.onDialerInteraction(number, action);
            mNumber = "";
            mDigitsView.setText("");
        }
    }

    /**
     * Prepare display based on parameters provided
     *
     * @param root {@link View} for which data have to be set
     */
    private void setDisplayInternal(View root) {
        if (mDigitsView == null) {
            mDigitsView = (TextView) root.findViewById(R.id.digits);
        }
        mDigitsView.setText(mNumber);
        if (mNameView == null) {
            mNameView = (TextView) root.findViewById(R.id.name);
        }
        if (mName.length() > 0 && mType.length() > 0) {
            mNameView.setText(Html.fromHtml(getString(R.string.sample_dialer_display_name, mName, mType)));
        } else {
            mNameView.setText("");
        }
    }

    /**
     * Updating dialer display
     */
//    private void updateDialerDisplay() {
//        if (TextUtils.isEmpty(mNumber) && mDigitsView != null && mNameView != null && mDigitsView.getHandler() != null) {
//            mDigitsView.getHandler().post(new Runnable() {
//                @Override
//                public void run() {
//                    mDigitsView.setText(mCurrentTime);
//                    mNameView.setText(mCurrentDate);
//                }
//            });
//        }
//    }

    /**
     * Setting {@link UIContactsViewAdaptor} for fragment
     *
     * @param UIContactsAdaptor
     */
    public void setUIContactsAdaptor(UIContactsViewAdaptor UIContactsAdaptor) {
        this.mUIContactsAdaptor = UIContactsAdaptor;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(String number) {
        if (mListener != null) {
            mListener.onDialerInteraction(number, ACTION.AUDIO);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDialerInteractionListener) {
            mListener = (OnDialerInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDialerInteractionListener");
        }
        try {
            mCallback = (OffHookTransduceButtonInterface) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "OnActiveCallInteractionListener cast failed");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (mDisplayTimer != null) {
            mDisplayTimer.cancel();
        }
        mDisplayTimer = null;
    }

    /**
     * Obtaining name to be redialed
     *
     * @return String representation of name to be redialed
     */
    public String getRedialName() {
        return mRedialName;
    }

    /**
     * Processing configuration change
     */
    @Override
    public void applyConfigChange() {
        View root = getView();
        configureRedialButton(root);
        enableVideo(SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled());
    }
    boolean isFirstDigitInDial = true;
    /**
     * Get character from keyboard
     */
    public void dialFromKeyboard(String number) {
        if (getResources().getBoolean(R.bool.is_landscape) == false) {
            mNumber += number;
            mDigitsView.setText(mNumber);
            mNameView.setText(getRedialName());
            getFirstContact(mNumber);
        }else if (getResources().getBoolean(R.bool.is_landscape) == true) {

            if(number.equalsIgnoreCase("+") && isFirstDigitInDial && mDigitsView.getText().toString().length() == 1){
                mNumber = "+";
                isFirstDigitInDial = false;
            }else  if(!number.equalsIgnoreCase("+")) {
                mNumber += number;
            }
            mDigitsView.setText(mNumber);
            mNameView.setText(getRedialName());
            getFirstContact(mNumber);
        }
    }

    /**
     * Processing message waiting status
     *
     * @param voiceMsgsAreWaiting boolean based on which we are showing or hiding voicemail button
     */
    @Override
    public void onMessageWaitingStatusChanged(boolean voiceMsgsAreWaiting) {
        Log.d(TAG, "onMessageWaitingStatusChanged " + voiceMsgsAreWaiting);
        mVoicemail.setChecked(voiceMsgsAreWaiting);
    }

    @Override
    public void onVoicemailNumberChanged(String voicemailNumber) {
        Log.d(TAG, "onVoicemailNumberChanged voicemailNumber="+voicemailNumber);
        if (voicemailNumber== null) {
            mVoicemail.setVisibility(View.INVISIBLE);
        }
        else {
            mVoicemail.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCallDigitCollectionCompleted(UICall call) {
        mMode = DialMode.EDIT;
    }


    public DialMode getMode() {
        return mMode;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDialerInteractionListener {
        boolean onDialerInteraction(String number, ACTION action);
    }

    /**
     * This method will be called every time Dialer fragment is active
     */
    public void fragmentSelected(Boolean voiceMail) {
        Log.e(TAG, "fragmentSelected: Dialer");
        onMessageWaitingStatusChanged(voiceMail);
    }

    /**
     * This method will search phones database for typed number and, if any contact is found with that
     * phone number, contact name is displayed on dialpad.
     *
     * @param number Currently typed number
     */
    private void getFirstContact(String number) {
        if (TextUtils.isEmpty(number)) {
            return;
        }
        mNameView.setText("");
        mFoundNumber = "";
        mPhoneType = "";
        String[] searchResults = LocalContactInfo.phoneNumberSearch(number);
        if (searchResults != null && searchResults[0].trim().length() > 0 && searchResults[1].trim().length() > 0) {
            mFoundNumber = searchResults[1];
            // make mPhoneType bold using html
            mPhoneType = "<b color =" + getActivity().getColor(R.color.primary) + ">"
                    + getPhoneType(searchResults[2]) + "</b>";
            mNameView.setText(Html.fromHtml(searchResults[0] + " " + mPhoneType));
        } else {
            if (mUIContactsAdaptor != null) {
                List<ContactData> mList = mUIContactsAdaptor.getEnterpriseContacts();
                mNameView.setText("");
                mFoundNumber = "";
                for (int i = 0; i < mList.size(); i++) {
                    if (mList.get(i).mPhones.size() > 0) {
                        for (int j = 0; j < mList.get(i).mPhones.size(); j++) {
                            if (mList.get(i).mPhones.get(j).Number.replaceAll("[\\D]", "").startsWith(number, 0)) {
                                mNameView.setText(mList.get(i).mName);
                                return;
                            }
                        }
                    }
                }
            }
        }

    }

    private String getPhoneType(String type) {
        try {
            if ("WORK".equalsIgnoreCase(type)) {
                return getContext().getResources().getText(R.string.contact_details_work).toString();
            } else if ("MOBILE".equalsIgnoreCase(type)) {
                return getContext().getResources().getText(R.string.contact_details_mobile).toString();
            } else if ("HOME".equalsIgnoreCase(type)) {
                return getContext().getResources().getText(R.string.contact_details_home).toString();
            } else if ("HANDLE".equalsIgnoreCase(type)) {
                return getContext().getResources().getText(R.string.contact_details_handle).toString();
            } else if ("FAX".equalsIgnoreCase(type)) {
                return getContext().getResources().getText(R.string.contact_details_fax).toString();
            } else if ("PAGER".equalsIgnoreCase(type)) {
                return getContext().getResources().getText(R.string.contact_details_pager).toString();
            } else if ("ASSISTANT".equalsIgnoreCase(type)) {
                return getContext().getResources().getText(R.string.contact_details_assistant).toString();
            } else if ("OTHER".equalsIgnoreCase(type)) {
                return getContext().getResources().getText(R.string.contact_details_other).toString();
            }
        }catch (Exception e){
            return "";
        }
        return "";
    }

    /**
     * Auto scroll digits to the right when we type new number
     */
//    private void scrollRight() {
//
//        getHandler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mTextScroll != null) {
//                    mTextScroll.fullScroll(View.FOCUS_RIGHT);
//                }
//                Log.d(TAG, "DelayScroll");
//            }
//        }, 100);
//    }

    /**
     * Making sure we create handler only once
     *
     * @return handler
     */
//    private Handler getHandler() {
//        if (mHandler == null) {
//            mHandler = new Handler();
//        }
//        return mHandler;
//    }

    /**
     * Setting up redial button
     *
     * @param root inflater
     */
    private void configureRedialButton(View root) {
        if (root != null) {
            enableRedial = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_REDIAL);
            ImageButton redial = (ImageButton) root.findViewById(R.id.redialButton);
            if (redial != null) {
                redial.setEnabled(enableRedial);
                if(enableRedial)
                    redial.setVisibility(View.VISIBLE);
                else
                    redial.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * Enabling video
     *
     * @param enable should we enable video
     */
    private void enableVideo(Boolean enable) {
        mVideoCall.setEnabled(enable);
        if (enable) {
            mVideoCall.setAlpha(1f);
            mVideoCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mNumber.length() > 0 /*&& mNumber.matches(VIDEO_DIAL_PATTERN)*/) {
                        doAction(mNumber, ACTION.VIDEO);
                        GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_DIALER_EVENT);
                    } else if (mNumber.length() == 0 && enableRedial) {
                        String redialNumber = mSharedPref.getString(REDIAL_NUMBER, "");
                        if (redialNumber.length() > 0) {
                            mNumber = redialNumber;
                            getFirstContact(mNumber);
                            mDigitsView.setText(mNumber);
                            if (mMode == DialMode.EDIT) {
                                clockWrapper.setVisibility(View.INVISIBLE);
                                mDelete.setVisibility(View.VISIBLE);
                                mNameView.setVisibility(View.VISIBLE);
                                if(isAdded()&&mRedialButton!=null&&getResources().getBoolean(R.bool.is_landscape) == true) {
                                    mRedialButton.setVisibility(View.INVISIBLE);
                                }
                            }
                        }
                    }
                }
            });
        } else {
            mVideoCall.setAlpha(0.5f);
            mVideoCall.setClickable(false);
        }
    }

    /**
     * Setting up {@link DialMode}
     *
     * @param mode {@link DialMode} to be set
     */
    public void setMode(DialMode mode) {
        mMode = mode;
        mVideoCall.setEnabled(mMode == DialMode.EDIT);
        enableRedial = (mMode == DialMode.EDIT);
        View view = getView();
        if (view != null) {
            mAudioButton = (ImageButton) view.findViewById(R.id.audioButton);
            mRedialButton = (ImageButton) view.findViewById(R.id.redialButton);

            if (mAudioButton != null) {
                mAudioButton.setEnabled(mMode == DialMode.EDIT);
            }
            if (mRedialButton != null) {
                mRedialButton.setEnabled(mMode == DialMode.EDIT);
            }
            if (mDelete != null) {
                clockWrapper.setVisibility(mMode == DialMode.EDIT ?View.INVISIBLE: View.VISIBLE );
                mDelete.setVisibility(mMode == DialMode.EDIT ? View.VISIBLE : View.INVISIBLE);
                mNameView.setVisibility(mMode == DialMode.EDIT ? View.VISIBLE : View.INVISIBLE);
                if(isAdded()&&mRedialButton!=null&&getResources().getBoolean(R.bool.is_landscape) == true) {
                    enableRedial = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_REDIAL);
                    if(enableRedial)
                        mRedialButton.setVisibility(mMode == DialMode.EDIT ? View.INVISIBLE : View.VISIBLE);
                    else
                        mRedialButton.setVisibility(View.INVISIBLE);
                }

            }
        }
        clear();
    }

    /**
     * Clearing digits and number view on {@link DialerFragment}
     */
    public void clear() {
        mNumber = "";
        mDigitsView.setText("");
        isFirstDigitInDial = true;
    }

    public enum DialMode {OFF_HOOK, EDIT}

    public enum ACTION {AUDIO, VIDEO, REDIAL, DIGIT}

    /**
     * This Async Task will be used to get contact name by search query, and display it on TextView.
     * In case no contact is found under local contacts, we search by Enterprise contacts.
     */
    private class GetPhoneNumberAsync extends AsyncTask<Void, Void, Void> {
        private String[] searchResults;
        private String asContactName;
        private String asContactNumber;
        private String asNumberType;
        private String asSearchQuery;
        WeakReference<TextView> asWeakDigits;

        private GetPhoneNumberAsync(String searchQuery) {
            this.asSearchQuery = searchQuery;
        }

        @Override
        protected Void doInBackground(Void... params) {
            searchResults = LocalContactInfo.phoneNumberSearch(asSearchQuery);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mNameView != null) {
                asWeakDigits = new WeakReference<>(mNameView);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mMode != DialMode.OFF_HOOK ) {
                clockWrapper.setVisibility(View.INVISIBLE);
                mNameView.setVisibility(View.VISIBLE);
            }
            TextView name = asWeakDigits.get();
            if (name != null && searchResults != null && searchResults[0].trim().length() > 0 && searchResults[1].trim().length() > 0) {
                asContactName = searchResults[0];
                asContactNumber = searchResults[1];
                asNumberType = getPhoneType(searchResults[2]);
                if (ElanApplication.getContext() != null) {
                    mNameView.setText(Html.fromHtml(ElanApplication.getContext().getString(R.string.sample_dialer_display_name, asContactName, asNumberType)));
                }
            } else {
                if (name != null) {
                    mNameView.setText("");

                    if (mUIContactsAdaptor != null) {
                        List<ContactData> mList = mUIContactsAdaptor.getEnterpriseContacts();
                        for (int i = 0; i < mList.size(); i++) {
                            if (mList.get(i).mPhones.size() > 0) {
                                for (int j = 0; j < mList.get(i).mPhones.size(); j++) {
                                    if (mList.get(i).mPhones.get(j).Number.replaceAll("[\\D]", "").startsWith(asSearchQuery, 0)) {
                                        mNameView.setText(mList.get(i).mName);
                                        if (mList.get(i).mPhones.get(j).Number != null) {
                                            mFoundNumber = CallData.parsePhone(mList.get(i).mPhones.get(j).Number);
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                    }

                }
            }
            Log.d(TAG, "onPostExecute: " + asContactName + "," + asContactNumber + ", ");
        }
    }

    /**
     * Displays the number in the digits view and contact name if matched
     * @param number number to be called
     */
    public void setDialer(String number) {
        mNumber = number;
        getFirstContact(mNumber);
        mDigitsView.setText(mNumber);
    }

    /**
     * Sets visibility of the VideoCall button and adjust the look of
     * the audio button accordingly
     * @param visibility View visibility
     */
    public void setVideoButtonVisibility(int visibility){
        mVideoCall.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            mAudioButton.setImageResource(R.drawable.dialer_audio);
        }else if (visibility == View.GONE) {
            mAudioButton.setImageResource(R.drawable.dialer_audio_center);
        }
    }
}
