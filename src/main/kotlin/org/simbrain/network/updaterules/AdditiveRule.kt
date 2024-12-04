package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.applyFunction
import org.simbrain.util.propertyeditor.CustomTypeName
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import smile.math.matrix.Matrix
import kotlin.math.atan

/**
 * Decay-type neuron used with continuous Hopfield networks.
 * See Haykin (2002), section 14.5 and the original Hopfield PNAS article.
 */
@CustomTypeName("Additive (Continuous Hopfield)")
class AdditiveRule : NeuronUpdateRule<EmptyScalarData, EmptyMatrixData>(), NoisyUpdateRule {

    @UserParameter(
        label = "Lambda",
        description = "A gain parameter that controls the steepness of the sigmoid curve. Higher lambda makes the function more like a step function; lower lambda produces a gentler slope",
        minimumValue = 0.0,
        increment = .1,
        order = 10)
    var lambda: Double = 1.4

    @UserParameter(
        label = "Resistance",
        description = "Controls the rate of activation decay; higher resistance slows decay, lower resistance causes the activation to decay more quickly",
        minimumValue = 0.0,
        increment = .1,
        order = 20)
    var resistance: Double = 1.0

    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    override var addNoise: Boolean = false

    override val timeType: Network.TimeType
        get() = Network.TimeType.CONTINUOUS

    override fun copy(): AdditiveRule {
        val an = AdditiveRule()
        an.lambda = lambda
        an.resistance = resistance
        an.addNoise = addNoise
        an.noiseGenerator = noiseGenerator.copy()
        return an
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {

        neuron.activation += timeStep * (-neuron.activation / resistance + neuron.input)

        if (addNoise) {
            neuron.activation += noiseGenerator.sampleDouble()
        }

    }

    /**
     * Implements a Hopfield type sigmoidal function.
     */
    override fun synapticInputModifier(input: Double): Double {
        return 2 / Math.PI * atan((Math.PI * lambda * input) / 2)
    }

    override fun synapticInputModifier(input: Matrix): Matrix {
        return input.applyFunction(::synapticInputModifier)
    }

    override val name: String
        get() = "Additive (Continuous Hopfield)"

}