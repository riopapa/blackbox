package com.urrecliner.blackbox;

import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

import static com.urrecliner.blackbox.Vars.mCameraBuilder;
import static com.urrecliner.blackbox.Vars.photoSaved;
import static com.urrecliner.blackbox.Vars.mBackgroundImage;
import static com.urrecliner.blackbox.Vars.mVideoRequestBuilder;
import static com.urrecliner.blackbox.Vars.mCaptureSession;
import static com.urrecliner.blackbox.Vars.photoSurface;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.zoomBiggerL;
import static com.urrecliner.blackbox.Vars.zoomBiggerR;
import static com.urrecliner.blackbox.Vars.zoomHuge;
import static com.urrecliner.blackbox.Vars.zoomHugeC;
import static com.urrecliner.blackbox.Vars.zoomHugeL;
import static com.urrecliner.blackbox.Vars.zoomHugeR;

public class PhotoCapture {
    private static final int STATE_WAIT_LOCK = 1;
    private static final int STATE_PREVIEW = 0;
    private static int mCaptureState = STATE_PREVIEW;

    static void photoInit() {
//        mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        mCameraBuilder.addTarget(photoSurface);
        mCameraBuilder.set(CaptureRequest.JPEG_ORIENTATION, -90);
        mCameraBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    static void zoomShotCamera() {
        mCaptureState = STATE_WAIT_LOCK;
        try {
            mCaptureSession.capture(mCameraBuilder.build(), zoomCameraPhotoCallback, mBackgroundImage);
//            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            mCaptureRequestBuilder.addTarget(photoSurface);
//            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, -90);
//            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        } catch (Exception e) {
            utils.logBoth("photo","zoom passed");
            utils.beepOnce(5,1f);
        }
    }

    static boolean leftRight = false;
    private static final CameraCaptureSession.CaptureCallback zoomCameraPhotoCallback = new
        CameraCaptureSession.CaptureCallback() {
            private void process(CaptureResult captureResult) {
                if (mCaptureState == STATE_WAIT_LOCK) {
                        mCaptureState = STATE_PREVIEW;
                        Rect rect;
                        if (zoomHuge) {
                            rect = zoomHugeC;
                        } else {
                            if (Math.random() < 0.5f) {
                                rect = leftRight ? zoomHugeL : zoomHugeR;
                            } else {
                                rect = leftRight ? zoomBiggerL : zoomBiggerR;
                            }
                        }
                        mVideoRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, rect);
                }
            }

            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                           TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                photoSaved = false;
                process(result);
            }
        };
}