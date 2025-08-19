package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.TrainingDataset
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
    bp.hiddenLayers().forEach { layer ->
        layer.updateRule = LinearRule().apply { clippingType = LinearRule.ClippingType.Relu }
    }
    bp.outputLayer.updateRule = SoftmaxRule()

    val trainInputsCSV =
        fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_train_inputs.csv") ?: return@newSim
    val trainLabelsCSV =
        fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_train_labels.csv") ?: return@newSim
    val testInputsCSV =
        fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_test_inputs.csv") ?: return@newSim
    val testLabelsCSV =
        fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_test_labels.csv") ?: return@newSim

    bp.trainingSet = TrainingDataset(
        inputs = csvToDouble2DArray(trainInputsCSV).map { it.toMutableList() }.toMutableList(),
        targets = csvToDouble2DArray(trainLabelsCSV).map { it.toMutableList() }.toMutableList(),
    )
    bp.testingSet = TrainingDataset(
        inputs = csvToDouble2DArray(testInputsCSV).map { it.toMutableList() }.toMutableList(),
        targets = csvToDouble2DArray(testLabelsCSV).map { it.toMutableList() }.toMutableList(),
    )
    bp.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
    bp.trainerConfig.learningRate = .001
    bp.trainerConfig.updateType = SupervisedTrainer.UpdateMethod.Batch(35)
    bp.initBiases()
    bp.initWeights()

    bp.inputLayer.gridMode = true
    bp.inputLayer.offset(-350.0, -225.0)
    bp.inputLayer.inputData = csvToDouble2DArray(testInputsCSV).toMatrix()
    bp.outputLayer.offset(-350.0, 225.0)
    bp.outputLayer.circleMode = true
    bp.outputLayer.gridMode = true
    bp.outputLayer.labelArray = Array(10) { "$it" }

    net.addNetworkModels(bp)

    addSidebarInfo(
        """ 
        # Introduction
        The [MNIST](https://en.wikipedia.org/wiki/MNIST_database) (Modified National Institute of Standards and Technology) 
        data base is a dataset of 70,000 28x28 pixel grayscale images of handwritten digits 0-9. 
        It consists of 60,000 training images and 10,000testing images, used in machine learning and image processing. 
        It provides a reliable benchmark for developing and testing models, like neural networks, in classification.
         
        This is a subsampling of the dataset down to 10,000 training and 1,000 20 x 20 images,  thanks to Melissa Almeida.
        
        The simulation trains the network to recognize digits. This simulation takes an image of the network as input, and learns to recognize and identify what digit between 0 and 9 it is. 
       
        # How to train the network
        1. Enter the "Train Network" dialog by right-clicking the "BackpropNetwork_1" network (under the "Tiny Mnist" window) and selecting "Edit/ Tain Backprop...".
        2. Train the simulation by clicking the Play button.
            This button should display "Iterate training until stop button is pressed" when hovered over.
        3. Train this simulation until the "Mean Error" reaches below 0.2 or 0.1. With the default settings it will hover around there.
            At that point it achieves decent results. The blue line on the graph shows how well the model is generalizing to test data.
        
        # Things you can do after training
        - Manually try specific training or testing images. To do this, under the "Inputs" toolbar, 
            Go to the table of interest and click the button with this tooltip: `Apply current row as input to network` when hovered 
            Then observe how it classifies written digits
            The values of the output layer correspond to the probability it assigns the input to a digit 0-9. 
        - Draw your own image. Right click on the image layer and select `Add coupled image world` then draw your own image and see how the network does. 
          It generally does poorly since it was trained on anti-aliased images. 
    """.trimIndent()
    )

    // Location of the network in the desktop
    withGui {
        place(networkComponent, 0, 0, 700, 700)
    }

    val trainer = SupervisedTrainer(net, bp)
    // Iterate trainer and network once to get it to display a number in the input
    trainer.trainOnce()
    net.update()

}