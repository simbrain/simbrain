package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.trainers.LeCun
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.place


val xorSim = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("XOR")
    val net = networkComponent.network

    val inputLayer = NeuronGroup(2).apply {
        isClamped = true
    }
    val hiddenLayer = NeuronGroup(2).apply {
        updateRule = SigmoidalRule()
    }
    val outputLayer = NeuronGroup(1).apply {
        updateRule = SigmoidalRule()
    }
    val sg1 = SynapseGroup(inputLayer, hiddenLayer)
    val sg2 = SynapseGroup(hiddenLayer, outputLayer)
    val sm = SupervisedModel(inputLayer, outputLayer).apply {
        trainerConfig.weightInitializationStrategy = LeCun()
    }
    net.addNetworkModels(inputLayer, hiddenLayer, outputLayer, sg1, sg2, sm)
    offsetNetworkModel(inputLayer, hiddenLayer, Direction.NORTH, 150.0)
    offsetNetworkModel(hiddenLayer, outputLayer, Direction.NORTH, 150.0)
    alignNetworkModels(inputLayer, hiddenLayer, Alignment.VERTICAL)
    alignNetworkModels(inputLayer, outputLayer, Alignment.VERTICAL)
    sm.randomize()

    sm.trainingSet = TrainingDataset(
        inputs = mutableListOf(
            mutableListOf(0.0, 0.0),
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 1.0)
        ),
        targets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )
    )


    addSidebarInfo(
        """ 
            # Introduction
            A basic demonstration of the backprop algorithm 
            
            # What to Do
            Double-click on the “Backprop” network in the “Backprop” network window to open the “Train Network” window.
            In the top area of this window, select the “Iterate training until the stop button is pressed” button to play the simulation. 
            The “Sum Squared Error” should decrease as the iterations increase and end up around 0. The goal is to get the error as low as possible.
            Click “Apply current row as input and increment selected row” to check the network's performance. 
            As you click the pattern on the bottom should be matched by the pattern on the top.
            
      
        """.trimIndent()
    )

    // Location of the network in the desktop
    withGui {
        place(networkComponent, 0, 0, 700, 700)
    }

}