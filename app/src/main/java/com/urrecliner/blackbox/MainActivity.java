package com.urrecliner.blackbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.DELAY_AUTO_RECORD;
import static com.urrecliner.blackbox.Vars.DELAY_WAIT_EXIT;
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
import static com.urrecliner.blackbox.Vars.obdAccess;
import static com.urrecliner.blackbox.Vars.sharedPref;
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
import static com.urrecliner.blackbox.Vars.vTextureView;
import static com.urrecliner.blackbox.Vars.vTodayKms;
import static com.urrecliner.blackbox.Vars.videoUtils;
import static com.urrecliner.blackbox.Vars.viewFinder;

public class MainActivity extends Activity {

    private static final String logID = "Main";
    static EventMerge eventMerge0 = new EventMerge();
    static EventMerge eventMerge1 = new EventMerge();
    static EventMerge eventMerge2 = new EventMerge();
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
        askPermission();
        setContentView(R.layout.main_activity);
        mActivity = this;
        mContext = this;
        gpsTracker = new GPSTracker(mContext);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        sharedPref = getApplicationContext().getSharedPreferences("blackBox", MODE_PRIVATE);
        vTextureView = findViewById(R.id.textureView);
        utils.logOnly(logID, "Main Started");
        final TextView textureBox = findViewById(R.id.textureBox);
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
        setBlackBoxFolders();
        initiate();

        textureBox.setOnClickListener(v -> {
            viewFinder = !viewFinder;
            vTextureView.setVisibility((viewFinder)? View.VISIBLE:View.INVISIBLE);
        });

        vTextureView.post(() -> {
            readyCamera();
            vTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            int width = vTextureView.getWidth();
            int height = vTextureView.getHeight();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, width, height);
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            matrix.postRotate(-90, centerX, centerY);
            vTextureView.setTransform(matrix);
            RelativeLayout.LayoutParams textureLP = (RelativeLayout.LayoutParams) vTextureView.getLayoutParams();
            RelativeLayout.LayoutParams tvLP = (RelativeLayout.LayoutParams) textureBox.getLayoutParams();
            textureLP.setMargins(0,0,0,0);
            textureLP.bottomMargin = tvLP.bottomMargin+6;
            textureLP.rightMargin = tvLP.rightMargin+6;
            vTextureView.setLayoutParams(textureLP);
            vTextureView.setScaleX(1.7f);
        });

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        DisplayTime displayTime = new DisplayTime();
        displayTime.run();
        displayBattery.init();
        obdAccess.prepare();
        new Timer().schedule(new TimerTask() {  // autoStart
            public void run() {
                mActivity.runOnUiThread(() -> {
                    startHandler.sendEmptyMessage(0);
                    vBtnEvent.setImageResource(R.mipmap.event_ready);
                });
            }
        }, DELAY_AUTO_RECORD*1000);
        utils.deleteOldLogs();
    }

    final Handler startHandler = new Handler() {
        public void handleMessage(Message msg) { startStopExit.startVideo();
        }
    };
    final Handler stopHandler = new Handler() {
        public void handleMessage(Message msg) { startStopExit.stopVideo();
        }
    };

    static void startEventSaving() {

        if (!mIsRecording) return;
        utils.logBoth(logID,"Event Starting ...");

        gpsTracker.askLocation();
        long startTime = System.currentTimeMillis() - INTERVAL_EVENT - INTERVAL_EVENT;
        EventMerge ev = new EventMerge();
        ev.merge(startTime);
//            eventMerge0.merge(startTime);
//        else if (activeEventCount == 1)
//            eventMerge1.merge(startTime);
//        else if (activeEventCount == 2)
//            eventMerge2.merge(startTime);
        activeEventCount++;
        mActivity.runOnUiThread(() -> {
            String text = "<  "+activeEventCount+"  >\n";
            vTextActiveCount.setText(text);
            vBtnEvent.setImageResource(R.mipmap.event_blue);
            utils.customToast("EVENT button Pressed", Toast.LENGTH_LONG, Color.RED);
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
                new BeBackSoon().execute("x", "Exit & Reload", "10");
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
        vTodayKms = findViewById(R.id.obdKms);
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

//    private void stopBackgroundThread() {
//        if (mBackgroundHandlerThread.getLooper().getThread().isAlive()) {
//            mBackgroundHandlerThread.quitSafely();
//            try {
//                mBackgroundHandlerThread.join();
//                mBackgroundHandlerThread = null;
//                mBackgroundImage = null;
//            } catch (InterruptedException e) {
//                utils.logE(logID, e.toString());
//                e.printStackTrace();
//            }
//        }
//    }

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
                if (willBack)
                    startStopExit.exitBlackBoxApp();
                if ((keyOldTime + 20000) < keyNowTime)  // if gap is big, reset to current
                    keyOldTime = keyNowTime;
                if ((keyOldTime + 800 > keyNowTime) &&
                        (keyOldTime + 50 < keyNowTime)) {   // if gap is small double clicked so exit app
                    willBack = true;
                    if (mIsRecording)
                        stopHandler.sendEmptyMessage(0);
                    new BeBackSoon().execute("x", "Exit & Reload", DELAY_WAIT_EXIT + "");
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

// ↑ ↑ ↑ ↑ P E R M I S S I O N    RELATED /////// ↑ ↑ ↑ 완벽하진 않음
}
