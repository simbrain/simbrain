package org.simbrain.network.trainers

import org.simbrain.util.BiMap
import org.simbrain.util.getDiagonal2DDoubleArray

/**
 * Encapsulates a 2d array of feature vectors and a set of String labels used to train a classifier.
 *
 * String labels are used to set targets, and these are then associated with integers which can be retrieved using
 * [getIntegerTargets].
 *
 */
class ClassificationDataset(numFeatures: Int, numSamples: Int) {

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
    var targetLabels: Array<String> = Array(numSamples) { "" }
        set(value) {
            field  = value
            val labels = field.toSet()
            labelTargetMap.clear()
            if (labels.size == 2) {
                labelTargetMap[labels.first()] = -1
                labelTargetMap[labels.last()] = 1
            } else {
                labels.forEachIndexed { i, label -> labelTargetMap[label] = i }
            }
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