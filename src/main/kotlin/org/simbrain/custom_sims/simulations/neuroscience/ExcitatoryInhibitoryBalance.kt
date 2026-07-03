package org.simbrain.custom_sims.simulations.neuroscience

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.DistanceBased
import org.simbrain.network.connections.RandomWeightInitializer
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeurons
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.LogNormalDistribution
import kotlin.math.sqrt

val excitatoryInhibitoryBalance = newSim {

    val numNeurons = 400
    val gridSpace = 50.0
    val percentExcitatory = 80.0

    var excDrive = 0.0
    var inhDrive = 0.0

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("E/I Balance Network")
    val network = networkComponent.network

    val layout = GridLayout(gridSpace, gridSpace, (sqrt(numNeurons.toDouble())).toInt())

    val neurons = network.addNeurons(numNeurons) {
        updateRule = IzhikevichRule().apply {
            a = 0.02
            b = 0.2
            c = -65.0
            d = 2.0
            backgroundCurrent = 0.0
        }
    }

    val neuronCollection = NeuronCollection(neurons)
    network.addNetworkModelAsync(neuronCollection)

    layout.layoutNeurons(neurons)

    val numExcitatory = (numNeurons * percentExcitatory / 100.0).toInt()
    val shuffledIndices = neurons.indices.shuffled()
    shuffledIndices.take(numExcitatory).forEach { i ->
        neurons[i].polarity = Polarity.EXCITATORY
    }
    shuffledIndices.drop(numExcitatory).forEach { i ->
        neurons[i].polarity = Polarity.INHIBITORY
    }

    val distanceBased = DistanceBased(
        usePolarityMode = true,
        eeDecayFunction = GaussianDecayFunction(dispersion = 200.0).apply { baseMultiplier = 0.5 },
        eiDecayFunction = GaussianDecayFunction(dispersion = 150.0).apply { baseMultiplier = 0.3 },
        ieDecayFunction = GaussianDecayFunction(dispersion = 250.0).apply { baseMultiplier = 0.6 },
        iiDecayFunction = GaussianDecayFunction(dispersion = 150.0).apply { baseMultiplier = 0.3 },
        allowSelfConnections = false
    )

    distanceBased.weightInitializer = RandomWeightInitializer().apply {
        exRandomizer = LogNormalDistribution(3.0, 0.5, false)
        inRandomizer = LogNormalDistribution(3.2, 0.5, true)
    }

    val synapses = distanceBased.connectNeurons(neurons, neurons)

    network.addNetworkModelsAsync(synapses)

    network.flatSynapseList.forEach { s ->
        s.spikeResponder = JumpAndDecay().apply {
            timeConstant = 3.0
        }
    }

    neurons.take(20).forEach { it.activation = 30.0 }

    val rasterPlot = RasterPlotComponent("Spike Raster Plot")
    workspace.addWorkspaceComponent(rasterPlot)

    fun updateDrives() {
        neurons.forEach { neuron ->
            (neuron.updateRule as IzhikevichRule).backgroundCurrent =
                if (neuron.polarity == Polarity.EXCITATORY) excDrive else inhDrive
        }
    }

    withGui {
        getNetworkPanel(networkComponent).freeWeightsVisible = false
        val controlPanel = createControlPanel("E/I Balance Controls", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addSlider("Excitatory Drive", 0.0, 30.0, excDrive, 0.5) { value ->
                excDrive = value
                updateDrives()
            }
            addSlider("Inhibitory Drive", 0.0, 30.0, inhDrive, 0.5) { value ->
                inhDrive = value
                updateDrives()
            }
            addButton("Randomize Activations") {
                neuronCollection.randomize()
            }
            addButton("Clear Activation") {
                neurons.forEach { it.activation = 0.0 }
            }
        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 600, 600)
        place(rasterPlot, controlPanel.rightEdgeWithGap() + 600 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 600, 600)
    }

    with(couplingManager) {
        neuronCollection.getProducer(neuronCollection::spikes) couple
            rasterPlot.model.rasterConsumerList[0].getConsumer("setValues")
    }

    addSidebarInfo(
        """
        # Excitatory/Inhibitory Balance Network

        This simulation demonstrates the balance between excitatory and inhibitory neurons in a cortical network.
        You can control the drive to excitatory vs inhibitory populations and observe how this affects network dynamics,
        from suppressed activity to seizure-like behavior.

        # Simulation Details

        ## Background

        The cerebral cortex maintains a balance between excitation and inhibition. Approximately 80% of cortical
        neurons are excitatory (glutamatergic) and 20% are inhibitory (GABAergic). This E/I balance is critical for normal
        brain function. When inhibition is too weak, networks can enter pathological states resembling epileptic seizures
        with excessive firing. When inhibition is too strong, networks become suppressed and unresponsive.

        This simulation uses ~200 Izhikevich spiking neurons with distance-based connectivity that varies by cell type.
        The connectivity uses separate Gaussian decay functions for each polarity combination (E→E, E→I, I→E, I→I),
        creating more realistic cortical-like connectivity patterns. Each neuron is assigned excitatory or inhibitory
        polarity, which determines the sign of its outgoing synaptic connections.

        The key feature is independent control over excitatory and inhibitory drive via background current. This allows
        you to explore the parameter space from healthy balanced activity to pathological extremes.

        # What to Do

        ## Exploring E/I Balance

        1. Click `Run` to start the simulation with default parameters (both drives at 0).

        2. Adjust the `Excitatory Drive` slider to ~5-8. This provides tonic input to excitatory neurons. You should
        see increased spontaneous activity.

        3. Experiment with the `Inhibitory Drive` slider:
            - **High inhibitory drive** (e.g., 8-12): Network activity should be suppressed. Even with excitatory drive,
            inhibition dominates and prevents sustained firing.
            - **Balanced drive** (e.g., excitatory=6-8, inhibitory=4-6): Network should show irregular asynchronous activity,
            similar to cortical recordings.
            - **Low inhibitory drive** (e.g., excitatory=6-8, inhibitory=0-2): Network may show excessive synchrony,
            sustained bursts, or runaway activity resembling seizures.

        ## Other Things to Try

        - Select all neurons (`n`) and double-click to adjust Izhikevich parameters (a, b, c, d) for different firing patterns
        - Select all synapses (`w`) and double-click to modify spike responders or add learning rules like STDP
        - Try different ratios of excitatory drive to inhibitory drive and observe the transition from suppressed to
        balanced to hyperactive states
        - Adjust details of connectivity and other features directly in the script

        # References

        Brunel, N. (2000). [_Dynamics of sparsely connected networks of excitatory and inhibitory spiking neurons_](https://doi.org/10.1023/A:1008925309027).
        _Journal of Computational Neuroscience_, _8_(3), 183-208.

        Vogels, T. P., & Abbott, L. F. (2009). [_Gating multiple signals through detailed balance of excitation and inhibition in spiking networks_](https://doi.org/10.1038/nn.2276).
        _Nature Neuroscience_, _12_(4), 483-491.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        """.trimIndent()
    )
}
