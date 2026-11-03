package com.ananta.streamcatcher

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Capture(val url: String, val source: String, val time: String)

class MainActivity : Activity() {
    private val bg = Color.rgb(8, 9, 20)
    private val panel = Color.rgb(22, 23, 43)
    private val purple = Color.rgb(177, 74, 255)
    private val cyan = Color.rgb(0, 220, 220)
    private val white = Color.WHITE
    private val captures = mutableListOf<Capture>()
    private var selectedPackage: String? = null
    private lateinit var content: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShare(intent)
        showHome()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
        showHome()
    }

    private fun handleShare(i: Intent?) {
        if (i == null) return
        val chunks = mutableListOf<String>()
        when (i.action) {
            Intent.ACTION_SEND -> {
                i.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.let(chunks::add)
                (i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { chunks.add(it.toString()) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                i.getCharSequenceArrayListExtra(Intent.EXTRA_TEXT)?.forEach { chunks.add(it.toString()) }
                i.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.forEach { chunks.add(it.toString()) }
            }
        }
        val source = i.getStringExtra(Intent.EXTRA_TITLE) ?: "Shared from another app"
        chunks.flatMap { extractPlaylistUrls(it) }.distinct().forEach { url ->
            if (captures.none { it.url == url }) {
                captures.add(0, Capture(url, source, timestamp()))
            }
        }
    }

    private fun extractPlaylistUrls(text: String): List<String> {
        val regex = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)
        return regex.findAll(text).map { it.value.trimEnd(',', ')', ']', ';') }
            .filter { u ->
                val l = u.lowercase(Locale.ROOT)
                l.contains(".m3u8") || l.contains(".m3u") || l.contains(".mpd") ||
                    l.contains("format=m3u8") || l.contains("type=hls")
            }.toList()
    }

    private fun timestamp() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    private fun base(title: String, subtitle: String): LinearLayout {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(18, 18, 18, 20)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("◈  $title", 23, white, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("●", 18, cyan, true))
        outer.addView(header)
        outer.addView(label(subtitle, 12, Color.LTGRAY, false).apply { setPadding(2, 5, 0, 18) })
        content = outer
        return outer
    }

    private fun showHome() {
        val root = base("ANANTA STREAM CATCHER", "NETWORK INSPECTOR  •  v2.0.0")
        card(root, "CAPTURE STATUS", "Ready • Share a playlist URL from another app to import it.")
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton("SOURCE APPS") { showSources() }, LinearLayout.LayoutParams(0, 54, 1f).apply { marginEnd = 6 })
        actions.addView(actionButton("CAPTURES (${captures.size})") { showCaptures() }, LinearLayout.LayoutParams(0, 54, 1f).apply { marginStart = 6 })
        root.addView(actions)
        root.addView(space(12))
        card(root, "HOW TO CAPTURE FROM AN IPTV APP",
            "1. Open the IPTV app.\n2. Use Share / Copy link if the app provides it.\n3. Choose Ananta Stream Catcher from Android's Share menu.\n4. Review and save the detected playlist URL.")
        card(root, "IMPORTANT ANDROID LIMITATION",
            "Selecting an app does not grant access to its private network requests. HTTPS encrypts URL paths and query strings. This app does not decrypt TLS, bypass certificate pinning, or defeat DRM.")
        root.addView(actionButton("VIEW CAPTURE HISTORY") { showCaptures() })
        root.addView(space(8))
        root.addView(actionButton("EXPORT M3U") { exportM3u() })
        setContentView(root)
    }

    private fun showSources() {
        val root = base("TARGET APPS", "Choose an installed app as your preferred IPTV source")
        root.addView(actionButton("← BACK") { showHome() })
        root.addView(space(10))
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase(Locale.getDefault()) }
        if (apps.isEmpty()) {
            card(root, "NO APPS FOUND", "No launchable apps were returned by Android.")
        } else {
            apps.forEach { (pkg, name) ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(12, 10, 12, 10)
                    background = rounded(panel, 16)
                }
                val icon = try { pm.getApplicationIcon(pkg) } catch (_: Exception) { null }
                if (icon != null) row.addView(ImageView(this).apply { setImageDrawable(icon) },
                    LinearLayout.LayoutParams(42, 42).apply { marginEnd = 12 })
                val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                text.addView(label(name, 15, white, true))
                text.addView(label(pkg, 10, Color.LTGRAY, false))
                row.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(actionButton(if (selectedPackage == pkg) "SELECTED" else "SELECT") {
                    selectedPackage = pkg
                    Toast.makeText(this, "$name selected as preferred source", Toast.LENGTH_SHORT).show()
                    showSources()
                })
                root.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 })
            }
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun showCaptures() {
        val root = base("CAPTURE HISTORY", "${captures.size} imported playlist URL(s)")
        root.addView(actionButton("← BACK") { showHome() })
        root.addView(space(10))
        if (captures.isEmpty()) card(root, "NO CAPTURES YET", "Share a playlist URL to this app from an IPTV app or browser.")
        captures.forEach { c ->
            card(root, "HLS / PLAYLIST URL", "${c.url}\n\nSource: ${c.source}\nTime: ${c.time}")
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(actionButton("COPY") {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Playlist URL", c.url))
                Toast.makeText(this, "URL copied", Toast.LENGTH_SHORT).show()
            }, LinearLayout.LayoutParams(0, 48, 1f).apply { marginEnd = 5 })
            row.addView(actionButton("OPEN") {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(c.url)))
            }, LinearLayout.LayoutParams(0, 48, 1f).apply { marginStart = 5 })
            root.addView(row)
            root.addView(space(8))
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun exportM3u() {
        val body = buildString {
            append("#EXTM3U\n")
            captures.forEachIndexed { n, c ->
                append("#EXTINF:-1,Ananta Capture ${n + 1}\n")
                append(c.url).append('\n')
            }
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Ananta Stream Catcher playlist")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(Intent.createChooser(send, "Export M3U"))
    }

    private fun card(parent: LinearLayout, title: String, body: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(15, 14, 15, 14)
            background = rounded(panel, 18)
        }
        box.addView(label(title, 13, cyan, true))
        box.addView(label(body, 13, white, false).apply { setPadding(0, 8, 0, 0) })
        parent.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })
    }

    private fun actionButton(text: String, click: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 11f
        setTextColor(white)
        isAllCaps = false
        background = rounded(0xFF35204F.toInt(), 14)
        setOnClickListener { click() }
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat()
        setStroke(1, if (color == panel) 0xFF6B48A8.toInt() else purple)
    }

    private fun space(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, h) }
}
