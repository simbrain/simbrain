package org.simbrain.network.llm

import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.util.ProvidesDisplayTokenizer
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.geom.Point2D

/**
 * The generation protocol shared by the language model families: the coupling vocabulary
 * ([generatedToken] and [hiddenState] out, [injectText] and the [contextWindow] document sync
 * in) and the feed-queue discipline. Subclasses own the model math — their update/step
 * functions and the protected hooks at the bottom.
 *
 * There is no run mode and no prompt: [canAdvance] derives whether the next workspace
 * iteration moves generation forward — something to feed or continue from, and nothing
 * halting it (a sealed stream, a full window, a spent budget; each family says which of
 * those it has). Pausing is the workspace's job, and the coupled document is the single
 * source of the context: edits through [contextWindow] rebuild it, [clearWindow] empties it,
 * and on reopen the saved document replays itself into the model.
 *
 * Feed queue: [pending] holds token ids waiting to enter the model one per iteration —
 * whatever [injectText] and the subclasses' seeding paths append. While non-empty the
 * model is prefilling; once drained it feeds back its own sampled token. At any decode pause
 * one sampled-but-unfed token may already be in [text], so every injection path must queue it
 * first, keeping the context in [text]'s order.
 *
 * The annotated members are deliberately concrete here and must not be overridden: coupling
 * discovery reads annotations off the most-derived declaration (an un-annotated override
 * silently drops the attribute), and the producer cache resolves [hiddenStateDescription] once
 * for the shared base method, so it too must stay on the base.
 */
abstract class GenerativeModel : LocatableModel(), EditableObject, AttributeContainer,
    ProvidesDisplayTokenizer {

    /** How the next token is chosen from the distribution. */
    abstract var samplingStrategy: SamplingStrategy

    /** Seed plus generated continuation from the current window. */
    var text: String = ""
        protected set

    /**
     * Whether the next workspace iteration advances generation: there is something to feed
     * (or a context to continue) and no family-specific halt applies.
     */
    val canAdvance: Boolean
        get() = (pending.isNotEmpty() || hasContinuation()) && !isHalted()

    /** Token ids waiting to enter the model, one per iteration: the seed, then injections. */
    @Transient
    protected var pending = ArrayDeque<Int>()

    /** Whether the feed queue is non-empty: the model is reading fed text, not generating. */
    val isPromptProcessing: Boolean
        get() = pending.isNotEmpty()

    /** Backing span for [currentTokenSpan]; subclasses update it as they process tokens. */
    @Transient
    protected var currentSpan: IntArray = IntArray(0)

    /** The freshest sampled token, which may not have entered the model's context yet. */
    @Transient
    protected var sampledToken = -1

    @Transient
    protected var lastGenerated = ""

    @Transient
    protected var syncGate = DocumentSyncGate()

    /** Test seam: scripts the sampled token stream when set, bypassing [samplingStrategy]. */
    @Transient
    internal var sampleOverride: (() -> Int)? = null

    override var location: Point2D = Point2D.Double()
        set(value) {
            field = value
            events.locationChanged.fire()
        }

    /**
     * Empties the window and run state; the model waits for new text. A coupled non-empty
     * document restores itself on the next play (the document is the truth) — clear the
     * document too for a full reset.
     */
    @Synchronized
    fun clearWindow() {
        onClear()
        pending = ArrayDeque()
        sampledToken = -1
        lastGenerated = ""
        currentSpan = IntArray(0)
        text = ""
        syncGate.reset()
        events.updated.fire()
    }

    /** Text of the token generated this iteration; empty while prefilling or halted. */
    @get:Producible
    val generatedToken: String
        get() = lastGenerated

    /**
     * Char range `[start, end)` in [contextWindow]'s text of the token this iteration
     * processed — the token being read while the feed queue drains, or the freshly sampled
     * token during generation. Empty while idle. Couple to a text world's highlight-span
     * consumer to sweep a highlight across the document as the model works.
     */
    @get:Producible
    val currentTokenSpan: IntArray
        get() = currentSpan

    /** The model's hidden state at the current position; [hiddenStateLabel] says which one. */
    @get:Producible(customDescriptionMethod = "hiddenStateDescription")
    val hiddenState: DoubleArray
        get() = computeHiddenState()

    fun hiddenStateDescription() = "$id:hiddenState (${hiddenStateLabel()})"

    /**
     * Encodes [newText] and appends it to the feed queue, extending prefill: the model walks
     * the injected tokens one per iteration before resuming its own continuation. Feeding the
     * queue is what moves a waiting or sealed model forward. Meant for advancing sources — a
     * static string producer re-injects its value every coupling update.
     */
    @Synchronized
    @Consumable
    fun injectText(newText: String) {
        if (newText.isEmpty()) return
        val ids = encodeText(newText) ?: return
        if (ids.isEmpty()) return
        if (pending.isEmpty() && sampledToken >= 0) pending.addLast(sampledToken)
        ids.forEach { pending.addLast(it) }
        onInjected(newText, ids)
        syncGate.invalidate()
    }

    /**
     * The context window as text — exactly what the model reads. Producing follows an
     * ownership rule: the window is published while the model is advancing, plus until a
     * halted window has echoed back, and is empty otherwise, so a paired document consumer is
     * never clobbered while the user may be editing it. Consuming an unrecognized value is an
     * edit: [applyWindowEdit] rebuilds the context and the model continues from it.
     */
    @get:Producible
    @set:Consumable
    var contextWindow: String
        @Synchronized
        get() {
            val window = windowText() ?: return ""
            return syncGate.publish(window, canAdvance)
        }
        @Synchronized
        set(value) {
            val current = windowText() ?: return
            if (value.isEmpty()) {
                if (syncGate.hasPublishedNonEmptyWindow && (current.isNotEmpty() || canAdvance)) clearWindow()
                return
            }
            if (!syncGate.isEdit(value, current, canAdvance)) return
            val ids = encodeText(value) ?: return
            if (ids.isEmpty()) return
            applyWindowEdit(ids)
            sampledToken = -1
            lastGenerated = ""
            syncGate.invalidate()
            events.updated.fire()
        }

    override suspend fun delete(): List<NetworkModel> {
        onDelete()
        events.deleted.fire(this)
        return listOf(this)
    }

    /** [textIn] as model token ids, or null while the model cannot encode (weights unloaded). */
    protected abstract fun encodeText(textIn: String): IntArray?

    /** The live context window as text, or null while there is no window to publish. */
    protected abstract fun windowText(): String?

    /** Replaces the model's context with an edited window's [ids] and updates [text]. */
    protected abstract fun applyWindowEdit(ids: IntArray)

    /** Resets model-specific state for an empty window. */
    protected abstract fun onClear()

    /** Whether the model has a context to continue from once the feed queue drains. */
    protected abstract fun hasContinuation(): Boolean

    /** Family-specific halts: a sealed stream, a full window, a spent budget. */
    protected open fun isHalted(): Boolean = false

    /** Commits injected tokens: mirrors [ids] into model-side buffers and appends to [text]. */
    protected abstract fun onInjected(newText: String, ids: IntArray)

    protected abstract fun computeHiddenState(): DoubleArray

    /** Which hidden state [hiddenState] produces, for the attribute description. */
    protected abstract fun hiddenStateLabel(): String

    protected open suspend fun onDelete() {}
}
