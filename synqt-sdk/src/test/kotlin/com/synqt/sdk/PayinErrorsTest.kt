package com.synqt.sdk

import com.synqt.sdk.internal.parseErrors
import com.synqt.sdk.internal.sheet.payinErrors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PayinErrorsTest {

    private val refused = """[{"code":"INVALID_ACCOUNT_NUMBER","description":"Bad account","attrCode":"account.accountNumber","attrValue":"1234567890"}]"""

    @Test
    fun errorsKeepCodeAndFieldButNeverTheValue() {
        val error = SynqtException(406, parseErrors(refused))
        assertEquals(listOf(SynqtError("INVALID_ACCOUNT_NUMBER", "account.accountNumber", "Bad account")), error.errors)
        assertEquals("SYNQT HTTP 406: INVALID_ACCOUNT_NUMBER (account.accountNumber)", error.message)
        assertFalse(error.toString().contains("1234567890"))
    }

    @Test
    fun sheetMessagesNameTheFieldAndCode() {
        assertEquals(listOf("account.accountNumber" to "Check the account number and try again. (INVALID_ACCOUNT_NUMBER)"), payinErrors(SynqtException(406, parseErrors(refused))).map { it.en() })
        // A code with no payer wording asks the payer to check the field it names; SYNQT's description is for the merchant.
        val unknown = """[{"code":"EMAIL_INVALID","description":"Bad email","attrCode":"emailAddress"}]"""
        val shown = payinErrors(SynqtException(406, parseErrors(unknown))).map { it.en() }
        assertEquals(listOf("emailAddress" to "Check this field (EMAIL_INVALID)"), shown)
        assertFalse(shown.single().second.contains("Bad email"))
    }

    @Test
    fun systemErrorIsGeneral() {
        val error = payinErrors(SynqtException(406, parseErrors("""[{"code":"SYSTEM","description":null,"attrCode":null,"attrValue":null}]"""))).single()
        assertEquals(null, error.attr)
        assertEquals("SYNQT could not process this payment (SYSTEM). Try again later or choose another method.", error.en().second)
    }

    @Test
    fun otherStatusesAndBadBodiesFallBack() {
        assertEquals(emptyList<SynqtError>(), parseErrors("<html>"))
        assertEquals("The payment could not be created (HTTP 406).", payinErrors(SynqtException(406, emptyList())).single().en().second)
        assertEquals("SYNQT HTTP 401", SynqtException(401, emptyList()).message)
    }

    @Test
    fun codesWithPayerWordingKeepTheCode() {
        val error = payinErrors(SynqtException(406, parseErrors("""[{"code":"PAYMENT_PROCESS_ERROR","description":"For the merchant"}]"""))).single()
        assertEquals("Something went wrong while processing your payment. Please try again, or choose another payment method. " +
            "If it keeps happening, contact the merchant. (PAYMENT_PROCESS_ERROR)", error.en().second)
        val bare = payinErrors(SynqtException(406, parseErrors("""[{"code":"NEW_CODE","description":"For the merchant"}]"""))).single()
        assertEquals("Your payment could not be completed. Please try again, or choose another payment method. (NEW_CODE)", bare.en().second)
    }
}
