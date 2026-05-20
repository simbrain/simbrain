package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.setLabels
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.SmellSource
import org.simbrain.util.applyFunction
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor


val threeObjectDetector = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Object Detector")
    val net = networkComponent.network

    val inputLayer = net.addNeuronCollection(3).apply {
        isClamped = true
        applyLayout(LineLayout())
    }
    val hiddenLayer = net.addNeuronCollection(5) { updateRule = SigmoidalRule() }.apply {
        applyLayout(LineLayout())
    }
    val outputLayer = net.addNeuronCollection(3) { updateRule = SigmoidalRule() }.apply {
        applyLayout(LineLayout())
        setLabels(listOf("Gouda", "Blue", "Fish"))
    }
    val sg1 = SynapseGroup(inputLayer, hiddenLayer)
    val sg2 = SynapseGroup(hiddenLayer, outputLayer)
    val sm = SupervisedModel(inputLayer, outputLayer, trainTestSplit = 1.0)
    net.addNetworkModels(sg1, sg2, sm)
    offsetNetworkModel(inputLayer, hiddenLayer, Direction.NORTH, 150.0)
    offsetNetworkModel(hiddenLayer, outputLayer, Direction.NORTH, 150.0)
    alignNetworkModels(inputLayer, hiddenLayer, Alignment.VERTICAL)
    alignNetworkModels(inputLayer, outputLayer, Alignment.VERTICAL)

    val odorWorldComponent = addOdorWorldComponent("World")
    withGui {
        odorWorldComponent.scale(1.0)
    }

    val odorWorld = odorWorldComponent.world.apply {
        isObjectsBlockMovement = false
        isUseCameraCentering = false
    }

    val smellSensor = SmellSensor().apply {
        radius = 35.0
    }
    val mouse = odorWorld.addEntity(EntityType.Mouse).apply {
        location = point(134, 152)
        heading = 0.0
        addSensor(smellSensor)
    }

    val gouda = odorWorld.addEntity(EntityType.Gouda).apply {
        location = point(49, 43)
        smellSource = SmellSource(doubleArrayOf(1.0, 0.4, 0.2))
    }

    val blueCheese = odorWorld.addEntity(EntityType.BlueCheese).apply {
        location = point(251, 27)
        smellSource = SmellSource(doubleArrayOf(0.4, 1.0, 0.2))
    }

    val fish = odorWorld.addEntity(EntityType.Fish).apply {
        location = point(178, 300)
        smellSource = SmellSource(doubleArrayOf(0.4, 0.2, 1.0))
    }

    val steps = 10

    fun smellGenerator(smellSource: SmellSource) = Array(steps) { scalingFactor ->
        smellSource.stimulusVector!!.applyFunction { (0.5 / (steps)) * scalingFactor * it + 0.5 * it }
    }

    val smells = buildList {
        addAll(smellGenerator(gouda.smellSource).map { it.toMutableList() })
        addAll(smellGenerator(blueCheese.smellSource).map { it.toMutableList() })
        addAll(smellGenerator(fish.smellSource).map { it.toMutableList() })
    }.toMutableList()

    val targets = buildList {
        repeat(steps) { add(mutableListOf(1.0, 0.0, 0.0)) }
        repeat(steps) { add(mutableListOf(0.0, 1.0, 0.0)) }
        repeat(steps) { add(mutableListOf(0.0, 0.0, 1.0)) }
    }.toMutableList()

    val (training, testing) = splitDataSet(smells, targets, 0.8)

    val (testingInputs, testingTargets) = testing

    val trainingInputs = training.let { (i, _) ->
        buildList {
            addAll(i)
            add(mutableListOf(0.0, 0.0, 0.0))
        }.toMutableList()
    }

    val trainingTargets = training.let { (_, t) ->
        buildList {
            addAll(t)
            add(mutableListOf(0.0, 0.0, 0.0))
        }.toMutableList()
    }

    sm.trainingSet = TrainingDataset(
        inputs = trainingInputs,
        targets = trainingTargets,
        inputSize = smells.first().size,
        targetSize = targets.first().size
    )

    sm.testingSet = TrainingDataset(
        inputs = testingInputs,
        targets = testingTargets,
        inputSize = smells.first().size,
        targetSize = targets.first().size
    )

    sm.trainerConfig.testConfiguration.enabled = true

    with(couplingManager) {
        smellSensor.getProducer(SmellSensor::smellVector) couple inputLayer.getConsumer(NeuronCollection::setActivations)
    }

    withGui {
        place(odorWorldComponent,435, 0, 380, 508)
        place(networkComponent, 0, 0, 447, 595)
    }

    addSidebarInfo(
        """
            # Three Object Detector

            This simulation uses backprop to train a network to identify smells.

            # What to Do

            1. Click `Run`, then manually move the mouse around the world. Watch the output neurons as the mouse approaches each object. At first the network should do poorly at classifying the smells.

            2. Double-click on the `Backprop` model in the `Object Detector` network window to open the “Train Network” window.

            3. In the top area of this window, select the `Iterate training until the stop button is pressed` button to train the network.
                - The “Sum Squared Error” should decrease as the iterations increase and end up around `0`. The goal is to get the error as low as possible.

            4. Click `Apply current row as input and increment selected row` to check the network's performance. As you click the pattern on the bottom should be matched by the
            pattern on the top.

            5. Close the training window, click `Run` again, and move the mouse around the world. The output neurons should now classify the nearby object more accurately.

            # Credits

            [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        """.trimIndent()
    )


}
