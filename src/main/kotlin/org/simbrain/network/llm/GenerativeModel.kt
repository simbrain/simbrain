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
 * in), the armed run lifecycle, and the feed-queue discipline. Subclasses own the model math —
 * their update/step functions and the protected hooks at the bottom.
 *
 * Feed queue: [pending] holds token ids waiting to enter the model one per iteration — the
 * prompt after [startGeneration], plus anything [injectText] appends. While non-empty the
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

    /** Text generation restarts from; each subclass supplies its own editor metadata. */
    abstract var prompt: String

    /** How the next token is chosen from the distribution. */
    abstract var samplingStrategy: SamplingStrategy

    /** Prompt plus generated continuation from the current run. */
    var text: String = ""
        protected set

    @Transient
    var isGenerating = false
        protected set

    /** Token ids waiting to enter the model, one per iteration: prompt, then injections. */
    @Transient
    protected var pending = ArrayDeque<Int>()

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

    /** Resets run state and starts a fresh generation run from [prompt]. */
    @Synchronized
    fun startGeneration() {
        val ids = onRestart() ?: return
        pending = ArrayDeque(ids.toList())
        sampledToken = -1
        lastGenerated = ""
        syncGate.reset()
        isGenerating = true
        events.updated.fire()
    }

    @Synchronized
    fun stopGeneration() {
        isGenerating = false
    }

    /** Continues a stopped run where it left off, or starts fresh when there is none. */
    @Synchronized
    fun resumeGeneration() {
        if (!hasRunToContinue()) {
            startGeneration()
            return
        }
        if (!canResume()) return
        isGenerating = true
        events.updated.fire()
    }

    /** Text of the token generated this iteration; empty while prefilling or stopped. */
    @get:Producible
    val generatedToken: String
        get() = lastGenerated

    /** The model's hidden state at the current position; [hiddenStateLabel] says which one. */
    @get:Producible(customDescriptionMethod = "hiddenStateDescription")
    val hiddenState: DoubleArray
        get() = computeHiddenState()

    fun hiddenStateDescription() = "$id:hiddenState (${hiddenStateLabel()})"

    /**
     * Encodes [newText] and appends it to the feed queue, extending prefill: the model walks
     * the injected tokens one per iteration before resuming its own continuation. Does not
     * start or resume a stopped run. Meant for advancing sources — a static string producer
     * re-injects its value every coupling update.
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
     * ownership rule: the window is published while a run is generating, plus until a stopped
     * run's final window has echoed back, and is empty otherwise, so a paired document
     * consumer is never clobbered while the user may be editing it. Consuming an unrecognized
     * value is an edit: [applyWindowEdit] rebuilds the context, preserving the run state (a
     * stopped model stays stopped until resumed).
     */
    @get:Producible
    @set:Consumable
    var contextWindow: String
        @Synchronized
        get() {
            val window = windowText() ?: return ""
            return syncGate.publish(window, isGenerating)
        }
        @Synchronized
        set(value) {
            val current = windowText() ?: return
            if (!syncGate.isEdit(value, current, isGenerating)) return
            val ids = encodeText(value) ?: return
            if (ids.isEmpty()) return
            applyWindowEdit(ids)
            sampledToken = -1
            lastGenerated = ""
            syncGate.invalidate()
            events.updated.fire()
        }

    override suspend fun delete(): List<NetworkModel> {
        stopGeneration()
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

    /**
     * Resets model-specific state for a fresh run and returns the encoded prompt (setting
     * [text] to match), or null when the model is not ready to run.
     */
    protected abstract fun onRestart(): IntArray?

    /** Whether a paused run exists; when false, resuming falls back to [startGeneration]. */
    protected abstract fun hasRunToContinue(): Boolean

    protected open fun canResume(): Boolean = true

    /** Commits injected tokens: mirrors [ids] into model-side buffers and appends to [text]. */
    protected abstract fun onInjected(newText: String, ids: IntArray)

    protected abstract fun computeHiddenState(): DoubleArray

    /** Which hidden state [hiddenState] produces, for the attribute description. */
    protected abstract fun hiddenStateLabel(): String

    protected open suspend fun onDelete() {}
}
