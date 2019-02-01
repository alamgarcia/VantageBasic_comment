package com.avaya.android.vantage.basic.adaptors;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.android.vantage.basic.GoogleAnalyticsUtils;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.activities.MainActivity;
import com.avaya.android.vantage.basic.csdk.DeskPhoneServiceListener;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.settings.LogoutAlertDialog;
import com.avaya.android.vantage.basic.views.interfaces.IHardButtonListener;
import com.avaya.android.vantage.basic.views.interfaces.ILoginListener;
import com.avaya.deskphoneservices.HandsetType;
import com.avaya.deskphoneservices.HardButtonType;

import java.lang.ref.WeakReference;

/**
 * {@link DeskPhoneServiceListener} adaptor to be registered in
 * {@link com.avaya.android.vantage.basic.csdk.SDKManager}
 */

public class UIDeskPhoneServiceAdaptor implements DeskPhoneServiceListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private WeakReference<ILoginListener> mLoginListener;

    private WeakReference<IHookListener> mHookListener;

    private WeakReference<IHardButtonListener> mHardButtonListener;

    private Context mContext;

    private INameExtensionVisibilityInterface setNameExtensionVisibilityListner;

    /**
     * Public constructor for {@link UIDeskPhoneServiceAdaptor}
     * @param context {@link Context}
     * @param listener {@link ILoginListener} to be set
     */
    public UIDeskPhoneServiceAdaptor(Context context, ILoginListener listener,INameExtensionVisibilityInterface setNameExtensionVisibilityListner) {
        this.mContext = context;
        this.setNameExtensionVisibilityListner = setNameExtensionVisibilityListner;
        setLoginListener(listener);
    }

    /**
     * Setting up {@link ILoginListener}
     * @param listener {@link ILoginListener} to be set
     */
    public void setLoginListener(ILoginListener listener){
        mLoginListener = new WeakReference<>(listener);
    }

    /**
     * Setting up hook listener {@link IHookListener}
     * @param listener {@link IHookListener} to be set
     */
    public void setHookListener(IHookListener listener){
        mHookListener = new WeakReference<IHookListener>(listener);
    }

    /**
     * Setting up Hard Button listener {@link IHardButtonListener}
     * @param listener {@link IHardButtonListener} to be set
     */
    public void setHardButtonListener(IHardButtonListener listener){
        mHardButtonListener = new WeakReference<IHardButtonListener>(listener);
    }


    /**
     * Sending information to {@link ILoginListener} with data provided in parameters
     * @param displayName to be sent to listener
     * @param userId to be sent to listener
     */
    @Override
    public void onUserRegistrationSuccessful(String displayName, String userId) {
        if(mLoginListener!=null && mLoginListener.get()!=null){
            mLoginListener.get().onSuccessfulLogin(displayName,userId);
        }
    }

    /**
     * Creating alert in case there are parameter changes which can impact service
     * @param withUIRefresh boolean parameter which tell us should we perform UI refresh
     */
    @Override
    public void onServiceImpactingParamChange(boolean withUIRefresh) {
        LogoutAlertDialog.showLogoutToApplyChangesDialog(withUIRefresh);
    }

    /**
     * On non service parameter change we will restart {@link MainActivity}
     */
    @Override
    public void onNonServiceImpactingParamChange() {
        Log.v(LOG_TAG, "onNonServiceImpactingParamChange");
        GoogleAnalyticsUtils.setOperationalMode(mContext);

        SDKManager.getInstance().getDeskPhoneServiceAdaptor().setLogLevel();

        if(ElanApplication.isMainActivityVisible()) {
            Intent intent = new Intent(mContext, MainActivity.class);
            intent.setAction(MainActivity.NON_SERVICE_IMPACTING_CHANGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }

    }

    /**
     * {@link IHookListener} is informed af {@link HandsetType} off hook event
     * @param handsetType {@link HandsetType}
     */
    @Override
    public void onOffHook(HandsetType handsetType) {
        if (mHookListener != null && mHookListener.get() != null)
            mHookListener.get().onOffHook(IHookListener.HandSetType.valueOf(handsetType));
    }

    /**
     * {@link IHookListener} is informed af {@link HandsetType} on hook event
     * @param handsetType {@link HandsetType}
     */
    @Override
    public void onOnHook(HandsetType handsetType) {
        if (mHookListener != null && mHookListener.get() != null)
            mHookListener.get().onOnHook(IHookListener.HandSetType.valueOf(handsetType));
    }

    @Override
    public void setNameExtensionVisibility(int extensionNameDisplayOption) {
        setNameExtensionVisibilityListner.setNameExtensionVisibility(extensionNameDisplayOption);
    }

	@Override
    public void finishAndLock() {
        //TODO: check if needed as LOGOUT intent comes from platform which might lock device by itself.
        if (!mContext.getSystemService(KeyguardManager.class).isKeyguardLocked()) {
            Toast.makeText(mContext, R.string.logged_out, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onKeyUp(HardButtonType hardButtonType) {
        if(mHardButtonListener != null && mHardButtonListener.get() != null)
            mHardButtonListener.get().onKeyUp(hardButtonType);
    }

    @Override
    public void onKeyDown(HardButtonType hardButtonType) {
        if(mHardButtonListener != null && mHardButtonListener.get() != null)
            mHardButtonListener.get().onKeyDown(hardButtonType);
    }
}
