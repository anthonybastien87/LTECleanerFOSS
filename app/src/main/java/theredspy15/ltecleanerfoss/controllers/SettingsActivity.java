/*
 * Copyright 2021 Hunter J Drum
 */

package theredspy15.ltecleanerfoss.controllers;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import theredspy15.ltecleanerfoss.R;
import theredspy15.ltecleanerfoss.workers.CleanWorker;

public class SettingsActivity extends AppCompatActivity {

    private static final int PERIOD=900000; // 15 minutes
    private static final int INITIAL_DELAY=5000; // 5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager().beginTransaction().replace(R.id.layout, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.setHasOptionsMenu(true);

            findPreference("aggressive").setOnPreferenceChangeListener((preference, newValue) -> {
                boolean checked = ((CheckBoxPreference) preference).isChecked();
                if (!checked) {
                    String[] filtersFiles = getResources().getStringArray(R.array.aggressive_filter_folders);

                    AlertDialog alertDialog = new AlertDialog.Builder(requireContext(),R.style.MyAlertDialogTheme).create();
                    alertDialog.setTitle(getString(R.string.aggressive_filter_what_title));
                    alertDialog.setMessage(getString(R.string.adds_the_following)+" "+ Arrays.toString(filtersFiles));
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            (dialog, which) -> dialog.dismiss());
                    alertDialog.show();
                }

                return true;
            });

            findPreference("dailyclean").setOnPreferenceChangeListener((preference, newValue) -> {
                boolean checked = ((CheckBoxPreference) preference).isChecked();
                if (!checked) {
                    PeriodicWorkRequest.Builder builder = new PeriodicWorkRequest.Builder(CleanWorker.class, 24, TimeUnit.HOURS);
                    PeriodicWorkRequest periodicWorkRequest = builder
                            .build();
                    WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork("Cleaner Worker",  ExistingPeriodicWorkPolicy.KEEP,periodicWorkRequest);

                    CleanReceiver.scheduleAlarms(requireContext());

                    Toast.makeText(requireContext(), "Scheduled", Toast.LENGTH_LONG)
                            .show();
                }

                return true;
            });
        }

        /**
         * Inflate Preferences
         */
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}
