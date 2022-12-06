package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.PhotoCapture.leftRight;
import static com.urrecliner.blackbox.Vars.IMAGE_BUFFER_MAX_IMAGES;
import static com.urrecliner.blackbox.Vars.share_snap_interval;
import static com.urrecliner.blackbox.Vars.share_image_size;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mBackgroundCamera;
import static com.urrecliner.blackbox.Vars.mBackgroundImage;
import static com.urrecliner.blackbox.Vars.mCameraCharacteristics;
import static com.urrecliner.blackbox.Vars.mCameraDevice;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mImageReader;
import static com.urrecliner.blackbox.Vars.mImageSize;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPreviewReader;
import static com.urrecliner.blackbox.Vars.mPreviewSize;
import static com.urrecliner.blackbox.Vars.mVideoSize;
import static com.urrecliner.blackbox.Vars.photoSaved;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapNowPos;
import static com.urrecliner.blackbox.Vars.utils;

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

import com.urrecliner.blackbox.utility.CameraSize;

import java.nio.ByteBuffer;

public class CameraSub {
    CameraManager mCameraManager;
    String mCameraId = null;

    void readyCamera() {
        setupCamera();
        connectCamera();
    }

    void setupCamera() {
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            assert mCameraManager != null;
            for (String cameraId : mCameraManager.getCameraIdList()) {
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);

                if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK) {
//                    utils.logOnly("camera "+cameraId," is back camera ///// ");
//                if (SUFFIX == Vars.PhoneE.NOTE20 && cameraId.equals("2") || !SUFFIX.equals("N") && cameraId.equals("0")) {
//                    utils.logOnly("Camera= "+cameraId, "camera found ="+cameraId);
                    mCameraId = cameraId;
                    StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Size[] sizes = CameraSize.set(map, SUFFIX);
                    mPreviewSize = sizes[0];
                    mImageSize = sizes[1];
                    mVideoSize = sizes[2];
                }
            }
        } catch (CameraAccessException e) {
            utils.logE("CameraSub", "CameraAccessException", e);
        }
        try {
            mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, IMAGE_BUFFER_MAX_IMAGES);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundImage);
            mPreviewReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.RGB_565, 1);    // x, y swap
        } catch (Exception e) {
            utils.logE("CameraSub", "Exception ", e);
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
            utils.logE("CameraSub", "connectCamera Exception ", e);
        }
    }

    private final CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            if (mCameraDevice == null)
                mCameraDevice = camera;
//            if (mIsRecording) {
//                videoMain.prepareRecord();
//                mediaRecorder.start();
//            }
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
        long nowTime = System.currentTimeMillis();
        if (nowTime < shotTime || !mIsRecording) {
            return;
        }

        Image image = reader.acquireLatestImage();
        if (image == null)
            return;
        if (photoSaved) {
            image.close();
            return;
        }
        if (shotTime == 0)
            shotTime = nowTime;
        shotTime += share_snap_interval;
//        shotTime = nowTime + INTERVAL_SNAP_SHOT_SAVE;
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            snapBytes[snapNowPos] = bytes;
        } catch (Exception e) {
            utils.showOnly("img", "buffer short " + snapNowPos);
        }
        image.close();
        if (mIsRecording) {
            snapNowPos++;
            if (snapNowPos >= share_image_size)
                snapNowPos = 0;
        }
        photoSaved = true;
        leftRight = !leftRight;
    };
}