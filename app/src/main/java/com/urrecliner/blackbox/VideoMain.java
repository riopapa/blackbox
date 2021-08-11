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
import static com.urrecliner.blackbox.Vars.VIDEO_ENCODING_RATE;
import static com.urrecliner.blackbox.Vars.VIDEO_FRAME_RATE;
import static com.urrecliner.blackbox.Vars.VIDEO_ONE_WORK_FILE_SIZE;
import static com.urrecliner.blackbox.Vars.zoomBiggerL;
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
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextRecord;
import static com.urrecliner.blackbox.Vars.vPreviewView;
import static com.urrecliner.blackbox.Vars.zoomBiggerR;

public class VideoMain {

    private final String logID = "videoMain";
    private boolean isPrepared = false;
    private SurfaceTexture surface_Preview = null;
    private Surface previewSurface = null;
    Rect zoomNormal;

    void prepareRecord() {

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
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
        zoomNormal = calcPhotoZoom (ZOOM_NORMAL,0);
        zoomBiggerL = calcPhotoZoom (ZOOM_BIGGER, 1);
        zoomBiggerR = calcPhotoZoom (ZOOM_BIGGER, 2);
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
                mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomNormal);
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

    final float ZOOM_NORMAL = 1.2f, ZOOM_BIGGER = 1.7f;
    private Rect calcPhotoZoom(float zoom, int type) {

        int xSize = mImageSize.getWidth();
        int ySize = mImageSize.getHeight();
        int xZoomed = (int) (xSize / zoom);
        int yZoomed = (int) (ySize / zoom);
        int xShift = (xSize - xZoomed) / 3;
        int yShift = (ySize - yZoomed) / 4;
        int xLeft = (xSize-xZoomed) / 2;
        int yTop = ySize-yZoomed-yShift;
        Rect rect = new Rect();
        if (type == 1) // bigger zoom Left
            xLeft -= xShift;
        else if (type == 2) // bigger zoom Right
            xLeft += xShift;
        Log.w("size","xSize="+xSize+", ySize="+ySize+", xZ"+xZoomed+", yZ="+yZoomed+", xS="+xShift+", yS="+yShift+", xL="+xLeft+", yT="+yTop);
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
