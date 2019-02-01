package com.avaya.android.vantage.basic.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.RingerService;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.adaptors.UICallViewAdaptor;
import com.avaya.android.vantage.basic.csdk.LocalContactInfo;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.model.UICall;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.POWER_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 */
public class IncomingCallFragment extends DialogFragment {

    protected static final String TAG = "IncomingCallFragment";
    private static final int DIALOG_POSITION = 64;
    View mView;
    //Context mContext;
    UICallViewAdaptor mCallViewAdaptor = null;
    Map<Integer, View> map = new HashMap<Integer, View>();
    private IncomingCallInteraction mCallBack;
    private PowerManager.WakeLock mScreenLock;
    private boolean mIsDismissed=false;

    public IncomingCallFragment() {
    }

    public void init(UICallViewAdaptor callViewAdaptor) {
        //mContext=context;
        mCallViewAdaptor=callViewAdaptor;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()){
            @Override
            public void onBackPressed() {
                //do nothing if back was pressed during incoming call
                return;
            }
        };
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.incoming_call_list_layout, container, false);

        mCallBack = (IncomingCallInteraction) getActivity();

        if ( getDialog().getWindow() != null ) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            getDialog().setCanceledOnTouchOutside(false);
            WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.y = DIALOG_POSITION;
            getDialog().getWindow().setAttributes(params);
        }
        mCallBack.onIncomingCallStarted();

        PowerManager pm = ((PowerManager) getContext().getSystemService(POWER_SERVICE));
        if (!pm.isInteractive()) {
            mScreenLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
            mScreenLock.acquire();
        }

        setupFullscreen();

        return mView;
    }

    public boolean isDismissed() {
       return mIsDismissed;
    }

    /**
     * Sets IncomingCallView child elements with the values form the current call:
     * name, number, contact image
     * @param view View Incoming Call View object
     * @param call Current UICall object
     */
    protected void setParameters(View view, UICall call){

        TextView incomingName = (TextView) view.findViewById(R.id.incoming_dialog_name);
        TextView incomingNumber = (TextView) view.findViewById(R.id.incoming_dialog_number);

        String contactName = Utils.getContactName(call.getRemoteNumber(), mCallViewAdaptor.getRemoteDisplayName(call.getCallId()), mCallViewAdaptor.isCMConferenceCall(call.getCallId()));
        if (incomingName != null)
            incomingName.setText(contactName);
        if (incomingNumber !=null)
            incomingNumber.setText(call.getRemoteNumber());

        boolean isVideo =  call.isVideo() && SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled();
        setListeners(view, call.getCallId(), isVideo);

        setContactImage(view, call.getRemoteNumber(),call.getRemoteDisplayName());
    }

    /**
     * Implements on click listener for the accept and reject buttons of the
     * Incoming Call View
     * @param view Incoming Call View
     * @param callId ID of the current call
     * @param isVideo true if this is a video call
     */
    protected void setListeners( View view, final int callId, boolean isVideo) {
        View reject = view.findViewById(R.id.reject_call);
        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Dismiss activity");
                mCallViewAdaptor.denyCall(callId);
                removeCall(callId);
            }
        });
        View accept = view.findViewById(R.id.accept_audio);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Activate another call");
                mCallViewAdaptor.acceptCall(callId, false);
                removeCall(callId);
            }
        });

        if (isVideo) {
            View acceptVideo = view.findViewById(R.id.accept_video);
            acceptVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i(TAG, "Activate another call");
                    mCallViewAdaptor.acceptCall(callId, true);
                    removeCall(callId);
                }
            });
        }
    }

    /**
     * Accepts the specified Call and removes it from the list of incoming calls
     * @param callId ID of the call
     */
    public void acceptAudioCall(int callId){
        if (map.get(callId) != null) {
            mCallViewAdaptor.acceptCall(callId, false);
            removeCall(callId);
        }
    }

    /**
     * Removes the specified call from the map of the incoming calls,
     * Stops playing the ringtone
     * @param callId id of the Call to be removed
     */
    synchronized public void removeCall(int callId){

        Log.d(TAG, "remove call " + callId);
        View view = map.get(callId);
        if (view ==null) {
            return;
        }
        view.setVisibility(View.GONE);
        map.remove(callId);
        if (map.isEmpty()) {
            if (mView != null) {
                mIsDismissed = true;
                dismissAllowingStateLoss();
            }
            stopPlayRingtone();
        }

        FragmentManager fragmentManager = getFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        if(fragments != null){
            for(Fragment fragment : fragments){
                if(fragment != null && fragment.isVisible() && fragment instanceof VideoCallFragment ) {
                    if((MainActivity)getActivity()!=null) {
                        ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(true);
                    }
                 }
            }
        }

    }

    /**
     * If the specified call is still valid and actual incoming call,
     * it'll be assigned the IncomingCallView and be added in to the map
     * of incoming calls.
     *
     * @param call {@link UICall} object of the new incoming call.
     */
    synchronized public void addCall(UICall call){
        Log.d(TAG, "add call " + call.getCallId());
        if (map.get(call.getCallId()) != null) {
            Log.i(TAG, "Call already exists");
            return;
        }

        // prevent addition of the call that was ended meanwhile
        if (SDKManager.getInstance().getCallAdaptor().getCall(call.getCallId()) == null) {
            Log.i(TAG, "Call already ended");
            return;
        }

        boolean isVideo =  call.isVideo() && SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled();
        View view = getIncomingView(isVideo);

        if (map.isEmpty()){
            startPlayRingtone();
        }
        view.setVisibility(View.VISIBLE);
        setParameters(view, call);
        map.put(call.getCallId(), view);
    }

    /**
     * The Fragment can accommodate up to two incoming call views. This method
     * returns the first or the second View object (or their variation
     * if this is a video call) depending on the current status of incoming calls.
     * @param isVideo true if this is a video call
     * @return View object that for the incoming call dialog
     */
    protected View getIncomingView(boolean isVideo){

        View incomingVideo2 = mView.findViewById(R.id.incoming_video2);
        View incomingAudio2 = mView.findViewById(R.id.incoming2);

        if (map.isEmpty() ||
                (!map.isEmpty() && (incomingVideo2.getVisibility() == View.VISIBLE) || incomingAudio2.getVisibility() == View.VISIBLE)){
            if (isVideo)
                return mView.findViewById(R.id.incoming_video1);
            else
                return mView.findViewById(R.id.incoming1);
        }
        else {
            if (isVideo)
                return incomingVideo2;
            else
                return incomingAudio2;
        }

    }



    /**
     * Displaying contact photo thumbnail
     */
    private void setContactImage(View view, String number, String name) {
        ImageView incomingImage = (ImageView) view.findViewById(R.id.incoming_dialog_image);
        String[] searchResult = LocalContactInfo.phoneNumberSearch(number);
        if (getActivity() == null) {
            //fragment is not attched to activity
            Log.d(TAG, "setContactImage:  Fragment not attched to Activity");
            return;
        }
        if(name.isEmpty()) {
            incomingImage.setContentDescription(number + getString(R.string.is_calling_content_description));
        } else {
            incomingImage.setContentDescription(name + getString(R.string.is_calling_content_description));
        }
        if (searchResult != null && searchResult.length > 2 && searchResult[3] != null && searchResult[3].trim().length() > 0){
            Uri photoURI = Uri.parse(searchResult[3]);
                try {
                    Bitmap uriImage = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), photoURI);
                    RoundedBitmapDrawable contactThumbnail =
                            RoundedBitmapDrawableFactory.create(getResources(), uriImage);
                    contactThumbnail.setCircular(true);
                    incomingImage.setBackground(contactThumbnail);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        } else {
                incomingImage.setImageResource(R.drawable.ic_avatar_generic);
        }
    }

    /**
     * Change screen params to fullscreen preferences.
     */
    private void setupFullscreen() {
        if (mView != null) {
            mView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    /**
     * Stops playing the ringtone
     */
    public void stopPlayRingtone(){
        if (getContext() == null)
            return;
        getContext().stopService(new Intent(getContext(), RingerService.class));
    }

    /**
     * Starts playing the ringtone
     */
    private void startPlayRingtone(){
        // loading admin ringtone settings
        if (getContext() == null)
            return;
        getContext().startService(new Intent(getContext(), RingerService.class));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(mScreenLock != null && mScreenLock.isHeld()) {
            mScreenLock.release();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (!map.isEmpty()) {
            Log.d(TAG, "onDetach map is NOT empty");
            stopPlayRingtone();
        }
    }

    /**
     * Updates the corresponding Incoming call View with the remote contact name.
     * @param call {@link UICall} reference
     * @param newDisplayName String. Name of teh remote contact.
     */
    public void setNewRemoteName(UICall call, final String newDisplayName) {

        if (call == null)
            return;

        View view = map.get(call.getCallId());
        if (view == null)
            return;

        final TextView incomingName = (TextView) view.findViewById(R.id.incoming_dialog_name);
        if (incomingName != null) {
            String contactName = Utils.getContactName(call.getRemoteNumber(), newDisplayName, mCallViewAdaptor.isCMConferenceCall(call.getCallId()));
            if (contactName != null) {
                incomingName.setText(contactName);
            }
        }

        TextView incomingNumber = (TextView) view.findViewById(R.id.incoming_dialog_number);
        if (incomingNumber != null) {
            incomingNumber.setText(call.getRemoteNumber());
        }

        setContactImage(view, call.getRemoteNumber(), newDisplayName);
    }

    public interface IncomingCallInteraction {
        void onIncomingCallEnded();
        void onIncomingCallStarted();
    }
}
