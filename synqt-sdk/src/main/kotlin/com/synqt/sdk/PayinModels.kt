package com.synqt.sdk

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal

/** PayinAcceptedResponse, by which field SYNQT sent. */
public sealed interface PayinAcceptedResponse {
    public companion object {
        public fun fromJson(json: JSONObject): PayinAcceptedResponse = when {
            json.has("paymentMethodResponse") -> PayinAcceptedMethodResponse.fromJson(json)
            json.has("redirectTo") -> PayinAcceptedRedirectResponse.fromJson(json)
            else -> throw JSONException("PayinAcceptedResponse has none of paymentMethodResponse, redirectTo")
        }
    }
}

public data class AccountResponseDetail(
    /** Name of the bank account. min 1, max 64, pattern `^(\p{L}\p{M}*?|[\d, ;:_'"!%=@#&\-\.\?\$\(\)\+\*\|])*$`. */
    public val accountName: String? = null,
    /** Number of the bank account. */
    public val accountNumber: String? = null,
    /** Type of the bank account. min 1, max 64, pattern `^(\p{L}\p{M}*?|[, ;:_'"!%=@#&\-\.\?\$\(\)\+\*\|])*$`. */
    public val accountType: String? = null,
    /** Bank code of the bank. min 1. */
    public val bankCode: String? = null,
    /** Payment operator code. */
    public val paymentOperator: PaymentOperatorIncoming? = null,
    /** Name of the bank. min 1. */
    public val bankName: String? = null,
    /** Branch name of the bank. min 1. */
    public val bankBranch: String? = null,
    /** City of the bank. min 1. */
    public val bankCity: String? = null,
    /** Province of the bank. min 1. */
    public val bankProvince: String? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): AccountResponseDetail = AccountResponseDetail(
            accountName = json.str("accountName"),
            accountNumber = json.str("accountNumber"),
            accountType = json.str("accountType"),
            bankCode = json.str("bankCode"),
            paymentOperator = json.obj("paymentOperator") { PaymentOperatorIncoming.fromJson(it) },
            bankName = json.str("bankName"),
            bankBranch = json.str("bankBranch"),
            bankCity = json.str("bankCity"),
            bankProvince = json.str("bankProvince"),
        )
    }
}

public data class AvailablePayinOption(
    /** Payment method is a payment instrument that your customers use (such as online bank transfers, VA payments, UPI and so on). min 1. Always present per spec. */
    public val paymentMethodCode: String? = null,
    public val paymentOperators: List<PaymentOperatorIncoming>? = null,
    /** A customer segment which allows you to divide your customers into groups that reflect similarity among customers in each group. min 1. */
    public val segmentCode: String? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): AvailablePayinOption = AvailablePayinOption(
            paymentMethodCode = json.str("paymentMethodCode"),
            paymentOperators = json.list("paymentOperators") { array, index -> PaymentOperatorIncoming.fromJson(array.getJSONObject(index)) },
            segmentCode = json.str("segmentCode"),
        )
    }
}

/** The list of possible payment options. The list always contains at least one payment option. */
public data class AvailablePayinOptionList(
    public val data: List<AvailablePayinOption>? = null,
    public val dataVariantCurrencies: List<AvailablePayinOptionVariantCurrency>? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): AvailablePayinOptionList = AvailablePayinOptionList(
            data = json.list("data") { array, index -> AvailablePayinOption.fromJson(array.getJSONObject(index)) },
            dataVariantCurrencies = json.list("dataVariantCurrencies") { array, index -> AvailablePayinOptionVariantCurrency.fromJson(array.getJSONObject(index)) },
        )
    }
}

public data class AvailablePayinOptionVariantCurrency(
    /** Payment method is a payment instrument that your customers use (such as online bank transfers, VA payments, UPI and so on). min 1. Always present per spec. */
    public val paymentMethodCode: String? = null,
    public val paymentOperators: List<PaymentOperatorIncoming>? = null,
    /** A customer segment which allows you to divide your customers into groups that reflect similarity among customers in each group. min 1. */
    public val segmentCode: String? = null,
    /** Always present per spec. */
    public val money: Money? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): AvailablePayinOptionVariantCurrency = AvailablePayinOptionVariantCurrency(
            paymentMethodCode = json.str("paymentMethodCode"),
            paymentOperators = json.list("paymentOperators") { array, index -> PaymentOperatorIncoming.fromJson(array.getJSONObject(index)) },
            segmentCode = json.str("segmentCode"),
            money = json.obj("money") { Money.fromJson(it) },
        )
    }
}

/** Customer related to this payment. */
public data class CustomerDetail(
    /** The ID of customer given by merchant. min 1, max 250. */
    public val idCustomer: String? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): CustomerDetail = CustomerDetail(
            idCustomer = json.str("idCustomer"),
        )
    }
}

/** TODO: Add description */
public data class ExternalProvider(
    /** min 1, max 128. Always present per spec. */
    public val code: String? = null,
    /** min 1. Always present per spec. */
    public val name: String? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): ExternalProvider = ExternalProvider(
            code = json.str("code"),
            name = json.str("name"),
        )
    }
}

public data class ExternalProviders(
    public val providers: List<ExternalProvider>? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): ExternalProviders = ExternalProviders(
            providers = json.list("providers") { array, index -> ExternalProvider.fromJson(array.getJSONObject(index)) },
        )
    }
}

public data class FailureReasons(
    /** Date and time of the payment failure. date-time. Always present per spec. */
    public val failedAt: String? = null,
    /** * `SYSTEM` - Internal error. one of SYSTEM, PAYMENT_PROCESS_ERROR, ENTITY_EXPIRED, DIFFERENT_AMOUNT_CONFIRMED, CURRENCY_NOT_SUPPORTED, CURRENCY_PRECISION_EXCEEDED, PAYMENT_METHOD_NOT_FOUND, PAYMENT_METHOD_ERROR, PAYMENT_OPERATOR_NOT_FOUND, PAYMENT_OPERATOR_INVALID, PAYMENT_OPERATOR_UNAVAILABLE, PAYMENT_CHANNEL_NO_ACTIVE_FOUND, PAYMENT_CHANNEL_NO_OPENED_FOUND, PAYMENT_CHANNEL_NO_SEGMENT_FOUND, PAYMENT_CHANNEL_AMOUNT_LIMITS, PAYMENT_CHANNEL_DAILY_LIMITS, IP_DENIED, BALANCE_INSUFFICIENT, INVALID_ACCOUNT_NUMBER, INVALID_ACCOUNT_NAME, INVALID_PHONE_NUMBER, INVALID_ID_NUMBER, INCORRECT_CURRENCY, CUSTOMER_VERIFICATION_REJECTED, CUSTOMER_VERIFICATION_FAILED, ACCOUNT_TYPE_INELIGIBLE, NAME_SIMILARITY_CHECK_FAILED, INVALID_IFSC, FEATURE_UNAVAILABLE, CUSTOMER_ACCOUNT_LIMIT_EXCEEDED, CUSTOMER_VERIFICATION_INACTIVITY, CUSTOMER_NOT_REGISTERED, SECURITY_CHECK_FAILED, VOUCHER_INELIGIBLE. Always present per spec. */
    public val failureReasonCode: String? = null,
    /** Additional description of the failure reason. */
    public val message: String? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): FailureReasons = FailureReasons(
            failedAt = json.str("failedAt"),
            failureReasonCode = json.str("failureReasonCode"),
            message = json.str("message"),
        )
    }
}

public data class Money(
    /** Amount is accepted in the smallest currency unit. Always present per spec. */
    public val amount: BigDecimal? = null,
    /** For supported currencies please refer to `GET /currencies`. min 1. Always present per spec. */
    public val currencyCode: String? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): Money = Money(
            amount = json.dec("amount"),
            currencyCode = json.str("currencyCode"),
        )
    }
}

/** Fees charged for this payment. */
public data class MoneyFee(
    /** Amount is accepted in the smallest currency unit. Always present per spec. */
    public val amount: BigDecimal? = null,
    /** For supported currencies please refer to `GET /currencies`. min 1. Always present per spec. */
    public val currencyCode: String? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): MoneyFee = MoneyFee(
            amount = json.dec("amount"),
            currencyCode = json.str("currencyCode"),
        )
    }
}

public data class MoneyPaymentRequested(
    /** Requested payment amount. */
    public val amount: BigDecimal,
    /** For supported currencies please refer to `GET /currencies`. min 1. */
    public val currencyCode: String,
) {
    public fun toJson(): JSONObject = JSONObject()
        .put("amount", amount)
        .put("currencyCode", currencyCode)

    public companion object {
        public fun fromJson(json: JSONObject): MoneyPaymentRequested = MoneyPaymentRequested(
            amount = requireNotNull(json.dec("amount")) { "MoneyPaymentRequested.amount missing" },
            currencyCode = requireNotNull(json.str("currencyCode")) { "MoneyPaymentRequested.currencyCode missing" },
        )
    }
}

/** For every payment method there is appropriate payment specific request object in `paymentMethod` attribute. If you have used the `POST /payins/!availablePaymentOptions` API to get the list of the available payment options, then it is expected that you use the same input data here to make sure that the payment will be accepted. */
public data class Payin(
    /** The requested payment. */
    public val paymentRequested: PaymentRequested,
    /** Payment method is a payment instrument that your customers use (such as online bank transfers, VA payments, UPI and so on). */
    public val paymentMethod: PaymentMethodPayin,
    /** This is the URL where the customers will be redirected after completing a payment. max 1000. */
    public val returnUrl: String,
    /** This is the URL where the system will send the transaction final status after payment has been completed. max 1000. */
    public val callbackUrl: String? = null,
    /** The IP address of the customer making the payment in either the IPv4 or IPv6 format. */
    public val customerIp: String? = null,
) {
    public fun toJson(): JSONObject = JSONObject()
        .put("paymentRequested", paymentRequested.toJson())
        .put("paymentMethod", paymentMethod.toJson())
        .putOpt("callbackUrl", callbackUrl)
        .put("returnUrl", returnUrl)
        .putOpt("customerIp", customerIp)
}

/** For every payment method there is appropriate payment specific response object in `paymentMethodResponse` attribute. Use data from `paymentMethodResponse` for payment completion (for example show to the customer). */
public data class PayinAcceptedMethodResponse(
    /** The requested payment. Always present per spec. */
    public val paymentRequested: PaymentRequested? = null,
    /** Always present per spec. */
    public val paymentMethodResponse: PaymentMethodResponsePayin? = null,
) : PayinAcceptedResponse {
    public companion object {
        public fun fromJson(json: JSONObject): PayinAcceptedMethodResponse = PayinAcceptedMethodResponse(
            paymentRequested = json.obj("paymentRequested") { PaymentRequested.fromJson(it) },
            paymentMethodResponse = json.obj("paymentMethodResponse") { PaymentMethodResponsePayin.fromJson(it) },
        )
    }
}

/** Customer should be redirected to specified URL for the next payment process step. */
public data class PayinAcceptedRedirectResponse(
    /** The requested payment. Always present per spec. */
    public val paymentRequested: PaymentRequested? = null,
    /** Always present per spec. */
    public val redirectTo: Redirection? = null,
) : PayinAcceptedResponse {
    public companion object {
        public fun fromJson(json: JSONObject): PayinAcceptedRedirectResponse = PayinAcceptedRedirectResponse(
            paymentRequested = json.obj("paymentRequested") { PaymentRequested.fromJson(it) },
            redirectTo = json.obj("redirectTo") { Redirection.fromJson(it) },
        )
    }
}

public data class PayinDetail(
    /** The requested payment. Always present per spec. */
    public val paymentRequested: PaymentRequested? = null,
    /** Always present per spec. */
    public val process: PaymentProcess? = null,
    /** Fees charged for this payment. Always present per spec. */
    public val fee: MoneyFee? = null,
    /** Always present per spec. */
    public val paymentMethodResponse: PaymentMethodResponsePayin? = null,
    /** Payment method is a payment instrument that your customers use (such as online bank transfers, VA payments, UPI and so on). */
    public val settlement: SettlementMethod? = null,
    /** Customer related to this payment. */
    public val customer: CustomerDetail? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): PayinDetail = PayinDetail(
            paymentRequested = json.obj("paymentRequested") { PaymentRequested.fromJson(it) },
            process = json.obj("process") { PaymentProcess.fromJson(it) },
            fee = json.obj("fee") { MoneyFee.fromJson(it) },
            paymentMethodResponse = json.obj("paymentMethodResponse") { PaymentMethodResponsePayin.fromJson(it) },
            settlement = json.obj("settlement") { SettlementMethod.fromJson(it) },
            customer = json.obj("customer") { CustomerDetail.fromJson(it) },
        )
    }
}

public data class PaymentAccountDetail(
    /** Details of customer's account collected in payment request. Always present per spec. */
    public val customerAccountRequest: AccountResponseDetail? = null,
    /** Details of customer's account collected during payment process. Always present per spec. */
    public val customerAccountProcess: AccountResponseDetail? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): PaymentAccountDetail = PaymentAccountDetail(
            customerAccountRequest = json.obj("customerAccountRequest") { AccountResponseDetail.fromJson(it) },
            customerAccountProcess = json.obj("customerAccountProcess") { AccountResponseDetail.fromJson(it) },
        )
    }
}

public data class PaymentOperatorIncoming(
    /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
    public val code: String? = null,
    /** min 1, max 1000. Always present per spec. */
    public val name: String? = null,
    public val externalProviders: ExternalProviders? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): PaymentOperatorIncoming = PaymentOperatorIncoming(
            code = json.str("code"),
            name = json.str("name"),
            externalProviders = json.obj("externalProviders") { ExternalProviders.fromJson(it) },
        )
    }
}

public data class PaymentProcess(
    /** * `PROCESSING` - payment process is not finished yet. one of PROCESSING, SUCCESS, FAILED, REFUSED. Always present per spec. */
    public val status: String? = null,
    public val failureReasons: FailureReasons? = null,
    /** Date and time when payment was accepted by platform. date-time. Always present per spec. */
    public val createdAt: String? = null,
    /** Date and time when payment was processed by platform. date-time. */
    public val processedAt: String? = null,
    /** This flag is set to `true` when payment was done with testing merchant or testing channel. Always present per spec. */
    public val isTest: Boolean? = null,
    /** If present, this field specifies the status of the payment processing by an external processor. one of ACCEPTED, PROCESSED. */
    public val processorStatus: String? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): PaymentProcess = PaymentProcess(
            status = json.str("status"),
            failureReasons = json.obj("failureReasons") { FailureReasons.fromJson(it) },
            createdAt = json.str("createdAt"),
            processedAt = json.str("processedAt"),
            isTest = json.bool("isTest"),
            processorStatus = json.str("processorStatus"),
        )
    }
}

/** The requested payment. */
public data class PaymentRequested(
    public val money: MoneyPaymentRequested,
    /** A customer segment which allows you to divide your customers into groups that reflect similarity among customers in each group. min 1. */
    public val segmentCode: String? = null,
    /** For supported currencies please refer to `GET /currencies`. min 1. */
    public val exchangedToCurrency: String? = null,
) {
    public fun toJson(): JSONObject = JSONObject()
        .put("money", money.toJson())
        .putOpt("segmentCode", segmentCode)
        .putOpt("exchangedToCurrency", exchangedToCurrency)

    public companion object {
        public fun fromJson(json: JSONObject): PaymentRequested = PaymentRequested(
            money = requireNotNull(json.obj("money") { MoneyPaymentRequested.fromJson(it) }) { "PaymentRequested.money missing" },
            segmentCode = json.str("segmentCode"),
            exchangedToCurrency = json.str("exchangedToCurrency"),
        )
    }
}

public data class Redirection(
    /** The HTTP method that should be used for redirection. one of GET, POST. Always present per spec. */
    public val method: String? = null,
    /** The destination URL for redirect to. Always present per spec. */
    public val url: String? = null,
    /** The optional data in JSON format - a key-value pairs that must be used for redirection when POST method is used. */
    public val data: String? = null,
) {
    public companion object {
        public fun fromJson(json: JSONObject): Redirection = Redirection(
            method = json.str("method"),
            url = json.str("url"),
            data = json.str("data"),
        )
    }
}

private fun JSONObject.str(key: String): String? = if (isNull(key)) null else getString(key)
private fun JSONObject.dec(key: String): BigDecimal? = if (isNull(key)) null else BigDecimal(get(key).toString())
private fun JSONObject.int(key: String): Int? = if (isNull(key)) null else getInt(key)
private fun JSONObject.bool(key: String): Boolean? = if (isNull(key)) null else getBoolean(key)
private fun <T> JSONObject.obj(key: String, read: (JSONObject) -> T): T? = optJSONObject(key)?.let(read)
private fun <T> JSONObject.list(key: String, read: (JSONArray, Int) -> T): List<T>? = optJSONArray(key)?.let { array -> List(array.length()) { read(array, it) } }
