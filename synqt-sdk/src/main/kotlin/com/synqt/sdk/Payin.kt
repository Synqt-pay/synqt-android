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
import com.synqt.sdk.internal.parseCreatePayin
import com.synqt.sdk.internal.parsePayinStatus
import com.synqt.sdk.internal.parsePaymentOptions
import com.synqt.sdk.internal.requireSameMethod
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
 * One order's payin, from [Synqt.payin]. Holds the token, ID, amount and URLs so each call needs only what is new.
 * Calls throw [SynqtException] when SYNQT refuses them, and [IOException] when it cannot be reached.
 */
public class Payin internal constructor(
    private val synqt: Synqt,
    private val environment: Synqt.Environment,
    private val token: String,
    public val id: String,
    public val money: Money,
    private val returnUrl: String,
    private val callbackUrl: String,
) {
    /**
     * Collects the payer's details for [paymentMethodCode] (one of [Synqt.directPaymentMethods]) in a Synqt-branded
     * bottom sheet, then creates the payin as [create] does. Errors, including SYNQT's field errors, stay in the
     * sheet so the payer can correct them. Returns null if the payer closes it.
     *
     * The bank or provider field lists the banks [availablePaymentOptions] returns for this payin and
     * [paymentMethodCode] (PAYIN_CREATE allows that call); an optional one is hidden when SYNQT lists none. The summary
     * names the merchant when the token also allows `GET /merchant-info`. [prefill] is keyed
     * by field path, such as `emailAddress` or `customerName.lastName`. CARD is offered only with SYNQT's card add-on,
     * for PCI DSS compliant merchants.
     *
     * [onPresented] runs once the sheet is on screen, and [onPaymentStarted] each time a payment is sent to SYNQT (the
     * details passed the sheet's checks; SYNQT may still refuse them, and then there may be no payin). Both run on the
     * main thread. The call returns once the sheet has closed.
     *
     * The sheet is its own activity and survives rotation itself. Cancelling the call closes it, so call
     * this from a scope that outlives your activity being rebuilt, such as `viewModelScope`: from `lifecycleScope`, a
     * rotation (unless your activity handles it) cancels the call and closes the sheet. If Android stops your app
     * while the payer is in another app, the sheet closes; the payin carries on, so check [status] when they return.
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
    ): CreatePayinResult? {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { "presentPaymentSheet needs Android 8.0 (API 26) or later; use presentPaymentApp below that" }
        val addon = Synqt.addonMethods.find { it.code == paymentMethodCode }
        // An add-on's method draws its own fields, so the sheet gets an empty form for it.
        val form = requireNotNull(METHOD_FORMS.find { it.code == paymentMethodCode } ?: addon?.let { MethodForm(it.code, it.name, emptyList()) { error("built by the add-on") } }) {
            "Unknown paymentMethodCode $paymentMethodCode"
        }
        val result = CompletableDeferred<CreatePayinResult?>()
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
    internal suspend fun sheetContext(form: MethodForm, context: Context): Pair<List<PaymentOperator>?, PaymentSummary> {
        val (operators, merchant) = coroutineScope {
            val operators = async {
                if (form.fields.none { it.kind == FieldKind.OPERATOR || it.kind == FieldKind.PROVIDER }) emptyList() else sheetOperators(form)
            }
            val merchant = async { orNull { JSONObject(synqt.call("GET", "/merchant-info", token)).optJSONObject("channelInfo")?.optString("name") } }
            operators.await() to merchant.await()?.ifEmpty { null }
        }
        return operators to PaymentSummary(money.toString(), merchant, context.getString(form.name), id)
    }

    /** SYNQT's banks and providers for [form], empty if it lists none, or null if they could not be loaded (see [retrying]). */
    internal suspend fun sheetOperators(form: MethodForm, retryAfterMs: Long = 2_000): List<PaymentOperator>? =
        retrying(retryAfterMs) { availablePaymentOptions().firstOrNull { it.code == form.code }?.operators.orEmpty() }

    /**
     * Creates the payin with a payment method you built yourself. [customerIp] is checked against the merchant's
     * IP allow and deny lists, so pass the payer's real IP if you have it. A retry after a network failure is safe:
     * if SYNQT already accepted this payin ID, the existing payin is fetched instead. If that payin was made with a
     * different method, this throws [SynqtException] (409, PAYIN_ALREADY_STARTED): start a new payin to pay another way.
     */
    public suspend fun create(paymentMethod: PaymentMethod, customerIp: String? = null): CreatePayinResult =
        create(paymentMethod.toJson(), customerIp)

    /** [create] with a `paymentMethod` object an add-on built, for methods this SDK has no type for. */
    @SynqtAddonApi
    public suspend fun create(paymentMethod: JSONObject, customerIp: String? = null): CreatePayinResult {
        val body = JSONObject()
            .put("paymentRequested", JSONObject().put("money", money.toJson()))
            .put("paymentMethod", paymentMethod)
            .put("returnUrl", returnUrl)
            .put("callbackUrl", callbackUrl)
            .putOpt("customerIp", customerIp)
        val json = try {
            synqt.call("POST", "/payins/$id", token, body)
        } catch (error: SynqtException) {
            if (error.httpCode != 409) throw error
            synqt.call("GET", "/payins/$id", token).also { existing -> requireSameMethod(existing, paymentMethod.getString("paymentMethodCode")) }
        }
        return parseCreatePayin(json)
    }

    /** GET [path] with this payin's token, for add-ons that need more from SYNQT, such as the card encryption key. */
    @SynqtAddonApi
    public suspend fun get(path: String): JSONObject = JSONObject(synqt.call("GET", path, token))

    /** Methods, banks and providers SYNQT offers this merchant for [money]. Send the same [segmentCode] when paying. */
    public suspend fun availablePaymentOptions(segmentCode: String? = null): List<PaymentOption> {
        val body = JSONObject().put("money", money.toJson()).putOpt("segmentCode", segmentCode)
        return parsePaymentOptions(JSONObject(synqt.call("POST", "/payins/!availablePaymentOptions", token, body)))
    }

    /** For display only. Fulfil orders from your backend's own status check, never from this. */
    public suspend fun status(): PayinStatus = parsePayinStatus(JSONObject(synqt.call("GET", "/payins/$id", token)))

    /** The payer's bank or UTR reference, for methods that need it. The token must allow `POST /payins/{id}/!setExternalReference`. */
    public suspend fun setExternalReference(reference: String) {
        require(reference.length in 1..512) { "reference must be 1..512 characters" }
        synqt.call("POST", "/payins/$id/!setExternalReference", token, JSONObject().put("reference", reference))
    }

    /** Link to SYNQT's hosted Payment Web App for this payin. [extra] takes optional params such as `emailAddress`. */
    public fun paymentAppUrl(merchantCode: String, extra: Map<String, String> = emptyMap()): String {
        val params = linkedMapOf(
            "merchantCode" to merchantCode, "idPayin" to id, "token" to token,
            "amount" to money.amount.toPlainString(), "currencyCode" to money.currencyCode,
            "callbackUrl" to callbackUrl, "returnUrl" to returnUrl,
        ) + extra
        return environment.paymentAppBaseUrl + "?" + params.entries.joinToString("&") { (key, value) -> "${enc(key)}=${enc(value)}" }
    }

    /**
     * Shows SYNQT's hosted Payment Web App inside the app, over [activity], and returns once the payer reaches the
     * returnUrl or closes it. [onPresented] runs once it is on screen. Links to other apps (bank apps, UPI) open outside
     * it. Either way, ask [status] where the payment stands. Like the sheet, it survives rotation itself, and cancelling
     * the call closes it.
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
