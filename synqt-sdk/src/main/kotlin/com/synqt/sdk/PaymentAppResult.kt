package com.synqt.sdk

import android.net.Uri

/** How the Payment Web App closed. Either way, ask [Payin.status] where the payment stands. */
public sealed interface PaymentAppResult {
    /** The Payment Web App sent the payer to the returnUrl, with whatever it added to it. */
    public data class Returned(public val url: Uri) : PaymentAppResult

    /** The payer closed it first. */
    public data object Closed : PaymentAppResult
}
