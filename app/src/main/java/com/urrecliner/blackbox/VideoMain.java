package com.urrecliner.blackbox;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.FORMAT_TIME;
import static com.urrecliner.blackbox.Vars.cropBigger;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mCameraDevice;
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
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextRecord;
import static com.urrecliner.blackbox.Vars.vPreviewView;

public class VideoMain {

    private final String logID = "videoMain";
    private boolean isPrepared = false;
    private SurfaceTexture surface_Preview = null;
    private Surface previewSurface = null;
    Rect cropArea;
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
        readySurfaces();
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
//        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON); NO!

        buildCameraSession();
        isPrepared = true;
    }

    private void readySurfaces() {
        previewSurface = new Surface(surface_Preview);
        mCaptureRequestBuilder.addTarget(previewSurface);
        recordSurface = mediaRecorder.getSurface();
        mCaptureRequestBuilder.addTarget(recordSurface);
        photoSurface = mImageReader.getSurface();
        mCaptureRequestBuilder.addTarget(photoSurface);
        cropArea = calcPhotoZoom (CROP_ZOOM);
        cropBigger = calcPhotoZoom (CROP_ZOOM_BIGGER);
    }

    void buildCameraSession() {
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(recordSurface, photoSurface, previewSurface),
                    cameraStateCallBack(), null);
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error BB ", e);
        }
    }

    private CameraCaptureSession.StateCallback cameraStateCallBack() {
        return new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                mCaptureSession = session;
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

    final float CROP_ZOOM = 1.2f, CROP_ZOOM_BIGGER = 1.9f;
    private Rect calcPhotoZoom(float zoom) {
        int centerX = mImageSize.getWidth() / 2;
        int centerY = mImageSize.getHeight() / 2 + (int)((mImageSize.getHeight()) * zoom / 15f);
        int deltaX  = (int)((0.5f * mImageSize.getWidth()) / zoom);
        int deltaY  = (int)((0.5f * mImageSize.getHeight()) / zoom);
        Rect rect = new Rect();
        rect.set(centerX - deltaX, centerY - deltaY,
                centerX + deltaX, centerY + deltaY);
        Log.w("Y "+mImageSize.getHeight(),(centerY-deltaY)+" x "+(centerY+deltaY));
        return rect;
    }

    private void setupMediaRecorder() {

        final int VIDEO_FRAME_RATE;
        final int VIDEO_ENCODING_RATE;
        final long VIDEO_ONE_WORK_FILE_SIZE; // xMb
        if (SUFFIX.equals("8")) {
            VIDEO_FRAME_RATE = 60;
            VIDEO_ENCODING_RATE = 45*1000*1000;
            VIDEO_ONE_WORK_FILE_SIZE = 10*1024*1024;
        } else if (SUFFIX.equals("9")) {
            VIDEO_FRAME_RATE = 30;
            VIDEO_ENCODING_RATE = 45*1000*1000;
            VIDEO_ONE_WORK_FILE_SIZE = 20*1024*1024;
        } else {
            VIDEO_FRAME_RATE = 30;
            VIDEO_ENCODING_RATE = 35*1000*1000;
            VIDEO_ONE_WORK_FILE_SIZE = 20*1024*1024;
        }

//        utils.logBoth(logID," setup Media");
        mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);    // 1. setAudioSource
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);    // 2. setVideoSource
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);   // 3. setOutputFormat
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);  // 4. setAudioEncoder
        mediaRecorder.setVideoEncodingBitRate(VIDEO_ENCODING_RATE);
        mediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
        mediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setOutputFile(getNextFileName(0).toString());
        mediaRecorder.setMaxFileSize(VIDEO_ONE_WORK_FILE_SIZE);
        try {
            mediaRecorder.prepare();
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
