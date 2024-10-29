package org.simbrain.network.trainers

import org.simbrain.network.core.WeightMatrix
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import kotlin.math.sqrt
import kotlin.random.Random


sealed class WeightInitializationStrategy(val seed: Long? = null): CopyableObject {
    abstract fun initializeWeights(weightMatrix: WeightMatrix)

    override fun getTypeList(): List<Class<out CopyableObject>> = listOf(
        Randomize::class.java,
        Xavier::class.java,
        He::class.java,
        LeCun::class.java
    )
}

class Xavier(seed: Long? = null): WeightInitializationStrategy(seed) {

    enum class Distribution {
        UNIFORM, NORMAL
    }

    @UserParameter("Distribution")
    var distribution = Distribution.UNIFORM

    override fun initializeWeights(weightMatrix: WeightMatrix) {
        val numInputs = weightMatrix.src.size
        val numOutputs = weightMatrix.tar.size
        val randomizer = when (distribution){
            Distribution.UNIFORM -> UniformRealDistribution(-sqrt(6.0 / (numInputs + numOutputs)), sqrt(6.0 / (numInputs + numOutputs)))
            Distribution.NORMAL -> NormalDistribution(0.0, sqrt(2.0/ (numInputs+ numOutputs)))
        }.apply {randomSeed = seed}
        weightMatrix.randomize(randomizer)
    }

    override fun copy(): CopyableObject {
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

    override fun initializeWeights(weightMatrix: WeightMatrix) {
        val numInputs = weightMatrix.src.size
        val randomizer = when (distribution){
            Distribution.UNIFORM -> UniformRealDistribution(-sqrt(6.0 / numInputs), sqrt(6.0 / numInputs))
            Distribution.NORMAL -> NormalDistribution(0.0, sqrt(2.0/ numInputs))
        }.apply {randomSeed = seed}
        weightMatrix.randomize(randomizer)
    }

    override fun copy(): CopyableObject {
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

    override fun initializeWeights(weightMatrix: WeightMatrix) {
        val numInputs = weightMatrix.src.size
        val randomizer = when (distribution){
            Distribution.UNIFORM -> UniformRealDistribution(-sqrt(3.0 / numInputs), sqrt(3.0 / numInputs))
            Distribution.NORMAL -> NormalDistribution(0.0, sqrt(1.0/ numInputs))
        }.apply {randomSeed = seed}
        weightMatrix.randomize(randomizer)
    }

    override fun copy(): CopyableObject {
        return LeCun(seed).also {
            it.distribution = distribution
        }
    }
}

class Randomize(seed: Long? = null): WeightInitializationStrategy(seed) {

    @UserParameter("Distribution", showDetails = false)
    var distribution: ProbabilityDistribution = NormalDistribution()

    override fun initializeWeights(weightMatrix: WeightMatrix) {
        weightMatrix.randomize(distribution)
    }

    override fun copy(): CopyableObject {
        return Randomize(seed).also {
            it.distribution = distribution.copy()
        }
    }
}


