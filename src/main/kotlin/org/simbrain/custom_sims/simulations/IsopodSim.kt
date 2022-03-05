package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addOdorWorldComponent
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.connect
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.math.ProbDistributions.NormalDistribution
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor

/**
 * A simulation of Isopod navigation. With Peter Hinow and Kaiden Schmidt.
 */
val isopodSim = newSim {

    // TODO
    //  Button panel
    //  Logging
    //  Plots (time series, histogram; add easy hooks)

    workspace.clearWorkspace()

    // ----- Network construction ------

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val neuronLeftSensor = network.addNeuron {
        location = point(0,100)
        upperBound = 10.0
        label = "Left"
    }
    val neuronRightSensor = network.addNeuron {
        location = point(100,100)
        upperBound = 10.0
        label = "Right"
        with (updateRule as LinearRule) {
            noiseGenerator = NormalDistribution(0.0, 1.5)
            addNoise = true
        }
    }
    val neuronLeftTurning = network.addNeuron {
        location = point(0,0)
        upperBound = 10.0
        label = "Turn Left"
    }
    val neuronRightTurning = network.addNeuron {
        location = point(100,0)
        upperBound = 10.0
        label = "Turn Right"
    }
    val neuronStraight = network.addNeuron {
        location = point(50,0)
        upperBound = 10.0
        label = "Straight"
        (neuronDataHolder as BiasedScalarData).bias = 2.0
    }

    connect(neuronLeftSensor, neuronLeftTurning, 4.0)
    connect(neuronRightSensor, neuronRightTurning, 4.0)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    // ----- Build 2d World ------

    val odorWorldComponent = addOdorWorldComponent()
    val odorWorld = odorWorldComponent.world

    // Object references we'll need later
    val straightMovement : Effector
    val turnLeft : Effector
    val turnRight : Effector
    var leftSensor : ObjectSensor
    var rightSensor : ObjectSensor
    val isopod : OdorWorldEntity

    odorWorld.apply {

        wrapAround = true
        isObjectsBlockMovement = false

        tileMap.updateMapSize(10,10)
        events.fireTileMapChanged() // TODO
        tileMap.fill(2)

        // Body could be represented by a triangle or rhombus
        isopod = addEntity(150, 150, EntityType.MOUSE).apply {
            heading = 90.0
            addDefaultEffectors()
            straightMovement = effectors[0]
            turnLeft = effectors[1]
            turnRight = effectors[2]

            // Can add more smell sensors here
            // Options: new sensor in back; triangular array
            // TODO: Object or smell?
            leftSensor = ObjectSensor(this).apply {
                radius = 40.0
                theta = Math.PI / 4
                setObjectType(EntityType.FISH)
                decayFunction.dispersion = 400.0
                addSensor(this)
            }
            rightSensor = ObjectSensor(this).apply {
                radius = 40.0
                theta = -Math.PI / 4
                setObjectType(EntityType.FISH)
                decayFunction.dispersion = 400.0
                addSensor(this)
            }
            manualStraightMovementIncrement = 2.0
            manualMotionTurnIncrement = 2.0
        }

        odorWorld.addEntity(290, 290, EntityType.FISH)
        odorWorld.addEntity(10, 290, EntityType.FISH)
        odorWorld.addEntity(10, 10, EntityType.FISH)
        odorWorld.addEntity(290, 10, EntityType.FISH)

    }

    withGui {
        place(odorWorldComponent) {
            location = point(403, 0)
            width = 400
            height = 400
        }
    }

    // ----- Make Couplings ------

    with(couplingManager) {
        neuronStraight couple straightMovement
        neuronLeftTurning couple turnLeft
        neuronRightTurning couple turnRight
        leftSensor couple neuronLeftSensor
        rightSensor couple neuronRightSensor
    }

}

