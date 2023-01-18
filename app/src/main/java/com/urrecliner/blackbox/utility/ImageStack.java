package com.urrecliner.blackbox.utility;

import static com.urrecliner.blackbox.Vars.share_image_size;

public class ImageStack {
    public byte [][] snapBytes;
    public int snapNowPos = 0;
    final int arraySize;

    public ImageStack(int share_image_size) {
        snapBytes = new byte[share_image_size][];
        snapNowPos = 0;
        arraySize = share_image_size;
    }

    public void add(byte [] image) {
        snapBytes[snapNowPos] = image;
        snapNowPos++;
        if (snapNowPos >= arraySize)
            snapNowPos = 0;
    }

    public byte[][] getClone() {

        byte [][] images = new byte[arraySize][];
        int jpgIdx = 0;
        for (int i = snapNowPos; i < arraySize; i++) {
            images[jpgIdx++] = snapBytes[i];
            snapBytes[i] = null;
        }
        for (int i = 0; i < snapNowPos; i++) {
            images[jpgIdx++] = snapBytes[i];
            snapBytes[i] = null;
            if (jpgIdx > arraySize)
                break;
        }

        return images;
    }

}