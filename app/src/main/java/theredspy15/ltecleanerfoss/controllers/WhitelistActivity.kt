/*
 * Copyright 2021 Hunter J Drum
 */
package theredspy15.ltecleanerfoss.controllers

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.appcompat.app.AppCompatActivity
import dev.shreyaspatil.MaterialDialog.MaterialDialog
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface
import theredspy15.ltecleanerfoss.R
import theredspy15.ltecleanerfoss.databinding.ActivityWhitelistBinding
import java.util.*
import kotlin.collections.ArrayList

class WhitelistActivity : AppCompatActivity() {
    var binding: ActivityWhitelistBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whitelist)
        binding = ActivityWhitelistBinding.inflate(
            layoutInflater
        )
        setContentView(binding!!.root)
        binding!!.newButton.setOnClickListener { addToWhiteList() }
        getWhiteList(MainActivity.prefs)
        loadViews()
    }

    private fun loadViews() {
        binding!!.pathsLayout.removeAllViews()
        val layout = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layout.setMargins(0, 20, 0, 20)
        if (whiteList != null) {
            for (path in whiteList!!) {
                val button = Button(this)
                button.text = path
                button.textSize = 18f
                button.isAllCaps = false
                button.setOnClickListener { removePath(path, button) }
                button.setPadding(50, 50, 50, 50)
                layout.setMargins(0, 20, 0, 20)
                button.setBackgroundResource(R.drawable.rounded_view)
                val drawable = button.background as GradientDrawable
                drawable.setColor(Color.GRAY)
                drawable.alpha = 30
                runOnUiThread { binding!!.pathsLayout.addView(button, layout) }
            }
        }
        if (whiteList == null || whiteList!!.isEmpty()) {
            val textView = TextView(this) // no news feeds selected
            textView.setText(R.string.empty_whitelist)
            textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            textView.textSize = 18f
            runOnUiThread { binding!!.pathsLayout.addView(textView, layout) }
        }
    }

    private fun removePath(path: String?, button: Button?) {
        val mDialog = MaterialDialog.Builder(this)
            .setTitle(getString(R.string.remove_from_whitelist))
            .setMessage(path!!)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.delete)) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                whiteList.remove(path)
                MainActivity.prefs!!.edit().putStringSet("whitelist", HashSet(whiteList)).apply()
                binding!!.pathsLayout.removeView(button)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            .build()
        mDialog.show()
    }

    /**
     * Creates a dialog asking for a file/folder name to add to the whitelist
     */
    private fun addToWhiteList() {
        mGetContent.launch(Uri.fromFile(Environment.getDataDirectory()))
    }

    private var mGetContent = registerForActivityResult(
        OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            whiteList.add(uri.path!!.substring(uri.path!!.indexOf(":") + 1)) // TODO create file from uri, then just add its path once sd card support is finished
            MainActivity.prefs!!.edit().putStringSet("whitelist", HashSet(whiteList)).apply()
            loadViews()
        }
    }

    companion object {
        private var whiteList: ArrayList<String> = ArrayList()
        fun getWhiteList(prefs: SharedPreferences?): List<String?> {
            if (whiteList.isNullOrEmpty()) {
                if (prefs != null) {
                    whiteList = ArrayList(prefs.getStringSet("whitelist", emptySet()))
                }
                whiteList.remove("[")
                whiteList.remove("]")
            }
            return whiteList
        }
    }
}