package biz.riopapa.blackbox;

import static biz.riopapa.blackbox.Vars.FORMAT_TIME;
import static biz.riopapa.blackbox.Vars.PhoneE;
import static biz.riopapa.blackbox.Vars.SUFFIX;
import static biz.riopapa.blackbox.Vars.VIDEO_ENCODING_RATE;
import static biz.riopapa.blackbox.Vars.VIDEO_FRAME_RATE;
import static biz.riopapa.blackbox.Vars.VIDEO_ONE_WORK_FILE_SIZE;
import static biz.riopapa.blackbox.Vars.mCameraBuilder;
import static biz.riopapa.blackbox.Vars.mCameraDevice;
import static biz.riopapa.blackbox.Vars.mCaptureSession;
import static biz.riopapa.blackbox.Vars.mContext;
import static biz.riopapa.blackbox.Vars.mImageReader;
import static biz.riopapa.blackbox.Vars.mImageSize;
import static biz.riopapa.blackbox.Vars.mIsRecording;
import static biz.riopapa.blackbox.Vars.mPackageWorkingPath;
import static biz.riopapa.blackbox.Vars.mPreviewSize;
import static biz.riopapa.blackbox.Vars.mVideoRequestBuilder;
import static biz.riopapa.blackbox.Vars.mVideoSize;
import static biz.riopapa.blackbox.Vars.mediaRecorder;
import static biz.riopapa.blackbox.Vars.nextCount;
import static biz.riopapa.blackbox.Vars.photoSurface;
import static biz.riopapa.blackbox.Vars.previewSurface;
import static biz.riopapa.blackbox.Vars.recordSurface;
import static biz.riopapa.blackbox.Vars.rectShot;
import static biz.riopapa.blackbox.Vars.surface_Preview;
import static biz.riopapa.blackbox.Vars.utils;
import static biz.riopapa.blackbox.Vars.vPower;
import static biz.riopapa.blackbox.Vars.vPreviewView;
import static biz.riopapa.blackbox.Vars.vTextRecord;
import static biz.riopapa.blackbox.Vars.rectLeft;
import static biz.riopapa.blackbox.Vars.rectRight;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class VideoMain {

    private final String logID = "videoMain";
    private boolean isPrepared = false;
    Rect rectNormal;
    Drawable [] onRecord;

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
        onRecord = new Drawable[2];
        onRecord[0] = mContext.getResources().getDrawable(R.drawable.circle0);
        onRecord[1] = mContext.getResources().getDrawable(R.drawable.circle1);
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

        rectNormal = calcZoomSize(zoomNormal, "N");
        rectLeft = calcZoomSize(zoomHuge, "L");
        rectRight = calcZoomSize(zoomHuge, "R");
        rectShot = calcZoomSize(zoomShot, "N");
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
            public void onConfigured(@NonNull CameraCaptureSession session) {
                mCaptureSession = session;
                mVideoRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, rectNormal);
                try {
                    mCaptureSession.setRepeatingRequest(mVideoRequestBuilder.build(), null, null);
                } catch (CameraAccessException e) {
                    utils.logBoth(logID, "setRepeatingRequest Error");
                }
            }
            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                utils.logBoth(logID, "onConfigureFailed: while prepareRecord");
            }
        };
    }

    public static float zoomNormal, zoomHuge, zoomShot;
    private Rect calcZoomSize(float zoomFactor, String type) {

        int xOrgSize = mImageSize.getWidth();
        int yOrgSize = mImageSize.getHeight();
        int xZoomed = (int) (xOrgSize / zoomFactor);
        int yZoomed = (int) (yOrgSize / zoomFactor);
        int xLeft = 0;
        int yTop = 0; // yOrgSize-yZoomed;
        Rect rect = new Rect();
        switch (type) {
            case "N":
                xLeft = (xOrgSize - xZoomed) / 2;
                break;
            case "L":
                break;
            case "R":
                xLeft = (xOrgSize- xZoomed);
                break;
        }

        rect.set(xLeft, yTop, xLeft+xZoomed, yTop+yZoomed);
        return rect;
    }

    private void setupMediaRecorder() {

        mediaRecorder = new MediaRecorder(mContext);
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
            vPower.setImageDrawable(onRecord[nextCount%2]);
//            Animation aniRotateClk = AnimationUtils.loadAnimation(mContext,R.anim.rotate);
//            aniRotateClk.setRepeatCount(Animation.INFINITE);
//            vPower.startAnimation(aniRotateClk);

        }
    }
}