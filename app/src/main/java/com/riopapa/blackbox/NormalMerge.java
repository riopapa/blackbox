package com.riopapa.blackbox;

import android.graphics.Color;
import android.os.AsyncTask;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.riopapa.blackbox.utility.DiskSpace;

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

import static com.riopapa.blackbox.Vars.DATE_PREFIX;
import static com.riopapa.blackbox.Vars.FORMAT_TIME;
import static com.riopapa.blackbox.Vars.SUFFIX;
import static com.riopapa.blackbox.Vars.gpsTracker;
import static com.riopapa.blackbox.Vars.mPackageNormalDatePath;
import static com.riopapa.blackbox.Vars.mPackageNormalPath;
import static com.riopapa.blackbox.Vars.mPackageWorkingPath;
import static com.riopapa.blackbox.Vars.normal_duration;
import static com.riopapa.blackbox.Vars.sdfTime;
import static com.riopapa.blackbox.Vars.utils;

class NormalMerge {

    private static final String logID = "NormMerge";
    static long nextNormalTime = 0;

    void merge() {
//        gpsTracker.askLocation();
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
                nextNormalTime = System.currentTimeMillis() - normal_duration - 10000;
            beginTimeS = utils.getMilliSec2String(nextNormalTime, FORMAT_TIME);
            File []files2Merge;
            files2Merge = utils.getDirectoryList(mPackageWorkingPath);
            if (files2Merge.length < 3) {
                publishProgress("normal", "files short, len=" +files2Merge.length);
            }
            else {
                Arrays.sort(files2Merge);
                endTimeS = files2Merge[files2Merge.length - 3].getName();
//                utils.logBoth(logID, beginTimeS+" to "+endTimeS+" len="+files2Merge.length);
                Date date = null;
                try {
                    date = sdfTime.parse(endTimeS);
                } catch (ParseException e) {
                    utils.logE(logID, endTimeS+" parse Error", e);
                }
                assert date != null;
                nextNormalTime = date.getTime() - 6000;
                outputFile = new File(mPackageNormalDatePath, DATE_PREFIX+beginTimeS + SUFFIX
                        +" x"+gpsTracker.getLatitude() + "," + gpsTracker.getLongitude() + ".mp4").toString();
                merge2OneVideo(beginTimeS, endTimeS, files2Merge);
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
                if (myCollator.compare(shortFileName, beginTimeS) < 0) {
                    file.delete();  // remove old working file
                } else if (myCollator.compare(shortFileName, endTimeS) < 0) {
                    try {
                        listMovies.add(MovieCreator.build(file.toString()));
                    } catch (Exception e) {
                        utils.logBoth("normal "+files2Merge.length, "<mergeOne Exception~> "+shortFileName);
                    }
                }
            }
            for (Movie movie : listMovies) {
                for (Track track : movie.getTracks()) {
                    if (track.getHandler().equals("vide")) {
                        videoTracks.add(track);
                    }
                    else {
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
                    utils.logE(logID,"<IOException~> ", e);
                }
            } else {
                utils.beepOnce(3, 1f);
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String str = values[0];
            if (str.startsWith("<")) {
                utils.customToast(str, Toast.LENGTH_LONG, Color.RED);
    //                utils.logE("1", str);
            }
            else {
                utils.customToast(str, Toast.LENGTH_LONG, Color.YELLOW);
    //                utils.log("2", str);
            }
        }
        @Override
        protected void onCancelled(String result) {
            utils.logOnly(logID, "canceled result:"+result);
        }

        @Override
        protected void onPostExecute(String doI ) {
            String msg = new DiskSpace().squeeze(mPackageNormalPath);
            if (msg.length() > 0)
                utils.logBoth("DISK", msg);
        }
    }
}