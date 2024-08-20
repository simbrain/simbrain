package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.spikeresponders.RiseAndDecay
import org.simbrain.network.spikeresponders.ShortTermPlasticity
import org.simbrain.network.spikeresponders.StepResponder
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create a spiking neuron, with an input, and graph its activity and spike responders with a time series.
 */
val spikeResponderSimArray = newSim {

    val arraySize = 10

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val input = NeuronArray(arraySize).apply {
        label = "Input"
        location = point(100, 80)
        isClamped = true
        increment = 1.0
        repeat(10) {increment()}
    }
    val spiking =  NeuronArray(arraySize).apply {
        updateRule = IzhikevichRule().apply {
            setiBg(0.0)
        }
        label = "Izhikevich"
    }
    val weightsInput = WeightMatrix(input, spiking)
    network.addNetworkModels(input, spiking, weightsInput)
    offsetNetworkModel(input, spiking, Direction.EAST, 300.0)

    val stepResponder = NeuronArray(arraySize).apply {
        label = "Step Responder"
    }
    val weightsStep = WeightMatrix(spiking, stepResponder).apply {
        spikeResponder = StepResponder()
    }
    network.addNetworkModels(stepResponder, weightsStep, usePlacementManager = false)
    offsetNetworkModel(spiking, stepResponder, Direction.EAST, 400.0)
    offsetNetworkModel(spiking, stepResponder, Direction.NORTH, 300.0)

    val jumpAndDecay = NeuronArray(arraySize).apply {
        label = "Jump and Decay"
    }
    val weightsJump = WeightMatrix(spiking, jumpAndDecay).apply {
        spikeResponder = JumpAndDecay()
    }
    network.addNetworkModels(jumpAndDecay, weightsJump, usePlacementManager = false)
    alignNetworkModels(stepResponder, jumpAndDecay, Alignment.VERTICAL)
    offsetNetworkModel(stepResponder, jumpAndDecay, Direction.SOUTH, 200.0)

    val riseAndDecay = NeuronArray(arraySize).apply {
        label = "Rise and Decay"
    }
    val weightsRise = WeightMatrix(spiking, riseAndDecay).apply {
        spikeResponder = RiseAndDecay()
    }
    network.addNetworkModels(riseAndDecay, weightsRise, usePlacementManager = false)
    alignNetworkModels(stepResponder, riseAndDecay, Alignment.VERTICAL)
    offsetNetworkModel(jumpAndDecay, riseAndDecay, Direction.SOUTH, 200.0)

    val stp = NeuronArray(arraySize).apply {
        label = "Short Term Plasticity"
    }
    val weightsSTP = WeightMatrix(spiking, stp).apply {
        spikeResponder = ShortTermPlasticity()
    }
    network.addNetworkModels(stp, weightsSTP, usePlacementManager = false)
    alignNetworkModels(stepResponder, stp, Alignment.VERTICAL)
    offsetNetworkModel(riseAndDecay, stp, Direction.SOUTH, 200.0)

    val spikePlot = addTimeSeriesComponent("Izhikevich", "Membrane Potentials")
    withGui {
        placeComponent(networkComponent, 0, 0, 715, 733)
        placeComponent(spikePlot, 715, 0, 400, 400)
    }

    with(couplingManager) {
        spiking couple spikePlot.model
    }

}