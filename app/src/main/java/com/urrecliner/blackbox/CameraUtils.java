package com.urrecliner.blackbox;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

import static com.urrecliner.blackbox.Vars.mBackgroundImage;
import static com.urrecliner.blackbox.Vars.mCameraDevice;
import static com.urrecliner.blackbox.Vars.mCaptureRequestBuilder;
import static com.urrecliner.blackbox.Vars.mImageReader;
import static com.urrecliner.blackbox.Vars.mCaptureSession;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.zoom;

public class CameraUtils {
    private static final int STATE_WAIT_LOCK = 1;
    private static final int STATE_PREVIEW = 0;
    private static int mCaptureState = STATE_PREVIEW;

    static void snapshotCamera() {
        mCaptureState = STATE_WAIT_LOCK;
        try {
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
//            mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, LENS_FOCUS_NEAR); // 0.0 infinite ~ 10f nearest
            mCaptureSession.capture(mCaptureRequestBuilder.build(), mCameraCaptureCallback, mBackgroundImage);
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
//                            Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
//                            if(afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
//                                    afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                startStillCaptureRequest();
//                            }
                            break;
                    }
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
//                    float focusDistance = result.get(CaptureResult.LENS_FOCUS_DISTANCE);
//                    Log.w("focus",""+ focusDistance);
                    process(result);
                }
            };

    private static void startStillCaptureRequest() {
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, -90);
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
//            mCaptureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, LENS_FOCUS_FAR); // 0.0 infinite ~ 10f nearest
        } catch (CameraAccessException e) {
            utils.logE("cameraUtils", "CameraAccessException", e);
        }
    }
}
