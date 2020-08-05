package org.simbrain.util.math.DecayFunctions;

import org.simbrain.util.math.DecayFunction;

public class QuadraticDecayFunction extends DecayFunction {

    @Override
    public double getScalingFactor(double distance) {
        if (distance > getDispersion()) {
            return 0;
        } else {
            double peak = getPeakDistance();
            double stimulusDispersion = getDispersion();
            double scalingFactor = 1 - Math.pow((distance - peak) / (stimulusDispersion - peak), 2);
            if (scalingFactor < 0) {
                scalingFactor = 0;
            }
            return scalingFactor;
        }
    }

    public static QuadraticDecayFunction create() {
        return new QuadraticDecayFunction();
    }

    public static QuadraticDecayFunctionBuilder builder() {
        return new QuadraticDecayFunctionBuilder();
    }

    @Override
    public QuadraticDecayFunction copy() {
        QuadraticDecayFunction ret = new QuadraticDecayFunction();
        ret.setDispersion(this.getDispersion());
        ret.setPeakDistance(this.getPeakDistance());
        ret.setRandomizer(this.getRandomizer().deepCopy());
        return ret;
    }

    @Override
    public String getName() {
        return "Quadratic";
    }

    public static class QuadraticDecayFunctionBuilder
            extends DecayFunctionBuilder<
            QuadraticDecayFunctionBuilder,
            QuadraticDecayFunction
            > {

        private QuadraticDecayFunction product = new QuadraticDecayFunction();

        @Override
        protected QuadraticDecayFunction product() {
            return product;
        }

        @Override
        public QuadraticDecayFunction build() {
            return product;
        }
    }
}
