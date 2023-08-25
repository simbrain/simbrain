package org.simbrain.util

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

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