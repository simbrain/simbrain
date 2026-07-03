package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.spikeresponders.RiseAndDecay
import org.simbrain.network.spikeresponders.ShortTermPlasticity
import org.simbrain.network.spikeresponders.StepResponder
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
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
        labelArray = (1 .. arraySize).map { "$it" }.toTypedArray()
        updateRule = IzhikevichRule().apply {
            setiBg(0.0)
        }
        label = "Izhikevich"
    }
    val weightsInput = WeightMatrix(input, spiking)
    network.addNetworkModels(input, spiking, weightsInput)
    offsetNetworkModel(input, spiking, Direction.EAST, 400.0)

    val stepResponder = NeuronArray(arraySize).apply {
        label = "Step responder"
    }
    val weightsStep = WeightMatrix(spiking, stepResponder).apply {
        spikeResponder = StepResponder()
    }
    network.addNetworkModels(stepResponder, weightsStep, usePlacementManager = false)
    offsetNetworkModel(spiking, stepResponder, Direction.EAST, 600.0)
    offsetNetworkModel(spiking, stepResponder, Direction.NORTH, 400.0)

    val jumpAndDecay = NeuronArray(arraySize).apply {
        label = "Jump and Decay"
    }
    val weightsJump = WeightMatrix(spiking, jumpAndDecay).apply {
        spikeResponder = JumpAndDecay()
    }
    network.addNetworkModels(jumpAndDecay, weightsJump, usePlacementManager = false)
    alignNetworkModels(stepResponder, jumpAndDecay, Alignment.VERTICAL)
    offsetNetworkModel(stepResponder, jumpAndDecay, Direction.SOUTH, 400.0)

    val riseAndDecay = NeuronArray(arraySize).apply {
        label = "Rise and Decay"
    }
    val weightsRise = WeightMatrix(spiking, riseAndDecay).apply {
        spikeResponder = RiseAndDecay()
    }
    network.addNetworkModels(riseAndDecay, weightsRise, usePlacementManager = false)
    alignNetworkModels(stepResponder, riseAndDecay, Alignment.VERTICAL)
    offsetNetworkModel(jumpAndDecay, riseAndDecay, Direction.SOUTH, 300.0)

    val stp = NeuronArray(arraySize).apply {
        label = "Short term plasticity"
    }
    val weightsSTP = WeightMatrix(spiking, stp).apply {
        spikeResponder = ShortTermPlasticity()
    }
    network.addNetworkModels(stp, weightsSTP, usePlacementManager = false)
    alignNetworkModels(stepResponder, stp, Alignment.VERTICAL)
    offsetNetworkModel(riseAndDecay, stp, Direction.SOUTH, 300.0)

    val spikePlot = addTimeSeriesComponent("Izhikevich", "Membrane Potentials")
    withGui {
        placeComponent(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 715, 733)
        placeComponent(spikePlot, SIM_WINDOW_GAP + 715 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 400, 400)
    }

    with(couplingManager) {
        spiking couple spikePlot.model
    }

    addSidebarInfo(
        """
        # Spike Responders (Array Version)
        
        This simulation demonstrates spike responders using neuron arrays instead of individual neurons. It shows how different types of spike responders affect multiple 
        downstream neurons simultaneously, allowing you to see patterns across populations of neurons.
        
        It is basically the same as the spike responder simulation but showing how it can be implemented with neuron arrays as well as free neurons.

        # Simulation Details
        
        Similar to the single-neuron spike responder simulation, this version uses:
        - `Input Array`: An array of `10` clamped input neurons
        - `Izhikevich Array`: `10` spiking neurons that receive input and generate spikes
        - Four `Response Arrays`: Each containing `10` neurons with different spike responders:
            1. Step Responder: Fixed response duration
            2. Jump and Decay: Immediate rise with exponential decay
            3. Rise and Decay: Gradual rise and fall
            4. Short Term Plasticity: Adaptive responses based on spike history

        # What to Do
        
        1. `Run` the simulation to see the array-based spiking behavior.
        
        2. Adjust input levels by clicking on neurons in the input array and using arrow keys.
        
        3. Observe population dynamics in the membrane potential plot. You can see how different neurons in the array respond.
        
        4. Compare response patterns across the different spike responder arrays.
        
        # Credits
        
        Elijah Olson
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
                
        Kanly Thao
        
        """.trimIndent()
    )

}
