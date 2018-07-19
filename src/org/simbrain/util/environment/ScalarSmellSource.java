package org.simbrain.util.environment;

/**
 * A smell source that is scalar valued. Backed by an array-based {@link SmellSource} object.
 */
public class ScalarSmellSource {

    //TODO: Rename to ScalarStimulusSource? or just StimulusSource?
    //TODO Appropriate forwards to smell source to support property editor

    /**
     * Array-based smell source that backs this object.
     */
    private SmellSource smellSource;

    public ScalarSmellSource(final double maxValue) {
        this.smellSource = new SmellSource(new double[]{maxValue});
    }

    /**
     * Returns the value of this smell source based on how far away it is.
     *
     * @param distance how far away a smell sensor is from this source
     * @return the value, scaled by that distance.
     */
    public double getValue(double distance) {
        return smellSource.getStimulus(distance)[0];
    }

    /**
     * Update the smell source.
     */
    public void update() {
        smellSource.update();
    }


}
