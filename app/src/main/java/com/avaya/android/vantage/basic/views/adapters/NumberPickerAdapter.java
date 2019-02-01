package com.avaya.android.vantage.basic.views.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.model.ContactData;

import java.util.ArrayList;
import java.util.List;

/**
 * This adapter is used to control contact phone picker used when we want to add another participant
 * to the call or transfer the call to another contact
 */

public class NumberPickerAdapter extends ArrayAdapter<ContactData> {
    private static final String TAG = "ContactDetailsPhoneList";

    private ContactData mContactData;
    private boolean phonesUpdated = false;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private List<String> newPhoneNumbers = new ArrayList<>();
    private List<ContactData.PhoneNumber> mPhoneNumbers;

    public NumberPickerAdapter(Context context, ContactData contactData, List<ContactData.PhoneNumber> phoneNumbers) {
        super(context, R.layout.contact_picker_phone_layout);
        mContactData = contactData;
        this.mPhoneNumbers = phoneNumbers;
    }

    @Override
    public int getCount() {
        if (mContactData != null && !phonesUpdated) {
            return mPhoneNumbers.size();
        } else if (phonesUpdated && mContactData != null) {
            return newPhoneNumbers.size();
        } else {
            Log.d(TAG, "no phone numbers connected to account");
            return 0;
        }
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        final NumberPickerAdapter.ViewHolder viewHolder;
        if (convertView == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.contact_picker_phone_layout, parent, false);
            // initialize the view holder
            viewHolder = new NumberPickerAdapter.ViewHolder();
            viewHolder.phoneIcon = (ImageView) convertView.findViewById(R.id.picker_phone_icon);
            viewHolder.phoneType = (TextView) convertView.findViewById(R.id.contact_picker_phone_type);
            viewHolder.phoneNumber = (TextView) convertView.findViewById(R.id.contact_picker_phone_number);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (NumberPickerAdapter.ViewHolder) convertView.getTag();
        }

        // making sure we get data from correct list
        if (!phonesUpdated) {
            ContactData.PhoneNumber phoneNumber = mPhoneNumbers.get(position);
            //Fill up data
            if (phoneNumber.Primary) {
                viewHolder.phoneIcon.setImageResource(R.drawable.ic_call_grey_default24);
            } else {
                viewHolder.phoneIcon.setImageResource(R.drawable.ic_call_grey24);
            }
            viewHolder.phoneType.setText(getPhoneType(phoneNumber.Type));
            viewHolder.phoneNumber.setText(phoneNumber.Number);
        } else {
            viewHolder.phoneNumber.setText(newPhoneNumbers.get(position));
        }
        return convertView;
    }

    /**
     * Get user friendly name for phone type
     *
     * @param type ContactData.PhoneType enum value
     * @return String representing ContactData.PhoneType
     */
    private String getPhoneType(ContactData.PhoneType type) {
        if (ContactData.PhoneType.WORK.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_work).toString();
        } else if (ContactData.PhoneType.MOBILE.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_mobile).toString();
        } else if (ContactData.PhoneType.HOME.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_home).toString();
        } else if (ContactData.PhoneType.HANDLE.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_handle).toString();
        } else if (ContactData.PhoneType.FAX.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_fax).toString();
        } else if (ContactData.PhoneType.PAGER.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_pager).toString();
        } else if (ContactData.PhoneType.ASSISTANT.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_assistant).toString();
        } else if (ContactData.PhoneType.OTHER.equals(type)) {
            return getContext().getResources().getText(R.string.contact_details_other).toString();
        }
        return "";
    }


    /**
     * ViewHolder class
     */
    private static class ViewHolder {
        ImageView phoneIcon;
        TextView phoneType;
        TextView phoneNumber;
    }

}
