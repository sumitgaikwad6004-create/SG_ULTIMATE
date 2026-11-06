package com.sg.browser

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.core.view.setPadding
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer

@UnstableApi
class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var web: WebView
    private lateinit var playerView: PlayerView
    private lateinit var browserPanel: LinearLayout
    private lateinit var playerPanel: LinearLayout
    private var player: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var bass: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var lastUrl = "https://www.google.com"
    private var privateMode = false
    private var desktopMode = false
    private var adBlockEnabled = false

    private var saturation = 0f       // adjustment: -100..100
    private var lightness = 0f        // adjustment: -100..100
    private var hue = 0f              // degrees
    private var brightness = 0f       // -1..1
    private var contrast = 0f         // -1..1

    private val bg = Color.rgb(249, 250, 251)
    private val ink = Color.rgb(26, 31, 38)
    private val muted = Color.rgb(105, 113, 123)


    private fun showBrowserTools() {
        val items = arrayOf(
            if (privateMode) "Turn off private mode" else "Private mode",
            "Add bookmark",
            "Bookmarks",
            "Browsing history",
            if (desktopMode) "Turn off desktop site" else "Desktop site",
            "Find on page",
            "Share page",
            "Copy page URL",
            "Clear browsing data",
            "Privacy / ad-block setting",
            "Open video URL",
            "Audio studio"
        )
        AlertDialog.Builder(this).setTitle("SG_ULTIMATE tools").setItems(items) { _, which ->
            when (which) {
                0 -> {
                    privateMode = !privateMode
                    if (privateMode) toast("Private mode on: new visits won't be added to history")
                    else toast("Private mode off")
                }
                1 -> {
                    val prefs = getSharedPreferences("sg_browser", MODE_PRIVATE)
                    val old = prefs.getString("bookmarks", "") ?: ""
                    prefs.edit().putString("bookmarks", (lastUrl + "\n" + old).lines().distinct().take(100).joinToString("\n")).apply()
                    toast("Bookmark saved")
                }
                2 -> showSavedList("Bookmarks", "bookmarks")
                3 -> showSavedList("History", "history")
                4 -> {
                    desktopMode = !desktopMode
                    web.settings.userAgentString = if (desktopMode)
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36"
                    else WebView(this).settings.userAgentString
                    web.settings.useWideViewPort = desktopMode
                    web.settings.loadWithOverviewMode = desktopMode
                    web.reload()
                    toast(if (desktopMode) "Desktop site enabled" else "Mobile site enabled")
                }
                5 -> {
                    val input = EditText(this).apply { hint = "Find text on this page" }
                    AlertDialog.Builder(this).setTitle("Find on page").setView(input)
                        .setPositiveButton("Find") { _, _ -> web.findAllAsync(input.text.toString()) }
                        .setNegativeButton("Close", null).show()
                }
                6 -> sharePage()
                7 -> {
                    val clip = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clip.setPrimaryClip(android.content.ClipData.newPlainText("Page URL", lastUrl))
                    toast("URL copied")
                }
                8 -> {
                    AlertDialog.Builder(this).setTitle("Clear browsing data")
                        .setMessage("Clear cookies, cached pages, form data and saved history?")
                        .setPositiveButton("Clear") { _, _ ->
                            android.webkit.CookieManager.getInstance().removeAllCookies(null)
                            web.clearCache(true)
                            web.clearFormData()
                            getSharedPreferences("sg_browser", MODE_PRIVATE).edit().clear().apply()
                            toast("Browsing data cleared")
                        }.setNegativeButton("Cancel", null).show()
                }
                9 -> {
                    val options = arrayOf("Enable basic domain blocking", "Disable domain blocking")
                    AlertDialog.Builder(this).setTitle("Privacy setting").setItems(options) { _, selected ->
                        adBlockEnabled = selected == 0
                        toast("Basic blocking setting saved. Full content blocking needs a maintained filter list.")
                    }.show()
                }
                10 -> askVideoUrl()
                11 -> showAudioStudio()
            }
        }.show()
    }

    private fun showSavedList(title: String, key: String) {
        val values = (getSharedPreferences("sg_browser", MODE_PRIVATE).getString(key, "") ?: "")
            .lines().filter { it.isNotBlank() }.distinct()
        if (values.isEmpty()) {
            toast("Nothing saved yet")
            return
        }
        AlertDialog.Builder(this).setTitle(title).setItems(values.toTypedArray()) { _, index ->
            navigate(values[index])
        }.setNegativeButton("Close", null).show()
    }

    private fun sharePage() {
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, lastUrl)
        }
        startActivity(android.content.Intent.createChooser(send, "Share page"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        buildUi()
        web.loadUrl(lastUrl)
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12)
            setBackgroundColor(Color.WHITE)
        }
        val brandRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        brandRow.addView(TextView(this).apply {
            text = "SG"
            textSize = 25f
            setTextColor(ink)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        brandRow.addView(TextView(this).apply {
            text = "  ULTIMATE BROWSER  •  VERSION 5.0"
            textSize = 10f
            setTextColor(muted)
        })
        header.addView(brandRow)

        address = EditText(this).apply {
            singleLine = true
            textSize = 14f
            hint = "Search or enter website"
            setTextColor(ink)
            setHintTextColor(muted)
            setPadding(14, 8, 14, 8)
            setBackgroundColor(bg)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    navigate(text.toString()); true
                } else false
            }
        }
        header.addView(address, LinearLayout.LayoutParams(-1, 46).apply { topMargin = 8 })

        val nav = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        addButton(nav, "Back") { if (web.canGoBack()) web.goBack() }
        addButton(nav, "Next") { if (web.canGoForward()) web.goForward() }
        addButton(nav, "Reload") { web.reload() }
        addButton(nav, "Home") { navigate("https://www.google.com") }
        addButton(nav, "Play URL") { askVideoUrl() }
        addButton(nav, "Studio") { showAudioStudio() }
        addButton(nav, "More") { showBrowserTools() }
        header.addView(nav)
        root.addView(header)

        browserPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                settings.safeBrowsingEnabled = true
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!url.isNullOrBlank()) {
                        lastUrl = url
                        address.setText(url)
                        if (!privateMode) {
                            val prefs = getSharedPreferences("sg_browser", MODE_PRIVATE)
                            val old = prefs.getString("history", "") ?: ""
                            prefs.edit().putString("history", (url + "\n" + old).lines().distinct().take(100).joinToString("\n")).apply()
                        }
                    }
                }
            }
            webChromeClient = WebChromeClient()
            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                try {
                    val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
                    request.setMimeType(mimeType)
                    request.addRequestHeader("User-Agent", userAgent)
                    request.setTitle(android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType))
                    request.setDescription("Downloading with SG_ULTIMATE")
                    request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS,
                        android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType))
                    val manager = getSystemService(DOWNLOAD_SERVICE) as android.app.DownloadManager
                    manager.enqueue(request)
                    toast("Download started")
                } catch (_: Exception) { toast("Could not start download") }
            }
        }
        browserPanel.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(browserPanel, LinearLayout.LayoutParams(-1, 0, 1f))

        playerPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(8)
        }
        playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setControllerShowTimeoutMs(3500)
        }
        playerPanel.addView(playerView, LinearLayout.LayoutParams(-1, 0, 1f))
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER }
        addButton(controls, "−10s") { player?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) } }
        addButton(controls, "+10s") { player?.let { it.seekTo(it.currentPosition + 10_000) } }
        addButton(controls, "Speed") { chooseSpeed() }
        addButton(controls, "Quality") { chooseQuality() }
        addButton(controls, "Color") { showVideoTuning() }
        addButton(controls, "Rotate") { toggleLandscape() }
        addButton(controls, "Browser") { showBrowser() }
        playerPanel.addView(controls)
        root.addView(playerPanel, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun addButton(row: LinearLayout, label: String, action: () -> Unit) {
        row.addView(Button(this).apply {
            text = label
            textSize = 10f
            isAllCaps = false
            setTextColor(ink)
            setBackgroundColor(Color.WHITE)
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(0, 44, 1f))
    }

    private fun navigate(raw: String) {
        val value = raw.trim()
        if (value.isEmpty()) return
        val url = when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.contains(".") && !value.contains(" ") -> "https://$value"
            else -> "https://www.google.com/search?q=" + android.net.Uri.encode(value)
        }
        lastUrl = url
        address.setText(url)
        web.loadUrl(url)
        showBrowser()
    }

    private fun askVideoUrl() {
        val input = EditText(this).apply {
            hint = "Direct MP4 or HLS (.m3u8) URL"
            setText(lastUrl.takeIf { it.endsWith(".mp4") || it.contains(".m3u8") } ?: "")
            setPadding(16)
        }
        AlertDialog.Builder(this)
            .setTitle("Open video")
            .setMessage("Paste a direct video URL. Protected website streams may only play in their original website player.")
            .setView(input)
            .setPositiveButton("Play") { _, _ -> playVideo(input.text.toString().trim()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun playVideo(url: String) {
        if (url.isBlank()) return
        val normalized = if (url.startsWith("http")) url else "https://$url"
        try {
            if (player == null) {
                player = ExoPlayer.Builder(this).build().also { exo ->
                    playerView.player = exo
                    exo.addListener(object : Player.Listener {
                        override fun onAudioSessionIdChanged(audioSessionId: Int) {
                            setupAudioEffects(audioSessionId)
                        }
                    })
                }
            }
            // Effects must be configured before prepare. Apply current tuning values.
            applyVideoTuning()
            player?.setMediaItem(MediaItem.fromUri(normalized))
            player?.prepare()
            player?.playWhenReady = true
            playerPanel.visibility = View.VISIBLE
            browserPanel.visibility = View.GONE
        } catch (e: Exception) {
            toast("Unable to play this URL: ${e.message ?: "unsupported format"}")
        }
    }

    private fun chooseSpeed() {
        val labels = arrayOf("0.25×", "0.5×", "0.75×", "1×", "1.25×", "1.5×", "1.75×", "2×", "3×", "4×")
        val rates = floatArrayOf(.25f, .5f, .75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f, 4f)
        AlertDialog.Builder(this).setTitle("Playback speed")
            .setItems(labels) { _, i -> player?.playbackParameters = PlaybackParameters(rates[i]) }
            .show()
    }

    private fun chooseQuality() {
        val labels = arrayOf("Auto", "2160p / 4K max", "1440p max", "1080p max", "720p max", "480p max", "360p max")
        val heights = intArrayOf(Int.MAX_VALUE, 2160, 1440, 1080, 720, 480, 360)
        AlertDialog.Builder(this).setTitle("Maximum video resolution")
            .setItems(labels) { _, i ->
                val p = player?.trackSelectionParameters?.buildUpon()
                    ?.setMaxVideoSize(Int.MAX_VALUE, heights[i])?.build()
                if (p != null) player?.trackSelectionParameters = p
                toast("Applied when the stream provides that resolution")
            }.show()
    }

    private fun showVideoTuning() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18)
        }
        fun addTune(label: String, min: Int, max: Int, initial: Int, update: (Int) -> Unit) {
            panel.addView(TextView(this).apply {
                text = "$label: $initial"
                textSize = 13f
                setTextColor(ink)
            })
            val valueLabel = panel.getChildAt(panel.childCount - 1) as TextView
            val seek = SeekBar(this).apply {
                this.max = max - min
                progress = initial - min
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        val v = p + min
                        valueLabel.text = "$label: $v"
                        update(v)
                        applyVideoTuning()
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            panel.addView(seek)
        }
        addTune("Saturation", -100, 100, saturation.toInt()) { saturation = it.toFloat() }
        addTune("Lightness", -100, 100, lightness.toInt()) { lightness = it.toFloat() }
        addTune("Hue", -180, 180, hue.toInt()) { hue = it.toFloat() }
        addTune("Brightness", -100, 100, (brightness * 100).toInt()) { brightness = it / 100f }
        addTune("Contrast", -100, 100, (contrast * 100).toInt()) { contrast = it / 100f }
        val scroll = ScrollView(this).apply { addView(panel) }
        AlertDialog.Builder(this).setTitle("SG Video Tuning")
            .setView(scroll)
            .setNeutralButton("Reset") { _, _ ->
                saturation = 0f; lightness = 0f; hue = 0f; brightness = 0f; contrast = 0f
                applyVideoTuning()
                showVideoTuning()
            }
            .setPositiveButton("Done", null).show()
    }

    private fun applyVideoTuning() {
        val effects = mutableListOf<androidx.media3.common.Effect>()
        if (saturation != 0f || lightness != 0f || hue != 0f) {
            effects.add(HslAdjustment.Builder()
                .adjustHue(hue)
                .adjustSaturation(saturation)
                .adjustLightness(lightness)
                .build())
        }
        if (brightness != 0f) effects.add(Brightness(brightness))
        if (contrast != 0f) effects.add(Contrast(contrast))
        try {
            player?.setVideoEffects(effects)
        } catch (_: Exception) {
            toast("Video effects are unavailable for this stream or device")
        }
    }

    private fun toggleLandscape() {
        requestedOrientation =
            if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun showBrowser() {
        player?.pause()
        playerPanel.visibility = View.GONE
        browserPanel.visibility = View.VISIBLE
    }


    private fun setupAudioEffects(sessionId: Int) {
        try {
            equalizer?.release()
            bass?.release()
            virtualizer?.release()
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
            bass = BassBoost(0, sessionId).apply {
                setStrength(500)
                enabled = true
            }
            virtualizer = Virtualizer(0, sessionId).apply {
                setStrength(350)
                enabled = true
            }
        } catch (_: Exception) {
            equalizer = null
            bass = null
            virtualizer = null
            toast("This device does not support all SG audio effects")
        }
    }

    private fun showAudioStudio() {
        val eq = equalizer
        if (eq == null) {
            AlertDialog.Builder(this).setTitle("SG Audio Studio")
                .setMessage("Audio studio becomes available after the player creates an audio session. Start a compatible video first.")
                .setPositiveButton("OK", null).show()
            return
        }
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16) }
        panel.addView(TextView(this).apply { text = "Device graphic equalizer"; textSize = 17f; setTextColor(ink) })
        val range = eq.bandLevelRange
        for (i in 0 until eq.numberOfBands.toInt()) {
            val band = i.toShort()
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this).apply {
                text = "${eq.getCenterFreq(band) / 1000} kHz"
                textSize = 11f
                setTextColor(ink)
            }, LinearLayout.LayoutParams(62, -2))
            row.addView(SeekBar(this).apply {
                max = (range[1] - range[0]).toInt()
                progress = (eq.getBandLevel(band) - range[0]).toInt()
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        if (fromUser) try { eq.setBandLevel(band, (p + range[0]).toShort()) } catch (_: Exception) {}
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }, LinearLayout.LayoutParams(0, 42, 1f))
            panel.addView(row)
        }
        panel.addView(Switch(this).apply {
            text = "Bass boost"
            isChecked = bass?.enabled == true
            setOnCheckedChangeListener { _, checked -> try { bass?.enabled = checked } catch (_: Exception) {} }
        })
        panel.addView(Switch(this).apply {
            text = "Virtual surround"
            isChecked = virtualizer?.enabled == true
            setOnCheckedChangeListener { _, checked -> try { virtualizer?.enabled = checked } catch (_: Exception) {} }
        })
        AlertDialog.Builder(this).setTitle("SG Audio Studio")
            .setView(ScrollView(this).apply { addView(panel) })
            .setPositiveButton("Done", null).show()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onBackPressed() {
        when {
            playerPanel.visibility == View.VISIBLE -> showBrowser()
            web.canGoBack() -> web.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onDestroy() {
        web.destroy()
        player?.release()
        equalizer?.release()
        bass?.release()
        virtualizer?.release()
        player = null
        super.onDestroy()
    }
}
