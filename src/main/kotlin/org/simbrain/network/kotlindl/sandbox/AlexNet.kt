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
import org.jetbrains.kotlinx.dl.api.core.layer.normalization.BatchNorm
import org.jetbrains.kotlinx.dl.api.core.layer.pooling.AvgPool2D
import org.jetbrains.kotlinx.dl.api.core.layer.pooling.MaxPool2D
import org.jetbrains.kotlinx.dl.api.core.layer.reshaping.Flatten
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.core.optimizer.ClipGradientByValue
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import org.jetbrains.kotlinx.dl.dataset.handler.NUMBER_OF_CLASSES
import org.jetbrains.kotlinx.dl.dataset.mnist

private const val EPOCHS = 3
private const val TRAINING_BATCH_SIZE = 1000
private const val NUM_CHANNELS = 1L
private const val IMAGE_SIZE = 28L
private const val SEED = 12L
private const val TEST_BATCH_SIZE = 1000

/**
 * @author Ariel Lavi
 */
public val alexNet = Sequential.of(
        Input(
          IMAGE_SIZE,
          IMAGE_SIZE,
          NUM_CHANNELS
        ),
        // What about local response normalization for the CONV2D layers?
        Conv2D(
                filters = 6,
                kernelSize = longArrayOf(5, 5),
                strides = longArrayOf(1, 1, 1, 1),
                activation = Activations.Relu,
                kernelInitializer = GlorotNormal(SEED),       // not sure about this
                biasInitializer = Zeros(),
                padding = ConvPadding.SAME              // nor this
        ),
        //BatchNorm(),
        Conv2D(
                filters = 16,
                kernelSize = longArrayOf(3, 3),
                strides = longArrayOf(1, 1, 1, 1),
                activation = Activations.Relu,
                kernelInitializer = GlorotNormal(SEED),       // not sure about this
                biasInitializer = Zeros(),              // nor this
                padding = ConvPadding.SAME              // nor this
        ),
        MaxPool2D(
                poolSize = intArrayOf(1, 3, 3, 1),      // change to 3 channels
                strides = intArrayOf(1, 2, 2, 1),       // change to 3 channels
                padding = ConvPadding.VALID
        ),
        Conv2D(
                filters = 24,
                kernelSize = longArrayOf(3, 3),
                strides = longArrayOf(1, 1, 1, 1),
                activation = Activations.Relu,
                kernelInitializer = GlorotNormal(SEED),       // not sure about this
                biasInitializer = Zeros(),              // nor this
                padding = ConvPadding.SAME              // nor this
        ),
        Conv2D(
                filters = 24,
                kernelSize = longArrayOf(3, 3),
                strides = longArrayOf(1, 1, 1, 1),
                activation = Activations.Relu,
                kernelInitializer = GlorotNormal(SEED),       // not sure about this
                biasInitializer = Zeros(),              // nor this
                padding = ConvPadding.SAME              // nor this
        ),
        /*
        Conv2D(
                filters = 16,
                kernelSize = longArrayOf(3, 3),
                strides = longArrayOf(1, 1, 1, 1),
                activation = Activations.Relu,
                kernelInitializer = GlorotNormal(SEED),       // not sure about this
                biasInitializer = Zeros(),              // nor this
                padding = ConvPadding.SAME              // nor this
        ),

         */
        MaxPool2D(
                poolSize = intArrayOf(1, 3, 3, 1),      // change to 3 channels
                strides = intArrayOf(1, 2, 2, 1),       // change to 3 channels
                padding = ConvPadding.VALID
        ),
        Flatten(),
        Dense(
                outputSize = 256,
                activation = Activations.Tanh,
                kernelInitializer = GlorotNormal(SEED),
                biasInitializer = Constant(0.1f)
        ),
        Dense(
                outputSize = 256,
                activation = Activations.Tanh,
                kernelInitializer = GlorotNormal(SEED),
                biasInitializer = Constant(0.1f)
        ),
        Dense(
                outputSize = NUMBER_OF_CLASSES,
                //outputSize = 1000,
                activation = Activations.Softmax,
                kernelInitializer = GlorotNormal(SEED),
                biasInitializer = Constant(0.1f)
        )
)

/*

public val lenet5Classic = Sequential.of(
    Input(
        IMAGE_SIZE,
        IMAGE_SIZE,
        NUM_CHANNELS
    ),
    Conv2D(
        filters = 6,
        kernelSize = longArrayOf(5, 5),
        strides = longArrayOf(1, 1, 1, 1),
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
    Conv2D(
        filters = 16,
        kernelSize = longArrayOf(5, 5),
        strides = longArrayOf(1, 1, 1, 1),
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
*/
fun main() {

    // testDataset()

    val (train, test) = mnist()

    alexNet.use {
        it.compile(
            optimizer = Adam(clipGradient = ClipGradientByValue(0.1f)),
            loss = Losses.SOFT_MAX_CROSS_ENTROPY_WITH_LOGITS,
            metric = Metrics.ACCURACY
        )

        it.summary()

        it.fit(dataset = train, epochs = EPOCHS, batchSize = TRAINING_BATCH_SIZE)

        val accuracy = it.evaluate(dataset = test, batchSize = TEST_BATCH_SIZE).metrics[Metrics.ACCURACY]

        println("Accuracy: $accuracy")
    }
}
