package com.avaya.android.vantage.basic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.avaya.android.vantage.basic.model.CallData;
import com.avaya.android.vantage.basic.model.ContactData;
import com.avaya.android.vantage.basic.views.adapters.MyContactsRecyclerViewAdapter;
import com.avaya.android.vantage.basic.views.adapters.MyFavoritesRecyclerViewAdapter;
import com.avaya.android.vantage.basic.views.adapters.MyRecentsRecyclerViewAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

/**
 * Class used to load and cache photos
 */

public class PhotoLoadUtility {

    private static final String LOG_TAG = PhotoLoadUtility.class.getSimpleName();

    /**
     * Setting up circle photo
     *
     * @param contactData {@link ContactData}
     * @param photo       {@link View} in which photo have to be set
     * @param width       of photo
     * @param heigth      of photo
     */
    static public void setPhoto(ContactData contactData, final View photo, int width, int heigth) {
        if (photo instanceof TextView) {
            Glide.clear(photo);
            Glide.with(photo.getContext()).load(contactData.mPhoto).asBitmap().dontAnimate().into(new SimpleTarget<Bitmap>(width, heigth) {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(photo.getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    photo.setBackground(circularBitmapDrawable);
                }
            });
        }
    }

    /**
     * Set photo as background
     *
     * @param context     {@link Context}
     * @param contactData {@link ContactData}
     * @param adapter     {@link MyContactsRecyclerViewAdapter}
     */
    static public void setPhotoAsBackground(final Context context, final ContactData contactData, final MyContactsRecyclerViewAdapter adapter) {

        Glide.with(context).load(contactData.mPhoto).asBitmap().dontAnimate().into(new SimpleTarget<Bitmap>(40, 40) {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                RoundedBitmapDrawable circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                circularBitmapDrawable.setCircular(true);
                if (!adapter.isPhotoCached(contactData.mUUID)) {
                    adapter.cacheContactDrawable(contactData.mUUID, circularBitmapDrawable);
                }
                adapter.notifyItemChanged(adapter.getIndexOf(contactData), circularBitmapDrawable);
            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                Log.e(LOG_TAG, "error loading drawable", e);
                super.onLoadFailed(e, errorDrawable);
            }
        });
    }

    /**
     * Set photo as background
     *
     * @param context     {@link Context}
     * @param contactData {@link ContactData}
     * @param adapter     {@link MyContactsRecyclerViewAdapter}
     */
    static public void setPhotoAsBackground(final Context context, final ContactData contactData, final MyFavoritesRecyclerViewAdapter adapter) {

        Glide.with(context).load(contactData.mPhoto).asBitmap().dontAnimate().into(new SimpleTarget<Bitmap>(40, 40) {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                RoundedBitmapDrawable circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                circularBitmapDrawable.setCircular(true);
                if (!adapter.isPhotoCached(contactData.mUUID)) {
                    adapter.cacheContactDrawable(contactData.mUUID, circularBitmapDrawable);
                    adapter.notifyItemChanged(adapter.getIndexOf(contactData));
                }
            }
        });
    }

    /**
     * Set photo as background
     *
     * @param context     {@link Context}
     * @param contactData {@link ContactData}
     * @param adapter     {@link MyRecentsRecyclerViewAdapter}
     * @param callData    {@link CallData}
     */
    static public void setPhotoAsBackground(final Context context, final ContactData contactData, final MyRecentsRecyclerViewAdapter adapter, final CallData callData) {

        Glide.with(context).load(contactData.mPhoto).asBitmap().dontAnimate().into(new SimpleTarget<Bitmap>(40, 40) {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                RoundedBitmapDrawable circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                circularBitmapDrawable.setCircular(true);
                if (!adapter.isPhotoCached(contactData.mUUID)) {
                    adapter.cacheContactDrawable(contactData.mUUID, circularBitmapDrawable);
                    adapter.notifyItemChanged(adapter.getIndexOf(callData));
                }
            }
        });
    }
}
