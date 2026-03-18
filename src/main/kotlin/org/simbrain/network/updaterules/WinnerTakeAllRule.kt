package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import kotlin.random.Random

/**
 * Winner-take-all rule for [NeuronArray]. The neuron with the highest input takes on [winValue],
 * all others take on [loseValue]. In case of a tie, a random winner is chosen.
 *
 * Not defined for scalar neurons.
 */
class WinnerTakeAllRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), BoundedUpdateRule {

    @UserParameter(label = "Win value", description = "Activation value for the winning neuron", order = 10)
    var winValue = 1.0

    @UserParameter(label = "Lose value", description = "Activation value for losing neurons", order = 20)
    var loseValue = 0.0

    @UserParameter(
        label = "Use random",
        description = "If true, sometimes set the winner randomly",
        order = 30
    )
    var useRandom = false

    @UserParameter(
        label = "Random probability",
        description = "Probability of setting the winner randomly",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = .1,
        order = 40
    )
    var randomProb = 0.1

    context(Network) override fun apply(layer: Layer, dataHolder: EmptyMatrixData) {
        if (layer !is NeuronArray) return

        val inputs = layer.inputs
        val size = layer.size

        // Find winner index (max input), breaking ties randomly
        var maxVal = inputs[0, 0]
        val winners = mutableListOf(0)
        for (i in 1 until size) {
            val v = inputs[i, 0]
            if (v > maxVal) {
                maxVal = v
                winners.clear()
                winners.add(i)
            } else if (v == maxVal) {
                winners.add(i)
            }
        }

        var winnerIdx = if (winners.size == 1) winners[0] else winners[Random.nextInt(winners.size)]

        if (useRandom && Random.nextDouble() < randomProb) {
            winnerIdx = Random.nextInt(size)
        }

        val result = DoubleArray(size) { if (it == winnerIdx) winValue else loseValue }
        layer.setActivations(result)
    }

    context(Network) override fun apply(neuron: Neuron, data: EmptyScalarData) {
        throw UnsupportedOperationException("WinnerTakeAllRule does not support scalar data")
    }

    override val name = "Winner Take All"
    override val timeType = Network.TimeType.DISCRETE

    override fun createMatrixData(size: Int) = EmptyMatrixData

    override fun copy() = WinnerTakeAllRule().also {
        it.winValue = winValue
        it.loseValue = loseValue
        it.useRandom = useRandom
        it.randomProb = randomProb
    }

    override var upperBound: Double
        get() = winValue
        set(value) {}

    override var lowerBound: Double
        get() = loseValue
        set(value) {}
}
