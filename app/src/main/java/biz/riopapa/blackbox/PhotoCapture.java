package biz.riopapa.blackbox;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;

import static biz.riopapa.blackbox.Vars.mCameraBuilder;
import static biz.riopapa.blackbox.Vars.mBackgroundImage;
import static biz.riopapa.blackbox.Vars.mVideoRequestBuilder;
import static biz.riopapa.blackbox.Vars.mCaptureSession;
import static biz.riopapa.blackbox.Vars.photoSurface;
import static biz.riopapa.blackbox.Vars.utils;
import static biz.riopapa.blackbox.Vars.zoomLeft;
import static biz.riopapa.blackbox.Vars.zoomRight;

public class PhotoCapture {
    private static final int STATE_WAIT_LOCK = 1;
    private static final int STATE_PREVIEW = 0;
    private static int mCaptureState = STATE_PREVIEW;

    public void photoInit() {
//        mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        mCameraBuilder.addTarget(photoSurface);
        mCameraBuilder.set(CaptureRequest.JPEG_ORIENTATION, -90);
        mCameraBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    public static boolean leftRight = false;
    public void zoomShotCamera() {
        mCameraBuilder.set(CaptureRequest.SCALER_CROP_REGION, leftRight ? zoomLeft : zoomRight);
        mCaptureState = STATE_WAIT_LOCK;
        try {
            mCaptureSession.capture(mCameraBuilder.build(), zoomCameraPhotoCallback, mBackgroundImage);
        } catch (Exception e) {
        }
    }

    private final CameraCaptureSession.CaptureCallback zoomCameraPhotoCallback = new
        CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };
}