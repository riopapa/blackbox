package com.urrecliner.blackbox;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
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

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.DATE_PREFIX;
import static com.urrecliner.blackbox.Vars.FORMAT_TIME;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.mPackageEventPath;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;

class EventMerge {

    private static final String logID = "Event";

    void merge(final long startTime) {
        if (mExitApplication)
            return;
        try {
            new MergeFileTask().execute("" + startTime);
        } catch (Exception e) {
            utils.logE(logID, "Exception: ", e);
        }
    }

    private static class MergeFileTask extends AsyncTask<String, String, String> {
        String beginTimeS = null;
        String endTimeS;

        private String outputFile;

        @Override
        protected String doInBackground(String... inputParams) {
            long startTime = Long.parseLong(inputParams[0]);
            File[] files2Merge;

            beginTimeS = utils.getMilliSec2String(startTime, FORMAT_TIME);
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();
            files2Merge = utils.getDirectoryList(mPackageWorkingPath);
            if (files2Merge.length < 3) {
                publishProgress("<<file[] too short", "" + files2Merge.length);
            } else {
                Arrays.sort(files2Merge);
                endTimeS = files2Merge[files2Merge.length - 2].getName();
                outputFile = new File(mPackageEventPath, DATE_PREFIX+beginTimeS + SUFFIX
                        + " x" + latitude + "," + longitude + ".mp4").toString();
                merge2OneVideo(beginTimeS, endTimeS, files2Merge);
                MediaPlayer mp = new MediaPlayer();
                try {
                    mp.setDataSource(outputFile);
                    mp.prepare();
                } catch (IOException e) {
                    publishProgress("<<Event IO>>" , "" + files2Merge.length);
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
                        publishProgress("<<Event Merge>>" , "" + files2Merge.length);
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
            String str = values[0];
            utils.logBoth("Event", str);
            if (values[0].startsWith("<")) {
                utils.customToast(str, Toast.LENGTH_SHORT, Color.RED);
            } else {
                utils.customToast(str, Toast.LENGTH_SHORT, Color.YELLOW);
            }
        }

        @Override
        protected void onCancelled(String result) { }

        @Override
        protected void onPostExecute(String doI) {
            utils.beepOnce(3, 1f);
            mActivity.runOnUiThread(() -> {
                String countStr = "" + ++CountEvent;
                vTextCountEvent.setText(countStr);
                activeEventCount--;
                String text = (activeEventCount == 0) ? "" : " "+activeEventCount+" ";
                vTextActiveCount.setText(text);
            });
        }
    }
}
