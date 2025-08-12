package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.AbstractNeuronCollection
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.setLabels
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.*
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor
import smile.math.matrix.Matrix


val threeObjectDetector = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Object Detector")
    val net = networkComponent.network

    val inputLayer = NeuronGroup(3).apply {
        isClamped = true
        applyLayout(LineLayout())
    }
    val hiddenLayer = NeuronGroup(5).apply {
        updateRule = SigmoidalRule()
        applyLayout(LineLayout())
    }
    val outputLayer = NeuronGroup(3).apply {
        updateRule = SigmoidalRule()
        applyLayout(LineLayout())
        setLabels(listOf("Gouda", "Blue", "Fish"))
    }
    val sg1 = SynapseGroup(inputLayer, hiddenLayer)
    val sg2 = SynapseGroup(hiddenLayer, outputLayer)
    val sm = SupervisedModel(inputLayer, outputLayer, trainTestSplit = 1.0)
    net.addNetworkModels(inputLayer, hiddenLayer, outputLayer, sg1, sg2, sm)
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
        addAll(smellGenerator(gouda.smellSource))
        addAll(smellGenerator(blueCheese.smellSource))
        addAll(smellGenerator(fish.smellSource))
    }.toTypedArray()

    val targets = buildList {
        repeat(steps) { add(doubleArrayOf(1.0, 0.0, 0.0)) }
        repeat(steps) { add(doubleArrayOf(0.0, 1.0, 0.0)) }
        repeat(steps) { add(doubleArrayOf(0.0, 0.0, 1.0)) }
    }.toTypedArray()

    val (training, testing) = splitDataSet(Matrix.of(smells), Matrix.of(targets), 0.8)

    val (testingInputs, testingTargets) = testing

    val trainingInputs = training.let { (i, _) ->
        buildList {
            addAll(i.toArray())
            add(doubleArrayOf(0.0, 0.0, 0.0))
        }.toTypedArray().toMatrix()
    }

    val trainingTargets = training.let { (_, t) ->
        buildList {
            addAll(t.toArray())
            add(doubleArrayOf(0.0, 0.0, 0.0))
        }.toTypedArray().toMatrix()
    }

    sm.trainingSet = MatrixDataset(
        inputs = trainingInputs,
        targets = trainingTargets
    )

    sm.testingSet = MatrixDataset(
        inputs = testingInputs,
        targets = testingTargets
    )

    sm.trainerConfig.testConfiguration.enabled = true

    with(couplingManager) {
        smellSensor.getProducer(SmellSensor::smellVector) couple inputLayer.getConsumer(AbstractNeuronCollection::setActivations)
    }

    withGui {
        place(odorWorldComponent,435, 0, 380, 508)
        place(networkComponent, 0, 0, 447, 595)
    }

    addSidebarInfo(
        """ 
            # Introduction
            Using backprop to train a network to identify smells 
            
            # What to Do
            Double-click on the “Backprop” network in the “Backprop” network window to open the “Train Network” window.
            In the top area of this window, select the “Iterate training until the stop button is pressed” button to play the simulation. 
            The “Sum Squared Error” should decrease as the iterations increase and end up around 0. The goal is to get the error as low as possible.
            Click “Apply current row as input and increment selected row” to check the network's performance. 
            As you click the pattern on the bottom should be matched by the pattern on the top.
            
      
        """.trimIndent()
    )


}