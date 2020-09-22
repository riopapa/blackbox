package com.urrecliner.blackbox;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.util.Size;
import android.view.TextureView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Vars {

    static Activity mActivity = null;
    static Context mContext = null;
    static boolean mExitApplication = false;

    static Utils utils = new Utils();
    static VideoUtils videoUtils = new VideoUtils();
    static CameraUtils cameraUtils = new CameraUtils();
    static StartStopExit startStopExit = new StartStopExit();
    static GatherDiskSpace gatherDiskSpace = new GatherDiskSpace();
    static OBDAccess obdAccess = new OBDAccess();
    static DirectionSensor directionSensor = new DirectionSensor();
    static DisplayBattery displayBattery = new DisplayBattery();
    static GPSTracker gpsTracker;
    static DisplayTime displayTime = new DisplayTime();

    static TextView vTextDate = null;
    static TextView vTextTime = null;
    static TextView vTextCountEvent = null;
    static TextView vTextSpeed = null;
    static TextView vKm = null;
    static TextView vTextLogInfo = null;
    static TextView vTextActiveCount = null;
    static TextView vTextRecord = null;
    static TextView vTextBattery = null;
    static ImageView vImgBattery = null;
    static ImageView vExitApp = null;

    static ImageButton vBtnEvent = null;
    static TextureView vPreviewView;
    static ImageButton vBtnRecord;
    static ImageView vCompass = null;
    static ImageView vSatellite = null;

    static SharedPreferences sharedPref;

    static final String FORMAT_LOG_TIME = "yy-MM-dd HH.mm.ss.SSS";
    static final String FORMAT_DATE = "yy-MM-dd";
    static SimpleDateFormat sdfDate = new SimpleDateFormat(FORMAT_DATE, Locale.getDefault());
    static SimpleDateFormat sdfLogTime = new SimpleDateFormat(FORMAT_LOG_TIME, Locale.getDefault());
    static final String DATE_PREFIX = "V";

    private static final String PATH_PACKAGE = "BlackBox";
    private static final String PATH_EVENT = "event";
    private static final String PATH_EVENT_JPG = "jpgTemp";
    private static final String PATH_NORMAL = "normal";
    private static final String PATH_WORK = "work";
    private static final String PATH_LOG = "log";

    static File mPackagePath = new File(Environment.getExternalStorageDirectory(), PATH_PACKAGE);
    static File mPackageEventPath = new File(mPackagePath, PATH_EVENT);
    static File mPackageEventJpgTempPath = new File(mPackagePath, PATH_EVENT_JPG);
    static File mPackageNormalPath = new File(mPackagePath, PATH_NORMAL);
    static File mPackageNormalDatePath = new File(mPackageNormalPath, DATE_PREFIX+utils.getMilliSec2String(System.currentTimeMillis(), FORMAT_DATE));
    static File mPackageWorkingPath = new File(mPackagePath, PATH_WORK);
    static File mPackageLogPath = new File(mPackagePath, PATH_LOG);

    static long VIDEO_ONE_WORK_FILE_SIZE = 32 * 100000;
    static int VIDEO_ENCODING_RATE = 3000*1000;
    static int VIDEO_FRAME_RATE = 36;

    static int CountEvent;
    static int activeEventCount = 0;
    final static String DELAY_AUTO_RECORDING = "5000";
    final static int DELAY_WAIT_EXIT = 5;
    final static String DELAY_I_WILL_BACK = "40000";
    final static float LENS_FOCUS_FAR = 4f;   // 0: infinite 10: nearest
    final static float LENS_FOCUS_NEAR = 7f;   // 0: infinite 10: nearest
    static Handler mBackgroundImage;
    static Size mPreviewSize, mVideoSize, mImageSize;
    static ImageReader mImageReader, mPreviewReader;
    static boolean mIsRecording;
    static MediaRecorder mediaRecorder = new MediaRecorder();
    static int speedInt = 0;
    static boolean tryNear = false;
    static CaptureRequest.Builder mCaptureRequestBuilder;
    static CameraCaptureSession mCaptureSession;

    final static long INTERVAL_EVENT = 15 * 1000;
    final static int SNAP_SHOT_INTERVAL = 200;
    final static int MAX_IMAGES_SIZE = (int) ((INTERVAL_EVENT * 128 / 100) / SNAP_SHOT_INTERVAL);
    final static long INTERVAL_NORMAL = INTERVAL_EVENT * 6;
    static byte [][] snapBytes;
     static int snapMapIdx = 0;
    static NormalMerge normalMerge = new NormalMerge();

    final static int ASK_SPEED_INTERVAL = 400;
    static long gpsUpdateTime = 0;
    static long nextNormalTime = 0;
    static boolean viewFinder = true;
    static float azimuth = 0;
    static boolean isCompassShown = false;

    static CameraManager cameraManager;
    static CameraDevice mCameraDevice = null;
}
