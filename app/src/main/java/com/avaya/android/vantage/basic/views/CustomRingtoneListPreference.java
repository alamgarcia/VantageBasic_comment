package com.avaya.android.vantage.basic.views;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.app.AlertDialog;
import com.avaya.android.vantage.basic.R;
import android.util.AttributeSet;
import java.util.ArrayList;
import java.util.List;


public class CustomRingtoneListPreference extends ListPreference implements Runnable {

    private RingtoneManager mRingtoneManager;
    private Handler mHandler;

    private static final int POS_UNKNOWN = -1;
    /* Position of Default Ringtone*/
    private static final int POS_DEFAULT = 0;
    /* Position of Silent (None) Ringtone.*/
    private static final int POS_SILENT = 1;

    /** The titles of the ringtones */
    private CharSequence[] mRingtonesTitles;
    /** The URIs of the ringtones */
    private CharSequence[] mRingtonesURIs;
    /* Index of current selected ringtone */
    private int mClickedDialogEntryIndex = POS_UNKNOWN;
    /* Uri of current selected ringtone */
    private Uri mCurrentValue;
    /* Uri of default ringtone */
    private Uri mDefaultValue;
    /**
     * A Ringtone for the default ringtone. In most cases, the RingtoneManager
     * will stop the previous ringtone. However, the RingtoneManager doesn't
     * manage the default ringtone for us, so we should stop this one manually.
     */
    private Ringtone mDefaultRingtone;
    /**
     * The ringtone that's currently playing, unless the currently playing one is the default
     * ringtone.
     */
    private Ringtone mCurrentRingtone;

    public CustomRingtoneListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomRingtoneListPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateDialogView() {
        mHandler = new Handler();
        mRingtoneManager = new RingtoneManager(getContext());
        mDefaultValue = getDefaultRingtoneUri();
        mCurrentValue = onRestoreRingtone();

        ListView view = new ListView(getContext());
        view.setAdapter(adapter());
        initializeEntriesAndEntryValues();

        return view;
    }

    /* Returns Default ringtone URI*/
    private Uri getDefaultRingtoneUri() {
        return Settings.System.DEFAULT_RINGTONE_URI;
    }

    /* Initializing a list of Ringtones Titles and a list of Ringtones URIs.
    * Pushing Avaya Ringtones to the top of the list.
    * Adding an option for Default Ringtone.
    * Adding an potion for Silent (None) ringtone.
    */
    private void initializeEntriesAndEntryValues() {
        List<String> ringtonesTitlesList = new ArrayList<String>();
        List<String> ringtonesURIsList = new ArrayList<String>();
        Cursor ringtoneManagerCursor = mRingtoneManager.getCursor();
        int avayaRingtonePushIndex = 0;

        if (ringtoneManagerCursor != null){
            while (ringtoneManagerCursor.moveToNext()){
                String ringtoneTitle = ringtoneManagerCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                if (ringtoneTitle.toLowerCase().contains("avaya")){
                    ringtonesTitlesList.add(avayaRingtonePushIndex,ringtoneTitle);
                    ringtonesURIsList.add(avayaRingtonePushIndex, ContentUris.withAppendedId(Uri.parse(ringtoneManagerCursor.getString(RingtoneManager.URI_COLUMN_INDEX)), ringtoneManagerCursor
                            .getLong(RingtoneManager.ID_COLUMN_INDEX)).toString());
                    avayaRingtonePushIndex++;
                }
                else{
                    ringtonesTitlesList.add(ringtoneTitle);
                    ringtonesURIsList.add(ContentUris.withAppendedId(Uri.parse(ringtoneManagerCursor.getString(RingtoneManager.URI_COLUMN_INDEX)), ringtoneManagerCursor
                            .getLong(RingtoneManager.ID_COLUMN_INDEX)).toString());
                }

            }
            ringtoneManagerCursor.close();
        }

        addSilentAndDefaultItems(ringtonesTitlesList, ringtonesURIsList);

        mRingtonesTitles = ringtonesTitlesList.toArray(new CharSequence[ringtonesTitlesList.size()]);
        mRingtonesURIs = ringtonesURIsList.toArray(new CharSequence[ringtonesURIsList.size()]);

        setEntries(mRingtonesTitles);
        setEntryValues(mRingtonesURIs);
    }
    /*
    * Adds "Default ringtone" and "None" (Silent) options to the list.
    */
    private void addSilentAndDefaultItems(List<String> ringtonesTitlesList, List<String> ringtonesURIsList) {
        ringtonesTitlesList.add(POS_DEFAULT, getContext().getString(R.string.ringtone_default));
        ringtonesURIsList.add(POS_DEFAULT, mDefaultValue.toString());

        ringtonesTitlesList.add(POS_SILENT, getContext().getString(R.string.ringtone_silent));
        ringtonesURIsList.add(POS_SILENT, "");


    }

    private ListAdapter adapter() {
        return new ArrayAdapter(getContext(), android.R.layout.select_dialog_singlechoice);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        if (mRingtonesTitles == null || mRingtonesURIs == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        mClickedDialogEntryIndex = getCurrentRingtoneIndex(mCurrentValue);
        builder.setSingleChoiceItems(mRingtonesTitles, mClickedDialogEntryIndex,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mClickedDialogEntryIndex = which;
                        playRingtone();
                    }
                });

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Uri selectedRingtoneUri = mRingtonesURIs[mClickedDialogEntryIndex] != null ? Uri.parse(mRingtonesURIs[mClickedDialogEntryIndex].toString()) : null;
                onSaveRingtone(selectedRingtoneUri);
                callChangeListener(selectedRingtoneUri != null ? selectedRingtoneUri.toString() : "");
            }
        });
    }

    /* Returns index of selected ringtone */
    private int getCurrentRingtoneIndex(Uri mCurrentValue) {
        int currentRingtoneIndex = POS_UNKNOWN;
        if(currentRingtoneIndex == POS_UNKNOWN && RingtoneManager.isDefault(mCurrentValue)){
            currentRingtoneIndex = POS_DEFAULT;
        }
        if (currentRingtoneIndex == POS_UNKNOWN && mCurrentValue == null){
            currentRingtoneIndex = POS_SILENT;
        }
        if (currentRingtoneIndex == POS_UNKNOWN){
            currentRingtoneIndex = findIndexOfValue(mCurrentValue.toString());
        }
        if (currentRingtoneIndex == POS_UNKNOWN){
            currentRingtoneIndex = POS_DEFAULT;
        }
        return currentRingtoneIndex;
    }

    /* Saving URI of selected ringtone to Shared Preferences*/
    private void onSaveRingtone(Uri selectedRingtoneUri) {
        persistString(selectedRingtoneUri != null ? selectedRingtoneUri.toString() : "");
    }

    private void playRingtone() {
        mHandler.removeCallbacks(this);
        mHandler.postDelayed(this, 0);
    }

    public void run() {
        stopAnyPlayingRingtone();
        if (mClickedDialogEntryIndex == POS_SILENT) {
            return;
        }

        Ringtone ringtone;
        if (mClickedDialogEntryIndex == POS_DEFAULT) {
            if (mDefaultRingtone == null) {
                mDefaultRingtone = RingtoneManager.getRingtone(getContext(), mDefaultValue);
            }
           /*
            * Stream type of mDefaultRingtone is not set explicitly here.
            * It should be set in accordance with mRingtoneManager of this Activity.
            */
            if (mDefaultRingtone != null) {
                mDefaultRingtone.setStreamType(mRingtoneManager.inferStreamType());
            }
            ringtone = mDefaultRingtone;
            mCurrentRingtone = null;
        } else {
            ringtone = mRingtoneManager.getRingtone(getContext(), Uri.parse(mRingtonesURIs[mClickedDialogEntryIndex].toString()));
            mCurrentRingtone = ringtone;
        }
        ringtone.play();
    }

    /* Stopping any playing ringtone */
    private void stopAnyPlayingRingtone() {
        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            mDefaultRingtone.stop();
        }

        if (mCurrentRingtone != null && mCurrentRingtone.isPlaying()) {
            mCurrentRingtone.stop();
        }

        if (mRingtoneManager != null) {
            mRingtoneManager.stopPreviousRingtone();
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        /* Prevent from breaking fullscreen*/
        ((Activity)getContext()).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        AlertDialog dialog = (AlertDialog)getDialog();
        ListView listView = dialog.getListView();
        /* Disabling scrollbar fading to make sure it is more clear that the list is scrollable */
        listView.setScrollbarFadingEnabled(false);
    }


    /**
     * Called when the chooser is about to be shown and the current ringtone
     * should be marked. Can return null to not mark any ringtone.
     * <p>
     * By default, this restores the previous ringtone URI from the persistent
     * storage.
     *
     * @return The ringtone to be marked as the current ringtone.
     */
    protected Uri onRestoreRingtone() {
        final String uriString = getPersistedString(null);
        return !TextUtils.isEmpty(uriString) ? Uri.parse(uriString) : null;
    }

    /* Stopping any playing ringtone on dialog dismiss*/
    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        stopAnyPlayingRingtone();
    }
}

