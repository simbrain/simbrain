package org.simbrain.network.trainers

import org.simbrain.network.core.WeightMatrix
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import kotlin.math.sqrt

enum class Distribution {
    UNIFORM, NORMAL
}
sealed class WeightInitializationStrategy(val seed: Long? = null, val distribution: Distribution = Distribution.UNIFORM): CopyableObject {
    abstract fun initializeWeights(weightMatrix: WeightMatrix)

    override fun getTypeList(): List<Class<out CopyableObject>> = listOf(
        Xavier::class.java,
        He::class.java,
        LeCun::class.java
    )
}

class Xavier(seed: Long? = null, distribution: Distribution = Distribution.UNIFORM): WeightInitializationStrategy(seed) {

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
        return Xavier(seed)
    }
}

class He(seed: Long? = null, distribution: Distribution = Distribution.UNIFORM): WeightInitializationStrategy(seed) {


    override fun initializeWeights(weightMatrix: WeightMatrix) {
        val numInputs = weightMatrix.src.size
        val randomizer = when (distribution){
            Distribution.UNIFORM -> UniformRealDistribution(-sqrt(6.0 / numInputs), sqrt(6.0 / numInputs))
            Distribution.NORMAL -> NormalDistribution(0.0, sqrt(2.0/ numInputs))
        }.apply {randomSeed = seed}
        weightMatrix.randomize(randomizer)
    }

    override fun copy(): CopyableObject {
        return He(seed)
    }
}

class LeCun(seed: Long? = null, distribution: Distribution = Distribution.UNIFORM): WeightInitializationStrategy(seed) {

    override fun initializeWeights(weightMatrix: WeightMatrix) {
        val numInputs = weightMatrix.src.size
        val randomizer = when (distribution){
            Distribution.UNIFORM -> UniformRealDistribution(-sqrt(3.0 / numInputs), sqrt(3.0 / numInputs))
            Distribution.NORMAL -> NormalDistribution(0.0, sqrt(1.0/ numInputs))
        }.apply {randomSeed = seed}
        weightMatrix.randomize(randomizer)
    }

    override fun copy(): CopyableObject {
        return LeCun(seed)
    }
}

class Direct(seed: Long? = null, distribution: Distribution = Distribution.UNIFORM, val mean: Double = 0.0, val stddev: Double = 0.05): WeightInitializationStrategy(seed) {
    override fun initializeWeights(weightMatrix: WeightMatrix) {
        val numInputs = weightMatrix.src.size
        val randomizer = when (distribution){
            Distribution.UNIFORM -> UniformRealDistribution(-stddev, stddev)
            Distribution.NORMAL -> NormalDistribution(mean, stddev)
        }.apply {randomSeed = seed}
        weightMatrix.randomize(randomizer)
    }

    override fun copy(): CopyableObject {
        return Direct(seed)
    }
}


