/*
 * Copyright 2020 Hunter J Drum
 */
package theredspy15.ltecleanerfoss.controllers

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.installations.FirebaseInstallations
import dev.shreyaspatil.MaterialDialog.MaterialDialog
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface
import theredspy15.ltecleanerfoss.BuildConfig
import theredspy15.ltecleanerfoss.FileScanner
import theredspy15.ltecleanerfoss.R
import theredspy15.ltecleanerfoss.databinding.ActivityMainBinding
import java.io.File
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {
    var binding: ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        if (prefs == null) updateTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        binding!!.cleanBtn.setOnClickListener { clean() }
        binding!!.settingsBtn.setOnClickListener { settings() }
        binding!!.whitelistBtn.setOnClickListener { whitelist() }
        binding!!.analyzeBtn.setOnClickListener { analyze() }
        WhitelistActivity.getWhiteList(prefs)
        loadAdData()

        // crashlytics
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey("mode", BuildConfig.BUILD_TYPE)
        crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
        crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)
        FirebaseInstallations.getInstance().id
            .addOnCompleteListener { task: Task<String?> ->
                if (task.isSuccessful) {
                    FirebaseCrashlytics.getInstance().setUserId(task.result!!)
                    FirebaseAnalytics.getInstance(this).setUserId(task.result)
                } else {
                    FirebaseCrashlytics.getInstance().log("Failed to access FID")
                }
            }
    }

    private fun loadAdData() {
        FirebaseCrashlytics.getInstance().log("Loading advertisement")

        val unitId: String
        if (BuildConfig.DEBUG) {
            unitId = "ca-app-pub-3940256099942544/6300978111"
            Toast.makeText(this, "Debug mode active", Toast.LENGTH_SHORT).show()
        } else {
            unitId = "ca-app-pub-5128547878021429/8516214533" // production only!
        }
        MobileAds.initialize(this) { }
        val adRequest = AdRequest.Builder().build()
        val adView = AdView(this)
        adView.adSize = AdSize.BANNER
        adView.adUnitId = unitId
        binding!!.mainLayout.addView(adView)
        adView.loadAd(adRequest)
    }

    /**
     * Starts the settings activity
     */
    fun settings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun whitelist() {
        val intent = Intent(this, WhitelistActivity::class.java)
        startActivity(intent)
    }

    fun analyze() {
        requestWriteExternalPermission()
        if (!FileScanner.isRunning) {
            Thread { scan(false) }.start()
        }
    }

    private fun arrangeViews(isDelete: Boolean) {
        if (isDelete) arrangeForClean() else arrangeForAnalyze()
    }

    private fun arrangeForClean() {
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            FirebaseCrashlytics.getInstance().log("Arranging view for clean")
            binding!!.frameLayout.visibility = View.VISIBLE
            binding!!.fileScrollView.visibility = View.GONE
        }
    }

    private fun arrangeForAnalyze() {
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            FirebaseCrashlytics.getInstance().log("Arranging view for Analyze")
            binding!!.frameLayout.visibility = View.GONE
            binding!!.fileScrollView.visibility = View.VISIBLE
        }
    }

    /**
     * Runs search and delete on background thread
     */
    fun clean() {
        requestWriteExternalPermission()
        if (!FileScanner.isRunning) {
            if (prefs!!.getBoolean("one_click", false)) // one-click disabled
            {
                Thread { scan(true) }.start() // one-click enabled
            } else {
                val mDialog = MaterialDialog.Builder(this)
                    .setTitle(getString(R.string.are_you_sure_deletion_title))
                    .setAnimation("5453-shred-paper.json")
                    .setMessage(getString(R.string.are_you_sure_deletion))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.clean)) { dialogInterface: DialogInterface, _: Int ->
                        dialogInterface.dismiss()
                        Thread { scan(true) }.start()
                    }
                    .setNegativeButton(getString(R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                    .build()
                mDialog.animationView.scaleType = ImageView.ScaleType.FIT_CENTER
                mDialog.show()
            }
        }
    }

    private fun clearClipboard() {
        FirebaseCrashlytics.getInstance().log("Clearing clipboard")

        try {
            val mCbm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mCbm.clearPrimaryClip()
            } else {
                val clipService = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("", "")
                clipService.setPrimaryClip(clipData)
            }
        } catch (e: NullPointerException) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    R.string.clipboard_clear_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Searches entire device, adds all files to a list, then a for each loop filters
     * out files for deletion. Repeats the process as long as it keeps finding files to clean,
     * unless nothing is found to begin with
     */
    @SuppressLint("SetTextI18n")
    private fun scan(delete: Boolean) {
        FirebaseCrashlytics.getInstance().log("Starting clean")

        Looper.prepare()
        runOnUiThread {
            findViewById<View>(R.id.cleanBtn).isEnabled = !FileScanner.isRunning
            findViewById<View>(R.id.analyzeBtn).isEnabled = !FileScanner.isRunning
        }
        reset()
        if (prefs!!.getBoolean("clipboard", false)) clearClipboard()
        runOnUiThread {
            arrangeViews(delete)
            binding!!.statusTextView.text = getString(R.string.status_running)
        }
        val path = Environment.getExternalStorageDirectory()
        FirebaseCrashlytics.getInstance().log("External storage directory: $path")

        // scanner setup
        val fs = FileScanner(path, this)
            .setEmptyDir(prefs!!.getBoolean("empty", false))
            .setAutoWhite(prefs!!.getBoolean("auto_white", true))
            .setDelete(delete)
            .setCorpse(prefs!!.getBoolean("corpse", false))
            .setGUI(binding)
            .setContext(this)
            .setUpFilters(
                prefs!!.getBoolean("generic", true),
                prefs!!.getBoolean("aggressive", false),
                prefs!!.getBoolean("apk", false)
            )

        // failed scan
        if (path.listFiles() == null) { // is this needed? yes.
            FirebaseCrashlytics.getInstance().log("Failed scan - found files is null")
            val textView = quickTextView(getString(R.string.failed_scan), Color.RED)
            runOnUiThread { binding!!.fileListView.addView(textView) }
        }

        // kilobytes found/freed text
        val kilobytesTotal = fs.startScan()
        runOnUiThread {
            if (delete) binding!!.statusTextView.text =
                getString(R.string.freed) + " " + convertSize(kilobytesTotal) else binding!!.statusTextView.text =
                getString(R.string.found) + " " + convertSize(kilobytesTotal)

            // crappy but working fix for percentage never reaching 100 exactly
            binding!!.scanProgress.progress = binding!!.scanProgress.max
            binding!!.scanTextView.text = "100%"
        }
        binding!!.fileScrollView.post { binding!!.fileScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        runOnUiThread {
            findViewById<View>(R.id.cleanBtn).isEnabled = !FileScanner.isRunning
            findViewById<View>(R.id.analyzeBtn).isEnabled = !FileScanner.isRunning
        }
        Looper.loop()
    }

    /**
     * Convenience method to quickly create a textview
     * @param text - text of textview
     * @return - created textview
     */
    private fun quickTextView(text: String, color: Int): TextView {
        val textView = TextView(this@MainActivity)
        textView.setTextColor(color)
        textView.text = text
        textView.setPadding(3, 3, 3, 3)
        return textView
    }

    /**
     * Displaying text for files that have been removed
     */
    fun displayDeletion(file: File): TextView {
        // creating and adding a text view to the scroll view with path to file
        val textView = quickTextView(file.absolutePath, resources.getColor(R.color.colorAccent,resources.newTheme()))

        // adding to scroll view
        runOnUiThread { binding!!.fileListView.addView(textView) }

        // scroll to bottom
        binding!!.fileScrollView.post { binding!!.fileScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        return textView
    }

    /**
     * Displays generic text
     */
    fun displayText(text: String) {
        // creating and adding a text view to the scroll view with path to file
        val textView = quickTextView(text, Color.YELLOW)

        // adding to scroll view
        runOnUiThread { binding!!.fileListView.addView(textView) }

        // scroll to bottom
        binding!!.fileScrollView.post { binding!!.fileScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    /**
     * Removes all views present in fileListView (linear view), and sets found and removed
     * files to 0
     */
    private fun reset() {
        FirebaseCrashlytics.getInstance().log("Resetting views")

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        runOnUiThread {
            binding!!.fileListView.removeAllViews()
            binding!!.scanProgress.progress = 0
            binding!!.scanProgress.max = 1
        }
    }

    /**
     * Request write permission
     */
    private fun requestWriteExternalPermission() {
        FirebaseCrashlytics.getInstance().log("Requesting permissions")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // android 11 and up
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                ),
                1
            )
            if (!Environment.isExternalStorageManager()) { // all files
                Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
        }
    }

    /**
     * Handles the whether the user grants permission. Launches new fragment asking the user to give file permission.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) prompt()
    }

    /**
     * Launches the prompt activity
     */
    private fun prompt() {
        val intent = Intent(this, PromptActivity::class.java)
        startActivity(intent)
    }

    private fun updateTheme() {
        FirebaseCrashlytics.getInstance().log("Updating theme")

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val dark = resources.getStringArray(R.array.themes)[2]
        val light = resources.getStringArray(R.array.themes)[1]
        val selectedTheme = prefs!!.getString("theme", dark)
        if (selectedTheme == dark) { // dark
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else if (selectedTheme == light) { // light
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } // auto
    }

    companion object {
        @JvmField
        var prefs: SharedPreferences? = null
        @JvmStatic
        fun convertSize(length: Long): String {
            val format = DecimalFormat("#.##")
            val mib = (1024 * 1024).toLong()
            val kib: Long = 1024
            if (length > mib) {
                return format.format(length / mib) + " MB"
            }
            return if (length > kib) {
                format.format(length / kib) + " KB"
            } else format.format(length) + " B"
        }
    }
}