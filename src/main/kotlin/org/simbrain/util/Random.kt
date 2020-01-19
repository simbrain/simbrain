package org.simbrain.util

import kotlin.random.Random

fun Random.nextBoolean(probability: Double) = nextDouble() < probability