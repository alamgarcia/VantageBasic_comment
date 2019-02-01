package com.avaya.android.vantage.basic.model;

/**
 * Used for contact phone editing.
 */
public class EditablePhoneNumber {

    private String number;
    private ContactData.PhoneType type;
    private boolean primary;
    private String phoneNumberId;

    /**
     * Constructor
     *
     * @param number  phone number
     * @param type    phone type from {@link ContactData}
     * @param primary is phone number primary?
     * @param phoneId phone number ID
     */
    public EditablePhoneNumber(String number, ContactData.PhoneType type, boolean primary, String phoneId) {
        this.number = number;
        this.type = type;
        this.primary = primary;
        this.phoneNumberId = phoneId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public ContactData.PhoneType getType() {
        return type;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setType(ContactData.PhoneType type) {
        this.type = type;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
