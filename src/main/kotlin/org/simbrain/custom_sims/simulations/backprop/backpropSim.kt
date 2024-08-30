package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addDocViewer
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.place
import smile.math.matrix.Matrix


val backpropSim = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Backprop")
    val net = networkComponent.network

    val inputSize = 10
    val latentSize = 4

    val bp = BackpropNetwork(intArrayOf(inputSize, latentSize, inputSize)).apply {
        outputLayer.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.LOGISTIC
        }
        trainer.learningRate = .1
    }
    net.addNetworkModels(bp)

    bp.trainingSet = MatrixDataset(
        inputs = Matrix.eye(inputSize),
        targets = Matrix.eye(inputSize)
    )

    val docViewer = addDocViewer(
        "Information",
        """ 
            # Introduction
            Backpropagation is an algorithm for supervised learning using gradient descent. This is an auto-encoder where the network must learn to associate each input with an identical output. The trick is that it must do so by compressing the input to a smaller hidden layer.  The smaller the hidden layer, the harder the task. 
            
            # What to Do
            Double-click on the “Backprop” network in the “Backprop” network window to open the “Train Network” window.
            In the top area of this window, select the “Iterate training until the stop button is pressed” button to play the simulation. 
            The “Sum Squared Error” should decrease as the iterations increase and end up around 0. The goal is to get the error as low as possible.
            Click “Apply current row as input and increment selected row” to check the network's performance. 
            As you click the pattern on the bottom should be matched by the pattern on the top.
            
            # Further studies
            By default this kind of auto-encoding training set is used for networks, so it’s easy to set up this kind of simulation with different sized hidden layers, or different numbers of hidden layers. You can add a new network using `insert > subnetwork > backprop` and use different numbers for the hidden layer, and then follow the directions above.  As the hidden layer gets smaller, it should become harder to train.
            
            # Also see
            This is a very simple example of an auto-encode, which is related to variational auto-encoders [https://en.wikipedia.org/wiki/Variational_autoencoder]   
        
        """.trimIndent()
    )

    // Location of the network in the desktop
    withGui {
        place(docViewer, 0, 0, 450, 700)
        place(networkComponent,460, 0, 700, 700)
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