package com.synqt.sdk.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import com.synqt.sdk.R

// Helpers shared by SynqtUI and the payment sheet. Colours come from res/values/synqt_colors.xml (dark values in
// values-night), following the synqt-brand-design skill and its mobile type scale.

internal fun Context.dp(value: Int) = (value * resources.displayMetrics.density).toInt()

/** Instrument Sans from the SDK's res/font. XML font families need API 26, so older devices keep the system face. */
internal fun Context.synqtTypeface(weight: SynqtUI.Weight): Typeface? =
    if (Build.VERSION.SDK_INT >= 26) resources.getFont(
        when (weight) {
            SynqtUI.Weight.REGULAR -> R.font.synqt_instrument_sans_regular
            SynqtUI.Weight.MEDIUM -> R.font.synqt_instrument_sans_medium
            SynqtUI.Weight.SEMIBOLD -> R.font.synqt_instrument_sans_semibold
        },
    ) else null

/** One of the SDK's line icons, tinted and sized in dp. */
internal fun Context.synqtIcon(res: Int, colorRes: Int, sizeDp: Int = 24): Drawable = getDrawable(res)!!.mutate().apply {
    setTint(getColor(colorRes))
    setBounds(0, 0, dp(sizeDp), dp(sizeDp))
}

internal fun Context.shape(fill: Int, radius: Int, stroke: Int = 0, strokeColor: Int = 0) = GradientDrawable().apply {
    setColor(fill)
    cornerRadius = dp(radius).toFloat()
    if (stroke > 0) setStroke(dp(stroke), strokeColor)
}

/** Focused gets a 2dp ring in the focus-ring token (Off-Blue Black on light, aqua on dark). */
internal fun focusable(rest: Drawable, focused: Drawable) = StateListDrawable().apply {
    addState(intArrayOf(android.R.attr.state_focused), focused)
    addState(intArrayOf(), rest)
}

private fun Context.inputBackground(borderRes: Int) = focusable(
    shape(getColor(R.color.synqt_surface), 12, 1, getColor(borderRes)),
    shape(getColor(R.color.synqt_surface), 12, 2, getColor(R.color.synqt_focus_ring)),
)

/** A custom background drops the platform arrow, so the chevron is layered on. */
private fun Context.dropdownBackground(borderRes: Int): Drawable {
    fun withChevron(box: Drawable) = LayerDrawable(arrayOf(box, getDrawable(R.drawable.synqt_ic_chevron_down)!!.mutate().apply {
        setTint(getColor(R.color.synqt_text))
    })).apply {
        setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL)
        setLayerInsetEnd(1, dp(16))
        setLayerSize(1, dp(24), dp(24))
    }
    return focusable(
        withChevron(shape(getColor(R.color.synqt_surface), 12, 1, getColor(borderRes))),
        withChevron(shape(getColor(R.color.synqt_surface), 12, 2, getColor(R.color.synqt_focus_ring))),
    )
}

/** Chosen: action-tint with a 2dp action border (brand magenta is 2.96:1 on the light tint). Keyboard focus swaps in the focus ring. */
private fun Context.tileBackground(): Drawable {
    fun tile(fill: Int, border: Int, width: Int) = shape(getColor(fill), 12, width, getColor(border))
    val states = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused, android.R.attr.state_checked), tile(R.color.synqt_action_tint, R.color.synqt_focus_ring, 2))
        addState(intArrayOf(android.R.attr.state_focused), tile(R.color.synqt_surface, R.color.synqt_focus_ring, 2))
        addState(intArrayOf(android.R.attr.state_checked), tile(R.color.synqt_action_tint, R.color.synqt_action, 2))
        addState(intArrayOf(), tile(R.color.synqt_surface, R.color.synqt_border_control, 1))
    }
    return RippleDrawable(ColorStateList.valueOf(getColor(R.color.synqt_pressed_overlay)), states, shape(Color.WHITE, 12))
}

/**
 * One of several answers, as tiles. It is a real radio group, so TalkBack reads "selected, 1 of 3", drawn as the
 * brand's method picker: the chosen tile on action-tint with its border and radio dot in the action colour.
 */
internal fun Context.choiceTiles(question: String, labels: List<String>, selected: Int, onSelect: (Int) -> Unit): RadioGroup =
    RadioGroup(this).apply {
        orientation = RadioGroup.VERTICAL
        contentDescription = question
        labels.forEachIndexed { index, label ->
            addView(RadioButton(context).apply {
                id = View.generateViewId()
                text = label
                textSize = 16f
                setTextColor(getColor(R.color.synqt_text))
                synqtTypeface(SynqtUI.Weight.MEDIUM)?.let { typeface = it }
                minHeight = dp(56)
                // The platform draws the dot against the tile's edge, so it is inset towards the 16dp margin.
                buttonDrawable = InsetDrawable(buttonDrawable, dp(12), 0, 0, 0)
                buttonTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(getColor(R.color.synqt_action), getColor(R.color.synqt_text_muted)),
                )
                setPadding(dp(8), dp(12), dp(16), dp(12))
                background = tileBackground()
                isChecked = index == selected
            }, RadioGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(8) })
        }
        setOnCheckedChangeListener { group, id -> onSelect(group.indexOfChild(group.findViewById(id))) }
    }

/**
 * Synqt-styled views: the building blocks of the SDK's payment sheet, for apps that want their own screens to match.
 * Mobile type scale in Instrument Sans, spacing in 4, 8, 16 and 24dp, 12dp cards and inputs, 48dp pill buttons, a visible
 * focus ring, light and dark.
 */
public object SynqtUI {
    public enum class Weight { REGULAR, MEDIUM, SEMIBOLD }

    public fun text(context: Context, value: String, sizeSp: Float = 16f, colorRes: Int = R.color.synqt_text, weight: Weight = Weight.REGULAR): TextView =
        TextView(context).apply {
            text = value
            textSize = sizeSp
            setTextColor(context.getColor(colorRes))
            context.synqtTypeface(weight)?.let { typeface = it }
        }

    /** A panel with a title in the sheet's heading style (20sp SemiBold). */
    public fun card(context: Context, title: String, vararg children: View): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = context.shape(context.getColor(R.color.synqt_surface), 12, 1, context.getColor(R.color.synqt_border))
        setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(16))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = context.dp(16) }
        addView(text(context, title, 20f, weight = Weight.SEMIBOLD).apply { setPadding(0, 0, 0, context.dp(8)) })
        children.forEach(::addView)
    }

    /** Label above, input, helper below. [setError] swaps the helper for an error. */
    public class Field internal constructor(public val view: LinearLayout, public val input: EditText, private val helper: TextView, private val helperText: String) {
        private val error = input.readsError(helper)

        public fun setError(message: String?) {
            error.show(message)
            val ctx = view.context
            helper.text = message ?: helperText
            helper.visibility = if (helper.text.isNullOrEmpty()) View.GONE else View.VISIBLE
            helper.setTextColor(ctx.getColor(if (message != null) R.color.synqt_danger else R.color.synqt_text_muted))
            input.background = ctx.inputBackground(if (message != null) R.color.synqt_danger else R.color.synqt_border_control)
        }
    }

    public fun field(context: Context, label: String, helper: String = "", type: Int = InputType.TYPE_CLASS_TEXT, multiLine: Boolean = false, placeholder: String = ""): Field {
        val input = EditText(context).apply {
            id = View.generateViewId()
            inputType = type or if (multiLine) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
            // Done closes the keyboard so the payer can review; it never moves on or pays.
            if (multiLine) minLines = 2 else { setSingleLine(); imeOptions = EditorInfo.IME_ACTION_DONE }
            textSize = 16f
            setTextColor(context.getColor(R.color.synqt_text))
            context.synqtTypeface(Weight.REGULAR)?.let { typeface = it }
            gravity = Gravity.CENTER_VERTICAL
            minHeight = context.dp(52)
            setPadding(context.dp(16), context.dp(8), context.dp(16), context.dp(8))
            background = context.inputBackground(R.color.synqt_border_control)
            // Named by the label's labelFor: a contentDescription would make TalkBack read it instead of what was typed.
            hint = placeholder
            setHintTextColor(context.getColor(R.color.synqt_placeholder))
        }
        val helperView = helperText(context, helper)
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = context.dp(16) }
            addView(label(context, label).apply { labelFor = input.id })
            addView(input)
            addView(helperView)
        }
        return Field(view, input, helperView, helper)
    }

    /** Labelled dropdown in the input's style. [setError] works as on [Field]. */
    public class Dropdown internal constructor(public val view: LinearLayout, public val spinner: Spinner, private val helper: TextView, private val helperText: String) {
        private val error = spinner.readsError(helper)

        public fun setError(message: String?) {
            error.show(message)
            val ctx = view.context
            helper.text = message ?: helperText
            helper.visibility = if (helper.text.isNullOrEmpty()) View.GONE else View.VISIBLE
            helper.setTextColor(ctx.getColor(if (message != null) R.color.synqt_danger else R.color.synqt_text_muted))
            spinner.background = ctx.dropdownBackground(if (message != null) R.color.synqt_danger else R.color.synqt_border_control)
        }
    }

    public fun dropdown(context: Context, label: String, items: List<String>, helper: String = ""): Dropdown {
        val spinner = Spinner(context).apply {
            id = View.generateViewId()
            // Rows set their own colour and face: the platform row keeps the light theme's text colour, unreadable on dark.
            adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, items) {
                private fun style(view: View) = (view as TextView).apply {
                    textSize = 16f
                    setTextColor(context.getColor(R.color.synqt_text))
                    context.synqtTypeface(Weight.REGULAR)?.let { typeface = it }
                }
                override fun getView(position: Int, convertView: View?, parent: ViewGroup) = style(super.getView(position, convertView, parent))
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) = style(super.getDropDownView(position, convertView, parent))
            }
            background = context.dropdownBackground(R.color.synqt_border_control)
            minimumHeight = context.dp(52)
            setPadding(0, 0, context.dp(48), 0) // keep the selected text clear of the chevron
            setPopupBackgroundDrawable(context.shape(context.getColor(R.color.synqt_surface), 12, 1, context.getColor(R.color.synqt_border)))
            // Named by the label's labelFor, so TalkBack still reads the selected option.
        }
        val helperView = helperText(context, helper)
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = context.dp(16) }
            addView(label(context, label).apply { labelFor = spinner.id })
            addView(spinner, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(helperView)
        }
        return Dropdown(view, spinner, helperView, helper)
    }

    /** PRIMARY is the action colour (one per screen), ACCENT is brand magenta for dark sections, OUTLINE is secondary. */
    public enum class ButtonStyle { PRIMARY, ACCENT, OUTLINE }

    /** A 48dp pill. The view is 56dp tall so the focus ring can sit 2dp outside the pill, off its fill. */
    public fun button(context: Context, label: String, style: ButtonStyle): Button = Button(context).apply {
        text = label
        isAllCaps = false
        textSize = 16f
        context.synqtTypeface(Weight.MEDIUM)?.let { typeface = it }
        stateListAnimator = null
        minHeight = context.dp(56)
        minimumHeight = context.dp(56)
        setPadding(context.dp(24), 0, context.dp(24), 0)
        val (fill, textColor) = when (style) {
            ButtonStyle.PRIMARY -> R.color.synqt_action to R.color.synqt_on_action
            // White on brand magenta is 3.29:1, so the label is Off-Blue Black (5.14:1).
            ButtonStyle.ACCENT -> R.color.synqt_magenta to R.color.synqt_on_accent
            ButtonStyle.OUTLINE -> R.color.synqt_surface to R.color.synqt_text
        }
        setTextColor(context.getColor(textColor))
        val gap = context.dp(4)
        fun pill() = InsetDrawable(context.shape(context.getColor(fill), 999, if (style == ButtonStyle.OUTLINE) 1 else 0, context.getColor(R.color.synqt_border_control)), gap)
        val ring = context.shape(Color.TRANSPARENT, 999, 2, context.getColor(R.color.synqt_focus_ring))
        background = RippleDrawable(
            ColorStateList.valueOf(context.getColor(R.color.synqt_pressed_overlay)),
            focusable(pill(), LayerDrawable(arrayOf(ring, pill()))),
            pill(),
        )
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(8) }
    }

    /** Error banner: the words lead, with a warning icon, so colour is never the only cue. */
    public fun errorBanner(context: Context): TextView = text(context, "", 16f, R.color.synqt_danger).apply {
        background = context.shape(context.getColor(R.color.synqt_danger_tint), 12, 1, context.getColor(R.color.synqt_danger_line))
        setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(16))
        setCompoundDrawablesRelativeWithIntrinsicBounds(context.synqtIcon(R.drawable.synqt_ic_warning, R.color.synqt_danger), null, null, null)
        compoundDrawablePadding = context.dp(8)
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE // read out when it appears, like a field's error
        visibility = View.GONE
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = context.dp(16) }
    }

    private fun label(context: Context, value: String) = text(context, value, 16f, weight = Weight.MEDIUM).apply { setPadding(0, 0, 0, context.dp(8)) }

    private fun helperText(context: Context, value: String) = text(context, value, 13f, R.color.synqt_text_muted).apply {
        setPadding(0, context.dp(4), 0, 0)
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE // screen readers announce an error when it appears
        if (value.isEmpty()) visibility = View.GONE
    }
}

/**
 * TalkBack reads a field's error on the field itself, as Android's own error state, so the payer hears it on landing
 * there. While it shows the error, the line under the field is left out, so the message isn't read twice.
 */
private class FieldError(private val helper: TextView) : View.AccessibilityDelegate() {
    private var message: String? = null

    fun show(message: String?) {
        this.message = message
        helper.importantForAccessibility = if (message != null) View.IMPORTANT_FOR_ACCESSIBILITY_NO else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
    }

    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        info.isContentInvalid = message != null
        info.error = message
    }
}

private fun View.readsError(helper: TextView) = FieldError(helper).also { accessibilityDelegate = it }
