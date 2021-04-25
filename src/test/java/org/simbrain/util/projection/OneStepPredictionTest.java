package org.simbrain.util.projection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OneStepPredictionTest {

    @Test
    public void basicTest() {

        // Set up with a single source and target
        OneStepPrediction prediction = new OneStepPrediction();
        DataPointColored src = new DataPointColored(new double[]{1,0});
        DataPointColored tar = new DataPointColored(new double[]{1,1});
        prediction.addSourceTargetPair(src, tar);

        // Should have one source-target mapping now, and the target should have one entry
        assertEquals(1, prediction.getNumPairs());
        assertEquals(1, prediction.getNumTargetsForSource(src));

        // Add a duplicate target point
        DataPointColored tarDup = new DataPointColored(new double[]{1,1});
        prediction.addSourceTargetPair(src, tarDup);

        // Should still have one source-target pair and a target with one entry
        assertEquals(1, prediction.getNumPairs());
        assertEquals(1, prediction.getNumTargetsForSource(src));

        // Add a second target point with the same source
        DataPointColored tar2 = new DataPointColored(new double[]{0,1});
        prediction.addSourceTargetPair(src, tar2);

        // The target map should now have two entries
        assertEquals(2, prediction.getNumTargetsForSource(src));


        // TODO: Not sure what to do about this situation..
        //DataPointColored wrongSizeSrc = new DataPointColored(new double[]{0,1,1});
        //prediction.addPoint(wrongSizeSrc, tar);

    }

    @Test
    public void testProbabilities() {

        OneStepPrediction prediction = new OneStepPrediction();
        DataPointColored src = new DataPointColored(new double[]{1,0});
        DataPointColored tar = new DataPointColored(new double[]{1,1});
        DataPointColored tar2 = new DataPointColored(new double[]{0,1});
        prediction.addSourceTargetPair(src, tar);
        prediction.addSourceTargetPair(src, tar2);

        // Initially src->tar, src->tar2, so 1/2 chance for each
        assertEquals(.5, tar.getProbability(src), 0);
        assertEquals(.5, tar2.getProbability(src), 0);

        // Activate src-> tar two more times
        // Probabilities increase to 3/4 for first pair, 1/4 for the second
        prediction.addSourceTargetPair(src, tar);
        prediction.addSourceTargetPair(src, tar);
        assertEquals(.75, tar.getProbability(src), 0);
        assertEquals(.25, tar2.getProbability(src), 0);
    }
}
