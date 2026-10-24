/**
 * An insertion-ordered set for collections that one thread changes while others iterate, such as a network's models
 * and the network panel's selection.
 */
package org.simbrain.util

import java.util.Collections

/**
 * An insertion-ordered set whose iteration never sees a change in progress, like a copy-on-write set, but whose adds
 * and removals are O(1) instead of copying the whole set (and, for bulk adds, comparing every new element with every
 * element already present). Changes happen under a lock; iteration walks an immutable snapshot taken on the first
 * iteration after a change. Adding n elements therefore costs O(n) rather than O(n^2), and a pattern that iterates
 * after every change costs no more than copy-on-write did.
 */
class SnapshotSet<T> : AbstractSet<T>() {

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

    private inline fun change(block: LinkedHashSet<T>.() -> Boolean): Boolean = synchronized(items) {
        items.block().also { if (it) snapshot = null }
    }

    fun add(element: T): Boolean = change { add(element) }

    fun addAll(elements: Collection<T>): Boolean = change { addAll(elements) }

    fun remove(element: T): Boolean = change { remove(element) }

    fun removeAll(elements: Collection<T>): Boolean = change { removeAll(elements.toSet()) }

    fun clear() {
        change { isNotEmpty().also { clear() } }
    }
}
