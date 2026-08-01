/**
 * Tests for the row banding a table draws when its rows are consumed in groups rather than one at a time.
 *
 * Only the colour choice is testable without a realized table; where the separating rules land is covered
 * by the `bptt_trainer_data_bands` UI snapshot.
 */
package org.simbrain.util.table

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RowGroupingTest {

    @Test
    fun `an ungrouped table bands nothing`() {
        (0 until 10).forEach { row ->
            assertNull(rowBandColor(row, null))
        }
    }

    @Test
    fun `groups alternate so that each boundary is visible`() {
        val size = 4
        // Banding only every other group is what makes a boundary readable. Banding all of them would
        // leave no edge between consecutive groups.
        (0 until 4).forEach { assertNull(rowBandColor(it, size)) }
        (4 until 8).forEach { assertNotNull(rowBandColor(it, size)) }
        (8 until 12).forEach { assertNull(rowBandColor(it, size)) }
        (12 until 16).forEach { assertNotNull(rowBandColor(it, size)) }
    }

    @Test
    fun `every row of a group is shaded the same`() {
        val shades = (4 until 8).map { rowBandColor(it, 4) }.distinct()
        assertEquals(1, shades.size)
    }

    @Test
    fun `a group size below one is treated as no grouping`() {
        // Truncation depth is floored at one where it is used, but the table takes its size from whatever
        // hands it one and should not divide by zero if that ever slips.
        assertNull(rowBandColor(3, 0))
        assertNull(rowBandColor(3, -1))
    }
}
