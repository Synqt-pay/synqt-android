package com.synqt.sdk

import com.synqt.sdk.internal.form.FieldException
import com.synqt.sdk.internal.form.FieldKind
import com.synqt.sdk.internal.form.FormValues
import com.synqt.sdk.internal.form.METHOD_FORMS
import com.synqt.sdk.internal.form.defaultChoices
import com.synqt.sdk.internal.form.filledIn
import com.synqt.sdk.internal.form.prefillProblems
import com.synqt.sdk.internal.form.problem
import com.synqt.sdk.internal.form.problems
import com.synqt.sdk.internal.form.providers
import com.synqt.sdk.internal.form.readyToPay
import com.synqt.sdk.internal.form.shows
import com.synqt.sdk.internal.sheet.PayinError
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MethodFormsTest {

    private fun form(code: String) = METHOD_FORMS.single { it.code == code }

    @Test
    fun everyMethodExceptCardHasANamedForm() {
        assertEquals(32, METHOD_FORMS.map { it.code }.distinct().size)
        assertTrue(METHOD_FORMS.none { it.code == "CARD" })
        assertEquals("Capitec Pay", en(form("CAPITEC_PAY").name))
    }

    @Test
    fun payersNeverSeeJsonPaths() {
        val paths = METHOD_FORMS.flatMap { it.fields }.map { en(it.label) }.filter { '.' in it || it.matches(Regex("[a-z]+[A-Z].*")) }
        assertEquals(emptyList<String>(), paths)
    }

    @Test
    fun blankGroupsAreLeftOutAndFilledOnesBuilt() {
        val method = form("CAPITEC_PAY").build(FormValues(mapOf(
            "phoneNumber" to " 0821234567 ", "account.accountNumber" to "", "customerName.lastName" to "Doe",
        )))
        assertEquals(PaymentMethodPayin.CapitecPayMethod(phoneNumber = "0821234567", customerName = PaymentMethodPayin.PersonName(lastName = "Doe")), method)
        assertNull((method as PaymentMethodPayin.CapitecPayMethod).account)
    }

    @Test
    fun missingRequiredFieldNamesThePath() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            form("CAPITEC_PAY").build(FormValues(mapOf("customerName.firstName" to "Jane")))
        }
        assertEquals("customerName.lastName", (error as FieldException).path)
    }

    @Test
    fun problemsUseTheSpecLimitsInPlainWords() {
        val problems = form("CAPITEC_PAY").problems(mapOf("said" to "12345", "account.accountNumber" to "12345678ab", "emailAddress" to "nope", "customerName.lastName" to "Doe"), chosen = emptyList())
        assertEquals(
            listOf(
                "emailAddress" to "Enter a valid email address",
                "said" to "Enter 13 digits",
                "account.accountNumber" to "Enter 10 digits",
            ),
            problems.map { it.en() },
        )
    }

    @Test
    fun aSectionWithAMandatoryFieldIsMandatory() {
        val capitec = form("CAPITEC_PAY")
        assertEquals(listOf("customerName.lastName" to "Required"), capitec.problems(emptyMap(), chosen = emptyList()).map { it.en() })
        assertEquals("Your name", en(capitec.fields.single { it.path == "customerName.lastName" }.section!!))
        // A section with nothing mandatory in it stays optional.
        assertEquals("Bank account (optional)", en(form("EWALLET").fields.first { it.path.startsWith("account.") }.section!!))
        assertEquals("account.accountNumber" to "Required", form("FNB_DIRECT_EFT").problems(emptyMap()).first { it.attr == "account.accountNumber" }.en())
    }

    @Test
    fun oneFieldIsCheckedOnItsOwn() {
        val said = form("CAPITEC_PAY").fields.single { it.path == "said" }
        assertEquals("said" to "Enter 13 digits", problem(said, mapOf("said" to "12345"))?.en())
        assertNull(problem(said, mapOf("said" to "0000000000000")))
    }

    @Test
    fun absaPayAsksForOneDetailAndBankAccountIsTheDefault() {
        val absaPay = form("ABSA_PAY")
        val (bank, id) = absaPay.oneOfs.first().choices
        assertEquals("Bank account", en(bank.label))
        val lastName = "customerName.lastName" to "Required" // the name is asked for on Absa Pay, though the spec makes it optional
        assertEquals(listOf("account.accountNumber" to "Required", lastName), absaPay.problems(emptyMap()).map { it.en() })
        // Picking the ID number makes it the required one, and a stale account number is not checked or sent.
        assertEquals(listOf("said" to "Required", lastName), absaPay.problems(mapOf("account.accountNumber" to "x"), listOf(id)).map { it.en() })
        assertTrue(absaPay.shows("emailAddress", listOf(id)))
        assertTrue(!absaPay.shows("account.accountNumber", listOf(id)))
        val method = absaPay.build(FormValues(mapOf("said" to "0000000000000", "customerName.lastName" to "Doe"))) as PaymentMethodPayin.AbsaPayMethod
        assertEquals("0000000000000", method.said)
        assertNull(method.account)
    }

    @Test
    fun payShapTilesNeedTheirCompanionFields() {
        val payShap = form("PAYSHAP")
        val (bank, mobile) = payShap.oneOfs.first().choices
        val values = mapOf("paymentOperatorCode" to "BANK", "customerName.lastName" to "Doe")
        assertEquals(listOf("account.accountName", "account.accountNumber"), payShap.problems(values, listOf(bank)).map { it.attr })
        assertEquals(listOf("phoneNumber", "externalProviderCode"), payShap.problems(values, listOf(mobile)).map { it.attr })
        assertEquals(emptyList<PayinError>(), payShap.problems(values + mapOf("phoneNumber" to "000000000000", "externalProviderCode" to "X"), listOf(mobile)))
        assertEquals(listOf("Mobile number", "Provider"), payShap.fields.filter { it.path in mobile.paths }.map { en(it.label) })
    }

    @Test
    fun fnbPayAsksForIdOrPassportWithIdFirst() {
        val fnbPay = form("FNB_DIRECT_EFT")
        val (id, passport) = fnbPay.oneOfs.first().choices
        assertEquals(listOf("South African ID number", "Passport number"), listOf(en(id.label), en(passport.label)))
        assertEquals("said" to "Required", fnbPay.problems(emptyMap()).single { it.attr == "said" }.en())
        assertTrue(fnbPay.problems(emptyMap(), listOf(passport)).none { it.attr == "said" })
        assertEquals("passportNumber" to "Required", fnbPay.problems(emptyMap(), listOf(passport)).single { it.attr == "passportNumber" }.en())
    }

    @Test
    fun payShapAlsoAsksWhichIdButLeavesItOptional() {
        val payShap = form("PAYSHAP")
        val (pay, ids) = payShap.oneOfs
        assertEquals("Which ID do you want to give?", en(ids.question))
        val (id, passport) = ids.choices
        assertEquals(listOf("South African ID number (optional)", "Passport number (optional)"), payShap.fields.filter { it.path == "said" || it.path == "passportNumber" }.sortedBy { it.path != "said" }.map { en(it.label) })
        val chosen = listOf(pay.choices.first(), id)
        assertTrue(payShap.shows("said", chosen))
        assertTrue(!payShap.shows("passportNumber", chosen))
        // Both IDs are optional in the spec, so choosing one does not make it mandatory.
        assertTrue(payShap.problems(emptyMap(), chosen).none { it.attr == "said" })
        assertEquals("passportNumber" to "Enter a letter followed by 7 or 8 numbers",
            payShap.problems(mapOf("passportNumber" to "123"), listOf(pay.choices.first(), passport)).single { it.attr == "passportNumber" }.en())
    }

    @Test
    fun onlyValidPrefilledFieldsCountAsFilledIn() {
        val zapper = form("ZAPPER_QR")
        val prefill = mapOf("emailAddress" to "test@example.com", "customerName.firstName" to "Jane", "customerName.lastName" to "Doe")
        assertEquals(listOf("emailAddress", "customerName.firstName", "customerName.lastName"), zapper.filledIn(prefill, prefill, emptyList()))
        // A malformed email stays in view so the payer can fix it; a value the payer has since changed is theirs.
        val badEmail = prefill + ("emailAddress" to "nope")
        assertEquals(listOf("customerName.firstName", "customerName.lastName"), zapper.filledIn(badEmail, badEmail, emptyList()))
        assertEquals(listOf("emailAddress", "customerName.lastName"), zapper.filledIn(prefill + ("customerName.firstName" to "Janet"), prefill, emptyList()))
    }

    @Test
    fun prefilledValuesInViewShowTheirProblemFromTheStart() {
        val zapper = form("ZAPPER_QR")
        val prefill = mapOf("emailAddress" to "nope", "customerName.firstName" to "Jane")
        val values = prefill + ("customerName.lastName" to "")
        val filled = zapper.filledIn(values, prefill, emptyList())
        // Only the bad prefill is flagged; the payer's own empty last name waits for Pay.
        assertEquals(listOf("emailAddress" to "Enter a valid email address"), zapper.prefillProblems(values, prefill, filled, emptyList()).map { it.en() })
        // A bank the dropdown doesn't list reads as blank, but is still flagged rather than silently dropped.
        val duitNow = form("DUITNOW")
        val bank = mapOf("paymentOperatorCode" to "NOT_LISTED")
        val shown = bank + ("paymentOperatorCode" to "")
        assertEquals(listOf("paymentOperatorCode" to "Check this field"), duitNow.prefillProblems(shown, bank, emptyList(), emptyList()).map { it.en() })
        // A required bank gets the same message, not "Required".
        val eWallet = form("EWALLET")
        assertEquals(listOf("paymentOperatorCode" to "Check this field"), eWallet.prefillProblems(shown, bank, emptyList(), emptyList()).map { it.en() })
    }

    @Test
    fun aPrefilledFirstAndLastNameHideTheOptionalNameFields() {
        val capitec = form("CAPITEC_PAY")
        val names = mapOf("customerName.firstName" to "Jane", "customerName.lastName" to "Doe")
        assertEquals(
            listOf("customerName.firstName", "customerName.lastName", "customerName.titleBefore", "customerName.middleName", "customerName.titleAfter"),
            capitec.filledIn(names, names, capitec.defaultChoices),
        )
        // With only the last name, the payer still sees the rest of the name section.
        val last = mapOf("customerName.lastName" to "Doe")
        assertEquals(listOf("customerName.lastName"), capitec.filledIn(last, last, capitec.defaultChoices))
    }

    @Test
    fun theSheetPaysStraightAwayOnlyWhenEveryMandatoryFieldIsFilledIn() {
        val zapper = form("ZAPPER_QR")
        val all = mapOf("emailAddress" to "test@example.com", "customerName.firstName" to "Jane", "customerName.lastName" to "Doe")
        assertTrue(zapper.readyToPay(all, zapper.filledIn(all, all, emptyList()), emptyList()))
        val noEmail = all - "emailAddress"
        assertTrue(!zapper.readyToPay(noEmail, zapper.filledIn(noEmail, noEmail, emptyList()), emptyList()))
        // Net banking has only optional fields, so the payer still sees its form.
        assertTrue(!form("NETBANKING").readyToPay(emptyMap(), emptyList(), emptyList()))
    }

    @Test
    fun southAfricanRulesMakeSpecOptionalFieldsMandatory() {
        assertEquals(
            listOf("paymentOperatorCode", "emailAddress", "account.accountName", "account.accountNumber", "account.accountType"),
            form("ONLINE").fields.filter { it.required }.map { it.path },
        )
        assertEquals(listOf("paymentOperatorCode"), form("OFFLINE").fields.filter { it.path == "paymentOperatorCode" && it.required }.map { it.path })
        assertEquals("paymentOperatorCode" to "Required", form("OFFLINE").problems(emptyMap()).first { it.attr == "paymentOperatorCode" }.en())
    }

    @Test
    fun capitecPayAsksForOneDetail() {
        assertEquals(listOf("Bank account", "Mobile number", "South African ID number"), form("CAPITEC_PAY").oneOfs.first().choices.map { en(it.label) })
    }

    @Test
    fun paymentOperatorsCarryTheirExternalProviders() {
        val op = PaymentOperatorIncoming.fromJson(JSONObject(
            """{"code":"ZA_BANK","name":"A Bank","externalProviders":{"providers":[{"code":"P1","name":"Provider one"},{"name":"no code"}]}}""",
        ))
        assertEquals(PaymentOperatorIncoming("ZA_BANK", "A Bank", ExternalProviders(listOf(ExternalProvider("P1", "Provider one"), ExternalProvider(name = "no code")))), op)
        // The sheet skips providers without a code.
        assertEquals(listOf(ExternalProvider("P1", "Provider one")), providers(listOf(op)))
    }

    @Test
    fun providersListEveryBanksProvidersOnce() {
        // Shaped like Stable's PayShap options: bank codes with providers that repeat across banks.
        val p1 = ExternalProvider("STANDARDBANK", "StandardBank")
        val p2 = ExternalProvider("SHYFT", "Shyft")
        val p3 = ExternalProvider("CAPITEC", "CAPITEC")
        val banks = listOf(
            PaymentOperatorIncoming("ZA_STANDARD", "Standard Bank", ExternalProviders(listOf(p1, p2))),
            PaymentOperatorIncoming("ZA_CAPITEC", "Capitec", ExternalProviders(listOf(p3, p1))),
            PaymentOperatorIncoming("NG_BANK", "Another market", ExternalProviders(listOf(ExternalProvider("NG_P", "x")))),
        )
        assertEquals(listOf(p1, p2, p3, ExternalProvider("NG_P", "x")), providers(banks))
        assertEquals(FieldKind.PROVIDER, form("PAYSHAP").fields.single { it.path == "externalProviderCode" }.kind)
    }

    @Test
    fun saMobileNumbersAreUpTo12CharactersStartingWith0() {
        listOf("PAYSHAP", "CAPITEC_PAY").forEach { code ->
            val phone = form(code).fields.single { it.path == "phoneNumber" }
            assertNull(problem(phone, mapOf("phoneNumber" to "0000000000"))) // 10 digits, the usual SA length
            assertNull(problem(phone, mapOf("phoneNumber" to "000000000000")))
            assertEquals("phoneNumber" to "Enter up to 12 characters, starting with 0", problem(phone, mapOf("phoneNumber" to "1000000000"))?.en())
            assertEquals("phoneNumber" to "Enter up to 12 characters, starting with 0", problem(phone, mapOf("phoneNumber" to "0000000000000"))?.en())
        }
    }

    @Test
    fun paymentOptionsCarryOperatorsAndPayerFacingNames() {
        val options = AvailablePayinOptionList.fromJson(JSONObject(
            """{"data":[{"paymentMethodCode":"FPX","paymentOperators":[{"code":"MBB","name":"Maybank"},{"code":"X"}]},
               {"paymentMethodCode":"BRAND_NEW","segmentCode":"S1"}]}""",
        )).data!!
        assertEquals(AvailablePayinOption("FPX", listOf(PaymentOperatorIncoming("MBB", "Maybank"), PaymentOperatorIncoming("X")), null), options[0])
        assertEquals(AvailablePayinOption("BRAND_NEW", null, "S1"), options[1])
        assertEquals("FPX online banking", en(methodName("FPX")!!))
        assertNull(methodName("BRAND_NEW")) // the sheet cannot show it, so it has no payer-facing name
    }
}
