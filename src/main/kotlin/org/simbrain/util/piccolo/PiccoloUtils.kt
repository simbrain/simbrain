package org.simbrain.util.piccolo

import org.piccolo2d.PNode
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.simbrain.network.gui.nodes.ScreenElement
import java.awt.Color
import java.awt.event.MouseEvent

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

/**
 * Add a black border around a PImage. Must be called after the image's bounds have been set.
 */
fun PImage.addBorder(): PNode {
    val (x, y, w, h) = bounds
    val box = PPath.createRectangle(x, y, w, h)
    box.strokePaint = Color.BLACK
    box.paint = null
    addChild(box)
    return box
}

operator fun PBounds.component1() = x
operator fun PBounds.component2() = y
operator fun PBounds.component3() = width
operator fun PBounds.component4() = height