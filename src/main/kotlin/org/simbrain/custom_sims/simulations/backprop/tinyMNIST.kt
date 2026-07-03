package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.Layer
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.Probe
import org.simbrain.network.trainers.ProbeCreator
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.trainers.createProbe
import org.simbrain.network.trainers.harvestActivations
import org.simbrain.network.trainers.harvestedDataset
import org.simbrain.network.trainers.majorityClassProportion
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.createAction
import org.simbrain.util.createEditorDialog
import org.simbrain.util.csvToDouble2DArray
import org.simbrain.util.display
import org.simbrain.util.fetchDataWithCache
import org.simbrain.util.place
import org.simbrain.util.point
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

    // Linear probes: does the current digit contain a loop (0, 6, 8, 9)?
    val loopDigits = setOf(0, 6, 8, 9)

    fun loopTargets(labelRows: List<List<Double>>) = labelRows.map { row ->
        val digit = row.withIndex().maxBy { it.value }.index
        if (digit in loopDigits) mutableListOf(0.0, 1.0) else mutableListOf(1.0, 0.0)
    }.toMutableList()

    val probes = mutableListOf<Probe>()

    // Probes train on activations harvested by running the host over its dataset. Harvests go stale
    // whenever the host is (re)trained.
    fun harvestFor(probe: Probe, probedLayer: Layer) = with(net) {
        probe.trainingSet = harvestedDataset(
            bp.harvestActivations(probedLayer, bp.trainingSet.inputs),
            loopTargets(bp.trainingSet.targets)
        )
        probe.testingSet = harvestedDataset(
            bp.harvestActivations(probedLayer, bp.testingSet.inputs),
            loopTargets(bp.testingSet.targets)
        )
        probe.stale = false
    }

    suspend fun rebuildProbeDatasets() = probes.forEach { it.rebuildDataset() }

    fun addLoopProbe(probedLayer: Layer, label: String, hiddenSizes: List<Int> = emptyList()) = with(net) {
        val probe = createProbe(
            probedLayer = probedLayer,
            readoutSize = 2,
            readoutLabels = arrayOf("No loop", "Loop"),
            hiddenSizes = hiddenSizes,
            label = label,
            offset = point(550.0, probes.count { it.probedModel === probedLayer } * 300.0),
        ).apply {
            trainerConfig.learningRate = .001
            trainerConfig.updateType = SupervisedTrainer.UpdateMethod.Batch(35)
            trainerConfig.computeAccuracy = true
            trainerConfig.testConfiguration.enabled = true
            trainerConfig.testConfiguration.testFrequency = 10
            targetDescription = "Has loop: digit in {0, 6, 8, 9}, derived from MNIST labels"
            datasetRebuilder = { harvestFor(this, probedLayer) }
        }
        probes += probe
        harvestFor(probe, probedLayer)
        probe
    }

    addLoopProbe(bp.hiddenLayers().first(), "Loop probe")

    bp.hiddenLayers().forEachIndexed { i, layer ->
        layer.customContextMenuActions += createAction(name = "Add loop probe...") {
            val creator = ProbeCreator("Loop probe (hidden ${i + 1})")
            creator.createEditorDialog("Add Probe") {
                addLoopProbe(layer, creator.label, creator.parseHiddenSizes())
            }.display()
        }
    }

    val majorityBaseline = majorityClassProportion(loopTargets(bp.trainingSet.targets))

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

        1. Train the digit network first (see above). The probe's tab shows `(stale)` once the digit network's weights change, since the harvested activations no longer match
        2. Right-click the `Loop probe` outline and select `Rebuild Probe Dataset` (or click `Rebuild probe datasets` in the `Loop Probe` panel) to re-harvest
        3. Right-click the `Loop probe` outline and select `Train...`, then iterate training

        Compare the probe's accuracy to the majority baseline shown in the `Loop Probe` panel (always guessing "no loop"); `Probe Info...` in the probe's right-click menu shows the
        same baselines. Accuracy well above baseline means loop information is linearly decodable from the hidden layer. Try training the probe *before* training the digit network to
        see how decodable the information is from a random projection.

        You can add more probes: right-click either hidden layer and select `Add loop probe...`. Comparing probe accuracy on the first vs. second hidden layer shows how loop
        information changes across depth. Leaving `Hidden layer sizes` empty keeps the probe linear; adding hidden layers gives the probe more capacity, but then success may
        reflect the probe's own computation rather than what the layer encodes.

        To check for probe memorization, right-click the probe and select `Add Shuffled-Label Control`: a second probe with the same architecture and shuffled targets. If the control
        also beats baseline, the original probe's accuracy reflects its own capacity, not information in the layer. A linear probe's control should stay near baseline.

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
            addButton("Rebuild probe datasets") {
                rebuildProbeDatasets()
            }
        }
    }

    val trainer = SupervisedTrainer(net, bp)
    // Iterate trainer and network once to get it to display a number in the input
    trainer.trainOnce()
    net.update()

}
