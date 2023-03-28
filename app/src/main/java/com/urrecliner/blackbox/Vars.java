package com.urrecliner.blackbox;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.urrecliner.blackbox.utility.ImageStack;

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
    static DisplayBattery displayBattery;
    static GPSTracker gpsTracker;
    static DisplayTime displayTime;

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
    static TextView tvDegree = null;

    static ImageButton vBtnEvent = null;
    static TextureView vPreviewView;
    static SurfaceTexture surface_Preview = null;

    static ImageView vBtnRecord;

    public static SharedPreferences sharedPref;
    public static SharedPreferences.Editor sharedEditor;
    public static int share_image_size;
    public static long share_snap_interval;
    public static long share_left_right_interval;   // < SNAP_SHOT_INTERVAL

    static final String FORMAT_TIME = "yy-MM-dd HH.mm.ss";
    static final String FORMAT_DATE = "yy-MM-dd";
    static SimpleDateFormat sdfDate = new SimpleDateFormat(FORMAT_DATE, Locale.getDefault());
    static SimpleDateFormat sdfTime = new SimpleDateFormat(FORMAT_TIME, Locale.getDefault());
    static final String DATE_PREFIX = "V";

    static File mPackageWorkingPath, mPackageEventPath, mPackageEventJpgPath,
            mPackageNormalPath, mPackageNormalDatePath, mPackageLogPath;
    static int CountEvent;
    static int activeEventCount = 0;
    final static int DELAY_AUTO_RECORDING = 3000;
    final static int DELAY_WAIT_EXIT_SECONDS = 3;
    static Handler mBackgroundImage, mBackgroundCamera;
    static Size mPreviewSize, mVideoSize, mImageSize;
    static ImageReader mImageReader;
    static boolean mIsRecording;
    static MediaRecorder mediaRecorder;
    static int speedInt = -1;

    static long INTERVAL_EVENT;
    static long INTERVAL_NORMAL;

    static byte[] shot_00, shot_01, shot_02, shot_03;

    static ImageStack imageStack;
    static int VIDEO_FRAME_RATE;
    static int VIDEO_ENCODING_RATE;
    static long VIDEO_ONE_WORK_FILE_SIZE;
    static int IMAGE_BUFFER_MAX_IMAGES = 3;

    public enum PhoneE {  B, P, N, A} // S9Black, S9Phone, Note20, A32
    static boolean viewFinderActive = true;
    static boolean photoSaved = false;

    static CameraDevice mCameraDevice = null;
    static CameraCharacteristics mCameraCharacteristics;
    static CaptureRequest.Builder mVideoRequestBuilder, mCameraBuilder;
    static CameraCaptureSession mCaptureSession;
    static Surface recordSurface = null;
    static Surface photoSurface = null;
    static Surface previewSurface = null;
    static Rect zoomBiggerL, zoomBiggerR, zoomHugeL, zoomHugeR, zoomHugeC;
    static PhoneE SUFFIX;
    static boolean zoomHuge = false;

    static void setSuffix(Context context) {
        INTERVAL_EVENT = 25 * 1000;
        INTERVAL_NORMAL = INTERVAL_EVENT * 4L;
        switch (Build.MODEL) {
            case "SM-G965N":
//                @SuppressLint("HardwareIds")
//                String aID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
//                if (aID.endsWith("6ccd"))
                    SUFFIX = PhoneE.P;
//                else
//                    SUFFIX = PhoneE.B;
                break;
            case "SM-N986N":
                SUFFIX = PhoneE.N;
                break;
            case "SM-A325N":
                SUFFIX = PhoneE.A;
                break;
            default:
                utils.logBoth("Vars", "Model="+Build.MODEL);
                break;
        }

        switch (SUFFIX) {
            case P:           // galaxy s9 phone
                share_image_size = 121;
                share_snap_interval = 192;
                share_left_right_interval = 92;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 30*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 30*1024*1024;
                break;
            case N:           // galaxy note 20
                share_image_size = 121;
                share_snap_interval = 172;
                share_left_right_interval = 112;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 30*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 34*1024*1024;
                break;
            case B:           // galaxy s9 blackbox
                share_image_size = 119;
                share_snap_interval = 197;
                share_left_right_interval = 91;
                VIDEO_FRAME_RATE = 24;
                VIDEO_ENCODING_RATE = 24*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 30*1024*1024;
                break;
            case A:           // galaxy A32
                share_image_size = 115;
                share_snap_interval = 211;
                share_left_right_interval = 140;
                VIDEO_FRAME_RATE = 24;
                VIDEO_ENCODING_RATE = 24*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 32*1024*1024;
                break;
        }
    }
}