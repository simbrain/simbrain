package org.simbrain.util

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.withTimeout
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <O, T> O.lazyVar(function: () -> T): ReadWriteProperty<O, T> = LazyVarImpl(function)

private object UNINITIALIZED_VALUE

/**
 * Lazy delegation that can be mutated. Adapted from the kotlin `by lazy` implementation.
 */
class LazyVarImpl<O, T>(val initializer: () -> T) : ReadWriteProperty<O, T>, Lazy<T> {
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE

    override var value: @UnsafeVariance T
        get() {
            if (_value !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return _value as T
            } else {
                val typedValue = initializer()
                _value = typedValue
                return typedValue
            }
        }
        set(newValue) {
            _value = newValue
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun getValue(thisRef: O, property: KProperty<*>): T {
        return value
    }

    override fun setValue(thisRef: O, property: KProperty<*>, value: T) {
        this.value = value
    }

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}

/**
 * A generic class that acts as a cache for an object of type [T], initializing or refreshing its value
 * only when marked as dirty or explicitly set externally. It helps in reducing redundant computations
 * or re-initializations for cases where the value is expected to remain constant until [CachedObject.invalidate] is
 * called.
 *
 * Safe to invalidate from one thread while another reads: each value is stored with the invalidation count it was
 * computed under, so a computation that an invalidation overtook is never mistaken for current. Two readers racing
 * after an invalidation may both compute; either result is current.
 */
class CachedObject<T>(private val init: () -> T) {

    private class Entry<T>(val generation: Long, val value: T)

    private val generation = AtomicLong()

    @Volatile
    private var entry: Entry<T>? = null

    var value: T
        get() {
            val current = generation.get()
            entry?.let { if (it.generation == current) return it.value }
            return init().also { entry = Entry(current, it) }
        }
        set(value) {
            entry = Entry(generation.get(), value)
        }

    fun invalidate() {
        generation.incrementAndGet()
    }
}

/**
 * When dependencies change, the next time the value is accessed, it will be recalculated by calling the init function.
 * Not intended for high performance use cases.
 */
class DependenciesInvalidatingCachedObject<T>(private vararg val dependencies: KProperty<*>, private val init: () -> T) {

    private var dependencyValues: List<Any?> = dependencies.map { it.getter.call() }
    private var _value: T? = null

    operator fun getValue(baseObject: Any, property: KProperty<*>): T {
        val dependencyValue = dependencies.map { it.getter.call() }
        return if (this.dependencyValues.zip(dependencyValue).any { (a, b) -> a != b } || _value == null) {
            _value = init()
            this.dependencyValues = dependencyValue
            _value!!
        } else {
            _value!!
        }
    }

    operator fun setValue(baseObject: Any, property: KProperty<*>, value: T) {
        _value = value
    }
}

suspend fun waitFor(timeout: Long = 10000L, condition: () -> Boolean) {
    try {
        withTimeout(timeout) {
            var count = 0
            while (!condition()) {
                count++
                kotlinx.coroutines.delay(count * 100L)
            }
        }
    } catch (e: Exception) {
        throw IllegalStateException("Timeout waiting for condition", e)
    }
}