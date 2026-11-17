/**
 * Helpers and constants shared by the Leabra simulations.
 */
package org.simbrain.custom_sims.simulations.leabra

import java.awt.Color

/**
 * A pattern with 1 at [index] and 0 elsewhere, or all zeros when [index] is null.
 */
fun oneHot(size: Int, index: Int?) = DoubleArray(size) { if (it == index) 1.0 else 0.0 }

/**
 * Relative strength of feedback projections, which are weaker than feedforward ones.
 */
const val FEEDBACK_SCALE = 0.3

/**
 * Color of messages drawn on the network, such as `Learning!` and the gate state.
 */
val LEABRA_MESSAGE_COLOR = Color(215, 140, 0)
