package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

enum class PocketlySound {
    TAP,
    TICK,
    CHART_SELECT,
    SUCCESS,
    SHEET_OPEN,
    DELETE
}

/**
 * Global tactile (HapticFeedbackConstants) + synthesized micro-sound engine for Pocketly.
 *
 * - 100% offline, zero external audio assets, zero permissions.
 * - Pre-synthesizes gentle 16-bit PCM micro-sounds (14ms–95ms) off the main thread.
 * - Respects Android system ringer mode (stays silent when device is in silent/vibrate mode)
 *   and the user's "Sound effects" preference in Profile.
 * - Fails gracefully in headless/Robolectric environments.
 */
class PocketlySoundEngine(context: Context) {

    private val appContext = context.applicationContext
    private val sampleRate = 22_050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var buffers: Map<PocketlySound, ByteArray> = emptyMap()

    @Volatile
    private var lastTickPlayedMs: Long = 0L

    init {
        scope.launch {
            runCatching {
                buffers = mapOf(
                    PocketlySound.TAP to synthChirp(
                        startHz = 680.0,
                        endHz = 460.0,
                        durationMs = 20,
                        gain = 0.16,
                        overtoneMix = 0.18
                    ),
                    PocketlySound.TICK to synthChirp(
                        startHz = 1180.0,
                        endHz = 860.0,
                        durationMs = 13,
                        gain = 0.12,
                        overtoneMix = 0.10
                    ),
                    PocketlySound.CHART_SELECT to synthBell(
                        freqHz = 659.25, // E5 warm marimba ping
                        durationMs = 52,
                        gain = 0.18
                    ),
                    PocketlySound.SUCCESS to synthTwoNoteChime(
                        firstHz = 659.25, // E5
                        secondHz = 880.0, // A5
                        noteMs = 46,
                        gain = 0.18
                    ),
                    PocketlySound.SHEET_OPEN to synthChirp(
                        startHz = 360.0,
                        endHz = 560.0,
                        durationMs = 26,
                        gain = 0.12,
                        overtoneMix = 0.12
                    ),
                    PocketlySound.DELETE to synthChirp(
                        startHz = 380.0,
                        endHz = 195.0,
                        durationMs = 36,
                        gain = 0.16,
                        overtoneMix = 0.14
                    )
                )
            }
        }
    }

    fun play(sound: PocketlySound) {
        val now = System.currentTimeMillis()
        // Prevent rapid scrub drags from flooding AudioTrack instances
        if (sound == PocketlySound.TICK && now - lastTickPlayedMs < 24L) return
        if (sound == PocketlySound.TICK) lastTickPlayedMs = now

        val pcm = buffers[sound] ?: return
        scope.launch {
            runCatching {
                val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager != null && audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                    return@runCatching
                }

                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val format = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(pcm.size.coerceAtLeast(2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                val durationMs = ((pcm.size / 2) * 1000L / sampleRate) + 40L
                kotlinx.coroutines.delay(durationMs)
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        }
    }

    fun release() {
        scope.cancel()
    }

    private fun synthChirp(
        startHz: Double,
        endHz: Double,
        durationMs: Int,
        gain: Double,
        overtoneMix: Double
    ): ByteArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val out = ByteArray(totalSamples * 2)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples.coerceAtLeast(1)
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / sampleRate
            val attack = (i / 14.0).coerceIn(0.0, 1.0)
            val decay = exp(-5.2 * t)
            val wave = (sin(phase) + overtoneMix * sin(phase * 2.0)) / (1.0 + overtoneMix)
            val sample = (wave * attack * decay * gain * Short.MAX_VALUE)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[i * 2] = (sample and 0xFF).toByte()
            out[i * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun synthBell(freqHz: Double, durationMs: Int, gain: Double): ByteArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val out = ByteArray(totalSamples * 2)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples.coerceAtLeast(1)
            val sec = i.toDouble() / sampleRate
            val attack = (i / 18.0).coerceIn(0.0, 1.0)
            val decay = exp(-4.8 * t)
            val wave = (
                0.72 * sin(2.0 * PI * freqHz * sec) +
                    0.22 * sin(2.0 * PI * freqHz * 2.0 * sec) * exp(-2.5 * t) +
                    0.06 * sin(2.0 * PI * freqHz * 3.0 * sec) * exp(-4.0 * t)
                )
            val sample = (wave * attack * decay * gain * Short.MAX_VALUE)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[i * 2] = (sample and 0xFF).toByte()
            out[i * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun synthTwoNoteChime(
        firstHz: Double,
        secondHz: Double,
        noteMs: Int,
        gain: Double
    ): ByteArray {
        val noteSamples = (sampleRate * noteMs) / 1000
        val overlap = noteSamples / 4
        val totalSamples = noteSamples * 2 - overlap
        val out = ByteArray(totalSamples * 2)
        val secondStart = noteSamples - overlap

        for (i in 0 until totalSamples) {
            var acc = 0.0
            if (i < noteSamples) {
                val t = i.toDouble() / noteSamples
                val sec = i.toDouble() / sampleRate
                val attack = (i / 16.0).coerceIn(0.0, 1.0)
                val decay = exp(-4.4 * t)
                acc += (0.78 * sin(2.0 * PI * firstHz * sec) + 0.22 * sin(2.0 * PI * firstHz * 2.0 * sec)) * attack * decay
            }
            if (i >= secondStart) {
                val j = i - secondStart
                val t = j.toDouble() / noteSamples
                val sec = j.toDouble() / sampleRate
                val attack = (j / 16.0).coerceIn(0.0, 1.0)
                val decay = exp(-4.0 * t)
                acc += (0.78 * sin(2.0 * PI * secondHz * sec) + 0.22 * sin(2.0 * PI * secondHz * 2.0 * sec)) * attack * decay
            }
            val sample = (acc * gain * Short.MAX_VALUE)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[i * 2] = (sample and 0xFF).toByte()
            out[i * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
        }
        return out
    }
}

/**
 * Unified haptic + global sound controller accessed via [LocalPocketlyFeedback].
 */
class PocketlyFeedbackController(
    private val view: View?,
    private val soundEngine: PocketlySoundEngine?,
    private val soundEnabledProvider: () -> Boolean
) {
    /** Crisp detent tick while scrubbing across chart points, keypad keys, or steppers. */
    fun scrubTick() {
        performHaptic(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
        )
        playSound(PocketlySound.TICK)
    }

    /** Tactile click + warm crystalline ping when tapping/selecting a chart data point. */
    fun chartPointTap() {
        performHaptic(HapticFeedbackConstants.CONTEXT_CLICK)
        playSound(PocketlySound.CHART_SELECT)
    }

    /** Crisp UI tap for navigation tabs, chips, segmented controls, and switches. */
    fun tap() {
        performHaptic(HapticFeedbackConstants.CLOCK_TICK)
        playSound(PocketlySound.TAP)
    }

    /** Soft air-pop when opening a Pocketly bottom sheet. */
    fun sheetOpen() {
        performHaptic(HapticFeedbackConstants.CLOCK_TICK)
        playSound(PocketlySound.SHEET_OPEN)
    }

    /** Confirmative haptic + two-note mint chime on save / primary action completion. */
    fun confirm() {
        performHaptic(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
        )
        playSound(PocketlySound.SUCCESS)
    }

    /** Distinct tactile reject/delete thud on destructive confirmation or invalid PIN. */
    fun deleteOrReject() {
        performHaptic(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
        )
        playSound(PocketlySound.DELETE)
    }

    private fun performHaptic(constant: Int) {
        runCatching {
            view?.performHapticFeedback(constant)
        }
    }

    private fun playSound(sound: PocketlySound) {
        if (soundEnabledProvider()) {
            soundEngine?.play(sound)
        }
    }

    companion object {
        val NoOp = PocketlyFeedbackController(null, null) { false }
    }
}

val LocalPocketlyFeedback = compositionLocalOf { PocketlyFeedbackController.NoOp }

@Composable
fun rememberPocketlyFeedbackController(soundEnabled: Boolean): PocketlyFeedbackController {
    val context = LocalContext.current
    val view = LocalView.current
    val engine = remember(context) { PocketlySoundEngine(context) }
    val enabledRef = remember { mutableListOf(soundEnabled) }
    enabledRef[0] = soundEnabled

    DisposableEffect(engine) {
        onDispose { engine.release() }
    }

    return remember(view, engine) {
        PocketlyFeedbackController(
            view = view,
            soundEngine = engine,
            soundEnabledProvider = { enabledRef[0] }
        )
    }
}
