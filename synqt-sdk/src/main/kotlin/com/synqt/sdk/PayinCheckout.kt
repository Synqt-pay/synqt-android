package com.synqt.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.synqt.sdk.addon.SynqtAddonApi
import com.synqt.sdk.internal.form.FieldKind
import com.synqt.sdk.internal.form.METHOD_FORMS
import com.synqt.sdk.internal.form.MethodForm
import com.synqt.sdk.internal.sheet.Closer
import com.synqt.sdk.internal.sheet.Handover
import com.synqt.sdk.internal.sheet.PaymentSheetActivity
import com.synqt.sdk.internal.sheet.PaymentSummary
import com.synqt.sdk.internal.sheet.REQUEST
import com.synqt.sdk.internal.sheet.SheetRequest
import com.synqt.sdk.internal.webapp.PaymentAppActivity
import com.synqt.sdk.internal.webapp.PaymentAppRequest
import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * One order's payin for the ready-made screens, from [Synqt.checkout]: the payment sheet and SYNQT's hosted Payment
 * Web App. To call the API yourself, use [IncomingPaymentsApi]. Calls throw [SynqtException] when SYNQT refuses them,
 * and [IOException] when it cannot be reached.
 */
public class PayinCheckout internal constructor(
    private val synqt: Synqt,
    private val environment: Synqt.Environment,
    private val token: String,
    public val id: String,
    public val money: MoneyPaymentRequested,
    private val returnUrl: String,
    private val callbackUrl: String?,
) {
    /** The API for this checkout's token, for add-ons that need more from SYNQT. */
    @SynqtAddonApi
    public val api: IncomingPaymentsApi = IncomingPaymentsApi.create(synqt, token)

    /**
     * Collects the payer's details for [paymentMethodCode] (one of [Synqt.directPaymentMethods]) in a Synqt-branded
     * bottom sheet, then creates the payin with [IncomingPaymentsApi.createPayin] and returns what SYNQT accepted. Errors, including SYNQT's field errors, stay in the
     * sheet so the payer can correct them. Returns null if the payer closes it.
     *
     * The bank or provider field lists the banks [IncomingPaymentsApi.payinAvailablePaymentOptions] returns for this payin and
     * [paymentMethodCode] (PAYIN_CREATE allows that call); an optional one is hidden when SYNQT lists none. The summary
     * names the merchant when the token also allows `GET /merchant-info`. [prefill] is keyed
     * by field path, such as `emailAddress` or `customerName.lastName`. CARD is offered only with SYNQT's card add-on,
     * for PCI DSS compliant merchants.
     *
     * [onPresented] runs once the sheet is on screen, and [onPaymentStarted] each time a payment is sent to SYNQT (the
     * details passed the sheet's checks; SYNQT may still refuse them, and then there may be no payin). Both run on the
     * main thread. The call returns once the sheet has closed.
     *
     * The sheet is its own activity and survives rotation itself. Cancelling the call closes it, as on iOS, so call
     * this from a scope that outlives your activity being rebuilt, such as `viewModelScope`: from `lifecycleScope`, a
     * rotation (unless your activity handles it) cancels the call and closes the sheet. If Android stops your app
     * while the payer is in another app, the sheet closes; the payin carries on, so check [IncomingPaymentsApi.getPayin] when they return.
     *
     * The sheet needs Android 8.0 (API 26) or later; check `Build.VERSION.SDK_INT` first and offer [presentPaymentApp]
     * on older phones.
     *
     * @throws IllegalStateException below Android 8.0, before anything is shown.
     */
    public suspend fun presentPaymentSheet(
        activity: Activity,
        paymentMethodCode: String,
        prefill: Map<String, String> = emptyMap(),
        customerIp: String? = null,
        onPresented: () -> Unit = {},
        onPaymentStarted: () -> Unit = {},
    ): PayinAcceptedResponse? {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { "presentPaymentSheet needs Android 8.0 (API 26) or later; use presentPaymentApp below that" }
        val addon = Synqt.addonMethods.find { it.code == paymentMethodCode }
        // An add-on's method draws its own fields, so the sheet gets an empty form for it.
        val form = requireNotNull(METHOD_FORMS.find { it.code == paymentMethodCode } ?: addon?.let { MethodForm(it.code, it.name, emptyList()) { error("built by the add-on") } }) {
            "Unknown paymentMethodCode $paymentMethodCode"
        }
        val result = CompletableDeferred<PayinAcceptedResponse?>()
        val request = SheetRequest(this, form, addon, prefill, customerIp, onPresented, onPaymentStarted, result)
        return show(activity, Intent(activity, PaymentSheetActivity::class.java), request.closer, request, result)
    }

    /** Starts [intent]'s SDK activity for [request] and waits for [result]; cancelling closes the activity. */
    private suspend fun <T> show(activity: Activity, intent: Intent, closer: Closer, request: Any, result: CompletableDeferred<T>): T {
        withContext(Dispatchers.Main) { activity.startActivity(intent.putExtra(REQUEST, Handover.put(request))) }
        return try {
            result.await()
        } catch (error: CancellationException) {
            closer.close()
            throw error
        }
    }

    /**
     * The banks for [form]'s bank or provider field (null if they could not be loaded), and the order summary. The
     * merchant name is a nicety: without it the summary leaves that row out.
     */
    internal suspend fun sheetContext(form: MethodForm, context: Context): Pair<List<PaymentOperatorIncoming>?, PaymentSummary> {
        val (operators, merchant) = coroutineScope {
            val operators = async {
                if (form.fields.none { it.kind == FieldKind.OPERATOR || it.kind == FieldKind.PROVIDER }) emptyList() else sheetOperators(form)
            }
            val merchant = async { orNull { JSONObject(synqt.call("GET", "/merchant-info", token)).optJSONObject("channelInfo")?.optString("name") } }
            operators.await() to merchant.await()?.ifEmpty { null }
        }
        return operators to PaymentSummary(money.format(), merchant, context.getString(form.name), id)
    }

    /** SYNQT's banks and providers for [form], empty if it lists none, or null if they could not be loaded (see [retrying]). */
    internal suspend fun sheetOperators(form: MethodForm, retryAfterMs: Long = 2_000): List<PaymentOperatorIncoming>? = retrying(retryAfterMs) {
        api.payinAvailablePaymentOptions(PaymentRequested(money)).data.orEmpty().firstOrNull { it.paymentMethodCode == form.code }?.paymentOperators.orEmpty()
    }

    /** What the sheet sends: this checkout's payin with the payer's [paymentMethod]. */
    internal suspend fun createPayin(paymentMethod: PaymentMethodPayin, customerIp: String?): PayinAcceptedResponse =
        api.createPayin(id, Payin(PaymentRequested(money), paymentMethod, returnUrl, callbackUrl, customerIp))

    /** Link to SYNQT's hosted Payment Web App for this payin. [extra] takes optional params such as `emailAddress`. */
    public fun paymentAppUrl(merchantCode: String, extra: Map<String, String> = emptyMap()): String {
        val params = linkedMapOf(
            "merchantCode" to merchantCode, "idPayin" to id, "token" to token,
            "amount" to money.amount.toPlainString(), "currencyCode" to money.currencyCode,
        ) + listOfNotNull(callbackUrl?.let { "callbackUrl" to it }) + ("returnUrl" to returnUrl) + extra
        return environment.paymentAppBaseUrl + "?" + params.entries.joinToString("&") { (key, value) -> "${enc(key)}=${enc(value)}" }
    }

    /**
     * Shows SYNQT's hosted Payment Web App inside the app, over [activity], and returns once the payer reaches the
     * returnUrl or closes it. [onPresented] runs once it is on screen. Links to other apps (bank apps, UPI) open outside
     * it. Either way, ask [IncomingPaymentsApi.getPayin] where the payment stands. Like the sheet, it survives rotation itself, and cancelling
     * the call closes it, as on iOS.
     */
    public suspend fun presentPaymentApp(
        activity: Activity,
        merchantCode: String,
        extra: Map<String, String> = emptyMap(),
        onPresented: () -> Unit = {},
    ): PaymentAppResult {
        val result = CompletableDeferred<PaymentAppResult>()
        val request = PaymentAppRequest(paymentAppUrl(merchantCode, extra), returnUrl, onPresented, result)
        return show(activity, Intent(activity, PaymentAppActivity::class.java), request.closer, request, result)
    }

    /** Opens [paymentAppUrl] in the browser. Use a verified Android App Link as the returnUrl to come back to your app. To know when the payer is done in the app itself, use [presentPaymentApp]. */
    public fun openPaymentApp(context: Context, merchantCode: String, extra: Map<String, String> = emptyMap()) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(paymentAppUrl(merchantCode, extra))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun enc(text: String) = URLEncoder.encode(text, "UTF-8")

    private inline fun <T> orNull(block: () -> T): T? = try {
        block()
    } catch (error: IOException) {
        null
    } catch (error: JSONException) {
        null
    }
}

/**
 * [load], tried twice more [delayMs] apart after a failed connection, an unreadable answer or a SYNQT server error;
 * null if it still fails or SYNQT refuses it (4xx, not retried). Cancellation stops it.
 */
internal suspend fun <T> retrying(delayMs: Long = 2_000, load: suspend () -> T): T? {
    repeat(3) { attempt ->
        if (attempt > 0) delay(delayMs)
        try {
            return load()
        } catch (refused: SynqtException) {
            if (refused.httpCode < 500) return null
        } catch (error: IOException) {
            // tried again
        } catch (error: JSONException) {
            // tried again
        }
    }
    return null
}
