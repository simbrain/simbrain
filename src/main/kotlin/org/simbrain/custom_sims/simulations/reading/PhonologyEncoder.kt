/** Slot-based distributed phonological representation for a transparent starter lexicon. */
package org.simbrain.custom_sims.simulations.reading

class PhonologyEncoder(private val slots: Int = 4) {
    init {
        require(slots > 0) { "There must be at least one phonological slot" }
    }

    val featureNames = listOf(
        "consonant", "vowel", "voiced", "labial", "coronal", "dorsal", "glottal",
        "stop", "fricative", "nasal", "liquid", "glide", "high", "mid", "low", "front", "back", "tense"
    )
    val dimension = slots * featureNames.size
    val labels = (0 until slots).flatMap { slot -> featureNames.map { feature -> "${slot + 1}:$feature" } }

    fun encode(phonemes: List<String>): DoubleArray {
        require(phonemes.size <= slots) { "${phonemes.joinToString(" ")} exceeds the $slots-phoneme development representation" }
        return DoubleArray(dimension).also { vector ->
            phonemes.forEachIndexed { slot, phoneme ->
                val features = featureMap[phoneme] ?: error("Unsupported phoneme '$phoneme'")
                features.forEach { feature -> vector[slot * featureNames.size + featureNames.indexOf(feature)] = 1.0 }
            }
        }
    }

    fun decode(vector: DoubleArray): List<String> {
        require(vector.size == dimension) { "Expected $dimension phonological values" }
        return (0 until slots).mapNotNull { slot ->
            val segment = vector.copyOfRange(slot * featureNames.size, (slot + 1) * featureNames.size)
            val nearest = featureMap.minBy { (_, features) -> squaredDistance(segment, featureVector(features)) }
            nearest.key.takeIf { segment.sum() > 0.5 }
        }
    }

    private fun featureVector(features: Set<String>) = DoubleArray(featureNames.size) { index ->
        if (featureNames[index] in features) 1.0 else 0.0
    }

    private fun squaredDistance(a: DoubleArray, b: DoubleArray) = a.indices.sumOf { (a[it] - b[it]) * (a[it] - b[it]) }

    private fun features(vararg names: String) = names.toSet()

    private val featureMap = mapOf(
        "P" to features("consonant", "labial", "stop"), "B" to features("consonant", "voiced", "labial", "stop"),
        "T" to features("consonant", "coronal", "stop"), "D" to features("consonant", "voiced", "coronal", "stop"),
        "K" to features("consonant", "dorsal", "stop"), "G" to features("consonant", "voiced", "dorsal", "stop"),
        "F" to features("consonant", "labial", "fricative"), "V" to features("consonant", "voiced", "labial", "fricative"),
        "S" to features("consonant", "coronal", "fricative"), "Z" to features("consonant", "voiced", "coronal", "fricative"),
        "SH" to features("consonant", "coronal", "dorsal", "fricative"), "HH" to features("consonant", "glottal", "fricative"),
        "M" to features("consonant", "voiced", "labial", "nasal"), "N" to features("consonant", "voiced", "coronal", "nasal"),
        "L" to features("consonant", "voiced", "coronal", "liquid"), "R" to features("consonant", "voiced", "coronal", "dorsal", "liquid"),
        "W" to features("consonant", "voiced", "labial", "glide"), "Y" to features("consonant", "voiced", "glide"),
        "AE" to features("vowel", "low", "front"), "AH" to features("vowel", "mid"), "AO" to features("vowel", "mid", "back"),
        "AA" to features("vowel", "low", "back"), "EH" to features("vowel", "mid", "front"), "IH" to features("vowel", "high", "front"),
        "IY" to features("vowel", "high", "front", "tense"), "EY" to features("vowel", "mid", "front", "tense"),
        "AY" to features("vowel", "low", "front", "tense"), "OW" to features("vowel", "mid", "back", "tense"),
        "UW" to features("vowel", "high", "back", "tense")
    )
}
