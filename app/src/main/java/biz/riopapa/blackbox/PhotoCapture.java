package biz.riopapa.blackbox;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;

import static biz.riopapa.blackbox.Vars.activeEventCount;
import static biz.riopapa.blackbox.Vars.mCameraBigRequest;
import static biz.riopapa.blackbox.Vars.mCameraBuilder;
import static biz.riopapa.blackbox.Vars.mBackgroundImage;
import static biz.riopapa.blackbox.Vars.mCameraLeftRequest;
import static biz.riopapa.blackbox.Vars.mCameraRightRequest;
import static biz.riopapa.blackbox.Vars.mCameraShotRequest;
import static biz.riopapa.blackbox.Vars.mCaptureSession;
import static biz.riopapa.blackbox.Vars.photoSurface;
import static biz.riopapa.blackbox.Vars.rectLeft;
import static biz.riopapa.blackbox.Vars.rectNormal;
import static biz.riopapa.blackbox.Vars.rectRight;
import static biz.riopapa.blackbox.Vars.rectShot;

import java.util.Random;

public class PhotoCapture {
    private static final int STATE_WAIT_LOCK = 1;
    private static final int STATE_PREVIEW = 0;
//    private static int mCaptureState = STATE_PREVIEW;

    public void photoInit() {
//        mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        mCameraBuilder.addTarget(photoSurface);
        mCameraBuilder.set(CaptureRequest.JPEG_ORIENTATION, -90);
        mCameraBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        mCameraBuilder.set(CaptureRequest.SCALER_CROP_REGION, rectNormal);
        mCameraBigRequest = mCameraBuilder.build();
        mCameraBuilder.set(CaptureRequest.SCALER_CROP_REGION, rectRight);
        mCameraRightRequest = mCameraBuilder.build();
        mCameraBuilder.set(CaptureRequest.SCALER_CROP_REGION, rectLeft);
        mCameraLeftRequest = mCameraBuilder.build();
        mCameraBuilder.set(CaptureRequest.SCALER_CROP_REGION, rectShot);
        mCameraShotRequest = mCameraBuilder.build();
    }

    public static boolean takeLeft = false;
    Random random = new Random();
    public void zoomedShot() {
//        mCaptureState = STATE_WAIT_LOCK;
        try {
            if (activeEventCount > 0 || random.nextInt(10) > 7)
                mCaptureSession.capture(mCameraShotRequest, null, mBackgroundImage);
            else if (takeLeft)
                mCaptureSession.capture(mCameraLeftRequest, null, mBackgroundImage);
            else
                mCaptureSession.capture(mCameraRightRequest, null, mBackgroundImage);
//            mCameraBuilder.set(CaptureRequest.SCALER_CROP_REGION, leftRight ? rectLeft : rectRight);
//            mCaptureSession.capture(mCameraBuilder.build(), zoomCameraPhotoCallback, mBackgroundImage);
        } catch (Exception ignored) {
        }
    }

    private final CameraCaptureSession.CaptureCallback zoomCameraPhotoCallback = new
        CameraCaptureSession.CaptureCallback() {

//            @Override
//            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
//                super.onCaptureProgressed(session, request, partialResult);
//            }
//
//            @Override
//        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
//                                       TotalCaptureResult result) {
//            super.onCaptureCompleted(session, request, result);
//        }
    };
}