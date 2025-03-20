package org.simbrain.network.trainers

import org.simbrain.network.core.WeightMatrix
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.setValuesInPlace
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import smile.math.matrix.Matrix
import kotlin.math.sqrt


sealed class WeightInitializationStrategy(val seed: Long? = null): CopyableObject {
    abstract fun initializeWeights(weightMatrix: WeightMatrix)

    abstract fun initializeWeights(matrix: Matrix)

    override fun getTypeList(): List<Class<out CopyableObject>> = listOf(
        Randomize::class.java,
        Xavier::class.java,
        He::class.java,
        LeCun::class.java
    )

    abstract override fun copy(): WeightInitializationStrategy
}

class Xavier(seed: Long? = null): WeightInitializationStrategy(seed) {

    enum class Distribution {
        UNIFORM, NORMAL
    }

    @UserParameter("Distribution")
    var distribution = Distribution.UNIFORM

    private fun createRandomizer(numInputs: Int, numOutputs: Int): ProbabilityDistribution {
        return when (distribution){
            Distribution.UNIFORM -> UniformRealDistribution(-sqrt(6.0 / (numInputs + numOutputs)), sqrt(6.0 / (numInputs + numOutputs)))
            Distribution.NORMAL -> NormalDistribution(0.0, sqrt(2.0/ (numInputs + numOutputs)))
        }.apply { randomSeed = seed }
    }

    override fun initializeWeights(matrix: Matrix) {
        val randomizer = createRandomizer(matrix.ncol(), matrix.nrow())
        matrix.setValuesInPlace { _, _ -> randomizer.sampleDouble() }
    }

    override fun initializeWeights(weightMatrix: WeightMatrix) {
        val randomizer = createRandomizer(weightMatrix.source.size, weightMatrix.target.size)
        weightMatrix.randomize(randomizer)
    }

    override fun copy(): Xavier {
        return Xavier(seed).also {
            it.distribution = distribution
        }
    }
}

class He(seed: Long? = null): WeightInitializationStrategy(seed) {

    enum class Distribution {
        UNIFORM, NORMAL
    }

    @UserParameter("Distribution")
    var distribution = Distribution.UNIFORM

    private fun createRandomizer(numInputs: Int): ProbabilityDistribution {
        return when (distribution) {
            Distribution.UNIFORM -> UniformRealDistribution(-sqrt(6.0 / numInputs), sqrt(6.0 / numInputs))
            Distribution.NORMAL -> NormalDistribution(0.0, sqrt(2.0 / numInputs))
        }.apply { randomSeed = seed }
    }

    override fun initializeWeights(matrix: Matrix) {
        val randomizer = createRandomizer(matrix.ncol())
        matrix.setValuesInPlace { _, _ -> randomizer.sampleDouble() }
    }

    override fun initializeWeights(weightMatrix: WeightMatrix) {
        val randomizer = createRandomizer(weightMatrix.source.size)
        weightMatrix.randomize(randomizer)
    }

    override fun copy(): He {
        return He(seed).also {
            it.distribution = distribution
        }
    }
}

class LeCun(seed: Long? = null): WeightInitializationStrategy(seed) {

    enum class Distribution {
        UNIFORM, NORMAL
    }

    @UserParameter("Distribution")
    var distribution = Distribution.UNIFORM

    private fun createRandomizer(numInputs: Int): ProbabilityDistribution {
        return when (distribution) {
            Distribution.UNIFORM -> UniformRealDistribution(-sqrt(3.0 / numInputs), sqrt(3.0 / numInputs))
            Distribution.NORMAL -> NormalDistribution(0.0, sqrt(1.0 / numInputs))
        }.apply { randomSeed = seed }
    }

    override fun initializeWeights(matrix: Matrix) {
        val randomizer = createRandomizer(matrix.ncol())
        matrix.setValuesInPlace { _, _ -> randomizer.sampleDouble() }
    }

    override fun initializeWeights(weightMatrix: WeightMatrix) {
        val randomizer = createRandomizer(weightMatrix.source.size)
        weightMatrix.randomize(randomizer)
    }

    override fun copy(): LeCun {
        return LeCun(seed).also {
            it.distribution = distribution
        }
    }
}

class Randomize(seed: Long? = null): WeightInitializationStrategy(seed) {

    @UserParameter("Distribution", showDetails = false)
    var distribution: ProbabilityDistribution = NormalDistribution()

    override fun initializeWeights(matrix: Matrix) {
        matrix.setValuesInPlace { _, _ -> distribution.sampleDouble() }
    }

    override fun initializeWeights(weightMatrix: WeightMatrix) {
        weightMatrix.randomize(distribution)
    }

    override fun copy(): Randomize {
        return Randomize(seed).also {
            it.distribution = distribution.copy()
        }
    }
}


