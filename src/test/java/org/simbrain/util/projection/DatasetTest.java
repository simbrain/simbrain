package org.simbrain.util.projection;

import org.junit.Before;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;

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

    @Test
    public void testND4JArray() {
        data = new Dataset(3);
        data.addPoint(new DataPoint(new double[]{1,0,1}));
        data.addPoint(new DataPoint(new double[]{1,1,1}));
        data.addPoint(new DataPoint(new double[]{0,0,1}));
        data.addPoint(new DataPoint(new double[]{-1,0,1}));
        INDArray arr = data.getArray();
        assertEquals(3, arr.columns());
        assertEquals(4, arr.rows());
    }

    // TODO: Test isUnique
}