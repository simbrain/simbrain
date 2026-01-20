package org.simbrain.custom_sims.simulations.dynamical_systems

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.core.getSynapse
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.DecayRule
import org.simbrain.network.updaterules.ProductRule
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.DecayColoringManager
import org.simbrain.util.projection.PCAProjection

/**
 * Pure Simbrain implementation of the Lorenz System using only network components.
 *
 * This version implements the Lorenz equations using product rules and linear rules
 * with weighted connections, without any custom update actions. It serves as a validation
 * of the hand-coded version and demonstrates how complex dynamical systems can be
 * implemented using standard neural network components.
 *
 * The Lorenz equations:
 * dx/dt = σ(y - x)
 * dy/dt = x(ρ - z) - y
 * dz/dt = xy - βz
 */
val lorenzSystemSimbrain = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Lorenz System (Pure Simbrain)")
    val network = networkComponent.network

    // Set smaller time step for numerical stability
    network.timeStep = 0.01

    // Lorenz parameters (standard values for chaotic behavior)
    var sigma = 10.0
    var rho = 28.0
    var beta = 8.0 / 3.0

    // Create the three main state variable neurons (x, y, z)
    // Using DecayRule which does: activation += input (perfect for Euler integration!)
    val xNeuron = network.addNeuron {
        label = "x"
        updateRule = DecayRule().apply {
            baseLine = 0.0
            decayFraction = 0.0  // No decay - we handle it via synaptic weights
        }
        activation = 1.0
        upperBound = 100.0
        lowerBound = -100.0
    }

    val yNeuron = network.addNeuron {
        label = "y"
        updateRule = DecayRule().apply {
            baseLine = 0.0
            decayFraction = 0.0
        }
        activation = 1.0
        upperBound = 100.0
        lowerBound = -100.0
    }

    val zNeuron = network.addNeuron {
        label = "z"
        updateRule = DecayRule().apply {
            baseLine = 0.0
            decayFraction = 0.0
        }
        activation = 1.0
        upperBound = 100.0
        lowerBound = -100.0
    }

    // Create product neurons for nonlinear terms
    // xy product for dz/dt
    val xyProduct = network.addNeuron {
        label = "xy"
        updateRule = ProductRule().apply {
            useWeights = false
        }
        location = point(150.0, 200.0)
    }

    // xz product for dy/dt (x(ρ - z) = xρ - xz)
    val xzProduct = network.addNeuron {
        label = "xz"
        updateRule = ProductRule().apply {
            useWeights = false
        }
        location = point(250.0, 200.0)
    }

    // Create neuron collection for the state variables
    val lorenzNeurons = NeuronCollection(listOf(xNeuron, yNeuron, zNeuron))
    lorenzNeurons.apply {
        label = "Lorenz Variables"
        layout(GridLayout())
        location = point(50.0, 50.0)
    }
    network.addNetworkModel(lorenzNeurons)

    // Build the network structure implementing the Lorenz derivatives
    // DecayRule does: activation += input, so we just need to provide dx/dt * timeStep as input
    // The timeStep is factored into the weights

    // dx/dt = σ(y - x)
    // Input to x should be: σ(y - x) * dt = σy*dt - σx*dt
    network.addSynapse(yNeuron, xNeuron).apply {
        strength = sigma * network.timeStep
    }
    network.addSynapse(xNeuron, xNeuron).apply {
        strength = -sigma * network.timeStep
    }

    // dy/dt = x(ρ - z) - y = xρ - xz - y
    // Input to y should be: (xρ - xz - y) * dt = xρ*dt - xz*dt - y*dt
    network.addSynapse(xNeuron, yNeuron).apply {
        strength = rho * network.timeStep
    }
    network.addSynapse(yNeuron, yNeuron).apply {
        strength = -network.timeStep
    }

    // Connect to xz product
    network.addSynapse(xNeuron, xzProduct)
    network.addSynapse(zNeuron, xzProduct)
    network.addSynapse(xzProduct, yNeuron).apply {
        strength = -network.timeStep
    }

    // dz/dt = xy - βz
    // Input to z should be: (xy - βz) * dt = xy*dt - βz*dt
    network.addSynapse(zNeuron, zNeuron).apply {
        strength = -beta * network.timeStep
    }

    // Connect to xy product
    network.addSynapse(xNeuron, xyProduct)
    network.addSynapse(yNeuron, xyProduct)
    network.addSynapse(xyProduct, zNeuron).apply {
        strength = network.timeStep
    }

    // Add projection plot to visualize the attractor
    val projectionPlot = addProjectionPlot("Lorenz Attractor (Simbrain)")
    projectionPlot.projector.tolerance = 0.1
    projectionPlot.projector.projectionMethod = PCAProjection()
    projectionPlot.projector.coloringManager = DecayColoringManager()

    // Couple the neuron collection to the projection plot
    with(couplingManager) {
        lorenzNeurons couple projectionPlot
    }


    // Helper function to update synaptic weights when parameters change
    fun updateWeights() {
        // Update dx/dt weights: dx/dt = σ(y - x)
        getSynapse(yNeuron, xNeuron)?.strength = sigma * network.timeStep
        getSynapse(xNeuron, xNeuron)?.strength = -sigma * network.timeStep

        // Update dy/dt weights: dy/dt = xρ - xz - y
        getSynapse(xNeuron, yNeuron)?.strength = rho * network.timeStep
        getSynapse(yNeuron, yNeuron)?.strength = -network.timeStep

        // Update dz/dt weights: dz/dt = xy - βz
        getSynapse(zNeuron, zNeuron)?.strength = -beta * network.timeStep
    }

    withGui {
        place(projectionPlot, 638, 7, 519, 485)
        place(networkComponent, 259, 6, 386, 493)

        // Control panel for changing parameters
        createControlPanel("Lorenz Parameters", 4, 6) {

            addButton("Reset to Initial Conditions") {
                xNeuron.activation = 1.0
                yNeuron.activation = 1.0
                zNeuron.activation = 1.0
            }

            addSeparator()
            addLabel("<html><b>Parameter Presets</b></html>")

            addButton("Chaotic (Standard)") {
                sigma = 10.0
                rho = 28.0
                beta = 8.0 / 3.0
                updateWeights()
            }

            addButton("Stable - Converge to Origin") {
                sigma = 10.0
                rho = 0.5
                beta = 8.0 / 3.0
                updateWeights()
            }

            addButton("Stable - Fixed Points") {
                sigma = 10.0
                rho = 10.0
                beta = 8.0 / 3.0
                updateWeights()
            }

            addButton("Pre-Chaotic (Complex)") {
                sigma = 10.0
                rho = 24.5
                beta = 8.0 / 3.0
                updateWeights()
            }

            addSeparator()
            addLabel("<html><b>Custom Parameters</b></html>")

            val sigmaField = addTextField("σ (sigma)", "10.0")
            val rhoField = addTextField("ρ (rho)", "15.0")
            val betaField = addTextField("β (beta)", "2.6667")

            addButton("Apply Custom Parameters") {
                sigma = sigmaField.text.toDoubleOrNull() ?: sigma
                rho = rhoField.text.toDoubleOrNull() ?: rho
                beta = betaField.text.toDoubleOrNull() ?: beta

                sigmaField.text = sigma.toString()
                rhoField.text = rho.toString()
                betaField.text = beta.toString()

                updateWeights()
            }
        }
    }


    addSidebarInfo(
        """
        # Lorenz System (Pure Simbrain Implementation)

        A pure Simbrain implementation of the Lorenz attractor using only standard network components—no custom update actions. This demonstrates how complex dynamical systems can be built using decay rules (which do Euler integration), product rules (for nonlinear terms), and weighted connections.

        ## Background

        This simulation demonstrates an important principle: complex dynamical systems can be implemented as neural networks. The Lorenz equations, typically solved numerically with integration methods, are instead computed by a recurrent network where:

        - Neurons represent state variables
        - Synapses represent derivative terms
        - Product neurons handle nonlinearities
        - Network updates approximate continuous dynamics

        This connection between dynamical systems and neural networks underlies much of computational neuroscience and reservoir computing.

        # Simulation Details

        This simulation implements the Lorenz equations using neural network building blocks:

        - `dx/dt = σ(y - x)`
        - `dy/dt = x(ρ - z) - y`
        - `dz/dt = xy - βz`

        ## Network Architecture

        **State Variables**: Three neurons (`x`, `y`, `z`) with `DecayRule` update rules. The DecayRule performs Euler integration: `activation += input`.

        **Product Neurons**: Two neurons with `ProductRule` compute nonlinear terms:
        - `xy` neuron: Computes the product `x·y` for the `dz/dt` equation
        - `xz` neuron: Computes the product `x·z` for the `dy/dt` equation

        **Weighted Connections**: Synaptic weights encode the derivatives. Since DecayRule adds input to activation, the weights directly represent `derivative * timeStep`.

        ## How It Works

        The key insight is using `DecayRule` with `decayFraction = 0`, which does:
        ```
        activation += input
        ```

        This is exactly Euler integration! The weighted inputs provide `dx/dt * timeStep`, and DecayRule adds them to the current activation.

        For example, for `dx/dt = σ(y - x)`:
        - `y→x` connection has weight `σ*dt` (contributes +`σy*dt` to input)
        - `x→x` connection has weight `-σ*dt` (contributes -`σx*dt` to input)
        - DecayRule computes: `x += (σy*dt - σx*dt) = x + σ(y - x)*dt` ✓

        No cheating with time steps in self-connections! The update rule itself handles integration, and synaptic weights encode the actual derivative coefficients.

        # What to Do

        Press `Play` to start the simulation. The behavior is identical to the hand-coded version—this validates that both implementations produce the same Lorenz attractor.

        ## Explore Different Regimes

        Use the preset buttons to explore different dynamical behaviors:

        1. Click `Chaotic (Standard)` and run the simulation. The butterfly attractor emerges in the projection plot.

        2. Try `Stable - Converge to Origin` to see trajectories collapse toward zero.

        3. Explore `Stable - Fixed Points` to observe convergence to steady states.

        4. Test `Pre-Chaotic (Complex)` for transitional behavior near the chaos threshold.

        ## Compare with Hand-Coded Version

        Open the standard `Lorenz attractor` simulation alongside this one and run both with identical parameters. The trajectories should match, validating this pure Simbrain approach.

        ## Experiment with Network Structure

        This implementation exposes the underlying network structure:

        - Right-click neurons to see their update rules (Linear or Product)
        - Click on synapses to view weights (which encode `σ`, `ρ`, `β` scaled by time step)
        - Observe how product neurons compute nonlinear terms
        - Watch the network update to see how derivatives flow through connections

        ## Modify Parameters

        When you change parameters using the control panel:

        1. Select a preset or enter custom values
        2. Click `Apply Custom Parameters`
        3. The synaptic weights update automatically to reflect new parameter values
        4. Right-click the projection plot and `Clear data`
        5. Run to see the new dynamics

        # References

        Lorenz, E. N. (1963). [Deterministic nonperiodic flow](https://doi.org/10.1175/1520-0469(1963)020<0130:DNF>2.0.CO;2). _Journal of the Atmospheric Sciences_, _20_(2), 130-141.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        """.trimIndent()
    )

    network.events.zoomToFitPage.fire()
}
