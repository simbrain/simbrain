package org.simbrain.util.projection;

import org.junit.jupiter.api.Test;
import org.simbrain.util.math.SimbrainMath;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectPCATest {

    @Test
    public void checkPCAProjection() {

        // Creation projector
        Projector proj = new Projector(3);
        proj.setUseColorManager(false);
        proj.setProjectionMethod("PCA");

        // Create upstairs data
        proj.addDatapoint(new DataPoint(new double[]{-1, -1, 1}));
        proj.addDatapoint(new DataPoint(new double[]{-2, -1, 2}));
        proj.addDatapoint(new DataPoint(new double[]{-3, -2, 1}));

        // Reference interpoint distances based on sklearn PCA
        double largestInterpointDist = 2.2360679774997894;
        double middleInterpointDist = 1.7320508075688776;
        double smallestInterpointDist = 1.4142135623730954;

        // Get Simbrain interpoint distances
        double[][] downstairs = proj.getDownstairs().getDoubleArray();
        double[] interpointDistances = new double[3];
        interpointDistances[0] = SimbrainMath.distance(downstairs[0], downstairs[1]);
        interpointDistances[1] = SimbrainMath.distance(downstairs[1], downstairs[2]);
        interpointDistances[2] = SimbrainMath.distance(downstairs[0], downstairs[2]);
        Arrays.sort(interpointDistances);

        // Compare to sklearn reference distances.
        assertEquals(smallestInterpointDist, interpointDistances[0], 0.01);
        assertEquals(middleInterpointDist, interpointDistances[1], 0.01);
        assertEquals(largestInterpointDist, interpointDistances[2], 0.01);

    }

}