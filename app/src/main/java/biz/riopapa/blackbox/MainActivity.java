package biz.riopapa.blackbox;

import static biz.riopapa.blackbox.Vars.CountEvent;
import static biz.riopapa.blackbox.Vars.DATE_PREFIX;
import static biz.riopapa.blackbox.Vars.DELAY_AUTO_RECORDING;
import static biz.riopapa.blackbox.Vars.displayBattery;
import static biz.riopapa.blackbox.Vars.displayTime;
import static biz.riopapa.blackbox.Vars.gpsTracker;
import static biz.riopapa.blackbox.Vars.imageStack;
import static biz.riopapa.blackbox.Vars.mActivity;
import static biz.riopapa.blackbox.Vars.mContext;
import static biz.riopapa.blackbox.Vars.mIsRecording;
import static biz.riopapa.blackbox.Vars.mPackageEventJpgPath;
import static biz.riopapa.blackbox.Vars.mPackageEventPath;
import static biz.riopapa.blackbox.Vars.mPackageLogPath;
import static biz.riopapa.blackbox.Vars.mPackageNormalDatePath;
import static biz.riopapa.blackbox.Vars.mPackageNormalPath;
import static biz.riopapa.blackbox.Vars.mPackageWorkingPath;
import static biz.riopapa.blackbox.Vars.mainLayout;
import static biz.riopapa.blackbox.Vars.normal_duration;
import static biz.riopapa.blackbox.Vars.scrollLog;
import static biz.riopapa.blackbox.Vars.share_event_sec;
import static biz.riopapa.blackbox.Vars.share_image_size;
import static biz.riopapa.blackbox.Vars.share_left_right_interval;
import static biz.riopapa.blackbox.Vars.share_snap_interval;
import static biz.riopapa.blackbox.Vars.share_work_size;
import static biz.riopapa.blackbox.Vars.startStopExit;
import static biz.riopapa.blackbox.Vars.tvDegree;
import static biz.riopapa.blackbox.Vars.utils;
import static biz.riopapa.blackbox.Vars.vBtnEvent;
import static biz.riopapa.blackbox.Vars.vPower;
import static biz.riopapa.blackbox.Vars.vImgBattery;
import static biz.riopapa.blackbox.Vars.vKm;
import static biz.riopapa.blackbox.Vars.vPreviewView;
import static biz.riopapa.blackbox.Vars.vTextActiveCount;
import static biz.riopapa.blackbox.Vars.vTextBattery;
import static biz.riopapa.blackbox.Vars.vTextCountEvent;
import static biz.riopapa.blackbox.Vars.vTextDate;
import static biz.riopapa.blackbox.Vars.vTextKilo;
import static biz.riopapa.blackbox.Vars.vTextLogInfo;
import static biz.riopapa.blackbox.Vars.vTextRecord;
import static biz.riopapa.blackbox.Vars.vTextSpeed;
import static biz.riopapa.blackbox.Vars.vTextTime;
import static biz.riopapa.blackbox.Vars.viewFinderActive;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import biz.riopapa.blackbox.utility.Celcius;
import biz.riopapa.blackbox.utility.DiskSpace;
import biz.riopapa.blackbox.utility.ImageStack;
import biz.riopapa.blackbox.utility.Permission;
import biz.riopapa.blackbox.utility.SettingsActivity;
import biz.riopapa.blackbox.utility.VideoSize;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private static final int SETTING_ACTIVITY = 101;
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

        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", this.getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }

        vPreviewView = findViewById(R.id.previewView);

        SettingsActivity.getPreference();   // should be after VideoSize().set()
        new VideoSize().set();

        normal_duration = share_event_sec * 4000;

        readyBlackBoxFolders();
        utils.deleteOldFiles(mPackageNormalPath);
        utils.deleteOldLogs();
        cameraSub = new CameraSub();
        gpsTracker = new GPSTracker();
        gpsTracker.init(mActivity, mContext);
        utils.makeEventShotArray();

        utils.setFullScreen();
        isRunning = false;

        startStopExit = new StartStopExit();

        setViewVars();

        vBtnEvent.setOnClickListener(v -> startEventSaving());

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
                startStopExit.stopVideo();
//                stopHandler.sendEmptyMessage(0);
            new BeBackSoon().execute("x");
        });
        ImageButton btnMove = findViewById(R.id.btnPauseMoving);
        btnMove.setOnClickListener(v -> {
            btnMove.setImageAlpha(50);
            if (mIsRecording)
                startStopExit.stopVideo();
//                stopHandler.sendEmptyMessage(0);
            new BeBackSoon().execute("M");
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

        if (isRunning)
            return;
        isRunning = true;

        new Timer().schedule(new TimerTask() {
        @Override
        public void run() {
                startStopExit.startVideo();
//                startHandler.sendEmptyMessage(0);
                displayBattery = new DisplayBattery();
                displayBattery.start();
                displayBattery.show();
                displayTime = new DisplayTime();    // displayBattery first
                displayTime.run();
                Celcius.start(mContext);
            }
        }, DELAY_AUTO_RECORDING);

//        utils.setVolume(70);
        String msg = new DiskSpace().squeeze(mPackageNormalPath);
        if (!msg.isEmpty())
            utils.logBoth("DISK", msg);

        showParams();
        mActivity.runOnUiThread(() -> {
            String txt = " " + CountEvent + " ";
            vTextCountEvent.setText(txt);
            vTextActiveCount.setText("");
        });
    }

    private static void showParams() {
        String s = "\nImage_Arrays : "+ share_image_size +
                "\nInterval (Shot: "+ share_snap_interval +
                ", LeftRight: "+ share_left_right_interval+") " +
                "\nEvent Duration: "+share_event_sec +
                "\nWork Size : "+share_work_size;
        utils.logBoth("Preference",s);
    }
//
//    final static Handler startHandler = new Handler(Looper.getMainLooper()) {
//        public void handleMessage(@NonNull Message msg) { startStopExit.startVideo();
//        }
//    };
//    final static Handler stopHandler = new Handler(Looper.getMainLooper()) {
//        public void handleMessage(Message msg) { startStopExit.stopVideo();}
//    };
//    final Handler eventHandler = new Handler(Looper.getMainLooper()) {
//        public void handleMessage(Message msg) { new EventRecord().start();}
//    };

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        showParams();
        imageStack = new ImageStack(share_image_size);
    }

    void startEventSaving() {
        new EventRecord().start();
//        eventHandler.sendEmptyMessage(0);
    }

    private void setViewVars() {
        mainLayout = findViewById(R.id.main_layout);
        vTextDate = findViewById(R.id.textDate);
        vTextTime = findViewById(R.id.textTime);
        vTextSpeed = findViewById(R.id.textSpeed);
        vKm = findViewById(R.id.textKm);
        vTextKilo = findViewById(R.id.todayKm);
        scrollLog = findViewById(R.id.scroll_log);
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
                        startStopExit.stopVideo();
//                        stopHandler.sendEmptyMessage(0);
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