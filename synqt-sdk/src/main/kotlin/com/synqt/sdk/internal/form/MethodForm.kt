package com.synqt.sdk.internal.form

import com.synqt.sdk.ExternalProvider
import com.synqt.sdk.PaymentMethod
import com.synqt.sdk.PaymentOperator
import com.synqt.sdk.R
import com.synqt.sdk.internal.sheet.PayinError

/**
 * How the payment sheet asks for a field. OPERATOR lists the banks or providers availablePaymentOptions
 * returns for the method;
 * PROVIDER lists the external providers of those banks.
 */
internal enum class FieldKind { TEXT, EMAIL, PHONE, NUMBER, CHOICE, OPERATOR, PROVIDER }

/** Every provider of [operators], once each, whichever bank the payer picks. */
internal fun providers(operators: List<PaymentOperator>): List<ExternalProvider> =
    operators.flatMap { it.externalProviders }.distinctBy { it.code }

/**
 * One payer-facing input for a text field of a payment method, keyed by its JSON path (e.g. `customerName.lastName`).
 * Text is held as string resources (res/values/synqt_form_strings.xml) so it can be translated.
 */
internal class FormField(
    val path: String,
    val label: Int,
    val kind: FieldKind,
    val required: Boolean,
    val hint: Int? = null,
    /** An example of what to type, shown inside the empty field instead of [hint] under it. */
    val placeholder: Int? = null,
    /** Heading of the group this field sits in, such as "Bank account". */
    val section: Int? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    /** From the spec; matched anywhere in the value, as JSON Schema does. */
    val pattern: String? = null,
    val invalid: Int? = null,
    /** CHOICE values sent to SYNQT, with the label shown for each. */
    val options: List<Pair<String, Int>> = emptyList(),
)

/** One answer to a [OneOf] question. Once it is chosen its [required] paths must be filled in (none, for an optional question); the rest are optional. */
internal class Choice(val label: Int, val paths: List<String>, val required: List<String> = paths.take(1))

/** The payer gives one of several details. The first choice is the default; the other choices' fields are hidden and not sent. */
internal class OneOf(val question: Int, val choices: List<Choice>)

/** A direct API method's inputs and how they become the typed [PaymentMethod]. Instances are generated into MethodFormData.kt. */
internal class MethodForm(val code: String, val name: Int, val fields: List<FormField>, val oneOfs: List<OneOf> = emptyList(), val build: (FormValues) -> PaymentMethod)

/** The first choice of each question, which the sheet starts with. */
internal val MethodForm.defaultChoices: List<Choice> get() = oneOfs.map { it.choices.first() }

/**
 * Whether the field at [path] is shown, checked and sent while [chosen] (one per question, in order) is picked. A
 * question with nothing chosen, as when [chosen] is empty, shows all its fields.
 */
internal fun MethodForm.shows(path: String, chosen: List<Choice>): Boolean =
    oneOfs.zip(chosen).all { (question, choice) -> path in choice.paths || question.choices.none { path in it.paths } }

/** Payer-facing problems with [values], by field path, found before anything is sent. */
internal fun MethodForm.problems(values: Map<String, String>, chosen: List<Choice> = defaultChoices): List<PayinError> =
    fields.filter { shows(it.path, chosen) }.mapNotNull { field -> problem(field, values, mustFill = chosen.any { field.path in it.required }) }

/**
 * Fields the sheet can leave out of view: those the app prefilled whose value is still in place and passes its checks.
 * Once the first and last name are filled in, the name's optional extras (title, middle name, suffix) are left out
 * too, so the whole name section goes. [values] are the form's current values, as read from the sheet.
 */
internal fun MethodForm.filledIn(values: Map<String, String>, prefill: Map<String, String>, chosen: List<Choice>): List<String> {
    val filled = fields.filter { field ->
        val value = values[field.path].orEmpty()
        value.isNotBlank() && value == prefill[field.path] && problem(field, values, mustFill = chosen.any { field.path in it.required }) == null
    }.map { it.path }
    val named = listOf("customerName.firstName", "customerName.lastName").all { it in filled }
    val extras = if (!named) emptyList() else fields.filter { field ->
        field.path.startsWith("customerName.") && field.path !in filled && !field.required && values[field.path].isNullOrBlank()
    }.map { it.path }
    return filled + extras
}

/**
 * Problems with the prefilled values still in view (not in [filledIn]), so the sheet shows them from the start. A value
 * the field could not take as given (a dropdown with no such option, or one cut to the field's length) is flagged too.
 */
internal fun MethodForm.prefillProblems(values: Map<String, String>, prefill: Map<String, String>, filledIn: Collection<String>, chosen: List<Choice>): List<PayinError> =
    fields.filter { field -> field.path in values && field.path !in filledIn && !prefill[field.path].isNullOrBlank() && shows(field.path, chosen) }.mapNotNull { field ->
        // Not the field's own invalid text, which is about typing.
        if (values[field.path] != prefill[field.path]) PayinError(field.path, R.string.synqt_sheet_check_field)
        else problem(field, values, mustFill = chosen.any { field.path in it.required })
    }

/**
 * True when the payer has nothing left to give: the method has mandatory fields, every one shown for [chosen] is in
 * [filledIn], and nothing fails its checks. Methods with only optional fields keep their form for the payer.
 */
internal fun MethodForm.readyToPay(values: Map<String, String>, filledIn: Collection<String>, chosen: List<Choice>): Boolean {
    val mandatory = fields.filter { field -> shows(field.path, chosen) && (field.required || chosen.any { field.path in it.required }) }
    return mandatory.isNotEmpty() && mandatory.all { it.path in filledIn } && problems(values, chosen).isEmpty()
}

/**
 * The problem with one field, if any. [mustFill] is for the required fields of the chosen [Choice], which the spec itself
 * marks optional.
 */
internal fun problem(field: FormField, values: Map<String, String>, mustFill: Boolean = false): PayinError? {
    val value = values[field.path]?.trim().orEmpty()
    val malformed = value.isNotEmpty() && (
        field.minLength != null && value.length < field.minLength ||
            field.maxLength != null && value.length > field.maxLength ||
            field.pattern != null && !Regex(field.pattern).containsMatchIn(value) ||
            field.kind == FieldKind.EMAIL && !EMAIL.matches(value)
        )
    return when {
        value.isEmpty() && (mustFill || field.required) -> PayinError(field.path, R.string.synqt_sheet_required)
        malformed -> PayinError(field.path, field.invalid ?: R.string.synqt_sheet_check_field)
        else -> null
    }
}

private val EMAIL = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

/** Form input by JSON path. Blank means not set, so optional fields are left out of the request. */
internal class FormValues(private val values: Map<String, String>) {
    fun str(path: String): String? = values[path]?.trim()?.takeIf { it.isNotEmpty() }

    fun <T : Any> req(path: String, value: T?): T = value ?: throw FieldException(path)

    /** Builds an optional nested object only when at least one of its fields is filled in. */
    fun <T> group(prefix: String, build: () -> T): T? =
        if (values.any { (key, value) -> key.startsWith("$prefix.") && value.isNotBlank() }) build() else null
}

/** A required form value that is missing; [path] says which input to mark. */
internal class FieldException(val path: String) : IllegalArgumentException("$path is required")
