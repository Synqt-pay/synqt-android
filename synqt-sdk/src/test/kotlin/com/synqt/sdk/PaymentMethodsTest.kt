package com.synqt.sdk

import com.synqt.sdk.internal.form.METHOD_FORMS
import com.synqt.sdk.internal.parseCreatePayin
import com.synqt.sdk.internal.requireSameMethod
import com.synqt.sdk.internal.sheet.APP_PACKAGES
import com.synqt.sdk.internal.sheet.appLinks
import com.synqt.sdk.internal.sheet.payinErrors
import com.synqt.sdk.internal.sheet.timeLeft
import com.synqt.sdk.internal.webapp.isReturn
import java.math.BigDecimal
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentMethodsTest {

    @Test
    fun requestWritesCodeAndOnlySetFields() {
        val json = PaymentMethod.CapitecPay(
            phoneNumber = "0821234567",
            customerName = PaymentMethod.PersonName(lastName = "Doe", firstName = "Jane"),
        ).toJson()
        assertEquals("CAPITEC_PAY", json.getString("paymentMethodCode"))
        assertEquals("0821234567", json.getString("phoneNumber"))
        assertEquals("Doe", json.getJSONObject("customerName").getString("lastName"))
        assertFalse(json.has("said"))
        assertFalse(json.getJSONObject("customerName").has("middleName"))
    }

    @Test
    fun sharedAccountIsCheckedAgainstEachMethodsOwnFields() {
        val missing = runCatching { PaymentMethod.Pse(account = PaymentMethod.Account(), emailAddress = "a@b.co", phoneNumber = "+27821234567") }
        assertEquals("PaymentMethod.Pse needs account.accountName", missing.exceptionOrNull()?.message)
        val extra = runCatching {
            PaymentMethod.Pse(account = PaymentMethod.Account(accountName = "A Payer", accountNumber = "1"), emailAddress = "a@b.co", phoneNumber = "+27821234567")
        }
        assertEquals("PaymentMethod.Pse does not take account.accountNumber", extra.exceptionOrNull()?.message)
        // The same class serves a method that needs only the number.
        val json = PaymentMethod.CapitecPay(phoneNumber = "0821234567", account = PaymentMethod.Account(accountNumber = "123")).toJson()
        assertEquals("123", json.getJSONObject("account").getString("accountNumber"))
        assertFalse(json.getJSONObject("account").has("accountName"))
    }

    @Test
    fun responseAccountsShareOneClass() {
        val absa = PaymentMethodResponse.fromJson(JSONObject("""{"paymentMethodCode":"ABSA_PAY","accountCustomer":{"accountName":"A","accountNumber":"1"}}"""))
        val pse = PaymentMethodResponse.fromJson(JSONObject("""{"paymentMethodCode":"PSE","accountCustomer":{"accountName":"B"}}"""))
        assertEquals(PaymentMethodResponse.Account(accountName = "A", accountNumber = "1"), (absa as PaymentMethodResponse.AbsaPay).accountCustomer)
        assertEquals(PaymentMethodResponse.Account(accountName = "B"), (pse as PaymentMethodResponse.Pse).accountCustomer)
    }

    @Test
    fun cardRequestHasNoRawCardField() {
        val json = PaymentMethod.Card(card = PaymentMethod.Card.CardData(cardDataVault = PaymentMethod.Card.CardDataVault(vaultToken = "vt_1"))).toJson()
        assertEquals("vt_1", json.getJSONObject("card").getJSONObject("cardDataVault").getString("vaultToken"))
        assertFalse(json.getJSONObject("card").has("cardDataRaw"))
    }

    @Test
    fun parsesKnownResponse() {
        val response = PaymentMethodResponse.fromJson(JSONObject(
            """{"paymentMethodCode":"CAPITEC_PAY","idPayin":"o-1","reference":"REF1",
               "money":{"amount":100.50,"currencyCode":"ZAR"}}""",
        ))
        response as PaymentMethodResponse.CapitecPay
        assertEquals("REF1", response.reference)
        assertEquals(0, BigDecimal("100.5").compareTo(response.money!!.amount)) // scale differs between JSON parsers
        assertNull(response.phoneNumber)
    }

    @Test
    fun aRetryMustBeTheSameMethodAsTheExistingPayin() {
        val existing = """{"paymentMethodResponse":{"paymentMethodCode":"CAPITEC_PAY"}}"""
        requireSameMethod(existing, "CAPITEC_PAY") // a real retry passes
        requireSameMethod("""{"redirectTo":{"method":"GET","url":"https://example.com"}}""", "FNB_DIRECT_EFT") // nothing to compare
        val error = runCatching { requireSameMethod(existing, "FNB_DIRECT_EFT") }.exceptionOrNull() as SynqtException
        assertEquals(409, error.httpCode)
        assertEquals("This payin was already started with CAPITEC_PAY. Start a new payin to pay another way.", error.errors.single().description)
        assertEquals("This payment was already started with another payment method. Start a new payment to pay another way.", payinErrors(error).single().en().second)
    }

    @Test
    fun zapperInstructionsCarryTheNextStep() {
        val result = parseCreatePayin(
            """{"paymentMethodResponse":{"paymentMethodCode":"ZAPPER_QR","qrCode":"https://example.com/qr.png",
               "qrRawData":"https://example.com/pay?qr=1","zapperQrDeepLink":"https://example.com/pay?qr=1","reference":"",
               "expireAt":"2026-10-27T15:15:49.725253752Z"}}""",
        ) as CreatePayinResult.Instructions
        assertTrue(result.paymentMethod is PaymentMethodResponse.ZapperQr)
        assertEquals(
            NextStep(appLink = "https://example.com/pay?qr=1", qrCodeImageUrl = "https://example.com/qr.png", qrCodeData = "https://example.com/pay?qr=1",
                expiresAt = "2026-10-27T15:15:49.725253752Z"),
            result.nextStep,
        ) // the empty reference is left out
    }

    @Test
    fun offlineInstructionsCarryTheAccountToPayInto() {
        val result = parseCreatePayin(
            """{"paymentMethodResponse":{"paymentMethodCode":"OFFLINE","reference":"REF123",
               "account":{"accountName":"Test Payee","accountNumber":"0000000000","bankName":"Test Bank","bankCode":"000000"},
               "accountCustomer":{"accountName":"Test Payer"}}}""",
        ) as CreatePayinResult.Instructions
        assertEquals(PaymentMethodResponse.Account(accountName = "Test Payee", accountNumber = "0000000000", bankName = "Test Bank", bankCode = "000000"), result.nextStep.payInto)
        assertEquals("REF123", result.nextStep.reference)
        // Other methods' `account` is not shown as somewhere to pay.
        val other = parseCreatePayin("""{"paymentMethodResponse":{"paymentMethodCode":"PAYSHAP","account":{"accountNumber":"0000000000"}}}""") as CreatePayinResult.Instructions
        assertEquals(null, other.nextStep.payInto)
    }

    @Test
    fun everyPaymentAppIsVisibleToTheSheet() {
        // Without a <queries> entry Android 11+ hides the app, and the sheet would always fall back to the browser.
        val manifest = java.io.File("src/main/AndroidManifest.xml").readText()
        APP_PACKAGES.values.forEach { assertTrue("$it is not in <queries>", manifest.contains("<package android:name=\"$it\" />")) }
        assertTrue(APP_PACKAGES.keys.all { code -> METHOD_FORMS.any { it.code == code } })
    }

    @Test
    fun appLinksFallBackToTheAppsOwnScheme() {
        assertEquals(listOf("https://pos.snapscan.io/qr/abc?id=1", "snapscan://pos.snapscan.io/qr/abc?id=1"), appLinks("https://pos.snapscan.io/qr/abc?id=1", "snapscan"))
        assertEquals(listOf("https://www.zapper.com/payWithZapper?qr=1"), appLinks("https://www.zapper.com/payWithZapper?qr=1", null))
    }

    @Test
    fun timeLeftCountsDownToTheExpiry() {
        val now = java.time.Instant.parse("2026-10-27T13:16:32Z")
        assertEquals("01:59:17", timeLeft("2026-10-27T15:15:49.725253752Z", now))
        assertEquals("", timeLeft("2026-10-27T13:00:00Z", now))
        assertNull(timeLeft("not a time", now))
    }

    @Test
    fun failureReasonsHaveAPayerMessage() {
        // FailureReason.payerMessage(context) resolves these; codes without wording fall back to synqt_failure_general.
        assertTrue(en(payerMessageFor("PAYMENT_PROCESS_ERROR")!!).startsWith("Something went wrong while processing your payment."))
        assertNull(payerMessageFor("NEW_CODE"))
        assertEquals("Your payment could not be completed. Please try again, or choose another payment method.", en(R.string.synqt_failure_general))
    }

    @Test
    fun unknownMethodDoesNotThrow() {
        val response = PaymentMethodResponse.fromJson(JSONObject("""{"paymentMethodCode":"NEW_METHOD","x":1}"""))
        assertTrue(response is PaymentMethodResponse.Unknown)
        assertEquals("NEW_METHOD", (response as PaymentMethodResponse.Unknown).paymentMethodCode)
    }

    @Test
    fun instructionsExposeTypedMethod() {
        val result = parseCreatePayin("""{"paymentMethodResponse":{"paymentMethodCode":"PAYSHAP","reference":"R9"}}""")
        val method = (result as CreatePayinResult.Instructions).paymentMethod
        assertEquals("R9", (method as PaymentMethodResponse.PayShap).reference)
    }

    @Test
    fun returnUrlMatchesItsPathOnly() {
        val back = "https://x.io/return"
        assertTrue(isReturn("https://x.io/return?idPayin=1", back))
        assertFalse(isReturn("https://x.io/returned", back))
        assertFalse(isReturn("https://evil.io/return", back))
    }
}
