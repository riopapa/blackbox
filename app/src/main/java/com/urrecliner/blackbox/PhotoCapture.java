package com.urrecliner.blackbox;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

import static com.urrecliner.blackbox.Vars.cropArea;
import static com.urrecliner.blackbox.Vars.mBackgroundImage;
import static com.urrecliner.blackbox.Vars.mCameraDevice;
import static com.urrecliner.blackbox.Vars.mCaptureRequestVideoBuilder;
import static com.urrecliner.blackbox.Vars.mImageReader;
import static com.urrecliner.blackbox.Vars.mCaptureSession;
import static com.urrecliner.blackbox.Vars.utils;

public class PhotoCapture {
    private static final int STATE_WAIT_LOCK = 1;
    private static final int STATE_PREVIEW = 0;
    private static int mCaptureState = STATE_PREVIEW;

    static void snapshotCamera() {
        mCaptureState = STATE_WAIT_LOCK;
        try {
            mCaptureSession.capture(mCaptureRequestVideoBuilder.build(), mCameraCaptureCallback, mBackgroundImage);
        } catch (CameraAccessException e) {
            StartStopExit.reRunApplication("CameraAccessException",e);
        }
    }

    private static CameraCaptureSession.CaptureCallback mCameraCaptureCallback = new
            CameraCaptureSession.CaptureCallback() {
                private void process(CaptureResult captureResult) {
                    switch (mCaptureState) {
                        case STATE_PREVIEW:
                            // Do nothing
                            break;
                        case STATE_WAIT_LOCK:
                            mCaptureState = STATE_PREVIEW;
                            startStillCaptureRequest();
                            break;
                    }
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result)                 {
                    super.onCaptureCompleted(session, request, result);
                    process(result);
                }
            };

    private static void startStillCaptureRequest() {
        try {
            mCaptureRequestVideoBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestVideoBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestVideoBuilder.set(CaptureRequest.JPEG_ORIENTATION, -90);
            mCaptureRequestVideoBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropArea);
        } catch (CameraAccessException e) {
            utils.logE("cameraUtils", "CameraAccessException", e);
        }
    }
}
