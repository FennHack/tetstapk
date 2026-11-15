package id.pijarsekolah.sman8

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val PORTAL_URL = "https://portal.pijarsekolah.id/sman8yogyakarta"
        const val PORTAL_HOST = "pijarsekolah.id"
        const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36"
        const val REQ_FILE = 1001
    }

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnDesktop: ImageButton

    private var mobileUA: String = ""
    private var desktopMode = false
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enterFullScreen()

        webView = findViewById(R.id.webView)
        progress = findViewById(R.id.progress)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnDesktop = findViewById(R.id.btnDesktop)

        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.loadsImagesAutomatically = true
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        mobileUA = s.userAgentString

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val host = r.url.host ?: return false
                if (host.endsWith(PORTAL_HOST)) return false
                startActivity(Intent(Intent.ACTION_VIEW, r.url))
                return true
            }

            override fun onPageStarted(v: WebView, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(v: WebView, url: String?) {
                progress.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
            }

            override fun onShowFileChooser(
                w: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    startActivityForResult(params.createIntent(), REQ_FILE)
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }
        }

        webView.setDownloadListener { url, userAgent, disposition, mime, _ ->
            val req = DownloadManager.Request(Uri.parse(url))
            req.setMimeType(mime)
            req.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
            req.addRequestHeader("User-Agent", userAgent)
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            req.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(url, disposition, mime)
            )
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            Toast.makeText(this, "Mengunduh file…", Toast.LENGTH_SHORT).show()
        }

        // Pojok kiri atas: refresh
        btnRefresh.setOnClickListener { webView.reload() }

        // Pojok kanan atas: mode desktop
        btnDesktop.setOnClickListener { setDesktopMode(!desktopMode) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(PORTAL_URL)
        }
    }

    private fun setDesktopMode(on: Boolean) {
        desktopMode = on
        val s = webView.settings
        s.userAgentString = if (on) DESKTOP_UA else mobileUA
        s.useWideViewPort = on
        s.loadWithOverviewMode = on
        btnDesktop.imageTintList = ColorStateList.valueOf(
            if (on) 0xFF51C8EC.toInt() else 0xFFFFFFFF.toInt()
        )
        Toast.makeText(
            this,
            if (on) "Mode desktop: aktif" else "Mode desktop: nonaktif",
            Toast.LENGTH_SHORT
        ).show()
        webView.reload()
    }

    private fun enterFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.hide(WindowInsetsCompat.Type.systemBars())
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterFullScreen()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_FILE) {
            fileCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            )
            fileCallback = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
