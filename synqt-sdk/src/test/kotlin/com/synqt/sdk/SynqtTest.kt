package com.synqt.sdk

import com.sun.net.httpserver.HttpServer
import java.math.BigDecimal
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SynqtTest {

    private val money = MoneyPaymentRequested(BigDecimal("12.30"), "ZAR")

    private fun checkout(id: String = "order-1") =
        Synqt(Synqt.Environment.SANDBOX).checkout("a.b+c", id, money, "https://x.io/return", "https://x.io/cb")

    @Test
    fun paymentAppUrlEncodesParams() {
        assertEquals(
            "https://payment.sandbox.synqt.io/?merchantCode=MID&idPayin=order-1&token=a.b%2Bc&amount=12.30" +
                "&currencyCode=ZAR&callbackUrl=https%3A%2F%2Fx.io%2Fcb&returnUrl=https%3A%2F%2Fx.io%2Freturn&emailAddress=a%40b.co",
            checkout().paymentAppUrl("MID", mapOf("emailAddress" to "a@b.co")),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsafeId() {
        checkout("../x")
    }

    @Test
    fun rejectsReturnUrlThatIsNotHttps() {
        listOf("http://x.io/return", "myapp://return", "x.io/return").forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { Synqt(Synqt.Environment.SANDBOX).checkout("t", "order-1", money, url) }
        }
    }

    @Test
    fun payinRequestHasTheJavaSdksShape() {
        val payin = Payin(
            PaymentRequested(money, segmentCode = "S1"),
            PaymentMethodPayin.CapitecPayMethod(phoneNumber = "0821234567"),
            returnUrl = "https://x.io/return",
            customerIp = "10.0.0.1",
        )
        val json = payin.toJson()
        assertEquals(setOf("paymentRequested", "paymentMethod", "returnUrl", "customerIp"), json.keys().asSequence().toSet()) // callbackUrl is optional
        assertEquals("S1", json.getJSONObject("paymentRequested").getString("segmentCode"))
        assertEquals("ZAR", json.getJSONObject("paymentRequested").getJSONObject("money").getString("currencyCode"))
        assertEquals("CAPITEC_PAY", json.getJSONObject("paymentMethod").getString("paymentMethodCode"))
    }

    @Test
    fun acceptedResponseIsChosenByTheFieldSynqtSent() {
        val redirect = PayinAcceptedResponse.fromJson(JSONObject(
            """{"paymentRequested":{"money":{"amount":12.3,"currencyCode":"ZAR"}},
               "redirectTo":{"method":"POST","url":"https://p.io","data":"{\"k\":\"v\",\"n\":1}"}}""",
        ))
        redirect as PayinAcceptedRedirectResponse
        assertEquals("ZAR", redirect.paymentRequested?.money?.currencyCode)
        assertEquals(mapOf("k" to "v", "n" to "1"), redirect.redirect()?.formData())
        assertNull(redirect.nextStep())

        val method = PayinAcceptedResponse.fromJson(JSONObject("""{"paymentMethodResponse":{"paymentMethodCode":"PAYSHAP","reference":"REF1"}}"""))
        method as PayinAcceptedMethodResponse
        assertEquals("REF1", (method.paymentMethodResponse as PaymentMethodResponsePayin.PayShapMethodResponse).reference)
        assertEquals("REF1", method.nextStep()?.reference)
        assertNull(method.redirect())
    }

    @Test
    fun parsesPayinDetail() {
        val detail = PayinDetail.fromJson(JSONObject(
            """{"paymentRequested":{"money":{"amount":100.50,"currencyCode":"ZAR"}},"fee":{"amount":2,"currencyCode":"ZAR"},
               "process":{"status":"FAILED","isTest":true,"createdAt":"2026-10-26T10:00:00Z",
                 "failureReasons":{"failureReasonCode":"ENTITY_EXPIRED","message":"Expired","failedAt":"2026-10-26T12:00:00Z"}},
               "paymentMethodResponse":{"paymentMethodCode":"CAPITEC_PAY"},
               "settlement":{"paymentMethodCode":"CRYPTO_TRANSFER","paymentOperatorCode":"OP","account":{"accountNumber":"A1"}},
               "customer":{"idCustomer":"C1"}}""",
        ))
        assertEquals(PaymentStatus.FAILED, detail.process?.status)
        assertTrue(detail.isFinal && detail.process?.isTest == true)
        assertEquals(0, BigDecimal("100.5").compareTo(detail.paymentRequested?.money?.amount)) // scale differs between JSON parsers
        assertEquals("ENTITY_EXPIRED", detail.process?.failureReasons?.failureReasonCode)
        assertTrue(detail.paymentMethodResponse is PaymentMethodResponsePayin.CapitecPayMethodResponse)
        assertEquals("OP", (detail.settlement as SettlementMethod.SettlementCryptoTransferMethod).paymentOperatorCode)
        assertEquals("C1", detail.customer?.idCustomer)
    }

    @Test
    fun unknownStatusIsNotFinal() {
        assertFalse(PayinDetail.fromJson(JSONObject("""{"process":{"status":"ON_HOLD"}}""")).isFinal)
    }

    // Over HTTP, against a local server standing in for SYNQT.
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val received = mutableListOf<String>()

    @After
    fun stop() = server.stop(0)

    private fun api(vararg answers: Pair<Int, String>): IncomingPaymentsApi {
        var next = 0
        server.createContext("/") { exchange ->
            received += "${exchange.requestMethod} ${exchange.requestURI} ${exchange.requestBody.bufferedReader().readText()}"
            val (status, body) = answers[minOf(next++, answers.size - 1)]
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val synqt = Synqt(Synqt.Environment.SANDBOX).apply { apiBaseUrl = "http://127.0.0.1:${server.address.port}" }
        return IncomingPaymentsApi.create(synqt, "t")
    }

    private val capitec = Payin(PaymentRequested(money), PaymentMethodPayin.CapitecPayMethod(phoneNumber = "0821234567"), "https://x.io/return")

    @Test
    fun createPayinPostsThePayinAndReturnsWhatSynqtAccepted() = runBlocking {
        val accepted = api(201 to """{"paymentRequested":{"money":{"amount":12.3,"currencyCode":"ZAR"}},"paymentMethodResponse":{"paymentMethodCode":"CAPITEC_PAY"}}""")
            .createPayin("order-1", capitec)
        assertTrue(accepted is PayinAcceptedMethodResponse)
        val (method, path, body) = received.single().split(" ", limit = 3)
        assertEquals("POST /payins/order-1", "$method $path")
        assertEquals(capitec.toJson().toString(), JSONObject(body).toString())
    }

    @Test
    fun aRetryOfTheSamePayinReturnsTheExistingOne() = runBlocking {
        val existing = """{"paymentRequested":{"money":{"amount":12.3,"currencyCode":"ZAR"}},"paymentMethodResponse":{"paymentMethodCode":"CAPITEC_PAY","reference":"R1"}}"""
        val accepted = api(409 to "[]", 200 to existing).createPayin("order-1", capitec)
        assertEquals("R1", accepted.nextStep()?.reference)
        assertEquals("GET /payins/order-1", received[1].substringBeforeLast(" ").trim())
    }

    @Test
    fun aRetryWithAnotherMethodIsRefused() = runBlocking {
        val existing = """{"paymentMethodResponse":{"paymentMethodCode":"FNB_DIRECT_EFT"}}"""
        val error = runCatching { api(409 to "[]", 200 to existing).createPayin("order-1", capitec) }.exceptionOrNull() as SynqtException
        assertEquals(409, error.httpCode)
        assertEquals("PAYIN_ALREADY_STARTED", error.errors.single().code)
    }

    @Test
    fun aRefusalKeepsItsCodesButNotWhatThePayerTyped() = runBlocking {
        val error = runCatching {
            api(406 to """[{"code":"INVALID_PHONE_NUMBER","attrCode":"phoneNumber","attrValue":"0821234567","description":"bad"}]""").createPayin("order-1", capitec)
        }.exceptionOrNull() as SynqtException
        assertEquals(listOf(SynqtError("INVALID_PHONE_NUMBER", "phoneNumber", "bad")), error.errors)
        assertFalse(error.message.orEmpty().contains("0821234567"))
    }
}
