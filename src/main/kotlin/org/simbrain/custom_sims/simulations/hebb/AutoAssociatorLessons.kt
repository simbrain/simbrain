/** Combines the binary and bipolar nine-neuron Hebbian auto-associator lessons. */
package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.connect
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.place
import org.simbrain.network.NetworkComponent

val autoAssociator = newSim("auto_associator") {
    buildAutoAssociatorLesson()
}.registerReopenFunction { workspace ->
    val component = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val neurons = component.network.getModels(NeuronCollection::class.java).first()
    addAutoAssociatorControls(component, AutoAssociatorState(neurons, component.network.freeSynapses.toList()))
}

private suspend fun SimulationScope.buildAutoAssociatorLesson() {
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Hebbian associative memory")
    val network = networkComponent.network
    val neurons = network.addNeuronCollection(9).apply {
        label = AutoAssociatorState.labelFor(PatternEncoding.Binary)
        layout(GridLayout(70.0, 70.0))
        isClamped = true
        neuronList.forEach { neuron ->
            neuron.updateRule = LinearRule().apply {
                lowerBound = -1.0
                upperBound = 1.0
            }
        }
    }
    val synapses = network.connect(neurons.neuronList, neurons.neuronList, AllToAll()).apply {
        forEach { synapse ->
            synapse.strength = 0.0
            synapse.learningRule = HebbianRule().apply { learningRate = 1.0 }
        }
    }
    addSidebarInfo("""
        # Hebbian associative memory: binary and bipolar patterns

        This nine-neuron network represents items on a grocery list. Every neuron connects to every other neuron, with no self-connections. All 72 weights start at zero and use the Hebbian rule, Δw = source activation × target activation.

        ## Binary patterns: Part 1

        1. With **Pattern encoding** set to **Binary (0/1)**, load **Pattern 1** and click **Train one step**. Co-active neurons form positive connections.
        2. Click **Cue pattern 1**, then **Recall one step** a few times. A single active item should bring back the rest of its list.
        3. Load **Pattern 2** and train one step. Try recalling each list again. The patterns share the center item, so activity may spread into both memories.
        4. Try deleting one synapse and recalling again to explore redundancy.

        ## Bipolar patterns: Part 2

        Select **Bipolar (−1/+1)**. Changing encoding clears the old weights and activations, so the comparison begins fresh. Load and train each pattern again. Absent items now have activation −1; connections between present and absent items become inhibitory and can reduce interference between the two memories.

        Cue each pattern and compare recall. Loading a pattern enables learning; loading a cue freezes the weights for recall. You can also edit neuron activations manually before training another pattern.

        **Reset weights** clears the current exercise without changing its encoding.
    """.trimIndent())

    addAutoAssociatorControls(networkComponent, AutoAssociatorState(neurons, synapses))
}

private suspend fun SimulationScope.addAutoAssociatorControls(
    networkComponent: NetworkComponent,
    state: AutoAssociatorState
) {
    withGui {
        val controls = createControlPanel("Hebbian associative memory", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addComboBox("Pattern encoding", PatternEncoding.entries, state.encoding) { state.selectEncoding(it) }.apply {
                toolTipText = "Switch between 0/1 and −1/+1 patterns. Changing encoding resets weights and activations."
            }
            addButton("Pattern 1") { state.showPattern(0) }.apply {
                toolTipText = "Load the first pattern and enable Hebbian learning."
            }
            addButton("Pattern 2") { state.showPattern(1) }.apply {
                toolTipText = "Load the overlapping second pattern and enable Hebbian learning."
            }
            addButton("Train one step") {
                state.setTrainingMode(true)
                workspace.iterateSuspend()
            }.apply {
                toolTipText = "Update Hebbian weights once using the current neuron activations."
            }
            addSeparator()
            addButton("Cue pattern 1") { state.cue(0) }.apply {
                toolTipText = "Activate one item unique to pattern 1 and freeze weights for recall."
            }
            addButton("Cue pattern 2") { state.cue(2) }.apply {
                toolTipText = "Activate one item unique to pattern 2 and freeze weights for recall."
            }
            addButton("Recall one step") {
                state.setTrainingMode(false)
                workspace.iterateSuspend()
            }.apply {
                toolTipText = "Advance the network once without changing its weights."
            }
            addSeparator()
            addButton("Reset weights") { state.reset() }.apply {
                toolTipText = "Clear all learned weights and activations; keep the selected encoding."
            }
        }.awaitLayout()
        place(networkComponent, controls.rightEdgeWithGap(), SIM_WINDOW_GAP, 540, 540)
    }
}

internal enum class PatternEncoding(private val displayName: String) {
    Binary("Binary (0/1)"),
    Bipolar("Bipolar (−1/+1)");

    override fun toString() = displayName
}

internal class AutoAssociatorState(
    private val neurons: NeuronCollection,
    private val synapses: List<Synapse>
) {
    companion object {
        private val patterns = listOf(setOf(0, 3, 4, 7, 8), setOf(2, 4, 5, 6))

        fun labelFor(encoding: PatternEncoding) = "Hebbian associative memory (${encoding.name})"
    }

    var encoding = if (neurons.label == labelFor(PatternEncoding.Bipolar) ||
        neurons.label == "Hebbian Associative Memory (Bipolar)" ||
        neurons.label == "Hebbian Auto Associator (Bipolar)"
    ) PatternEncoding.Bipolar else PatternEncoding.Binary
        private set

    fun selectEncoding(selected: PatternEncoding) {
        if (selected == encoding) return
        encoding = selected
        neurons.label = labelFor(selected)
        reset()
    }

    fun showPattern(patternIndex: Int) {
        setTrainingMode(true)
        val active = patterns[patternIndex]
        neurons.setActivations(DoubleArray(neurons.neuronList.size) { index ->
            if (index in active) 1.0 else if (encoding == PatternEncoding.Bipolar) -1.0 else 0.0
        })
    }

    fun cue(index: Int) {
        setTrainingMode(false)
        neurons.setActivations(DoubleArray(neurons.neuronList.size) { if (it == index) 1.0 else 0.0 })
    }

    fun setTrainingMode(training: Boolean) {
        neurons.isClamped = training
        synapses.forEach { it.clamped = !training }
    }

    fun reset() {
        synapses.forEach { it.strength = 0.0 }
        neurons.setActivations(DoubleArray(neurons.neuronList.size))
        setTrainingMode(true)
    }
}
