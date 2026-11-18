package com.synqt.sdk.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OrbitLoaderTest {

    @Test
    fun easingMatchesTheCurveEnds() {
        assertEquals(0f, ease(0f), 0.001f)
        assertEquals(0.5f, ease(0.5f), 0.001f)
        assertEquals(1f, ease(1f), 0.001f)
    }

    @Test
    fun dotsFollowTheLottieKeyframes() {
        assertEquals(0f, orbitDot(0f).first, 0.001f) // magenta starts at the top
        assertEquals(40f, orbitDot(0f).second, 0.001f)
        assertEquals(360f, orbitDot(96.05f).first, 0.01f) // one full turn
        assertEquals(0f, orbitDot(130f).second, 0.001f) // resting in the hub
        assertEquals(40f, orbitDot(174f).second, 0.001f) // back out, ready to loop
        // Aqua runs 7.2 frames behind; the file starts it at -35.51 on the y axis.
        assertEquals(35.51f, orbitDot(-OrbitLoader.DELAY).second, 0.05f)
    }
}
