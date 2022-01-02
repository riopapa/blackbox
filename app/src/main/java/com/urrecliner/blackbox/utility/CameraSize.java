package com.urrecliner.blackbox.utility;

import static com.urrecliner.blackbox.Vars.utils;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;

public class CameraSize {

// "SM-G965N" "P";
// "SM-A325N" "A";

    public static Size[] set(StreamConfigurationMap map, String SUFFIX) {

        Size mPreviewSize = null, mShotSize = null, mVideoSize = null;
//        String model = Build.MODEL;
//        utils.logBoth(logID, "CamSize on "+model);
//        dumpVariousCameraSizes(map);
        map.getOutputFormats();

        switch (SUFFIX) {

            case "P":
            case "S":
            /* galaxy s9+
            4032x3024 1.3, 4032x2268 1.8, 4032x1960 2.1, 3024x3024 1.0, 3984x2988 1.3, 3840x2160 1.8 ,
            3264x2448 1.3, 3264x1836 1.8, 2976x2976 1.0, 2880x2160 1.3, 2560x1440 1.8, 2160x2160 1.0 ,
            2224x1080 2.1, 2048x1152 1.8, 1920x1080 1.8, 1440x1080 1.3, 1088x1088 1.0, 1280x720 1.8 ,
            1056x704 1.5, 1024x768 1.3, 960x720 1.3, 960x540 1.8, 800x450 1.8, 720x720 1.0 ,
            720x480 1.5, 640x480 1.3, 352x288 1.2, 320x240 1.3, 256x144 1.8, 176x144 1.2 ,
             */
                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                    if (size.getWidth() == 640 && size.getHeight() == 480)
                        mPreviewSize = size;
                    else if (size.getWidth() == 4032 && size.getHeight() == 3024)
                        mShotSize = size;
                    else if (size.getWidth() == 3264 && size.getHeight() == 1836)
                        mVideoSize = size;
                }
                break;
            case "A":
            /* galaxy A32
                2560x1440 1.8, 1920x1080 1.8, 1440x1080 1.3, 1280x960 1.3, 1280x720 1.8 ,
                1088x1088 1.0, 960x720 1.3, 720x480 1.5, 640x480 1.3, 512x384 1.3, 512x288 1.8 ,
                 384x384 1.0, 352x288 1.2, 320x240 1.3, 256x144 1.8, 176x144 1.2 ,
             */
//                dumpVariousCameraSizes(map);
                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                    if (size.getWidth() == 640 && size.getHeight() == 480)
                        mPreviewSize = size;
                    else if (size.getWidth() == 2560 && size.getHeight() == 1440)
                        mShotSize = size;
                    else if (size.getWidth() == 1920 && size.getHeight() == 1080)
                        mVideoSize = size;
                }
                break;
            default:
                utils.logBoth("Model", "size undefined");
        }
        Size[] sizes = new Size[3];
        sizes[0] = mPreviewSize; sizes[1] = mShotSize; sizes[2] = mVideoSize;
        return sizes;
    }

    private void dumpVariousCameraSizes(StreamConfigurationMap map) {

        String sb = "// DUMP CAMERA POSSIBLE SIZES // ";
        for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
            sb += size.getWidth()+"x"+ size.getHeight()+
                    String.format(" %,3.1f , ", (float)size.getWidth() / (float)size.getHeight());
        }
        utils.logOnly("Camera Size",sb);
    }

}