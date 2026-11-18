package com.synqt.sdk

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/** The `paymentMethod` object of POST /payins/{idPayin}. Field rules differ per method and sometimes per currency. */
public sealed interface PaymentMethod {
    public fun toJson(): JSONObject

    /** Absa Pay offers a simple, quick and secure way to make payments without needing to input banking login details. Customers can select their preferred account for payment and securely authorize the transaction through the banking app. */
    public data class AbsaPay(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** min 8, max 9, pattern `^[a-zA-Z][0-9]+$`. */
        public val passportNumber: String? = null,
        /** min 13, max 13, pattern `^[0-9]*$`. */
        public val said: String? = null,
        /** Customer's first, middle and last name. */
        public val customerName: PersonName? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountType == null) { "PaymentMethod.AbsaPay does not take account.accountType" }
            require(customerName == null || customerName.lastName != null) { "PaymentMethod.AbsaPay needs customerName.lastName" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "ABSA_PAY")
            .putOpt("account", account?.toJson())
            .putOpt("emailAddress", emailAddress)
            .putOpt("passportNumber", passportNumber)
            .putOpt("said", said)
            .putOpt("customerName", customerName?.toJson())
    }

    /** Capitec Pay is an easy, fast and safe way to pay without having to enter the bank card details or share banking login information. The customer can choose the account he wants to pay from and authenticate the payment safely in the banking app. */
    public data class CapitecPay(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Your customer's mobile phone number, starting with 0. min 12, max 12. */
        public val phoneNumber: String? = null,
        /** min 13, max 13, pattern `^[0-9]*$`. */
        public val said: String? = null,
        /** Customer's first, middle and last name. */
        public val customerName: PersonName? = null,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountType == null) { "PaymentMethod.CapitecPay does not take account.accountType" }
            require(customerName == null || customerName.lastName != null) { "PaymentMethod.CapitecPay needs customerName.lastName" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "CAPITEC_PAY")
            .putOpt("account", account?.toJson())
            .putOpt("emailAddress", emailAddress)
            .putOpt("phoneNumber", phoneNumber)
            .putOpt("said", said)
            .putOpt("customerName", customerName?.toJson())
            .putOpt("paymentOperatorCode", paymentOperatorCode)
    }

    /** Card payment method. */
    public data class Card(
        public val card: CardData,
        public val paymentContacts: CardPaymentContacts? = null,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
        public val saveCardForFutureUse: Boolean? = null,
        public val recurring: RecurringPayment? = null,
    ) : PaymentMethod {
        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "CARD")
            .put("card", card.toJson())
            .putOpt("paymentContacts", paymentContacts?.toJson())
            .putOpt("paymentOperatorCode", paymentOperatorCode)
            .putOpt("saveCardForFutureUse", saveCardForFutureUse)
            .putOpt("recurring", recurring?.toJson())

        public data class CardData(
            public val cardDataVault: CardDataVault? = null,
        ) {
            public fun toJson(): JSONObject = JSONObject()
                .putOpt("cardDataVault", cardDataVault?.toJson())
        }

        public data class CardDataVault(
            /** min 1. */
            public val vaultToken: String,
            public val securityCode: EnvelopeEncryption? = null,
        ) {
            public fun toJson(): JSONObject = JSONObject()
                .put("vaultToken", vaultToken)
                .putOpt("securityCode", securityCode?.toJson())
        }

        public data class CardPaymentContact(
            /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
            public val phoneNumber: String? = null,
            /** min 1. */
            public val administrativeArea: String? = null,
            /** min 1. */
            public val country: String? = null,
            /** min 1. */
            public val countryCode: String? = null,
            /** min 1. */
            public val locality: String? = null,
            /** min 1. */
            public val postalCode: String? = null,
            /** Person's first, middle and last name. */
            public val personName: PersonName? = null,
            /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
            public val emailAddress: String? = null,
            public val addressLines: List<String>? = null,
        ) {
            init {
                require(personName == null || personName.lastName != null) { "PaymentMethod.Card.CardPaymentContact needs personName.lastName" }
            }

            public fun toJson(): JSONObject = JSONObject()
                .putOpt("addressLines", addressLines?.let { JSONArray(it) })
                .putOpt("administrativeArea", administrativeArea)
                .putOpt("country", country)
                .putOpt("countryCode", countryCode)
                .putOpt("locality", locality)
                .putOpt("postalCode", postalCode)
                .putOpt("personName", personName?.toJson())
                .putOpt("emailAddress", emailAddress)
                .putOpt("phoneNumber", phoneNumber)
        }

        public data class CardPaymentContacts(
            public val billingContact: CardPaymentContact? = null,
            public val shippingContact: CardPaymentContact? = null,
        ) {
            public fun toJson(): JSONObject = JSONObject()
                .putOpt("billingContact", billingContact?.toJson())
                .putOpt("shippingContact", shippingContact?.toJson())
        }

        public data class EnvelopeEncryption(
            /** min 1. */
            public val cipherText: String,
            /** min 1. */
            public val wrappedDEK: String,
            /** min 1. */
            public val initializationVector: String,
            /** min 1. */
            public val kekId: String,
        ) {
            public fun toJson(): JSONObject = JSONObject()
                .put("cipherText", cipherText)
                .put("wrappedDEK", wrappedDEK)
                .put("initializationVector", initializationVector)
                .put("kekId", kekId)
        }

        public data class RecurringPayment(
            public val isFixedAmount: Boolean,
            public val isFixedPeriod: Boolean,
            public val idOfInitialTransaction: String? = null,
        ) {
            public fun toJson(): JSONObject = JSONObject()
                .putOpt("idOfInitialTransaction", idOfInitialTransaction)
                .put("isFixedAmount", isFixedAmount)
                .put("isFixedPeriod", isFixedPeriod)
        }
    }

    /** Convenience Store Payments. Payment method which requires customer to copy Payment Code from the Payment application right after the payment is submitted into kiosk in convenience store during the payment on the cashier. */
    public data class CodePayment(
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String,
        /** Specifies location where a customer is making the payment (e.g. */
        public val paymentLocation: PaymentLocation,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
    ) : PaymentMethod {
        init {
            require(account == null || account.accountName != null) { "PaymentMethod.CodePayment needs account.accountName" }
            require(account?.accountNumber == null) { "PaymentMethod.CodePayment does not take account.accountNumber" }
            require(account?.accountType == null) { "PaymentMethod.CodePayment does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "CODE_PAYMENT")
            .putOpt("account", account?.toJson())
            .put("paymentOperatorCode", paymentOperatorCode)
            .put("emailAddress", emailAddress)
            .put("phoneNumber", phoneNumber)
            .put("paymentLocation", paymentLocation.toJson())
    }

    /** Crypto Offline is a payment method that allows customers to pay by crypto currency from they crypto wallet to an address that they are provided. */
    public data class CryptoOffline(
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "CRYPTO_OFFLINE")
            .put("paymentOperatorCode", paymentOperatorCode)
            .putOpt("emailAddress", emailAddress)
    }

    /** Payment method which requires customer to scan QR code from the Payment application by Customer's Payment service application right after the payment is submitted. */
    public data class DuitNow(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        init {
            require(account.accountName != null) { "PaymentMethod.DuitNow needs account.accountName" }
            require(account.accountType == null) { "PaymentMethod.DuitNow does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "DUITNOW")
            .put("account", account.toJson())
            .putOpt("paymentOperatorCode", paymentOperatorCode)
            .putOpt("emailAddress", emailAddress)
    }

    /** E-Wallet is a payment option on Indonesian market that allows customers to use major Indonesian wallets for payments. Customer is either automatically redirected to a chosen wallet with all payment details pre-populated to finalize the payment or directly receives push notification to authorize the payment in case of OVO wallet. */
    public data class EWallet(
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountNumber == null) { "PaymentMethod.EWallet does not take account.accountNumber" }
            require(account?.accountType == null) { "PaymentMethod.EWallet does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "EWALLET")
            .putOpt("account", account?.toJson())
            .putOpt("emailAddress", emailAddress)
            .putOpt("phoneNumber", phoneNumber)
            .put("paymentOperatorCode", paymentOperatorCode)
    }

    /** FNB Direct EFT is a digital payment method that enables customers to make secure payments directly through their FNB app. Rather than manually entering bank details, customers can approve the payment on their phone, creating a faster, safer, and more seamless checkout experience. */
    public data class FnbDirectEFT(
        /** Customer's first and last name. */
        public val customerName: PersonName,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** min 8, max 9, pattern `^[a-zA-Z][0-9]+$`. */
        public val passportNumber: String? = null,
        /** min 13, max 13, pattern `^[0-9]*$`. */
        public val said: String? = null,
    ) : PaymentMethod {
        init {
            require(customerName.firstName != null) { "PaymentMethod.FnbDirectEFT needs customerName.firstName" }
            require(customerName.lastName != null) { "PaymentMethod.FnbDirectEFT needs customerName.lastName" }
            require(customerName.titleBefore == null) { "PaymentMethod.FnbDirectEFT does not take customerName.titleBefore" }
            require(customerName.middleName == null) { "PaymentMethod.FnbDirectEFT does not take customerName.middleName" }
            require(customerName.titleAfter == null) { "PaymentMethod.FnbDirectEFT does not take customerName.titleAfter" }
            require(account.accountNumber != null) { "PaymentMethod.FnbDirectEFT needs account.accountNumber" }
            require(account.accountName == null) { "PaymentMethod.FnbDirectEFT does not take account.accountName" }
            require(account.accountType == null) { "PaymentMethod.FnbDirectEFT does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "FNB_DIRECT_EFT")
            .put("customerName", customerName.toJson())
            .put("emailAddress", emailAddress)
            .put("account", account.toJson())
            .putOpt("passportNumber", passportNumber)
            .putOpt("said", said)
    }

    /** FPX (Financial Process Exchange) is a real-time online payment system in Malaysia that enables secure, instant payments by redirecting users to their selected FPX-participating banks. */
    public data class Fpx(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        init {
            require(account.accountName != null) { "PaymentMethod.Fpx needs account.accountName" }
            require(account.accountType != null) { "PaymentMethod.Fpx needs account.accountType" }
            require(account.accountNumber == null) { "PaymentMethod.Fpx does not take account.accountNumber" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "FPX")
            .put("account", account.toJson())
            .putOpt("emailAddress", emailAddress)
            .put("paymentOperatorCode", paymentOperatorCode)
    }

    /** It is India market version of the Offline Bank Transfer which differs in the data which need to be collected and used during a Payment process. */
    public data class IMPS(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        init {
            require(account.accountName != null) { "PaymentMethod.IMPS needs account.accountName" }
            require(account.accountType == null) { "PaymentMethod.IMPS does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "IMPS")
            .put("account", account.toJson())
            .putOpt("emailAddress", emailAddress)
    }

    /** Mobile Money is a payment method which allows customers to simply complete a payment from their mobile money wallet. */
    public data class MobileMoney(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        init {
            require(account.accountName != null) { "PaymentMethod.MobileMoney needs account.accountName" }
            require(account.accountNumber == null) { "PaymentMethod.MobileMoney does not take account.accountNumber" }
            require(account.accountType == null) { "PaymentMethod.MobileMoney does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "MOBILE_MONEY")
            .put("account", account.toJson())
            .putOpt("paymentOperatorCode", paymentOperatorCode)
            .putOpt("emailAddress", emailAddress)
            .put("phoneNumber", phoneNumber)
    }

    /** MoMo is a mobile payment method that allows users to complete transactions via the MoMo app. Payments are initiated through QR codes, where users authorize the transaction within the app. */
    public data class MomoPay(
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "MOMOPAY")
            .putOpt("emailAddress", emailAddress)
    }

    /** Nedbank Direct EFT is a digital payment method that allows customers to pay quickly and securely using their Nedbank app. Instead of entering bank details, customers simply approve the payment on their phone, making the checkout process faster, safer, and more convenient. */
    public data class NedbankDirectEFT(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** min 13, max 13, pattern `^[0-9]*$`. */
        public val said: String,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Customer's first, middle and last name. */
        public val customerName: PersonName? = null,
    ) : PaymentMethod {
        init {
            require(account.accountName != null) { "PaymentMethod.NedbankDirectEFT needs account.accountName" }
            require(account.accountNumber == null) { "PaymentMethod.NedbankDirectEFT does not take account.accountNumber" }
            require(account.accountType == null) { "PaymentMethod.NedbankDirectEFT does not take account.accountType" }
            require(customerName == null || customerName.lastName != null) { "PaymentMethod.NedbankDirectEFT needs customerName.lastName" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "NEDBANK_DIRECT_EFT")
            .put("account", account.toJson())
            .put("said", said)
            .putOpt("emailAddress", emailAddress)
            .putOpt("customerName", customerName?.toJson())
    }

    /** Netbanking, also known as online banking or Net Banking is a payment option offered by banks and financial institutions on Indian market that allow customers to use banking services over the internet. In our case, we take advantage of the Netbanking services to offer your customer UPI Payment method to pay funds directly from your customer Net Banking account. */
    public data class NetBanking(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountType == null) { "PaymentMethod.NetBanking does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "NETBANKING")
            .putOpt("account", account?.toJson())
            .putOpt("emailAddress", emailAddress)
            .putOpt("phoneNumber", phoneNumber)
            .putOpt("paymentOperatorCode", paymentOperatorCode)
    }

    /** Offline Bank Transfer. Payment method which requires customer to copy Payment instruction from the Payment application right after the incoming payment is submitted and create the Payment transfer using the instructions within customer's own payment service such as Internet or mobile banking or wallet. */
    public data class Offline(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
        /** Identification of a product related to a payment order. max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val productId: String? = null,
        /** Tax payer identification number of a customer. min 8, max 18. */
        public val taxId: String? = null,
        /** Customer's first, middle and last name. */
        public val customerName: PersonName? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountType == null) { "PaymentMethod.Offline does not take account.accountType" }
            require(customerName == null || customerName.firstName != null) { "PaymentMethod.Offline needs customerName.firstName" }
            require(customerName == null || customerName.lastName != null) { "PaymentMethod.Offline needs customerName.lastName" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "OFFLINE")
            .putOpt("account", account?.toJson())
            .putOpt("paymentOperatorCode", paymentOperatorCode)
            .putOpt("emailAddress", emailAddress)
            .putOpt("phoneNumber", phoneNumber)
            .putOpt("productId", productId)
            .putOpt("taxId", taxId)
            .putOpt("customerName", customerName?.toJson())
    }

    /** Online Bank Transfer. Payment method where Customer is redirected to his own preferred Payment service application right after the payment is submitted and create the Payment transfer with pre-filled Payment instruction. */
    public data class Online(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "ONLINE")
            .putOpt("account", account?.toJson())
            .putOpt("paymentOperatorCode", paymentOperatorCode)
            .putOpt("emailAddress", emailAddress)
    }

    /** OTT Voucher is a cash-to-digital payment method that enables customers to make secure online payments using prepaid vouchers. Rather than relying on a credit card or bank account, customers simply enter a unique 12-digit PIN at checkout, creating an accessible, safe, and seamless payment experience. */
    public data class OttVoucher(
        /** OTT voucher PIN. min 1. */
        public val voucherPin: String,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String,
        /** Person's first, middle and last name. */
        public val customerName: PersonName,
    ) : PaymentMethod {
        init {
            require(customerName.firstName != null) { "PaymentMethod.OttVoucher needs customerName.firstName" }
            require(customerName.lastName != null) { "PaymentMethod.OttVoucher needs customerName.lastName" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "OTT_VOUCHER")
            .put("voucherPin", voucherPin)
            .put("phoneNumber", phoneNumber)
            .put("emailAddress", emailAddress)
            .put("customerName", customerName.toJson())
    }

    /** Phone to Account. Payment method which requires customer to scan QR code from the Payment application by Customer's Payment service application right after the payment is submitted. */
    public data class P2AV2(
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountType == null) { "PaymentMethod.P2AV2 does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "P2A_V2")
            .putOpt("account", account?.toJson())
            .put("paymentOperatorCode", paymentOperatorCode)
            .putOpt("emailAddress", emailAddress)
    }

    /** PayShap offers a quick, easy, and secure way to send money without having to input banking login details. Customers can make payments using just a phone number or account number, with transactions processed instantly. */
    public data class PayShap(
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String,
        /** Your customer's mobile phone number, starting with 0. min 12, max 12. */
        public val phoneNumber: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** min 8, max 9, pattern `^[a-zA-Z][0-9]+$`. */
        public val passportNumber: String? = null,
        /** min 13, max 13, pattern `^[0-9]*$`. */
        public val said: String? = null,
        /** Customer's first, middle and last name. */
        public val customerName: PersonName? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** min 1. */
        public val externalProviderCode: String? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountType == null) { "PaymentMethod.PayShap does not take account.accountType" }
            require(customerName == null || customerName.lastName != null) { "PaymentMethod.PayShap needs customerName.lastName" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "PAYSHAP")
            .putOpt("account", account?.toJson())
            .putOpt("phoneNumber", phoneNumber)
            .putOpt("emailAddress", emailAddress)
            .putOpt("passportNumber", passportNumber)
            .putOpt("said", said)
            .putOpt("customerName", customerName?.toJson())
            .put("paymentOperatorCode", paymentOperatorCode)
            .putOpt("externalProviderCode", externalProviderCode)
    }

    /** Pay Me is payment method intended for South Africa market which allows customers to send payments by scanning QR codes from supported banks and e-wallets. */
    public data class PayMe(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
    ) : PaymentMethod {
        init {
            require(account.accountName != null) { "PaymentMethod.PayMe needs account.accountName" }
            require(account.accountType == null) { "PaymentMethod.PayMe does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "PAY_ME")
            .put("account", account.toJson())
            .putOpt("emailAddress", emailAddress)
            .putOpt("phoneNumber", phoneNumber)
    }

    /** Pix is a Brazilian payment method which allows customers to send payments using their preferred banking app by either scanning QR code or copy-pasting Pix EMV code. */
    public data class PixQR(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** Brazilian tax payer identification number - CPF for individuals / CNPJ for businesses. min 14, max 14. */
        public val taxId: String,
    ) : PaymentMethod {
        init {
            require(account.accountName != null) { "PaymentMethod.PixQR needs account.accountName" }
            require(account.accountNumber == null) { "PaymentMethod.PixQR does not take account.accountNumber" }
            require(account.accountType == null) { "PaymentMethod.PixQR does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "PIXQR")
            .put("account", account.toJson())
            .put("taxId", taxId)
    }

    /** Payment method which requires customer to scan QR code from the Payment application by Customer's Payment service application right after the payment is submitted. */
    public data class PromptPay(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        init {
            require(account == null || account.accountNumber != null) { "PaymentMethod.PromptPay needs account.accountNumber" }
            require(account?.accountType == null) { "PaymentMethod.PromptPay does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "PROMPTPAY")
            .putOpt("account", account?.toJson())
            .putOpt("paymentOperatorCode", paymentOperatorCode)
            .putOpt("emailAddress", emailAddress)
    }

    /** PSE (Pagos Seguros en L?nea) is a real-time online payment system in Colombia that enables secure, instant bank transfer payments - customers simply log in into their internet banking and authorize pre-populated payment details. */
    public data class Pse(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String,
    ) : PaymentMethod {
        init {
            require(account.accountName != null) { "PaymentMethod.Pse needs account.accountName" }
            require(account.accountNumber == null) { "PaymentMethod.Pse does not take account.accountNumber" }
            require(account.accountType == null) { "PaymentMethod.Pse does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "PSE")
            .put("account", account.toJson())
            .put("emailAddress", emailAddress)
            .put("phoneNumber", phoneNumber)
    }

    /** QRIS is payment method intended for the Indonesian market which allows users to pay using scanning QR codes by theirs Payment application. */
    public data class QrisPay(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountType == null) { "PaymentMethod.QrisPay does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "QRISPAY")
            .putOpt("account", account?.toJson())
            .putOpt("emailAddress", emailAddress)
    }

    /** QR Ph is payment method intended for Philippine market which allows customers to send payments by scanning QR codes from supported banks and e-wallets. */
    public data class QrPh(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountType == null) { "PaymentMethod.QrPh does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "QRPH")
            .putOpt("account", account?.toJson())
            .putOpt("emailAddress", emailAddress)
            .putOpt("paymentOperatorCode", paymentOperatorCode)
            .putOpt("phoneNumber", phoneNumber)
    }

    /** SnapScan is a QR-based payment method that allows customers to make fast and secure payments by scanning a QR code using the SnapScan app or by clicking on the payment deep link. */
    public data class SnapScanQr(
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String,
        /** Person's first, middle and last name. */
        public val customerName: PersonName,
    ) : PaymentMethod {
        init {
            require(customerName.firstName != null) { "PaymentMethod.SnapScanQr needs customerName.firstName" }
            require(customerName.lastName != null) { "PaymentMethod.SnapScanQr needs customerName.lastName" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "SNAPSCAN_QR")
            .put("emailAddress", emailAddress)
            .put("customerName", customerName.toJson())
    }

    /** UPI Collect is a method of fund transfers in India. This payment method allows users to simply authorize payments through their registered UPI app after receiving a payment collection request. */
    public data class UpiCollect(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** Virtual payment address of the customer on UPI provided by the merchant or obtained during the processing on our platform. max 100, pattern `[a-zA-Z0-9_\-\.]+@[a-zA-Z0-9_\.]+`. */
        public val upiId: String,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String,
    ) : PaymentMethod {
        init {
            require(account.accountName != null) { "PaymentMethod.UpiCollect needs account.accountName" }
            require(account.accountNumber == null) { "PaymentMethod.UpiCollect does not take account.accountNumber" }
            require(account.accountType == null) { "PaymentMethod.UpiCollect does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "UPICOLLECT")
            .put("account", account.toJson())
            .put("upiId", upiId)
            .put("emailAddress", emailAddress)
            .put("phoneNumber", phoneNumber)
    }

    /** UPI (Unified Payments Interface) is a method of fund transfers in India. The mechanism allows two parties to exchange funds using a UPI ID without having to share the banking information with the other party. */
    public data class UpiId(
        /** Virtual payment address of the customer on UPI provided by the merchant or obtained during the processing on our platform. max 100, pattern `[a-zA-Z0-9_\-\.]+@[a-zA-Z0-9_\.]+`. */
        public val upiId: String,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "UPIID")
            .put("upiId", upiId)
            .putOpt("emailAddress", emailAddress)
    }

    /** UPI QR is payment method intended for the Indian market. This payment method requires customers to scan QR code by Customer's Payment service application compatible with UPI payment schema. */
    public data class UpiQR(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** Virtual payment address of the customer on UPI provided by the merchant or obtained during the processing on our platform. max 100, pattern `[a-zA-Z0-9_\-\.]+@[a-zA-Z0-9_\.]+`. */
        public val upiId: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountNumber == null) { "PaymentMethod.UpiQR does not take account.accountNumber" }
            require(account?.accountType == null) { "PaymentMethod.UpiQR does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "UPIQR")
            .putOpt("account", account?.toJson())
            .putOpt("upiId", upiId)
            .putOpt("emailAddress", emailAddress)
            .putOpt("phoneNumber", phoneNumber)
            .putOpt("paymentOperatorCode", paymentOperatorCode)
    }

    /** Virtual Accounts. Payment method which requires customer to copy Payment instruction with generated Virtual Account from the Payment application right after the payment is submitted and create the Payment transfer using the instructions within customer's own payment service such as Internet or mobile banking, wallet or ATM. */
    public data class VaPay(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account? = null,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
        /** Specifies location where a customer is making the payment (e.g. */
        public val paymentLocation: PaymentLocation? = null,
    ) : PaymentMethod {
        init {
            require(account?.accountType == null) { "PaymentMethod.VaPay does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "VAPAY")
            .putOpt("account", account?.toJson())
            .putOpt("paymentOperatorCode", paymentOperatorCode)
            .putOpt("emailAddress", emailAddress)
            .putOpt("phoneNumber", phoneNumber)
            .putOpt("paymentLocation", paymentLocation?.toJson())
    }

    /** Virtual Accounts With Verified Bank Account. Payment method which requires customer to copy Payment instruction with assigned Virtual Account from the Payment application right after the payment is submitted and create the Payment transfer using the instructions within customer's own payment service such as Internet or mobile banking, wallet or ATM. */
    public data class VaPayVerif(
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val account: Account,
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        init {
            require(account.accountNumber != null) { "PaymentMethod.VaPayVerif needs account.accountNumber" }
            require(account.accountType == null) { "PaymentMethod.VaPayVerif does not take account.accountType" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "VAPAY_VERIF")
            .put("account", account.toJson())
            .put("paymentOperatorCode", paymentOperatorCode)
            .putOpt("emailAddress", emailAddress)
    }

    /** Viet QR is a payment method intended for the Vietnamese market which allows users to pay using scanning QR codes by theirs payment application or internet banking. */
    public data class VietQR(
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val paymentOperatorCode: String,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
    ) : PaymentMethod {
        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "VIETQR")
            .put("paymentOperatorCode", paymentOperatorCode)
            .putOpt("emailAddress", emailAddress)
    }

    /** Zapper is a QR-based payment method that allows customers to make fast and secure payments by scanning a QR code using the Zapper app or by clicking on the payment deep link. */
    public data class ZapperQr(
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String,
        /** Customer's first and last name. */
        public val customerName: PersonName,
    ) : PaymentMethod {
        init {
            require(customerName.firstName != null) { "PaymentMethod.ZapperQr needs customerName.firstName" }
            require(customerName.lastName != null) { "PaymentMethod.ZapperQr needs customerName.lastName" }
            require(customerName.titleBefore == null) { "PaymentMethod.ZapperQr does not take customerName.titleBefore" }
            require(customerName.middleName == null) { "PaymentMethod.ZapperQr does not take customerName.middleName" }
            require(customerName.titleAfter == null) { "PaymentMethod.ZapperQr does not take customerName.titleAfter" }
        }

        override fun toJson(): JSONObject = JSONObject()
            .put("paymentMethodCode", "ZAPPER_QR")
            .put("emailAddress", emailAddress)
            .put("customerName", customerName.toJson())
    }

    /** The payer's bank or wallet account. Each method needs different fields and checks them when you create it. */
    public data class Account(
        /** Account Name is the name of the person who holds the bank or wallet account which your customer sends funds from to make his payment. Rules differ per payment method. */
        public val accountName: String? = null,
        /** Account Number is the number of your customer's bank account which your customer sends funds from to make his payment. Rules differ per payment method. */
        public val accountNumber: String? = null,
        /** The customer's account type. Rules differ per payment method. */
        public val accountType: String? = null,
    ) {
        public fun toJson(): JSONObject = JSONObject()
            .putOpt("accountName", accountName)
            .putOpt("accountNumber", accountNumber)
            .putOpt("accountType", accountType)
    }

    public data class PaymentLocation(
        /** Postal code where customer is paying at (e.g. min 1. */
        public val postalCode: String,
        /** City which customer is paying in (e.g. min 1. */
        public val city: String,
        /** Disctrict which customer is paying in (e.g. min 1. */
        public val district: String,
    ) {
        public fun toJson(): JSONObject = JSONObject()
            .put("postalCode", postalCode)
            .put("city", city)
            .put("district", district)

        public companion object {
            public fun fromJson(json: JSONObject): PaymentLocation = PaymentLocation(
                postalCode = requireNotNull(json.str("postalCode")) { "PaymentLocation.postalCode missing" },
                city = requireNotNull(json.str("city")) { "PaymentLocation.city missing" },
                district = requireNotNull(json.str("district")) { "PaymentLocation.district missing" },
            )
        }
    }

    /** A person's name. Each method needs different parts and checks them when you create it. */
    public data class PersonName(
        /** A title before the name. min 1. */
        public val titleBefore: String? = null,
        /** A first name. min 1. */
        public val firstName: String? = null,
        /** A middle name. min 1. */
        public val middleName: String? = null,
        /** A last name. min 1. */
        public val lastName: String? = null,
        /** A title after the name. min 1. */
        public val titleAfter: String? = null,
    ) {
        public fun toJson(): JSONObject = JSONObject()
            .putOpt("titleBefore", titleBefore)
            .putOpt("firstName", firstName)
            .putOpt("middleName", middleName)
            .putOpt("lastName", lastName)
            .putOpt("titleAfter", titleAfter)
    }
}

/** `paymentMethodResponse` of a created payin. Unknown methods parse to [Unknown]. */
public sealed interface PaymentMethodResponse {

    public data class AbsaPay(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        public val accounts: List<Account>? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val accountCustomer: Account? = null,
        /** Contains information about the account used by the customer. */
        public val accountCustomerUsedForPayment: Account? = null,
        /** min 8, max 9, pattern `^[a-zA-Z][0-9]+$`. */
        public val passportNumber: String? = null,
        /** min 13, max 13, pattern `^[0-9]*$`. */
        public val said: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class CapitecPay(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val accountCustomer: Account? = null,
        /** Contains information about the account used by the customer. */
        public val accountCustomerUsedForPayment: Account? = null,
        /** Your customer's mobile phone number, starting with 0. min 12, max 12. */
        public val phoneNumber: String? = null,
        /** min 13, max 13, pattern `^[0-9]*$`. */
        public val said: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class Card(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
        /** TODO: */
        public val userInteraction: UserInteraction? = null,
    ) : PaymentMethodResponse

    public data class CodePayment(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of a Convenience Store Payment that your customer use to send funds to make a payment. Always present per spec. */
        public val account: Account? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. Always present per spec. */
        public val emailAddress: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. Always present per spec. */
        public val phoneNumber: String? = null,
        /** Specifies location where a customer is making the payment (e.g. Always present per spec. */
        public val paymentLocationCustomer: PaymentMethod.PaymentLocation? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). Always present per spec. */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class CryptoOffline(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of a crypto wallet where we expect that your customer send funds to make a payment. Always present per spec. */
        public val account: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). Always present per spec. */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class DuitNow(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        public val moneyRequired: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class EWallet(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class FnbDirectEFT(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. Always present per spec. */
        public val emailAddress: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** min 8, max 9, pattern `^[a-zA-Z][0-9]+$`. */
        public val passportNumber: String? = null,
        /** min 13, max 13, pattern `^[0-9]*$`. */
        public val said: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class Fpx(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    /** The account parameters for this payment method are used to show payment instructions to the customer. */
    public data class IMPS(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of a bank account where we expect that your customer send funds to make a payment. Always present per spec. */
        public val account: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** Always present per spec. */
        public val qrCode: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class MobileMoney(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. Always present per spec. */
        public val phoneNumber: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class MomoPay(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** The raw string / payload encoded within the QR code to be used for the Copy and Paste functionality or rendered as a QR code. Always present per spec. */
        public val qrRawData: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class NedbankDirectEFT(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** min 13, max 13, pattern `^[0-9]*$`. Always present per spec. */
        public val said: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class NetBanking(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Virtual payment address where we expect that your customer sends funds to make a payment. max 100, pattern `[a-zA-Z0-9_\-\.]+@[a-zA-Z0-9_\.]+`. */
        public val upiId: String? = null,
        /** It can be used as deep link button target (what is typically known as an intent trigger) or to generate a QR code that can be scanned with any UPI enabled app. */
        public val deepLink: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    /** The account parameters for this payment method are used to show payment instructions to the customer. */
    public data class Offline(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of a bank account where we expect that your customer send funds to make a payment. Always present per spec. */
        public val account: Account? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val moneyRequired: Money? = null,
        public val vat: Money? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** The external processor of the payment. one of POC, PAYSTAGE, MTPX, TPAY, LCSX, PRL. */
        public val processor: String? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. */
        public val qrCode: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Identification of a product related to a payment order. max 30, pattern `^[A-Za-z0-9_-]+$`. */
        public val productId: String? = null,
        /** Tax payer identification number of a customer. min 8, max 18. */
        public val taxId: String? = null,
        /** Tax payer identification number of a customer. min 8, max 18. */
        public val taxIdCustomer: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class Online(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of a bank account where we expect that your customer send funds to make a payment. Always present per spec. */
        public val account: Account? = null,
        /** Contains information about the account used by the customer. */
        public val accountCustomerUsedForPayment: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val moneyRequired: Money? = null,
        public val vat: Money? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class OttVoucher(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. Always present per spec. */
        public val phoneNumber: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. Always present per spec. */
        public val emailAddress: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    /** The account parameters for this payment method are used to show payment instructions to the customer. */
    public data class P2AV2(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of a bank account where we expect that your customer send funds to make a payment. Always present per spec. */
        public val account: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Specifies the payment address (e.g. Always present per spec. */
        public val paymentAddress: PaymentAddress? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** External payment / transaction reference is a unique identifier assigned to a financial transaction. */
        public val externalReference: ExternalReference? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). Always present per spec. */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse {
        /** Specifies the payment address (e.g. in UPI payment system) where customer can send the payment using theirs wallets or others banking services. */
        public data class PaymentAddress(
            /** The name of the payment address ID e.g. Always present per spec. */
            public val name: String? = null,
            /** The payment address ID where customer can send the payment. Always present per spec. */
            public val idAddress: String? = null,
        ) {
            public companion object {
                public fun fromJson(json: JSONObject): PaymentAddress = PaymentAddress(
                    name = json.str("name"),
                    idAddress = json.str("idAddress"),
                )
            }
        }
    }

    public data class PayShap(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val accountCustomer: Account? = null,
        /** Your customer's mobile phone number, starting with 0. min 12, max 12. */
        public val phoneNumber: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). Always present per spec. */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** TODO: */
        public val externalProvider: ExternalProvider? = null,
        /** min 13, max 13, pattern `^[0-9]*$`. */
        public val said: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse {
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
    }

    public data class PayMe(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of a bank account where we expect that your customer send funds to make a payment. Always present per spec. */
        public val account: Account? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val moneyRequired: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** It can be used as deep link button target (what is typically known as an intent trigger) or to generate a QR code that can be scanned with any UPI enabled app. */
        public val payMeDeepLink: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class PixQR(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** The raw string / payload encoded within the QR code to be used for the Copy and Paste functionality or rendered as a QR code. Always present per spec. */
        public val qrRawData: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Brazilian tax payer identification number - CPF for individuals / CNPJ for businesses. min 14, max 14. Always present per spec. */
        public val taxId: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class PromptPay(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        public val moneyRequired: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class Pse(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. Always present per spec. */
        public val emailAddress: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. Always present per spec. */
        public val phoneNumber: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class QrisPay(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Contains information about the account used by the customer. */
        public val accountCustomerUsedForPayment: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** The raw string / payload encoded within the QR code (e.g. Always present per spec. */
        public val qrRawData: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class QrPh(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of a bank account where we expect that your customer send funds to make a payment. Always present per spec. */
        public val account: Account? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val moneyRequired: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class SnapScanQr(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** The raw string / payload encoded within the QR code to be used for the Copy and Paste functionality or rendered as a QR code. Always present per spec. */
        public val qrRawData: String? = null,
        /** Can be used as a deep link target to launch the app directly, or to generate a QR code that can be scanned by SnapScan application. Always present per spec. */
        public val snapscanQrDeepLink: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class UpiCollect(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val accountCustomer: Account? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Virtual payment address of the customer on UPI provided by the merchant or obtained during the processing on our platform. max 100, pattern `[a-zA-Z0-9_\-\.]+@[a-zA-Z0-9_\.]+`. Always present per spec. */
        public val upiIdCustomer: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class UpiId(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class UpiQR(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. */
        public val accountCustomer: Account? = null,
        /** Contains information about the account used by the customer. */
        public val accountCustomerUsedForPayment: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** External payment / transaction reference is a unique identifier assigned to a financial transaction. */
        public val externalReference: ExternalReference? = null,
        /** The external processor of the payment. one of POC, PAYSTAGE, MTPX, TPAY, LCSX, PRL. */
        public val processor: String? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** It can be used as deep link button target (what is typically known as an intent trigger) or to generate a QR code that can be scanned with any UPI enabled app. */
        public val upiQrDeepLink: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** Virtual payment address where we expect that your customer sends funds to make a payment. max 100, pattern `[a-zA-Z0-9_\-\.]+@[a-zA-Z0-9_\.]+`. */
        public val upiId: String? = null,
        /** Virtual payment address of the customer on UPI provided by the merchant or obtained during the processing on our platform. max 100, pattern `[a-zA-Z0-9_\-\.]+@[a-zA-Z0-9_\.]+`. */
        public val upiIdCustomer: String? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    /** The account parameters for this payment method are used to show payment instructions to the customer. */
    public data class VaPay(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of a bank account where we expect that your customer send funds to make a payment. Always present per spec. */
        public val account: Account? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** The external processor of the payment. one of POC, PAYSTAGE, MTPX, TPAY, LCSX, PRL. */
        public val processor: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** One of following can serve as Payment Operator: */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** Your customer's mobile phone number in full international telephone number format, including country code. min 7, max 16, pattern `^\+?[0-9]+$`. */
        public val phoneNumber: String? = null,
        /** Your customer's e-mail address in RFC 5322 format that is used for identification of the customer's payins. */
        public val emailAddress: String? = null,
        /** Specifies location where a customer is making the payment (e.g. */
        public val paymentLocationCustomer: PaymentMethod.PaymentLocation? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
        public val paymentInstructionList: List<PaymentInstruction>? = null,
    ) : PaymentMethodResponse {
        /** If not empty then it specifies special instructions for different banking services your customers has to follow when entering payment in their banking service. */
        public data class PaymentInstruction(
            /** It specifies your customer banking service to which companyCode belongs. Always present per spec. */
            public val bankingService: String? = null,
            /** It informs your customers that companyCode has to be used in their banking service together with Virtual Account Number from accountNumber parameter e.g. Always present per spec. */
            public val companyCode: String? = null,
        ) {
            public companion object {
                public fun fromJson(json: JSONObject): PaymentInstruction = PaymentInstruction(
                    bankingService = json.str("bankingService"),
                    companyCode = json.str("companyCode"),
                )
            }
        }
    }

    public data class VaPayVerif(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Parameters of a bank account where we expect that your customer send funds to make a payment. Always present per spec. */
        public val account: Account? = null,
        /** Parameters of your customer's bank or wallet account which your customer sends funds from. Always present per spec. */
        public val accountCustomer: Account? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). Always present per spec. */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class VietQR(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** The name of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** PaymentOperator is a financial institution that manages transactions in your customer's account (such as banks, card payment processors, mobile wallets and so on). */
        public val paymentOperator: SelectedPaymentOperatorIncoming? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    public data class ZapperQr(
        /** Unique Merchant Order ID provided by merchant. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayin: String? = null,
        /** The Unique reference ID of the Order generated by platform. max 250, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val idPayment: String? = null,
        /** Always present per spec. */
        public val money: Money? = null,
        public val vat: Money? = null,
        /** Always present per spec. */
        public val merchantName: String? = null,
        /** The URL of the QR code image to be scanned by a wallet or payment service compatible with this payment method. Always present per spec. */
        public val qrCode: String? = null,
        /** The raw string / payload encoded within the QR code to be used for the Copy and Paste functionality or rendered as a QR code. Always present per spec. */
        public val qrRawData: String? = null,
        /** Can be used as a deep link target to launch the app directly, or to generate a QR code that can be scanned by Zapper application. Always present per spec. */
        public val zapperQrDeepLink: String? = null,
        /** Reference number of transaction. Always present per spec. */
        public val reference: String? = null,
        /** This is the URL where the customers will be redirected after completing a payment. max 1000. Always present per spec. */
        public val returnUrl: String? = null,
        /** Date and time when payment was accepted. date-time. Always present per spec. */
        public val acceptedAt: String? = null,
        /** Date and time of payment expiration. date-time. Always present per spec. */
        public val expireAt: String? = null,
    ) : PaymentMethodResponse

    /** An account as SYNQT reports it: the payer's, or the one to pay into. Which fields are set depends on the method. */
    public data class Account(
        /** Name of the bank account where we expect that your customer sends funds to make a payment. Rules differ per payment method. */
        public val accountName: String? = null,
        /** Number of the bank account where we expect that your customer sends funds to make a payment. Rules differ per payment method. */
        public val accountNumber: String? = null,
        /** Account type of the receiving bank account e.g. Rules differ per payment method. */
        public val accountType: String? = null,
        /** Bank code of the bank where we expect that your customer sends funds to make a payment. Rules differ per payment method. */
        public val bankCode: String? = null,
        /** Name of the bank where we expect that your customer sends funds to make a payment. Rules differ per payment method. */
        public val bankName: String? = null,
        /** Branch name of the bank where we expect that your customer sends funds to make a payment. Rules differ per payment method. */
        public val bankBranch: String? = null,
        /** City of the bank where we expect that your customer sends funds to make a payment. min 1. */
        public val bankCity: String? = null,
        /** Province of the bank where we expect that your customer sends funds to make a payment. min 1. */
        public val bankProvince: String? = null,
        /** int32. */
        public val accountIndex: Int? = null,
    ) {
        public companion object {
            public fun fromJson(json: JSONObject): Account = Account(
                accountName = json.str("accountName"),
                accountNumber = json.str("accountNumber"),
                accountType = json.str("accountType"),
                bankCode = json.str("bankCode"),
                bankName = json.str("bankName"),
                bankBranch = json.str("bankBranch"),
                bankCity = json.str("bankCity"),
                bankProvince = json.str("bankProvince"),
                accountIndex = json.int("accountIndex"),
            )
        }
    }

    /** External payment / transaction reference is a unique identifier assigned to a financial transaction. Customers can usually find it in their banking service such as mobile wallets, internet banking services or bank slips. */
    public data class ExternalReference(
        /** min 1, max 512. Always present per spec. */
        public val reference: String? = null,
    ) {
        public companion object {
            public fun fromJson(json: JSONObject): ExternalReference = ExternalReference(
                reference = json.str("reference"),
            )
        }
    }

    /** An amount in major units and its ISO 4217 currency. */
    public data class Money(
        /** Actual amount that end-customer should pay if `paymentMethodResponse.moneyRequired.amount` is not returned. Rules differ per payment method. */
        public val amount: BigDecimal? = null,
        /** For supported currencies please refer to `GET /currencies`. min 1. */
        public val currencyCode: String? = null,
    ) {
        public companion object {
            public fun fromJson(json: JSONObject): Money = Money(
                amount = json.dec("amount"),
                currencyCode = json.str("currencyCode"),
            )
        }
    }

    public data class SelectedPaymentOperatorIncoming(
        /** One of following can serve as Payment Operator: min 1, max 30, pattern `^[A-Za-z0-9_-]+$`. Always present per spec. */
        public val code: String? = null,
        /** min 1, max 1000. Always present per spec. */
        public val name: String? = null,
    ) {
        public companion object {
            public fun fromJson(json: JSONObject): SelectedPaymentOperatorIncoming = SelectedPaymentOperatorIncoming(
                code = json.str("code"),
                name = json.str("name"),
            )
        }
    }

    /** A `paymentMethodCode` this SDK version does not know yet; new values are non-breaking API changes. */
    public data class Unknown(public val paymentMethodCode: String, public val json: JSONObject) : PaymentMethodResponse

    public companion object {
        public fun fromJson(json: JSONObject): PaymentMethodResponse = when (val code = json.optString("paymentMethodCode")) {
            "ABSA_PAY" -> AbsaPay(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                accounts = json.list("accounts") { array, index -> Account.fromJson(array.getJSONObject(index)) },
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                accountCustomerUsedForPayment = json.obj("accountCustomerUsedForPayment") { Account.fromJson(it) },
                passportNumber = json.str("passportNumber"),
                said = json.str("said"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "CAPITEC_PAY" -> CapitecPay(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                accountCustomerUsedForPayment = json.obj("accountCustomerUsedForPayment") { Account.fromJson(it) },
                phoneNumber = json.str("phoneNumber"),
                said = json.str("said"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "CARD" -> Card(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
                userInteraction = json.obj("userInteraction") { UserInteraction.fromJson(it) },
            )
            "CODE_PAYMENT" -> CodePayment(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                account = json.obj("account") { Account.fromJson(it) },
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                emailAddress = json.str("emailAddress"),
                phoneNumber = json.str("phoneNumber"),
                paymentLocationCustomer = json.obj("paymentLocationCustomer") { PaymentMethod.PaymentLocation.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "CRYPTO_OFFLINE" -> CryptoOffline(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                account = json.obj("account") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "DUITNOW" -> DuitNow(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                moneyRequired = json.obj("moneyRequired") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "EWALLET" -> EWallet(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "FNB_DIRECT_EFT" -> FnbDirectEFT(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                emailAddress = json.str("emailAddress"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                passportNumber = json.str("passportNumber"),
                said = json.str("said"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "FPX" -> Fpx(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                emailAddress = json.str("emailAddress"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "IMPS" -> IMPS(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                account = json.obj("account") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                qrCode = json.str("qrCode"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "MOBILE_MONEY" -> MobileMoney(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                emailAddress = json.str("emailAddress"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                phoneNumber = json.str("phoneNumber"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "MOMOPAY" -> MomoPay(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                emailAddress = json.str("emailAddress"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                qrCode = json.str("qrCode"),
                qrRawData = json.str("qrRawData"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "NEDBANK_DIRECT_EFT" -> NedbankDirectEFT(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                said = json.str("said"),
                emailAddress = json.str("emailAddress"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "NETBANKING" -> NetBanking(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                upiId = json.str("upiId"),
                deepLink = json.str("deepLink"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "OFFLINE" -> Offline(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                account = json.obj("account") { Account.fromJson(it) },
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                moneyRequired = json.obj("moneyRequired") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                reference = json.str("reference"),
                processor = json.str("processor"),
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                returnUrl = json.str("returnUrl"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                phoneNumber = json.str("phoneNumber"),
                emailAddress = json.str("emailAddress"),
                productId = json.str("productId"),
                taxId = json.str("taxId"),
                taxIdCustomer = json.str("taxIdCustomer"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "ONLINE" -> Online(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                account = json.obj("account") { Account.fromJson(it) },
                accountCustomerUsedForPayment = json.obj("accountCustomerUsedForPayment") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                moneyRequired = json.obj("moneyRequired") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "OTT_VOUCHER" -> OttVoucher(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                phoneNumber = json.str("phoneNumber"),
                emailAddress = json.str("emailAddress"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "P2A_V2" -> P2AV2(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                account = json.obj("account") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                paymentAddress = json.obj("paymentAddress") { P2AV2.PaymentAddress.fromJson(it) },
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                reference = json.str("reference"),
                externalReference = json.obj("externalReference") { ExternalReference.fromJson(it) },
                returnUrl = json.str("returnUrl"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "PAYSHAP" -> PayShap(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                phoneNumber = json.str("phoneNumber"),
                emailAddress = json.str("emailAddress"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                externalProvider = json.obj("externalProvider") { PayShap.ExternalProvider.fromJson(it) },
                said = json.str("said"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "PAY_ME" -> PayMe(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                account = json.obj("account") { Account.fromJson(it) },
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                moneyRequired = json.obj("moneyRequired") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                payMeDeepLink = json.str("payMeDeepLink"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "PIXQR" -> PixQR(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                qrRawData = json.str("qrRawData"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                taxId = json.str("taxId"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "PROMPTPAY" -> PromptPay(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                moneyRequired = json.obj("moneyRequired") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "PSE" -> Pse(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                emailAddress = json.str("emailAddress"),
                phoneNumber = json.str("phoneNumber"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "QRISPAY" -> QrisPay(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                accountCustomerUsedForPayment = json.obj("accountCustomerUsedForPayment") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                qrRawData = json.str("qrRawData"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "QRPH" -> QrPh(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                account = json.obj("account") { Account.fromJson(it) },
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                moneyRequired = json.obj("moneyRequired") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "SNAPSCAN_QR" -> SnapScanQr(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                qrCode = json.str("qrCode"),
                qrRawData = json.str("qrRawData"),
                snapscanQrDeepLink = json.str("snapscanQrDeepLink"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "UPICOLLECT" -> UpiCollect(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                phoneNumber = json.str("phoneNumber"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                upiIdCustomer = json.str("upiIdCustomer"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "UPIID" -> UpiId(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "UPIQR" -> UpiQR(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                accountCustomerUsedForPayment = json.obj("accountCustomerUsedForPayment") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                externalReference = json.obj("externalReference") { ExternalReference.fromJson(it) },
                processor = json.str("processor"),
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                upiQrDeepLink = json.str("upiQrDeepLink"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                upiId = json.str("upiId"),
                upiIdCustomer = json.str("upiIdCustomer"),
                phoneNumber = json.str("phoneNumber"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "VAPAY" -> VaPay(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                account = json.obj("account") { Account.fromJson(it) },
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                reference = json.str("reference"),
                processor = json.str("processor"),
                returnUrl = json.str("returnUrl"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                phoneNumber = json.str("phoneNumber"),
                emailAddress = json.str("emailAddress"),
                paymentLocationCustomer = json.obj("paymentLocationCustomer") { PaymentMethod.PaymentLocation.fromJson(it) },
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
                paymentInstructionList = json.list("paymentInstructionList") { array, index -> VaPay.PaymentInstruction.fromJson(array.getJSONObject(index)) },
            )
            "VAPAY_VERIF" -> VaPayVerif(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                account = json.obj("account") { Account.fromJson(it) },
                accountCustomer = json.obj("accountCustomer") { Account.fromJson(it) },
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "VIETQR" -> VietQR(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                reference = json.str("reference"),
                qrName = json.str("qrName"),
                qrCode = json.str("qrCode"),
                paymentOperator = json.obj("paymentOperator") { SelectedPaymentOperatorIncoming.fromJson(it) },
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            "ZAPPER_QR" -> ZapperQr(
                idPayin = json.str("idPayin"),
                idPayment = json.str("idPayment"),
                money = json.obj("money") { Money.fromJson(it) },
                vat = json.obj("vat") { Money.fromJson(it) },
                merchantName = json.str("merchantName"),
                qrCode = json.str("qrCode"),
                qrRawData = json.str("qrRawData"),
                zapperQrDeepLink = json.str("zapperQrDeepLink"),
                reference = json.str("reference"),
                returnUrl = json.str("returnUrl"),
                acceptedAt = json.str("acceptedAt"),
                expireAt = json.str("expireAt"),
            )
            else -> Unknown(code, json)
        }
    }
}

/** Extra step a CARD payment needs from the payer (3DS and similar). */
public sealed interface UserInteraction {

    public data class Content(
        /** Always present per spec. */
        public val content: String? = null,
    ) : UserInteraction

    public data class Form(
        /** Always present per spec. */
        public val content: String? = null,
    ) : UserInteraction

    public data object OtpRequired : UserInteraction

    public data class Redirect(
        /** Always present per spec. */
        public val url: String? = null,
    ) : UserInteraction

    /** A `type` this SDK version does not know yet; new values are non-breaking API changes. */
    public data class Unknown(public val type: String, public val json: JSONObject) : UserInteraction

    public companion object {
        public fun fromJson(json: JSONObject): UserInteraction = when (val code = json.optString("type")) {
            "CONTENT" -> Content(
                content = json.str("content"),
            )
            "FORM" -> Form(
                content = json.str("content"),
            )
            "OTP_REQUIRED" -> OtpRequired
            "REDIRECT" -> Redirect(
                url = json.str("url"),
            )
            else -> Unknown(code, json)
        }
    }
}

private fun JSONObject.str(key: String): String? = if (isNull(key)) null else getString(key)
private fun JSONObject.dec(key: String): BigDecimal? = if (isNull(key)) null else BigDecimal(get(key).toString())
private fun JSONObject.int(key: String): Int? = if (isNull(key)) null else getInt(key)
private fun JSONObject.bool(key: String): Boolean? = if (isNull(key)) null else getBoolean(key)
private fun <T> JSONObject.obj(key: String, read: (JSONObject) -> T): T? = optJSONObject(key)?.let(read)
private fun <T> JSONObject.list(key: String, read: (JSONArray, Int) -> T): List<T>? = optJSONArray(key)?.let { array -> List(array.length()) { read(array, it) } }
