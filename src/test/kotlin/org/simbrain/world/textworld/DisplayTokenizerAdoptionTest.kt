package org.simbrain.world.textworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.llm.TeachingTransformer
import org.simbrain.network.llm.TeachingTransformerConfig
import org.simbrain.util.CharacterTokenizer
import org.simbrain.workspace.Workspace

class DisplayTokenizerAdoptionTest {

    private fun awaitAdoption(world: TextWorld): Boolean {
        repeat(100) {
            if (world.displayTokenizer != null) return true
            Thread.sleep(10)
        }
        return false
    }

    private class Rig {
        val workspace = Workspace()
        val world: TextWorld
        val transformer: TeachingTransformer
        val textWorldComponent: TextWorldComponent

        init {
            val network = Network()
            workspace.addWorkspaceComponent(NetworkComponent("net", network))
            textWorldComponent = TextWorldComponent("text")
            workspace.addWorkspaceComponent(textWorldComponent)
            world = textWorldComponent.world
            transformer = TeachingTransformer(TeachingTransformerConfig(
                contextSize = 6, embedDim = 8, numHeads = 2, hiddenDim = 8, vocabSize = 6, numLayers = 1,
            ))
            transformer.tokenLabels = arrayListOf("a", "b", "c", "d", "e", "f")
            transformer.tokenizer = CharacterTokenizer()
            runBlocking { network.addNetworkModel(transformer) }
        }
    }

    @Test
    fun `a document coupling adopts the producer's display tokenizer as a copy`() {
        val rig = Rig()
        with(rig.workspace.couplingManager) {
            createCoupling(
                rig.transformer.getProducer("getContextWindow"),
                rig.world.getConsumer("setTextIfChanged"),
            )
        }

        assertTrue(awaitAdoption(rig.world), "the coupling triggers adoption")
        assertTrue(rig.world.displayTokenizer is CharacterTokenizer)
        assertFalse(rig.world.displayTokenizer === rig.transformer.tokenizer,
            "the world owns an independent copy")
        assertTrue(rig.world.lockWhileRunning,
            "a document coupling locks the world while the workspace runs")

        rig.world.text = "ab cd"
        assertEquals(5, rig.world.tokens.size,
            "token boxes follow the adopted character tokenizer, not the word default")
    }

    @Test
    fun `a non-document coupling does not hijack the tokenizer`() {
        val rig = Rig()
        with(rig.workspace.couplingManager) {
            createCoupling(
                rig.transformer.getProducer("getHiddenState"),
                rig.world.getConsumer("displayClosestWord"),
            )
        }

        Thread.sleep(300)
        assertNull(rig.world.displayTokenizer,
            "only text-document consumers adopt a display tokenizer")
        assertFalse(rig.world.lockWhileRunning,
            "a non-document coupling does not lock the world")
    }

    @Test
    fun `closing a text world component cancels its coupling listener`() {
        val rig = Rig()
        rig.textWorldComponent.close()

        with(rig.workspace.couplingManager) {
            createCoupling(
                rig.transformer.getProducer("getContextWindow"),
                rig.world.getConsumer("setTextIfChanged"),
            )
        }
        Thread.sleep(300)
        assertNull(rig.world.displayTokenizer,
            "a closed component's adoption listener must not fire")
    }
}
