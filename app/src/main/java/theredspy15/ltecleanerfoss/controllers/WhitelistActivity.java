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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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
        binding.pathsLayout.removeAllViews();
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layout.setMargins(0, 20, 0, 20);

        if (whiteList != null) {
            for (String path : whiteList) {
                Button button = new Button(this);
                button.setText(path);
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
        } if (whiteList == null || whiteList.isEmpty()) {
            TextView textView = new TextView(this); // no news feeds selected
            textView.setText(R.string.empty_whitelist);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setTextSize(18);
            runOnUiThread(() -> binding.pathsLayout.addView(textView, layout));
        }
    }

    void removePath(String path, Button button) {
        MaterialDialog mDialog = new MaterialDialog.Builder(this)
                .setTitle(getString(R.string.remove_from_whitelist))
                .setMessage(path)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.delete), (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                    whiteList.remove(path);
                    MainActivity.prefs.edit().putStringSet("whitelist", new HashSet<>(whiteList)).apply();
                    binding.pathsLayout.removeView(button);
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, which) -> dialogInterface.dismiss())
                .build();
        mDialog.show();
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
                    whiteList.add(uri.getPath().substring(uri.getPath().indexOf(":")+1)); // TODO create file from uri, then just add its path once sd card support is finished
                    MainActivity.prefs.edit().putStringSet("whitelist", new HashSet<>(whiteList)).apply();
                    loadViews();
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
