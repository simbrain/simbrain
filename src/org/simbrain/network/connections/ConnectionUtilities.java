/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.connections;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A set of static utility functions/interfaces/etc for manipulating synapses.
 * Usually, for manipulating loose synapses since most changes to Synapses in a
 * synapse group should be done through the synapse group, but there are
 * counter-examples.
 * <p>
 * TODO: Make synapse group use more of these functions.
 *
 * @author Zoë Tosi
 */
public class ConnectionUtilities {

    /**
     * The default excitatory strength.
     */
    public static final double DEFAULT_EXCITATORY_STRENGTH = 1;

    /**
     * The default inhibitory strength.
     */
    public static final double DEFAULT_INHIBITORY_STRENGTH = -1;

    /**
     * Randomizes a collection of synapses based on excitatory and inhibitory
     * (polarized appropriately) randomizers, which cannot be the same
     * randomizer. This method will always attempt to maintain the ratio of
     * excitatory synapses specified. However if some of the source neurons are
     * themselves polarized, this may not always be possible. In such a case,
     * this method will get as close as possible to the desired ratio. This,
     * however is not recommended.
     * <p>
     * If the source neurons to these synapses are themselves, by and large,
     * polarized, this method can be, but should <b>NOT</b> be used.
     * <p>
     * Null values for either PolarizedRandomizer is permitted. Synapses are
     * assigned default strengths based on their polarity depending on which
     * randomizers are null.
     *
     * @param exciteRand      the randomizer to be used to determine the weights
     *                        of excitatory synapses.
     * @param inhibRand       the randomizer to be used to determine the weights
     *                        of inhibitory synapses.
     * @param excitatoryRatio the ration of excitatory to inhibitory synapses.
     * @param synapses        the synapses to modify
     * @throws IllegalArgumentException
     */
    public static void randomizeAndPolarizeSynapses(Collection<Synapse> synapses, ProbabilityDistribution exciteRand, ProbabilityDistribution inhibRand, double excitatoryRatio) throws IllegalArgumentException {
        if (exciteRand.equals(inhibRand)) {
            throw new IllegalArgumentException("Randomization has failed." + " The excitatory and inhibitory randomizers cannot be" + " the same object.");
        } else if (excitatoryRatio > 1 || excitatoryRatio < 0) {
            throw new IllegalArgumentException("Randomization had failed." + " The ratio of excitatory synapses " + " cannot be greater than 1 or less than 0.");
        } else {
            checkPolarityMatches(exciteRand, Polarity.EXCITATORY);
            checkPolarityMatches(inhibRand, Polarity.INHIBITORY);
            int exciteCount = (int) (excitatoryRatio * synapses.size());
            int inhibCount = synapses.size() - exciteCount;
            int remaining = synapses.size();
            boolean excitatory = false;
            for (Synapse s : synapses) {
                excitatory = shouldBeExcitatory(excitatoryRatio, exciteCount, inhibCount, s);
                // Set the strength based on the polarity.
                if (excitatory) {
                    s.setStrength(exciteRand != null ? exciteRand.getRandom() : DEFAULT_EXCITATORY_STRENGTH);
                    exciteCount--;
                    // Change the excitatoryRatio to maintain balance
                    excitatoryRatio = exciteCount / (double) remaining;
                } else {
                    s.setStrength(inhibRand != null ? inhibRand.getRandom() : DEFAULT_INHIBITORY_STRENGTH);
                    inhibCount--;
                    // Change the excitatoryRatio to maintain balance.
                    excitatoryRatio = (remaining - inhibCount) / (double) remaining;
                }
                remaining--;
            }
        }
    }

    /**
     * Randomize and polarize synapses using default excitatory and inhibitory
     * polarizers (uniform 0 to 1).
     *
     * @param synapses        the synapses to modify
     * @param excitatoryRatio the ration of excitatory to inhibitory synapses.
     */
    public static void randomizeAndPolarizeSynapses(Collection<Synapse> synapses, double excitatoryRatio) {

        ProbabilityDistribution exciteRand =
            UniformDistribution.builder()
                .polarity(Polarity.EXCITATORY)
                .build();

        ProbabilityDistribution inhibRand =
            UniformDistribution.builder()
                .polarity(Polarity.INHIBITORY)
                .build();

        randomizeAndPolarizeSynapses(synapses, exciteRand, inhibRand, excitatoryRatio);
    }

    /**
     * @param synapses   the synapses to modify
     * @param exciteRand the randomizer to be used to determine the weights of
     *                   excitatory synapses.
     * @param inhibRand  the randomizer to be used to determine the weights of
     *                   inhibitory synapses.
     */
    public static void randomizeSynapses(Collection<Synapse> synapses, ProbabilityDistribution exciteRand, ProbabilityDistribution inhibRand) {
        if (exciteRand.equals(inhibRand)) {
            throw new IllegalArgumentException("Randomization has failed." + " The excitatory and inhibitory randomizers cannot be" + " the same object.");
        } else {
            checkPolarityMatches(exciteRand, Polarity.EXCITATORY);
            checkPolarityMatches(inhibRand, Polarity.INHIBITORY);
            boolean excitatory = false;
            for (Synapse s : synapses) {
                excitatory = s.getStrength() > 0;
                // Set the strength based on the polarity.
                if (excitatory) {
                    s.setStrength(exciteRand != null ? exciteRand.getRandom() : DEFAULT_EXCITATORY_STRENGTH);
                } else {
                    s.setStrength(inhibRand != null ? inhibRand.getRandom() : DEFAULT_INHIBITORY_STRENGTH);
                }
            }
        }
    }

    /**
     * Randomizes the excitatory synapses in the given list of synapses using
     * the given excitatory randomizer.
     *
     * @param synapses   the synapses to modify
     * @param exciteRand the randomizer to be used to determine the weights of
     *                   excitatory synapses.
     */
    public static void randomizeExcitatorySynapses(Collection<Synapse> synapses, ProbabilityDistribution exciteRand) {
        checkPolarityMatches(exciteRand, Polarity.EXCITATORY);
        for (Synapse s : synapses) {
            if (Polarity.EXCITATORY.equals(s.getSource().getPolarity()) || s.getStrength() > 0) {
                s.setStrength(exciteRand != null ? exciteRand.getRandom() : DEFAULT_EXCITATORY_STRENGTH);
            }
        }
    }

    /**
     * Randomizes the given synapses using the given excitatory randomizer
     * without checking first to make sure that the given synapses or their
     * source neurons are not inhibitory. Used for speed when the polarity of
     * the synapses in the list is known ahead of time.
     *
     * @param synapses   the synapses to modify
     * @param exciteRand the randomizer to be used to determine the weights of
     *                   excitatory synapses.
     */
    public static void randomizeExcitatorySynapsesUnsafe(Collection<Synapse> synapses, ProbabilityDistribution exciteRand) {
        checkPolarityMatches(exciteRand, Polarity.EXCITATORY);
        for (Synapse s : synapses) {
            s.setStrength(exciteRand != null ? exciteRand.getRandom() : DEFAULT_EXCITATORY_STRENGTH);
        }
    }

    /**
     * Randomizes the inhibitory synapses in the given list of synapses using
     * the given inhibitory randomizer.
     *
     * @param synapses  the synapses to modify
     * @param inhibRand the randomizer to be used to determine the weights of
     *                  inhibitory synapses.
     */
    public static void randomizeInhibitorySynapses(Collection<Synapse> synapses, ProbabilityDistribution inhibRand) {
        checkPolarityMatches(inhibRand, Polarity.INHIBITORY);
        for (Synapse s : synapses) {
            if (Polarity.INHIBITORY.equals(s.getSource().getPolarity()) || s.getStrength() < 0) {
                s.setStrength(inhibRand != null ? inhibRand.getRandom() : DEFAULT_INHIBITORY_STRENGTH);
            }
        }
    }

    /**
     * Randomizes the given synapses using the given inhibitory randomizer
     * without checking first to make sure that the given synapses or their
     * source neurons are not excitatory. Used for speed when the polarity of
     * the synapses in the list is known ahead of time.
     *
     * @param synapses  the synapses to modify
     * @param inhibRand the randomizer to be used to determine the weights of
     *                  inhibitory synapses.
     */
    public static void randomizeInhibitorySynapsesUnsafe(Collection<Synapse> synapses, ProbabilityDistribution inhibRand) {
        checkPolarityMatches(inhibRand, Polarity.INHIBITORY);
        for (Synapse s : synapses) {
            s.setStrength(inhibRand != null ? inhibRand.getRandom() : DEFAULT_INHIBITORY_STRENGTH);
        }
    }

    /**
     * @param synapses the synapses to modify
     * @return excitatory synapses
     */
    public static ArrayList<Synapse> getExcitatorySynapses(Collection<Synapse> synapses) {
        ArrayList<Synapse> exSyns = new ArrayList<Synapse>(synapses.size() / 2);
        for (Synapse s : synapses) {
            if (s.getStrength() > 0 || Polarity.EXCITATORY.equals(s.getSource().getPolarity())) {
                exSyns.add(s);
            }
        }
        return exSyns;
    }

    /**
     * @param synapses the synapses to modify
     * @return inhibitory synapses
     */
    public static ArrayList<Synapse> getInhibitorySynapses(Collection<Synapse> synapses) {
        ArrayList<Synapse> inSyns = new ArrayList<Synapse>(synapses.size() / 2);
        for (Synapse s : synapses) {
            if (s.getStrength() < 0 || Polarity.INHIBITORY.equals(s.getSource().getPolarity())) {
                inSyns.add(s);
            }
        }
        return inSyns;
    }

    /**
     * Changes all the synapses in a given collection such that
     * <b>excitatoryRatio</b> of them are excitatory and <b>1 -
     * excitatoryRatio</b> of them are inhibitory, assigning default strengths
     * respectively to each.
     * <p>
     * This method will attempt to maintain the requested excitatoryRatio even
     * if some or all of the source neurons are themselves polarized. In such
     * cases the polarity of the Neurons efferent synapses will not be
     * overridden. Though it may not be possible to obtain the desired
     * excitatoryRatio in this case, this method will get as close as possible.
     *
     * @param synapses        the synapses to polarize
     * @param excitatoryRatio the ration of excitatory synapses (1 for all
     *                        exctitatory)
     */
    public static void polarizeSynapses(Collection<Synapse> synapses, double excitatoryRatio) {
        if (excitatoryRatio > 1 || excitatoryRatio < 0) {
            throw new IllegalArgumentException("Randomization had failed." + " The ratio of excitatory synapses " + " cannot be greater than 1 or less than 0.");
        } else {
            int exciteCount = (int) (excitatoryRatio * synapses.size());
            int inhibCount = synapses.size() - exciteCount;
            int remaining = synapses.size();
            boolean excitatory = false;
            for (Synapse s : synapses) {
                excitatory = shouldBeExcitatory(excitatoryRatio, exciteCount, inhibCount, s);
                // Set the strength based on the polarity.
                if (excitatory) {
                    s.setStrength(DEFAULT_EXCITATORY_STRENGTH);
                    exciteCount--;
                    // Change the excitatoryRatio to maintain balance
                    excitatoryRatio = exciteCount / (double) remaining;
                } else {
                    s.setStrength(DEFAULT_INHIBITORY_STRENGTH);
                    inhibCount--;
                    // Change the excitatoryRatio to maintain balance.
                    excitatoryRatio = (remaining - inhibCount) / (double) remaining;
                }
                remaining--;
            }
        }
    }

    /**
     * Makes the synapses in the given collection conform to the parameters of
     * the given template synapses, which are essentially information ferries.
     * Throws an exception if the template synapses do not match the appropriate
     * polarities implied by their names.
     *
     * @param synapses          the synpases to modify
     * @param exTemplateSynapse temporary set containing all excitatory
     *                          synapses
     * @param inTemplateSynapse temporary set containing all inhibitory
     *                          synapses
     */
    public static void conformToTemplates(Collection<Synapse> synapses, Synapse exTemplateSynapse, Synapse inTemplateSynapse) {
        if (exTemplateSynapse.getStrength() <= 0) {
            throw new IllegalArgumentException("Excitatory template synapse" + " must be excitatory (having strength > 0).");
        }
        if (inTemplateSynapse.getStrength() >= 0) {
            throw new IllegalArgumentException("Inhibitory template synapse" + " must be inhibitory (having strength < 0).");
        }
        for (Synapse s : synapses) {
            if (s.getStrength() < 0) {
                s.setStrength(inTemplateSynapse.getStrength());
                s.setLearningRule(inTemplateSynapse.getLearningRule().deepCopy());
                s.setUpperBound(inTemplateSynapse.getUpperBound());
                s.setLowerBound(inTemplateSynapse.getLowerBound());
                s.setDelay(inTemplateSynapse.getDelay());
                s.setEnabled(inTemplateSynapse.isEnabled());
                s.setFrozen(inTemplateSynapse.isFrozen());
                s.setIncrement(inTemplateSynapse.getIncrement());
                if (inTemplateSynapse.getSpikeResponder() != null) {
                    s.setSpikeResponder(inTemplateSynapse.getSpikeResponder().deepCopy());
                }
            }
            if (s.getStrength() > 0) {
                s.setStrength(exTemplateSynapse.getStrength());
                s.setLearningRule(exTemplateSynapse.getLearningRule().deepCopy());
                s.setUpperBound(exTemplateSynapse.getUpperBound());
                s.setLowerBound(exTemplateSynapse.getLowerBound());
                s.setDelay(exTemplateSynapse.getDelay());
                s.setEnabled(exTemplateSynapse.isEnabled());
                s.setFrozen(exTemplateSynapse.isFrozen());
                s.setIncrement(exTemplateSynapse.getIncrement());
                if (exTemplateSynapse.getSpikeResponder() != null) {
                    s.setSpikeResponder(exTemplateSynapse.getSpikeResponder().deepCopy());
                }
            }
        }
    }

    /**
     * @param inQuestion
     * @param expectedPolarity
     * @throws IllegalArgumentException
     */
    private static void checkPolarityMatches(ProbabilityDistribution inQuestion, Polarity expectedPolarity) throws IllegalArgumentException {
        // TODO: Use "optional" instead when upgrade to Java 8
        if (inQuestion == null) {
            return;
        }

        // TODO: I'm commenting this out for now just to test code, but
        //     it's being thrown a lot and testing is needed.
        if (!expectedPolarity.equals(inQuestion.getPolarity())) {
            throw new IllegalArgumentException("Randomizer's polarity does" + " not match its implied polarity");
        }
    }

    /**
     * @param excitatoryRatio the ration of excitatory to inhibitory synapses.
     * @param exciteCount
     * @param inhibCount
     * @param s
     * @return
     */
    private static boolean shouldBeExcitatory(double excitatoryRatio, int exciteCount, int inhibCount, Synapse s) {
        boolean excitatory = false;
        if (s.getSource().isPolarized()) {
            if (Polarity.EXCITATORY == s.getSource().getPolarity()) {
                excitatory = true;
            } else {
                excitatory = false;
            }
        } else {
            if (exciteCount <= 0 || inhibCount <= 0) {
                if (exciteCount <= 0) {
                    excitatory = false;
                }
                if (inhibCount <= 0) {
                    excitatory = true;
                }
            } else {
                double exciteOrInhib = Math.random();
                if (exciteOrInhib < excitatoryRatio) {
                    excitatory = true;
                } else {
                    excitatory = false;
                }
            }
        }
        return excitatory;
    }

    /**
     * Tests whether or not these connections are recurrent, that is, whether or
     * not the neurons in the source list are the same as those in the target
     * list.
     *
     * @param sourceNeurons the starting neurons
     * @param targetNeurons the targeted neurons
     * @return true or false: whether or not these connections are recurrent.
     */
    public static boolean testRecurrence(List<Neuron> sourceNeurons, List<Neuron> targetNeurons) {
        if (sourceNeurons.size() != targetNeurons.size()) {
            return false;
        } else {
            for (int i = 0; i < sourceNeurons.size(); i++) {
                if (sourceNeurons.get(i) != targetNeurons.get(i)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Automatically applies separate learning rules for excitatory
     * and inhibitory synapses.
     *
     * @param exciteRule excitatory rule to appy
     * @param inhibRule inhibitory rule to apply
     * @param synapses list of synapses to apply rule to
     */
    public static void applyLearningRules(SynapseUpdateRule exciteRule, SynapseUpdateRule inhibRule, List<Synapse> synapses) {
        for (Synapse s : synapses) {
            if (s.getStrength() < 0) {
                s.setLearningRule(inhibRule);
            } else {
                s.setLearningRule(exciteRule);
            }
        }
    }

    /**
     * Like {@link #getSeparatedInput(Neuron)} but excitatory
     * and inhibitory inputs are separated.
     *
     * @param n neuron to get normalized separated inputs from
     * @return the separated inputs
     */
    public static double[] getAverageSeparatedInput(Neuron n) {
        double[] ei = new double[2];
        double e = 0;
        double i = 0;
        for (Synapse s : n.getFanIn()) {
            double psr = s.calcPSR();
            if (psr > 0) {
                ei[0] += psr;
                e++;
            } else {
                ei[1] += psr;
                i++;
            }
        }
        if (e > 1) {
            ei[0] /= e;
        }
        if (i > 1) {
            ei[1] /= i;
        }
        return ei;
    }

    /**
     * Returns summed excitatory and inhibitory inputs.
     * Used for conductance-based models.
     *
     * @param n neuron to get separated inputs from
     * @return the separated inputs
     */
    public static double[] getSeparatedInput(Neuron n) {
        //TODO: Make this an argument so the array is not re-created
        // for each neuron
        double[] ei = new double[2];
        for (Synapse s : n.getFanIn()) {
            double psr = s.calcPSR();
            if (psr > 0) {
                ei[0] += psr;
            } else {
                ei[1] += psr;
            }
        }
        return ei;
    }
}
