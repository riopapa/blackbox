package com.riopapa.blackbox.utility;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class DiskSpace implements Runnable {

    public String squeeze(File mPackageNormalPath) {
        if (mPackageNormalPath.getFreeSpace() / 1000L > 4*1000*1000) // > 4Gb ?
            return "";

        File[] files = mPackageNormalPath.listFiles();
        if (files != null && files.length > 0) { // if any previous folder
            Arrays.sort(files);
            if (files.length > 1) { // more than 2 date directory
                deleteFolder(files[0]);
                return "Folder Squeezed to "+(mPackageNormalPath.getFreeSpace() / 1000L);
            } else {  // if this is only folder then remove some files within this directory
                File[] subFiles = files[0].listFiles();
                if (subFiles != null && subFiles.length > 5) {
                    Arrays.sort(subFiles);
                    int max = (subFiles.length > 10) ? 10 : subFiles.length - 1;
                    for (int i = 0; i < max; i++) {  // delete old file first
                        subFiles[i].delete();
                    }
                    return "File squeezed to "+(mPackageNormalPath.getFreeSpace() / 1000L);
                } else {
                    return "SPACE Something wrong";
                }
            }
        }
        return "";
    }

    void deleteFolder(File file) {
        String deleteCmd = "rm -r " + file.toString();
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(deleteCmd);
        } catch (IOException e) { }
    }

    @Override
    public void run() { }
}