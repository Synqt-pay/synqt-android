# Changelog

## 0.3.0 (Unreleased)

First public release, under the Apache License 2.0.

- A Synqt-branded payment sheet that asks the payer for exactly the details their payment method needs, checks them, sends the payin and shows the next step (open the payment app, QR code, countdown, or how to pay offline).
- The hosted Payment Web App inside the app, or in the browser.
- `IncomingPaymentsApi`, with the Carpentum Java SDK's payin calls and models: `payinAvailablePaymentOptions`, `createPayin(idPayin, Payin)`, `getPayin`, `getPayinAccounts` and `setPayinExternalReference`, returning SYNQT's full `PayinAcceptedResponse` and `PayinDetail`. `PayinCheckout` (from `Synqt.checkout`) runs the payment sheet and the Payment Web App.
- An add-on API for SYNQT's card add-on, which PCI DSS compliant merchants can get on request.
- New coordinates `com.synqt:synqt-android` on Maven Central, and package `com.synqt.sdk`.
