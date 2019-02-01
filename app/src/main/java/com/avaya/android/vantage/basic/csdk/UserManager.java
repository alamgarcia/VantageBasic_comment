package com.avaya.android.vantage.basic.csdk;

import android.content.ComponentName;
import android.content.Intent;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import com.avaya.android.vantage.basic.ElanApplication;
import com.avaya.clientservices.client.CreateUserCompletionHandler;
import com.avaya.clientservices.client.UserCreatedException;
import com.avaya.clientservices.common.SignalingServerGroup;
import com.avaya.clientservices.credentials.Challenge;
import com.avaya.clientservices.credentials.CredentialCompletionHandler;
import com.avaya.clientservices.credentials.CredentialProvider;
import com.avaya.clientservices.credentials.UserCredential;
import com.avaya.clientservices.user.User;
import com.avaya.clientservices.user.UserAdminChangeType;
import com.avaya.clientservices.user.UserAdminEventListener;
import com.avaya.clientservices.user.UserConfiguration;
import com.avaya.deskphoneservices.DeskPhoneServiceType;
import com.avaya.deskphoneservices.ServiceStatus;

import static com.avaya.android.vantage.basic.csdk.ConfigParametersNames.SIPDOMAIN;

public class UserManager {

    /**
     * Logout Signal intent sent by BrioLoginService received by TEC
     * This triggers login by TEC
     */
    public static final String LOGOUT_SIGNAL = "com.avaya.endpoint.action.LOGOUT_SIGNAL";

    private final String LOG_TAG = this.getClass().getSimpleName();

    public User getmUser() {
        return mUser;
    }

    public void setmUser(User mUser) {
        this.mUser = mUser;
    }

    public enum UserState {
        /**
         * There is currently no User object.
         */
        NO_USER,
        /**
         * A new User object is being created.
         */
        CREATING_USER,
        /**
         * There is currently a User object.
         */
        HAVE_USER,
        /**
         * Removing the current user to make way for a new one.
         */
        REMOVING_USER_FOR_CHANGE,
        /**
         * The client is being shut down, so expect the user to be removed.
         */
        SHUTTING_DOWN
    }

    private UserState mState = UserState.NO_USER;

    private User mUser;

    /**
     * if the user should be recreated after current operation completes.
     */
    private boolean forceRecreateUser;

    /**
     * Get {@link UserState}
     *
     * @return {@link UserState}
     */
    public UserState getState() {
        return mState;
    }

    /**
     * Setting {@link UserState}
     *
     * @param newState
     */
    public void setState(UserState newState) {
        mState = newState;
    }

    /**
     * Changing {@link UserState}
     *
     * @param newState
     */
    private void changeState(UserState newState) {
        if (newState != mState) {
            Log.d(LOG_TAG, "Changing state from {" + mState + "} to {" + newState + "}");
            mState = newState;
        }
    }

    /**
     * Perform user operation based on {@link UserState}
     */
    public void createUser() {

        Log.d(LOG_TAG, "create user");

        switch (mState) {
            case NO_USER:
                startCreatingUser();
                break;
            case HAVE_USER:
                replaceUser();
                break;
            case REMOVING_USER_FOR_CHANGE:
                Log.d(LOG_TAG, "Ignoring request to create a user in state " + mState);
                break;
            case CREATING_USER:
                Log.d(LOG_TAG, "createUser() called in " + mState + " state. User will be recreated after current " +
                        "operation completes");
                forceRecreateUser = true;
                break;
            case SHUTTING_DOWN:
                // do nothing: either the user will be created after shutting down or it shall not be created at all
                Log.d(LOG_TAG, "createUser in SHUTTING_DOWN state is ignored.");
                break;
            default:
                throw new IllegalStateException("Shouldn't call createUser() in state " + mState);
        }
    }

    /**
     * Start process of user creation
     */
    private void startCreatingUser() {

        Log.d(LOG_TAG, "startCreatingUser");

        changeState(UserState.CREATING_USER);

        final UserConfiguration sdkUserConfiguration = SDKManager.getInstance().getDeskPhoneServiceAdaptor().getSdkUserConfiguration();
        SDKManager.getInstance().getClient().createUser(sdkUserConfiguration,
                new CreateUserCompletionHandler() {
                    @Override
                    public void onSuccess(User user) {
                        onUserCreated(user);
                    }

                    @Override
                    public void onError(UserCreatedException e) {
                        onUserCreationFailure(e);
                    }
                });
    }

    /**
     * When {@link User} is created performing further action
     *
     * @param user
     */
    private void onUserCreated(User user) {
        Log.d(LOG_TAG, "SDK created user " + user.getUserId() + " (forceRecreateUser= " + forceRecreateUser + ")");
        setmUser(user);
        if (forceRecreateUser) {
            forceRecreateUser = false;
            replaceUser();
        } else {
            changeState(UserState.HAVE_USER);
            SDKManager.getInstance().getDeskPhoneServiceAdaptor().onUserCreated();
            user.start();
        }

    }

    /**
     * In case of user creation failure and if forceRecreateUser is true will try to
     * create user again
     *
     * @param e
     */
    private void onUserCreationFailure(UserCreatedException e) {
        Log.w(LOG_TAG, "User creation failed: {}" + e.getFailureReason());
        if (forceRecreateUser) {
            forceRecreateUser = false;
            startCreatingUser();
        } else {
            changeState(UserState.NO_USER);
            SDKManager.getInstance().getDeskPhoneServiceAdaptor().onUserCreationFailure(e);
        }
    }

    /**
     * Replacing user and starting in SDKManager process of removing
     */
    private void replaceUser() {
        Log.d(LOG_TAG, "replaceUser");
        changeState(UserState.REMOVING_USER_FOR_CHANGE);
        try {
            SDKManager.getInstance().getClient().removeUser(getmUser(), true);
        }
        catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "replaceUser: remove User failed");
        }
    }

    /**
     * Removing current user and start of creating of new user if mState is
     * equal to REMOVING_USER_FOR_CHANGE. If mState is equal to SHUTTING_DOWN
     * we are changing state to NO_USER.
     *
     * @param user {@link User}
     */
    public void onUserRemoved(User user) {

        Log.d(LOG_TAG, "onUserRemoved");

        setmUser(null);
        if (mState == UserState.REMOVING_USER_FOR_CHANGE) {
            startCreatingUser();
        } else if (mState == UserState.SHUTTING_DOWN) {
            changeState(UserState.NO_USER);
        } else {
            Log.e(LOG_TAG, "onUserRemoved() called in unexpected state " + mState);
        }
    }

    /**
     * Shut down the User management.
     *
     * @return true on success, false on failure
     */
    public boolean shutdown() {
        Log.d(LOG_TAG, "shutdown in state " + mState);
        switch (mState) {
            case HAVE_USER:
            case REMOVING_USER_FOR_CHANGE:
                changeState(UserState.SHUTTING_DOWN);
                // No need to start user removal here, since it'll be triggered
                // from inside the SDK.
                break;
            case NO_USER:
            case SHUTTING_DOWN:
                // Ignore, because nothing to do
                return false;
            default:
                Log.d(LOG_TAG, "Shouldn't call shutdown() in state " + mState);
                //throw new IllegalStateException("Shouldn't call shutdown() in state " + mState);
                return false;
        }

        return true;
    }

}
