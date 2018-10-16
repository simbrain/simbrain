package org.simbrain.util.math.DecayFunctions;

import org.simbrain.util.math.DecayFunction;
import org.simbrain.util.propertyeditor2.EditableObject;

public class LinearDecayFunction extends DecayFunction {

    @Override
    public double getScalingFactor(double distance) {
        double scalingFactor = 0;
        if (distance > getDispersion()) {
            return scalingFactor;
        } else {
            double stimulusDispersion = getDispersion();
            double peak = getPeakDistance();
            if (distance < peak) {
                scalingFactor = (stimulusDispersion - (2 * peak) + distance) / (stimulusDispersion - peak);

                if (scalingFactor < 0) {
                    scalingFactor = 0;
                }
            } else {
                scalingFactor = (stimulusDispersion - distance) / (stimulusDispersion - peak);
            }
            return scalingFactor;
        }
    }

    public static LinearDecayFunction create() {
        return new LinearDecayFunction();
    }

    public static LinearDecayFunctionBuilder builder() {
        return new LinearDecayFunctionBuilder();
    }

    @Override
    public EditableObject copy() {
        LinearDecayFunction ret = new LinearDecayFunction();
        ret.setDispersion(this.getDispersion());
        ret.setPeakDistance(this.getPeakDistance());
        ret.setRandomizer(this.getRandomizer().deepCopy());
        return ret;
    }

    public static class LinearDecayFunctionBuilder
            extends DecayFunctionBuilder<
            LinearDecayFunctionBuilder,
            LinearDecayFunction
            > {

        private LinearDecayFunction product = new LinearDecayFunction();

        @Override
        protected LinearDecayFunction product() {
            return product;
        }

        @Override
        public LinearDecayFunction build() {
            return product;
        }
    }
}
