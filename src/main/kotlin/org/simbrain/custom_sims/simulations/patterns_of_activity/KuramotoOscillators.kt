package org.simbrain.custom_sims.simulations.patterns_of_activity

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.RandomWeightInitializer
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.updaterules.KuramotoRule
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.util.SimbrainConstants
import org.simbrain.util.piccolo.loadEmptyMap
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.SmellSensor
import javax.swing.SwingUtilities
import kotlin.math.sqrt

/**
 * Simulate a reservoir of neurons exposed to smell inputs
 * and visualize the "cognitive maps" that develop in a PCA projetion labelled
 * by environmental inputs.
 */
val kuramotoOscillators = newSim("kuramotoOscillators") {
    val netSize = 50
    val spacing = 40.0
    val dispersion = 140.0

    // Clear workspace
    workspace.clearWorkspace()

    // Set up world
    val oc = addOdorWorldComponent("World")
    oc.world.isObjectsBlockMovement = false
    oc.world.isUseCameraCentering = false
    oc.world.tileMap = loadEmptyMap()

    // Mouse
    val mouse = oc.world.addEntity(202, 176, EntityType.Mouse)
    val smellSensor = SmellSensor("Smell-Center", 0.0, 0.0)
    mouse.addSensor(smellSensor)
    mouse.heading = 90.0

    // Objects
    val cheese = oc.world.addEntity(55, 306, EntityType.Swiss, doubleArrayOf(25.0, 0.0, 0.0))
    cheese.smellSource.dispersion = dispersion
    val flower = oc.world.addEntity(351, 311, EntityType.Flower, doubleArrayOf(0.0, 25.0, 0.0))
    flower.smellSource.dispersion = dispersion
    val fish = oc.world.addEntity(160, 14, EntityType.Fish, doubleArrayOf(0.0, 0.0, 25.0))
    fish.smellSource.dispersion = dispersion

    // Set up network
    val networkComponent = addNetworkComponent("Patterns of Activity")
    val net = networkComponent.network
    net.timeStep = 0.5

    // Main recurrent net
    val reservoirNet = net.addNeuronCollection(netSize) {
        // Allostatic also works pretty nicely here
        updateRule = KuramotoRule()
        if (Math.random() < 0.5) {
            polarity = SimbrainConstants.Polarity.EXCITATORY
        } else {
            polarity = SimbrainConstants.Polarity.INHIBITORY
        }
    }.apply {
        layout = HexagonalGridLayout(spacing, spacing, sqrt(netSize.toDouble()).toInt())
        setLocation(185.0, 50.0)
        applyLayout(-5, -85)
        label = "Reservoir"
    }

    // Set up recurrent synapses
    val connection = Sparse(0.1, true, false).apply {
        percentExcitatory = 100.0
    }
    val recurrentSyns = SynapseGroup(reservoirNet, reservoirNet, connection).apply {
        label = "Synapses"
        net.addNetworkModel(this)
    }

    // Inputs
    val inputNetwork = net.addNeuronCollection(3) {
        updateRule = LinearRule().apply {
            addNoise = true
            noiseGenerator = NormalDistribution(0.0, .1)
        }
    }.apply {
        setLowerBound(-100.0)
        setUpperBound(100.0)
        label = "Sensory Neurons"
        layout = LineLayout()
        applyLayout()
        setLocation(130.0, 660.0)
    }

    // Inputs to reservoir
    val sparseExcitatory = Sparse(0.7, true, false).apply {
        percentExcitatory = 100.0
    }
    sparseExcitatory.weightInitializer = RandomWeightInitializer().apply {
        exRandomizer = NormalDistribution(10.0, 1.0)
    }
    val inputToRes = SynapseGroup(inputNetwork, reservoirNet, sparseExcitatory).apply {
        net.addNetworkModel(this)
        SwingUtilities.invokeLater { displaySynapses = false }
        randomizeExcitatory()
    }

    // Couple from mouse to input nodes
    couplingManager.createCoupling(smellSensor, inputNetwork)

    // Set up projection plot
    val plot = addProjectionPlot("Cognitive Map")
    plot.projector.tolerance = 20.0
    couplingManager.createCoupling(reservoirNet, plot)

    // Text of nearest world object to projection plot current dot
    with(couplingManager) {
        mouse.getProducer(OdorWorldEntity::getNearbyObjectName) couple
                plot.getConsumer(ProjectionComponent::setLabel)
    }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 443, 450)
        place(oc, 443 + 2 * SIM_WINDOW_GAP, SIM_WINDOW_GAP, 426, 437)
        place(plot, 443 + 426 + 3 * SIM_WINDOW_GAP, SIM_WINDOW_GAP, 460, 437)
    }

    // // "Halo" based on prediction error
    // workspace.addUpdateAction(ColorPlotKt.createColorPlotUpdateAction(
    //         plot.projector,
    //         predictionRes,
    //         errorNeuron.activation
    // ))
}
