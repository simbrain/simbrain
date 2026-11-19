/**
 * Tests of the 1-2-AX task logic and of [OneTwoAxModel] learning, run headlessly on the simulation's network.
 */
package org.simbrain.custom_sims.simulations.leabra

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.network.NetworkComponent
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.random.Random

class OneTwoAxTest {

    private fun opened(random: Random): OneTwoAxModel = runBlocking {
        val scope = SimulationScope()
        oneTwoAx.task.invoke(scope, null)
        OneTwoAxModel((scope.workspace.getComponent("Leabra network") as NetworkComponent).network, random)
    }

    /**
     * Practice in blocks of 100 until a block has under 5% wrong answers and 5% missing memory, up to [maxBlocks].
     * Starting weights are not seeded, so outcomes vary from run to run.
     */
    private fun OneTwoAxModel.practiceUntilLearned(random: Random, maxBlocks: Int): Boolean {
        repeat(maxBlocks) {
            practice(100, random)
            if (wrongResponses < 0.05 && memoryMissing < 0.05) return true
        }
        return false
    }

    /**
     * Show [inputs] without learning and return the response to the last one.
     */
    private fun OneTwoAxModel.respond(vararg inputs: String) = inputs.map { test(axIndex(it)) }.last()

    @Test
    fun `sequences are scored by the digit and the pair`() {
        fun target(vararg inputs: String) = axTrial(inputs.map { axIndex(it) }).target
        assertEquals(AX_R, target("1", "A", "X"))
        assertEquals(AX_L, target("2", "A", "X"))
        assertEquals(AX_R, target("2", "C", "Z", "B", "Y"))
        assertEquals(AX_L, target("1", "B", "Y"))
        assertEquals(AX_L, target("1", "A"))
        val trial = axTrial(listOf("1", "A", "X").map { axIndex(it) })
        assertTrue(trial.rewarded)
        assertEquals(listOf(axIndex("1"), axIndex("A")), trial.needed)
    }

    @Test
    fun `an untrained network often loses what the answer needs`() {
        val rates = (0 until 5).map { seed ->
            val random = Random(seed)
            val model = opened(random)
            model.reset()
            var rewarded = 0
            var missing = 0
            repeat(50) {
                model.clear()
                for (input in axSequence(random)) {
                    val (trial, _) = model.test(input)
                    if (trial.rewarded) {
                        rewarded++
                        if (!model.holds(trial.needed)) missing++
                    }
                }
            }
            missing.toDouble() / rewarded
        }
        assertTrue(rates.average() > 0.5, "$rates")
    }

    @Test
    fun `nearly all networks learn within eight practice blocks`() {
        val learned = (0 until 10).count { seed ->
            val random = Random(seed)
            val model = opened(random)
            model.reset()
            model.practiceUntilLearned(random, 8)
        }
        assertTrue(learned >= 9, "$learned of 10 learned")
    }

    /**
     * The problems, if any, with a trained network on the sequences in the sidebar's Activity 3.
     */
    private fun OneTwoAxModel.activityProblems(): List<String> {
        val problems = mutableListOf<String>()
        fun check(ok: Boolean, what: String) { if (!ok) problems += what }
        clear()
        check(respond("1", "A", "X").second == AX_R, "1 A X")
        val digitStripe = (0 until STRIPES).firstOrNull { held(it) == axIndex("1") } ?: return problems + "digit not held"
        val letterStripe = 1 - digitStripe
        check(held(letterStripe) == axIndex("A"), "A kept through X")
        val da = reward(axTrial(history), learn = false)
        check(abs(da) < 0.1, "dopamine $da")
        check(respond("B", "Y").second == AX_L, "1 A X B Y")
        check(respond("A", "X").second == AX_R, "1 A X B Y A X")
        check(held(digitStripe) == axIndex("1"), "digit kept")
        clear()
        check(respond("2", "A", "X").second == AX_L, "2 A X")
        val w = gatingWeights.weights
        fun goMinusNoGo(stripe: Int, input: String) = w[2 * stripe, axIndex(input)] - w[2 * stripe + 1, axIndex(input)]
        for (input in listOf("1", "2")) check(goMinusNoGo(digitStripe, input) > 0, "Go for $input")
        for (input in listOf("A", "B", "C")) check(goMinusNoGo(letterStripe, input) > 0, "Go for $input")
        return problems
    }

    @Test
    fun `after learning the stripes split the roles and the activities give the right answers`() {
        val results = mutableListOf<List<String>>()
        var seed = 0
        while (results.size < 4 && seed < 10) {
            val random = Random(seed++)
            val model = opened(random)
            model.reset()
            if (!model.practiceUntilLearned(random, 10)) continue
            model.practice(300, random)
            results += model.activityProblems()
        }
        assertEquals(4, results.size)
        assertTrue(results.count { it.isEmpty() } >= 3, "$results")
    }

    @Test
    fun `simulation survives save and reopen with its expected reward`() = runBlocking {
        val scope = SimulationScope()
        oneTwoAx.task.invoke(scope, null)
        val workspace = scope.workspace
        val before = OneTwoAxModel((workspace.getComponent("Leabra network") as NetworkComponent).network, Random(0))
        val expected = 0.6
        before.expectedReward = expected
        val serializer = WorkspaceSerializer(workspace)
        val bytes = ByteArrayOutputStream().also { serializer.serialize(it, true) }.toByteArray()
        workspace.clearWorkspace()
        serializer.deserialize(ByteArrayInputStream(bytes))
        oneTwoAx.reopen(workspace)
        val model = OneTwoAxModel((workspace.getComponent("Leabra network") as NetworkComponent).network)
        assertEquals(expected, model.expectedReward, 1e-4)
        model.test(axIndex("1"))
        assertTrue(model.hidden.activationArray.sum() > 0.0)
    }
}
