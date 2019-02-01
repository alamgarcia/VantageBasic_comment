package com.avaya.android.vantage.basic.csdk;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.media.AudioManager.AUDIOFOCUS_LOSS;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;

/**
 * Manager responsible for releasing and requesting audio focus for calls
 */
public class AudioFocusManager {

    private static final String TAG = "AudioFocusManager";

    private static AudioFocusManager sInstance;
    //TODO We need to remove this Context as it is memory leak
    private final Context mContext;


    public static AudioFocusManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AudioFocusManager.class) {
                if (sInstance == null) {
                    sInstance = new AudioFocusManager(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Public constructor for {@link AudioFocusManager}
     * @param context {@link Context}
     */
    private AudioFocusManager(Context context) {
        mContext = context;
    }


    private final AudioManager.OnAudioFocusChangeListener sAudioFocusChangeListener
            = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
                case AUDIOFOCUS_LOSS_TRANSIENT:
                case AUDIOFOCUS_LOSS:
                    releaseAudioFocus(0);
                    break;

                case AUDIOFOCUS_GAIN:
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Release audio focus from {@link AudioManager} in case we don't have any active call for which
     * we would need audio focus.
     *
     * @param numOfCalls total number of active calls
     */
    public void releaseAudioFocus(int numOfCalls) {
        if (numOfCalls == 0) {
            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            audioManager.abandonAudioFocus(sAudioFocusChangeListener);
        } else {
            Log.d(TAG, "releaseAudioFocus: Focus is not released. Existing call");
        }
    }

    /**
     * Request audio focus from {@link AudioManager} based on steam type we have
     *
     * @param streamType stream type from call for which we are requesting audio focus
     */
    private void requestAudioFocus(int streamType) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(sAudioFocusChangeListener, streamType, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
    }


    /**
     * Based on number of calls and stream type we are requesting audio focus
     *
     * @param streamType int which represent stream type for call
     * @param numOfCalls int representing number of calls
     */
    public void requestAudioFocusIfNeeded(int streamType, int numOfCalls) {
        if (numOfCalls == 1) {
            requestAudioFocus(streamType);
        } else if (numOfCalls > 1) {
            Log.d(TAG, "requestAudioFocus: we should already have focus");
        } else {
            Log.d(TAG, "requestAudioFocus: we don't need focus");
        }
    }

}
