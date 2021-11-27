/*
 * Copyright 2021 Hunter J Drum
 */
package theredspy15.ltecleanerfoss.controllers

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import theredspy15.ltecleanerfoss.CleanReceiver.Companion.cancelAlarm
import theredspy15.ltecleanerfoss.CleanReceiver.Companion.scheduleAlarm
import theredspy15.ltecleanerfoss.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
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
        }

        /**
         * Inflate Preferences
         */
        override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
            addPreferencesFromResource(R.xml.preferences)
        }
    }
}