package org.simbrain.custom_sims.simulations.hippocampus;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.subnetworks.CompetitiveGroup;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

/**
 * Extends competitive group with functions specific to the hippocampus
 * simulation.
 */
public class AlvarezSquire extends CompetitiveGroup {

    /**
     * Noise generator.
     */
    private ProbabilityDistribution noiseGenerator = UniformDistribution.create();

    /**
     * Reference to parent simulation.
     */
    private Hippocampus hippo;

    /**
     * Construct the group.
     *
     * @param hippo      reference to parent sim
     * @param numNeurons number neurons
     */
    public AlvarezSquire(Hippocampus hippo, int numNeurons) {
        super(hippo.network, numNeurons);
        this.hippo = hippo;

        noiseGenerator =
            UniformDistribution.builder()
                .lowerBound(-0.05)
                .upperBound(0.05)
                .build();
    }

    @Override
    public void update() {
        super.update();

        // For this simulation we can assume that if one neuron is clamped, they
        // all are
        boolean clamped = getNeuronList().get(0).isClamped();
        Neuron winner = WinnerTakeAll.getWinner(getNeuronList(), clamped);

        // Update weights on winning neuron
        for (int i = 0; i < getNeuronList().size(); i++) {
            Neuron neuron = getNeuronList().get(i);
            if (neuron == winner) {
                // TODO: Allow user to choose update function
                //neuron.setActivation(this.getWinValue());
                alvarezSquireUpdate(neuron);
                updateWeights(neuron);
            } else {
                neuron.setActivation(this.getLoseValue());
            }
        }
        decaySynapses();
    }

    /**
     * Simple decay with random noise.
     */
    private void alvarezSquireUpdate(Neuron neuron) {
        // TODO: Use library for clipping
        double val = .7 * neuron.getActivation() + neuron.getWeightedInputs() + noiseGenerator.getRandom();
        neuron.forceSetActivation((val > 0) ? val : 0);
        neuron.forceSetActivation((val < 1) ? val : 1);
    }

    /**
     * Decay attached synapses in accordance with Alvarez and Squire 1994.
     */
    private void decaySynapses() {
        double rho;
        for (Neuron n : getNeuronList()) {
            for (Synapse synapse : n.getFanIn()) {
                if (synapse.getSource().getParentGroup() == hippo.hippocampus) {
                    rho = .04;
                } else if (synapse.getTarget().getParentGroup() == hippo.hippocampus) {
                    rho = .04;
                } else {
                    rho = .0008;
                }
                synapse.decay(rho);
            }
        }

    }

    /**
     * Custom weight update.
     *
     * @param neuron winning neuron whose incoming synapses will be updated
     */
    private void updateWeights(final Neuron neuron) {
        double lambda;

        for (Synapse synapse : neuron.getFanIn()) {
            if (synapse.getSource().getParentGroup() == hippo.hippocampus) {
                lambda = .1;
            } else if (synapse.getTarget().getParentGroup() == hippo.hippocampus) {
                lambda = .1;
            } else {
                lambda = .002;
            }
            double deltaw = lambda * synapse.getTarget().getActivation() * (synapse.getSource().getActivation() - synapse.getTarget().getAverageInput());
            synapse.setStrength(synapse.clip(synapse.getStrength() + deltaw));

        }
    }

}
