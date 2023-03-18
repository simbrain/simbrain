package org.simbrain.util.projection

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.simbrain.util.math.SimbrainMath
import java.util.*

class PCAProjection2Test {
    @Test
    fun checkPCAProjection() {

        // Creation projector
        val proj = Projector2(3)
        proj.projectionMethod = PCAProjection2(3)

        // Create upstairs data
        proj.addDataPoint(doubleArrayOf(-1.0, -1.0, 1.0))
        proj.addDataPoint(doubleArrayOf(-2.0, -1.0, 2.0))
        proj.addDataPoint(doubleArrayOf(-3.0, -2.0, 1.0))
        proj.project()

        // Reference interpoint distances based on sklearn PCA
        val largestInterpointDist = 2.2360679774997894
        val middleInterpointDist = 1.7320508075688776
        val smallestInterpointDist = 1.4142135623730954

        // Get Simbrain interpoint distances
        val downstairs = proj.dataset.computeDownstairsArray()
        val interpointDistances = DoubleArray(3)
        interpointDistances[0] = SimbrainMath.distance(downstairs[0], downstairs[1])
        interpointDistances[1] = SimbrainMath.distance(downstairs[1], downstairs[2])
        interpointDistances[2] = SimbrainMath.distance(downstairs[0], downstairs[2])
        Arrays.sort(interpointDistances)

        // Compare to sklearn reference distances.
        Assertions.assertEquals(smallestInterpointDist, interpointDistances[0], 0.01)
        Assertions.assertEquals(middleInterpointDist, interpointDistances[1], 0.01)
        Assertions.assertEquals(largestInterpointDist, interpointDistances[2], 0.01)
    }
}