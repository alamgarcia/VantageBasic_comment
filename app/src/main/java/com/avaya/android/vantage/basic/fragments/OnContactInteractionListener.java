package com.avaya.android.vantage.basic.fragments;

import android.graphics.Rect;
import android.os.Parcelable;

import com.avaya.android.vantage.basic.model.CallData;
import com.avaya.android.vantage.basic.model.ContactData;

/**
 * This interface must be implemented by activities that contain this
 * fragment to allow an interaction in this fragment to be communicated
 * to the activity and potentially other fragments contained in that
 * activity.
 * <p/>
 * See the Android Training lesson <a href=
 * "http://developer.android.com/training/basics/fragments/communicating.html"
 * >Communicating with Other Fragments</a> for more information.
 */
public interface OnContactInteractionListener {
    // TODO: Update argument type and name
    void onContactsFragmentInteraction(ContactData item);

    void onCallContactAudio(ContactData item, String phoneNumber);

    void onCallContactVideo(ContactData item, String phoneNumber);

    void onCreateNewContact();

    // inform Main activity that notifyDataSetChanged() has been called. And we need to check if
    // we have any contacts in the list. If not, hide the filter
    void checkFilterVisibility();

    // Inform MainActivity that search has been started.
    // Used to remove keyboard later, when necessary.
    void onStartSearching(Rect searchLayoutArea);

    void onCallAddParticipant(ContactData item);

    //Returns the Tab name (Favorites, Contacts, History) used for the access to the specific Contact details
    String getContactCapableTab();

    void saveSelectedCallCategory(CallData.CallCategory callCategory);

    void onPositionToBeSaved(Parcelable position);
}
