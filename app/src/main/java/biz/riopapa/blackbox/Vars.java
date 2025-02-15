package biz.riopapa.blackbox;

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
import android.os.Handler;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import biz.riopapa.blackbox.utility.ImageStack;

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
    static ScrollView scrollLog = null;
    static TextView vTextLogInfo = null;
    static TextView vTextActiveCount = null;
    static TextView vTextRecord = null;
    static TextView vTextBattery = null;
    static ImageView vImgBattery = null;
    static ImageView vPower = null;
    static TextView tvDegree = null;
    static ConstraintLayout mainLayout;
    static ImageButton vBtnEvent = null;
    static TextureView vPreviewView;
    static SurfaceTexture surface_Preview = null;

    static public int nextCount = 0;

    public static SharedPreferences sharedPref;
    public static SharedPreferences.Editor sharedEditor;
    public static int share_image_size;
    public static long share_snap_interval;
    public static long share_left_right_interval;   // < SNAP_SHOT_INTERVAL
    public static long share_event_sec; // Event Duration in sec
    public static long normal_duration;   // Normal Duration in mili Sec
    public static long share_work_size; // wor file size
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
    final static int DELAY_WAIT_EXIT_SECONDS = 2;
    static Handler mBackgroundImage, mBackgroundCamera;
    static Size mPreviewSize, mVideoSize, mImageSize;
    static ImageReader mImageReader;
    static boolean mIsRecording;
    static MediaRecorder mediaRecorder;
    static int speedNow = -1;

    static byte[] shot_00, shot_01;

    static ImageStack imageStack;
    public static int VIDEO_FRAME_RATE;
    public static int VIDEO_ENCODING_RATE;
    public static long VIDEO_ONE_WORK_FILE_SIZE;
    public  static int IMAGE_BUFFER_MAX_IMAGES = 3;

    public static PhoneE SUFFIX;
    public enum PhoneE {N, H } // S10Phone, Note20
    static boolean viewFinderActive = true;
    static CameraDevice mCameraDevice = null;
    static CameraCharacteristics mCameraCharacteristics;
    static CaptureRequest.Builder mVideoRequestBuilder, mCameraBuilder;
    static CaptureRequest mCameraBigRequest, mCameraLeftRequest, mCameraRightRequest, mCameraShotRequest;
    static CameraCaptureSession mCaptureSession;
    static Surface recordSurface = null;
    static Surface photoSurface = null;
    static Surface previewSurface = null;
    static Rect rectNormal, rectLeft, rectRight, rectBigShot;

}