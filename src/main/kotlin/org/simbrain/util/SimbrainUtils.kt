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