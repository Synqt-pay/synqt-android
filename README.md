# SYNQT Android SDK

Take payments (SYNQT payins) in an Android app. The SDK can show a Synqt-branded payment sheet that asks the payer for exactly the details their payment method needs, send the payment, and tell you what to do next. You don't build or maintain any payment forms.

It covers SYNQT API version 2 (spec 2.84): the direct API for all payment methods except CARD, and the hosted Payment Web App. CARD comes from a separate card add-on, available to PCI DSS compliant merchants on request from SYNQT.

## Requirements

- Android 7.0 (API 24) or later for the Payment Web App and the API calls; Android 8.0 (API 26) or later for the payment sheet. Below 8.0, `presentPaymentSheet` throws `IllegalStateException` before showing anything, so check `Build.VERSION.SDK_INT` and use `presentPaymentApp` there.
- Kotlin with coroutines. Every network call is a `suspend` function.
- A backend of your own that mints a SYNQT token for each order. The app never holds the merchant's private key or secret key.
- A SYNQT merchant account: the Merchant ID, and the RSA public key you uploaded to SYNQT (Back-office, Home > Info).

## Integrating the SDK, step by step

Follow these in order. Steps 1 and 2 happen once; steps 3 to 7 happen for every order.

### 1. Add the SDK to your project

The SDK is on Maven Central as `com.synqt:synqt-android`. No token or extra repository is needed:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.synqt:synqt-android:0.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7") // for viewModelScope, step 5
}
```

The SDK needs Java 17 (`compileOptions` in your app module) and declares the `INTERNET` permission, its two screens and the queries for the Zapper and SnapScan apps in its own manifest, so you add nothing to yours. Sync Gradle and build once to check it compiles.

Apps that ship the SDK must include the Phosphor Icons MIT notice and the Instrument Sans OFL notice in `synqt-sdk/THIRD_PARTY_NOTICES.md` in this repo (the package itself doesn't carry it), for example on an open-source licences screen.

### 2. Add a token endpoint to your backend

SYNQT calls need a short-lived token, and minting one needs the merchant's private key, so this runs on your server only. For each order, your backend:

1. Creates a payin ID that is new for this attempt, such as `order-1042-1` (letters, digits, `_` and `-`, at most 250 characters).
2. Calls `POST /auth-tokens-by-action` with header `X-API-Version: 2` and this body, signed with SHA256withRSA over the exact bytes sent, base64-encoded in an `X-SIGNATURE` header:

   ```json
   {"merchantCode":"MID","validitySecs":1800,"action":"PAYIN_CREATE","idPayment":"order-1042-1",
    "money":{"amount":1250.00,"currencyCode":"ZAR"},"timestamp":1793625059}
   ```

3. Returns the `token` from the response, the payin ID and the amount to the app.

`PAYIN_CREATE` covers everything the sheet and the Payment Web App need. To show the merchant's name in the sheet's summary, use `POST /auth-tokens` instead, with the same signature and an `operations` list: `POST /payins/!availablePaymentOptions`, `POST /payins/order-1042-1`, `GET /payins/order-1042-1` and `GET /merchant-info`. The examples repo's `scripts/sandbox-token.sh` is a working example of the signing, in shell.

The `money` in the token locks it to that amount, so the app can't change it. Never return a token with wildcard operations to an app.

### 3. Create the payin in the app

When the payer taps your checkout button, fetch a token from your backend (off the main thread), then:

```kotlin
val payin = Synqt(Synqt.Environment.SANDBOX).payin(
    token = response.token,
    payinId = response.payinId,
    money = Money(BigDecimal("1250.00"), "ZAR"), // the same amount and currency as the token
    returnUrl = "https://shop.example.com/payment/return", // https, see "Other ways to pay"
    callbackUrl = "https://shop.example.com/synqt/callback", // your backend, step 7
)
```

This makes no network call. Use `SANDBOX` while testing and `LIVE` for real payments.

### 4. Choose the payment method

Ask SYNQT what this merchant can take for this amount, and keep the ones the sheet supports:

```kotlin
val offered = payin.availablePaymentOptions()
val supported = Synqt.directPaymentMethods.map { it.code }
val choices = offered.filter { it.code in supported }
```

Show `choices` in your own checkout, using `option.name(context)` as the payer-facing name, and let the payer pick one. If your checkout only offers one method, you can skip this step and pass its code, such as `"CAPITEC_PAY"`, in step 5.

### 5. Show the payment sheet

Call it from a ViewModel, so a rotation doesn't cancel it (cancelling the call closes the sheet):

```kotlin
class CheckoutViewModel : ViewModel() {
    fun pay(activity: Activity, payin: Payin, methodCode: String, email: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            val result = payin.presentPaymentSheet(
                activity,
                methodCode,
                prefill = mapOf(
                    "emailAddress" to email,
                    "customerName.firstName" to firstName,
                    "customerName.lastName" to lastName,
                ),
                onPaymentStarted = { /* details sent: start showing progress, step 6 */ },
            )
            when (result) {
                null -> { /* the payer closed the sheet without paying */ }
                is CreatePayinResult.Instructions -> { /* the sheet already showed the next step; go to step 6 */ }
                is CreatePayinResult.Redirect -> { /* open result.url, see below */ }
            }
        }
    }
}
```

Pass in `prefill` whatever your app already knows about the payer. The sheet hides those fields, and pays straight away when nothing mandatory is left to ask. See "The payment sheet" below for what it shows.

A `Redirect` means the payer finishes on a bank's page. For `httpMethod` GET, open `result.url` in a WebView or the browser. For POST, submit `result.formData` to it as an HTML form. The demo's `WebViewActivity` does both.

### 6. Show the payer where the payment stands

Once the sheet closes with a result, poll `payin.status()` every few seconds until it is final:

```kotlin
val s = payin.status()
when (s.status) {
    PayinStatus.Status.SUCCESS -> { /* show "Paid", but fulfil only from step 7 */ }
    PayinStatus.Status.FAILED, PayinStatus.Status.REFUSED -> showError(s.failureReason?.payerMessage(context))
    else -> { /* PROCESSING: keep polling */ }
}
```

Stop polling when the payer leaves the screen. Before the payment is created, `status()` throws `SynqtException` with HTTP 410.

### 7. Confirm the payment on your backend

SYNQT posts `{"idEntity":"order-1042-1"}` to your `callbackUrl` on every status change. Reply 200 at once, then call `GET /payins/order-1042-1` from your backend and fulfil the order only when `process.status` is `SUCCESS` and the amount matches. The callback carries no status and no signature, and the app's view of the status is not proof of payment.

### 8. Go live

Switch to `Synqt.Environment.LIVE` and your live merchant credentials, test one real payment per method you offer, and work through "Security and compliance" and "Before merchants use this" below.

## The payment sheet

`presentPaymentSheet` shows a sheet along the bottom on phones, and a centred card on tablets (600dp and wider, as on iPad), with:

- the Synqt wordmark, an order summary (amount due, payment method, reference, and the merchant name if the token allows `GET /merchant-info`)
- one plain-language field per detail the method needs, grouped into sections, with optional fields marked. A section with any mandatory field in it is mandatory as a whole, which is stricter than SYNQT's spec
- the right keyboard for each field, and checks on length and format before anything is sent
- a dropdown of the banks or providers SYNQT returns for this payin and payment method (`POST /payins/!availablePaymentOptions`), instead of raw operator codes. An optional bank field is hidden when SYNQT lists none, as on Capitec Pay, Absa Pay, FNB Pay and Nedbank Pay, which accept no bank code
- a Pay button with the amount, and a processing state with the Synqt Orbit loader while the request runs. It is drawn natively from the designer's Lottie file, so the SDK needs no animation library, and it follows light and dark mode

Fields the app prefills (through `prefill`) are hidden when their value passes its checks, and a section goes when all of its fields are filled in; once the first and last name are filled in, the optional title, middle name and suffix go too. When every mandatory field of a method is filled in this way, the sheet skips the form and pays straight away, showing only the processing screen. If SYNQT refuses a value, the form appears with the error so the payer can correct it. Methods with only optional fields always show their form.

When SYNQT accepts a payment the payer finishes in another app (Zapper, SnapScan, UPI and others), the sheet stays open and shows the next step: an "Open Zapper" button for this phone, the QR code for scanning from another phone, the amount (or the exact amount SYNQT requires), the reference, and a live "Time left to pay" countdown. For Pay offline the next step is "How to pay": the one-time reference in its own card at the top ("Your payment will fail without this reference."), then the bank account to transfer to (account holder, account number, bank and any account type SYNQT sends, then the branch, or the bank code when there is no branch) and the amount, with copy buttons on the values a payer types into their banking app (the amount is copied as a bare number), then a four-point "Before you pay" notice based on the Payment Web App's (`nextStep.payInto`). The notice covers exact amounts, one-time references, transfer fees and banking hours, and third-party payments; it is draft wording and needs sign-off from SYNQT's compliance owner before release. Done, or closing the sheet, returns the result. Apps with their own screens get the same details as `CreatePayinResult.Instructions.nextStep`.

If SYNQT refuses the payment, the reason appears in the sheet under the field it is about, so the payer can fix it and try again. The sheet follows the phone's light or dark setting, also when it changes while the sheet is open (it is rebuilt where the payer left it, unless a payment is being sent at that moment).

`onPresented` runs once the sheet is on screen, and `onPaymentStarted` each time the payer's details are sent to SYNQT (they passed the sheet's checks; SYNQT may still refuse them). Use them to show your own progress, such as a status indicator you start polling once a payment is sent.

The sheet runs in its own activity (declared in the SDK's manifest, so you add nothing), which keeps the payer's details and the next step through rotation. Cancelling the call closes the sheet, as on iOS. So call it from a scope that outlives your activity being rebuilt, such as a ViewModel's `viewModelScope`: from `lifecycleScope`, a rotation (unless your activity handles rotation itself) cancels the call and the sheet closes with it. The demo shows the pattern.

The sheet rebuilds itself in place when the payer switches light or dark mode, font size or language. If Android stops your app while the payer is in another app (their banking app or Zapper, say), the sheet is closed when they return; the payin itself carries on.

`prefill` takes field paths such as `emailAddress`, `phoneNumber` or `customerName.lastName`. A prefilled value that passes its checks is hidden, so the payer cannot change it; one that fails is shown with its error.

## Other ways to pay

**Build the payment method yourself.** Every method is a typed class, and its required fields must be set or the code won't compile. Objects that every method uses, such as `PaymentMethod.Account` and `PaymentMethod.PersonName`, are one class shared by all methods, so each method checks the parts it needs when you create it and throws `IllegalArgumentException` naming the missing or unexpected field:

```kotlin
val result = payin.create(
    PaymentMethod.CapitecPay(
        phoneNumber = "0821234567",
        customerName = PaymentMethod.PersonName(lastName = "Doe"),
    ),
)
```

Retrying `create` after a network failure is safe: if SYNQT already has the payin, the SDK fetches it instead of creating another.

**Use SYNQT's hosted Payment Web App** when you don't want any payment UI in the app:

```kotlin
when (val r = payin.presentPaymentApp(activity, merchantCode = "MID", onPresented = { /* on screen */ })) {
    is PaymentAppResult.Returned -> { /* reached returnUrl (r.url) */ }
    PaymentAppResult.Closed -> { /* the payer closed it */ }
}
// Either way, ask payin.status() where the payment stands.
```

Prefill it and choose the method with `extra`, which takes the Payment Web App's URL parameters, such as `emailAddress`, `phoneNumber`, `accountName` (it has no first or last name parameters), `paymentMethodCode` and `paymentOperatorCode`. Add `optionsAvailable=true` so the payer can still choose another method, and `allowCustomerDataChange=true` so they can edit what you prefilled. SYNQT's Payment Web App spec lists every parameter.

`presentPaymentApp` shows it inside the app under a bar with only a close button (the page shows the Synqt logo), and returns when the payer reaches the `returnUrl` or closes it; links to bank and payment apps open outside it, and Back steps back through its pages. Like the sheet, it survives rotation itself, and cancelling the call closes it. To use the browser instead, `payin.openPaymentApp(context, merchantCode = "MID")` (or `payin.paymentAppUrl(...)`), with a verified Android App Link as the `returnUrl` to come back to the app. The `returnUrl` must be https.

## Results and errors

| Call | Returns |
|---|---|
| `presentPaymentSheet` | `CreatePayinResult?` (null when the payer closes the sheet) |
| `presentPaymentApp` | `PaymentAppResult`: `Returned(url)` at the `returnUrl`, or `Closed` |
| `create` | `CreatePayinResult`: `Redirect(httpMethod, url, formData)` or `Instructions(paymentMethod)` |
| `status` | `PayinStatus`: `status` (PROCESSING, SUCCESS, FAILED, REFUSED or UNKNOWN), amounts, fee, failure reason. Show the payer `failureReason.payerMessage(context)`; SYNQT's own `message` is written for the merchant |
| `availablePaymentOptions` | `List<PaymentOption>`: code, banks or providers, and `name(context)`, the payer-facing name (null for methods the sheet doesn't support) |
| `setExternalReference` | nothing; sends the payer's bank or UTR reference for methods that need it |
| `Synqt.paymentOperators(token)` | `List<PaymentOperator>`: every bank and provider SYNQT knows, each with its `externalProviders` |

`paymentOperators` is not tied to a payin, and a `PAYIN_CREATE` token cannot call it. Your backend mints a token with `POST /auth-tokens` and the operation `GET /payment-operators`. It lists the whole system; to see what one merchant can use for a given amount, use `availablePaymentOptions`.

When SYNQT refuses a request, calls throw `SynqtException` with `httpCode` and `errors`, a list of `SynqtError(code, field, description)`. A 406 is a business error such as `INVALID_PHONE_NUMBER`, and `field` names the input it is about. The exception never contains the raw response, because SYNQT echoes what the payer typed in it. Network failures throw `IOException`.

A payin ID can be used once. Calling `create` again with the same ID is a safe retry and returns the existing payin, but only for the same payment method. If the ID was already used with another method, `create` throws `SynqtException` with HTTP 409 and the code `PAYIN_ALREADY_STARTED`, and the sheet tells the payer to start a new payment. Use a new payin ID and token for each attempt.

## Environments

| | API | Use |
|---|---|---|
| `SANDBOX` | `api.sandbox.synqt.io` | Testing. The amount decides the outcome (see SYNQT's sandbox docs). |
| `LIVE` | `api.synqt.io` | Real payments. |
| `STABLE` | `api.stable.aopay.io` | SYNQT's internal development platform. Not for merchants. |

## Matching your own screens

`SynqtUI` has the building blocks the sheet uses, for apps that want their screens to match: `text`, `card`, `field`, `dropdown`, `button` and `errorBanner`. Colours, the Instrument Sans font and the dark theme come with the SDK, prefixed `synqt_` so they can't clash with your resources.

## Translations

Every piece of text the SDK shows a payer is a string resource, so the sheet follows the phone's language once translations exist:

- `synqt-sdk/src/main/res/values/synqt_strings.xml`: the sheet's own text (headings, buttons, next-step, error and failure messages). It is generated too, with the same wording in every SYNQT SDK.
- `synqt-sdk/src/main/res/values/synqt_form_strings.xml`: method names, field labels, hints, validation messages and tile questions. It is generated with the code that uses it (see below).

To add a language, copy both files to `values-<language>` (for example `values-af` or `values-zu`) and translate the text, keeping the names and the `%1$s` placeholders. Names, amounts and bank names fill the placeholders at runtime. A string with no translation falls back to English. Two things stay in English whatever the language: SYNQT's own error descriptions and bank or provider names, which come from SYNQT's servers.

Because names and messages depend on the phone's language, `DirectPaymentMethod.name`, `PaymentOption.name` and `FailureReason.payerMessage` take a `Context`.

## Security and compliance

- Never put the merchant's private key or secret key in an app. Tokens come from your backend, one per order, locked to the amount.
- Only your backend's own status check proves a payment. The return URL, the sheet closing and `payin.status()` do not.
- This SDK has no card code. CARD is offered only with the separate card add-on, available to PCI DSS compliant merchants on request from SYNQT, which draws, checks and encrypts the card fields itself. The add-on uses this SDK's add-on API (`com.synqt.sdk.addon`, behind the `SynqtAddonApi` opt-in), which apps never need.
- Payment details include personal information (ID numbers, account numbers, phone numbers, emails) covered by POPIA. Don't log requests, responses or `SynqtError` descriptions alongside customer data.

## Generated code and the SYNQT API version

The payment method models (`PaymentMethods.kt`), the sheet's fields (`MethodFormData.kt`), all of the sheet's text (`synqt_form_strings.xml` and `synqt_strings.xml`), the colours (`synqt_colors.xml`, light and dark) and the shared field test cases (`FieldCasesTest.kt`) are generated from SYNQT's OpenAPI spec by a separate SYNQT app, not by anything in this repo, so change them there rather than here. CARD's raw card number fields are left out of this SDK, as they put the app in PCI DSS scope; only the card add-on has them.

They are generated from SYNQT API 2.84 today, and the spec lives with the generator app. A new SYNQT API version replaces the generated files in one commit, and every SYNQT SDK moves to the same version together, so they all ask payers for the same things in the same words.

The spec is written for SYNQT's global audience, and some fields it leaves optional are mandatory for South African payers. They are every Online banking field (bank, email address and the whole bank account section), and the bank or provider for Pay offline. A section holding one of them becomes mandatory too, and the payer sees them without "(optional)".

Where SYNQT's spec says a payer must give "at least one of" several details (Absa Pay, Capitec Pay and PayShap today), or where SYNQT has decided one detail is enough (FNB Pay: South African ID number or passport number, ID number first), the sheet turns them into tiles with radio behaviour. The first tile, in the spec's order, is the default, and only the chosen tile's fields are checked and sent. A tile can name companion fields that become mandatory with it, such as PayShap's account holder name. Provider code fields (`externalProviderCode`, on PayShap today) are dropdowns of every provider of those banks, whichever bank the payer picks, and fall back to a typed field when SYNQT lists none. Separately, wherever the ID number and passport number are both optional and not already in tiles (PayShap today), the payer picks which one to give, ID number first; both stay optional, as in the spec. A method can have more than one tile question.

## Running the demo

The demo app lives in the [synqt-examples](https://github.com/synqt-pay/synqt-examples) repo, under `android/`. This SDK is its `base` build variant. It installs the published SDK by default; with `-Psynqt.local=true` it builds this checkout instead, so clone that repo as `demos` next to this one to try changes before releasing them. Its README has the steps.

## Release

Releases are published to Maven Central (Central Portal) from a developer's Mac; there is no CI workflow.

1. Set `version` in `synqt-sdk/build.gradle.kts` (and the card add-on's, which always has the same version), and move the version's `CHANGELOG.md` entry from "Unreleased" to the date.
2. Run the checks: `./gradlew :synqt-sdk:testDebugUnitTest -q`, then the demo with `-Psynqt.local=true`.
3. Provide the Central Portal token and SYNQT's release signing key as Gradle properties (in `~/.gradle/gradle.properties`, never in this repo) or environment variables: `mavenCentralUsername`, `mavenCentralPassword`, `signingInMemoryKey` (the ASCII-armoured private key), `signingInMemoryKeyId` and `signingInMemoryKeyPassword`, or the same names prefixed `ORG_GRADLE_PROJECT_` in the environment.
4. Run `./gradlew :synqt-sdk:publishToMavenCentral`, then check the deployment and release it in the Central Portal (or run `./gradlew :synqt-sdk:publishAndReleaseToMavenCentral` to release it automatically). Maven Central won't replace a version that is already there, so every release needs a new one.
5. Tag the repo `v<version>`, then publish the card add-on's same version (its own README).

`./gradlew :synqt-sdk:publishToMavenLocal` builds the same AAR, sources and javadoc jars and POM into `~/.m2` to inspect before publishing. Without a signing key it publishes unsigned.

## Licence

The SDK is licensed under the Apache License, Version 2.0: see `LICENSE` and `NOTICE`. The Instrument Sans font and the Phosphor icons it ships keep their own licences (SIL Open Font License and MIT), listed in `synqt-sdk/THIRD_PARTY_NOTICES.md`.

## Before merchants use this

Still open, for the people who own them:

- payment method names not covered by the payment glossary, and the generated field labels and hints (including the new "OTT Voucher" and "Voucher PIN" drafts, the tile wording for Capitec Pay and PayShap, PayShap's "Which ID do you want to give?", the payer-facing failure messages in `synqt_strings.xml` (one per SYNQT failure reason code, new and in draft), and the provider dropdown's "Provider" label)
- the provisional mobile type scale
- the typeface: Instrument Sans (SIL Open Font License) replaces Inter in every SDK, chosen on 16 November 2026 and awaiting the Innovation Manager's sign-off
- the light Orbit loader puts aqua `#27FFE2` on a light background, which the brand reserves for dark backgrounds (1.27:1 on white); it is decorative and sits beside "Processing your payment"

Decided by the Innovation Manager on 26 October 2026:

- The wordmark in `synqt_logo.xml`, from the synqt-brand-design skill, is the master artwork.
- "Secure checkout" and the sheet's processing and error messages are approved for now.
- "Amount due" stays an uppercase label in the eyebrow colour, not the mobile label style's text colour.

Decided by the brand owner on 26 October 2026:

- The dark theme error colour is `#FFB4AE`.
- Field borders (`border-control`) are slate-muted `#7A8296` in both themes, which gives the field edge at least 3:1 contrast.
- Field labels are 16sp Medium.
- In dark mode, `action-tint` is magenta at 12%.
- UI icons come from Phosphor Icons, Regular weight. The sheet's padlock, close, warning and dropdown chevron are Phosphor `lock`, `x`, `warning-circle` and `caret-down`. The MIT licence notice is in `synqt-sdk/THIRD_PARTY_NOTICES.md`, and apps that ship the SDK must include it.
