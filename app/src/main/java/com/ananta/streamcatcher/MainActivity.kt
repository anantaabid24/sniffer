package com.ananta.streamcatcher

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import java.net.URLDecoder
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var address: EditText
    private lateinit var results: LinearLayout
    private lateinit var web: WebView
    private lateinit var countLabel: TextView
    private lateinit var browserPanel: LinearLayout
    private val links = linkedMapOf<String, Long>()
    private val prefs by lazy { getSharedPreferences("captures", Context.MODE_PRIVATE) }

    private val bg = Color.rgb(8, 9, 20)
    private val panel = Color.rgb(20, 22, 42)
    private val cyan = Color.rgb(75, 225, 245)
    private val purple = Color.rgb(166, 119, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        loadSaved()
        buildInterface()
        handleIncomingIntent(intent)
        renderResults()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun buildInterface() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(24))
        }
        scroll.addView(content)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(label("ANANTA", 12f, cyan, true))
        brand.addView(label("STREAM CATCHER", 21f, Color.WHITE, true))
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(pill("LIVE SCANNER", cyan))
        content.addView(top)
        content.addView(label("HLS STREAM INSPECTOR  •  BROWSER + SHARE", 10f, Color.rgb(157, 162, 190), false).apply {
            setPadding(0, dp(5), 0, dp(16))
        })

        val hero = glassCard()
        hero.addView(label("Find your stream.", 23f, Color.WHITE, true))
        hero.addView(label("Detect playlist requests from pages you browse, then inspect, copy or test the links you are allowed to access.", 12f, Color.rgb(190, 195, 220), false).apply {
            setPadding(0, dp(5), 0, dp(12))
        })
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        stats.addView(label("●  Browser monitor ready", 11f, cyan, true), LinearLayout.LayoutParams(0, -2, 1f))
        countLabel = label("${links.size} FOUND", 11f, Color.WHITE, true)
        stats.addView(countLabel)
        hero.addView(stats)
        content.addView(hero, marginParams(bottom = 14))

        val browserCard = glassCard()
        browserCard.addView(sectionTitle("BROWSER CAPTURE", "01"))
        address = EditText(this).apply {
            hint = "https://website.example/live"
            setSingleLine(true)
            textSize = 13f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(125, 130, 157))
            setPadding(dp(13), dp(11), dp(13), dp(11))
            background = rounded(Color.rgb(11, 14, 30), 14, Color.rgb(57, 62, 91), 1)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        browserCard.addView(address, marginParams(top = 10, bottom = 9))
        val browserActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        browserActions.addView(action("OPEN PAGE", true) { openWebsite() }, LinearLayout.LayoutParams(0, dp(44), 1.2f))
        browserActions.addView(action("PASTE", false) { pasteAndOpen() }, LinearLayout.LayoutParams(0, dp(44), 0.8f).apply { leftMargin = dp(8) })
        browserCard.addView(browserActions)
        browserPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        web = WebView(this).apply {
            setBackgroundColor(Color.rgb(11, 13, 27))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.safeBrowsingEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            addJavascriptInterface(PageResourceBridge(), "AnantaCapture")
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): android.webkit.WebResourceResponse? {
                    val raw = request?.url?.toString() ?: return null
                    extractPlaylistCandidates(raw).forEach { candidate -> runOnUiThread { addCapture(candidate) } }
                    return null
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectResourceObserver()
                }
            }
        }
        browserPanel.addView(web, LinearLayout.LayoutParams(-1, dp(255)))
        browserCard.addView(browserPanel, marginParams(top = 12))
        val browserToggle = action("HIDE BROWSER VIEW", false) {
            val visible = web.visibility == View.VISIBLE
            web.visibility = if (visible) View.GONE else View.VISIBLE
            (it as Button).text = if (visible) "SHOW BROWSER VIEW" else "HIDE BROWSER VIEW"
        }
        browserCard.addView(browserToggle, marginParams(top = 8))
        content.addView(browserCard, marginParams(bottom = 14))

        val shareCard = glassCard()
        shareCard.addView(sectionTitle("CAPTURE FROM OTHER APPS", "02"))
        shareCard.addView(label("Use Android Share or Copy Link in another app, then choose Ananta Stream Catcher. We can identify M3U8 URLs included in the shared text; encrypted traffic inside another app is not readable.", 12f, Color.rgb(185, 190, 215), false).apply {
            setPadding(0, dp(8), 0, dp(10))
        })
        shareCard.addView(action("HOW CROSS-APP SHARE WORKS", false) {
            AlertDialog.Builder(this)
                .setTitle("Capture from another app")
                .setMessage("1. In the source app, use Share or Copy Link.\n2. Choose Ananta Stream Catcher or paste the copied text here.\n3. If the shared text contains a playlist URL, it will appear below.\n\nAndroid isolates apps and encrypts HTTPS traffic. A normal app cannot inspect every other app's private requests or bypass DRM/certificate pinning.")
                .setPositiveButton("GOT IT", null).show()
        })
        shareCard.addView(action("SELECT SOURCE APP", true) { showSourceAppSelector() }, marginParams(top = 10))
        content.addView(shareCard, marginParams(bottom = 14))

        val captureCard = glassCard()
        val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        heading.addView(sectionTitle("DETECTED PLAYLISTS", "03"), LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(action("EXPORT M3U", false) { exportPlaylist() })
        captureCard.addView(heading)
        captureCard.addView(label("Only candidate playlist URLs are listed. A detected URL may still require a valid session or authorization to play.", 11f, Color.rgb(150, 157, 188), false).apply {
            setPadding(0, dp(5), 0, dp(10))
        })
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        captureCard.addView(results)
        val footerActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        footerActions.addView(action("CLEAR HISTORY", false) { clearResults() }, LinearLayout.LayoutParams(0, dp(42), 1f))
        footerActions.addView(action("PASTE LINK", false) { pasteAndOpen() }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(8) })
        captureCard.addView(footerActions, marginParams(top = 8))
        content.addView(captureCard)

        page.addView(label("ANANTA STREAM CATCHER  •  PRIVATE LOCAL CAPTURE", 9f, Color.rgb(105, 111, 143), false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
        })
        setContentView(page)
    }

    private inner class PageResourceBridge {
        @JavascriptInterface fun onResource(url: String) {
            extractPlaylistCandidates(url).forEach { candidate -> runOnUiThread { addCapture(candidate) } }
        }
    }

    private fun injectResourceObserver() {
        val js = """(function(){if(window.__anantaObserver)return;window.__anantaObserver=true;var seen={};function scan(){try{performance.getEntriesByType('resource').forEach(function(e){var u=e.name;if(u&&!seen[u]){seen[u]=1;window.AnantaCapture.onResource(u);}});}catch(e){}}scan();setInterval(scan,1200);var f=window.fetch;window.fetch=function(){try{var u=arguments[0]&&arguments[0].url?arguments[0].url:arguments[0];if(u)window.AnantaCapture.onResource(String(u));}catch(e){}return f.apply(this,arguments);};var o=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(m,u){try{if(u)window.AnantaCapture.onResource(String(u));}catch(e){}return o.apply(this,arguments);};})();"""
        web.evaluateJavascript(js, null)
    }

    private fun glassCard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(15), dp(15), dp(15))
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(30, 31, 59), Color.rgb(16, 21, 42), Color.rgb(19, 16, 39))).apply {
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), Color.rgb(65, 70, 111))
        }
        elevation = dp(3).toFloat()
    }

    private fun sectionTitle(title: String, number: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(label(number, 10f, cyan, true).apply {
            background = rounded(Color.rgb(30, 54, 77), 7, Color.rgb(55, 101, 127), 1)
            setPadding(dp(7), dp(5), dp(7), dp(5))
        })
        addView(label(title, 12f, Color.WHITE, true).apply { setPadding(dp(9), 0, 0, 0) })
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private fun pill(text: String, color: Int) = label(text, 9f, color, true).apply {
        background = rounded(Color.rgb(24, 48, 62), 30, Color.rgb(57, 111, 131), 1)
        setPadding(dp(11), dp(7), dp(11), dp(7))
    }

    private fun action(text: String, primary: Boolean, callback: (View) -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 10f
        isAllCaps = false
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setTextColor(if (primary) Color.rgb(8, 16, 29) else Color.WHITE)
        background = if (primary) GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.rgb(79, 225, 242), Color.rgb(160, 126, 255))).apply { cornerRadius = dp(13).toFloat() }
            else rounded(Color.rgb(39, 39, 70), 13, Color.rgb(82, 83, 128), 1)
        setPadding(dp(8), 0, dp(8), 0)
        setOnClickListener { callback(it) }
    }

    private fun rounded(color: Int, radius: Int, stroke: Int, strokeWidth: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(strokeWidth), stroke)
    }

    private fun marginParams(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply {
        if (top > 0) topMargin = dp(top)
        if (bottom > 0) bottomMargin = dp(bottom)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun openWebsite() {
        var url = address.text.toString().trim()
        if (url.isBlank()) return
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) url = "https://$url"
        val uri = Uri.parse(url)
        if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) {
            toast("Enter a valid HTTP or HTTPS URL"); return
        }
        address.setText(url)
        web.visibility = View.VISIBLE
        web.loadUrl(url)
    }

    private fun pasteAndOpen() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val value = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (value.isBlank()) { toast("Clipboard is empty"); return }
        handleSharedText(value)
    }

    private fun handleIncomingIntent(incoming: Intent?) {
        if (incoming == null) return
        when (incoming.action) {
            Intent.ACTION_SEND -> {
                val text = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                    ?: incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.toString()
                if (!text.isNullOrBlank()) handleSharedText(text)
                else toast("No text or URL was included in the shared item")
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val streams = incoming.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                val combined = streams.orEmpty().joinToString("\n") { it.toString() }
                if (combined.isNotBlank()) handleSharedText(combined) else toast("No shared URLs found")
            }
            Intent.ACTION_VIEW -> incoming.dataString?.let { handleSharedText(it) }
        }
    }

    private fun showSourceAppSelector() {
        val pm = packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.ROOT) }
        if (apps.isEmpty()) { toast("No launchable apps found"); return }
        val labels = apps.map { "${it.loadLabel(pm)}  ·  ${it.activityInfo.packageName}" }.toTypedArray()
        val saved = prefs.getString("preferred_source_package", "").orEmpty()
        val selected = apps.indexOfFirst { it.activityInfo.packageName == saved }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Choose source app")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                val info = apps[which].activityInfo
                prefs.edit().putString("preferred_source_package", info.packageName)
                    .putString("preferred_source_label", apps[which].loadLabel(pm).toString()).apply()
                toast("Preferred source: ${apps[which].loadLabel(pm)}")
                dialog.dismiss()
            }
            .setNeutralButton("CLEAR SELECTION") { dialog, _ ->
                prefs.edit().remove("preferred_source_package").remove("preferred_source_label").apply()
                toast("Source selection cleared")
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("INFO", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("About source selection")
                            .setMessage("Selecting an app only saves it as your preferred source for sharing/import workflows. Android does not grant access to that app's private traffic. HTTPS/TLS encryption and DRM-protected streams cannot be read by this app.")
                            .setPositiveButton("OK", null).show()
                    }
                }
            }.show()
    }

    private fun handleSharedText(text: String) {
        val candidates = extractPlaylistCandidates(text)
        if (candidates.isNotEmpty()) {
            candidates.forEach { addCapture(it) }
            toast("Found ${candidates.size} playlist link(s)"); return
        }
        val url = Regex("https?://[^\\s<>\\\"']+", RegexOption.IGNORE_CASE).find(text)?.value
        if (url != null) {
            address.setText(url.trimEnd('.', ',', ')', ']'))
            web.visibility = View.VISIBLE
            web.loadUrl(address.text.toString())
            toast("Opened shared URL in browser")
        } else toast("No usable URL found in shared text")
    }

    private fun extractPlaylistCandidates(raw: String): Set<String> {
        val out = linkedSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(raw.trim())
        repeat(4) {
            val batch = queue.toList(); queue.clear()
            for (candidate in batch) {
                val cleaned = candidate.trim().trim('"', '\'', '<', '>', '(', ')', '[', ']', ',')
                if (isPlaylistUrl(cleaned)) out.add(cleaned)
                val decoded = runCatching { URLDecoder.decode(cleaned, "UTF-8") }.getOrDefault(cleaned)
                if (decoded != cleaned) queue.add(decoded)
                val uri = runCatching { Uri.parse(cleaned) }.getOrNull()
                uri?.queryParameterNames?.forEach { key ->
                    val value = runCatching { uri.getQueryParameter(key) }.getOrNull() ?: return@forEach
                    if (value.startsWith("http", true)) queue.add(value)
                }
                Regex("https?://[^\\s<>\\\"']+", RegexOption.IGNORE_CASE).findAll(decoded)
                    .forEach { queue.add(it.value.trimEnd('.', ',', ')', ']')) }
            }
        }
        return out
    }

    private fun isPlaylistUrl(raw: String): Boolean {
        if (!raw.startsWith("http://", true) && !raw.startsWith("https://", true)) return false
        val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
        val uri = runCatching { Uri.parse(decoded) }.getOrNull() ?: return false
        val path = uri.path.orEmpty().lowercase(Locale.ROOT)
        val full = decoded.lowercase(Locale.ROOT)
        return path.endsWith(".m3u8") || path.endsWith(".m3u") ||
            full.contains(".m3u8%3f") || full.contains("application/vnd.apple.mpegurl") ||
            full.contains("application/x-mpegurl")
    }

    private fun addCapture(url: String) {
        if (links.containsKey(url)) return
        links[url] = System.currentTimeMillis()
        saveAll(); renderResults()
        toast("M3U8 candidate detected")
    }

    private fun renderResults() {
        if (!::results.isInitialized) return
        results.removeAllViews()
        if (::countLabel.isInitialized) countLabel.text = "${links.size} FOUND"
        if (links.isEmpty()) {
            results.addView(label("No playlist candidates yet. Open a page that you are authorized to inspect, or share a playlist URL into this app.", 12f, Color.rgb(160, 166, 193), false).apply {
                setPadding(dp(10), dp(15), dp(10), dp(15))
            })
            return
        }
        links.entries.toList().asReversed().forEachIndexed { index, entry ->
            val url = entry.key
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(11), dp(12), dp(10))
                background = rounded(Color.rgb(12, 17, 34), 15, Color.rgb(58, 69, 105), 1)
            }
            val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            top.addView(pill("HLS  •  ${index + 1}", cyan))
            top.addView(label(Uri.parse(url).host.orEmpty(), 10f, Color.rgb(157, 164, 196), true).apply {
                setPadding(dp(8), 0, 0, 0); maxLines = 1
            }, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(top)
            card.addView(label(url, 11f, Color.WHITE, false).apply {
                setPadding(0, dp(9), 0, dp(9)); setTextIsSelectable(true); maxLines = 4
            })
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(action("COPY", false) { copy(url) }, LinearLayout.LayoutParams(0, dp(38), 1f))
            actions.addView(action("PLAY TEST", true) { testStream(url) }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(7) })
            actions.addView(action("REMOVE", false) { links.remove(url); saveAll(); renderResults() }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(7) })
            card.addView(actions)
            results.addView(card, marginParams(bottom = 9))
        }
    }

    private fun testStream(url: String) {
        val playerView = PlayerView(this)
        val player = ExoPlayer.Builder(this).build()
        playerView.player = player
        val dialog = AlertDialog.Builder(this).setTitle("Stream test").setView(playerView)
            .setNegativeButton("CLOSE", null).create()
        dialog.setOnDismissListener { player.release() }
        dialog.show()
        player.setMediaItem(MediaItem.fromUri(url))
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { toast("Playback failed: ${error.errorCodeName}") }
        })
        player.prepare(); player.playWhenReady = true
    }

    private fun copy(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("M3U8 URL", value))
        toast("Link copied")
    }

    private fun clearResults() { links.clear(); saveAll(); renderResults() }

    private fun exportPlaylist() {
        if (links.isEmpty()) { toast("No playlist links detected yet"); return }
        val body = links.keys.joinToString("\n", prefix = "#EXTM3U\n") { "#EXTINF:-1,Detected stream\n$it" }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "audio/x-mpegurl"
            putExtra(Intent.EXTRA_SUBJECT, "Ananta detected streams.m3u")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(Intent.createChooser(send, "Export M3U playlist"))
    }

    private fun loadSaved() {
        val array = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrDefault(JSONArray())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val url = item.optString("url")
            if (url.isNotBlank()) links[url] = item.optLong("time")
        }
    }

    private fun saveAll() {
        val array = JSONArray()
        links.forEach { (url, time) -> array.put(org.json.JSONObject().put("url", url).put("time", time)) }
        prefs.edit().putString("items", array.toString()).apply()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() { if (::web.isInitialized) web.destroy(); super.onDestroy() }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() { if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed() }
}
