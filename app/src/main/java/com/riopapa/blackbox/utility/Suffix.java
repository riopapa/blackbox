package com.riopapa.blackbox.utility;

import static com.riopapa.blackbox.Vars.INTERVAL_EVENT;
import static com.riopapa.blackbox.Vars.INTERVAL_NORMAL;
import static com.riopapa.blackbox.Vars.SUFFIX;
import static com.riopapa.blackbox.Vars.utils;
import static com.riopapa.blackbox.Vars.share_image_size;
import static com.riopapa.blackbox.Vars.share_snap_interval;
import static com.riopapa.blackbox.Vars.share_left_right_interval;
import static com.riopapa.blackbox.Vars.VIDEO_ENCODING_RATE;
import static com.riopapa.blackbox.Vars.VIDEO_FRAME_RATE;
import static com.riopapa.blackbox.Vars.VIDEO_ONE_WORK_FILE_SIZE;

import android.os.Build;

import com.riopapa.blackbox.Vars;

public class Suffix {
    public void set() {
        INTERVAL_EVENT = 25 * 1000;
        INTERVAL_NORMAL = INTERVAL_EVENT * 4L;
        switch (Build.MODEL) {
            case "SM-G965N":
                SUFFIX = Vars.PhoneE.P;
                break;
            case "SM-N986N":
                SUFFIX = Vars.PhoneE.N;
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
                VIDEO_FRAME_RATE = 24;
                VIDEO_ENCODING_RATE = 24*1000*1000;
                VIDEO_ONE_WORK_FILE_SIZE = 16*1024*1024;
                break;
            case N:           // galaxy note 20
                share_image_size = 151;
                share_snap_interval = 172;
                share_left_right_interval = 112;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 24*1000*1000;
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
