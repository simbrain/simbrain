/**
 * The part of a screen element that is drawn among the connections rather than where the element itself is on the
 * canvas, such as a connector's arrow. The panel keeps it in the connection tier, under everything connectors draw on
 * their arrows, while the element sits in a tier above. It still belongs to the element: a click on it selects the
 * element, and mouse events on it reach the element's listeners.
 */
package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventListener

/**
 * @param owner the element this is part of
 * @param layout lays out what is drawn here, and may place what [owner] draws on it. Asked for with
 * [invalidateLayout], which is safe from event handlers but not from the owner's own layout: the connection tier
 * is validated before the owner's tier, so a mark made from there is left behind by the pass that is running.
 */
class ConnectionDrawing(val owner: ScreenElement, private val layout: () -> Unit = {}) : PNode() {

    init {
        // Piccolo offers an event to the nodes above the picked one, which the owner is not
        addInputEventListener(object : PInputEventListener {
            override fun processEvent(event: PInputEvent, type: Int) {
                val listeners = owner.listenerList?.getListeners(PInputEventListener::class.java) ?: return
                for (listener in listeners) {
                    listener.processEvent(event, type)
                    if (event.isHandled) return
                }
            }
        })
    }

    override fun layoutChildren() {
        layout()
        // A validation pass only goes back over children changed by a layout when the node laid out is itself marked
        invalidateFullBounds()
    }
}
