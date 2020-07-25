package com.urrecliner.blackbox;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.content.ContentValues.TAG;
import static com.urrecliner.blackbox.Vars.FORMAT_DATE;
import static com.urrecliner.blackbox.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mPackageLogPath;
import static com.urrecliner.blackbox.Vars.mPackageNormalPath;
import static com.urrecliner.blackbox.Vars.sdfDate;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextLogInfo;


class Utils {
    private final String LOG_PREFIX = "log_";
    private String logDate = getMilliSec2String(System.currentTimeMillis(),FORMAT_DATE);
    private String logFile = LOG_PREFIX+logDate+".txt";

    String getMilliSec2String(long milliSec, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        return dateFormat.format(new Date(milliSec));
    }

    String getNowTimeString(String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        return dateFormat.format(System.currentTimeMillis());
    }

    static class ScreenInfo {
        int width, height;
        int widthPixels, heightPixels;
        int densityDpi;
        float xdpi, ydpi;
//        int screenWidth, screenHeight;
        double screenInch;
        String screenType;
        ScreenInfo() {}
    }

    ScreenInfo getScreenSize(Activity activity) {
        ScreenInfo screenInfo = new ScreenInfo();
        Display display = activity.getWindowManager().getDefaultDisplay();
//        String displayName = display.getName();  // minSdkVersion=17+
//        Log.w("screen", "displayName  = " + displayName);

// display size in pixels
        Point size = new Point();
        display.getSize(size);
        screenInfo.width = size.x;
        screenInfo.height = size.y;
// pixels, dpi
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenInfo.heightPixels = metrics.heightPixels;
        screenInfo.widthPixels = metrics.widthPixels;
        screenInfo.densityDpi = metrics.densityDpi;
        screenInfo.xdpi = metrics.xdpi;
        screenInfo.ydpi = metrics.ydpi;
        screenInfo.screenInch = Math.sqrt(Math.pow(screenInfo.width/screenInfo.xdpi,2)+Math.pow(screenInfo.height/screenInfo.ydpi,2));
//
//// deprecated
//        int screenHeight = display.getHeight();
//        int screenWidth = display.getWidth();
//        Log.w(TAG, "screenHeight = " + screenHeight);
//        Log.w(TAG, "screenWidth  = " + screenWidth);

// orientation (either ORIENTATION_LANDSCAPE, ORIENTATION_PORTRAIT)
//        int orientation = activity.getResources().getConfiguration().orientation;
//        Log.w(TAG, "orientation  = " + orientation);
        int screenSize = activity.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE)
            screenInfo.screenType = "L";
        else if (screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL)
            screenInfo.screenType = "N";
        else if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL)
            screenInfo.screenType = "S";
        else
            screenInfo.screenType = "U";
        return screenInfo;
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
        if (fileOrDirectory.isDirectory()) {
            utils.logOnly("Delete Old Folder ", fileOrDirectory.toString());
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        }
        return fileOrDirectory.delete();
    }

    void deleteOldNormalEvents(File target, int days) {

        String oldDate = sdfDate.format(System.currentTimeMillis() - days*24*60*60*1000L);
        File[] oldFiles = utils.getDirectoryList(target);
        Collator myCollator = Collator.getInstance();
        for (File file : oldFiles) {
            String shortFileName = file.getName();
            if (!shortFileName.substring(0,1).equals(".") && myCollator.compare(shortFileName, oldDate) < 0) {
                deleteRecursive(file);
            }
        }
    }

    void deleteOldLogs(int days) {
        final SimpleDateFormat sdfDate = new SimpleDateFormat(FORMAT_DATE, Locale.US);

        String oldDate = LOG_PREFIX + sdfDate.format(System.currentTimeMillis() - days*24*60*60*1000L);
        File[] files = mPackageLogPath.listFiles();
        Collator myCollator = Collator.getInstance();
        for (File file : files) {
            String shortFileName = file.getName();
            if (myCollator.compare(shortFileName, oldDate) < 0) {
                if (!file.delete())
                    Log.e("file","Delete Error "+file);
            }
        }
    }

    void logBoth(String tag, String text) {
//        int pid = android.os.Process.myPid();
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text;
        Log.w(tag , log);
        append2file(mPackageLogPath, logFile, getMilliSec2String(System.currentTimeMillis(), FORMAT_LOG_TIME)+" "+tag+": " + log);
        text = vTextLogInfo.getText().toString() + "\n" + getMilliSec2String(System.currentTimeMillis(), "HH:mm ")+tag+": "+text;
        text = truncLine(text);
        final String fText = text;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vTextLogInfo.setText(fText);
            }
        });
    }

    void logOnly (String tag, String text) {
//        int pid = android.os.Process.myPid();
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text;
        Log.w(tag , log);
        append2file(mPackageLogPath, logFile, getMilliSec2String(System.currentTimeMillis(), FORMAT_LOG_TIME) +  ": " + log);
    }

    void logE(String tag, String text, Exception e) {
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " [err:"+ tag + "] " + text;
        append2file(mPackageLogPath, logFile, "<logE Start>\n"+getMilliSec2String(System.currentTimeMillis(), FORMAT_LOG_TIME) +  "// " + log+ "\n"+ getStackTrace(e)+"<End>");
        text = vTextLogInfo.getText().toString() + "\n" + text;
        text = truncLine(text);
        final String fText = tag+" : "+text;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vTextLogInfo.setText(fText);
            }
        });
        append2file(mPackageLogPath, logFile, getMilliSec2String(System.currentTimeMillis(), FORMAT_LOG_TIME) +  ": " + log);
        e.printStackTrace();
    }

    String getStackTrace(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        return errors.toString();
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
            bw.write("\n" + text);
        } catch (IOException e) {
            e.printStackTrace();
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
            R.raw.i_will_be_back_soon_kr,           // I will back
            R.raw.exit_application                  // Exit application
            };
    private int[] soundNbr = new int[beepSound.length];

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
            handler.postDelayed(() -> beepSound(id, vol), 500);
        } else {
            beepSound(soundId, volume);
        }
    }
    private void beepSound(int soundId, float volume) {
        soundPool.play(soundNbr[soundId], volume, volume, 1, 0, 1f);
    }

}
