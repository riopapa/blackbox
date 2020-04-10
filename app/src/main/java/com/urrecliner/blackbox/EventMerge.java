package com.urrecliner.blackbox;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileOutputStream;
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

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackbox.Vars.INTERVAL_EVENT;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.mPackageEventPath;
import static com.urrecliner.blackbox.Vars.mPackageWorkingPath;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;

class EventMerge {

    private static String logID = "Event";
    private static int idx;

    private static byte[][] jpgBytes = new byte[MAX_IMAGES_SIZE][];
    private static CountDownTimer countDownTimer;
    private static File thisEventPath;

    void merge(long startTime) {
        if (mExitApplication)
            return;
        final long fromTime = startTime;
        new Timer().schedule(new TimerTask() {
            public void run() {
                if (snapMapIdx == 0)
                    jpgBytes = snapBytes.clone();
                else {
                    int jdx = 0;
                    for (int i = snapMapIdx; i < MAX_IMAGES_SIZE; i++)
                        jpgBytes[jdx++] = snapBytes[i];
                    for (int i = 0; i < snapMapIdx; i++)
                        jpgBytes[jdx++] = snapBytes[i];
                }
                try {
                    new MergeFileTask().execute("" + fromTime);
                } catch (Exception e) {
                    utils.logE(logID, "Exception: " + e.toString());
                }
            }
        }, INTERVAL_EVENT + INTERVAL_EVENT / 4);
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
                outputFile = new File(mPackageEventPath, beginTimeS + " x" + latitude + "," + longitude + ".mp4").toString();
                merge2OneVideo(beginTimeS, endTimeS, files2Merge);
                MediaPlayer mp = new MediaPlayer();
                try {
                    mp.setDataSource(outputFile);
                    mp.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                    utils.logE(logID, "IOException: " + e.toString());
                }
                mp.release();
            }
            return beginTimeS;
        }

        private void merge2OneVideo(String beginTimeS, String endTimeS, File[] files2Merge) {
            List<Movie> listMovies = new ArrayList<>();
            List<Track> videoTracks = new LinkedList<>();
            Collator myCollator = Collator.getInstance();
            for (File file : files2Merge) {
                String shortFileName = file.getName();
                if (myCollator.compare(shortFileName, beginTimeS) >= 0 &&
                        myCollator.compare(shortFileName, endTimeS) < 0) {
//                    Log.w("add to movie " + isEventMerge, shortFileName);
                    try {
                        listMovies.add(MovieCreator.build(file.toString()));
                    } catch (Exception e) {
                        utils.logE(logID, "mergeOne~ " + file.toString());
                    }
                }
            }
            for (Movie movie : listMovies) {
                for (Track track : movie.getTracks()) {
                    if (track.getHandler().equals("vide")) {
                        videoTracks.add(track);
                    }
                }
            }

            if (!videoTracks.isEmpty()) {
                Movie outputMovie = new Movie();
                try {
                    outputMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[0])));
                    Container container = new DefaultMp4Builder().build(outputMovie);
                    FileChannel fileChannel = new RandomAccessFile(outputFile, "rw").getChannel();
                    container.writeContainer(fileChannel);
                    fileChannel.close();
                } catch (IOException e) {
                    utils.logE(logID, "IOException~ " + e.toString());
                }
            } else {
                utils.beepOnce(3, 1f);
                utils.logE(logID, "IOException~ ");
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
        protected void onCancelled(String result) {
        }

        @Override
        protected void onPostExecute(String doI) {
            idx = 0;
            thisEventPath = new File(mPackageEventPath, beginTimeS);
            utils.readyPackageFolder(thisEventPath);
            countDownTimer();
            countDownTimer.start();
        }

        private void countDownTimer() {

            utils.log(logID, "Copying Event Images..");
            countDownTimer = new CountDownTimer(2000 * MAX_IMAGES_SIZE, 300) {
                public void onTick(long millisUntilFinished) {
                    if (idx < MAX_IMAGES_SIZE) {
                        if (jpgBytes[idx] != null && jpgBytes[idx].length > 0) {
                            File jpgFile = new File(thisEventPath, beginTimeS + "_" + ("" + (1000 + idx)).substring(1, 4) + ".jpg");
//                        utils.log(logID, " jpg " + idx + " " + jpgFile.toString());
                            bytes2File(jpgBytes[idx], jpgFile);
                        }
                        idx++;
                    } else {
                        utils.beepOnce(3, .7f);
                        String countStr = "" + ++CountEvent;
                        vTextCountEvent.setText(countStr);
                        activeEventCount--;
                        String text = (activeEventCount == 0) ? "" : "< " + activeEventCount + " >\n";
                        vTextActiveCount.setText(text);
                        ImageButton mEventButton = mActivity.findViewById(R.id.btnEvent);
                        mEventButton.setImageResource(R.mipmap.event_ready);
                        //                    utils.log(logID, idx + " jpgs "+(Runtime.getRuntime().freeMemory()));
                        countDownTimer.cancel();
                        utils.customToast("Event Recording completed", Toast.LENGTH_SHORT, Color.CYAN);
//                        utils.log(logID, "Event File: "+thisEventPath.getName());
                    }
                }
                public void onFinish() { }
            };
        }

        private void bytes2File(byte[] bytes, File file) {

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
