package com.avaya.android.vantage.basic.fragments.settings;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;

/**
 * Fragment used for user preferences. In this fragment user can choose desired settings group.
 */

public class UserPreferencesFragment extends PreferenceFragment {
    public SettingsItemSelected mListener;

    /**
     * Interface used for callback
     */
    public interface SettingsItemSelected {
        void fragmentSwitcher(String fragmentKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getContext());

        PreferenceScreen applicationCategory = getPreferenceManager().createPreferenceScreen(getContext());
        applicationCategory.setTitle(getString(R.string.application));
        applicationCategory.setKey("application");

        screen.addPreference(applicationCategory);

        PreferenceScreen audioVideoCategory = getPreferenceManager().createPreferenceScreen(getContext());
        if (Utils.isCameraSupported()) {
            audioVideoCategory.setTitle(getString(R.string.audio_video));
        } else {
            audioVideoCategory.setTitle(getString(R.string.audio_preferences));
        }
        audioVideoCategory.setKey("audioVideo");

        screen.addPreference(audioVideoCategory);

        setPreferenceScreen(screen);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (SettingsItemSelected) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SettingsItemSelected");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        switch (preference.getKey()) {
            case "application": // starting application settings fragment
                mListener.fragmentSwitcher(preference.getKey());
                break;
            case "audioVideo": // starting audio/video settings fragment
                mListener.fragmentSwitcher(preference.getKey());
                break;
            default:
                // do nothing
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
