package org.simbrain.util.projection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DatasetTest {

    var dataset: Dataset = Dataset(2)
    var p1 = DataPoint(doubleArrayOf(1.0, 0.0))
    var p2 = DataPoint(doubleArrayOf(0.5, 0.0))
    var p3 = DataPoint(doubleArrayOf(0.0, 0.0))

    @Test
    fun `test last added point`() {
        dataset.addPoint(p1)
        dataset.addPoint(p2)
        assertEquals(p2, dataset.lastAddedPoint)
    }

    @Test
    fun `test tolerance`() {
        dataset.addPoint(p1)
        dataset.addPoint(p2)
        // p3 is .5 away from the nearest point, within tolerance = 1, so don't add it
        dataset.addPoint(p3, 1.0)
        assertEquals(2, dataset.numPoints)
        // p3 is .5 away from the nearest point, abpve tolerance = .4, so add it
        dataset.addPoint(p3, .4)
        assertEquals(3, dataset.numPoints)
    }

    @Test
    fun `test distances`() {
        assertEquals(.5, dataset.getDistance(p1, p2))
        assertEquals(.5, dataset.getDistance(p2, p3))
        assertEquals(1.0, dataset.getDistance(p1, p3))
    }


}

