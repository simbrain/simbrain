/**
 * Times the Izhikevich network demos with the GUI open, per etc/benchmarks/HEADED_SPEC.md.
 * Runs the registered simulation (neuron collection + synapse group, or neuron array + weight matrix), optionally
 * without its raster plot, and iterates through the workspace so couplings and views update as in normal use.
 *
 * Modes: headless (no desktop), paced (each iteration drawn before the next, the default), fast (runAsFastAsPossible;
 * views refresh at frame rate). Frames are paints of the network canvas, counted by an invisible camera child.
 *
 * Usage: IzhikevichHeadedBenchmarkKt <headless|paced|fast> <network|raster> <objects|arrays> [simMs] [warmupMs]
 *   [neurons] [connectionProbability] [weightScale]
 *
 * The size arguments default to the 1000-neuron demo. The size sweep's networks scale weights by 100 / in-degree.
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.neuroscience.IZHIKEVICH_CONNECTION_PROBABILITY
import org.simbrain.custom_sims.simulations.neuroscience.IZHIKEVICH_NEURONS
import org.simbrain.custom_sims.simulations.neuroscience.izhikevichNetworkArraySim
import org.simbrain.custom_sims.simulations.neuroscience.izhikevichNetworkSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.updaterules.IzhikevichMatrixData
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.util.UiWork
import org.simbrain.util.updateAction
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import java.util.concurrent.atomic.AtomicLong
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

/**
 * With -Dbench.dirtyLog=true, counts dirty-region requests by component class, to see what the GUI repaints per step.
 */
private class DirtyCountingRepaintManager : javax.swing.RepaintManager() {
    val counts = java.util.concurrent.ConcurrentHashMap<String, AtomicLong>()
    private val reported = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    override fun addDirtyRegion(c: javax.swing.JComponent, x: Int, y: Int, w: Int, h: Int) {
        counts.computeIfAbsent(c.javaClass.name) { AtomicLong() }.incrementAndGet()
        if ((c is javax.swing.JList<*> || c.javaClass == javax.swing.JLabel::class.java) && reported.add(c.javaClass.name)) {
            val path = generateSequence(c as java.awt.Component) { it.parent }.take(8).joinToString(" < ") { it.javaClass.simpleName.ifEmpty { it.javaClass.name } }
            System.err.println("first dirty ${c.javaClass.name}: ${(c as? javax.swing.JLabel)?.text ?: ""} | $path")
            Throwable().stackTrace.take(25).forEach { System.err.println("    at $it") }
        }
        super.addDirtyRegion(c, x, y, w, h)
    }
}

fun main(args: Array<String>) {
    val mode = args[0]
    val variant = args[1]
    val representation = args[2]
    val simMs = args.getOrNull(3)?.toDouble() ?: 2000.0
    val warmupMs = args.getOrNull(4)?.toDouble() ?: 500.0
    val n = args.getOrNull(5)?.toInt() ?: IZHIKEVICH_NEURONS
    val p = args.getOrNull(6)?.toDouble() ?: IZHIKEVICH_CONNECTION_PROBABILITY
    val weightScale = args.getOrNull(7)?.toDouble() ?: 1.0
    require(mode in listOf("headless", "paced", "fast")) { "mode: headless | paced | fast" }
    require(variant in listOf("network", "raster")) { "variant: network | raster" }
    val sim = when (representation) {
        "objects" -> izhikevichNetworkSim(n, p, weightScale)
        "arrays" -> izhikevichNetworkArraySim(n, p, weightScale)
        else -> error("representation: objects | arrays")
    }
    runBlocking { run(sim, mode, variant, representation, simMs, warmupMs, p, weightScale) }
    exitProcess(0)
}

private suspend fun run(
    sim: org.simbrain.custom_sims.NewSimulation,
    mode: String,
    variant: String,
    representation: String,
    simMs: Double,
    warmupMs: Double,
    p: Double,
    weightScale: Double
) {
    val headed = mode != "headless"
    val t0 = System.nanoTime()
    val workspace: Workspace
    val dirtyLog = if (headed && System.getProperty("bench.dirtyLog") == "true") {
        DirtyCountingRepaintManager().also { m -> SwingUtilities.invokeAndWait { javax.swing.RepaintManager.setCurrentManager(m) } }
    } else null
    if (headed) {
        SimbrainDesktop.main(arrayOf())
        withContext(Dispatchers.Swing) {
            SimbrainDesktop.frame.setBounds(0, 0, 1600, 1000)
        }
        sim.run(SimbrainDesktop)
        workspace = SimbrainDesktop.workspace
    } else {
        val scope = SimulationScope()
        sim.task(scope, null)
        workspace = scope.workspace
    }
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().single()
    val network = networkComponent.network
    if (variant == "network") {
        val rasters = workspace.componentList.filterIsInstance<RasterPlotComponent>()
        rasters.forEach { it.setChangedSinceLastSave(false) }
        if (headed) {
            withContext(Dispatchers.Swing) { rasters.forEach { SimbrainDesktop.getDesktopComponentBlocking(it).close() } }
        } else {
            rasters.forEach { it.close() }
        }
        // Closing removes the plot's couplings asynchronously
        while (workspace.couplingManager.couplings.isNotEmpty()) delay(10)
    }
    workspace.runAsFastAsPossible = mode == "fast"

    val models = network.allModels
    val collection = models.filterIsInstance<NeuronCollection>().firstOrNull()
    val array = models.filterIsInstance<NeuronArray>().firstOrNull()
    val neurons = collection?.neuronList?.size ?: array!!.size
    val synapses = models.filterIsInstance<SynapseGroup>().sumOf { it.synapses.size.toLong() } +
            models.filterIsInstance<WeightMatrix>().sumOf { wm -> wm.weights.toArray().sumOf { row -> row.count { it != 0.0 } }.toLong() }
    var counting = false
    var spikes = 0L
    val arraySpikes = (array?.dataHolder as? IzhikevichMatrixData)?.spikes
    if (System.getProperty("bench.noCount") != "true") network.addUpdateAction(updateAction("Count spikes") {
        if (counting) {
            spikes += if (collection != null) with(network) { collection.neuronList.count { it.isSpike } } else arraySpikes!!.count { it }
        }
    })

    val counter = FrameCounter()
    if (headed) {
        val panel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        withContext(Dispatchers.Swing) { panel.canvas.camera.addChild(counter) }
        // Let the windows open and settle before timing
        delay(2000)
        // First-run tips would otherwise cover part of the windows
        SimbrainDesktop.onboardingManager.dismissAll()
        UiWork.awaitIdle()
        SwingUtilities.invokeAndWait { }
    }
    // With -Dbench.syncLog=true, records which UI primitives are pending when each step's display sync flushes
    val syncLog = java.util.concurrent.ConcurrentHashMap<String, AtomicLong>()
    if (headed && System.getProperty("bench.syncLog") == "true") {
        val activeField = UiWork::class.java.getDeclaredField("active").apply { isAccessible = true }
        val original = workspace.displaySync!!
        val panel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        val refresh = panel.javaClass.getDeclaredField("refreshAfterUpdate").apply { isAccessible = true }.get(panel)
        val running = org.simbrain.util.UiPoster::class.java.getDeclaredField("running").apply { isAccessible = true }.get(refresh) as java.util.concurrent.atomic.AtomicBoolean
        val dirty = refresh.javaClass.getDeclaredField("dirty").apply { isAccessible = true }.get(refresh) as java.util.concurrent.atomic.AtomicBoolean
        workspace.displaySync = {
            withContext(Dispatchers.Swing) {
                @Suppress("UNCHECKED_CAST")
                val active = (activeField.get(UiWork) as Set<Any>).toList()
                syncLog.computeIfAbsent("syncs") { AtomicLong() }.incrementAndGet()
                syncLog.computeIfAbsent("active=${active.size}") { AtomicLong() }.incrementAndGet()
                val state = "panel refresh running=${running.get()} active=${refresh in active} dirty=${dirty.get()}"
                syncLog.computeIfAbsent(state) { AtomicLong() }.incrementAndGet()
                active.forEach { poster ->
                    val work = listOf("block", "apply", "drain").firstNotNullOfOrNull { name ->
                        runCatching { poster.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(poster) }.getOrNull()
                    }
                    val key = "${poster.javaClass.simpleName} ${work?.javaClass?.name} #${System.identityHashCode(poster)}"
                    syncLog.computeIfAbsent(key) { AtomicLong() }.incrementAndGet()
                }
            }
            original()
        }
    }
    val buildS = (System.nanoTime() - t0) / 1e9

    val dt = network.timeStep
    val t1 = System.nanoTime()
    workspace.iterateSuspend(Math.round(warmupMs / dt).toInt())
    val warmupS = (System.nanoTime() - t1) / 1e9

    val steps = Math.round(simMs / dt).toInt()
    counting = true
    dirtyLog?.counts?.clear()
    syncLog.clear()
    dirtyLog?.let { System.err.println("=== timed run starts") }
    val frames0 = counter.frames.get()
    val t2 = System.nanoTime()
    if (System.getProperty("bench.direct") == "true") repeat(steps) { network.update() } else workspace.iterateSuspend(steps)
    val runS = (System.nanoTime() - t2) / 1e9
    val frames = counter.frames.get() - frames0
    counting = false

    dirtyLog?.counts?.entries?.sortedByDescending { it.value.get() }?.take(12)?.forEach { (name, n) ->
        System.err.println("dirty %8d %s".format(n.get(), name))
    }
    syncLog.entries.sortedByDescending { it.value.get() }.forEach { (name, n) -> System.err.println("sync %8d %s".format(n.get(), name)) }
    val rateHz = spikes / neurons.toDouble() / (simMs / 1000.0)
    println(
        "{\"simulator\":\"simbrain-gui\",\"representation\":\"$representation\",\"variant\":\"$variant\",\"mode\":\"$mode\"," +
                "\"neurons\":$neurons,\"p\":$p,\"weight_scale\":$weightScale,\"synapses\":$synapses,\"dt_ms\":$dt,\"sim_ms\":$simMs,\"build_s\":%.3f,\"warmup_s\":%.3f,".format(buildS, warmupS) +
                "\"run_s\":%.4f,\"rt_factor\":%.4f,\"iterations_per_s\":%.1f,\"frames\":$frames,\"frames_per_s\":%.1f,\"rate_hz\":%.3f}".format(
                    runS, runS / (simMs / 1000.0), steps / runS, frames / runS, rateHz
                )
    )
}
