package com.synqt.sdk

import com.synqt.sdk.internal.sheet.PayinError
import java.io.File
import java.util.Locale

// Local unit tests run without Android resources, so the English text is read from the SDK's values/*.xml.
// That keeps the tests checking real wording, and fails if a string the code uses is missing.

private val strings: Map<String, String> by lazy {
    val entry = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    File("src/main/res/values").listFiles { file -> file.name.endsWith(".xml") }.orEmpty().flatMap { file ->
        entry.findAll(file.readText()).map { match -> match.groupValues[1] to unescape(match.groupValues[2]) }.toList()
    }.toMap()
}

private fun unescape(text: String) = text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
    .replace(Regex("""\\(.)""")) { it.groupValues[1] }

/** The English text of string resource [id], filled in with [args] as Android would. */
internal fun en(id: Int, vararg args: Any): String {
    val name = R.string::class.java.fields.first { it.getInt(null) == id }.name
    val text = strings[name] ?: error("No English text for R.string.$name")
    return if (args.isEmpty()) text else String.format(Locale.UK, text, *args)
}

/** A sheet error as the field it marks and its English text. */
internal fun PayinError.en(): Pair<String?, String> = attr to en(message, *args.map { if (it is PayinError) it.en().second else it }.toTypedArray())
