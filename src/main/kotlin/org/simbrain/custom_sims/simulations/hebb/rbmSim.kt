package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.custom_sims.simulations.hebb.*
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.util.*
import org.simbrain.util.stats.distributions.NormalDistribution

/**
 * Demo for studying restricted Boltzmann machines.
 */
val rbmSim = newSim {

    val rbmCreator = RestrictedBoltzmannMachine.RBMCreator().apply {
        numVisible = 100
        numHidden = 150
    }.showAPEOptionDialog ("Create RBM Sim")
    val rbm = rbmCreator?.create()?:return@newSim

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    network.addNetworkModel(rbm)

    addSidebarInfo(
        """ 
            # Introduction
            
             Simulation of a restricted Boltzmann machine. 

            # What to do
            
            - Select an input pattern and click the train button on the Control panel to train the network on the selected pattern. 
            - The model learns the pattern and “remembers” it.
            - When randomizing the network (by clicking “N” [Neuron], “R” [Randomize], and “Space” [Iterate], or using “I” [Wand Mode] over the nodes), the network adjusts the nodes on each iteration to reconfigure the inputted pattern. 
            - The Network remembers the pattern and the antipattern, and when iterating (“Space”), it iterates to recreate the pattern with the most similar nodes. 
            
            You can get the pattern to memorize all the different patterns and antipatterns by training each one, randomizing and iterating to see if it is remembered, and training that pattern again if it needs to be learned. 

        """.trimIndent()
    )

    withGui {
        place(networkComponent, 230, 0, 815, 619)
        var numTrainIterations = 100
        createPatternControlPanel(
            rbm.visibleLayer,
            false
        ) { rbm.randomizeWeights() }?.apply {
            addTextField("Training iterations", "" + numTrainIterations) {
                it.toIntOrNull()?.let { num ->
                    numTrainIterations = num
                }
            }
            addButton("Train On All Patterns") {
                with(network) {
                    repeat(numTrainIterations) {
                        applyCirclePattern(rbm.visibleLayer)
                        rbm.trainOnCurrentPattern()
                        applySquarePattern(rbm.visibleLayer)
                        rbm.trainOnCurrentPattern()
                        applyLinePattern(rbm.visibleLayer, "diagonal")
                        rbm.trainOnCurrentPattern()
                        applyCrossPattern(rbm.visibleLayer)
                        rbm.trainOnCurrentPattern()
                    }
                }
            }
            addSeparator()
            addButton("Train on current pattern") {
                with(network) {
                    repeat(numTrainIterations) {
                        rbm.trainOnCurrentPattern()
                    }
                }
            }
            addSeparator()
            addButton("Add noise") {
                rbm.visibleLayer.activations += NormalDistribution(standardDeviation = .1)
                    .sampleDouble(rbm.visibleLayer.size)
                    .toMatrix()
            }
        }
    }
}