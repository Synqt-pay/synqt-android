package com.synqt.sdk

import android.content.Context
import com.synqt.sdk.addon.MethodSection
import com.synqt.sdk.addon.SheetMethod
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AddonTest {
    private class Method(override val code: String) : SheetMethod {
        override val name = R.string.synqt_failure_general
        override val secure = false
        override fun section(context: Context, checkout: PayinCheckout, prefill: Map<String, String>, restore: Any?): MethodSection = error("not drawn in unit tests")
    }

    @Test
    fun addedMethodIsOfferedAndNamedOnce() {
        Synqt.addMethod(Method("TEST_ADDON"))
        Synqt.addMethod(Method("TEST_ADDON"))
        assertEquals(1, Synqt.directPaymentMethods.count { it.code == "TEST_ADDON" })
        assertEquals(R.string.synqt_failure_general, methodName("TEST_ADDON"))
    }

    @Test
    fun builtInMethodsCantBeReplaced() {
        assertThrows(IllegalArgumentException::class.java) { Synqt.addMethod(Method("CAPITEC_PAY")) }
        assertTrue(Synqt.directPaymentMethods.none { it.code == "CARD" })
    }

    @Test
    fun threeDSecureRedirectBecomesARedirect() {
        val json = JSONObject().put("paymentMethodResponse", JSONObject().put("paymentMethodCode", "CARD")
            .put("userInteraction", JSONObject().put("type", "REDIRECT").put("url", "https://acs.example/3ds"))).toString()
        assertEquals(Redirection(method = "GET", url = "https://acs.example/3ds"), PayinAcceptedResponse.fromJson(JSONObject(json)).redirect())
    }
}
