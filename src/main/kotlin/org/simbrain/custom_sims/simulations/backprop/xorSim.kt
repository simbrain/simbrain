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
        
        A basic demonstration of the backpropagation algorithm using the classic XOR problem. XOR (exclusive or) is a logical function that cannot be solved by a single layer network, making it a good test case for multi-layer neural networks.
        
        # What to Do
        
        1. Double-click on the "XOR" network in the "XOR" network window to open the "Train Network" window
        
        2. In the top area of this window, select the "Iterate training until the stop button is pressed" button to play the simulation
        
        3. The "Sum Squared Error" should decrease as the iterations increase and end up around 0. The goal is to get the error as low as possible
        
        4. Click "Apply current row as input and increment selected row" to check the network's performance
        
        5. As you click through the training data, the pattern on the bottom should be matched by the pattern on the top
        
        # Training Data
        
        The XOR training set consists of:
        - Input (0,0) should produce output 0
        - Input (1,0) should produce output 1  
        - Input (0,1) should produce output 1
        - Input (1,1) should produce output 0
        
        """.trimIndent()
    )

    // Location of the network in the desktop
    withGui {
        place(networkComponent, 0, 0, 700, 700)
    }

}