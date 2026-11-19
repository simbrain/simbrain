/**
 * Tests of [LearnedGatingModel] before and after practice, of the dopamine it gives, and of its expected reward
 * surviving save and reopen.
 */
package org.simbrain.custom_sims.simulations.leabra

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.leabra.WmStep.*
import org.simbrain.network.NetworkComponent
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.random.Random

class LearnedGatingTest {

    private fun opened(random: Random): LearnedGatingModel = runBlocking {
        val scope = SimulationScope()
        learnedGating.task.invoke(scope, null)
        LearnedGatingModel((scope.workspace.getComponent("Leabra network") as NetworkComponent).network, random)
    }

    private val a = ITEMS.indexOf("A")
    private val b = ITEMS.indexOf("B")
    private val c = ITEMS.indexOf("C")

    @Test
    fun `an untrained gate makes many gate mistakes`() {
        val rates = (0 until 10).map { seed ->
            val random = Random(seed)
            val model = opened(random)
            model.reset()
            model.resetGateCounts()
            repeat(50) { wmSequence(random).forEach { model.test(it) } }
            model.gateMistakes.toDouble() / model.gateDecisions
        }
        assertTrue(rates.average() > 0.3, "$rates")
    }

    /**
     * Practice in blocks of 100 until a block has under 5% gate mistakes and 15% wrong recalls, up to [maxBlocks].
     * Returns whether that happened. Starting weights are not seeded, so outcomes vary from run to run.
     */
    private fun LearnedGatingModel.practiceUntilLearned(random: Random, maxBlocks: Int): Boolean {
        repeat(maxBlocks) {
            resetGateCounts()
            val wrong = practice(100, random)
            if (wrong < 0.15 && gateMistakes.toDouble() / gateDecisions < 0.05) return true
        }
        return false
    }

    @Test
    fun `most networks learn the gate and recall within ten practice blocks`() {
        val learned = (0 until 10).count { seed ->
            val random = Random(seed)
            val model = opened(random)
            model.reset()
            model.practiceUntilLearned(random, 10)
        }
        assertTrue(learned >= 6, "learned on $learned of 10")
    }

    @Test
    fun `after learning the gate opens only on remember, recall is right, and dopamine is near zero`() {
        var checked = 0
        for (seed in 0 until 8) {
            if (checked == 3) break
            val random = Random(seed)
            val model = opened(random)
            model.reset()
            if (!model.practiceUntilLearned(random, 12)) continue
            model.practice(300, random)
            model.clear()
            assertNull(model.test(WmTrial(REMEMBER, a)), "seed $seed")
            assertTrue(model.gateOpen, "seed $seed")
            assertNull(model.test(WmTrial(DISTRACTOR, b)), "seed $seed")
            assertFalse(model.gateOpen, "seed $seed")
            assertNull(model.test(WmTrial(DISTRACTOR, c)), "seed $seed")
            assertFalse(model.gateOpen, "seed $seed")
            assertEquals(a, model.test(WmTrial(RECALL, null)), "seed $seed")
            val da = model.reward(learn = false)
            assertTrue(abs(da) < 0.1, "seed $seed: dopamine $da")
            val fromTask = model.gatingWeights.first { it.source == model.task }.weights
            assertTrue(fromTask[0, REMEMBER.ordinal] > fromTask[1, REMEMBER.ordinal], "seed $seed")
            assertTrue(fromTask[0, DISTRACTOR.ordinal] < fromTask[1, DISTRACTOR.ordinal], "seed $seed")
            checked++
        }
        assertEquals(3, checked)
    }

    @Test
    fun `an untrained network expects no reward, so a right guess gives dopamine of one`() {
        val model = opened(Random(0))
        model.reset()
        model.test(WmTrial(REMEMBER, a))
        model.test(WmTrial(RECALL, null))
        val acts = model.output.activationArray
        val expected = if (acts.indices.maxBy { acts[it] } == a) 1.0 else 0.0
        assertEquals(expected, model.reward(learn = false), 1e-9)
    }

    @Test
    fun `simulation survives save and reopen with its expected reward`() = runBlocking {
        val scope = SimulationScope()
        learnedGating.task.invoke(scope, null)
        val workspace = scope.workspace
        val before = LearnedGatingModel((workspace.getComponent("Leabra network") as NetworkComponent).network, Random(0))
        before.practice(50, Random(0))
        val expected = before.expectedReward
        assertTrue(expected > 0.0)
        val serializer = WorkspaceSerializer(workspace)
        val bytes = ByteArrayOutputStream().also { serializer.serialize(it, true) }.toByteArray()
        workspace.clearWorkspace()
        serializer.deserialize(ByteArrayInputStream(bytes))
        learnedGating.reopen(workspace)
        val model = LearnedGatingModel((workspace.getComponent("Leabra network") as NetworkComponent).network)
        assertEquals(expected, model.expectedReward, 1e-4)
        model.test(WmTrial(REMEMBER, a))
        assertTrue(model.hidden.activationArray.sum() > 0.0)
    }
}
