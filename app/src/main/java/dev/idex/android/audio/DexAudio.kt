package dev.idex.android.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dev.idex.android.data.AppLanguage
import dev.idex.android.data.Pokemon
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/** Local chimes, bundled cries, and the system speech engine. No network. */
class DexAudio(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var speech: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var soundEnabled: Boolean = true
    var onSpeaking: (Boolean) -> Unit = {}

    fun start() {
        if (speech != null) return
        speech = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = report(true)
                    override fun onDone(utteranceId: String?) = report(false)
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = report(false)
                    override fun onError(utteranceId: String?, errorCode: Int) = report(false)
                })
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
        if (!enabled) stopAll()
    }

    fun playButton() = blip(880.0, 0.045)
    fun playOpen() = blip(520.0, 0.07)
    fun playError() = blip(180.0, 0.12)
    fun playScan() = sweep()

    fun announce(pokemon: Pokemon, language: AppLanguage) {
        stopAll()
        if (!soundEnabled) return
        playCry(pokemon.id) {
            speak(pokemon.spokenEntry(language), language)
        }
    }

    fun speak(text: String, language: AppLanguage) {
        if (!soundEnabled || text.isBlank()) return
        val engine = speech ?: return
        engine.language = language.speechLocale()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "idex-entry")
    }

    fun stopSpeaking() {
        speech?.stop()
        report(false)
    }

    fun stopAll() {
        executor.execute {
            player?.runCatching {
                stop()
                release()
            }
            player = null
        }
        stopSpeaking()
    }

    fun release() {
        stopAll()
        speech?.shutdown()
        speech = null
        executor.shutdown()
    }

    private fun playCry(id: Int, then: () -> Unit) {
        executor.execute {
            try {
                val formatted = "%03d".format(id)
                val afd = appContext.assets.openFd("cries/$formatted.wav")
                val media = MediaPlayer()
                media.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                media.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                media.setOnCompletionListener {
                    it.release()
                    if (player == it) player = null
                    main.post(then)
                }
                media.setOnErrorListener { mp, _, _ ->
                    mp.release()
                    main.post(then)
                    true
                }
                media.prepare()
                player = media
                media.start()
            } catch (_: Exception) {
                main.post(then)
            }
        }
    }

    private fun report(speaking: Boolean) {
        main.post { onSpeaking(speaking) }
    }

    private fun blip(frequency: Double, seconds: Double) {
        if (!soundEnabled) return
        executor.execute { tone(frequency, seconds, sweep = false) }
    }

    private fun sweep() {
        if (!soundEnabled) return
        executor.execute { tone(640.0, 0.22, sweep = true) }
    }

    private fun tone(frequency: Double, seconds: Double, sweep: Boolean) {
        val sampleRate = 22050
        val count = (sampleRate * seconds).toInt().coerceAtLeast(1)
        val buffer = ShortArray(count)
        for (index in 0 until count) {
            val time = index / sampleRate.toDouble()
            val current = if (sweep) frequency + 900.0 * (index / count.toDouble()) else frequency
            val envelope = sin(PI * index / count)
            buffer[index] = (sin(2.0 * PI * current * time) * envelope * 7000.0).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(buffer.size * 2)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
        try {
            Thread.sleep((seconds * 1000).toLong() + 30)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        track.release()
    }
}
