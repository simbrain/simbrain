/** Checks the teaching difference between binary and bipolar Hebbian learning. */
package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronCollection
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class AutoAssociatorLessonsTest {
    @Test
    fun `binary lesson learns only positive connections among active items`() = runBlocking {
        val scope = SimulationScope()
        autoAssociator.task.invoke(scope, null)
        val network = (scope.workspace.getComponent("Hebbian associative memory") as NetworkComponent).network
        val neurons = network.getModels(NeuronCollection::class.java).first()
        val synapses = network.freeSynapses.toList()
        assertEquals(72, synapses.size)
        assertTrue(synapses.all { it.strength == 0.0 })

        neurons.setActivations(DoubleArray(9) { if (it in setOf(0, 3, 4, 7, 8)) 1.0 else 0.0 })
        network.update()
        assertTrue(synapses.any { it.strength > 0.0 })
        assertTrue(synapses.none { it.strength < 0.0 })
    }

    @Test
    fun `bipolar lesson also learns inhibitory connections`() = runBlocking {
        val scope = SimulationScope()
        autoAssociator.task.invoke(scope, null)
        val network = (scope.workspace.getComponent("Hebbian associative memory") as NetworkComponent).network
        val neurons = network.getModels(NeuronCollection::class.java).first()
        val synapses = network.freeSynapses.toList()
        val state = AutoAssociatorState(neurons, synapses)

        state.selectEncoding(PatternEncoding.Bipolar)
        state.showPattern(0)
        network.update()
        assertTrue(synapses.any { it.strength > 0.0 })
        assertTrue(synapses.any { it.strength < 0.0 })
    }

    @Test
    fun `switching encoding clears learning and restoring workspace keeps selection`() = runBlocking {
        val scope = SimulationScope()
        autoAssociator.task.invoke(scope, null)
        val network = (scope.workspace.getComponent("Hebbian associative memory") as NetworkComponent).network
        val neurons = network.getModels(NeuronCollection::class.java).first()
        val synapses = network.freeSynapses.toList()
        val state = AutoAssociatorState(neurons, synapses)

        state.showPattern(0)
        network.update()
        assertTrue(synapses.any { it.strength > 0.0 })
        state.selectEncoding(PatternEncoding.Bipolar)
        assertEquals(PatternEncoding.Bipolar, state.encoding)
        assertTrue(synapses.all { it.strength == 0.0 })
        assertTrue(neurons.activationArray.all { it == 0.0 })
        assertTrue(neurons.isClamped)

        val bytes = ByteArrayOutputStream().also { WorkspaceSerializer(scope.workspace).serialize(it, true) }.toByteArray()
        val restored = SimulationScope()
        WorkspaceSerializer(restored.workspace).deserialize(ByteArrayInputStream(bytes))
        autoAssociator.reopen(restored.workspace)
        val restoredNetwork = (restored.workspace.getComponent("Hebbian associative memory") as NetworkComponent).network
        val restoredNeurons = restoredNetwork.getModels(NeuronCollection::class.java).first()
        val restoredState = AutoAssociatorState(restoredNeurons, restoredNetwork.freeSynapses.toList())
        assertEquals(PatternEncoding.Bipolar, restoredState.encoding)
        restoredState.showPattern(0)
        assertTrue(restoredNeurons.activationArray.any { it == -1.0 })
    }

    @Test
    fun `loading a cue freezes weights and loading a pattern enables learning`() = runBlocking {
        val scope = SimulationScope()
        autoAssociator.task.invoke(scope, null)
        val network = (scope.workspace.getComponent("Hebbian associative memory") as NetworkComponent).network
        val neurons = network.getModels(NeuronCollection::class.java).first()
        val synapses = network.freeSynapses.toList()
        val state = AutoAssociatorState(neurons, synapses)

        state.cue(0)
        assertFalse(neurons.isClamped)
        assertTrue(synapses.all { it.clamped })

        state.showPattern(1)
        assertTrue(neurons.isClamped)
        assertTrue(synapses.none { it.clamped })
    }
}
