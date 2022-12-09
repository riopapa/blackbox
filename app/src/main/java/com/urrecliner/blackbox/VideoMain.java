package com.urrecliner.blackbox;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.FORMAT_TIME;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.VIDEO_ENCODING_RATE;
import static com.urrecliner.blackbox.Vars.VIDEO_FRAME_RATE;
import static com.urrecliner.blackbox.Vars.VIDEO_ONE_WORK_FILE_SIZE;
import static com.urrecliner.blackbox.Vars.mCameraBuilder;
import static com.urrecliner.blackbox.Vars.vBtnRecord;
import static com.urrecliner.blackbox.Vars.zoomBiggerL;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mCameraDevice;
import static com.urrecliner.blackbox.Vars.mVideoRequestBuilder;
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
import static com.urrecliner.blackbox.Vars.zoomBiggerR;
import static com.urrecliner.blackbox.Vars.zoomHugeC;
import static com.urrecliner.blackbox.Vars.zoomHugeL;
import static com.urrecliner.blackbox.Vars.zoomHugeR;
import static com.urrecliner.blackbox.Vars.PhoneE;

public class VideoMain {

    private final String logID = "videoMain";
    private boolean isPrepared = false;
    private SurfaceTexture surface_Preview = null;
    private Surface previewSurface = null;
    Rect zoomNormal;

    void prepareRecord() {

        if (isPrepared)
            return;
        setupMediaRecorder();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            utils.logE(logID, "prepareRecord sleep", e);
        }
        readySurfaces();
        mVideoRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        mVideoRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
        mVideoRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
//        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON); NO!

        buildCameraSession();
        isPrepared = true;
    }

    private void readySurfaces() {
//        try {
            vPreviewView = mActivity.findViewById(R.id.previewView);
//        } catch (Exception e) {
//            utils.logE(logID, "vPreviewView AA ///", e);
//        }
        if (vPreviewView == null)
            utils.logBoth(logID, "vPreviewView is null ///");
        try {
            surface_Preview = vPreviewView.getSurfaceTexture();
        } catch (Exception e) {
            utils.logE(logID, "surface_Preview  ///", e);
        }
        if (surface_Preview == null)
            utils.logBoth(logID, "surface_Preview is null ///");
        surface_Preview.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        try {
            mVideoRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCameraBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (mVideoRequestBuilder == null)
            utils.logBoth(logID, "mCaptureRequestBuilder is null ///");

        previewSurface = new Surface(surface_Preview);
        if (SUFFIX.equals(PhoneE.B) || SUFFIX.equals(PhoneE.P))        // 왜 note 20 은 안 되는지 모름
            mVideoRequestBuilder.addTarget(previewSurface);
        recordSurface = mediaRecorder.getSurface();
        mVideoRequestBuilder.addTarget(recordSurface);
        photoSurface = mImageReader.getSurface();
        mVideoRequestBuilder.addTarget(photoSurface);
        zoomNormal = calcPhotoZoom (ZOOM_FACTOR_NORMAL,"N");
        zoomBiggerL = calcPhotoZoom (ZOOM_FACTOR_BIGGER, "L");
        zoomBiggerR = calcPhotoZoom (ZOOM_FACTOR_BIGGER, "R");
        zoomHugeL = calcPhotoZoom (ZOOM_FACTOR_HUGE, "L");
        zoomHugeR = calcPhotoZoom (ZOOM_FACTOR_HUGE, "R");
        zoomHugeC = calcPhotoZoom (ZOOM_FACTOR_HUGE, "C");
    }

    void buildCameraSession() {
        List list = Arrays.asList(recordSurface, photoSurface, previewSurface);
        if (SUFFIX.equals(PhoneE.N))
            list = Arrays.asList(recordSurface, photoSurface);
        try {
            mCameraDevice.createCaptureSession(list, cameraStateCallBack(), null);
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error BB ", e);
        }
    }

    private CameraCaptureSession.StateCallback cameraStateCallBack() {
        return new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                mCaptureSession = session;
                mVideoRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomNormal);
                try {
                    mCaptureSession.setRepeatingRequest(mVideoRequestBuilder.build(), null, null);
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

    final float ZOOM_FACTOR_NORMAL = 1.2f, ZOOM_FACTOR_BIGGER = 1.6f, ZOOM_FACTOR_HUGE = 1.9f;
    private Rect calcPhotoZoom(float zoomFactor, String type) {

        int xOrgSize = mImageSize.getWidth();
        int yOrgSize = mImageSize.getHeight();
        int xZoomed = (int) (xOrgSize / zoomFactor);
        int yZoomed = (int) (yOrgSize / zoomFactor);
        int xLeft = 0;
        int yTop = 0;
        Rect rect = new Rect();
        switch (type) {
            case "N":
                xLeft = (xOrgSize - xZoomed) / 2;
                break;
            case "L":
                xLeft = (xOrgSize - xZoomed) / 6;
                yTop = (yOrgSize-yZoomed) * 2 / 3;
                break;
            case "R":
                xLeft = (xOrgSize- xZoomed);
                yTop = (yOrgSize-yZoomed) * 2 / 3;
                break;
            case "C":
                xLeft = (xOrgSize- xZoomed)/2;
                yTop = (yOrgSize-yZoomed)/2;
                break;
        }

        rect.set(xLeft, yTop, xLeft+xZoomed, yTop+yZoomed);
        return rect;
    }

    private void setupMediaRecorder() {

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);    // 1. setAudioSource
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);    // 2. setVideoSource
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);   // 3. setOutputFormat
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);  // 4. setAudioEncoder
        mediaRecorder.setVideoEncodingBitRate(VIDEO_ENCODING_RATE);
        mediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
        mediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
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
            vBtnRecord.setImageResource((nextCount%2 == 0) ? R.mipmap.recording_on: R.mipmap.recording_on2);

        }
    }
}