package com.avaya.android.vantage.basic.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.adaptors.UICallViewAdaptor;
import com.avaya.android.vantage.basic.model.UICall;

import java.util.List;


/**
 * {@link VideoCallFragment} responsible for showing video call surface and processing video call data
 */
public class VideoCallFragment extends ActiveCallFragment {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private boolean mDeferInit = false;
    ViewGroup mVideoViewGroup, mCallControls, mTopLayout;
    private Slide slide;
    private boolean inTransition=false;

    /**
     * Empty public constructor
     */
    public VideoCallFragment() {
        // Required empty public constructor
        slide = new Slide();
    }



    @Override
    public void init(UICall call, UICallViewAdaptor callViewAdaptor) {
        super.init(call, callViewAdaptor);
        if(mDeferInit) {
            Log.w(LOG_TAG, "init: performing deferred initialization");
            mCallViewAdaptor.initVideoCall(getActivity(), mCallId);
            mDeferInit = false;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (mCallViewAdaptor != null) {
            mCallViewAdaptor.initVideoCall(getActivity(), mCallId);
        }
        else {
            Log.e(LOG_TAG, "onCreateView: init was not called yet");
            mDeferInit = true;
        }

        // Inflate the layout for this fragment
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mVideoViewGroup = (ViewGroup) view.findViewById(R.id.video_layout);
        mCallControls = (ViewGroup) view.findViewById(R.id.call_controls);
        ImageView contactImage = (ImageView)view.findViewById(R.id.contact_image);
        contactImage.setVisibility(View.GONE);

        if( getResources().getBoolean(R.bool.is_landscape) == false ) {
            makeUIChangesForPortraitView(view);
        }else {
            makeUIChangesForLandscapeView(view);
        }
        return view;
    }

    private void makeUIChangesForPortraitView(View view){
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, R.id.more);
        params.addRule(RelativeLayout.RIGHT_OF, R.id.back);
        params.setMargins(0, 24, 0, 0);
        mContactName.setLayoutParams(params);
        mContactName.setTextSize(20);
        mContactName.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        mContactName.setGravity(Gravity.CENTER_HORIZONTAL);
        mCallStateView.setTextSize(20);
        mCallStateView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    }

    private void makeUIChangesForLandscapeView(View view){
        mTopLayout = (ViewGroup) view.findViewById(R.id.top_layout);
        view.findViewById(R.id.contact_name).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.call_state).setVisibility(View.INVISIBLE);
        TextView contactName = (TextView)view.findViewById(R.id.video_contact_name);
        contactName.setVisibility(View.VISIBLE);
        contactName.setText(mContactName.getText());
        mContactName = contactName;
        TextView callStateView = (TextView) view.findViewById(R.id.video_call_state);
        callStateView.setText(mCallStateView.getText());
        callStateView.setVisibility(View.VISIBLE);
        mCallStateView = callStateView;
        ViewGroup audioViewGroup = (ViewGroup) view.findViewById(R.id.audio_layout);
        audioViewGroup.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int visibility = mCallControls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
                changeVisibilityForVideoButtons(visibility);
                return false;
            }
        });

        RelativeLayout.LayoutParams paramsactiveCallRoot = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        if((MainActivity)getActivity()!=null) {
            ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(true);
            paramsactiveCallRoot.height = 700;
            activeCallRoot.setLayoutParams(paramsactiveCallRoot);
        }
    }

    private void changeVisibilityForVideoButtons(int visibility){
        mContactName.setVisibility(visibility);
        mCallStateView.setVisibility(visibility);

        addSlideAnimation(visibility);
    }

    @Override
    public void setVisible() {
        if (mCallback != null) {
            mCallback.cancelContactPicker();
        }
        if (getView() != null) {
            getView().setVisibility(View.VISIBLE);
        }
        if( getResources().getBoolean(R.bool.is_landscape) == true && (MainActivity)getActivity()!=null) {
            ((MainActivity) getActivity()).changeUiForFullScreenInLandscape(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getView() != null) {
            if (mCallViewAdaptor == null) {
             new UICallViewAdaptor().startVideo(mVideoViewGroup, mCallId);
            }
            else {
                mCallViewAdaptor.startVideo(mVideoViewGroup, mCallId);
            }
        }

        if( getResources().getBoolean(R.bool.is_landscape) == true) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    changeVisibilityForVideoButtons(View.INVISIBLE);
                }
            }, 4000);
        }
    }

    /**
     * Starts slide animation for mCallControls and mTopLayout
     * @param visibility View visibility
     */
    private void addSlideAnimation(int visibility){
        Log.d(LOG_TAG, "addSlideAnimation visibility=" + visibility);

        if (inTransition){
            Log.d(LOG_TAG, "Transition not finished. Aborting");
            return;
        }
        try {
            if (isAdded() && getResources().getBoolean(R.bool.is_landscape) == true) {

                //slide.setDuration(300);
                slide.addListener(new Transition.TransitionListener() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        inTransition=true;
                        Log.d(LOG_TAG, "onTransitionStart");
                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        inTransition=false;
                        Log.d(LOG_TAG, "onTransitionEnd");
                    }

                    @Override
                    public void onTransitionCancel(Transition transition) {

                    }

                    @Override
                    public void onTransitionPause(Transition transition) {

                    }

                    @Override
                    public void onTransitionResume(Transition transition) {

                    }
                });
                slide.setSlideEdge(Gravity.BOTTOM);
                TransitionManager.beginDelayedTransition(mCallControls, slide);
                slide.setSlideEdge(Gravity.TOP);
                TransitionManager.beginDelayedTransition(mTopLayout, slide);
                mCallControls.setVisibility(visibility);
                mTopLayout.setVisibility(visibility);
            }
        }catch (IllegalStateException e){
            e.printStackTrace();
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        if(mCallViewAdaptor == null) {
            new UICallViewAdaptor().stopVideo(mCallId);
        }
        else {
            mCallViewAdaptor.stopVideo(mCallId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(LOG_TAG, "onDestroyView()");

        if(mCallViewAdaptor == null) {
         new UICallViewAdaptor().onDestroyVideoView(mCallId);
        }
        else {
            mCallViewAdaptor.onDestroyVideoView(mCallId);
        }

        cancelFullScreenMode();
    }
}
