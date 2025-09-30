package io.curizm.tv2

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import io.curizm.tv2.databinding.ActivityQrSetupBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URISyntaxException

class QRSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrSetupBinding
    private var socket: Socket? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var timeLeft = 180 // 3 minutes in seconds
    private var connectionCode = ""
    private var sessionId = ""
    private var websocketUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        initializeTVSession()
    }

    private fun setupUI() {
        updateTimerDisplay()
        updateConnectionStatus(ConnectionStatus.CONNECTING)
    }

    private fun initializeTVSession() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.curizm.io/api/v1/tv/generate-session")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.DISCONNECTED)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        sessionId = jsonResponse.getString("sessionId")
                        websocketUrl = jsonResponse.getString("websocketUrl")
                        connectionCode = jsonResponse.getString("connectionCode")

                        runOnUiThread {
                            generateQRCode(sessionId)
                            binding.connectionCodeText.text = connectionCode
                            connectWebSocket()
                            startTimer()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            updateConnectionStatus(ConnectionStatus.DISCONNECTED)
                        }
                    }
                }
            }
        })
    }

    private fun generateQRCode(sessionId: String) {
        try {
            val qrUrl = "https://www.curizm.io/connect?session=$sessionId"
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(qrUrl, BarcodeFormat.QR_CODE, 300, 300)
            binding.qrCodeImage.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun connectWebSocket() {
        try {
            val socketUrl = websocketUrl.replace("ws://", "http://").replace("wss://", "https://")
            socket = IO.socket(socketUrl)

            socket?.on(Socket.EVENT_CONNECT) {
                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.CONNECTED)
                    socket?.emit("TV_JOIN_SESSION", JSONObject().put("sessionId", sessionId))
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                runOnUiThread {
                    updateConnectionStatus(ConnectionStatus.DISCONNECTED)
                }
            }

            socket?.on("TV_SETUP_COMPLETE") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val redirectUrl = data.optString("redirectUrl")
                    if (redirectUrl.isNotEmpty()) {
                        runOnUiThread {
                            startVideoReceiverActivity(redirectUrl)
                        }
                    }
                }
            }

            socket?.on("NEW_CONNECTION_CODE") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val newCode = data.optString("connectionCode")
                    if (newCode.isNotEmpty()) {
                        runOnUiThread {
                            connectionCode = newCode
                            binding.connectionCodeText.text = connectionCode
                            timeLeft = 180 // Reset timer
                        }
                    }
                }
            }

            socket?.connect()

        } catch (e: URISyntaxException) {
            updateConnectionStatus(ConnectionStatus.DISCONNECTED)
        }
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (timeLeft > 0) {
                    timeLeft--
                    updateTimerDisplay()
                    handler.postDelayed(this, 1000)
                } else {
                    // Reset timer when it reaches 0
                    timeLeft = 180
                    updateTimerDisplay()
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun updateTimerDisplay() {
        val minutes = timeLeft / 60
        val seconds = timeLeft % 60
        val formattedTime = String.format("%02d:%02d", minutes, seconds)
        binding.timerText.text = formattedTime
    }

    private fun updateConnectionStatus(status: ConnectionStatus) {
        when (status) {
            ConnectionStatus.CONNECTING -> {
                binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_connecting)
                binding.statusText.text = "Connecting..."
            }
            ConnectionStatus.CONNECTED -> {
                binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_connected)
                binding.statusText.text = "Connected - Waiting for setup..."
            }
            ConnectionStatus.DISCONNECTED -> {
                binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_disconnected)
                binding.statusText.text = "Connection lost - Retrying..."
                // Retry connection after delay
                handler.postDelayed({ initializeTVSession() }, 5000)
            }
        }
    }

    private fun startVideoReceiverActivity(redirectUrl: String) {
        // Extract parameters from redirect URL
        val uri = android.net.Uri.parse(redirectUrl)
        val wsUrl = uri.getQueryParameter("ws") ?: "wss://api.curizm.io"
        val companyName = uri.getQueryParameter("companyName") ?: "default"
        val secretCode = uri.getQueryParameter("secretCode") ?: "room"
        val apiUrl = uri.getQueryParameter("api") ?: ""
        
        // Save configuration for future launches
        val prefs = getSharedPreferences("curizm_tv_prefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("has_receiver_config", true)
            .putString("ws_url", wsUrl)
            .putString("company_name", companyName)
            .putString("secret_code", secretCode)
            .putString("api_url", apiUrl)
            .apply()
        
        val intent = Intent(this, VideoReceiverActivity::class.java)
        intent.putExtra("ws", wsUrl)
        intent.putExtra("companyName", companyName)
        intent.putExtra("secretCode", secretCode)
        intent.putExtra("api", apiUrl)
        
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerRunnable?.let { handler.removeCallbacks(it) }
        socket?.disconnect()
    }

    enum class ConnectionStatus {
        CONNECTING, CONNECTED, DISCONNECTED
    }
}
