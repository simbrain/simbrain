/**
 * Keeps a desktop frame's content region at a fixed width-to-height ratio.
 *
 * The ratio applies to a component inside the frame (for example a world canvas), not to the frame itself, so
 * every calculation subtracts the chrome between the two (title bar, menu bar, toolbars, borders), constrains
 * the content size, and adds the chrome back. The chrome offset is measured live from the realized components
 * and is constant for the duration of a drag because everything between frame and content is fixed-height.
 *
 * [AspectLockingDesktopManager] is the single enforcement point for interactive resizing and maximizing on the
 * [javax.swing.JDesktopPane]; programmatic sizing goes through the pure helpers directly.
 */
package org.simbrain.workspace.gui

import java.awt.Component
import java.awt.Dimension
import java.awt.Rectangle
import java.beans.PropertyVetoException
import javax.swing.DefaultDesktopManager
import javax.swing.JComponent
import javax.swing.JInternalFrame
import javax.swing.SwingConstants
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Implemented by a frame's content pane when a component inside it should keep a fixed aspect ratio.
 */
interface AspectRatioLockedContent {

    /**
     * Target width divided by height for [aspectLockedComponent], or null while the lock is off.
     */
    val lockedAspectRatio: Double?

    /**
     * The component whose size is constrained. Chrome between it and the frame is measured, not declared.
     */
    val aspectLockedComponent: Component
}

/**
 * Largest width-by-height size with the given ratio that fits inside a box.
 */
fun fitToBox(ratio: Double, boxWidth: Int, boxHeight: Int): Dimension {
    if (boxWidth <= 0 || boxHeight <= 0 || ratio <= 0 || ratio.isNaN()) return Dimension(max(boxWidth, 0), max(boxHeight, 0))
    return if (boxWidth.toDouble() / boxHeight > ratio) {
        Dimension(max(1, (boxHeight * ratio).roundToInt()), boxHeight)
    } else {
        Dimension(boxWidth, max(1, (boxWidth / ratio).roundToInt()))
    }
}

/**
 * Adjusts a proposed frame rectangle so that the content region (frame minus [chromeOffset]) has [ratio].
 *
 * [direction] is the resize handle being dragged as a [SwingConstants] compass value, or 0 when unknown.
 * Edge drags let the dragged axis drive the other; corner and unknown drags let the axis that moved more
 * (relative to [current]) drive. West and north drags keep the opposite edge anchored so the frame does not
 * slide under the cursor.
 */
fun constrainFrameBounds(
    proposed: Rectangle,
    current: Rectangle,
    chromeOffset: Dimension,
    ratio: Double,
    direction: Int = 0
): Rectangle {
    val contentWidth = proposed.width - chromeOffset.width
    val contentHeight = proposed.height - chromeOffset.height
    if (contentWidth <= 0 || contentHeight <= 0 || ratio <= 0 || ratio.isNaN()) return Rectangle(proposed)

    val widthDrives = when (direction) {
        SwingConstants.EAST, SwingConstants.WEST -> true
        SwingConstants.NORTH, SwingConstants.SOUTH -> false
        else -> {
            val widthChange = relativeChange(contentWidth, current.width - chromeOffset.width)
            val heightChange = relativeChange(contentHeight, current.height - chromeOffset.height)
            widthChange >= heightChange
        }
    }
    val content = if (widthDrives) {
        Dimension(contentWidth, max(1, (contentWidth / ratio).roundToInt()))
    } else {
        Dimension(max(1, (contentHeight * ratio).roundToInt()), contentHeight)
    }
    val frameWidth = content.width + chromeOffset.width
    val frameHeight = content.height + chromeOffset.height

    val anchorsRightEdge = direction == SwingConstants.WEST ||
            direction == SwingConstants.NORTH_WEST || direction == SwingConstants.SOUTH_WEST
    val anchorsBottomEdge = direction == SwingConstants.NORTH ||
            direction == SwingConstants.NORTH_WEST || direction == SwingConstants.NORTH_EAST
    val x = if (anchorsRightEdge) proposed.x + proposed.width - frameWidth else proposed.x
    val y = if (anchorsBottomEdge) proposed.y + proposed.height - frameHeight else proposed.y
    return Rectangle(x, y, frameWidth, frameHeight)
}

private fun relativeChange(proposed: Int, current: Int) = abs(proposed - current).toDouble() / max(current, 1)

/**
 * Ratio and live chrome offset for a frame whose content declares an aspect lock, or null when unlocked
 * or not yet laid out.
 */
fun JComponent.aspectLock(): Pair<Double, Dimension>? {
    val contentPane = (this as? JInternalFrame)?.contentPane ?: return null
    val locked = contentPane as? AspectRatioLockedContent
        ?: contentPane.components.firstNotNullOfOrNull { it as? AspectRatioLockedContent }
        ?: return null
    val ratio = locked.lockedAspectRatio ?: return null
    val target = locked.aspectLockedComponent
    if (target.width <= 0 || target.height <= 0) return null
    return ratio to Dimension(width - target.width, height - target.height)
}

/**
 * Desktop manager that filters interactive resizes and maximizes through the aspect lock declared by a frame's
 * content, before the bounds are applied, so there is no corrective bounce after the fact.
 */
class AspectLockingDesktopManager : DefaultDesktopManager() {

    private var resizeDirection = 0

    override fun beginResizingFrame(f: JComponent, direction: Int) {
        resizeDirection = direction
        super.beginResizingFrame(f, direction)
    }

    override fun endResizingFrame(f: JComponent) {
        super.endResizingFrame(f)
        resizeDirection = 0
    }

    override fun resizeFrame(f: JComponent, newX: Int, newY: Int, newWidth: Int, newHeight: Int) {
        val (ratio, offset) = f.aspectLock() ?: return super.resizeFrame(f, newX, newY, newWidth, newHeight)
        val bounds = constrainFrameBounds(
            Rectangle(newX, newY, newWidth, newHeight), f.bounds, offset, ratio, resizeDirection
        )
        super.resizeFrame(f, bounds.x, bounds.y, bounds.width, bounds.height)
    }

    override fun maximizeFrame(f: JInternalFrame) {
        val (ratio, offset) = f.aspectLock() ?: return super.maximizeFrame(f)
        val desktop = f.parent ?: return super.maximizeFrame(f)
        if (f.isIcon) return super.maximizeFrame(f)
        f.normalBounds = f.bounds
        val content = fitToBox(ratio, desktop.width - offset.width, desktop.height - offset.height)
        val frameWidth = content.width + offset.width
        val frameHeight = content.height + offset.height
        setBoundsForFrame(f, (desktop.width - frameWidth) / 2, (desktop.height - frameHeight) / 2, frameWidth, frameHeight)
        try {
            f.isSelected = true
        } catch (e: PropertyVetoException) {
            // Selection is cosmetic here; the frame is already maximized.
        }
    }
}
