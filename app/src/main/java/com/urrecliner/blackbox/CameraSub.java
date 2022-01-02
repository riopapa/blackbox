package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.Vars.IMAGE_BUFFER_MAX_IMAGES;
import static com.urrecliner.blackbox.Vars.INTERVAL_SNAP_SHOT_SAVE;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
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
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.photoCaptureLeft;
import static com.urrecliner.blackbox.Vars.photoSaved;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.videoMain;

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
//                utils.logOnly(logID, "cameraID="+cameraId);
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
                } else
                    continue;
                StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = CameraSize.set(map, SUFFIX);
                mPreviewSize = sizes[0];
                mImageSize = sizes[1];
                mVideoSize = sizes[2];
            }
        } catch (CameraAccessException e) {
            utils.logE("CameraSub", "CameraAccessException", e);
        }
        try {
            mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, IMAGE_BUFFER_MAX_IMAGES);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundImage);
            mPreviewReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.RGB_565, 1);
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
            photoCaptureLeft = !leftRight;
            if (mIsRecording) {
                videoMain.prepareRecord();
                mediaRecorder.start();
            }
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
    boolean leftRight = false;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = reader -> {
        long nowTime = System.currentTimeMillis();
        if (nowTime < shotTime || !mIsRecording) {
            return;
        }

        Image image = reader.acquireLatestImage();
        if (photoSaved) {
            image.close();
            return;
        }
        if (shotTime == 0)
            shotTime = nowTime;
        shotTime += INTERVAL_SNAP_SHOT_SAVE;
//        shotTime = nowTime + INTERVAL_SNAP_SHOT_SAVE;
        leftRight = !leftRight;
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            snapBytes[snapMapIdx] = bytes;
        } catch (Exception e) {
            utils.showOnly("img", "buffer short " + snapMapIdx);
        }
        image.close();
        if (mIsRecording) {
            snapMapIdx++;
            if (snapMapIdx >= MAX_IMAGES_SIZE)
                snapMapIdx = 0;
        }
        photoSaved = true;
    };
}