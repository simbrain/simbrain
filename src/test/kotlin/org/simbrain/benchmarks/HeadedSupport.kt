/**
 * Shared pieces of the GUI-open benchmarks (etc/benchmarks/HEADED_SPEC.md): opening a demo with or without the
 * desktop, letting the desktop settle before timing, and counting paints of a network canvas.
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.custom_sims.NewSimulation
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.network.NetworkComponent
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.UiWork
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Container
import java.util.concurrent.atomic.AtomicLong
import javax.swing.SwingUtilities

/** Counts paints of the canvas whose camera it is added to; it covers the whole canvas and draws nothing. */
internal class FrameCounter : PNode() {
    val frames = AtomicLong()

    init {
        pickable = false
        setBounds(-1e7, -1e7, 2e7, 2e7)
    }

    override fun paint(paintContext: PPaintContext) {
        frames.incrementAndGet()
    }
}

/** Runs [sim] on a 1600 x 1000 desktop when [headed], otherwise in a bare workspace, and returns its workspace. */
internal suspend fun openDemo(sim: NewSimulation, headed: Boolean, options: String? = null): Workspace {
    if (!headed) {
        val scope = SimulationScope()
        sim.task(scope, options)
        return scope.workspace
    }
    SimbrainDesktop.main(arrayOf())
    withContext(Dispatchers.Swing) { SimbrainDesktop.frame.setBounds(0, 0, 1600, 1000) }
    sim.run(SimbrainDesktop, options)
    return SimbrainDesktop.workspace
}

internal suspend fun NetworkComponent.networkPanel(): NetworkPanel =
    (SimbrainDesktop.getDesktopComponent(this) as NetworkDesktopComponent).networkPanel

/** Starts counting paints of this component's network canvas. */
internal suspend fun NetworkComponent.countFrames() =
    FrameCounter().also { counter ->
        val panel = networkPanel()
        withContext(Dispatchers.Swing) { panel.canvas.camera.addChild(counter) }
    }

/** Lets the windows open and settle before timing. */
internal suspend fun settleDesktop() {
    delay(2000)
    // First-run tips would otherwise cover part of the windows
    SimbrainDesktop.onboardingManager.dismissAll()
    UiWork.awaitIdle()
    SwingUtilities.invokeAndWait { }
}

/** The first component of type [type] in this container's tree. */
internal fun <T> Container.findComponent(type: Class<T>): T? =
    if (type.isInstance(this)) type.cast(this)
    else components.filterIsInstance<Container>().firstNotNullOfOrNull { it.findComponent(type) }

/** Reads a private field that [owner] declares; the dialogs keep their trainers in one. */
internal fun Any.privateField(owner: Class<*>, name: String): Any? =
    owner.getDeclaredField(name).apply { isAccessible = true }.get(this)
