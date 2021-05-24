package com.urrecliner.blackbox;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.urrecliner.blackbox.utility.DiskSpace;
import com.urrecliner.blackbox.utility.Permission;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.DATE_PREFIX;
import static com.urrecliner.blackbox.Vars.DELAY_AUTO_RECORDING;
import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.FORMAT_TIME;
import static com.urrecliner.blackbox.Vars.INTERVAL_EVENT;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.displayBattery;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.displayTime;
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
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.startStopExit;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vBtnEvent;
import static com.urrecliner.blackbox.Vars.vBtnRecord;
import static com.urrecliner.blackbox.Vars.vExitApp;
import static com.urrecliner.blackbox.Vars.vImgBattery;
import static com.urrecliner.blackbox.Vars.vKm;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextBattery;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;
import static com.urrecliner.blackbox.Vars.vTextDate;
import static com.urrecliner.blackbox.Vars.vTextLogInfo;
import static com.urrecliner.blackbox.Vars.vTextRecord;
import static com.urrecliner.blackbox.Vars.vTextSpeed;
import static com.urrecliner.blackbox.Vars.vTextTime;
import static com.urrecliner.blackbox.Vars.vPreviewView;
import static com.urrecliner.blackbox.Vars.viewFinder;

public class MainActivity extends Activity {

    private static final String logID = "Main";
    CameraSub cameraSub;
    boolean surfaceReady = false;

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            readyCamera();
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

//        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

        try {
            PackageInfo info = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            Permission.ask(this, this, info);
        } catch (Exception e) {
            Log.e("Permission", "No Permission "+e.toString());
        }
        if (Build.MODEL.equals("SM-G950N"))
            SUFFIX = "8";
        else if (Build.MODEL.equals("SM-G965N"))
            SUFFIX = "9";

        readyBlackBoxFolders();
        utils.deleteOldFiles(mPackageNormalPath, (SUFFIX.startsWith("9")) ? 2:6);
        utils.deleteOldFiles(mPackageEventJpgPath, 4);
        utils.deleteOldLogs();
//        utils.deleteOldFiles(mPackageWorkingPath, -3);
        cameraSub = new CameraSub();
        prepareMain();
        String msg = new DiskSpace().squeeze(mPackageNormalPath);
        if (msg.length() > 0)
            utils.logBoth("DISK", msg);
    }

    private void prepareMain() {
        mActivity = this;
        mContext = this;
        gpsTracker = new GPSTracker(mContext);
        gpsTracker.init();
        sharedPref = getApplicationContext().getSharedPreferences("blackBox", MODE_PRIVATE);
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
        utils.logOnly("snapBytes","size = "+snapBytes.length);
        utils.beepsInitiate();
        gpsTracker.askLocation();
        CountEvent = utils.getRecordEventCount();
        vExitApp = findViewById(R.id.btnExit);
        vExitApp.setOnClickListener(v -> startStopExit.exitBlackBoxApp());
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
        if (!mPackageNormalDatePath.exists())
            mPackageNormalDatePath.mkdir();
        utils.beepsInitiate();
//        new StartBackground().run();

        framePreview.setOnClickListener(v -> {
            viewFinder = !viewFinder;
            vPreviewView.setVisibility((viewFinder)? View.VISIBLE:View.INVISIBLE);
        });
        vPreviewView.post(() -> {
            readyCamera();
            vPreviewView.setSurfaceTextureListener(mSurfaceTextureListener);
            int width = vPreviewView.getWidth();
            int height = vPreviewView.getHeight();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, width, height);
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            matrix.postRotate(-90, centerX, centerY);
            vPreviewView.setTransform(matrix);
            vPreviewView.setScaleX(1.9f);
        });

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        DisplayTime displayTime = new DisplayTime();
        displayTime.run();
        displayBattery.init();
        obdAccess.prepare();
        showInitialValues();
//        SystemClock.sleep(delayedStart);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startHandler.sendEmptyMessage(0);
//                vBtnEvent.setImageResource(R.mipmap.event_ready);
            }
        }, DELAY_AUTO_RECORDING);
        lNewsLine = findViewById(R.id.newsLine);
    }

    final static Handler startHandler = new Handler() {
        public void handleMessage(Message msg) { startStopExit.startVideo();
        }
    };
    final static Handler stopHandler = new Handler() {
        public void handleMessage(Message msg) { startStopExit.stopVideo();
        }
    };
    final Handler eventHandler = new Handler() {
        public void handleMessage(Message msg) { eventRecording();
        }
    };

    static float save_focus = -1f;    // 0: infinite 10: nearest
    static void focusChange(int speed) {
//        utils.logBoth("nearSwitch","switched to NEAR");
//        float focus;
//        if (speed < 5)
//            focus = 9f;
//        else if (speed < 10)
//            focus = 8f;
////        else if (speed < 20)
////            focus = 8f;
////        else if (speed < 30)
////            focus = 6f;
////        else if (speed < 40)
////            focus = 5f;
//        else
//            focus = 1f;
//        if (focus != save_focus) {
//            if (focus == 1f) {
////                mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f);
//                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
//                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
//            } else {
//                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
//                mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus);
//            }
//            save_focus = focus;
//        }
    }


//    static void control_Exposure(int percent) {
//            int brightness = (int) (minCompensationRange + (maxCompensationRange - minCompensationRange) * (percent / 100f));
//        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, brightness);
//        }

//        private void applySettings() {
//            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
//        }

//    }

    void readyCamera() {
        if (!surfaceReady) {
            cameraSub.setupCamera();
            cameraSub.connectCamera();
            surfaceReady = true;
        }
    }

    void startEventSaving() {
        eventHandler.sendEmptyMessage(0);
    }

    void eventRecording() {

        if (!mIsRecording) return;
        utils.logBoth(logID,"Event Starting ...");

//        cameraZoomIn = new Timer();
//        zoomFactor = 1.818f;
//        TimerTask cameraTask = new TimerTask() {
//            @Override
//            public void run() {
//                if (zoomFactor < 2.8f) {
//                    utils.logOnly("zoom","change factor "+zoomFactor);
//                    videoUtils.buildCameraSession(zoomFactor);
//                    zoomFactor += 0.1f;
//                }
//                else
//                    cameraZoomIn.cancel();
//            }
//        };
//        cameraZoomIn.schedule(cameraTask, 100, 100);

        final long startTime = System.currentTimeMillis() - INTERVAL_EVENT - INTERVAL_EVENT;
        final File thisEventJpgPath = new File(mPackageEventJpgPath, DATE_PREFIX+utils.getMilliSec2String(startTime, FORMAT_TIME)+ SUFFIX);
        utils.readyPackageFolder(thisEventJpgPath);

        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave1 = new SnapShotSave();
                snapShotSave1.startSave(thisEventJpgPath, snapMapIdx, 1);
            }
        }, 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave2 = new SnapShotSave();
                snapShotSave2.startSave(thisEventJpgPath, snapMapIdx, 2);
            }
        }, INTERVAL_EVENT * 5 / 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave3 = new SnapShotSave();
                snapShotSave3.startSave(thisEventJpgPath, snapMapIdx,3);
            }
        }, INTERVAL_EVENT * 9 / 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave3 = new SnapShotSave();
                snapShotSave3.startSave(thisEventJpgPath, snapMapIdx,4);
            }
        }, INTERVAL_EVENT * 13 / 10);

        gpsTracker.askLocation();

        new Timer().schedule(new TimerTask() {
            public void run() {
                EventMerge ev = new EventMerge();
                ev.merge(startTime);
            }
        }, INTERVAL_EVENT * 12 / 10);

        activeEventCount++;
        mActivity.runOnUiThread(() -> {
            String text = " "+activeEventCount+" ";
            vTextActiveCount.setText(text);
            utils.customToast("EVENT\nbutton\nPressed", Toast.LENGTH_LONG, Color.RED);
        });
    }

    private void showInitialValues() {
        mActivity.runOnUiThread(() -> {
            String txt = "" + CountEvent;
            vTextCountEvent.setText(txt);
            vTextActiveCount.setText("");
        });
    }

    private void setViewVars() {
//        vTextureView = findViewById(R.id.textureView);
        vTextDate = findViewById(R.id.textDate);
        vTextTime = findViewById(R.id.textTime);
        vTextSpeed = findViewById(R.id.textSpeed);
//        vTodayKms = findViewById(R.id.obdKms);
        vKm = findViewById(R.id.textKm);
        vTextLogInfo = findViewById(R.id.textLogInfo);
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
        utils.readyPackageFolder(mPackageLogPath);
        utils.readyPackageFolder(mPackageWorkingPath);
        utils.readyPackageFolder(mPackageEventPath);
        utils.readyPackageFolder(mPackageEventJpgPath);
        utils.readyPackageFolder(mPackageNormalPath);
        utils.readyPackageFolder(mPackageNormalDatePath);
    }

    static long keyOldTime = 0, keyNowTime = 0;
    static boolean willBack = false;
    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {

        keyNowTime = System.currentTimeMillis();
//        long diff = keyNowTime - keyOldTime;
//        utils.log("KeyDown",keyCode+" keyUp diff = "+diff+willBack);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (willBack) {
//                    launchJpg2PhotoApp();
                    startStopExit.exitBlackBoxApp();
                }
                if ((keyOldTime + 20000) < keyNowTime)  // if gap is big, reset to current
                    keyOldTime = keyNowTime;
                if ((keyOldTime + 800 > keyNowTime) &&
                        (keyOldTime + 50 < keyNowTime)) {   // if gap is small double clicked so exit app
                    willBack = true;
                    if (mIsRecording)
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
