package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.Vars.ChronoLog;
import static com.urrecliner.blackbox.Vars.DATE_PREFIX;
import static com.urrecliner.blackbox.Vars.FORMAT_DATE;
import static com.urrecliner.blackbox.Vars.FORMAT_TIME;
import static com.urrecliner.blackbox.Vars.bytesEventActive;
import static com.urrecliner.blackbox.Vars.bytesEventStarted;
import static com.urrecliner.blackbox.Vars.bytesRecordOn;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mPackageEventPath;
import static com.urrecliner.blackbox.Vars.mPackageLogPath;
import static com.urrecliner.blackbox.Vars.sdfDate;
import static com.urrecliner.blackbox.Vars.sharedPref;
import static com.urrecliner.blackbox.Vars.vTextLogInfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utils {
    private final String LOG_PREFIX = "log_";
    private final String logDate = getMilliSec2String(System.currentTimeMillis(),FORMAT_DATE);
    private final String logFile = LOG_PREFIX+logDate+".txt";

    public void readyPackageFolder (File dir){
        try {
            if (!dir.exists() && !dir.mkdirs())
                Log.e("make dir", "Error");
        } catch (Exception e) {
            Log.e("creating Folder error", dir + "_" + e);
        }
    }

    public File[] getDirectoryList(File fullPath) {
        return fullPath.listFiles();
    }

    public int getRecordEventCount() {
        File[] files = mPackageEventPath.listFiles(file -> (file.getPath().endsWith("mp4") && file.length() > 100));
        return (files == null)? 0: files.length;
    }

    public void showOnly(String tag, String text) {
        text = vTextLogInfo.getText().toString() + "\n" + getMilliSec2String(System.currentTimeMillis(), "HH:mm ")+tag+": "+text;
        final String fText = last4Lines(text);
        mActivity.runOnUiThread(() -> vTextLogInfo.setText(fText));
    }

    public void logBoth(String tag, String text) {
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text;
        Log.w(tag , log);
        append2file(mPackageLogPath, logFile, getMilliSec2String(System.currentTimeMillis(), FORMAT_TIME)+" "+tag+": " + log);
        text = vTextLogInfo.getText().toString() + "\n" + getMilliSec2String(System.currentTimeMillis(), "HH:mm ")+tag+": "+text;
        final String fText = last4Lines(text);
        mActivity.runOnUiThread(() -> vTextLogInfo.setText(fText));
    }

    public void logShow(String tag, String text) {
        final String fText = last4Lines(vTextLogInfo.getText().toString() + "\n" + getMilliSec2String(System.currentTimeMillis(), "HH:mm ")+tag+": "+text);
        mActivity.runOnUiThread(() -> vTextLogInfo.setText(fText));
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
        append2file(mPackageLogPath, logFile, "<logE Start>\n"+getMilliSec2String(System.currentTimeMillis(), FORMAT_TIME) +  "// " + log+ "\n"+ getStackTrace(e)+"<End>");
        text = last4Lines(vTextLogInfo.getText().toString() + "\n" + text+"\n");
        final String fText = tag+" : "+text;
        mActivity.runOnUiThread(() -> vTextLogInfo.setText(fText));
        append2file(mPackageLogPath, logFile, getMilliSec2String(System.currentTimeMillis(), FORMAT_TIME) +  ": " + log);
        e.printStackTrace();
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

    void customToast  (final String text, final int short_Long, final int foreColor) {

        mActivity.runOnUiThread(() -> {
            Toast toast = Toast.makeText(mContext, text, short_Long);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER, 0,0);
            View toastView = toast.getView();
            TextView toastMessage = toastView.findViewById(android.R.id.message);
            toastMessage.setTextSize(24);
            int backColor = foreColor ^ 0xececec;
            toastMessage.setTextColor(foreColor);
            toastMessage.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.event_shot, 0, 0, 0);
            toastMessage.setCompoundDrawablePadding(16);
            toastMessage.setPadding(4,4,24,4);
            toastView.setBackgroundColor(backColor);
            toast.show();
//                log("customToast",text);
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

    private String last4Lines(String str) {
        String[] lines = str.split("\n");
        StringBuilder result = new StringBuilder();
        int begLine = (lines.length > 4) ? lines.length-4 : 0;
        for (int i = begLine; i < lines.length; ) {
            result.append(lines[i]);
            if (++i< lines.length)
                result.append("\n");
        }
        return result.toString();
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
    private final int[] beepSound = {
            R.raw.beep0_animato,                    //  event button pressed
            R.raw.beep1_ddok,                       //  file limit reached
            R.raw.beep2_dungdong,                   //  close app, free storage
            R.raw.beep3_haze,                       //  event merge finished
            R.raw.beep4_recording,                  //  record button pressed
            R.raw.beep5_s_dew_drops,                //  normal merge finished
            R.raw.beep6_stoprecording,              // stop recording
            R.raw.beep7_i_will_be_back_soon_kr,           // I will back
            R.raw.beep8_exit_application                  // Exit application
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

//    String getExternalStoragePath(Context context) {
//
//        StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
//        Class<?> storageVolumeClazz = null;
//        try {
//            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
//            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
//            Method getPath = storageVolumeClazz.getMethod("getPath");
//            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
//            Object result = getVolumeList.invoke(mStorageManager);
//            final int length = Array.getLength(result);
//            for (int i = 0; i < length; i++) {
//                Object storageVolumeElement = Array.get(result, i);
//                String path = (String) getPath.invoke(storageVolumeElement);
//                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
//                if (removable) {
//                    return path;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//

//    final int DIRECTORY_REQUEST = 101;
//    public void openDirectory(Uri uriToLoad) {
//        // Choose a directory using the system's file picker.
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//
//        // Provide read access to files and sub-directories in the user-selected
//        // directory.
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//        // Optionally, specify a URI for the directory that should be opened in
//        // the system file picker when it loads.
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);
//
//        mActivity.startActivityForResult(intent, DIRECTORY_REQUEST);
//    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode,
//                                 Intent resultData) {
//        if (requestCode == DIRECTORY_REQUEST
//                && resultCode == Activity.RESULT_OK) {
//            // The result data contains a URI for the document or directory that
//            // the user selected.
//            Uri uri = null;
//            if (resultData != null) {
//                uri = resultData.getData();
//                utils.logOnly("uri",uri.toString());
//                // Perform operations on the document using its URI.
//            }
//        }
//    }

    ArrayList<ChronoLog> getTodayTable() {

        ArrayList<ChronoLog> list;
        Gson gson = new Gson();
        String json = sharedPref.getString("chrono", "");
        if (json.isEmpty()) {
            list = new ArrayList<>();
        } else {
            Type type = new TypeToken<List<ChronoLog>>() {
            }.getType();
            list = gson.fromJson(json, type);
        }
        return list;
    }

    void makeEventShotArray() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.event_shot);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        bytesEventStarted = stream.toByteArray();
        bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.i_will_be_back);
        stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        bytesEventActive = stream.toByteArray();
        bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.recording_on);
        stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        bytesRecordOn = stream.toByteArray();
    }

}