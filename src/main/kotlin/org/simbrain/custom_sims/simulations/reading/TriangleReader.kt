/** Feed-forward training and evaluation logic kept independent of the reading simulation UI. */
package org.simbrain.custom_sims.simulations.reading

import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.toDoubleArray

data class ReadingResult(
    val word: ReadingWord,
    val error: Double,
    val decodedPronunciation: List<String>
)

class TriangleReader(
    private val network: Network,
    val model: BackpropNetwork,
    val dataset: ReadingDataset,
    val orthography: OrthographyEncoder = OrthographyEncoder(),
    val phonology: PhonologyEncoder = PhonologyEncoder(),
    seed: Int = DEFAULT_SEED
) {
    private val sampler = WordSampler(dataset, seed)
    private val trainer = SupervisedTrainer(network, model)
    private var baselineErrors = emptyMap<String, Double>()

    var trainingPresentations = 0
        private set

    init {
        require(model.inputLayer.size == orthography.dimension) { "Input layer does not match orthography" }
        require(model.outputLayer.size == phonology.dimension) { "Output layer does not match phonology" }
        model.trainingSet = TrainingDataset(
            inputs = dataset.words.map { orthography.encode(it.word).toMutableList() }.toMutableList(),
            targets = dataset.words.map { phonology.encode(it.pronunciation).toMutableList() }.toMutableList(),
            inputRowNames = dataset.words.map { it.word },
            targetRowNames = dataset.words.map { it.word }
        )
        captureBaselineErrors()
    }

    fun reset(seed: Int = DEFAULT_SEED) {
        sampler.reset(seed)
        model.initWeights()
        model.initBiases()
        with(trainer) { model.trainerConfig.optimizer.reset() }
        trainingPresentations = 0
        captureBaselineErrors()
    }

    fun trainSampled(samples: Int, mode: SamplingMode): Double {
        require(samples > 0) { "Training samples must be positive" }
        var error = 0.0
        repeat(samples) {
            val index = sampler.nextIndex(mode)
            error += trainer.trainBatch(index until index + 1)
        }
        trainingPresentations += samples
        return error / samples
    }

    fun evaluate(word: ReadingWord): ReadingResult {
        val target = phonology.encode(word.pronunciation)
        val output = with(network) {
            model.inputLayer.setActivations(orthography.encode(word.word))
            model.forwardPass()
            model.outputLayer.activations.toDoubleArray()
        }
        return ReadingResult(word, mse(output, target), phonology.decode(output))
    }

    fun evaluateAll() = dataset.words.map(::evaluate)

    fun relativeError(result: ReadingResult): Double = result.error / (baselineErrors[result.word.word] ?: 1.0)

    private fun captureBaselineErrors() {
        baselineErrors = dataset.words.associate { word -> word.word to evaluate(word).error }
    }

    private fun mse(output: DoubleArray, target: DoubleArray) = output.indices.sumOf {
        val difference = output[it] - target[it]
        difference * difference
    } / output.size

    companion object {
        const val DEFAULT_SEED = 42
    }
}
