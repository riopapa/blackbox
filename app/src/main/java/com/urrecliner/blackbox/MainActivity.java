package com.urrecliner.blackbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.annotation.NonNull;

import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
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
import static com.urrecliner.blackbox.Vars.mBackgroundImage;
import static com.urrecliner.blackbox.Vars.mCaptureRequestVideoBuilder;
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
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(getApplicationContext(),"Rotate Phone to Landscape, pls ",Toast.LENGTH_LONG).show();
            return;
        }
        askPermission();
        setContentView(R.layout.main_activity);
        utils.deleteOldFiles(mPackageNormalPath, 7);
        utils.deleteOldFiles(mPackageEventJpgPath, 5);
        utils.deleteOldLogs();
//        utils.deleteOldFiles(mPackageWorkingPath, -3);
        cameraSub = new CameraSub();
        prepareMain();
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


//        Utils.ScreenInfo screenInfo = utils.getScreenSize(mActivity);
//        utils.logOnly(logID,"Screen Type is "+screenInfo.screenType);
//        utils.logOnly(logID,"Screen Inch is "+screenInfo.screenInch);

        vBtnEvent = findViewById(R.id.btnEvent);
        vBtnEvent.setOnClickListener(v -> startEventSaving());

        setViewVars();
        setBlackBoxFolders();
        mIsRecording = false;
        snapBytes = new byte[MAX_IMAGES_SIZE][];
        utils.logOnly("snapBytes","size = "+snapBytes.length);
        utils.beepsInitiate();
        gpsTracker.askLocation();
        CountEvent = utils.getRecordEventCount();
        vExitApp = findViewById(R.id.btnExit);
        vExitApp.setOnClickListener(v -> {
            startStopExit.exitBlackBoxApp();
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
        if (!mPackageNormalDatePath.exists())
            mPackageNormalDatePath.mkdir();
        utils.beepsInitiate();
        startBackgroundThread();

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
            vPreviewView.setScaleX(2.5f);
//            RelativeLayout.LayoutParams textureLP = (RelativeLayout.LayoutParams) vPreviewView.getLayoutParams();
//            RelativeLayout.LayoutParams tvLP = (RelativeLayout.LayoutParams) textureBox.getLayoutParams();
//            textureLP.setMargins(0,0,0,0);
//            textureLP.bottomMargin = tvLP.bottomMargin+6;
//            textureLP.rightMargin = tvLP.rightMargin+6;
//            vPreviewView.setLayoutParams(textureLP);
//            vPreviewView.setScaleX(1.79f);
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
        lNewsLine = (LinearLayout) findViewById(R.id.newsLine);
    }

    final Handler startHandler = new Handler() {
        public void handleMessage(Message msg) { startStopExit.startVideo();
        }
    };
    final Handler stopHandler = new Handler() {
        public void handleMessage(Message msg) { startStopExit.stopVideo();
        }
    };
    final Handler eventHandler = new Handler() {
        public void handleMessage(Message msg) { eventRecording();
        }
    };
//    static final Handler switchHandler = new Handler() {
//        public void handleMessage(Message msg) { offNearSwitch();
//        }
//    };

    static float save_focus = -1f;    // 0: infinite 10: nearest
    static void focusChange(int speed) {
//        utils.logBoth("nearSwitch","switched to NEAR");
        float focus;
        if (speed < 5)
            focus = 9f;
        else if (speed < 10)
            focus = 8.5f;
        else if (speed < 20)
            focus = 7f;
        else if (speed < 30)
            focus = 6f;
//        else if (speed < 40)
//            focus = 5f;
        else
            focus = 1f;
        if (focus != save_focus) {
            if (focus == 1f) {
                mCaptureRequestVideoBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                mCaptureRequestVideoBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f);
            } else {
                mCaptureRequestVideoBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                mCaptureRequestVideoBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus);
            }
            save_focus = focus;
        }
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
        final File thisEventJpgPath = new File(mPackageEventJpgPath, DATE_PREFIX+utils.getMilliSec2String(startTime, FORMAT_TIME));
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
        }, INTERVAL_EVENT * 8 / 10);

        gpsTracker.askLocation();

        new Timer().schedule(new TimerTask() {
            public void run() {
                EventMerge ev = new EventMerge();
                ev.merge(startTime, thisEventJpgPath);
            }
        }, INTERVAL_EVENT * 120 / 100);

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
        vTextSpeed.setText("_");
    }

    private void setBlackBoxFolders() {
        utils.readyPackageFolder(mPackagePath);
        utils.readyPackageFolder(mPackageLogPath);
        utils.readyPackageFolder(mPackageWorkingPath);
        utils.readyPackageFolder(mPackageEventPath);
        utils.readyPackageFolder(mPackageEventJpgPath);
        utils.readyPackageFolder(mPackageNormalPath);
        utils.readyPackageFolder(mPackageNormalDatePath);
    }

    private void startBackgroundThread() {
        HandlerThread mBackgroundHandlerThread;
        mBackgroundHandlerThread = new HandlerThread("BlackBox");
        mBackgroundHandlerThread.start();
        mBackgroundImage = new Handler(mBackgroundHandlerThread.getLooper());
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

// ↓ ↓ ↓ P E R M I S S I O N   RELATED /////// ↓ ↓ ↓ ↓  BEST CASE
private final static int ALL_PERMISSIONS_RESULT = 101;
    ArrayList permissionsToRequest;
    ArrayList<String> permissionsRejected = new ArrayList<>();
    String [] permissions;

    private void askPermission() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            permissions = info.requestedPermissions;//This array contain
        } catch (Exception e) {
            utils.logE("Permission", "Not done", e);
        }

        permissionsToRequest = findUnAskedPermissions();
        if (permissionsToRequest.size() != 0) {
            requestPermissions((String[]) permissionsToRequest.toArray(new String[0]),
//            requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                    ALL_PERMISSIONS_RESULT);
        }
    }

    private ArrayList findUnAskedPermissions() {
        ArrayList <String> result = new ArrayList<String>();
        for (String perm : permissions) if (hasPermission(perm)) result.add(perm);
        return result;
    }
    private boolean hasPermission(@NonNull String permission) {
        return (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ALL_PERMISSIONS_RESULT) {
            for (Object perms : permissionsToRequest) {
                if (hasPermission((String) perms)) {
                    permissionsRejected.add((String) perms);
                }
            }
            if (permissionsRejected.size() > 0) {
                if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                    String msg = "These permissions are mandatory for the application. Please allow access.";
                    showDialog(msg);
                }
            }
            else
                Toast.makeText(mContext, "Permissions not granted.", Toast.LENGTH_LONG).show();
        }
    }
    private void showDialog(String msg) {
        showMessageOKCancel(msg,
                (dialog, which) -> requestPermissions(permissionsRejected.toArray(
                        new String[0]), ALL_PERMISSIONS_RESULT));
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(mActivity)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

// ↑ ↑ ↑ ↑ P E R M I S S I O N    RELATED /////// ↑ ↑ ↑
}
