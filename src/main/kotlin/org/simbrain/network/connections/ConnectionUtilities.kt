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
package org.simbrain.network.connections

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseUpdateRule
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * Utility functions/interfaces/etc for manipulating synapses.
 * Usually, for manipulating loose synapses since most changes to Synapses in a
 * synapse group should be done through the synapse group, but there are
 * counter-examples.
 *
 * TODO: Make synapse group use more of these functions.
 *
 * @author Zoë Tosi
 */

const val DEFAULT_EXCITATORY_STRENGTH = 1.0

const val DEFAULT_INHIBITORY_STRENGTH = -1.0

/**
 * Randomizes a collection of synapses based on excitatory and inhibitory
 * (polarized appropriately) randomizers, which cannot be the same
 * randomizer. This method will always attempt to maintain the ratio of
 * excitatory synapses specified. However if some of the source neurons are
 * themselves polarized, this may not always be possible. In such a case,
 * this method will get as close as possible to the desired ratio. This,
 * however is not recommended.
 *
 *
 * If the source neurons to these synapses are themselves, by and large,
 * polarized, this method can be, but should **NOT** be used.
 *
 *
 * Null values for either PolarizedRandomizer is permitted. Synapses are
 * assigned default strengths based on their polarity depending on which
 * randomizers are null.
 *
 * @param exciteRand      the randomizer to be used to determine the weights
 * of excitatory synapses.
 * @param inhibRand       the randomizer to be used to determine the weights
 * of inhibitory synapses.
 * @param excitatoryRatio the ration of excitatory to inhibitory synapses.
 * @param synapses        the synapses to modify
 * @throws IllegalArgumentException
 */
@Throws(IllegalArgumentException::class)
fun randomizeAndPolarizeSynapses(
    synapses: Collection<Synapse>,
    exciteRand: ProbabilityDistribution?,
    inhibRand: ProbabilityDistribution?,
    excitatoryRatio: Double
) {
    var excitatoryRatio = excitatoryRatio
    if (exciteRand == inhibRand) {
        throw IllegalArgumentException("Randomization has failed." + " The excitatory and inhibitory randomizers cannot be" + " the same object.")
    } else // Change the excitatoryRatio to maintain balance.// Change the excitatoryRatio to maintain balance// Set the strength based on the polarity.
        require(!(excitatoryRatio > 1 || excitatoryRatio < 0)) { "Randomization had failed." + " The ratio of excitatory synapses " + " cannot be greater than 1 or less than 0." }
    checkPolarityMatches(exciteRand, Polarity.EXCITATORY)
    checkPolarityMatches(inhibRand, Polarity.INHIBITORY)
    var exciteCount = (excitatoryRatio * synapses.size).toInt()
    var inhibCount = synapses.size - exciteCount
    var remaining = synapses.size
    var excitatory = false
    for (s in synapses) {
        excitatory = shouldBeExcitatory(excitatoryRatio, exciteCount, inhibCount, s)
        // Set the strength based on the polarity.
        if (excitatory) {
            s.strength = if (exciteRand != null) exciteRand.sampleDouble() else DEFAULT_EXCITATORY_STRENGTH
            exciteCount--
            // Change the excitatoryRatio to maintain balance
            excitatoryRatio = exciteCount / remaining.toDouble()
        } else {
            s.strength = if (inhibRand != null) inhibRand.sampleDouble() else DEFAULT_INHIBITORY_STRENGTH
            inhibCount--
            // Change the excitatoryRatio to maintain balance.
            excitatoryRatio = (remaining - inhibCount) / remaining.toDouble()
        }
        remaining--
    }
}

/**
 * Randomize and polarize synapses using default excitatory and inhibitory
 * polarizers (uniform 0 to 1).
 *
 * @param synapses        the synapses to modify
 * @param excitatoryRatio the ration of excitatory to inhibitory synapses.
 */
fun randomizeAndPolarizeSynapses(synapses: Collection<Synapse>, excitatoryRatio: Double) {
    val exciteRand: ProbabilityDistribution = UniformRealDistribution()
    val inhibRand: ProbabilityDistribution = UniformRealDistribution()
    randomizeAndPolarizeSynapses(synapses, exciteRand, inhibRand, excitatoryRatio)
}

/**
 * @param synapses   the synapses to modify
 * @param exciteRand the randomizer to be used to determine the weights of
 * excitatory synapses.
 * @param inhibRand  the randomizer to be used to determine the weights of
 * inhibitory synapses.
 */
fun randomizeSynapses(
    synapses: Collection<Synapse>,
    exciteRand: ProbabilityDistribution?,
    inhibRand: ProbabilityDistribution?
) {
    if (exciteRand == inhibRand) {
        throw IllegalArgumentException("Randomization has failed." + " The excitatory and inhibitory randomizers cannot be" + " the same object.")
    } else {
        checkPolarityMatches(exciteRand, Polarity.EXCITATORY)
        checkPolarityMatches(inhibRand, Polarity.INHIBITORY)
        var excitatory = false
        for (s in synapses) {
            excitatory = s.strength > 0
            // Set the strength based on the polarity.
            if (excitatory) {
                s.strength = if (exciteRand != null) exciteRand.sampleDouble() else DEFAULT_EXCITATORY_STRENGTH
            } else {
                s.strength = if (inhibRand != null) inhibRand.sampleDouble() else DEFAULT_INHIBITORY_STRENGTH
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
 * excitatory synapses.
 */
fun randomizeExcitatorySynapses(synapses: Collection<Synapse>, exciteRand: ProbabilityDistribution?) {
    checkPolarityMatches(exciteRand, Polarity.EXCITATORY)
    for (s in synapses) {
        if (Polarity.EXCITATORY == s.source.polarity || s.strength > 0) {
            s.strength = if (exciteRand != null) exciteRand.sampleDouble() else DEFAULT_EXCITATORY_STRENGTH
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
 * excitatory synapses.
 */
fun randomizeExcitatorySynapsesUnsafe(synapses: Collection<Synapse>, exciteRand: ProbabilityDistribution?) {
    checkPolarityMatches(exciteRand, Polarity.EXCITATORY)
    for (s in synapses) {
        s.strength = if (exciteRand != null) exciteRand.sampleDouble() else DEFAULT_EXCITATORY_STRENGTH
    }
}

/**
 * Randomizes the inhibitory synapses in the given list of synapses using
 * the given inhibitory randomizer.
 *
 * @param synapses  the synapses to modify
 * @param inhibRand the randomizer to be used to determine the weights of
 * inhibitory synapses.
 */
fun randomizeInhibitorySynapses(synapses: Collection<Synapse>, inhibRand: ProbabilityDistribution?) {
    checkPolarityMatches(inhibRand, Polarity.INHIBITORY)
    for (s in synapses) {
        if (Polarity.INHIBITORY == s.source.polarity || s.strength < 0) {
            s.strength = if (inhibRand != null) inhibRand.sampleDouble() else DEFAULT_INHIBITORY_STRENGTH
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
 * inhibitory synapses.
 */
fun randomizeInhibitorySynapsesUnsafe(synapses: Collection<Synapse>, inhibRand: ProbabilityDistribution?) {
    checkPolarityMatches(inhibRand, Polarity.INHIBITORY)
    for (s in synapses) {
        s.strength = if (inhibRand != null) inhibRand.sampleDouble() else DEFAULT_INHIBITORY_STRENGTH
    }
}

/**
 * @param synapses the synapses to modify
 * @return excitatory synapses
 */
fun getExcitatorySynapses(synapses: Collection<Synapse>): ArrayList<Synapse> {
    val exSyns = ArrayList<Synapse>(synapses.size / 2)
    for (s in synapses) {
        if (s.strength > 0 || Polarity.EXCITATORY == s.source.polarity) {
            exSyns.add(s)
        }
    }
    return exSyns
}

/**
 * @param synapses the synapses to modify
 * @return inhibitory synapses
 */
fun getInhibitorySynapses(synapses: Collection<Synapse>): ArrayList<Synapse> {
    val inSyns = ArrayList<Synapse>(synapses.size / 2)
    for (s in synapses) {
        if (s.strength < 0 || Polarity.INHIBITORY == s.source.polarity) {
            inSyns.add(s)
        }
    }
    return inSyns
}

/**
 * Changes all the synapses in a given collection such that
 * **excitatoryRatio** of them are excitatory and **1 -
 * excitatoryRatio** of them are inhibitory, assigning default strengths
 * respectively to each.
 *
 *
 * This method will attempt to maintain the requested excitatoryRatio even
 * if some or all of the source neurons are themselves polarized. In such
 * cases the polarity of the Neurons efferent synapses will not be
 * overridden. Though it may not be possible to obtain the desired
 * excitatoryRatio in this case, this method will get as close as possible.
 *
 * @param synapses        the synapses to polarize
 * @param excitatoryRatio the ration of excitatory synapses (1 for all
 * exctitatory)
 */
fun polarizeSynapses(synapses: Collection<Synapse>, excitatoryRatio: Double) {
    var excitatoryRatio = excitatoryRatio
    if (excitatoryRatio > 1 || excitatoryRatio < 0) {
        throw IllegalArgumentException("Randomization had failed." + " The ratio of excitatory synapses " + " cannot be greater than 1 or less than 0.")
    } else {
        var exciteCount = (excitatoryRatio * synapses.size).toInt()
        var inhibCount = synapses.size - exciteCount
        var remaining = synapses.size
        var excitatory = false
        for (s in synapses) {
            excitatory = shouldBeExcitatory(excitatoryRatio, exciteCount, inhibCount, s)
            // Set the strength based on the polarity.
            if (excitatory) {
                s.strength = DEFAULT_EXCITATORY_STRENGTH
                exciteCount--
                // Change the excitatoryRatio to maintain balance
                excitatoryRatio = exciteCount / remaining.toDouble()
            } else {
                s.strength = DEFAULT_INHIBITORY_STRENGTH
                inhibCount--
                // Change the excitatoryRatio to maintain balance.
                excitatoryRatio = (remaining - inhibCount) / remaining.toDouble()
            }
            remaining--
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
 * synapses
 * @param inTemplateSynapse temporary set containing all inhibitory
 * synapses
 */
fun conformToTemplates(synapses: Collection<Synapse>, exTemplateSynapse: Synapse, inTemplateSynapse: Synapse) {
    if (exTemplateSynapse.strength <= 0) {
        throw IllegalArgumentException("Excitatory template synapse" + " must be excitatory (having strength > 0).")
    }
    if (inTemplateSynapse.strength >= 0) {
        throw IllegalArgumentException("Inhibitory template synapse" + " must be inhibitory (having strength < 0).")
    }
    for (s in synapses) {
        if (s.strength < 0) {
            s.strength = inTemplateSynapse.strength
            s.learningRule = inTemplateSynapse.learningRule.deepCopy()
            s.upperBound = inTemplateSynapse.upperBound
            s.lowerBound = inTemplateSynapse.lowerBound
            s.delay = inTemplateSynapse.delay
            s.isEnabled = inTemplateSynapse.isEnabled
            s.isFrozen = inTemplateSynapse.isFrozen
            s.increment = inTemplateSynapse.increment
            if (inTemplateSynapse.spikeResponder != null) {
                s.spikeResponder = inTemplateSynapse.spikeResponder.deepCopy()
            }
        }
        if (s.strength > 0) {
            s.strength = exTemplateSynapse.strength
            s.learningRule = exTemplateSynapse.learningRule.deepCopy()
            s.upperBound = exTemplateSynapse.upperBound
            s.lowerBound = exTemplateSynapse.lowerBound
            s.delay = exTemplateSynapse.delay
            s.isEnabled = exTemplateSynapse.isEnabled
            s.isFrozen = exTemplateSynapse.isFrozen
            s.increment = exTemplateSynapse.increment
            if (exTemplateSynapse.spikeResponder != null) {
                s.spikeResponder = exTemplateSynapse.spikeResponder.deepCopy()
            }
        }
    }
}

/**
 * @param inQuestion
 * @param expectedPolarity
 * @throws IllegalArgumentException
 */
@Throws(IllegalArgumentException::class)
private fun checkPolarityMatches(inQuestion: ProbabilityDistribution?, expectedPolarity: Polarity) {
    // TODO: Use "optional" instead when upgrade to Java 8
    if (inQuestion == null) {
        return
    }

    // // TODO: I'm commenting this out for now just to test code, but
    // //     it's being thrown a lot and testing is needed.
    // if (expectedPolarity != inQuestion.polarity) {
    //     throw IllegalArgumentException("Randomizer's polarity does" + " not match its implied polarity")
    // }
}

/**
 * @param excitatoryRatio the ratio of excitatory to inhibitory synapses.
 */
private fun shouldBeExcitatory(excitatoryRatio: Double, exciteCount: Int, inhibCount: Int, s: Synapse): Boolean {
    var excitatory = false
    if (s.source.isPolarized) {
        excitatory = if (Polarity.EXCITATORY === s.source.polarity) {
            true
        } else {
            false
        }
    } else {
        if (exciteCount <= 0 || inhibCount <= 0) {
            if (exciteCount <= 0) {
                excitatory = false
            }
            if (inhibCount <= 0) {
                excitatory = true
            }
        } else {
            val exciteOrInhib = Math.random()
            excitatory = if (exciteOrInhib < excitatoryRatio) {
                true
            } else {
                false
            }
        }
    }
    return excitatory
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
fun testRecurrence(sourceNeurons: List<Neuron>, targetNeurons: List<Neuron>): Boolean {
    if (sourceNeurons.size != targetNeurons.size) {
        return false
    } else {
        for (i in sourceNeurons.indices) {
            if (sourceNeurons[i] !== targetNeurons[i]) {
                return false
            }
        }
    }
    return true
}

/**
 * Automatically applies separate learning rules for excitatory
 * and inhibitory synapses.
 *
 * @param exciteRule excitatory rule to appy
 * @param inhibRule inhibitory rule to apply
 * @param synapses list of synapses to apply rule to
 */
fun applyLearningRules(exciteRule: SynapseUpdateRule?, inhibRule: SynapseUpdateRule?, synapses: List<Synapse>) {
    for (s in synapses) {
        if (s.strength < 0) {
            s.learningRule = inhibRule
        } else {
            s.learningRule = exciteRule
        }
    }
}

/**
 * Like [.getSeparatedInput] but excitatory
 * and inhibitory inputs are separated.
 *
 * @param n neuron to get normalized separated inputs from
 * @return the separated inputs
 */
fun getAverageSeparatedInput(n: Neuron): DoubleArray {
    val ei = DoubleArray(2)
    var e = 0.0
    var i = 0.0
    for (s in n.fanIn) {
        s.update()
        val psr = s.psr
        if (psr > 0) {
            ei[0] += psr
            e++
        } else {
            ei[1] += psr
            i++
        }
    }
    if (e > 1) {
        ei[0] /= e
    }
    if (i > 1) {
        ei[1] /= i
    }
    return ei
}

/**
 * Returns summed excitatory and inhibitory inputs.
 * Used for conductance-based models.
 *
 * @param n neuron to get separated inputs from
 * @return the separated inputs
 */
fun getSeparatedInput(n: Neuron): DoubleArray {
    //TODO: Make this an argument so the array is not re-created
    // for each neuron
    val ei = DoubleArray(2)
    for (s in n.fanIn) {
        s.updateOutput()
        val psr = s.psr
        if (psr > 0) {
            ei[0] += psr
        } else {
            ei[1] += psr
        }
    }
    return ei
}
