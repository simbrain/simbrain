package org.simbrain.network.updaterules

import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * An abstract superclass for discrete and continuous time sigmodial squashing
 * function based update rules containing methods and variables common to both.
 *
 * @author Zoë Tosi
 */
abstract class AbstractSigmoidalRule : NeuronUpdateRule<BiasedScalarData, BiasedMatrixData>(),
    DifferentiableUpdateRule, NoisyUpdateRule, BoundedUpdateRule {

    @UserParameter(label = "Implementation", order = 10)
    var type: SigmoidFunctionEnum = DEFAULT_SIGMOID_TYPE

    override var upperBound: Double = 1.0

    override var lowerBound: Double = 0.0

    @UserParameter(
        label = "Slope",
        description = "This represents how steep the sigmoidal is.",
        increment = .1,
        order = 40
    )
    open var slope: Double = 1.0

    /**
     * Noise generator.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    override var addNoise: Boolean = false

    override fun createScalarData(): BiasedScalarData {
        return BiasedScalarData()
    }

    override fun createMatrixData(size: Int): BiasedMatrixData {
        return BiasedMatrixData(size)
    }

    /**
     * Copy the overlapping bits of the rule for subclasses.
     *
     * @param sr the sigmoid rule to copy
     * @return the copy.
     */
    protected fun baseDeepCopy(sr: AbstractSigmoidalRule): AbstractSigmoidalRule {
        sr.type = type
        sr.slope = slope
        sr.addNoise = addNoise
        sr.lowerBound = lowerBound
        sr.upperBound = upperBound
        sr.noiseGenerator = noiseGenerator.deepCopy()
        return sr
    }

    companion object {
        /**
         * The default squashing function, informs the default upper and lower bounds.
         */
        val DEFAULT_SIGMOID_TYPE: SigmoidFunctionEnum = SigmoidFunctionEnum.LOGISTIC
    }
}