package biz.riopapa.blackbox;

import static biz.riopapa.blackbox.PhotoCapture.leftRight;
import static biz.riopapa.blackbox.Vars.IMAGE_BUFFER_MAX_IMAGES;
import static biz.riopapa.blackbox.Vars.SUFFIX;
import static biz.riopapa.blackbox.Vars.imageStack;
import static biz.riopapa.blackbox.Vars.mActivity;
import static biz.riopapa.blackbox.Vars.mBackgroundCamera;
import static biz.riopapa.blackbox.Vars.mBackgroundImage;
import static biz.riopapa.blackbox.Vars.mCameraCharacteristics;
import static biz.riopapa.blackbox.Vars.mCameraDevice;
import static biz.riopapa.blackbox.Vars.mContext;
import static biz.riopapa.blackbox.Vars.mImageReader;
import static biz.riopapa.blackbox.Vars.mImageSize;
import static biz.riopapa.blackbox.Vars.mIsRecording;
import static biz.riopapa.blackbox.Vars.mPreviewSize;
import static biz.riopapa.blackbox.Vars.mVideoSize;
import static biz.riopapa.blackbox.Vars.share_snap_interval;
import static biz.riopapa.blackbox.Vars.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.util.Size;

import androidx.core.content.ContextCompat;

import biz.riopapa.blackbox.utility.CameraSize;

public class CameraSub {
    CameraManager mCameraManager;
    String mCameraId = null;

    void readyCamera() {
        if (mCameraId == null) {
            setupCamera();
            connectCamera();
        }
    }

    void setupCamera() {
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            assert mCameraManager != null;
            for (String cameraId : mCameraManager.getCameraIdList()) {
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);

                if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK) {
                    StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Size[] sizes = CameraSize.set(map, SUFFIX);
                    if (sizes[0] == null || sizes[1] == null || sizes[2] == null)   // in case of multi camera
                        continue;
                    mCameraId = cameraId;
                    mPreviewSize = sizes[0];
                    mImageSize = sizes[1];
                    mVideoSize = sizes[2];
                    break;
                    // S10 5G has good 2 camera
                    // first one is better physical size 5.645x4.234;
                    // second one resolution is better;
                }
            }
            mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, IMAGE_BUFFER_MAX_IMAGES);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundImage);
        } catch (CameraAccessException e) {
            utils.logE("CameraSub", "CameraAccessException", e);
        }

    }

    void connectCamera() {
        CameraManager cameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                assert cameraManager != null;
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundCamera);
            }
        } catch (CameraAccessException e) {
//            utils.logE("CameraSub", "connectCamera Exception ", e);
        }
    }

    private final CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            if (mCameraDevice == null)
                mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    long shotTime = 0;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = reader -> {
        if (System.currentTimeMillis() < shotTime || !mIsRecording) {
            return;
        }
        Image image = reader.acquireLatestImage();
        if (image == null)
            return;
//        Log.w("Short", (System.currentTimeMillis()-shotTime) +" , left ="+leftRight);
        imageStack.addImageBuff(image.getPlanes()[0].getBuffer());
        image.close();
        shotTime = System.currentTimeMillis() + share_snap_interval;
//        leftRight = !leftRight;
    };
}