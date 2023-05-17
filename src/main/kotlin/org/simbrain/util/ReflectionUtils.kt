package org.simbrain.util

import kotlin.reflect.KClass

fun <T : Any> KClass<T>.callNoArgConstructor(): T = constructors.asSequence().mapNotNull {
    try {
        it.callBy(mapOf())
    } catch (e: IllegalArgumentException) {
        null
    }
}.first()