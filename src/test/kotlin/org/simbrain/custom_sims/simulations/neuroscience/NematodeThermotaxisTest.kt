/**
 * Behavioral checks for the fitted thermotaxis steering circuit.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class NematodeThermotaxisTest {

    @Test
    fun `AFD activity override fixes the sensory input at its requested value`() {
        val model = createModel()

        repeat(100) {
            assertEquals(2.0, model.step(temperature = 14.0, activityOverrides = overrides(0 to 2.0)).afdState, 1e-12)
        }
    }

    @Test
    fun `CPG activity override removes its rhythmic motor input`() {
        val model = createModel()

        repeat(100) {
            assertEquals(0.0, model.step(temperature = 17.0, activityOverrides = overrides(6 to 0.0)).cpgOutput, 1e-12)
        }
    }

    @Test
    fun `removing CPG synapse weights matches clamping CPG to zero`() {
        val synapseLesionModel = createModel()
        val neuronClampModel = createModel()
        val cpgLesionWeights = ThermotaxisWeights(cpgToDmn = 0.0, cpgToVmn = 0.0)

        repeat(100) {
            val synapseLesion = synapseLesionModel.step(temperature = 17.0, weights = cpgLesionWeights)
            val neuronClamp = neuronClampModel.step(temperature = 17.0, activityOverrides = overrides(6 to 0.0))
            assertEquals(neuronClamp.curvature, synapseLesion.curvature, 1e-12)
        }
    }

    @Test
    fun `interneuron activity override replaces its circuit output`() {
        val model = createModel()

        repeat(100) {
            assertEquals(0.0, model.step(temperature = 17.0, activityOverrides = overrides(3 to 0.0)).outputs[2], 1e-12)
        }
    }

    @Test
    fun `higher fixed AFD activity reduces mean steering bias`() {
        val lowAfdBias = abs(averageCurvature(0.0))
        val highAfdBias = abs(averageCurvature(2.0))

        assertTrue(
            highAfdBias < lowAfdBias,
            "Expected high AFD steering bias ($highAfdBias) to be below low AFD steering bias ($lowAfdBias)"
        )
    }

    @Test
    fun `higher AFD activity reduces long-timescale steering bias`() {
        val result = ThermotaxisAfdValidation.run()

        assertTrue(result.passes, result.summary())
    }

    @Test
    fun `population simulation loads author tables and produces bounded worm paths`() {
        val result = requireNotNull(ThermotaxisPopulationSimulation.run(worms = 3, seconds = 3, seed = 7))

        assertEquals(3, result.paths.size)
        assertTrue(result.paths.all { it.points.size == 4 })
        assertTrue(result.paths.flatMap { it.points }.all { it.x in 0.0..136.0 && it.y in 0.0..96.0 })
    }

    @Test
    fun `reversed gradient mirrors empirical turn headings`() {
        val heading = 0.7
        val seed = (1..10_000).first { candidate ->
            ThermotaxisTurnPolicy.select(17.0, 0.0, heading, kotlin.random.Random(candidate)) != null
        }
        val forward = requireNotNull(ThermotaxisTurnPolicy.select(17.0, 0.0, heading, kotlin.random.Random(seed)))
        val reversed = requireNotNull(ThermotaxisTurnPolicy.select(17.0, 0.0, Math.PI - heading, kotlin.random.Random(seed), -1.0))

        assertEquals(forward.label, reversed.label)
        assertEquals(forward.durationSeconds, reversed.durationSeconds, 1e-12)
        assertEquals(forward.displacement, reversed.displacement, 1e-12)
        assertEquals(Math.PI - forward.heading, reversed.heading, 1e-12)
    }

    private fun averageCurvature(afdValue: Double): Double = curvatureSamples(afdValue).average()

    private fun curvatureSamples(afdValue: Double): List<Double> {
        val model = createModel()
        return (1..1_000)
            .map { model.step(temperature = 17.0, activityOverrides = overrides(0 to afdValue)).curvature }
            .drop(200)
    }

    private fun overrides(vararg values: Pair<Int, Double>) = MutableList<Double?>(7) { null }.apply {
        values.forEach { (index, value) -> this[index] = value }
    }

    private fun createModel() = ThermotaxisModel(
        states = DoubleArray(5),
        biases = doubleArrayOf(
            0.261331049344628,
            -9.94979936474547,
            -11.8836526406511,
            -0.243075226129511,
            4.21550001866696
        )
    )
}
