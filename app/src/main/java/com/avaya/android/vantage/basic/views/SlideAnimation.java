package com.avaya.android.vantage.basic.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

/**
 * This class creates sliding animation for elements (view's)
 * Example of how to use the class ("audioMenu" will represent element we need to show or hide using animation)
 * 1. slideAnimation.reDrawListener(audioMenu); - use this in OnCreate method
 * 2. to show element using animation: slideAnimation.expand(audioMenu);
 * 3. hide element using animation: slideAnimation.collapse(audioMenu);
 * IMPORTANT: instantiate this animation class every time you need to assign animation to the element, otherwise, second element will not work.
 */

public class SlideAnimation {

    private static final long ANIMATION_SPEED = 200;
    private ValueAnimator mAnimator;
    private static final String TAG = "SlideAnimation";

    /**
     * making sure position is correct
     *
     * @param view element we need to animate
     */
    public void reDrawListener(final View view) {

        view.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {

                    @Override
                    public boolean onPreDraw() {
                        view.getViewTreeObserver()
                                .removeOnPreDrawListener(this);
                        view.setVisibility(View.GONE);

                        final int widthSpec = View.MeasureSpec.makeMeasureSpec(
                                0, View.MeasureSpec.UNSPECIFIED);
                        final int heightSpec = View.MeasureSpec
                                .makeMeasureSpec(0,
                                        View.MeasureSpec.UNSPECIFIED);
                        view.measure(widthSpec, heightSpec);

                        mAnimator = slideAnimator(0,
                                view.getMeasuredHeight(), view);
                        return true;
                    }
                });
    }

    /**
     * expand using animation
     *
     * @param view element we need to animate
     */
    public void expand(View view,boolean isLandScape) {
        // set Visible
        view.setVisibility(View.VISIBLE);

        mAnimator.setDuration(ANIMATION_SPEED);
        if(!isLandScape)
            mAnimator.start();
    }

    /**
     * collapse using animation
     *
     * @param view element we need to animate
     */
    public void collapse(final View view,boolean isLandScape) {

        int finalHeight = view.getHeight();
        ValueAnimator mAnimator = slideAnimator(finalHeight, 0, view);
        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        mAnimator.setDuration(ANIMATION_SPEED);
        if(!isLandScape) {
            mAnimator.start();
        }else{
            view.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * setting up animation
     *
     * @param start start position
     * @param end   end position
     * @param view  element we need to animate
     * @return ValueAnimator preferences
     */
    private ValueAnimator slideAnimator(int start, int end, final View view) {

        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                // Updating Height
                int value = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = view
                        .getLayoutParams();
                layoutParams.height = value;
                view.setLayoutParams(layoutParams);
            }
        });
        return animator;
    }

}
