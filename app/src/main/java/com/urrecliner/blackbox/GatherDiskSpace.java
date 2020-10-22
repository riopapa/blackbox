package com.urrecliner.blackbox;

import java.io.File;
import java.util.Arrays;

import static com.urrecliner.blackbox.Vars.mPackageNormalPath;
import static com.urrecliner.blackbox.Vars.mPackagePath;
import static com.urrecliner.blackbox.Vars.utils;

class GatherDiskSpace implements Runnable {

    GatherDiskSpace() {
    }

    public void run() {
        /* delete old directory / files if storage is less than xx */
        File[] files = utils.getDirectoryList(mPackageNormalPath);
        if (files.length > 0) { // if any previous folder
            Arrays.sort(files);
            if (files.length > 1) { // more than 2 date directory
                utils.deleteRecursive(files[0]);
                utils.logBoth("Disk", "Squeezed to "+mPackagePath.getFreeSpace() / 1000L);
            } else {  // if this is only folder then remove some files within this directory
                File[] subFiles = utils.getDirectoryList(files[0]);
                if (subFiles.length > 5) {
                    Arrays.sort(subFiles);
                    int max = (subFiles.length > 10) ? 10 : subFiles.length - 1;
                    for (int i = 0; i < max; i++) {  // delete old file first
                        subFiles[i].delete();
                    }
                    utils.logBoth("Disk", "Size squeezed");
                } else {
                    utils.logBoth("No FreeSize", "DISK SPACE Something wrong");
                }
            }
        }
    }
}

