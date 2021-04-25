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

public class VideoMain {

    private final String logID = "videoMain";
    final float zoomFactor = 1.23f;

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

        if (preparePrevSurface()) return;
        if (prepareVideoSurface()) return;

        buildCameraSession();   // zoomFactor
        isPrepared = true;
    }

    private boolean prepareVideoSurface() {
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
            return true;
        }
        return false;
    }

    private boolean preparePrevSurface() {
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
            return true;
        }
        return false;
    }

    void buildCameraSession() {
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface, mImageReader.getSurface()),
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
                new Zoom(cameraCharacteristics, mCaptureRequestVideoBuilder, zoomFactor);
//                utils.logOnly("zoom set","setZoom to "+zoomFactor);
                try {
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
