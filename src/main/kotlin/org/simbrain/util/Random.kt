package org.simbrain.util

import kotlin.random.Random

fun Random.nextBoolean(probability: Double) = nextDouble() < probability

fun Random.nextNegate() = if (nextBoolean()) 1 else -1