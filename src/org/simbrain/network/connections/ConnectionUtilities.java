package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.randomizer.PolarizedRandomizer;

public class ConnectionUtilities {

    /** The default excitatory strength. */
    public static final double DEFAULT_EXCITATORY_STRENGTH = 1;

    /** The default inhibitory strength. */
    public static final double DEFAULT_INHIBITORY_STRENGTH = -1;

    /**
     * Randomizes a collection of synapses based on excitatory and inhibitory
     * (polarized appropriately) randomizers, which cannot be the same
     * randomizer. This method will always attempt to maintain the percent of
     * excitatory synapses specified. However if some of the source neurons are
     * themselves polarized, this may not always be possible. In such a case,
     * this method will get as close as possible to the desired percent. This,
     * however is not reccommended.
     * 
     * If the source neurons to these synapses are themselves, by and large,
     * polarized, this method can be, but should <b>NOT</b> be used.
     * 
     * Null values for either PolarizedRandomizer is permitted. Synapses are
     * assigned default strengths based on their polarity depending on which
     * randomizers are null.
     * 
     * @param exciteRand
     * @param inhibRand
     * @param percentExcitatory
     * @param synapses
     * @throws IllegalArgumentException
     */
    public static void randomizeAndPolarizeSynapses(
        Collection<Synapse> synapses, PolarizedRandomizer exciteRand,
        PolarizedRandomizer inhibRand, double percentExcitatory)
        throws IllegalArgumentException {
        if (exciteRand.equals(inhibRand)) {
            throw new IllegalArgumentException("Randomization has failed."
                + " The excitatory and inhibitory randomizers cannot be"
                + " the same object.");
        } else if (percentExcitatory > 1 || percentExcitatory < 0) {
            throw new IllegalArgumentException("Randomization had failed."
                + " The percent of excitatory synapses "
                + " cannot be greater than 1 or less than 0.");
        } else {
            checkPolarityMatches(exciteRand, Polarity.EXCITATORY);
            checkPolarityMatches(inhibRand, Polarity.INHIBITORY);
            int exciteCount = (int) (percentExcitatory * synapses.size());
            int inhibCount = synapses.size() - exciteCount;
            int remaining = synapses.size();
            boolean excitatory = false;
            for (Synapse s : synapses) {
                excitatory = shouldBeExcitatory(percentExcitatory, exciteCount,
                    inhibCount, s);
                // Set the strength based on the polarity.
                if (excitatory) {
                    s.setStrength(exciteRand != null ? exciteRand.getRandom()
                        : DEFAULT_EXCITATORY_STRENGTH);
                    exciteCount--;
                    // Change the percentExcitatory to maintain balance
                    percentExcitatory = exciteCount / (double) remaining;
                } else {
                    s.setStrength(inhibRand != null ? inhibRand.getRandom()
                        : DEFAULT_INHIBITORY_STRENGTH);
                    inhibCount--;
                    // Change the percentExcitatory to maintain balance.
                    percentExcitatory = (remaining - inhibCount)
                        / (double) remaining;
                }
                remaining--;
            }
        }
    }

    /**
     * 
     * @param synapses
     * @param exciteRand
     * @param inhibRand
     */
    public static void randomizeSynapses(Collection<Synapse> synapses,
        PolarizedRandomizer exciteRand, PolarizedRandomizer inhibRand) {
        if (exciteRand.equals(inhibRand)) {
            throw new IllegalArgumentException("Randomization has failed."
                + " The excitatory and inhibitory randomizers cannot be"
                + " the same object.");
        } else {
            checkPolarityMatches(exciteRand, Polarity.EXCITATORY);
            checkPolarityMatches(inhibRand, Polarity.INHIBITORY);
            boolean excitatory = false;
            for (Synapse s : synapses) {
                excitatory = s.getStrength() > 0;
                // Set the strength based on the polarity.
                if (excitatory) {
                    s.setStrength(exciteRand != null ? exciteRand.getRandom()
                        : DEFAULT_EXCITATORY_STRENGTH);
                } else {
                    s.setStrength(inhibRand != null ? inhibRand.getRandom()
                        : DEFAULT_INHIBITORY_STRENGTH);
                }
            }
        }
    }

    /**
     * Randomizes the excitatory synapses in the given list of synapses using
     * the given excitatory randomizer.
     * 
     * @param synapses
     * @param exciteRand
     */
    public static void randomizeExcitatorySynapses(
        Collection<Synapse> synapses, PolarizedRandomizer exciteRand) {
        checkPolarityMatches(exciteRand, Polarity.EXCITATORY);
        for (Synapse s : synapses) {
            if (s.getSource().getPolarity().equals(Polarity.EXCITATORY)
                || s.getStrength() > 0) {
                s.setStrength(exciteRand != null ? exciteRand.getRandom()
                    : DEFAULT_EXCITATORY_STRENGTH);
            }
        }
    }

    /**
     * Randomizes the given synapses using the given excitatory randomizer
     * without checking first to make sure that the given synapses or their
     * source neurons are not inhibitory. Used for speed when the polarity of
     * the synapses in the list is known ahead of time.
     * 
     * @param synapses
     * @param exciteRand
     */
    public static void randomizeExcitatorySynapsesUnsafe(
        Collection<Synapse> synapses, PolarizedRandomizer exciteRand) {
        checkPolarityMatches(exciteRand, Polarity.EXCITATORY);
        for (Synapse s : synapses) {
            s.setStrength(exciteRand != null ? exciteRand.getRandom()
                : DEFAULT_EXCITATORY_STRENGTH);
        }
    }

    /**
     * Randomizes the inhibitory synapses in the given list of synapses using
     * the given inhibitory randomizer.
     * 
     * @param synapses
     * @param inhibRand
     */
    public static void randomizeInhibitorySynapses(
        Collection<Synapse> synapses, PolarizedRandomizer inhibRand) {
        checkPolarityMatches(inhibRand, Polarity.INHIBITORY);
        for (Synapse s : synapses) {
            if (s.getSource().getPolarity().equals(Polarity.INHIBITORY)
                || s.getStrength() < 0) {
                s.setStrength(inhibRand != null ? inhibRand.getRandom()
                    : DEFAULT_INHIBITORY_STRENGTH);
            }
        }
    }

    /**
     * Randomizes the given synapses using the given inhibitory randomizer
     * without checking first to make sure that the given synapses or their
     * source neurons are not excitatory. Used for speed when the polarity of
     * the synapses in the list is known ahead of time.
     * 
     * @param synapses
     * @param inhibRand
     */
    public static void randomizeInhibitorySynapsesUnsafe(
        Collection<Synapse> synapses, PolarizedRandomizer inhibRand) {
        checkPolarityMatches(inhibRand, Polarity.INHIBITORY);
        for (Synapse s : synapses) {
            s.setStrength(inhibRand != null ? inhibRand.getRandom()
                : DEFAULT_INHIBITORY_STRENGTH);
        }
    }

    /**
     * 
     * @param synapses
     * @return
     */
    public static ArrayList<Synapse> getExcitatorySynapses(
        Collection<Synapse> synapses) {
        ArrayList<Synapse> exSyns = new ArrayList<Synapse>(synapses.size() / 2);
        for (Synapse s : synapses) {
            if (s.getStrength() > 0 || Polarity.EXCITATORY.equals(s.getSource()
                .getPolarity())) {
                exSyns.add(s);
            }
        }
        return exSyns;
    }

    /**
     * 
     * @param synapses
     * @return
     */
    public static ArrayList<Synapse> getInhibitorySynapses(
        Collection<Synapse> synapses) {
        ArrayList<Synapse> inSyns = new ArrayList<Synapse>(synapses.size() / 2);
        for (Synapse s : synapses) {
            if (s.getStrength() < 0 || Polarity.INHIBITORY.equals(s.getSource()
                .getPolarity())) {
                inSyns.add(s);
            }
        }
        return inSyns;
    }

    /**
     * Changes all the synapses in a given collection such that
     * <b>percentExcitatory</b> of them are excitatory and <b>1 -
     * percentExcitatory</b> of them are inhibitory, assigning default strengths
     * respectively to each.
     * 
     * This method will attempt to maintain the requested percentExcitatory even
     * if some or all of the source neurons are themselves polarized. In such
     * cases the polarity of the Neurons efferent synapses will not be
     * overridden. Though it may not be possible to obtain the desired
     * percentExcitatory in this case, this method will get as close as
     * possible.
     * 
     * @param synapses
     * @param percentExcitatory
     */
    public static void polarizeSynapses(Collection<Synapse> synapses,
        double percentExcitatory) {
        if (percentExcitatory > 1 || percentExcitatory < 0) {
            throw new IllegalArgumentException("Randomization had failed."
                + " The percent of excitatory synapses "
                + " cannot be greater than 1 or less than 0.");
        } else {
            int exciteCount = (int) (percentExcitatory * synapses.size());
            int inhibCount = synapses.size() - exciteCount;
            int remaining = synapses.size();
            boolean excitatory = false;
            for (Synapse s : synapses) {
                excitatory = shouldBeExcitatory(percentExcitatory, exciteCount,
                    inhibCount, s);
                // Set the strength based on the polarity.
                if (excitatory) {
                    s.setStrength(DEFAULT_EXCITATORY_STRENGTH);
                    exciteCount--;
                    // Change the percentExcitatory to maintain balance
                    percentExcitatory = exciteCount / (double) remaining;
                } else {
                    s.setStrength(DEFAULT_INHIBITORY_STRENGTH);
                    inhibCount--;
                    // Change the percentExcitatory to maintain balance.
                    percentExcitatory = (remaining - inhibCount)
                        / (double) remaining;
                }
                remaining--;
            }
        }
    }

    /**
     * 
     * @param inQuestion
     * @param expectedPolarity
     * @throws IllegalArgumentException
     */
    private static void checkPolarityMatches(PolarizedRandomizer inQuestion,
        Polarity expectedPolarity) throws IllegalArgumentException {
        // TODO: Use "optional" instead when upgrade to Java 8
        if (inQuestion == null)
            return;
        if (!expectedPolarity.equals(inQuestion.getPolarity())) {
            throw new IllegalArgumentException("Randomizer's polarity does"
                + " not match its implied polarity");
        }
    }

    /**
     * 
     * @param percentExcitatory
     * @param exciteCount
     * @param inhibCount
     * @param s
     * @return
     */
    private static boolean shouldBeExcitatory(double percentExcitatory,
        int exciteCount, int inhibCount, Synapse s) {
        boolean excitatory = false;
        if (s.getSource().isPolarized()) {
            if (s.getSource().getPolarity().equals(Polarity.EXCITATORY)) {
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
                if (exciteOrInhib < percentExcitatory) {
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
     * @return true or false: whether or not these connections are recurrent.
     */
    public static boolean testRecurrence(List<Neuron> sourceNeurons,
        List<Neuron> targetNeurons) {
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
     * 
     * @param exciteRule
     * @param inhibRule
     * @param synapses
     */
    public static void applyLearningRules(SynapseUpdateRule exciteRule,
        SynapseUpdateRule inhibRule, List<Synapse> synapses) {
        for (Synapse s : synapses) {
            if (s.getStrength() < 0) {
                s.setLearningRule(inhibRule);
            } else {
                s.setLearningRule(exciteRule);
            }
        }
    }

}
