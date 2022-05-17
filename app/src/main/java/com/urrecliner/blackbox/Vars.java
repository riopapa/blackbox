package com.urrecliner.blackbox;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Vars {

    static Activity mActivity = null;
    public static Context mContext = null;
    static boolean mExitApplication = false;

    public static Utils utils = new Utils();
    static VideoMain videoMain = new VideoMain();
    static PhotoCapture photoCapture = new PhotoCapture();
    static StartStopExit startStopExit = new StartStopExit();

//    static OBDAccess_Unused obdAccessUnused = new OBDAccess_Unused();
//    static boolean OBDConnected = false;
//    static DirectionSensor directionSensor = new DirectionSensor();
    static DisplayBattery displayBattery = new DisplayBattery();
    static GPSTracker gpsTracker;
    static DisplayTime displayTime = new DisplayTime();

    static TextView vTextDate = null;
    static TextView vTextTime = null;
    static TextView vTextCountEvent = null;
    static TextView vTextSpeed = null;
    static TextView vTextKilo = null;
    static TextView vKm = null;
    static TextView vTextLogInfo = null;
    static TextView vTextActiveCount = null;
    static TextView vTextRecord = null;
    static TextView vTextBattery = null;
    static ImageView vImgBattery = null;
    static ImageView vExitApp = null;
    static TextView tvCelcius = null;
    static LinearLayout lNewsLine = null;

    static ImageButton vBtnEvent = null;
    static TextureView vPreviewView;
    static ImageButton vBtnRecord;

    public static SharedPreferences sharedPref;
//    static int chronoKiloMeter = 0;
//    static String chronoNowDate = null;
//    static int todayKiloMeter = 0;

    static final String FORMAT_TIME = "yy-MM-dd HH.mm.ss";
    static final String FORMAT_DATE = "yy-MM-dd";
    static SimpleDateFormat sdfDate = new SimpleDateFormat(FORMAT_DATE, Locale.getDefault());
    static SimpleDateFormat sdfTime = new SimpleDateFormat(FORMAT_TIME, Locale.getDefault());
    static final String DATE_PREFIX = "V";

    private static final String PATH_PACKAGE = "BlackBox";
    private static final String PATH_EVENT = "event";
    private static final String PATH_EVENT_JPG = "EventJpg";
    private static final String PATH_NORMAL = "normal";
    private static final String PATH_WORK = "work";
    private static final String PATH_LOG = "log";

    static File mPackagePath = new File(Environment.getExternalStorageDirectory(), PATH_PACKAGE);
    static File mPackageEventPath = new File(mPackagePath, PATH_EVENT);
    static File mPackageEventJpgPath = new File(mPackagePath, PATH_EVENT_JPG);
    static File mPackageNormalPath = new File(mPackagePath, PATH_NORMAL);
    static File mPackageNormalDatePath = new File(mPackageNormalPath, DATE_PREFIX+utils.getMilliSec2String(System.currentTimeMillis(), FORMAT_DATE));
    static File mPackageWorkingPath = new File(mPackagePath, PATH_WORK);
    static File mPackageLogPath = new File(mPackagePath, PATH_LOG);

    static int CountEvent;
    static int activeEventCount = 0;
    final static int DELAY_AUTO_RECORDING = 2000;
    final static int DELAY_WAIT_EXIT_SECONDS = 3;
    static Handler mBackgroundImage, mBackgroundCamera;
    static Size mPreviewSize, mVideoSize, mImageSize;
    static ImageReader mImageReader, mPreviewReader;
    static boolean mIsRecording;
    static MediaRecorder mediaRecorder;
    static int speedInt = -1;

    static long INTERVAL_EVENT;
    static long INTERVAL_NORMAL;
    static byte [][] snapBytes;
    static int snapMapIdx = 0;
    static byte[] bytesRecordOff;
    static byte[] bytesRecordOn;
    static int VIDEO_FRAME_RATE;
    static int VIDEO_ENCODING_RATE;
    static long VIDEO_ONE_WORK_FILE_SIZE;
    static int IMAGE_BUFFER_MAX_IMAGES = 5;

    public static boolean USE_CUSTOM_VALUES;
    public static int MAX_IMAGES_SIZE;
    public static long INTERVAL_SNAP_SHOT_SAVE;
    public static long INTERVAL_LEFT_RIGHT;   // < SNAP_SHOT_INTERVAL


    static boolean viewFinder = true;
    static boolean photoCaptureLeft = false;
    static boolean photoSaved = false;

    static CameraDevice mCameraDevice = null;
    static CameraCharacteristics mCameraCharacteristics;
    static CaptureRequest.Builder mCaptureRequestBuilder;
    static CameraCaptureSession mCaptureSession;
    static Surface recordSurface = null;
    static Surface photoSurface = null;
    static Rect zoomBiggerL, zoomBiggerR, zoomHugeL, zoomHugeR;
    static String SUFFIX;
    static boolean zoomHuge = false;

    static void setSuffix(Context context) {
        INTERVAL_EVENT = 16 * 1000;
        INTERVAL_NORMAL = INTERVAL_EVENT * 4L;
        if (Build.MODEL.equals("SM-G965N")) {
            @SuppressLint("HardwareIds")
            String aID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
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

        switch (SUFFIX) {
            case "S":           // galaxy s9 blackbox
                MAX_IMAGES_SIZE = 119;
                INTERVAL_SNAP_SHOT_SAVE = 197;
                INTERVAL_LEFT_RIGHT = 91;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 30*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 20*1024*1024;
                break;
            case "P":           // galaxy s9 phone
                MAX_IMAGES_SIZE = 121;
                INTERVAL_SNAP_SHOT_SAVE = 192;
                INTERVAL_LEFT_RIGHT = 92;
                VIDEO_FRAME_RATE = 24;
                VIDEO_ENCODING_RATE = 30*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 20*1024*1024;
                break;
            case "A":           // galaxy A32
                MAX_IMAGES_SIZE = 135;
                INTERVAL_SNAP_SHOT_SAVE = 211;
                INTERVAL_LEFT_RIGHT = 110;
                VIDEO_FRAME_RATE = 24;
                VIDEO_ENCODING_RATE = 24*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 32*1024*1024;
                break;
        }
    }
}