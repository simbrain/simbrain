/** Fixed-position spelling representation used by the triangle reading simulation. */
package org.simbrain.custom_sims.simulations.reading

class OrthographyEncoder(private val slots: Int = 5) {
    init {
        require(slots > 0) { "There must be at least one orthographic slot" }
    }

    val symbolsPerSlot = SYMBOLS.size
    val dimension = slots * symbolsPerSlot

    val labels = (0 until slots).flatMap { slot -> SYMBOLS.map { symbol -> "${slot + 1}:$symbol" } }

    fun encode(word: String): DoubleArray {
        require(word.length <= slots) { "'$word' exceeds the $slots-letter development representation" }
        require(word.all { it in 'a'..'z' || it in 'A'..'Z' }) { "Words must contain letters only" }
        return DoubleArray(dimension).also { vector ->
            word.lowercase().padEnd(slots, BLANK).forEachIndexed { slot, letter ->
                vector[slot * SYMBOLS.size + SYMBOLS.indexOf(letter)] = 1.0
            }
        }
    }

    companion object {
        private const val BLANK = '_'
        private val SYMBOLS = ('a'..'z').toList() + BLANK
    }
}
