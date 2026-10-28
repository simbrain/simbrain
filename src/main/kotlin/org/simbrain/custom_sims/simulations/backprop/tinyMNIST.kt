package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.custom_sims.simulations.backprop.TinyMnistModel
import org.simbrain.custom_sims.simulations.backprop.createTinyMnistControlPanel
import org.simbrain.custom_sims.simulations.backprop.loadTinyMnist
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.place
import org.simbrain.util.placeBeside
import org.simbrain.util.toMatrix
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.world.imageworld.ImageWorldComponent
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import javax.swing.SwingUtilities

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

    val data = loadTinyMnist() ?: return@newSim
    bp.trainingSet = data.trainingSet
    bp.testingSet = data.testingSet
    bp.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
    bp.trainerConfig.learningRate = .001
    bp.trainerConfig.updateType = SupervisedTrainer.UpdateMethod.Batch(35)
    bp.trainerConfig.computeAccuracy = true
    bp.initBiases()
    bp.initWeights()

    bp.inputLayer.gridMode = true
    bp.inputLayer.offset(-350.0, -225.0)
    bp.inputLayer.inputData = data.testingSet.inputs.toMatrix()
    bp.outputLayer.offset(-350.0, 225.0)
    bp.outputLayer.circleMode = true
    bp.outputLayer.gridMode = true
    bp.outputLayer.labelArray = Array(10) { "$it" }

    net.addNetworkModels(bp)

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

        # Control Panel

        - `Train network...`: Opens the training dialog. This is a convenience; right-clicking the network and choosing `Train...` opens the same dialog.

        - `Training examples`: Train on the first `10,000`, `1,000`, or `300` training images. With fewer examples the network memorizes the training set, so training error falls well below testing error (overfitting).

        - `Testing set`: Test on the original images, or on copies shifted `1` or `2` pixels, rotated `20` degrees, or with added noise. This sets the blue test curve, the `Testing data` tab, and the `Inputs` table.

        - `Random training image` and `Random test image`: Run a randomly chosen training or test image through the network so you can see how it classifies it.

        - `Draw your own digit`: Opens an image world coupled to the input layer. Draw a digit, then press `Run` or `Step` to see the network's answer.

        - `Test generalization`: Reports accuracy on the training set and on every testing set. The progress bar below it shows how far along it is. Stop the simulation first if it is running.

        # What to Do

        1. Open the training dialog with `Train network...` in the control panel, or by right-clicking the `BackpropNetwork_1` network (under the `Tiny Mnist` window) and selecting `Train...`

        2. Train the simulation by clicking the `Iterate training until stop button is pressed` button

        3. Train this simulation until the `Training Error` reaches below `0.2` or `0.1`. With the default settings it will hover around there. At that point it achieves decent results. The blue line on the graph shows how well the model is generalizing to test data

        Notice that the training error (red line) appears jagged compared to the testing error (blue line). This is because training uses batches of 35 examples when updating, while testing (which happens every 10 iterations, and is thus not visible initially) evaluates the entire test set. See the [docs on supervised learning](https://docs.simbrain.net/docs/network/learning/supervisedLearning.html)

        ## Things You Can Do After Training

        - Press `Random test image` a few times to see how the network classifies images it was not trained on.

        - Manually try specific training or testing images. To do this, under the `Inputs` toolbar, go to the table of interest and click the button with this tooltip: `Apply current row as
        input to network` when hovered. Then observe how it classifies written digits. The values of the output layer correspond to the probability it assigns the input to a digit `0-9`.

        - Draw your own image. Press `Draw your own digit` (or right click on the image layer and select `Add image world`), draw a digit, and see how the network does. It generally does poorly since it
        was trained on anti-aliased images.

        - Press `Test generalization`. Accuracy on the original testing images is high, but it falls quickly when the same digits are shifted a pixel or two. The network has learned which pixels matter, not what shape a digit is, and has no notion that a shifted `7` is still a `7`. Set `Testing set` to a shifted set and press `Random test image` to see examples. Compare the `CNN MNIST` simulation, whose pooling layers tolerate small shifts better.

        - Set `Training examples` to `300`, press `Init parameters` in the training dialog, and train again. Watch the red training curve drop well below the blue testing curve.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        Melissa Almeida

        """.trimIndent()
    )

    withGui {
        val networkPanel = getNetworkPanel(networkComponent)
        var drawingWorld: ImageWorldComponent? = null
        val model = object : TinyMnistModel {
            override var trainingSet: TrainingDataset
                get() = bp.trainingSet
                set(value) {
                    bp.trainingSet = value
                }
            override var testingSet: TrainingDataset
                get() = bp.testingSet
                set(value) {
                    bp.testingSet = value
                    bp.inputLayer.inputData = value.inputs.toMatrix()
                }

            override suspend fun show(image: DoubleArray): DoubleArray {
                bp.inputLayer.setActivations(image)
                with(net) { bp.forwardPass() }
                return bp.outputLayer.activationArray
            }

            override suspend fun openTrainingDialog() {
                SwingUtilities.invokeLater {
                    with(networkPanel) { bp.getSupervisedTrainingDialog() }.run {
                        pack()
                        setLocationRelativeTo(null)
                        isVisible = true
                    }
                }
            }

            override suspend fun addDrawingWorld() {
                if (drawingWorld?.let { it in workspace.componentList } == true) return
                val imageWorld = ImageWorldComponent("Draw a digit")
                workspace.addWorkspaceComponent(imageWorld)
                imageWorld.world.resetImageAlbum(20, 20)
                with(workspace.couplingManager) {
                    imageWorld.world.imagePipelineCollection.currentPipeline
                        .getProducer(ImageProcessingPipeline::brightness) couple
                        bp.inputLayer.getConsumer(NeuronArray::activationArray)
                }
                bp.inputLayer.isClamped = true
                placeBeside(imageWorld, networkComponent)
                drawingWorld = imageWorld
            }
        }
        val controlPanel = createTinyMnistControlPanel(data, model).awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 700, 700)
    }

    val trainer = SupervisedTrainer(net, bp)
    // Iterate trainer and network once to get it to display a number in the input
    trainer.trainOnce()
    net.update()

}
