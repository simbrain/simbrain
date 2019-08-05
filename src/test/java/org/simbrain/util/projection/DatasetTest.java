package org.simbrain.util.projection;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DatasetTest {

    Dataset data;

    @Before
    public void setUp() throws Exception {
        data = new Dataset(2);
        data.addPoint(new DataPoint(new double[]{1,0}));
    }

    @Test
    public void toleranceTest() {

        // Should be added. Within tolerance
        data.addPoint(new DataPoint(new double[]{2.1,0}), 1);

        assertEquals(2, data.getNumPoints());
        // Should not be added. Too close to previous.
        data.addPoint(new DataPoint(new double[]{1.5,0}), 1);
        assertEquals(2, data.getNumPoints());

    }

    @Test
    public void testLastPoint() {
        assertEquals(1.0, data.getLastAddedPoint().get(0),0 );
        assertEquals(0.0, data.getLastAddedPoint().get(1),0 );
    }

    // TODO: Test isUnique
}