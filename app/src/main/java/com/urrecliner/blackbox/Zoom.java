package com.urrecliner.blackbox;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import static com.urrecliner.blackbox.Vars.utils;

public class Zoom
{
    private static final float DEFAULT_ZOOM_FACTOR = 1.0f;

    @NonNull
    private final Rect cropArea = new Rect();

    public final float maxZoom;

    @Nullable
    private final Rect mSensorSize;

    public final boolean hasSupport;

    public Zoom(@NonNull final CameraCharacteristics characteristics)
    {
        this.mSensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        if (this.mSensorSize == null)
        {
            this.maxZoom = Zoom.DEFAULT_ZOOM_FACTOR;
            this.hasSupport = false;
            return;
        }

        final Float value = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);

        this.maxZoom = ((value == null) || (value < Zoom.DEFAULT_ZOOM_FACTOR))
                ? Zoom.DEFAULT_ZOOM_FACTOR
                : value;

        this.hasSupport = (Float.compare(this.maxZoom, Zoom.DEFAULT_ZOOM_FACTOR) > 0);
    }

    public void setZoom(CaptureRequest.Builder builder, float zoom)
    {
        if (!this.hasSupport)
            return;

        final float newZoom = MathUtils.clamp(zoom, Zoom.DEFAULT_ZOOM_FACTOR, this.maxZoom);

        final int centerX = this.mSensorSize.width() / 2;
        final int centerY = this.mSensorSize.height() / 2;
        final int deltaX  = (int)((0.5f * this.mSensorSize.width()) / newZoom);
        final int deltaY  = (int)((0.5f * this.mSensorSize.height()) / newZoom);

        this.cropArea.set(centerX - deltaX,
                centerY - deltaY,
                centerX + deltaX,
                centerY + deltaY);
        utils.logOnly("zoomed","zoom="+zoom+" width="+this.mSensorSize.width() +">"+(deltaX*2)+" height="+this.mSensorSize.height()+">"+(deltaY*2));
        builder.set(CaptureRequest.SCALER_CROP_REGION, this.cropArea);
    }
}