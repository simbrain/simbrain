package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.activations
import org.simbrain.network.core.auxValues
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import org.simbrain.workspace.updater.updateAction
import kotlin.math.sqrt

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

    val trainInputsCSV = fetchDataFromUrl("https://downloads.simbrain.net/simbraindata/tiny_mnist_train_inputs.csv")?:return@newSim
    val trainLabelsCSV = fetchDataFromUrl("https://downloads.simbrain.net/simbraindata/tiny_mnist_train_labels.csv")?:return@newSim
    val testInputsCSV = fetchDataFromUrl("https://downloads.simbrain.net/simbraindata/tiny_mnist_test_inputs.csv")?:return@newSim

    bp.trainingSet = MatrixDataset(
        inputs = csvToDouble2DArray(trainInputsCSV).toMatrix(),
        targets = csvToDouble2DArray(trainLabelsCSV).toMatrix(),
    )
    bp.trainer.lossFunction = BackpropLossFunction.CrossEntropy
    bp.trainer.learningRate = .001
    bp.trainer.updateType = SupervisedTrainer.UpdateMethod.Batch(35)

    bp.inputLayer.gridMode = true
    bp.inputLayer.offset(-300.0, -250.0)
    bp.inputLayer.inputData = csvToDouble2DArray(testInputsCSV).toMatrix()
    bp.outputLayer.offset(-300.0, 230.0)

    net.addNetworkModels(bp).awaitAll()

    // Location of the network in the desktop
    withGui {
        place(networkComponent, 0, 0, 700, 700)
    }

    // Label
    val outputClassLabel = NetworkTextObject().apply {
        fontSize = 18
    }
    net.addNetworkModel(outputClassLabel)?.await()
    outputClassLabel.location = point(-77, -175)

    // Update the text label
    net.addUpdateAction(updateAction("Update class label") {
        outputClassLabel.text = "Classification: ${bp.outputLayer
            .activationArray.withIndex().maxByOrNull {it.value}?.index}"
    })

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