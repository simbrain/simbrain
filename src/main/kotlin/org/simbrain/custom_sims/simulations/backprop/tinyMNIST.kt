package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.SupervisedModel
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
    bp.trainerConfig.computeAccuracy = true
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

    // Linear probe on the first hidden layer: does the current digit contain a loop (0, 6, 8, 9)?
    val loopDigits = setOf(0, 6, 8, 9)
    val probedLayer = bp.hiddenLayers().first()

    val probeReadout = NeuronArray(2).apply {
        label = "Probe readout"
        updateRule = SoftmaxRule()
        gridMode = true
        labelArray = arrayOf("No loop", "Loop")
    }
    probeReadout.setLocation(probedLayer.locationX + 550, probedLayer.locationY)
    val probeWeights = WeightMatrix(probedLayer, probeReadout)
    val probe = SupervisedModel(probedLayer, probeReadout).apply {
        label = "Loop probe"
        trainerConfig.learningRate = .001
        trainerConfig.updateType = SupervisedTrainer.UpdateMethod.Batch(35)
        trainerConfig.computeAccuracy = true
        trainerConfig.testConfiguration.enabled = true
        trainerConfig.testConfiguration.testFrequency = 10
    }
    net.addNetworkModels(probeReadout, probeWeights, probe)

    fun loopTargets(labelRows: List<List<Double>>) = labelRows.map { row ->
        val digit = row.withIndex().maxBy { it.value }.index
        if (digit in loopDigits) mutableListOf(0.0, 1.0) else mutableListOf(1.0, 0.0)
    }.toMutableList()

    // The probe is trained on the host's hidden layer activations, harvested by running the host
    // over its dataset. Must be re-run after the host is (re)trained or the harvest is stale.
    fun harvestProbeDataset(hostData: TrainingDataset) = with(net) {
        val hiddenActivations = hostData.inputs.map { row ->
            bp.inputLayer.setActivations(row.toDoubleArray())
            bp.forwardPass()
            probedLayer.activationArray.toMutableList()
        }.toMutableList()
        TrainingDataset(
            inputs = hiddenActivations,
            targets = loopTargets(hostData.targets),
            inputSize = probedLayer.size,
            targetSize = 2
        )
    }

    fun rebuildProbeDatasets() {
        probe.trainingSet = harvestProbeDataset(bp.trainingSet)
        probe.testingSet = harvestProbeDataset(bp.testingSet)
    }
    rebuildProbeDatasets()

    val loopFraction = loopTargets(bp.trainingSet.targets).count { it[1] == 1.0 }.toDouble() / bp.trainingSet.size
    val majorityBaseline = maxOf(loopFraction, 1 - loopFraction)

    addSidebarInfo(
        """
        # Tiny MNIST

        The [MNIST](https://en.wikipedia.org/wiki/MNIST_database) (Modified National Institute of Standards and Technology) database is a dataset of `70,000` `28x28` pixel grayscale images of
        handwritten digits `0-9`. It consists of `60,000` training images and `10,000` testing images, used in machine learning and image processing. It provides a reliable benchmark for developing
        and testing models, like neural networks, in classification.

        This is a subsampling of the dataset down to `10,000` training and `1,000` `20 x 20` images, thanks to Melissa Almeida.

        The simulation trains the network to recognize digits. This simulation takes an image of the network as input, and learns to recognize and identify what digit between `0` and `9` it is.

        # Simulation Details

        Training uses the smaller `20 x 20` images in the bundled Tiny MNIST dataset, with separate training and testing examples so you can compare learning and generalization.

        # What to Do

        1. Enter the `Train Network` dialog by right-clicking the `BackpropNetwork_1` network (under the `Tiny Mnist` window) and selecting `Edit/Train Backprop...`

        2. Train the simulation by clicking the `Iterate training until stop button is pressed` button

        3. Train this simulation until the `Mean Error` reaches below `0.2` or `0.1`. With the default settings it will hover around there. At that point it achieves decent results. The blue line on the graph shows how well the model is generalizing to test data

        Notice that the training error (red line) appears jagged compared to the testing error (blue line). This is because training uses batches of 35 examples when updating, while testing (which happens every 10 iterations, and is thus not visible initially) evaluates the entire test set. See the [docs on supervised learning](https://docs.simbrain.net/docs/network/learning/supervisedLearning.html)

        ## Things You Can Do After Training

        - Manually try specific training or testing images. To do this, under the `Inputs` toolbar, go to the table of interest and click the button with this tooltip: `Apply current row as
        input to network` when hovered. Then observe how it classifies written digits. The values of the output layer correspond to the probability it assigns the input to a digit `0-9`.

        - Draw your own image. Right click on the image layer and select `Add coupled image world` then draw your own image and see how the network does. It generally does poorly since it
        was trained on anti-aliased images.

        # Linear Probe

        A [linear probe](https://en.wikipedia.org/wiki/Probing_(machine_learning)) is a simple classifier trained on a hidden layer's activations to test what information that layer
        represents. Here the probe (`Loop probe` on the right) reads the first hidden layer and predicts whether the current digit contains a loop (`0`, `6`, `8`, `9`).

        The probe is trained on *harvested* activations: the digit network is run over its dataset and the hidden layer's activations are recorded as the probe's inputs. Training the
        probe never changes the digit network's weights.

        1. Train the digit network first (see above)
        2. Click `Rebuild probe dataset` in the `Loop Probe` panel — the harvested activations are stale whenever the digit network is retrained
        3. Right-click the `Loop probe` outline and select `Train...`, then iterate training

        Compare the probe's accuracy to the majority baseline shown in the `Loop Probe` panel (always guessing "no loop"). Accuracy well above baseline means loop information is
        linearly decodable from the hidden layer. Try training the probe *before* training the digit network to see how decodable the information is from a random projection.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        Melissa Almeida

        """.trimIndent()
    )

    // Location of the network in the desktop
    withGui {
        place(networkComponent, 0, 0, 700, 700)
        createControlPanel("Loop Probe", 710, 0) {
            addLabelledText("Majority baseline", "${(majorityBaseline * 100).let { "%.1f".format(it) }}%")
            addButton("Rebuild probe dataset") {
                rebuildProbeDatasets()
            }
        }
    }

    val trainer = SupervisedTrainer(net, bp)
    // Iterate trainer and network once to get it to display a number in the input
    trainer.trainOnce()
    net.update()

}
