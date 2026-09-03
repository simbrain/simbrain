/**
 * Desktop wrapper for an odor world: hosts the [OdorWorldPanel], owns the frame menu, and sizes the parent
 * frame to the world. When the world's aspect lock is on it also declares the canvas as
 * [AspectRatioLockedContent] so the desktop manager constrains interactive resizes to the world's shape.
 */
package org.simbrain.world.odorworld

import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.workspace.gui.AspectRatioLockedContent
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.constrainFrameBounds
import org.simbrain.workspace.gui.fitToBox
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.JInternalFrame
import javax.swing.SwingUtilities
import kotlin.math.min

class OdorWorldDesktopComponent(frame: GenericFrame, component: OdorWorldComponent) :
    DesktopComponent<OdorWorldComponent>(frame, component), AspectRatioLockedContent {

    val worldPanel: OdorWorldPanel = OdorWorldPanel(component, component.world)

    var menu: OdorWorldFrameMenu

    private val world get() = worldPanel.world

    override val lockedAspectRatio: Double?
        get() = if (world.lockAspectRatio && world.height > 0) world.width / world.height else null

    override val aspectLockedComponent: Component get() = worldPanel.canvas

    init {
        layout = BorderLayout()
        add("Center", worldPanel)
        menu = OdorWorldFrameMenu(this, world)
        menu.setUpMenus()
        parentFrame.jMenuBar = menu

        world.events.tileMapChanged.on { fitFrameToWorldSize() }
        world.events.propertiesChanged.on { applyAspectLock() }
        SwingUtilities.invokeLater { fitFrameToWorldSize() }
    }

    /**
     * Frame size minus canvas size, valid once the frame has been laid out.
     */
    private fun frameChromeOffset() = Dimension(
        parentFrame.size.width - worldPanel.canvas.width,
        parentFrame.size.height - worldPanel.canvas.height
    )

    /**
     * Set frame size to fit the world size. If the world size is too large constrain it to a default maximum,
     * uniformly when the aspect lock is on so the world's shape is preserved.
     */
    fun fitFrameToWorldSize() {
        val defaultMaxSize = 800
        val offset = frameChromeOffset()
        val boxWidth = min(world.width.toInt() + offset.width, defaultMaxSize) - offset.width
        val boxHeight = min(world.height.toInt() + offset.height, defaultMaxSize) - offset.height
        val content = lockedAspectRatio?.let { fitToBox(it, boxWidth, boxHeight) } ?: Dimension(boxWidth, boxHeight)
        parentFrame.preferredSize = Dimension(content.width + offset.width, content.height + offset.height)
        SwingUtilities.invokeLater { worldPanel.scalingFactor = 1.0 }
        parentFrame.pack()
    }

    /**
     * Snap the current frame to the world's shape, shrinking one axis to fit. No-op when unlocked.
     */
    fun applyAspectLock() {
        val ratio = lockedAspectRatio ?: return
        parentFrame.bounds = constrainFrameBounds(parentFrame.bounds, frameChromeOffset(), ratio)
    }

    /**
     * Resizes the parent frame so the canvas is [width] by [height] (or the largest world-shaped canvas that fits
     * in that box when the aspect lock is on), then zooms all the way out.
     */
    fun zoomToFitSize(width: Int, height: Int) {
        val offset = frameChromeOffset()
        val content = lockedAspectRatio?.let { fitToBox(it, width, height) } ?: Dimension(width, height)
        (parentFrame as JInternalFrame).setSize(content.width + offset.width, content.height + offset.height)
        SwingUtilities.invokeLater { worldPanel.canvas.scale(0.01) }
    }
}
