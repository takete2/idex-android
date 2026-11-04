package dev.idex.android.recognition

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import dev.idex.android.data.PokemonCatalog
import kotlinx.serialization.json.Json
import java.nio.FloatBuffer

/**
 * On-device 151-way classifier. Photos are read into a tensor and discarded
 * with the session; nothing is uploaded.
 */
class KantoClassifier(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()
    private var session: OrtSession? = null
    private var environment: OrtEnvironment? = null
    private var inputName: String = "input"
    private var labels: List<ModelLabel> = emptyList()

    fun ensureLoaded() {
        synchronized(lock) {
            if (session != null) return
            val labelsText = appContext.assets.open("catalog/pokemon_labels.json").bufferedReader().use { it.readText() }
            val loaded = json.decodeFromString<List<ModelLabel>>(labelsText)
            if (loaded.size != 151 || loaded.map { it.id }.toSet() != (1..151).toSet()) {
                throw RecognitionException("The recognition label data is invalid.")
            }
            val bytes = appContext.assets.open("model/kanto-classifier.onnx").use { it.readBytes() }
            val env = OrtEnvironment.getEnvironment()
            val options = OrtSession.SessionOptions()
            val created = env.createSession(bytes, options)
            environment = env
            session = created
            inputName = created.inputNames.first()
            labels = loaded
            Log.i(TAG, "Loaded on-device classifier input=$inputName bytes=${bytes.size}")
        }
    }

    fun classify(bitmap: Bitmap): RecognitionResult {
        ensureLoaded()
        val env = environment ?: throw RecognitionException("The bundled recognition model is unavailable.")
        val current = session ?: throw RecognitionException("The bundled recognition model is unavailable.")
        val tensorData = ImagePrep.toModelInput(bitmap)
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(tensorData), longArrayOf(1, 3, 224, 224))
        tensor.use {
            current.run(mapOf(inputName to tensor)).use { result ->
                val logits = logitsOf(result.get(0).value)
                val recognition = Scoring.interpret(logits, labels)
                Log.i(
                    TAG,
                    "top=${recognition.candidates.firstOrNull()?.dexId} " +
                        "conf=${recognition.candidates.firstOrNull()?.confidence} " +
                        "margin=${recognition.logitMargin} accepted=${recognition.accepted != null}",
                )
                return recognition
            }
        }
    }

    private fun logitsOf(value: Any?): FloatArray {
        val row: FloatArray = when (value) {
            is Array<*> -> {
                val first = value.firstOrNull()
                when (first) {
                    is FloatArray -> first
                    is DoubleArray -> FloatArray(first.size) { index -> first[index].toFloat() }
                    else -> throw RecognitionException("The recognition model returned invalid scores.")
                }
            }
            is FloatArray -> value
            else -> throw RecognitionException("The recognition model returned invalid scores.")
        }
        return row
    }

    companion object {
        private const val TAG = "iDex"
    }
}

fun loadCatalog(context: Context): PokemonCatalog {
    val text = context.assets.open("catalog/pokemon-kanto.json").bufferedReader().use { it.readText() }
    return PokemonCatalog.fromJson(text)
}
