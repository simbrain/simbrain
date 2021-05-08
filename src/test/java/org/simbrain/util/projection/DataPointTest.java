package org.simbrain.util.projection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataPointTest {

    @Test
    public void testDataPointInitialization() {

        double[] testData = {0.0, 1.2, 3.1};
        DataPoint point = new DataPoint(testData);

        // Make sure the point is initialized properly
        assertTrue(point.getDimension() == 3);
        assertTrue(point.getVector()[1] == 1.2);

        // Make sure changes to the input array don't change
        // the datapoint
        testData[1] = -1.2;
        assertTrue(point.getVector()[1] == 1.2);
    }
}