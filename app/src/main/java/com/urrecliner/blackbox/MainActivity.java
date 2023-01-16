package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.DATE_PREFIX;
import static com.urrecliner.blackbox.Vars.DELAY_AUTO_RECORDING;
import static com.urrecliner.blackbox.Vars.mPreviewSize;
import static com.urrecliner.blackbox.Vars.share_left_right_interval;
import static com.urrecliner.blackbox.Vars.share_snap_interval;
import static com.urrecliner.blackbox.Vars.share_image_size;
import static com.urrecliner.blackbox.Vars.displayBattery;
import static com.urrecliner.blackbox.Vars.displayTime;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPackageEventJpgPath;
import static com.urrecliner.blackbox.Vars.mPackageEventPath;
import static com.urrecliner.blackbox.Vars.mPackageLogPath;
import static com.urrecliner.blackbox.Vars.mPackageNormalDatePath;
import static com.urrecliner.blackbox.Vars.mPackageNormalPath;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapNowPos;
import static com.urrecliner.blackbox.Vars.startStopExit;
import static com.urrecliner.blackbox.Vars.surface_Preview;
import static com.urrecliner.blackbox.Vars.tvDegree;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vBtnEvent;
import static com.urrecliner.blackbox.Vars.vBtnRecord;
import static com.urrecliner.blackbox.Vars.vExitApp;
import static com.urrecliner.blackbox.Vars.vImgBattery;
import static com.urrecliner.blackbox.Vars.vKm;
import static com.urrecliner.blackbox.Vars.vPreviewView;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextBattery;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;
import static com.urrecliner.blackbox.Vars.vTextDate;
import static com.urrecliner.blackbox.Vars.vTextKilo;
import static com.urrecliner.blackbox.Vars.vTextLogInfo;
import static com.urrecliner.blackbox.Vars.vTextRecord;
import static com.urrecliner.blackbox.Vars.vTextSpeed;
import static com.urrecliner.blackbox.Vars.vTextTime;
import static com.urrecliner.blackbox.Vars.videoMain;
import static com.urrecliner.blackbox.Vars.viewFinderActive;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.urrecliner.blackbox.utility.Celcius;
import com.urrecliner.blackbox.utility.DiskSpace;
import com.urrecliner.blackbox.utility.Permission;
import com.urrecliner.blackbox.utility.SettingsActivity;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private static final String logID = "Main";
    private static final int SETTING_ACTIVITY = 101;
    private boolean recordable = true;

    CameraSub cameraSub;

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//            surface_Preview = surface;
//            Log.w("onSurfaceTextureAvailable", "accepted");
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            surfaceTexture.release();
            return true;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mContext = this;
        mActivity = this;

        try {
            PackageInfo info = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            Permission.ask(this, this, info);
        } catch (Exception e) {
            Log.e("Permission", "No Permission "+e);
        }
        vPreviewView = findViewById(R.id.previewView);

        Vars.setSuffix(getApplicationContext());
        SettingsActivity.getPreference();

        readyBlackBoxFolders();
        utils.deleteOldFiles(mPackageNormalPath, 6);
        utils.deleteOldLogs();
        cameraSub = new CameraSub();
        gpsTracker = new GPSTracker();
        gpsTracker.init(mActivity, mContext);
        utils.makeEventShotArray();

        utils.setFullScreen();
        prepareMain();
        String s = "MAX_IMAGES_SIZE="+ share_image_size +"\nINTERVAL_SNAP_SHOT_SAVE="+ share_snap_interval +"\nINTERVAL_LEFT_RIGHT="+ share_left_right_interval;
        utils.logBoth("PREFERENCE",s);
        String msg = new DiskSpace().squeeze(mPackageNormalPath);
        if (msg.length() > 0)
            utils.logBoth("DISK", msg);
    }

    private void prepareMain() {

        utils.logOnly(logID, "Main Started ..");
        startStopExit = new StartStopExit();

        vBtnEvent = findViewById(R.id.btnEvent);
        vBtnEvent.setOnClickListener(v -> startEventSaving());

        setViewVars();

        mIsRecording = false;
        snapBytes = new byte[share_image_size][];
        snapNowPos = 0;

        utils.beepsInitiate();
        gpsTracker.askLocation();
        CountEvent = utils.getRecordEventCount();
        vExitApp = findViewById(R.id.btnExit);
        vExitApp.setOnClickListener(v -> {
            vExitApp.setClickable(false);
            startStopExit.exitApp(false);
        });
        ImageButton btnBeBack = findViewById(R.id.btnPauseAMinute);
        btnBeBack.setOnClickListener(v -> {
            btnBeBack.setImageAlpha(50);
            if (mIsRecording)
                stopHandler.sendEmptyMessage(0);
//                reStarting();
            new BeBackSoon().execute("x");
        });
        vTextDate.setText(utils.getMilliSec2String(System.currentTimeMillis(), "MM-dd(EEE)"));
        utils.readyPackageFolder(mPackageNormalDatePath);

        FrameLayout framePreview = findViewById(R.id.framePreview);
        framePreview.setOnClickListener(v -> {
            viewFinderActive = !viewFinderActive;
            vPreviewView.setVisibility((viewFinderActive)? View.VISIBLE:View.INVISIBLE);
        });

        ImageButton btnSetting = findViewById(R.id.btnSetting);
        btnSetting.setOnClickListener(v -> {
            Intent setInt = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(setInt,SETTING_ACTIVITY) ;
        });

        cameraSub.readyCamera();

        mActivity.runOnUiThread(() -> {
            vPreviewView.setRotation(-90);
            vPreviewView.setScaleY(1.7f);
        });

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        showInitialValues();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startHandler.sendEmptyMessage(0);
                displayBattery = new DisplayBattery();
                displayBattery.start();
                displayBattery.show();
                displayTime = new DisplayTime();    // displayBattery first
                displayTime.run();
                Celcius.start(mContext);
            }
        }, DELAY_AUTO_RECORDING);

        utils.setVolume(70);
    }

    final static Handler startHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) { startStopExit.startVideo();
        }
    };
    final static Handler stopHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) { startStopExit.stopVideo();}
    };
    final Handler eventHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) { new EventRecord().start();}
    };

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SETTING_ACTIVITY) {
            startStopExit.exitApp(true);
        }
    }

    void startEventSaving() {
        eventHandler.sendEmptyMessage(0);
    }

    private void showInitialValues() {
        mActivity.runOnUiThread(() -> {
            String txt = " " + CountEvent + " ";
            vTextCountEvent.setText(txt);
            vTextActiveCount.setText("");
        });
    }

    private void setViewVars() {
        vTextDate = findViewById(R.id.textDate);
        vTextTime = findViewById(R.id.textTime);
        vTextSpeed = findViewById(R.id.textSpeed);
        vKm = findViewById(R.id.textKm);
        vTextKilo = findViewById(R.id.todayKm);
        vTextLogInfo = findViewById(R.id.logInfo);
        vTextCountEvent = findViewById(R.id.textCountEvent);
        vTextActiveCount = findViewById(R.id.activeEvent);
        vTextRecord = findViewById(R.id.textCountRecords);
        vTextBattery = findViewById(R.id.textBattery);
        vImgBattery = findViewById(R.id.imgBattery);
        vBtnRecord = findViewById(R.id.btnRecord);
        tvDegree = findViewById(R.id.degree);
        vTextSpeed.setText(R.string.under_bar);
        vPreviewView = findViewById(R.id.previewView);
//        vPreviewView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private void readyBlackBoxFolders() {

        File mPackagePath = new File(Environment.getExternalStorageDirectory(), "BlackBox");
        mPackageEventPath = new File(mPackagePath, "event");
        mPackageEventJpgPath = new File(mPackagePath, "EventJpg");
        mPackageNormalPath = new File(mPackagePath, "normal");
        mPackageNormalDatePath = new File(mPackageNormalPath, DATE_PREFIX+utils.getMilliSec2String(System.currentTimeMillis(), "yy-MM-dd"));
        mPackageWorkingPath = new File(mPackagePath, "work");
        mPackageLogPath = new File(mPackagePath, "log");

        utils.readyPackageFolder(mPackagePath);
        utils.readyPackageFolder(mPackageEventPath);
        utils.readyPackageFolder(mPackageEventJpgPath);
        utils.readyPackageFolder(mPackageLogPath);
        utils.readyPackageFolder(mPackageWorkingPath);
        utils.readyPackageFolder(mPackageNormalPath);
        utils.readyPackageFolder(mPackageNormalDatePath);
    }

//    long keyOldTime = 0, keyNowTime = 0;
    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (!mIsRecording)
                    break;
                if (recordable) {
                    recordable = false;
                    startEventSaving();
                }
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        recordable = true;
                    }
                }, 5000);
                break;
            default:
                utils.logBoth("key", keyCode + " Pressed");
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
}