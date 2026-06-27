package org.simbrain.util.piccolo

import org.piccolo2d.PCamera
import org.piccolo2d.PCanvas
import org.piccolo2d.PNode
import org.piccolo2d.activities.PTransformActivity
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PAffineTransform
import org.piccolo2d.util.PBounds
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.util.NetworkTheme
import org.simbrain.util.component1
import org.simbrain.util.component2
import org.simbrain.util.minus
import org.simbrain.util.plus
import org.simbrain.util.point
import java.awt.BasicStroke
import java.awt.Color
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.geom.Rectangle2D
import javax.swing.JPopupMenu
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import kotlin.math.max

val PNode.parents
    get() = generateSequence(parent) { it.parent }

val PNode.screenElements
    get() = generateSequence(this) { it.parent }.filterIsInstance<ScreenElement>()

val PNode.firstScreenElement
    get() = screenElements.firstOrNull()

val PNode?.hasScreenElement
    get() = this?.parents?.any { it is ScreenElement } ?: false

val PInputEvent.isDoubleClick
    get() = clickCount == 2 && button == MouseEvent.BUTTON1

fun Collection<PNode>.unionOfGlobalFullBounds() = map { it.globalFullBounds }.fold(PBounds()) { acc, b -> acc.add(b); acc }

/**
 * Add a themed border around a PImage. Must be called after the image's bounds have been set.
 */
fun PImage.addBorder(strokeWidth: Float = 1f): PNode {
    val (x, y, w, h) = bounds
    val box = PPath.createRectangle(x, y, w, h)
    box.strokePaint = NetworkTheme.current.imageBorder
    box.stroke = BasicStroke(strokeWidth)
    box.paint = null
    addChild(box)
    return box
}

operator fun PBounds.component1() = x
operator fun PBounds.component2() = y
operator fun PBounds.component3() = width
operator fun PBounds.component4() = height

/**
 * Copied from [PCamera.animateViewToCenterBounds]. See also [PCamera.setViewBounds].
 */
fun PCamera.setViewBoundsNoOverflow(centerBounds: Rectangle2D): PTransformActivity? {
    val delta = viewBounds.deltaRequiredToCenter(centerBounds)
    val newTransform: PAffineTransform = viewTransform
    newTransform.translate(delta.width, delta.height)
    val s = max(
        viewBounds.getWidth() / centerBounds.width,
        viewBounds.getHeight() / centerBounds.height
    )
    if (s != Double.POSITIVE_INFINITY && s != 0.0) {
        newTransform.scaleAboutPoint(s, centerBounds.centerX, centerBounds.centerY)
    }
    return animateViewToTransform(newTransform, 0)
}

data class PNodeAnchor(val node: PNode, val offsetX: Double, val offsetY: Double) {
    val globalPoint get() = node.globalFullBounds.let { (gx, gy) -> point(gx + offsetX, gy + offsetY) }
}

fun PNode.anchor(offsetX: Double = 0.0, offsetY: Double = 0.0) = PNodeAnchor(this, offsetX, offsetY)
fun PNode.anchorRelative(offsetX: Double = 0.0, offsetY: Double = 0.0) =
    PNodeAnchor(this, fullBounds.width * offsetX, fullBounds.height * offsetY)

fun PNode.anchorCenter() = anchorRelative(0.5, 0.5)

fun PNode.anchorCenterLeft() = anchorRelative(0.0, 0.5)
fun PNode.anchorCenterRight() = anchorRelative(1.0, 0.5)
fun PNode.anchorCenterBottom() = anchorRelative(0.5, 1.0)
fun PNode.anchorCenterTop() = anchorRelative(0.5, 0.0)
fun PNode.anchorTopLeft() = anchorRelative(0.0, 0.0)
fun PNode.anchorTopRight() = anchorRelative(1.0, 0.0)
fun PNode.anchorBottomLeft() = anchorRelative(0.0, 1.0)
fun PNode.anchorBottomRight() = anchorRelative(1.0, 1.0)

fun align(reference: PNodeAnchor, target: PNodeAnchor, offsetX: Double = 0.0, offsetY: Double = 0.0) {
    val referenceAnchorPoint = reference.globalPoint
    val targetAnchorPoint = target.globalPoint

    val (dx, dy) = referenceAnchorPoint - targetAnchorPoint + point(offsetX, offsetY)

    val (ox, oy) = target.node.offset
    target.node.setOffset(ox + dx, oy + dy)
}

fun alignX(reference: PNodeAnchor, target: PNodeAnchor, offsetX: Double = 0.0) {
    val referenceAnchorPoint = reference.globalPoint
    val targetAnchorPoint = target.globalPoint

    val dx = referenceAnchorPoint.x - targetAnchorPoint.x + offsetX

    val (ox, oy) = target.node.offset
    target.node.setOffset(ox + dx, oy)
}

fun alignY(reference: PNodeAnchor, target: PNodeAnchor, offsetY: Double = 0.0) {
    val referenceAnchorPoint = reference.globalPoint
    val targetAnchorPoint = target.globalPoint

    val dy = referenceAnchorPoint.y - targetAnchorPoint.y + offsetY

    val (ox, oy) = target.node.offset
    target.node.setOffset(ox, oy + dy)
}

fun PNodeAnchor.alignTo(reference: PNodeAnchor, offsetX: Double = 0.0, offsetY: Double = 0.0) =
    align(reference, this, offsetX, offsetY)

fun PNodeAnchor.alignXTo(reference: PNodeAnchor, offsetX: Double = 0.0) =
    alignX(reference, this, offsetX)

fun PNodeAnchor.alignYTo(reference: PNodeAnchor, offsetY: Double = 0.0) =
    alignY(reference, this, offsetY)

/**
 * Creates a PopupMenuListener that fixes the Piccolo2D mouse button counting bug
 * when JPopupMenu consumes mouse release events.
 *
 * When right-clicking to open a context menu, the JPopupMenu consumes the mouse release event.
 * This causes Piccolo2D's internal button counter (buttonsPressed field) to become misaligned -
 * it thinks a button is still pressed when it's actually released. This listener fixes that
 * by dispatching a synthetic mouse release event when the popup menu closes.
 */
fun createMouseButtonFixListener(canvas: PCanvas): PopupMenuListener {
    return object : PopupMenuListener {
        override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {}

        override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
            try {
                val inputManager = canvas.root.defaultInputManager
                val buttonsField = inputManager.javaClass.getDeclaredField("buttonsPressed")
                buttonsField.isAccessible = true
                val currentCount = buttonsField.getInt(inputManager)

                // Only fix if we have exactly 1 unmatched button (the right mouse button)
                if (currentCount == 1) {
                    // Create a synthetic mouse release event for the right button
                    val mouseEvent = MouseEvent(
                        canvas,
                        MouseEvent.MOUSE_RELEASED,
                        System.currentTimeMillis(),
                        InputEvent.BUTTON3_DOWN_MASK,
                        0, 0, // x, y coordinates (not important for this fix)
                        1, // click count
                        false, // popup trigger = false to prevent feedback loop
                        MouseEvent.BUTTON3
                    )

                    // Dispatch the synthetic event through the canvas
                    canvas.dispatchEvent(mouseEvent)
                }
            } catch (ex: Exception) {
                // Silently ignore reflection errors
            }
        }

        override fun popupMenuCanceled(e: PopupMenuEvent?) {}
    }
}

/**
 * Extension function to apply the mouse button fix to a JPopupMenu.
 * Call this before showing the popup menu.
 */
fun JPopupMenu.applyMouseButtonFix(canvas: PCanvas) {
    addPopupMenuListener(createMouseButtonFixListener(canvas))
}