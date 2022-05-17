package com.urrecliner.blackbox.utility;

import static com.urrecliner.blackbox.Vars.INTERVAL_LEFT_RIGHT;
import static com.urrecliner.blackbox.Vars.INTERVAL_SNAP_SHOT_SAVE;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.USE_CUSTOM_VALUES;
import static com.urrecliner.blackbox.Vars.sharedPref;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.urrecliner.blackbox.R;
import com.urrecliner.blackbox.Vars;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity  {

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
            prefMaxImageSize.setSummary(String.format(Locale.getDefault(),fmtMaxImageSize, MAX_IMAGES_SIZE));
            prefMaxImageSize.setOnPreferenceChangeListener((preference, newValue) -> {
                MAX_IMAGES_SIZE = Integer.parseInt(newValue.toString());
                prefMaxImageSize.setSummary(String.format(Locale.getDefault(),fmtMaxImageSize, MAX_IMAGES_SIZE));
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
            final String fmtIntervalSnapShotSave = "사진을 찍는 간격은 %d milli sec 입니다";
            prefIntervalSnapShotSave = findPreference(NAME_INTERVAL_SNAP_SHOT_SAVE);
            final String strSS = String.format(Locale.getDefault(),fmtIntervalSnapShotSave, INTERVAL_SNAP_SHOT_SAVE);
            prefIntervalSnapShotSave.setSummary(strSS);
            prefIntervalSnapShotSave.setOnPreferenceChangeListener((preference, newValue) -> {
                INTERVAL_SNAP_SHOT_SAVE = Long.parseLong(newValue.toString());
                prefIntervalSnapShotSave.setSummary(strSS);
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
            prefIntervalLeftRight = findPreference(NAME_INTERVAL_LEFT_RIGHT);
            final String strLR = String.format(Locale.getDefault(),fmtIntervalSnapShotSave, INTERVAL_LEFT_RIGHT);
            prefIntervalLeftRight.setSummary(strLR);
            prefIntervalLeftRight.setOnPreferenceChangeListener((preference, newValue) -> {
                INTERVAL_LEFT_RIGHT = Integer.parseInt(newValue.toString());
                prefIntervalLeftRight.setSummary(strLR);
                return true;
            });
        }
    }

    public static void getPreference() {
        final String NAME_USE_CUSTOM_VALUES = "use_custom_values";
        sharedPref = PreferenceManager.getDefaultSharedPreferences(Vars.mContext);
        USE_CUSTOM_VALUES = sharedPref.getBoolean(NAME_USE_CUSTOM_VALUES, false);
        MAX_IMAGES_SIZE = Integer.parseInt(sharedPref.getString(NAME_MAX_IMAGES_SIZE, "116"));
        if (MAX_IMAGES_SIZE == 0) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(NAME_USE_CUSTOM_VALUES, true);
            editor.putString(NAME_MAX_IMAGES_SIZE, "116");
            editor.putString(NAME_INTERVAL_SNAP_SHOT_SAVE, "197");
            editor.putString(NAME_INTERVAL_LEFT_RIGHT, "100");
            editor.apply();
        }
        if (USE_CUSTOM_VALUES) {
            MAX_IMAGES_SIZE = Integer.parseInt(sharedPref.getString("max_images_size", "116"));
            INTERVAL_SNAP_SHOT_SAVE = Long.parseLong(sharedPref.getString(NAME_INTERVAL_SNAP_SHOT_SAVE, "185"));
            INTERVAL_LEFT_RIGHT = Long.parseLong(sharedPref.getString(NAME_INTERVAL_LEFT_RIGHT, "100"));
        }
    }

    @Override
    protected void onDestroy() {
        getPreference();
        super.onDestroy();
    }

}