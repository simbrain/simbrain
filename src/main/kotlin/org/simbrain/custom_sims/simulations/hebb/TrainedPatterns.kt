/**
 * A simulation-side record of the patterns trained with a pattern simulation's control panel buttons, used to
 * show a corr plot of what the network has actually been taught. It lives in the simulation rather than the
 * network model, so it does not see training done elsewhere, such as from the network's context menu, and the
 * network's own training data is left alone.
 */
package org.simbrain.custom_sims.simulations.hebb

import org.simbrain.util.showMessageDialog
import kotlin.math.max
import kotlin.math.min

/**
 * Patterns are identified by which units are on, meaning above [onThreshold], so retraining the same pattern
 * increases its count rather than adding a row. A pattern is named after the closest of [namedPatterns] when it
 * matches exactly ("Circle") or is within a few flipped units ("Circle ~3"); otherwise it is numbered in
 * training order ("Pattern 1"). Different patterns that end up with the same near-match name get a letter
 * ("Circle ~3", "Circle ~3b"). With [nameInverses], flipped versions of named patterns are matched too, since
 * Hopfield networks store both a pattern and its inverse.
 */
class TrainedPatterns(
    private val namedPatterns: Map<String, DoubleArray>,
    private val onThreshold: Double,
    private val nameInverses: Boolean = false,
) {

    private class Entry(val pattern: DoubleArray, val bits: BooleanArray, val name: String) {
        var count = 1
    }

    private val entries = mutableListOf<Entry>()

    private var unnamedCount = 0

    private val nearMatchNameCounts = mutableMapOf<String, Int>()

    private val namedBits by lazy {
        namedPatterns.flatMap { (name, pattern) ->
            val bits = pattern.toBits()
            if (nameInverses) {
                listOf(name to bits, "$name (inverted)" to BooleanArray(bits.size) { !bits[it] })
            } else {
                listOf(name to bits)
            }
        }
    }

    private fun DoubleArray.toBits() = BooleanArray(size) { this[it] > onThreshold }

    /**
     * One label per trained pattern, in training order, with a repeat count when a pattern was trained more
     * than once.
     */
    val labels: List<String>
        get() = entries.map { if (it.count > 1) "${it.name} ×${it.count}" else it.name }

    fun record(pattern: DoubleArray) {
        val bits = pattern.toBits()
        val existing = entries.firstOrNull { it.bits.contentEquals(bits) }
        if (existing != null) {
            existing.count++
        } else {
            entries += Entry(pattern.copyOf(), bits, nameFor(bits))
        }
    }

    fun clear() {
        entries.clear()
        unnamedCount = 0
        nearMatchNameCounts.clear()
    }

    /**
     * How many units a pattern may differ from [named] and still be named after it. Besides a cap of a tenth of
     * the layer, the limit is a third of the named pattern's minority units, so a sparse pattern like a 10-unit
     * diagonal line is not matched by a blank layer that is only 10 units away from it.
     */
    private fun nearMatchLimit(named: BooleanArray): Int {
        val on = named.count { it }
        return max(1, min(named.size / 10, min(on, named.size - on) / 3))
    }

    private fun nameFor(bits: BooleanArray): String {
        val closest = namedBits
            .filter { (_, named) -> named.size == bits.size }
            .map { (name, named) -> Triple(name, bits.indices.count { bits[it] != named[it] }, named) }
            .filter { (_, distance, named) -> distance <= nearMatchLimit(named) }
            .minByOrNull { (_, distance, _) -> distance }
            ?: return "Pattern ${++unnamedCount}"
        val (name, distance) = closest
        if (distance == 0) {
            return name
        }
        val nearName = "$name ~$distance"
        val repeats = nearMatchNameCounts.merge(nearName, 1, Int::plus)!!
        return if (repeats == 1) nearName else nearName + ('a' + repeats - 1)
    }

    fun showCorrelations() {
        if (entries.isEmpty()) {
            showMessageDialog(
                "No patterns have been trained yet. Load a pattern and press a train button first.",
                "Trained pattern correlations",
                rows = 3
            )
            return
        }
        showPatternCorrelations(
            labels,
            entries.map { it.pattern },
            "Trained pattern correlations"
        )
    }
}
