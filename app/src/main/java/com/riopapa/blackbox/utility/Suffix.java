package com.riopapa.blackbox.utility;

import static com.riopapa.blackbox.Vars.SUFFIX;
import static com.riopapa.blackbox.Vars.VIDEO_ENCODING_RATE;
import static com.riopapa.blackbox.Vars.VIDEO_FRAME_RATE;
import static com.riopapa.blackbox.Vars.VIDEO_ONE_WORK_FILE_SIZE;
import static com.riopapa.blackbox.Vars.share_image_size;
import static com.riopapa.blackbox.Vars.share_left_right_interval;
import static com.riopapa.blackbox.Vars.share_snap_interval;
import static com.riopapa.blackbox.Vars.utils;
import static com.riopapa.blackbox.VideoMain.zoomHuge;
import static com.riopapa.blackbox.VideoMain.zoomNormal;

import android.os.Build;

import com.riopapa.blackbox.Vars;

public class Suffix {
    public void set() {
        switch (Build.MODEL) {
            case "SM-G965N":
                SUFFIX = Vars.PhoneE.P;
                zoomNormal = 1.1f;
                zoomHuge = 1.6f;
                break;
            case "SM-N986N":
                SUFFIX = Vars.PhoneE.N;
                zoomNormal = 1.3f;
                zoomHuge = 1.9f;
                break;
            case "SM-A325N":
                SUFFIX = Vars.PhoneE.A;
                break;
            default:
                utils.logBoth("Vars", "UnKnown Model="+Build.MODEL);
                break;
        }

        switch (SUFFIX) {
            case P:           // galaxy s9 phone
                share_image_size = 157;
                share_snap_interval = 177;
                share_left_right_interval = 78;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 30*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 16*1024*1024;
                break;
            case N:           // galaxy note 20
                share_image_size = 151;
                share_snap_interval = 172;
                share_left_right_interval = 112;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 30*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 18*1024*1024;
                break;
            case A:           // galaxy A32
                share_image_size = 125;
                share_snap_interval = 211;
                share_left_right_interval = 140;
                VIDEO_FRAME_RATE = 24;
                VIDEO_ENCODING_RATE = 20*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 16*1024*1024;
                break;
        }
    }
}
