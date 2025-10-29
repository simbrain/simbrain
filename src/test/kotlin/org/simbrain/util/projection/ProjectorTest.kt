package org.simbrain.util.projection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.plot.projection.ProjectionComponent
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class ProjectorTest {

    @Test
    fun `changing dimension resets dataset`() {
        val projector = Projector(3)
        val originalDataset = projector.dataset
        projector.dimension = 5
        assertEquals(5, projector.dimension)
        assert(projector.dataset !== originalDataset)
        assertEquals(0, projector.dataset.kdTree.size)
    }

    @Test
    fun `set projector to same dimension does not reset dataset`() {
        val projector = Projector(4)
        val originalDataset = projector.dataset
        projector.dimension = 4
        assert(projector.dataset === originalDataset)
    }

    @Test
    fun `adding point with new dimension resets dataset`() {
        val projector = Projector(2)
        projector.addDataPoint(doubleArrayOf(1.0, 2.0))
        val originalDataset = projector.dataset
        projector.addDataPoint(doubleArrayOf(1.0, 2.0, 3.0)) // Triggers reset
        assertEquals(3, projector.dimension)
        assert(projector.dataset !== originalDataset)
    }

    @Test
    fun testXStream() {

        // Create a projection plot with 5 points
        val pc = ProjectionComponent("Test", Projector(4))
        pc.addPoint(doubleArrayOf(1.0,2.0,3.0,4.0))
        pc.addPoint(doubleArrayOf(2.0,0.0,2.0,4.0))
        pc.addPoint(doubleArrayOf(3.0,0.0,3.0,1.0))
        pc.addPoint(doubleArrayOf(4.0,1.0,5.0,4.0))
        pc.addPoint(doubleArrayOf(1.0,-1.0,-5.0,4.0))

        val xstream = pc.xml
        //println(xstream)
        val stream: InputStream = ByteArrayInputStream(xstream.toByteArray(StandardCharsets.UTF_8))

        // Unmarshall from xstream
        val pc2 = ProjectionComponent.open(stream, "test2", "xml")
        assertEquals(4,pc2.projector.dimension)
        assertEquals(5,pc2.projector.dataset.kdTree.size)
    }

    @Test
    fun `normal values are added successfully`() {
        val projector = Projector(3)
        projector.addDataPoint(doubleArrayOf(1.0, 2.0, 3.0))
        projector.addDataPoint(doubleArrayOf(100.0, 200.0, 300.0))
        projector.addDataPoint(doubleArrayOf(-50.0, -100.0, -150.0))
        
        assertEquals(3, projector.dataset.kdTree.size)
    }

    @Test
    fun `extremely large values are filtered out`() {
        val projector = Projector(2)
        val initialSize = projector.dataset.kdTree.size
        
        // Add normal point first
        projector.addDataPoint(doubleArrayOf(1.0, 2.0))
        assertEquals(initialSize + 1, projector.dataset.kdTree.size)
        
        // Try to add point with extremely large values (default threshold is 1e50)
        projector.addDataPoint(doubleArrayOf(1e100, 2.0))
        projector.addDataPoint(doubleArrayOf(1.0, 1e60))
        projector.addDataPoint(doubleArrayOf(-1e75, 3.0))
        
        // Should still only have 1 point
        assertEquals(initialSize + 1, projector.dataset.kdTree.size)
    }

    @Test
    fun `infinite values are filtered out`() {
        val projector = Projector(2)
        val initialSize = projector.dataset.kdTree.size
        
        // Try to add points with infinite values
        projector.addDataPoint(doubleArrayOf(Double.POSITIVE_INFINITY, 2.0))
        projector.addDataPoint(doubleArrayOf(1.0, Double.NEGATIVE_INFINITY))
        projector.addDataPoint(doubleArrayOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
        
        // No points should be added
        assertEquals(initialSize, projector.dataset.kdTree.size)
    }

    @Test
    fun `NaN values are filtered out`() {
        val projector = Projector(2)
        val initialSize = projector.dataset.kdTree.size
        
        // Try to add points with NaN values
        projector.addDataPoint(doubleArrayOf(Double.NaN, 2.0))
        projector.addDataPoint(doubleArrayOf(1.0, Double.NaN))
        projector.addDataPoint(doubleArrayOf(Double.NaN, Double.NaN))
        
        // No points should be added
        assertEquals(initialSize, projector.dataset.kdTree.size)
    }

    @Test
    fun `configurable threshold works correctly`() {
        val projector = Projector(2)
        projector.extremeValueThreshold = 1000.0  // Set lower threshold
        
        val initialSize = projector.dataset.kdTree.size
        
        // Values below threshold should be accepted
        projector.addDataPoint(doubleArrayOf(500.0, 600.0))
        assertEquals(initialSize + 1, projector.dataset.kdTree.size)
        
        // Values at or above threshold should be filtered
        projector.addDataPoint(doubleArrayOf(1000.0, 2.0))  // >= threshold
        projector.addDataPoint(doubleArrayOf(1.0, 2000.0))  // >= threshold
        
        // Should still only have 1 point
        assertEquals(initialSize + 1, projector.dataset.kdTree.size)
    }

    @Test
    fun `threshold boundary values work correctly`() {
        val projector = Projector(2)
        projector.extremeValueThreshold = 100.0
        
        val initialSize = projector.dataset.kdTree.size
        
        // Values just below threshold should be accepted
        projector.addDataPoint(doubleArrayOf(99.9, 50.0))
        projector.addDataPoint(doubleArrayOf(-99.9, 25.0))
        assertEquals(initialSize + 2, projector.dataset.kdTree.size)
        
        // Values at threshold should be filtered
        projector.addDataPoint(doubleArrayOf(100.0, 1.0))
        projector.addDataPoint(doubleArrayOf(-100.0, 1.0))
        
        // Should still only have 2 points
        assertEquals(initialSize + 2, projector.dataset.kdTree.size)
    }

    @Test
    fun `mixed normal and extreme values work correctly`() {
        val projector = Projector(3)
        val initialSize = projector.dataset.kdTree.size
        
        // Add mix of normal and extreme values
        projector.addDataPoint(doubleArrayOf(1.0, 2.0, 3.0))        // Normal - should be added
        projector.addDataPoint(doubleArrayOf(1e100, 2.0, 3.0))      // Extreme - should be filtered
        projector.addDataPoint(doubleArrayOf(4.0, 5.0, 6.0))        // Normal - should be added
        projector.addDataPoint(doubleArrayOf(7.0, Double.NaN, 9.0)) // NaN - should be filtered
        projector.addDataPoint(doubleArrayOf(10.0, 11.0, 12.0))     // Normal - should be added
        
        // Should only have 3 normal points
        assertEquals(initialSize + 3, projector.dataset.kdTree.size)
    }
}

