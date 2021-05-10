package com.urrecliner.blackbox;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.FORMAT_TIME;
import static com.urrecliner.blackbox.Vars.cropBigger;
import static com.urrecliner.blackbox.Vars.cropArea;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mCameraDevice;
import static com.urrecliner.blackbox.Vars.mCapturePhotoBuilder;
import static com.urrecliner.blackbox.Vars.mCaptureRequestBuilder;
import static com.urrecliner.blackbox.Vars.mCaptureSession;
import static com.urrecliner.blackbox.Vars.mImageReader;
import static com.urrecliner.blackbox.Vars.mImageSize;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.mPreviewSize;
import static com.urrecliner.blackbox.Vars.mVideoSize;
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.photoSurface;
import static com.urrecliner.blackbox.Vars.recordSurface;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextRecord;
import static com.urrecliner.blackbox.Vars.vPreviewView;

public class VideoMain {

    private final String logID = "videoMain";
    private boolean isPrepared = false;
    private SurfaceTexture surface_Preview = null;
    private Surface previewSurface = null;

    void prepareRecord() throws CameraAccessException {

        if (isPrepared)
            return;
        try {
            setupMediaRecorder();
            vPreviewView = mActivity.findViewById(R.id.previewView);
            surface_Preview = vPreviewView.getSurfaceTexture();
            surface_Preview.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } catch (Exception e) {
            utils.logE(logID, "preView AA ///", e);
        }
        mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        if (preparePrevSurface()) return;
        if (prepareVideoSurface()) return;
        if (preparePhotoSurface()) return;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
//        readyCapturePhoto();

        buildCameraSession();
        isPrepared = true;
    }

    private void readyCapturePhoto()  {
        try {
            mCapturePhotoBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCapturePhotoBuilder.addTarget(photoSurface);
            mCapturePhotoBuilder.set(CaptureRequest.JPEG_ORIENTATION, -90);
//            mCapturePhotoBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, ); api 30 이상에서
            mCapturePhotoBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropBigger);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private boolean preparePrevSurface() {
//        try {
            previewSurface = new Surface(surface_Preview);
//        } catch (Exception e) {
//            utils.logE(logID, "Preview Error BB ///", e);
//        }
//        try {
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
//            mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f); // 0.0 infinite ~ 10f nearest
//        } catch (Exception e) {
//            utils.logE(logID, "Prepare mCaptureRequestBuilder Error CC ///", e);
//        }
//
//        if (previewSurface == null) {
//            utils.logBoth(logID, "previewSurface is null");
//            return true;
//        }
        return false;
    }

    private boolean prepareVideoSurface() {
//        try {
            recordSurface = mediaRecorder.getSurface();
            mCaptureRequestBuilder.addTarget(recordSurface);
//            mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f); // 0.0 infinite ~ 10f nearest
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
//        } catch (Exception e) {
//            utils.logE(logID, "Prepare Error recordSurface ///", e);
//        }
//        if (recordSurface == null) {
//            utils.logBoth(logID, "recordSurface is null ------");
//            return true;
//        }
        return false;
    }

    private boolean preparePhotoSurface() {
//        try {
            photoSurface = mImageReader.getSurface();
//        } catch (Exception e) {
//            utils.logE(logID, "Preview Error BB ///", e);
//        }
//        try {
//            mCaptureRequestVideoBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
            mCaptureRequestBuilder.addTarget(photoSurface);
//            mCaptureRequestVideoBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
//            mCaptureRequestVideoBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f); // 0.0 infinite ~ 10f nearest
//        } catch (Exception e) {
//            utils.logE(logID, "Prepare mCaptureRequestBuilder photo CC ///", e);
//        }

        if (previewSurface == null) {
            utils.logBoth(logID, "photoSurface is null");
            return true;
        }
        return false;
    }

    void buildCameraSession() {
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(recordSurface, photoSurface, previewSurface),
                    cameraStateCallBack(), null);
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error BB ", e);
        }
    }

    final float CROP_ZOOM = 1.17f, CROP_ZOOM_BIGGER = 1.8f;
    private CameraCaptureSession.StateCallback cameraStateCallBack() {
        return new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                mCaptureSession = session;
                cropArea = calcPhotoZoom (CROP_ZOOM);
                cropBigger = calcPhotoZoom (CROP_ZOOM_BIGGER);
//                new Zoom(mCameraCharacteristics, mCaptureRequestBuilder, zoomFactor);
                mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropArea);
                try {
                    mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
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

    private Rect calcPhotoZoom(float zoom) {
        int centerX = mImageSize.getWidth() / 2;
        int centerY = mImageSize.getHeight() / 2;
        int deltaX  = (int)((0.5f * mImageSize.getWidth()) / zoom);
        int deltaY  = (int)((0.5f * mImageSize.getHeight()) / zoom);
        Rect cropArea = new Rect();
        cropArea.set(centerX - deltaX,
                centerY - deltaY,
                centerX + deltaX,
                centerY + deltaY);
        return cropArea;
    }

    private void setupMediaRecorder() {

        final int VIDEO_FRAME_RATE = 60;
        final int VIDEO_ENCODING_RATE = 45*1000*1000;
        final long VIDEO_ONE_WORK_FILE_SIZE = 9000*1000;

        utils.logBoth(logID," setup Media");
        mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);    // 1. setAudioSource
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);    // 2. setVideoSource
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);   // 3. setOutputFormat
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);  // 4. setAudioEncoder
        mediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
        mediaRecorder.setVideoEncodingBitRate(VIDEO_ENCODING_RATE);
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
