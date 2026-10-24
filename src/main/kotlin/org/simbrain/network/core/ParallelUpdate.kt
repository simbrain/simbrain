/**
 * Parallel execution for the phases of a buffered network update ([Network.bufferedUpdate]).
 *
 * Work is split into contiguous chunks balanced by an estimated cost (for neurons, the size of their fan-in), and
 * every network submits its chunks to one shared, bounded dispatcher. Several networks updating in the same workspace
 * iteration therefore share the same cores rather than each claiming all of them, and an idle thread picks up the
 * remaining chunks of whichever network is still running. Small networks produce a single chunk and run inline,
 * so they pay nothing for the machinery.
 */
package org.simbrain.network.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.simbrain.network.gui.dialogs.NetworkPreferences

/**
 * Number of chunks that may run at once. One core is left for the Swing event thread so the interface stays
 * responsive while a large network runs.
 */
val modelUpdateParallelism = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1)

/**
 * Dispatcher shared by every network's parallel update phases.
 */
val modelUpdateDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(modelUpdateParallelism)

/**
 * Minimum estimated cost of a chunk (roughly, synapse visits). Below this, launching a coroutine costs more than it
 * saves.
 */
private const val MIN_CHUNK_COST = 4096L

/**
 * Splits [items] into contiguous chunks of roughly equal total [cost]. Contiguous chunks keep neighboring models,
 * which are usually allocated together, on the same thread. Returns a single chunk when the total cost is too small
 * to be worth splitting.
 */
fun <T> chunkByCost(items: List<T>, cost: (T) -> Int): List<List<T>> {
    if (items.isEmpty()) return emptyList()
    val costs = LongArray(items.size) { cost(items[it]).toLong().coerceAtLeast(1) }
    val total = costs.sum()
    val numChunks = minOf(modelUpdateParallelism * 4L, total / MIN_CHUNK_COST, items.size.toLong()).toInt()
    if (numChunks <= 1) return listOf(items)
    val target = total.toDouble() / numChunks
    val chunks = ArrayList<List<T>>(numChunks)
    var start = 0
    var accumulated = 0L
    for (i in items.indices) {
        accumulated += costs[i]
        if (accumulated >= target * (chunks.size + 1) && chunks.size < numChunks - 1) {
            chunks += items.subList(start, i + 1)
            start = i + 1
        }
    }
    if (start < items.size) chunks += items.subList(start, items.size)
    return chunks
}

/**
 * Replaces [NetworkPreferences.parallelUpdate] without persisting a preference, so tests can compare a parallel
 * update with a serial reference. Null defers to the preference.
 */
@Volatile
internal var parallelUpdateOverride: Boolean? = null

@PublishedApi
internal val parallelUpdateEnabled: Boolean get() = parallelUpdateOverride ?: NetworkPreferences.parallelUpdate

/**
 * Runs [action] on every item of every chunk and returns once all have finished. Chunks run concurrently on
 * [modelUpdateDispatcher] unless there is only one chunk or parallel updating is turned off in [NetworkPreferences].
 * [action] must only touch state owned by its item, since items in different chunks run at the same time.
 */
suspend inline fun <T> List<List<T>>.forEachInParallel(crossinline action: (T) -> Unit) {
    if (size <= 1 || !parallelUpdateEnabled) {
        for (chunk in this) chunk.forEach(action)
        return
    }
    coroutineScope {
        for (chunk in this@forEachInParallel) {
            launch(modelUpdateDispatcher) { chunk.forEach(action) }
        }
    }
}
