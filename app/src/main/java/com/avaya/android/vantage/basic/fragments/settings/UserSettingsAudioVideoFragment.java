package com.avaya.android.vantage.basic.fragments.settings;

import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.csdk.ConfigParametersNames;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.views.CustomRingtoneListPreference;

import static com.avaya.android.vantage.basic.Constants.CUSTOM_RINGTONE_PREFERENCES;

/**
 * Class used for audio / video settings. User can choose ringtone in this fragment
 */
public class UserSettingsAudioVideoFragment extends PreferenceFragment {
    private static final String TAG = "SettingsAudioVideo";
    private static final String UNKNOWN_RINGTONE = "Unknown ringtone";
    private static final int POS_UNKNOWN = -1;
    CustomRingtoneListPreference mPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting_audio_video);
        mPreference = (CustomRingtoneListPreference) findPreference(CUSTOM_RINGTONE_PREFERENCES);

        mPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateSummary();
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummary();
    }

    /**
     * Updates Summary to the {@link CustomRingtoneListPreference}
     */
    private void updateSummary() {
        String title;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        String adminChoiceRingtone = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.ADMIN_CHOICE_RINGTONE);

        // if user already set ringtone, show his settings
        if (prefs.contains(CUSTOM_RINGTONE_PREFERENCES)) {
            getRingtoneName();
        } else {
            // if no ringtone is selected and admin has set a ringtone, display that ringtone
            if (adminChoiceRingtone != null && adminChoiceRingtone.trim().length() > 0) {
                mPreference.setSummary(adminChoiceRingtone);
            } else {
                // if user and admin did not set any ringtone, just display what is default ringtone
                Uri ringtoneUri = Uri.parse("content://settings/system/ringtone");
                Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), ringtoneUri);
                if (ringtone != null) {
                    title = ringtone.getTitle(getActivity());
                    mPreference.setSummary(title);
                }
            }
        }
    }

    /**
     * This method gets ringtone name and adds subtitle to the "Choose ringtone" audio preference
     */
    public void getRingtoneName() {
        String title = UNKNOWN_RINGTONE;
        if (getActivity() != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
            String strRingtonePreference = prefs.getString(CUSTOM_RINGTONE_PREFERENCES, getString(R.string.ringtone_default));
            Uri ringtoneUri = Uri.parse(strRingtonePreference);

            // checking if the ringtone actually exists. If not, using the Default Ringtone.
            RingtoneManager rm = new RingtoneManager(getContext());
            int ringtonePosition = rm.getRingtonePosition(ringtoneUri);
            if (ringtonePosition != POS_UNKNOWN) {
                Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), ringtoneUri);
                if (ringtone != null) {
                    title = ringtone.getTitle(getActivity());
                }
            } else {
                if (ringtoneUri.toString().equals("")){
                    title = getString(R.string.ringtone_silent);
                }
                else{
                    title = getString(R.string.ringtone_default);
                }
            }
            mPreference.setSummary(title);
        } else {
            mPreference.setSummary(getString(R.string.ringtone_default));
            Log.d(TAG, "Activity is null, setting ringtone title to Default");
        }
    }
}
