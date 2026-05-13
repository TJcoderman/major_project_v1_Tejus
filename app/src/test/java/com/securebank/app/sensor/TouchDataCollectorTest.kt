package com.securebank.app.sensor

import androidx.compose.ui.geometry.Offset
import com.securebank.app.data.model.TouchData
import com.securebank.app.data.model.TouchType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TouchDataCollector.
 * Validates pressure/size sampling, touch classification, and the
 * averaging logic introduced with the multi-sample enrollment rework.
 *
 * Uses runBlocking instead of runTest because TouchDataCollector uses
 * System.currentTimeMillis() internally and needs real time to pass
 * for touch classification (TAP vs LONG_PRESS thresholds).
 */
class TouchDataCollectorTest {

    private lateinit var collector: TouchDataCollector

    @Before
    fun setUp() {
        collector = TouchDataCollector()
    }

    // ── Collection lifecycle ──

    @Test
    fun `startCollection enables data capture`() = runBlocking {
        collector.startCollection("session1")

        val emitted = withTimeoutOrNull(2000) {
            async {
                collector.touchEvents.first()
            }.also {
                collector.onTouchStart(Offset(10f, 10f), 0.5f, 1f)
                delay(50)
                collector.onTouchEnd(Offset(10f, 10f), 0.5f, 1f)
            }.await()
        }

        assertNotNull("Should emit a touch event", emitted)
        assertEquals("session1", emitted!!.sessionId)
    }

    @Test
    fun `stopCollection prevents further events`() = runBlocking {
        collector.startCollection("s1")
        collector.stopCollection()

        val emitted = withTimeoutOrNull(200) {
            async {
                collector.touchEvents.firstOrNull()
            }.also {
                collector.onTouchStart(Offset.Zero, 0.5f, 1f)
                collector.onTouchEnd(Offset.Zero, 0.5f, 1f)
            }.await()
        }

        // The flow never emits, so withTimeoutOrNull returns null
        assertNull("Events should not be emitted after stop", emitted)
    }

    // ── Pressure / size sampling ──

    @Test
    fun `pressure is averaged across move samples`() = runBlocking {
        collector.startCollection("avg-pressure")

        val emitted = withTimeoutOrNull(2000) {
            async {
                collector.touchEvents.first()
            }.also {
                delay(10)
                collector.onTouchStart(Offset(0f, 0f), 0.2f, 1f)
                collector.onTouchMove(Offset(1f, 0f), 0.4f, 1f)
                collector.onTouchMove(Offset(2f, 0f), 0.6f, 1f)
                collector.onTouchMove(Offset(3f, 0f), 0.8f, 1f)
                delay(30)
                collector.onTouchEnd(Offset(4f, 0f), 1.0f, 1f)
            }.await()
        }

        assertNotNull(emitted)
        // Average of samples {0.2, 0.4, 0.6, 0.8, 1.0} = 0.6
        val pressure = emitted!!.pressure
        assertTrue(
            "Pressure should be the average of samples (~0.6), got $pressure",
            pressure in 0.55f..0.65f
        )
    }

    @Test
    fun `size is averaged across move samples`() = runBlocking {
        collector.startCollection("avg-size")

        val emitted = withTimeoutOrNull(2000) {
            async {
                collector.touchEvents.first()
            }.also {
                delay(10)
                collector.onTouchStart(Offset(0f, 0f), 0.5f, 2f)
                collector.onTouchMove(Offset(1f, 0f), 0.5f, 4f)
                collector.onTouchMove(Offset(2f, 0f), 0.5f, 6f)
                delay(30)
                collector.onTouchEnd(Offset(3f, 0f), 0.5f, 8f)
            }.await()
        }

        assertNotNull(emitted)
        // Average of {2, 4, 6, 8} = 5.0
        val size = emitted!!.touchSize
        assertTrue(
            "Touch size should average to ~5.0, got $size",
            size in 4.5f..5.5f
        )
    }

    @Test
    fun `pressure clamped to 0-1 range in samples`() = runBlocking {
        collector.startCollection("clamp")

        val emitted = withTimeoutOrNull(2000) {
            async {
                collector.touchEvents.first()
            }.also {
                delay(10)
                collector.onTouchStart(Offset.Zero, 2.5f, 1f)  // over 1.0
                delay(30)
                collector.onTouchEnd(Offset.Zero, -0.5f, 1f)   // negative
            }.await()
        }

        assertNotNull(emitted)
        val p = emitted!!.pressure
        assertTrue("Pressure must be in [0,1], got $p", p in 0f..1f)
    }

    // ── Touch classification ──

    @Test
    fun `short stationary touch classified as TAP`() = runBlocking {
        collector.startCollection("tap-test")

        val emitted = withTimeoutOrNull(2000) {
            async {
                collector.touchEvents.first()
            }.also {
                delay(10)
                val pos = Offset(100f, 100f)
                collector.onTouchStart(pos, 0.5f, 1f)
                delay(50) // well under 200ms TAP_MAX_DURATION
                collector.onTouchEnd(pos, 0.5f, 1f)
            }.await()
        }

        assertNotNull(emitted)
        assertEquals(TouchType.TAP, emitted!!.touchType)
    }

    @Test
    fun `long stationary touch classified as LONG_PRESS`() = runBlocking {
        collector.startCollection("lp-test")

        val emitted = withTimeoutOrNull(3000) {
            async {
                collector.touchEvents.first()
            }.also {
                delay(10)
                val pos = Offset(100f, 100f)
                collector.onTouchStart(pos, 0.5f, 1f)
                delay(600) // above 500ms LONG_PRESS_MIN_DURATION
                collector.onTouchEnd(Offset(102f, 102f), 0.5f, 1f) // tiny drift
            }.await()
        }

        assertNotNull(emitted)
        assertEquals(TouchType.LONG_PRESS, emitted!!.touchType)
    }

    @Test
    fun `fast horizontal drag classified as SWIPE_RIGHT`() = runBlocking {
        collector.startCollection("swipe-right")

        val emitted = withTimeoutOrNull(3000) {
            async {
                collector.touchEvents.first()
            }.also {
                delay(10)
                val start = Offset(50f, 200f)
                collector.onTouchStart(start, 0.5f, 1f)
                for (i in 1..10) {
                    collector.onTouchMove(Offset(50f + i * 30f, 200f), 0.5f, 1f)
                    delay(10)
                }
                collector.onTouchEnd(Offset(350f, 200f), 0.5f, 1f)
            }.await()
        }

        assertNotNull(emitted)
        assertEquals(TouchType.SWIPE_RIGHT, emitted!!.touchType)
    }

    @Test
    fun `fast upward drag classified as SWIPE_UP`() = runBlocking {
        collector.startCollection("swipe-up")

        val emitted = withTimeoutOrNull(3000) {
            async {
                collector.touchEvents.first()
            }.also {
                delay(10)
                val start = Offset(200f, 500f)
                collector.onTouchStart(start, 0.5f, 1f)
                for (i in 1..10) {
                    collector.onTouchMove(Offset(200f, 500f - i * 30f), 0.5f, 1f)
                    delay(10)
                }
                collector.onTouchEnd(Offset(200f, 200f), 0.5f, 1f)
            }.await()
        }

        assertNotNull(emitted)
        assertEquals(TouchType.SWIPE_UP, emitted!!.touchType)
    }

    // ── Hold duration tracking ──

    @Test
    fun `hold duration accumulated for stationary touch`() = runBlocking {
        collector.startCollection("hold-dur")

        val emitted = withTimeoutOrNull(3000) {
            async {
                collector.touchEvents.first()
            }.also {
                delay(10)
                val pos = Offset(100f, 100f)
                collector.onTouchStart(pos, 0.5f, 1f)
                // Stay roughly stationary for ~300ms
                for (i in 1..6) {
                    collector.onTouchMove(Offset(100f + (i % 2).toFloat(), 100f), 0.5f, 1f)
                    delay(50)
                }
                collector.onTouchEnd(Offset(101f, 100f), 0.5f, 1f)
            }.await()
        }

        assertNotNull(emitted)
        assertTrue(
            "Hold duration should be > 0 for stationary contact, got ${emitted!!.holdDuration}",
            emitted.holdDuration > 0
        )
    }

    // ── onTouchMove default parameters ──

    @Test
    fun `onTouchMove uses last pressure and size when not specified`() = runBlocking {
        collector.startCollection("defaults")

        val emitted = withTimeoutOrNull(2000) {
            async {
                collector.touchEvents.first()
            }.also {
                delay(10)
                collector.onTouchStart(Offset(0f, 0f), 0.7f, 3f)
                // Move without specifying pressure/size → should fall back to 0.7 / 3.0
                collector.onTouchMove(Offset(1f, 0f))
                collector.onTouchMove(Offset(2f, 0f))
                delay(30)
                collector.onTouchEnd(Offset(3f, 0f), 0.7f, 3f)
            }.await()
        }

        assertNotNull(emitted)
        // All samples are 0.7 for pressure → average should be ~0.7
        assertTrue(
            "Pressure should stay ~0.7 when defaults used, got ${emitted!!.pressure}",
            emitted.pressure in 0.65f..0.75f
        )
    }

    // ── Multiple sequential touches ──

    @Test
    fun `collector emits independent events for sequential touches`() = runBlocking {
        collector.startCollection("sequential")

        val events = mutableListOf<TouchData>()

        val job = launch {
            collector.touchEvents.collect { events.add(it) }
        }

        delay(10)

        // Touch 1
        collector.onTouchStart(Offset(10f, 10f), 0.3f, 1f)
        delay(50)
        collector.onTouchEnd(Offset(10f, 10f), 0.3f, 1f)

        delay(50)

        // Touch 2
        collector.onTouchStart(Offset(200f, 200f), 0.9f, 1f)
        delay(50)
        collector.onTouchEnd(Offset(200f, 200f), 0.9f, 1f)

        delay(100) // let events propagate
        job.cancel()

        assertEquals("Should have 2 touch events", 2, events.size)
        // First touch pressure ~0.3, second ~0.9
        assertTrue(
            "First event pressure (${events[0].pressure}) should be less than second (${events[1].pressure})",
            events[0].pressure < events[1].pressure
        )
    }
}
