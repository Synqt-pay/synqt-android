package com.synqt.sdk

import android.content.Context
import com.synqt.sdk.internal.form.FAILURE_MESSAGES
import com.synqt.sdk.internal.form.METHOD_FORMS
import com.synqt.sdk.internal.sheet.formatMoney
import java.io.IOException
import java.math.BigDecimal
import org.json.JSONArray
import org.json.JSONObject

/** An amount in major units (12.34, not 1234) and its ISO 4217 currency. IDR and INR take whole numbers only. */
public data class Money(public val amount: BigDecimal, public val currencyCode: String) {
    /** R1,250.00 for rand, "USD 1,250.00" otherwise. */
    override fun toString(): String = formatMoney(amount, currencyCode)

    internal fun toJson(): JSONObject = JSONObject().put("amount", amount).put("currencyCode", currencyCode)
}

/** A payment method the SDK's payment sheet supports: [code] for the API, [name] to show the payer. */
public data class DirectPaymentMethod internal constructor(public val code: String) {
    /** The payer-facing name, in the phone's language where the SDK has a translation. */
    public fun name(context: Context): String = context.getString(checkNotNull(methodName(code)))
}

/** A method SYNQT offers this merchant, with the banks or providers to choose from. */
public data class PaymentOption(
    public val code: String,
    public val operators: List<PaymentOperator>,
    public val segmentCode: String?,
) {
    /** The payer-facing name in the phone's language, or null for methods the sheet does not support. */
    public fun name(context: Context): String? = methodName(code)?.let(context::getString)
}

/** The string resource naming [code] for payers, or null if the sheet does not support it. */
internal fun methodName(code: String): Int? =
    METHOD_FORMS.firstOrNull { it.code == code }?.name ?: Synqt.addonMethods.firstOrNull { it.code == code }?.name

/** A bank or provider: send [code], show [name]. [externalProviders] are the providers it routes through, where SYNQT lists any. */
public data class PaymentOperator(
    public val code: String,
    public val name: String,
    public val externalProviders: List<ExternalProvider> = emptyList(),
)

/** A provider behind a [PaymentOperator]: send [code] (for example as PayShap's `externalProviderCode`), show [name]. */
public data class ExternalProvider(public val code: String, public val name: String)

/** What happened when a payin was created. Not proof of payment: check [Payin.status] (and your backend) for that. */
public sealed interface CreatePayinResult {
    /** Send the payer to [url]. GET: open it. POST: submit a form to it with one field per [formData] entry (in a WebView). */
    public data class Redirect(public val httpMethod: String, public val url: String, public val formData: Map<String, String>) : CreatePayinResult

    /**
     * Show these instructions yourself: QR code, deep link, account, reference, expiry, and moneyRequired if present
     * (the payer must pay exactly that amount, so show it prominently). [nextStep] gathers the common ones;
     * [Payin.presentPaymentSheet] already shows them.
     */
    public data class Instructions(
        public val paymentMethod: PaymentMethodResponse,
        public val nextStep: NextStep = NextStep(),
    ) : CreatePayinResult
}

/**
 * What the payer does next, from whichever of these fields the method returned. On this phone, open [appLink]; from
 * another phone, scan the image at [qrCodeImageUrl] (a temporary link) or a QR code you draw from [qrCodeData].
 */
public data class NextStep(
    public val appLink: String? = null,
    public val qrCodeImageUrl: String? = null,
    public val qrCodeData: String? = null,
    /** If set, the payer must pay exactly this instead of the requested amount. */
    public val amountToPay: Money? = null,
    public val reference: String? = null,
    /** ISO 8601. After this the payment fails and SYNQT sends a callback. */
    public val expiresAt: String? = null,
    /** For OFFLINE: the account the payer transfers to from their own banking app, quoting [reference]. */
    public val payInto: PaymentMethodResponse.Account? = null,
)

/** A payin's state from GET /payins/{id}. */
public data class PayinStatus(
    public val status: Status,
    public val requested: Money,
    public val fee: Money?,
    public val isTest: Boolean,
    public val failureReason: FailureReason?,
    public val paymentMethod: PaymentMethodResponse?,
    public val createdAt: String?,
    public val processedAt: String?,
) {
    public enum class Status {
        PROCESSING, SUCCESS, FAILED, REFUSED,

        /** A status this SDK version does not know yet. Treat it like PROCESSING. */
        UNKNOWN;

        /** SUCCESS, FAILED and REFUSED, do not change again. */
        public val isFinal: Boolean get() = this == SUCCESS || this == FAILED || this == REFUSED
    }
}

/** Why a payin failed or was refused. [code] is one of SYNQT's FailureReasonCode values, such as ENTITY_EXPIRED. */
public data class FailureReason(public val code: String, public val message: String?, public val failedAt: String?) {
    /** What to tell the payer, in the phone's language where the SDK has a translation. [message] is SYNQT's own, written for the merchant. */
    public fun payerMessage(context: Context): String = context.getString(payerMessageFor(code) ?: R.string.synqt_failure_general)
}

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
