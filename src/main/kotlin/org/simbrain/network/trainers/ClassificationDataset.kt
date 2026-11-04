package org.simbrain.network.trainers

import org.simbrain.util.DependenciesInvalidatingCachedObject
import org.simbrain.util.stats.distributions.NormalDistribution
import smile.data.DataFrame
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import java.util.Random as JavaRandom

/**
 * Gaussian clusters, one class per cluster.
 *
 * @param centers cluster centers, each with [nFeatures] values; by default random, drawn with standard deviation 10
 */
fun makeBlobs(
    nSamples: Int,
    nCenters: Int,
    nFeatures: Int,
    clusterStd:  Double,
    encoding: ClassificationDatasetEncoding = ClassificationDatasetEncoding.Integer,
    randomSeed: Long = 42L,
    centers: List<DoubleArray>? = null
): ClassificationDataset {
    // Validate inputs for Bipolar encoding
    if (encoding == ClassificationDatasetEncoding.Bipolar) {
        require(nCenters == 2) { "Bipolar encoding requires exactly 2 centers, but got $nCenters" }
    }
    
    val normalDistribution = NormalDistribution(0.0, clusterStd).apply { this.randomSeed = randomSeed }
    val centerDistribution = NormalDistribution(0.0, 10.0).apply { this.randomSeed = randomSeed }
    
    // Generate random centers for each cluster unless they are given
    val centers = centers ?: List(nCenters) { DoubleArray(nFeatures) { centerDistribution.sampleDouble() } }
    require(centers.size == nCenters && centers.all { it.size == nFeatures }) {
        "Expected $nCenters centers with $nFeatures values each"
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
 * Two interleaving half circles in 2D, a standard dataset for nonlinear classifiers. Class 0 is the upper moon.
 *
 * @param noise standard deviation of Gaussian noise added to each coordinate
 */
fun makeMoons(nSamples: Int, noise: Double = 0.1, randomSeed: Long = 42L): ClassificationDataset {
    val random = JavaRandom(randomSeed)
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<Int>()
    repeat(nSamples) { i ->
        val label = i % 2
        val angle = random.nextDouble() * Math.PI
        val (x, y) = if (label == 0) {
            cos(angle) to sin(angle)
        } else {
            1 - cos(angle) to 0.5 - sin(angle)
        }
        inputs.add(mutableListOf(x + noise * random.nextGaussian(), y + noise * random.nextGaussian()))
        targets.add(label)
    }
    return ClassificationDataset(inputs, targets)
}

/**
 * A small circle of class 1 inside a larger ring of class 0, in 2D. No straight line can separate them.
 *
 * @param factor radius of the inner circle relative to the outer ring
 * @param noise standard deviation of Gaussian noise added to each coordinate
 */
fun makeCircles(nSamples: Int, factor: Double = 0.4, noise: Double = 0.1, randomSeed: Long = 42L): ClassificationDataset {
    val random = JavaRandom(randomSeed)
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<Int>()
    repeat(nSamples) { i ->
        val label = i % 2
        val radius = if (label == 0) 1.0 else factor
        val angle = random.nextDouble() * 2 * Math.PI
        inputs.add(mutableListOf(
            radius * cos(angle) + noise * random.nextGaussian(),
            radius * sin(angle) + noise * random.nextGaussian()
        ))
        targets.add(label)
    }
    return ClassificationDataset(inputs, targets)
}

/**
 * Four clusters at the corners of a square in 2D, labeled like exclusive or: diagonal corners share a class. No
 * straight line can separate them.
 *
 * @param noise standard deviation of each cluster
 */
fun makeXorClusters(nSamples: Int, noise: Double = 0.3, randomSeed: Long = 42L): ClassificationDataset {
    val corners = listOf(doubleArrayOf(-1.0, -1.0), doubleArrayOf(1.0, 1.0), doubleArrayOf(-1.0, 1.0), doubleArrayOf(1.0, -1.0))
    val clusters = makeBlobs(nSamples, 4, 2, noise, randomSeed = randomSeed, centers = corners)
    return ClassificationDataset(clusters.inputs, clusters.targets.map { it / 2 }.toMutableList())
}

/**
 * Copy of this dataset with each input column rescaled to mean 0 and standard deviation 1, so that features measured
 * in large units do not dominate distance-based classifiers.
 *
 * @param reference data the means and standard deviations are computed from. Pass the training rows, so that no
 * information about the test rows reaches the model.
 */
fun ClassificationDataset.standardized(reference: ClassificationDataset = this): ClassificationDataset {
    val numFeatures = inputs.firstOrNull()?.size ?: return copy()
    val columns = List(numFeatures) { j -> reference.inputs.map { it[j] }.toDoubleArray() }
    val means = columns.map { it.average() }
    val sds = columns.mapIndexed { j, column ->
        sqrt(column.sumOf { (it - means[j]) * (it - means[j]) } / column.size).takeIf { it > 0 } ?: 1.0
    }
    return ClassificationDataset(
        inputs.map { row -> MutableList(numFeatures) { j -> (row[j] - means[j]) / sds[j] } }.toMutableList(),
        targets.toMutableList()
    )
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
