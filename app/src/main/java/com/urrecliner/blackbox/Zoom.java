package com.urrecliner.blackbox;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;

class Zoom
{

    public Zoom(CameraCharacteristics characteristics, CaptureRequest.Builder builder, float zoom)
    {
        Rect mSensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int centerX = mSensorSize.width() / 2;
        int centerY = mSensorSize.height() / 2;
        int deltaX  = (int)((0.5f * mSensorSize.width()) / zoom);
        int deltaY  = (int)((0.5f * mSensorSize.height()) / zoom);

        Rect cropArea = new Rect();
        cropArea.set(centerX - deltaX,
                centerY - deltaY,
                centerX + deltaX,
                centerY + deltaY);
//        utils.logOnly("zoomed","zoom="+zoom+" width="+mSensorSize.width() +">"+(deltaX*2)+" height="+mSensorSize.height()+">"+(deltaY*2));
        builder.set(CaptureRequest.SCALER_CROP_REGION, cropArea);
    }
}