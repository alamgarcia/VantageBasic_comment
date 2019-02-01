package com.avaya.android.vantage.basic.csdk;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.avaya.android.vantage.basic.R;
import com.avaya.clientservices.call.Call;
import com.avaya.clientservices.call.CallCompletionHandler;
import com.avaya.clientservices.call.CallException;
import com.avaya.clientservices.call.MediaDirection;
import com.avaya.clientservices.call.VideoChannel;
import com.avaya.clientservices.call.VideoMode;
import com.avaya.clientservices.media.VideoInterface;
import com.avaya.clientservices.media.capture.VideoCamera;
import com.avaya.clientservices.media.capture.VideoCaptureCompletionHandler;
import com.avaya.clientservices.media.capture.VideoCaptureController;
import com.avaya.clientservices.media.capture.VideoCaptureException;
import com.avaya.clientservices.media.gui.Destroyable;
import com.avaya.clientservices.media.gui.PipPlane;
import com.avaya.clientservices.media.gui.PlaneViewGroup;
import com.avaya.clientservices.media.gui.VideoLayerLocal;
import com.avaya.clientservices.media.gui.VideoLayerRemote;
import com.avaya.clientservices.media.gui.VideoPlaneLocal;
import com.avaya.clientservices.media.gui.VideoPlaneRemote;
import com.avaya.clientservices.media.gui.VideoSink;
import com.avaya.clientservices.media.gui.VideoSource;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link VideoManager} is responsible for processing video calls in Vantage Basic application. It is
 * responsible for starting and stopping video call.
 */

public class VideoManager {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private final VideoCaptureController mVideoCaptureController;

    private VideoCamera mCurrentCamera;
    private final List<Destroyable> mDestroyables;
    private VideoLayerLocal mVideoLayerLocal;
    private VideoLayerRemote mVideoLayerRemote;
    private VideoPlaneLocal mVideoPlaneLocal;
    private VideoPlaneRemote mVideoPlaneRemote;
    private PlaneViewGroup mVideoPlaneViewGroup;
    private int mChannelId;
    boolean mLocalVideoActive = true;
    boolean mRemoteVideoActive = false;
    ViewGroup mVideoViewGroup=null;

    public VideoManager(PlaneViewGroup videoPlaneViewGroup){
        mVideoCaptureController = new VideoCaptureController();
        mDestroyables = new ArrayList<>();
        mVideoPlaneViewGroup  = videoPlaneViewGroup;
    }
    /**
     * Initialize {@link VideoManager} related values
     *
     * @param context
     */
    public void init(Context context) {

        /* Since there is a leak in PlaneViewGroup class, PlaneViewGroup is created only one time in CallAdaptor
         and it is reused in each video call (instead of recreating it each video call) */
        //mVideoPlaneViewGroup = new PlaneViewGroup(context);

        // create the renders
        mVideoLayerLocal = new VideoLayerLocal();
        mVideoLayerRemote = new VideoLayerRemote();

        // create the local video plane
        mVideoPlaneLocal = new VideoPlaneLocal(context);
        mVideoPlaneLocal.setLocalVideoLayer(mVideoLayerLocal);


        // create the remote video plane
        mVideoPlaneRemote = new VideoPlaneRemote(context);
        mVideoPlaneRemote.setRemoteVideoLayer(mVideoLayerRemote);

        // set the remote video plane as a child beneath the local video plane
        mVideoPlaneLocal.setPlane(mVideoPlaneRemote);

        mVideoLayerLocal.setCornerRadius(0);
        mVideoLayerLocal.setBorderWidth(1);
        mVideoLayerLocal.setMirrored(true);
        mVideoPlaneLocal.setPipAbsWidth(246);

        mVideoPlaneViewGroup.setPlane(mVideoPlaneLocal);
        mVideoPlaneViewGroup.setVisibility(View.VISIBLE);
        mVideoPlaneViewGroup.onStart();

        // maintain a list of all objects we need to destroy
        mDestroyables.add(mVideoLayerLocal);
        mDestroyables.add(mVideoLayerRemote);
    }

    /**
     * Start video for provided {@link ViewGroup}
     *
     * @param videoViewGroup
     */
    public void startVideo(ViewGroup videoViewGroup,List<VideoChannel> list) {
        onCameraSelected(mCurrentCamera);

        mVideoViewGroup=videoViewGroup;

        if (mVideoPlaneLocal == null || mVideoPlaneViewGroup == null || mVideoViewGroup == null) {
            Log.e(LOG_TAG, "one of the video views is null - this should NOT happen - returning");
            return;
        }


        if( mVideoViewGroup.getResources().getBoolean(R.bool.is_landscape) == true) {
            mVideoPlaneLocal.setPipCorner(PipPlane.Corner.NE);
            mVideoPlaneLocal.setPipAbsWidth(360);
        }else{
            mVideoPlaneLocal.setPipCorner(PipPlane.Corner.SE);
            mVideoPlaneLocal.setPipAbsWidth(246);
        }

        mVideoPlaneViewGroup.setPlane(mVideoPlaneLocal);
        mVideoPlaneViewGroup.setVisibility(ViewGroup.VISIBLE);

        if (mVideoPlaneViewGroup.getParent() == null) {
            mVideoViewGroup.addView(mVideoPlaneViewGroup, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            ViewGroup parentViewGroup = (ViewGroup) mVideoPlaneViewGroup.getParent();
            if (!parentViewGroup.equals(videoViewGroup)) {
                parentViewGroup.removeView(mVideoPlaneViewGroup);
                mVideoViewGroup.addView(mVideoPlaneViewGroup, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }

        setActiveVideoChannelId(list);
        if (mLocalVideoActive) {
            startVideoTransmission();
        }

        if (mRemoteVideoActive) {
            startVideoReception();
        }
    }

    /**
     * Starting video transmission for video call
     */
    private void startVideoTransmission() {
        Log.d(LOG_TAG, "Start video transmission. mChannelId=" + mChannelId);
        VideoInterface videoInterface = SDKManager.getInstance().getClient().getMediaEngine().getVideoInterface();
        if(videoInterface != null) {
            final VideoSink videoSink = videoInterface.getLocalVideoSink(mChannelId);
            mVideoCaptureController.getVideoSource().setVideoSink(videoSink);
            mVideoCaptureController.setLocalVideoLayer(mVideoLayerLocal);
        }
        // No need to destroy video capture and local video source as we are using a single instance of it
    }

    /**
     * Starting video reception for video call
     */
    private void startVideoReception() {
        Log.d(LOG_TAG, "Start video reception. mChannelId=" + mChannelId);
        VideoInterface videoInterface = SDKManager.getInstance().getClient().getMediaEngine().getVideoInterface();
        VideoSource videoSourceRemote = videoInterface.getRemoteVideoSource(mChannelId);
        if (videoSourceRemote != null) {
            videoSourceRemote.setVideoSink(mVideoLayerRemote);
            mDestroyables.add(videoSourceRemote);
        }
    }

    /**
     * {@link VideoCaptureCompletionHandler} use {@link VideoCamera} to set video to
     * local video plane
     *
     * @param videoCamera {@link VideoCamera}
     */
    private void onCameraSelected(final VideoCamera videoCamera) {
        Log.d(LOG_TAG, "onCameraSelected = " + videoCamera);
        mVideoCaptureController.useVideoCamera(videoCamera, new VideoCaptureCompletionHandler() {
            /**
             * Reports that the operation was successfully completed.
             */
            @Override
            public void onSuccess() {
                if (mVideoPlaneLocal != null) {
                    mVideoPlaneLocal.setLocalVideoHidden(videoCamera == null);
                }
            }

            /**
             * Reports that the operation has failed.
             *
             * @param error Information about failure details.
             */
            @Override
            public void onError(VideoCaptureException error) {
                Log.e(LOG_TAG, "onCameraSelected::getVideoCaptureController().useVideoCamera error", error);
            }

        });
    }

    /**
     * Obtain current camera
     *
     * @return {@link VideoCamera}
     */
    public VideoCamera getCurrentCamera() {
        return mCurrentCamera;
    }

    /**
     * Set the camera and return video mode for initializing video.
     *
     * @return {@link VideoCamera}
     */
    public VideoMode setupCamera() {
        // Check if device has camera
        try {
            if (mVideoCaptureController.hasVideoCamera(VideoCamera.Front)) {
                mCurrentCamera = VideoCamera.Front;
                return VideoMode.SEND_RECEIVE;
            } else if (mVideoCaptureController.hasVideoCamera(VideoCamera.Back)) {
                mCurrentCamera = VideoCamera.Back;
                return VideoMode.SEND_RECEIVE;
            }
        } catch (VideoCaptureException e) {
            Log.e(LOG_TAG, "exception in setupCamera", e);

        }
        // No cameras found
        return VideoMode.RECEIVE_ONLY;
    }

    /**
     * Set {@link VideoMode} for provided {@link Call}
     *
     * @param call
     * @param videoMode
     */
    public void setVideoMode(Call call, VideoMode videoMode) {
        if (null != call && call.getVideoMode() != videoMode) {
            call.setVideoMode(videoMode, new CallCompletionHandler() {
                @Override
                public void onSuccess() {
                    Log.d(LOG_TAG, "Video mode has been set");
                }

                @Override
                public void onError(CallException e) {
                    Log.e(LOG_TAG, "Video mode can't be set. Exception: " + e.getError());
                }
            });
        } else {
            Log.e(LOG_TAG, "SetVideoMode, active call is null");
        }
    }

    /**
     * Set video mode for {@link Call}
     *
     * @param call {@link Call}
     */
    public void setVideoMode(Call call) {
        setVideoMode(call, setupCamera());
    }

    /**
     * Accept video for provided {@link Call}
     *
     * @param call {@link Call} for which video is accepted
     */
    public void acceptVideo(Call call) {
        call.acceptVideo(setupCamera(), new CallCompletionHandler() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "Video mode has been set");
            }

            @Override
            public void onError(CallException e) {
                Log.e(LOG_TAG, "Video mode can't be set. Exception: " + e.getError());
            }
        });
    }

    /**
     * Setting active {@link VideoChannel} id
     *
     * @param list {@link VideoChannel}
     */
    private void setActiveVideoChannelId(List<VideoChannel> list) {
        if (list != null && !list.isEmpty()) {
            mChannelId = list.get(0).getChannelId();
        }
    }

    /**
     * Checking list of {@link VideoChannel} for updating channel
     *
     * @param list
     */
    public void onCallVideoChannelsUpdated(List<VideoChannel> list) {
        Log.d(LOG_TAG, "onCallVideoChannelsUpdated");
        if (!list.isEmpty()) {
            // Get video channel id
            setActiveVideoChannelId(list);
            // Get negotiated media direction
            MediaDirection mediaDirection = list.get(0).getNegotiatedDirection();
            Log.d(LOG_TAG, "onCallVideoChannelsUpdated video mode is " + mediaDirection);
            if (mVideoPlaneLocal != null) {
                if (mediaDirection == MediaDirection.INACTIVE ||  mediaDirection == MediaDirection.SEND_ONLY) {
                    Log.d(LOG_TAG, "onCallVideoChannelsUpdated set black video");
                    mVideoPlaneLocal.setPlane(null);
                }
                else {
                    mVideoPlaneLocal.setPlane(mVideoPlaneRemote);
                }
            }
            if (mediaDirection == MediaDirection.SEND_RECEIVE || mediaDirection == MediaDirection.SEND_ONLY) {
                Log.d(LOG_TAG, "onCallVideoChannelsUpdated SEND_RECEIVE/SEND_ONLY");
                mLocalVideoActive = true;
                startVideoTransmission();
            }
            if (mediaDirection == MediaDirection.SEND_RECEIVE || mediaDirection == MediaDirection.RECEIVE_ONLY) {
                Log.d(LOG_TAG, "onCallVideoChannelsUpdated SEND_RECEIVE/RECEIVE_ONLY");
                mRemoteVideoActive = true;
                startVideoReception();
            }
        } else {
            mLocalVideoActive = false;
            mRemoteVideoActive = false;
        }
    }


    /**
     * Destroying video view
     */
    public void onDestroyVideoView() {
        Log.d(LOG_TAG, "onDestroyVideoView()");

        for (Destroyable destroyable : mDestroyables) {
            destroyable.destroy();
        }

        if (mVideoPlaneViewGroup != null) {
            mVideoPlaneViewGroup.onStop();
        }
        if ((mVideoViewGroup != null) && (mVideoPlaneViewGroup != null))
            mVideoViewGroup.removeView(mVideoPlaneViewGroup);

        mChannelId=-2;
    }

    /**
     * Stop video on {@link PlaneViewGroup}
     */
    public void stopVideo() {
        mVideoPlaneViewGroup.setVisibility(ViewGroup.INVISIBLE);
        mVideoPlaneViewGroup.setPlane(null);
        VideoSource videoSource = mVideoCaptureController.getVideoSource();
        if (videoSource != null) {
            videoSource.setVideoSink(null);
        }
        mVideoCaptureController.useVideoCamera(null, null);
    }

    /**
     * Destroys {@link VideoCaptureController}
     */
    public void onDestroy () {
        mVideoCaptureController.destroy();
    }
}
