package com.avaya.android.vantage.basic.views.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.avaya.android.vantage.basic.GoogleAnalyticsUtils;
import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.Utils;
import com.avaya.android.vantage.basic.csdk.FavoritesAdaptorListener;
import com.avaya.android.vantage.basic.csdk.LocalContactInfo;
import com.avaya.android.vantage.basic.csdk.SDKManager;
import com.avaya.android.vantage.basic.fragments.OnContactInteractionListener;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.views.interfaces.IContactsViewInterface;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.signature.StringSignature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Favorite contacts viewAdapter.
 */
public class MyFavoritesRecyclerViewAdapter extends RecyclerView.Adapter<MyFavoritesRecyclerViewAdapter.ItemViewHolder> implements IContactsViewInterface {

    private static final String TAG = "FavoritesAdapter";
    private List<ContactData> mAllFavoriteContacts = new ArrayList<>();
    private List<ContactData> mLocalFavoriteContacts = new ArrayList<>();
    private List<ContactData> mEnterpriseFavoriteContacts = new ArrayList<>();
    private final OnContactInteractionListener mListener;
    private Map<String, Drawable> mContactPhotoCache = new HashMap<>();
    private FavoritesAdaptorListener mAdapterListener;
    private boolean mFirstNameFirst = true;
    private Context mContext;
    private boolean addParticipant = false;


    // TODO One contact list observer can be reused for all fragments.

    /**
     * Constuctor
     *
     * @param items     list of favorite items
     * @param listener  contact listener used when user clicks on list item
     * @param aListener callback listener
     * @param mContext  Fragment context
     */
    public MyFavoritesRecyclerViewAdapter(List<ContactData> items, OnContactInteractionListener listener, FavoritesAdaptorListener aListener, Context mContext) {
        this.mContext = mContext;
        mAdapterListener = aListener;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isFavorite()) {
                mAllFavoriteContacts.add(items.get(i));
                if (items.get(i).mCategory == ContactData.Category.ENTERPRISE) {
                    mEnterpriseFavoriteContacts.add(items.get(i));
                } else if (items.get(i).mCategory == ContactData.Category.LOCAL) {
                    mLocalFavoriteContacts.add(items.get(i));
                }
            }
        }
        Collections.sort(mAllFavoriteContacts, new Comparator<ContactData>() {
            public int compare(ContactData o1, ContactData o2) {
                return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
            }
        });
        mListener = listener;
    }


    /**
     * Return the view type of the item at <code>position</code> for the purposes
     * of view recycling.
     * <p>
     * <p>The default implementation of this method returns 0, making the assumption of
     * a single view type for the adapter. Unlike ListView adapters, types need not
     * be contiguous. Consider using id resources to uniquely identify item view types.
     *
     * @param position position to query
     * @return integer value identifying the type of the view needed to represent the item at
     * <code>position</code>. Type codes need not be contiguous.
     */
    @Override
    public int getItemViewType(int position) {
        //return super.getItemViewType(position);
        return R.layout.favorite_list_item;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MyFavoritesRecyclerViewAdapter.ItemViewHolder holder, int position) {

        ContactData data = mAllFavoriteContacts.get(position);
        holder.mItem = data;

        setInitials(holder.mPhoto, holder.mItem);
        if (!mFirstNameFirst) {
            holder.mName.setText(data.mLastName + " " + data.mFirstName);
        } else {
            holder.mName.setText(data.mFirstName + " " + data.mLastName);
        }
        holder.mLocation.setText(data.mLocation);

        if (!data.mHasPhone) {
            holder.mCallAudio.setAlpha(0.5f);
            holder.mCallVideo.setAlpha(0.5f);
        } else {
            holder.mCallAudio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that an item has been selected.
                        holder.mItem.audioCall(mContext, mListener);
                        //Call from Favorites main page
                        GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_CONTACTS_EVENT);

                    }
                }
            });
            if (!Utils.isCameraSupported()) {
                holder.mCallVideo.setVisibility(View.GONE);
            } else {
                // Enable video
                if (!SDKManager.getInstance().getDeskPhoneServiceAdaptor().isVideoEnabled()) {
                    holder.mCallVideo.setAlpha(0.5f);
                    holder.mCallVideo.setEnabled(false);
                } else {
                    holder.mCallVideo.setEnabled(true);
                    holder.mCallVideo.setAlpha(1f);
                    holder.mCallVideo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (null != mListener) {
                                holder.mItem.videoCall(mContext, mListener);
                                //Call from Favorites main page
                                GoogleAnalyticsUtils.logEvent(GoogleAnalyticsUtils.Event.CALL_FROM_CONTACTS_EVENT);
                            }
                        }
                    });
                }
            }
            holder.mAddParticipant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that an item has been selected.
                        if (holder.mItem.mCategory == ContactData.Category.LOCAL) {
                            List<ContactData.PhoneNumber> phoneNumbers = LocalContactInfo.getPhoneNumbers(Uri.parse(holder.mItem.mURI), mContext);
                            ContactData contactItem = new ContactData(holder.mItem.mName, holder.mItem.mFirstName, holder.mItem.mLastName, null, holder.mItem.isFavorite(),
                                    holder.mItem.mLocation, holder.mItem.mCity, holder.mItem.mPosition, holder.mItem.mCompany, phoneNumbers, holder.mItem.mCategory,
                                    holder.mItem.mUUID, holder.mItem.mURI, holder.mItem.mPhotoThumbnailURI, holder.mItem.mHasPhone, holder.mItem.mEmail, holder.mItem.mPhotoURI, "", "", "");
                            mListener.onCallAddParticipant(contactItem);
                        } else {
                            mListener.onCallAddParticipant(holder.mItem);
                        }
                        setAddParticipant(false);
                        notifyDataSetChanged();
                    }
                }
            });
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {

                    // using handler to delay showing of a fragment a little bit and displaying ripple effect
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onContactsFragmentInteraction(holder.mItem);
                        }
                    }, 100);
                }
            }
        });
    }


    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * should update the contents of the {ViewHolder#itemView} to reflect the item at
     * the given position.
     * <p>
     * Note that unlike {@link ListView}, RecyclerView will not call this method
     * again if the position of the item changes in the data set unless the item itself is
     * invalidated or the new position cannot be determined. For this reason, you should only
     * use the <code>position</code> parameter while acquiring the related data item inside
     * this method and should not keep a copy of it. If you need the position of an item later
     * on (e.g. in a click listener), use {ViewHolder#getAdapterPosition()} which will
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
     * last name or first name when we are showing full name.
     *
     * @param firstNameFirst boolean
     */
    public void firstNameFirst(boolean firstNameFirst) {
        mFirstNameFirst = firstNameFirst;
    }

    /**
     * Set initials for contact. In case {@link ContactData} have photo url we we use it.
     * If there is no url we will create initial view from name of contact
     *
     * @param photo       {@link TextView} in which we are showing initials
     * @param contactData {@link ContactData} from which we are taking data
     */
    private void setInitials(final TextView photo, ContactData contactData) {
        if (contactData.mPhotoThumbnailURI != null && contactData.mPhotoThumbnailURI.length() > 0) {
            photo.setText("");
            Glide.clear(photo);
            Glide.with(photo.getContext())
                    .load(contactData.mPhotoThumbnailURI)
                    .asBitmap()
                    .signature(new StringSignature(contactData.mPhotoURI))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .into(new ViewTarget<TextView, Bitmap>(photo) {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation anim) {
                            TextView photoView = this.view;
                            if (photoView == null) {
                                return;
                            }
                            // making bitmap roundable
                            RoundedBitmapDrawable circularBitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(photoView.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            photoView.setBackground(circularBitmapDrawable);
                        }
                    });
        } else {
            mContactPhotoCache.remove(contactData.mUUID);
            String name = contactData.mName;
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
    }

    @Override
    public void onViewRecycled(ItemViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.mPhoto != null) {
            Glide.clear(holder.mPhoto);
        }
    }

    @Override
    public int getIndexOf(ContactData item) {
        return mAllFavoriteContacts.indexOf(item);
    }

    @Override
    public int getItemCount() {
        return mAllFavoriteContacts.size();
    }

    @Override
    public void recreateData(List<ContactData> items, ContactData.Category contactCategory) {
        if (contactCategory == ContactData.Category.LOCAL) {
            mLocalFavoriteContacts = items;
        }

        if (contactCategory == ContactData.Category.ENTERPRISE) {
            mEnterpriseFavoriteContacts.clear();
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isFavorite()) {
                    mEnterpriseFavoriteContacts.add(items.get(i));
                }
            }
        }

        mAllFavoriteContacts.clear();
        mAllFavoriteContacts.addAll(mLocalFavoriteContacts);
        mAllFavoriteContacts.addAll(mEnterpriseFavoriteContacts);

        Collections.sort(mAllFavoriteContacts, new Comparator<ContactData>() {
            public int compare(ContactData o1, ContactData o2) {
                return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
            }
        });

        notifyDataSetChanged();
        mAdapterListener.notifyFavoritesChanged();
    }

    @Override
    public void updateItem(ContactData item) {
        Log.i(TAG, "updateItem: " + item.mName);
        if (mAllFavoriteContacts.contains(item)) {
            notifyItemChanged(mAllFavoriteContacts.indexOf(item));
            mAdapterListener.notifyFavoritesChanged();
        }
    }

    @Override
    public void removeItem(int position) {
        mAllFavoriteContacts.remove(position);
        notifyItemRemoved(position);
        mAdapterListener.notifyFavoritesChanged();
    }

    @Override
    public void removeItem(ContactData contact) {

    }

    @Override
    public void addItem(ContactData item) {
        if (item.isFavorite()) {
            mAllFavoriteContacts.add(item);
            notifyItemInserted(mAllFavoriteContacts.size() - 1);
            mAdapterListener.notifyFavoritesChanged();
        }
    }

    @Override
    public void cacheContactDrawable(String mUUID, RoundedBitmapDrawable circularBitmapDrawable) {
        mContactPhotoCache.put(mUUID, circularBitmapDrawable);
    }

    @Override
    public boolean isPhotoCached(String uuid) {
        return mContactPhotoCache.containsKey(uuid);
    }

    /**
     * ViewHolder for recycler view adapter
     */
    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        public final TextView mName;
        private final TextView mLocation;
        private final TextView mPhoto;
        private final ImageView mCallAudio;
        private final ImageView mCallVideo;
        ContactData mItem;
        private final ImageView mAddParticipant;

        private ItemViewHolder(View view) {
            super(view);

            mView = view;
            mName = (TextView) view.findViewById(R.id.contact_name);
            mLocation = (TextView) view.findViewById(R.id.contact_location);
            mPhoto = (TextView) view.findViewById(R.id.initials);
            mCallAudio = (ImageView) view.findViewById(R.id.call_audio);
            mCallVideo = (ImageView) view.findViewById(R.id.call_video);
            mAddParticipant = (ImageView) view.findViewById(R.id.add_participant);
            if (addParticipant) {
                android.view.ViewGroup.LayoutParams params = mLocation.getLayoutParams();
                params.width = 296;
                mLocation.setLayoutParams(params);
                mCallAudio.setVisibility(View.INVISIBLE);
                mCallVideo.setVisibility(View.INVISIBLE);
                mAddParticipant.setVisibility(View.VISIBLE);
            }

            if(mContext.getResources().getBoolean(R.bool.is_landscape) == true){
                android.view.ViewGroup.LayoutParams params = mLocation.getLayoutParams();
                params.width = 484;
                mLocation.setLayoutParams(params);
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mName.getText() + "'";
        }
    }

    /**
     * Setting up is view open for adding participant from join conference call
     *
     * @param add are we adding participant
     */
    public void setAddParticipant(boolean add) {
        addParticipant = add;
    }
}
