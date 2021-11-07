/*
 * Copyright 2021 Hunter J Drum
 */

package theredspy15.ltecleanerfoss.controllers;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import theredspy15.ltecleanerfoss.R;
import theredspy15.ltecleanerfoss.databinding.ActivityWhitelistBinding;

public class WhitelistActivity extends AppCompatActivity {

    private static List<String> whiteList;

    ActivityWhitelistBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist);

        binding = ActivityWhitelistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.newButton.setOnClickListener(this::addToWhiteList);

        getWhiteList(MainActivity.prefs);
        loadViews();
    }

    void loadViews() {
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layout.setMargins(0, 20, 0, 20);

        if (whiteList != null) {
            for (String path : whiteList) {
                Button button = new Button(this);
                button.setText(path);
                button.setTextColor(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));
                button.setTextSize(18);
                button.setAllCaps(false);
                button.setOnClickListener(v -> removePath(path, button));
                button.setPadding(50,50,50,50);
                layout.setMargins(0,20,0,20);
                button.setBackgroundResource(R.drawable.rounded_view);
                GradientDrawable drawable = (GradientDrawable) button.getBackground();
                drawable.setColor(Color.GRAY);
                drawable.setAlpha(30);
                runOnUiThread(()->binding.pathsLayout.addView(button,layout));
            }
        } else if (whiteList == null || whiteList.isEmpty()) {
            TextView textView = new TextView(this); // no news feeds selected
            textView.setText(R.string.empty_whitelist);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setTextSize(18);
            runOnUiThread(() -> binding.pathsLayout.addView(textView, layout));
        }
    }

    void removePath(String path, Button button) {
        MaterialDialog mDialog = new MaterialDialog.Builder(this)
                .setTitle("Remove from whitelist?")
                .setMessage(path)
                .setCancelable(false)
                .setPositiveButton("Delete", (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                    whiteList.remove(path);
                    binding.pathsLayout.removeView(button);
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, which) -> dialogInterface.dismiss())
                .build();
        mDialog.show();
    }

    /**
     * Clears the whitelist, then sets it up again without loading saved one from stash
     * @param view the view that is clicked
     */
    public final void emptyWhitelist(View view) {

        new AlertDialog.Builder(WhitelistActivity.this,R.style.MyAlertDialogTheme)
                .setTitle(R.string.reset_whitelist)
                .setMessage(R.string.are_you_reset_whitelist)
                .setPositiveButton(R.string.reset, (dialog, whichButton) -> {
                    whiteList.clear();
                    MainActivity.prefs.edit().putStringSet("whitelist", new HashSet<>(whiteList)).apply();
                })
                .setNegativeButton(R.string.cancel, (dialog, whichButton) -> { }).show();
    }

    public void addRecommended(View view) {
        String externalDir = this.getExternalFilesDir(null).getPath();
        externalDir = externalDir.substring(0, externalDir.indexOf("Android"));

        if (!whiteList.contains(new File(externalDir, "Music").getPath())) {
            whiteList.add(new File(externalDir, "Music").getPath());
            whiteList.add(new File(externalDir, "Podcasts").getPath());
            whiteList.add(new File(externalDir, "Ringtones").getPath());
            whiteList.add(new File(externalDir, "Alarms").getPath());
            whiteList.add(new File(externalDir, "Notifications").getPath());
            whiteList.add(new File(externalDir, "Pictures").getPath());
            whiteList.add(new File(externalDir, "Movies").getPath());
            whiteList.add(new File(externalDir, "Download").getPath());
            whiteList.add(new File(externalDir, "DCIM").getPath());
            whiteList.add(new File(externalDir, "Documents").getPath());
            MainActivity.prefs.edit().putStringSet("whitelist", new HashSet<>(whiteList)).apply();
        } else
            Toast.makeText(this, "Already added",
                    Toast.LENGTH_LONG).show();
    }

    /**
     * Creates a dialog asking for a file/folder name to add to the whitelist
     * @param view the view that is clicked
     */
    public final void addToWhiteList(View view) {
        mGetContent.launch(Uri.fromFile(Environment.getDataDirectory()));
    }

    ActivityResultLauncher<Uri> mGetContent = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    Toast.makeText(this, ""+uri.getPath(), Toast.LENGTH_LONG).show();
                    whiteList.add(uri.getPath().substring(uri.getPath().indexOf(":")+1)); // TODO create file from uri, then just add its path once sd card support is finished
                    MainActivity.prefs.edit().putStringSet("whitelist", new HashSet<>(whiteList)).apply();
                }
            });

    public static synchronized List<String> getWhiteList(SharedPreferences prefs) {
        if (whiteList == null) {
            whiteList = new ArrayList<>(prefs.getStringSet("whitelist",new HashSet<>()));
            whiteList.remove("[");
            whiteList.remove("]");
        }
        return whiteList;
    }
}
