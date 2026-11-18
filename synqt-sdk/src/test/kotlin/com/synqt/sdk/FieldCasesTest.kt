package com.synqt.sdk

import com.synqt.sdk.internal.form.METHOD_FORMS
import com.synqt.sdk.internal.form.problem
import org.junit.Assert.assertEquals
import org.junit.Test

/** The same inputs, checked the same way, in every SYNQT SDK. Generated: add cases to the shared table, not here. */
class FieldCasesTest {
    @Test
    fun everySdkChecksTheseFieldsTheSameWay() {
        val cases = listOf(
            listOf("CAPITEC_PAY", "said", "0000000000000", null),
            listOf("CAPITEC_PAY", "said", "12345", "Enter 13 digits"),
            listOf("CAPITEC_PAY", "account.accountNumber", "1234567890", null),
            listOf("CAPITEC_PAY", "account.accountNumber", "12345678ab", "Enter 10 digits"),
            listOf("CAPITEC_PAY", "emailAddress", "test@example.com", null),
            listOf("CAPITEC_PAY", "emailAddress", "nope", "Enter a valid email address"),
            listOf("CAPITEC_PAY", "phoneNumber", "0821234567", null),
            listOf("CAPITEC_PAY", "phoneNumber", "821234567", "Enter up to 12 characters, starting with 0"),
            listOf("CAPITEC_PAY", "customerName.lastName", "", "Required"),
            listOf("CAPITEC_PAY", "customerName.lastName", "Doe", null),
            listOf("UPIID", "upiId", "name@bank", null),
            listOf("UPIID", "upiId", "namebank", "Enter a UPI ID like name@bank"),
            listOf("FNB_DIRECT_EFT", "passportNumber", "A1234567", null),
            listOf("FNB_DIRECT_EFT", "passportNumber", "1234", "Enter a letter followed by 7 or 8 numbers"),
            listOf("PIXQR", "taxId", "12345678901234", null),
            listOf("PIXQR", "taxId", "123", "Enter your 14-character CPF or CNPJ"),
            listOf("OTT_VOUCHER", "voucherPin", "123456789012", null),
        )
        for ((code, path, value, want) in cases) {
            val field = METHOD_FORMS.single { it.code == code }.fields.single { it.path == path }
            assertEquals("$code $path \"$value\"", want, problem(field, mapOf(path!! to value!!))?.en()?.second)
        }
    }
}
