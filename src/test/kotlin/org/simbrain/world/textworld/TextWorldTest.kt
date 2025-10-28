package org.simbrain.world.textworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.workspace.Workspace

class TextWorldTest {

    var world = TextWorld()

    init {
        world.text = "This is some text"
    }

    @Test
    fun `test update increments current item`() {
        world.autoAdvance = true
        assertEquals("this", world.currentToken)
        runBlocking { world.update() }
        assertEquals("is", world.currentToken)
    }

    @Test
    fun `test wraparound`() {
        world.text = "Word1 Word2"
        world.autoAdvance = true
        runBlocking {
            world.update()
            world.update()
        }
        assertEquals("word1", world.currentToken)
    }

    @Test
    fun testXML() {
        val xmlRep = getTextWorldXStream().toXML(world)
        print(xmlRep)
        val fromXml = getTextWorldXStream().fromXML(xmlRep) as TextWorld
        assertNotNull(fromXml)
        assertEquals("This is some text", fromXml.text)
    }

    @Test
    fun `test coupling with mismatched sizes`() {

        runBlocking {
            val workspace = Workspace()
            val textWorldComponent = TextWorldComponent("Test")
            val networkComponent = NetworkComponent("Network")
            val nc1 = networkComponent.network.addNeuronCollection(4)
            val nc2 = networkComponent.network.addNeuronCollection(14)
            workspace.addWorkspaceComponent(textWorldComponent)
            workspace.addWorkspaceComponent(networkComponent)

            val coupling1 = with(workspace.couplingManager) {
                createCoupling(nc1, textWorldComponent.world)
            }
            workspace.simpleIterate()
            assert(textWorldComponent.world.text.isNotEmpty())
            println(textWorldComponent.world.text)
            with(workspace.couplingManager) {
                removeCoupling(coupling1)
            }

            val coupling2 = with(workspace.couplingManager) {
                createCoupling(nc2, textWorldComponent.world)
            }
            workspace.simpleIterate()
            assert(textWorldComponent.world.text.isNotEmpty())
            println(textWorldComponent.world.text)

        }

    }

    @Test
    fun `test saving and reopening with couplings`() {

        runBlocking {
            val workspace = Workspace()
            val textWorldComponent = TextWorldComponent("Text World")
            textWorldComponent.world.text = "Was this saved?"
            val networkComponent = NetworkComponent("Network")
            val nc = networkComponent.network.addNeuronCollection(6)
            workspace.addWorkspaceComponent(textWorldComponent)
            workspace.addWorkspaceComponent(networkComponent)
            with(workspace.couplingManager) {
                createCoupling(nc, textWorldComponent.world)
            }
            val zipData = workspace.generateZipData(true)
            workspace.clearWorkspace()
            workspace.openFromZipData(zipData)
            val openedWorld = workspace.getComponent("Text World") as TextWorldComponent
            assertEquals("Was this saved?", openedWorld.world.text)
            assertEquals(2, workspace.componentList.size)
            assertEquals(1, workspace.couplings.size)
            //println(workspace)
        }
    }

    // Tests for cursor position synchronization fixes

    @Test
    fun `test empty text returns safe defaults`() {
        val emptyWorld = TextWorld()
        emptyWorld.text = ""
        
        // Should not crash and return safe defaults
        assertEquals(0, emptyWorld.currentTokenIndex)
        assertEquals("", emptyWorld.currentToken)
        assertEquals(0, emptyWorld.currentVector.size)
        assertEquals(0, emptyWorld.position)
    }

    @Test
    fun `test currentTokenIndex handles empty tokens safely`() {
        val emptyWorld = TextWorld()
        emptyWorld.text = ""
        
        // Setting currentTokenIndex on empty tokens should not crash
        emptyWorld.currentTokenIndex = 5
        assertEquals(0, emptyWorld.currentTokenIndex) // Should stay at 0
    }

    @Test
    fun `test setPosition updates currentTokenIndex`() {
        val testWorld = TextWorld()
        testWorld.text = "hello world foo bar"
        // Tokens: "hello" (0-4), "world" (6-10), "foo" (12-14), "bar" (16-18)
        
        // Position at start of first token
        testWorld.setPosition(0, false)
        assertEquals(0, testWorld.currentTokenIndex)
        assertEquals("hello", testWorld.currentToken)
        
        // Position in middle of second token
        testWorld.setPosition(8, false)
        assertEquals(1, testWorld.currentTokenIndex)
        assertEquals("world", testWorld.currentToken)
        
        // Position at start of third token
        testWorld.setPosition(12, false)
        assertEquals(2, testWorld.currentTokenIndex)
        assertEquals("foo", testWorld.currentToken)
        
        // Position at end of last token
        testWorld.setPosition(18, false)
        assertEquals(3, testWorld.currentTokenIndex)
        assertEquals("bar", testWorld.currentToken)
    }

    @Test
    fun `test setPosition at token boundaries`() {
        val testWorld = TextWorld()
        testWorld.text = "cat dog"
        // Tokens: "cat" (0-2), "dog" (4-6)
        
        // Position at end of first token
        testWorld.setPosition(2, false)
        assertEquals(0, testWorld.currentTokenIndex)
        assertEquals("cat", testWorld.currentToken)
        
        // Position in space between tokens (should pick last token or next)
        testWorld.setPosition(3, false)
        // Behavior: position 3 is after "cat", should be in first or second token
        // Since it's past the end of "cat", it should find the next best match
        assert(testWorld.currentTokenIndex in 0..1)
        
        // Position at start of second token
        testWorld.setPosition(4, false)
        assertEquals(1, testWorld.currentTokenIndex)
        assertEquals("dog", testWorld.currentToken)
    }

    @Test
    fun `test setPosition beyond text length`() {
        val testWorld = TextWorld()
        testWorld.text = "test"
        
        val originalPosition = testWorld.position
        
        // Try to set position beyond text length
        testWorld.setPosition(100, false)
        
        // Position should not change
        assertEquals(originalPosition, testWorld.position)
    }

    @Test
    fun `test position clamps when text changes`() {
        val testWorld = TextWorld()
        testWorld.text = "hello world foo bar"
        testWorld.position = 15
        
        // Shorten text
        testWorld.text = "hi"
        
        // Position should be clamped to text length
        assert(testWorld.position <= testWorld.text.length)
    }

    @Test
    fun `test currentVector returns empty array for empty text`() {
        val emptyWorld = TextWorld()
        emptyWorld.text = ""
        
        val vector = emptyWorld.currentVector
        assertNotNull(vector)
        assertEquals(0, vector.size)
    }

    @Test
    fun `test currentToken returns empty string for empty text`() {
        val emptyWorld = TextWorld()
        emptyWorld.text = ""
        
        val token = emptyWorld.currentToken
        assertNotNull(token)
        assertEquals("", token)
    }

    @Test
    fun `test multiple position changes track correct tokens`() {
        val testWorld = TextWorld()
        testWorld.text = "one two three four five"
        // Token positions: "one" (0-2), "two" (4-6), "three" (8-12), "four" (14-17), "five" (19-22)
        
        // Simulate clicking through the text
        val positions = listOf(
            0 to "one",    // Start of first token
            5 to "two",    // Middle of second token
            10 to "three", // Middle of third token
            15 to "four",  // Middle of fourth token
            20 to "five"   // Middle of last token
        )
        
        for ((pos, expectedToken) in positions) {
            testWorld.setPosition(pos, false)
            assertEquals(expectedToken, testWorld.currentToken, 
                "Position $pos should be in token '$expectedToken'")
        }
    }

    @Test
    fun `test currentTokenIndex bounds checking`() {
        val testWorld = TextWorld()
        testWorld.text = "word1 word2 word3"
        
        // Try to set index beyond bounds
        testWorld.currentTokenIndex = 100
        
        // Should be clamped to last valid index
        assert(testWorld.currentTokenIndex <= 2) // 3 tokens means lastIndex is 2
    }

    @Test
    fun `test currentTokenIndex negative value`() {
        val testWorld = TextWorld()
        testWorld.text = "word1 word2"
        
        // Try to set negative index
        testWorld.currentTokenIndex = -5
        
        // Should be clamped to 0
        assertEquals(0, testWorld.currentTokenIndex)
    }

    @Test
    fun `test position and currentTokenIndex stay synchronized`() {
        val testWorld = TextWorld()
        testWorld.text = "alpha beta gamma"
        
        // Set position to second token
        testWorld.setPosition(8, false) // Middle of "beta"
        val tokenAfterPositionSet = testWorld.currentTokenIndex
        
        // Current token should be "beta"
        assertEquals("beta", testWorld.currentToken)
        
        // Advance to next token via update
        testWorld.currentTokenIndex++
        
        // Position should update to reflect new token
        // (Note: advance() does this, but direct index change also updates position via setter)
        assertEquals("gamma", testWorld.currentToken)
    }

    @Test
    fun `test cursor at end of token highlights correct token`() {
        val testWorld = TextWorld()
        testWorld.text = "hello world foo"
        // Tokens: "hello" (0-4), "world" (6-10), "foo" (12-14)
        
        // Place cursor right after "hello" (position 5 is the space after)
        testWorld.setPosition(5, false)
        // Should highlight "hello", not the last token
        assertEquals("hello", testWorld.currentToken)
        assertEquals(0, testWorld.currentTokenIndex)
        
        // Place cursor right after "world" (position 11 is the space after)  
        testWorld.setPosition(11, false)
        // Should highlight "world", not the last token
        assertEquals("world", testWorld.currentToken)
        assertEquals(1, testWorld.currentTokenIndex)
        
        // Place cursor at the very end (position 15, after "foo")
        testWorld.setPosition(15, false)
        // Should highlight "foo" (the actual last token)
        assertEquals("foo", testWorld.currentToken)
        assertEquals(2, testWorld.currentTokenIndex)
    }


}