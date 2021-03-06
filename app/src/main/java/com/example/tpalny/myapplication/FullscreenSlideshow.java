package com.example.tpalny.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class FullscreenSlideshow extends AppCompatActivity {


    private static final int MAX_NUM_OF_ATTEMPTS = 10;
    protected static TextView mText;
    protected static ViewFlipper mViewFlipper;
    private SharedPreferences settings;
    protected static int heightPixels;
    protected static int widthPixels;
    protected static ImageView imageView1;
    protected static ImageView imageView2;
    protected static ImageView imageView3;
    protected static int i = 0;
    private final ScheduledExecutorService textScheduler =
            Executors.newScheduledThreadPool(1);
    protected static final ScheduledExecutorService imageScheduler =
            Executors.newScheduledThreadPool(1);
    protected static final ScheduledExecutorService picsRefreshScheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> textUpdateHandle;
    protected static ScheduledFuture<?> pictureDisplayHandle;
    protected static ScheduledFuture<?> picsFileRefreshHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen_slideshow);
        settings = getSharedPreferences("com.example.tpalny.myapplication_preferences", Context.MODE_PRIVATE);
        mText = (TextView) findViewById(R.id.my_text);
        mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);

        imageView1 = (ImageView) findViewById(R.id.im1);
        imageView2 = (ImageView) findViewById(R.id.im2);
        imageView3 = (ImageView) findViewById(R.id.im3);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        heightPixels = metrics.heightPixels;
        widthPixels = metrics.widthPixels;


        //checking if the device has navigation bar at the bottom before hiding it
        //boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        boolean hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey();
        if (!hasMenuKey) {
            // Do whatever you need to do, this device has a navigation bar
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        int x = 0;


        if (Select_Folders.isSlideShowWithText) {

            new ReadTextFile().execute();
            Runnable textUpdate = new Runnable() {
                @Override
                public void run() {
                    new SearchTask(FullscreenSlideshow.this, false, true).execute();
                    new ReadTextFile().execute();
                }
            };
            Integer rate = Integer.parseInt(Select_Folders.textFileRefreshRate.getText().toString());
            textUpdateHandle = textScheduler.scheduleAtFixedRate(textUpdate, rate, rate, TimeUnit.MINUTES);
        }
        startTimer();

    }


    public void startTimer() {
        Integer delay = Integer.parseInt(Select_Folders.slideShowDelay.getText().toString());
        Integer picsRefreshTime = Integer.parseInt(Select_Folders.pictureFileRefreshRate.getText().toString());
        Runnable imageDisplay = new Runnable() {
            @Override
            public void run() {
                new DisplayImage(FullscreenSlideshow.this).execute();
            }
        };

        Runnable picturesRefresh = new Runnable() {
            @Override
            public void run() {
                new SearchTask(FullscreenSlideshow.this, true, false).execute();
            }
        };

        //Timer for changing between picstures on the screen
        pictureDisplayHandle = imageScheduler.scheduleAtFixedRate(imageDisplay, 0, delay, TimeUnit.SECONDS);
        //Timer for updating the list of picture files on the local storage
        picsFileRefreshHandle = picsRefreshScheduler.scheduleAtFixedRate(picturesRefresh, picsRefreshTime,
                picsRefreshTime, TimeUnit.MINUTES);

    }


    public void onImageClicked(View view) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(Select_Folders.USER_CANCELLED_SLIDESHOW, true).apply();
        /*cancelTimer();
        clearMem();*/
        finish();
    }

    private void clearMem() {
        imageView1.setImageBitmap(null);
        imageView2.setImageBitmap(null);
        imageView3.setImageBitmap(null);
        /*imageView4.setImageBitmap(null);*/
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(Select_Folders.USER_CANCELLED_SLIDESHOW, true).apply();
        finish();
    }

    private void cancelTimer() {
        if (textUpdateHandle != null) {
            textUpdateHandle.cancel(true);
        }
        pictureDisplayHandle.cancel(true);
        picsFileRefreshHandle.cancel(true);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        clearMem();
        unbindDrawables(findViewById(R.id.root_view));
        System.gc();
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

}
