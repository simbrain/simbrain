package org.simbrain.world.textworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextWorldTest {

    var world = TextWorld()

    @Test
    fun `test update increments current item`() {
        world.text = "This is some text"
        runBlocking { world.update() }
        assertEquals("This", world.currentItem?.text)
        runBlocking { world.update() }
        assertEquals("is", world.currentItem?.text)
    }

    @Test
    fun `test wraparound`() {
        world.text = "Word1 Word2"
        runBlocking {
            world.update()
            world.update()
            world.update()
        }
        assertEquals("Word1", world.currentItem?.text)
    }

}