/**
 * Headless Izhikevich (2003) network benchmark used for the Simbrain 4 paper's performance comparison with Simbrain 3
 * and Brian2 (etc/benchmarks). 80% excitatory / 20% inhibitory neurons with the heterogeneous parameters of Izhikevich
 * (2003), a Gaussian noise current redrawn every step, and one-step current pulses (step responders) on random
 * connections.
 *
 * Two network representations are timed: "objects" (free [Neuron] and [Synapse] objects, the GUI's default) and
 * "arrays" (a [NeuronArray] recurrently connected by a [WeightMatrix]; homogeneous excitatory parameters because a
 * neuron array has a single update rule).
 *
 * Usage: IzhikevichBenchmarkKt <objects|arrays> <neurons> <connectionProbability> <dtMs> <simMs> <warmupMs> <seed> [weightScale]
 *
 * weightScale multiplies every weight; scaling by 100 / in-degree keeps each neuron's input, and so firing rates,
 * comparable across network sizes.
 */
package org.simbrain.benchmarks

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.modelUpdateParallelism
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.spikeresponders.StepResponder
import org.simbrain.network.updaterules.IzhikevichMatrixData
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.network.updaterules.IzhikevichScalarData
import org.simbrain.util.stats.distributions.NormalDistribution
import kotlin.random.Random
import kotlin.system.exitProcess

private class Built(val network: Network, val synapses: Long, val countSpikes: () -> Int)

private fun buildObjects(n: Int, p: Double, rng: Random, scale: Double): Built {
    val net = Network()
    val numExc = (n * 0.8).toInt()
    val neurons = List(n) { i ->
        val excitatory = i < numExc
        val r = rng.nextDouble()
        val rule = IzhikevichRule().apply {
            backgroundCurrent = 0.0
            threshold = 30.0
            addNoise = true
            if (excitatory) {
                a = 0.02; b = 0.2; c = -65.0 + 15 * r * r; d = 8.0 - 6 * r * r
                noiseGenerator = NormalDistribution(0.0, 5.0)
            } else {
                a = 0.02 + 0.08 * r; b = 0.25 - 0.05 * r; c = -65.0; d = 2.0
                noiseGenerator = NormalDistribution(0.0, 2.0)
            }
        }
        Neuron(rule).apply {
            activation = -65.0
            (dataHolder as IzhikevichScalarData).recovery = rule.b * -65.0
        }
    }
    val synapses = ArrayList<Synapse>()
    val targetMajor = System.getProperty("bench.targetMajor") == "true"
    for (outer in 0 until n) {
        for (inner in 0 until n) {
            val src = if (targetMajor) inner else outer
            val tgt = if (targetMajor) outer else inner
            if (src == tgt || rng.nextDouble() >= p) continue
            val w = scale * if (src < numExc) 0.5 * rng.nextDouble() else -rng.nextDouble()
            synapses += Synapse(neurons[src], neurons[tgt]).apply {
                upperBound = 100.0
                lowerBound = -100.0
                forceSetStrength(w)
                spikeResponder = StepResponder(1)
            }
        }
    }
    net.addNetworkModelsAsync(neurons, usePlacementManager = false)
    net.addNetworkModelsAsync(synapses, usePlacementManager = false)
    return Built(net, synapses.size.toLong()) { with(net) { neurons.count { it.isSpike } } }
}

private fun buildArrays(n: Int, p: Double, rng: Random, scale: Double): Built {
    val net = Network()
    val numExc = (n * 0.8).toInt()
    val array = NeuronArray(n).apply {
        updateRule = IzhikevichRule().apply {
            backgroundCurrent = 0.0
            threshold = 30.0
            addNoise = true
            a = 0.02; b = 0.2; c = -65.0; d = 8.0
            noiseGenerator = NormalDistribution(0.0, 5.0)
        }
    }
    for (i in 0 until n) {
        array.activations[i, 0] = -65.0
        (array.dataHolder as IzhikevichMatrixData).recoveryMatrix[i, 0] = 0.2 * -65.0
    }
    val wm = WeightMatrix(array, array).apply {
        // -Dbench.responder=jumpAndDecay times a jump-and-decay response; -Dbench.fullPsr=true forces the full PSR matrix
        spikeResponder = when (System.getProperty("bench.responder")) {
            "jumpAndDecay" -> JumpAndDecay().apply { timeConstant = 3.0 }
            else -> StepResponder(1)
        }.apply { if (System.getProperty("bench.fullPsr") == "true") useSpikeProbability = true }
    }
    var synapses = 0L
    for (tgt in 0 until n) {
        for (src in 0 until n) {
            if (src == tgt || rng.nextDouble() >= p) {
                wm.weights[tgt, src] = 0.0
            } else {
                wm.weights[tgt, src] = scale * if (src < numExc) 0.5 * rng.nextDouble() else -rng.nextDouble()
                synapses++
            }
        }
    }
    net.addNetworkModelsAsync(array, wm, usePlacementManager = false)
    val spikes = (array.dataHolder as IzhikevichMatrixData).spikes
    return Built(net, synapses) { spikes.count { it } }
}

fun main(args: Array<String>) {
    if (args.size < 7) {
        System.err.println("Usage: <objects|arrays> <neurons> <p> <dtMs> <simMs> <warmupMs> <seed>")
        exitProcess(1)
    }
    val variant = args[0]
    val n = args[1].toInt()
    val p = args[2].toDouble()
    val dt = args[3].toDouble()
    val simMs = args[4].toDouble()
    val warmupMs = args[5].toDouble()
    val rng = Random(args[6].toLong())
    val scale = args.getOrNull(7)?.toDouble() ?: 1.0

    val t0 = System.nanoTime()
    val built = when (variant) {
        "objects" -> buildObjects(n, p, rng, scale)
        "arrays" -> buildArrays(n, p, rng, scale)
        else -> error("unknown variant $variant")
    }
    val net = built.network
    net.timeStep = dt
    val buildS = (System.nanoTime() - t0) / 1e9

    val warmupSteps = Math.round(warmupMs / dt).toInt()
    val steps = Math.round(simMs / dt).toInt()

    val t1 = System.nanoTime()
    repeat(warmupSteps) { net.update() }
    val warmupS = (System.nanoTime() - t1) / 1e9

    var spikes = 0L
    val t2 = System.nanoTime()
    repeat(steps) {
        net.update()
        spikes += built.countSpikes()
    }
    val runS = (System.nanoTime() - t2) / 1e9
    val rateHz = spikes / n.toDouble() / (simMs / 1000.0)
    val synapses = built.synapses
    println(
        "{\"simulator\":\"simbrain-$variant\",\"neurons\":$n,\"p\":$p,\"weight_scale\":$scale,\"workers\":${modelUpdateParallelism},\"synapses\":$synapses,\"dt_ms\":$dt," +
                "\"sim_ms\":$simMs,\"build_s\":%.4f,\"warmup_s\":%.4f,\"run_s\":%.4f,\"rt_factor\":%.4f,\"rate_hz\":%.3f}".format(
                    buildS, warmupS, runS, runS / (simMs / 1000.0), rateHz
                )
    )
    exitProcess(0)
}
