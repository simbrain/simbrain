package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.util.point
import java.awt.geom.Rectangle2D

class PlacementManagerTest {

    private fun Network.addPlacedNeuron() = Neuron().also { addNetworkModelAsync(it) }

    /**
     * Mimics the clipboard: copies start on top of their sources, are added unplaced, then placed as a group.
     */
    private fun Network.copyOf(source: List<Neuron>, placement: CopyPlacement): List<Neuron> {
        val copies = source.map { Neuron(it) }
        copies.forEach {
            it.shouldBePlaced = false
            addNetworkModelAsync(it)
            it.shouldBePlaced = true
        }
        placementManager.placeCopies(copies, source, placement)
        return copies
    }

    private fun assertNoOverlap(models: List<LocatableModel>) {
        val locations = models.map { it.location }
        assertEquals(locations.size, locations.distinct().size, "Models share a location: $locations")
    }

    @Test
    fun `test repeated adds form a row`() {
        val net = Network()
        val n1 = net.addPlacedNeuron()
        val n2 = net.addPlacedNeuron()
        val n3 = net.addPlacedNeuron()
        assertEquals(n1.x + NEURON_FOOTPRINT, n2.x, .01)
        assertEquals(n1.x + 2 * NEURON_FOOTPRINT, n3.x, .01)
        assertEquals(n1.y, n3.y, .01)
    }

    @Test
    fun `test adds start at the insertion point and it persists`() {
        val net = Network()
        net.placementManager.insertionPoint = point(100.0, 50.0)
        val n1 = net.addPlacedNeuron()
        val n2 = net.addPlacedNeuron()
        assertEquals(100.0, n1.x, .01)
        assertEquals(50.0, n1.y, .01)
        assertEquals(100.0 + NEURON_FOOTPRINT, n2.x, .01)
        assertEquals(50.0, n2.y, .01)
    }

    @Test
    fun `test off-screen insertion point falls back to the center of the visible canvas`() {
        val net = Network()
        net.placementManager.visibleBounds = { Rectangle2D.Double(1000.0, 1000.0, 200.0, 100.0) }
        net.placementManager.insertionPoint = point(0.0, 0.0)
        val n1 = net.addPlacedNeuron()
        assertEquals(1100.0, n1.x, .01)
        assertEquals(1050.0, n1.y, .01)
    }

    @Test
    fun `test repeated adds are evenly spaced when the view re-centers after each add`() {
        val net = Network()
        // Auto zoom recenters the view on the contents after every add
        net.placementManager.visibleBounds = {
            val center = net.getModels<Neuron>().takeIf { it.isNotEmpty() }?.centerLocation ?: point(0.0, 0.0)
            Rectangle2D.Double(center.x - 200.0, center.y - 100.0, 400.0, 200.0)
        }
        val row = List(4) { net.addPlacedNeuron() }
        row.zipWithNext().forEach { (a, b) ->
            assertEquals(a.x + NEURON_FOOTPRINT, b.x, .01)
            assertEquals(a.y, b.y, .01)
        }
    }

    @Test
    fun `test duplicating the first neuron in a row does not land on its neighbor`() {
        val net = Network()
        val row = List(3) { net.addPlacedNeuron() }
        val (copy) = net.copyOf(listOf(row[0]), CopyPlacement.DUPLICATE)
        assertNoOverlap(row + copy)
        // Right of the source is taken, so the copy goes below it
        assertEquals(row[0].x, copy.x, .01)
        assertEquals(row[0].y + NEURON_FOOTPRINT, copy.y, .01)
    }

    @Test
    fun `test duplicating a row does not overlap the row`() {
        val net = Network()
        val row = List(4) { net.addPlacedNeuron() }
        val copies = net.copyOf(row, CopyPlacement.DUPLICATE)
        assertNoOverlap(row + copies)
        assertEquals(row.last().x + NEURON_FOOTPRINT, copies.first().x, .01)
        assertEquals(row.first().y, copies.first().y, .01)
    }

    @Test
    fun `test moving a duplicate and duplicating again repeats the move`() {
        val net = Network()
        val n1 = net.addPlacedNeuron()
        val (copy1) = net.copyOf(listOf(n1), CopyPlacement.DUPLICATE)
        copy1.location = point(n1.x + 30, n1.y + 70)
        val (copy2) = net.copyOf(listOf(copy1), CopyPlacement.DUPLICATE)
        assertEquals(n1.x + 60, copy2.x, .01)
        assertEquals(n1.y + 140, copy2.y, .01)
        // The move keeps repeating without being made again
        val (copy3) = net.copyOf(listOf(copy2), CopyPlacement.DUPLICATE)
        assertEquals(n1.x + 90, copy3.x, .01)
        assertEquals(n1.y + 210, copy3.y, .01)
    }

    @Test
    fun `test duplicating something else ends the repeated move`() {
        val net = Network()
        val n1 = net.addPlacedNeuron()
        val (copy1) = net.copyOf(listOf(n1), CopyPlacement.DUPLICATE)
        copy1.location = point(n1.x + 30, n1.y + 70)
        val (copy2) = net.copyOf(listOf(n1), CopyPlacement.DUPLICATE)
        assertEquals(n1.x + NEURON_FOOTPRINT, copy2.x, .01)
        assertEquals(n1.y, copy2.y, .01)
    }

    @Test
    fun `test dragging an in-place copy away and duplicating it repeats the drag`() {
        val net = Network()
        val n1 = net.addPlacedNeuron()
        val (copy1) = net.copyOf(listOf(n1), CopyPlacement.IN_PLACE)
        assertEquals(n1.location, copy1.location)
        copy1.location = point(n1.x - 20, n1.y + 60)
        val (copy2) = net.copyOf(listOf(copy1), CopyPlacement.DUPLICATE)
        assertEquals(n1.x - 40, copy2.x, .01)
        assertEquals(n1.y + 120, copy2.y, .01)
    }

    @Test
    fun `test pastes after dragging an in-place copy away continue in the direction of the drag`() {
        val net = Network()
        val n1 = net.addPlacedNeuron()
        net.placementManager.onCopy()
        val (dragged) = net.copyOf(listOf(n1), CopyPlacement.IN_PLACE)
        dragged.location = point(n1.x + 10, n1.y - 80)
        val (paste1) = net.copyOf(listOf(n1), CopyPlacement.PASTE)
        val (paste2) = net.copyOf(listOf(n1), CopyPlacement.PASTE)
        assertEquals(n1.x + 20, paste1.x, .01)
        assertEquals(n1.y - 160, paste1.y, .01)
        assertEquals(n1.x + 30, paste2.x, .01)
        assertEquals(n1.y - 240, paste2.y, .01)
    }

    @Test
    fun `test moving a paste and pasting again repeats the move until the clipboard changes`() {
        val net = Network()
        val n1 = net.addPlacedNeuron()
        net.placementManager.onCopy()
        val (paste1) = net.copyOf(listOf(n1), CopyPlacement.PASTE)
        paste1.location = point(n1.x, n1.y + 100)
        val (paste2) = net.copyOf(listOf(n1), CopyPlacement.PASTE)
        assertEquals(n1.x, paste2.x, .01)
        assertEquals(n1.y + 200, paste2.y, .01)

        net.placementManager.onCopy()
        val (paste3) = net.copyOf(listOf(n1), CopyPlacement.PASTE)
        assertEquals(n1.x + NEURON_FOOTPRINT, paste3.x, .01)
        assertEquals(n1.y, paste3.y, .01)
    }

    @Test
    fun `test paste goes to an insertion point set after the copy`() {
        val net = Network()
        val n1 = net.addPlacedNeuron()
        net.placementManager.onCopy()
        net.placementManager.insertionPoint = point(500.0, 300.0)
        val (copy) = net.copyOf(listOf(n1), CopyPlacement.PASTE)
        assertEquals(500.0, copy.x, .01)
        assertEquals(300.0, copy.y, .01)
    }

    @Test
    fun `test paste ignores an insertion point set before the copy and successive pastes trail`() {
        val net = Network()
        net.placementManager.insertionPoint = point(500.0, 300.0)
        val n1 = net.addPlacedNeuron()
        net.placementManager.onCopy()
        val (paste1) = net.copyOf(listOf(n1), CopyPlacement.PASTE)
        val (paste2) = net.copyOf(listOf(n1), CopyPlacement.PASTE)
        assertEquals(n1.x + NEURON_FOOTPRINT, paste1.x, .01)
        assertEquals(n1.x + 2 * NEURON_FOOTPRINT, paste2.x, .01)
        assertEquals(n1.y, paste2.y, .01)
    }

    @Test
    fun `test neuron arrays are placed side by side without overlapping`() {
        val net = Network()
        val na1 = NeuronArray(20).also { net.addNetworkModelAsync(it) }
        val na2 = NeuronArray(20).also { net.addNetworkModelAsync(it) }
        assertEquals(na1.location.y, na2.location.y, .01)
        assertTrue(na2.location.x > na1.location.x)
        assertTrue(!na1.footprint().createIntersection(na2.footprint()).let { it.width > 1 && it.height > 1 })
    }

    @Test
    fun `test first free offset skips blocked candidates`() {
        val footprint = Rectangle2D.Double(0.0, 0.0, 10.0, 10.0)
        val occupied = listOf(Rectangle2D.Double(0.0, 0.0, 10.0, 10.0), Rectangle2D.Double(10.0, 0.0, 10.0, 10.0))
        val candidates = sequenceOf(point(0.0, 0.0), point(10.0, 0.0), point(20.0, 0.0))
        assertEquals(point(20.0, 0.0), firstFreeOffset(footprint, occupied, candidates))
    }

}
