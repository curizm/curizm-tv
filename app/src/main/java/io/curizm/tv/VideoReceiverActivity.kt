package io.curizm.tv

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.curizm.tv.databinding.ActivityVideoReceiverBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URISyntaxException

class VideoReceiverActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoReceiverBinding
    private var player: ExoPlayer? = null
    private var bgmPlayer: ExoPlayer? = null
    private var socket: Socket? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private var playlist = mutableListOf<PlaylistItem>()
    private var currentIndex = -1
    private var captionsOn = true
    private var videoVolume = 1.0f
    private var bgmVolume = 0.25f
    
    private var wsUrl = ""
    private var companyName = ""
    private var secretCode = ""
    private var apiUrl = ""
    
    // Reset key sequence (Konami code: ↑↑↓↓←→←→)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentParameters()
        setupPlayer()
        setupBgmPlayer()
        setupUI()
        connectWebSocket()
        
        if (apiUrl.isNotEmpty()) {
            fetchPlaylistFromApi()
        }
        
        startHeartbeat()
    }

    private fun extractIntentParameters() {
        wsUrl = intent.getStringExtra("ws") ?: "wss://api.curizm.io"
        companyName = intent.getStringExtra("companyName") ?: "default"
        secretCode = intent.getStringExtra("secretCode") ?: "room"
        apiUrl = intent.getStringExtra("api") ?: ""
    }

    private fun setupPlayer() {
        // Keep screen on during playback
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Optimized ExoPlayer configuration for HLS
        player = ExoPlayer.Builder(this)
            .setLoadControl(
                com.google.android.exoplayer2.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        5000,   // minBufferMs - start playback after 5s
                        25000,  // maxBufferMs - stop loading after 25s
                        1000,   // bufferForPlaybackMs - minimum buffer for start
                        3000    // bufferForPlaybackAfterRebufferMs - buffer after rebuffer
                    )
                    .setPrioritizeTimeOverSizeThresholds(true) // Better for HLS
                    .build()
            )
            .build()
            
        binding.videoPlayer.player = player
        binding.videoPlayer.useController = false // Hide default controls for TV
        
        // Configure subtitle style for proper wrapping
        binding.videoPlayer.subtitleView?.apply {
            setStyle(com.google.android.exoplayer2.ui.CaptionStyleCompat.DEFAULT)
            setUserDefaultStyle()
            setUserDefaultTextSize()
            // Enable word wrapping for long subtitles
            setFixedTextSize(0, 16f) // 16sp text size
        }
        
        // Configure PlayerView for proper video scaling
        // Using RESIZE_MODE_FIT to prevent zoom-in issues
        binding.videoPlayer.resizeMode = com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        binding.videoPlayer.setShowBuffering(com.google.android.exoplayer2.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        
        // Use SurfaceView for better performance (less GPU intensive than TextureView)
        binding.videoPlayer.setUseController(false)
        
        // Initial layout setup
        binding.videoPlayer.post {
            binding.videoPlayer.requestLayout()
            android.util.Log.d("VideoReceiver", "PlayerView layout: ${binding.videoPlayer.width}x${binding.videoPlayer.height}")
        }
        
        // Add logging for debugging
        android.util.Log.d("VideoReceiver", "ExoPlayer configured with optimized HLS settings")
        
        // Add player event listeners
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateString = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING" 
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                android.util.Log.d("VideoReceiver", "Playback state changed: $stateString")
                
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        android.util.Log.d("VideoReceiver", "Player idle")
                    }
                    Player.STATE_READY -> {
                        hideBlackOverlay()
                        // Start BGM only after video is ready to prevent audio overlap
                        if (currentIndex >= 0 && currentIndex < playlist.size) {
                            val item = playlist[currentIndex]
                            if (item.audio.isNotEmpty()) {
                                startBgmPlayback()
                            }
                        }
                        android.util.Log.d("VideoReceiver", "Video ready, hiding overlay")
                    }
                    Player.STATE_ENDED -> {
                        if (playlist.size > 1) {
                            loadAt(currentIndex + 1)
                        }
                    }
                    Player.STATE_BUFFERING -> {
                        android.util.Log.d("VideoReceiver", "Video buffering...")
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                android.util.Log.d("VideoReceiver", "Playing changed: $isPlaying")
                if (isPlaying) {
                    hideStatusPills()
                }
            }
            
            override fun onVideoSizeChanged(videoSize: com.google.android.exoplayer2.video.VideoSize) {
                android.util.Log.d("VideoReceiver", "Video size changed: ${videoSize.width}x${videoSize.height}")
                
                // Fix for network-related scaling issues - ensure consistent resize mode
                handler.post {
                    // Reset resize mode to prevent zoom issues
                    binding.videoPlayer.resizeMode = com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    binding.videoPlayer.requestLayout()
                    android.util.Log.d("VideoReceiver", "Forced layout refresh after video size change")
                }
            }
            
            override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                android.util.Log.e("VideoReceiver", "ExoPlayer error: ${error.message}", error)
                
                // Reset resize mode on error to prevent zoom issues
                handler.post {
                    binding.videoPlayer.resizeMode = com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    binding.videoPlayer.requestLayout()
                }
            }
            
            override fun onTracksChanged(tracks: com.google.android.exoplayer2.Tracks) {
                super.onTracksChanged(tracks)
                
                // Log available tracks for debugging
                android.util.Log.d("VideoReceiver", "Tracks changed. Captions enabled: $captionsOn")
                
                if (captionsOn) {
                    // Find any unselected subtitle tracks and force selection
                    for (group in tracks.groups) {
                        if (group.type == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT && !group.isSelected) {
                            android.util.Log.d("VideoReceiver", "Found unselected subtitle track, forcing selection...")
                            
                            // Force reload with subtitles enabled to trigger selection
                            handler.postDelayed({
                                if (currentIndex >= 0 && currentIndex < playlist.size) {
                                    val item = playlist[currentIndex]
                                    if (item.subtitle.isNotEmpty()) {
                                        android.util.Log.d("VideoReceiver", "Auto-applying subtitles: ${item.subtitle}")
                                        applySubtitles(item.subtitle)
                                    }
                                }
                            }, 500)
                            break
                        }
                    }
                }
            }
        })
    }
    
    private fun setupBgmPlayer() {
        // Create separate ExoPlayer for background music
        bgmPlayer = ExoPlayer.Builder(this).build()
        bgmPlayer?.volume = bgmVolume
        bgmPlayer?.repeatMode = Player.REPEAT_MODE_ALL // Loop BGM
        
        android.util.Log.d("VideoReceiver", "BGM Player initialized")
    }
    
    private fun playBgm(bgmUrl: String) {
        if (bgmUrl.isEmpty()) {
            stopBgm()
            return
        }
        
        try {
            // Keep screen on when BGM is playing
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Bypass SSL for BGM as well
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(10000)
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent("CurizmTV/1.0")
            
            val mediaSource = if (bgmUrl.endsWith(".m3u8")) {
                // HLS BGM
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(bgmUrl)))
            } else {
                // Regular audio file
                com.google.android.exoplayer2.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(bgmUrl)))
            }
            
            bgmPlayer?.setMediaSource(mediaSource)
            bgmPlayer?.prepare()
            bgmPlayer?.volume = bgmVolume
            // Don't play immediately - wait for video to be ready
            android.util.Log.d("VideoReceiver", "BGM prepared: $bgmUrl at volume $bgmVolume")
        } catch (e: Exception) {
            android.util.Log.e("VideoReceiver", "Error playing BGM: ${e.message}", e)
        }
    }
    
    private fun startBgmPlayback() {
        // Start BGM only after video is ready
        if (bgmPlayer?.playbackState == Player.STATE_READY) {
            bgmPlayer?.play()
            android.util.Log.d("VideoReceiver", "BGM playback started")
        }
    }
    
    private fun stopBgm() {
        bgmPlayer?.stop()
        android.util.Log.d("VideoReceiver", "BGM stopped")
    }
    
    private fun setBgmVolume(volume: Float) {
        bgmVolume = volume.coerceIn(0f, 1f)
        bgmPlayer?.volume = bgmVolume
        android.util.Log.d("VideoReceiver", "BGM volume set to: $bgmVolume")
    }
    
    private fun applySubtitles(subtitleUrl: String) {
        try {
            // Enable or disable subtitles based on captionsOn
            if (subtitleUrl.isEmpty() || !captionsOn) {
                // Disable subtitles
                player?.trackSelectionParameters = player?.trackSelectionParameters!!
                    .buildUpon()
                    .setTrackTypeDisabled(com.google.android.exoplayer2.C.TRACK_TYPE_TEXT, true)
                    .build()
                android.util.Log.d("VideoReceiver", "Subtitles disabled")
                return
            }
            
            // Determine MIME type based on URL
            val mimeType = when {
                subtitleUrl.contains(".srt", ignoreCase = true) -> com.google.android.exoplayer2.util.MimeTypes.APPLICATION_SUBRIP
                subtitleUrl.contains(".vtt", ignoreCase = true) -> com.google.android.exoplayer2.util.MimeTypes.TEXT_VTT
                else -> com.google.android.exoplayer2.util.MimeTypes.APPLICATION_SUBRIP // default to SRT
            }
            
            // Create subtitle configuration
            val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                .setMimeType(mimeType)
                .setLanguage("ko")
                .setSelectionFlags(com.google.android.exoplayer2.C.SELECTION_FLAG_DEFAULT)
                .build()
            
            // Get current media item
            val currentMediaItem = player?.currentMediaItem
            if (currentMediaItem != null) {
                // Build new media item with subtitle
                val newMediaItem = currentMediaItem.buildUpon()
                    .setSubtitleConfigurations(listOf(subtitle))
                    .build()
                
                // Save current position
                val currentPosition = player?.currentPosition ?: 0
                val wasPlaying = player?.isPlaying ?: false
                
                // Set new media item with subtitle
                player?.setMediaItem(newMediaItem)
                player?.seekTo(currentPosition)
                player?.prepare()
                
                // Enable subtitle track
                player?.trackSelectionParameters = player?.trackSelectionParameters!!
                    .buildUpon()
                    .setTrackTypeDisabled(com.google.android.exoplayer2.C.TRACK_TYPE_TEXT, false)
                    .build()
                
                if (wasPlaying) {
                    player?.play()
                }
                
                android.util.Log.d("VideoReceiver", "Subtitles applied: $subtitleUrl (type: $mimeType)")
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoReceiver", "Error applying subtitles: ${e.message}", e)
        }
    }

    private fun setupUI() {
        val roomId = "${companyName}__${secretCode}".lowercase()
        binding.roomPill.text = "Room: $roomId"
        binding.connectionPill.text = "WS: connecting..."
        
        // Initially hide overlays
        binding.infoOverlay.alpha = 0f
        binding.infoOverlay.visibility = View.GONE
        binding.blackOverlay.alpha = 0f
        binding.blackOverlay.visibility = View.GONE
        
        // Log PlayerView dimensions for debugging
        binding.videoPlayer.post {
            android.util.Log.d("VideoReceiver", "PlayerView dimensions: ${binding.videoPlayer.width}x${binding.videoPlayer.height}")
            android.util.Log.d("VideoReceiver", "Screen dimensions: ${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}")
        }
    }

    private fun connectWebSocket() {
        try {
            socket = IO.socket(wsUrl)

            socket?.on(Socket.EVENT_CONNECT) {
                runOnUiThread {
                    binding.connectionPill.text = "WS: connected"
                    val helloData = JSONObject().apply {
                        put("role", "receiver")
                        put("companyName", companyName)
                        put("secretCode", secretCode)
                    }
                    socket?.emit("HELLO", helloData)
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                runOnUiThread {
                    binding.connectionPill.text = "WS: closed"
                    // Retry connection after delay
                    handler.postDelayed({ connectWebSocket() }, 1500)
                }
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) {
                runOnUiThread {
                    binding.connectionPill.text = "WS: error"
                }
            }

            socket?.on("SET_PLAYLIST") { args ->
                android.util.Log.d("VideoReceiver", "=== SET_PLAYLIST received ===")
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val itemsArray = data.optJSONArray("items")
                    android.util.Log.d("VideoReceiver", "Items array size: ${itemsArray?.length() ?: 0}")
                    
                    if (itemsArray != null) {
                        runOnUiThread {
                            parsePlaylist(itemsArray)
                            android.util.Log.d("VideoReceiver", "Playlist parsed, size: ${playlist.size}")
                            if (currentIndex < 0 && playlist.isNotEmpty()) {
                                android.util.Log.d("VideoReceiver", "Auto-loading first video")
                                loadAt(0)
                            } else {
                                android.util.Log.d("VideoReceiver", "Not auto-loading: currentIndex=$currentIndex, playlist.size=${playlist.size}")
                            }
                        }
                    } else {
                        android.util.Log.e("VideoReceiver", "ERROR: No items array in SET_PLAYLIST")
                    }
                } else {
                    android.util.Log.e("VideoReceiver", "ERROR: Empty args in SET_PLAYLIST")
                }
            }

            socket?.on("COMMAND") { args ->
                if (args.isNotEmpty()) {
                    val command = args[0] as JSONObject
                    runOnUiThread {
                        handleCommand(command)
                    }
                }
            }

            socket?.connect()

        } catch (e: URISyntaxException) {
            binding.connectionPill.text = "WS: failed"
            handler.postDelayed({ connectWebSocket() }, 2000)
        }
    }

    private fun fetchPlaylistFromApi() {
        val client = OkHttpClient()
        val separator = if (apiUrl.contains("?")) "&" else "?"
        val fullUrl = "$apiUrl${separator}companyName=$companyName&secretCode=$secretCode"
        
        val request = Request.Builder()
            .url(fullUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle API failure silently
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val playlistArray = jsonResponse.optJSONArray("playlist")
                        if (playlistArray != null) {
                            runOnUiThread {
                                parsePlaylist(playlistArray)
                                if (playlist.isNotEmpty()) {
                                    loadAt(0)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Handle parsing error silently
                    }
                }
            }
        })
    }

    private fun parsePlaylist(itemsArray: JSONArray) {
        playlist.clear()
        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.optJSONObject(i)
            if (item != null && item.has("video")) {
                val playlistItem = PlaylistItem(
                    video = item.getString("video"),
                    audio = item.optString("audio", ""),
                    subtitle = item.optString("subtitle", ""),
                    poster = item.optString("poster", ""),
                    title = item.optString("artworkName", item.optString("title", "")),
                    artist = item.optString("artistName", item.optString("artist", "")),
                    size = item.optString("size", ""),
                    material = item.optString("material", ""),
                    order = item.optInt("order", 0)
                )
                playlist.add(playlistItem)
            }
        }
        
        // Sort by order
        playlist.sortBy { it.order }
    }

    private fun loadAt(index: Int) {
        android.util.Log.d("VideoReceiver", "=== loadAt() called with index: $index ===")
        android.util.Log.d("VideoReceiver", "Playlist size: ${playlist.size}")
        
        if (playlist.isEmpty()) {
            android.util.Log.e("VideoReceiver", "ERROR: Playlist is empty, cannot load video")
            return
        }
        
        currentIndex = (index + playlist.size) % playlist.size
        val item = playlist[currentIndex]
        
        android.util.Log.d("VideoReceiver", "Loading item: ${item.title}")
        android.util.Log.d("VideoReceiver", "Video URL: ${item.video}")
        
        val roomId = "${companyName}__${secretCode}".lowercase()
        binding.roomPill.text = "Room: $roomId  |  #${currentIndex + 1}/${playlist.size}"
        
        // Show black overlay during transitions
        showBlackOverlay()
        
        handler.postDelayed({
            android.util.Log.d("VideoReceiver", "=== Starting video load process ===")
            
            try {
                // Load HLS stream with optimized configuration
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setConnectTimeoutMs(10000) // 10 second connection timeout
                    .setReadTimeoutMs(10000)    // 10 second read timeout
                    .setAllowCrossProtocolRedirects(true)
                    .setUserAgent("CurizmTV/1.0")
                
                android.util.Log.d("VideoReceiver", "DataSource factory created")
                
                // Build MediaItem with subtitle if available
                val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(item.video))
                
                if (item.subtitle.isNotEmpty() && captionsOn) {
                    val mimeType = when {
                        item.subtitle.contains(".srt", ignoreCase = true) -> com.google.android.exoplayer2.util.MimeTypes.APPLICATION_SUBRIP
                        item.subtitle.contains(".vtt", ignoreCase = true) -> com.google.android.exoplayer2.util.MimeTypes.TEXT_VTT
                        else -> com.google.android.exoplayer2.util.MimeTypes.APPLICATION_SUBRIP
                    }
                    
                    val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.parse(item.subtitle))
                        .setMimeType(mimeType)
                        .setLanguage("ko")
                        .setSelectionFlags(com.google.android.exoplayer2.C.SELECTION_FLAG_DEFAULT)
                        .build()
                    
                    mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
                    android.util.Log.d("VideoReceiver", "Subtitle configured: ${item.subtitle}")
                }
                
                val mediaItem = mediaItemBuilder.build()
                
                val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true) // Faster HLS startup
                    .createMediaSource(mediaItem)
                
                android.util.Log.d("VideoReceiver", "HLS MediaSource created")
                android.util.Log.d("VideoReceiver", "Player state before setMediaSource: ${player?.playbackState}")
                
                player?.setMediaSource(hlsMediaSource)
                android.util.Log.d("VideoReceiver", "MediaSource set on player")
                
                player?.prepare()
                android.util.Log.d("VideoReceiver", "Player prepare() called")
                
                player?.volume = videoVolume
                android.util.Log.d("VideoReceiver", "Volume set to: $videoVolume")
                
                // Enable subtitles by default if captionsOn is true
                if (captionsOn) {
                    player?.trackSelectionParameters = player?.trackSelectionParameters!!
                        .buildUpon()
                        .setTrackTypeDisabled(com.google.android.exoplayer2.C.TRACK_TYPE_TEXT, false)
                        .build()
                    android.util.Log.d("VideoReceiver", "Subtitles enabled by default")
                } else {
                    player?.trackSelectionParameters = player?.trackSelectionParameters!!
                        .buildUpon()
                        .setTrackTypeDisabled(com.google.android.exoplayer2.C.TRACK_TYPE_TEXT, true)
                        .build()
                    android.util.Log.d("VideoReceiver", "Subtitles disabled")
                }
                
                player?.play()
                android.util.Log.d("VideoReceiver", "Player play() called")
                
                // Force subtitle application if captions are on
                if (captionsOn && item.subtitle.isNotEmpty()) {
                    handler.postDelayed({
                        applySubtitles(item.subtitle)
                        android.util.Log.d("VideoReceiver", "Auto-applying subtitles after playback start")
                    }, 1000)
                }
                
                // Prepare BGM if available (will start after video is ready)
                if (item.audio.isNotEmpty()) {
                    playBgm(item.audio)
                } else {
                    stopBgm()
                }
                
                // Show artwork overlay
                showArtworkOverlay(item)
                android.util.Log.d("VideoReceiver", "Artwork overlay shown")
                
            } catch (e: Exception) {
                android.util.Log.e("VideoReceiver", "ERROR in loadAt(): ${e.message}", e)
            }
        }, 100)
    }

    private fun showBlackOverlay() {
        binding.blackOverlay.visibility = View.VISIBLE
        binding.blackOverlay.alpha = 1f
    }

    private fun hideBlackOverlay() {
        ObjectAnimator.ofFloat(binding.blackOverlay, "alpha", 1f, 0f).apply {
            duration = 200
            start()
        }
        handler.postDelayed({
            binding.blackOverlay.visibility = View.GONE
        }, 200)
    }

    private fun showArtworkOverlay(item: PlaylistItem) {
        binding.artworkTitle.text = item.title.ifEmpty { "Untitled Artwork" }
        
        val details = mutableListOf<String>()
        if (item.artist.isNotEmpty()) details.add(item.artist)
        if (item.size.isNotEmpty()) details.add(item.size)
        if (item.material.isNotEmpty()) details.add(item.material)
        
        binding.artworkDetails.text = if (details.isNotEmpty()) {
            details.joinToString(" • ")
        } else {
            "No additional information"
        }
        
        binding.infoOverlay.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(binding.infoOverlay, "alpha", 0f, 1f).apply {
            duration = 200
            start()
        }
        
        // Auto-hide overlay
        val displayDuration = maxOf(4000, minOf(8000, 
            (binding.artworkTitle.text.length + binding.artworkDetails.text.length) * 50))
        
        handler.postDelayed({
            ObjectAnimator.ofFloat(binding.infoOverlay, "alpha", 1f, 0f).apply {
                duration = 200
                start()
            }
            handler.postDelayed({
                binding.infoOverlay.visibility = View.GONE
            }, 200)
        }, displayDuration.toLong())
    }

    private fun hideStatusPills() {
        ObjectAnimator.ofFloat(binding.statusPills, "alpha", 0.6f, 0f).apply {
            duration = 200
            start()
        }
    }

    private fun handleCommand(command: JSONObject) {
        val action = command.optString("action")
        val value = command.opt("value")
        val startAt = command.optLong("startAt", 0)
        
        android.util.Log.d("VideoReceiver", "Command received: action=$action, value=$value, startAt=$startAt")
        
        when (action) {
            "PLAY" -> {
                if (startAt > 0 && startAt > System.currentTimeMillis()) {
                    val delay = startAt - System.currentTimeMillis()
                    android.util.Log.d("VideoReceiver", "Scheduled PLAY in ${delay}ms")
                    handler.postDelayed({
                        player?.play()
                        bgmPlayer?.play()
                        android.util.Log.d("VideoReceiver", "Scheduled PLAY executed")
                    }, delay)
                } else {
                    player?.play()
                    bgmPlayer?.play()
                    android.util.Log.d("VideoReceiver", "Immediate PLAY executed")
                }
            }
            "PAUSE" -> {
                player?.pause()
                bgmPlayer?.pause()
            }
            "NEXT" -> loadAt(currentIndex + 1)
            "PREV" -> loadAt(currentIndex - 1)
            "JUMP_TO_INDEX" -> {
                if (value is Number) {
                    loadAt(value.toInt())
                }
            }
            "SEEK" -> {
                if (value is Number) {
                    player?.seekTo(maxOf(0, value.toLong() * 1000))
                }
            }
            "SET_VIDEO_VOLUME" -> {
                if (value is Number) {
                    videoVolume = maxOf(0f, minOf(1f, value.toFloat()))
                    player?.volume = videoVolume
                }
            }
            "SET_BGM_VOLUME" -> {
                if (value is Number) {
                    setBgmVolume(value.toFloat())
                }
            }
            "SET_CAPTIONS" -> {
                if (value is Boolean) {
                    captionsOn = value
                    // Re-apply subtitles with new caption setting
                    if (currentIndex >= 0 && currentIndex < playlist.size) {
                        val item = playlist[currentIndex]
                        if (item.subtitle.isNotEmpty()) {
                            applySubtitles(item.subtitle)
                        }
                    }
                }
            }
        }
    }

    private fun startHeartbeat() {
        val heartbeatRunnable = object : Runnable {
            override fun run() {
                socket?.let { socket ->
                    if (socket.connected()) {
                        val heartbeatData = JSONObject().apply {
                            put("ts", System.currentTimeMillis())
                            put("idx", currentIndex)
                            put("paused", player?.isPlaying != true)
                            put("t", (player?.currentPosition ?: 0) / 1000)
                        }
                        socket.emit("HEARTBEAT", heartbeatData)
                    }
                }
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(heartbeatRunnable)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle reset key sequence (Konami code style)
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || 
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN || 
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT || 
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            
            resetKeySequence.add(keyCode)
            
            // Keep only the last 8 keys
            if (resetKeySequence.size > RESET_SEQUENCE.size) {
                resetKeySequence.removeAt(0)
            }
            
            // Check if the sequence matches
            if (resetKeySequence.size == RESET_SEQUENCE.size && 
                resetKeySequence == RESET_SEQUENCE) {
                resetToQrSetup()
                return true
            }
        }
        
        return super.onKeyDown(keyCode, event)
    }
    
    private fun resetToQrSetup() {
        // Clear saved receiver configuration
        val prefs = getSharedPreferences("curizm_tv_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("has_receiver_config", false)
            .remove("ws_url")
            .remove("company_name")
            .remove("secret_code")
            .remove("api_url")
            .apply()
        
        // Launch QR Setup Activity
        val intent = Intent(this, QRSetupActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        bgmPlayer?.release()
        socket?.disconnect()
    }

    data class PlaylistItem(
        val video: String,
        val audio: String,
        val subtitle: String,
        val poster: String,
        val title: String,
        val artist: String,
        val size: String,
        val material: String,
        val order: Int
    )
}

