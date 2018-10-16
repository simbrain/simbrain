package org.simbrain.util.math.DecayFunctions;

import org.simbrain.util.math.DecayFunction;
import org.simbrain.util.propertyeditor2.EditableObject;

public class GaussianDecayFunction extends DecayFunction {

    @Override
    public double getScalingFactor(double distance) {
        if (distance > getDispersion()) {
            return 0;
        } else {
            double temp = distance;
            temp -= getPeakDistance();
            double sigma = .5 * (getDispersion() - getPeakDistance());
            double scalingFactor = Math.exp(-(temp * temp) / (2 * sigma * sigma));
            return scalingFactor;
        }
    }

    public static GaussianDecayFunction create() {
        return new GaussianDecayFunction();
    }

    public static GaussianDecayFunctionBuilder builder() {
        return new GaussianDecayFunctionBuilder();
    }

    @Override
    public EditableObject copy() {
        GaussianDecayFunction ret = new GaussianDecayFunction();
        ret.setDispersion(this.getDispersion());
        ret.setPeakDistance(this.getPeakDistance());
        ret.setRandomizer(this.getRandomizer().deepCopy());
        return ret;
    }

    public static class GaussianDecayFunctionBuilder
            extends DecayFunctionBuilder<
            GaussianDecayFunctionBuilder,
            GaussianDecayFunction
            > {

        private GaussianDecayFunction product = new GaussianDecayFunction();

        @Override
        protected GaussianDecayFunction product() {
            return product;
        }

        @Override
        public GaussianDecayFunction build() {
            return product;
        }
    }
}
