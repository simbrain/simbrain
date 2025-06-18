package org.simbrain.util.projection

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
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
}

