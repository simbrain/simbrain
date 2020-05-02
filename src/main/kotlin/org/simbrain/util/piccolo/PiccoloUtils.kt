package org.simbrain.util.piccolo

import org.piccolo2d.PNode
import org.piccolo2d.event.PInputEvent
import org.simbrain.network.gui.nodes.ScreenElement
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