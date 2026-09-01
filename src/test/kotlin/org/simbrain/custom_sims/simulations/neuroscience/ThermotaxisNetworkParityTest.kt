/**
 * Parity scaffolding for the native-component thermotaxis circuit. The buffered-semantics variant of
 * [ThermotaxisModel] delays AFD and CPG contributions by one step to match Simbrain's buffered network
 * update; these tests pin down that the variant is a faithful behavioral stand-in for the C++-verified
 * original. The native network trace producer is compared against the variant here once it exists.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class ThermotaxisNetworkParityTest {

    @Test
    fun `buffered variant tracks the original model under a prescribed temperature sweep`() {
        val original = ThermotaxisModel(states = DoubleArray(5), biases = fittedBiases)
        val variant = ThermotaxisModel(states = DoubleArray(5), biases = fittedBiases, bufferedSemantics = true)
        val temperatures = (1..6000).map { 17.0 + 1.5 * sin(2.0 * PI * it * 0.1 / 60.0) }

        val originalSteps = temperatures.map { original.step(it) }
        val variantSteps = temperatures.map { variant.step(it) }

        originalSteps.zip(variantSteps).forEach { (expected, actual) ->
            assertEquals(expected.afdState, actual.afdState, 0.0, "AFD depends only on temperature history")
            assertEquals(expected.cpgOutput, actual.cpgOutput, 0.0, "CPG depends only on time")
        }
        repeat(5) { neuron ->
            val originalStates = originalSteps.map { it.states[neuron] }
            val variantStates = variantSteps.map { it.states[neuron] }
            val floor = if (neuron < 3) 0.99 else 0.98
            val r = correlation(originalStates, variantStates)
            assertTrue(r > floor, "State of neuron $neuron correlates at only $r across the semantics change")
        }
    }

    @Test
    fun `buffered variant preserves the AFD differentiator property`() {
        val steady = ThermotaxisModel(states = DoubleArray(5), biases = fittedBiases, bufferedSemantics = true)
            .let { model -> (1..2_000).map { model.step(temperature = 17.0) }.last().afdState }
        val warming = ThermotaxisModel(states = DoubleArray(5), biases = fittedBiases, bufferedSemantics = true)
            .let { model -> (1..2_000).map { model.step(temperature = 16.0 + it * 0.001) }.last().afdState }

        assertTrue(abs(steady) < 0.75, "A constant temperature should leave AFD near rest, but it was $steady")
        assertTrue(warming > steady + 1.0, "Steady warming should raise AFD above its resting value ($warming vs $steady)")
    }

    @Test
    fun `buffered variant passes the Figure 7B steering validation`() {
        val result = ThermotaxisAfdValidation.run(bufferedSemantics = true)

        assertTrue(result.passes, result.summary())
        assertEquals(0.5, result.minimizingAfd, 0.3, "Figure 7B places the curvature minimum near AFD 0.4 to 0.6")
        assertEquals(14.0, result.coldSteeringBias * 180.0 / PI, 4.0, "Figure 7B reads about 14 deg/s at AFD -3")
    }

    @Test
    fun `buffered variant stays behaviorally close to the original in the open-loop protocol`() {
        val originalRows = ThermotaxisTraceRecorder.trace(seconds = 600)
        val variantRows = ThermotaxisTraceRecorder.trace(seconds = 600, bufferedSemantics = true)

        val afd = variantRows.map { it.afdState }
        listOf(0 to 0.95, 1 to 0.90, 2 to 0.90).forEach { (neuron, floor) ->
            val r = correlation(afd, variantRows.map { it.states[neuron] })
            assertTrue(r > floor, "Expected AFD to drive neuron $neuron in the variant, but R was $r")
        }
        val originalWarmDrift = originalRows.last().x - originalRows.first().x
        val variantWarmDrift = variantRows.last().x - variantRows.first().x
        assertTrue(
            abs(originalWarmDrift - variantWarmDrift) < 20.0,
            "Endpoint drift diverged: original $originalWarmDrift vs variant $variantWarmDrift"
        )
    }

    @Test
    fun `native circuit matches the buffered variant at 1e-9 over 300 seconds`() {
        assertNativeCircuitParity(seconds = 300)
    }

    @Test
    fun `native circuit matches the buffered variant over the full 1800 second run`() {
        assertNativeCircuitParity(seconds = 1800)
    }

    private fun assertNativeCircuitParity(seconds: Int) {
        val circuit = ThermotaxisNativeCircuit.build()
        val networkRows = ThermotaxisTraceRecorder.trace(seconds = seconds) { temperature -> circuit.step(temperature) }
        val modelRows = ThermotaxisTraceRecorder.trace(seconds = seconds, bufferedSemantics = true)

        modelRows.zip(networkRows).forEachIndexed { step, (expected, actual) ->
            assertEquals(expected.temperature, actual.temperature, 1e-9) { "temperature mismatch at step $step" }
            assertEquals(expected.afdState, actual.afdState, 1e-9) { "AFD mismatch at step $step" }
            repeat(5) { neuron ->
                assertEquals(expected.states[neuron], actual.states[neuron], 1e-9) {
                    "state of neuron $neuron mismatch at step $step"
                }
                assertEquals(expected.outputs[neuron], actual.outputs[neuron], 1e-9) {
                    "output of neuron $neuron mismatch at step $step"
                }
            }
            assertEquals(expected.cpgOutput, actual.cpgOutput, 1e-9) { "CPG mismatch at step $step" }
            assertEquals(expected.curvature, actual.curvature, 1e-9) { "curvature mismatch at step $step" }
            assertEquals(expected.x, actual.x, 1e-9) { "x mismatch at step $step" }
            assertEquals(expected.y, actual.y, 1e-9) { "y mismatch at step $step" }
        }
    }

    @Test
    fun `reset clears the variant delay state`() {
        val model = ThermotaxisModel(states = DoubleArray(5), biases = fittedBiases, bufferedSemantics = true)
        val fresh = ThermotaxisModel(states = DoubleArray(5), biases = fittedBiases, bufferedSemantics = true)
        repeat(500) { model.step(temperature = 16.0 + it * 0.01) }
        model.reset()

        repeat(200) {
            val replayed = model.step(temperature = 17.5)
            val reference = fresh.step(temperature = 17.5)
            assertEquals(reference.states.toList(), replayed.states.toList(), "Reset must restore initial dynamics")
        }
    }

    private fun correlation(first: List<Double>, second: List<Double>): Double {
        val firstMean = first.average()
        val secondMean = second.average()
        val covariance = first.indices.sumOf { (first[it] - firstMean) * (second[it] - secondMean) }
        val firstSpread = first.sumOf { (it - firstMean) * (it - firstMean) }
        val secondSpread = second.sumOf { (it - secondMean) * (it - secondMean) }
        return covariance / sqrt(firstSpread * secondSpread)
    }
}
