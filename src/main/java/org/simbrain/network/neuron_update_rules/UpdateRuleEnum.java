package org.simbrain.network.neuron_update_rules;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.activity_generators.LogisticRule;
import org.simbrain.network.neuron_update_rules.activity_generators.RandomNeuronRule;
import org.simbrain.network.neuron_update_rules.activity_generators.SinusoidalRule;
import org.simbrain.network.neuron_update_rules.activity_generators.StochasticRule;
import org.simbrain.network.updaterules.*;

/**
 * Enum of {@link NeuronUpdateRule}'s that is used for several GUI purposes.
 */
public enum UpdateRuleEnum {

    LOGISTIC(LogisticRule.class),
    STOCHASTIC(StochasticRule.class),
    SINUSOID(SinusoidalRule.class),
    RANDOMNEURON(RandomNeuronRule.class),
    ADEX(AdExIFRule.class),
    BINARY(BinaryRule.class),
    DECAY(DecayRule.class),
    FITZHUGHNAGUMO(FitzhughNagumo.class),
    IAC(IACRule.class),
    INTEGRATEANDFIRE(IntegrateAndFireRule.class),
    IZHIKEVICH(IzhikevichRule.class),
    KURAMOTO(KuramotoRule.class),
    LINEAR(LinearRule.class),
    MORRISLECAR(MorrisLecarRule.class),
    NAKARUSHTON(NakaRushtonRule.class),
    PRODUCT(ProductRule.class),
    CONTINUOUSSIGMOIDAL(ContinuousSigmoidalRule.class),
    SIGMOIDAL(SigmoidalRule.class),
    SPIKINGTHRESHOLD(SpikingThresholdRule.class),
    THREEVALUE(ThreeValueRule.class),
    TIMEDACCUMULATOR(TimedAccumulatorRule.class);

    /**
     * A specific {@link NeuronUpdateRule}.
     */
    private Class<? extends NeuronUpdateRule> rule;

    /**
     * Construct a member of the enum
     */
    UpdateRuleEnum(Class<? extends NeuronUpdateRule> rule) {
        this.rule = rule;
    }

    /**
     * Return the rule associate with an enum value.
     */
    public Class<? extends NeuronUpdateRule> getRule() {
        return rule;
    }

    /**
     * Returns the enum member based on a provided {@link NeuronUpdateRule}
     */
    public static UpdateRuleEnum get(NeuronUpdateRule toCheck) {
        for (int i = 0; i < UpdateRuleEnum.values().length ; i++) {
           if (UpdateRuleEnum.values()[i].getRule() == toCheck.getClass()) {
               return UpdateRuleEnum.values()[i];
           }
        }
        return null;
    }

    @Override
    public String toString() {
        return rule.getSimpleName();
    }

}
