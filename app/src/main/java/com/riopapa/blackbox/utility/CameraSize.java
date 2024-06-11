package com.riopapa.blackbox.utility;

import static com.riopapa.blackbox.Vars.utils;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;

import com.riopapa.blackbox.Vars;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;

public class CameraSize {

// "SM-G986N" "N";  note 20
// "SM-G965N" "P";
// "SM-A325N" "A";

    public static Size[] set(StreamConfigurationMap map, Vars.PhoneE SUFFIX) {

        Size sizePreview = null, sizeCamera = null, sizeVideo = null;
//        String model = Build.MODEL;
//        utils.logBoth("logID", "CamSize on "+model);
//        dumpVariousCameraSizes(map);
        map.getOutputFormats();

        switch (SUFFIX) {

            case P:
            /* galaxy s9+
            4032x3024 1.3, 4032x2268 1.8, 4032x1960 2.1, 3024x3024 1.0, 3984x2988 1.3, 3840x2160 1.8 ,
            3264x2448 1.3, 3264x1836 1.8, 2976x2976 1.0, 2880x2160 1.3, 2560x1440 1.8, 2160x2160 1.0 ,
            2224x1080 2.1, 2048x1152 1.8, 1920x1080 1.8, 1440x1080 1.3, 1088x1088 1.0, 1280x720 1.8 ,
            1056x704 1.5, 1024x768 1.3, 960x720 1.3, 960x540 1.8, 800x450 1.8, 720x720 1.0 ,
            720x480 1.5, 640x480 1.3, 352x288 1.2, 320x240 1.3, 256x144 1.8, 176x144 1.2 ,
             */
                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                    if (size.getWidth() == 960 && size.getHeight() == 720)
                        sizePreview = size;
                    else if (size.getWidth() == 3264 && size.getHeight() == 2448) // 1.8
                        sizeCamera = size;
                    else if (size.getWidth() == 1440 && size.getHeight() == 1080)   // 1.8
                        sizeVideo = size;
                }
                break;

            case N:
            /* galaxy note20
                4000x3000 1.3 , 4000x2252 1.8 , 4000x1868 2.1 , 2992x2992 1.0 , 1920x824 2.3 ,
                1920x900 2.1 , 3840x2160 1.8 , 1920x1080 1.8 , 2320x1080 2.1 , 1920x1440 1.3 ,
                1440x1080 1.3 , 1088x1088 1.0 , 1280x720 1.8 , 960x720 1.3 , 720x480 1.5 ,
                640x480 1.3 , 640x360 1.8
             */

                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                    if (size.getWidth() == 960 && size.getHeight() == 720)
                        sizePreview = size;
                    else if (size.getWidth() == 4000 && size.getHeight() == 2252)
                        sizeCamera = size;
                    else if (size.getWidth() == 1920 && size.getHeight() == 1440)
                        sizeVideo = size;
                }
                break;

            case A:
            /* galaxy A32
                2560x1440 1.8, 1920x1080 1.8, 1440x1080 1.3, 1280x960 1.3, 1280x720 1.8 ,
                1088x1088 1.0, 960x720 1.3, 720x480 1.5, 640x480 1.3, 512x384 1.3, 512x288 1.8 ,
                 384x384 1.0, 352x288 1.2, 320x240 1.3, 256x144 1.8, 176x144 1.2 ,
             */
                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                    if (size.getWidth() == 640 && size.getHeight() == 480)
                        sizePreview = size;
                    else if (size.getWidth() == 2560 && size.getHeight() == 1440)
                        sizeCamera = size;
                    else if (size.getWidth() == 1920 && size.getHeight() == 1080)
                        sizeVideo = size;
                }
                break;
            default:
                utils.logBoth("Model", "size undefined");
        }
        Size[] sizes = new Size[3];
        sizes[0] = sizePreview; sizes[1] = sizeCamera; sizes[2] = sizeVideo;
        return sizes;
    }

    private static void dumpVariousCameraSizes(StreamConfigurationMap map) {

        String sb = "// DUMP CAMERA POSSIBLE SIZES // ";
        for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
            sb += size.getWidth()+"x"+ size.getHeight()+
                    String.format(" %,3.1f , ", (float)size.getWidth() / (float)size.getHeight());
        }
        utils.logOnly("Camera Size",sb);
    }

    @SuppressWarnings("unchecked")
    private static Object serializeStreamConfigurationMap(StreamConfigurationMap map)
            throws org.json.JSONException {
        // TODO: Serialize the rest of the StreamConfigurationMap fields.
        JSONObject mapObj = new JSONObject();
        JSONArray cfgArray = new JSONArray();
        int []fmts = map.getOutputFormats();
        if (fmts != null) {
            for (int fi = 0; fi < Array.getLength(fmts); fi++) {
                Size []sizes = map.getOutputSizes(fmts[fi]);
                if (sizes != null) {
                    String sb = "// DUMP CAMERA POSSIBLE SIZES // "+fmts[fi] +" list ";
                    for (int si = 0; si < Array.getLength(sizes); si++) {
                        JSONObject obj = new JSONObject();
                        obj.put("format", fmts[fi]);
                        obj.put("width", sizes[si].getWidth());
                        obj.put("height", sizes[si].getHeight());
                        obj.put("input", false);
                        obj.put("minFrameDuration", map.getOutputMinFrameDuration(fmts[fi], sizes[si]));
                        cfgArray.put(obj);//from w w  w  . ja  v  a 2s  . c o m
                        sb += si+") "+sizes[si].getWidth()+"x"+sizes[si].getHeight()+  ", ";
                    }
                    utils.logOnly("Camera Size "+fi,sb);
                }
            }
        }
        mapObj.put("availableStreamConfigurations", cfgArray);
        return mapObj;
    }

}