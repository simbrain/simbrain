package org.simbrain.util

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

fun <T : Any> KClass<T>.callNoArgConstructor(): T = constructors.asSequence().mapNotNull {
    try {
        it.callBy(mapOf())
    } catch (e: IllegalArgumentException) {
        null
    }
}.first()

fun Any.allPropertiesToString(separator: String = "\n") = this::class.declaredMemberProperties.joinToString(separator) { "${it.name} = ${(it as KProperty1<Any, Any>).get(this)}" }