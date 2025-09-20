package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.simulations.hebb.*
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.util.place
import org.simbrain.util.plus
import org.simbrain.util.showAPEOptionDialog
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.toColumnVector

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
    network.addNetworkModelAsync(rbm)

    addSidebarInfo(
        """ 
            # Introduction
            
             Simulation of a restricted Boltzmann machine. 

            # What to do
            
            - Click train on all patterns or click a pattern and click train on current pattern
            - To test the trained network, click on the visible layer, click `R` to randomize it, and click space to see if the pattern is recreated
            - Try training in different amounts on different groups of patterns and seeing how well they are remembered.

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
                    .toColumnVector()
            }
        }
    }
}