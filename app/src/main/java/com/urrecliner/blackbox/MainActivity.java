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
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.AUTO_START_RECORDING;
import static com.urrecliner.blackbox.Vars.DELAY_WAIT_EXIT;
import static com.urrecliner.blackbox.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackbox.Vars.INTERVAL_EVENT;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.displayBattery;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.displayTime;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mBackgroundImage;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPackageEventPath;
import static com.urrecliner.blackbox.Vars.mPackageLogPath;
import static com.urrecliner.blackbox.Vars.mPackageNormalDatePath;
import static com.urrecliner.blackbox.Vars.mPackageNormalPath;
import static com.urrecliner.blackbox.Vars.mPackagePath;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.obdAccess;
import static com.urrecliner.blackbox.Vars.sharedPref;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.startStopExit;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vBtnEvent;
import static com.urrecliner.blackbox.Vars.vBtnRecord;
import static com.urrecliner.blackbox.Vars.vCompass;
import static com.urrecliner.blackbox.Vars.vExitApp;
import static com.urrecliner.blackbox.Vars.vSatellite;
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
import static com.urrecliner.blackbox.Vars.vTodayKms;
import static com.urrecliner.blackbox.Vars.videoUtils;
import static com.urrecliner.blackbox.Vars.viewFinder;
import static com.urrecliner.blackbox.Vars.willBack;

public class MainActivity extends Activity {

    private static final String logID = "Main";
    boolean surfaceReady = false;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            readyCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            surfaceTexture.release();
//            emExoPlayer.blockingClearSurface();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    void readyCamera() {
        if (!surfaceReady) {
            videoUtils.setupCamera();
            videoUtils.connectCamera();
            surfaceReady = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(getApplicationContext(), "Rotate Phone to Landscape, pls ", Toast.LENGTH_LONG).show();
            return;
        }
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        askPermission();
        setContentView(R.layout.main_activity);
        mActivity = this;
        mContext = this;
        gpsTracker = new GPSTracker(mContext);
        sharedPref = getApplicationContext().getSharedPreferences("blackBox", MODE_PRIVATE);
        vPreviewView = findViewById(R.id.previewView);
        utils.logOnly(logID, "\n* * * Main Started * * *\n");
        final TextView textureBox = findViewById(R.id.textureBox);
        vBtnRecord = findViewById(R.id.btnRecord);
        vBtnRecord.setOnClickListener(v -> {
            utils.logBoth(logID, " start button clicked");
            if (mIsRecording)
                startStopExit.stopVideo();
            else {
                startStopExit.startVideo();
            }
        });

        mediaRecorder = new MediaRecorder();
//        Utils.ScreenInfo screenInfo = utils.getScreenSize(mActivity);
//        utils.logOnly(logID,"Screen Type is "+screenInfo.screenType);
//        utils.logOnly(logID,"Screen Inch is "+screenInfo.screenInch);

        vBtnEvent = findViewById(R.id.btnEvent);
        vBtnEvent.setOnClickListener(v -> startEventSaving());

        setViewVars();
        setBlackBoxFolders();
        initiate();

        textureBox.setOnClickListener(v -> {
            viewFinder = !viewFinder;
            vPreviewView.setVisibility((viewFinder) ? View.VISIBLE : View.INVISIBLE);
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
            RelativeLayout.LayoutParams textureLP = (RelativeLayout.LayoutParams) vPreviewView.getLayoutParams();
            RelativeLayout.LayoutParams tvLP = (RelativeLayout.LayoutParams) textureBox.getLayoutParams();
            textureLP.setMargins(0, 0, 0, 0);
            textureLP.bottomMargin = tvLP.bottomMargin + 6;
            textureLP.rightMargin = tvLP.rightMargin + 6;
            vPreviewView.setLayoutParams(textureLP);
            vPreviewView.setScaleX(1.7f);
        });

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        DisplayTime displayTime = new DisplayTime();
        new Timer().schedule(new TimerTask() {
            public void run() {
                displayTime.run();
                displayBattery.init();
                obdAccess.prepare();
                utils.deleteOldNormalEvents(mPackageNormalPath, 2);
                utils.deleteOldNormalEvents(mPackageEventPath, 3);
                utils.deleteOldLogs(4);
            }
        }, 100);
        new Timer().schedule(new TimerTask() {  // autoStart
            public void run() {
//                mActivity.runOnUiThread(() -> {
                    startHandler.sendEmptyMessage(0);
//                });
            }
        }, AUTO_START_RECORDING * 1000);

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

    void startEventSaving() {
        eventHandler.sendEmptyMessage(0);
    }

    void eventRecording() {
        if (!mIsRecording) return;
        utils.logBoth(logID,"Event Starting ...");

        gpsTracker.askLocation();
        final long startTime = System.currentTimeMillis() - INTERVAL_EVENT - INTERVAL_EVENT;
        final File thisEventPath = new File(mPackageEventPath, utils.getMilliSec2String(startTime, FORMAT_LOG_TIME));
        utils.readyPackageFolder(thisEventPath);
//        utils.logBoth("Event","Prev Snapshot");
//        SnapShotSave prevSnapShot = new SnapShotSave();
//        prevSnapShot.start(thisEventPath, snapBytes.clone(), snapMapIdx, true);
        new SnapShotSave().start(thisEventPath, snapBytes.clone(), snapMapIdx, true);
        new Timer().schedule(new TimerTask() {
            public void run() {
                EventMerge ev = new EventMerge();
                ev.merge(startTime, thisEventPath);
            }
        }, INTERVAL_EVENT + INTERVAL_EVENT / 4);

        activeEventCount++;
        mActivity.runOnUiThread(() -> {
            String text = ""+activeEventCount;
            vTextActiveCount.setText(text);
            vBtnEvent.setImageResource(R.mipmap.event_blue);
            utils.customToast("EVENT\nbutton\nPressed", Toast.LENGTH_SHORT, Color.RED);
        });
    }

    private void initiate() {

        mIsRecording = false;
        vSatellite.setVisibility(View.INVISIBLE);
        vCompass.setVisibility(View.INVISIBLE);
        utils.beepsInitiate();
        gpsTracker.askLocation();
        CountEvent = utils.getDirectoryFiltered(mPackageEventPath, "mp4").length;
        String txt = "" + CountEvent;
        vTextCountEvent.setText(txt);
        vTextActiveCount.setText("");
        vExitApp = findViewById(R.id.btnExit);
        vExitApp.setOnClickListener(v -> startStopExit.exitBlackBoxApp());
        ImageButton btnNapping = findViewById(R.id.btnIWillBack);
        btnNapping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnNapping.setImageAlpha(50);
                willBack = true;
                if (mIsRecording)
                    stopHandler.sendEmptyMessage(0);
//                reStarting();
                new BeBackSoon().execute("x", getString(R.string.i_will_back), ""+DELAY_WAIT_EXIT);
            }
        });
        vTextDate.setText(utils.getMilliSec2String(System.currentTimeMillis(), "MM-dd(EEE)"));
        if (!mPackageNormalDatePath.exists())
            mPackageNormalDatePath.mkdir();
        utils.beepsInitiate();
        startBackgroundThread();
//        directionSensor.start();
    }

    private void setViewVars() {
//        vTextureView = findViewById(R.id.textureView);
        vTextDate = findViewById(R.id.textDate);
        vTextTime = findViewById(R.id.textTime);
        vTextSpeed = findViewById(R.id.textSpeed);
        vKm = findViewById(R.id.textKm);
        vTextLogInfo = findViewById(R.id.textLogInfo);
        vTextCountEvent = findViewById(R.id.textCountEvent);
        vTextActiveCount = findViewById(R.id.activeEvent);
        vTextRecord = findViewById(R.id.textCountRecords);
        vTextBattery = findViewById(R.id.textBattery);
        vImgBattery = findViewById(R.id.imgBattery);
        vCompass = findViewById(R.id.iVCompass);
        vSatellite = findViewById(R.id.gpsActive);
        vBtnRecord = findViewById(R.id.btnRecord);
        vTextSpeed.setText("_");
    }

    private void setBlackBoxFolders() {
        utils.readyPackageFolder(mPackagePath);
        utils.readyPackageFolder(mPackageLogPath);
        utils.readyPackageFolder(mPackageWorkingPath);
        utils.readyPackageFolder(mPackageEventPath);
        utils.readyPackageFolder(mPackageNormalPath);
        utils.readyPackageFolder(mPackageNormalDatePath);
    }

    private void startBackgroundThread() {
        HandlerThread mBackgroundHandlerThread;
        mBackgroundHandlerThread = new HandlerThread("BlackBox");
        mBackgroundHandlerThread.start();
        mBackgroundImage = new Handler(mBackgroundHandlerThread.getLooper());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
            Toast.makeText(mContext,"화면을 가로로 바꾸어 주세요",Toast.LENGTH_LONG).show();
    }

    static long keyOldTime = 0, keyNowTime = 0;
    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {

        keyNowTime = System.currentTimeMillis();
        long diff = keyNowTime - keyOldTime;
        utils.logBoth("Key Pressed",keyCode+" time diff = "+diff+" willBack "+willBack);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (willBack)
                    startStopExit.exitBlackBoxApp();
                if ((keyOldTime + 20000) < keyNowTime)  // if gap is big, reset to current
                    keyOldTime = keyNowTime;
                if ((keyOldTime + 800 > keyNowTime) &&
                        (keyOldTime + 10 < keyNowTime)) {   // if gap is small double clicked so exit app
                    willBack = true;
                    if (mIsRecording)
                        stopHandler.sendEmptyMessage(0);
                    new BeBackSoon().execute("x", getString(R.string.i_will_back), ""+DELAY_WAIT_EXIT);
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
                }, 1000);
                break;
//            default:      backKey (= 4) is ignored
//                utils.logBoth("key", keyCode + " Pressed");
//                break;
        }
        keyOldTime = keyNowTime;
        return super.onKeyDown(keyCode, event);
    }

    // ↓ ↓ ↓ P E R M I S S I O N    RELATED /////// ↓ ↓ ↓ ↓
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
        for (String perm : permissions) if (hasPermission(perm)) {
            Log.e("permission", perm); result.add(perm);}
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

// ↑ ↑ ↑ ↑ P E R M I S S I O N    RELATED /////// ↑ ↑ ↑ 완벽하진 않음
}
