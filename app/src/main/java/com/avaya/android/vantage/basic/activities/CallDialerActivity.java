package com.avaya.android.vantage.basic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.csdk.ConfigParametersNames;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.views.interfaces.FinishCallDialerActivityInterface;

import static com.avaya.android.vantage.basic.Constants.DigitKeys;

/**
 * Activity responsible for showing and working with call dialer which is used for
 * call transfers or adding additional person to call and creating conference call.
 */

public class CallDialerActivity extends AppCompatActivity implements FinishCallDialerActivityInterface {

    // TODO: Rename and change types of parameters
    private static final String TAG = "CallDialerActivity";
    private String mNumber = "";
    private String mName = "";
    private String mType = "";
    private TextView mDigitsView;
    private TextView mNameView;
    private TextView mAudioCallButton;
    private ImageView mExitDialpad;
    private ImageButton mContactItemCallVideo;
    private ImageButton mRedialButton;
    private ImageView mDelete;
    private HorizontalScrollView mTextScroll;
    private Handler mHandler;
    private String mRedialNumber = "";
    private String mRedialName = "";
    private int mCallActiveCallID = -1;
    private final long fontNormal = 56;
    private final long fontSmall = 42;
    private final long fontSmaller = 28;
    private String mRequestName;

    boolean isToLockPressButton = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.callDialerActivity = this;

        setContentView(R.layout.call_dialpad_layout);
        boolean isConferenceCall = getIntent().getBooleanExtra(Constants.IS_CONFERENCE, false);
        mCallActiveCallID = getIntent().getIntExtra(Constants.CALL_ID, -1);
        setupFullscreen();

        mDigitsView = (TextView) findViewById(R.id.call_digits);
        mNameView = (TextView) findViewById(R.id.call_name);
        if (mDigitsView != null) {
            mDigitsView.setText("");
        }
        if (mNameView != null) {
            mNameView.setText("");
        }
        mTextScroll = (HorizontalScrollView) findViewById(R.id.scroll_call_digits);

        TableLayout dialerGrid = (TableLayout) findViewById(R.id.activity_dialer_pad);
        if (dialerGrid != null) {
            dialerGrid.setClickable(false);
        }
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


        if (dialerGrid != null) {
            for (int i = 0; i < buttonIds.length; i++) {
                configureButton(dialerGrid.findViewById(buttonIds[i]), digits[i], letters[i]);
            }
        }

        mDelete = (ImageView) findViewById(R.id.call_delete);
        if (mDelete != null) {
            mDelete.setVisibility(View.INVISIBLE);
            mDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mNumber.length() > 0) {
                        mNumber = mNumber.substring(0, mNumber.length() - 1);
                        mDigitsView.setText(mNumber);
                    }
                }
            });
            mDelete.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mNumber = "";
                    mDigitsView.setText("");
                    return true;
                }
            });
        }

        configureRedialButton();

        mAudioCallButton = (TextView) findViewById(R.id.audio_call_button);
        if (isConferenceCall) {
            if (mAudioCallButton != null) {
                mAudioCallButton.setText(getText(R.string.feature_dialog_conference));
            }
            mRequestName = getResources().getString(R.string.merge_complete);
        } else {
            if (mAudioCallButton != null) {
                mAudioCallButton.setText(getText(R.string.feature_dialog_transfer));
            }
            mRequestName = getResources().getString(R.string.trasfer_complete);
        }

        if (mAudioCallButton != null) {
            mAudioCallButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mDigitsView.getText().toString().equalsIgnoreCase("")) {
                        Utils.sendSnackBarData(v.getContext(), mRequestName, Utils.SNACKBAR_LONG);
                        Intent data = new Intent(getPackageName() + Constants.ACTION_TRANSFER);
                        data.putExtra(Constants.CALL_ID, mCallActiveCallID);
                        data.putExtra(Constants.TARGET, mDigitsView.getText().toString());
                        Log.d(TAG, "Call ID: " + mCallActiveCallID + ", Number: " + mDigitsView.getText().toString());
                        setResult(RESULT_OK, data);
                        finish();
                    } else {
                        Utils.sendSnackBarData(v.getContext(), getString(R.string.no_number_message), Utils.SNACKBAR_LONG);
                    }
                }
            });
        }

        mContactItemCallVideo = (ImageButton) findViewById(R.id.contact_item_call_video);
        if (SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
            if (mContactItemCallVideo != null) {
                mContactItemCallVideo.setEnabled(true);
                mContactItemCallVideo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
            }
        } else {
            if (mContactItemCallVideo != null) {
                mContactItemCallVideo.setEnabled(false);
            }
        }

        mExitDialpad = (ImageView) findViewById(R.id.exit_dialpad);
        if (mExitDialpad != null) {
            mExitDialpad.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }

        mDigitsView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mNumber.equals("")) {
                    mDelete.setVisibility(View.INVISIBLE);
                } else {
                    mDelete.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // resizing text in TextView depending of a character number
                int mTextLength = mDigitsView.getText().length();

                if (mTextLength >= 11 && mTextLength < 15) {
                    mDigitsView.setTextSize(fontSmall);
                } else if (mTextLength >= 15) {
                    mDigitsView.setTextSize(fontSmaller);
                } else if (mTextLength < 11) {
                    mDigitsView.setTextSize(fontNormal);
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
                scrollRight();
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
                        scrollRight();
                        return true;
                    } else {
                        return false;
                    }

                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(getResources().getBoolean(R.bool.is_landscape) == true && !isToLockPressButton) {

            isToLockPressButton = true;

            int keyunicode = event.getUnicodeChar(event.getMetaState());
            char character = (char) keyunicode;
            String digit = "" + character;

            mNumber += digit;
            mDigitsView.setText(mNumber);
            mNameView.setText(getRedialName());

            if(DigitKeys.contains(event.getKeyCode())) {
                event.startTracking();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            if(keyCode == KeyEvent.KEYCODE_0 ){
                mNumber = mNumber.replace('0','+');
                mDigitsView.setText(mNumber);
            }
        }
        if(DigitKeys.contains(event.getKeyCode()))
            return true;
        else
            return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (getResources().getBoolean(R.bool.is_landscape) == true) {
            isToLockPressButton = false;
        }
        if(DigitKeys.contains(event.getKeyCode()))
            return true;
        else
            return super.onKeyUp(keyCode, event);
    }


    /**
     * Perform contact search based by number
     *
     * @param number which we search for
     * @return null at moment
     */
    private String doContactNameSearchByNumber(String number) {
        return null;
    }

    /**
     * Return String representation of last number to be redialed
     *
     * @return String with representation of last dialed number
     */
    public String getRedialNumber() {
        return mRedialNumber;
    }

    /**
     * Return String representation of name of last user which have to be redialed
     *
     * @return String with representation of name of last user which have to be redialed
     */
    public String getRedialName() {
        return mRedialName;
    }

    /**
     * Change screen params to fullscreen preferences.
     */
    private void setupFullscreen() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    /**
     * Method responsible for scrolling text in TextView while we are typing number which have
     * to be called dialed
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
     * Returning Handler require for performing some delayed jobs in {@link #scrollRight()}
     *
     * @return Handler required for use in {@link #scrollRight()}
     */
    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler();
        }
        return mHandler;
    }

    /**
     * Setting configuration for redial button. In case in class {@link ConfigParametersNames}
     * parameter {@link ConfigParametersNames#ENABLE_REDIAL} is set to true we will enable redial
     * functionality.
     */
    private void configureRedialButton() {

        boolean enableRedial = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getConfigBooleanParam(ConfigParametersNames.ENABLE_REDIAL);
        mRedialButton = (ImageButton) findViewById(R.id.redialButton);
        if (enableRedial) {
            if (mRedialButton != null) {
                mRedialButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String redialNumber = getRedialNumber();
                        if (redialNumber.length() > 0) {
                            mNumber = redialNumber;
                            mDigitsView.setText(mNumber);
                            mNameView.setText(getRedialName());
                        }
                    }
                });
            }
        }
        if (mRedialButton != null) {
            mRedialButton.setEnabled(enableRedial);
        }
    }

    private class OnDialpadButtonClickListener implements GridView.OnItemClickListener {


        /**
         * Callback method to be invoked when an item in this AdapterView has
         * been clicked.
         * <p/>
         * Implementers can call getItemAtPosition(position) if they need
         * to access the data associated with the selected item.
         *
         * @param parent   The AdapterView where the click happened.
         * @param view     The view within the AdapterView that was clicked (this
         *                 will be a view provided by the adapter)
         * @param position The position of the view in the adapter.
         * @param id       The row id of the item that was clicked.
         */
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Pair<String, String> data = null;
            if (parent.getItemAtPosition(position) instanceof Pair) {
                //noinspection unchecked
                data = (Pair<String, String>) parent.getItemAtPosition(position);
            }
            if (data != null) {
                mNumber += data.first;
                mDigitsView.setText(mNumber);
                String contactName = doContactNameSearchByNumber(mNumber);//TODO: make this Async
                //noinspection ConstantConditions
                if (TextUtils.isEmpty(contactName)) {
                    mNameView.setText("");
                }
                scrollRight();
            }
        }
    }



    @Override
    public void killCallDialerActivity() {
        finish();
    }

}