/**
 * Base class for nodes on the network canvas: shared interactions (context menu, property dialog, tooltip), the node's
 * lifetime ([nodeScope], [dispose]) and the dirty-mark contract by which model changes reach its visuals once per
 * frame instead of once per event.
 */
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.CoroutineScope
import org.piccolo2d.PCamera
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.LaidOutConnector
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createTooltipTextWithLocation
import org.simbrain.util.StandardDialog
import org.simbrain.util.display
import org.simbrain.util.int
import org.simbrain.util.piccolo.NodeSync
import org.simbrain.util.piccolo.ScopedNode
import org.simbrain.util.piccolo.firstScreenElement
import org.simbrain.util.piccolo.nodeScopeIn
import java.awt.event.InputEvent
import java.beans.PropertyChangeListener
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

/**
 * **ScreenElement** extends a Piccolo node with property change, tool tip,
 * and property dialog, and support. Screen elements are automatically support the primary user interactions in the
 * network panel.
 */
abstract class ScreenElement protected constructor(val networkPanel: NetworkPanel) : PPath.Float(), ScopedNode {
    /**
     * Create a new abstract screen element with the specified network panel.
     */
    init {
        addInputEventListener(ContextMenuEventHandler())
        addInputEventListener(PropertyDialogEventHandler())
        addInputEventListener(object : ToolTipTextUpdater(networkPanel) {
            override fun getToolTipText(): String? {
                return this@ScreenElement.toolTipText
            }
        })
    }

    final override val nodeScope: CoroutineScope = nodeScopeIn(networkPanel.viewScope)

    /**
     * Aspects of the model that changed since this node last synced, as subclass-defined bits. Model events only set
     * bits here; the panel's per-frame sync pass then calls [syncFromModel] once with everything accumulated, so a
     * model firing thousands of changes a second costs the EDT one update per frame rather than one task per change.
     */
    private val sync = NodeSync(this, nodeScope, networkPanel.nodeSync) { bits -> syncFromModel(bits) }

    /**
     * Records changed aspects and queues this node for the next sync pass, once however many marks arrive. Bits are
     * numbered per class hierarchy: an open node class keeps its bits protected, so a subclass numbers its own after
     * them.
     */
    protected fun markDirty(bits: Int) = sync.mark(bits)

    /** Runs a pending sync now; called on the EDT by the panel's sync pass. */
    internal fun syncPending() = sync.runPending()

    /**
     * Updates visuals from the model's current state for the aspects in [bits]. Runs on the EDT, at most once per
     * frame, and should only touch visuals whose drawn value actually differs.
     */
    protected open fun syncFromModel(bits: Int) {}

    /**
     * Returns a reference to the model object this node represents.
     */
    abstract val model: NetworkModel

    /**
     * What this element draws among the connections, under everything connectors draw on their arrows, or null if
     * it draws nothing there. The panel puts it in the connection tier when it adds this element and takes it out
     * when it removes the element.
     */
    open val connectionDrawing: ConnectionDrawing? get() = null

    /**
     * Everything this element draws, in canvas coordinates: its own node and its [connectionDrawing], which is not
     * part of the node's full bounds.
     */
    val globalDrawnBounds: PBounds
        get() = globalFullBounds.also { bounds -> connectionDrawing?.let { bounds.add(it.globalFullBounds) } }

    /**
     * The space this element takes up on the canvas, in canvas coordinates: what it draws in its own place, without
     * its [connectionDrawing] and without selection handles, which come and go. What is laid out around an
     * element, such as a connector's arrow or a subnetwork's outline, is laid out against this, so that it does not
     * shift when the element is selected. An element that does not say otherwise counts everything in its node.
     */
    val footprint: PBounds
        get() {
            // Validates the node, so that its parts are measured where its own layout puts them. The validation
            // pass may not have reached it yet
            fullBoundsReference
            return measureFootprint()
        }

    /** Measures the [footprint]. Called once the node's own layout is up to date. */
    protected open fun measureFootprint(): PBounds = globalFullBounds

    private var viewListener: PropertyChangeListener? = null

    /** Have the panel lay this connector out again, once however many times it is asked before it does. */
    protected fun LaidOutConnector.requestLayout() = networkPanel.connectionLayout.request(this)

    /**
     * Lay this connector out again whenever the view pans or zooms. Label boxes are rescaled with the zoom, and
     * what a connector places around its label box has to follow. Ends when the node is disposed.
     */
    protected fun LaidOutConnector.layoutWithView() {
        if (viewListener != null) return
        val listener = PropertyChangeListener { requestLayout() }
        viewListener = listener
        networkPanel.canvas.camera.addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM, listener)
    }

    override fun dispose() {
        super.dispose()
        viewListener?.let {
            networkPanel.canvas.camera.removePropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM, it)
        }
        viewListener = null
    }

    /**
     * Return true if this screen element accepts a source [NodeHandle].
     */
    open fun acceptsSourceHandle(): Boolean {
        return false
    }

    open fun createEditDialog(): StandardDialog? {
        return null
    }

    /**
     * Return true if this screen element is draggable.
     */
    abstract val isDraggable: Boolean

    /**
     * Return a String to use as tool tip text for this screen element. Return null if this
     * screen element does not have tool tip text.
     */
    open val toolTipText: String?
        get() = (model as? LocatableModel)?.let { createTooltipTextWithLocation(it) }

    /**
     * Return a context menu specific to this screen element or null if none.
     */
    open val contextMenu: JPopupMenu?
        get() = null

    /**
     * Return a property dialog for this screen element, or null if it does not have one.
     */
    open val propertyDialog: StandardDialog?
        get() = null

    /**
     * Screen element-specific context menu event handler.
     */
    private inner class ContextMenuEventHandler : PBasicInputEventHandler() {
        private fun showContextMenu(event: PInputEvent) {
            event.isHandled = true
            val (x, y) = event.canvasPosition.int
            
            contextMenu?.let { menu ->
                // Apply both drag reset and mouse button fixes using the utility
                org.simbrain.network.gui.MouseEventUtils.applyContextMenuFixes(networkPanel, event, menu)
                menu.show(networkPanel.canvas, x, y)
            }
            
            event.pickedNode.firstScreenElement?.let {
                networkPanel.selectionManager.add(it)
            }
        }

        override fun mousePressed(event: PInputEvent) {
            if (event.isPopupTrigger) {
                showContextMenu(event)
            }
        }

        override fun mouseReleased(event: PInputEvent) {
            if (event.isPopupTrigger) {
                showContextMenu(event)
            }
        }
    }

    /**
     * Property dialog event handler.
     */
    private inner class PropertyDialogEventHandler : PBasicInputEventHandler() {
        init {
            eventFilter = PInputEventFilter(InputEvent.BUTTON1_MASK)
        }

        override fun mouseClicked(event: PInputEvent) {
            if (event.clickCount == 2) {
                event.isHandled = true
                SwingUtilities.invokeLater {
                    propertyDialog?.display()
                }
            }
        }
    }

    /**
     * Returns true if the provided bounds intersect this screen element
     */
    open fun isIntersecting(bound: PBounds?): Boolean {
        return globalDrawnBounds.intersects(bound)
    }

    /**
     * Select this element.
     */
    fun select() {
        networkPanel.selectionManager.add(this)
    }

    /**
     * Re-apply theme-derived colors after a light/dark switch. Default does nothing; nodes that cache
     * themed paints (rather than re-reading them on a model event) override this to re-set them. Invoked
     * by [NetworkPanel] when the canvas is recolored.
     */
    open fun refreshTheme() {}
}

/**
 * The bounds of everything in [parts] that takes up space. A part with nothing to draw has bounds without size at a
 * place that means nothing, which must not be counted.
 */
internal fun footprintOf(vararg parts: PBounds?) = PBounds().apply {
    parts.forEach { if (it != null && it.width > 0 && it.height > 0) add(it) }
}
