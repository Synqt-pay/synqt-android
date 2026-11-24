package com.synqt.sdk

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The sheet's bank list: SYNQT's server errors and failed connections are tried twice more, a refusal is not. */
class BankListRetryTest {
    private var requests = 0

    /** Answers each try with the next status in [statuses] (the last one repeats), as Synqt.call would: 200 loads, others throw. */
    private fun load(vararg statuses: Int): List<String>? = runBlocking {
        retrying(delayMs = 1) {
            val status = statuses[minOf(requests++, statuses.size - 1)]
            when (status) {
                200 -> listOf("ABSA")
                0 -> throw IOException("no connection")
                else -> throw SynqtException(status, emptyList())
            }
        }
    }

    @Test
    fun serverErrorsAreRetriedTwiceThenTheListLoads() {
        assertEquals(listOf("ABSA"), load(503, 500, 200))
        assertEquals(3, requests)
    }

    @Test
    fun threeFailuresMeanTheListCouldNotBeLoaded() {
        assertNull(load(503))
        assertEquals(3, requests)
    }

    @Test
    fun aFailedConnectionIsRetried() {
        assertEquals(listOf("ABSA"), load(0, 200))
        assertEquals(2, requests)
    }

    @Test
    fun aRefusalIsNotRetried() {
        assertNull(load(403))
        assertEquals(1, requests)
    }
}
