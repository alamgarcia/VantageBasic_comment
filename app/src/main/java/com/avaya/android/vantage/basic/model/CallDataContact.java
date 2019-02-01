package com.avaya.android.vantage.basic.model;

import java.util.ArrayList;
import java.util.List;

/**
 * An object representation of CallDataContact
 */

public class CallDataContact {

    private String mNativeDisplayName;
    private String mNativeFirstName;
    private String mNativeLastName;
    private String mPhoneNumber;
    private String mEmail;
    private boolean mHawPicture;
    private byte[] mPictureData;
    private boolean mIsFavorite;
    private String mLocation;
    private String mCity;
    private String mTitle;
    private String mCompany;
    private String mUniqueAddressForMatching;
    private String mExtraField;
    private List<ContactData.PhoneNumber> mPhones;
    private ContactData.Category mCategory;
    private ArrayList<String> mListOfParticipants;

    public String getmNativeDisplayName() {
        return mNativeDisplayName;
    }

    public void setmNativeDisplayName(String mNativeDisplayName) {
        this.mNativeDisplayName = mNativeDisplayName;
    }

    public String getmNativeFirstName() {
        return mNativeFirstName;
    }

    public void setmNativeFirstName(String mNativeFirstName) {
        this.mNativeFirstName = mNativeFirstName;
    }

    public String getmNativeLastName() {
        return mNativeLastName;
    }

    public void setmNativeLastName(String mNativeLastName) {
        this.mNativeLastName = mNativeLastName;
    }

    public String getmPhoneNumber() {
        return mPhoneNumber;
    }

    public void setmPhoneNumber(String mPhoneNumber) {
        this.mPhoneNumber = mPhoneNumber;
    }

    public String getmEmail() {
        return mEmail;
    }

    public void setmEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public boolean ismHawPicture() {
        return mHawPicture;
    }

    public void setmHawPicture(boolean mHawPicture) {
        this.mHawPicture = mHawPicture;
    }

    public byte[] getmPictureData() {
        return mPictureData;
    }

    public void setmPictureData(byte[] mPictureData) {
        this.mPictureData = mPictureData;
    }

    public boolean ismIsFavorite() {
        return mIsFavorite;
    }

    public void setmIsFavorite(boolean mIsFavorite) {
        this.mIsFavorite = mIsFavorite;
    }

    public String getmLocation() {
        return mLocation;
    }

    public void setmLocation(String mLocation) {
        this.mLocation = mLocation;
    }

    public String getmCity() {
        return mCity;
    }

    public void setmCity(String mCity) {
        this.mCity = mCity;
    }

    public String getmTitle() {
        return mTitle;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getmCompany() {
        return mCompany;
    }

    public void setmCompany(String mCompany) {
        this.mCompany = mCompany;
    }

    public String getmUniqueAddressForMatching() {
        return mUniqueAddressForMatching;
    }

    public void setmUniqueAddressForMatching(String mUniqueAddressForMatching) {
        this.mUniqueAddressForMatching = mUniqueAddressForMatching;
    }

    public String getmExtraField() {
        return mExtraField;
    }

    public void setmExtraField(String mExtraField) {
        this.mExtraField = mExtraField;
    }

    public List<ContactData.PhoneNumber> getmPhones() {
        return mPhones;
    }

    public void setmPhones(List<ContactData.PhoneNumber> mPhones) {
        this.mPhones = mPhones;
    }

    public ContactData.Category getmCategory() {
        return mCategory;
    }

    public void setmCategory(ContactData.Category mCategory) {
        this.mCategory = mCategory;
    }

    public ArrayList<String> getmListOfParticipants() {
        return mListOfParticipants;
    }

    public void setmListOfParticipants(ArrayList<String> mListOfParticipants) {
        this.mListOfParticipants = mListOfParticipants;
    }
}
