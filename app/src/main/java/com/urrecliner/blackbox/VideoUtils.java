package com.urrecliner.blackbox;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.FORMAT_TIME;
import static com.urrecliner.blackbox.Vars.VIDEO_ENCODING_RATE;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.VIDEO_FRAME_RATE;
import static com.urrecliner.blackbox.Vars.VIDEO_ONE_WORK_FILE_SIZE;
import static com.urrecliner.blackbox.Vars.cameraCharacteristics;
import static com.urrecliner.blackbox.Vars.cameraManager;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mBackgroundImage;
import static com.urrecliner.blackbox.Vars.mCameraDevice;
import static com.urrecliner.blackbox.Vars.mCaptureRequestVideoBuilder;
import static com.urrecliner.blackbox.Vars.mCaptureSession;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mImageReader;
import static com.urrecliner.blackbox.Vars.mImageSize;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.mPreviewReader;
import static com.urrecliner.blackbox.Vars.mPreviewSize;
import static com.urrecliner.blackbox.Vars.mVideoSize;
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.previewSurface;
import static com.urrecliner.blackbox.Vars.recordSurface;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextRecord;
import static com.urrecliner.blackbox.Vars.vPreviewView;
import static com.urrecliner.blackbox.Vars.zoom;

public class VideoUtils {

    String mCameraId = null;
    private String logID = "videoUtils";

    void setupCamera() {
        cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            assert cameraManager != null;
            for(String cameraId : cameraManager.getCameraIdList()){
//                utils.logOnly(logID, "cameraID="+cameraId);
                cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
//                    float maxzoom = (cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*10;
//                    utils.logOnly("zoom","maxzoom="+maxzoom);
                }
                else
                    continue;
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                setCameraSize(map);

            }
        } catch (CameraAccessException e) {
            utils.logE(logID, "CameraAccessException", e);
        }
        try {
            mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 10); // MAX_IMAGES_SIZE);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundImage);
            mPreviewReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.RGB_565, 1);
        } catch (Exception e) {
            utils.logE(logID, "Exception ", e);
        }

    }

    private void setCameraSize(StreamConfigurationMap map) {

        String model = Build.MODEL;
//        utils.logBoth(logID, "CamSize on "+model);
//        dumpVariousCameraSizes(map);
        map.getOutputFormats();

        switch (model) {
            case "SM-G950N":
            case "SM-G965N":
            /* galaxy s8
            4032x3024 1.3 , 4032x2268 1.8 , 3024x3024 1.0 , 3984x2988 1.3 , 3264x2448 1.3 , 3264x1836 1.8 , 2976x2976 1.0 ,
            2880x2160 1.3 , 2560x1440 1.8 , 2160x2160 1.0 , 2048x1152 1.8 , 1920x1080 1.8 , 1440x1080 1.3 , 1088x1088 1.0 ,
            1280x720 1.8 , 1056x704 1.5 , 1024x768 1.3 , 960x720 1.3 , 800x450 1.8 , 720x720 1.0 , 720x480 1.5 , 640x480 1.3 ,
             */
            /* galaxy s9+
            4032x3024 1.3 , 4032x2268 1.8 , 4032x1960 2.1 , 3024x3024 1.0 , 3984x2988 1.3 , 3840x2160 1.8 ,
            3264x2448 1.3 , 3264x1836 1.8 , 2976x2976 1.0 , 2880x2160 1.3 , 2560x1440 1.8 , 2160x2160 1.0 ,
            2224x1080 2.1 , 2048x1152 1.8 , 1920x1080 1.8 , 1440x1080 1.3 , 1088x1088 1.0 , 1280x720 1.8 ,
            1056x704 1.5 , 1024x768 1.3 , 960x720 1.3 , 960x540 1.8 , 800x450 1.8 , 720x720 1.0 ,
            720x480 1.5 , 640x480 1.3 , 352x288 1.2 , 320x240 1.3 , 256x144 1.8 , 176x144 1.2 ,
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
//            case "LM-G710N":
//            /* LG G7
//                4656x3492 1.3 , 4656x2620 1.8 , 4656x2218 2.1 , 4160x3120 1.3 , 4160x2080 2.0 , 4000x3000 1.3 ,
//                4000x2250 1.8 , 3840x2160 1.8 , 3492x3492 1.0 , 3264x2448 1.3 , 3264x1836 1.8 , 3264x1632 2.0 ,
//                3264x1554 2.1 , 2560x1920 1.3 , 2560x1440 1.8 , 2560x1080 2.4 , 2048x1536 1.3 , 1920x1080 1.8 ,
//                1440x1080 1.3 , 1440x960 1.5 , 1440x720 2.0 , 1408x1152 1.2 , 1280x768 1.7 , 1280x960 1.3 ,
//                1280x720 1.8 , 960x720 1.3 , 960x540 1.8 , 720x720 1.0 , 720x540 1.3 , 720x480 1.5 , 640x480 1.3
//             */
//                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
//                    if (size.getWidth() == 720 && size.getHeight() == 480)
//                        mPreviewSize = size;
//                    else if (size.getWidth() == 4000 && size.getHeight() == 2250)
//                        mImageSize = size;
//                    else if (size.getWidth() == 1440 && size.getHeight() == 960)
//                        mVideoSize = size;
//                }
//                break;
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
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundImage);
            }
        } catch (CameraAccessException e) {
            utils.logE(logID, "connectCamera Exception ", e);
        }
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            if (mCameraDevice == null)
                mCameraDevice = camera;
            if(mIsRecording) {
                prepareRecord();
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

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = reader -> {
        Image image = reader.acquireLatestImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        if (mIsRecording) {
            snapBytes[snapMapIdx] = bytes;
            snapMapIdx++;
            if (snapMapIdx >= MAX_IMAGES_SIZE)
                snapMapIdx = 0;
        }
        image.close();
    };

    private boolean isPrepared = false;
    private SurfaceTexture surface_Preview = null;

    void prepareRecord() {

        if (isPrepared)
            return;
        try {
            setupMediaRecorder();
            vPreviewView = mActivity.findViewById(R.id.previewView);
            surface_Preview = vPreviewView.getSurfaceTexture();
            surface_Preview.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error preView AA ///", e);
        }
        try {
            previewSurface = new Surface(surface_Preview);
        } catch (Exception e) {
            utils.logE(logID, "Prepare surface_Preview Error BB ///", e);
        }
        try {
            mCaptureRequestVideoBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestVideoBuilder.addTarget(previewSurface);
            mCaptureRequestVideoBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
//            mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, LENS_FOCUS_FAR); // 0.0 infinite ~ 10f nearest
        } catch (Exception e) {
            utils.logE(logID, "Prepare mCaptureRequestBuilder Error CC ///", e);
        }

        if (previewSurface == null) {
            utils.logBoth(logID, "previewSurface is null ====");
            return;
        }
        try {
            recordSurface = mediaRecorder.getSurface();
            mCaptureRequestVideoBuilder.addTarget(recordSurface);
            mCaptureRequestVideoBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 2f); // 0.0 infinite ~ 10f nearest
            mCaptureRequestVideoBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            mCaptureRequestVideoBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error recordSurface ///", e);
        }
        if (recordSurface == null) {
            utils.logBoth(logID, "recordSurface is null ------");
            return;
        }
        buildCameraSession(1.23f);   // zoomFactor
        isPrepared = true;

//        try {
//            mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
//        } catch (Exception e) { e.printStackTrace();}
    }

    void buildCameraSession(float zoomFactor) {
//        startBackgroundLooper();
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface, mImageReader.getSurface()),
                    cameraStateCallBack(zoomFactor), null);
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error BB ", e);
        }
    }

    private CameraCaptureSession.StateCallback cameraStateCallBack(float zoomFactor) {
        return new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                mCaptureSession = session;
                zoom = new Zoom(cameraCharacteristics);
                zoom.setZoom(mCaptureRequestVideoBuilder, zoomFactor);
                utils.logOnly("zoom set","setZoom to "+zoomFactor);
                try {
//                       mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, LENS_FOCUS_FAR);
//                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                    mCaptureSession.setRepeatingRequest(
                            mCaptureRequestVideoBuilder.build(), null, null
                    );
                } catch (CameraAccessException e) {
                    utils.logBoth(logID, "setRepeatingRequest Error");
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                utils.logBoth(logID, "onConfigureFailed: while prepareRecord");
            }
        };
    }

    private void setupMediaRecorder() {

        utils.logBoth(logID," setup Media");
        mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);    // 1. setAudioSource
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);    // 2. setVideoSource
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);   // 3. setOutputFormat
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);  // 4. setAudioEncoder
        mediaRecorder.setVideoEncodingBitRate(VIDEO_ENCODING_RATE); // 1000000
        mediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
        mediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setOutputFile(getNextFileName(0).toString());
        mediaRecorder.setMaxFileSize(VIDEO_ONE_WORK_FILE_SIZE);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mediaRecorder.setNextOutputFile(getNextFileName(2000));
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaRecorder.setOnInfoListener((mediaRecorder, what, extra) -> {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED) {
                assignNextFile();
            }
        });
    }

    private File getNextFileName(long after) {
        String time = utils.getMilliSec2String(System.currentTimeMillis() + after, FORMAT_TIME);
        return new File(mPackageWorkingPath, time + ".mp4");
    }

    private int nextCount = 0;
    private void assignNextFile() {
        if (mIsRecording) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    try {
                        mediaRecorder.setNextOutputFile(getNextFileName(2000));
                    } catch (Exception e) {
                        utils.logE("Error", "nxtFile", e);
                    }
                }
            }, 10);
            String s = ++nextCount + "";
            vTextRecord.setText(s);
        }
    }

}
