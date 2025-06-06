package org.simbrain.network.trainers

import org.simbrain.util.DependenciesInvalidatingCachedObject
import org.simbrain.util.stats.distributions.NormalDistribution
import smile.data.DataFrame
import kotlin.random.Random

fun makeBlobs(
    nSamples: Int,
    nCenters: Int,
    nFeatures: Int,
    clusterStd:  Double,
    encoding: ClassificationDatasetEncoding = ClassificationDatasetEncoding.Integer,
    randomSeed: Long = 42L
): ClassificationDataset {
    // Validate inputs for Bipolar encoding
    if (encoding == ClassificationDatasetEncoding.Bipolar) {
        require(nCenters == 2) { "Bipolar encoding requires exactly 2 centers, but got $nCenters" }
    }
    
    val normalDistribution = NormalDistribution(0.0, clusterStd).apply { this.randomSeed = randomSeed }
    val centerDistribution = NormalDistribution(0.0, 10.0).apply { this.randomSeed = randomSeed }
    
    // Generate random centers for each cluster
    val centers = Array(nCenters) { DoubleArray(nFeatures) }
    for (i in 0 until nCenters) {
        for (j in 0 until nFeatures) {
            centers[i][j] = centerDistribution.sampleDouble() // Random center positions
        }
    }
    
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<Int>()
    
    // Generate samples for each cluster
    val samplesPerCluster = nSamples / nCenters
    val remainingSamples = nSamples % nCenters
    
    for (clusterId in 0 until nCenters) {
        val numSamplesForThisCluster = samplesPerCluster + if (clusterId < remainingSamples) 1 else 0
        
        repeat(numSamplesForThisCluster) {
            val sample = mutableListOf<Double>()
            for (featureIdx in 0 until nFeatures) {
                // Add noise around the cluster center
                val value = centers[clusterId][featureIdx] + normalDistribution.sampleDouble()
                sample.add(value)
            }
            inputs.add(sample)
            
            // Assign target based on encoding
            val target = when (encoding) {
                ClassificationDatasetEncoding.Bipolar -> if (clusterId == 0) -1 else 1
                ClassificationDatasetEncoding.Integer -> clusterId
            }
            targets.add(target)
        }
    }
    
    return createClassificationDataset(inputs, targets, encoding)
}

/**
 * Create classification dataset using a Smile [DataFrame] object (https://haifengl.github.io/data.html)
 */
fun createClassificationDataset(
    dataFrame: DataFrame,
    inputColumns: IntArray,
    targetColumn: Int
): ClassificationDataset {
    val inputs = dataFrame.select(*inputColumns).toMatrix().toArray().map { it.toMutableList() }.toMutableList()
    val targetLabels = dataFrame.column(targetColumn).toStringArray().toMutableList()

   val targetLabelMap = targetLabels.toSortedSet().mapIndexed { index, label -> label to index }.toMap()

    return ClassificationDataset(
        inputs = inputs,
        targets = targetLabels.map { targetLabelMap[it]!! }.toMutableList(),
    )

}

fun createClassificationDataset(
    inputs: MutableList<MutableList<Double>>,
    targets: MutableList<Int>,
    encoding: ClassificationDatasetEncoding = ClassificationDatasetEncoding.Integer
) = ClassificationDataset(
    inputs,
    targets
)

fun ClassificationDataset.split(ratio: Double = 0.8, seed: Long = 42L): Pair<ClassificationDataset, ClassificationDataset> {
    val inputs = this.inputs.shuffled(Random(seed))
    val targets = this.targets.shuffled(Random(seed))
    val splitIndex = (ratio * inputs.size).toInt()
    val trainingData = ClassificationDataset(
        inputs.subList(0, splitIndex).map { it.toMutableList() }.toMutableList(),
        targets.subList(0, splitIndex).toMutableList(),
    )
    val testData = ClassificationDataset(
        inputs.subList(splitIndex, inputs.size).map { it.toMutableList() }.toMutableList(),
        targets.subList(splitIndex, targets.size).toMutableList(),
    )
    return trainingData to testData
}

enum class ClassificationDatasetEncoding(val description: String) {
    Bipolar ("-1/1"),
    Integer("0,1,...")
}

class ClassificationDataset(
    val inputs: MutableList<MutableList<Double>>,
    val targets: MutableList<Int>
) {
    val numClasses by DependenciesInvalidatingCachedObject(::targets) { targets.toSet().size }

    val inputArrays: Array<DoubleArray> by DependenciesInvalidatingCachedObject(::inputs) { inputs.map { it.toDoubleArray() }.toTypedArray() }

    val targetArray: IntArray by DependenciesInvalidatingCachedObject(::targets) { targets.toIntArray() }

    fun copy() = ClassificationDataset(
        inputs.map { it.toMutableList() }.toMutableList(),
        targets.toMutableList()
    )
}
