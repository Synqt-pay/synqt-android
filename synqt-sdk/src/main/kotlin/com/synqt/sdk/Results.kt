package com.synqt.sdk

import android.content.Context
import com.synqt.sdk.internal.form.FAILURE_MESSAGES
import com.synqt.sdk.internal.form.METHOD_FORMS
import com.synqt.sdk.internal.sheet.formatMoney
import java.io.IOException
import org.json.JSONObject

/** R1,250.00 for rand, "USD 1,250.00" otherwise. */
public fun MoneyPaymentRequested.format(): String = formatMoney(amount, currencyCode)

/** R1,250.00 for rand, "USD 1,250.00" otherwise; null without an amount. */
public fun PaymentMethodResponsePayin.Money.format(): String? = amount?.let { formatMoney(it, currencyCode.orEmpty()) }

/** A payment method the SDK's payment sheet supports: [code] for the API, [name] to show the payer. */
public data class DirectPaymentMethod internal constructor(public val code: String) {
    /** The payer-facing name, in the phone's language where the SDK has a translation. */
    public fun name(context: Context): String = context.getString(checkNotNull(methodName(code)))
}

/** The payer-facing name of this option's method in the phone's language, or null for methods the sheet does not support. */
public fun AvailablePayinOption.name(context: Context): String? = methodName(paymentMethodCode)?.let(context::getString)

/** The string resource naming [code] for payers, or null if the sheet does not support it. */
internal fun methodName(code: String?): Int? =
    METHOD_FORMS.firstOrNull { it.code == code }?.name ?: Synqt.addonMethods.firstOrNull { it.code == code }?.name

/**
 * What the payer does next, from whichever of these fields the method returned. On this phone, open [appLink]; from
 * another phone, scan the image at [qrCodeImageUrl] (a temporary link) or a QR code you draw from [qrCodeData].
 * Every [PaymentMethodResponsePayin] gives one with `nextStep()`; [PayinCheckout.presentPaymentSheet] already shows it.
 */
public data class NextStep(
    public val appLink: String? = null,
    public val qrCodeImageUrl: String? = null,
    public val qrCodeData: String? = null,
    /** If set, the payer must pay exactly this instead of the requested amount. */
    public val amountToPay: PaymentMethodResponsePayin.Money? = null,
    public val reference: String? = null,
    /** ISO 8601. After this the payment fails and SYNQT sends a callback. */
    public val expiresAt: String? = null,
    /** For OFFLINE: the account the payer transfers to from their own banking app, quoting [reference]. */
    public val payInto: PaymentMethodResponsePayin.Account? = null,
)

/** The next step of a payin SYNQT accepted with instructions, or null when it sent a redirect instead. */
public fun PayinAcceptedResponse.nextStep(): NextStep? = (this as? PayinAcceptedMethodResponse)?.paymentMethodResponse?.nextStep()

/**
 * Where to send the payer, if anywhere: SYNQT's `redirectTo`, or a card's 3-D Secure page (CARD comes from the card
 * add-on). GET: open [Redirection.url]. POST: submit a form to it with [formData]'s fields (in a WebView).
 */
public fun PayinAcceptedResponse.redirect(): Redirection? = when (this) {
    is PayinAcceptedRedirectResponse -> redirectTo
    // FORM, CONTENT and OTP_REQUIRED stay on the method response for the app to show.
    is PayinAcceptedMethodResponse -> ((paymentMethodResponse as? PaymentMethodResponsePayin.CardMethodResponse)?.userInteraction as? UserInteraction.Redirect)
        ?.url?.let { Redirection(method = "GET", url = it) }
}

/** The fields to POST to [Redirection.url]: SYNQT sends them as a JSON string in `data`. */
public fun Redirection.formData(): Map<String, String> {
    val data = data?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return emptyMap()
    return data.keys().asSequence().associateWith { data.get(it).toString() }
}

/** SYNQT's payment statuses. New ones may come; treat an unknown status like [PROCESSING]. */
public object PaymentStatus {
    public const val PROCESSING: String = "PROCESSING"
    public const val SUCCESS: String = "SUCCESS"
    public const val FAILED: String = "FAILED"
    public const val REFUSED: String = "REFUSED"
}

/** SUCCESS, FAILED and REFUSED, which do not change again. For display only: fulfil orders from your backend's own check. */
public val PayinDetail.isFinal: Boolean
    get() = process?.status in setOf(PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.REFUSED)

/** What to tell the payer, in the phone's language where the SDK has a translation. [FailureReasons.message] is SYNQT's own, written for the merchant. */
public fun FailureReasons.payerMessage(context: Context): String =
    context.getString(failureReasonCode?.let(::payerMessageFor) ?: R.string.synqt_failure_general)

/** Payer-facing wording (a string resource) for SYNQT codes whose own description is meant for the merchant. */
internal fun payerMessageFor(code: String): Int? = FAILURE_MESSAGES[code]

/** One reason SYNQT refused a request. [field] names the input it is about (SYNQT's attrCode), if any. */
public data class SynqtError(public val code: String, public val field: String?, public val description: String?)

/**
 * SYNQT refused a request. [errors] holds its reasons (for 406, the business errors such as INVALID_PHONE_NUMBER).
 * The raw response is not kept: SYNQT echoes what the payer typed in it, which must not reach logs.
 */
public class SynqtException internal constructor(public val httpCode: Int, public val errors: List<SynqtError>) :
    IOException("SYNQT HTTP $httpCode" + errors.joinToString(prefix = ": ") { error -> error.code + (error.field?.let { " ($it)" } ?: "") }.takeIf { errors.isNotEmpty() }.orEmpty())
