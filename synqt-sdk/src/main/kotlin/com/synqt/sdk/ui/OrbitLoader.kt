package com.synqt.sdk.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.view.View
import android.view.animation.LinearInterpolator
import com.synqt.sdk.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The Synqt Orbit loader, drawn natively from the designer's Lottie file (synqt-loaders/lottie/orbit-*.json): a hub
 * in the text colour, with a magenta and an aqua dot that each turn once, glide into the hub and back out. Aqua
 * follows magenta 7.2 frames behind. Decorative, so screen readers skip it; with system animations off it holds still.
 */
internal class OrbitLoader(context: Context) : View(context) {
    private val hub = paint(R.color.synqt_text)
    private val magenta = paint(R.color.synqt_magenta)
    private val aqua = paint(R.color.synqt_aqua)
    private var frame = 0f
    private val animator = ValueAnimator.ofFloat(0f, LOOP).apply {
        duration = 2900 // 174 frames at 60fps
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { frame = it.animatedValue as Float; invalidate() }
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun paint(colorRes: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(colorRes) }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val unit = min(width, height) / 100f // the Lottie canvas is 100 by 100
        val cx = width / 2f
        val cy = height / 2f
        val radius = 11f * unit
        canvas.drawCircle(cx, cy, radius, hub)
        // Later dots are drawn on top, as in the Lottie file: hub, then aqua, then magenta.
        for ((offset, paint) in listOf(DELAY to aqua, 0f to magenta)) {
            val (angle, distance) = orbitDot(frame - offset)
            val rad = Math.toRadians(angle.toDouble())
            canvas.drawCircle(cx + distance * unit * sin(rad).toFloat(), cy - distance * unit * cos(rad).toFloat(), radius, paint)
        }
    }

    internal companion object {
        const val LOOP = 174f
        const val DELAY = 7.2f
    }
}

/**
 * Where an Orbit dot is at [frame] (60fps, looping every 174): degrees clockwise from the top, and distance from the
 * centre on the 100-unit canvas. Keyframes from the Lottie file: turn 0 to 96.05, glide in to 120.06, wait, glide out
 * from 149.99 to 174.
 */
internal fun orbitDot(frame: Float): Pair<Float, Float> {
    val time = ((frame % OrbitLoader.LOOP) + OrbitLoader.LOOP) % OrbitLoader.LOOP
    val angle = if (time < 96.05f) 360f * ease(time / 96.05f) else 360f
    val distance = when {
        time < 96.05f -> 40f
        time < 120.06f -> 40f * (1 - ease((time - 96.05f) / 24.01f))
        time < 149.99f -> 0f
        else -> 40f * ease((time - 149.99f) / 24.01f)
    }
    return angle to distance
}

/** The file's one easing curve, cubic-bezier(0.65, 0, 0.35, 1), solved for x by bisection. */
internal fun ease(progress: Float): Float {
    fun bezier(fraction: Float, first: Float, second: Float) = 3 * (1 - fraction) * (1 - fraction) * fraction * first + 3 * (1 - fraction) * fraction * fraction * second + fraction * fraction * fraction
    var lo = 0f
    var hi = 1f
    repeat(24) { val mid = (lo + hi) / 2; if (bezier(mid, 0.65f, 0.35f) < progress) lo = mid else hi = mid }
    return bezier((lo + hi) / 2, 0f, 1f)
}
