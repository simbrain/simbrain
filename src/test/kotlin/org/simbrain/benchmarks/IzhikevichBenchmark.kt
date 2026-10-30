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
 * The network is the Izhikevich demo's, from its builders in IzhikevichNetwork.kt, so the demo is what is timed.
 *
 * Usage: IzhikevichBenchmarkKt <objects|arrays> <neurons> <connectionProbability> <dtMs> <simMs> <warmupMs> <seed> [weightScale]
 *
 * The numeric arguments come in the same order in all four Izhikevich harnesses (this one, IzhikevichHeadedBenchmark,
 * the two Simbrain 3 harnesses and izh_brian2.py). weightScale multiplies every weight; scaling by 100 / in-degree
 * keeps each neuron's input, and so firing rates, comparable across network sizes.
 *
 * Diagnostic switches, recorded in the output when set: -Dbench.targetMajor=true orders the free synapses by target
 * rather than source (the same network, a different memory layout); -Dbench.responder=jumpAndDecay gives the weight
 * matrix a jump-and-decay response; -Dbench.fullPsr=true makes it compute the full PSR matrix.
 */
package org.simbrain.benchmarks

import org.simbrain.custom_sims.simulations.neuroscience.izhikevichArray
import org.simbrain.custom_sims.simulations.neuroscience.izhikevichNeurons
import org.simbrain.custom_sims.simulations.neuroscience.izhikevichSynapses
import org.simbrain.network.core.Network
import org.simbrain.network.core.modelUpdateParallelism
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.updaterules.IzhikevichMatrixData
import kotlin.random.Random
import kotlin.system.exitProcess

private class Built(val network: Network, val synapses: Long, val countSpikes: () -> Int)

private fun buildObjects(n: Int, p: Double, rng: Random, scale: Double): Built {
    val net = Network()
    val neurons = izhikevichNeurons(n, rng)
    var synapses = izhikevichSynapses(neurons, p, rng, scale)
    if (System.getProperty("bench.targetMajor") == "true") {
        val index = neurons.withIndex().associate { (i, neuron) -> neuron to i }
        synapses = synapses.sortedBy { index.getValue(it.target) }
    }
    net.addNetworkModelsAsync(neurons, usePlacementManager = false)
    net.addNetworkModelsAsync(synapses, usePlacementManager = false)
    return Built(net, synapses.size.toLong()) { with(net) { neurons.count { it.isSpike } } }
}

private fun buildArrays(n: Int, p: Double, rng: Random, scale: Double): Built {
    val net = Network()
    val (array, wm) = izhikevichArray(n, p, rng, scale)
    val jumpAndDecay = System.getProperty("bench.responder") == "jumpAndDecay"
    val fullPsr = System.getProperty("bench.fullPsr") == "true"
    if (jumpAndDecay || fullPsr) {
        val responder = if (jumpAndDecay) JumpAndDecay().apply { timeConstant = 3.0 } else wm.spikeResponder
        if (fullPsr) responder.useSpikeProbability = true
        wm.spikeResponder = responder
    }
    val synapses = wm.weights.toArray().sumOf { row -> row.count { it != 0.0 }.toLong() }
    net.addNetworkModelsAsync(array, wm, usePlacementManager = false)
    val spikes = (array.dataHolder as IzhikevichMatrixData).spikes
    return Built(net, synapses) { spikes.count { it } }
}

fun main(args: Array<String>) {
    if (args.size < 7) {
        System.err.println("Usage: <objects|arrays> <neurons> <p> <dtMs> <simMs> <warmupMs> <seed> [weightScale]")
        exitProcess(1)
    }
    val variant = args[0]
    val n = args[1].toInt()
    val p = args[2].toDouble()
    val dt = args[3].toDouble()
    val simMs = args[4].toDouble()
    val warmupMs = args[5].toDouble()
    val seed = args[6].toLong()
    val scale = args.getOrNull(7)?.toDouble() ?: 1.0

    val rng = Random(seed)
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
    emitResult(
        "simulator" to "simbrain-$variant",
        "neurons" to n,
        "p" to p,
        "weight_scale" to scale,
        "seed" to seed,
        "workers" to modelUpdateParallelism,
        "synapses" to built.synapses,
        "dt_ms" to dt,
        "sim_ms" to simMs,
        "warmup_ms" to warmupMs,
        "build_s" to fixed(buildS, 4),
        "warmup_s" to fixed(warmupS, 4),
        "run_s" to fixed(runS, 4),
        "rt_factor" to fixed(runS / (simMs / 1000.0), 4),
        "rate_hz" to fixed(spikes / n.toDouble() / (simMs / 1000.0), 3),
    )
    exitProcess(0)
}
