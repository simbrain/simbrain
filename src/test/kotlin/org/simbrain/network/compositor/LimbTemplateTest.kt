package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LimbTemplateTest {

    @Test
    fun `parse splits rows into cells with empties and shared cells`() {
        val template = LimbTemplate.parse(
            """
            a  b+c  .
            .  d    e
            """
        )
        assertEquals(
            listOf(
                listOf(listOf("a"), listOf("b", "c"), emptyList()),
                listOf(emptyList(), listOf("d"), listOf("e")),
            ),
            template.cells,
        )
        assertEquals(setOf("a", "b", "c", "d", "e"), template.keys)
    }

    @Test
    fun `rows must share one column count`() {
        assertThrows(IllegalArgumentException::class.java) {
            LimbTemplate.parse(
                """
                a  b
                c
                """
            )
        }
    }

    @Test
    fun `a key may appear only once`() {
        assertThrows(IllegalArgumentException::class.java) {
            LimbTemplate.parse(
                """
                a  b
                b  c
                """
            )
        }
    }

    @Test
    fun `row gaps must cover exactly the row boundaries`() {
        assertThrows(IllegalArgumentException::class.java) {
            LimbTemplate.parse("a  b", rowGaps = listOf(10.0))
        }
    }
}
