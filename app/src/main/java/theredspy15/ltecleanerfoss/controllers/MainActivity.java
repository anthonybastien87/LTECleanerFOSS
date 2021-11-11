/*
 * Copyright 2020 Hunter J Drum
 */

package theredspy15.ltecleanerfoss.controllers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.text.DecimalFormat;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import theredspy15.ltecleanerfoss.BuildConfig;
import theredspy15.ltecleanerfoss.FileScanner;
import theredspy15.ltecleanerfoss.R;
import theredspy15.ltecleanerfoss.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public static SharedPreferences prefs;

    public ActivityMainBinding binding;

    @SuppressLint("LogConditional")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (prefs == null) updateTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.cleanBtn.setOnClickListener(this::clean);
        binding.settingsBtn.setOnClickListener(this::settings);
        binding.whitelistBtn.setOnClickListener(this::whitelist);
        binding.analyzeBtn.setOnClickListener(this::analyze);

        WhitelistActivity.getWhiteList(prefs);

        loadAdData();
    }

    private void loadAdData() {
        String unitId;
        if (BuildConfig.DEBUG) {
            unitId = "ca-app-pub-3940256099942544/6300978111";
            Toast.makeText(this, "Debug mode active", Toast.LENGTH_SHORT).show();
        } else {
            unitId = "ca-app-pub-5128547878021429/8516214533"; // production only!
        }

        MobileAds.initialize(this, initializationStatus -> { });
        AdRequest adRequest = new AdRequest.Builder().build();
        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(unitId);
        binding.mainLayout.addView(adView);
        adView.loadAd(adRequest);
    }

    /**
     * Starts the settings activity
     * @param view the view that is clickedprefs = getSharedPreferences("Settings",0);
     */
    public final void settings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public final void whitelist(View view) {
        Intent intent = new Intent(this, WhitelistActivity.class);
        startActivity(intent);
    }

    public final void analyze(View view) {
        requestWriteExternalPermission();

        if (!FileScanner.isRunning) {
            new Thread(()-> scan(false)).start();
        }
    }

    private void arrangeViews(boolean isDelete) {
        if (isDelete) arrangeForClean();
        else arrangeForAnalyze();
    }

    private void arrangeForClean() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.frameLayout.setVisibility(View.VISIBLE);
            binding.fileScrollView.setVisibility(View.GONE);
        }
    }

    private void arrangeForAnalyze() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.frameLayout.setVisibility(View.GONE);
            binding.fileScrollView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Runs search and delete on background thread
     */
    public final void clean(View view) {
        requestWriteExternalPermission();

        if (!FileScanner.isRunning) {
            if (prefs.getBoolean("one_click", false)) // one-click disabled
            {
                new Thread(()-> scan(true)).start(); // one-click enabled
            } else {
                MaterialDialog mDialog = new MaterialDialog.Builder(this)
                        .setTitle(getString(R.string.are_you_sure_deletion_title))
                        .setAnimation("5453-shred-paper.json")
                        .setMessage(getString(R.string.are_you_sure_deletion))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.clean), (dialogInterface, which) -> {
                            dialogInterface.dismiss();
                            new Thread(()-> scan(true)).start();
                        })
                        .setNegativeButton(getString(R.string.cancel), (dialogInterface, which) -> dialogInterface.dismiss())
                        .build();
                mDialog.getAnimationView().setScaleType(ImageView.ScaleType.FIT_CENTER);
                mDialog.show();
            }
        }
    }

    private void clearClipboard() {
        try {
            ClipboardManager mCbm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mCbm.clearPrimaryClip();
            } else {
                ClipboardManager clipService = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("", "");
                clipService.setPrimaryClip(clipData);
            }
        } catch (NullPointerException e) {
            runOnUiThread(()->Toast.makeText(this, R.string.clipboard_clear_failed, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Searches entire device, adds all files to a list, then a for each loop filters
     * out files for deletion. Repeats the process as long as it keeps finding files to clean,
     * unless nothing is found to begin with
     */
    @SuppressLint("SetTextI18n")
    private void scan(boolean delete) {
        Looper.prepare();
        runOnUiThread(()-> {
            findViewById(R.id.cleanBtn).setEnabled(!FileScanner.isRunning);
            findViewById(R.id.analyzeBtn).setEnabled(!FileScanner.isRunning);
        });
        reset();

        if (prefs.getBoolean("clipboard",false)) clearClipboard();

        runOnUiThread(()-> {
            arrangeViews(delete);
            binding.statusTextView.setText(getString(R.string.status_running));
        });

        File path = Environment.getExternalStorageDirectory();

        // scanner setup
        FileScanner fs = new FileScanner(path, this)
                .setEmptyDir(prefs.getBoolean("empty", false))
                .setAutoWhite(prefs.getBoolean("auto_white", true))
                .setDelete(delete)
                .setCorpse(prefs.getBoolean("corpse", false))
                .setGUI(binding)
                .setContext(this)
                .setUpFilters(
                        prefs.getBoolean("generic", true),
                        prefs.getBoolean("aggressive", false),
                        prefs.getBoolean("apk", false));

        // failed scan
        if (path.listFiles() == null) { // is this needed? yes.
            TextView textView = printTextView(getString(R.string.failed_scan), Color.RED);
            runOnUiThread(() -> binding.fileListView.addView(textView));
        }

        // kilobytes found/freed text
        long kilobytesTotal = fs.startScan();
        runOnUiThread(() -> {
            if (delete)
                binding.statusTextView.setText(getString(R.string.freed) + " " + convertSize(kilobytesTotal));
            else
                binding.statusTextView.setText(getString(R.string.found) + " " + convertSize(kilobytesTotal));

            // crappy but working fix for percentage never reaching 100 exactly
            binding.scanProgress.setProgress(binding.scanProgress.getMax());
            binding.scanTextView.setText("100%");
        });
        binding.fileScrollView.post(() -> binding.fileScrollView.fullScroll(ScrollView.FOCUS_DOWN));

        runOnUiThread(()-> {
            findViewById(R.id.cleanBtn).setEnabled(!FileScanner.isRunning);
            findViewById(R.id.analyzeBtn).setEnabled(!FileScanner.isRunning);
        });
        Looper.loop();
    }

    /**
     * Convenience method to quickly create a textview
     * @param text - text of textview
     * @return - created textview
     */
    private synchronized TextView printTextView(String text, int color) {
        TextView textView = new TextView(MainActivity.this);
        textView.setTextColor(color);
        textView.setText(text);
        textView.setPadding(3,3,3,3);
        return textView;
    }

    public static String convertSize(long length) {
        final DecimalFormat format = new DecimalFormat("#.##");
        final long MiB = 1024 * 1024;
        final long KiB = 1024;

        if (length > MiB) {
            return format.format(length / MiB) + " MB";
        }
        if (length > KiB) {
            return format.format(length / KiB) + " KB";
        }
        return format.format(length) + " B";
    }

    /**
     * Increments amount removed, then creates a text view to add to the scroll view.
     * If there is any error while deleting, turns text view of path red
     * @param file file to delete
     */
    public synchronized TextView displayDeletion(File file) {
        // creating and adding a text view to the scroll view with path to file
        TextView textView = printTextView(file.getAbsolutePath(), getResources().getColor(R.color.colorAccent));

        // adding to scroll view
        runOnUiThread(() -> binding.fileListView.addView(textView));

        // scroll to bottom
        binding.fileScrollView.post(() -> binding.fileScrollView.fullScroll(ScrollView.FOCUS_DOWN));

        return textView;
    }

    public synchronized TextView displayText(String text) {
        // creating and adding a text view to the scroll view with path to file
        TextView textView = printTextView(text, Color.YELLOW);

        // adding to scroll view
        runOnUiThread(() -> binding.fileListView.addView(textView));

        // scroll to bottom
        binding.fileScrollView.post(() -> binding.fileScrollView.fullScroll(ScrollView.FOCUS_DOWN));

        return textView;
    }


    /**
     * Removes all views present in fileListView (linear view), and sets found and removed
     * files to 0
     */
    private synchronized void reset() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        runOnUiThread(() -> {
            binding.fileListView.removeAllViews();
            binding.scanProgress.setProgress(0);
            binding.scanProgress.setMax(1);
        });
    }

    /**
     * Request write permission
     */
    public synchronized void requestWriteExternalPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // android 11 and up
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.MANAGE_EXTERNAL_STORAGE},
                    1);

            if (!Environment.isExternalStorageManager()) { // all files
                Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }
    }

    /**
     * Handles the whether the user grants permission. Launches new fragment asking the user to give file permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 &&
                grantResults.length > 0 &&
                grantResults[0] != PackageManager.PERMISSION_GRANTED)
            prompt();
    }

    /**
     * Launches the prompt activity
     */
    public final void prompt() {
        Intent intent = new Intent(this, PromptActivity.class);
        startActivity(intent);
    }

    public void updateTheme() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String dark = getResources().getStringArray(R.array.themes)[2];
        final String light = getResources().getStringArray(R.array.themes)[1];
        String selectedTheme = prefs.getString("theme",dark);

        if (dark.equals(selectedTheme)) { // dark
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (light.equals(selectedTheme)) { // light
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } // auto
    }
}
