package com.synqt.sdk.internal.sheet

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.synqt.sdk.PayinAcceptedResponse
import com.synqt.sdk.NextStep
import com.synqt.sdk.PaymentMethodPayin
import com.synqt.sdk.PaymentMethodResponsePayin
import com.synqt.sdk.PaymentOperatorIncoming
import com.synqt.sdk.format
import com.synqt.sdk.nextStep
import com.synqt.sdk.R
import com.synqt.sdk.SynqtException
import com.synqt.sdk.addon.MethodSection
import com.synqt.sdk.addon.MethodSectionException
import com.synqt.sdk.internal.form.Choice
import com.synqt.sdk.internal.form.FieldException
import com.synqt.sdk.internal.form.FieldKind
import com.synqt.sdk.internal.form.FormField
import com.synqt.sdk.internal.form.FormValues
import com.synqt.sdk.internal.form.MethodForm
import com.synqt.sdk.internal.form.filledIn
import com.synqt.sdk.internal.form.prefillProblems
import com.synqt.sdk.internal.form.problem
import com.synqt.sdk.internal.form.problems
import com.synqt.sdk.internal.form.providers
import com.synqt.sdk.internal.form.readyToPay
import com.synqt.sdk.internal.form.shows
import com.synqt.sdk.payerMessageFor
import com.synqt.sdk.ui.OrbitLoader
import com.synqt.sdk.ui.SynqtUI
import com.synqt.sdk.ui.choiceTiles
import com.synqt.sdk.ui.dp
import com.synqt.sdk.ui.shape
import com.synqt.sdk.ui.synqtIcon
import java.io.IOException
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** One rendered field: how to read, mark and clear its value, and the control that takes focus. */
private class Input(val view: View, val read: () -> String, val mark: (String?) -> Unit, val control: View, val clear: () -> Unit)

/** An add-on's method: whether it is secret, and how to make its fields for a sheet (with a rebuilt sheet's state). */
internal class SheetAddon(val secure: Boolean, val section: (Context, Any?) -> MethodSection)

/** What the payer does to a field: [edit] on every change, [leave] when focus moves away. */
private class Events(val edit: () -> Unit, val leave: () -> Unit)

/** A bank or provider field's "couldn't load" block: its banner, and Try again with the spinner shown while loading. */
private class BanksFailed(val banner: TextView, val button: Button, val spinner: View)

/**
 * Synqt-branded payment sheet: wordmark header, order summary, one payer-facing input per field of [form] under
 * section headings, and a Pay button pinned to the bottom. Pay checks the values, builds the typed method and hands
 * it to [submit]; a processing state covers the form meanwhile, and every error comes back next to its field.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class PaymentMethodSheet(
    private val host: Activity,
    private val form: MethodForm,
    private val prefill: Map<String, String>,
    /** Null when SYNQT's list of banks and providers could not be loaded. */
    operators: List<PaymentOperatorIncoming>?,
    private val summary: PaymentSummary,
    private val result: CompletableDeferred<PayinAcceptedResponse?>,
    private val restore: SheetState? = null,
    onPresented: () -> Unit = {},
    private val onPaymentStarted: () -> Unit = {},
    /** Loads the banks and providers again, with the same retries; null if they still could not be loaded. */
    private val reloadOperators: suspend () -> List<PaymentOperatorIncoming>? = { null },
    private val addon: SheetAddon? = null,
    private val submit: suspend (PaymentMethodPayin) -> PayinAcceptedResponse,
) {
    private val scope = MainScope()
    private val activity = host.withSystemNightMode()
    private val methodName = activity.getString(form.name)
    private val dialog = Dialog(activity)
    // Mandatory bank or provider fields shown as "couldn't load" with Try again, by path, until the list loads.
    private val banksFailed = mutableMapOf<String, BanksFailed>()
    private var loadingBanks = false
    private val inputs: MutableMap<String, Input> = form.fields.mapNotNull { field ->
        // An optional field stays as it is without a list: a bank is hidden, a provider takes a typed code.
        val missing = operators == null && field.required && (field.kind == FieldKind.OPERATOR || field.kind == FieldKind.PROVIDER)
        (if (missing) banksFailed(field) else activity.input(field, (restore?.values ?: prefill)[field.path], operators.orEmpty(), events(field.path)))?.let { field.path to it }
    }.toMap(mutableMapOf())
    // An add-on method's own fields, which it draws, checks and builds itself.
    private val addonSection: MethodSection? = addon?.section?.invoke(activity, restore?.addon)

    // Fields are checked as the payer goes, but a field they have not typed in only shows "Required" after Pay.
    private val edited = mutableSetOf<String>()
    private val invalid = mutableSetOf<String>() // showing a problem found here, rechecked on every change
    private val refused = mutableSetOf<String>() // showing SYNQT's error, kept until the payer edits that field
    // The tile picked for each question: the one the app prefilled, else the first.
    private val chosen = restore?.chosen?.toMutableList()
        ?: form.oneOfs.map { oneOf -> oneOf.choices.firstOrNull { !prefill[it.paths.first()].isNullOrBlank() } ?: oneOf.choices.first() }.toMutableList()
    private val banner = SynqtUI.errorBanner(activity)
    // Fields the app prefilled with a value that passes its checks: hidden, but still sent. Shown again if SYNQT refuses it.
    private val filledIn = mutableSetOf<String>()
    private val detailsHeading = SynqtUI.text(activity, activity.getString(R.string.synqt_sheet_your_details), 20f, weight = SynqtUI.Weight.SEMIBOLD)
    // The intro only says there is nothing to enter; with fields to fill, the heading leads straight into them.
    private val nothingToEnter = inputs.isEmpty() && addonSection == null
    private val intro = SynqtUI.text(activity, activity.getString(R.string.synqt_sheet_intro_nothing_to_enter), 16f, R.color.synqt_text_muted)
    private val headings = mutableListOf<Pair<Int, View>>() // section heading views, by section
    private val questions = mutableListOf<View>() // each tile question's view, in order
    private val scroll = ScrollView(activity)
    private val processing = activity.processingView()
    // The sheet itself, at the bottom of a transparent full-screen window: a wrap-content window keeps its old
    // height after a rotation, so the sheet couldn't grow into a taller screen.
    private val panel = FrameLayout(activity)
    private var bottomBar: LinearLayout
    // The payin result once SYNQT accepted it, returned however the payer closes the sheet afterwards.
    private var accepted: PayinAcceptedResponse? = restore?.accepted
    private var openedApp = restore?.openedApp ?: false
    // On a large screen the sheet is a card in the middle, as on iPad, not a sheet along the bottom.
    private var centered = false
    private var detaching = false // replaced by a rebuilt sheet, so closing this one is not the payer closing it

    init {
        val context = activity
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(8))
            addView(context.summaryCard(summary, secure = true))
            addView(detailsHeading.apply { setPadding(0, context.dp(24), 0, context.dp(if (nothingToEnter) 8 else 16)) })
            addView(intro.apply { setPadding(0, 0, 0, context.dp(16)) })
            var section: Int? = null
            form.fields.forEach { field ->
                val input = inputs[field.path] ?: return@forEach
                val question = form.oneOfs.indexOfFirst { oneOf -> oneOf.choices.any { field.path in it.paths } }
                if (question >= 0) {
                    // A question's tiles and all its fields go where the first of those fields would have been.
                    val paths = form.oneOfs[question].choices.flatMap { it.paths }
                    if (field.path == paths.first { it in inputs }) addView(oneOfView(question, paths).also(questions::add))
                    return@forEach
                }
                if (field.section != null && field.section != section) {
                    val heading = SynqtUI.text(context, context.getString(field.section), 17f, weight = SynqtUI.Weight.SEMIBOLD).apply { setPadding(0, context.dp(8), 0, context.dp(16)) }
                    headings += field.section to heading
                    addView(heading)
                }
                section = field.section
                addView(input.view)
            }
            addonSection?.let { addView(it.view) }
            addView(banner)
        }
        scroll.addView(content)
        chosen.forEachIndexed(::choose)
        filledIn += restore?.filledIn ?: form.filledIn(values(), prefill, chosen)
        showFields()
        // A prefilled value left in view failed its checks, so the payer sees why straight away.
        if (restore == null) form.prefillProblems(values(), prefill, filledIn, chosen).forEach { error ->
            val path = checkNotNull(error.attr)
            inputs.getValue(path).mark(error.text(activity))
            invalid += path
        }

        bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            // 12dp + the button's 4dp focus-ring gap lines the pill up with the fields' 16dp margin.
            setPadding(context.dp(12), context.dp(8), context.dp(12), context.dp(16))
            addView(SynqtUI.button(context, context.getString(R.string.synqt_sheet_pay, summary.amount), SynqtUI.ButtonStyle.PRIMARY).apply {
                fontFeatureSettings = "tnum"
                capScale(16f)
                (layoutParams as LinearLayout.LayoutParams).topMargin = 0
                setOnClickListener { onPay() }
            })
        }
        val bottom = bottomBar.paddingBottom
        bottomBar.setOnApplyWindowInsetsListener { view, insets ->
            val inset = if (Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime()).bottom
            } else {
                @Suppress("DEPRECATION") insets.systemWindowInsetBottom
            }
            // A centred card doesn't reach the bottom edge; the window still shrinks for the keyboard.
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottom + if (centered) 0 else inset)
            insets
        }

        // Tall forms scroll between context fixed header and context fixed Pay button; short ones keep the sheet short.
        val body = CappedColumn(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(context.sheetHeader { dialog.cancel() })
            addView(context.hairline())
            addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, 1f))
            addView(context.hairline())
            addView(bottomBar)
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        // A stray tap would lose what the payer typed or the payment instructions; X and Back still close it.
        dialog.setCanceledOnTouchOutside(false)
        panel.background = GradientDrawable().apply { setColor(context.getColor(R.color.synqt_surface)) }
        panel.clipToOutline = true // keeps the processing view inside the rounded corners
        panel.addView(body)
        panel.addView(processing, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        dialog.setContentView(FrameLayout(context).apply {
            addView(panel, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        })
        dialog.window?.apply {
            setBackgroundDrawable(GradientDrawable().apply { setColor(Color.TRANSPARENT) })
            setLayout(MATCH_PARENT, MATCH_PARENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.6f)
            // Now the window reaches the navigation bar, draw its background (clear) ourselves, or Android paints it black.
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            @Suppress("DEPRECATION") navigationBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION") setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            if (addon?.secure == true) addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        fitWindow()
        dialog.setOnShowListener { onPresented() }
        dialog.setOnDismissListener {
            // What the payer typed doesn't outlive the sheet (context rebuilt one has already taken it over in SheetState).
            addonSection?.clear()
            if (detaching) return@setOnDismissListener
            result.complete(accepted) // null if the payer closed it before paying; no-op once context result has completed it
            scope.cancel()
            host.finish()
        }
    }

    /** [result] completes when the payer closes it: null if they never paid. */
    fun show() {
        dialog.show()
        val next = accepted?.nextStep()
        when {
            next != null -> showNextStep(next) // rebuilt after SYNQT accepted the payment
            // Everything mandatory came from the app, so pay straight away; the processing view covers the form, and
            // any refusal brings the form back with the error.
            restore == null && addonSection == null && form.readyToPay(values(), filledIn, chosen) -> onPay()
        }
    }

    /** False while a payment is being sent, as rebuilding would drop its result. */
    val canRebuild: Boolean get() = processing.visibility != View.VISIBLE

    /** Closes this sheet for a rebuilt one, without counting as the payer closing it, and returns where they were. */
    fun detach(): SheetState {
        val state = SheetState(inputs.mapValues { it.value.read() }, chosen.toList(), filledIn.toSet(), accepted, openedApp, addonSection?.state())
        detaching = true
        scope.cancel()
        dialog.dismiss()
        return state
    }

    fun close() {
        if (dialog.isShowing) dialog.dismiss()
    }

    /**
     * A centred card with all corners round on a tablet (600dp and wider either way, as iPad's form sheet), else a sheet
     * along the bottom. At most 640dp wide, Material's bottom sheet maximum, so it doesn't stretch across a wide
     * screen; phones stay full width. Rerun on rotation.
     */
    fun fitWindow() {
        val config = host.resources.configuration
        centered = config.smallestScreenWidthDp >= 600
        val radius = activity.dp(24).toFloat()
        (panel.background as GradientDrawable).cornerRadii = floatArrayOf(radius, radius, radius, radius) + if (centered) floatArrayOf(radius, radius, radius, radius) else FloatArray(4)
        panel.layoutParams = (panel.layoutParams as FrameLayout.LayoutParams).apply {
            width = if (config.screenWidthDp > 640) activity.dp(640) else MATCH_PARENT
            gravity = if (centered) Gravity.CENTER else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        bottomBar.requestApplyInsets()
    }

    private fun events(path: String) = Events({ onEdit(path) }, { onLeave(path) })

    /**
     * In place of a mandatory bank or provider field whose list didn't load: its label, why, and Try again. Pay's
     * required check still scrolls here, as this is the field's view.
     */
    private fun banksFailed(field: FormField): Input {
        val context = activity
        val banner = SynqtUI.errorBanner(context).apply {
            text = context.getString(R.string.synqt_sheet_banks_failed)
            visibility = View.VISIBLE
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = 0
        }
        val button = SynqtUI.button(context, context.getString(R.string.synqt_sheet_try_again), SynqtUI.ButtonStyle.OUTLINE).apply { setOnClickListener { retryBanks() } }
        val spinner = ProgressBar(context).apply {
            indeterminateTintList = ColorStateList.valueOf(context.getColor(R.color.synqt_text))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO // the button keeps its name, and reads as disabled
            visibility = View.GONE
        }
        banksFailed[field.path] = BanksFailed(banner, button, spinner)
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = context.dp(12) }
            addView(SynqtUI.text(context, context.getString(field.label), 16f, weight = SynqtUI.Weight.MEDIUM).apply { setPadding(0, 0, 0, context.dp(8)) })
            addView(banner)
            // The pill's 4dp focus-ring gap overhangs, so the pill lines up with the banner, 8dp under it.
            addView(FrameLayout(context).apply {
                addView(button, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                addView(spinner, FrameLayout.LayoutParams(context.dp(24), context.dp(24), Gravity.CENTER))
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(-context.dp(4), context.dp(4), -context.dp(4), 0) })
        }
        return Input(view, { "" }, {}, button) {}
    }

    /** Try again: loads the banks and providers once more; on success the real fields take the blocks' places. */
    private fun retryBanks() {
        if (loadingBanks) return
        fun loading(on: Boolean) {
            loadingBanks = on
            banksFailed.values.forEach { block ->
                block.button.isEnabled = !on
                block.button.setTextColor(activity.getColor(if (on) android.R.color.transparent else R.color.synqt_text))
                block.spinner.visibility = if (on) View.VISIBLE else View.GONE
            }
        }
        loading(true)
        scope.launch {
            val loaded = reloadOperators()
            loading(false)
            if (loaded == null) {
                // Set again so the banner, a live region, tells TalkBack it failed once more.
                banksFailed.values.forEach { it.banner.text = activity.getString(R.string.synqt_sheet_banks_failed) }
                return@launch
            }
            banksFailed.keys.forEach { path ->
                val old = inputs.getValue(path)
                val input = activity.input(form.fields.first { it.path == path }, prefill[path], loaded, events(path)) ?: return@forEach
                val parent = old.view.parent as ViewGroup
                parent.addView(input.view, parent.indexOfChild(old.view))
                parent.removeView(old.view)
                inputs[path] = input
                invalid -= path
                refused -= path
            }
            banksFailed.clear()
            showFields()
        }
    }

    /** Fields of tiles the payer did not pick read as blank, so they are neither checked nor sent. */
    private fun values() = inputs.mapValues { (path, input) -> if (form.shows(path, chosen)) input.read() else "" }

    private fun oneOfView(question: Int, paths: List<String>) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        val context = activity
        val oneOf = form.oneOfs[question]
        val asked = context.getString(oneOf.question)
        addView(SynqtUI.text(context, asked, 17f, weight = SynqtUI.Weight.SEMIBOLD).apply { setPadding(0, context.dp(8), 0, context.dp(16)) })
        addView(context.choiceTiles(asked, oneOf.choices.map { context.getString(it.label) }, oneOf.choices.indexOf(chosen[question])) { choose(question, oneOf.choices[it]) })
        addView(View(context), LinearLayout.LayoutParams(0, context.dp(16)))
        paths.mapNotNull(inputs::get).forEach { addView(it.view) }
    }

    /** Shows the picked tile's fields. The others are emptied, so a detail the payer switched away from is never sent. */
    private fun choose(question: Int, choice: Choice) {
        chosen[question] = choice
        form.oneOfs[question].choices.flatMap { it.paths }.forEach { path ->
            val input = inputs[path] ?: return@forEach
            val show = path in choice.paths
            if (!show) {
                input.clear()
                input.mark(null)
                edited -= path
                invalid -= path
                refused -= path
                filledIn -= path // emptied, so the payer enters it if they come back to this tile
            }
        }
        showFields()
    }

    /**
     * Shows each field unless its tile is not picked or the app filled it in. A section heading or tile question with
     * nothing left to show is hidden too.
     */
    private fun showFields() {
        fun visible(path: String) = form.shows(path, chosen) && path !in filledIn
        inputs.forEach { (path, input) -> input.view.visibility = if (visible(path)) View.VISIBLE else View.GONE }
        questions.forEachIndexed { index, view ->
            val answered = chosen[index].paths.filter { it in inputs }.all { it in filledIn }
            view.visibility = if (answered) View.GONE else View.VISIBLE
        }
        headings.forEach { (section, view) ->
            view.visibility = if (form.fields.any { it.section == section && it.path in inputs && visible(it.path) }) View.VISIBLE else View.GONE
        }
        // Everything filled in by the app: the whole details section goes, leaving the summary and Pay.
        val allFilledIn = inputs.isNotEmpty() && inputs.keys.none(::visible)
        detailsHeading.visibility = if (allFilledIn) View.GONE else View.VISIBLE
        intro.visibility = if (nothingToEnter) detailsHeading.visibility else View.GONE
    }

    private fun onEdit(path: String) {
        edited += path
        if (refused.remove(path)) inputs.getValue(path).mark(null)
        if (path in invalid) check(path)
    }

    private fun onLeave(path: String) {
        if (path in edited && path !in refused) check(path)
    }

    private fun check(path: String) {
        val message = problem(form.fields.first { it.path == path }, values(), mustFill = chosen.any { path in it.required })?.text(activity)
        inputs.getValue(path).mark(message)
        if (message == null) invalid -= path else invalid += path
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onPay() {
        inputs.values.forEach { it.mark(null) }
        invalid.clear()
        refused.clear()
        banner.visibility = View.GONE
        val values = values()
        form.problems(values, chosen).takeIf { it.isNotEmpty() }?.let { return showErrors(it) }
        val section = addonSection
        if (section != null && !section.check()) return
        val method = if (section != null) null else try {
            form.build(FormValues(values))
        } catch (error: FieldException) {
            return showErrors(listOf(PayinError(error.path, R.string.synqt_sheet_required)))
        }
        setProcessing(true)
        scope.launch {
            try {
                // An add-on builds its method only now, so it can do slow work such as encrypting card details.
                val chosen = method ?: try {
                    PaymentMethodPayin.Addon(form.code, checkNotNull(section).paymentMethod())
                } catch (error: MethodSectionException) {
                    return@launch showMessage(error.payerMessage)
                }
                onPaymentStarted()
                val created = submit(chosen)
                val next = created.nextStep()
                if (next?.appLink != null || next?.qrCodeImageUrl != null || next?.payInto != null) {
                    accepted = created
                    showNextStep(next)
                } else {
                    result.complete(created)
                    dialog.dismiss()
                }
            } catch (error: SynqtException) {
                showErrors(payinErrors(error), fromSynqt = true)
            } catch (error: IOException) {
                showErrors(listOf(PayinError(null, R.string.synqt_error_unreachable)))
            } finally {
                setProcessing(false)
            }
        }
    }

    /**
     * For methods the payer finishes in another app: open it on this phone, scan the QR code from another one, or (Pay
     * offline) transfer to SYNQT's account from their banking app.
     * The form is replaced; Done or closing the sheet returns the accepted result.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNextStep(next: NextStep) {
        val context = activity
        val problem = SynqtUI.errorBanner(context)
        val app = next.appLink
        // With the method's own app installed, the payment opens straight in it; otherwise the link goes to the browser.
        val inApp = { listOfNotNull(app, next.qrCodeData).firstNotNullOfOrNull(::appIntent) }
        val transfer = next.payInto
        val amount = next.amountToPay?.format() ?: summary.amount
        val how = when {
            transfer != null -> context.getString(R.string.synqt_next_transfer)
            app != null && next.qrCodeImageUrl != null -> context.getString(R.string.synqt_next_open_or_scan, methodName)
            app != null -> context.getString(R.string.synqt_next_open, methodName)
            else -> context.getString(R.string.synqt_next_scan, methodName)
        }
        scroll.removeAllViews()
        scroll.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(8))
            // A transfer reference that contains the order ID would read as context second, near-identical "Reference", so the order ID goes.
            val orderIdInReference = transfer != null && next.reference?.contains(summary.reference) == true
            addView(context.summaryCard(summary.copy(amount = amount), showReference = !orderIdInReference))
            val timer = next.expiresAt?.let { countdown(it) }
            val reference = next.reference.takeIf { transfer != null }
            // The countdown (with context transfer's reference) sits in one card above the instructions, the same for every method.
            if (reference != null || timer != null) addView(context.referenceCard(reference, timer).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = context.dp(16) })
            val title = if (transfer != null) context.getString(R.string.synqt_next_transfer_title) else context.getString(R.string.synqt_next_title, methodName)
            addView(SynqtUI.text(context, title, 20f, weight = SynqtUI.Weight.SEMIBOLD).apply { setPadding(0, context.dp(24), 0, context.dp(8)) })
            addView(SynqtUI.text(context, how, 16f, R.color.synqt_text_muted).apply { setPadding(0, 0, 0, context.dp(8)) })
            next.amountToPay?.let { addView(SynqtUI.text(context, context.getString(R.string.synqt_next_pay_exactly, it.format() ?: amount), 16f, weight = SynqtUI.Weight.MEDIUM).apply { fontFeatureSettings = "tnum"; setPadding(0, 0, 0, context.dp(8)) }) }
            if (transfer != null) {
                // A banking app wants the number alone, not "R100.00".
                val copyAmount = next.amountToPay?.amount?.toPlainString() ?: amount.filter { it.isDigit() || it == '.' }
                addView(context.transferCard(transfer, amount, copyAmount))
                addView(context.transferNotice())
            } else next.reference?.let { addView(SynqtUI.text(context, context.getString(R.string.synqt_next_reference, it), 16f, weight = SynqtUI.Weight.MEDIUM).apply { setPadding(0, 0, 0, context.dp(8)) }) }
            next.qrCodeImageUrl?.let { addView(qrCode(it)) }
            addView(problem)
        })
        scroll.scrollTo(0, 0)
        bottomBar.removeAllViews()
        if (app != null) {
            bottomBar.addView(SynqtUI.button(context, context.getString(R.string.synqt_next_open_app, methodName), SynqtUI.ButtonStyle.PRIMARY).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = 0
                capScale(16f)
                // Checked again on each tap, as the payer may install the app while the sheet is open.
                setOnClickListener { val intent = inApp(); if (intent == null || runCatching { host.startActivity(intent) }.isFailure) open(app, problem) }
            })
        }
        bottomBar.addView(SynqtUI.button(context, context.getString(R.string.synqt_next_done), if (app != null) SynqtUI.ButtonStyle.OUTLINE else SynqtUI.ButtonStyle.PRIMARY).apply {
            if (app == null) (layoutParams as LinearLayout.LayoutParams).topMargin = 0
            capScale(16f)
            setOnClickListener { dialog.dismiss() }
        })
        // Once only: coming back from the app shows this step, with the button to open it again.
        if (!openedApp) inApp()?.let {
            openedApp = true
            runCatching { host.startActivity(it) }
        }
    }

    /**
     * "Time left to pay" over a large "01:59:17", right-aligned and ticking every second until [expiresAt], then a line
     * saying time ran out.
     */
    private fun countdown(expiresAt: String) = LinearLayout(activity).apply {
        val context = activity
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.END
        setPadding(0, context.dp(8), 0, context.dp(8))
        val label = SynqtUI.text(context, context.getString(R.string.synqt_next_time_left), 13f, R.color.synqt_eyebrow, SynqtUI.Weight.MEDIUM).apply { isAllCaps = true; letterSpacing = 0.08f }
        val time = SynqtUI.text(context, "", 28f, weight = SynqtUI.Weight.SEMIBOLD).apply {
            fontFeatureSettings = "tnum"
            gravity = Gravity.END
            setCompoundDrawablesRelativeWithIntrinsicBounds(context.synqtIcon(R.drawable.synqt_ic_timer, R.color.synqt_text, 28), null, null, null)
            compoundDrawablePadding = context.dp(8)
        }
        // Wrapped, not full width, so the label sits right and the icon stays next to the time.
        addView(label, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        addView(time, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        scope.launch {
            while (true) {
                val left = timeLeft(expiresAt, Instant.now())
                if (left == null) { visibility = View.GONE; break }
                if (left.isEmpty()) {
                    label.visibility = View.GONE
                    time.setCompoundDrawablesRelative(null, null, null, null)
                    time.textSize = 16f
                    time.setTextColor(context.getColor(R.color.synqt_danger))
                    time.text = context.getString(R.string.synqt_next_expired)
                    break
                }
                time.text = left
                contentDescription = context.getString(R.string.synqt_next_time_left) + " " + left
                delay(1_000)
            }
        }
    }

    /** [link] as an intent for this method's own app (see [APP_PACKAGES]), or null if that app is not installed or cannot open it. */
    private fun appIntent(link: String): Intent? {
        val pkg = APP_PACKAGES[form.code] ?: return null
        return appLinks(link, APP_SCHEMES[form.code]).map { Intent(Intent.ACTION_VIEW, Uri.parse(it)).setPackage(pkg) }
            .firstOrNull { it.resolveActivity(activity.packageManager) != null }
    }

    private fun open(link: String, problem: TextView) = try {
        host.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    } catch (error: ActivityNotFoundException) {
        problem.text = activity.getString(R.string.synqt_next_no_app, methodName)
        problem.visibility = View.VISIBLE
    }

    /** SYNQT's QR image, on white in both themes because scanners need a light ground. */
    private fun qrCode(url: String) = LinearLayout(activity).apply {
        val context = activity
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(0, context.dp(16), 0, context.dp(16))
        val image = ImageView(context).apply {
            contentDescription = context.getString(R.string.synqt_next_qr_description, methodName)
            background = context.shape(Color.WHITE, 12, 1, context.getColor(R.color.synqt_border))
            setPadding(context.dp(12), context.dp(12), context.dp(12), context.dp(12))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val caption = SynqtUI.text(context, context.getString(R.string.synqt_next_qr_loading), 13f, R.color.synqt_text_muted).apply { setPadding(0, context.dp(8), 0, 0) }
        addView(image, LinearLayout.LayoutParams(context.dp(240), context.dp(240)))
        addView(caption)
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { runCatching { download(url) }.getOrNull() }
            if (bitmap != null) {
                image.setImageBitmap(bitmap)
                caption.text = context.getString(R.string.synqt_next_qr_caption, methodName)
            } else {
                image.visibility = View.GONE
                caption.text = context.getString(R.string.synqt_next_qr_failed)
            }
        }
    }

    /** While the request runs the form is covered and the sheet cannot be dismissed half way through. */
    private fun setProcessing(on: Boolean) {
        processing.visibility = if (on) View.VISIBLE else View.GONE
        // TalkBack can't wander into the covered form, and hears the processing message as it appears.
        panel.getChildAt(0).importantForAccessibility = if (on) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        if (on && Build.VERSION.SDK_INT >= 28) processing.accessibilityPaneTitle = activity.getString(R.string.synqt_sheet_processing)
        dialog.setCancelable(!on)
    }

    /** [text] in the banner above the Pay button, for a problem no field owns. */
    private fun showMessage(text: String) {
        banner.text = text
        banner.visibility = View.VISIBLE
        scroll.post { scroll.smoothScrollTo(0, banner.top) }
    }

    /**
     * Errors for a shown field go under it; the rest go in the banner above the Pay button, named by label, not JSON
     * path. The first field with an error takes focus so the payer, and a screen reader, land on it.
     */
    private fun showErrors(errors: List<PayinError>, fromSynqt: Boolean = false) {
        val general = mutableListOf<String>()
        var first: View? = null
        var firstInput: Input? = null
        errors.forEach { error ->
            val section = addonSection
            if (error.attr != null && section?.showError(error.attr, error.text(activity)) == true) {
                if (first == null) first = section.view
                return@forEach
            }
            val path = error.attr?.let { attr -> form.fields.firstOrNull { attr == it.path || attr.endsWith(".${it.path}") }?.path }
            val input = path?.takeIf { form.shows(it, chosen) }?.let(inputs::get)
            if (input != null) {
                if (filledIn.remove(path)) showFields() // a prefilled value was refused: let the payer correct it
                input.mark(error.text(activity))
                if (fromSynqt) refused += path else invalid += path
                if (first == null) {
                    first = input.view
                    firstInput = input
                }
            } else {
                val label = path?.let { fieldPath -> activity.getString(form.fields.first { it.path == fieldPath }.label) }
                general += if (label != null) activity.getString(R.string.synqt_sheet_field_error, label, error.text(activity)) else error.text(activity)
            }
        }
        if (general.isNotEmpty()) {
            banner.text = general.joinToString("\n")
            banner.visibility = View.VISIBLE
            if (first == null) first = banner
        }
        first?.let { view -> scroll.post { scroll.smoothScrollTo(0, view.top) } }
        firstInput?.control?.requestFocus()
    }
}

/**
 * The sheet follows the phone's light or dark setting even when the host app forces its own mode (AppCompat's
 * setDefaultNightMode, for example, overrides only the app's configuration, not the system one).
 */
private fun Activity.withSystemNightMode(): Context {
    val night = Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val config = Configuration(resources.configuration).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
    }
    // Wrapping the activity keeps its window token, so the dialog still attaches to it.
    return ContextThemeWrapper(this, 0).apply {
        applyOverrideConfiguration(config)
        theme.applyStyle(R.style.SynqtSheetTint, true)
    }
}

/**
 * Caps its height at [share] of the window so the sheet never covers the whole screen; the weighted scroll area gives way
 * first. The same on phones and tablets: a lower cap for the centred card left a landscape tablet about one field.
 */
private class CappedColumn(context: Context) : LinearLayout(context) {
    private val share = 0.92f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // From the space the window is given now, so it follows rotation; resources can still hold the old size.
        val size = MeasureSpec.getSize(heightMeasureSpec)
        val cap = ((if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) resources.displayMetrics.heightPixels else size) * share).toInt()
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(cap, MeasureSpec.AT_MOST))
    }
}

/**
 * The Pay bar stays on screen, so its text stops growing at this font scale (about iOS's accessibility1) and the
 * scrolling form between it and the header keeps the room. The header has no text, so it doesn't grow at all.
 */
private const val BAR_MAX_SCALE = 1.6f

/** Caps this [sp]-sized text at [BAR_MAX_SCALE]; below it the text follows the payer's font size as usual. */
private fun TextView.capScale(sp: Float) {
    val max = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sp * BAR_MAX_SCALE, resources.displayMetrics)
    if (textSize > max) setTextSize(TypedValue.COMPLEX_UNIT_PX, max)
}

/** What autofill may offer from the payer's saved details for a field; email and phone get theirs from their kind. */
private val AUTOFILL_HINTS = mapOf(
    "customerName.titleBefore" to "personNamePrefix", "customerName.firstName" to "personGivenName",
    "customerName.middleName" to "personMiddleName", "customerName.lastName" to "personFamilyName",
    "customerName.titleAfter" to "personNameSuffix", "account.accountName" to View.AUTOFILL_HINT_NAME,
    "paymentLocation.postalCode" to View.AUTOFILL_HINT_POSTAL_CODE, "paymentLocation.city" to "addressLocality",
) // androidx.autofill's HintConstants values, without adding the library for eight strings

internal fun Context.hairline() = View(this).apply {
    setBackgroundColor(getColor(R.color.synqt_border))
    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 1)
}

/** Wordmark on the left (26dp, the smallest size the brand uses), close button on the right. No wordmark over the Payment Web App, which shows its own. */
internal fun Context.sheetHeader(logo: Boolean = true, onClose: () -> Unit) = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    setPadding(dp(16), dp(8), dp(4), dp(8)) // 4dp + the 48dp button's 12dp inset puts the glyph on the 16dp margin
    if (logo) addView(ImageView(context).apply { setImageResource(R.drawable.synqt_logo); contentDescription = getString(R.string.synqt_sheet_logo) })
    addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
    addView(ImageButton(context).apply {
        setImageDrawable(synqtIcon(R.drawable.synqt_ic_close, R.color.synqt_text))
        background = RippleDrawable(ColorStateList.valueOf(getColor(R.color.synqt_pressed_overlay)), null, null)
        contentDescription = getString(R.string.synqt_sheet_close)
        setOnClickListener { onClose() }
    }, LinearLayout.LayoutParams(dp(48), dp(48)))
}

/** The checkout's order summary: amount due in large type, then who, how and which order; on the form, "Secure checkout" last. */
private fun Context.summaryCard(summary: PaymentSummary, showReference: Boolean = true, secure: Boolean = false) = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    background = shape(getColor(R.color.synqt_surface_panel), 12)
    setPadding(dp(16), dp(16), dp(16), dp(16))
    // Eyebrow, not brand magenta: magenta on the light panel is 2.89:1.
    addView(SynqtUI.text(context, getString(R.string.synqt_sheet_amount_due), 13f, R.color.synqt_eyebrow, SynqtUI.Weight.MEDIUM).apply { isAllCaps = true; letterSpacing = 0.08f })
    addView(SynqtUI.text(context, summary.amount, 28f, weight = SynqtUI.Weight.SEMIBOLD).apply { fontFeatureSettings = "tnum"; setPadding(0, dp(4), 0, dp(16)) })
    addView(hairline())
    listOfNotNull(
        summary.merchant?.let { getString(R.string.synqt_sheet_paying) to it },
        getString(R.string.synqt_sheet_payment_method) to summary.method,
        if (showReference) getString(R.string.synqt_sheet_reference) to summary.reference else null,
    ).forEach { (label, value) -> addView(detailRow(label, value)) }
    // One TextView with the lock as its drawable, so TalkBack reads it as a single element.
    if (secure) addView(SynqtUI.text(context, getString(R.string.synqt_sheet_secure_checkout), 13f, R.color.synqt_text_muted).apply {
        setCompoundDrawablesRelativeWithIntrinsicBounds(synqtIcon(R.drawable.synqt_ic_lock, R.color.synqt_text_muted, 16), null, null, null)
        compoundDrawablePadding = dp(8)
        gravity = Gravity.CENTER
        setPadding(0, dp(12), 0, 0)
        // Centred as a whole, so the lock stays next to the words.
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL }
    })
}

/** The countdown card at the top of every next step; for OFFLINE it leads with the reference the payer must quote, as a missing one fails the payment. */
private fun Context.referenceCard(reference: String?, timer: View?) = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.END
    background = shape(getColor(R.color.synqt_surface), 12, 1, getColor(R.color.synqt_border))
    setPadding(dp(16), dp(16), dp(16), dp(16))
    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(0, dp(8), 0, dp(8)) }
    if (reference != null) {
        addView(SynqtUI.text(context, getString(R.string.synqt_next_reference_title), 13f, R.color.synqt_eyebrow, SynqtUI.Weight.MEDIUM).apply { isAllCaps = true; letterSpacing = 0.08f; gravity = Gravity.END })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            addView(SynqtUI.text(context, reference, 24f, weight = SynqtUI.Weight.SEMIBOLD).apply { setTextIsSelectable(true); gravity = Gravity.END }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            // The button's touch area overhangs the padding so the icon itself ends on the card's edge, like the time below.
            addView(copyButton(getString(R.string.synqt_sheet_reference), reference).apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = -dp(12) })
        })
        addView(SynqtUI.text(context, getString(R.string.synqt_next_reference_needed), 14f, R.color.synqt_text_muted).apply { gravity = Gravity.END })
    }
    timer?.let {
        if (reference != null) addView(hairline().apply { (layoutParams as LinearLayout.LayoutParams).setMargins(0, dp(16), 0, dp(8)) })
        addView(it, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }
}

/** The bank details an OFFLINE payer types into their own banking app, each with a copy button. */
private fun Context.transferCard(account: PaymentMethodResponsePayin.Account, amount: String, copyAmount: String) = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    background = shape(getColor(R.color.synqt_surface_panel), 12)
    setPadding(dp(16), dp(8), dp(16), dp(16))
    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(0, dp(8), 0, dp(8)) }
    listOfNotNull(
        account.accountName?.let { Triple(getString(R.string.synqt_next_account_name), it, it) },
        account.accountNumber?.let { Triple(getString(R.string.synqt_next_account_number), it, it) },
        account.bankName?.let { Triple(getString(R.string.synqt_next_bank), it, null) },
        account.accountType?.let { Triple(getString(R.string.synqt_next_account_type), it, null) },
        // Branch is the primary detail and often repeats the bank code, so the bank code only stands in when there is no branch.
        account.bankBranch.shown()?.let { Triple(getString(R.string.synqt_next_branch), it, it) }
            ?: account.bankCode.shown()?.let { Triple(getString(R.string.synqt_next_bank_code), it, it) },
        Triple(getString(R.string.synqt_sheet_amount_due), amount, copyAmount),
    ).filter { it.second.shown() != null }
        .forEach { (label, value, copy) -> addView(detailRow(label, value, copy, copyColumn = true, tabular = label == getString(R.string.synqt_sheet_amount_due))) }
}

/** The value, unless it is blank or the "-" SYNQT sends for a detail it does not have. */
private fun String?.shown() = this?.takeIf { it.isNotBlank() && it != "-" }

/** What the payer must know before transferring. Draft wording, based on the Payment Web App's notice. */
private fun Context.transferNotice() = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(0, dp(16), 0, dp(8))
    addView(SynqtUI.text(context, getString(R.string.synqt_next_notice_title), 16f, weight = SynqtUI.Weight.MEDIUM).apply { setPadding(0, 0, 0, dp(8)) })
    listOf(R.string.synqt_next_notice_exact, R.string.synqt_next_notice_once, R.string.synqt_next_notice_fees, R.string.synqt_next_notice_own_account).forEachIndexed { index, item ->
        addView(SynqtUI.text(context, getString(R.string.synqt_next_notice_item, index + 1, getString(item)), 16f, R.color.synqt_text_muted).apply { setPadding(0, 0, 0, dp(8)) })
    }
}

/** A label and value; with [copy], a button that puts that text on the clipboard. [tabular] lines up an amount's digits. */
private fun Context.detailRow(label: String, value: String, copy: String? = null, copyColumn: Boolean = copy != null, tabular: Boolean = false) = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    setPadding(0, dp(8), 0, 0)
    addView(SynqtUI.text(context, label, 16f, R.color.synqt_text_muted).apply { setPadding(0, 0, dp(16), 0) }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
    addView(SynqtUI.text(context, value, 16f, weight = SynqtUI.Weight.MEDIUM).apply { gravity = Gravity.END; if (tabular) fontFeatureSettings = "tnum" })
    // In a card with copy buttons every row gets the button's height and column, so values line up and spacing stays even.
    if (copyColumn) addView(copy?.let { copyButton(label, it) } ?: View(context), LinearLayout.LayoutParams(dp(48), dp(48)))
}

private fun Context.copyButton(label: String, text: String) = ImageButton(this).apply {
    setImageDrawable(synqtIcon(R.drawable.synqt_ic_copy, R.color.synqt_text_muted, 20))
    background = null
    contentDescription = getString(R.string.synqt_next_copy, label)
    layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
    setOnClickListener {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
        // Android 13 and later confirm the copy themselves.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, getString(R.string.synqt_next_copied, label), Toast.LENGTH_SHORT).show()
    }
}

private fun download(url: String) = (URL(url).openConnection() as HttpURLConnection).run {
    connectTimeout = 15_000
    readTimeout = 30_000
    try {
        inputStream.use(BitmapFactory::decodeStream)
    } finally {
        disconnect()
    }
}

/** Time from [now] to [iso] as "01:59:17", or "" once it has passed. Null if [iso] is unreadable. */
@androidx.annotation.RequiresApi(26)
internal fun timeLeft(iso: String, now: Instant): String? {
    val at = runCatching { Instant.parse(iso) }.getOrNull() ?: return null
    val secs = java.time.Duration.between(now, at).seconds
    return if (secs <= 0) "" else String.format(Locale.UK, "%02d:%02d:%02d", secs / 3600, secs % 3600 / 60, secs % 60)
}

/** The Android app of each method that has one, by method code. Each must also be listed under <queries> in the manifest. */
internal val APP_PACKAGES = mapOf("ZAPPER_QR" to "com.zapper.android", "SNAPSCAN_QR" to "com.fireid.snapscan")

/**
 * An app's own URL scheme, tried when it does not accept SYNQT's https link. SnapScan's https path filter is a glob of
 * "/qr/" plus a star, which in Android means "/qr" then any number of "/", so it never matches a real payment link.
 */
internal val APP_SCHEMES = mapOf("SNAPSCAN_QR" to "snapscan")

/** [link], then the same link on [scheme] if it is an https link. */
internal fun appLinks(link: String, scheme: String?): List<String> =
    listOfNotNull(link, scheme?.let { appScheme -> link.takeIf { it.startsWith("https://") }?.let { appScheme + it.removePrefix("https") } })

/** Shown over the form while the payment is created. */
private fun Context.processingView() = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER
    setBackgroundColor(getColor(R.color.synqt_surface))
    setPadding(dp(32), dp(32), dp(32), dp(32))
    isClickable = true // keeps taps off the form underneath
    visibility = View.GONE
    addView(OrbitLoader(context), LinearLayout.LayoutParams(dp(72), dp(72)))
    addView(SynqtUI.text(context, getString(R.string.synqt_sheet_processing), 20f, weight = SynqtUI.Weight.SEMIBOLD).apply { gravity = Gravity.CENTER; setPadding(0, dp(24), 0, dp(8)) })
    addView(SynqtUI.text(context, getString(R.string.synqt_sheet_processing_detail), 16f, R.color.synqt_text_muted).apply { gravity = Gravity.CENTER })
}

/** The input for one field, or null when there is nothing the payer could enter (an optional bank with no list). */
private fun Context.input(formField: FormField, prefill: String?, operators: List<PaymentOperatorIncoming>, events: Events): Input? {
    val choices = when (formField.kind) {
        FieldKind.CHOICE -> formField.options.map { (value, label) -> value to getString(label) }
        FieldKind.OPERATOR -> operators.mapNotNull { op -> op.code?.let { it to (op.name ?: it) } }.ifEmpty { if (formField.required) null else return null }
        // Typed when SYNQT lists no providers.
        FieldKind.PROVIDER -> providers(operators).mapNotNull { provider -> provider.code?.let { it to (provider.name ?: it) } }.ifEmpty { null }
        else -> null
    }
    if (choices != null) {
        // A provider is only asked for when it is needed, so it never offers "No preference".
        val header = "" to getString(if (formField.required || formField.kind == FieldKind.PROVIDER) R.string.synqt_sheet_choose_one else R.string.synqt_sheet_no_preference)
        val entries = listOf(header) + choices
        val dropdown = SynqtUI.dropdown(this, getString(formField.label), entries.map { it.second }, formField.hint?.let(::getString).orEmpty())
        entries.indexOfFirst { it.first == prefill }.takeIf { it > 0 }?.let(dropdown.spinner::setSelection)
        // A spinner reports its first selection on layout; only the payer's picks count as edits.
        var picked = dropdown.spinner.selectedItemPosition
        dropdown.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != picked) picked = position.also { events.edit() }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        return Input(dropdown.view, { entries.getOrNull(dropdown.spinner.selectedItemPosition)?.first.orEmpty() }, dropdown::setError, dropdown.spinner) { dropdown.spinner.setSelection(0) }
    }
    // A required bank or provider with no list from SYNQT: let the payer type the code they were given.
    val hint = when (formField.kind) {
        FieldKind.OPERATOR -> getString(R.string.synqt_sheet_hint_bank_code)
        FieldKind.PROVIDER -> getString(R.string.synqt_sheet_hint_provider_code)
        else -> formField.hint?.let(::getString).orEmpty()
    }
    val type = when (formField.kind) {
        FieldKind.EMAIL -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        FieldKind.PHONE -> InputType.TYPE_CLASS_PHONE
        FieldKind.NUMBER -> InputType.TYPE_CLASS_NUMBER
        else -> InputType.TYPE_CLASS_TEXT or if (formField.path.endsWith("Name")) InputType.TYPE_TEXT_FLAG_CAP_WORDS else 0
    }
    val field = SynqtUI.field(this, getString(formField.label), hint, type, placeholder = formField.placeholder?.let(::getString).orEmpty())
    formField.maxLength?.let { field.input.filters = arrayOf(InputFilter.LengthFilter(it)) }
    when (formField.kind) {
        FieldKind.EMAIL -> field.input.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS)
        FieldKind.PHONE -> field.input.setAutofillHints(View.AUTOFILL_HINT_PHONE)
        else -> AUTOFILL_HINTS[formField.path]?.let { field.input.setAutofillHints(it) }
    }
    field.input.setText(prefill.orEmpty())
    // Added after the prefill, so a pre-filled value does not count as the payer's edit.
    field.input.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(text: Editable?) = events.edit()
    })
    field.input.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) events.leave() }
    return Input(field.view, { field.input.text.toString() }, field::setError, field.input) { field.input.setText("") }
}

/** A problem to show the payer: [message] is a string resource, filled in with [args] (which may be other messages). */
internal data class PayinError(val attr: String?, val message: Int, val args: List<Any> = emptyList()) {
    fun text(context: Context): String = context.getString(message, *args.map { if (it is PayinError) it.text(context) else it }.toTypedArray())
}

/** Payer-facing messages for a refused POST /payins. */
internal fun payinErrors(error: SynqtException): List<PayinError> {
    val messages = error.errors.map { err ->
        val payer = payerMessageFor(err.code)
        when {
            err.code == "SYSTEM" -> PayinError(err.field, R.string.synqt_error_system)
            err.code == "PAYIN_ALREADY_STARTED" -> PayinError(err.field, R.string.synqt_error_already_started)
            payer != null -> PayinError(err.field, R.string.synqt_error_with_code, listOf(PayinError(null, payer), err.code))
            // SYNQT's description is written for the merchant, so it is never shown to the payer.
            else -> PayinError(err.field, R.string.synqt_error_with_code, listOf(PayinError(null, if (err.field != null) R.string.synqt_sheet_check_field else R.string.synqt_failure_general), err.code))
        }
    }
    if (messages.isNotEmpty()) return messages
    val message = when (error.httpCode) {
        401 -> PayinError(null, R.string.synqt_error_session_expired)
        403 -> PayinError(null, R.string.synqt_error_not_allowed)
        else -> PayinError(null, R.string.synqt_error_http, listOf(error.httpCode))
    }
    return listOf(message)
}

/** Where the payer was in a sheet that is rebuilt, for a light or dark switch: what they entered, picked and reached. */
internal class SheetState(
    val values: Map<String, String>,
    val chosen: List<Choice>,
    val filledIn: Set<String>,
    val accepted: PayinAcceptedResponse?,
    val openedApp: Boolean,
    /** An add-on method's own state, from [com.synqt.sdk.addon.MethodSection.state]. */
    val addon: Any? = null,
)

/** What the sheet's order summary shows. [merchant] is null when the token cannot read GET /merchant-info. */
internal data class PaymentSummary(val amount: String, val merchant: String?, val method: String, val reference: String)

/**
 * Rand as R1,250.00 (the synqt-output-format convention); other currencies as "USD 1,250.00". Keeps extra
 * decimals the amount really has, so nothing is rounded away.
 */
internal fun formatMoney(amount: BigDecimal, currencyCode: String): String {
    val digits = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).apply {
        maximumFractionDigits = maxOf(2, amount.stripTrailingZeros().scale())
    }.format(amount)
    return if (currencyCode == "ZAR") "R$digits" else "$currencyCode $digits"
}
