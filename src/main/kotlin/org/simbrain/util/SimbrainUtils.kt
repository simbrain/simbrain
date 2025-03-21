package org.simbrain.util

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

class CachedObject<T>(private val init: () -> T) {

    private var isDirty = true
    private var _value: T? = null

    var value: T
        get() = if (isDirty) {
            _value = init()
            isDirty = false
            _value!!
        } else {
            _value!!
        }
        set(value) {
            _value = value
            isDirty = false
        }

    fun invalidate() {
        isDirty = true
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