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
