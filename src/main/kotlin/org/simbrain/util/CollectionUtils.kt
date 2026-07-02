package org.simbrain.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Consider sets A and B.  "Left complement" is in A but not B.  "Right complement" is in B but not A.
 */
data class SetDifference<T>(val leftComp: Set<T>, val rightComp: Set<T>) {
    fun isIdentical() = leftComp.isEmpty() && rightComp.isEmpty()
}

/**
 * Custom infix relative complementation operator.
 */
infix fun <T> Set<T>.complement(other: Set<T>) = SetDifference(this - other, other - this)

infix fun <T> List<T>.complement(other: List<T>): SetDifference<T> {
    val leftSet = LinkedHashSet(this)
    val rightSet = LinkedHashSet(other)
    return SetDifference(leftSet - rightSet, rightSet - leftSet)
}

/**
 * Map a pair of lists to a list of pairs.
 *
 * Ex: (1,2) cartesianProduct (3,4) -> ((1,3),(1,4),(2,3), (2,4))
 */
infix fun <T, U> Iterable<T>.cartesianProduct(other: Iterable<U>) = this.flatMap { a -> other.map { b -> a to b } }

/**
 * [cartesianProduct] for a pair of sequences.
 */
infix fun <T, U> Sequence<T>.cartesianProduct(other: Sequence<U>) = this.flatMap { a -> other.map { b -> a to b } }

fun List<List<Double>>.toDoubleArray(): Array<DoubleArray> {
    return map {
        it.toDoubleArray()
    }.toTypedArray()
}

/**
 * Flatten a 2d double array into a 1-d double array
 */
fun flattenArray(array: Array<DoubleArray>) = sequence {
    for (row in array) {
        for (element in row) {
            yield(element)
        }
    }
}.toList().toDoubleArray()

/**
 * Flatten a 2d double array into a 1-d double array
 */
fun flattenArray(array: Array<FloatArray>) = sequence {
    for (row in array) {
        for (element in row) {
            yield(element)
        }
    }
}.toList().toFloatArray()

/**
 * Recursively get the shape of each dimension of an arbitrarily deep array
 */
val Array<*>.shape: IntArray
    get() {
        fun sizeOf(array: Any) = when (array) {
            is Array<*> -> array.size
            is FloatArray -> array.size
            else -> 0
        }

        fun firstOrNullOf(array: Any) = when (array) {
            is Array<*> -> array.firstOrNull()
            is FloatArray -> array.firstOrNull()
            else -> null
        }

        fun getShape(current: Any): IntArray {
            val size = sizeOf(current)
            val first = firstOrNullOf(current)
            return if (first is Array<*> || first is FloatArray) {
                intArrayOf(size, *getShape(first))
            } else {
                intArrayOf(size)
            }
        }
        return getShape(this)
    }

/**
 * Reshape a 1-d double array into a 2d array with the indicated number of rows and columns.
 *
 * Values are filled in row-major order, across rows first.
 */
fun reshape(rows: Int, cols: Int, array: DoubleArray): Array<DoubleArray> {
    require(array.size == rows * cols) {
        "Cannot reshape array of size ${array.size} into shape ($rows, $cols)"
    }
    return Array(rows) { i ->
        val row = DoubleArray(cols)
        for (j in 0 until cols) {
            row[j] = array[i * cols + j]
        }
        row
    }
}

/**
 * Convert integer array to long array
 */
fun IntArray.toLongArray(): LongArray {
    return map { it.toLong() }.toLongArray()
}

/**
 * Randomly shuffles k integers in a list. The first k elements are randomly
 * swapped with other elements in the list. This method will alter the list
 * passed to it, so situations where this would be undesirable should pass
 * this method a copy.
 *
 * @param inds a list of integers. This methods WILL shuffle inds, so pass a
 * copy unless inds being shuffled is not a problem.
 * @param k    how many elements will be shuffled
 * @param rand a random number generator
 */
fun randShuffleK(inds: ArrayList<Int?>, k: Int, rand: Random) {
    for (i in 0 until k) {
        Collections.swap(inds, i, rand.nextInt(inds.size))
    }
}

/**
 * A numpy-style linspace command. Returns an array of [numPoints] integers between [start] and [stop].
 *
 * @see https://stackoverflow.com/questions/55786239/how-to-autogenerate-array-in-kotlin-similar-to-numpy
 */
fun linspace(start: Int, stop: Int, numPoints: Int) = Array(numPoints) { start + it * ((stop - start) / (numPoints - 1)) }

fun linspace(start: Double, stop: Double, numPoints: Int) = DoubleArray(numPoints) { start + it * ((stop - start) /
        (numPoints - 1)) }

fun Array<DoubleArray>.flatten() = flattenArray(this)
fun Array<FloatArray>.flatten() = flattenArray(this)

inline fun DoubleArray.applyFunctionInPlace(fn: (Double) -> Double): DoubleArray {
    for (i in indices) {
        this[i] = fn(this[i])
    }
    return this
}

fun DoubleArray.applyFunction(fn: (Double) -> Double): DoubleArray {
    val retArray = DoubleArray(size)
    for (i in indices) {
        retArray[i] = fn(this[i])
    }
    return retArray
}


fun <T> ListIterator<T>.toSequence() = sequence {
    while (hasNext()) {
        yield(next())
    }
}

/**
 * Normalize the values in this collection to the range [0, 1], using min-max normalization.
 * Recenter and scale by the range.
 */
fun Collection<Double>.minMaxNormalize(): List<Double> {
    val min = minOrNull() ?: 0.0
    val max = maxOrNull() ?: 1.0
    val range = max - min
    return if (range == 0.0) map { 0.0 } else map { (it - min) / range }
}

/**
 * Normalize so that the values sum to 1, by dividing each member by the sum.
 *
 * Does not enforce non-negativity. If all values are positive it produces a probability distribution.
 */
fun Collection<Double>.normalize(): List<Double> {
    val sum = sum()
    return map { it / sum }
}

/**
 * Returns the size of this collection, including the given item if it's not already in the collection.
 * Useful for context menus where the right-clicked item may not yet be in the selection.
 */
fun <T> Collection<T>.sizeIncluding(item: T?): Int {
    return if (item != null && !contains(item)) size + 1 else size
}

/**
 * A map that associates keys with deferred values, allowing asynchronous retrieval and completion.
 *
 * Useful when a value is expected to be provided later, and other code can `await` its availability.
 *
 * @param timeoutMillis Maximum time in milliseconds to wait for a value before timing out.
 */
class CompletableDeferredHashMap<K, V : Any>(
    // Generous: only a backstop against a value that truly never arrives (a bug), not a load-sensitivity
    // knob. The previous 1s value caused spurious timeouts whenever node creation lagged under EDT load.
    private val timeoutMillis: Long = 10_000,
    // Short: getImmediately only briefly waits on an in-flight value before reporting absence.
    private val immediateTimeoutMillis: Long = 1_000,
) {
    private val map = ConcurrentHashMap<K, CompletableDeferred<V>>()

    suspend fun <T : V> get(key: K): T {
        @Suppress("UNCHECKED_CAST")
        return withTimeout(timeoutMillis) {
            map.computeIfAbsent(key) { CompletableDeferred() }.await() as T
        }
    }

    /**
     * The value if it is (or quickly becomes) available, else null. Unlike [get], a slow or
     * never-arriving value is reported as null after [immediateTimeoutMillis] rather than thrown, so a
     * caller can recreate or skip instead of crashing the whole operation. This is what its name implies
     * — "give it to me if it's readily there" — and matches its `?: createNode(...)` / `?.let { }` uses.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : V> getImmediately(key: K): T? {
        return withTimeoutOrNull(immediateTimeoutMillis) { map[key]?.await() as T? }
    }

    operator fun set(key: K, value: V): CompletableDeferred<V> {
        return map.compute(key) { _, existingValue ->
            // A pending deferred has a waiter (a get); complete it so the waiter unblocks. An already
            // completed deferred is stale (e.g. the model's previous node, whose async removal has not
            // run yet); replace it so the map points at the new value rather than silently dropping it,
            // since complete() is a no-op on an already completed deferred.
            if (existingValue != null && !existingValue.isCompleted) {
                existingValue.apply { complete(value) }
            } else {
                CompletableDeferred(value)
            }
        }!!
    }

    fun remove(key: K) {
        map.remove(key)
    }

    /**
     * Atomically remove [key] only if it currently maps to a completed value satisfying [predicate].
     *
     * Used to make asynchronous node removal identity-safe: a delayed removal of an old node must not
     * clear the mapping if the model has since been re-added with a new node (e.g. undo followed by redo).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun removeIfValue(key: K, predicate: (V) -> Boolean) {
        map.computeIfPresent(key) { _, deferred ->
            if (deferred.isCompleted && predicate(deferred.getCompleted())) null else deferred
        }
    }

    /**
     * Non-blocking lookup: return the value if [key] is present and already resolved, else null.
     *
     * Unlike [get]/[getImmediately] this never suspends and never waits on a pending value, so it is safe
     * for gating logic that must not block (e.g. deciding during a restore whether dependent nodes exist).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun peek(key: K): V? = map[key]?.let { if (it.isCompleted) it.getCompleted() else null }

    val keys get() = map.keys

    val size get() = map.size

    fun clear() = map.clear()
}