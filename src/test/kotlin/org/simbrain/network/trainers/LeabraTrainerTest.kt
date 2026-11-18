package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.updaterules.LeabraRule
import kotlin.random.Random

class LeabraTrainerTest {

    private fun randomPatterns(count: Int, size: Int, active: Int, random: Random) = List(count) {
        val on = (0 until size).shuffled(random).take(active).toSet()
        DoubleArray(size) { if (it in on) 1.0 else 0.0 }
    }

    @Test
    fun `layer inhibition keeps activity sparse as input strength varies`() {
        val network = Network()
        val input = leabraLayer(25, "Input")
        val hidden = leabraLayer(49, "Hidden")
        val trainer = LeabraTrainer(network, listOf(input, hidden), listOf(leabraProjection(input, hidden)), input, hidden)
        val pattern = randomPatterns(1, 25, 6, Random(1)).first()
        val fractions = listOf(0.5, 1.0).map { strength ->
            trainer.startTrial(DoubleArray(25) { pattern[it] * strength })
            repeat(trainer.minusCycles) { trainer.cycle(fireEvents = false) }
            hidden.activationArray.count { it > 0.5 }.toDouble() / hidden.size
        }
        fractions.forEach { assertTrue(it in 0.05..0.35, "fraction active $it") }
    }

    @Test
    fun `random associator learns all patterns`() {
        val network = Network()
        val input = leabraLayer(25, "Input")
        val hidden1 = leabraLayer(49, "Hidden 1")
        val hidden2 = leabraLayer(49, "Hidden 2")
        val output = leabraLayer(25, "Output")
        (output.updateRule as LeabraRule).inhibitionGain = 1.4
        val projections = listOf(
            leabraProjection(input, hidden1),
            leabraProjection(hidden1, hidden2),
            leabraProjection(hidden2, hidden1, 0.3),
            leabraProjection(hidden2, output),
            leabraProjection(output, hidden2, 0.3),
        )
        val trainer = LeabraTrainer(network, listOf(input, hidden1, hidden2, output), projections, input, output)
        trainer.expectedActivity[input] = 0.25
        trainer.expectedActivity[output] = 0.25
        val random = Random(1)
        val inputs = randomPatterns(25, 25, 6, random)
        val targets = randomPatterns(25, 25, 6, random)

        var errorRate = 1.0
        for (epoch in 1..50) {
            trainer.trainEpoch(inputs, targets)
            errorRate = trainer.errorRate(inputs, targets)
            if (errorRate == 0.0) break
        }
        assertEquals(0.0, errorRate)
    }
}
