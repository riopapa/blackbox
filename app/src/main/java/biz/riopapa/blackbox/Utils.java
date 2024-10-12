package biz.riopapa.blackbox;

import static biz.riopapa.blackbox.Vars.DATE_PREFIX;
import static biz.riopapa.blackbox.Vars.FORMAT_DATE;
import static biz.riopapa.blackbox.Vars.FORMAT_TIME;
import static biz.riopapa.blackbox.Vars.shot_02;
import static biz.riopapa.blackbox.Vars.shot_00;
import static biz.riopapa.blackbox.Vars.shot_01;
import static biz.riopapa.blackbox.Vars.mActivity;
import static biz.riopapa.blackbox.Vars.mContext;
import static biz.riopapa.blackbox.Vars.mPackageEventPath;
import static biz.riopapa.blackbox.Vars.mPackageLogPath;
import static biz.riopapa.blackbox.Vars.sdfDate;
import static biz.riopapa.blackbox.Vars.shot_03;
import static biz.riopapa.blackbox.Vars.vTextLogInfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Utils {
    private final String LOG_PREFIX = "log_";
    private final String logDate = getMilliSec2String(System.currentTimeMillis(),FORMAT_DATE);
    private final String logFile = LOG_PREFIX+logDate+".txt";
    private String uText;

    public void readyPackageFolder (File dir){
        try {
            if (!dir.exists() && !dir.mkdirs())
                Log.e("make dir", "Error");
        } catch (Exception e) {
            Log.e("creating Folder error", dir + "_" + e);
        }
    }
    void setFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {   // >= android 11
//        getSupportActionBar().hide();       // let Full Screen
            WindowInsetsController controller = mActivity.getWindow().getInsetsController();
            if (controller != null) {
//                controller.hide(WindowInsets.Type.statusBars() |
//                        WindowInsets.Type.navigationBars());
                controller.hide(WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            mActivity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

//                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

    public File[] getDirectoryList(File fullPath) {
        return fullPath.listFiles();
    }

    public int getRecordEventCount() {
        File[] files = mPackageEventPath.listFiles(file -> (file.getPath().endsWith("mp4") && file.length() > 100));
        return (files == null)? 0: files.length;
    }

    public void showOnly(String tag, String text) {
        uText = lastNLines(vTextLogInfo.getText().toString() + "\n"+ getMilliSec2String(System.currentTimeMillis(), "HH:mm ")+tag+": "+text);
        mActivity.runOnUiThread(() -> vTextLogInfo.setText(uText));
    }

    public void logBoth(String tag, String text) {
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text;
        Log.w(tag , log);
        append2file(mPackageLogPath, logFile, getMilliSec2String(System.currentTimeMillis(), FORMAT_TIME)+" "+tag+": " + log);
        uText = lastNLines(vTextLogInfo.getText().toString() + "\n"+ getMilliSec2String(System.currentTimeMillis(), "HH:mm ")+tag+": "+text);
        mActivity.runOnUiThread(() -> vTextLogInfo.setText(uText));
    }

    public void logOnly (String tag, String text) {
//        int pid = android.os.Process.myPid();
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text;
        Log.w(tag , log);
        append2file(mPackageLogPath, logFile, getMilliSec2String(System.currentTimeMillis(), FORMAT_TIME) +  ": " + log);
    }

    public void logE(String tag, String text, Exception e) {
        beepOnce(0, .7f);
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " [err:"+ tag + "] " + text;
        append2file(mPackageLogPath, logFile, "\n\n<logE ------------------- Start>\n"+getMilliSec2String(System.currentTimeMillis(), FORMAT_TIME) +  "// " + log+ "\n"+ getStackTrace(e)+"<End ---------- >\n\n");
        uText = tag+" : "+lastNLines(vTextLogInfo.getText().toString() + text);
        mActivity.runOnUiThread(() -> vTextLogInfo.setText(uText));
        Log.e("Error",uText);
//        beepOnce(1, .7f);
    }

    String getStackTrace(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    static private final String []omits = { "performResume", "performCreate", "dispatch",
            "callActivityOnResume", "access$", "handleReceiver", "handleMessage",
            "dispatchKeyEvent", "moveToState", "mainLoop"};

    private String traceName (String s) {
        for (String o : omits) {
            if (s.contains(o)) return "";
        }
        return s + "> ";
    }

    private String traceClassName(String s) {
        return s.substring(s.lastIndexOf(".")+1);
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
                String s = directory.toString() + filename + " close~" + e;
                Log.e("appendIOExcept2",  s);
            }
        }
    }
//
//    void write2file (File directory, String filename, String text) {
//        final File file = new File(directory, filename);
//        try
//        {
//            file.createNewFile();
//            FileOutputStream fOut = new FileOutputStream(file);
//            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
//            myOutWriter.write(text);
//            myOutWriter.close();
//            fOut.flush();
//            fOut.close();
//        }
//        catch (IOException e) {
//            String s = file.toString() + " Err:" + e.toString();
//            e.printStackTrace();
//        }
//    }

    String getMilliSec2String(long milliSec, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        return dateFormat.format(new Date(milliSec));
    }

    void deleteOldFiles(File target, int days) {

        String oldDate = DATE_PREFIX+sdfDate.format(System.currentTimeMillis() - days*24*60*60*1000L);
        File[] oldFiles = getDirectoryList(target);
        if (oldFiles == null)
            return;
        Collator myCollator = Collator.getInstance();
        for (File file : oldFiles) {
            String shortFileName = file.getName();
            if (shortFileName.charAt(0) != '.' && myCollator.compare(shortFileName, oldDate) < 0) {
                deleteFolder(file);
            }
        }
    }

    void deleteFolder(File file) {
        String deleteCmd = "rm -r " + file.toString();
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(deleteCmd);
        } catch (IOException ignored) { }
    }

//    void deleteRecursive(File fileOrDirectory) {
//        if (fileOrDirectory.isDirectory()) {
////            logOnly("Delete Old Folder ", fileOrDirectory.toString());
//            for (File child : fileOrDirectory.listFiles())
//                deleteRecursive(child);
//        }
//        fileOrDirectory.delete();
//    }

    void deleteOldLogs() {
        final int days = 10;
        final SimpleDateFormat sdfDate = new SimpleDateFormat(FORMAT_DATE, Locale.US);

        String oldDate = LOG_PREFIX + sdfDate.format(System.currentTimeMillis() - days*24*60*60*1000L);
        File[] files = mPackageLogPath.listFiles();
        if (files == null)
            return;
        Collator myCollator = Collator.getInstance();
        for (File file : files) {
            String shortFileName = file.getName();
            if (myCollator.compare(shortFileName, oldDate) < 0) {
                if (!file.delete())
                    Log.e("file", "Delete Error " + file);
            }
        }
    }

    void customToast(final String text) {

        mActivity.runOnUiThread(() -> {

            LayoutInflater inflater = LayoutInflater.from(mContext);
            View layout = inflater.inflate(android.R.layout.simple_list_item_1, null); // Using a built-in layout, can replace with a custom one if needed

            // Get the TextView and set its properties
            TextView textV = layout.findViewById(android.R.id.text1);
            textV.setText(text);
            textV.setTextSize(24); // Set text size to 32sp
            textV.setBackgroundColor(Color.YELLOW); // Set background color to red
            textV.setTextColor(Color.RED); // Set text color to white for visibility
            textV.setPadding(15, 16, 16, 16); // Set padding for the text

            // Create the toast object
            Toast toast = new Toast(mContext);
            toast.setDuration(Toast.LENGTH_LONG); // Set the duration for the toast
            toast.setView(layout); // Set the custom layout
            toast.setGravity(Gravity.CENTER, 0, 0); // Show toast in the center

            // Show the toast
            toast.show();
        });
    }

    void displayCount(String text) {

        Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER, 0,0);
        View toastView = toast.getView();

        /* And now you can get the TextView of the default View of the Toast. */
        TextView tm = toastView.findViewById(android.R.id.message);
        tm.setTextSize(48);
        tm.setGravity(Gravity.CENTER);
        tm.setMaxWidth(2000);
        tm.setWidth(2000);

        tm.setTextColor(Color.WHITE);
//        toastMessage.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_launcher, 0, 0, 0);
        tm.setCompoundDrawablePadding(16);
        tm.setPadding(4,4,4,4);
        toastView.setBackgroundColor(Color.DKGRAY);
        toast.show();
    }

    private String lastNLines(String str) {
        String[] lines = str.split("\n");
        if (lines.length > 5)
            return IntStream.range(lines.length - 5, lines.length).mapToObj(i -> "\n"+lines[i]).collect(Collectors.joining());
        else
            return str;
    }

    private SoundPool soundPool = null;
    private final int[] beepSound = {
            R.raw.beep0_animato,                    //  event button pressed
            R.raw.beep1_ddok,                       //  file limit reached
            R.raw.beep2_dungdong,                   //  close app, free storage
            R.raw.beep3_haze,                       //  event merge finished
            R.raw.beep4_recording,                  //  record button pressed
            R.raw.beep5_s_dew_drops,                //  normal merge finished
            R.raw.beep6_stoprecording,              // stop recording
            R.raw.beep7_i_will_be_back_soon_kr,     // I will back
            R.raw.beep8_exit_application,           // Exit application
            R.raw.beep9_so_hot,                     // over 43 degree
            R.raw.beepa_so_hot_hot                  // over 45 degree
            };
    private final int[] soundNbr = new int[beepSound.length];

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

    void setVolume(int percent) {
        AudioManager mAudio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int volume = mAudio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * percent / 100;
        mAudio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND);
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

    void makeEventShotArray() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.shot_00);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        shot_00 = stream.toByteArray();

        bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.shot_01);
        stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        shot_01 = stream.toByteArray();

        bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.shot_02);
        stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        shot_02 = stream.toByteArray();

    }

}