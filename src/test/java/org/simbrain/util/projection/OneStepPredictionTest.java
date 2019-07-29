package org.simbrain.util.projection;

import org.junit.Test;

import static org.junit.Assert.*;

public class OneStepPredictionTest {

    @Test
    public void basicTest() {


        // Set up with a single source and target
        OneStepPrediction prediction = new OneStepPrediction();
        DataPoint src = new DataPoint(new double[]{1,0});
        DataPoint tar = new DataPoint(new double[]{1,1});
        prediction.addSourceTargetPair(src, tar);

        // Should have one source-target mapping now, and the target should have one entry
        assertEquals(1, prediction.getNumPairs());
        assertEquals(1, prediction.getNumTargetsForSource(src));

        // Add a duplicate target point
        DataPoint tarDup = new DataPoint(new double[]{1,1});
        prediction.addSourceTargetPair(src, tarDup);

        // Should still have one source-target pair and a target with one entry
        assertEquals(1, prediction.getNumPairs());
        assertEquals(1, prediction.getNumTargetsForSource(src));

        // Add a second target point with the same source
        DataPoint tar2 = new DataPoint(new double[]{0,1});
        prediction.addSourceTargetPair(src, tar2);

        // The target map should now have two entries
        assertEquals(2, prediction.getNumTargetsForSource(src));


        // TODO: Not sure what to do about this situation..
        //DataPoint wrongSizeSrc = new DataPoint(new double[]{0,1,1});
        //prediction.addPoint(wrongSizeSrc, tar);


    }

    @Test
    public void testProbabilities() {

        OneStepPrediction prediction = new OneStepPrediction();
        DataPoint src = new DataPoint(new double[]{1,0});
        DataPoint tar = new DataPoint(new double[]{1,1});
        DataPoint tar2 = new DataPoint(new double[]{0,1});
        prediction.addSourceTargetPair(src, tar);
        prediction.addSourceTargetPair(src, tar2);

        // Initially 1/2 chance for each
        assertEquals(.5, prediction.getProbability(src, tar), .01);
        assertEquals(.5, prediction.getProbability(src, tar2), .01);

        // Probabilities increase to 3/4 for first pair, 1/4 for the second
        prediction.addSourceTargetPair(src, tar);
        prediction.addSourceTargetPair(src, tar);
        assertEquals(.75, prediction.getProbability(src, tar), .01);
        assertEquals(.25, prediction.getProbability(src, tar2), .01);

    }
}
