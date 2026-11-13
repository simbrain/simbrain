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
 */
class ConnectionDrawing(val owner: ScreenElement) : PNode() {

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
}
