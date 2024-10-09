package biz.riopapa.blackbox.utility;

import static biz.riopapa.blackbox.Vars.SUFFIX;
import static biz.riopapa.blackbox.Vars.VIDEO_ENCODING_RATE;
import static biz.riopapa.blackbox.Vars.VIDEO_FRAME_RATE;
import static biz.riopapa.blackbox.Vars.VIDEO_ONE_WORK_FILE_SIZE;
import static biz.riopapa.blackbox.Vars.share_image_size;
import static biz.riopapa.blackbox.Vars.share_left_right_interval;
import static biz.riopapa.blackbox.Vars.share_snap_interval;
import static biz.riopapa.blackbox.Vars.share_work_size;
import static biz.riopapa.blackbox.Vars.utils;
import static biz.riopapa.blackbox.VideoMain.zoomHuge;
import static biz.riopapa.blackbox.VideoMain.zoomNormal;

import android.os.Build;

import biz.riopapa.blackbox.Vars;

public class VideoSize {
    public void set() {
        switch (Build.MODEL) {
            case "SM-G977N":
                SUFFIX = Vars.PhoneE.H;
                zoomNormal = 1.1f;
                zoomHuge = 1.6f;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 20000 * 1000;
                VIDEO_ONE_WORK_FILE_SIZE = share_work_size * 10000;
                break;
            case "SM-N986N":
                SUFFIX = Vars.PhoneE.N;
                zoomNormal = 1.2f;
                zoomHuge = 1.7f;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 20000 * 1000;
                VIDEO_ONE_WORK_FILE_SIZE = share_work_size * 10000;
                break;
            default:
                utils.logBoth("Vars", "UnKnown Model=" + Build.MODEL);
                break;
        }
    }
}
