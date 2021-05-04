package com.urrecliner.blackbox;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

import static com.urrecliner.blackbox.Vars.cropArea;
import static com.urrecliner.blackbox.Vars.cropArea2;
import static com.urrecliner.blackbox.Vars.mBackgroundImage;
import static com.urrecliner.blackbox.Vars.mCameraDevice;
import static com.urrecliner.blackbox.Vars.mCaptureRequestBuilder;
import static com.urrecliner.blackbox.Vars.mImageReader;
import static com.urrecliner.blackbox.Vars.mCaptureSession;
import static com.urrecliner.blackbox.Vars.photoSurface;
import static com.urrecliner.blackbox.Vars.recordSurface;
import static com.urrecliner.blackbox.Vars.utils;

public class PhotoCapture {
    private static final int STATE_WAIT_LOCK = 1;
    private static final int STATE_PREVIEW = 0;
    private static int mCaptureState = STATE_PREVIEW;

    static void snapshotCamera() {
        mCaptureState = STATE_WAIT_LOCK;
        try {
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

//    static int swCount = 0;
    private static void startStillCaptureRequest() {
//        try {
//            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            mCaptureRequestBuilder.addTarget(photoSurface);
//            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, -90);
//            swCount++;
//            if (swCount == 1)
//                mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropArea2);
//            else if (swCount == 2)
//                mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropArea2);
//            else {
//                mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropArea);
//                swCount = 0;
//            }
//        } catch (CameraAccessException e) {
//            utils.logE("cameraUtils", "CameraAccessException", e);
//        }
    }
}
