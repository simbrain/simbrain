package org.simbrain.util

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

fun <T : Any> KClass<T>.callNoArgConstructor(): T = constructors.asSequence().mapNotNull {
    try {
        it.callBy(mapOf())
    } catch (e: IllegalArgumentException) {
        null
    }
}.first()

fun Any.allPropertiesToString(separator: String = "\n") = this::class.declaredMemberProperties.joinToString(separator) {
    val name = it.name
    val valueString = when(val value = it.getter.call(this)) {
        is String -> value
        is List<*> -> value.joinToString(", ")
        is Array<*> -> value.contentDeepToString()
        is DoubleArray -> value.joinToString(", ")
        is FloatArray -> value.joinToString(", ")
        else -> value.toString()
    }
    "$name = $valueString"
}

/**
 * Provide a block where you can run code as if the property was public, without actually changing the accessibility of that property.
 *
 * Calls the given block with this property accessible, then restores the original accessibility.
 */
fun <T, U, R> KProperty1<T, U>.withTempPublicAccess(block: KProperty1<T, U>.() -> R): R {
    val oldAccessible = isAccessible
    isAccessible = true
    val result = block()
    isAccessible = oldAccessible
    return result
}

fun Class<*>.isKotlinClass(): Boolean {
    return this.declaredAnnotations.any {
        it.annotationClass.qualifiedName == "kotlin.Metadata"
    }
}

fun Field.isTransient() = modifiers.let { mod -> Modifier.isTransient(mod) }