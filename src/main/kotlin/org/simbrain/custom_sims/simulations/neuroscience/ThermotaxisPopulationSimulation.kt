/**
 * Small, data-driven population simulation illustrating the empirical turning
 * scaffold used with the fitted thermotaxis steering circuit.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.math.*
import kotlin.random.Random

internal object ThermotaxisPopulationSimulation {

    private const val dt = 0.1
    private const val width = PLATE_WIDTH
    private const val height = PLATE_HEIGHT
    private const val stepDistance = CRAWLING_SPEED * dt
    fun run(
        worms: Int = 12,
        seconds: Int = 120,
        seed: Int = Random.nextInt(),
        weights: ThermotaxisWeights = ThermotaxisWeights(),
        gradientDirection: Double = 1.0,
        temperatureOffset: Double = 0.0,
        centerTemperature: Double = PLATE_CENTER_TEMPERATURE,
        halfSpan: Double = PLATE_GRADIENT,
        bufferedSemantics: Boolean = false,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
        shouldCancel: () -> Boolean = { false }
    ): ThermotaxisEnsembleResult? {
        require(worms > 0) { "At least one worm is required" }
        require(seconds > 0) { "At least one second is required" }
        val paths = mutableListOf<ThermotaxisPath>()
        repeat(worms) { index ->
            if (shouldCancel()) return null
            paths += runWorm(
                seconds, Random(seed + index), weights, gradientDirection, temperatureOffset,
                centerTemperature, halfSpan, bufferedSemantics
            )
            onProgress(index + 1, worms)
        }
        val endpoints = paths.map { it.points.last().x }
        return ThermotaxisEnsembleResult(
            worms,
            seconds.toDouble(),
            endpoints.average(),
            endpoints.count { (it - width / 2) * gradientDirection > 0.0 } / worms.toDouble(),
            paths,
            gradientDirection,
            temperatureOffset
        )
    }

    private fun runWorm(
        seconds: Int,
        random: Random,
        weights: ThermotaxisWeights,
        gradientDirection: Double,
        temperatureOffset: Double,
        centerTemperature: Double = PLATE_CENTER_TEMPERATURE,
        halfSpan: Double = PLATE_GRADIENT,
        bufferedSemantics: Boolean = false
    ): ThermotaxisPath {
        val model = ThermotaxisModel(DoubleArray(5), fittedBiases, bufferedSemantics = bufferedSemantics)
        var x = width / 2 + random.nextDouble(-5.0, 5.0)
        var y = height / 2 + random.nextDouble(-5.0, 5.0)
        var heading = random.nextDouble(0.0, 2 * PI)
        val dorsalVentral = if (random.nextBoolean()) 1.0 else -1.0
        var remainingTurnSteps = 0
        var turnStepX = 0.0
        var turnStepY = 0.0
        val points = mutableListOf(ThermotaxisPosition(x, y))
        repeat(seconds * 10) { step ->
            val temperature = centerTemperature + temperatureOffset +
                gradientDirection * halfSpan * (2.0 * x / width - 1.0)
            if (remainingTurnSteps > 0) {
                x += turnStepX
                y += turnStepY
                remainingTurnSteps--
            } else {
                val turn = ThermotaxisTurnPolicy.select(temperature, step * dt, heading, random, gradientDirection)
                if (turn == null) {
                    heading += dorsalVentral * model.step(temperature, weights).curvature * dt
                    x += stepDistance * cos(heading)
                    y -= stepDistance * sin(heading)
                } else {
                    heading = turn.heading
                    val duration = turn.durationSeconds.coerceAtLeast(dt)
                    val displacement = turn.displacement
                    turnStepX = dt * displacement * cos(heading) / duration
                    turnStepY = -dt * displacement * sin(heading) / duration
                    remainingTurnSteps = (duration / dt).roundToInt()
                }
            }
            if (x !in 0.0..width) {
                heading = PI - heading
                x = x.coerceIn(0.0, width)
            }
            if (y !in 0.0..height) {
                heading = -heading
                y = y.coerceIn(0.0, height)
            }
            if ((step + 1) % 10 == 0) points += ThermotaxisPosition(x, y)
        }
        return ThermotaxisPath(points)
    }
}

internal data class ThermotaxisTurn(
    val heading: Double,
    val durationSeconds: Double,
    val displacement: Double,
    val label: String
)

internal object ThermotaxisTurnPolicy {
    private val data by lazy { TurnData.load() }

    fun select(
        temperature: Double,
        time: Double,
        heading: Double,
        random: Random,
        gradientDirection: Double = 1.0
    ): ThermotaxisTurn? {
        val canonicalHeading = if (gradientDirection > 0.0) heading else PI - heading
        val event = data.event(temperature, time, canonicalHeading, random) ?: return null
        return ThermotaxisTurn(
            data.exitHeading(temperature, time, canonicalHeading, event, random).let { exitHeading ->
                if (gradientDirection > 0.0) exitHeading else PI - exitHeading
            },
            data.duration(temperature, time, event),
            data.displacement(temperature, time, event),
            listOf("Omega turn", "Reversal", "Reversal turn", "Shallow turn")[event]
        )
    }
}

private class TurnData(
    private val frequencies: Array<Array<DoubleArray>>,
    private val exits: Array<Array<DoubleArray>>,
    private val durations: Array<DoubleArray>
) {
    fun event(temperature: Double, time: Double, heading: Double, random: Random): Int? {
        val values = frequencies[zone(temperature)][timeBin(time)]
        val direction = directionBin(heading)
        val probabilities = (0..3).map { values[it * 6 + direction] / 600.0 }
        val draw = random.nextDouble()
        var cumulative = 0.0
        probabilities.forEachIndexed { event, probability ->
            cumulative += probability
            if (draw <= cumulative) return event
        }
        return null
    }

    fun exitHeading(temperature: Double, time: Double, heading: Double, event: Int, random: Random): Double {
        val row = timeBin(time) * 13 + directionBin(heading)
        val bins = exits[zone(temperature)][row]
        val offset = event * 13
        val draw = random.nextDouble()
        var cumulative = 0.0
        for (bin in 0 until 12) {
            cumulative += bins[offset + bin]
            if (draw <= cumulative) {
                val angleBin = if (bin <= 5) bin else 17 - bin
                return random.nextDouble(angleBin * PI / 6, (angleBin + 1) * PI / 6)
            }
        }
        return heading
    }

    fun duration(temperature: Double, time: Double, event: Int) = durations[timeBin(time)][event * 2]
    fun displacement(temperature: Double, time: Double, event: Int) = durations[timeBin(time)][event * 2 + 1]
    private fun zone(temperature: Double) = when {
        temperature < 15.5 -> 0
        temperature > 18.5 -> 2
        else -> 1
    }
    private fun timeBin(time: Double) = floor(time / 600.0).toInt().coerceIn(0, 2)
    private fun directionBin(heading: Double): Int {
        val bin = floor(((heading % (2 * PI) + 2 * PI) % (2 * PI)) / (PI / 6)).toInt()
        return if (bin <= 5) bin else 11 - bin
    }

    companion object {
        fun load(): TurnData {
            val freq = arrayOf("14C", "17C", "20C").map { loadCsv("freq_$it.csv.gz.b64", 3, 24) }.toTypedArray()
            val exits = arrayOf("14C", "17C", "20C").map { loadCsv("prob_$it.csv.gz.b64", 38, 51) }.toTypedArray()
            return TurnData(freq, exits, loadCsv("time_dispersion_17C.csv.gz.b64", 3, 8))
        }

        private fun loadCsv(name: String, rows: Int, columns: Int): Array<DoubleArray> {
            val stream = requireNotNull(TurnData::class.java.getResourceAsStream("/org/simbrain/custom_sims/neuroscience/thermotaxis/$name"))
            val encoded = stream.readBytes().toString(Charsets.UTF_8)
            val decoded = Base64.getMimeDecoder().decode(encoded)
            val lines = BufferedReader(InputStreamReader(GZIPInputStream(decoded.inputStream()))).readLines()
            require(lines.size >= rows) { "$name expected at least $rows rows, found ${lines.size}" }
            return Array(rows) { row ->
                lines[row].split(',').map(String::toDouble).also { require(it.size == columns) { "$name row $row expected $columns columns" } }.toDoubleArray()
            }
        }
    }
}
