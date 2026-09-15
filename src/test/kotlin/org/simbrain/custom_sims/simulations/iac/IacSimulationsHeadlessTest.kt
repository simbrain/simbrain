/**
 * Runs every IAC simulation headlessly to check that each one builds its network component from its resource.
 */
package org.simbrain.custom_sims.simulations.iac

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.network.NetworkComponent

class IacSimulationsHeadlessTest {

    @Test
    fun `every iac simulation builds a populated network headlessly`() = runBlocking {
        val sims = mapOf(
            "Jets and Sharks" to iacJetsSharksFull,
            "Jets and Sharks (5 people)" to iacJetsSharks5People,
            "Games at Alivia's" to iacGames,
            "Language Classifier" to iacLanguages,
            "Movies" to iacMovies,
            "Novels" to iacNovels,
            "SpongeBob" to iacSpongeBob
        )
        sims.forEach { (componentName, sim) ->
            val scope = SimulationScope()
            sim.task.invoke(scope, null)
            val network = (scope.workspace.getComponent(componentName) as NetworkComponent).network
            assertTrue(network.freeNeurons.size >= 18, "$componentName neurons")
            assertTrue(network.freeSynapses.size > 100, "$componentName synapses")
        }
    }

    @Test
    fun `cueing one character in the spongebob network does not saturate the whole network`() = runBlocking {
        val scope = SimulationScope()
        iacSpongeBob.task.invoke(scope, null)
        val network = (scope.workspace.getComponent("SpongeBob") as NetworkComponent).network
        val spongeBob = network.freeNeurons.first { it.label == "SpongeBob" }
        spongeBob.activation = 1.0
        repeat(100) {
            network.update()
            spongeBob.activation = 1.0
        }
        val strongly = network.freeNeurons.count { it.activation > 0.5 }
        assertTrue(strongly in 2..12, "expected sparse retrieval, but $strongly nodes are above 0.5")
        assertTrue(network.freeNeurons.first { it.label == "Bikini Bottom" }.activation > 0.5)
        assertTrue(network.freeNeurons.first { it.label == "Protagonist" }.activation > 0.5)
    }

    @Test
    fun `directly named movie and language nodes retrieve their properties`() = runBlocking {
        for ((sim, componentName, retrieval) in listOf(
            Triple(iacMovies, "Movies", "Star Wars" to listOf("Sci-Fi", "Exciting")),
            Triple(iacLanguages, "Language Classifier", "Arabic" to listOf("Semitic", "Arabic / Abjad", "Fusional", "2"))
        )) {
            val (cue, expected) = retrieval
            val scope = SimulationScope()
            sim.task.invoke(scope, null)
            val network = (scope.workspace.getComponent(componentName) as NetworkComponent).network
            val node = network.freeNeurons.single { it.label == cue }
            node.clamped = true
            node.activation = 1.0
            repeat(100) { network.update() }
            for (label in expected) {
                assertTrue(network.freeNeurons.single { it.label == label }.activation > 0.5, "$cue retrieves $label")
            }
        }
    }

    @Test
    fun `sidebar text has no indented body lines that markdown would treat as code`() {
        val text = iacSidebarText(
            title = "Title",
            body = """
                First line
                second line continues here.
            """,
            credits = "Someone"
        )
        assertTrue(text.lines().none { it.startsWith("    ") }, text)
        assertTrue(text.startsWith("# Title"))
    }
}
