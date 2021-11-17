    package com.urrecliner.blackbox;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Size;

import androidx.core.content.ContextCompat;
import java.nio.ByteBuffer;

import static com.urrecliner.blackbox.Vars.IMAGE_BUFFER_MAX_IMAGES;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.INTERVAL_SNAP_SHOT_SAVE;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.mBackgroundCamera;
import static com.urrecliner.blackbox.Vars.mCameraCharacteristics;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mBackgroundImage;
import static com.urrecliner.blackbox.Vars.mCameraDevice;
import static com.urrecliner.blackbox.Vars.mCaptureRequestBuilder;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mImageReader;
import static com.urrecliner.blackbox.Vars.mImageSize;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPreviewReader;
import static com.urrecliner.blackbox.Vars.mPreviewSize;
import static com.urrecliner.blackbox.Vars.mVideoSize;
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.photoCaptureLeft;
import static com.urrecliner.blackbox.Vars.photoSaved;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.videoMain;
import static com.urrecliner.blackbox.Vars.zoomBiggerL;
import static com.urrecliner.blackbox.Vars.zoomBiggerR;
import static com.urrecliner.blackbox.Vars.zoomHuge;
import static com.urrecliner.blackbox.Vars.zoomHugeL;
import static com.urrecliner.blackbox.Vars.zoomHugeR;

    public class CameraSub {
    CameraManager mCameraManager;
    String mCameraId = null;

    void readyCamera() {
        setupCamera();
        connectCamera();
    }

    void setupCamera() {
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            assert mCameraManager != null;
            for(String cameraId : mCameraManager.getCameraIdList()){
//                utils.logOnly(logID, "cameraID="+cameraId);
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                if(mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
                } else
                    continue;
                StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                setCameraSize(map);

            }
        } catch (CameraAccessException e) {
            utils.logE("CameraSub", "CameraAccessException", e);
        }
        try {
            mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, IMAGE_BUFFER_MAX_IMAGES);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundImage);
            mPreviewReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.RGB_565, 1);
        } catch (Exception e) {
            utils.logE("CameraSub", "Exception ", e);
        }

//        CameraPreview.params.setZoom(Zoom);
//        CameraPreview.mCamera.setParameters(CameraPreview.params);

    }


// "LM-G710N" "0";
// "SM-G965N" "P";
// "SM-A325N" "A";

    private void setCameraSize(StreamConfigurationMap map) {

//        String model = Build.MODEL;
//        utils.logBoth(logID, "CamSize on "+model);
//        dumpVariousCameraSizes(map);
        map.getOutputFormats();

        switch (SUFFIX) {
            case "L":
            /* LG G7
                4656x3492 1.3, 4656x2620 1.8, 4656x2218 2.1, 4160x3120 1.3, 4160x2080 2.0, 4000x3000 1.3 ,
                4000x2250 1.8, 3840x2160 1.8, 3492x3492 1.0, 3264x2448 1.3, 3264x1836 1.8, 3264x1632 2.0 ,
                3264x1554 2.1, 2560x1920 1.3, 2560x1440 1.8, 2560x1080 2.4, 2048x1536 1.3, 1920x1080 1.8 ,
                1440x1080 1.3, 1440x960 1.5, 1440x720 2.0, 1408x1152 1.2, 1280x768 1.7, 1280x960 1.3 ,
                1280x720 1.8, 960x720 1.3, 960x540 1.8, 720x720 1.0, 720x540 1.3, 720x480 1.5, 640x480 1.3
             */
                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                    if (size.getWidth() == 720 && size.getHeight() == 480)
                        mPreviewSize = size;
                    else if (size.getWidth() == 4656 && size.getHeight() == 2620)
                        mImageSize = size;
                    else if (size.getWidth() == 3264 && size.getHeight() == 1836)
                        mVideoSize = size;
                }
                break;
            case "P":
            case "S":
            /* galaxy s9+
            4032x3024 1.3, 4032x2268 1.8, 4032x1960 2.1, 3024x3024 1.0, 3984x2988 1.3, 3840x2160 1.8 ,
            3264x2448 1.3, 3264x1836 1.8, 2976x2976 1.0, 2880x2160 1.3, 2560x1440 1.8, 2160x2160 1.0 ,
            2224x1080 2.1, 2048x1152 1.8, 1920x1080 1.8, 1440x1080 1.3, 1088x1088 1.0, 1280x720 1.8 ,
            1056x704 1.5, 1024x768 1.3, 960x720 1.3, 960x540 1.8, 800x450 1.8, 720x720 1.0 ,
            720x480 1.5, 640x480 1.3, 352x288 1.2, 320x240 1.3, 256x144 1.8, 176x144 1.2 ,
             */
                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                    if (size.getWidth() == 640 && size.getHeight() == 480)
                        mPreviewSize = size;
                    else if (size.getWidth() == 4032 && size.getHeight() == 2268)
                        mImageSize = size;
                    else if (size.getWidth() == 3264 && size.getHeight() == 1836)
                        mVideoSize = size;
                }
                break;
            case "A":
            /* galaxy A32
                2560x1440 1.8, 1920x1080 1.8, 1440x1080 1.3, 1280x960 1.3, 1280x720 1.8 ,
                1088x1088 1.0, 960x720 1.3, 720x480 1.5, 640x480 1.3, 512x384 1.3, 512x288 1.8 ,
                 384x384 1.0, 352x288 1.2, 320x240 1.3, 256x144 1.8, 176x144 1.2 ,
             */
//                dumpVariousCameraSizes(map);
                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                    if (size.getWidth() == 640 && size.getHeight() == 480)
                        mPreviewSize = size;
                    else if (size.getWidth() == 2560 && size.getHeight() == 1440)
                        mImageSize = size;
                    else if (size.getWidth() == 1920 && size.getHeight() == 1080)
                        mVideoSize = size;
                }
                break;
            default:
                utils.logBoth("Model", "size undefined");
        }
    }

    private void dumpVariousCameraSizes(StreamConfigurationMap map) {

        String sb = "// DUMP CAMERA POSSIBLE SIZES // ";
        for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
            sb += size.getWidth()+"x"+ size.getHeight()+
                    String.format(" %,3.1f , ", (float)size.getWidth() / (float)size.getHeight());
        }
        utils.logOnly("Camera Size",sb);
    }

    void connectCamera() {
        CameraManager cameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if(ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                assert cameraManager != null;
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundCamera);
            }
        } catch (CameraAccessException e) {
            utils.logE("CameraSub", "connectCamera Exception ", e);
        }
    }

    private final CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            if (mCameraDevice == null)
                mCameraDevice = camera;
            photoCaptureLeft = !leftRight;
            if(mIsRecording) {
                videoMain.prepareRecord();
                mediaRecorder.start();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    long shotTime = 0;
    boolean leftRight = false;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = reader -> {
        long  nowTime = System.currentTimeMillis();
        if (nowTime < shotTime || !mIsRecording) {
            return;
        }
        if (shotTime == 0)
            shotTime = nowTime;

        Image image = reader.acquireLatestImage();
        if (photoCaptureLeft == leftRight) {
            try {
                image.close();
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        shotTime += INTERVAL_SNAP_SHOT_SAVE;
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            snapBytes[snapMapIdx] = bytes;
        } catch (Exception e) {
            utils.showOnly("img", "buffer short " + snapMapIdx);
        }
        image.close();
        leftRight = !leftRight;
        if (mIsRecording) {
            snapMapIdx++;
            if (snapMapIdx >= MAX_IMAGES_SIZE)
                snapMapIdx = 0;
        }
        photoSaved = true;

    };
}