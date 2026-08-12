package com.xraypulse.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity

/**
 * QR-only scanner (not generic barcode).
 * Unlocks orientation so the camera can be held landscape for square QR codes.
 */
class QrScanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Portrait mode for QR scanning
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        IntentIntegrator(this).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("QR کد کانفیگ را داخل کادر قرار دهید\nAlign QR code inside the frame")
            setBeepEnabled(false)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
            setCameraId(0)
            setCaptureActivity(QrCaptureActivity::class.java)
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

/**
 * Capture UI dedicated to QR scanning (formats limited by IntentIntegrator).
 * Uses sensor orientation so the camera can rotate to landscape.
 */
class QrCaptureActivity : CaptureActivity()
