package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.updaterules.SoftmaxRule
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities
import kotlin.random.Random

class CnnLayoutSnapshot : UiSnapshotDef {

    override val name = "cnn_layout"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("CNN layout snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(700, 900)
        }

        runBlocking {
            val leftX = -275.0
            val rightX = 275.0
            val inputY = -446.0
            val conv1Y = 24.0
            val pool1Y = 449.0
            val conv2Y = 446.0
            val pool2Y = 144.0
            val flatY = -125.0
            val outputY = -442.0

            val inputShape = TensorShape(20, 20, 1)
            val inputTensorLayer = TensorLayer(inputShape).apply {
                label = "Input (20x20x1)"
                isClamped = true
                activations = DoubleArray(inputShape.size) { Random.nextDouble() }
            }
            inputTensorLayer.setLocation(leftX, inputY)

            val conv1OutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 5)
            val conv1Out = TensorLayer(conv1OutShape).apply {
                label = "Conv1 ($conv1OutShape)"
                activationFunction = TensorActivation.RELU
                activations = DoubleArray(conv1OutShape.size) { Random.nextDouble() }
            }
            conv1Out.setLocation(leftX, conv1Y)
            ConvolutionConnector(inputTensorLayer, conv1Out, kernelSize = 3, numFilters = 5, stride = 1, padding = Padding.SAME)

            val pool1OutShape = conv1OutShape.poolOutputShape(2, 2)
            val pool1Out = TensorLayer(pool1OutShape).apply {
                label = "Pool1 ($pool1OutShape)"
                activations = DoubleArray(pool1OutShape.size) { Random.nextDouble() }
            }
            pool1Out.setLocation(leftX, pool1Y)
            PoolingConnector(conv1Out, pool1Out, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

            val conv2OutShape = pool1OutShape.convOutputShape(3, 1, Padding.SAME, 8)
            val conv2Out = TensorLayer(conv2OutShape).apply {
                label = "Conv2 ($conv2OutShape)"
                activationFunction = TensorActivation.RELU
                activations = DoubleArray(conv2OutShape.size) { Random.nextDouble() }
            }
            conv2Out.setLocation(rightX, conv2Y)
            ConvolutionConnector(pool1Out, conv2Out, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME)

            val pool2OutShape = conv2OutShape.poolOutputShape(2, 2)
            val pool2Out = TensorLayer(pool2OutShape).apply {
                label = "Pool2 ($pool2OutShape)"
                activations = DoubleArray(pool2OutShape.size) { Random.nextDouble() }
            }
            pool2Out.setLocation(rightX, pool2Y)
            PoolingConnector(conv2Out, pool2Out, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

            val flatArray = NeuronArray(pool2OutShape.size).apply {
                label = "Flatten (${pool2OutShape.size})"
                activationArray = DoubleArray(pool2OutShape.size) { Random.nextDouble() }
            }
            flatArray.setLocation(rightX, flatY)
            FlattenConnector(pool2Out, flatArray)

            val outputArray = NeuronArray(10).apply {
                label = "Output (10)"
                updateRule = SoftmaxRule()
                circleMode = true
                gridMode = true
                labelArray = Array(10) { "$it" }
            }
            outputArray.setLocation(rightX, outputY)
            WeightMatrix(flatArray, outputArray)

            network.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray) {
                label = "CNN MNIST"
            }
        }

        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            network.events.zoomToFitPage.fire()
        }

        return panel
    }
}
