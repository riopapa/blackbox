package com.urrecliner.blackbox;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
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
    static TextView vTodayKms = null;
    static TextView vKm = null;
    static TextView vTextLogInfo = null;
    static TextView vTextActiveCount = null;
    static TextView vTextRecord = null;
    static TextView vTextBattery = null;
    static ImageView vImgBattery = null;
    static ImageView vExitApp = null;

    static ImageButton vBtnEvent = null;
    static TextureView vTextureView;
    static ImageButton vBtnRecord;
    static ImageView vCompass = null;
    static ImageView vSatellite = null;

    static SharedPreferences sharedPref;
    static SharedPreferences.Editor editor;
    static String todayStr;

    static final String FORMAT_LOG_TIME = "yy-MM-dd HH.mm.ss.SSS";
    static final String FORMAT_DATE = "yy-MM-dd";

    private static final String PATH_PACKAGE = "BlackBox";
    private static final String PATH_EVENT = "event";
    private static final String PATH_NORMAL = "normal";
    private static final String PATH_WORK = "work";
    private static final String PATH_LOG = "log";

    static File mPackagePath = new File(Environment.getExternalStorageDirectory(), PATH_PACKAGE);
    static File mPackageEventPath = new File(mPackagePath, PATH_EVENT);
    static File mPackageNormalPath = new File(mPackagePath, PATH_NORMAL);
    static File mPackageNormalDatePath = new File(mPackageNormalPath, utils.getMilliSec2String(System.currentTimeMillis(), FORMAT_DATE));
    static File mPackageWorkingPath = new File(mPackagePath, PATH_WORK);
    static File mPackageLogPath = new File(mPackagePath, PATH_LOG);

    static double evtLatitude = 0;
    static double evtLongitude = 0;

    static long ONE_WORK_FILE_SIZE = 25 * 100000;
    static int ENCODING_RATE = 3000*1000;
    static int FRAME_RATE = 30;

    static int CountEvent;
    static int activeEventCount = 0;
    final static int DELAY_AUTO_RECORD = 5;
    final static int DELAY_WAIT_EXIT = 10;
    final static int DELAY_I_WILL_BACK = 50;

    static Handler mBackgroundImage, mBackgroundPreview;
    static Size mPreviewSize, mVideoSize, mImageSize;
    static ImageReader mImageReader, mPreviewReader;
    static CameraDevice mCameraDevice;
    static boolean mIsRecording;
    static MediaRecorder mediaRecorder = new MediaRecorder();

    static String mVideoFileName;
    static CaptureRequest.Builder mCaptureRequestBuilder, mPrevBuilder;
    static CameraCaptureSession mCaptureSession, mPrevSession;

    final static long INTERVAL_NORMAL = 80 * 1000;
    final static long INTERVAL_EVENT = 17 * 1000;
    final static int SNAP_SHOT_INTERVAL = 500;
    final static int MAX_IMAGES_SIZE = (int) ((INTERVAL_EVENT * 2 + INTERVAL_EVENT) / SNAP_SHOT_INTERVAL);
    static byte [][] snapBytes = new byte[MAX_IMAGES_SIZE][];
    static int snapMapIdx = 0;
    static NormalMerge normalMerge = new NormalMerge();

    static long gpsUpdateTime = 0;
    static boolean viewFinder = true;
    static String speedNow = "now", speedOld = "old";
    static float azimuth = 0;
    static boolean isCompassShown = false;
}
