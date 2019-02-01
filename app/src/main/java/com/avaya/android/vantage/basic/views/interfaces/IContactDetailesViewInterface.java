package com.avaya.android.vantage.basic.views.interfaces;

import android.support.v4.graphics.drawable.RoundedBitmapDrawable;

/**
 * Interface used for contact picture caching
 */

public interface IContactDetailesViewInterface {

    void cacheContactDrawable(String mUUID, RoundedBitmapDrawable circularBitmapDrawable);

    boolean isPhotoCached(String uuid);
}
