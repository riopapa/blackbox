package com.urrecliner.blackbox;

import android.graphics.Color;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static com.urrecliner.blackbox.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackbox.Vars.INTERVAL_NORMAL;
import static com.urrecliner.blackbox.Vars.gatherDiskSpace;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.mPackageNormalDatePath;
import static com.urrecliner.blackbox.Vars.mPackagePath;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.sdfDate;
import static com.urrecliner.blackbox.Vars.utils;

class NormalMerge {

    private static String logID = "NormMerge";
    private static long normalStartTime = 0;

    void merge() {
        if (mExitApplication)
            return;
        gpsTracker.askLocation();
        if (normalStartTime == 0) {
            normalStartTime = System.currentTimeMillis() - INTERVAL_NORMAL;
        }
        try {
            new MergeFileTask().execute("" + normalStartTime);
        } catch (Exception e) {
            utils.logException(logID,"Merge Exception: ", e);
        }
    }
    private static class MergeFileTask extends AsyncTask< String, String, String> {
        String beginTimeS = null;
        String endTimeS;
        String outputFile;

        @Override
        protected void onPreExecute() {
        }
        @Override
        protected String doInBackground(String... inputParams) {
            long startTime = Long.parseLong(inputParams[0]);
            File []files2Merge;

            beginTimeS = utils.getMilliSec2String(startTime, FORMAT_LOG_TIME);

            files2Merge = utils.getDirectoryList(mPackageWorkingPath);
            if (files2Merge.length < 3) {
                publishProgress("<<file[] too short", "" +files2Merge.length);
            }
            else {
                Arrays.sort(files2Merge);
                endTimeS = files2Merge[files2Merge.length - 2].getName();
                try {
                    Date date = sdfDate.parse(endTimeS);
                    normalStartTime = date.getTime();
                } catch (ParseException e) {
                    utils.logException("parse", endTimeS, e);
                }
                outputFile = new File(mPackageNormalDatePath, beginTimeS + " x"+gpsTracker.getLatitude() + "," + gpsTracker.getLongitude() + ".mp4").toString();
                merge2OneVideo(beginTimeS, endTimeS, files2Merge);
            }
            return beginTimeS;
       }

        private void merge2OneVideo(String beginTimeS, String endTimeS, File[] files2Merge) {
            List<Movie> listMovies = new ArrayList<>();
            List<Track> videoTracks = new LinkedList<>();
//            List<Track> audioTracks = new LinkedList<>();
    //            Log.w("#of files","files2Merge "+files2Merge.length);
    //            Log.w("time","begin " +beginTimeS+" end "+endTimeS);
            for (File file : files2Merge) {
                String shortFileName = file.getName();
                Collator myCollator = Collator.getInstance();

                if (myCollator.compare(shortFileName, beginTimeS) < 0)
                    file.delete();  // remove old work file
                if (myCollator.compare(shortFileName, beginTimeS) >= 0 &&
                        myCollator.compare(shortFileName, endTimeS) < 0) {
    //                    Log.w("add to movie " + isEventMerge, shortFileName);
                    try {
                        listMovies.add(MovieCreator.build(file.toString()));
                    } catch (Exception e) {
                        utils.logException(logID, "mergeOne~ ", e);
                    }
                }
            }
            for (Movie movie : listMovies) {
                for (Track track : movie.getTracks()) {
                    if (track.getHandler().equals("vide")) {
                        videoTracks.add(track);
                    }
//                    else { // track.getHandler().equals("soun")
//                        audioTracks.add(track);
//                    }
                }
            }

            if (!videoTracks.isEmpty()) {
                Movie outputMovie = new Movie();
                try {
                        outputMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[0])));
//                        outputMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[0])));
                        Container container = new DefaultMp4Builder().build(outputMovie);
                        FileChannel fileChannel = new RandomAccessFile(outputFile, "rw").getChannel();
                        container.writeContainer(fileChannel);
                        fileChannel.close();
                } catch (IOException e) {
                    utils.logException(logID,"IOException~ ", e);
                }
            } else {
                utils.beepOnce(3, 1f);
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String debugText = values[0];
            TextView mLogInfo = mActivity.findViewById(R.id.textLogInfo);
            mLogInfo.setText(debugText);
            if (values[0].substring(0,1).equals("<")) {
                utils.customToast(debugText, Toast.LENGTH_SHORT, Color.RED);
    //                utils.logE("1", debugText);
            }
            else {
                utils.customToast(debugText, Toast.LENGTH_SHORT, Color.YELLOW);
    //                utils.log("2", debugText);
            }
        }
        @Override
        protected void onCancelled(String result) {
            utils.logOnly(logID, "canceled result:"+result);
        }

        @Override
        protected void onPostExecute(String doI ) {

            if (mPackagePath.getFreeSpace() / 1000L < 2500000) { // 2.5Gb free storage ;
                gatherDiskSpace.run();
            }
        }
    }
}
