package org.simbrain.network.llm

/**
 * The two-way document-sync protocol shared by the language model families' `contextWindow`
 * attribute. The gate enforces one ownership rule: the window is published while the model is
 * advancing, plus until a halted model's final window has been echoed back by a consumer —
 * and is silent otherwise, so a paired document is never clobbered while the user may be
 * editing it. On the consuming side, values found in the ring of recently published windows
 * are echoes (couplings lag their producer by one iteration); anything else is an edit.
 */
class DocumentSyncGate {

    private val ring = ArrayDeque<String>()

    val hasPublishedNonEmptyWindow get() = ring.any(String::isNotEmpty)

    private var tailSynced = true

    /** Returns [current] when it should be published (recording it), or "" while silent. */
    fun publish(current: String, advancing: Boolean): String {
        if (!advancing && tailSynced) return ""
        ring.addLast(current)
        while (ring.size > 2) ring.removeFirst()
        return current
    }

    /**
     * Classifies an incoming value: true means it is an edit the model should apply. Echoes
     * return false; an echo of the live [current] window while halted confirms the tail is
     * synced, silencing the producer. The older ring entry only counts as an echo while the
     * one-iteration coupling lag can still exist — while advancing, or after halting until the
     * tail is confirmed. Once confirmed, a document that matches the previous window is a real
     * edit: the user deleted the last token.
     */
    fun isEdit(incoming: String, current: String, advancing: Boolean): Boolean {
        if (incoming.isEmpty()) return false
        val lagPossible = advancing || !tailSynced
        if (incoming == ring.lastOrNull() || (lagPossible && ring.contains(incoming))) {
            if (!advancing && incoming == current) tailSynced = true
            return false
        }
        return true
    }

    /** Marks the window changed, so it publishes again until the new tail is confirmed. */
    fun invalidate() {
        tailSynced = false
    }

    fun reset() {
        ring.clear()
        tailSynced = false
    }
}
