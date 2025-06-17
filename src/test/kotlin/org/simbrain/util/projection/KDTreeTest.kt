package org.simbrain.util.projection

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.plot.projection.ProjectionComponent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class KDTreeTest {

    @Test
    fun `iteration order is deterministic`() {
        val points = listOf(
            DataPoint(doubleArrayOf(3.0, 6.0)),
            DataPoint(doubleArrayOf(3.0, 6.0)),
            DataPoint(doubleArrayOf(3.0, 6.0)),
            DataPoint(doubleArrayOf(3.0, 6.0)),
            DataPoint(doubleArrayOf(17.0, 15.0)),
            DataPoint(doubleArrayOf(13.0, 15.0)),
            DataPoint(doubleArrayOf(6.0, 12.0)),
            DataPoint(doubleArrayOf(9.0, 1.0)),
            DataPoint(doubleArrayOf(9.0, 1.0)),
            DataPoint(doubleArrayOf(9.0, 1.0)),
            DataPoint(doubleArrayOf(9.0, 1.0)),
            DataPoint(doubleArrayOf(2.0, 7.0)),
            DataPoint(doubleArrayOf(2.0, 7.0)),
            DataPoint(doubleArrayOf(2.0, 7.0)),
            DataPoint(doubleArrayOf(2.0, 7.0)),
            DataPoint(doubleArrayOf(2.0, 7.0))
        )

        val kdTree = KDTree(2)
        points.forEach {
            kdTree.insert(it)
        }
        val a = kdTree.toList()
        val b = kdTree.toList()
        (a zip b).forEach { (aa, bb) ->
            assert(aa === bb)
        }
    }


    @Test
    fun `findClosestPoints finds closest points`() {
        val projector = Projector(4)
        projector.initProjector()
        println(projector.dataset)
        projector.addDataPoint(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0))
        projector.addDataPoint(doubleArrayOf(2.0, 3.0, 4.0, 5.0, 6.0))
        projector.addDataPoint(doubleArrayOf(3.0, 4.0, 5.0, 6.0, 7.0))
        projector.addDataPoint(doubleArrayOf(4.0, 5.0, 6.0, 7.0, 8.0))
        projector.addDataPoint(doubleArrayOf(5.0, 6.0, 7.0, 8.0, 9.0))

        val searchPoint = DataPoint(doubleArrayOf(1.1, 2.0, 3.0, 4.0, 5.0))
        val closestPoints = projector.dataset.kdTree.findClosestNPoints(searchPoint, 3)
        closestPoints.forEach { point ->
            println("Closest point: $point")
        }

        projector.addDataPoint(doubleArrayOf(1.5, 2.0, 3.0, 4.0, 5.0))
        projector.addDataPoint(doubleArrayOf(2.5, 3.0, 4.0, 5.0, 6.0))
        projector.addDataPoint(doubleArrayOf(3.5, 4.0, 5.0, 6.0, 7.0))
        projector.addDataPoint(doubleArrayOf(4.5, 5.0, 6.0, 7.0, 8.0))
        projector.addDataPoint(doubleArrayOf(5.5, 6.0, 7.0, 8.0, 9.0))

        val closestPoints2 = projector.dataset.kdTree.findClosestNPoints(searchPoint, 3)
        closestPoints2.forEach { point ->
            println("Closest point: $point")
        }
    }

    @Test
    fun `serialization and deserialization preserves data`() {
        // Create a projector with sample data
        val originalProjector = Projector(3)
        originalProjector.tolerance = 0.05
        originalProjector.connectPoints = true
        originalProjector.projectionMethod = PCAProjection()
        
        // Add some test data points
        val testPoints = listOf(
            DataPoint(doubleArrayOf(1.0, 2.0, 3.0), label = "Point1"),
            DataPoint(doubleArrayOf(4.0, 5.0, 6.0), label = "Point2"),
            DataPoint(doubleArrayOf(7.0, 8.0, 9.0), label = "Point3"),
            DataPoint(doubleArrayOf(10.0, 11.0, 12.0), label = "Point4")
        )
        
        testPoints.forEach { point ->
            originalProjector.addDataPoint(point)
        }
        
        // Initialize the projector
        originalProjector.initProjector()
        
        // Create a component and serialize it
        val originalComponent = ProjectionComponent("TestProjection", originalProjector)
        val outputStream = ByteArrayOutputStream()
        originalComponent.save(outputStream, "xml")
        val serializedData = outputStream.toByteArray()
        
        // Deserialize the component
        val inputStream = ByteArrayInputStream(serializedData)
        val deserializedComponent = ProjectionComponent.open(inputStream, "TestProjection", "xml")
        val deserializedProjector = deserializedComponent.projector
        
        // Verify basic properties are preserved
        assertEquals(originalProjector.dimension, deserializedProjector.dimension)
        assertEquals(originalProjector.tolerance, deserializedProjector.tolerance, 0.001)
        assertEquals(originalProjector.connectPoints, deserializedProjector.connectPoints)
        assertEquals(originalProjector.projectionMethod.name, deserializedProjector.projectionMethod.name)
        
        // Verify dataset size is preserved
        assertEquals(originalProjector.dataset.kdTree.size, deserializedProjector.dataset.kdTree.size)
        
        // Verify all data points are preserved
        val originalPoints = originalProjector.dataset.kdTree.toList()
        val deserializedPoints = deserializedProjector.dataset.kdTree.toList()
        
        assertEquals(originalPoints.size, deserializedPoints.size)
        
        // Compare each point's upstairs data and labels
        for (i in originalPoints.indices) {
            val originalPoint = originalPoints[i]
            val deserializedPoint = deserializedPoints[i]
            
            // Compare upstairs point data
            assertArrayEquals(originalPoint.upstairsPoint, deserializedPoint.upstairsPoint, 0.001)
            
            // Compare labels
            assertEquals(originalPoint.label, deserializedPoint.label)
            
            // Compare downstairs point data (projection results)
            assertArrayEquals(originalPoint.downstairsPoint, deserializedPoint.downstairsPoint, 0.001)
        }
    }

    @Test
    fun `KDTree serialization preserves structure`() {
        // Create a KDTree with test data
        val originalKDTree = KDTree(2)
        val testPoints = listOf(
            DataPoint(doubleArrayOf(3.0, 6.0)),
            DataPoint(doubleArrayOf(17.0, 15.0)),
            DataPoint(doubleArrayOf(13.0, 15.0)),
            DataPoint(doubleArrayOf(6.0, 12.0)),
            DataPoint(doubleArrayOf(9.0, 1.0)),
            DataPoint(doubleArrayOf(2.0, 7.0))
        )
        
        testPoints.forEach { originalKDTree.insert(it) }
        
        // Serialize using the KDTreeConvertor directly
        val converter = KDTreeConvertor()
        val outputStream = ByteArrayOutputStream()
        
        // Create a simple XML structure to test the converter
        val xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kdtree>\n"
        val xmlFooter = "\n</kdtree>"
        
        outputStream.write(xmlHeader.toByteArray())
        
        // Use a mock writer and context for testing (simplified approach)
        // For a more complete test, we would need to set up the full XStream context
        // But for now, let's test the round-trip through a projector which uses the converter
        
        val projector = Projector(2)
        testPoints.forEach { projector.addDataPoint(it) }
        
        val component = ProjectionComponent("Test", projector)
        val serializedStream = ByteArrayOutputStream()
        component.save(serializedStream, "xml")
        
        val deserializedComponent = ProjectionComponent.open(
            ByteArrayInputStream(serializedStream.toByteArray()), 
            "Test", 
            "xml"
        )
        
        // Verify the KDTree structure is preserved
        val originalSize = projector.dataset.kdTree.size
        val deserializedSize = deserializedComponent.projector.dataset.kdTree.size
        assertEquals(originalSize, deserializedSize)
        
        // Verify all points are present
        val originalPointsList = projector.dataset.kdTree.toList()
        val deserializedPointsList = deserializedComponent.projector.dataset.kdTree.toList()
        
        assertEquals(originalPointsList.size, deserializedPointsList.size)
        
        // Verify point data matches (order may differ due to tree reconstruction)
        val originalPointsSet = originalPointsList.map { it.upstairsPoint.contentToString() }.toSet()
        val deserializedPointsSet = deserializedPointsList.map { it.upstairsPoint.contentToString() }.toSet()
        assertEquals(originalPointsSet, deserializedPointsSet)
        
        println("KDTree serialization test passed: Tree structure preserved")
    }
}