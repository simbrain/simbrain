package org.simbrain.custom_sims.simulations.behaviorism

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.getModelById
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.updaterules.BinaryRule
import org.simbrain.util.place
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.updater.updateAction
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.ObjectSensor

/**
 * Simulation to demonstrate classical and operant conditioning.
 * Discriminative case
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
val classicalConditioning = newSim("classical conditioning") {

    workspace.clearWorkspace()
    val nc = addNetworkComponent("Agent Brain (Black Box)")
    val net = nc.network

    // Construct the network
    val bellDetectorNeuron = Neuron()
    net.addNetworkModelAsync(bellDetectorNeuron)
    bellDetectorNeuron.setLocation(295.0, 194.0)
    bellDetectorNeuron.label = "Bell Detector"

    val cheeseDetectorNeuron = Neuron()
    net.addNetworkModelAsync(cheeseDetectorNeuron)
    cheeseDetectorNeuron.setLocation(160.0, 194.0)
    cheeseDetectorNeuron.label = "Cheese Detector"

    val responseRule = BinaryRule()
    responseRule.threshold = .5
    responseRule.lowerBound = 0.0
    val salivationResponse = Neuron()
    net.addNetworkModelAsync(salivationResponse)
    salivationResponse.setLocation(160.0, 60.0)
    salivationResponse.label = "Salivation"

    val cheeseToSalivation = Synapse(cheeseDetectorNeuron, salivationResponse, 1.0)
    net.addNetworkModelAsync(cheeseToSalivation)

    val association = Synapse(bellDetectorNeuron, cheeseDetectorNeuron).apply {
        strength = 0.0
        lowerBound = 0.0
        upperBound = 1.0
    }

    net.addNetworkModelAsync(association)
    withGui {
        (getDesktopComponent(nc) as NetworkDesktopComponent)
            .networkPanel.selectionManager.clear()
        place(nc, 0, 14, 350, 443)
    }

    // Create the odor world
    val oc = addOdorWorldComponent("Environment")
    oc.world.isObjectsBlockMovement = false
    oc.world.isUseCameraCentering = false

    val mouse = oc.world.addEntity(125, 211, EntityType.Mouse)
    mouse.heading = 90.0

    // Set up world
    val cheese = oc.world.addEntity(13, 67, EntityType.Swiss)
    val bell = oc.world.addEntity(234, 67, EntityType.Bell)

    // Set up object sensors
    val swissSensor = mouse.addObjectSensor(EntityType.Swiss, 10.0, 0.0, 45.0)
    val bellSensor = mouse.addObjectSensor(EntityType.Bell, 10.0, 0.0, 45.0)

    // Create a time series plot
    val plot = addTimeSeriesComponent("Association Strength", "Strength")
    plot.model.isAutoRange = false
    plot.model.fixedWidth = true
    plot.model.windowSize = 1500
    with(couplingManager) {
        // Couple sensors to neurons
        swissSensor.getProducer(ObjectSensor::currentValue) couple
                cheeseDetectorNeuron.getConsumer(Neuron::addInputValue)
        bellSensor.getProducer(ObjectSensor::currentValue) couple
                bellDetectorNeuron.getConsumer(Neuron::addInputValue)

        // Plot association strength
        association.getProducer("getStrength") couple
                plot.model.timeSeriesList[0].getConsumer("setValue")
    }

    withGui {
        place(nc, 0, 14, 350, 443)
        place(oc, 351, 13, 377, 444)
        place(plot, 728, 13, 406, 444)
    }

    addSidebarInfo(
        """
        # Classical Conditioning in Simbrain
        
        ## Basic Definitions and Setup
        
        This lesson demonstrates classical conditioning using a simple simulation in Simbrain. In this simulation, visual elements such as circles and synapses are used to represent behaviors and associations rather than actual neurons.
        
        Key terms:
        
        - **Unconditioned Stimulus (US):** A stimulus that naturally triggers a response (e.g., cheese).
        - **Unconditioned Response (UR):** A natural, automatic reaction to the unconditioned stimulus (e.g., mouse salivates or moves toward cheese).
        - **Neutral Stimulus (NS):** A stimulus that initially does not trigger any response (e.g., bell).
        - **Conditioned Stimulus (CS):** A previously neutral stimulus that, after being paired with the US, elicits a response.
        - **Conditioned Response (CR):** The learned response to the conditioned stimulus.
        - **Extinction:** The weakening of the association when the CS is presented without the US over time.
        
        To get started, run the simulation. You can move items like the bell and cheese near the agent (mouse) to observe learning in action.
        
        > **Note:** Although this simulation uses neurons and synapses, think of the connections as abstract representations of behavioral associations, not biological accuracy.
        
        ---
        
        ## Walkthrough
        
        1. **Unconditioned Stimulus and Response**
           - Run the simulation and move the **cheese** near the **mouse**.
           - The mouse will respond (e.g., approach or activate). 
           - **Cheese** is the **US**; the mouse's reaction is the **UR**.
        
        2. **Neutral Stimulus**
           - Move the **bell** near the mouse.
           - There is **no response**—the bell is currently a **neutral stimulus (NS)**.
        
        3. **Pairing NS with US**
           - Move both the **bell** and **cheese** near the mouse.
           - Run the simulation.
           - The mouse begins to associate the bell with the cheese.
           - The bell becomes a **conditioned stimulus (CS)** as it begins to evoke a response.
        
        4. **Conditioned Response**
           - Eventually, the **bell alone** will trigger the same behavior.
           - The response to the bell is now a **conditioned response (CR)**.
        
        5. **Extinction**
           - Show the **bell alone** repeatedly, without the cheese.
           - The response gradually disappears—this is **extinction**.
        
        6. **Tracking Learning**
           - The **strength of the association** is shown in the **time series window**.
           - When this strength exceeds **0.7**, the CS-CR link is well established.
        
        ---
        
        ## Lesson Activities
        
        Provide your own examples of US, UR, NS, etc. out loud.  They can be from everyday life. As you do this show how these concepts
        work in Simbrain.
        
        1. **Unconditioned Response Demo**
           - Provide your own example of a **US** and **UR**.
           - Demonstrate by dragging the agent over the cheese and observing the neuron produce the UR.
        
        2. **Neutral Stimulus Demo**
           - Provide your own example of a **neutral stimulus (NS)**.
           - Show that the bell does **not produce a response** when presented alone.
        
        3. **Training a Conditioned Response**
           - Describe your own example of an NS becoming a US
           - Show this in Simrian by pairing the **NS** with the **US** by placing both near the agent.
           - Run the simulation until the **association strength > 0.7**.
           - Identify the new **CS** and the resulting **CR**.
        
        You are encouraged to explore different pairings and observe how associations are formed and extinguished.

            """.trimIndent()
    )

    setUpClassicalConditioning(workspace)

}.registerReopenFunction { workspace -> setUpClassicalConditioning(workspace) }

fun setUpClassicalConditioning(workspace: Workspace) {

    val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
    val bellDetectorNeuron = network.getModelByLabel<Neuron>("Bell Detector")
    val cheeseDetectorNeuron = network.getModelByLabel<Neuron>("Cheese Detector")
    val association = network.getModelById<Synapse>("Synapse_2")

    // Add custom network update action
    // This must happen before buffered update (which clear input values)
    network.updateManager.addAction(0, updateAction("Custom behaviorism update") {
        if ((bellDetectorNeuron.input > 0.0) && (cheeseDetectorNeuron.input > 0.0)) {
            // Learning
            association.increment = .001 // learning rate
            association.increment()
        } else if ((bellDetectorNeuron.input > 0.0) && (cheeseDetectorNeuron.input <= 0.0)) {
            // Extinction
            association.increment = .0005 // extinction rate. helps to have it low when demoing
            association.decrement()
        }
    })
}
