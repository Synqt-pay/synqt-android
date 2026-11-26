package com.synqt.sdk

import com.synqt.sdk.addon.SheetMethod
import com.synqt.sdk.addon.SynqtAddonApi
import com.synqt.sdk.internal.form.METHOD_FORMS
import com.synqt.sdk.internal.parseErrors
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Entry point of the SYNQT payins SDK. Create one per environment. For the ready-made payment sheet or Payment Web App,
 * make one [PayinCheckout] per order:
 *
 * ```
 * val checkout = Synqt(Synqt.Environment.SANDBOX)
 *     .checkout(token, payinId, MoneyPaymentRequested(BigDecimal("100.00"), "ZAR"), returnUrl, callbackUrl)
 * val accepted = checkout.presentPaymentSheet(activity, "CAPITEC_PAY")
 * ```
 *
 * To call the API yourself, as with the Carpentum Java SDK, use [IncomingPaymentsApi.create].
 *
 * The token is minted per order by YOUR backend (POST /auth-tokens-by-action, action PAYIN_CREATE, with `money`
 * set). The merchant private key and Secret key must never ship in the app. A payment is only paid when your
 * backend's own GET /payins/{id} says SUCCESS; nothing the app sees is proof.
 */
public class Synqt(private val environment: Environment) {
    /** Where API calls go; tests point it at a local server. */
    internal var apiBaseUrl: String = environment.apiBaseUrl

    /** Where each SYNQT environment lives. The base URLs are public for backend calls such as minting tokens. */
    public enum class Environment(public val apiBaseUrl: String, public val paymentAppBaseUrl: String) {
        SANDBOX("https://api.sandbox.synqt.io", "https://payment.sandbox.synqt.io/"),

        /** SYNQT internal development platform, as in the WordPress plugin. Not for merchants. */
        STABLE("https://api.stable.aopay.io", "https://payment.stable.aopay.io/"),
        LIVE("https://api.synqt.io", "https://payment.synqt.io/"),
    }

    /**
     * One order's payin for the payment sheet or Payment Web App. [payinId] is your own ID for it (letters, digits, `_`
     * and `-`, at most 250) and must match the token. [returnUrl] must be https: the Payment Web App rejects other
     * schemes. [callbackUrl] is where SYNQT sends your backend the final status.
     *
     * @throws IllegalArgumentException if [payinId] is not a valid ID or [returnUrl] is not an https URL.
     */
    public fun checkout(token: String, payinId: String, money: MoneyPaymentRequested, returnUrl: String, callbackUrl: String? = null): PayinCheckout {
        IncomingPaymentsApi.requireId(payinId)
        require(returnUrl.startsWith("https://", ignoreCase = true)) { "returnUrl must be an https URL" }
        return PayinCheckout(this, environment, token, payinId, money, returnUrl, callbackUrl)
    }

    /**
     * Every bank and provider SYNQT knows, with the providers behind each. SYNQT may add codes at any time, so treat
     * unknown ones as normal. For what this merchant can use on a payin, use [IncomingPaymentsApi.payinAvailablePaymentOptions] instead.
     *
     * [token] must allow `GET /payment-operators`. PAYIN_CREATE tokens do not, so mint one on your backend with
     * `POST /auth-tokens` and that operation.
     */
    public suspend fun paymentOperators(token: String): List<PaymentOperatorIncoming> {
        val data = JSONObject(call("GET", "/payment-operators", token)).optJSONArray("data") ?: return emptyList()
        return List(data.length()) { data.optJSONObject(it) }.filterNotNull().map(PaymentOperatorIncoming::fromJson)
    }

    internal suspend fun call(method: String, path: String, token: String, body: JSONObject? = null): String =
        withContext(Dispatchers.IO) {
            val conn = URL(apiBaseUrl + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("X-API-Version", "2") // missing header gives 404, not 400
                conn.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toString().toByteArray()) }
                }
                val code = conn.responseCode
                val stream = if (code < 400) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                // The body is parsed into codes here and then dropped: 406 attrValue echoes what the payer typed.
                if (code !in 200..299) throw SynqtException(code, parseErrors(text))
                text
            } finally {
                conn.disconnect()
            }
        }

    public companion object {
        /** Methods [PayinCheckout.presentPaymentSheet] can collect details for. Offer only those [IncomingPaymentsApi.payinAvailablePaymentOptions] also returns. */
        public val directPaymentMethods: List<DirectPaymentMethod>
            get() = (METHOD_FORMS.map { it.code } + addonMethods.map { it.code }).map(::DirectPaymentMethod)

        /** Adds an add-on's method, such as the card add-on's CARD, to the sheet. Once is enough; adding it again replaces it. */
        @SynqtAddonApi
        public fun addMethod(method: SheetMethod) {
            require(METHOD_FORMS.none { it.code == method.code }) { "${method.code} is already built in" }
            synchronized(ADDONS) { ADDONS.removeAll { it.code == method.code }; ADDONS += method }
        }

        internal val addonMethods: List<SheetMethod> get() = synchronized(ADDONS) { ADDONS.toList() }

        private val ADDONS = mutableListOf<SheetMethod>()
    }
}
