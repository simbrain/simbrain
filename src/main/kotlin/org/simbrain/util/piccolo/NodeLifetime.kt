/**
 * Lifetime and per-frame sync shared by canvas nodes that follow a model, such as network screen elements and odor
 * world entity nodes. A node's subscriptions live in its [ScopedNode.nodeScope], a child of its panel's view scope, so
 * they end when the node is disposed or the panel closes. Model events only mark the node dirty through [NodeSync];
 * the panel's [nodeSyncPass] then syncs each marked node once per frame, so a model firing thousands of changes a
 * second costs the EDT one update per frame rather than one task per change.
 */
package org.simbrain.util.piccolo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import org.piccolo2d.PNode
import org.simbrain.util.UI_FRAME_MS
import org.simbrain.util.UiInbox
import org.simbrain.util.cancelWith
import org.simbrain.util.uiInbox
import java.util.concurrent.atomic.AtomicInteger

/**
 * A node whose subscriptions to its model end when it leaves the canvas, so a node replaced by undo stops reacting to
 * the model it used to show. The model's own event scope is never closed: undo reuses model instances.
 */
interface ScopedNode {

    /** Lifetime of this node's subscriptions; create it with [nodeScopeIn]. */
    val nodeScope: CoroutineScope

    /** Ends this node's subscriptions; called when it is removed from the canvas for good. */
    fun dispose() {
        nodeScope.cancel()
    }

    /** Keeps a subscription only for as long as this node is on the canvas. */
    fun Job.untilDisposed(): Job = cancelWith(nodeScope)

    /** [untilDisposed] for awaitable-event subscriptions, which return a remover. */
    fun (() -> Unit).untilDisposed() = cancelWith(nodeScope)
}

/** A node scope that is a child of [viewScope], so closing the panel ends every node's subscriptions too. */
fun nodeScopeIn(viewScope: CoroutineScope): CoroutineScope = CoroutineScope(SupervisorJob(viewScope.coroutineContext.job))

/**
 * The aspects of [node]'s model that changed since it last synced, as node-defined bits. [mark] records them and
 * queues the node on its panel's [nodeSyncPass] once however many marks arrive; the pass then calls [runPending],
 * which hands everything accumulated to [sync] in one call.
 */
class NodeSync(
    private val node: PNode,
    private val nodeScope: CoroutineScope,
    private val pass: UiInbox<NodeSync>,
    private val sync: (bits: Int) -> Unit
) {
    private val dirty = AtomicInteger()

    /** Records changed aspects and queues the node for the next sync pass. Cheap and safe to call from any thread. */
    fun mark(bits: Int) {
        if (dirty.getAndUpdate { it or bits } == 0) {
            pass.post(this)
        }
    }

    /** Runs a pending sync now, unless the node has left the canvas or been disposed since it was marked. EDT only. */
    fun runPending() {
        val bits = dirty.getAndSet(0)
        if (bits != 0 && node.parent != null && nodeScope.isActive) {
            sync(bits)
        }
    }
}

/**
 * A panel's sync pass: once per frame, syncs every node marked since the last one. One node whose sync throws does
 * not stop the others syncing.
 */
fun CoroutineScope.nodeSyncPass(): UiInbox<NodeSync> = uiInbox(UI_FRAME_MS) { marked ->
    marked.forEach {
        try {
            it.runPending()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
