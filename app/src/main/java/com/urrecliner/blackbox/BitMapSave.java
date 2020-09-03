package com.urrecliner.blackbox;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.urrecliner.blackbox.Vars.utils;

class BitMapSave {

    static File imageFile;
    static byte [] imageBytes;

    void save(byte[] images, File fullName) {
        imageFile = fullName;
        imageBytes = images;
        new SaveTask().execute();
    }

    static class SaveTask extends AsyncTask<String, String, String> {

//        @Override
//        protected void onPreExecute() {
//        }

        @Override
        protected String doInBackground(String... inputParams) {

//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            BitmapFactory.decodeByteArray( imageBytes, 0, imageBytes.length)
//                    .compress(Bitmap.CompressFormat.JPEG, 90, stream);
//                    .copy(Bitmap.Config.RGB_565, false)
            Log.w("id",imageFile.getName());
//            bytes2File(imageBytes, imageFile);
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) { }

        @Override
        protected void onCancelled(String result) { }

        @Override
        protected void onPostExecute(String doI) { }
    }

}
