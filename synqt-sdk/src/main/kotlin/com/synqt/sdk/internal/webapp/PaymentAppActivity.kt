package com.synqt.sdk.internal.webapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.widget.LinearLayout
import com.synqt.sdk.PaymentAppResult
import com.synqt.sdk.R
import com.synqt.sdk.internal.sheet.Closer
import com.synqt.sdk.internal.sheet.Handover
import com.synqt.sdk.internal.sheet.REQUEST
import com.synqt.sdk.internal.sheet.hairline
import com.synqt.sdk.internal.sheet.sheetHeader
import java.net.URI
import kotlinx.coroutines.CompletableDeferred

/** What [Payin.presentPaymentApp] hands the activity, kept in memory like the sheet's request. */
internal class PaymentAppRequest(val url: String, val returnUrl: String, val onPresented: () -> Unit, val result: CompletableDeferred<PaymentAppResult>) {
    val closer = Closer()
}

/**
 * The Payment Web App in a WebView under a bar with only a close button; the page carries the Synqt logo. Like the sheet, it handles rotation
 * itself (see the manifest), so the page and anything typed into it stay.
 */
internal class PaymentAppActivity : Activity() {
    private var request: PaymentAppRequest? = null
    private var web: WebView? = null
    // Android 13+: steps back through the page while it can; with none left, the system's Back closes the activity.
    private val back: Any? = if (Build.VERSION.SDK_INT >= 33) OnBackInvokedCallback { web?.goBack() } else null
    private var backRegistered = false

    @SuppressLint("SetJavaScriptEnabled") // the Payment Web App needs it
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restored after Android stopped the app: the request is gone, so close; the payin carries on.
        val request = if (savedInstanceState == null) Handover.take<PaymentAppRequest>(intent.getIntExtra(REQUEST, -1)) else null
        if (request == null) return finish()
        this.request = request
        request.closer.attach { done(PaymentAppResult.Closed) } // the caller was cancelled
        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            webViewClient = object : WebViewClient() {
                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) = updateBack()

                override fun shouldOverrideUrlLoading(view: WebView, webRequest: WebResourceRequest): Boolean = when {
                    isReturn(webRequest.url.toString(), request.returnUrl) -> { done(PaymentAppResult.Returned(webRequest.url)); true }
                    webRequest.url.scheme in setOf("http", "https", "about") -> false
                    else -> { openOutside(webRequest.url.toString()); true }
                }
            }
            loadUrl(request.url)
        }
        this.web = web
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            setBackgroundColor(getColor(R.color.synqt_surface))
            addView(sheetHeader(logo = false) { done(PaymentAppResult.Closed) })
            addView(hairline())
            addView(web, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        })
        window.decorView.post(request.onPresented)
    }

    /** Registers [back] only while the page can go back, so Android 13+ (and predictive Back) close it from the first page. */
    private fun updateBack() {
        if (Build.VERSION.SDK_INT < 33 || web?.canGoBack() == backRegistered) return
        val callback = back as OnBackInvokedCallback
        if (backRegistered) onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
        else onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
        backRegistered = !backRegistered
    }

    /** Below Android 13, and apps not opted in to the new Back: steps back through the pages, and closes from the first. */
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val web = web
        if (web != null && web.canGoBack()) web.goBack() else done(PaymentAppResult.Closed)
    }

    override fun onDestroy() {
        request?.result?.complete(PaymentAppResult.Closed) // no-op once it returned
        web?.destroy()
        super.onDestroy()
    }

    private fun done(result: PaymentAppResult) {
        request?.result?.complete(result)
        finish()
    }

    /** Bank and UPI links leave the WebView. The page controls an intent: link, so only what a browser would allow. */
    private fun openOutside(url: String) {
        // A malformed intent: link (parseUri throws) or no app for it: the payer stays on the page.
        runCatching {
            val target = if (url.startsWith("intent:")) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            startActivity(target)
        }
    }
}

/** True when [url] is the [returnUrl], whatever query the Payment Web App added; a longer path is not. */
internal fun isReturn(url: String, returnUrl: String): Boolean {
    val (uri, returnUri) = runCatching { URI(url) to URI(returnUrl) }.getOrNull() ?: return false
    return uri.scheme == returnUri.scheme && uri.host == returnUri.host && uri.port == returnUri.port && uri.path == returnUri.path
}
