package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.GlorotNormal
import org.jetbrains.kotlinx.dl.api.core.initializer.Zeros
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.core.optimizer.ClipGradientByValue
import org.jetbrains.kotlinx.dl.dataset.handler.TEST_IMAGES_ARCHIVE
import org.jetbrains.kotlinx.dl.dataset.handler.extractImages
import org.jetbrains.kotlinx.dl.dataset.mnist
import org.simbrain.custom_sims.*
import org.simbrain.network.kotlindl.*
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.toGrayScaleImage
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.world.imageworld.filters.Filter
import org.simbrain.world.imageworld.filters.ThresholdOp

private const val SEED = 12L

/**
 * Create with a deep net simulation
 * TODO: Try to set up a simulation that categorizes images somehow.
 *  Maybe with all or part of mnist.
 */
val deepNetSim = newSim {

    val mainScope = MainScope()

    val (trainingSet, testingSet) = mnist()

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    // TODO: Maybe make it possible to set conv2dlayer props in constructor
    val deepNet = DeepNet(network,
        arrayListOf(
            TFInputLayer(28,28,1),
            TFConv2DLayer().apply {
                nfilters = 6
                activations = Activations.Tanh
                kernelInitializer = GlorotNormal(SEED)
                biasInitializer = Zeros()
            },
            TFAvgPool2DLayer().apply {
                poolSize = intArrayOf(1, 2, 2, 1)  // 2x2 pool filters
                strides = intArrayOf(1, 2, 2, 1)   // moving  2 steps at a time
                padding = ConvPadding.VALID
            },
            TFConv2DLayer().apply {
                nfilters = 16
                activations = Activations.Tanh
                kernelInitializer = GlorotNormal(SEED)
                biasInitializer = Zeros()
            },
            TFAvgPool2DLayer().apply {
                poolSize = intArrayOf(1, 2, 2, 1)
                strides = intArrayOf(1, 2, 2, 1)
                padding = ConvPadding.VALID
            },
            TFFlattenLayer(),
            TFDenseLayer(120).apply {
                activations = Activations.Tanh
            },
            TFDenseLayer(84).apply {
                activations = Activations.Tanh
            },
            TFDenseLayer(10).apply {
                activations = Activations.Linear
            }
        ),
        4
    )
    network.addNetworkModel(deepNet)
    deepNet.trainingDataset = trainingSet
    deepNet.testingDataset = testingSet
    deepNet.trainingParams.apply {
        epochs = 10
    }
    deepNet.optimizerParams.apply {
        optimizerWrapper.optimizer = Adam(clipGradient = ClipGradientByValue(0.1f))
        lossFunction = Losses.SOFT_MAX_CROSS_ENTROPY_WITH_LOGITS
        metric = Metrics.ACCURACY
    }
    deepNet.buildNetwork()
    deepNet.train(1000, 1000)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    val iwc = addImageWorld("Image World")
    placeComponent(iwc,166,9,500,405)
    val world = iwc.world
    // world.setCurrentFilter("Threshold 10x10")

    val threshold400 = Filter("Threshold 20x20", world.imageAlbum, ThresholdOp(), 28, 28)
    world.filterCollection.addFilter(threshold400)
    world.filterCollection.currentFilter = threshold400

    // TODO: Performance issues..
    mainScope.launch {
        val progressWindow = ProgressWindow(1000, "Images Loaded")
        progressWindow.setUpdateAction(0) { i ->
            progressWindow.value = i
            progressWindow.text = "Extracted $i/1000 images"
        }

        progressWindow.text = "Extracted 0/1000 images"
        progressWindow.pack()
        launch(Dispatchers.Default) {
            extractImages("cache/$TEST_IMAGES_ARCHIVE")
                .take(1000)
                .map {it.toGrayScaleImage(28,28)}
                .forEachIndexed { i, it ->
                    world.imageAlbum.addImage(it)
                    progressWindow.invokeUpdateAction(i)
                }
            progressWindow.close()
        }
    }

    withGui {
        place(iwc) {
            location = point(410, 0)
            width = 400
            height = 400
        }
    }

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        world.currentFilter couple deepNet
    }

}