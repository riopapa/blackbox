package com.urrecliner.blackbox;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Size;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.ACTIVITY_SERVICE;
import static com.urrecliner.blackbox.Vars.VIDEO_ENCODING_RATE;
import static com.urrecliner.blackbox.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.VIDEO_FRAME_RATE;
import static com.urrecliner.blackbox.Vars.VIDEO_ONE_WORK_FILE_SIZE;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mBackgroundImage;
import static com.urrecliner.blackbox.Vars.mBackgroundPreview;
import static com.urrecliner.blackbox.Vars.mCameraDevice;
import static com.urrecliner.blackbox.Vars.mCaptureRequestBuilder;
import static com.urrecliner.blackbox.Vars.mCaptureSession;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.mImageReader;
import static com.urrecliner.blackbox.Vars.mImageSize;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.mPrevBuilder;
import static com.urrecliner.blackbox.Vars.mPrevSession;
import static com.urrecliner.blackbox.Vars.mPreviewReader;
import static com.urrecliner.blackbox.Vars.mPreviewSize;
import static com.urrecliner.blackbox.Vars.mVideoFileName;
import static com.urrecliner.blackbox.Vars.mVideoSize;
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextRecord;
import static com.urrecliner.blackbox.Vars.vTextureView;
import static com.urrecliner.blackbox.Vars.videoUtils;

public class VideoUtils {

    private String logID = "videoUtils";
    private String mCameraId;
    void setupCamera() {
//        String model = Build.MODEL;
//        utils.logBoth(logID, "Start setupCamera on ["+model+"]");
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            assert cameraManager != null;
            for(String cameraId : cameraManager.getCameraIdList()){
//                utils.logOnly(logID, "cameraID="+cameraId);
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK)
                    mCameraId = cameraId;
                else
                    continue;
//                utils.logOnly(logID, "M cameraID="+cameraId);
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                setCameraSize(map);
//                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
//                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
            }
//                mPreviewReader.setOnImageAvailableListener(mOnPreviewAvailableListener, mBackgroundPreview);
//                mPreviewReader.setOnImageAvailableListener(null, null);
        } catch (CameraAccessException e) {
            utils.logE(logID, "CameraAccessException", e);
        }
        utils.logOnly(logID, "mPrev "+mPreviewSize.getWidth()+"x"+mPreviewSize.getHeight());
        utils.logOnly(logID, "mImage "+mImageSize.getWidth()+"x"+mImageSize.getHeight()+" array "+MAX_IMAGES_SIZE);
        utils.logOnly(logID, "mVideo "+mVideoSize.getWidth()+"x"+mVideoSize.getHeight());
        try {
            mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 6); // MAX_IMAGES_SIZE);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundImage);
            mPreviewReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 1);
        } catch (Exception e) {
            utils.logE(logID, "Exception ", e);
        }
    }

    private void setCameraSize(StreamConfigurationMap map) {
        String model = Build.MODEL;
//        utils.logBoth(logID, "setCameraSize on "+model);

        if (model.equals("SM-G965N")) {
            /* galaxy s9+
            4032x3024 1.3 , 4032x2268 1.8 , 4032x1960 2.1 , 3024x3024 1.0 , 3984x2988 1.3 , 3840x2160 1.8 ,
            3264x2448 1.3 , 3264x1836 1.8 , 2976x2976 1.0 , 2880x2160 1.3 , 2560x1440 1.8 , 2160x2160 1.0 ,
            2224x1080 2.1 , 2048x1152 1.8 , 1920x1080 1.8 , 1440x1080 1.3 , 1088x1088 1.0 , 1280x720 1.8 ,
            1056x704 1.5 , 1024x768 1.3 , 960x720 1.3 , 960x540 1.8 , 800x450 1.8 , 720x720 1.0 ,
            720x480 1.5 , 640x480 1.3 , 352x288 1.2 , 320x240 1.3 , 256x144 1.8 , 176x144 1.2 ,
             */
//            String sb = "";
//            for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
//                sb += size.getWidth()+"x"+ size.getHeight()+
//                        String.format(" %,3.1f , ", (float)size.getWidth() / (float)size.getHeight());
//            }
//            Log.w("SIZE",sb);

                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                if (size.getWidth() == 720 && size.getHeight() == 480)
                    mPreviewSize = size;
                else if (size.getWidth() == 4032 && size.getHeight() == 2268)
                    mImageSize = size;
                else if (size.getWidth() == 2048 && size.getHeight() == 1152)
                    mVideoSize = size;
            }
        }
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
                mVideoFileName = videoUtils.getOutputFileName(0).toString();
//                utils.logBoth(logID, "Step 2 prepareRecord");
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

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) mActivity.getSystemService(ACTIVITY_SERVICE);
        assert activityManager != null;
        activityManager.getMemoryInfo(mi);
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
    private SurfaceTexture surface_Texture = null;
    private Surface previewSurface = null;
    private Surface recordSurface = null;
    void prepareRecord() {

        if (isPrepared)
            return;
        try {
            setupMediaRecorder();
            vTextureView = mActivity.findViewById(R.id.textureView);
            if (vTextureView == null)
                utils.logOnly(logID,"Texture NULL");
            surface_Texture = vTextureView.getSurfaceTexture();
            if (surface_Texture == null)
                utils.logOnly(logID,"surface_Texture NULL");
            surface_Texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error AA ///", e);
        }
        try {
            previewSurface = new Surface(surface_Texture);
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error BB ///", e);
        }
        if (previewSurface == null) {
            utils.logBoth(logID, "previewSurface is null /,/,/,");
            return;
        }
        try {
            recordSurface = mediaRecorder.getSurface();
            mCaptureRequestBuilder.addTarget(recordSurface);
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error recordSurface ///", e);
        }
        if (recordSurface == null) {
            utils.logBoth(logID, "recordSurface is null ------");
            return;
        }
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface, mImageReader.getSurface()),
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mCaptureSession = session;
                    try {
                        mCaptureSession.setRepeatingRequest(
                                mCaptureRequestBuilder.build(), null, null
                        );
                    } catch (CameraAccessException e) {
                        utils.logBoth(logID, "setRepeatingRequest Error");
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    utils.logBoth(logID, "onConfigureFailed: while prepareRecord");
                }
            }, null);

        } catch (Exception e) {
            utils.logE(logID, "Prepare Error BB ", e);
        }
        isPrepared = true;
    }

    private void setupMediaRecorder() throws IOException {

//        utils.logBoth(logID," setup Media");
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncodingBitRate(VIDEO_ENCODING_RATE); // 1000000
        mediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
        mediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setOutputFile(getOutputFileName(0).toString());
        mediaRecorder.setMaxFileSize(VIDEO_ONE_WORK_FILE_SIZE);
        mediaRecorder.prepare();
        mediaRecorder.setNextOutputFile(getOutputFileName(3000));
        setUpNextFile();
    }

    private File getOutputFileName(long after) {
        String time = utils.getMilliSec2String(System.currentTimeMillis() + after, FORMAT_LOG_TIME);
        return new File(mPackageWorkingPath, time + ".mp4");
    }

    private void setUpNextFile() {

        mediaRecorder.setOnInfoListener((mediaRecorder, what, extra) -> {
            switch (what) {
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                    utils.logOnly(logID,"MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED");
//                    startStopExit.stopVideo();
//                    startStopExit.startVideo();
                    utils.logBoth(logID, "***** MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED ***");
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    break;
                case MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED:
                    prepareNextFile();
                    break;
                default:
//                        utils.log("d","default " + what);
            }
        });
    }

    private final Handler nextFileHandler = new Handler() {
        public void handleMessage(Message msg) { assignNextFile();
        }
    };
    private void prepareNextFile() {
        if (mIsRecording && !mExitApplication) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    nextFileHandler.sendEmptyMessage(0);
                }
            }, 100);
        }
    }

    private int nextCount = 0;
    private void assignNextFile() {
        if (mIsRecording) {
            try {
                File nextFileName = getOutputFileName(3000);
                mediaRecorder.setNextOutputFile(nextFileName);
                String s = ++nextCount + "";
                vTextRecord.setText(s);
//                utils.log("assign " + s, nextFileName.toString());
            } catch (IOException e) {
                utils.logE("Error", "nxtFile", e);
            }
        }
    }

    void startPreview() {
        utils.logOnly(logID, "startPreview 1");

        surface_Texture = vTextureView.getSurfaceTexture();
        surface_Texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        Surface preview_Surface = new Surface(surface_Texture);

        try {
            mPrevBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPrevBuilder.addTarget(preview_Surface);
            mCameraDevice.createCaptureSession(Arrays.asList(preview_Surface, mPreviewReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
//                            utils.log(logID, "onConfigured: startPreview");
                            mPrevSession = session;
                            try {
                                mPrevSession.setRepeatingRequest(mPrevBuilder.build(),
                                        null, mBackgroundPreview);
                            } catch (Exception e) {
                                utils.logBoth(logID, "mPreSession error"+e.toString());
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            utils.logOnly(logID, "onConfigureFailed: startPreview");
                        }
                    }, null);
        } catch (Exception e) {
            utils.logE(logID, "// startPreview //", e);
        }
        utils.logOnly(logID, "startPreview 2");
    }
}
