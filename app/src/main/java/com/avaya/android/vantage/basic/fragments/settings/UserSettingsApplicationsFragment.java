package com.avaya.android.vantage.basic.fragments.settings;

import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.csdk.ConfigParametersNames;
import com.avaya.android.vantage.basic.csdk.SDKManager;

import java.util.Arrays;

import static com.avaya.android.vantage.basic.Constants.EXIT_PIN;
import static com.avaya.android.vantage.basic.Constants.NEW_CONTACT_PREF;
import static com.avaya.android.vantage.basic.Constants.REFRESH_CONTACTS;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_IPOFFICE;
import static com.avaya.android.vantage.basic.fragments.settings.UserSettingsApplicationsFragment.PreferenceItem.DISPLAY;
import static com.avaya.android.vantage.basic.fragments.settings.UserSettingsApplicationsFragment.PreferenceItem.NEW_TYPE;
import static com.avaya.android.vantage.basic.fragments.settings.UserSettingsApplicationsFragment.PreferenceItem.SORT;
import static com.avaya.android.vantage.basic.fragments.settings.UserSettingsApplicationsFragment.PreferenceItem.firstChoicePreferenceIndex;
import static com.avaya.android.vantage.basic.fragments.settings.UserSettingsApplicationsFragment.PreferenceItem.secondChoicePreferenceIndex;

/**
 * This class controls application settings for user.
 */
public class UserSettingsApplicationsFragment extends PreferenceFragment implements ConfigChangeApplier {


    private static final String TAG = UserSettingsApplicationsFragment.class.getSimpleName();

    private BroadcastReceiver mConfigChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.LOCAL_CONFIG_CHANGE.equalsIgnoreCase(intent.getAction())) {
                applyConfigChange();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.LOCAL_CONFIG_CHANGE);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mConfigChangeReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mConfigChangeReceiver);
    }

    private static final String NAME_DISPLAY_PREFERENCE = Constants.NAME_DISPLAY_PREFERENCE;
    private static final String NAME_SORT_PREFERENCE = Constants.NAME_SORT_PREFERENCE;
    private static final String REFRESH_FAVORITES = "refreshFavorites";
    private static final String REFRESH_RECENTS = "refreshRecents";
    private static final String USER_PREFERENCE = Constants.USER_PREFERENCE;

    private SharedPreferences mSharedPreferences;

    private Dialog mDialog;
    private PreferenceItem mSelectedPreferenceItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_applications);
        mSharedPreferences = getActivity().getSharedPreferences(USER_PREFERENCE, Context.MODE_PRIVATE);
        loadSummary();
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
        handlePinningMode();
    }

    /**
     * This method controls the Application pinning mode:
     * 1. Enables/Disables the corresponding switch for exit pinning mode based on config settings
     * 2. Implements entering and exiting of the pinning mode when the pref value is changed by the switch.
     */
    public void handlePinningMode() {
        String pinApp = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.PIN_APP);

        boolean isPinningAllowedByPolicy = (getContext() != null && getContext().getSystemService(DevicePolicyManager.class).isLockTaskPermitted(getContext().getPackageName()));
        final SwitchPreference p = (SwitchPreference) findPreference(Constants.EXIT_PIN);


        if (pinApp != null){
            p.setEnabled(isPinningAllowedByPolicy && Arrays.asList(pinApp.split(",")).contains(getActivity().getApplicationInfo().packageName));
            if (p.isEnabled()) {
                if(p.isChecked()) {
                    getActivity().startLockTask();
                    ElanApplication.isPinAppLock = true;
                }
                else if (ElanApplication.isPinAppLock) {
                    getActivity().stopLockTask();
                    ElanApplication.isPinAppLock = false;
                }
            }
        } else {
            p.setEnabled(false);
            getActivity().stopLockTask();
            ElanApplication.isPinAppLock = false;
        }

        p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean isPinned = (Boolean) newValue;

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                final AlertDialog alertDialog = builder.setView(R.layout.unpin_password).setNegativeButton(R.string.cancel, null).create();
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getText(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText passwordView = (EditText) alertDialog.findViewById(R.id.password);
                        String admin_passwd = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue("ADMIN_PASSWORD");
                        if (TextUtils.isEmpty(admin_passwd))
                            admin_passwd = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue("PROCPSWD");
                        else if (TextUtils.isEmpty(admin_passwd))
                            admin_passwd = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue("ADMIN_PASSWD");
                        if (!TextUtils.isEmpty(admin_passwd) && passwordView != null && TextUtils.equals(passwordView.getText().toString(), admin_passwd)) {
                            if (isPinned) {
                                getActivity().startLockTask();
                                ElanApplication.isPinAppLock = true;
                            } else {
                                getActivity().stopLockTask();
                                ElanApplication.isPinAppLock = false;
                            }
                            p.setChecked(isPinned);
                        } else {
                            if (TextUtils.isEmpty(admin_passwd) && getView() != null) {
                                Snackbar.make(getView(), R.string.admin_pass_not_set, Snackbar.LENGTH_SHORT).show();
                            } else {
                                if (getView() != null) {
                                    Snackbar.make(getView(), R.string.incorrect_password, Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                });
                setImmersiveDialog(alertDialog, getActivity());
                alertDialog.show();
                //Set the dialog to immersive
                alertDialog.getWindow().getDecorView().setSystemUiVisibility(getActivity().getWindow().getDecorView().getSystemUiVisibility());
                //Clear the not focusable flag from the window
                alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

                return false;
            }
        });
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        mSelectedPreferenceItem = PreferenceItem.fromKey(preference.getKey());
        if (!Constants.EXIT_PIN.equals(mSelectedPreferenceItem.key)) {
            showImmersiveDialog(mSelectedPreferenceItem);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Show immersive fullscreen dialog for selected preference
     *
     * @param selectedPreferenceItem selected preference item of the clicked {@link Preference}
     */
    private void showImmersiveDialog(final PreferenceItem selectedPreferenceItem) {
        mDialog = new Dialog(getActivity());

        mDialog.setContentView(R.layout.dialog_preference_item);

        ((TextView) mDialog.findViewById(R.id.dialogPreferenceTitle)).setText(selectedPreferenceItem.title);
        final RadioButton firstRadio = (RadioButton) mDialog.findViewById(R.id.firstRadioButton);
        final RadioButton secondRadio = (RadioButton) mDialog.findViewById(R.id.secondRadioButton);

        final int choice = selectedPreferenceItem.choiceFromPreferences(getActivity(), mSharedPreferences, "");
        selectedPreferenceItem.setRadioText(choice, firstRadio, secondRadio);

        firstRadio.setOnClickListener(clickListener);
        secondRadio.setOnClickListener(clickListener);
        mDialog.findViewById(R.id.cancel).setOnClickListener(clickListener);

        setImmersiveDialog(mDialog, getActivity());
        mDialog.show();
    }

    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final int id = view.getId();
            if (id != R.id.cancel) { // radio button is clicked
                final int choice = id == R.id.secondRadioButton
                        ? secondChoicePreferenceIndex : firstChoicePreferenceIndex;
                ((RadioButton) view).setChecked(true);
                mSelectedPreferenceItem.saveSelectedChoice(getActivity(), mSharedPreferences, choice);
                loadSummary();
            }
            if (mDialog != null) mDialog.dismiss();
        }
    };

    /**
     * Update preferences summary UI
     */
    private void loadSummary() {
        String adminNameSortOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_SORT_ORDER);
        String adminNameDisplayOrder = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.NAME_DISPLAY_ORDER);

        DISPLAY.setSummary(getActivity(), mSharedPreferences, findPreference(DISPLAY.key), adminNameDisplayOrder);

        SORT.setSummary(getActivity(), mSharedPreferences, findPreference(SORT.key), adminNameSortOrder);

        final PreferenceItem newTypeItem = PreferenceItem.fromKey(NEW_TYPE.key); // resolve for IPO
        newTypeItem.setSummary(getActivity(), mSharedPreferences, findPreference(newTypeItem.key), "");
    }

    @Override
    public void applyConfigChange() {
        handlePinningMode();
    }

    /**
     * A workaround approach to the framework issue with Dialog disturbing Immersive/FullScreen mode<br>
     * Call this method on the instantiated {@link Dialog} in order to preserve fullscreen<br>
     * even when mDialog is shown.
     *
     * @param dialog   {@link Dialog} to be shown
     * @param activity parent {@link Activity}
     */
    public void setImmersiveDialog(final Dialog dialog, final Activity activity) {
        //Set the dialog to not focusable
        final Window window = dialog.getWindow();
        if (window == null) return;
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        final View decor = window.getDecorView();
        if (decor == null) return;
        setImmersiveUi(decor);

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                //Clear the not focusable flag from the window
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

                //Update the WindowManager with the new attributes
                WindowManager manager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
                if (manager != null) {
                    manager.updateViewLayout(
                            dialog.getWindow().getDecorView(), dialog.getWindow().getAttributes());
                }
            }
        });

        decor.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    setImmersiveUi(decor);
                }

            }
        });
    }

    /**
     * Set appropriate flags on the DecorView, to produce immersive fullscreen mode.
     *
     * @param decor DecorView to set immersive fullscreen to
     */
    public void setImmersiveUi(View decor) {
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Represents available Preference type ViewModel for the Application settings screen.<br>
     * Handles retrieval and saving of the respective covered preferences.<br>
     * Contains fields and methods required for handling preferences:<br>
     * - key represents corresponding {@link SharedPreferences} key,<br>
     * - dialog title represents corresponding {@link SharedPreferences} dialog title,<br>
     * - first summary text for the displayed {@link Preference},<br>
     * - second summary text.<br>
     * Also covers variance for types between IPO & ENTERPRISE for the same key.<br>
     * Observe method {@link PreferenceItem#fromKey(String)} for more info.
     */
    enum PreferenceItem {

        DISPLAY(NAME_DISPLAY_PREFERENCE,            /*SharedPreferences key*/
                R.string.name_display_preferences,
                R.string.first_name_first,
                R.string.last_name_first),
        SORT(NAME_SORT_PREFERENCE,
                R.string.name_sort_preferences,
                R.string.first_name_first,
                R.string.last_name_first),
        NEW_TYPE(NEW_CONTACT_PREF,
                R.string.new_contact_type_preferences,
                R.string.contact_details_add_contact_local,
                R.string.contact_details_add_contact_enterprise),
        NEW_TYPE_IPO(NEW_CONTACT_PREF,
                R.string.new_contact_type_preferences,
                R.string.contact_details_add_contact_local,
                R.string.contact_details_add_contact_personal_directory),

        /**
         * PINNING is added for consistency, to support given key resolution<br>
         * and has its own alert dialog, not managed by the {@link PreferenceItem}.
         */
        PINNING(EXIT_PIN, R.string.password_dialog_title, 0, 0);

        private final String key;
        private final int title;
        private final int summaryFirst;
        private final int summarySecond;

        static final int firstChoicePreferenceIndex = Constants.FIRST_NAME_FIRST;
        static final int secondChoicePreferenceIndex = Constants.LAST_NAME_FIRST;
        private final String firstNameFirstTag = "last,first";

        PreferenceItem(String key, int title, int summaryFirst, int summarySecond) {
            this.key = key;
            this.title = title;
            this.summaryFirst = summaryFirst;
            this.summarySecond = summarySecond;
        }

        /**
         * Returns the {@link PreferenceItem} corresponding to the passed key.<br>
         * Method also accounts for the variance between IPO/ENTERPRISE,<br>
         * effectively resolving for the same key if the type should be one of:<br>
         * - {@link PreferenceItem#NEW_TYPE} for ENTERPRISE or<br>
         * - {@link PreferenceItem#NEW_TYPE_IPO} for IPO
         *
         * @param key preferences key
         * @return {@link PreferenceItem} corresponding to the given key
         */
        static PreferenceItem fromKey(String key) {
            for (PreferenceItem item : PreferenceItem.values()) {
                if (item.key.equals(key)) {
                    if (item == NEW_TYPE || item == NEW_TYPE_IPO) { // resolve new type for IPO
                        final boolean isIpo = SDKManager.getInstance().getDeskPhoneServiceAdaptor()
                                .getConfigBooleanParam(ENABLE_IPOFFICE);
                        return isIpo ? NEW_TYPE_IPO : NEW_TYPE;
                    }
                    return item;
                }
            }
            return DISPLAY; // default result in case there is no matching key
        }

        /**
         * Set text for the passed radio button using the respective summary for the current {@link PreferenceItem}<br>
         * Two buttons are supported with respective resource ids:<br>
         * - {@link PreferenceItem#summaryFirst} and<br>
         * - {@link PreferenceItem#summarySecond}<br>
         * Respective {@link RadioButton} of the choice index is set checked.
         *
         * @param choice       index of the {@link RadioButton} to be set as checked
         * @param radioButtons array of {@link RadioButton}s - only two will be handled: 0 & 1
         */
        void setRadioText(int choice, RadioButton... radioButtons) {
            radioButtons[firstChoicePreferenceIndex].setText(summaryFirst);
            radioButtons[secondChoicePreferenceIndex].setText(summarySecond);
            radioButtons[choice].setChecked(true);
        }

        /**
         * Sets the {@link Preference} summary for the given {@link PreferenceItem}.<br>
         * First tries to get the choice from the {@link SharedPreferences} and passing a<br>
         * default value if SharedPreferences doesn't have the given key set.<br>
         * Take a look at the {@link PreferenceItem#choiceFromPreferences(Activity, SharedPreferences, String)}<br>
         * to see the choice resolution.
         *
         * @param activity {@link Activity} a context to get defaultSharedPreferences from see {@link #saveSelectedChoice(Activity, SharedPreferences, int)}
         * @param sharedPreferences {@link SharedPreferences} to query for the given key
         * @param preference        {@link Preference} who's summary is being updated
         * @param adminChoice       {@link String} first fallback option in case SharedPrefernces is empty
         */
        void setSummary(final Activity activity, SharedPreferences sharedPreferences, Preference preference, String adminChoice) {
            final int choice = choiceFromPreferences(activity, sharedPreferences, adminChoice);
            preference.setSummary(choice == firstChoicePreferenceIndex ? summaryFirst : summarySecond);
        }

        /**
         * Saves respective choice for the current {@link PreferenceItem#key}.<br>
         * This is done with an android recommended non-blocking call.<br>
         * Method distinguishes saving of the preference choice into the<br>
         * {@link PreferenceItem#NEW_TYPE} and {@link PreferenceItem#NEW_TYPE_IPO} into the<br>
         * {@link SDKManager#displayFirstNameFirst(boolean)}}<br>
         * Note<p>
         *     This note concerns the new contact type saving.<br>
         *     As {@link com.avaya.android.vantage.basic.fragments.ContactEditFragment} uses<br>
         *     PreferenceManager.getDefaultSharedPreferences(activity);<br>
         *     We need to adhere to the variation in preference saving.<br>
         *     As preferences are sensitive to change, recommendation is to write it as before,<br>
         *     for two release versions, after which, we can remove the .getDefaultSharedPreferences<br>
         *     and continue using only one preference storage, as the data is grouped in application.<br>
         * </p>
         *
         * @param sharedPreferences {@link SharedPreferences} to save value to
         * @param choice            int value to save to {@link SharedPreferences}
         */
        void saveSelectedChoice(final Activity activity, SharedPreferences sharedPreferences, int choice) {
            final boolean isNewTypeKey = key.equals(NEW_TYPE.key);
            if (isNewTypeKey) {
                final SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                final SharedPreferences.Editor editor = defaultPreferences.edit();
                editor.putString(key, ""+valueForChoice(choice));
                editor.apply();
            } else {
                SDKManager.getInstance().displayFirstNameFirst(choice == firstChoicePreferenceIndex);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(key, valueForChoice(choice));
            editor.putBoolean(REFRESH_FAVORITES, true);
            editor.putBoolean(REFRESH_CONTACTS, true);
            editor.putBoolean(REFRESH_RECENTS, true);
            editor.apply();
        }

        /**
         * Utility method for choice value resolution.<br>
         * An int value parameter is resolved into one of available choice values:<br>
         * - {@link PreferenceItem#firstChoicePreferenceIndex}<br>
         * - {@link PreferenceItem#secondChoicePreferenceIndex}
         *
         * @param choice choice to resolve
         * @return result is one of:<br>
         * <p>
         * - {@link PreferenceItem#firstChoicePreferenceIndex} or<br>
         * - {@link PreferenceItem#secondChoicePreferenceIndex}
         * </p>
         */
        int valueForChoice(int choice) {
            return choice == 0 ? firstChoicePreferenceIndex : secondChoicePreferenceIndex;
        }

        /**
         * Checks if {@link SharedPreferences} of this {@link PreferenceItem} contains a value<br>
         * for the corresponding key. If there is no value for the key, a default value<br>
         * is resolved by comparing admin choice equality with the "last,first" string.<br>
         * Note<p>
         *     This note concerns the new contact type saving.<br>
         *     Same as for {{@link #saveSelectedChoice(Activity, SharedPreferences, int)}} difference in<br>
         *     preferences saving location is respected as found. This is done to preserve the<br>
         *     preference saved value on application upgrade. After two consecutive application<br>
         *     version updates that have reached user devices, preferences can be merged to same location.
         * </p>
         *
         * @param sharedPreferences {@link SharedPreferences} to get value for the key
         * @param adminChoice       {@String} for use as a default choice resolution
         * @return int representing first choice index or second choice index
         */
        int choiceFromPreferences(final Activity activity, SharedPreferences sharedPreferences, String adminChoice) {
            final boolean isNewTypeKey = key.equals(NEW_TYPE.key);
            if (isNewTypeKey) {
                final SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                try {
                    return Integer.parseInt(defaultPreferences.getString(key, secondChoicePreferenceIndex + ""));
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, nfe.getMessage());
                    return secondChoicePreferenceIndex;
                }
            } else {
                return sharedPreferences.getInt(key, firstNameFirstTag.equals(adminChoice)
                        ? secondChoicePreferenceIndex : firstChoicePreferenceIndex);
            }
        }
    }
}