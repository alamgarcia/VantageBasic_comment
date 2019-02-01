package com.avaya.android.vantage.basic;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.util.SparseArray;

import com.avaya.clientservices.media.AudioFilePlayer;
import com.avaya.clientservices.media.AudioFilePlayerListener;
import com.avaya.clientservices.media.AudioTone;

import static android.media.AudioManager.STREAM_VOICE_CALL;

/**
 * Class  used to play tones
 */

public class ToneManager implements AudioFilePlayerListener {

    public static final String TAG = ToneManager.class.getSimpleName();

    private Context mContext;

    private int mSequenceNumber = 0;
    private int mCurrentPlaying = 0;


    private final static SoundPool sSoundPool;

    private static SparseArray<Integer> sResource2SoundMap = new SparseArray<>();

    static {
        SoundPool.Builder builder = new SoundPool.Builder();
        AudioAttributes.Builder b = new AudioAttributes.Builder();
        b.setLegacyStreamType(STREAM_VOICE_CALL);
        AudioAttributes audioAttributes = b.build();
        builder.setAudioAttributes(audioAttributes);
        builder.setMaxStreams(1);
        sSoundPool = builder.build();
    }

    private boolean mIsDialTone = false;

    public ToneManager(Context context) {
        mContext = context;
        if (sResource2SoundMap.size() == 0) {
            int id;
            id = sSoundPool.load(context, R.raw.reorder, 1);
            sResource2SoundMap.put(R.raw.reorder, id);

            id = sSoundPool.load(context, R.raw.intercept, 1);
            sResource2SoundMap.put(R.raw.intercept, id);

            id = sSoundPool.load(context, R.raw.busy, 1);
            sResource2SoundMap.put(R.raw.busy, id);

            id = sSoundPool.load(context, R.raw.dialtone, 1);
            sResource2SoundMap.put(R.raw.dialtone, id);

            id = sSoundPool.load(context, R.raw.ringback, 1);
            sResource2SoundMap.put(R.raw.ringback, id);

            id = sSoundPool.load(context, R.raw.dtmf0, 1);
            sResource2SoundMap.put(R.raw.dtmf0, id);

            id = sSoundPool.load(context, R.raw.dtmf1, 1);
            sResource2SoundMap.put(R.raw.dtmf1, id);

            id = sSoundPool.load(context, R.raw.dtmf2, 1);
            sResource2SoundMap.put(R.raw.dtmf2, id);

            id = sSoundPool.load(context, R.raw.dtmf3, 1);
            sResource2SoundMap.put(R.raw.dtmf3, id);

            id = sSoundPool.load(context, R.raw.dtmf4, 1);
            sResource2SoundMap.put(R.raw.dtmf4, id);

            id = sSoundPool.load(context, R.raw.dtmf5, 1);
            sResource2SoundMap.put(R.raw.dtmf5, id);

            id = sSoundPool.load(context, R.raw.dtmf6, 1);
            sResource2SoundMap.put(R.raw.dtmf6, id);

            id = sSoundPool.load(context, R.raw.dtmf7, 1);
            sResource2SoundMap.put(R.raw.dtmf7, id);

            id = sSoundPool.load(context, R.raw.dtmf8, 1);
            sResource2SoundMap.put(R.raw.dtmf8, id);

            id = sSoundPool.load(context, R.raw.dtmf9, 1);
            sResource2SoundMap.put(R.raw.dtmf9, id);

            id = sSoundPool.load(context, R.raw.dtmfstar, 1);
            sResource2SoundMap.put(R.raw.dtmfstar, id);

            id = sSoundPool.load(context, R.raw.dtmfpound, 1);
            sResource2SoundMap.put(R.raw.dtmfpound, id);

            id = sSoundPool.load(context, R.raw.dtmfpause, 1);
            sResource2SoundMap.put(R.raw.dtmfpause, id);

            id = sSoundPool.load(context, R.raw.call_failed, 1);
            sResource2SoundMap.put(R.raw.call_failed, id);

            id = sSoundPool.load(context, R.raw.callwaiting, 1);
            sResource2SoundMap.put(R.raw.callwaiting, id);
        }
    }


    /**
     * Play tone
     *
     * @param tone tone to be played
     */
    public void play(AudioTone tone) {
        mIsDialTone = false;
        switch (tone) {
            case UNDEFINED:
                break;
            case INCOMING_CALL_INTERNAL:
                break;
            case INCOMING_CALL_EXTERNAL:
                break;
            case INCOMING_CALL_INTERCOM:
                break;
            case INCOMING_CALL_PRIORITY:
                break;
            case INCOMING_CALL_AUTO_ANSWER:
                break;
            case INCOMING_AUTO_CALL_BACK:
                break;
            case REORDER:
                mSequenceNumber = 0;
                play(R.raw.reorder, true);
                break;
            case INTERCEPT:
                mSequenceNumber = 0;
                play(R.raw.intercept, true);
                break;
            case BUSY:
                mSequenceNumber = 0;
                play(R.raw.busy, true);
                break;
            case DIAL:
                play(R.raw.dialtone, true);
                mIsDialTone = true;
                break;
            case PUBLIC_DIAL:
                mSequenceNumber = 0;
                play(R.raw.dialtone, true);
                mIsDialTone = true;
                break;
            case RING_BACK:
                mSequenceNumber = 0;
                play(R.raw.ringback, true);
                break;
            case COVERAGE:
                break;
            case CALL_PICKUP_ALERT:
                break;
            case CALL_PICKUP_END_ALERT:
                break;
            case DTMF_ZERO:
                play(R.raw.dtmf0, false);
                break;
            case DTMF_ONE:
                play(R.raw.dtmf1, false);
                break;
            case DTMF_TWO:
                play(R.raw.dtmf2, false);
                break;
            case DTMF_THREE:
                play(R.raw.dtmf3, false);
                break;
            case DTMF_FOUR:
                play(R.raw.dtmf4, false);
                break;
            case DTMF_FIVE:
                play(R.raw.dtmf5, false);
                break;
            case DTMF_SIX:
                play(R.raw.dtmf6, false);
                break;
            case DTMF_SEVEN:
                play(R.raw.dtmf7, false);
                break;
            case DTMF_EIGHT:
                play(R.raw.dtmf8, false);
                break;
            case DTMF_NINE:
                play(R.raw.dtmf9, false);
                break;
            case DTMF_STAR:
                play(R.raw.dtmfstar, false);
                break;
            case DTMF_POUND:
                play(R.raw.dtmfpound, false);
                break;
            case DTMF_A:
                break;
            case DTMF_B:
                break;
            case DTMF_C:
                break;
            case DTMF_D:
                break;
            case DTMF_PAUSE:
                play(R.raw.dtmfpause, false);
                break;
            case BUTTON_CLICK_EFFECT:
                break;
            case ERROR_BEEP:
                mSequenceNumber = 0;
                play(R.raw.call_failed, true);
                break;
        }
    }

    /**
     * Play tone
     *
     * @param tone tone to be played
     */
    public int play(char tone) {
        switch (tone) {
            case '0':
                play(R.raw.dtmf0, true);
                break;
            case '1':
                play(R.raw.dtmf1, true);
                break;
            case '2':
                play(R.raw.dtmf2, true);
                break;
            case '3':
                play(R.raw.dtmf3, true);
                break;
            case '4':
                play(R.raw.dtmf4, true);
                break;
            case '5':
                play(R.raw.dtmf5, true);
                break;
            case '6':
                play(R.raw.dtmf6, true);
                break;
            case '7':
                play(R.raw.dtmf7, true);
                break;
            case '8':
                play(R.raw.dtmf8, true);
                break;
            case '9':
                play(R.raw.dtmf9, true);
                break;
            case '*':
                play(R.raw.dtmfstar, true);
                break;
            case '#':
                play(R.raw.dtmfpound, true);
                break;
        }
        mSequenceNumber++;
        return (mSequenceNumber);
    }

    /**
     * Play tone
     *
     * @param resourceId resourceID of a tone to be played
     * @param loop       should we loop the tone?
     */
    private void play(final int resourceId, boolean loop) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mCurrentPlaying = sSoundPool.play(sResource2SoundMap.get(resourceId, R.raw.errortone), 1.0f, 1.0f, 1, loop ? -1 : 0, 1.0f);
        Log.v(TAG, "playing " + resourceId);

        /// TODO: maybe use CSDK AudioFilePlayerInterface instead of MediaPlayer
    }

    /**
     * Stop playing the tone
     */
    public void stop() {
        Log.v(TAG, "stop playing ");
        sSoundPool.stop(mCurrentPlaying);
        mIsDialTone = false;
    }

    /**
     * Signal that the player has started playing.
     *
     * @param player The audio file player control interface.
     */
    @Override
    public void onAudioFileDidStartPlaying(AudioFilePlayer player) {

    }

    /**
     * Signal that the player has stopped playing.
     *
     * @param player The audio file player control interface.
     */
    @Override
    public void onAudioFileDidStopPlaying(AudioFilePlayer player) {

    }

    /**
     * running sequence number of audiotone instance
     *
     * @return
     */
    public int getSequnceNumber() {
        return mSequenceNumber;
    }

    /**
     * check if dial tone is playing
     *
     * @return true if dialtone is playing
     */
    public boolean isDialTone() {
        return mIsDialTone;
    }
}
