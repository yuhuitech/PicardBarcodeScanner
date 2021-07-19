/*
 * Copyright (C) 2012, 2021 Philipp Wolfer <ph.wolfer@gmail.com>
 * Copyright (C) 2021 Akshat Tiwari
 * 
 * This file is part of MusicBrainz Picard Barcode Scanner.
 * 
 * MusicBrainz Picard Barcode Scanner is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * MusicBrainz Picard Barcode Scanner is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MusicBrainz Picard Barcode Scanner. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.musicbrainz.picard.barcodescanner.activities

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.musicbrainz.picard.barcodescanner.R
import org.musicbrainz.picard.barcodescanner.util.Constants
import org.musicbrainz.picard.barcodescanner.webservice.PicardClient
import java.util.*

class ScannerActivity : BaseActivity() {
    private var mAutoStart = false
    private val uiScope = CoroutineScope(Dispatchers.Main)

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSubView(R.layout.activity_scanner)
        val connectBtn = findViewById<View>(R.id.btn_scan_barcode) as Button
        connectBtn.setOnClickListener { startScanner() }
        handleIntents()

        if (!checkIfSettingsAreComplete()) {
            val configurePicard = Intent(
                this@ScannerActivity,
                PreferencesActivity::class.java
            )
            startActivity(configurePicard)
        } else if (mAutoStart) {
            startScanner()
        } else {
            uiScope.launch {
                checkConnectionStatus()
            }
        }
    }

    override fun handleIntents() {
        super.handleIntents()
        val extras = intent.extras
        if (extras != null) {
            mAutoStart = extras.getBoolean(Constants.INTENT_EXTRA_AUTOSTART_SCANNER, false)
        }
    }

    private val zxingActivityResultLauncher  = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val scanResult = IntentIntegrator.parseActivityResult(it.resultCode, it.data)
        if (scanResult != null) {
            val barcode = scanResult.contents
            if (barcode != null) {
                startSearchActivity(barcode)
            }
        }
    }

    private fun startSearchActivity(barcode: String) {
        val resultIntent = Intent(
            this@ScannerActivity,
            PerformSearchActivity::class.java
        )
        resultIntent.putExtra(Constants.INTENT_EXTRA_BARCODE, barcode)
        startActivity(resultIntent)
    }

    private fun startScanner() {
        val integrator = IntentIntegrator(this@ScannerActivity)
        integrator.setOrientationLocked(false)
        zxingActivityResultLauncher.launch(integrator.createScanIntent())
    }

    private suspend fun checkConnectionStatus() {
        if (checkIfSettingsAreComplete()) {
            val client = PicardClient(preferences.ipAddress!!, preferences.port)
            val status = client.ping()
            val appLabel = findViewById<View>(R.id.status_application_name) as TextView
            val hostLabel = findViewById<View>(R.id.status_connection_host) as TextView
            val errorLabel = findViewById<View>(R.id.status_no_connection) as TextView
            if (status.active) {
                appLabel.visibility = View.VISIBLE
                hostLabel.visibility = View.VISIBLE
                errorLabel.visibility = View.GONE
                appLabel.text = status.application
                hostLabel.text = "%s:%d".format(preferences.ipAddress, preferences.port)
            } else {
                appLabel.visibility = View.GONE
                hostLabel.visibility = View.GONE
                errorLabel.visibility = View.VISIBLE
            }
        }
    }
}
