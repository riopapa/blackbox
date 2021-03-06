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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.urrecliner.blackbox.Vars.DATE_PREFIX;
import static com.urrecliner.blackbox.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackbox.Vars.INTERVAL_NORMAL;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mPackageNormalDatePath;
import static com.urrecliner.blackbox.Vars.mPackagePath;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.nextNormalTime;
import static com.urrecliner.blackbox.Vars.sdfLogTime;
import static com.urrecliner.blackbox.Vars.utils;

class NormalMerge {

    private static final String logID = "NormMerge";

    void merge() {
        gpsTracker.askLocation();
        try {
            new MergeFileTask().execute("");
        } catch (Exception e) {
            utils.logE(logID,"Merge Exception: ", e);
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
            if (nextNormalTime == 0)
                nextNormalTime = System.currentTimeMillis() - INTERVAL_NORMAL - 10000;
            beginTimeS = utils.getMilliSec2String(nextNormalTime, FORMAT_LOG_TIME);
            File []files2Merge;
            files2Merge = utils.getDirectoryList(mPackageWorkingPath);
            if (files2Merge.length < 5) {
                publishProgress("<<file[] too short", "" +files2Merge.length);
            }
            else {
                Arrays.sort(files2Merge);
                endTimeS = files2Merge[files2Merge.length - 3].getName();
//                utils.logBoth(logID, beginTimeS+" to "+endTimeS+" len="+files2Merge.length);
                Date date = null;
                try {
                    date = sdfLogTime.parse(endTimeS);
                } catch (ParseException e) {
                    utils.logE(logID, endTimeS+" parse Error", e);
                }
                nextNormalTime = date.getTime() - 2000;
                outputFile = new File(mPackageNormalDatePath, DATE_PREFIX+beginTimeS + " x"+gpsTracker.getLatitude() + "," + gpsTracker.getLongitude() + ".mp4").toString();
                merge2OneVideo(beginTimeS, endTimeS, files2Merge);
            }
            return beginTimeS;
       }

        private void merge2OneVideo(String beginTimeS, String endTimeS, File[] files2Merge) {
            List<Movie> listMovies = new ArrayList<>();
            List<Track> videoTracks = new LinkedList<>();
            List<Track> audioTracks = new LinkedList<>();
    //            Log.w("#of files","files2Merge "+files2Merge.length);
    //            Log.w("time","begin " +beginTimeS+" end "+endTimeS);
            for (File file : files2Merge) {
                String shortFileName = file.getName();
                Collator myCollator = Collator.getInstance();

                if (myCollator.compare(shortFileName, beginTimeS) < 0)
                    file.delete();  // remove old work file
                else if (myCollator.compare(shortFileName, endTimeS) < 0) {
                    try {
                        listMovies.add(MovieCreator.build(file.toString()));
                    } catch (Exception e) {
                        utils.logE(logID, "mergeOne~ ", e);
                    }
                }
            }
            for (Movie movie : listMovies) {
                for (Track track : movie.getTracks()) {
                    if (track.getHandler().equals("vide")) {
                        videoTracks.add(track);
                    }
                    else { // track.getHandler().equals("soun")
                        audioTracks.add(track);
                    }
                }
            }

            if (!videoTracks.isEmpty()) {
                Movie outputMovie = new Movie();
                try {
                        outputMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[0])));
                        outputMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[0])));
                        Container container = new DefaultMp4Builder().build(outputMovie);
                        FileChannel fileChannel = new RandomAccessFile(outputFile, "rw").getChannel();
                        container.writeContainer(fileChannel);
                        fileChannel.close();
                } catch (IOException e) {
                    utils.logE(logID,"IOException~ ", e);
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
            if (values[0].startsWith("<")) {
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

        boolean checkFreeSpace = true;
        @Override
        protected void onPostExecute(String doI ) {
            checkFreeSpace = !checkFreeSpace;
            if (checkFreeSpace && mPackagePath.getFreeSpace() / 1000L < 500000) { // 500Mb free storage ;
                new GatherDiskSpace().run();
            }
        }
    }
}
