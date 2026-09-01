/**
 * Headless activation recorder for the thermotaxis circuit. Runs the fitted model open-loop on the thermal
 * plate with turning suppressed, so a run is fully deterministic and can be diffed against a reference trace
 * from the authors' C++ implementation. Deliberately free of Simbrain and Swing types so it can be driven
 * from tests or from `runSim -PoptionString=record-trace` without a workspace.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class ThermotaxisTraceRow(
    val time: Double,
    val x: Double,
    val y: Double,
    val temperature: Double,
    val afdState: Double,
    val states: DoubleArray,
    val outputs: DoubleArray,
    val cpgOutput: Double,
    val curvature: Double
)

internal data class ThermotaxisTraceResult(val rows: Int, val file: File?)

internal object ThermotaxisTraceRecorder {

    private const val dt = 0.1
    private const val halfWidth = PLATE_WIDTH / 2.0
    private const val halfHeight = PLATE_HEIGHT / 2.0

    val columnNames = listOf(
        "time", "x", "y", "temperature", "afd",
        "aib_state", "aiy_state", "aiz_state", "dmn_state", "vmn_state",
        "aib", "aiy", "aiz", "dmn", "vmn", "cpg", "curvature"
    )

    /**
     * Plate coordinates here are centered on the origin, spanning [-68, 68] by [-48, 48] mm, matching the
     * reference implementation rather than the odor world's corner-origin frame.
     */
    fun trace(
        seconds: Int = 1800,
        weights: ThermotaxisWeights = ThermotaxisWeights(),
        biases: DoubleArray = fittedBiases,
        dorsalVentral: Double = 1.0,
        bufferedSemantics: Boolean = false
    ): List<ThermotaxisTraceRow> {
        val model = ThermotaxisModel(states = DoubleArray(5), biases = biases, bufferedSemantics = bufferedSemantics)
        return trace(seconds, dorsalVentral) { temperature -> model.step(temperature, weights) }
    }

    /**
     * Same open-loop protocol against an arbitrary circuit stepper, so the fitted model and the native
     * Simbrain network implementation can produce directly comparable traces.
     */
    fun trace(
        seconds: Int,
        dorsalVentral: Double = 1.0,
        stepper: (temperature: Double) -> ThermotaxisStep
    ): List<ThermotaxisTraceRow> {
        require(seconds > 0) { "At least one second is required" }
        var x = 0.0
        var y = 0.0
        var heading = 0.0
        val speed = CRAWLING_SPEED * dt
        return (1..seconds * 10).map { index ->
            val temperature = PLATE_CENTER_TEMPERATURE + PLATE_GRADIENT * x / halfWidth
            val step = stepper(temperature)
            heading += dorsalVentral * step.curvature * dt
            x += speed * cos(heading)
            y += speed * sin(heading)
            heading = reflectHeading(x, y, heading)
            x = reflect(x, halfWidth)
            y = reflect(y, halfHeight)
            ThermotaxisTraceRow(
                index * dt, x, y, temperature,
                step.afdState, step.states, step.outputs, step.cpgOutput, step.curvature
            )
        }
    }

    fun run(seconds: Int = 1800, outputFile: File? = defaultOutputFile()): ThermotaxisTraceResult {
        val rows = trace(seconds)
        outputFile?.apply {
            parentFile?.mkdirs()
            writeText(toCsv(rows))
        }
        return ThermotaxisTraceResult(rows.size, outputFile)
    }

    fun toCsv(rows: List<ThermotaxisTraceRow>): String = buildString {
        appendLine(columnNames.joinToString(","))
        rows.forEach { row ->
            val values = listOf(row.time, row.x, row.y, row.temperature, row.afdState) +
                row.states.toList() + row.outputs.toList() + listOf(row.cpgOutput, row.curvature)
            appendLine(values.joinToString(",") { "%.10g".format(it) })
        }
    }

    private fun defaultOutputFile() = File("simulation_outputs", "thermotaxis_trace.csv")

    /** Only one wall is resolved per step, matching the reference implementation's chained conditional. */
    private fun reflectHeading(x: Double, y: Double, heading: Double) = when {
        x > halfWidth || x < -halfWidth -> PI - heading
        y > halfHeight || y < -halfHeight -> -heading
        else -> heading
    }

    private fun reflect(value: Double, limit: Double) = when {
        value > limit -> 2 * limit - value
        value < -limit -> -2 * limit - value
        else -> value
    }
}
