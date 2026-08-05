/**
 * Behavioral checks for the fitted thermotaxis steering circuit, plus a trace-level comparison against a
 * reference run of the authors' C++ implementation recorded under identical deterministic conditions.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*
import java.util.zip.GZIPInputStream
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
    fun `a clamped interneuron holds a membrane state consistent with its clamped output`() {
        val model = createModel()

        repeat(100) {
            val step = model.step(temperature = 17.0, activityOverrides = overrides(1 to 0.25))
            assertEquals(0.25, step.outputs[0], 1e-12)
            assertEquals(0.25, 1.0 / (1.0 + Math.exp(-(step.states[0] + fittedBiases[0]))), 1e-9)
        }
    }

    @Test
    fun `the AFD response function differentiates rather than low-pass filters its input`() {
        val steady = createModel().let { model -> (1..2_000).map { model.step(temperature = 17.0) }.last().afdState }
        val warming = createModel().let { model ->
            (1..2_000).map { model.step(temperature = 16.0 + it * 0.001) }.last().afdState
        }

        assertTrue(abs(steady) < 0.75, "A constant temperature should leave AFD near rest, but it was $steady")
        assertTrue(warming > steady + 1.0, "Steady warming should raise AFD above its resting value ($warming vs $steady)")
    }

    @Test
    fun `interneuron membrane states track AFD as reported for the fitted parameter set`() {
        val rows = ThermotaxisTraceRecorder.trace(seconds = 600)

        val afd = rows.map { it.afdState }
        listOf(0 to 0.95, 1 to 0.90, 2 to 0.90).forEach { (neuron, floor) ->
            val correlation = correlation(afd, rows.map { it.states[neuron] })
            assertTrue(correlation > floor, "Expected AFD to drive neuron $neuron, but R was $correlation")
        }
    }

    @Test
    fun `no interneuron is pinned to a constant activity`() {
        val rows = ThermotaxisTraceRecorder.trace(seconds = 600)

        listOf("AIB", "AIY", "AIZ").forEachIndexed { neuron, label ->
            val outputs = rows.map { it.outputs[neuron] }
            val span = outputs.max() - outputs.min()
            assertTrue(span > 0.01, "$label output spanned only $span, so it carries no signal")
        }
    }

    @Test
    fun `circuit trace matches the reference implementation`() {
        val reference = loadReferenceTrace()
        val rows = ThermotaxisTraceRecorder.trace(seconds = reference.size)

        assertEquals(reference.size, rows.size / 10)
        reference.forEachIndexed { second, expected ->
            val actual = rows[second * 10 + 9]
            assertEquals(expected[0], actual.time, 1e-9) { "time mismatch at ${second + 1} s" }
            assertEquals(expected[3], actual.temperature, 1e-9) { "temperature mismatch at ${second + 1} s" }
            assertEquals(expected[4], actual.afdState, 1e-9) { "AFD mismatch at ${second + 1} s" }
            repeat(5) { neuron ->
                assertEquals(expected[5 + neuron], actual.states[neuron], 1e-9) {
                    "state of neuron $neuron mismatch at ${second + 1} s"
                }
                assertEquals(expected[10 + neuron], actual.outputs[neuron], 1e-9) {
                    "output of neuron $neuron mismatch at ${second + 1} s"
                }
            }
            assertEquals(expected[15], actual.cpgOutput, 1e-9) { "CPG mismatch at ${second + 1} s" }
        }
    }

    @Test
    fun `steering curvature is minimized at an intermediate AFD level`() {
        val result = ThermotaxisAfdValidation.run()

        assertTrue(result.passes, result.summary())
        assertEquals(0.5, result.minimizingAfd, 0.3, "Figure 7B places the curvature minimum near AFD 0.4 to 0.6")
        assertEquals(14.0, result.coldSteeringBias * 180.0 / Math.PI, 4.0, "Figure 7B reads about 14 deg/s at AFD -3")
        assertEquals(4.0, result.profile.last().second * 180.0 / Math.PI, 2.0, "Figure 7B reads about 4 deg/s at AFD 2")
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

    private fun correlation(first: List<Double>, second: List<Double>): Double {
        val firstMean = first.average()
        val secondMean = second.average()
        val covariance = first.indices.sumOf { (first[it] - firstMean) * (second[it] - secondMean) }
        val firstSpread = first.sumOf { (it - firstMean) * (it - firstMean) }
        val secondSpread = second.sumOf { (it - secondMean) * (it - secondMean) }
        return covariance / Math.sqrt(firstSpread * secondSpread)
    }

    private fun loadReferenceTrace(): List<DoubleArray> {
        val path = "/org/simbrain/custom_sims/neuroscience/thermotaxis/reference_trace.csv.gz.b64"
        val stream = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing reference trace $path" }
        val decoded = Base64.getMimeDecoder().decode(stream.readBytes())
        return GZIPInputStream(decoded.inputStream()).bufferedReader().readLines()
            .filter { it.isNotBlank() }
            .map { line -> line.split(',').map(String::toDouble).toDoubleArray() }
    }

    private fun overrides(vararg values: Pair<Int, Double>) = MutableList<Double?>(7) { null }.apply {
        values.forEach { (index, value) -> this[index] = value }
    }

    private fun createModel() = ThermotaxisModel(states = DoubleArray(5), biases = fittedBiases)
}
