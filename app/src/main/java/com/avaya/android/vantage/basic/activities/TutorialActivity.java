package com.avaya.android.vantage.basic.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.avaya.android.vantage.basic.R;


/**
 * {@link TutorialActivity} is responsible for showing screens with simple explanation how to use
 * basic function of Vantage Basic application.
 */

public class TutorialActivity extends AppCompatActivity {

    private int currentImage = 0;
    private static final int TOTAL_IMAGES = 6;
    private static final String CURRENT_PAGE = "current_page";

    private final static int[] imageTitleStrings = {R.string.quick_tutorial, R.string.dial_pad,
            R.string.call_screen, R.string.contacts, R.string.call_feature, R.string.recents};

    private FrameLayout mTutorialFrame;

    private ImageView mImageView;
    private TextView mImageTitle, mLeftButton, mRightButton, mDoneButton;

    private View mFirstPage, mSecondPage, mThirdPage, mForthPage, mFifthPage, mSixtPage;

    private TextView mTempText1, mTempText2, mTempText3, mTempText4, mTempText5;

    private int darkGrey;

    //Swipe detection based values
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_activity_layout);
        //hideSystemUI();

        mTutorialFrame = (FrameLayout) findViewById(R.id.tutorial_frame_layout);

        mImageView = (ImageView) findViewById(R.id.tutorial_image_view);
        mImageTitle = (TextView) findViewById(R.id.tutorial_image_title);
        mLeftButton = (TextView) findViewById(R.id.tutorial_left);
        mRightButton = (TextView) findViewById(R.id.tutorial_right);
        mDoneButton = (TextView) findViewById(R.id.tutorial_done);

        //temporary TextViews used to show different description on each tutorial page
        mTempText1 = (TextView) findViewById(R.id.empty_text_1);
        mTempText2 = (TextView) findViewById(R.id.empty_text_2);
        mTempText3 = (TextView) findViewById(R.id.empty_text_3);
        mTempText4 = (TextView) findViewById(R.id.empty_text_4);
        mTempText5 = (TextView) findViewById(R.id.empty_text_5);

        //Already a View, no need to cast
        mFirstPage = findViewById(R.id.tutorial_first_image);
        mSecondPage = findViewById(R.id.tutorial_second_image);
        mThirdPage = findViewById(R.id.tutorial_third_image);
        mForthPage = findViewById(R.id.tutorial_forth_image);
        mFifthPage = findViewById(R.id.tutorial_fifth_image);
        mSixtPage = findViewById(R.id.tutorial_sixt_image);

        showImageAt(currentImage);
        updateImageTitle(currentImage);
        updateButtonTitle(currentImage);

        setupSwipeDetector();

        //Set color resources based on Android version
        if (Build.VERSION.SDK_INT >= 23) {
            darkGrey = getResources().getColor(R.color.colorDarkGrey, getTheme());
        } else {
            darkGrey = this.getColor(R.color.colorDarkGrey);
        }

        //Set page indicator only after set color
        setPageIndicator(currentImage);

        mRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextButtonPressed();
            }
        });

        mLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousButtonPressed();
            }
        });

        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideTutorial();
            }
        });
    }

    /**
     * Setting up swipe detection for specified WebView
     */
    private void setupSwipeDetector() {
        final GestureDetector swipeDetector = new GestureDetector(getApplicationContext(), new GestureListener());
        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                swipeDetector.onTouchEvent(event);
                return true;
            }
        });
    }

    /**
     * Updating title of buttons depending of image index shown
     * @param imageIndex
     */
    private void updateButtonTitle(int imageIndex) {
        try {
            if (imageIndex == 0) {                          // First page
                mLeftButton.setText(R.string.skip_button);
                mRightButton.setText(R.string.next);
                mDoneButton.setVisibility(View.INVISIBLE);
            } else if (imageIndex == TOTAL_IMAGES - 1) {      // Last page
                mLeftButton.setText(R.string.back);
                mRightButton.setText(R.string.done);
                mDoneButton.setVisibility(View.INVISIBLE);
            } else {                                        // Others
                mLeftButton.setText(R.string.back);
                mRightButton.setText(R.string.next);
                mDoneButton.setVisibility(View.VISIBLE);
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }


    /**
     * Close tutorial
     */
    private void hideTutorial() {
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_PAGE, currentImage);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            currentImage = savedInstanceState.getInt(CURRENT_PAGE);
            setPageIndicator(currentImage);
            showImageAt(currentImage);
            updateButtonTitle(currentImage);
            updateImageTitle(currentImage);
        }
    }

    /**
     * Update title of image shown on specified position
     * @param currentImage
     */
    private void updateImageTitle(int currentImage) {
        mImageTitle.setText(imageTitleStrings[currentImage]);
    }

    /**
     * Check what action have to be performed on Next button pressed
     */
    private void nextButtonPressed() {
        if (currentImage == TOTAL_IMAGES-1) {
            //finish tutorial
            hideTutorial();
        } else {
            showNextImage();
        }
    }

    /**
     * Check what action have to be performed on Previous button pressed
     */
    private void previousButtonPressed() {
        if (currentImage == 0) {
            //finish tutorial
            hideTutorial();
        } else {
            showPreviousImage();
        }
    }

    /**
     * Move to previous image or finalize
     */
    private void showPreviousImage() {
        currentImage --;
        showImageAt(currentImage);
        updateButtonTitle(currentImage);
        setPageIndicator(currentImage);
        updateImageTitle(currentImage);
    }

    /**
     * Move to next image or finalize activity
     */
    private void showNextImage() {
        currentImage ++;
        showImageAt(currentImage);
        updateButtonTitle(currentImage);
        setPageIndicator(currentImage);
        updateImageTitle(currentImage);
    }

    /**
     * Update screen with appropriate data and set position to each TextView.
     * @param imageIndex tutorial page number.
     */
    private void showImageAt(int imageIndex) {
        try {
            switch (imageIndex) {
                case 0:
                    mTempText1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 27);
                    mTempText1.setText(getString(R.string.tutorial_welcome));
                    mTempText1.setGravity(Gravity.CENTER_HORIZONTAL);
                    mTempText1.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    mTempText1.setPadding(0, 125, 0, 0);

                    mTempText2.setText(getString(R.string.tutorial_optional_handset));
                    mTempText2.setPadding(30, 190, 0, 0);

                    mTempText3.setVisibility(View.VISIBLE);
                    mTempText3.setText(getString(R.string.tutorial_stand_angle));
                    mTempText3.setPadding(600, 770, 0, 0);

                    mTempText4.setVisibility(View.VISIBLE);
                    mTempText4.setText(getString(R.string.tutorial_volume));
                    mTempText4.setPadding(540, 935, 0, 0);

                    mTempText5.setVisibility(View.INVISIBLE);

                    mImageView.setImageResource(R.drawable.tutorial_1);
                    break;
                case 1:
                    mTempText1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
                    mTempText1.setGravity(Gravity.NO_GRAVITY);
                    mTempText1.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    mTempText1.setText(getString(R.string.tutorial_tap_access));
                    mTempText1.setPadding(100, 142, 350, 0);

                    mTempText2.setText(getString(R.string.tutorial_block));
                    mTempText2.setPadding(40, 960, 0, 0);

                    mTempText3.setVisibility(View.VISIBLE);
                    mTempText3.setText(getString(R.string.tutorial_mute));
                    mTempText3.setPadding(250, 960, 370, 0);

                    mTempText4.setVisibility(View.VISIBLE);
                    mTempText4.setText(getString(R.string.tutorial_tap_handsfree));
                    mTempText4.setPadding(450, 960, 0, 0);

                    mTempText5.setVisibility(View.INVISIBLE);

                    mImageView.setImageResource(R.drawable.tutorial_2);
                    break;
                case 2:
                    mTempText1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
                    mTempText1.setGravity(Gravity.NO_GRAVITY);
                    mTempText1.setText(getString(R.string.tutorial_tap_back));
                    mTempText1.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    mTempText1.setPadding(40, 115, 355, 0);

                    mTempText2.setText(getString(R.string.tutorial_tap_advanced));
                    mTempText2.setPadding(435, 125, 0, 0);

                    mTempText3.setVisibility(View.INVISIBLE);
                    mTempText4.setVisibility(View.INVISIBLE);

                    mTempText5.setVisibility(View.INVISIBLE);

                    mImageView.setImageResource(R.drawable.tutorial_3);
                    break;
                case 3:
                    mTempText1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
                    mTempText1.setGravity(Gravity.NO_GRAVITY);
                    mTempText1.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    mTempText1.setText(getString(R.string.tutorial_sync_contacts));
                    mTempText1.setPadding(175, 142, 0, 0);

                    mTempText2.setText(getString(R.string.tutorial_add_new));
                    mTempText2.setPadding(556, 167, 0, 0);

                    mTempText3.setVisibility(View.VISIBLE);
                    mTempText3.setText(getString(R.string.tutorial_tap_row));
                    mTempText3.setPadding(50, 960, 250, 0);

                    mTempText4.setVisibility(View.VISIBLE);
                    mTempText4.setText(getString(R.string.tutorial_tap_default));
                    mTempText4.setPadding(532, 960, 20, 0);

                    mTempText5.setVisibility(View.INVISIBLE);

                    mImageView.setImageResource(R.drawable.tutorial_5);
                    break;
                case 4:
                    mTempText1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
                    mTempText1.setGravity(Gravity.NO_GRAVITY);
                    mTempText1.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    mTempText1.setText(getString(R.string.tutorial_add_participant));
                    mTempText1.setPadding(145, 110, 350, 0);

                    mTempText2.setText(getString(R.string.tutorial_transfer_call));
                    mTempText2.setPadding(535, 85, 20, 0);

                    mTempText3.setVisibility(View.VISIBLE);
                    mTempText3.setText(getString(R.string.tutorial_merge_call));
                    mTempText3.setPadding(50, 1040, 250, 0);

                    mTempText4.setVisibility(View.VISIBLE);
                    mTempText4.setText(getString(R.string.tutorial_hold_and_start_new_one));
                    mTempText4.setPadding(180, 968, 260, 0);

                    mTempText5.setVisibility(View.VISIBLE);
                    mTempText5.setText(getString(R.string.tutorial_escalate_video));
                    mTempText5.setPadding(532, 975, 20, 0);

                    mImageView.setImageResource(R.drawable.tutorial_4);
                    break;
                case 5:
                    mTempText1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
                    mTempText1.setGravity(Gravity.NO_GRAVITY);
                    mTempText1.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    mTempText1.setText(getString(R.string.tutorial_bring_focus));
                    mTempText1.setPadding(165, 142, 0, 0);

                    mTempText2.setText(getString(R.string.tutorial_sync_recent));

                    if(mTempText2.getText().toString().length() > 40)
                        mTempText2.setPadding(550, 90, 0, 0);
                    else
                        mTempText2.setPadding(550, 167, 0, 0);

                    mTempText3.setVisibility(View.VISIBLE);
                    mTempText3.setText(getString(R.string.tutorial_tap_to_call));
                    mTempText3.setPadding(556, 960, 0, 0);

                    mTempText4.setVisibility(View.INVISIBLE);

                    mTempText5.setVisibility(View.INVISIBLE);

                    mImageView.setImageResource(R.drawable.tutorial_6);
                    break;
                default:
                    break;
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    /**
     * Set color of indicator for selected page
     * @param position of indicator
     */
    private void setPageIndicator(int position){
        position++;
        switch (position){
            case 1:
                mFirstPage.setEnabled(true);
                mSecondPage.setEnabled(false);
                mThirdPage.setEnabled(false);
                mForthPage.setEnabled(false);
                mFifthPage.setEnabled(false);
                mSixtPage.setEnabled(false);
                break;
            case 2:
                mFirstPage.setEnabled(false);
                mSecondPage.setEnabled(true);
                mThirdPage.setEnabled(false);
                mForthPage.setEnabled(false);
                mFifthPage.setEnabled(false);
                mSixtPage.setEnabled(false);
                break;
            case 3:
                mFirstPage.setEnabled(false);
                mSecondPage.setEnabled(false);
                mThirdPage.setEnabled(true);
                mForthPage.setEnabled(false);
                mFifthPage.setEnabled(false);
                mSixtPage.setEnabled(false);
                break;
            case 4:
                mFirstPage.setEnabled(false);
                mSecondPage.setEnabled(false);
                mThirdPage.setEnabled(false);
                mForthPage.setEnabled(true);
                mFifthPage.setEnabled(false);
                mSixtPage.setEnabled(false);
                break;
            case 5:
                mFirstPage.setEnabled(false);
                mSecondPage.setEnabled(false);
                mThirdPage.setEnabled(false);
                mForthPage.setEnabled(false);
                mFifthPage.setEnabled(true);
                mSixtPage.setEnabled(false);
                break;
            case 6:
                mFirstPage.setEnabled(false);
                mSecondPage.setEnabled(false);
                mThirdPage.setEnabled(false);
                mForthPage.setEnabled(false);
                mFifthPage.setEnabled(false);
                mSixtPage.setEnabled(true);
                break;
            default:
                mFirstPage.setEnabled(true);
                mSecondPage.setEnabled(false);
                mThirdPage.setEnabled(false);
                mForthPage.setEnabled(false);
                mFifthPage.setEnabled(false);
                mSixtPage.setEnabled(false);
                break;
        }
    }

    /**
     * Set window to immersive mode.
     */
    private void hideSystemUI() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Simple Gesture listener which allow to us to incorporate custom
     * behaviour on different swipes
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Horizontal swipes
            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                //Right to left swipe
                nextButtonPressed();
                return false;
            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                //Left to right swipe
                previousButtonPressed();
                return false;
            }

            //Vertical swipes
            if(e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                //Bottom to top swipe
                return false;
            }  else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                //Top to bottom swipe
                return false;
            }
            return false;
        }
    }
}
