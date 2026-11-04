package dev.idex.android.recognition

import kotlinx.serialization.Serializable
import kotlin.math.exp

@Serializable
data class ModelLabel(val id: Int, val name: String)

data class RecognitionCandidate(
    val dexId: Int,
    val name: String,
    val confidence: Double,
    val logit: Double,
)

data class RecognitionResult(
    val candidates: List<RecognitionCandidate>,
    val accepted: RecognitionCandidate?,
    val logitMargin: Double,
)

class RecognitionException(message: String) : Exception(message)

/**
 * Same acceptance rule as iDex: temperature-1 softmax at least 0.25 and a raw
 * logit margin of at least 0.5. These are not calibrated probabilities, and
 * there is no trained "not a Pokémon" class.
 */
object Scoring {
    const val MIN_CONFIDENCE = 0.25
    const val MIN_LOGIT_MARGIN = 0.5

    fun interpret(logits: FloatArray, labels: List<ModelLabel>): RecognitionResult {
        if (logits.size != labels.size || labels.size != 151) {
            throw RecognitionException("The recognition model returned invalid scores.")
        }
        if (logits.any { !it.isFinite() }) {
            throw RecognitionException("The recognition model returned invalid scores.")
        }
        val maxLogit = logits.max()
        val exponents = DoubleArray(logits.size)
        var denominator = 0.0
        for (index in logits.indices) {
            val value = exp((logits[index] - maxLogit).toDouble())
            exponents[index] = value
            denominator += value
        }
        val order = logits.indices.sortedByDescending { logits[it] }
        val candidates = order.take(3).map { index ->
            RecognitionCandidate(
                dexId = labels[index].id,
                name = labels[index].name,
                confidence = exponents[index] / denominator,
                logit = logits[index].toDouble(),
            )
        }
        val margin = (logits[order[0]] - logits[order[1]]).toDouble()
        val first = candidates.first()
        val accepted = if (first.confidence >= MIN_CONFIDENCE && margin >= MIN_LOGIT_MARGIN) first else null
        return RecognitionResult(candidates, accepted, margin)
    }
}
