package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.place
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.View3DSensor

/**
 * Demo simulation showing a CNN pipeline connected to a 3D view sensor.
 *
 * Pipeline: View3DSensor (64x64x3) -> Conv1 -> ReLU -> Pool1 -> Conv2 -> ReLU -> Pool2 -> Flatten -> Dense
 */
val cnnDemo = newSim {

    workspace.clearWorkspace()

    // --- OdorWorld ---
    val odorWorldComponent = addOdorWorldComponent("Odor World")
    val odorWorld = odorWorldComponent.world
    odorWorld.apply {
        tileMap = TileMap(16, 16)
        wrapAround = false
        isObjectsBlockMovement = false
    }
    odorWorld.tileMap.layers.first().let { layer ->
        for (x in 0 until odorWorld.tileMap.width) {
            for (y in 0 until odorWorld.tileMap.height) {
                layer[x, y] = 6
            }
        }
    }

    val mouse = odorWorld.addEntity(
        odorWorld.width / 2,
        odorWorld.height / 2,
        EntityType.Mouse
    ).apply {
        heading = 0.0
        name = "Agent"
    }

    val view3dSensor = View3DSensor().apply {
        label = "3D View"
        fov = 90.0
        viewDistance = 400.0
        outputWidth = 64
        outputHeight = 64
    }
    mouse.addSensor(view3dSensor)

    // Add some objects
    odorWorld.addEntity(100, 100, EntityType.Swiss).name = "Swiss"
    odorWorld.addEntity(300, 150, EntityType.Flower).name = "Flower"
    odorWorld.addEntity(200, 350, EntityType.Cow).name = "Cow"

    // --- Network ---
    val networkComponent = addNetworkComponent("CNN")
    val network = networkComponent.network

    // Input tensor: matches View3DSensor output
    val inputShape = TensorShape(64, 64, 3)
    val inputTensorLayer = TensorLayer(inputShape).apply {
        label = "Input (64x64x3)"
        isClamped = true
    }
    network.addNetworkModelAsync(inputTensorLayer, usePlacementManager = false)
    inputTensorLayer.setLocation(0.0, 0.0)

    // Conv1: 3x3, 8 filters, SAME padding -> output 64x64x8
    val conv1OutputShape = inputShape.convOutputShape(3, 1, Padding.SAME, 8)
    val conv1Output = TensorLayer(conv1OutputShape).apply {
        label = "Conv1 (${conv1OutputShape})"
        activationFunction = TensorActivation.RELU
    }
    network.addNetworkModelAsync(conv1Output, usePlacementManager = false)
    conv1Output.setLocation(0.0, 400.0)
    val conv1 = ConvolutionConnector(inputTensorLayer, conv1Output, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME)
    network.addNetworkModelAsync(conv1, usePlacementManager = false)

    // MaxPool1: 2x2 -> output 32x32x8
    val pool1OutputShape = conv1OutputShape.poolOutputShape(2, 2)
    val pool1Layer = TensorLayer(pool1OutputShape).apply {
        label = "Pool1 (${pool1OutputShape})"
    }
    network.addNetworkModelAsync(pool1Layer, usePlacementManager = false)
    pool1Layer.setLocation(0.0, 800.0)
    val pool1 = PoolingConnector(conv1Output, pool1Layer, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)
    network.addNetworkModelAsync(pool1, usePlacementManager = false)

    // Conv2: 3x3, 16 filters, SAME padding -> output 32x32x16
    val conv2OutputShape = pool1OutputShape.convOutputShape(3, 1, Padding.SAME, 16)
    val conv2Layer = TensorLayer(conv2OutputShape).apply {
        label = "Conv2 (${conv2OutputShape})"
        activationFunction = TensorActivation.RELU
    }
    network.addNetworkModelAsync(conv2Layer, usePlacementManager = false)
    conv2Layer.setLocation(0.0, 1200.0)
    val conv2 = ConvolutionConnector(pool1Layer, conv2Layer, kernelSize = 3, numFilters = 16, stride = 1, padding = Padding.SAME)
    network.addNetworkModelAsync(conv2, usePlacementManager = false)

    // MaxPool2: 2x2 -> output 16x16x16
    val pool2OutputShape = conv2OutputShape.poolOutputShape(2, 2)
    val pool2Layer = TensorLayer(pool2OutputShape).apply {
        label = "Pool2 (${pool2OutputShape})"
    }
    network.addNetworkModelAsync(pool2Layer, usePlacementManager = false)
    pool2Layer.setLocation(0.0, 1600.0)
    val pool2 = PoolingConnector(conv2Layer, pool2Layer, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)
    network.addNetworkModelAsync(pool2, usePlacementManager = false)

    // Flatten: 16x16x16 = 4096 -> NeuronArray(4096)
    val flattenSize = pool2OutputShape.size
    val flattenArray = NeuronArray(flattenSize).apply {
        label = "Flatten ($flattenSize)"
    }
    network.addNetworkModelAsync(flattenArray, usePlacementManager = false)
    flattenArray.setLocation(0.0, 2000.0)
    val flatten = FlattenConnector(pool2Layer, flattenArray)
    network.addNetworkModelAsync(flatten, usePlacementManager = false)

    // Dense output: small output layer (e.g. 3 categories)
    val outputArray = NeuronArray(3).apply {
        label = "Output (3)"
    }
    network.addNetworkModelAsync(outputArray, usePlacementManager = false)
    outputArray.setLocation(0.0, 2400.0)
    val dense = WeightMatrix(flattenArray, outputArray)
    network.addNetworkModelAsync(dense, usePlacementManager = false)

    // --- Coupling: View3DSensor.rgbTensor -> inputTensor.setActivations ---
    with(couplingManager) {
        view3dSensor.getProducer(View3DSensor::rgbTensorLayer) couple
                inputTensorLayer.getConsumer(TensorLayer::setActivations)
    }

    // --- Layout ---
    place(odorWorldComponent, 0, 0, 400, 400)
    place(networkComponent, 410, 0, 500, 800)

    addSidebarInfo(
        """
        # CNN Pipeline Demo

        This simulation shows a convolutional neural network (CNN) pipeline
        connected to a 3D view sensor. The agent sees a pseudo-3D first-person view
        of the OdorWorld, which feeds into a chain of CNN layers.

        ## Pipeline
        - **Input**: 64x64x3 RGB tensor from View3DSensor
        - **Conv1**: 3x3 kernel, 8 filters, SAME padding, ReLU
        - **MaxPool1**: 2x2 pool
        - **Conv2**: 3x3 kernel, 16 filters, SAME padding, ReLU
        - **MaxPool2**: 2x2 pool
        - **Flatten**: 16x16x16 = 4096 -> NeuronArray
        - **Dense**: 4096 -> 3 output neurons (via WeightMatrix)

        ## How to use
        - Use arrow keys in the OdorWorld to move the agent
        - Observe channel activations updating in each tensor layer
        - Right-click on tensors to navigate channels or toggle RGB view
        - Right-click on conv connectors to browse kernel weights
        - The output neurons show the full CNN-to-dense pipeline in action
        """.trimIndent()
    )
}
