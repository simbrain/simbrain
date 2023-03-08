package org.simbrain.util.projection

import org.junit.jupiter.api.Test

class ProjectTriangulateTest {

    var p1 = DataPoint(doubleArrayOf(1.0, 0.0))
    var p2 = DataPoint(doubleArrayOf(0.5, 0.0))
    var p3 = DataPoint(doubleArrayOf(0.0, 0.0))

    // Just an initial platform for testing

    @Test
    fun `initial test`() {
        val proj = Projector(5)
        proj.projectionMethod = ProjectTriangulate(proj)
        proj.addDatapoint(p1)
        proj.projectionMethod.project()
        print(proj.downstairs)
        proj.addDatapoint(p2)
        proj.projectionMethod.project()
        print(proj.downstairs)
        proj.addDatapoint(p3)
        proj.projectionMethod.project()
        print(proj.downstairs)
        
    }

}

