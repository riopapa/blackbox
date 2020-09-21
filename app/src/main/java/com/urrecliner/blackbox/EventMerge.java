package com.urrecliner.blackbox;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.DATE_PREFIX;
import static com.urrecliner.blackbox.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackbox.Vars.INTERVAL_EVENT;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.mPackageEventPath;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;

class EventMerge {

    private static String logID = "Event";
    private static File thisEventPath;

    void merge(final long startTime, final File eventPath) {
        if (mExitApplication)
            return;
        thisEventPath = eventPath;
        try {
            new EventMerge.MergeFileTask().execute("" + startTime);
        } catch (Exception e) {
            utils.logE(logID, "Exception: ", e);
        }
    }

    private static class MergeFileTask extends AsyncTask<String, String, String> {
        String beginTimeS = null;
        String endTimeS;

        private String outputFile;

//        @Override
//        protected void onPreExecute() {
//        }

        @Override
        protected String doInBackground(String... inputParams) {
            long startTime = Long.parseLong(inputParams[0]);
            File[] files2Merge;

            beginTimeS = utils.getMilliSec2String(startTime, FORMAT_LOG_TIME);
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();
            files2Merge = utils.getDirectoryList(mPackageWorkingPath);
            if (files2Merge.length < 3) {
                publishProgress("<<file[] too short", "" + files2Merge.length);
            } else {
                Arrays.sort(files2Merge);
                endTimeS = files2Merge[files2Merge.length - 2].getName();
                Log.w("Event Last Time", endTimeS);
                outputFile = new File(mPackageEventPath, DATE_PREFIX+beginTimeS + " x" + latitude + "," + longitude + ".mp4").toString();
                merge2OneVideo(beginTimeS, endTimeS, files2Merge);
                MediaPlayer mp = new MediaPlayer();
                try {
                    mp.setDataSource(outputFile);
                    mp.prepare();
                } catch (IOException e) {
                    utils.logE(logID, "IOException: ", e);
                }
                mp.release();
            }
            return beginTimeS;
        }

        private void merge2OneVideo(String beginTimeS, String endTimeS, File[] files2Merge) {
            List<Movie> listMovies = new ArrayList<>();
            List<Track> videoTracks = new LinkedList<>();
            List<Track> audioTracks = new LinkedList<>();
            Collator myCollator = Collator.getInstance();
            for (File file : files2Merge) {
                String shortFileName = file.getName();
                if (myCollator.compare(shortFileName, beginTimeS) >= 0 &&
                        myCollator.compare(shortFileName, endTimeS) < 0) {
//                    Log.w("add to movie " + isEventMerge, shortFileName);
                    try {
                        listMovies.add(MovieCreator.build(file.toString()));
                    } catch (Exception e) {
                        utils.logBoth(logID, "mergeOne~ " + file.toString());
                    }
                }
            }
            for (Movie movie : listMovies) {
                for (Track track : movie.getTracks()) {
                    if (track.getHandler().equals("vide")) {    // excluding "audi"
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
                    utils.logE(logID, "IOException~ ", e);
                }
            } else {
                utils.beepOnce(3, 1f);
                utils.logOnly(logID, "IOException~ ");
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String debugText = values[0];
            TextView mLogInfo = mActivity.findViewById(R.id.textLogInfo);
            mLogInfo.setText(debugText);
            if (values[0].substring(0, 1).equals("<")) {
                utils.customToast(debugText, Toast.LENGTH_SHORT, Color.RED);
//                utils.logE("1", debugText);
            } else {
                utils.customToast(debugText, Toast.LENGTH_SHORT, Color.YELLOW);
//                utils.log("2", debugText);
            }
        }

        @Override
        protected void onCancelled(String result) { }

        @Override
        protected void onPostExecute(String doI) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.start(thisEventPath, snapMapIdx,false);
                }
            }, 10);
        }
    }
}
