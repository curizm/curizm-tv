package io.curizm.tv

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var prefs: SharedPreferences
    private var resetKeySequence = mutableListOf<Int>()
    private val RESET_SEQUENCE = listOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT
    )
    
    companion object {
        private const val PREFS_NAME = "curizm_tv_prefs"
        private const val KEY_HAS_RECEIVER_CONFIG = "has_receiver_config"
        private const val KEY_WS_URL = "ws_url"
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_SECRET_CODE = "secret_code"
        private const val KEY_API_URL = "api_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("MainActivity", "=== MainActivity onCreate() ===")
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Check if we have saved receiver configuration
        val hasConfig = prefs.getBoolean(KEY_HAS_RECEIVER_CONFIG, false)
        Log.d("MainActivity", "Has saved config: $hasConfig")
        
        if (hasConfig) {
            // Launch Video Receiver Activity with saved parameters
            Log.d("MainActivity", "Launching VideoReceiverActivity")
            startVideoReceiverActivity()
        } else {
            // Launch QR Setup Activity
            Log.d("MainActivity", "Launching QRSetupActivity")
            startQRSetupActivity()
        }
        
        finish()
    }
    
    private fun startQRSetupActivity() {
        val intent = Intent(this, QRSetupActivity::class.java)
        startActivity(intent)
    }
    
    private fun startVideoReceiverActivity() {
        val wsUrl = prefs.getString(KEY_WS_URL, "wss://api.curizm.io") ?: "wss://api.curizm.io"
        val companyName = prefs.getString(KEY_COMPANY_NAME, "default") ?: "default"
        val secretCode = prefs.getString(KEY_SECRET_CODE, "room") ?: "room"
        val apiUrl = prefs.getString(KEY_API_URL, "") ?: ""
        
        Log.d("MainActivity", "Config values - wsUrl: $wsUrl, company: $companyName, secret: $secretCode")
        
        val intent = Intent(this, VideoReceiverActivity::class.java).apply {
            putExtra("wsUrl", wsUrl)
            putExtra("companyName", companyName)
            putExtra("secretCode", secretCode)
            putExtra("apiUrl", apiUrl)
        }
        startActivity(intent)
    }
    
    fun saveReceiverConfig(wsUrl: String, companyName: String, secretCode: String, apiUrl: String = "") {
        prefs.edit()
            .putBoolean(KEY_HAS_RECEIVER_CONFIG, true)
            .putString(KEY_WS_URL, wsUrl)
            .putString(KEY_COMPANY_NAME, companyName)
            .putString(KEY_SECRET_CODE, secretCode)
            .putString(KEY_API_URL, apiUrl)
            .apply()
    }
    
    private fun resetToQrSetup() {
        // Clear saved receiver configuration
        prefs.edit()
            .putBoolean(KEY_HAS_RECEIVER_CONFIG, false)
            .remove(KEY_WS_URL)
            .remove(KEY_COMPANY_NAME)
            .remove(KEY_SECRET_CODE)
            .remove(KEY_API_URL)
            .apply()
        
        resetKeySequence.clear()
        
        // Restart to QR setup
        startQRSetupActivity()
        
        // Show confirmation toast
        Toast.makeText(this, "Reset to QR setup", Toast.LENGTH_SHORT).show()
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle reset key sequence (Konami code style)
        Log.d("MainActivity", "Key pressed: $keyCode")
        
        // Only process D-pad keys for reset sequence
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || 
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN || 
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT || 
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            
            resetKeySequence.add(keyCode)
            
            // Keep only the last 8 keys
            if (resetKeySequence.size > RESET_SEQUENCE.size) {
                resetKeySequence.removeAt(0)
            }
            
            Log.d("MainActivity", "Current sequence: $resetKeySequence")
            Log.d("MainActivity", "Target sequence: $RESET_SEQUENCE")
            
            // Check if the sequence matches
            if (resetKeySequence.size == RESET_SEQUENCE.size && 
                resetKeySequence == RESET_SEQUENCE) {
                Log.d("MainActivity", "Reset sequence detected!")
                resetToQrSetup()
                return true
            }
        }
        
        return super.onKeyDown(keyCode, event)
    }
}