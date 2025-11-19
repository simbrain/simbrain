package org.simbrain.custom_sims.simulations.dynamical_systems

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.labels
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.DecayColoringManager
import org.simbrain.util.projection.PCAProjection

/**
 * Lorenz System Simulation
 *
 * A demonstration of the famous Lorenz attractor, one of the first examples of chaotic dynamics.
 * The system exhibits sensitive dependence on initial conditions - the "butterfly effect."
 *
 * The Lorenz equations are:
 * dx/dt = σ(y - x)
 * dy/dt = x(ρ - z) - y
 * dz/dt = xy - βz
 *
 * Standard parameters: σ = 10, ρ = 28, β = 8/3
 */
val lorenzSystem = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Lorenz System")
    val network = networkComponent.network

    // Set smaller time step for numerical stability
    network.timeStep = 0.01

    // Create a neuron collection for the three state variables (x, y, z)
    val lorenzNeurons = network.addNeuronCollection(3) {
        updateRule = LinearRule()
        activation = 1.0  // Initial condition
    }
    lorenzNeurons.apply {
        label = "Lorenz Variables"
        layout(LineLayout())
        location = point(50.0, 100.0)
        neuronList.labels = listOf("x", "y", "z")
    }

    // Lorenz parameters (standard values for chaotic behavior)
    var sigma = 10.0
    var rho = 28.0
    var beta = 8.0 / 3.0

    // Clear default network update and add custom update action
    network.updateManager.clear()
    workspace.addUpdateAction("Lorenz System Update", position = 0) {

        // Get current state
        val x = lorenzNeurons.neuronList[0].activation
        val y = lorenzNeurons.neuronList[1].activation
        val z = lorenzNeurons.neuronList[2].activation

        val dt = network.timeStep

        // Lorenz equations
        val dx = sigma * (y - x)
        val dy = x * (rho - z) - y
        val dz = x * y - beta * z

        // Update using Euler method
        lorenzNeurons.neuronList[0].activation = x + dx * dt
        lorenzNeurons.neuronList[1].activation = y + dy * dt
        lorenzNeurons.neuronList[2].activation = z + dz * dt
    }

    // Add projection plot to visualize the attractor
    val projectionPlot = addProjectionPlot("Lorenz Attractor")
    projectionPlot.projector.tolerance = 0.1
    projectionPlot.projector.projectionMethod = PCAProjection()
    projectionPlot.projector.coloringManager = DecayColoringManager()

    // Couple the neuron collection to the projection plot
    with(couplingManager) {
        lorenzNeurons couple projectionPlot
    }

    withGui {
        place(projectionPlot, 638, 7, 519, 485)
        place(networkComponent, 259, 6, 386, 493)

        // Control panel for changing parameters and observing different regimes
        createControlPanel("Lorenz Parameters", 4, 6) {

            addButton("Reset to Initial Conditions") {
                lorenzNeurons.neuronList[0].activation = 1.0
                lorenzNeurons.neuronList[1].activation = 1.0
                lorenzNeurons.neuronList[2].activation = 1.0
            }

            addSeparator()
            addLabel("<html><b>Parameter Presets</b></html>")

            addButton("Chaotic (Standard)") {
                sigma = 10.0
                rho = 28.0
                beta = 8.0 / 3.0
            }

            addButton("Stable - Converge to Origin") {
                sigma = 10.0
                rho = 0.5  // ρ < 1
                beta = 8.0 / 3.0
            }

            addButton("Stable - Fixed Points") {
                sigma = 10.0
                rho = 10.0  // 1 < ρ < ~24.74
                beta = 8.0 / 3.0
            }

            addButton("Pre-Chaotic (Complex)") {
                sigma = 10.0
                rho = 24.5  // Just below chaos threshold
                beta = 8.0 / 3.0
            }

            addSeparator()
            addLabel("<html><b>Custom Parameters</b></html>")

            val sigmaField = addTextField("σ (sigma)", "10.0")
            val rhoField = addTextField("ρ (rho)", "15.0")
            val betaField = addTextField("β (beta)", "2.7")

            addButton("Apply Custom Parameters") {
                sigma = sigmaField.text.toDoubleOrNull() ?: sigma
                rho = rhoField.text.toDoubleOrNull() ?: rho
                beta = betaField.text.toDoubleOrNull() ?: beta

                // Update text fields to show actual values
                sigmaField.text = sigma.toString()
                rhoField.text = rho.toString()
                betaField.text = beta.toString()
            }
        }
    }

    addSidebarInfo(
        """
        # Lorenz System

        A demonstration of the famous Lorenz attractor, one of the first examples of chaotic dynamics discovered by Edward Lorenz in 1963. The system exhibits sensitive dependence on initial conditions—the "butterfly effect"—where tiny changes in starting values lead to dramatically different trajectories.

        # Simulation Details

        The Lorenz system is governed by three coupled differential equations:

        - dx/dt = σ(y - x)
        - dy/dt = x(ρ - z) - y
        - dz/dt = xy - βz

        Where `x`, `y`, and `z` are state variables representing simplified atmospheric convection, and `σ` (sigma), `ρ` (rho), and `β` (beta) are parameters controlling the system's behavior.

        ## Implementation

        This simulation uses three neurons in the `Lorenz Variables` collection to represent `x`, `y`, and `z`. The equations are implemented using a custom workspace update action that directly computes the derivatives and updates the neuron activations using the Euler integration method with a time step of `0.01`.

        ## Dynamic Regimes

        The Lorenz system exhibits qualitatively different behaviors depending on parameter values:

        **Stable - Converge to Origin** (ρ < 1): All trajectories converge to the origin (0, 0, 0). The system is predictable and stable.

        **Stable - Fixed Points** (1 < ρ < ~24.74): Trajectories converge to one of two symmetric steady states. The system settles into non-zero equilibria.

        **Pre-Chaotic** (ρ ≈ 24.5): Near the chaos threshold, the system exhibits complex oscillations. This transitional regime shows increasingly intricate dynamics.

        **Chaotic** (ρ = 28, standard parameters): The famous butterfly attractor emerges. Trajectories loop unpredictably around two centers, never repeating but remaining bounded. This is the regime that made the Lorenz system famous.

        # What to Do

        Press `Play` in the toolbar to start the simulation. The `Lorenz Attractor` projection plot shows the system's trajectory in 2D using PCA, with recent points shown in red that fade to transparency over time.

        ## Explore Different Regimes

        Use the preset buttons in the `Lorenz Parameters` control panel to explore different dynamical behaviors:

        1. Click `Chaotic (Standard)` and run the simulation. Watch the characteristic butterfly or owl-face shape emerge in the projection plot as the trajectory loops around two centers.

        2. Click `Reset to Initial Conditions`, then try `Stable - Converge to Origin`. The trajectory collapses toward the center as all variables decay to zero.

        3. Try `Stable - Fixed Points` to see the system converge to a single point (one of two possible steady states).

        4. Explore `Pre-Chaotic (Complex)` to observe transitional behavior near the edge of chaos.

        ## Test Sensitivity to Initial Conditions

        With chaotic parameters active:

        1. Click `Reset to Initial Conditions` and let the simulation run briefly
        2. Note the trajectory pattern in the projection plot
        3. Click inside the `Lorenz System` network window and manually adjust one neuron's activation slightly (e.g., change `x` from `1.0` to `1.001`)
        4. Watch how quickly the trajectories diverge despite nearly identical starting points

        ## Experiment with Parameters

        The `Custom Parameters` section shows values for a stable fixed-point regime (σ=10, ρ=15, β=2.67). Try these or your own values:

        1. Enter custom values in the parameter fields
        2. Click `Apply Custom Parameters`
        3. Click `Reset to Initial Conditions`
        4. Right-click on the `Lorenz Attractor` projection plot and select `Clear data` to remove old trajectories
        5. Run the simulation to see the new behavior

        **Important**: Always clear the projection plot data when changing parameters to avoid mixing trajectories from different parameter regimes.

        ## Visualization Tips

        - The projection plot uses decay coloring: recent points appear red and gradually fade to transparency, making it easy to see the current trajectory
        - For chaotic parameters, the butterfly shape becomes visible after ~500 iterations
        - Right-click the `Lorenz Variables` neuron collection and create a time series plot to see how `x`, `y`, and `z` evolve over time
        - Different regimes produce distinct patterns: settling points (stable), loops (pre-chaotic), or irregular wandering (chaotic)

        # Background

        Edward Lorenz discovered this system in 1963 while studying atmospheric convection for weather prediction. When he re-ran a simulation using slightly rounded initial conditions, he found completely different results. This led to the discovery of deterministic chaos and the popularization of the "butterfly effect"—the idea that a butterfly flapping its wings in Brazil could theoretically cause a tornado in Texas.

        The Lorenz attractor became an icon of chaos theory, demonstrating how simple deterministic rules can produce unpredictable behavior. It has influenced fields from mathematics and physics to philosophy and art.

        # References

        Lorenz, E. N. (1963). [Deterministic nonperiodic flow](https://doi.org/10.1175/1520-0469(1963)020<0130:DNF>2.0.CO;2). _Journal of the Atmospheric Sciences_, _20_(2), 130-141.

        # Credits

        Jeff Yoshimi
        """.trimIndent()
    )

    network.events.zoomToFitPage.fire()
}
