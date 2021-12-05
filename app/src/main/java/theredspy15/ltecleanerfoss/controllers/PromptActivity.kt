/*
 * Copyright 2020 Hunter J Drum
 */
package theredspy15.ltecleanerfoss.controllers

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import theredspy15.ltecleanerfoss.R
import kotlin.system.exitProcess

class PromptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prompt)

        FirebaseCrashlytics.getInstance().log("Displaying prompt activity")

        val button = findViewById<Button>(R.id.button1)
        button.setOnClickListener {
            resultLauncher.launch(Intent(Settings.ACTION_SETTINGS))
            exitProcess(0)
        }
    }

    //Instead of onActivityResult() method use this one
    private var resultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            // Here, no request code
            val data = result.data
        }
    }
}