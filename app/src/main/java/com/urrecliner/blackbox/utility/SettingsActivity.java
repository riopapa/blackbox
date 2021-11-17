package com.urrecliner.blackbox.utility;

import static com.urrecliner.blackbox.Vars.INTERVAL_LEFT_RIGHT;
import static com.urrecliner.blackbox.Vars.INTERVAL_SNAP_SHOT_SAVE;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.USE_CUSTOM_VALUES;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.sharedPref;
import static com.urrecliner.blackbox.Vars.utils;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.urrecliner.blackbox.BeBackSoon;
import com.urrecliner.blackbox.R;
import com.urrecliner.blackbox.Utils;
import com.urrecliner.blackbox.Vars;

import java.util.Timer;
import java.util.TimerTask;

public class SettingsActivity extends AppCompatActivity  {

    static final String NAME_USE_CUSTOM_VALUES = "use_custom_values";
    static final String NAME_MAX_IMAGES_SIZE = "max_images_size";
    static final String NAME_INTERVAL_SNAP_SHOT_SAVE = "interval_snap_shot_save";
    static final String NAME_INTERVAL_LEFT_RIGHT = "interval_left_right";
    static Preference prefMaxImageSize, prefIntervalSnapShotSave, prefIntervalLeftRight;
    static ListPreference listPreferenceCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.set_preferences, rootKey);

            getPreference();

            listPreferenceCategory = (ListPreference) findPreference(NAME_MAX_IMAGES_SIZE);
            if (listPreferenceCategory != null) {
                CharSequence[] entries = new String[20];
                for (int i = 0; i < 20; i++) {
                    entries[i] = ""+(MAX_IMAGES_SIZE + i - 10);
                }
                listPreferenceCategory.setEntries(entries);
                listPreferenceCategory.setEntryValues(entries);
            }
            final String fmtMaxImageSize = "테이블의 길이는 %d 입니다";
            prefMaxImageSize = findPreference(NAME_MAX_IMAGES_SIZE);
            prefMaxImageSize.setSummary(String.format(fmtMaxImageSize, MAX_IMAGES_SIZE));
            prefMaxImageSize.setOnPreferenceChangeListener((preference, newValue) -> {
                MAX_IMAGES_SIZE = Integer.parseInt(newValue.toString());
                prefMaxImageSize.setSummary(String.format(fmtMaxImageSize, MAX_IMAGES_SIZE));
                return true;
            });

            listPreferenceCategory = (ListPreference) findPreference(NAME_INTERVAL_SNAP_SHOT_SAVE);
            if (listPreferenceCategory != null) {
                CharSequence[] entries = new String[40];
                for (int i = 0; i < 40; i++) {
                    entries[i] = ""+(INTERVAL_SNAP_SHOT_SAVE + (i - 20)* 3L);
                }
                listPreferenceCategory.setEntries(entries);
                listPreferenceCategory.setEntryValues(entries);
            }
            final String fmtIntervalSnapShotSave = "사진을 찍는 간격은 %d milisec 입니다";
            prefIntervalSnapShotSave = findPreference(NAME_INTERVAL_SNAP_SHOT_SAVE);
            prefIntervalSnapShotSave.setSummary(String.format(fmtIntervalSnapShotSave, INTERVAL_SNAP_SHOT_SAVE));
            prefIntervalSnapShotSave.setOnPreferenceChangeListener((preference, newValue) -> {
                INTERVAL_SNAP_SHOT_SAVE = Long.parseLong(newValue.toString());
                prefIntervalSnapShotSave.setSummary(String.format(fmtIntervalSnapShotSave, INTERVAL_SNAP_SHOT_SAVE));
                return true;
            });

            listPreferenceCategory = (ListPreference) findPreference(NAME_INTERVAL_LEFT_RIGHT);
            if (listPreferenceCategory != null) {
                CharSequence[] entries = new String[40];
                for (int i = 0; i < 40; i++) {
                    entries[i] = ""+(INTERVAL_LEFT_RIGHT + (i - 20)* 3L);
                }
                listPreferenceCategory.setEntries(entries);
                listPreferenceCategory.setEntryValues(entries);
            }
            final String fmtIntervalLeftRight = "좌 우 변경 간격은 %d milisec 입니다";
            prefIntervalLeftRight = findPreference(NAME_INTERVAL_LEFT_RIGHT);
            prefIntervalLeftRight.setSummary(String.format(fmtIntervalSnapShotSave, INTERVAL_LEFT_RIGHT));
            prefIntervalLeftRight.setOnPreferenceChangeListener((preference, newValue) -> {
                INTERVAL_LEFT_RIGHT = Integer.parseInt(newValue.toString());
                prefIntervalLeftRight.setSummary(String.format(fmtIntervalSnapShotSave, INTERVAL_LEFT_RIGHT));
                return true;
            });

        }
    }

    public static void getPreference() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(Vars.mContext);
        USE_CUSTOM_VALUES = sharedPref.getBoolean(NAME_USE_CUSTOM_VALUES, false);
        MAX_IMAGES_SIZE = Integer.parseInt(sharedPref.getString(NAME_MAX_IMAGES_SIZE, "0"));
        if (MAX_IMAGES_SIZE == 0) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(NAME_USE_CUSTOM_VALUES, true);
            editor.putString(NAME_MAX_IMAGES_SIZE, ""+MAX_IMAGES_SIZE);
            editor.putString(NAME_INTERVAL_SNAP_SHOT_SAVE, ""+INTERVAL_SNAP_SHOT_SAVE);
            editor.putString(NAME_INTERVAL_LEFT_RIGHT, ""+INTERVAL_LEFT_RIGHT);
            editor.apply();
        }
        if (USE_CUSTOM_VALUES) {
            MAX_IMAGES_SIZE = Integer.parseInt(sharedPref.getString("max_images_size", "0"));
            INTERVAL_SNAP_SHOT_SAVE = Long.parseLong(sharedPref.getString(NAME_INTERVAL_SNAP_SHOT_SAVE, "200"));
            INTERVAL_LEFT_RIGHT = Long.parseLong(sharedPref.getString(NAME_INTERVAL_LEFT_RIGHT, "160"));
        }
        String s = "Pref Values\nUSE_CUSTOM_VALUES="+USE_CUSTOM_VALUES+"\nMAX_IMAGES_SIZE="+MAX_IMAGES_SIZE+"\nINTERVAL_SNAP_SHOT_SAVE="+INTERVAL_SNAP_SHOT_SAVE+"\nINTERVAL_LEFT_RIGHT="+INTERVAL_LEFT_RIGHT;
        Toast.makeText(mContext, s, Toast.LENGTH_LONG).show();
        utils.logOnly("PREFERENCE",s);
    }

    @Override
    protected void onDestroy() {
//        getPreference();
        super.onDestroy();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                new BeBackSoon().execute("n");
            }
        }, 100);

    }

}