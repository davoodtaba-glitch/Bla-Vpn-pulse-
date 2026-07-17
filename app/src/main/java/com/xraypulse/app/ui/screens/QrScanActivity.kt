package com.xraypulse.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity

/**
 * Thin wrapper around ZXing embedded scanner.
 * Returns scanned text via setResult.
 */
class QrScanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IntentIntegrator(this).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Scan VLESS / VMess / Trojan QR")
            setBeepEnabled(false)
            setOrientationLocked(true)
            setCaptureActivity(CaptureActivity::class.java)
            initiateScan()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_RESULT, result.contents))
            } else {
                setResult(Activity.RESULT_CANCELED)
            }
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val EXTRA_RESULT = "qr_result"
    }
}
