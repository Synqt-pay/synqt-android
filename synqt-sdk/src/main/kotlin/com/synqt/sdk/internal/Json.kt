package com.synqt.sdk.internal

import android.content.Context
import com.synqt.sdk.CreatePayinResult
import com.synqt.sdk.ExternalProvider
import com.synqt.sdk.FailureReason
import com.synqt.sdk.Money
import com.synqt.sdk.NextStep
import com.synqt.sdk.PayinStatus
import com.synqt.sdk.PaymentMethodResponse
import com.synqt.sdk.PaymentOperator
import com.synqt.sdk.PaymentOption
import com.synqt.sdk.SynqtError
import com.synqt.sdk.SynqtException
import com.synqt.sdk.UserInteraction
import java.io.IOException
import java.math.BigDecimal
import org.json.JSONArray
import org.json.JSONObject

/** SYNQT error bodies, as a list or a single object. attrValue is dropped on purpose. */
internal fun parseErrors(body: String): List<SynqtError> {
    val items = runCatching { JSONArray(body) }.getOrNull()
        ?: runCatching { JSONArray().put(JSONObject(body)) }.getOrNull()
        ?: return emptyList()
    return List(items.length()) { items.optJSONObject(it) }.filterNotNull().mapNotNull { item ->
        item.optString("code").ifEmpty { null }?.let { SynqtError(it, item.text("attrCode"), item.text("description")) }
    }
}

/** After a 409, the payin [existing] must be the same payment being retried, not an earlier attempt with another method. */
internal fun requireSameMethod(existing: String, requested: String) {
    val used = runCatching { JSONObject(existing).optJSONObject("paymentMethodResponse")?.text("paymentMethodCode") }.getOrNull() ?: return
    if (used == requested) return
    // The description is for developers; the sheet shows its own translatable message for this code.
    throw SynqtException(409, listOf(SynqtError("PAYIN_ALREADY_STARTED", null, "This payin was already started with $used. Start a new payin to pay another way.")))
}

internal fun parseCreatePayin(json: String): CreatePayinResult {
    val body = JSONObject(json)
    body.optJSONObject("redirectTo")?.let { redirect ->
        val data = redirect.text("data")?.let(::JSONObject)
        val form = data?.keys()?.asSequence()?.associateWith { data.get(it).toString() }.orEmpty()
        return CreatePayinResult.Redirect(redirect.getString("method"), redirect.getString("url"), form)
    }
    val response = body.optJSONObject("paymentMethodResponse") ?: JSONObject()
    val method = PaymentMethodResponse.fromJson(response)
    // A card's 3-D Secure page (CARD comes from the card add-on); FORM, CONTENT and OTP_REQUIRED stay in Instructions for the app to show.
    ((method as? PaymentMethodResponse.Card)?.userInteraction as? UserInteraction.Redirect)?.url?.let { return CreatePayinResult.Redirect("GET", it, emptyMap()) }
    return CreatePayinResult.Instructions(method, parseNextStep(response))
}

// Every deep link field in the spec; a method returns at most one.
private val APP_LINKS = listOf("deepLink", "zapperQrDeepLink", "snapscanQrDeepLink", "upiQrDeepLink", "payMeDeepLink")

internal fun parseNextStep(response: JSONObject) = NextStep(
    appLink = APP_LINKS.firstNotNullOfOrNull { response.text(it) },
    qrCodeImageUrl = response.text("qrCode"),
    qrCodeData = response.text("qrRawData"),
    amountToPay = parseMoney(response.optJSONObject("moneyRequired")),
    reference = response.text("reference"),
    expiresAt = response.text("expireAt"),
    // shortcut: OFFLINE only, as some methods' `account` is the payer's own; add others (such as VAPAY) once checked.
    payInto = response.optJSONObject("account")?.takeIf { response.text("paymentMethodCode") == "OFFLINE" }?.let(PaymentMethodResponse.Account::fromJson),
)

internal fun parsePayinStatus(payin: JSONObject): PayinStatus {
    val process = payin.optJSONObject("process") ?: JSONObject()
    val status = runCatching { PayinStatus.Status.valueOf(process.optString("status")) }.getOrDefault(PayinStatus.Status.UNKNOWN)
    return PayinStatus(
        status = status,
        requested = parseMoney(payin.optJSONObject("paymentRequested")?.optJSONObject("money")) ?: Money(BigDecimal.ZERO, ""),
        fee = parseMoney(payin.optJSONObject("fee")),
        isTest = process.optBoolean("isTest"),
        failureReason = process.optJSONObject("failureReasons")?.let { failure ->
            failure.text("failureReasonCode")?.let { FailureReason(it, failure.text("message"), failure.text("failedAt")) }
        },
        paymentMethod = payin.optJSONObject("paymentMethodResponse")?.let(PaymentMethodResponse::fromJson),
        createdAt = process.text("createdAt"),
        processedAt = process.text("processedAt"),
    )
}

internal fun parsePaymentOptions(body: JSONObject): List<PaymentOption> {
    val data = body.optJSONArray("data") ?: return emptyList()
    return List(data.length()) { data.optJSONObject(it) }.filterNotNull().mapNotNull { option ->
        val code = option.text("paymentMethodCode") ?: return@mapNotNull null
        PaymentOption(code, parseOperators(option.optJSONArray("paymentOperators")), option.text("segmentCode"))
    }
}

/** Operators from availablePaymentOptions or GET /payment-operators; entries without a code are skipped, a missing name shows the code. */
internal fun parseOperators(array: JSONArray?): List<PaymentOperator> = objects(array).mapNotNull { op ->
    val code = op.text("code") ?: return@mapNotNull null
    val providers = objects(op.optJSONObject("externalProviders")?.optJSONArray("providers")).mapNotNull { provider ->
        provider.text("code")?.let { ExternalProvider(it, provider.text("name") ?: it) }
    }
    PaymentOperator(code, op.text("name") ?: code, providers)
}

private fun objects(array: JSONArray?): List<JSONObject> = if (array == null) emptyList() else List(array.length()) { array.optJSONObject(it) }.filterNotNull()

private fun parseMoney(money: JSONObject?): Money? {
    val amount = money?.text("amount")?.toBigDecimalOrNull() ?: return null
    return Money(amount, money.text("currencyCode").orEmpty())
}

/** The value of [key] as text, or null when missing, null or empty. */
private fun JSONObject.text(key: String): String? = if (isNull(key)) null else optString(key).ifEmpty { null }
