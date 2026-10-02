/**
 * Base class for nodes on the network canvas: shared interactions (context menu, property dialog, tooltip), the node's
 * lifetime ([nodeScope], [dispose]) and the dirty-mark contract by which model changes reach its visuals once per
 * frame instead of once per event.
 */
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createTooltipTextWithLocation
import org.simbrain.util.StandardDialog
import org.simbrain.util.display
import org.simbrain.util.int
import org.simbrain.util.piccolo.firstScreenElement
import java.awt.event.InputEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

/**
 * **ScreenElement** extends a Piccolo node with property change, tool tip,
 * and property dialog, and support. Screen elements are automatically support the primary user interactions in the
 * network panel.
 */
abstract class ScreenElement protected constructor(val networkPanel: NetworkPanel) : PPath.Float() {
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

    /**
     * Lifetime of this node's subscriptions to its model, cancelled by [dispose] when the node leaves the canvas, so a
     * node replaced by undo stops reacting to the model it used to show. The model's own event scope is never closed:
     * undo reuses model instances.
     */
    val nodeScope: CoroutineScope = CoroutineScope(SupervisorJob(networkPanel.ui.coroutineContext.job))

    /** Ends this node's subscriptions; called when it is removed from the canvas for good. */
    open fun dispose() {
        nodeScope.cancel()
    }

    /** Keeps a subscription only for as long as this node is on the canvas. */
    protected fun Job.untilDisposed(): Job = also { subscription ->
        nodeScope.coroutineContext.job.invokeOnCompletion { subscription.cancel() }
    }

    /** [untilDisposed] for awaitable-event subscriptions, which return a remover. */
    protected fun (() -> Unit).untilDisposed() {
        nodeScope.coroutineContext.job.invokeOnCompletion { this() }
    }

    /**
     * Aspects of the model that changed since this node last synced, as subclass-defined bits. Model events only set
     * bits here; the panel's per-frame sync pass then calls [syncFromModel] once with everything accumulated, so a
     * model firing thousands of changes a second costs the EDT one update per frame rather than one task per change.
     */
    private val dirty = AtomicInteger()

    /** Records changed aspects and queues this node for the next sync pass, once however many marks arrive. */
    protected fun markDirty(bits: Int) {
        if (dirty.getAndUpdate { it or bits } == 0) {
            networkPanel.nodeSync.post(this)
        }
    }

    /** Runs a pending sync now; called on the EDT by the panel's sync pass. */
    internal fun syncPending() {
        val bits = dirty.getAndSet(0)
        if (bits != 0 && parent != null) {
            syncFromModel(bits)
        }
    }

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
        return globalFullBounds.intersects(bound)
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
