package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.place
import smile.math.matrix.Matrix


val xorSim = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("XOR")
    val net = networkComponent.network

    val inputLayer = NeuronGroup(2).apply {
        isClamped = true
    }
    val hiddenLayer = NeuronGroup(4).apply {
        updateRule = SigmoidalRule()
    }
    val outputLayer = NeuronGroup(1)
    val wm1 = WeightMatrix(inputLayer, hiddenLayer)
    val wm2 = WeightMatrix(hiddenLayer, outputLayer)
    val sm = SupervisedModel(inputLayer, outputLayer)
    net.addNetworkModels(inputLayer, hiddenLayer, outputLayer, wm1, wm2, sm).awaitAll()
    offsetNetworkModel(inputLayer, hiddenLayer, Direction.NORTH, 250.0)
    offsetNetworkModel(hiddenLayer, outputLayer, Direction.NORTH, 250.0)
    alignNetworkModels(inputLayer, hiddenLayer, Alignment.VERTICAL)
    alignNetworkModels(inputLayer, outputLayer, Alignment.VERTICAL)

    sm.trainingSet = MatrixDataset(
        inputs = Matrix.of(
            arrayOf(
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, 1.0),
                doubleArrayOf(1.0, 1.0),
            )
        ),
        targets = Matrix.of(
            arrayOf(
                doubleArrayOf(0.0),
                doubleArrayOf(1.0),
                doubleArrayOf(1.0),
                doubleArrayOf(0.0),
            )
        ),
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

//    // Adding a docviewer
//    val docViewer = addDocViewer(
//        "Information",
//        """
//            # Projection demo
//            In this demo simply run the simulations and observe how the network activations are projected.
//            Some things you can try:
//            - Randomize the weight matrix by clicking on it and pressing "r" to randomize
//            - Changing the type of projection, for example changing it to `Sammon` and pressing `play` to see an alternative way of projecting data
//            - At any time you can press the `clear` button (the eraser) in the projection plot to start over with your plot
//        """.trimIndent()
//    )
//    withGui {
//        place(docViewer, 784, 3, 400, 400)
//    }

}