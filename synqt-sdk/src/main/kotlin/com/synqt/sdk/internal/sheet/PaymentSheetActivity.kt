package com.synqt.sdk.internal.sheet

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.synqt.sdk.CreatePayinResult
import com.synqt.sdk.Payin
import com.synqt.sdk.PaymentMethod
import com.synqt.sdk.PaymentOperator
import com.synqt.sdk.addon.SheetMethod
import com.synqt.sdk.internal.form.MethodForm
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hosts the payment sheet, so it survives rotation whatever the app's own activity does. It handles size,
 * orientation and appearance changes itself (see the manifest) instead of being rebuilt, so the payer's details and
 * the next step stay on screen. Transparent: only the sheet's dialog shows.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class PaymentSheetActivity : Activity() {
    private val scope = MainScope()
    private var request: SheetRequest? = null
    private var setup: Pair<List<PaymentOperator>, PaymentSummary>? = null
    private var sheet: PaymentMethodSheet? = null
    private lateinit var shown: Configuration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A saved state means Android rebuilt or restored this activity (after stopping the app, say), and the
        // request died with the old one, so close; the payin carries on and the app checks its status.
        val request = if (savedInstanceState == null) Handover.take<SheetRequest>(intent.getIntExtra(REQUEST, -1)) else null
        if (request == null) return finish()
        this.request = request
        // No screenshots, screen recording or recents thumbnail of what an add-on marks as secret, such as card details.
        if (request.addon?.secure == true) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        shown = Configuration(resources.configuration)
        request.closer.attach { sheet?.close() ?: finish() } // the caller was cancelled
        scope.launch {
            setup = request.payin.sheetContext(request.form, this@PaymentSheetActivity)
            sheet = open(null).also { it.show() }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val old = sheet ?: return
        // Colours, text and sizes are set when the views are made, so a light or dark, font size or language switch
        // rebuilds the sheet where the payer left it. Not while a payment is being sent: that would drop its result;
        // the next change tries again.
        if (shown.diff(newConfig) and APPEARANCE != 0 && old.canRebuild) {
            shown = Configuration(newConfig)
            sheet = open(old.detach()).also { it.show() }
        } else {
            old.fitWindow()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        sheet?.close()
        request?.result?.complete(null) // closed before the sheet showed; no-op once it has a result
        super.onDestroy()
    }

    private fun open(restore: SheetState?): PaymentMethodSheet {
        val sheetRequest = checkNotNull(request)
        val (operators, summary) = checkNotNull(setup)
        return PaymentMethodSheet(
            this, sheetRequest.form, sheetRequest.prefill, operators, summary, sheetRequest.result, restore,
            onPresented = if (restore == null) sheetRequest.onPresented else ({}), onPaymentStarted = sheetRequest.onPaymentStarted,
            addon = sheetRequest.addon?.let { addon -> SheetAddon(addon.secure) { context, state -> addon.section(context, sheetRequest.payin, sheetRequest.prefill, state) } },
        ) { method -> sheetRequest.payin.create(method, sheetRequest.customerIp) }
    }

    private companion object {
        const val APPEARANCE = ActivityInfo.CONFIG_UI_MODE or ActivityInfo.CONFIG_FONT_SCALE or
            ActivityInfo.CONFIG_LOCALE or ActivityInfo.CONFIG_DENSITY or ActivityInfo.CONFIG_LAYOUT_DIRECTION
    }
}

/** What [Payin.presentPaymentSheet] hands the sheet. Kept in memory, as the token and callbacks can't go in an Intent. */
internal class SheetRequest(
    val payin: Payin,
    val form: MethodForm,
    val addon: SheetMethod?,
    val prefill: Map<String, String>,
    val customerIp: String?,
    val onPresented: () -> Unit,
    val onPaymentStarted: () -> Unit,
    val result: CompletableDeferred<CreatePayinResult?>,
) {
    val closer = Closer()
}

/** The Intent extra holding a request's [Handover] key. */
internal const val REQUEST = "com.synqt.sdk.REQUEST"

/**
 * Lets a cancelled caller close the SDK activity showing its request, as on iOS, even if that activity has not
 * started yet. Main thread only, so the two can't race.
 */
internal class Closer {
    private var close: (() -> Unit)? = null
    private var closed = false

    /** Called by the activity once it shows the request. */
    fun attach(action: () -> Unit) {
        if (closed) action() else close = action
    }

    fun close() {
        Handler(Looper.getMainLooper()).post {
            closed = true
            close?.invoke()
        }
    }
}

/** Requests waiting for the SDK activity that shows them, by the key in its Intent. */
internal object Handover {
    private val pending = mutableMapOf<Int, Any>()
    private var next = 0

    fun put(request: Any): Int = synchronized(pending) { (++next).also { pending[it] = request } }

    /** Once only, so a restored activity can't pick up a request that is already showing. */
    inline fun <reified T> take(key: Int): T? = takeAny(key) as? T

    fun takeAny(key: Int): Any? = synchronized(pending) { pending.remove(key) }
}
