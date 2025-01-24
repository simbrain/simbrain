package org.simbrain.util.propertyeditor

import org.simbrain.util.withTempPublicAccess
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

abstract class AutoCopyObject<T: CopyableObject>: CopyableObject {


    override fun copy(): T {
        this::class.primaryConstructor?.let { constructor ->
            val properties = (this::class.allSuperclasses + this::class)
                .map { it.declaredMemberProperties }
                .flatten()
            val args = constructor.parameters.associate { parameter ->
                val property = properties.find { it.name == parameter.name }
                val value = property!!.withTempPublicAccess {
                    property.getter.call(this@AutoCopyObject)
                }
                parameter to value
            }
            val delegated = properties
                .filterIsInstance<KMutableProperty1<T, *>>()
                .onEach { it.isAccessible = true }
                .mapNotNull { property ->
                    property.getDelegate(this as T)?.also { property.get(this) }
                }
                .filterIsInstance<GuiEditable<T, *>>()
                .associate { it.property.name to it.property }
            val annotated = properties
                .filterNot {
                    // If a property has both a UserParameter annotation and a GuiEditable delegation, only keep the delegation
                    delegated.keys.contains(it.name)
                }.associate { it.name to it }
            val allProperties = delegated + annotated
            return (constructor.callBy(args) as T).also {
                allProperties.filterNot { args.keys.map { it.name }.contains(it.key) }.forEach { (name, property) ->
                    property.withTempPublicAccess {
                        (property as? KMutableProperty1<Any, Any>)?.setter?.call(it, property.getter.call(this))
                    }
                }
            }
        } ?: throw IllegalStateException("No primary constructor found for ${this::class.simpleName}")
    }


}