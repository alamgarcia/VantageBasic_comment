package com.avaya.android.vantage.basic.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.avaya.android.vantage.basic.R;
import com.avaya.android.vantage.basic.tools.FastScrollRecyclerSection;

/*
* Copyright 2017 MyInnos
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

public class FastScrollRecyclerView extends RecyclerView {

    private FastScrollRecyclerSection mScroller = null;
    private GestureDetector mGestureDetector = null;
    private static final String TAG = "FastScrollRecyclerView";
    public static int setIndexTextSize = 12;
    public static float mIndexbarWidth = 20;
    public static float mIndexbarMargin = 5;
    public static int mPreviewPadding = 5;
    public static int mIndexBarCornerRadius = 5;
    public static float mIndexBarTransparentValue = (float) 0.6;
    public static String mIndexbarBackgroudColor = "#000000";
    public static String mIndexbarTextColor = "#FFFFFF";
    public static boolean mEnableAlphaBar = true;
    public static float mFontSize = 200;

    public FastScrollRecyclerView(Context context) {
        super(context);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public void setmSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
        mScroller.setmSwipeRefresh(swipeRefreshLayout);
    }

    private void init(Context context, AttributeSet attrs) {

        mScroller = new FastScrollRecyclerSection(context, this);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FastScrollRecyclerView, 0, 0);

            if (typedArray != null) {
                try {
                    setIndexTextSize = typedArray.getInt(R.styleable.FastScrollRecyclerView_setIndexTextSize, setIndexTextSize);
                    mIndexbarWidth = typedArray.getFloat(R.styleable.FastScrollRecyclerView_setIndexbarWidth, mIndexbarWidth);
                    mIndexbarMargin = typedArray.getFloat(R.styleable.FastScrollRecyclerView_setIndexbarMargin, mIndexbarMargin);
                    mPreviewPadding = typedArray.getInt(R.styleable.FastScrollRecyclerView_setPreviewPadding, mPreviewPadding);
                    mIndexBarCornerRadius = typedArray.getInt(R.styleable.FastScrollRecyclerView_setIndexBarCornerRadius, mIndexBarCornerRadius);
                    mIndexBarTransparentValue = typedArray.getFloat(R.styleable.FastScrollRecyclerView_setIndexBarTransparentValue, mIndexBarTransparentValue);

                    if (typedArray.getString(R.styleable.FastScrollRecyclerView_setIndexBarColor) != null) {
                        mIndexbarBackgroudColor = typedArray.getString(R.styleable.FastScrollRecyclerView_setIndexBarColor);
                    }

                    if (typedArray.getString(R.styleable.FastScrollRecyclerView_setIndexBarTextColor) != null) {
                        mIndexbarTextColor = typedArray.getString(R.styleable.FastScrollRecyclerView_setIndexBarTextColor);
                    }

                } finally {
                    typedArray.recycle();
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Overlay index bar
        if (mScroller != null && mEnableAlphaBar) {
            mScroller.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Intercept ListView's touch event
        if (mEnableAlphaBar) {
            if (mScroller != null && mScroller.onTouchEvent(ev))
                return true;

            if (mGestureDetector == null) {
                mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        return super.onFling(e1, e2, velocityX, velocityY);
                    }

                });
            }
            mGestureDetector.onTouchEvent(ev);
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getX() < 60 && mEnableAlphaBar) {
            return mScroller.contains(ev.getX(), ev.getY()) || super.onInterceptTouchEvent(ev);
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        if (mScroller != null) {
            mScroller.setAdapter(adapter);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mScroller != null && mEnableAlphaBar) {
            mScroller.onSizeChanged(w, h, oldw, oldh);
        }
    }

    /**
     * @param value int to set the text size of the index bar
     */
    public void setIndexTextSize(int value) {
        mScroller.setIndexTextSize(value);
    }

    /**
     * @param value float to set the width of the index bar
     */
    public void setIndexbarWidth(float value) {
        mScroller.setIndexbarWidth(value);
    }

    /**
     * enable or disable alpha bar
     */
    public void setAlphaBarEnabled (boolean enabled){
        mEnableAlphaBar = enabled;
    }

    /**
     * set alpha bar preview font size
     */
    public void setPreviewFontSize (float fontSize){
        mFontSize = fontSize;
    }

    /**
     * @param value float to set the margin of the index bar
     */
    public void setIndexbarMargin(float value) {
        mScroller.setIndexbarMargin(value);
    }

    /**
     * @param value int to set the preview padding
     */
    public void setPreviewPadding(int value) {
        mScroller.setPreviewPadding(value);
    }

    /**
     * @param value int to set the corner radius of the index bar
     */
    public void setIndexBarCornerRadius(int value) {
        mScroller.setIndexBarCornerRadius(value);
    }

    /**
     * @param value float to set the transparency value of the index bar
     */
    public void setIndexBarTransparentValue(float value) {
        mScroller.setIndexBarTransparentValue(value);
    }

    /**
     * @param typeface Typeface to set the typeface of the preview & the index bar
     */
    public void setTypeface(Typeface typeface) {
        mScroller.setTypeface(typeface);
    }

    /**
     * @param color The color for the index bar
     */
    public void setIndexBarColor(String color) {
        mScroller.setIndexBarColor(color);
    }

    /**
     * @param color The text color for the index bar
     */
    public void setIndexBarTextColor(String color) {
        mScroller.setIndexBarTextColor(color);
    }

}