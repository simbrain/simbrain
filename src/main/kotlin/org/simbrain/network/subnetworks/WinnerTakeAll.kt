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
package org.simbrain.network.subnetworks

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.GuiEditable
import java.util.*
import java.util.function.Function

/**
 * **WinnerTakeAll**.The neuron with the highest weighted input in a
 * winner-take-all network takes on an upper value, all other neurons take on
 * the lower value. In case of a tie a randomly chosen member of the "winners"
 * is returned.
 */
class WinnerTakeAll : NeuronGroup {
    /**
     * Winning value.
     */
    @UserParameter(label = "Win value", order = 50)
    var winValue = 1.0

    /**
     * Losing value.
     */
    var loseValue by GuiEditable(
        label = "Lose value",
        initValue = 0.0,
        order = 60,
    )

    /**
     * If true, sometimes set the winner randomly.
     */
    var isUseRandom by GuiEditable(
        initValue = false,
        label = "Random winner",
        order = 70,
    )

    /**
     * Probability of setting the winner randomly, when useRandom is true.
     */
    var randomProb by GuiEditable(
        label = "Random prob",
        initValue = .1,
        order = 80,
        conditionallyEnabledBy = WinnerTakeAll::isUseRandom,
    )

    /**
     * Copy constructor.
     *
     * @param newRoot new root net
     * @param oldNet  old network
     */
    constructor(newRoot: Network?, oldNet: WinnerTakeAll) : super(newRoot, oldNet) {
        loseValue = oldNet.loseValue
        winValue = oldNet.winValue
        isUseRandom = oldNet.isUseRandom
        randomProb = oldNet.randomProb
        label = "WTA Group (copy)"
    }

    /**
     * Creates a new winner take all network.
     *
     * @param root       the network containing this subnetwork
     * @param numNeurons Number of neurons in new network
     */
    constructor(root: Network?, numNeurons: Int) : super(root) {
        for (i in 0 until numNeurons) {
            // TODO: Prevent invalid states like this?
            addNeuron(Neuron(root, LinearRule()))
        }
        label = "Winner take all network"
    }

    override fun deepCopy(newNetwork: Network): WinnerTakeAll {
        return WinnerTakeAll(newNetwork, this)
    }

    override fun getTypeDescription(): String {
        return "Winner Take All Group"
    }

    override fun update() {
        var winner = winner
        if (isUseRandom) {
            if (Math.random() < randomProb) {
                winner = getNeuronList()[rand.nextInt(getNeuronList().size)]
            }
        }
        for (neuron in getNeuronList()) {
            if (neuron === winner) {
                neuron.setActivation(winValue)
            } else {
                neuron.setActivation(loseValue)
            }
        }
    }

    val winner: Neuron?
        /**
         * Returns the neuron with the greatest net input.
         *
         * @return winning neuron
         */
        get() = getWinner(getNeuronList())

    /**
     * Called by reflection via [UserParameter.conditionalEnablingMethod]
     */
    fun useRandomWinner(): Function<Map<String?, Any?>, Boolean?> {
        return Function { map: Map<String?, Any?> -> map["Random winner"] as Boolean? }
    }

    companion object {
        /**
         * Random number generator.
         */
        private val rand = Random()

        /**
         * Returns the neuron in the provided list with the greatest net input (or a
         * randomly chosen neuron among those that "win").
         *
         * @param neuronList the list to check
         * @return the neuron with the highest net input
         */
        @JvmStatic
        fun getWinner(neuronList: List<Neuron>): Neuron? {
            return getWinner(neuronList, false)
        }

        /**
         * Returns the neuron in the provided list with the greatest net input or
         * activation (or a randomly chosen neuron among those that "win").
         *
         * @param neuronList     the list to check
         * @param useActivations if true, use activations instead of net input to
         * determine winner
         * @return the neuron with the highest net input
         */
        @JvmStatic
        fun getWinner(neuronList: List<Neuron>, useActivations: Boolean): Neuron? {
            if (neuronList.isEmpty()) {
                return null
            }
            val winners: MutableList<Neuron> = ArrayList()
            var winner = neuronList[0]
            winners.add(winner)
            for (n in neuronList) {
                val winnerVal = if (useActivations) winner.activation else winner.getWeightedInputs()
                val `val` = if (useActivations) n.activation else n.getWeightedInputs()
                if (`val` == winnerVal) {
                    winners.add(n)
                } else if (`val` > winnerVal) {
                    winners.clear()
                    winner = n
                    winners.add(n)
                }
            }
            return if (winners.size == 1) {
                winner
            } else {
                winners[rand.nextInt(winners.size)]
            }
        }
    }
}