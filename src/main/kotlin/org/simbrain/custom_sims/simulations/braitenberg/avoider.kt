package org.simbrain.custom_sims.simulations.braitenberg

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapseAsync
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.SmellSource
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize
import org.simbrain.world.odorworld.sensors.SmellSensor

val avoider = newSim {
    // Clear workspace
    workspace.clearWorkspace()

    // Create network component
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Create neurons - from Network6.xml
    val leftSensor = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 1.0
            lowerBound = -1.0
        }
        location = point(41.1, 87.2)
        label = "Left sensor"
    }

    val rightSensor = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 1.0
            lowerBound = -1.0
        }
        location = point(159.7, 89.9)
        label = "Right sensor"
    }

    val turnLeft = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 40.0
            lowerBound = -1.0
        }
        location = point(37.6, 11.2)
        label = "Turn left"
    }

    val straight = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 10.0
            lowerBound = -1.0
            increment = 0.25
            activation = 0.5
            clamped = true
        }
        location = point(96.6, 10.7)
        label = "Straight"
    }

    val turnRight = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 40.0
            lowerBound = -1.0
        }
        location = point(159.8, 15.6)
        label = "Turn right"
    }

    // Create synapses - from Network6.xml
    // Note that the connections in the avoider configuration are cross-coupled
    // Left sensor connects to right turn, and right sensor connects to left turn
    network.addSynapseAsync(leftSensor, turnRight) {
        strength = 45.0
        upperBound = 100.0
        lowerBound = -10.0
    }

    network.addSynapseAsync(rightSensor, turnLeft) {
        strength = 45.0
        upperBound = 100.0
        lowerBound = -10.0
    }

    // Create odor world component
    val odorWorldComponent = addOdorWorldComponent("OdorWorld")
    val odorWorld = odorWorldComponent.world

    // Configure odor world - from OdorWorld1.xml
    odorWorld.apply {
        wrapAround = true
        isObjectsBlockMovement = false
    }

    // Create mouse agent
    val mouse = odorWorld.addEntity(269, 127, EntityType.Mouse).apply {
        heading = 155.4
        name = "Agent_1"
    }

    // Add smell sensors at specific angles
    val leftSmellSensor = SmellSensor().apply {
        theta = 22.5
        radius = 50.0
        label = "Smell-Left"
    }
    mouse.addSensor(leftSmellSensor)

    val centerSmellSensor = SmellSensor().apply {
        theta = 0.0
        radius = 0.0
        label = "Smell-Center"
    }
    mouse.addSensor(centerSmellSensor)

    val rightSmellSensor = SmellSensor().apply {
        theta = -22.5
        radius = 50.0
        label = "Smell-Right"
    }
    mouse.addSensor(rightSmellSensor)

    // Add effectors to mouse
    mouse.addDefaultEffectors()

    // Get effector references
    val straightMovement = mouse.effectors[0] as StraightMovement
    val leftTurn = mouse.effectors[1] as Turning
    val rightTurn = mouse.effectors[2] as Turning

    // Configure effectors
    straightMovement.apply {
        label = "Go-straight"
        scalingFactor = 1.0
        amount = 2.75
    }

    leftTurn.apply {
        label = "Go-left"
        amount = 0.0
    }

    rightTurn.apply {
        label = "Go-right"
        amount = 0.0
    }

    // Add cheese objects with smell sources
    val swissCheese = odorWorld.addEntity(170, 309, EntityType.Swiss).apply {
        smellSource = SmellSource(doubleArrayOf(0.7, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)).apply {
            dispersion = 70.0
            decayFunction = GaussianDecayFunction()
        }
    }

    val goudaCheese = odorWorld.addEntity(153, 48, EntityType.Gouda).apply {
        smellSource = SmellSource(doubleArrayOf(0.7, 0.0, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0)).apply {
            dispersion = 70.0
            decayFunction = GaussianDecayFunction()
        }
    }

    val blueCheese = odorWorld.addEntity(112, 175, EntityType.BlueCheese).apply {
        smellSource = SmellSource(doubleArrayOf(0.7, 0.0, 0.0, 0.0, 0.3, 0.0, 0.0, 0.0)).apply {
            dispersion = 70.0
            decayFunction = GaussianDecayFunction()
        }
    }

    // Add documentation sidebar
    addSidebarInfo(
        """
        # Avoider

        [Braitenberg vehicles](http://en.wikipedia.org/wiki/Braitenberg_vehicle) are simple agents that move around in response to environmental stimuli. This simulation simulates a Braitenberg vehicle as an agent in an odor world that avoids an object.

        # Simulation Details

        In this simulation, the Braitenberg vehicle has the characteristics of an avoider where it demonstrates movement similar to vehicle 2a (the coward vehicle) from Braitenberg's book (see link below). Note that the connections are not the same as in Braitenberg's book.

        The agent will orient to the left if its right sensor is activated by an object on its right side, moving away from it. If it senses an object on its left side, the agent will orient to the right and move away from the object.

        # What to Do

        In this simulation similar to the other Braitenberg simulations, simply press `Run` on the top toolbar for the simulation to run. Below are the steps:

        1. While it runs, observe the behavior of the agent in the odor world

        2. Move any of the objects around and watch the agent avoid it and observe the activity of the neurons in the network window

        3. Press `Stop` to stop the network from running

        ## Other Observations

        In this simulation, you can control the velocity of the agent's movement. There are three key value ranges to observe: positive, zero, negative. To do this:

        1. Click on the `Straight` neuron

        2) Press the up/down arrow keys to control the neuron's activation (e.g., velocity).

        In addition to controlling the velocity, you can control the strength of the agent's sensors by changing the values of the weights from sensors to orientation. This changes
        how aggressive the agent turns in response to the object. To do this:

        1) Click on either weights between the sensor neurons and turn neurons.

        2) Press the up/down arrow keys to control their strength (e.g., the agent's turn aggression towards the object).

        # References

        Braitenberg, V. (1986). [_Vehicles: Experiments in synthetic psychology_](https://mitpress.mit.edu/9780262521123/vehicles/). MIT Press.

        Hotton, S., & Yoshimi, J. (2024). [_The Open Dynamics of Braitenberg Vehicles_](https://mitpress.mit.edu/9780262548199/the-open-dynamics-of-braitenberg-vehicles/). MIT Press.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        Kanly Thao

        Saraching Chao

        """.trimIndent()
    )

    // GUI layout
    withGui {
        place(networkComponent) {
            location = point(SIM_WINDOW_GAP, SIM_WINDOW_GAP)
            width = 400
            height = 400
        }

        place(odorWorldComponent) {
            location = point(SIM_WINDOW_GAP + 400 + SIM_WINDOW_GAP, SIM_WINDOW_GAP)
            width = 400
            height = 435
        }
        odorWorldComponent.getDesktopComponentAs<OdorWorldDesktopComponent>().fitWorldToFrameSize()

    }

    // Create couplings - from contents.xml
    with(couplingManager) {
        // Neuron outputs to effectors
        turnRight couple rightTurn
        turnLeft couple leftTurn
        straight couple straightMovement

        // Sensors to neurons
        leftSmellSensor couple leftSensor
        rightSmellSensor couple rightSensor
    }
}
