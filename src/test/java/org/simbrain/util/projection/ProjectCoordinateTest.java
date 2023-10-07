package org.simbrain.util.projection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectCoordinateTest {

    @Test
    public void testProjection() {
        Projector proj = new Projector(5);

        // Check the first two items in a downstairs projection
        var projectionMethod = new CoordinateProjection();
        proj.setProjectionMethod(projectionMethod);
        projectionMethod.setDim1(0);
        projectionMethod.setDim2(1);
        proj.addDataPoint(new DataPoint(new double[]{1,0,0,0,0}));
        // proj.addDataPoint(new DataPoint(new double[]{0,1,0,0,0}));

        var downstairs = proj.getDataset().computeDownstairsArray();
        assertEquals(1, downstairs[0][0], 0);
        assertEquals(0, downstairs[0][1], 0);
        // assertEquals(0, downstairs[1][0], 0);
        // assertEquals(1, downstairs[1][1], 0);
    }

    // @Test
    // public void testAutofind() {
    //     Projector proj = new Projector(5);
    //     // Dim 2 has most variance, and 4 has second most (0 indexed)
    //     proj.addDatapoint(new DataPointColored(new double[]{0,0,1,0,.5}));
    //     proj.addDatapoint(new DataPointColored(new double[]{0,0,0,0,0}));
    //
    //     // Check the first two items in a downstairs projection
    //     proj.setProjectionMethod("Coordinate Projection");
    //     ((ProjectCoordinate)proj.getProjectionMethod()).setAutoFind(true);
    //     proj.getProjectionMethod().project();
    //     assertEquals(2,((ProjectCoordinate)proj.getProjectionMethod()).getHiD1());
    //     assertEquals(4,((ProjectCoordinate)proj.getProjectionMethod()).getHiD2());
    // }

    //TODO: Test initialization of the projector
}