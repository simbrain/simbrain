package org.simbrain.network.trainers

import org.simbrain.util.getDiagonal2DDoubleArray

/**
 * Encapsulates a 2d array of feature vectors, and for each of these an integer class label.
 *
 * Used in training classifiers.
 */
class ClassificationDataset(numFeatures: Int, numSamples: Int) {

    /**
     * A 2d array. Rows correspond to feature vectors
     *
     * Xor example: [[0,0],[1,0],[0,1],[1,1]]
     */
    var featureVectors: Array<DoubleArray> = getDiagonal2DDoubleArray(numSamples, numFeatures)

    /**
     * Associates each row of trainingInputs with a classification into one of a set of categories. These can be
     * represented in different ways depending on the classifier.
     *
     * Xor example: [-1,1,1,-1]
     *
     */
    var targets: IntArray = IntArray(numSamples)

}