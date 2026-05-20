package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.trainers.CnnLossFunction
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.place
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.world.odorworld.OdorWorldPreferences
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.View3DSensor
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Object recognition from a first-person 3D view.
 *
 * A control panel lets the user configure sampling parameters and pick
 * three object types plus an optional "No Object" class. Clicking
 * Generate samples the [View3DSensor] at many randomized poses around
 * a target entity, assembles a CNN, and attaches train/test datasets.
 * Training itself is triggered from the network's right-click
 * Train... dialog. Clicking Regenerate tears down the network and
 * reruns sampling with the current control-panel settings.
 */
val objectRecognition3D = newSim {

    val entityChoices: List<EntityType> = listOf(
        EntityType.Swiss, EntityType.Gouda, EntityType.BlueCheese,
        EntityType.Bell, EntityType.Poison, EntityType.Candle,
        EntityType.Fish, EntityType.Flower, EntityType.Dandelions,
        EntityType.Geraniums, EntityType.Tulip, EntityType.Flax, EntityType.Pansy,
    )

    var samplesPerClass = 2048
    var innerRadius = 20.0
    var outerRadius = 180.0
    var headingJitterDeg = 30.0
    var targetMargin = 80.0
    var viewDistance = 300.0
    var trainFraction = 0.85
    var class1: EntityType = EntityType.Swiss
    var class2: EntityType = EntityType.Poison
    var class3: EntityType = EntityType.Flower
    var includeNoObject = true

    val seed = 42L
    val viewWidth = 64
    val viewHeight = 64
    val pixelsPerImage = viewWidth * viewHeight * 3

    workspace.clearWorkspace()

    val odorWorldComponent = addOdorWorldComponent("Odor World")
    val odorWorld = odorWorldComponent.world
    odorWorld.apply {
        tileMap = loadTileMap(File(OdorWorldPreferences.tileMapDirectory, "yulins_world.tmx"))
        wrapAround = false
        isObjectsBlockMovement = false
    }

    val worldCenterX = odorWorld.width / 2.0
    val worldCenterY = odorWorld.height / 2.0

    val mouse = odorWorld.addEntity(worldCenterX.toInt(), worldCenterY.toInt(), EntityType.Mouse).apply {
        heading = 0.0
        name = "Agent"
    }

    val view3dSensor = View3DSensor().apply {
        label = "3D View"
        fov = 90.0
        this.viewDistance = viewDistance
        outputWidth = viewWidth
        outputHeight = viewHeight
    }
    mouse.addSensor(view3dSensor)

    val networkComponent = addNetworkComponent("CNN")
    val network = networkComponent.network

    suspend fun generateAndBuild() {
        view3dSensor.viewDistance = viewDistance

        if (network.allModels.isNotEmpty()) {
            network.deleteModels(network.allModels.toList())
        }
        odorWorld.entityList.filter { it != mouse }.forEach { it.delete() }

        val target = odorWorld.addEntity(worldCenterX.toInt(), worldCenterY.toInt(), class1).apply {
            name = "Target"
        }

        val classEntityTypes = listOf(class1, class2, class3)
        val classes: List<EntityType?> =
            if (includeNoObject) classEntityTypes + listOf<EntityType?>(null) else classEntityTypes
        val classLabels = classEntityTypes.map { it.description } +
                if (includeNoObject) listOf("No Object") else emptyList()
        val totalSamples = classes.size * samplesPerClass

        val rng = Random(seed)
        val allInputs = ArrayList<MutableList<Double>>(totalSamples)
        val allTargets = ArrayList<MutableList<Double>>(totalSamples)
        val targetXRange = odorWorld.width - 2 * targetMargin
        val targetYRange = odorWorld.height - 2 * targetMargin

        val progressWindow = ProgressWindow(totalSamples, "Generating dataset")
        var globalIdx = 0
        for ((classIdx, type) in classes.withIndex()) {
            if (type != null) target.entityType = type
            val oneHot = DoubleArray(classes.size) { if (it == classIdx) 1.0 else 0.0 }
            repeat(samplesPerClass) {
                val tx = targetMargin + rng.nextDouble() * targetXRange
                val ty = targetMargin + rng.nextDouble() * targetYRange
                target.x = tx
                target.y = ty
                val r = innerRadius + rng.nextDouble() * (outerRadius - innerRadius)
                val theta = rng.nextDouble() * 2 * PI
                mouse.x = tx + r * cos(theta)
                mouse.y = ty + r * sin(theta)
                val toTarget = Math.toDegrees(atan2(mouse.y - ty, tx - mouse.x))
                // For "No Object" samples the mouse faces 180° away so the target falls behind the FOV.
                val baseHeading = if (type == null) toTarget + 180.0 else toTarget
                val jitter = (rng.nextDouble() * 2 - 1) * headingJitterDeg
                mouse.heading = baseHeading + jitter
                view3dSensor.update(mouse)
                val pixels = view3dSensor.rgbTensorLayer
                val row = ArrayList<Double>(pixelsPerImage)
                for (v in pixels) row.add(v)
                allInputs.add(row)
                allTargets.add(oneHot.toMutableList())
                globalIdx++
                if (globalIdx % 100 == 0) {
                    progressWindow.value = globalIdx
                    progressWindow.text = "Generating dataset: $globalIdx / $totalSamples"
                }
            }
        }
        progressWindow.close()

        target.delete()
        val uniqueTypes = classEntityTypes.toSet().toList()
        val offsets = listOf(-128 to 0, 128 to 0, 0 to -128, 0 to 128)
        for ((idx, type) in uniqueTypes.withIndex()) {
            val (dx, dy) = offsets[idx % offsets.size]
            odorWorld.addEntity((worldCenterX + dx).toInt(), (worldCenterY + dy).toInt(), type).name = type.description
        }
        mouse.x = worldCenterX
        mouse.y = worldCenterY + 120
        mouse.heading = 90.0
        view3dSensor.update(mouse)

        val order = (0 until allInputs.size).shuffled(Random(seed))
        val splitIdx = (order.size * trainFraction).toInt()
        val trainOrder = order.subList(0, splitIdx)
        val testOrder = order.subList(splitIdx, order.size)

        val trainingSet = TrainingDataset(
            inputs = trainOrder.map { allInputs[it] }.toMutableList(),
            targets = trainOrder.map { allTargets[it] }.toMutableList(),
            inputSize = pixelsPerImage,
            targetSize = classes.size,
        )
        val testingSet = TrainingDataset(
            inputs = testOrder.map { allInputs[it] }.toMutableList(),
            targets = testOrder.map { allTargets[it] }.toMutableList(),
            inputSize = pixelsPerImage,
            targetSize = classes.size,
        )

        val leftX = 0.0
        val rightX = 500.0
        val topY = 0.0
        val stepY = 400.0

        val inputShape = TensorShape(viewHeight, viewWidth, 3)
        val inputTensorLayer = TensorLayer(inputShape).apply {
            label = "Input (${viewHeight}x${viewWidth}x3)"
            isClamped = true
            rgbComposite = true
        }
        inputTensorLayer.setLocation(leftX, topY)

        val conv1OutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 8)
        val conv1Out = TensorLayer(conv1OutShape).apply {
            label = "Conv1 ($conv1OutShape)"
            activationFunction = TensorActivation.RELU
        }
        conv1Out.setLocation(leftX, topY + stepY)
        ConvolutionConnector(inputTensorLayer, conv1Out, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME)

        val pool1OutShape = conv1OutShape.poolOutputShape(2, 2)
        val pool1 = TensorLayer(pool1OutShape).apply {
            label = "Pool1 ($pool1OutShape)"
        }
        pool1.setLocation(leftX, topY + stepY * 2)
        PoolingConnector(conv1Out, pool1, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

        val conv2OutShape = pool1OutShape.convOutputShape(3, 1, Padding.SAME, 16)
        val conv2Out = TensorLayer(conv2OutShape).apply {
            label = "Conv2 ($conv2OutShape)"
            activationFunction = TensorActivation.RELU
        }
        conv2Out.setLocation(leftX, topY + stepY * 3)
        ConvolutionConnector(pool1, conv2Out, kernelSize = 3, numFilters = 16, stride = 1, padding = Padding.SAME)

        val pool2OutShape = conv2OutShape.poolOutputShape(2, 2)
        val pool2 = TensorLayer(pool2OutShape).apply {
            label = "Pool2 ($pool2OutShape)"
        }
        pool2.setLocation(rightX, topY + stepY * 3)
        PoolingConnector(conv2Out, pool2, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

        val flatSize = pool2OutShape.size
        val flatArray = NeuronArray(flatSize).apply {
            label = "Flatten ($flatSize)"
        }
        flatArray.setLocation(rightX, topY + stepY * 2)
        FlattenConnector(pool2, flatArray)

        val outputArray = NeuronArray(classes.size).apply {
            label = "Output (${classes.size})"
            updateRule = SoftmaxRule()
            circleMode = true
            gridMode = false
            labelArray = classLabels.toTypedArray()
        }
        outputArray.setLocation(rightX, topY)
        WeightMatrix(flatArray, outputArray)

        val cnnModel = network.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray) {
            label = "3D Object CNN"
            this.trainingSet = trainingSet
            this.testingSet = testingSet
        }
        cnnModel.trainerConfig.apply {
            learningRate = 0.001
            batchSize = 32
            lossFunction = CnnLossFunction.CrossEntropy
            computeAccuracy = true
            testConfiguration.enabled = true
            testConfiguration.testFrequency = 10
        }

        with(couplingManager) {
            view3dSensor.getProducer(View3DSensor::rgbTensorLayer) couple
                    inputTensorLayer.getConsumer(inputTensorLayer::activations)
        }
    }

    val gui = withGui {
        place(odorWorldComponent, 0, 0, 400, 400)
        place(networkComponent, 410, 0, 600, 800)
        createControlPanel("Configuration", 1020, 0) {
            addFormattedNumericTextField("Samples per class", samplesPerClass) { samplesPerClass = it }
            addFormattedNumericTextField("Inner radius", innerRadius) { innerRadius = it }
            addFormattedNumericTextField("Outer radius", outerRadius) { outerRadius = it }
            addFormattedNumericTextField("Heading jitter (deg)", headingJitterDeg) { headingJitterDeg = it }
            addFormattedNumericTextField("Target margin", targetMargin) { targetMargin = it }
            addFormattedNumericTextField("View distance", viewDistance) { viewDistance = it }
            addFormattedNumericTextField("Train fraction", trainFraction) { trainFraction = it }
            addSeparator()
            addComboBox("Class 1", entityChoices, class1) { class1 = it }
            addComboBox("Class 2", entityChoices, class2) { class2 = it }
            addComboBox("Class 3", entityChoices, class3) { class3 = it }
            addCheckBox("Include 'No Object' class", includeNoObject) { includeNoObject = it }
            addSeparator()
            var hasGenerated = false
            addButton("Generate") {
                val prior = text
                text = if (hasGenerated) "Regenerating..." else "Generating..."
                isEnabled = false
                try {
                    generateAndBuild()
                    hasGenerated = true
                    text = "Regenerate"
                } catch (e: Throwable) {
                    text = prior
                    throw e
                } finally {
                    isEnabled = true
                }
            }
        }
    }
    if (gui == null) {
        generateAndBuild()
    }

    addSidebarInfo(
        """
        # 3D Object Recognition

        A CNN learns to classify first-person 3D views of objects in an
        [OdorWorld](https://docs.simbrain.net/docs/worlds/odorworld.html). Use the `Configuration` panel to pick parameters
        and three object classes, then click `Generate` to build the
        training / testing datasets and assemble the CNN.

        # Simulation Details

        ## Pose Sampling
        - For each class the target entity is teleported to a random
          position (within the world margins) and the mouse is placed in
          an annulus of radius `innerRadius`–`outerRadius` around it with
          a heading pointed toward the target ± the heading jitter.
        - For the optional `No Object` class the heading is flipped 180°
          so the target falls behind the FOV and the view contains only
          the tilemap and boundary walls.

        # What to Do
        1. Adjust the parameters and class selections in the `Configuration` panel.
        2. Click `Generate` to build the datasets and CNN.
        3. Right-click the `3D Object CNN` subnet and select `Train...`
        4. Close the dialog and drive the mouse around the world
           (arrow keys) to watch the live predictions.
        5. Tweak parameters and click `Regenerate` to rerun with fresh data.
        """.trimIndent()
    )
}
