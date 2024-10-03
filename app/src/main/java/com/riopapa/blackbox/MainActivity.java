package com.riopapa.blackbox;

import static com.riopapa.blackbox.Vars.CountEvent;
import static com.riopapa.blackbox.Vars.DATE_PREFIX;
import static com.riopapa.blackbox.Vars.DELAY_AUTO_RECORDING;
import static com.riopapa.blackbox.Vars.displayBattery;
import static com.riopapa.blackbox.Vars.displayTime;
import static com.riopapa.blackbox.Vars.gpsTracker;
import static com.riopapa.blackbox.Vars.imageStack;
import static com.riopapa.blackbox.Vars.mActivity;
import static com.riopapa.blackbox.Vars.mContext;
import static com.riopapa.blackbox.Vars.mIsRecording;
import static com.riopapa.blackbox.Vars.mPackageEventJpgPath;
import static com.riopapa.blackbox.Vars.mPackageEventPath;
import static com.riopapa.blackbox.Vars.mPackageLogPath;
import static com.riopapa.blackbox.Vars.mPackageNormalDatePath;
import static com.riopapa.blackbox.Vars.mPackageNormalPath;
import static com.riopapa.blackbox.Vars.mPackageWorkingPath;
import static com.riopapa.blackbox.Vars.mainLayout;
import static com.riopapa.blackbox.Vars.normal_duration;
import static com.riopapa.blackbox.Vars.share_event_sec;
import static com.riopapa.blackbox.Vars.share_image_size;
import static com.riopapa.blackbox.Vars.share_left_right_interval;
import static com.riopapa.blackbox.Vars.share_snap_interval;
import static com.riopapa.blackbox.Vars.startStopExit;
import static com.riopapa.blackbox.Vars.tvDegree;
import static com.riopapa.blackbox.Vars.utils;
import static com.riopapa.blackbox.Vars.vBtnEvent;
import static com.riopapa.blackbox.Vars.vPower;
import static com.riopapa.blackbox.Vars.vImgBattery;
import static com.riopapa.blackbox.Vars.vKm;
import static com.riopapa.blackbox.Vars.vPreviewView;
import static com.riopapa.blackbox.Vars.vTextActiveCount;
import static com.riopapa.blackbox.Vars.vTextBattery;
import static com.riopapa.blackbox.Vars.vTextCountEvent;
import static com.riopapa.blackbox.Vars.vTextDate;
import static com.riopapa.blackbox.Vars.vTextKilo;
import static com.riopapa.blackbox.Vars.vTextLogInfo;
import static com.riopapa.blackbox.Vars.vTextRecord;
import static com.riopapa.blackbox.Vars.vTextSpeed;
import static com.riopapa.blackbox.Vars.vTextTime;
import static com.riopapa.blackbox.Vars.viewFinderActive;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.riopapa.blackbox.utility.Celcius;
import com.riopapa.blackbox.utility.DiskSpace;
import com.riopapa.blackbox.utility.ImageStack;
import com.riopapa.blackbox.utility.Permission;
import com.riopapa.blackbox.utility.SettingsActivity;
import com.riopapa.blackbox.utility.VideoSize;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private static final int SETTING_ACTIVITY = 101;
    private boolean started = false;
    private static boolean isRunning = false;
    CameraSub cameraSub;

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
            Log.e("Permission", "No Permission " + e);
        }
        vPreviewView = findViewById(R.id.previewView);

        SettingsActivity.getPreference();   // should be after VideoSize().set()
        new VideoSize().set();

        normal_duration = share_event_sec * 4000;

        readyBlackBoxFolders();
        utils.deleteOldFiles(mPackageNormalPath, 6);
        utils.deleteOldLogs();
        cameraSub = new CameraSub();
        gpsTracker = new GPSTracker();
        gpsTracker.init(mActivity, mContext);
        utils.makeEventShotArray();

        utils.setFullScreen();
        isRunning = false;
        prepareMain();
    }

    private void prepareMain() {

        if (started)
            return;
        started = true;
        startStopExit = new StartStopExit();

        setViewVars();

        vBtnEvent.setOnClickListener(v -> startEventSaving());
        vTextLogInfo.setOnClickListener(v -> startEventSaving());

        mIsRecording = false;

        imageStack = new ImageStack(share_image_size);

        utils.beepsInitiate();
        gpsTracker.askLocation();
        CountEvent = utils.getRecordEventCount();
        vPower = findViewById(R.id.btnPower);
        vPower.setOnClickListener(v -> {
            vPower.setClickable(false);
            startStopExit.exitApp();
        });
        ImageButton btnBeBack = findViewById(R.id.btnPauseAMinute);
        btnBeBack.setOnClickListener(v -> {
            btnBeBack.setImageAlpha(50);
            if (mIsRecording)
                stopHandler.sendEmptyMessage(0);
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
            isRunning = false;
        });

        cameraSub.readyCamera();

        mActivity.runOnUiThread(() -> {
            vPreviewView.setRotation(-90);
            vPreviewView.setScaleY(2f);
        });

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        showInitialValues();

        if (isRunning)
            return;
        isRunning = true;

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
        String msg = new DiskSpace().squeeze(mPackageNormalPath);
        if (!msg.isEmpty())
            utils.logBoth("DISK", msg);

        showParams();
    }

    private static void showParams() {
        String s = "\nImage_Arrays = "+ share_image_size +
                "\nInterval (Shot: "+ share_snap_interval +
                ", LeftRight: "+ share_left_right_interval+") " +
                "\nEvent Duration: "+share_event_sec;
        utils.logBoth("Preference",s);
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
        showParams();
        imageStack = new ImageStack(share_image_size);
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
        mainLayout = findViewById(R.id.main_layout);
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
        tvDegree = findViewById(R.id.degree);
        vTextSpeed.setText(R.string.under_bar);
        vPreviewView = findViewById(R.id.previewView);
        vBtnEvent = findViewById(R.id.btnEvent);
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

    long keyOldTime = 0, keyNowTime = 0;
    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (!mIsRecording)
                    break;
                keyNowTime = System.currentTimeMillis();
                if (keyOldTime == 0) {  // first Time
                    keyOldTime = keyNowTime;
                    new Timer().schedule(new TimerTask() {
                        public void run() {
                            if (keyOldTime > 0)
                                startEventSaving();
                            keyOldTime = 0;
                        }
                    }, 3000);
                } else if (keyNowTime - keyOldTime < 2000 && keyNowTime - keyOldTime > 300) {
                    keyOldTime = 0;
                    if (mIsRecording)
                        stopHandler.sendEmptyMessage(0);
                    new BeBackSoon().execute("x");
                } else
                    keyOldTime = 0;
                break;
            default:
                utils.logBoth("key", keyCode + " Pressed");
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

}