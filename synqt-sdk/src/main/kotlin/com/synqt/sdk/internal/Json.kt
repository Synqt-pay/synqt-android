package com.synqt.sdk.internal

import com.synqt.sdk.NextStep
import com.synqt.sdk.PaymentMethodResponsePayin
import com.synqt.sdk.SynqtError
import com.synqt.sdk.SynqtException
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

// Every deep link field in the spec; a method returns at most one.
private val APP_LINKS = listOf("deepLink", "zapperQrDeepLink", "snapscanQrDeepLink", "upiQrDeepLink", "payMeDeepLink")

/** The next step of a method this SDK version doesn't know, from the field names every method uses. */
internal fun parseNextStep(response: JSONObject) = NextStep(
    appLink = APP_LINKS.firstNotNullOfOrNull { response.text(it) },
    qrCodeImageUrl = response.text("qrCode"),
    qrCodeData = response.text("qrRawData"),
    amountToPay = response.optJSONObject("moneyRequired")?.let(PaymentMethodResponsePayin.Money::fromJson),
    reference = response.text("reference"),
    expiresAt = response.text("expireAt"),
    // No payInto: only OFFLINE's account is one to pay into, and OFFLINE is never unknown.
)

/** The value of [key] as text, or null when missing, null or empty. */
private fun JSONObject.text(key: String): String? = if (isNull(key)) null else optString(key).ifEmpty { null }
