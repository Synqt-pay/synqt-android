package com.synqt.sdk.addon

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import com.synqt.sdk.PayinCheckout
import org.json.JSONObject

/** For SYNQT's own add-ons, such as the card add-on. Apps never need it, and it may change in any release. */
@RequiresOptIn(message = "For SYNQT add-ons such as the card add-on, not for apps.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class SynqtAddonApi

/**
 * A payment method an add-on adds to the payment sheet, registered with [com.synqt.sdk.Synqt.addMethod]. The sheet
 * keeps its header, summary, Pay button, processing state, general errors and next steps; the add-on draws, checks and
 * builds only its own fields, so what the payer types there never reaches the base SDK.
 */
@SynqtAddonApi
public interface SheetMethod {
    /** The `paymentMethodCode`, such as `CARD`. */
    public val code: String

    /** The payer-facing name, as a string resource. */
    @get:StringRes
    public val name: Int

    /** True keeps the sheet out of screenshots, screen recording and the recents thumbnail while it shows this method. */
    public val secure: Boolean

    /**
     * The method's fields, for one sheet. [context] is the sheet's themed context; [prefill] is the app's, as given to
     * [PayinCheckout.presentPaymentSheet]. [restore] is what [MethodSection.state] returned when the sheet was rebuilt for a
     * light or dark, font size or language change, else null.
     */
    public fun section(context: Context, checkout: PayinCheckout, prefill: Map<String, String>, restore: Any?): MethodSection
}

/** One sheet's fields for a [SheetMethod]. Main thread only. */
@SynqtAddonApi
public interface MethodSection {
    /** Added to the sheet under "Your details". */
    public val view: View

    /** Checks every field when the payer taps Pay and shows each problem under its field. True if all pass. */
    public fun check(): Boolean

    /**
     * The `paymentMethod` object to send, built after [check] passed. Throw [MethodSectionException] with payer-facing
     * text if it can't be built; the sheet shows it and sends nothing.
     */
    public suspend fun paymentMethod(): JSONObject

    /** Shows SYNQT's refusal under the field SYNQT names in [attr], and returns true, or false if no field here matches. */
    public fun showError(attr: String, message: String): Boolean

    /** What the payer entered, kept in memory only, for a rebuilt sheet. */
    public fun state(): Any?

    /** Wipes what the payer typed. Called when the sheet closes or is replaced by a rebuilt one. */
    public fun clear()
}

/** A [MethodSection] could not build its request. [payerMessage] is shown in the sheet as it is. */
@SynqtAddonApi
public class MethodSectionException(public val payerMessage: String) : Exception(payerMessage)
