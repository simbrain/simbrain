package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.addDocViewer
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.csvToDouble2DArray
import org.simbrain.util.fetchDataWithCache
import org.simbrain.util.place
import org.simbrain.util.toMatrix

/**
 * A small implementation of MNIst
 *
 * @author Melissa Almeida
 * @author Jeff Yoshimi
 */
val tinyMNIST = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Tiny Mnist")
    val net = networkComponent.network

    val bp = BackpropNetwork(intArrayOf(400, 150, 100, 10))
    bp.hiddenLayers().forEach{layer ->
        layer.updateRule = LinearRule().apply{clippingType = LinearRule.ClippingType.Relu}}
    bp.outputLayer.updateRule = SoftmaxRule()

    val trainInputsCSV = fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_train_inputs.csv")?:return@newSim
    val trainLabelsCSV = fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_train_labels.csv")?:return@newSim
    val testInputsCSV = fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_test_inputs.csv")?:return@newSim
    val testLabelsCSV = fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_test_labels.csv")?:return@newSim

    bp.trainingSet = MatrixDataset(
        inputs = csvToDouble2DArray(trainInputsCSV).toMatrix(),
        targets = csvToDouble2DArray(trainLabelsCSV).toMatrix(),
    )
    bp.testingSet = MatrixDataset(
        inputs = csvToDouble2DArray(testInputsCSV).toMatrix(),
        targets = csvToDouble2DArray(testLabelsCSV).toMatrix(),
    )
    bp.trainer.lossFunction = BackpropLossFunction.CrossEntropy
    bp.trainer.learningRate = .001
    bp.trainer.updateType = SupervisedTrainer.UpdateMethod.Batch(35)
    bp.initBiases()
    bp.initWeights()

    bp.inputLayer.gridMode = true
    bp.inputLayer.offset(-350.0, -225.0)
    bp.inputLayer.inputData = csvToDouble2DArray(testInputsCSV).toMatrix()
    bp.outputLayer.offset(-350.0, 225.0)
    bp.outputLayer.circleMode = true
    bp.outputLayer.gridMode = true
    bp.outputLayer.labelArray = Array(10) {"$it"}

    net.addNetworkModels(bp).awaitAll()

    val docViewer = addDocViewer(
        "Information",
        """ 
        # Introduction
        MNIST (Modified National Institute of Standards and Technology) data base is a large dataset of 70,000 28x28 pixel grayscale images of handwritten digits 0-9. It consists of 60,000 training images and 10,000 testing images,  used in machine learning and image processing. It provides a reliable benchmark for developing and testing models, like neural networks, in classification. 
        
        The Tint MNIST simulation trains the network to recognize digits. This simulation takes an image of the network as input, and learns to recognize and identify what digit between 0 and 9 it is. 
        
        For more information, read more [here] (https://en.wikipedia.org/wiki/MNIST_database).
        
        # What to Do
        1. Enter the "Train Network" window by right-clicking the "BackpropNetwork_1" network (under the "Tiny Mnist" window) and selecting "Edit/ Tain Backprop...".
        2. Train the simulation by clicking the Play button.
            This button should display "Iterate training until stop button is pressed" when hovered over.
        3. Train this simulation until the "Mean Error" reaches below 0.1.
            Ideally, the error should hover near 0.02.
        4. Under the "Inputs" toolbar, select the Next button. 
            This button should display "Apply current row as input to network" when hovered 
        5. Click "Ok" to exit the "Train Network" window and return to the "Tiny Mnist" network.
        6.  Observe how it classifies written digits
            The values of the output layer correspond to the probability it assigns the input to a digit 0-9. 
        
    """.trimIndent()
    )

    // Location of the network in the desktop
    withGui {
        place(networkComponent, 515, 0, 700, 700)
        place(docViewer, 0, 0, 516, 700)
    }

    // Iterate trainer and network once to get it to display a number in the input
    with(net) { with(bp) {
        bp.trainer.trainOnce()
        net.update()
    } }

//    // Adding a docviewer
//    val docViewer = addDocViewer(
//        "Information",
//        """
//            # MNist
//            In this demo train a net to learn MNIST. [Info on what it is, how to run sim, etc.]
//        """.trimIndent()
//    )
//    withGui {
//        place(docViewer, 784, 3, 400, 400)
//    }

}