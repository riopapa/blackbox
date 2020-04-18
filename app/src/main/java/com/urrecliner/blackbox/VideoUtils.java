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
        String model = Build.MODEL;
        utils.logBoth(logID, "Start setupCamera on ["+model+"]");
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            assert cameraManager != null;
            for(String cameraId : cameraManager.getCameraIdList()){
//                utils.logOnly(logID, "cameraID="+cameraId);
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                mCameraId = cameraId;
//                utils.logOnly(logID, "M cameraID="+cameraId);
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                setCameraSize(map);
//                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
//                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
            }
//                mPreviewReader.setOnImageAvailableListener(mOnPreviewAvailableListener, mBackgroundPreview);
//                mPreviewReader.setOnImageAvailableListener(null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        utils.logOnly(logID, "mPrevw "+mPreviewSize.getWidth()+"x"+mPreviewSize.getHeight());
        utils.logOnly(logID, "mImage "+mImageSize.getWidth()+"x"+mImageSize.getHeight()+" array "+MAX_IMAGES_SIZE);
        utils.logOnly(logID, "mVideo "+mVideoSize.getWidth()+"x"+mVideoSize.getHeight());
        try {
            mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 5); // MAX_IMAGES_SIZE);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundImage);
            mPreviewReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCameraSize(StreamConfigurationMap map) {
        String model = Build.MODEL;
        utils.logBoth(logID, "setCameraSize on "+model);
        if (model.equals("Nexus 6P")) {
            /* nexus 6p resolution
            3264 x 2448 : 1.3, 3200 x 2400 : 1.3, 2592 x 1944 : 1.3, 2688 x 1512 : 1.7, 2048 x 1536 : 1.3,
            1920 x 1080 : 1.7, 1600 x 1200 : 1.3, 1440 x 1080 : 1.3, 1280 x 960 : 1.3, 1280 x 768 : 1.6, 1280 x 720 : 1.7
            1024 x 768 : 1.3, 800 x 600 : 1.3, 864 x 480 : 1.8, 800 x 480 : 1.66, 2 720 x 480 : 1.5, 640 x 480 : 1.3
            640 x 360 : 1.7, 352 x 288 : 1.2, 320 x 240 : 1.3, 176 x 144 : 1.2, 160 x 120 : 1.3 */
            for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
    //                    Log.w("size", size.getWidth()+"x"+ size.getHeight());
                if (size.getWidth() == 640)
                    mPreviewSize = size;
                else if (size.getWidth() == 3200 && size.getHeight() == 2400)
                    mImageSize = size;
                else if (size.getWidth() == 1440 && size.getHeight() == 1080)
                    mVideoSize = size;
            }
        }
        if (model.equals("SM-G965N")) {
            /* galaxy s9+
            4032 x 3024 : 1.3, 4032 x 2268 : 1.7, 4032 x 1960 : 2.0, 3024 x 3024 : 1.0, 3984 x 2988 : 1.3, 3840 x 2160 : 1.7,
            3264 x 2448 : 1.3, 3264 x 1836 : 1.7, 2976 x 2976 : 1.0, 2880 x 2160 : 1.3, 2560 x 1440 : 1.7, 2160 x 2160 : 1.0,
            2224 x 1080 : 2.0, 2048 x 1152 : 1.7, 1920 x 1080 : 1.7, 1440 x 1080 : 1.3, 1088 x 1088 : 1.0, 1280 x 720 : 1.7,
            1056 x 704 : 1.5, 1024 x 768 : 1.3, 960 x 720 : 1.3, 960 x 540 : 1.7, 800 x 450 : 1.7, 720 x 720 : 1.0,
            720 x 480 : 1.5, 640 x 480 : 1.3, 352 x 288 : 1.2 */
            for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
    //                    Log.w("size", size.getWidth()+"x"+ size.getHeight());
                    if (size.getWidth() == 640)
                        mPreviewSize = size;
                    else if (size.getWidth() == 4032 && size.getHeight() == 2268)
                        mImageSize = size;
                    else if (size.getWidth() == 2560 && size.getHeight() == 1440)
                        mVideoSize = size;
            }
        }
    }

    void connectCamera() {
        CameraManager cameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if(ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundImage);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            if(mIsRecording) {
                mVideoFileName = videoUtils.getOutputFileName(0, "mp4").toString();
//                utils.logBoth(logID, "Step 2 prepareRecord");
                prepareRecord();
                mediaRecorder.start();
            } else {
                startPreview();
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

    void prepareRecord() {

        SurfaceTexture surface_Texture = null;
        Surface previewSurface = null;
        Surface recordSurface = null;
        try {
//            if(!mIsRecording)
                setupMediaRecorder();
            surface_Texture = vTextureView.getSurfaceTexture();
            surface_Texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            previewSurface = new Surface(surface_Texture);
            recordSurface = mediaRecorder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(recordSurface);
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error AA ", e);
            e.printStackTrace();
        }
        if (previewSurface == null || recordSurface == null) {
            utils.logBoth(logID, "previewSurface or recordSurface is null");
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
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    utils.logBoth(logID, "onConfigureFailed: while prepareRecord");
                }
            }, null);

        } catch (Exception e) {
            utils.logE(logID, "Prepare Error BB ", e);
            e.printStackTrace();
        }
    }

    private void setupMediaRecorder() throws IOException {

        utils.logBoth(logID," setup Media");
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncodingBitRate(VIDEO_ENCODING_RATE); // 1000000
//        mediaRecorder.setVideoFrameRate(FRAME_RATE);
        mediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
        mediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(getOutputFileName(0, "mp4").toString());
        mediaRecorder.setMaxFileSize(VIDEO_ONE_WORK_FILE_SIZE);
        mediaRecorder.prepare();
        mediaRecorder.setNextOutputFile(getOutputFileName(3000, "mp4"));
        setUpNextFile();
    }

    private File getOutputFileName(long after, String fileType) {
        String time = utils.getMilliSec2String(System.currentTimeMillis() + after, FORMAT_LOG_TIME);
        return new File(mPackageWorkingPath, time + "." + fileType);
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

    int nextCount = 0;
    private void assignNextFile() {
        if (mIsRecording) {
            try {
                File nextFileName = getOutputFileName(3000, "mp4");
                mediaRecorder.setNextOutputFile(nextFileName);
                nextCount++;
                String s = nextCount + "";
                vTextRecord.setText(s);
//                utils.log("assign " + s, nextFileName.toString());
            } catch (IOException e) {
                utils.logE("Error", "nxtFile", e);
            }
        }
    }

    void startPreview() {
        SurfaceTexture surfaceTexture = vTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mPrevBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPrevBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mPreviewReader.getSurface()),
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
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            utils.logOnly(logID, "onConfigureFailed: startPreview");
                        }
                    }, null);
        } catch (Exception e) {
            utils.logE(logID, "startPreview ", e);
            e.printStackTrace();
        }
    }
}
