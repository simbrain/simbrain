package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VisibleTokenTest {

    @Test
    fun `whitespace characters become visible symbols`() {
        assertEquals("␣", visibleToken(" "))
        assertEquals("↵", visibleToken("\n"))
        assertEquals("␍↵", visibleToken("\r\n"))
        assertEquals("⇥", visibleToken("\t"))
        assertEquals("·", visibleToken(" "))
    }

    @Test
    fun `a leading space is shown and ordinary text is untouched`() {
        assertEquals("␣context", visibleToken(" context"))
        assertEquals("cat", visibleToken("cat"))
        assertEquals("", visibleToken(""))
    }
}
