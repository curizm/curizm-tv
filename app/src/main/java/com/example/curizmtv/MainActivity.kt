package com.example.curizmtv

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
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
        private const val KEY_RECEIVER_URL = "receiver_url"
        private const val QR_SETUP_URL = "https://www.curizm.io/qr"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        webView = findViewById(R.id.webView)
        
        setupWebView()
        setupBackPressedCallback()
        loadInitialPage()
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
                // If this is a receiver URL (contains required parameters), save it
                if (url != null && isReceiverUrl(url)) {
                    saveReceiverUrl(url)
                }
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Handle URL redirects within the WebView
                return false
            }
        }
    }
    
    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    private fun loadInitialPage() {
        val savedReceiverUrl = prefs.getString(KEY_RECEIVER_URL, null)
        
        if (savedReceiverUrl != null) {
            // Load the saved receiver URL
            webView.loadUrl(savedReceiverUrl)
        } else {
            // Load QR setup page
            webView.loadUrl(QR_SETUP_URL)
        }
    }
    
    private fun isReceiverUrl(url: String): Boolean {
        // Check if URL contains receiver parameters
        return url.contains("curizm.io/tv/receiver") || 
               (url.contains("companyName=") && url.contains("secretCode="))
    }
    
    private fun saveReceiverUrl(url: String) {
        prefs.edit()
            .putString(KEY_RECEIVER_URL, url)
            .apply()
    }
    
    private fun resetToQrPage() {
        // Clear saved receiver URL and reload QR page
        prefs.edit()
            .remove(KEY_RECEIVER_URL)
            .apply()
        
        webView.loadUrl(QR_SETUP_URL)
        resetKeySequence.clear()
        
        // Show confirmation toast
        Toast.makeText(this, "Reset to QR setup page", Toast.LENGTH_SHORT).show()
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
                resetToQrPage()
                return true
            }
        }
        
        return super.onKeyDown(keyCode, event)
    }
    
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        // Ensure key events are processed even if WebView tries to intercept them
        if (event?.action == KeyEvent.ACTION_DOWN) {
            val handled = onKeyDown(event.keyCode, event)
            if (handled) return true
        }
        return super.dispatchKeyEvent(event)
    }
}