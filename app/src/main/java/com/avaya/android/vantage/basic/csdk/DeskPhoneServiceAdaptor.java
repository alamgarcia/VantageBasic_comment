package com.avaya.android.vantage.basic.csdk;

import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.avaya.android.vantage.basic.BuildConfig;
import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.SystemPropertiesProxy;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.model.UIAudioDevice;
import com.avaya.android.vantage.basic.notifications.CallNotificationFactory;
import com.avaya.clientservices.call.CallService;
import com.avaya.clientservices.client.Client;
import com.avaya.clientservices.client.ClientConfiguration;
import com.avaya.clientservices.client.UserCreatedException;
import com.avaya.clientservices.common.ConnectionPolicy;
import com.avaya.clientservices.common.MessageBodyPart;
import com.avaya.clientservices.common.RegistrationGroup;
import com.avaya.clientservices.common.ServerInfo;
import com.avaya.clientservices.common.SignalingServer;
import com.avaya.clientservices.common.SignalingServerGroup;
import com.avaya.clientservices.credentials.Challenge;
import com.avaya.clientservices.credentials.CredentialCompletionHandler;
import com.avaya.clientservices.credentials.CredentialProvider;
import com.avaya.clientservices.credentials.UserCredential;
import com.avaya.clientservices.dialingrules.DialingRulesConfiguration;
import com.avaya.clientservices.media.AudioInterface;
import com.avaya.clientservices.media.MediaServicesInstance;
import com.avaya.clientservices.media.OpusCodecProfileMode;
import com.avaya.clientservices.media.VoIPConfigurationAudio;
import com.avaya.clientservices.media.VoIPConfigurationVideo;
import com.avaya.clientservices.presence.PresenceConfiguration;
import com.avaya.clientservices.provider.acs.ACSConfiguration;
import com.avaya.clientservices.provider.conference.ConferenceConfiguration;
import com.avaya.clientservices.provider.ec500.EC500Configuration;
import com.avaya.clientservices.provider.media.MediaConfiguration;
import com.avaya.clientservices.provider.ppm.PPMConfiguration;
import com.avaya.clientservices.provider.sip.MobilityMode;
import com.avaya.clientservices.provider.sip.SIPClientConfiguration;
import com.avaya.clientservices.provider.sip.SIPUserConfiguration;
import com.avaya.clientservices.settingsfile.SettingsFileParser;
import com.avaya.clientservices.user.LocalContactConfiguration;
import com.avaya.clientservices.user.MediaTransportPreference;
import com.avaya.clientservices.user.OutboundSubscriptionConfiguration;
import com.avaya.clientservices.user.RegistrationException;
import com.avaya.clientservices.user.User;
import com.avaya.clientservices.user.UserConfiguration;
import com.avaya.clientservices.user.UserRegistrationListener;
import com.avaya.clientservices.user.VideoUserConfiguration;
import com.avaya.clientservices.voicemessaging.VoiceMessagingService;
import com.avaya.deskphoneservices.CompletionHandler;
import com.avaya.deskphoneservices.DeskPhoneEventListener;
import com.avaya.deskphoneservices.DeskPhoneService;
import com.avaya.deskphoneservices.DeskPhoneServiceLibrary;
import com.avaya.deskphoneservices.HandsetType;
import com.avaya.deskphoneservices.HardButtonType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.avaya.android.vantage.basic.Constants.ANONYMOUS_USER;
import static com.avaya.android.vantage.basic.Constants.CONFIG_CHANGED;
import static com.avaya.android.vantage.basic.Constants.LOGIN_SIGNAL;
import static com.avaya.android.vantage.basic.Constants.USER_AGENT_INSTANCE_ID;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ACSENABLED;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ACSPORT;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ACSSECURE;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ACSSRVR;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.APPLY_DIALING_RULES_TO_PLUS_NUMBERS;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.AUTO_APPLY_ARS_TO_SHORT_NUMBERS;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.CONFERENCE_FACTORY_URI;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.DIALPLANAREACODE;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.DIALPLANEXTENSIONLENGTHLIST;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.DIALPLANLOCALCALLPREFIX;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.DIALPLANNATIONALPHONENUMLENGTHLIST;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.DIAL_PLAN_PBX_PREFIX;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.DSCPAUD;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.DSCPSIG;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.DSCPVID;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.EC500ENABLED;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_IPOFFICE;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_OPUS;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_PPM_CALL_JOURNALING;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENABLE_VIDEO;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENCRYPT_SRTCP;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.ENHDIALSTAT;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.EXTENSION_NAME_DISPLAY_OPTIONS;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.HUNDRED_REL_SUPPORT;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.LOGLOCAL;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.LOG_VERBOSITY;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.MEDIAENCRYPTION;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PHNCC;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PHNDPLENGTH;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PHNIC;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PHNLD;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PHNLDLENGTH;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PHNOL;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PHN_PBX_MAIN_PREFIX;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PHN_REMOVE_AREA_CODE;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.POUND_KEY_AS_CALL_TRIGGER;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.PSTN_VM_NUM;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.REGISTERWAIT;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.RTP_PORT_LOW;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.RTP_PORT_RANGE;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SIMULTANEOUS_REGISTRATIONS;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SIPDOMAIN;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SIPHA1;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SIPPASSWORD;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SIPUSERNAME;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SIP_CONTROLLER_LIST;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SIP_USER_DISPLAY_NAME;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SP_AC;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SSOPASSWORD;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SSOUSERID;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SUBSCRIBE_LIST_NON_AVAYA;
import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.VIDEO_MAX_BANDWIDTH_ANY_NETWORK;
import static com.avaya.android.vantage.basic.csdk.ErrorManager.EMPTY_CREDENTIALS_ERROR;
import static com.avaya.android.vantage.basic.csdk.UserManager.UserState.HAVE_USER;
import static com.avaya.android.vantage.basic.csdk.UserManager.UserState.NO_USER;
import static com.avaya.android.vantage.basic.csdk.UserManager.UserState.SHUTTING_DOWN;

/**
 * {@link DeskPhoneServiceAdaptor} is used in {@link SDKManager} for setting up client and
 * configuration
 */
public class DeskPhoneServiceAdaptor implements UserRegistrationListener, CredentialProvider, DeskPhoneEventListener {

    private static final String LOG_TAG = DeskPhoneServiceAdaptor.class.getSimpleName();
    private static final int INITIAL_RECONNECT_INTERVAL = 10;
    private static final int DEFAULT_ACS_PORT = 7777;

    private WeakReference<DeskPhoneServiceListener> mUiObj;

    private DeskPhoneService deskPhoneService;
    private Application mApplication;
    private Context mContext;
    private Client mClient;
    private UserConfiguration mUserConfiguration;
    private boolean mIsAtLeastOneSuccessfulRegistration = false;
    private UserManager mUserManager = new UserManager();

    private HashMap<String, String> mConfig = new HashMap<>();
    private HashMap<String, String> mPreviousConfig = new HashMap<>();
    private HashMap<String, String> mCredentials = new HashMap<>();
    private String mLastUserName;
    private SettingsFileParser mConfigPasrser, mCredentialParser;
    private String mCredentialUserName="anonymous";
    private String mCredentialPassword;
    private String mCredentialSha1;
    private ClientConfiguration mClientConfiguration;
    private boolean mLogoutInProgress = false;
    private CallNotificationFactory mNotifFactory;

    private static final HashMap<String, Client.LogLevel> mapLogLocalToCsdkLoglevel;
    static  {
        mapLogLocalToCsdkLoglevel = new HashMap<>();
        mapLogLocalToCsdkLoglevel.put("0", null);
        mapLogLocalToCsdkLoglevel.put("1", Client.LogLevel.ERROR);
        mapLogLocalToCsdkLoglevel.put("2", Client.LogLevel.ERROR);
        mapLogLocalToCsdkLoglevel.put("3", Client.LogLevel.ERROR);
        mapLogLocalToCsdkLoglevel.put("4", Client.LogLevel.ERROR);
        mapLogLocalToCsdkLoglevel.put("5", Client.LogLevel.ERROR);
        mapLogLocalToCsdkLoglevel.put("6", Client.LogLevel.WARNING);
        mapLogLocalToCsdkLoglevel.put("7", Client.LogLevel.INFO);
        mapLogLocalToCsdkLoglevel.put("8", Client.LogLevel.DEBUG);
    }

    /**
     * {@link DeskPhoneServiceAdaptor} public constructor
     * @param application {@link Application}
     */
    public DeskPhoneServiceAdaptor(Application application) {

        mApplication = application;
        mContext = application.getApplicationContext();
        mNotifFactory = new CallNotificationFactory(mContext);
        mConfigPasrser = new SettingsFileParser() {
            @Override
            protected String getVariable(String name) {
                Log.d(LOG_TAG, "getVariable");
                return "0";
            }

            @Override
            protected void setVariable(String name, String value) {
                mConfig.put(name, value);
            }

            @Override
            protected void onAdditionalFileRequested(String path) {
                Log.d(LOG_TAG, "onAdditionalFileRequested");

            }
        };

        mCredentialParser = new SettingsFileParser() {
            @Override
            public String getVariable(String name) {
                return null;
            }

            @Override
            protected void setVariable(String name, String value) {
                mCredentials.put(name, value);
            }

            @Override
            protected void onAdditionalFileRequested(String path) {
                Log.d(LOG_TAG, "onAdditionalFileRequested");

            }
        };

    }

    /**
     * Registering {@link DeskPhoneServiceListener}
     *
     * @param uiObj {@link DeskPhoneServiceListener}
     */
    public void registerListener(DeskPhoneServiceListener uiObj) {

        mUiObj = new WeakReference<>(uiObj);

        setNameExtensionVisibility();

    }

    /**
     * Notifies {@link DeskPhoneServiceListener} of the current configuration
     * for displaying name and extension
     */
    private void setNameExtensionVisibility(){
        if (mUiObj != null && mUiObj.get() != null){
            int extensionNameDisplayConfig = 0;
            try {
                extensionNameDisplayConfig = Integer.valueOf(getParam(EXTENSION_NAME_DISPLAY_OPTIONS));
                mUiObj.get().setNameExtensionVisibility(extensionNameDisplayConfig);
            }catch (NumberFormatException | NullPointerException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Set {@link ClientConfiguration}
     *
     * @return {@link ClientConfiguration}
     */
    public ClientConfiguration setupClientConfiguration() {
        // Create client configuration

        Log.d(LOG_TAG, "setupClientConfiguration");

        ClientConfiguration clientConfiguration = new ClientConfiguration(mApplication.getString(mApplication.getApplicationInfo().labelRes),
                Build.PRODUCT, Build.VERSION.RELEASE, "Android", "6.0", "1.0", "Avaya");

        // Set user agent name
        String agent = BuildConfig.USER_AGENT_NAME + '/' + BuildConfig.AVAYA_VERSION + " (" +
                BuildConfig.AVAYA_BUILD_NUMBER + "; " + SystemPropertiesProxy.get(Constants.AVAYA_PRODUCT_MODEL, Build.MODEL) +"; " + BuildConfig.CSDK_VERSION + ')';
        String debugAgent = getParamValue("IPO_DEBUG");
        clientConfiguration.setUserAgentName(TextUtils.isEmpty(debugAgent) ? agent : debugAgent);
        clientConfiguration.setUserAgentInstanceId(getUserAgentInstanceID().toString());
        // Set media configuration
        final MediaConfiguration mediaConfiguration = new MediaConfiguration();
        VoIPConfigurationAudio ac = new VoIPConfigurationAudio();

        /* According to 174409-150, value of DSCPAUD shall be taken by CSDK from PPM */
        /* So currently the parameter is taken from the platform only for IPO */
        if (getConfigBooleanParam(ENABLE_IPOFFICE) && getParam(DSCPAUD) != null) {
            try {
                ac.setDscpAudio(Integer.parseInt(getParam(DSCPAUD)));
            } catch (NumberFormatException e) {
                Log.d(LOG_TAG, "Value of DSCPAUD parameter is illegal. Invalid int:" + getParam(DSCPAUD));
            }
        }


        if (getParam(ENABLE_OPUS) != null) {
            try {
                OpusValue opusValue = OpusValue.getOpusValue(Integer.parseInt(getParam(ENABLE_OPUS)));
                OpusCodecProfileMode opusMode = opusValue.getOpusMode();
                Log.d(LOG_TAG, "Setting ENABLE_OPUS to " + opusMode);
                ac.setOpusMode(opusMode);
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "Illegal configuration for ENABLE_OPUS: " + e.getMessage());
                Log.d(LOG_TAG, "ENABLE_OPUS configuration remains unchanged");
            }
        }

        if (getParam(RTP_PORT_LOW) != null) {
            try {
                int portLow = Integer.parseInt(getParam(RTP_PORT_LOW));
                Log.d(LOG_TAG, "portLow = " + portLow);
                ac.setMinPortRange(portLow);
                if (getParam(RTP_PORT_RANGE) != null) {
                    ac.setMaxPortRange(portLow + Integer.parseInt(getParam(RTP_PORT_RANGE)));
                    Log.d(LOG_TAG, "Max port = " + portLow + Integer.parseInt(getParam(RTP_PORT_RANGE)));
                }
            } catch (NumberFormatException e) {
                Log.d(LOG_TAG, "Value one of RTP_PORT_LOW or RTP_PORT_RANGE parameters is illegal. Shall be integer");
            }
        }


        mediaConfiguration.setVoIPConfigurationAudio(ac);

        VoIPConfigurationVideo vc = new VoIPConfigurationVideo();
        /* According to 174409-150, value of DSCPVID shall be taken by CSDK from PPM */
        /* So currently the parameter is taken from the platform only for IPO        */
        if (getConfigBooleanParam(ENABLE_IPOFFICE) && getParam(DSCPVID) != null) {
            try {
                vc.setDscpVideo(Integer.parseInt(getParam(DSCPVID)));
            } catch (NumberFormatException e) {
                Log.d(LOG_TAG, "Value of DSCPVID parameter is illegal. Invalid int:" + getParam(DSCPVID));
            }
        }

        if (getParam(RTP_PORT_LOW) != null) {
            try {
                int portLow = Integer.parseInt(getParam(RTP_PORT_LOW));
                vc.setMinPortRange(portLow);
                if (getParam(RTP_PORT_RANGE) != null) {
                    vc.setMaxPortRange(portLow + Integer.parseInt(getParam(RTP_PORT_RANGE)));
                }
            } catch (NumberFormatException e) {
                Log.d(LOG_TAG, "Value one of RTP_PORT_LOW or RTP_PORT_RANGE parameters is illegal. Shall be integer");
            }
        }
        if (getParam(VIDEO_MAX_BANDWIDTH_ANY_NETWORK) != null) {
            try {
                vc.setAnyNetworkBandwidthLimitKbps(Integer.parseInt(getParam(VIDEO_MAX_BANDWIDTH_ANY_NETWORK)));
                Log.d(LOG_TAG, "Set VIDEO_MAX_BANDWIDTH_ANY_NETWORK to " + Integer.parseInt(getParam(VIDEO_MAX_BANDWIDTH_ANY_NETWORK)));
            } catch (NumberFormatException e) {
                Log.d(LOG_TAG, "Value of VIDEO_MAX_BANDWIDTH_ANY_NETWORK parameter is illegal. Invalid int:" + getParam(VIDEO_MAX_BANDWIDTH_ANY_NETWORK));
            }
        }
        mediaConfiguration.setVoIPConfigurationVideo(vc);

        clientConfiguration.setMediaConfiguration(mediaConfiguration);

        return clientConfiguration;
    }

    /**
     * Set Client configuration and create client
     */
    public void setupClient() {

        if (mClient != null) {
            //clearMediaServicesInstance(); 03 august 2017 - commenting out this as CSDK is donig the cleaning
        }

        mClientConfiguration = setupClientConfiguration();
        mClient = new Client(mClientConfiguration, mApplication, SDKManager.getInstance());

        setLogLevel();
    }

    ClientConfiguration getClientConfiguration() {
        return mClientConfiguration;
    }

    /**
     * Set user configuration and create user
     */
    public void setupUserConfiguration() {

        try {
            if (deskPhoneService == null) {
                // initialize the deskphone service
                Log.d(LOG_TAG, "call to deskPhone initialize");
                deskPhoneService = DeskPhoneServiceLibrary.initialize(mContext, this, mClient, null);
            } else {
                // This is a case of recreate client and simply update the client
                // reference on the deskphone service.
                Log.d(LOG_TAG, "call to deskPhone client change");
                deskPhoneService.updateCommunicationsClient(mClient);
            }

            Log.d(LOG_TAG, "after call to deskPhone initialize");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception in setupUserConfiguration", e);
        }
    }

    /**
     * Initializing after crash or by manual launch
     */
    public void initAfterCrash() {

        // if the crash was handled by wochdog - we have nothing to do
        if (getCallService() != null ) {
            Log.d(LOG_TAG, "No special handling after crash is needed.");
            return;
        }

        // this part is needed for case where watchdog service has not yet recognized the application crash
        // and it was already restarted manually
        Log.d(LOG_TAG, "Special handling after crash: SEND CONFIG_CHANGE and then LOGIN_SIGNAL intents");
        notifyDeskPhoneServices();
    }

    /**
     * This method will notify deskphone services that we recovered from crash by means of local broadcast
     * this is done as from android oreo system broadcast are prohibited.
     * we use reflection as deskphone services does not expose its receiver
     */
    private void notifyDeskPhoneServices() {
        Object deskphoneEventsBroadcastReceiver = null;
        try {
            Object loadedapk = mContext.getClass().getField("mLoadedApk").get(mContext);
            Field fieldRecers = loadedapk.getClass().getDeclaredField("mReceivers");
            fieldRecers.setAccessible(true);
            ArrayMap receiversmap =  (ArrayMap) fieldRecers.get(loadedapk);
            Collection receivers = ((ArrayMap)receiversmap.get(mApplication)).keySet();

            for (Object rx:receivers
                    ) {
                if (rx.getClass().getSimpleName().equals("DeskPhoneEventsBroadcastReceiver")) {
                    deskphoneEventsBroadcastReceiver = rx;
                    break;
                }
            }

        } catch (NoSuchFieldException e) {
            Log.e(LOG_TAG, "Deskphone Services API changed: mReceivers was not found");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);


        Intent configChange = new Intent(CONFIG_CHANGED);
        configChange.setPackage(mContext.getPackageName());
        Intent loginSignal = new Intent(LOGIN_SIGNAL);
        loginSignal.setPackage(mContext.getPackageName());
        IntentFilter filter = new IntentFilter();
        filter.addAction(CONFIG_CHANGED);
        filter.addAction(LOGIN_SIGNAL);

        if(deskphoneEventsBroadcastReceiver != null) {
            localBroadcastManager.registerReceiver((BroadcastReceiver) deskphoneEventsBroadcastReceiver, filter);
        }
        else {
            Log.e(LOG_TAG, "registerReceiver: could not find deskphoneEventsBroadcastReceiver");
        }
        //mContext.sendBroadcast(configChange);
        localBroadcastManager.sendBroadcastSync(configChange);

        //mContext.sendBroadcast(loginSignal);
        localBroadcastManager.sendBroadcastSync(loginSignal);

        if(deskphoneEventsBroadcastReceiver != null) {
            localBroadcastManager.unregisterReceiver((BroadcastReceiver) deskphoneEventsBroadcastReceiver);
        }
        else {
            Log.e(LOG_TAG, "unregisterReceiver: could not find deskphoneEventsBroadcastReceiver");
        }
    }

    /**
     * Returning {@link Client}
     * @return {@link Client}
     */
    public Client getClient() {
        return mClient;
    }

    /**
     * Obtain {@link CallService}
     *
     * @return {@link CallService}
     */
    public CallService getCallService() {
        if (mUserManager != null && mUserManager.getmUser() != null) {
            return mUserManager.getmUser().getCallService();
        } else {
            return null;
        }
    }

    /**
     * Get {@link AudioInterface}
     *
     * @return {@link AudioInterface}
     */
    public AudioInterface getAudioInterface() {
        if (SDKManager.getInstance().getClient() == null || deskPhoneService == null)
            return null;
        return deskPhoneService.getAudioInterface();
    }

    /**
     * Check if user is logged in
     * @return boolean if user is logged in
     */
    public boolean isAnonymous() {

        return getCredential(SIPUSERNAME) == null || getCredential(SIPUSERNAME).equals(ANONYMOUS_USER) || getCredential(SIPUSERNAME).isEmpty();

    }

    /**
     * When user is created we are registering listener {@link UserRegistrationListener}.
     * Also we are adding to users {@link CallService} {@link com.avaya.clientservices.call.CallServiceListener}
     * To users {@link VoiceMessagingService} we will also add
     * {@link com.avaya.clientservices.voicemessaging.VoiceMessagingServiceListener}. After
     * those operations are finished we are performing start of {@link User}.
     */
    public void onUserCreated() {
        Log.d(LOG_TAG, "onUserCreated");
        // Initialize class member mUser if we created user successfully
        Log.d(LOG_TAG, "User Id = " + mUserManager.getmUser().getUserId());
        mUserManager.getmUser().addRegistrationListener(DeskPhoneServiceAdaptor.this);

        CallService callService = mUserManager.getmUser().getCallService();
        if (callService != null) {
            Log.d(LOG_TAG, "CallService is ready to use");
            // Subscribe to CallService events for incoming call handling
            callService.addListener(SDKManager.getInstance().getCallAdaptor());
        }

        VoiceMessagingService voiceMessageService = mUserManager.getmUser().getVoiceMessagingService();
        if (voiceMessageService != null) {
            Log.d(LOG_TAG, "VoiceMessageService is ready to use");
            // Subscribe to VoiceMessageService events for voice mail handling
            voiceMessageService.addListener(SDKManager.getInstance().getVoiceMessageAdaptor());
        }

        // And login
        mIsAtLeastOneSuccessfulRegistration = false;
        mUserManager.getmUser().start();
    }

    /**
     * In case that we had problem with creating user {@link UserCreatedException} is created and
     * this is place where we work with it.
     *
     * @param e {@link UserCreatedException}
     */
    public void onUserCreationFailure(UserCreatedException e) {
        Log.e(LOG_TAG, "createUser onError " + e.getFailureReason());
    }

    /**
     * Inform {@link UserManager} that we want to remove {@link User}
     *
     * @param user {@link User}
     */
    public void onUserRemoved(User user) {
        mUserManager.getmUser().removeRegistrationListener(DeskPhoneServiceAdaptor.this);
        mUserManager.onUserRemoved(user);
    }

    @Override
    public void onInitialised() {
        Log.d(LOG_TAG, "onInitialised");
    }

    /**
     * Processing configuration change
     * @param s String containing configuration data
     * @param completionHandler {@link CompletionHandler} to be informed on finishing
     *                                                   configuration change
     */
    @Override
    synchronized public void onConfigurationChange(String s, @SuppressWarnings("NullableProblems") CompletionHandler completionHandler) {
        Log.d(LOG_TAG, "onConfigurationChange s= " + s);

        /* if this is first time the configuration was received - we need to recreate the client */
        boolean firstConfiguration = false;
        boolean ismapsAreEqual = false;
        if (mPreviousConfig.size() == 0)
            firstConfiguration = true;

        if (mConfig.size() != 0)
            //noinspection unchecked
            mPreviousConfig = (HashMap<String, String>) mConfig.clone();
        try {
            mConfig.clear();

            mConfigPasrser.parseStream(new ByteArrayInputStream(s.getBytes("UTF-8")));

            ismapsAreEqual = mapsAreEqual(mConfig,mPreviousConfig);
        } catch (Exception e) {
            Log.d(LOG_TAG, "Can not parse the new configuration.");
        }

        // check whether some service-effective parameters have changed
        if(ismapsAreEqual==false)
            reconfigureCSDK(areCredentialsChanged(), firstConfiguration);
        if (mUiObj != null && mUiObj.get() != null && mUserConfiguration != null && mUserConfiguration.getSIPUserConfiguration() != null) {
            final String clientPlatformName = getParam(SIP_USER_DISPLAY_NAME);
            mUiObj.get().onUserRegistrationSuccessful(clientPlatformName, getCredential(SIPUSERNAME));
        }

        setNameExtensionVisibility();


        completionHandler.onFinish();
    }

    private boolean mapsAreEqual(Map<String, String> mapA, Map<String, String> mapB) {

        try{
            if (mapA == null || mapB == null)
                return false;

            for (String k : mapB.keySet())
            {
                if (!mapA.get(k).equals(mapB.get(k))) {
                    return false;
                }
            }
            for (String y : mapA.keySet())
            {
                if (!mapB.containsKey(y)) {
                    return false;
                }
            }
        } catch (NullPointerException np) {
            return false;
        }
        return true;
    }
    /**
     * Processing login request with provided data and {@link CompletionHandler}
     * @param s String representation of required data for login of user
     * @param completionHandler {@link CompletionHandler}
     */
    @Override
    public void onLogin(String s, @SuppressWarnings("NullableProblems") CompletionHandler completionHandler) {
        Log.d(LOG_TAG, "onLogin");
        try {
            mCredentialParser.parseStream(new ByteArrayInputStream(s.getBytes("UTF-8")));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error credentials parsing", e);
        }

        // make sure the createUser process is performed on the same thread on which CSDK's callbacks are called
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                // if this is the first time user is created after configuration was received - the client shall be recreated
                createUser((mPreviousConfig.size() == 0) && (mConfig.size() != 0));
            }
        });

        completionHandler.onFinish();
    }

    /**
     * Processing offHook request
     * @param handsetType {@link HandsetType} for which event is called
     */
    @Override
    public void offHook(@NonNull HandsetType handsetType) {
        if (!ElanApplication.isMainActivityVisible()) {
            Intent intent = new Intent(mContext, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(mContext, 0, intent, 0);
            try {
                pendingIntent.send();
                Utils.wakeDevice(mContext);
            } catch (PendingIntent.CanceledException e) {
                Log.e(LOG_TAG, "failed to activate MainActivity from pending intent while it was not visible");
            }

        }

        //if EULA has not been accepted, offHook event can't be received
        SharedPreferences preferences = mContext.getSharedPreferences(Constants.EULA_PREFS_NAME, MODE_PRIVATE);
        boolean eulaAccepted = preferences.getBoolean(Constants.KEY_EULA_ACCEPTED, false);
        if (!eulaAccepted) {
            return;
        }

        SDKManager.getInstance().getAudioDeviceAdaptor().handleOffHook(mContext,convertToUIAudioDevice(handsetType));
        if (mUiObj != null && mUiObj.get() != null)
            mUiObj.get().onOffHook(handsetType);
    }

    /**
     * Processing onHook request
     * @param handsetType {@link HandsetType} for which event is called
     */
    @Override
    public void onHook(@NonNull HandsetType handsetType) {
        SDKManager.getInstance().getAudioDeviceAdaptor().handleOnHook(convertToUIAudioDevice(handsetType));
        if (mUiObj != null && mUiObj.get() != null)
            mUiObj.get().onOnHook(handsetType);
    }

    @Override
    public void onLogout() {
        // make sure the logout process is performed on the same thread on which CSDK's callbacks are called
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                onLogoutJob();
            }
        });
    }

    public void onLogoutJob() {
        Log.i(LOG_TAG, "received logout intent from platform, performing graceful logout");

        // if the device is already in logout process, no need to start logout again
        if (isLogoutInProgress()) {
            Log.d(LOG_TAG, "Logout was started already.");
            return;
        }

        setLogoutInProgress(true);
        if ( !initUserShutdown() )
            setLogoutInProgress(false);

        if (mUiObj != null && mUiObj.get() != null) {
            mUiObj.get().finishAndLock();
        }
        mNotifFactory.showOnLine(mContext.getString(R.string.logged_out));
        mIsAtLeastOneSuccessfulRegistration = false;
    }

    @Override
    public void onKeyUp(@NonNull HardButtonType hardButtonType) {
        if (mUiObj != null && mUiObj.get() != null) {
            mUiObj.get().onKeyUp(hardButtonType);
        }
    }

    @Override
    public void onKeyDown(@NonNull HardButtonType hardButtonType) {
        if (mUiObj != null && mUiObj.get() != null) {
            mUiObj.get().onKeyDown(hardButtonType);
        }
    }

    /**
     * @return true if CORDLESS_HANDSET is available
     */
    public boolean isOffHookBTHandset(){
        Set<HandsetType> handsetTypes = deskPhoneService.getHandsetManager().getOffHookHandsets();
        return (handsetTypes.size() > 0 && handsetTypes.contains(HandsetType.CORDLESS_HANDSET));
    }

    /**
     * Converting {@link HandsetType} to {@link UIAudioDevice}
     * @param handsetType {@link HandsetType} to be converted
     * @return {@link UIAudioDevice} to which {@link HandsetType} is converted
     */
    private UIAudioDevice convertToUIAudioDevice(HandsetType handsetType){
        if (handsetType == HandsetType.WIRED_HEADSET)
            return UIAudioDevice.RJ9_HEADSET;
        else if (handsetType == HandsetType.CORDLESS_HANDSET)
            return UIAudioDevice.WIRELESS_HANDSET;
        else
            return UIAudioDevice.HANDSET;
    }

    /**
     * Obtain {@link UserConfiguration} from CSDK
     *
     * @return {@link UserConfiguration}
     */
    synchronized public UserConfiguration getSdkUserConfiguration() {

        final UserConfiguration userInfo = new UserConfiguration();
        populateAcsConfiguration(userInfo.getACSConfiguration());
        populateDialingRulesConfiguration(userInfo.getDialingRulesConfiguration());
        enableLocalContactConfiguration(userInfo.getLocalContactConfiguration());
        populateSipConfiguration(userInfo.getSIPUserConfiguration());
        setPopulateVideoUserConfiguration(userInfo.getVideoUserConfiguration());
        populatePpmConfiguration(userInfo.getPPMConfiguration());
        enablePresenceConfiguration(userInfo.getPresenceConfiguration());
        setEC500Configuration(userInfo.getEC500Configuration());
        populateConferenceConfiguration(userInfo.getConferenceConfiguration());

        if (mContext.getExternalFilesDir(null) != null) {
            final String logFile = mContext.getExternalFilesDir(null).getAbsolutePath() + File.separatorChar + getCredential(SIPUSERNAME) + "_call_logs.xml";
            Log.d(LOG_TAG, "log file set to " + logFile);
            //noinspection ConstantConditions
            userInfo.setLocalCallLogFilePath(logFile);
        }
        mUserConfiguration = userInfo;
        return userInfo;
    }

    /**
     * Enable {@link PresenceConfiguration}
     *
     * @param presenceConfiguration {@link PresenceConfiguration}
     */
    private void enablePresenceConfiguration(PresenceConfiguration presenceConfiguration) {
        presenceConfiguration.setEnabled(true);
    }

    /**
     * Setting up {@link VideoUserConfiguration} enabled
     * preferences {@link MediaTransportPreference}
     *
     * @param videoConfig {@link VideoUserConfiguration}
     */
    private void setPopulateVideoUserConfiguration(VideoUserConfiguration videoConfig) {
        videoConfig.setEnabledPreference(getVideoEnabledPreferences());
    }

    /**
     * Obtain {@link MediaTransportPreference}
     *
     * @return {@link MediaTransportPreference} for enabled video
     */
    private MediaTransportPreference getVideoEnabledPreferences() {
        if (getConfigBooleanParam(ENABLE_VIDEO)) {
            return MediaTransportPreference.ALL_TRANSPORTS;
        } else {
            return MediaTransportPreference.NO_MEDIA;
        }
    }

    /**
     * Populating ACS configuration {@link ACSConfiguration}
     *
     * @param acsConfiguration {@link ACSConfiguration} to be populated
     */
    private void populateAcsConfiguration(@NonNull ACSConfiguration acsConfiguration) {

        Log.d(LOG_TAG, "populateAcsConfiguration");

        acsConfiguration.setEnabled(getConfigBooleanParam(ACSENABLED));

        try {
            String acsPort = getParam(ACSPORT);
            int nAcsPort = (acsPort == null) ? DEFAULT_ACS_PORT : Integer.parseInt(acsPort);
            final boolean bAcsSecure = getConfigBooleanParam(ACSSECURE);
            ServerInfo acsServerInfo = new ServerInfo(getParam(ACSSRVR), nAcsPort, bAcsSecure);
            acsConfiguration.setServerInfo(acsServerInfo);
        } catch (NumberFormatException e) {
            Log.d(LOG_TAG, "Value of ACSPORT parameter is illegal. Invalid int:" + getParam(ACSPORT));
        }

        acsConfiguration.setCredentialProvider(new CredentialProvider() {
            @Override
            public void onAuthenticationChallenge(Challenge challenge, CredentialCompletionHandler credentialCompletionHandler) {
                Log.d(LOG_TAG, "ACSConfiguration.onAuthenticationChallenge : Challenge = "
                        + challenge);

                // Getting login information from settings
                String extension = getCredential(SSOUSERID);
                String password = getCredential(SSOPASSWORD);
                String domain = getParam(SIPDOMAIN);

                // Login with saved credentials
                UserCredential userCredential = new UserCredential(extension, password, domain);
                credentialCompletionHandler.onCredentialProvided(userCredential);
            }

            @Override
            public void onCredentialAccepted(Challenge challenge) {
                Log.d(LOG_TAG, "ACSConfiguration.onCredentialAccepted");
            }

            @Override
            public void onAuthenticationChallengeCancelled(Challenge challenge) {
                Log.d(LOG_TAG, "ACSConfiguration.onAuthenticationChallengeCancelled");
            }
        });
    }

    /**
     * Enable {@link LocalContactConfiguration}
     *
     * @param localContactConfiguration {@link LocalContactConfiguration} to be enabled
     */
    private void enableLocalContactConfiguration(@NonNull LocalContactConfiguration localContactConfiguration) {
        localContactConfiguration.setEnabled(true);
    }

    /**
     * Setup {@link SIPUserConfiguration}
     *
     * @param sipConfig {@link SIPUserConfiguration} to be populated
     */
    private void populateSipConfiguration(@NonNull SIPUserConfiguration sipConfig) {

        Log.d(LOG_TAG, "populateSipConfiguration");

        final ConnectionPolicy connectionPolicy = createConnectionPolicy();
        sipConfig.setConnectionPolicy(connectionPolicy);

        sipConfig.setEnabled(true);
        sipConfig.setMobilityMode(MobilityMode.FIXED);
        String sipUserName = getCredential(SIPUSERNAME);
        if (sipUserName != null) {
            //noinspection deprecation
            sipConfig.setUserId(sipUserName);
            mLastUserName = sipUserName;
        }
        if (getParam(SIPDOMAIN) != null) {
            sipConfig.setDomain(getParam(SIPDOMAIN));
        } else {
            Log.d(LOG_TAG, "No domain provided in settings");
        }

        sipConfig.setVideoEnabled(/*getConfigBooleanParam(ENABLE_VIDEO) <put in remarks becuase ELAN-622>*/true);

        sipConfig.setSrtcpEnabled(getConfigBooleanParam(ENCRYPT_SRTCP));

        /* Get the configuration for MEDIAENCRYPTION*/
        String mediaEncrypt = getParam(MEDIAENCRYPTION);
        if (mediaEncrypt != null && !mediaEncrypt.isEmpty()) {
            /* Since MEDIAENCRYPTION might be a list of comma-separated numbers, */
            /* try to split it into an array                                     */
            String mediaEncryptionTypes[] = mediaEncrypt.split(",");
            ArrayList<MediaEncryptionValue> mediaEncryptionTypeArrayList = new ArrayList<>();
            for (String type : mediaEncryptionTypes) {
                try {
                    mediaEncryptionTypeArrayList.add(MediaEncryptionValue.getMediaEncryptionValue(Integer.parseInt(type)));
                } catch (NumberFormatException e) {
                    Log.d(LOG_TAG, "Value of MEDIAENCRYPTION parameter is illegal. Invalid int:" + getParam(MEDIAENCRYPTION));
                }
            }
            Log.d(LOG_TAG, "Media-encryption values: " + MediaEncryptionValue.mediaEncryptionTypeList(mediaEncryptionTypeArrayList));
            sipConfig.setMediaEncryptionTypeList(MediaEncryptionValue.mediaEncryptionTypeList(mediaEncryptionTypeArrayList));
        }

        mCredentialUserName = getCredential(SIPUSERNAME);
        mCredentialPassword = getCredential(SIPPASSWORD);
        mCredentialSha1= getCredential(SIPHA1);

        sipConfig.setCredentialProvider(this);
        if (getConfigBooleanParam(ConfigParametersNames.ENABLE_IPOFFICE)) {
            sipConfig.setDialPlan(getParam(ConfigParametersNames.DIALPLAN));

            sipConfig.getSIPClientConfiguration().setInterDigitTimeout(Integer.valueOf(getParam(ConfigParametersNames.INTER_DIGIT_TIMEOUT, true)));
            sipConfig.getSIPClientConfiguration().setNoDigitTimeout(Integer.valueOf(getParam(ConfigParametersNames.NO_DIGIT_TIMEOUT, true)));
        }

        if (getConfigBooleanParam(ENABLE_IPOFFICE) && (getParam(SUBSCRIBE_LIST_NON_AVAYA) != null)) {

            String[] subscribeEvents = getParam(SUBSCRIBE_LIST_NON_AVAYA).split("\\s*,\\s*");  // split using , and trim in one shot
            List eventList = Arrays.asList(subscribeEvents);
            OutboundSubscriptionConfiguration outboundSubscriptionConfig = new OutboundSubscriptionConfiguration();
            //outboundSubscriptionConfig.setAllSubscriptionsDisabled(true);      // Turning everything off except what we need for IPO.
            outboundSubscriptionConfig.setRegistrationInfoEnabled( eventList.contains("reg") );
            outboundSubscriptionConfig.setDialogInfoEnabled( eventList.contains("dialog") );
            outboundSubscriptionConfig.setMessageSummaryEnabled( eventList.contains("mwi")  || eventList.contains("message-summary") );
            outboundSubscriptionConfig.setCCSProfileEnabled( eventList.contains("ccs") || eventList.contains("avaya-ccs-profile") );

            sipConfig.setOutboundSubscriptionConfig(outboundSubscriptionConfig);
        }

        SIPClientConfiguration sipClientConfiguration = sipConfig.getSIPClientConfiguration();
        if (sipClientConfiguration == null)
            return;

        if (getParam(REGISTERWAIT) != null) {
            try {
                sipClientConfiguration.setRegistrationTimeout(Integer.parseInt(getParam(REGISTERWAIT)));
            } catch (NumberFormatException e) {
                Log.d(LOG_TAG, "Value of REGISTERWAIT parameter is illegal. Invalid int:" + getParam(REGISTERWAIT));
            }
        }
        /* According to 174409-150, value of DSCPSIG shall be taken by CSDK from PPM
        So currently the parameter is taken from the platform only for IPO    */
        if (getConfigBooleanParam(ENABLE_IPOFFICE) && getParam(DSCPSIG) != null) {
            try {
                sipClientConfiguration.setSignalingDSCP(Integer.parseInt(getParam(DSCPSIG)));
            } catch (NumberFormatException e) {
                Log.d(LOG_TAG, "Value of DSCPSIG parameter is illegal. Invalid int:" + getParam(DSCPSIG));
            }
        }

        if (getConfigBooleanParam(ENABLE_IPOFFICE)) {
            sipClientConfiguration.setReliableProvisionalResponsesEnabled(getConfigBooleanParam(HUNDRED_REL_SUPPORT));
        }
    }

    /**
     * Populate {@link DialingRulesConfiguration}
     *
     * @param configuration {@link DialingRulesConfiguration} to be populated
     */
    private void populateDialingRulesConfiguration(DialingRulesConfiguration configuration) {

        // Determine if dialing rules are enabled or not
        configuration.setEnabled(getConfigBooleanParam(ENHDIALSTAT));

        // set the number to dial to access external line
        if (getParam(PHNOL) != null) {
            configuration.setOutsideLineAccessCode(getParam(PHNOL));
        }

        // set the country code
        if (getParam(PHNCC) != null) {
            configuration.setCountryCode(getParam(PHNCC));
        }

        // set the area code
        if (getParam(DIALPLANAREACODE ) != null) {
            configuration.setAreaCode(getParam(DIALPLANAREACODE));
        } else if (getParam(SP_AC) != null) {
            configuration.setAreaCode(getParam(SP_AC));
        }

        if (getParamValue(DIALPLANLOCALCALLPREFIX) != null) {
            configuration.setRemoveAreaCodeForLocalCalls(getConfigBooleanParam(DIALPLANLOCALCALLPREFIX));
        } else {
            configuration.setRemoveAreaCodeForLocalCalls(getConfigBooleanParam(PHN_REMOVE_AREA_CODE));
        }

        // set the PBX Main Prefix
        if (getParam(DIAL_PLAN_PBX_PREFIX) != null) {
            configuration.setPBXPrefix(getParam(DIAL_PLAN_PBX_PREFIX));
        } else if (getParam(PHN_PBX_MAIN_PREFIX) != null) {
            configuration.setPBXPrefix(getParam(PHN_PBX_MAIN_PREFIX));
        }

        // set the long distance number
        if (getParam(PHNLD) != null) {
            configuration.setLongDistanceAccessCode(getParam(PHNLD));
        }

        // set the international call number
        String phnic = getParam(PHNIC);
        if (phnic != null) {
            configuration.setInternationalAccessCode(phnic);
        }

        // set Internal extension length list: DIALPLANEXTENSIONLENGTHLIST takes precedence over PHNDPLENGTH
        String[] stringArray = null;
        if (getParam(DIALPLANEXTENSIONLENGTHLIST) != null) {
            stringArray = getParam(DIALPLANEXTENSIONLENGTHLIST).split(",");
        }
        else if (getParam(PHNDPLENGTH) != null) {
            stringArray = getParam(PHNDPLENGTH).split(",");
        }
        if (stringArray != null) {
            int phnDpLen[] = new int[stringArray.length];
            try {
                for (int i = 0; i < stringArray.length; i++) {
                    phnDpLen[i] = Integer.parseInt(stringArray[i]);
                }
                configuration.setInternalExtensionLengths(phnDpLen);
            } catch (NumberFormatException e) {
                Log.d(LOG_TAG, "Value of DIALPLANEXTENSIONLENGTHLIST or PHNDPLENGTH parameter is illegal. Shall include only integers.");
            }
        }

        // set National number length list: DIALPLANNATIONALPHONENUMLENGTHLIST takes precedence over PHNLDLENGTH
        stringArray=null;
        if (getParam(DIALPLANNATIONALPHONENUMLENGTHLIST) != null) {
            stringArray = getParam(DIALPLANNATIONALPHONENUMLENGTHLIST).split(",");
        }
        else if (getParam(PHNLDLENGTH) != null) {
            stringArray = getParam(PHNLDLENGTH).split(",");
        }
        if (stringArray != null) {
            int phnLdLen[] = new int[stringArray.length];
            try {
                for (int i = 0; i < stringArray.length; i++) {
                    phnLdLen[i] = Integer.parseInt(stringArray[i]);
                }
                configuration.setNationalNumberLengths(phnLdLen);
            } catch (NumberFormatException e) {
                Log.d(LOG_TAG, "Value of PHNLDLENGTH or DIALPLANNATIONALPHONENUMLENGTHLIST parameter is illegal. Shall include only inregers.");
            }
        }

        configuration.setApplyARSToShortNumbers(getConfigBooleanParam(AUTO_APPLY_ARS_TO_SHORT_NUMBERS));
        // Note the negation here. The SDK has implemented the opposite sense of
        // the definition of the APPLY_DIALINGRULES_TO_PLUS_NUMBERS setting.
        configuration.setE164PassthroughEnabled(!getConfigBooleanParam(APPLY_DIALING_RULES_TO_PLUS_NUMBERS));

        if ((getParam(POUND_KEY_AS_CALL_TRIGGER) != null) && (getParam(POUND_KEY_AS_CALL_TRIGGER) != POUND_KEY_AS_CALL_TRIGGER.getDefaultValue())) {
            configuration.setEnabled(true);
            configuration.setPoundKeyUsedAsCallTrigger(getConfigBooleanParam(POUND_KEY_AS_CALL_TRIGGER));
        }
    }

    /**
     * Set {@link EC500Configuration}
     *
     * @param configuration {@link EC500Configuration} to be enabled
     */
    private void setEC500Configuration(EC500Configuration configuration) {

        configuration.setEnabled(getConfigBooleanParam(EC500ENABLED));
        if (getConfigBooleanParam(ENABLE_IPOFFICE) && (getParam(PSTN_VM_NUM) != null)){
            configuration.setVoicemailNumber(getParam(PSTN_VM_NUM));
        }
    }

    /**
     * Sets the CONFERENCE_FACTORY_URI from the config file in the
     * ConferenceConfiguration object
     * @param config {@link ConferenceConfiguration}
     */
    private void populateConferenceConfiguration(ConferenceConfiguration config) {
        if (getConfigBooleanParam(ENABLE_IPOFFICE) && getParam(CONFERENCE_FACTORY_URI) != null) {
            config.setConferenceFactoryUri(getParam(CONFERENCE_FACTORY_URI));
        }
    }

    /**
     * Prepare {@link ConnectionPolicy}
     *
     * @return {@link ConnectionPolicy}
     */
    private ConnectionPolicy createConnectionPolicy() {

        /* get the configuration for SIP_CONTROLLER_LIST */
        String sipServersEntry = getParam(SIP_CONTROLLER_LIST);
        if (sipServersEntry == null) {
            Log.e(LOG_TAG, "Missing SIP_CONTROLLER_LIST in settings file");
            return null;
        }

        /* Since there may be more than one Sip Controllers in the list, split them into an array */
        String sipServers[] = sipServersEntry.split(",");
        SignalingServer[] serverList = new SignalingServer[sipServers.length];
        for (int i = 0; i < sipServers.length; i++) {

            String server = sipServers[i];

            String serverDetails[] = server.split(";");
            int idx = serverDetails[0].indexOf(':');
            String ip = serverDetails[0];
            if (idx != -1)
                ip = serverDetails[0].substring(0, idx);
            SignalingServer.TransportType transportType = SignalingServer.TransportType.AUTO;
            if (serverDetails.length > 1)
                transportType = serverDetails[1].equals("transport=tls") ? SignalingServer.TransportType.TLS : SignalingServer.TransportType.AUTO;
            int port = (transportType == SignalingServer.TransportType.TLS) ? 5061 : 5060;
            if (idx != -1) {
                try {
                    port = Integer.parseInt(serverDetails[0].substring(idx + 1));
                } catch (NumberFormatException e) {
                    Log.d(LOG_TAG, "Port value for one of servers in SIP_CONTROLLER_LIST parameter is illegal. Shall be int:" + getParam(SIP_CONTROLLER_LIST));
                }
            }

            Log.d(LOG_TAG, "SIP SERVER " + ip + ":" + port + ":" + " transport=" + transportType);
            serverList[i] = new SignalingServer(transportType, ip, port, SignalingServer.FailbackPolicy.AUTOMATIC);
        }

        RegistrationGroup[] registrationGroups = getRegistrationGroups(serverList);
        final ConnectionPolicy connectionPolicy = new ConnectionPolicy(new SignalingServerGroup(registrationGroups));
        connectionPolicy.setInitialReconnectInterval(INITIAL_RECONNECT_INTERVAL);

        return connectionPolicy;
    }

    private RegistrationGroup[] getRegistrationGroups(SignalingServer[] serverList) {

        int simultaneousReg = Integer.parseInt(SIMULTANEOUS_REGISTRATIONS.getDefaultValue());
        if (getParam(SIMULTANEOUS_REGISTRATIONS) != null) {
            try {
                simultaneousReg = Integer.parseInt(getParam(SIMULTANEOUS_REGISTRATIONS));
            } catch (NumberFormatException e) {
                Log.d(LOG_TAG, "Value of SIMULTANEOUS_REGISTRATIONS parameter is illegal. Invalid int:" + getParam(SIMULTANEOUS_REGISTRATIONS));
            }
        }

        RegistrationGroup[] registrationGroups = null;
        if (simultaneousReg > 1) {
            registrationGroups = new RegistrationGroup[1]; // all servers in the same group
            registrationGroups[0] = new RegistrationGroup(serverList);
        } else {
            registrationGroups = new RegistrationGroup[serverList.length];
            for (int i = 0; i < serverList.length; ++i) {
                registrationGroups[i] = new RegistrationGroup(serverList[i]);
            }
        }

        return registrationGroups;
    }

    /**
     * Set {@link PPMConfiguration} enabled and set {@link CredentialProvider} and
     * enable contacts for it.
     *
     * @param ppmConfig {@link PPMConfiguration}
     */
    private void populatePpmConfiguration(PPMConfiguration ppmConfig) {

        if (getConfigBooleanParam(ENABLE_IPOFFICE)){
            ppmConfig.setEnabled(false);
        } else {
            ppmConfig.setCredentialProvider(this);
            ppmConfig.setEnabled(true);
            ppmConfig.setCallJournalingEnabled (getConfigBooleanParam(ENABLE_PPM_CALL_JOURNALING));
            ppmConfig.setContactsEnabled(true);
        }
    }

    /**
     * Return current method name for purpose of logging
     *
     * @return String
     */
    private static String getCurrentMethodName() {
        final StackTraceElement e = Thread.currentThread().getStackTrace()[3];
        final String s = e.getClassName();
        return s.substring(s.lastIndexOf('.') + 1, s.length()) + "." + e.getMethodName();
    }

    /**
     * Processing user registration progress
     * @param user {@link User} for which registration is in progress
     * @param server {@link SignalingServer}
     */
    @Override
    public void onUserRegistrationInProgress(User user, SignalingServer server) {
        Log.d(LOG_TAG, getCurrentMethodName());
        SDKManager.getInstance().getContactsAdaptor().setUser(user);
    }

    /**
     * Processing successful {@link User} registration
     * @param user {@link User} which is registered
     * @param server {@link SignalingServer}
     */
    @Override
    public void onUserRegistrationSuccessful(User user, SignalingServer server) {
        Log.d(LOG_TAG, getCurrentMethodName()+" Server:"+server);

        SDKManager.getInstance().getContactsAdaptor().setUser(user);

        SDKManager.getInstance().getHistoryAdaptor().setLogService(user.getCallLogService());
        mIsAtLeastOneSuccessfulRegistration = true;
        mNotifFactory.showOnLine("anonymous".equals(getCredential(ConfigParametersNames.SIPUSERNAME)) ?
                mContext.getString(R.string.logged_out) :
                mContext.getString(R.string.notification_online));
    }

    /**
     * Processing failed {@link User} registration
     * @param user {@link User} for which failed registration have to be processed
     * @param server {@link SignalingServer}
     * @param exception {@link Exception} which is return with failed registration
     */
    @Override
    public void onUserRegistrationFailed(User user, SignalingServer server, Exception exception) {
        Log.d(LOG_TAG, getCurrentMethodName());

        if (exception instanceof RegistrationException) {
            final RegistrationException registrationException = (RegistrationException) exception;
            Log.w(LOG_TAG, "Registration failed for server " + server + ": " + exception.getMessage() + " " + registrationException.getProtocolErrorCode() + " " +
                    registrationException.getProtocolErrorReason());
        }
        SDKManager.getInstance().getContactsAdaptor().setUser(user);
        if(!mIsAtLeastOneSuccessfulRegistration)
            mNotifFactory.showOnLine( mApplication.getApplicationContext().getString(R.string.logged_out) );
    }

    /**
     * Processing {@link User} successful registration
     * @param user {@link User} which is registered
     */
    @Override
    public void onUserAllRegistrationsSuccessful(final User user) {
        Log.d(LOG_TAG, getCurrentMethodName());

        SDKManager.getInstance().getContactsAdaptor().setUser(user);
        final String clientPlatformName = getParam(SIP_USER_DISPLAY_NAME);
        if (mUiObj != null && mUiObj.get() != null)
            mUiObj.get().onUserRegistrationSuccessful(clientPlatformName, getCredential(SIPUSERNAME));
    }


    /**
     * Processing {@link User} registration failed
     * @param user {@link User}
     * @param willRetry
     */
    @Override
    public void onUserAllRegistrationsFailed(User user, boolean willRetry) {
        Log.d(LOG_TAG, getCurrentMethodName()+" willRetry="+willRetry);
        mNotifFactory.showOnLine(mApplication.getApplicationContext().getString(R.string.logged_out));
    }

    @Override
    public void onUserUnregistrationInProgress(User user, SignalingServer server) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onUserUnregistrationSuccessful(User user, SignalingServer server) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onUserUnregistrationFailed(User user, SignalingServer server, Exception exception) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onUserUnregistrationComplete(User user) {

        Log.d(LOG_TAG, getCurrentMethodName());
        mNotifFactory.showOnLine(mContext.getString(R.string.logged_out));
    }

    @Override
    public void onRegistrationResponsePayloadReceived(User user, List<MessageBodyPart> payloadParts, SignalingServer server) {
        if(payloadParts != null && payloadParts.size() > 0) {
            Log.d(LOG_TAG, getCurrentMethodName() + " server:" + server.getHostname() + " " + StandardCharsets.UTF_8.decode(ByteBuffer.wrap(payloadParts.get(0).getData())).toString());
        }
    }

    /**
     * Processing authentication challenge
     * @param challenge {@link Challenge}
     * @param credentialCompletionHandler {@link CredentialCompletionHandler}
     */
    @Override
    public void onAuthenticationChallenge(Challenge challenge, CredentialCompletionHandler credentialCompletionHandler) {
        Log.d(LOG_TAG, "UserCredentialProvider.onAuthenticationChallenge : Challenge = "
                + challenge);

        // Getting login information from settings
        String extension = mCredentialUserName;
        if (extension == null) {
            extension = getCredential(SIPUSERNAME);
        }

        Log.d(LOG_TAG, "UserCredentialProvider.onAuthenticationChallenge : extension = "
                + extension);

        // Note: Although this sample application manages passwords as clear text this application
        // is intended as a learning tool to help users become familiar with the Avaya SDK.
        // Managing passwords as clear text is not illustrative of a secure process to protect
        // passwords in an enterprise quality application.
        String password = "";
        if (mCredentialPassword != null) {
            password = mCredentialPassword;
        } else if (getCredential(SIPPASSWORD) != null) {
            password = getCredential(SIPPASSWORD);
        }

        String sipha1 = "";
        if (mCredentialSha1 != null) {
            sipha1 = mCredentialSha1;
        } else if (getCredential(SIPHA1) != null) {
            sipha1 = getCredential(SIPHA1);
        }

        if (extension.isEmpty() || (password.isEmpty() && sipha1.isEmpty())) {
            ErrorManager.getInstance().addErrorToList(EMPTY_CREDENTIALS_ERROR);
        } else {
            ErrorManager.getInstance().removeErrorFromList(EMPTY_CREDENTIALS_ERROR);
        }

        String domain = getParam(SIPDOMAIN);

        // Login with saved credentials
        UserCredential userCredential = new UserCredential(extension, password, domain, sipha1);
        credentialCompletionHandler.onCredentialProvided(userCredential);
    }

    @Override
    public void onCredentialAccepted(Challenge challenge) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    @Override
    public void onAuthenticationChallengeCancelled(Challenge challenge) {
        Log.d(LOG_TAG, getCurrentMethodName());
    }

    /**
     * Return registered user display name
     *
     * @return String representation of registered user display name
     */
    public String getRegisteredUserDisplayName() {

        if (mUserManager.getState() == HAVE_USER)
            return mUserConfiguration.getSIPUserConfiguration().getDisplayName();

        return null;
    }

    /**
     * Returns name of the registered user or null of no user is registered
     *
     * @return String name of registered user
     */
    public String getRegisteredUser() {

        if (mUserManager.getState() == HAVE_USER) {
            //noinspection deprecation
            return mUserConfiguration.getSIPUserConfiguration().getUserId();
        }
        return null;
    }

    /**
     * Apply configuration change
     *
     * @param includeUIImpactChanges boolean telling us do changed performed impact UI
     */
    public void applyConfigChanges(boolean includeUIImpactChanges) {
        SDKManager.getInstance().getCallAdaptor().endAllCalls();
        createUser(true);
        if (includeUIImpactChanges && (mUiObj.get() != null) && mContext!=null) {
            mUiObj.get().onNonServiceImpactingParamChange();
        }
    }

    /**
     * Check whether any service-impacting parameters has changed and
     * if this is the case, inform the user about need CSDK reconfiguration
     *
     * @param credentialChange boolean which telling us are credentials changed
     */
    private void reconfigureCSDK(boolean credentialChange, boolean firstConfiguration) {

        Log.d(LOG_TAG, "reconfigureCSDK credentialChange=" + credentialChange + " firstConfiguration=" + firstConfiguration);

        ArrayList<String> changedParams = getListOfChangedParams();

        boolean includeServiceImpactingParam = includeServiceImpactingParam(changedParams);
        boolean includeUIImpactingParam = includeUIImpactingParam(changedParams);
        if( !includeServiceImpactingParam ||
                (includeUIImpactingParam && (credentialChange || firstConfiguration))) {
            Log.d(LOG_TAG, "Signalling application to apply config changes on resume");
            ElanApplication.setApplyConfigChange(true);
            if(mUiObj != null && mUiObj.get() != null && mContext!=null ) {
                mUiObj.get().onNonServiceImpactingParamChange();
            }
        }
        if (credentialChange || firstConfiguration) {
            SDKManager.getInstance().getCallAdaptor().endAllCalls();
            final boolean isClientRecreationRequired = includeServiceImpactingParam || firstConfiguration;
            // make sure the createUser process is performed on the same thread on which CSDK's callbacks are called
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                         @Override
                                                         public void run () {
                createUser(isClientRecreationRequired);
                                                         }
            });
            return;
        }

        if(mUiObj != null && mUiObj.get() != null) {
            if (includeServiceImpactingParam) {
                mUiObj.get().onServiceImpactingParamChange(includeUIImpactingParam);
            }
        } else {
            // TODO need fixing
            Log.e(LOG_TAG, "Cannot reconfigure CSDK. Weak reference is null.");
        }
    }

    /**
     * Processing creation of user and informing {@link UserManager} about user creation. In case
     * provided parameter is true {@link UserManager} is shut down and {@link Client} is asked for
     * shut down and {@link User} is created.
     * @param isClientRecreationRequired
     */
    public void createUser(boolean isClientRecreationRequired) {

        Log.d(LOG_TAG, "createUser isClientRecreationRequired=" + isClientRecreationRequired);

        if (getConfigBooleanParam(ENABLE_IPOFFICE) && isAnonymous()) {
            Log.d(LOG_TAG, "Anonymous user has no meaning for IPOffice.");
            return;
        }

        // if the new user is anonymous - close all existing calls
        if (isAnonymous()) {
            SDKManager.getInstance().getCallAdaptor().endAllCalls();
        }

        if (isClientRecreationRequired) {
            Log.d(LOG_TAG, "Shutting down the user.");

            // start user shutdown. If client is shutdown, user creation will be started in onClientShutdown callback
            if (initUserShutdown()) {
                Log.d(LOG_TAG, "Client was shutdown. User creation will be done in onClientShutdown.");
                return;
            }
        }

        Log.d(LOG_TAG, "Client shutdown was not needed. User will be created now.");
        if (getConfigBooleanParam(ENABLE_IPOFFICE) && (mUserManager.getState() == NO_USER)) {
            setupClient();
            setupUserConfiguration();
            SDKManager.getInstance().getAudioDeviceAdaptor().init();
        }
        mUserManager.createUser();
    }

    synchronized private boolean initUserShutdown() {

        if (mUserManager.shutdown()) {

            /* the user will be created in onClientShutdown callback */
            SDKManager.getInstance().getAudioDeviceAdaptor().shutdown();

            //mClient.getMediaEngine().close();
            Log.d(LOG_TAG, "Shutting down the client.");
            mClient.shutdown(true);

            return true;
        }

        if (mUserManager.getState() == SHUTTING_DOWN)
            return true;

        return false;
    }
    /**
     * Compare previous and current configurations and build the list of changed parameters
     *
     * @return {@link ArrayList<String>} of parameters which are changed
     */
    private ArrayList<String> getListOfChangedParams() {

        ArrayList<String> changedParam = new ArrayList<String>();

        // go through the new configuration and for each parameter check
        // whether its value has changed
        for (String key : mConfig.keySet()) {
            boolean isChanged = true;
            String previousValue = mPreviousConfig.get(key);
            if (previousValue != null) {
                if (mConfig.get(key).equals(previousValue)) {
                    isChanged = false;
                }
            }

            // the value of the parameter has changed
            if (isChanged) {
                Log.d(LOG_TAG, "Value of the parameter " + key + " has changed.");
                changedParam.add(key);
            }
        }

        // go through the old configuration and check whether any of old parameters
        // has disappeared from it
        for (String key : mPreviousConfig.keySet()) {
            if (mConfig.get(key) == null) {
                Log.d(LOG_TAG, "Value of the parameter " + key + " has changed.");
                changedParam.add(key);
            }
        }
        return changedParam;
    }

    /**
     * Check whether list of parameters includes service-impacting ones
     *
     * @param changedParam
     * @return boolean
     */
    private boolean includeServiceImpactingParam(ArrayList<String> changedParam) {

        for (String param : changedParam) {
            if (ConfigParametersNames.isServiceImpacting(param)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether list of parameters includes non-service-impacting ones
     *
     * @param changedParam
     * @return
     */
    private boolean includeUIImpactingParam(ArrayList<String> changedParam) {

        for (String param : changedParam) {
            if (ConfigParametersNames.isUIImpacting(param)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check do we have changed credentials
     *
     * @return
     */
    private boolean areCredentialsChanged() {

        String sipUserName = getCredential(SIPUSERNAME);

        if (mLastUserName == null) {
            return sipUserName != null;
        }

        return sipUserName == null || !mLastUserName.equals(sipUserName);
    }

    /**
     * Return parameter value by parameter name
     *
     * @param paramName
     * @return parameter value
     */
    public String getParamValue(String paramName) {
        return mConfig.get(paramName);
    }

    /**
     * Return parameter value by parameter name
     *
     * @param param
     * @return parameter value
     */
    public String getParam(ConfigParametersNames param) {
        return mConfig.get(param.getName());
    }

    public String getParam(@NonNull ConfigParametersNames param, boolean getDefaultOnNull) {
        if(getDefaultOnNull) {
            String result = mConfig.get(param.getName());
            return (result == null) ? param.getDefaultValue() : result;
        }
        else {
            return mConfig.get(param.getName());
        }
    }
    /**
     * Return credential value by parameter name
     *
     * @param param
     * @return credential value
     */
    public String getCredential(ConfigParametersNames param) {
        /*if (param == SIPUSERNAME)
            return null;*/
        return mCredentials.get(param.getName());
    }

    /**
     * Return values for parameters which are booleans
     *
     * @param param
     * @return
     */
    public boolean getConfigBooleanParam(ConfigParametersNames param) {

        boolean isEnable = param.getDefaultValue().equals("1");
        String isEnableValue = mConfig.get(param.getName());
        if (isEnableValue != null) {
            // only if the parameter has legal boolean value, take it into consideration
            // otherwise, stay with the default value.
            if (isEnableValue.equals("1") || isEnableValue.equals("0")) {
                isEnable = isEnableValue.equals("1");
            }
        }
        return isEnable;
    }

    /**
     * Return parameter value by parameter name
     *
     * @param param
     * @return parameter value
     */
    public String getParamValue(ConfigParametersNames param) {
        return mConfig.get(param.getName());
    }

    /**
     * Check if hold parameter is enabled
     *
     * @return
     */
    public boolean isHoldEnabled() {

        boolean holdStat = getConfigBooleanParam(ConfigParametersNames.CCBTNSTAT);
        if (!holdStat) {
            holdStat = getConfigBooleanParam(ConfigParametersNames.HOLDSTAT);
        }
        return holdStat;
    }

    /**
     * Use for getting Configuration parameter XFERSTAT state and changing mute options
     *
     * @return Boolean value of parameter XFERSTAT.
     */
    public boolean isTransferEnabled() {

        boolean transferStat = getConfigBooleanParam(ConfigParametersNames.CCBTNSTAT);

        if (!transferStat) {
            transferStat = getConfigBooleanParam(ConfigParametersNames.XFERSTAT);
        }

        return transferStat;
    }

    /**
     * Use for getting Configuration parameter CONFSTAT state and changing conference options
     *
     * @return Boolean value of parameter CONFSTAT.
     */
    public boolean isConferenceEnabled() {

        boolean conferenceStat = getConfigBooleanParam(ConfigParametersNames.CCBTNSTAT);

        if (!conferenceStat) {
            conferenceStat = getConfigBooleanParam(ConfigParametersNames.CONFSTAT);
        }

        return conferenceStat;
    }

    /**
     * Use for getting Configuration parameter MUTESTAT state and changing mute options
     *
     * @return Boolean value of parameter MUTESTAT.
     */
    public boolean isMuteEnabled() {

        boolean muteStat = getConfigBooleanParam(ConfigParametersNames.CCBTNSTAT);
        if (!muteStat) {
            muteStat = getConfigBooleanParam(ConfigParametersNames.MUTESTAT);
        }

        return muteStat;
    }

    /**
     * Check if video is enabled
     *
     * @returnonly using Config
     */
    public boolean isVideoEnabled() {

        return getConfigBooleanParam(ConfigParametersNames.ENABLE_VIDEO);
    }

    /**
     * Use for getting Configuration parameter ENABLE_VIDEO state
     *
     * @return String value of parameter ENABLE_VIDEO.
     */
    public String isVideonabled() {

        String enableVideoStat = "1";
        String enableVideoStatValue = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getParamValue(ConfigParametersNames.ENABLE_VIDEO);
        if (enableVideoStatValue != null) {
            enableVideoStat = enableVideoStatValue;
        }
        Log.e(LOG_TAG, "state " + enableVideoStat);
        return enableVideoStat;
    }

    public boolean isEmergencyNumber(String number) {

        if (getCallService() == null)
            return true;

        if ((getCallService().getEmergencyNumbers() != null) && getCallService().getEmergencyNumbers().contains(number)) {
            Log.d(LOG_TAG, number + " is emergency");
            return true;
        }

        Log.d(LOG_TAG, number + " is NOT emergency");
        return false;
    }

    /**
     * Obtaining user agent instance id from {@link SharedPreferences}
     * @return String with requested data
     */
    private String getUserAgentInstanceID() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (!preferences.contains(USER_AGENT_INSTANCE_ID)) {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(USER_AGENT_INSTANCE_ID, Utils.uniqueDeviceUUID().toString());
            editor.apply();
        }
        return preferences.getString(USER_AGENT_INSTANCE_ID, "");
    }


    //The following method is a workaround, since CSDK hasn't committed a fix
    //There is a leak in MediaServiceInstance that is caesed by remaing receiver in MediaServiceInstace
    //This method calls onAlarmFired that unregister the receiver and remove it.
    private void clearMediaServicesInstance() {

        mClient.getMediaEngine().close();
        MediaServicesInstance mediaInstance = mClient.getMediaEngine();
        try {
            Field field = MediaServicesInstance.class.getDeclaredField("mAlarms");
            field.setAccessible(true);
            Object value = field.get(mediaInstance);
            Map<Long, PendingIntent> alarmMap = (Map<Long, PendingIntent>)value;
            if (alarmMap == null)
                Log.d(LOG_TAG, "Could not get the alarmMap.");
            else {
                Log.d(LOG_TAG, "alarmMap is " + (alarmMap.isEmpty() ? "empty" : "not empty"));

                //Class<?>[] onAlarmFiredTypes = new Class[] {long.class, long.class};
                //Method onAlarmFiredMethod = mediaInstance.getClass().getMethod("onAlarmFired", onAlarmFiredTypes);
                for ( Long key : alarmMap.keySet()) {
                    Log.d(LOG_TAG, "onAlarmFired");
                    //onAlarmFiredMethod.invoke(mediaInstance, new Object[]{key, 0});
                    mediaInstance.onAlarmFired(key,0);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException /*| NoSuchMethodException /*| InvocationTargetException*/ e) {
            e.printStackTrace();
        }
    }

    public boolean isLogoutInProgress() {
        return mLogoutInProgress;
    }

    public void setLogoutInProgress(boolean logoutInProgress) {
        mLogoutInProgress = logoutInProgress;
    }


    /**
     * Sets Client log level with the LOGLOCAL value read from config file
     */
    public void setLogLevel() {
        String logLevel = getParam(LOGLOCAL, true);
        Client.LogLevel clientLogLevel = mapLogLocalToCsdkLoglevel.get(logLevel);
        Log.d(LOG_TAG, "configuration LOGLOCAL="+logLevel+" translated to Client.LogLevel="+clientLogLevel);


        Client.LogLevel oldLogLevel = Client.getLogLevel();

        if (getConfigBooleanParam(LOG_VERBOSITY) && oldLogLevel != Client.LogLevel.DEBUG) {
            Client.setLogLevel(Client.LogLevel.DEBUG);
        }
        else if (oldLogLevel != Client.LogLevel.ERROR){
            Client.setLogLevel(Client.LogLevel.ERROR);
        }

        if (clientLogLevel == oldLogLevel) {
            Log.w(LOG_TAG, "setLogLevel: no need to change log level");
            return;
        }

        if (clientLogLevel != null) {
            Client.setLogLevel(clientLogLevel);
        }


        Client.setLogListener(new Client.LogListener() {
            @Override
            public void onLogMessage(Client.LogLevel level, String tag, String msg) {
                Log.d(LOG_TAG, level + "[" + tag + "]:" + msg);
            }
        });
    }
}
