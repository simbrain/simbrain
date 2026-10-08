/**
 * The per-type model store behind [NetworkModelList].
 */
package org.simbrain.network.core

import java.util.Collections

/**
 * An insertion-ordered set whose iteration never sees a change in progress, like the copy-on-write set it replaces,
 * but whose adds and removals are O(1) instead of copying the whole set. Changes happen under a lock; iteration walks
 * an immutable snapshot taken on the first iteration after a change. Adding n models therefore costs O(n) rather than
 * O(n^2), and a pattern that iterates after every change costs no more than copy-on-write did.
 */
class ModelSet<T> : AbstractSet<T>() {

    private val items = LinkedHashSet<T>()

    @Volatile
    private var snapshot: List<T>? = emptyList()

    override val size: Int get() = synchronized(items) { items.size }

    override fun isEmpty(): Boolean = size == 0

    override fun contains(element: T): Boolean = synchronized(items) { element in items }

    override fun iterator(): Iterator<T> = snapshot().iterator()

    private fun snapshot(): List<T> {
        snapshot?.let { return it }
        synchronized(items) {
            return snapshot ?: Collections.unmodifiableList(ArrayList(items)).also { snapshot = it }
        }
    }

    fun add(element: T): Boolean = synchronized(items) {
        items.add(element).also { if (it) snapshot = null }
    }

    fun remove(element: T): Boolean = synchronized(items) {
        items.remove(element).also { if (it) snapshot = null }
    }
}
