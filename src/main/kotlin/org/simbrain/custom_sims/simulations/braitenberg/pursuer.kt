package org.simbrain.custom_sims.simulations.braitenberg

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapseAsync
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.SmellSource
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize
import org.simbrain.world.odorworld.sensors.SmellSensor

val pursuer = newSim {
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
        location = point(34.0, 102.6)
        label = "Left sensor"
    }
    
    val rightSensor = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 1.0
            lowerBound = -1.0
        }
        location = point(165.0, 102.6)
        label = "Right sensor"
    }
    
    val turnRight = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 150.0
            lowerBound = -1.0
        }
        location = point(165.0, 11.6)
        label = "Turn right"
    }
    
    val straight = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 10.0
            lowerBound = -1.0
            activation = 0.5
            clamped = true
        }
        location = point(99.0, 11.6)
        label = "Straight"
    }
    
    val turnLeft = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 150.0
            lowerBound = -1.0
        }
        location = point(34.0, 11.6)
        label = "Turn left"
    }
    
    // Create synapses - from Network6.xml
    network.addSynapseAsync(leftSensor, turnLeft) {
        strength = 10.0
        upperBound = 200.0
        lowerBound = -10.0
    }
    
    network.addSynapseAsync(rightSensor, turnRight) {
        strength = 10.0
        upperBound = 200.0
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
    val mouse = odorWorld.addEntity(157, 293, EntityType.Mouse).apply {
        heading = 81.56
        name = "Agent_1"
    }
    
    // Add smell sensors at specific angles
    val leftSmellSensor = SmellSensor().apply {
        theta = 22.5
        radius = 50.0
        label = "Smell-Left"
    }
    mouse.addSensor(leftSmellSensor)
    
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
    }
    
    leftTurn.apply {
        label = "Go-left"
    }
    
    rightTurn.apply {
        label = "Go-right"
    }
    
    // Add cheese (target)
    val cheese = odorWorld.addEntity(174, 87, EntityType.Swiss).apply {
        name = "Entity_2"
        smellSource = SmellSource(doubleArrayOf(0.7, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)).apply {
            dispersion = 200.0
            decayFunction = LinearDecayFunction()
        }
    }
    
    // Add documentation sidebar
    addSidebarInfo(
        """
        # Pursuer

        [Braitenberg vehicles](http://en.wikipedia.org/wiki/Braitenberg_vehicle) are simple agents that move around in response to environmental stimuli. This simulation simulates a Braitenberg vehicle as an agent in an odor world that is attracted to an object.

        # Simulation Details

        In this simulation, the Braitenberg vehicle has the characteristics of a pursuer where it demonstrates movement similar to vehicle 2b (the aggressive vehicle) from Braitenberg's book (see link below).

        The agent will move to the left if its left sensor is activated by an object on its left side, heading straight towards it. And if it senses a source on its right side, the agent will move to the right and straight towards the object. 

        # What to Do
        
        In this simulation similar to the other Braitenberg simulations, simply press the `play` button on the top toolbar for the simulation to run. Below are the steps:
        
        1. While it runs, observe the behavior of the agent in the odor world
        
        2. Move the cheese around and watch the agent chase it and observe the activity of the neurons in the network window
        
        3. Press the `stop` button to stop the network from running
        
        ## Other Observations
        
        In this simulation, you can control the velocity of the agent's movement. There are three key value ranges to observe: positive, zero, negative. To do this: 
        
        1. Click on the `Straight` neuron
        
        2. Press the up/down arrow keys to control the neuron's activation (i.e., velocity)
        
        In addition to controlling the velocity, you can control the strength of the agent's sensors by changing the values of the weights from sensors to orientation. This changes how aggressive the agent turns in response to the object. To do this:
        
        1. Click on either weights between the sensor neurons and turn neurons
        
        2. Press the up/down arrow keys to control their strength (i.e., the agent's turn aggression towards the object) 
        
        # References
        
        Braitenberg, V. (1986). [_Vehicles: Experiments in synthetic psychology_](https://mitpress.mit.edu/9780262521123/vehicles/). MIT press.
            
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
            location = point(0, 0)
            width = 400
            height = 400
        }
        
        place(odorWorldComponent) {
            location = point(400, 0)
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