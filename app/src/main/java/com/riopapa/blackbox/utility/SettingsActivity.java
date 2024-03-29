package com.riopapa.blackbox.utility;

import static com.riopapa.blackbox.Vars.share_event_sec;
import static com.riopapa.blackbox.Vars.sharedEditor;
import static com.riopapa.blackbox.Vars.mContext;
import static com.riopapa.blackbox.Vars.share_image_size;
import static com.riopapa.blackbox.Vars.share_left_right_interval;
import static com.riopapa.blackbox.Vars.share_snap_interval;
import static com.riopapa.blackbox.Vars.sharedPref;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.riopapa.blackbox.R;

public class SettingsActivity extends AppCompatActivity  {

    static final String NAME_IMAGES_SIZE = "images_size";
    static final String NAME_SNAP_INTERVAL = "snap_interval";
    static final String NAME_LEFT_RIGHT = "left_right";
    static final String NAME_DURATION = "event_sec";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_activity);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setImageSize();
        setSnapInterval();
        setLeftRight();
        setDuration();
    }

    void setImageSize() {

        TextView tvSize = findViewById(R.id.image_size);
        TextView tvSizeUp = findViewById(R.id.image_size_up);
        TextView tvSizeDown = findViewById(R.id.image_size_down);
        String txt = "" + share_image_size;
        tvSize.setText(txt);
        tvSizeDown.setOnClickListener(v -> {
            share_image_size--;
            String t = "" + share_image_size;
            tvSize.setText(t);
            sharedEditor.putInt(NAME_IMAGES_SIZE, share_image_size).apply();
//            snapBytes = new byte[share_image_size][];
//            snapNowPos = 0;
        });
        tvSizeUp.setOnClickListener(v -> {
            share_image_size++;
            String t = "" + share_image_size;
            tvSize.setText(t);
            sharedEditor.putInt(NAME_IMAGES_SIZE, share_image_size).apply();
//            snapBytes = new byte[share_image_size][];
//            snapNowPos = 0;
        });
    }

    void setSnapInterval() {

        TextView tvSize = findViewById(R.id.snap_interval);
        TextView tvSizeUp = findViewById(R.id.snap_interval_up);
        TextView tvSizeDown = findViewById(R.id.snap_interval_down);
        String txt = "" + share_snap_interval;
        tvSize.setText(txt);
        tvSizeDown.setOnClickListener(v -> {
            share_snap_interval--;
            String t = "" + share_snap_interval;
            tvSize.setText(t);
            sharedEditor.putInt(NAME_SNAP_INTERVAL, (int) share_snap_interval).apply();
        });
        tvSizeUp.setOnClickListener(v -> {
            share_snap_interval++;
            String t = "" + share_snap_interval;
            tvSize.setText(t);
            sharedEditor.putInt(NAME_SNAP_INTERVAL, (int) share_snap_interval).apply();
        });
    }

    void setLeftRight() {

        TextView tvSize = findViewById(R.id.left_right);
        TextView tvSizeUp = findViewById(R.id.left_right_up);
        TextView tvSizeDown = findViewById(R.id.left_right_down);
        String txt = "" + share_left_right_interval;
        tvSize.setText(txt);
        tvSizeDown.setOnClickListener(v -> {
            share_left_right_interval--;
            String t = "" + share_left_right_interval;
            tvSize.setText(t);
            sharedEditor.putInt(NAME_LEFT_RIGHT, (int) share_left_right_interval).apply();
        });
        tvSizeUp.setOnClickListener(v -> {
            share_left_right_interval++;
            String t = "" + share_left_right_interval;
            tvSize.setText(t);
            sharedEditor.putInt(NAME_LEFT_RIGHT, (int) share_left_right_interval).apply();
        });
    }
    void setDuration() {

        TextView tvSize = findViewById(R.id.duration);
        TextView tvSizeUp = findViewById(R.id.duration_up);
        TextView tvSizeDown = findViewById(R.id.duration_down);
        String txt = "" + share_event_sec;
        tvSize.setText(txt);
        tvSizeDown.setOnClickListener(v -> {
            share_event_sec--;
            String t = "" + share_event_sec;
            tvSize.setText(t);
            sharedEditor.putInt(NAME_DURATION, (int) share_event_sec).apply();
        });
        tvSizeUp.setOnClickListener(v -> {
            share_event_sec++;
            String t = "" + share_event_sec;
            tvSize.setText(t);
            sharedEditor.putInt(NAME_DURATION, (int) share_event_sec).apply();
        });
    }

    public static void getPreference() {

        sharedPref = mContext.getSharedPreferences("blackbox", MODE_PRIVATE);
        sharedEditor = sharedPref.edit();
//        sharedPref = PreferenceManager.getDefaultSharedPreferences(Vars.mContext);
        share_image_size = sharedPref.getInt(NAME_IMAGES_SIZE, 0);
        if (share_image_size == 0) {
            share_image_size = 116;
            sharedEditor.putInt(NAME_IMAGES_SIZE, 126);
            sharedEditor.putInt(NAME_SNAP_INTERVAL, 187);
            sharedEditor.putInt(NAME_LEFT_RIGHT, 100);
            sharedEditor.putInt(NAME_DURATION, 100);
            sharedEditor.apply();
        }
        share_image_size = sharedPref.getInt(NAME_IMAGES_SIZE, 197);
        share_snap_interval = sharedPref.getInt(NAME_SNAP_INTERVAL, 197);
        share_left_right_interval = sharedPref.getInt(NAME_LEFT_RIGHT, 100);
        share_event_sec = sharedPref.getInt(NAME_DURATION, 22);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_OK);
    }
}