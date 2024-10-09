package biz.riopapa.blackbox.utility;

import static biz.riopapa.blackbox.Vars.utils;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Size;

import biz.riopapa.blackbox.Vars;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;

public class CameraSize {

// "SM-G986N" "N";  note 20
// "SM-G965N" "P";
// "SM-A325N" "A";
// "SM-G977N" "H"; S10

    public static Size[] set(StreamConfigurationMap map, Vars.PhoneE SUFFIX) {

//        dumpVariousCameraSizes(map);

        Size sizePreview = null, sizeCamera = null, sizeVideo = null;

        //        String model = Build.MODEL;
        //        utils.logBoth("logID", "CamSize on "+model);
        //        dumpVariousCameraSizes(map);
        map.getOutputFormats();
        Log.w("CameraSize", "map.getOutputFormats()");
        switch (SUFFIX) {

            case H:
            /* galaxy S10 first
            4032x3024 1.3 , 4032x2268 1.8 , 4032x1908 2.1 , 3024x3024 1.0 , 960x540 1.8 ,
            800x600 1.3 , 3840x2160 1.8 , 2288x1080 2.1 , 1920x1440 1.3 , 1920x1080 1.8 ,
            1920x912 2.1 , 1440x1080 1.3 , 1280x720 1.8 , 1088x1088 1.0 , 960x720 1.3 ,
            720x480 1.5 , 640x480 1.3 , 640x360 1.8 , 352x288 1.2 , 320x240 1.3 ,
            256x144 1.8 , 176x144 1.2 ,

            /* galaxy S10 second
            4608x3456 1.3 , 4608x2592 1.8 , 4608x2184 2.1 , 3456x3456 1.0 , 3840x2160 1.8 ,
            2288x1080 2.1 , 1920x1440 1.3 , 1920x1080 1.8 , 1920x912 2.1 , 1440x1080 1.3 ,
            1280x720 1.8 , 1088x1088 1.0 , 960x720 1.3 , 720x480 1.5 , 640x480 1.3 ,
            640x360 1.8 , 352x288 1.2 , 320x240 1.3 , 256x144 1.8 , 176x144 1.2 ,

             */
                for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                    if (size.getWidth() == 640 && size.getHeight() == 480)
                        sizePreview = size;
                    else if (size.getWidth() == 4608 && size.getHeight() == 2592) // 1.8
                        sizeCamera = size;  // s10 5G second camera
                    else if (size.getWidth() == 1280 && size.getHeight() == 720)   // 1.8
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
                    else if (size.getWidth() == 1280 && size.getHeight() == 720)
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