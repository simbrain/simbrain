package org.simbrain.util.projection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DatasetTest {

    var projector = Projector(2)

    val dataset = projector.dataset
    var p1 = DataPoint(doubleArrayOf(1.0, 0.0))
    var p2 = DataPoint(doubleArrayOf(0.5, 0.0))
    var p3 = DataPoint(doubleArrayOf(0.0, 0.0))

    @Test
    fun `test last added point`() {
        projector.addDataPoint(p1)
        projector.addDataPoint(p2)
        assertEquals(p2, dataset.currentPoint)
    }

    @Test
    fun `test tolerance`() {
        projector.addDataPoint(p1)
        projector.addDataPoint(p2)
        // p3 is .5 away from the nearest point, within tolerance = 1, so don't add it
        projector.tolerance = 1.0
        projector.addDataPoint(p3)
        assertEquals(2, dataset.kdTree.size)
        // p3 is .5 away from the nearest point, abpve tolerance = .4, so add it
        projector.tolerance = 0.4
        projector.addDataPoint(p3)
        assertEquals(3, dataset.kdTree.size)
    }

    @Test
    fun `test distances`() {
        assertEquals(.5, p1.euclideanDistance(p2))
        assertEquals(.5, p2.euclideanDistance(p3))
        assertEquals(1.0, p1.euclideanDistance(p3))
    }


}

