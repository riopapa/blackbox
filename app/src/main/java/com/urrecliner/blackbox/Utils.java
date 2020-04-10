package com.urrecliner.blackbox;

import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.urrecliner.blackbox.Vars.FORMAT_DATE;
import static com.urrecliner.blackbox.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mPackageLogPath;
import static com.urrecliner.blackbox.Vars.vTextLogInfo;


class Utils {
    private String logDate = getMilliSec2String(System.currentTimeMillis(),FORMAT_DATE);

    String getMilliSec2String(long milliSec, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        return dateFormat.format(new Date(milliSec));
    }

    String getNowTimeString(String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        return dateFormat.format(System.currentTimeMillis());
    }

    boolean readyPackageFolder (File dir){
        try {
            if (!dir.exists()) return dir.mkdirs();
            else
                return true;
        } catch (Exception e) {
            Log.e("creating Folder error", dir + "_" + e.toString());
        }
        return false;
    }

    File[] getDirectoryList(File fullPath) {
        return fullPath.listFiles();
    }

    File[] getDirectoryFiltered(File fullPath, final String fileType) {
        File[] files = fullPath.listFiles(file -> (file.getPath().endsWith(fileType) && file.length() > 100));
        return files;
    }

    /* delete files within directory if name is less than fileName */

    void deleteFiles(File directory, String fileName) {

        File[] files = directory.listFiles();
        if(null!=files){
            Collator myCollator = Collator.getInstance();
            for (File file : files) {
                String shortFileName = file.getName();
                if (myCollator.compare(shortFileName, fileName) < 0) {
                    file.delete();
                }
            }
        }
    }

    /* delete directory and files under that directory */
    boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        return fileOrDirectory.delete();
    }

    void deleteOldLogs() {
        final SimpleDateFormat sdfDate = new SimpleDateFormat(FORMAT_DATE, Locale.US);

        String oldDate = "log_" + sdfDate.format(System.currentTimeMillis() - 3*24*60*60*1000L);
        File[] files = getCurrentFileList(mPackageLogPath);
        Collator myCollator = Collator.getInstance();
        for (File file : files) {
            String shortFileName = file.getName();
            if (myCollator.compare(shortFileName, oldDate) < 0) {
                if (!file.delete())
                    Log.e("file","Delete Error "+file);
            }
        }
    }

    private File[] getCurrentFileList(File fullPath) {
        return fullPath.listFiles();
    }


    void log (String tag, String text) {
//        int pid = android.os.Process.myPid();
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text;
        Log.w(tag , log);
        append2file(mPackageLogPath, "log_" + logDate + ".txt", getMilliSec2String(System.currentTimeMillis(), FORMAT_LOG_TIME) +  ": " + log);
        text = vTextLogInfo.getText().toString() + "\n" + getMilliSec2String(System.currentTimeMillis(), "HH:mm ")+text;
        text = truncLine(text);
        final String fText = tag + ":" + text;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vTextLogInfo.setText(fText);
            }
        });
    }

    void logE(String tag, String text) {
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " [err:"+ tag + "] " + text;
        Log.e("<" + tag + ">" , log);
        append2file(mPackageLogPath, "log_" + logDate + ".txt", getMilliSec2String(System.currentTimeMillis(), FORMAT_LOG_TIME) +  "> " + log);
        append2file(mPackageLogPath, "log_" + logDate + "E.txt", getMilliSec2String(System.currentTimeMillis(), FORMAT_LOG_TIME) +  "> " + log);
        text = vTextLogInfo.getText().toString() + "\n" + text;
        text = truncLine(text);
        final String fText = text;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vTextLogInfo.setText(fText);
            }
        });
    }

    static private String []omits = { "performResume", "performCreate", "dispatch", "callActivityOnResume", "access$",
            "handleReceiver", "handleMessage", "dispatchKeyEvent", "moveToState", "mainLoop"};
    private String traceName (String s) {
        for (String o : omits) {
            if (s.contains(o)) return "";
        }
        return s + "> ";
    }

    private String traceClassName(String s) {
        return s.substring(s.lastIndexOf(".")+1);
    }

    void customToast  (final String text, final int short_Long, final int foreColor) {

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(mContext, text, short_Long);
                toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER, 0,0);
                View toastView = toast.getView(); // This'll return the default View of the Toast.
                TextView toastMessage = toastView.findViewById(android.R.id.message);
                toastMessage.setTextSize(24);
                int backColor = foreColor ^ 0xececec;
                toastMessage.setTextColor(foreColor);
                toastMessage.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.spy_car, 0, 0, 0);
                toastMessage.setCompoundDrawablePadding(16);
                toastMessage.setPadding(4,4,24,4);
                toastView.setBackgroundColor(backColor);
                toast.show();
//                log("customToast",text);
            }
        });

    }

    void displayCount(String text, int short_Long, int backColor) {

        Toast toast = Toast.makeText(mContext, text, short_Long);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER, 0,0);
        View toastView = toast.getView(); // This'll return the default View of the Toast.

        /* And now you can get the TextView of the default View of the Toast. */
        TextView tm = toastView.findViewById(android.R.id.message);
        tm.setTextSize(48);
        tm.setGravity(Gravity.CENTER);
        tm.setMaxWidth(2000);
        tm.setWidth(2000);

        if (backColor == Color.YELLOW)
            tm.setTextColor(Color.BLUE);
        else
            tm.setTextColor(Color.WHITE);
//        toastMessage.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_launcher, 0, 0, 0);
        tm.setCompoundDrawablePadding(16);
        tm.setPadding(4,4,4,4);
        toastView.setBackgroundColor(backColor);
        toast.show();
    }

    private String truncLine(String str) {
        String[] strArray = str.split("\n");
        if (strArray.length > 5) {
            String result = "";
            for (int i = strArray.length - 5; i < strArray.length; i++)
                result += strArray[i]+"\n";
            return result.substring(0,result.length()-1);
        }
        return str;
    }

    void append2file (File directory, String filename, String text) {
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            File file = new File(directory, filename);
            if (!file.exists()) {
                if(!file.createNewFile()) {
                    Log.e("createFile"," Error");
                }
            }
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write("\n" + text + "\n");
        } catch (IOException e) {
            String s = directory.toString() + filename + " Err:" + e.toString();
            logE("append",s);
            Log.e("appendIOExcept1",  e.getMessage());
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                String s = directory.toString() + filename + " close~" + e.toString();
                Log.e("appendIOExcept2",  e.getMessage());
            }
        }
    }

    void write2file (File directory, String filename, String text) {
        final File file = new File(directory, filename);
        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.write(text);
            myOutWriter.close();
            fOut.flush();
            fOut.close();
        }
        catch (IOException e) {
            String s = file.toString() + " Err:" + e.toString();
            logE("write",s);
            e.printStackTrace();
        }
    }

//    public void singleBeep(Activity activity,int type) {
//        try {
//            Uri notification;
//            if (type == 0) {
//                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            }
//            else {
//                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            }
//            Ringtone r = RingtoneManager.getRingtone(activity.getApplicationContext(), notification);
//            r.play();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private SoundPool soundPool = null;
    private int[] beepSound = {
            R.raw.beep0_animato,                    //  event button pressed
            R.raw.beep1_ddok,                       //  file limit reached
            R.raw.beep2_dungdong,                   //  close app, free storage
            R.raw.beep3_haze,                       //  event merge finished
            R.raw.beep4_recording,                  //  record button pressed
            R.raw.beep5_s_dew_drops,                //  normal merge finished
            R.raw.beep6_stoprecording,              // stop recording
            R.raw.i_will_be_back
            };
    private int[] soundNbr = {0,0,0,0,0,0,0,0,0,0};

    void beepsInitiate() {

        SoundPool.Builder builder;
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        builder = new SoundPool.Builder();
        builder.setAudioAttributes(audioAttributes).setMaxStreams(5);
        soundPool = builder.build();
        for (int i = 0; i < beepSound.length; i++) {
            soundNbr[i] = soundPool.load(mContext, beepSound[i], 1);
        }
    }

    void beepOnce(int soundId,float volume) {

        if (soundPool == null) {
            beepsInitiate();
            final int id = soundId;
            final float vol = volume;
            Handler handler = new Handler();
            handler.postDelayed(() -> beepSound(id, vol), 2000);
        } else {
            beepSound(soundId, volume);
        }
    }
    private void beepSound(int soundId, float volume) {
        soundPool.play(soundNbr[soundId], volume, volume, 1, 0, 1f);
    }

}
