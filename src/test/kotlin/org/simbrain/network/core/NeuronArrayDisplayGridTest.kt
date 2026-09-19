/**
 * Tests for the grid a [NeuronArray] reports for drawing itself: automatic and explicit column counts,
 * transposition by a vertical layout, and the mapping between neuron indices and display cells.
 */
package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NeuronArrayDisplayGridTest {

    @Test
    fun `grid columns of zero gives a square grid`() {
        val array = NeuronArray(16).apply { gridMode = true }
        assertEquals(4, array.displayColumns)
        assertEquals(4, array.displayRows)
    }

    @Test
    fun `explicit grid columns wrap rows at that width`() {
        val array = NeuronArray(12).apply {
            gridMode = true
            gridColumns = 5
        }
        assertEquals(5, array.displayColumns)
        assertEquals(3, array.displayRows)
        assertEquals(1 to 2, array.displayCellOf(7))
        assertEquals(7, array.indexAtDisplayCell(1, 2))
        assertNull(array.indexAtDisplayCell(2, 2)) { "Cells past the last neuron are empty" }
    }

    @Test
    fun `grid columns larger than the array give a single row`() {
        val array = NeuronArray(4).apply {
            gridMode = true
            gridColumns = 10
        }
        assertEquals(4, array.displayColumns)
        assertEquals(1, array.displayRows)
    }

    @Test
    fun `grid columns are ignored outside grid mode`() {
        val array = NeuronArray(12).apply { gridColumns = 5 }
        assertEquals(12, array.displayColumns)
        assertEquals(1, array.displayRows)
    }

    @Test
    fun `vertical layout transposes a grid with explicit columns`() {
        val array = NeuronArray(12).apply {
            gridMode = true
            gridColumns = 5
            verticalLayout = true
        }
        assertEquals(3, array.displayColumns)
        assertEquals(5, array.displayRows)
        assertEquals(2 to 1, array.displayCellOf(7))
        assertEquals(7, array.indexAtDisplayCell(2, 1))
        assertNull(array.indexAtDisplayCell(2, 2))
        (0 until 12).forEach { i ->
            val (row, col) = array.displayCellOf(i)
            assertEquals(i, array.indexAtDisplayCell(row, col))
        }
    }

    @Test
    fun `vertical layout does not transpose the automatic square grid`() {
        val array = NeuronArray(6).apply {
            gridMode = true
            verticalLayout = true
        }
        assertFalse(array.isGridTransposed)
        assertEquals(0 to 1, array.displayCellOf(1))
    }

    @Test
    fun `display order rearranges values only for a transposed grid`() {
        val values = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0)
        val array = NeuronArray(5).apply {
            gridMode = true
            gridColumns = 2
        }
        assertSame(values, array.toDisplayOrder(values))

        array.verticalLayout = true
        // Columns of two filled top to bottom: 0 2 4 / 1 3 _
        assertArrayEquals(doubleArrayOf(0.0, 2.0, 4.0, 1.0, 3.0, -1.0), array.toDisplayOrder(values, empty = -1.0))
    }

    @Test
    fun `copy keeps grid columns and circle spacing`() {
        val array = NeuronArray(12).apply {
            gridColumns = 4
            circleSpacingX = 80.0
            circleSpacingY = 60.0
        }
        val copy = array.copy()
        assertEquals(4, copy.gridColumns)
        assertEquals(80.0, copy.circleSpacingX)
        assertEquals(60.0, copy.circleSpacingY)
    }
}
