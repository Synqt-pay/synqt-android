package com.synqt.sdk

import com.synqt.sdk.internal.parseCreatePayin
import com.synqt.sdk.internal.parsePayinStatus
import java.math.BigDecimal
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SynqtTest {

    private fun payin(id: String = "order-1") =
        Synqt(Synqt.Environment.SANDBOX).payin("a.b+c", id, Money(BigDecimal("12.30"), "ZAR"), "https://x.io/return", "https://x.io/cb")

    @Test
    fun paymentAppUrlEncodesParams() {
        assertEquals(
            "https://payment.sandbox.synqt.io/?merchantCode=MID&idPayin=order-1&token=a.b%2Bc&amount=12.30" +
                "&currencyCode=ZAR&callbackUrl=https%3A%2F%2Fx.io%2Fcb&returnUrl=https%3A%2F%2Fx.io%2Freturn&emailAddress=a%40b.co",
            payin().paymentAppUrl("MID", mapOf("emailAddress" to "a@b.co")),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsafeId() {
        payin("../x")
    }

    @Test
    fun rejectsReturnUrlThatIsNotHttps() {
        val money = Money(BigDecimal("12.30"), "ZAR")
        listOf("http://x.io/return", "myapp://return", "x.io/return").forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { Synqt(Synqt.Environment.SANDBOX).payin("t", "order-1", money, url, "https://x.io/cb") }
        }
    }

    @Test
    fun parsesRedirect() {
        val result = parseCreatePayin("""{"redirectTo":{"method":"POST","url":"https://p.io","data":"{\"k\":\"v\",\"n\":1}"}}""")
        assertEquals(CreatePayinResult.Redirect("POST", "https://p.io", mapOf("k" to "v", "n" to "1")), result)
    }

    @Test
    fun parsesInstructions() {
        val result = parseCreatePayin("""{"paymentMethodResponse":{"paymentMethodCode":"PAYSHAP","reference":"REF1"}}""")
        assertEquals("REF1", ((result as CreatePayinResult.Instructions).paymentMethod as PaymentMethodResponse.PayShap).reference)
    }

    @Test
    fun parsesStatus() {
        val status = parsePayinStatus(JSONObject(
            """{"paymentRequested":{"money":{"amount":100.50,"currencyCode":"ZAR"}},"fee":{"amount":2,"currencyCode":"ZAR"},
               "process":{"status":"FAILED","isTest":true,"createdAt":"2026-10-26T10:00:00Z",
                 "failureReasons":{"failureReasonCode":"ENTITY_EXPIRED","message":"Expired","failedAt":"2026-10-26T12:00:00Z"}},
               "paymentMethodResponse":{"paymentMethodCode":"CAPITEC_PAY"}}""",
        ))
        assertEquals(PayinStatus.Status.FAILED, status.status)
        assertTrue(status.status.isFinal && status.isTest)
        assertEquals(0, BigDecimal("100.5").compareTo(status.requested.amount)) // scale differs between JSON parsers
        assertEquals("ENTITY_EXPIRED", status.failureReason?.code)
        assertTrue(status.paymentMethod is PaymentMethodResponse.CapitecPay)
    }

    @Test
    fun unknownStatusIsNotFinal() {
        val status = parsePayinStatus(JSONObject("""{"process":{"status":"ON_HOLD"}}"""))
        assertEquals(PayinStatus.Status.UNKNOWN, status.status)
        assertFalse(status.status.isFinal)
    }
}
