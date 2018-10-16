package org.simbrain.util.math.DecayFunctions;

import org.simbrain.util.math.DecayFunction;
import org.simbrain.util.propertyeditor2.EditableObject;

public class StepDecayFunction extends DecayFunction {

    @Override
    public double getScalingFactor(double distance) {
        if (distance > getDispersion()) {
            return 0;
        } else {
            return distance >= getPeakDistance() ? 1 : 0;
        }
    }

    public static StepDecayFunction create() {
        return new StepDecayFunction();
    }

    public static StepDecayFunctionBuilder builder() {
        return new StepDecayFunctionBuilder();
    }

    @Override
    public EditableObject copy() {
        StepDecayFunction ret = new StepDecayFunction();
        ret.setDispersion(this.getDispersion());
        ret.setPeakDistance(this.getPeakDistance());
        ret.setRandomizer(this.getRandomizer().deepCopy());
        return ret;
    }

    @Override
    public String getName() {
        return "Step";
    }

    public static class StepDecayFunctionBuilder
        extends DecayFunctionBuilder<
                    StepDecayFunctionBuilder,
                    StepDecayFunction
            > {

        private StepDecayFunction product = new StepDecayFunction();

        @Override
        protected StepDecayFunction product() {
            return product;
        }

        @Override
        public StepDecayFunction build() {
            return product;
        }
    }
}
