package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.DELAY_AUTO_RECORDING;
import static com.urrecliner.blackbox.Vars.INTERVAL_LEFT_RIGHT;
import static com.urrecliner.blackbox.Vars.INTERVAL_SNAP_SHOT_SAVE;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.USE_CUSTOM_VALUES;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.chronoKiloMeter;
import static com.urrecliner.blackbox.Vars.chronoLogs;
import static com.urrecliner.blackbox.Vars.chronoNowDate;
import static com.urrecliner.blackbox.Vars.displayBattery;
import static com.urrecliner.blackbox.Vars.displayTime;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.lNewsLine;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPackageEventJpgPath;
import static com.urrecliner.blackbox.Vars.mPackageEventPath;
import static com.urrecliner.blackbox.Vars.mPackageLogPath;
import static com.urrecliner.blackbox.Vars.mPackageNormalDatePath;
import static com.urrecliner.blackbox.Vars.mPackageNormalPath;
import static com.urrecliner.blackbox.Vars.mPackagePath;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.obdAccess;
import static com.urrecliner.blackbox.Vars.sharedPref;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.startStopExit;
import static com.urrecliner.blackbox.Vars.todayKiloMeter;
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
import static com.urrecliner.blackbox.Vars.viewFinder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.urrecliner.blackbox.utility.DiskSpace;
import com.urrecliner.blackbox.utility.Permission;
import com.urrecliner.blackbox.utility.SettingsActivity;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private static final String logID = "Main";
    private static final int SETTING_ACTIVITY = 101;
    CameraSub cameraSub;
//    boolean surfaceReady = false;

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//            cameraSub.readyCamera();
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

//        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

        try {
            PackageInfo info = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            Permission.ask(this, this, info);
        } catch (Exception e) {
            Log.e("Permission", "No Permission "+e.toString());
        }

        if (Build.MODEL.equals("SM-G965N")) {
            @SuppressLint("HardwareIds")
            String aID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            //  S9+ = 66fb7229f2286ccd
            //  S9 blackbox = f4367a4dc1e43732
            if (aID.endsWith("6ccd"))
                SUFFIX = "P";
            else
                SUFFIX = "S";
        }
        else if (Build.MODEL.equals("SM-A325N"))
            SUFFIX = "A";
        else
            utils.logBoth("Model", Build.MODEL);
        Vars.set();
        SettingsActivity.getPreference();

        readyBlackBoxFolders();
        utils.deleteOldFiles(mPackageNormalPath, 6);
        utils.deleteOldFiles(mPackageEventJpgPath, 4);
        utils.deleteOldLogs();
        cameraSub = new CameraSub();
        prepareMain();
        String s = "MAX_IMAGES_SIZE="+MAX_IMAGES_SIZE+"\nINTERVAL_SNAP_SHOT_SAVE="+INTERVAL_SNAP_SHOT_SAVE+"\nINTERVAL_LEFT_RIGHT="+INTERVAL_LEFT_RIGHT;
        utils.logBoth("PREFERENCE",s);
        String msg = new DiskSpace().squeeze(mPackageNormalPath);
        if (msg.length() > 0)
            utils.logBoth("DISK", msg);
    }

    private void prepareMain() {

        gpsTracker = new GPSTracker(mContext);
        gpsTracker.init();
        chronoNowDate = sharedPref.getString("today","new");
        chronoKiloMeter = sharedPref.getInt("kilo", -1);
        todayKiloMeter = 0;
        chronoLogs = utils.getTodayTable();

        vPreviewView = findViewById(R.id.previewView);
        FrameLayout framePreview = findViewById(R.id.framePreview);
        utils.logOnly(logID, "Main Started ..");
        startStopExit = new StartStopExit();

        vBtnRecord = findViewById(R.id.btnRecord);
        vBtnRecord.setOnClickListener(v -> {
            utils.logBoth(logID," start button clicked");
            if (mIsRecording)
                startStopExit.stopVideo();
            else {
                startStopExit.startVideo();
            }
        });

        vBtnEvent = findViewById(R.id.btnEvent);
        vBtnEvent.setOnClickListener(v -> startEventSaving());

        setViewVars();
        mIsRecording = false;
        snapBytes = new byte[MAX_IMAGES_SIZE][];
//        snapBuffs = new ByteBuffer[MAX_IMAGES_SIZE];
        utils.beepsInitiate();
        gpsTracker.askLocation();
        CountEvent = utils.getRecordEventCount();
        vExitApp = findViewById(R.id.btnExit);
        vExitApp.setOnClickListener(v -> {
                startStopExit.exitBlackBoxApp();
                vExitApp.setClickable(false);
        });
        ImageButton btnBeBack = findViewById(R.id.btnIWillBack);
        btnBeBack.setOnClickListener(v -> {
            btnBeBack.setImageAlpha(50);
            willBack = true;
            if (mIsRecording)
                stopHandler.sendEmptyMessage(0);
//                reStarting();
            new BeBackSoon().execute("x");
        });
        vTextDate.setText(utils.getMilliSec2String(System.currentTimeMillis(), "MM-dd(EEE)"));
        utils.readyPackageFolder(mPackageNormalDatePath);

        framePreview.setOnClickListener(v -> {
            viewFinder = !viewFinder;
            vPreviewView.setVisibility((viewFinder)? View.VISIBLE:View.INVISIBLE);
        });

        ImageButton btnSetting = findViewById(R.id.btnSetting);
        btnSetting.setOnClickListener(v -> {
            startStopExit.stopVideo();
            Intent setInt = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(setInt,SETTING_ACTIVITY) ;
        });

        vPreviewView.post(() -> {
            cameraSub.readyCamera();
            vPreviewView.setSurfaceTextureListener(mSurfaceTextureListener);
//            vPreviewView.setLayoutParams(new FrameLayout.LayoutParams(vPreviewView.getWidth(), vPreviewView.getHeight()));


//            int width = vPreviewView.getWidth();
//            int height = vPreviewView.getHeight();
//            Log.w("preview size",width+" x "+height);
//            vPreviewView.setRotation(-90);
//            width = vPreviewView.getWidth();
//            height = vPreviewView.getHeight();
//            Log.w("preview size",width+" x "+height);

            //            Matrix matrix = new Matrix();
//            RectF viewRect = new RectF(0, 0, width, height);
//            float centerX = viewRect.centerX();
//            float centerY = viewRect.centerY();
//            matrix.postRotate(-90, centerY, centerX);
//            vPreviewView.setTransform(matrix);
//            vPreviewView.setScaleX(1.4f);
        });

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        displayTime.run();
        displayBattery.init();
        obdAccess.start();
        showInitialValues();
        lNewsLine = findViewById(R.id.newsLine);
        displayBattery.showBattery("displayed");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startHandler.sendEmptyMessage(0);
                new ShowKmLogs().show(chronoLogs);
            }
        }, DELAY_AUTO_RECORDING);
    }

    final static Handler startHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) { startStopExit.startVideo();
        }
    };
    final static Handler stopHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) { startStopExit.stopVideo();
        }
    };
    final Handler eventHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) { new EventRecord().start();
        }
    };

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SETTING_ACTIVITY) {
//            if (resultCode == RESULT_OK) {
                // A contact was picked.  Here we will just display it
                // to the user.
                Log.w("result", "RESTART --------- ///  "+RESULT_OK);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Intent thisInt = new Intent(MainActivity.this, MainActivity.class);
                    thisInt.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    thisInt.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    thisInt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(thisInt);
                }
            }, 8000);

//                Intent mStartActivity = new Intent(context, StartActivity.class);
//                int mPendingIntentId = 123456;
//                PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
//                AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
//                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
//                System.exit(0);
            }
//        }
    }

    void startEventSaving() {
        eventHandler.sendEmptyMessage(0);
    }

    private void showInitialValues() {
        mActivity.runOnUiThread(() -> {
            String txt = "" + CountEvent;
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
        vTextSpeed.setText("__");
    }

    private void readyBlackBoxFolders() {

        utils.readyPackageFolder(mPackagePath);
        utils.readyPackageFolder(mPackageEventPath);
        utils.readyPackageFolder(mPackageEventJpgPath);
        utils.readyPackageFolder(mPackageLogPath);
        utils.readyPackageFolder(mPackageWorkingPath);
        utils.readyPackageFolder(mPackageNormalPath);
        utils.readyPackageFolder(mPackageNormalDatePath);
    }

    static long keyOldTime = 0, keyNowTime = 0;
    static boolean willBack = false;
    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {

        keyNowTime = System.currentTimeMillis();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (willBack) {
                    startStopExit.exitBlackBoxApp();
                }
                if (!mIsRecording)
                    break;
                if ((keyOldTime + 10000) < keyNowTime)  // if gap is big, reset to current
                    keyOldTime = keyNowTime;
                if ((keyOldTime + 1000 > keyNowTime) &&
                        (keyOldTime + 150 < keyNowTime)) {   // if gap is small double clicked so exit app
                    willBack = true;
                    stopHandler.sendEmptyMessage(0);
                    new BeBackSoon().execute("x");
                }
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        try {
                            if (!willBack && mIsRecording)
                                startEventSaving();
                        } catch (Exception e) {
                            utils.logE(logID, "// start Eventing //", e);
                        }
                    }
                }, 800);
                break;
            default:
                utils.logBoth("key", keyCode + " Pressed");
                break;
        }
        keyOldTime = keyNowTime;
        return super.onKeyDown(keyCode, event);
    }

}