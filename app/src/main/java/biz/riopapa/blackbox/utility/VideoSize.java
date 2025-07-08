package biz.riopapa.blackbox.utility;

import static biz.riopapa.blackbox.Vars.SUFFIX;
import static biz.riopapa.blackbox.Vars.VIDEO_ENCODING_RATE;
import static biz.riopapa.blackbox.Vars.VIDEO_FRAME_RATE;
import static biz.riopapa.blackbox.Vars.VIDEO_ONE_WORK_FILE_SIZE;
import static biz.riopapa.blackbox.Vars.share_work_size;
import static biz.riopapa.blackbox.Vars.utils;
import static biz.riopapa.blackbox.VideoMain.zoomBig;
import static biz.riopapa.blackbox.VideoMain.zoomNormal;
import static biz.riopapa.blackbox.VideoMain.zoomBigShot;

import android.os.Build;

import biz.riopapa.blackbox.Vars;

public class VideoSize {
    public void set() {
        zoomNormal = 1.2f;
        zoomBig = 1.7f;
        zoomBigShot = 2.2f;
        switch (Build.MODEL) {
            case "SM-G977N":    // black box
                SUFFIX = Vars.PhoneE.H;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 20000 * 1000;
                VIDEO_ONE_WORK_FILE_SIZE = share_work_size * 10000;
                break;
            case "SM-N986N":    // note 20
                SUFFIX = Vars.PhoneE.N;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 20000 * 1000;
                VIDEO_ONE_WORK_FILE_SIZE = share_work_size * 10000;
                break;
            case "SM-S938N":    // note 20
                SUFFIX = Vars.PhoneE.U;
                VIDEO_FRAME_RATE = 30;
                VIDEO_ENCODING_RATE = 24000 * 1000;
                VIDEO_ONE_WORK_FILE_SIZE = share_work_size * 10000;
                break;
            default:
                utils.logBoth("Vars", "UnKnown Model=" + Build.MODEL);
                break;
        }
    }
}
