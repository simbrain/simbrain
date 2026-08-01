package org.simbrain.network.compositor

import org.simbrain.network.tensor.FloatTensor
import kotlin.math.exp

/** Immutable next-token probabilities copied at a model-update boundary for canvas display. */
data class TokenProbabilitySnapshot(
    val entries: List<Entry>,
    val argmaxTokenId: Int,
    val sampledTokenId: Int,
    val showAll: Boolean,
) {
    data class Entry(val tokenId: Int, val probability: Double)

    companion object {
        fun full(probabilities: DoubleArray, sampledTokenId: Int): TokenProbabilitySnapshot {
            val argmax = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
            return TokenProbabilitySnapshot(
                probabilities.indices.map { Entry(it, probabilities[it]) }, argmax, sampledTokenId, true,
            )
        }

        /**
         * Reads only the requested candidates from logits. The log-sum-exp pass keeps this cheap
         * for large vocabularies and avoids retaining a vocabulary-sized probability array.
         */
        fun topK(logits: FloatArray, count: Int, sampledTokenId: Int, temperature: Double = 1.0): TokenProbabilitySnapshot =
            topK(logits.size, { logits[it].toDouble() }, count, sampledTokenId, temperature)

        fun topK(logits: FloatTensor, count: Int, sampledTokenId: Int, temperature: Double = 1.0): TokenProbabilitySnapshot =
            topK(logits.size, { logits.data.get(it).toDouble() }, count, sampledTokenId, temperature)

        private fun topK(
            size: Int,
            logitAt: (Int) -> Double,
            count: Int,
            sampledTokenId: Int,
            temperature: Double,
        ): TokenProbabilitySnapshot {
            require(count > 0)
            require(temperature > 0.0)
            if (size == 0) return TokenProbabilitySnapshot(emptyList(), -1, sampledTokenId, false)
            var maximum = logitAt(0) / temperature
            var argmax = 0
            val selected = ArrayList<Int>(count + 1)
            for (i in 0 until size) {
                val value = logitAt(i) / temperature
                if (value > maximum) {
                    maximum = value
                    argmax = i
                }
                val insertion = selected.indexOfFirst { value > logitAt(it) / temperature }
                if (insertion >= 0) selected.add(insertion, i) else if (selected.size < count) selected.add(i)
                if (selected.size > count) selected.removeLast()
            }
            var sum = 0.0
            for (i in 0 until size) sum += exp(logitAt(i) / temperature - maximum)
            if (sampledTokenId in 0 until size && sampledTokenId !in selected) selected += sampledTokenId
            return TokenProbabilitySnapshot(
                selected.map { Entry(it, exp(logitAt(it) / temperature - maximum) / sum) }, argmax, sampledTokenId, false,
            )
        }
    }
}
