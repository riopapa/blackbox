package com.riopapa.blackbox;

import static com.riopapa.blackbox.PhotoCapture.leftRight;
import static com.riopapa.blackbox.Vars.IMAGE_BUFFER_MAX_IMAGES;
import static com.riopapa.blackbox.Vars.SUFFIX;
import static com.riopapa.blackbox.Vars.imageStack;
import static com.riopapa.blackbox.Vars.mActivity;
import static com.riopapa.blackbox.Vars.mBackgroundCamera;
import static com.riopapa.blackbox.Vars.mBackgroundImage;
import static com.riopapa.blackbox.Vars.mCameraCharacteristics;
import static com.riopapa.blackbox.Vars.mCameraDevice;
import static com.riopapa.blackbox.Vars.mContext;
import static com.riopapa.blackbox.Vars.mImageReader;
import static com.riopapa.blackbox.Vars.mImageSize;
import static com.riopapa.blackbox.Vars.mIsRecording;
import static com.riopapa.blackbox.Vars.mPreviewSize;
import static com.riopapa.blackbox.Vars.mVideoSize;
import static com.riopapa.blackbox.Vars.share_snap_interval;
import static com.riopapa.blackbox.Vars.utils;

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

import com.riopapa.blackbox.utility.CameraSize;

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
                    mCameraId = cameraId;
                    StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Size[] sizes = CameraSize.set(map, SUFFIX);
                    mPreviewSize = sizes[0];
                    mImageSize = sizes[1];
                    mVideoSize = sizes[2];
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