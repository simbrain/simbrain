package org.simbrain.util.projection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataPointTest {

    @Test
    public void testDataPointInitialization() {

        double[] testData = {0.0, 1.2, 3.1};
        DataPoint point = new DataPoint(testData);

        // Make sure the point is initialized properly
        assertEquals(3, point.getUpstairsPoint().length);
        assertEquals(1.2, point.getUpstairsPoint()[1]);

    }
}