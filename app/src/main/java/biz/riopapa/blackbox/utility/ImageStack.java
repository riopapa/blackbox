package biz.riopapa.blackbox.utility;

import static biz.riopapa.blackbox.StartStopExit.zoomChangeTimer;
import static biz.riopapa.blackbox.Vars.utils;
import static biz.riopapa.blackbox.PhotoCapture.leftRight;

import android.util.Log;

import java.nio.ByteBuffer;

public class ImageStack {
    public static byte [][] snapBytes;
    public static long [] snapTime;
    public int snapNowPos = 0;
    final int arraySize;

    public ImageStack(int share_image_size) {
        snapBytes = new byte[share_image_size][];
        snapTime = new long[share_image_size];
        snapNowPos = 0;
        arraySize = share_image_size;
    }

    public void addImageBuff(ByteBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            snapTime[snapNowPos] = System.currentTimeMillis();
            snapBytes[snapNowPos] = bytes.clone();
            snapNowPos++;
            if (snapNowPos >= arraySize)
                snapNowPos = 0;
            leftRight = !leftRight;
            zoomChangeTimer.sendEmptyMessage(0);
        } catch (Exception e) {
        }
    }

    public void addShotBuff(byte[] bytes) {
        snapTime[snapNowPos] = System.currentTimeMillis();
        snapBytes[snapNowPos] = bytes.clone();
        snapNowPos++;
        if (snapNowPos >= arraySize)
            snapNowPos = 0;
    }

}