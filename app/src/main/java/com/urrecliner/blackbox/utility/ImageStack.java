package com.urrecliner.blackbox.utility;

import java.nio.ByteBuffer;

public class ImageStack {
    public byte [][] snapBytes;
    public int snapNowPos = 0;
    final int arraySize;

    public ImageStack(int share_image_size) {
        snapBytes = new byte[share_image_size][];
        snapNowPos = 0;
        arraySize = share_image_size;
    }

    public void addBuff(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        snapBytes[snapNowPos] = bytes.clone();
        snapNowPos++;
        if (snapNowPos >= arraySize)
            snapNowPos = 0;
    }

    public void addShot(byte[] bytes) {
        snapBytes[snapNowPos] = bytes.clone();
        snapNowPos++;
        if (snapNowPos >= arraySize)
            snapNowPos = 0;
    }

    public byte[][] getClone(int startPos) {

        byte [][] images = new byte[arraySize][];
        int jpgIdx = 0;
        for (int i = startPos; i < arraySize; i++) {
            images[jpgIdx++] = snapBytes[i].clone();
            snapBytes[i] = null;
        }
        for (int i = 0; i < startPos-1; i++) {
            if (jpgIdx >= arraySize)
                break;
            images[jpgIdx++] = snapBytes[i].clone();
            snapBytes[i] = null;
        }
        return images;
    }

}