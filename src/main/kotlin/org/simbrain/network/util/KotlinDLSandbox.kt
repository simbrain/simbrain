package org.simbrain.network.util

import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.Constant
import org.jetbrains.kotlinx.dl.api.core.initializer.GlorotNormal
import org.jetbrains.kotlinx.dl.api.core.initializer.Zeros
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.Conv2D
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.layer.pooling.AvgPool2D
import org.jetbrains.kotlinx.dl.api.core.layer.reshaping.Flatten
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.core.optimizer.ClipGradientByValue
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import org.jetbrains.kotlinx.dl.dataset.embedded.NUMBER_OF_CLASSES
import org.jetbrains.kotlinx.dl.dataset.embedded.mnist

private const val EPOCHS = 3
private const val TRAINING_BATCH_SIZE = 1000
private const val NUM_CHANNELS = 1L
private const val IMAGE_SIZE = 28L
private const val SEED = 12L
private const val TEST_BATCH_SIZE = 1000

// TODO: public is temporary
public val lenet5Classic = Sequential.of(
    Input(
        IMAGE_SIZE,
        IMAGE_SIZE,
        NUM_CHANNELS
    ),
    Conv2D(
        filters = 6,
        kernelSize = intArrayOf(5, 5),
        //  batch, height, width, channels
        strides = intArrayOf(1, 1, 1, 1),
        activation = Activations.Tanh,
        kernelInitializer = GlorotNormal(SEED),
        biasInitializer = Zeros(),
        padding = ConvPadding.SAME
    ),
    AvgPool2D(
        poolSize = intArrayOf(1, 2, 2, 1),  // 2x2 pool filters
        strides = intArrayOf(1, 2, 2, 1),   // moving  2 steps at a time
        padding = ConvPadding.VALID
    ),
    Conv2D(
        filters = 16,
        kernelSize = intArrayOf(5, 5),
        strides = intArrayOf(1, 1, 1, 1),
        activation = Activations.Tanh,
        kernelInitializer = GlorotNormal(SEED),
        biasInitializer = Zeros(),
        padding = ConvPadding.SAME
    ),
    AvgPool2D(
        poolSize = intArrayOf(1, 2, 2, 1),
        strides = intArrayOf(1, 2, 2, 1),
        padding = ConvPadding.VALID
    ),
    Flatten(), // 3136
    Dense(
        outputSize = 120,
        activation = Activations.Tanh,
        kernelInitializer = GlorotNormal(SEED),
        biasInitializer = Constant(0.1f)
    ),
    Dense(
        outputSize = 84,
        activation = Activations.Tanh,
        kernelInitializer = GlorotNormal(SEED),
        biasInitializer = Constant(0.1f)
    ),
    Dense(
        outputSize = NUMBER_OF_CLASSES,
        activation = Activations.Linear,
        kernelInitializer = GlorotNormal(SEED),
        biasInitializer = Constant(0.1f)
    )
)

fun testDataset() {

    var dataset = OnHeapDataset.create(
        arrayOf(floatArrayOf(1f, 2f), floatArrayOf(3f, 4f)),
        floatArrayOf(1f,2f))

    for (i in 0 until dataset.xSize()) {
        println("input: ${dataset.getX(i).contentToString()} target: ${dataset.getY(i)}")
    }

}

fun main() {

    // testDataset()

    val (train, test) = mnist()

    lenet5Classic.use {
        it.compile(
            optimizer = Adam(clipGradient = ClipGradientByValue(0.1f)),
            loss = Losses.SOFT_MAX_CROSS_ENTROPY_WITH_LOGITS,
            metric = Metrics.ACCURACY
        )

        it.summary()
        println(it.layers.map { l -> l.outputShape.rank() }.joinToString(","))
         it.init()
        //
        // // Studying prediction functions. Run these before training to save time.
        // println("Number of classes " + it.numberOfClasses)
        // println("Output shape "  + it.layers.last().outputShape[1].toInt())
        //
        // //
        // // // Predict outputs a class label
        // // // println("Prediction: " + it.predict(train.getX(0)))
        // //
        // // // Predict softly outputs a probability for each class label
        // // println("Prediction: " + it.predictSoftly(train.getX(0)).contentToString()) // Predict outputs a class label
        // //
        // // // Print and return activations, which  are massive. We may want to get them though for graphical purposes
         val (first, second) = it.predictAndGetActivations(train.getX(0))
         println("Class index: " + first)
         println("Activations: " + second.map { e -> (e as Array<*>).contentDeepToString()}
             .joinToString ("\n-----------\n"))

        // it.fit(dataset = train, epochs = EPOCHS, batchSize = TRAINING_BATCH_SIZE)
        //
        // val accuracy = it.evaluate(dataset = test, batchSize = TEST_BATCH_SIZE).metrics[Metrics.ACCURACY]
        //
        // println("Accuracy: $accuracy")
    }
}