package com.synqt.sdk

import com.synqt.sdk.addon.SynqtAddonApi
import com.synqt.sdk.internal.requireSameMethod
import java.io.IOException
import org.json.JSONObject

/**
 * SYNQT's incoming payments (payin) API, as in the Carpentum Java SDK's `IncomingPaymentsApi`: one instance per
 * token, each call naming the payin it is about.
 *
 * ```
 * val api = IncomingPaymentsApi.create(Synqt(Synqt.Environment.SANDBOX), token)
 * val requested = PaymentRequested(MoneyPaymentRequested(BigDecimal("100.00"), "ZAR"))
 * val accepted = api.createPayin(payinId, Payin(requested, PaymentMethodPayin.CapitecPayMethod(phoneNumber = "0821234567"), returnUrl))
 * ```
 *
 * The token is minted by YOUR backend, which holds the merchant's private key (POST /auth-tokens-by-action with
 * action PAYIN_CREATE and `money` set allows every call here for that payin). Calls throw [SynqtException] when SYNQT
 * refuses them, and [IOException] when it cannot be reached.
 */
public class IncomingPaymentsApi private constructor(private val synqt: Synqt, private val token: String) {

    /** Methods, banks and providers SYNQT offers this merchant for [paymentRequested]. Send the same `segmentCode` when paying. */
    public suspend fun payinAvailablePaymentOptions(paymentRequested: PaymentRequested): AvailablePayinOptionList =
        AvailablePayinOptionList.fromJson(JSONObject(synqt.call("POST", "/payins/!availablePaymentOptions", token, paymentRequested.toJson())))

    /**
     * Creates payin [idPayin] (your own ID for the order: letters, digits, `_` and `-`, at most 250, matching the
     * token). Not proof of payment: see [getPayin] and your backend's own check.
     *
     * The payment method is checked when you build it, and [Payin.returnUrl] must be https. A retry after a network
     * failure is safe: if SYNQT already accepted [idPayin], the existing payin is returned. If that payin was made with
     * a different method, this throws [SynqtException] (409, PAYIN_ALREADY_STARTED): start a new payin to pay another way.
     *
     * @throws IllegalArgumentException if [idPayin] is not a valid ID or [Payin.returnUrl] is not an https URL.
     */
    public suspend fun createPayin(idPayin: String, payin: Payin): PayinAcceptedResponse {
        requireId(idPayin)
        require(payin.returnUrl.startsWith("https://", ignoreCase = true)) { "returnUrl must be an https URL" }
        val json = try {
            synqt.call("POST", "/payins/$idPayin", token, payin.toJson())
        } catch (error: SynqtException) {
            if (error.httpCode != 409) throw error
            // GET /payins/{id} carries paymentRequested and paymentMethodResponse, as the accepted response does.
            synqt.call("GET", "/payins/$idPayin", token).also { existing -> requireSameMethod(existing, payin.paymentMethod.code()) }
        }
        return PayinAcceptedResponse.fromJson(JSONObject(json))
    }

    /** The payer's bank or UTR reference, for methods that need it. The token must allow `POST /payins/{id}/!setExternalReference`. */
    public suspend fun setPayinExternalReference(idPayin: String, reference: String) {
        requireId(idPayin)
        require(reference.length in 1..512) { "reference must be 1..512 characters" }
        synqt.call("POST", "/payins/$idPayin/!setExternalReference", token, JSONObject().put("reference", reference))
    }

    /** Payin [idPayin] as SYNQT has it now. For display only: fulfil orders from your backend's own check, never from this. */
    public suspend fun getPayin(idPayin: String): PayinDetail {
        requireId(idPayin)
        return PayinDetail.fromJson(JSONObject(synqt.call("GET", "/payins/$idPayin", token)))
    }

    /** The payer's account as they entered it and as the payment used it. The token must allow `GET /payins/{id}/accounts`. */
    public suspend fun getPayinAccounts(idPayin: String): PaymentAccountDetail {
        requireId(idPayin)
        return PaymentAccountDetail.fromJson(JSONObject(synqt.call("GET", "/payins/$idPayin/accounts", token)))
    }

    /** GET [path] with this token, for add-ons that need more from SYNQT, such as the card encryption key. */
    @SynqtAddonApi
    public suspend fun get(path: String): JSONObject = JSONObject(synqt.call("GET", path, token))

    public companion object {
        /** An API for [accessToken], on [synqt]'s environment. As the Java SDK's `IncomingPaymentsApi.create(context, token)`. */
        @JvmStatic
        public fun create(synqt: Synqt, accessToken: String): IncomingPaymentsApi = IncomingPaymentsApi(synqt, accessToken)

        private val ID = Regex("^[A-Za-z0-9_-]+$")

        /** @throws IllegalArgumentException if [id] is not a valid payin ID. */
        internal fun requireId(id: String) = require(id.length <= 250 && ID.matches(id)) { "idPayin must match ^[A-Za-z0-9_-]+$, max 250" }
    }
}

/** The `paymentMethodCode` this method sends. */
internal fun PaymentMethodPayin.code(): String = toJson().getString("paymentMethodCode")
