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
package org.simbrain.network.neurongroups

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CustomTypeName
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.sampleOne
import kotlin.random.Random

/**
 * The neuron with the highest weighted input in a winner-take-all network takes on an upper value,
 * all other neurons take on the lower value. In case of a tie a randomly chosen member of the "winners"
 * is returned.
 */
class WinnerTakeAll @JvmOverloads constructor(
    neurons: List<Neuron>,
    params: WinnerTakeAllParams = WinnerTakeAllParams()
) : NeuronGroup() {

    var params by GuiEditable(
        label = "Winner Take All Parameters",
        description = "Parameters for the WTA Group",
        initValue = params.apply { creationMode = false },
        order = 50
    )

    constructor(network: Network, numNeurons: Int) : this(List(numNeurons) { Neuron() })

    @XStreamConstructor
    private constructor(parentNetwork: Network): this(listOf())

    init {
        addNeurons(neurons)
    }

    override fun copy() = WinnerTakeAll(neuronList.map { it.copy() }, params.copy())

    fun getWinner(): Neuron = getWinner(neuronList)

    context(Network)
    override fun update() {
        neuronList.forEach { it.updateInputs() }
        neuronList.forEach { it.update() }
        var winner = getWinner(neuronList, false)
        if (params.isUseRandom) {
            if (Random.nextDouble() < params.randomProb) {
                winner = neuronList.sampleOne()
            }
        }
        for (neuron in neuronList) {
            if (neuron === winner) {
                neuron.activation = params.winValue
            } else {
                neuron.activation = params.loseValue
            }
        }
    }

}

@CustomTypeName("Winner Take All")
class WinnerTakeAllParams : NeuronGroupParams() {

    @UserParameter(label = "Wining value", order = 50)
    var winValue = 1.0

    var loseValue by GuiEditable(
        label = "Losing value",
        initValue = 0.0,
        order = 60,
    )

    var isUseRandom by GuiEditable(
        initValue = false,
        label = "Random winner",
        description = "If true, sometimes set the winner randomly",
        order = 70,
    )

    var randomProb by GuiEditable(
        label = "Random prob",
        description = "Probability of setting the winner randomly, when useRandom is true",
        initValue = .1,
        order = 80,
        conditionallyEnabledBy = WinnerTakeAllParams::isUseRandom,
    )

    override fun create(): WinnerTakeAll {
        return WinnerTakeAll(List(numNeurons) { Neuron() }, this)
    }

    override fun copy(): WinnerTakeAllParams {
        return WinnerTakeAllParams().also {
            commonCopy(it)
            it.winValue = winValue
            it.loseValue = loseValue
            it.isUseRandom = isUseRandom
            it.randomProb = randomProb
        }
    }
}

/**
 * Returns the neuron in the provided list with the greatest net input or
 * activation (or a randomly chosen neuron among those that "win").
 *
 * @param useActivations if true, use activations instead of net input to
 * determine winner
 * @return the neuron with the highest net input
 */
fun getWinner(neuronList: List<Neuron>, useActivations: Boolean = false): Neuron {
    if (neuronList.isEmpty()) {
        throw IllegalArgumentException("There are no winners in any empty neuron list")
    }
    val winners: MutableList<Neuron> = ArrayList()
    var winner = neuronList[0]
    winners.add(winner)
    for (n in neuronList) {
        val winnerVal = if (useActivations) winner.activation else winner.weightedInputs
        val value = if (useActivations) n.activation else n.weightedInputs
        if (value == winnerVal) {
            winners.add(n)
        } else if (value > winnerVal) {
            winners.clear()
            winner = n
            winners.add(n)
        }
    }
    return if (winners.size == 1) {
        winner
    } else {
        winners[Random.nextInt(winners.size)]
    }
}