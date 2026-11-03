/**
 * The queued changes shared by the bar and pie charts, which both show one value per component of an incoming
 * array, labelled by the coupled producer's names.
 */
package org.simbrain.plot

/** A change to a component chart's dataset, applied on the EDT. */
internal sealed interface ChartUpdate {
    class Values(val values: DoubleArray) : ChartUpdate
    class Names(val names: List<String>) : ChartUpdate
}

/**
 * Applies a batch in order. Values sent each iteration are latest-wins: values superseded by later ones in the same
 * batch are skipped.
 */
internal fun applyChartUpdates(
    batch: List<ChartUpdate>,
    applyValues: (DoubleArray) -> Unit,
    applyNames: (List<String>) -> Unit
) {
    val lastValues = batch.indexOfLast { it is ChartUpdate.Values }
    batch.forEachIndexed { i, update ->
        when (update) {
            is ChartUpdate.Values -> if (i == lastValues) applyValues(update.values)
            is ChartUpdate.Names -> applyNames(update.names)
        }
    }
}

/** Label for component [i]: its name, or its position counting from 1 when it has none. */
internal fun componentLabel(names: List<String>, i: Int) = names.getOrElse(i) { "${i + 1}" }
