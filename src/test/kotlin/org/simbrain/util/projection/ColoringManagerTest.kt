package org.simbrain.util.projection

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color

class ColoringManagerTest {

    private lateinit var projector: Projector
    private lateinit var point1: DataPoint
    private lateinit var point2: DataPoint
    private lateinit var point3: DataPoint

    @BeforeEach
    fun setUp() {
        projector = Projector(2)
        projector.baseColor = Color.GRAY
        projector.hotColor = Color.RED
        
        point1 = DataPoint(doubleArrayOf(1.0, 0.0))
        point2 = DataPoint(doubleArrayOf(0.0, 1.0))
        point3 = DataPoint(doubleArrayOf(0.5, 0.5))
        
        projector.addDataPoint(point1)
        projector.addDataPoint(point2)
        projector.addDataPoint(point3)
    }

    @Test
    fun `FrequencyColoringManager - initial state`() {
        val manager = FrequencyColoringManager()
        
        with(projector) {
            // Initially, no points have been activated
            assertEquals(0.0, manager.getActivation(point1), 0.001)
            assertEquals(0.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `FrequencyColoringManager - single activation`() {
        val manager = FrequencyColoringManager()
        
        // Activate point1 once
        manager.activate(point1)
        
        with(projector) {
            // point1 should have activation 1.0 (1 visit / 1 max)
            assertEquals(1.0, manager.getActivation(point1), 0.001)
            assertEquals(0.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `FrequencyColoringManager - multiple activations`() {
        val manager = FrequencyColoringManager()
        
        // Activate point1 three times, point2 once
        manager.activate(point1)
        manager.activate(point1)
        manager.activate(point1)
        manager.activate(point2)
        
        with(projector) {
            // point1 should have activation 1.0 (3 visits / 3 max)
            // point2 should have activation 0.33 (1 visit / 3 max)
            assertEquals(1.0, manager.getActivation(point1), 0.001)
            assertEquals(1.0/3.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `FrequencyColoringManager - reset clears all data`() {
        val manager = FrequencyColoringManager()
        
        // Activate some points
        manager.activate(point1)
        manager.activate(point2)
        manager.activate(point1)
        
        // Reset and verify everything is cleared
        manager.reset()
        
        with(projector) {
            assertEquals(0.0, manager.getActivation(point1), 0.001)
            assertEquals(0.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `FrequencyColoringManager - copy creates independent instance`() {
        val manager = FrequencyColoringManager()
        manager.highFrequencyColor = Color.BLUE
        manager.activate(point1)
        
        val copy = manager.copy()
        
        // Copy should have same configuration but independent state
        assertEquals(Color.BLUE, copy.highFrequencyColor)
        
        // Activate on copy should not affect original
        copy.activate(point2)
        
        with(projector) {
            assertEquals(1.0, manager.getActivation(point1), 0.001)
            assertEquals(0.0, manager.getActivation(point2), 0.001)
        }
    }

    @Test
    fun `FrequencyColoringManager - color interpolation`() {
        val manager = FrequencyColoringManager()
        manager.highFrequencyColor = Color.GREEN
        
        // Activate point1 twice, point2 once
        manager.activate(point1)
        manager.activate(point1)
        manager.activate(point2)
        
        with(projector) {
            val color1 = manager.getColor(point1)
            val color2 = manager.getColor(point2)
            
            // point1 should be closer to high frequency color (more green)
            // point2 should be closer to base color (more gray)
            assertNotNull(color1)
            assertNotNull(color2)
            
            // Colors should be different
            assertNotEquals(color1, color2)
        }
    }

    @Test
    fun `MarkovColoringManager - initial state`() {
        val manager = MarkovColoringManager()
        
        with(projector) {
            // Initially, no transitions have been recorded
            assertEquals(0.0, manager.getActivation(point1), 0.001)
            assertEquals(0.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `MarkovColoringManager - single transition`() {
        val manager = MarkovColoringManager()
        
        // Create transition: point1 -> point2
        manager.activate(point1)
        manager.activate(point2)
        
        with(projector) {
            // Set current point to point1 to check transition probabilities from point1
            dataset.currentPoint = point1
            
            // From point1, there should be 100% probability to go to point2
            assertEquals(1.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point1), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `MarkovColoringManager - multiple transitions`() {
        val manager = MarkovColoringManager()
        
        // Create transitions: point1 -> point2 (twice), point1 -> point3 (once)
        manager.activate(point1)
        manager.activate(point2)
        manager.activate(point1)
        manager.activate(point2)
        manager.activate(point1)
        manager.activate(point3)
        
        with(projector) {
            // Set current point to point1 to check transition probabilities from point1
            dataset.currentPoint = point1
            
            // From point1: 2 transitions to point2, 1 to point3 (max = 2)
            assertEquals(1.0, manager.getActivation(point2), 0.001) // 2/2 = 1.0
            assertEquals(0.5, manager.getActivation(point3), 0.001) // 1/2 = 0.5
            assertEquals(0.0, manager.getActivation(point1), 0.001) // 0/2 = 0.0
        }
    }

    @Test
    fun `MarkovColoringManager - different starting points`() {
        val manager = MarkovColoringManager()
        
        // Create transitions: point1 -> point2, point2 -> point3
        manager.activate(point1)
        manager.activate(point2)
        manager.activate(point3)
        
        with(projector) {
            // Check transitions from point1
            dataset.currentPoint = point1
            assertEquals(1.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
            
            // Check transitions from point2
            dataset.currentPoint = point2
            assertEquals(0.0, manager.getActivation(point1), 0.001)
            assertEquals(1.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `MarkovColoringManager - reset clears all data`() {
        val manager = MarkovColoringManager()
        
        // Create some transitions
        manager.activate(point1)
        manager.activate(point2)
        manager.activate(point3)
        
        // Reset and verify everything is cleared
        manager.reset()
        
        with(projector) {
            dataset.currentPoint = point1
            assertEquals(0.0, manager.getActivation(point1), 0.001)
            assertEquals(0.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `MarkovColoringManager - copy creates independent instance`() {
        val manager = MarkovColoringManager()
        manager.highProbabilityColor = Color.YELLOW
        
        // Create transition
        manager.activate(point1)
        manager.activate(point2)
        
        val copy = manager.copy()
        
        // Copy should have same configuration but independent state
        assertEquals(Color.YELLOW, copy.highProbabilityColor)
        
        // Activate on copy should not affect original
        copy.activate(point3)
        
        with(projector) {
            dataset.currentPoint = point1
            assertEquals(1.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `MarkovColoringManager - color interpolation`() {
        val manager = MarkovColoringManager()
        manager.highProbabilityColor = Color.BLUE
        
        // Create transitions with different probabilities
        manager.activate(point1)
        manager.activate(point2)
        manager.activate(point1)
        manager.activate(point2)
        manager.activate(point1)
        manager.activate(point3)
        
        with(projector) {
            dataset.currentPoint = point1
            
            val color2 = manager.getColor(point2) // Should be high probability color
            val color3 = manager.getColor(point3) // Should be medium probability color
            
            assertNotNull(color2)
            assertNotNull(color3)
            
            // Colors should be different due to different probabilities
            assertNotEquals(color2, color3)
        }
    }

    @Test
    fun `MarkovColoringManager - no previous point on first activation`() {
        val manager = MarkovColoringManager()
        
        // First activation should not create any transitions
        manager.activate(point1)
        
        with(projector) {
            dataset.currentPoint = point1
            assertEquals(0.0, manager.getActivation(point1), 0.001)
            assertEquals(0.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `MarkovColoringManager - handles current point being null`() {
        val manager = MarkovColoringManager()
        
        // Create some transitions
        manager.activate(point1)
        manager.activate(point2)
        
        with(projector) {
            // Set current point to null
            dataset.currentPoint = null
            
            // Should handle null current point gracefully
            assertEquals(0.0, manager.getActivation(point1), 0.001)
            assertEquals(0.0, manager.getActivation(point2), 0.001)
        }
    }

    @Test
    fun `MarkovColoringManager - edge case with single point repeatedly`() {
        val manager = MarkovColoringManager()
        
        // Activate the same point multiple times
        manager.activate(point1)
        manager.activate(point1)
        manager.activate(point1)
        
        with(projector) {
            dataset.currentPoint = point1
            
            // Should show 100% probability of self-transition
            assertEquals(1.0, manager.getActivation(point1), 0.001)
            assertEquals(0.0, manager.getActivation(point2), 0.001)
            assertEquals(0.0, manager.getActivation(point3), 0.001)
        }
    }

    @Test
    fun `both managers implement updateAllColors and copy correctly`() {
        val freqManager = FrequencyColoringManager()
        val markovManager = MarkovColoringManager()
        
        // Test updateAllColors doesn't throw exceptions
        assertDoesNotThrow {
            freqManager.updateAllColors()
            markovManager.updateAllColors()
        }
        
        // Test copy creates proper instances
        val freqCopy = freqManager.copy()
        val markovCopy = markovManager.copy()
        
        assertTrue(freqCopy is FrequencyColoringManager)
        assertTrue(markovCopy is MarkovColoringManager)
        assertNotSame(freqManager, freqCopy)
        assertNotSame(markovManager, markovCopy)
    }
} 