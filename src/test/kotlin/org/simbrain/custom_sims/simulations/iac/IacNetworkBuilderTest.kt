/**
 * Checks the IAC builder DSL (pool inhibition, instance links, layout, and label lookup) and that every IAC
 * simulation builds the expected structure and retrieves an instance's properties when its name is cued.
 */
package org.simbrain.custom_sims.simulations.iac

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.NewSimulation
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.IACRule
import org.simbrain.util.point

class IacNetworkBuilderTest {

    @Test
    fun `pools inhibit within and instances excite across, symmetrically`() = runBlocking {
        val network = Network()
        val iac = network.iacNetwork(excitatory = 0.5, inhibitory = -0.25) {
            val colors = pool("Colors", "Red", "Blue", at = point(0, 0))
            val sizes = pool("Sizes", "Big", "Small", "Medium", at = point(0, 200))
            instances("Things", at = point(0, 100)) {
                instance("Apple", colors["Red"], sizes["Small"])
                instance(colors["Blue"], sizes["Big"])
            }
        }
        assertEquals(7, network.freeNeurons.size)
        val expectedInhibitory = 2 * 1 + 3 * 2 + 2 * 1
        val expectedExcitatory = 2 * 2 * 2
        assertEquals(expectedInhibitory + expectedExcitatory, network.freeSynapses.size)
        assertTrue(network.freeSynapses.all { it.strength == 0.5 || it.strength == -0.25 })
        network.freeSynapses.forEach { synapse ->
            val reverse = network.freeSynapses.first { it.source == synapse.target && it.target == synapse.source }
            assertEquals(synapse.strength, reverse.strength)
        }
        assertEquals(3, iac.pools.size)
        assertEquals(setOf("Colors", "Sizes", "Things"), iac.pools.map { it.label }.toSet())
        assertTrue(iac.neurons.all { it.updateRule is IACRule })
        assertEquals("Apple", iac.pool("Things").neurons.first().label)
    }

    @Test
    fun `pools are laid out around the requested location`() = runBlocking {
        val network = Network()
        val iac = network.iacNetwork {
            pool("Sizes", "Big", "Small", "Medium", "Tiny", at = point(300, -100), columns = 2)
        }
        val pool = iac.pool("Sizes")
        assertEquals(300.0, pool.collection.location.x, 1e-6)
        assertEquals(-100.0, pool.collection.location.y, 1e-6)
        assertEquals(4, pool.neurons.map { it.location }.toSet().size)
        assertEquals(2, pool.neurons.map { it.location.x }.toSet().size)
    }

    @Test
    fun `an unknown node label fails loudly`() = runBlocking {
        val network = Network()
        val error = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                network.iacNetwork {
                    val colors = pool("Colors", "Red", at = point(0, 0))
                    colors["Green"]
                }
            }
        }
        assertTrue(error.message!!.contains("Green"))
    }

    private val expectedStructure = mapOf(
        "Jets and Sharks" to Triple(iacJetsSharksFull, 68, 1754),
        "Jets and Sharks (5 people)" to Triple(iacJetsSharks5People, 24, 124),
        "Games at Alivia's" to Triple(iacGames, 83, 3168),
        "Language Classifier" to Triple(iacLanguages, 57, 1024),
        "Movies" to Triple(iacMovies, 44, 638),
        "Novels" to Triple(iacNovels, 27, 234),
        "SpongeBob" to Triple(iacSpongeBob, 52, 746)
    )

    private suspend fun build(sim: NewSimulation, componentName: String): Network {
        val scope = SimulationScope()
        sim.task.invoke(scope, null)
        return (scope.workspace.getComponent(componentName) as NetworkComponent).network
    }

    @Test
    fun `every iac simulation builds the same structure as the original workspace`() = runBlocking {
        expectedStructure.forEach { (componentName, expected) ->
            val (sim, neurons, synapses) = expected
            val network = build(sim, componentName)
            assertEquals(neurons, network.freeNeurons.size, "$componentName neurons")
            assertEquals(synapses, network.freeSynapses.size, "$componentName synapses")
        }
    }

    @Test
    fun `activating a name retrieves that person's properties in the full jets and sharks network`() = runBlocking {
        val network = build(iacJetsSharksFull, "Jets and Sharks")
        val ralph = network.freeNeurons.first { it.label == "Ralph" }
        val instance = network.freeSynapses.first { it.source == ralph && it.strength > 0 }.target
        val properties = network.freeSynapses
            .filter { it.source == instance && it.strength > 0 && it.target != ralph }
            .map { it.target }
        assertEquals(5, properties.size)

        ralph.activation = 1.0
        repeat(100) {
            network.update()
            ralph.activation = 1.0
        }

        val pools = network.freeNeurons.groupBy { neuron ->
            network.allModels.filterIsInstance<org.simbrain.network.core.NeuronCollection>().first { neuron in it.neuronList }
        }
        properties.forEach { property ->
            val pool = pools.keys.first { property in it.neuronList }
            val mostActive: Neuron = pool.neuronList.maxBy { it.activation }
            assertEquals(property, mostActive, "most active node in ${pool.label} after cueing Ralph")
        }
    }
}
