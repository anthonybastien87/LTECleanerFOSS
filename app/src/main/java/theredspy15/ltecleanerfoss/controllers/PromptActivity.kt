/*
 * Copyright 2020 Hunter J Drum
 */

package theredspy15.ltecleanerfoss.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import theredspy15.ltecleanerfoss.R;

public class PromptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompt);

        Button button = findViewById(R.id.button1);
        button.setOnClickListener(view -> {
            resultLauncher.launch(new Intent(android.provider.Settings.ACTION_SETTINGS));
            System.exit(0);
        });
    }

    //Instead of onActivityResult() method use this one
    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Here, no request code
                    Intent data = result.getData();
                }
            });
}
