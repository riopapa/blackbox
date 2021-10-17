package com.urrecliner.blackbox;

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
import android.os.Environment;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class Vars {

    static Activity mActivity = null;
    static Context mContext = null;
    static boolean mExitApplication = false;

    static Utils utils = new Utils();
    static VideoMain videoMain = new VideoMain();
    static PhotoCapture photoCapture = new PhotoCapture();
    static StartStopExit startStopExit = new StartStopExit();

    static OBDAccess obdAccess = new OBDAccess();
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
    static LinearLayout lNewsLine = null;

    static ImageButton vBtnEvent = null;
    static TextureView vPreviewView;
    static ImageButton vBtnRecord;

    static SharedPreferences sharedPref;
    static int chronoKiloMeter = 0;
    static String chronoNowDate = null;
    static int todayKiloMeter = 0;

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
    final static int DELAY_AUTO_RECORDING = 3000;
    final static int DELAY_WAIT_EXIT_SECONDS = 3;
    static Handler mBackgroundImage, mBackgroundCamera;
    static Size mPreviewSize, mVideoSize, mImageSize;
    static ImageReader mImageReader, mPreviewReader;
    static boolean mIsRecording;
    static MediaRecorder mediaRecorder;
    static int speedInt = 0;

    final static long INTERVAL_EVENT = 15 * 1000;
    final static long INTERVAL_NORMAL = INTERVAL_EVENT * 6;
    static byte [][] snapBytes;
    static int snapMapIdx = 0;

    static int MAX_IMAGES_SIZE;
    static long SNAP_SHOT_INTERVAL;
    static int VIDEO_FRAME_RATE;
    static int VIDEO_ENCODING_RATE;
    static long VIDEO_ONE_WORK_FILE_SIZE;

    static boolean viewFinder = true;

    static CameraDevice mCameraDevice = null;
    static CameraCharacteristics mCameraCharacteristics;
    static CaptureRequest.Builder mCaptureRequestBuilder;
    static CameraCaptureSession mCaptureSession;
    static Surface recordSurface = null;
    static Surface photoSurface = null;
    static Rect zoomBiggerL, zoomBiggerR;
    static String SUFFIX;

    static ArrayList<ChronoLog> chronoLogs = null;

    public static class ChronoLog {
        String chroDate;
        int chroKilo;
        int todayKilo;
    }
}