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
import kotlin.random.Random

class WorkingMemoryOneStripeTest {

    private fun opened(): WorkingMemoryModel = runBlocking {
        val scope = SimulationScope()
        workingMemoryOneStripe.task.invoke(scope, null)
        WorkingMemoryModel((scope.workspace.getComponent("Leabra network") as NetworkComponent).network)
    }

    private fun WorkingMemoryModel.practicedWithGate(random: Random): WorkingMemoryModel {
        useGate = true
        practice(250, random)
        return this
    }

    private val a = ITEMS.indexOf("A")
    private val b = ITEMS.indexOf("B")
    private val c = ITEMS.indexOf("C")

    /**
     * Run remember A, distractor B, distractor C on fresh networks and count how often recall reports A.
     */
    private fun recallsOfAWhenUntrained(useGate: Boolean, expectedMemory: Int): Int {
        val model = opened()
        model.useGate = useGate
        return (1..20).count {
            model.reset()
            model.test(WmTrial(REMEMBER, a))
            model.test(WmTrial(DISTRACTOR, b))
            model.test(WmTrial(DISTRACTOR, c))
            assertEquals(expectedMemory, model.remembered)
            val recalled = model.test(WmTrial(RECALL, null))
            assertEquals(expectedMemory, model.remembered)
            recalled == a
        }
    }

    @Test
    fun `sequences remember an item, show other items as distractors, then recall`() {
        val random = Random(0)
        repeat(100) {
            val sequence = wmSequence(random)
            assertEquals(REMEMBER, sequence.first().step)
            assertEquals(WmTrial(RECALL, null), sequence.last())
            val distractors = sequence.drop(1).dropLast(1)
            assertTrue(distractors.size <= MAX_DISTRACTORS)
            assertTrue(distractors.all { it.step == DISTRACTOR && it.item != sequence.first().item })
        }
    }

    @Test
    fun `without a gate distractors overwrite memory and an untrained network rarely recalls`() {
        val recalled = recallsOfAWhenUntrained(useGate = false, expectedMemory = c)
        assertTrue(recalled <= 5, "recalled $recalled of 20")
    }

    @Test
    fun `with a gate memory keeps the item but an untrained network rarely recalls it`() {
        val recalled = recallsOfAWhenUntrained(useGate = true, expectedMemory = a)
        assertTrue(recalled <= 5, "recalled $recalled of 20")
    }

    @Test
    fun `with a gate and practice the network stays quiet until recall and recalls the remembered item`() {
        repeat(5) { seed ->
            val random = Random(seed)
            val model = opened().practicedWithGate(random)
            val wrong = model.practice(50, random)
            assertTrue(wrong <= 0.1, "seed $seed: $wrong")
            model.clear()
            assertNull(model.test(WmTrial(REMEMBER, a)), "seed $seed")
            assertNull(model.test(WmTrial(DISTRACTOR, b)), "seed $seed")
            assertNull(model.test(WmTrial(DISTRACTOR, c)), "seed $seed")
            assertEquals(a, model.remembered)
            assertEquals(a, model.test(WmTrial(RECALL, null)), "seed $seed")
        }
    }

    @Test
    fun `removing the gate after practice makes the network recall the last distractor and practice cannot fix it`() {
        repeat(3) { seed ->
            val random = Random(seed)
            val model = opened().practicedWithGate(random)
            model.clear()
            model.useGate = false
            model.test(WmTrial(REMEMBER, a))
            model.test(WmTrial(DISTRACTOR, b))
            model.test(WmTrial(DISTRACTOR, c))
            assertEquals(c, model.remembered)
            assertEquals(c, model.test(WmTrial(RECALL, null)), "seed $seed")
            val wrong = model.practice(100, random)
            assertTrue(wrong > 0.5, "seed $seed: $wrong")
        }
    }

    @Test
    fun `without a gate practice cannot fix recall`() {
        repeat(3) { seed ->
            val random = Random(seed)
            val model = opened()
            model.practice(200, random)
            val wrong = model.practice(100, random)
            assertTrue(wrong > 0.5, "seed $seed: $wrong")
        }
    }

    @Test
    fun `simulation survives save and reopen`() = runBlocking {
        val scope = SimulationScope()
        workingMemoryOneStripe.task.invoke(scope, null)
        val workspace = scope.workspace
        val serializer = WorkspaceSerializer(workspace)
        val bytes = ByteArrayOutputStream().also { serializer.serialize(it, true) }.toByteArray()
        workspace.clearWorkspace()
        serializer.deserialize(ByteArrayInputStream(bytes))
        workingMemoryOneStripe.reopen(workspace)
        val model = WorkingMemoryModel((workspace.getComponent("Leabra network") as NetworkComponent).network)
        model.useGate = true
        model.test(WmTrial(REMEMBER, a))
        model.test(WmTrial(DISTRACTOR, b))
        assertEquals(a, model.remembered)
        assertTrue(model.hidden.activationArray.sum() > 0.0)
    }
}
