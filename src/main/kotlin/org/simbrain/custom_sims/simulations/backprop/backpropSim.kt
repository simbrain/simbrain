package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.place
import smile.math.matrix.Matrix


val backpropSim = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Backprop")
    val net = networkComponent.network

    val bp = BackpropNetwork(intArrayOf(10, 7, 10))
    net.addNetworkModels(bp)

    bp.trainingSet = MatrixDataset(
        inputs = Matrix.eye(10),
        targets = Matrix.eye(10)
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