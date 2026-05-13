package org.simbrain.world.speechsynthesizer

data class DecodedPhoneme(val symbol: Char, val stress: Char? = null)

interface PhonemeCodec {

    val displayName: String

    val featureNames: List<String>

    val stressNames: List<String>

    val inputDimension: Int

    fun decodeFeatures(vector: DoubleArray): DecodedPhoneme

    fun symbolsToEspeak(symbols: String): String

    fun symbolsToIpa(symbols: String): String
}
