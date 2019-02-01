package com.avaya.android.vantage.basic.views.adapters;

import android.content.SharedPreferences;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CallLog;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.avaya.android.vantage.basic.Constants;
import com.avaya.android.vantage.basic.GoogleAnalyticsUtils;
import com.avaya.android.vantage.basic.PhotoLoadUtility;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.TimestampComparator;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.csdk.LocalContactInfo;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.basic.model.CallData;
import com.avaya.android.vantage.basic.model.CallDataContact;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.views.interfaces.IRecentCallsViewInterface;
import com.avaya.clientservices.calllog.CallLogItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.signature.StringSignature;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.avaya.android.vantage.basic.Constants.SYNC_HISTORY;
import static com.avaya.android.vantage.basic.Constants.USER_PREFERENCE;

/**
 * MyRecentsRecyclerViewAdapter is responsible for showing and properly rendering Recent Calls
 */

public class MyRecentsRecyclerViewAdapter extends RecyclerView.Adapter<MyRecentsRecyclerViewAdapter.ItemViewHolder> implements IRecentCallsViewInterface,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "RecentCallsAdapter";
    private final OnContactInteractionListener mListener;

    private List<CallData> mCategorizedContactData;
    private LinkedList<CallData> mSavedContactData;
    private Map<String, Drawable> mContactPhotoCache = new HashMap<>();
    private CallData.CallCategory mFilter = CallData.CallCategory.ALL;
    private boolean mFilteringPaired;

    private static final int SECONDS_IN_MINUTES = 60;
    private boolean mFirstNameFirst = true;
    private boolean addParticipant = false;
    private String mName;
    private List<CallData> mList;
    private Context mContext;
    private IRecentsCallback mRecentsCallback;
    private SharedPreferences mUserPreference;

    private static final String MISSED_CALL_DEFAULT_TIME = "0";
    private ArrayList<CallData> mBluetoothCallData;

    private static boolean mIsCameraSupported = Utils.isCameraSupported();

    private MyRecentsRecyclerViewAdapter.ItemRemovedListener mItemRemovedListener;

    public interface IRecentsCallback {
        void refreshBluetoothData();
        Parcelable getListPosition();
    }

    /**
     * Constructor
     *
     * @param recentCallItems list of recent calls
     * @param listener        listener used when user clicked on item
     * @param fragment        fragment
     */
    public MyRecentsRecyclerViewAdapter(List<CallData> recentCallItems, OnContactInteractionListener listener, Fragment fragment) {
        mContext = fragment.getContext();
        mListener = listener;
        mCategorizedContactData = recentCallItems;
        Collections.sort(mCategorizedContactData, new TimestampComparator());
        mSavedContactData = new LinkedList<CallData>(mCategorizedContactData);
        mBluetoothCallData = new ArrayList<>();
        mList = recentCallItems;
        this.mRecentsCallback = (IRecentsCallback) fragment;
        fragment.getActivity().getSupportLoaderManager().restartLoader(Constants.SYNC_CALLS_LOADER, null, this);
        mUserPreference = fragment.getActivity().getSharedPreferences(USER_PREFERENCE, Context.MODE_PRIVATE);
    }

    /**
     * Obtaining item view type based on position
     *
     * @param position int
     * @return int of resource file
     */
    @Override
    public int getItemViewType(int position) {
        switch (mList.get(position).mCategory) {
            case MISSED:
                return R.layout.missed_call_item;
            case INCOMING:
                return R.layout.recent_call_item;
            case OUTGOING:
                return R.layout.outgonig_call_item;
            default:
                return R.layout.outgonig_call_item;
        }
    }

    /**
     * Creating {@link ItemViewHolder} from view obtained by inflating
     *
     * @param parent   {@link ViewGroup} for which we are creating view holder
     * @param viewType
     * @return {@link ItemViewHolder}
     */
    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_call_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, final int position) {
        boolean photoSet = false;
        final CallData data = getItems(mFilter).get(position);
        holder.mItem = data;
        holder.mContactData = getContactData(data.mContact, data.mRemoteNumber);

        if (holder.mContactData != null && holder.mContactData.mPhotoThumbnailURI != null
                && holder.mContactData.mPhotoThumbnailURI.trim().length() > 0) {
            holder.mPhoto.setText("");
            Glide.clear(holder.mPhoto);
            Glide.with(holder.mPhoto.getContext())
                    .load(holder.mContactData.mPhotoThumbnailURI)
                    .asBitmap()
                    .signature(new StringSignature(holder.mContactData.mPhotoURI))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .into(new ViewTarget<TextView, Bitmap>(holder.mPhoto) {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation anim) {
                            TextView photoView = this.view;
                            if (photoView == null) {
                                return;
                            }
                            // making bitmap round
                            RoundedBitmapDrawable circularBitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(photoView.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            photoView.setBackground(circularBitmapDrawable);
                        }
                    });
            photoSet = true;

        } else {
            setInitials(holder.mPhoto, holder.mItem.mName);
        }

        if (holder.mContactData == null) {
            mName = "";
            if(data.mContact != null) {
                List<String> participants = data.mContact.getmListOfParticipants();
                if (participants != null) {
                    for (int i = 0; i < participants.size(); i++) {
                        if (!participants.get(i).equalsIgnoreCase("")) {
                            mName = participants.get(i);
                        } else {
                            mName = data.mName;
                        }
                    }
                } else {
                    mName = data.mName;
                }
            }
            holder.mName.setText(mName);
            holder.mPhoneType.setText("-");
            if(holder.mName.getText().toString().isEmpty()) {
                holder.mName.setText(data.mPhone);
            }
            setInitials(holder.mPhoto, holder.mItem.mName);
        } else {
            if (!mFirstNameFirst) {
                if (!photoSet) {
                    setInitials(holder.mPhoto, holder.mContactData, holder.mItem);
                }
                holder.mName.setText(holder.mContactData.mLastName + " " + holder.mContactData.mFirstName);
                holder.mPhoneType.setText(getPhoneType(LocalContactInfo.getPhoneTypeByNumber(data.mPhone, mContext)));
            } else {
                if (!photoSet) {
                    setInitials(holder.mPhoto, holder.mContactData, holder.mItem);
                }
                if (holder.mContactData.mCategory == ContactData.Category.IPO) {
                    holder.mName.setText(holder.mContactData.mFirstName);
                } else
                    holder.mName.setText(holder.mContactData.mFirstName + " " + holder.mContactData.mLastName);
                holder.mPhoneType.setText(getPhoneType(LocalContactInfo.getPhoneTypeByNumber(data.mPhone, mContext)));
            }
        }

        holder.mCallState.setImageResource(getCallStateIconResources(data));
        String callDate = holder.mCallDate.getContext().getString(R.string.recent_call_at, getSimpleDateString(data.mCallDate), getTimeString(holder.mCallAudio.getContext(), data.mCallTime));
        holder.mCallDate.setText(callDate);

        setDurationTimeString(holder, data);
        setTextColorBaseOnColorState(holder, data);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null && !addParticipant) {
                    // using handler to delay showing of a fragment a little bit and displaying ripple effect
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //noinspection ConstantConditions
                            if (null != mListener) {
                                // Notify the active callbacks interface (the activity, if the
                                // fragment is attached to one) that an item has been selected.
                                if (holder.mContactData != null) {
                                    mListener.onContactsFragmentInteraction(holder.mContactData);
                                } else {
                                    List<ContactData.PhoneNumber> phones = new ArrayList<>();
                                    phones.add(new ContactData.PhoneNumber(holder.mItem.mRemoteNumber, ContactData.PhoneType.WORK, false, null));
                                    mListener.onContactsFragmentInteraction(new ContactData(holder.mItem.mName, holder.mItem.mName, "", null, false, "", "", "",
                                            "", phones, ContactData.Category.ALL, holder.mItem.mName, "", "", true, "", "", "", "", ""));
                                }
                                mListener.saveSelectedCallCategory(mFilter);
                                mListener.onPositionToBeSaved(mRecentsCallback.getListPosition());
                            }
                        }
                    }, 100);
                }
            }
        });

        holder.mCallAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    if (holder.mContactData != null) {
                        mListener.onCallContactAudio(holder.mContactData, data.mPhone);
                    } else {
                        ArrayList<ContactData.PhoneNumber> phone = generatePhoneNumbersList(holder.mItem);
                        mListener.onCallContactAudio(new ContactData(holder.mItem.mName, "", "", null, true, "", "", "",
                                "", phone, ContactData.Category.ALL, holder.mItem.mName, "", "", true, "", "", "", "", ""), data.mPhone);
                    }
                }
                //Call from History main page
                GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_HISTORY_EVENT);
            }
        });

        if(holder.mAddParticipant.getVisibility() != View.VISIBLE) {
            setVideoCallButtonState(holder);
            setAudioCallButtonState(holder);
        }

        holder.mCallVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    if (holder.mContactData != null) {
                        mListener.onCallContactVideo(holder.mContactData, data.mPhone);
                    } else {
                        ArrayList<ContactData.PhoneNumber> phone = generatePhoneNumbersList(holder.mItem);
                        mListener.onCallContactVideo(new ContactData(holder.mItem.mName, "", "", null, true, "", "", "",
                                "", phone, ContactData.Category.ALL, holder.mItem.mName, "", "", true, "", "", "", "", ""), data.mPhone);
                    }
                }
            }
        });
        holder.mAddParticipant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.mContactData != null && holder.mContactData.mPhones!=null && holder.mContactData.mPhones.size()>0) {
                    mListener.onCallAddParticipant(holder.mContactData);
                } else {
                    ArrayList<ContactData.PhoneNumber> phone = generatePhoneNumbersList(holder.mItem);
                    mListener.onCallAddParticipant(new ContactData(holder.mItem.mName, "", "", null, true, "", "", "",
                            "", phone, ContactData.Category.ALL, holder.mItem.mName, "", "", true, "", "", "", "", ""));
                }
                setAddParticipant(false);
                notifyDataSetChanged();
            }
        });
    }

    /**
     * Processing view recycling event
     *
     * @param holder {@link ItemViewHolder}
     */
    @Override
    public void onViewRecycled(ItemViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.mPhoto != null) {
            Glide.clear(holder.mPhoto);
        }
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * should update the contents of the {@link} to reflect the item at
     * the given position.
     * <p>
     * Note that unlike {@link ListView}, RecyclerView will not call this method
     * again if the position of the item changes in the data set unless the item itself is
     * invalidated or the new position cannot be determined. For this reason, you should only
     * use the <code>position</code> parameter while acquiring the related data item inside
     * this method and should not keep a copy of it. If you need the position of an item later
     * on (e.g. in a click listener), use {@link} which will
     * have the updated adapter position.
     * <p>
     * Partial bind vs full bind:
     * <p>
     * The payloads parameter is a merge list from {@link #notifyItemChanged(int, Object)} or
     * {@link #notifyItemRangeChanged(int, int, Object)}.  If the payloads list is not empty,
     * the ViewHolder is currently bound to old data and Adapter may run an efficient partial
     * update using the payload info.  If the payload is empty,  Adapter must run a full bind.
     * Adapter should not assume that the payload passed in notify methods will be received by
     * onBindViewHolder().  For example when the view is not attached to the screen, the
     * payload in notifyItemChange() will be simply dropped.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     * @param payloads A non-null list of merged payloads. Can be empty list if requires full
     */
    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position, List<Object> payloads) {
        if (payloads != null && payloads.size() > 0) {
            if (payloads.get(0) instanceof RoundedBitmapDrawable) {
                holder.mPhoto.setBackground((RoundedBitmapDrawable) payloads.get(0));
                holder.mPhoto.setText("");
            }
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    /**
     * Set mFirstNameFirst boolean which represent do we set on first place
     * last mName or first mName when we are showing full mName.
     *
     * @param firstNameFirst boolean
     */
    public void firstNameFirst(boolean firstNameFirst) {
        mFirstNameFirst = firstNameFirst;
    }

    /**
     * From provided {@link CallLogItem} return {@link ContactData} associated with it.
     *
     * @param remoteNumber of required {@link ContactData}
     * @param contact      {@link CallDataContact} which provide us with relevant data
     * @return contact data item containing all contact information.
     */
    private ContactData getContactData(CallDataContact contact, String remoteNumber) {
        ContactData contactData = null;

        if (contact != null && contact.getmPhones() != null) {
            Log.d(TAG, "Name: " + contact.getmNativeDisplayName() + " hasPictures: " + contact.ismHawPicture()
                    + " isFavorite: " + contact.ismIsFavorite() + " Location: " + contact.getmLocation()
                    + " City: " + contact.getmCity() + " Title: " + contact.getmTitle()
                    + " Company: " + contact.getmCompany() + " UniqueAddress: " + contact.getmUniqueAddressForMatching());

            String nativeURI;
            nativeURI = contact.getmExtraField();
            String photoURI = "";
            String photoThumbnailURI = "";
            if (nativeURI != null && !nativeURI.isEmpty()) {
                String[] photoData = LocalContactInfo.getContactPhotoURI(Uri.parse(nativeURI), mContext);
                photoThumbnailURI = photoData[0];
                photoURI = photoData[1];
            } else {
                nativeURI = "";
            }

            contactData = new ContactData(
                    contact.getmNativeDisplayName(),
                    contact.getmNativeFirstName(),
                    contact.getmNativeLastName(),
                    contact.ismHawPicture() ? contact.getmPictureData() : null,
                    contact.ismIsFavorite(),
                    contact.getmLocation(),
                    contact.getmCity(),
                    contact.getmTitle(),
                    contact.getmCompany(),
                    contact.getmPhones(),
                    contact.getmCategory(),
                    contact.getmUniqueAddressForMatching(),
                    nativeURI,
                    photoThumbnailURI,
                    true,
                    contact.getmEmail(),
                    photoURI,
                    "",
                    "", "");
        } else {
            String[] info = LocalContactInfo.getContactInfoByNumber(remoteNumber, mContext);
            String name = info[0];
            String firstName = info[1];
            String lastName = info[2];
            String photoThumbnailURI = info[3];
            String contactID = info[4];
            String contactURI = info[5];
            String photoURI = info[6];
            String accountType = info[7];
            if (photoThumbnailURI == null) photoThumbnailURI = "";
            if (name != null && name.trim().length() > 0) {
                List<ContactData.PhoneNumber> phones;
                if (contact == null) {
                    phones = new ArrayList<ContactData.PhoneNumber>();
                    phones.add(new ContactData.PhoneNumber(remoteNumber, ContactData.PhoneType.WORK, true, "0"));
                } else {
                    phones = contact.getmPhones();
                }
                contactData = new ContactData(name, firstName, lastName, null, false, "", "", "", "", phones,
                        accountType.equals(Constants.IPO_CONTACT_TYPE) ? ContactData.Category.IPO : ContactData.Category.LOCAL, contactID, contactURI, photoThumbnailURI, true, "", photoURI, "", "", "");
            } else {
                contactData = null;
            }
        }
        return contactData;
    }

    /**
     * From provided duration String which represent duration of call in milliseconds
     * and is parsed in proper format as H:MM:SS
     *
     * @param duration String representation of call duration in milliseconds
     * @return String representation of properly formatted time
     */
    private String getDurationTimeString(String duration) {
        String result = "";
        int intDuration = 0;
        if (duration != null) {
            intDuration = Integer.parseInt(duration);
        }


        int seconds = intDuration % SECONDS_IN_MINUTES;
        int minutes = (intDuration - seconds) / SECONDS_IN_MINUTES;

        if (minutes < 10) {
            result = result + "0" + minutes;
        } else {
            result = result + minutes;
        }

        if (seconds < 10) {
            result = result + ":0" + seconds;
        } else {
            result = result + ":" + seconds;
        }

        return result;
    }


    /**
     * Setting color of missed call to red and all others to primary color
     *
     * @param holder contact data item
     * @param data   call data information
     */
    private void setTextColorBaseOnColorState(ItemViewHolder holder, CallData data) {
        if (data.mCategory == CallData.CallCategory.MISSED) {
            holder.mName.setTextColor(mContext.getColor(R.color.presenceRed));
            holder.mCallDate.setTextColor(mContext.getColor(R.color.presenceRed));
            holder.mCallDuration.setTextColor(mContext.getColor(R.color.presenceRed));
            holder.mPhoneType.setTextColor(mContext.getColor(R.color.presenceRed));
        } else {
            holder.mName.setTextColor(mContext.getColor(R.color.primary));
            holder.mCallDate.setTextColor(mContext.getColor(R.color.secondary));
            holder.mCallDuration.setTextColor(mContext.getColor(R.color.secondary));
            holder.mPhoneType.setTextColor(mContext.getColor(R.color.primary));
        }
    }

    /**
     * Updating filter category and calling update of data set
     *
     * @param category {@link CallData.CallCategory}
     */
    public void setFilter(CallData.CallCategory category) {
        mFilter = category;
        //TODO: check if we can notify in a smarter way on a range of change
        notifyDataSetChanged();
    }

    /**
     * Clearing filter settings and updating data set
     */
    public void clearFilter() {
        mFilter = CallData.CallCategory.ALL;
        notifyDataSetChanged();
    }

    /**
     * Update call icon
     *
     * @param data call data item
     * @return icon to display
     */
    private int getCallStateIconResources(CallData data) {
        //TODO add icons for calls from paired device
        if(data.isFromPaired) {
            if (data.mCategory == CallData.CallCategory.INCOMING) {
                return R.drawable.ic_sync_incoming_grey;
            } else if (data.mCategory == CallData.CallCategory.OUTGOING) {
                return R.drawable.ic_sync_outgoing_grey;
            }
            return R.drawable.ic_sync_missed;
        } else {
            if (data.mCategory == CallData.CallCategory.INCOMING) {
                return R.drawable.ic_recents_audio_incoming;
            } else if (data.mCategory == CallData.CallCategory.OUTGOING) {
                return R.drawable.ic_recents_audio_outgoing;
            }
            return R.drawable.ic_recents_audio_missed;
        }
    }

    /**
     * Obtaining list of item based on filtered category
     *
     * @param category {@link CallData.CallCategory} based on which we are filtering list
     * @return filtered list of {@link CallData}
     */
    private List<CallData> getItems(CallData.CallCategory category) {

        if (category == CallData.CallCategory.ALL) {
            if(ismFilteringPaired()) {
                mSavedContactData.clear();
                for (CallData data : mCategorizedContactData) {
                    if (!data.isFromPaired) {
                        mSavedContactData.add(data);
                    }
                }
                return mSavedContactData;
            } else {
                return mCategorizedContactData;
            }
        } else {
            //noinspection ConstantConditions
            if (category != CallData.CallCategory.ALL && category != CallData.CallCategory.DELETE) {
                mSavedContactData.clear();
                for (CallData data : mCategorizedContactData) {
                    if (data.mCategory.equals(category)) {
                        if(ismFilteringPaired()){
                            if(!data.isFromPaired) {
                                mSavedContactData.add(data);
                            }
                        } else {
                            mSavedContactData.add(data);
                        }
                    }
                }
                return mSavedContactData;
            } else {
                mCategorizedContactData.clear();
                mSavedContactData.clear();
                return new ArrayList<>();
            }
        }
    }

    /**
     * Setting # in colored circle if contact is not in the contact list
     *
     * @param photo View in which initials will be set
     * @param name  Name for which we are preparing initials
     */
    private void setInitials(TextView photo, String name) {
        int colors[] = photo.getResources().getIntArray(R.array.material_colors);
        photo.setBackgroundResource(R.drawable.empty_circle);
        ((GradientDrawable) photo.getBackground().mutate()).setColor(colors[Math.abs(name.hashCode() % colors.length)]);
        photo.setText("#");
    }

    /**
     * Set initials for contact. In case {@link ContactData} have photo url we we use it.
     * If there is no url we will create initial view from mName of contact
     *
     * @param photo       {@link TextView} in which we are showing initials
     * @param contactData {@link ContactData} from which we are taking data
     * @param callData    {@link CallData}
     */
    private void setInitials(final TextView photo, ContactData contactData, CallData callData) {
        String name;
        if (contactData.mPhoto != null && contactData.mPhoto.length > 0) {
            photo.setText("");
            Drawable drawable = mContactPhotoCache.get(contactData.mUUID);
            if (drawable != null) {
                photo.setBackground(drawable);
            } else {
                PhotoLoadUtility.setPhotoAsBackground(photo.getContext(), contactData, this, callData);
            }
            return;
        }
        mContactPhotoCache.remove(contactData.mUUID);
        name = contactData.mName;
        int colors[] = photo.getResources().getIntArray(R.array.material_colors);
        photo.setBackgroundResource(R.drawable.empty_circle);
        ((GradientDrawable) photo.getBackground().mutate()).setColor(colors[Math.abs(name.hashCode() % colors.length)]);

        String initials;
        String firstNameLetter;
        String lastNameLetter;

        if (contactData.mFirstName != null && contactData.mFirstName.trim().length() > 0) {
            firstNameLetter = String.valueOf(contactData.mFirstName.trim().charAt(0));
        } else {
            firstNameLetter = "";
        }

        if (contactData.mLastName != null && contactData.mLastName.trim().length() > 0) {
            lastNameLetter = String.valueOf(contactData.mLastName.trim().charAt(0));
        } else {
            lastNameLetter = "";
        }

        if (mFirstNameFirst) {
            initials = firstNameLetter + lastNameLetter;
        } else {
            initials = lastNameLetter + firstNameLetter;
        }

        photo.setText(initials.toUpperCase());

    }

    /**
     * Helper method for providing proper date string in format Today, Yesterday or exact date
     *
     * @param dateString String representation of date
     * @return String representing value required
     */
    private String getSimpleDateString(String dateString) {
        String result = "";
        SimpleDateFormat from = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzzzzzzzz yyyy", Locale.ENGLISH);

        Date date;

        try {
            date = from.parse(dateString);
            long now = System.currentTimeMillis();
            result = DateUtils.getRelativeTimeSpanString(date.getTime(), now, DateUtils.DAY_IN_MILLIS).toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    private ArrayList<ContactData.PhoneNumber> generatePhoneNumbersList (CallData callData) {
        ArrayList<ContactData.PhoneNumber> phone = new ArrayList<ContactData.PhoneNumber>();
        ContactData.PhoneNumber number = new ContactData.PhoneNumber(callData.mPhone, ContactData.PhoneType.WORK, true, null);
        phone.add(number);
        return phone;
    }

    /**
     * Helper method for providing proper time string in format hour:minute AM/PM
     *
     * @param dateString String representation of date
     * @return String representing value required as time
     */
    private String getTimeString(Context context, String dateString) {
        String result = "";
        SimpleDateFormat fromDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzzzzzzzz yyyy", Locale.ENGLISH);
        java.text.DateFormat toDate = android.text.format.DateFormat.getTimeFormat(context.getApplicationContext());
        Date dateNow;
        try {
            dateNow = fromDate.parse(dateString);
            result = toDate.format(dateNow);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Filling up {@link #mContactPhotoCache} with provided data
     *
     * @param mUUID                  String representing unique id of drawable
     * @param circularBitmapDrawable {@link RoundedBitmapDrawable}
     */
    @Override
    public void cacheContactDrawable(String mUUID, RoundedBitmapDrawable circularBitmapDrawable) {
        mContactPhotoCache.put(mUUID, circularBitmapDrawable);
    }

    /**
     * Check if photo for provided unique id cached
     *
     * @param uuid String of photo to be checked
     * @return boolean providing us with info if photo is cached
     */
    @Override
    public boolean isPhotoCached(String uuid) {
        return mContactPhotoCache.containsKey(uuid);
    }

    /**
     * Returning total item count in {@link #mFilter} selected
     *
     * @return int count number
     */
    @Override
    public int getItemCount() {
        try {
            return getItems(mFilter).size();
        }catch (NullPointerException e){
            e.printStackTrace();
            return 0;
        }
    }

    public int getItemCountWithFilter(CallData.CallCategory mFilter) {
        try {
            return getItems(mFilter).size();
        }catch (NullPointerException e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Recreating and categorizing data from provided {@link CallData} list
     *
     * @param items {@link CallData} list
     */
    @Override
    public void recreateData(List<CallData> items) {
        mCategorizedContactData.clear();
        mCategorizedContactData.addAll(items);
        mCategorizedContactData.addAll(mBluetoothCallData);
        Collections.sort(mCategorizedContactData, new TimestampComparator());
        notifyDataSetChanged();
    }

    @Override
    public void updateItem(CallData item) {

    }

    @Override
    public void removeItem(int position) {

    }

    @Override
    public void removeItem(CallData contact) {

    }

    @Override
    public void addItem(CallData item) {

    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        String[] PROJECTION_BLUETOOTH_SYNCED_DATA = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };
        return new CursorLoader(mContext, CallLog.Calls.CONTENT_URI, PROJECTION_BLUETOOTH_SYNCED_DATA, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == Constants.SYNC_CALLS_LOADER) {
            loadBluetoothData(cursor);
        }
    }

    /**
     * Load synced bluetooth data from paired device
     *
     * @param cursor Cursor used to load bluetooth data from paired device
     */
    private void loadBluetoothData(Cursor cursor) {
        String bluetoothSyncStated = mUserPreference.getString(SYNC_HISTORY, Utils.SyncState.SYNC_OFF.getStateName());
        mBluetoothCallData.clear();
        while (cursor != null && cursor.moveToNext()) {
            addPairedDeviceCallData(cursor);
        }
        mCategorizedContactData.addAll(mBluetoothCallData);
        Collections.sort(mCategorizedContactData, new TimestampComparator());
        if (mBluetoothCallData.size() > 0 && bluetoothSyncStated.equals(Utils.SyncState.SYNC_ON.getStateName())) {
            setmFilteringPaired(false);
        } else {
            setmFilteringPaired(true);
        }
        notifyDataSetChanged();
        mRecentsCallback.refreshBluetoothData();
    }

    /**
     * Add synced bluetooth data from paired device
     *
     * @param cursor Cursor used to load bluetooth data from paired device to the array list
     */
    private void addPairedDeviceCallData(Cursor cursor) {
        String phoneNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
        int callType = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
        String callDate = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
        Date callDayTime = new Date(Long.valueOf(callDate));
        String callDuration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION));
        CallData.CallCategory callCategory = null;
        switch (callType) {
            case CallLog.Calls.OUTGOING_TYPE:
                callCategory = CallData.CallCategory.OUTGOING;
                break;

            case CallLog.Calls.INCOMING_TYPE:
                callCategory = CallData.CallCategory.INCOMING;
                break;

            case CallLog.Calls.MISSED_TYPE:
                callCategory = CallData.CallCategory.MISSED;
                break;
            default:
                callCategory = CallData.CallCategory.INCOMING;
                break;
        }

        mBluetoothCallData.add(new CallData(phoneNumber,
                callCategory,
                callDayTime.toString(),
                callDayTime.getTime(),
                callDayTime.toString(),
                callDuration, phoneNumber,
                "", "", null, phoneNumber, true, false));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public int getIndexOf(CallData item) {
        return mSavedContactData.indexOf(item);
    }

    public boolean ismFilteringPaired() {
        return mFilteringPaired;
    }

    public void setmFilteringPaired(boolean mFilteringPaired) {
        this.mFilteringPaired = mFilteringPaired;
    }

    public void setCallHistoryList(List<CallData> recentCallItems) {
        mList = recentCallItems;
        mCategorizedContactData = recentCallItems;
        Collections.sort(mCategorizedContactData, new TimestampComparator());
        mSavedContactData = new LinkedList<CallData>(mCategorizedContactData);
    }

    /**
     * ViewHolder for specific item in adapter
     */
    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
        private final View mView;
        private final TextView mPhoto;
        public final TextView mName;
        private final ImageView mCallState;
        private final TextView mCallDate;
        private final TextView mCallDuration;
        private final TextView mPhoneType;
        private final ImageButton mCallAudio;
        private final ImageButton mCallVideo;
        private CallData mItem;
        private ContactData mContactData;
        private final ImageView mAddParticipant;

        ItemViewHolder(View view) {
            super(view);
            mView = view;
            mName = (TextView) view.findViewById(R.id.recent_name);
            mPhoto = (TextView) view.findViewById(R.id.initials);
            mCallState = (ImageView) view.findViewById(R.id.recent_call_type);
            mCallDate = (TextView) view.findViewById(R.id.recent_call_date);
            mPhoneType = (TextView) view.findViewById(R.id.recent_number_type);
            mCallDuration = (TextView) view.findViewById(R.id.recent_call_duration);
            mCallAudio = (ImageButton) view.findViewById(R.id.call_audio);
            mCallVideo = (ImageButton) view.findViewById(R.id.call_video);
            mAddParticipant = (ImageView) view.findViewById(R.id.add_participant);
            if (addParticipant) {
                mCallAudio.setVisibility(View.INVISIBLE);
                mCallVideo.setVisibility(View.INVISIBLE);
                mAddParticipant.setVisibility(View.VISIBLE);
            }

            if(mContext.getResources().getBoolean(R.bool.is_landscape) == true){
                android.view.ViewGroup.LayoutParams params = mName.getLayoutParams();
                params.width = 480;
                mName.setLayoutParams(params);
            }

            view.setOnLongClickListener(this);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mName.getText() + "'";
        }

        @Override
        public boolean onLongClick(View v) {
            mItemRemovedListener.onItemClick(getAdapterPosition(), mItem);
            return true;
        }
    }

    /**
     * Set addParticipant boolean
     *
     * @param participant arr we adding another participant?
     */
    public void setAddParticipant(boolean participant) {
        addParticipant = participant;
    }

    /**
     * Return String human readable representation of {@link ContactData.PhoneType}
     *
     * @param type {@link ContactData.PhoneType}
     * @return String human readable representation of {@link ContactData.PhoneType}
     */
    private String getPhoneType(ContactData.PhoneType type) {
        if (ContactData.PhoneType.WORK.equals(type)) {
            return mContext.getResources().getText(R.string.contact_details_work).toString();
        } else if (ContactData.PhoneType.MOBILE.equals(type)) {
            return mContext.getResources().getText(R.string.contact_details_mobile).toString();
        } else if (ContactData.PhoneType.HOME.equals(type)) {
            return mContext.getResources().getText(R.string.contact_details_home).toString();
        } else if (ContactData.PhoneType.HANDLE.equals(type)) {
            return mContext.getResources().getText(R.string.contact_details_handle).toString();
        } else if (ContactData.PhoneType.FAX.equals(type)) {
            return mContext.getResources().getText(R.string.contact_details_fax).toString();
        } else if (ContactData.PhoneType.PAGER.equals(type)) {
            return mContext.getResources().getText(R.string.contact_details_pager).toString();
        } else if (ContactData.PhoneType.ASSISTANT.equals(type)) {
            return mContext.getResources().getText(R.string.contact_details_assistant).toString();
        } else if (ContactData.PhoneType.OTHER.equals(type)) {
            return mContext.getResources().getText(R.string.contact_details_other).toString();
        }
        return "";
    }

    /**
     * We should set call duration for all missed calls to 00:00.
     * Otherwise, get current call duration.
     *
     * @param holder
     * @param callData
     */
    private void setDurationTimeString(ItemViewHolder holder, CallData callData) {
        holder.mCallDuration.setText(isMissedCall(callData) ?
                getDurationTimeString(MISSED_CALL_DEFAULT_TIME) : getDurationTimeString(callData.mCallDuration));
    }

    /**
     * Check if call is missed category
     *
     * @param callData
     * @return true if call is in missed category, otherwise false.
     */
    private boolean isMissedCall(CallData callData) {
        return callData.mCategory == CallData.CallCategory.MISSED;
    }

    /**
     * Method that sets state of video call button. If camera isn't supported
     * we should hide video button. Otherwise, check if video is enabled
     * or muted and set visibility. Also, for a while, conference can not be called
     * from history.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *               item at the given position in the data set.
     */
    private void setVideoCallButtonState(ItemViewHolder holder) {
        if (!mIsCameraSupported || holder.mItem.isNonCallableConference) {
            holder.mCallVideo.setVisibility(View.INVISIBLE);
            return;
        }

        if (!SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
            holder.mCallVideo.setAlpha(0.5f);
            holder.mCallVideo.setEnabled(false);
        } else {
            holder.mCallVideo.setEnabled(true);
            holder.mCallVideo.setAlpha(1f);
        }

        holder.mCallVideo.setVisibility(View.VISIBLE);
    }

    /**
     * Method that sets state of audio call button state. For a while,
     * conference can not be called from history
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *               item at the given position in the data set.
     */
    private void setAudioCallButtonState(ItemViewHolder holder) {
        if (holder.mItem.isNonCallableConference) {
            holder.mCallAudio.setVisibility(View.INVISIBLE);
        } else {
            holder.mCallAudio.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Deletes item from the list and notifies observer.
     *
     * @param position integer value: position of the item to be removed.
     */
    public void deleteItem(int position) {
        mCategorizedContactData.remove(position);
        notifyItemRemoved(position);
    }

    public void setOnItemClickListener(MyRecentsRecyclerViewAdapter.ItemRemovedListener itemRemovedListener) {
        mItemRemovedListener = itemRemovedListener;
    }

    public interface ItemRemovedListener {
        void onItemClick(int position, CallData callData);
    }
}
