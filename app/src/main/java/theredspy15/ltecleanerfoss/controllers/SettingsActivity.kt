/*
 * Copyright 2021 Hunter J Drum
 */
package theredspy15.ltecleanerfoss.controllers

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import theredspy15.ltecleanerfoss.CleanReceiver.Companion.cancelAlarm
import theredspy15.ltecleanerfoss.CleanReceiver.Companion.scheduleAlarm
import theredspy15.ltecleanerfoss.R







class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        FirebaseCrashlytics.getInstance().log("Displaying settings activity")

        supportFragmentManager.beginTransaction().replace(R.id.layout, MyPreferenceFragment())
            .commit()
    }

    class MyPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(true)
            findPreference<Preference>("aggressive")!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference, _: Any? ->
                    val checked = (preference as CheckBoxPreference).isChecked
                    if (!checked) {
                        val filtersFiles =
                            resources.getStringArray(R.array.aggressive_filter_folders)
                        val alertDialog = AlertDialog.Builder(requireContext()).create()
                        alertDialog.setTitle(getString(R.string.aggressive_filter_what_title))
                        alertDialog.setMessage(
                            getString(R.string.adds_the_following) + " " + filtersFiles.contentToString()
                        )
                        alertDialog.setButton(
                            AlertDialog.BUTTON_NEUTRAL, "OK"
                        ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                        alertDialog.show()
                    }
                    true
                }
            findPreference<Preference>("dailyclean")!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference, _: Any? ->
                    val checked = (preference as CheckBoxPreference).isChecked
                    if (!checked) {
                        scheduleAlarm(requireContext().applicationContext)
                    } else {
                        cancelAlarm(requireContext().applicationContext)
                    }
                    true
                }
            findPreference<Preference>("removeads")!!.setOnPreferenceClickListener {
                Toast.makeText(requireContext(),"clicked",Toast.LENGTH_LONG).show()

                val firebaseAnalytics = Firebase.analytics
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.PROMOTION_ID, "115")
                bundle.putString(FirebaseAnalytics.Param.PROMOTION_NAME, "lte_pro")
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_PROMOTION, bundle)

                val url = "https://play.google.com/store/apps/details?id=theredspy15.ltecleanerfosspro"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)

                true
            }
        }

        /**
         * Inflate Preferences
         */
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)
        }
    }
}