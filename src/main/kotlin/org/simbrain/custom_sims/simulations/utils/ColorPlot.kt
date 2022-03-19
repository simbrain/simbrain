package org.simbrain.custom_sims.simulations.utils

import org.simbrain.network.groups.NeuronGroup
import org.simbrain.util.projection.Halo
import org.simbrain.util.projection.Projector
import org.simbrain.workspace.updater.updateAction

/**
 * Creates a halo around the predicted next point at each iteration, to make
 * clear what points are being predicted.
 */
fun createColorPlotUpdateAction(projector: Projector, predictedState: DoubleArray, errorActivation: Double)
        = updateAction("Color projection points") {
    Halo.makeHalo(projector, predictedState, errorActivation.toFloat())
}

fun createColorPlotUpdateAction(projector: Projector, predictionNet: NeuronGroup, errorActivation: Double)
        = createColorPlotUpdateAction(projector, predictionNet.activations, errorActivation)