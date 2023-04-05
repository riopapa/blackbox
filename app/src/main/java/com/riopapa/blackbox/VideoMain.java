package com.riopapa.blackbox;

import static com.riopapa.blackbox.Vars.FORMAT_TIME;
import static com.riopapa.blackbox.Vars.PhoneE;
import static com.riopapa.blackbox.Vars.SUFFIX;
import static com.riopapa.blackbox.Vars.VIDEO_ENCODING_RATE;
import static com.riopapa.blackbox.Vars.VIDEO_FRAME_RATE;
import static com.riopapa.blackbox.Vars.VIDEO_ONE_WORK_FILE_SIZE;
import static com.riopapa.blackbox.Vars.mCameraBuilder;
import static com.riopapa.blackbox.Vars.mCameraDevice;
import static com.riopapa.blackbox.Vars.mCaptureSession;
import static com.riopapa.blackbox.Vars.mContext;
import static com.riopapa.blackbox.Vars.mImageReader;
import static com.riopapa.blackbox.Vars.mImageSize;
import static com.riopapa.blackbox.Vars.mIsRecording;
import static com.riopapa.blackbox.Vars.mPackageWorkingPath;
import static com.riopapa.blackbox.Vars.mPreviewSize;
import static com.riopapa.blackbox.Vars.mVideoRequestBuilder;
import static com.riopapa.blackbox.Vars.mVideoSize;
import static com.riopapa.blackbox.Vars.mediaRecorder;
import static com.riopapa.blackbox.Vars.photoSurface;
import static com.riopapa.blackbox.Vars.previewSurface;
import static com.riopapa.blackbox.Vars.recordSurface;
import static com.riopapa.blackbox.Vars.surface_Preview;
import static com.riopapa.blackbox.Vars.utils;
import static com.riopapa.blackbox.Vars.vBtnRecord;
import static com.riopapa.blackbox.Vars.vPreviewView;
import static com.riopapa.blackbox.Vars.vTextRecord;
import static com.riopapa.blackbox.Vars.zoomBiggerL;
import static com.riopapa.blackbox.Vars.zoomBiggerR;
import static com.riopapa.blackbox.Vars.zoomHugeC;
import static com.riopapa.blackbox.Vars.zoomHugeL;
import static com.riopapa.blackbox.Vars.zoomHugeR;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.view.Surface;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class VideoMain {

    private final String logID = "videoMain";
    private boolean isPrepared = false;
    Rect zoomNormal;

    void prepareRecord() {

        if (isPrepared)
            return;
        setupMediaRecorder();
        readySurfaces();
        mVideoRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        mVideoRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
        mVideoRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
//        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON); NO!

        buildCameraSession();
        isPrepared = true;
    }

    private void readySurfaces() {
        try {
            mVideoRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCameraBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (mVideoRequestBuilder == null)
            utils.logBoth(logID, "mCaptureRequestBuilder is null ///");

        try {
            surface_Preview = vPreviewView.getSurfaceTexture();
        } catch (Exception e) {
            utils.logE(logID, "surface_Preview  ///", e);
        }
        if (surface_Preview == null)
            utils.logBoth(logID, "surface_Preview is null ERROR ///");
        if (!SUFFIX.equals(PhoneE.N)) {
            surface_Preview.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            previewSurface = new Surface(surface_Preview);
            mVideoRequestBuilder.addTarget(previewSurface);
        }
        recordSurface = mediaRecorder.getSurface();
        mVideoRequestBuilder.addTarget(recordSurface);
        photoSurface = mImageReader.getSurface();
        mVideoRequestBuilder.addTarget(photoSurface);

        zoomNormal = calcPhotoZoom(ZOOM_FACTOR_NORMAL, "N");
        zoomBiggerL = calcPhotoZoom(ZOOM_FACTOR_BIGGER, "L");
        zoomBiggerR = calcPhotoZoom(ZOOM_FACTOR_BIGGER, "R");
        zoomHugeL = calcPhotoZoom(ZOOM_FACTOR_HUGE, "L");
        zoomHugeR = calcPhotoZoom(ZOOM_FACTOR_HUGE, "R");
        zoomHugeC = calcPhotoZoom(ZOOM_FACTOR_HUGE, "C");
    }

    void buildCameraSession() {
        List<Surface> captureList;
        if (SUFFIX.equals(PhoneE.N))    // note 20 ?
            captureList = Arrays.asList(recordSurface, photoSurface);
        else
            captureList = Arrays.asList(previewSurface, recordSurface, photoSurface);
        try {
            mCameraDevice.createCaptureSession(captureList, cameraStateCallBack(), null);
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
                yTop = (yOrgSize-yZoomed)/3;
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
            vBtnRecord.setImageResource((nextCount%2 == 0) ? R.drawable.circle0: R.drawable.circle1);
            Animation aniRotateClk = AnimationUtils.loadAnimation(mContext,R.anim.rotate);
            aniRotateClk.setRepeatCount(Animation.INFINITE);
            vBtnRecord.startAnimation(aniRotateClk);

        }
    }
}