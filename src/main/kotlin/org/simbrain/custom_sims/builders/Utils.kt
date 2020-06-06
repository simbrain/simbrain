package org.simbrain.custom_sims.builders

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AssignOnce<T : Any> : ReadWriteProperty<Any?, T> {

    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw IllegalStateException("Property ${property.name} should be initialized before get.")
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (this.value == null) {
            this.value = value
        } else {
            throw IllegalStateException("Property ${property.name} should be assigned only once.")
        }
    }

}
