package com.avaya.android.vantage.basic.model;

import android.support.annotation.NonNull;

import com.avaya.android.vantage.basic.R;

/**
 * An Object Representing CallData
 */

public class CallData {

    // enum used for call category.
    public enum CallCategory {
        ALL("All History", R.string.recent_call_all), MISSED("Missed Calls", R.string.recent_call_missed),
        OUTGOING("Outgoing Calls", R.string.recent_call_outgoing), INCOMING("Incoming Calls", R.string.recent_call_incoming),
        DELETE("Delete All History", R.string.recent_call_delete_history);
        String mName;
        int mId;

        CallCategory(String name, int resource_id) {
            mName = name;
            mId = resource_id;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    public final String mName;
    public final CallCategory mCategory;
    public final String mCallDate;
    public final long mCallDateTimestamp;
    public final String mCallTime;
    public final String mCallDuration;
    public final String mPhone;
    private final String mURI;
    private final String mPhotoThumbnailURI;
    public final CallDataContact mContact;
    public final String mRemoteNumber;
    public final boolean isFromPaired;
    public final boolean isNonCallableConference;

    public CallData(
            String name,
            @NonNull CallCategory category,
            String date,
            long timestamp,
            String time,
            String duration,
            String phone,
            String uri,
            String photoThumbnailURI,
            CallDataContact contact,
            String remoteNumber,
            boolean isFromPaired,
            boolean isNonCallableConference) {
        this.mName = name;
        this.mCategory = category;
        this.mCallDate = date;
        this.mCallDateTimestamp = timestamp;
        this.mCallTime = time;
        this.mCallDuration = duration;
        this.mPhone = phone;
        this.mURI = uri;
        this.mPhotoThumbnailURI = photoThumbnailURI;
        this.mContact = contact;
        this.mRemoteNumber = remoteNumber;
        this.isFromPaired = isFromPaired;
        this.isNonCallableConference = isNonCallableConference;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(mName);
        builder.append(mCategory);
        builder.append(mCallDate);
        builder.append(mCallTime);
        builder.append(mCallDuration);
        builder.append(mPhone);
        return builder.toString();
    }

    /**
     * If the phoneNumber argument contains '@', the part after this character
     * including '@' will be removed.
     *
     * @param phoneNumber String that represents a phone number
     * @return String phoneNumber
     */
    public static String parsePhone(String phoneNumber) {
        if (phoneNumber!=null && phoneNumber.length() > 1 && phoneNumber.indexOf("@") > 1) {
            phoneNumber = phoneNumber.substring(0, phoneNumber.indexOf("@"));
        }
        return phoneNumber;
    }
    /**
     * Method used to generate dummy call data
     *
     * @return dummy call data
     */
    public static CallData getDummyContactForPendingUpdate() {
        return new CallData("", CallCategory.INCOMING, "", 0, "", "", "", "", "", null, "", false, false);
    }
}
