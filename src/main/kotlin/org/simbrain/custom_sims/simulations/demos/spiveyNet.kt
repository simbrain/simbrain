package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.OneToOne
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.NormalizationGroup
import org.simbrain.network.neurongroups.SoftmaxGroup
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.util.place
import org.simbrain.util.plus
import org.simbrain.util.point
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.toMatrix
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import smile.math.matrix.Matrix
import java.awt.Dimension
import javax.swing.JButton

/**
 * Based on Spivey's 2024 paper
 */
val spiveyNet = newSim {

    workspace.clearWorkspace()

    // Network
    val networkComponent = addNetworkComponent("Spivey Net")
    val net = networkComponent.network

    val group1 = NormalizationGroup(4).apply {
        layout = LineLayout()
        applyLayout()
    }
    val group2 = NormalizationGroup(4).apply {
        layout = LineLayout()
        applyLayout()
    }
    net.addNetworkModels(group1, group2).awaitAll()
    group1.location = point(0,0)
    group2.location = point(0,100)

    val connector = OneToOne().apply {
        percentExcitatory = 100.0
        useBidirectionalConnections = true
    }
    net.addNetworkModels(connector.connectNeurons(group1.neuronList, group2.neuronList))

    // World
    val oc = addOdorWorldComponent()
    oc.world.isUseCameraCentering = false
    desktop?.getDesktopComponent(oc)?.title = "Mouse Trace"
    oc.world.addEntity(157, 271, EntityType.MOUSE).apply {
        heading = 90.0
        isShowTrail = true
    }
    oc.world.addEntity(38, 49, EntityType.CANDLE)
    oc.world.addEntity(287, 44, EntityType.BELL)

    withGui {
        //place(docViewer, 0, 0, 464, 619)
        place(networkComponent, 222, 15, 400, 400)
        place(oc, 613, 15, 391, 455)
        createControlPanel("Control Panel", 15, 15) {
            addButton("Pattern 1") {
                group1.setActivations(doubleArrayOf(1.0,1.0,1.0,1.0))
                group2.setActivations(doubleArrayOf(1.0,1.0,1.0,1.0))
            }.apply {
                // Hack to make the panel wider
                preferredSize = Dimension(170, 30)
            }
            addButton("Pattern 2") {
                group1.setActivations(doubleArrayOf(-1.0,1.0,-1.0,1.0))
                group2.setActivations(doubleArrayOf(1.0,-1.0,1.0,-1.0))
            }
        }
    }

    //val docViewer = addDocViewer(
    //    "Information",
    //    """
    //        # Introduction
    //
    //        The Hopfield simulation is a recurrent neural network with a synaptic connection pattern for pattern recognition and memory retrieval.
    //
    //        # What to do
    //
    //        - Select an input pattern and click the train button on the Control panel to train the network on the selected pattern.
    //        - The model learns the pattern and “remembers” it.
    //        - When randomizing the network (by clicking “N” [Neuron], “R” [Randomize], and “Space” [Iterate], or using “I” [Wand Mode] over the nodes), the network adjusts the nodes on each iteration to reconfigure the inputted pattern.
    //        - The Network remembers the pattern and the antipattern, and when iterating (“Space”), it iterates to recreate the pattern with the most similar nodes.
    //
    //        You can get the pattern to memorize all the different patterns and antipatterns by training each one, randomizing and iterating to see if it is remembered, and training that pattern again if it needs to be learned.
    //
    //
    //    """.trimIndent()
    //)
}