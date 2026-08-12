/**
 * Headless verification of the modern Hopfield simulation's associative retrieval dynamics.
 */
package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.getModelByLabel

class ModernHopfieldHeadlessTest {
    @Test
    fun `selected memory becomes more probable under iterative retrieval`() = runBlocking {
        val scope = SimulationScope()
        modernHopfieldSim.task.invoke(scope, null)
        val network = (scope.workspace.getComponent("Modern Hopfield Network") as NetworkComponent).network
        val query = network.getModelByLabel<NeuronArray>("Query / current state")
        val retrieved = network.getModelByLabel<NeuronArray>("Retrieved pattern")
        val attention = network.getModelByLabel<NeuronArray>("Memory probabilities")

        query.setActivations(query.activationArray.mapIndexed { index, value ->
            if (index in 0..3) -value else value
        }.toDoubleArray())
        network.update()
        val initialProbability = attention.activationArray[0]
        query.setActivations(retrieved.activationArray)
        network.update()

        assertTrue(attention.activationArray[0] > initialProbability)
        assertTrue(attention.activationArray[0] > attention.activationArray.drop(1).max())
    }
}
