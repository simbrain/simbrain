package org.simbrain.network.trainers

import org.simbrain.util.BiMap
import org.simbrain.util.getDiagonal2DDoubleArray
import org.simbrain.util.stats.distributions.UniformIntegerDistribution

/**
 * Encapsulates a 2d array of feature vectors and a set of String labels used to train a classifier.
 *
 * String labels are used to set targets, and these are then associated with integers which can be retrieved using
 * [getIntegerTargets].
 *
 */
class ClassificationDataset(val numFeatures: Int, val numOutputs: Int, val numSamples: Int) {

    /**
     * Method of associating string labels to integer indices.
     */
    enum class LabelEncoding(val description: String) {
        Bipolar ("-1/1"),
        Integer("0,1,...")}

    var labelEncoding = LabelEncoding.Integer

    /**
     * A 2d array. Rows correspond to feature vectors
     *
     * Xor example: [[0,0],[1,0],[0,1],[1,1]]
     */
    var featureVectors: Array<DoubleArray> = getDiagonal2DDoubleArray(numSamples, numFeatures)

    /**
     * String labels that the user interacts with. Unique labels are automatically associated with integers by the
     * [labelTargetMap], and those integers are what are used to train the underlying machine learning model.
     *
     * Xor example: ["F","T","T","F"]
     */
    var targetLabels: Array<String> = getRandStringLabels()
        set(value) {
            field  = value
            val labels = field.toSet()
            labelTargetMap.clear()
            if (labelEncoding == LabelEncoding.Bipolar) {
                if (labels.size == 1) {
                    labelTargetMap[labels.first()] = 1
                } else if (labels.size == 2) {
                    labelTargetMap[labels.first()] = -1
                    labelTargetMap[labels.last()] = 1
                } else {
                    throw IllegalArgumentException("Binary encodings require one or two labels, but ${labels.size} " +
                            "were provided")
                }
            } else {
                labels.forEachIndexed { i, label -> labelTargetMap[label] = i }
            }
        }

    /**
     * Returns a set of random labels based on number of possible outputs. "Class 1",...,"Class N". The user can of
     * course change these.
     */
    fun getRandStringLabels(): Array<String> {
        return (0 until numSamples).map { "Class " + UniformIntegerDistribution(floor = 1, ceil = numOutputs)
            .sampleInt()}
            .toTypedArray()
    }

    /**
     * Load a set of integer targets t1...tn. These will be associated with string target labels "Class t1"..."Class tn"
     * Also the integers will themselves be used in the [labelTargetMap]
     */
    fun setIntegerTargets(targets: IntArray) {
        targetLabels = targets.map{ i -> "Class $i" }.toTypedArray()
    }

    /**
     * Associates labels with integers.
     */
    var labelTargetMap = BiMap<String, Int>()

    /**
     * Get the integer targets associated with the target labels.
     */
    fun getIntegerTargets(): IntArray {
        return targetLabels.map { labelTargetMap[it]!!}.toIntArray()
    }

}