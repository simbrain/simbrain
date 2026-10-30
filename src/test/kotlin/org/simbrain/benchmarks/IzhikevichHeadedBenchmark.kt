/**
 * Times the Izhikevich network demos with the GUI open, per etc/benchmarks/HEADED_SPEC.md.
 * Runs the registered simulation (neuron collection + synapse group, or neuron array + weight matrix), optionally
 * without its raster plot, and iterates through the workspace so couplings and views update as in normal use.
 *
 * Modes: headless (no desktop), paced (each iteration drawn before the next, the default), fast (runAsFastAsPossible;
 * views refresh at frame rate). Frames are paints of the network canvas, counted by an invisible camera child.
 *
 * Usage: IzhikevichHeadedBenchmarkKt <headless|paced|fast> <network|raster> <objects|arrays> <neurons>
 *   <connectionProbability> <dtMs> <simMs> <warmupMs> <seed> [weightScale]
 *
 * The numeric arguments are in the order of the headless IzhikevichBenchmark, and a seed builds the same network as
 * that harness does from it. The demo is 1000 neurons at p = 0.1 and dt = 0.5; the size sweep's networks scale weights
 * by 100 / in-degree.
 *
 * Diagnostic switches, recorded in the output when set: -Dbench.dirtyLog=true counts dirty-region requests by
 * component class; -Dbench.syncLog=true records which UI primitives are pending at each step's display sync;
 * -Dbench.noCount=true leaves out the spike counting (and so the firing rate); -Dbench.direct=true updates the network
 * directly instead of iterating the workspace.
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.simbrain.custom_sims.NewSimulation
import org.simbrain.custom_sims.simulations.neuroscience.izhikevichNetworkArraySim
import org.simbrain.custom_sims.simulations.neuroscience.izhikevichNetworkSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.IzhikevichMatrixData
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.util.UiWork
import org.simbrain.util.updateAction
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.RepaintManager
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

/**
 * With -Dbench.dirtyLog=true, counts dirty-region requests by component class, to see what the GUI repaints per step.
 */
private class DirtyCountingRepaintManager : RepaintManager() {
    val counts = ConcurrentHashMap<String, AtomicLong>()
    private val reported = ConcurrentHashMap.newKeySet<String>()

    override fun addDirtyRegion(c: JComponent, x: Int, y: Int, w: Int, h: Int) {
        counts.computeIfAbsent(c.javaClass.name) { AtomicLong() }.incrementAndGet()
        if ((c is JList<*> || c.javaClass == JLabel::class.java) && reported.add(c.javaClass.name)) {
            val path = generateSequence(c as Component) { it.parent }.take(8).joinToString(" < ") { it.javaClass.simpleName.ifEmpty { it.javaClass.name } }
            System.err.println("first dirty ${c.javaClass.name}: ${(c as? JLabel)?.text ?: ""} | $path")
            Throwable().stackTrace.take(25).forEach { System.err.println("    at $it") }
        }
        super.addDirtyRegion(c, x, y, w, h)
    }
}

private class Settings(
    val mode: String,
    val variant: String,
    val representation: String,
    val neurons: Int,
    val p: Double,
    val dt: Double,
    val simMs: Double,
    val warmupMs: Double,
    val seed: Long,
    val weightScale: Double
)

fun main(args: Array<String>) {
    if (args.size < 9) {
        System.err.println("Usage: <headless|paced|fast> <network|raster> <objects|arrays> <neurons> <p> <dtMs> <simMs> <warmupMs> <seed> [weightScale]")
        exitProcess(1)
    }
    val settings = Settings(
        mode = args[0],
        variant = args[1],
        representation = args[2],
        neurons = args[3].toInt(),
        p = args[4].toDouble(),
        dt = args[5].toDouble(),
        simMs = args[6].toDouble(),
        warmupMs = args[7].toDouble(),
        seed = args[8].toLong(),
        weightScale = args.getOrNull(9)?.toDouble() ?: 1.0
    )
    require(settings.mode in listOf("headless", "paced", "fast")) { "mode: headless | paced | fast" }
    require(settings.variant in listOf("network", "raster")) { "variant: network | raster" }
    val sim = with(settings) {
        when (representation) {
            "objects" -> izhikevichNetworkSim(neurons, p, weightScale, seed)
            "arrays" -> izhikevichNetworkArraySim(neurons, p, weightScale, seed)
            else -> error("representation: objects | arrays")
        }
    }
    runBlocking { run(sim, settings) }
    exitProcess(0)
}

private suspend fun run(sim: NewSimulation, settings: Settings) {
    val headed = settings.mode != "headless"
    val t0 = System.nanoTime()
    val dirtyLog = if (headed && System.getProperty("bench.dirtyLog") == "true") {
        DirtyCountingRepaintManager().also { m -> SwingUtilities.invokeAndWait { RepaintManager.setCurrentManager(m) } }
    } else null
    val workspace = openDemo(sim, headed)
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().single()
    val network = networkComponent.network
    network.timeStep = settings.dt
    if (settings.variant == "network") {
        val rasters = workspace.componentList.filterIsInstance<RasterPlotComponent>()
        rasters.forEach { it.setChangedSinceLastSave(false) }
        if (headed) {
            withContext(Dispatchers.Swing) { rasters.forEach { SimbrainDesktop.getDesktopComponentBlocking(it).close() } }
        } else {
            rasters.forEach { it.close() }
        }
        // Closing removes the plot's couplings asynchronously
        withTimeout(30_000) {
            while (workspace.couplingManager.couplings.isNotEmpty()) delay(10)
        }
    }
    workspace.runAsFastAsPossible = settings.mode == "fast"

    val models = network.allModels
    val collection = models.filterIsInstance<NeuronCollection>().firstOrNull()
    val array = models.filterIsInstance<NeuronArray>().firstOrNull()
    val neurons = collection?.neuronList?.size ?: array!!.size
    val synapses = models.filterIsInstance<SynapseGroup>().sumOf { it.synapses.size.toLong() } +
            models.filterIsInstance<WeightMatrix>().sumOf { wm -> wm.weights.toArray().sumOf { row -> row.count { it != 0.0 } }.toLong() }
    var counting = false
    var spikes = 0L
    val arraySpikes = (array?.dataHolder as? IzhikevichMatrixData)?.spikes
    val countSpikes = System.getProperty("bench.noCount") != "true"
    if (countSpikes) network.addUpdateAction(updateAction("Count spikes") {
        if (counting) {
            spikes += if (collection != null) with(network) { collection.neuronList.count { it.isSpike } } else arraySpikes!!.count { it }
        }
    })

    val counter = if (headed) networkComponent.countFrames() else null
    if (headed) settleDesktop()
    val syncLog = ConcurrentHashMap<String, AtomicLong>()
    if (headed && System.getProperty("bench.syncLog") == "true") {
        val original = workspace.displaySync!!
        val refresh = networkComponent.networkPanel().refreshAfterUpdate
        workspace.displaySync = {
            withContext(Dispatchers.Swing) {
                val active = UiWork.activePosters()
                syncLog.computeIfAbsent("syncs") { AtomicLong() }.incrementAndGet()
                syncLog.computeIfAbsent("active=${active.size}") { AtomicLong() }.incrementAndGet()
                val state = "panel refresh running=${refresh.isRunning} active=${refresh in active} dirty=${refresh.hasPending()}"
                syncLog.computeIfAbsent(state) { AtomicLong() }.incrementAndGet()
                active.forEach { poster ->
                    syncLog.computeIfAbsent("$poster #${System.identityHashCode(poster)}") { AtomicLong() }.incrementAndGet()
                }
            }
            original()
        }
    }
    val buildS = (System.nanoTime() - t0) / 1e9

    val dt = network.timeStep
    val t1 = System.nanoTime()
    workspace.iterateSuspend(Math.round(settings.warmupMs / dt).toInt())
    val warmupS = (System.nanoTime() - t1) / 1e9

    val steps = Math.round(settings.simMs / dt).toInt()
    counting = true
    dirtyLog?.counts?.clear()
    syncLog.clear()
    dirtyLog?.let { System.err.println("=== timed run starts") }
    val frames0 = counter?.frames?.get() ?: 0
    val t2 = System.nanoTime()
    if (System.getProperty("bench.direct") == "true") repeat(steps) { network.update() } else workspace.iterateSuspend(steps)
    val runS = (System.nanoTime() - t2) / 1e9
    val frames = (counter?.frames?.get() ?: 0) - frames0
    counting = false

    dirtyLog?.counts?.entries?.sortedByDescending { it.value.get() }?.take(12)?.forEach { (name, n) ->
        System.err.println("dirty %8d %s".format(n.get(), name))
    }
    syncLog.entries.sortedByDescending { it.value.get() }.forEach { (name, n) -> System.err.println("sync %8d %s".format(n.get(), name)) }
    val simS = settings.simMs / 1000.0
    emitResult(
        "simulator" to "simbrain-gui",
        "representation" to settings.representation,
        "variant" to settings.variant,
        "mode" to settings.mode,
        "neurons" to neurons,
        "p" to settings.p,
        "weight_scale" to settings.weightScale,
        "seed" to settings.seed,
        "synapses" to synapses,
        "dt_ms" to dt,
        "sim_ms" to settings.simMs,
        "warmup_ms" to settings.warmupMs,
        "build_s" to fixed(buildS, 3),
        "warmup_s" to fixed(warmupS, 3),
        "run_s" to fixed(runS, 4),
        "rt_factor" to fixed(runS / simS, 4),
        "iterations_per_s" to fixed(steps / runS, 1),
        "frames" to frames,
        "frames_per_s" to fixed(frames / runS, 1),
        // Without spike counting there is no rate to report
        "rate_hz" to if (countSpikes) fixed(spikes / neurons.toDouble() / simS, 3) else null,
    )
}
