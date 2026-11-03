package org.simbrain.network.smile

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.network.smile.classifiers.RandomForestClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.trainers.*
import smile.io.Read

class ClassifierTypesTest {

    private fun irisDataset() = createClassificationDataset(
        Read.arff("simulations/tables/iris.arff"), intArrayOf(0, 1, 2, 3), 4
    )

    private fun <T : SimbrainClassifier> T.trained(): T = apply { fitTrainingData() }

    @Test
    fun `random forest learns iris`() {
        val forest = RandomForestClassifier(irisDataset()).trained()
        assertTrue(forest.testingAccuracy > 0.85)
        assertEquals(3, forest.getOutputArray(forest.predict(forest.testingData.inputArrays[0])).size)
    }

    @Test
    fun `random forest with depth one makes a single split per tree`() {
        val forest = RandomForestClassifier(makeTwoClusters()).apply { maxDepth = 1 }.trained()
        assertEquals(1.0, forest.trainingAccuracy)
    }

    @Test
    fun `a one tree forest predicts what its rules say`() {
        val forest = RandomForestClassifier(makeMoons(200, noise = 0.25)).apply {
            numTrees = 1
            maxDepth = 1
        }.trained()
        val split = forest.tree(0) as RandomForestClassifier.TreeRule.Split
        val belowClass = (split.ifTrue as RandomForestClassifier.TreeRule.Leaf).label
        val aboveClass = (split.ifFalse as RandomForestClassifier.TreeRule.Leaf).label
        val below = DoubleArray(2).also { it[split.feature] = split.threshold - 0.05 }
        val above = DoubleArray(2).also { it[split.feature] = split.threshold + 0.05 }
        assertEquals(belowClass, forest.predict(below))
        assertEquals(aboveClass, forest.predict(above))
    }

    @Test
    fun `svm handles more than two classes`() {
        val svm = SVMClassifier(irisDataset()).trained()
        assertTrue(svm.testingAccuracy > 0.85)
        val prediction = svm.predict(svm.testingData.inputArrays[0])
        assertEquals(1.0, svm.getOutputArray(prediction).sum())
    }

    @Test
    fun `svm accepts zero and one labels and reports them`() {
        val svm = SVMClassifier(makeTwoClusters()).trained()
        assertEquals(1.0, svm.trainingAccuracy)
        assertEquals(setOf(0, 1), svm.trainingData.inputArrays.map { svm.predict(it) }.toSet())
        assertArrayEquals(doubleArrayOf(0.0, 1.0), svm.getOutputArray(1))
    }

    @Test
    fun `svm maps bipolar labels onto outputs in order`() {
        val dataset = makeTwoClusters().let { ClassificationDataset(it.inputs, it.targets.map { t -> 2 * t - 1 }.toMutableList()) }
        val svm = SVMClassifier(dataset).trained()
        assertArrayEquals(doubleArrayOf(1.0, 0.0), svm.getOutputArray(-1))
        assertArrayEquals(doubleArrayOf(0.0, 1.0), svm.getOutputArray(1))
    }

    @Test
    fun `knn with k of one fits its training data exactly`() {
        val knn = KNNClassifier(makeMoons(200, noise = 0.3)).apply { k = 1 }.trained()
        assertEquals(1.0, knn.trainingAccuracy)
        assertEquals("Accuracy: 1.000", knn.trainingStats)
    }

    @Test
    fun `classifier network accepts a replacement classifier of the same shape only`() {
        val network = ClassifierNetwork(LogisticRegClassifier(makeMoons(100)).trained())
        val replacement = KNNClassifier(makeCircles(100)).trained()
        network.classifier = replacement
        assertSame(replacement, network.classifier)
        assertThrows<IllegalArgumentException> {
            network.classifier = KNNClassifier(irisDataset()).trained()
        }
    }

    @Test
    fun `toy datasets have two balanced classes in two dimensions`() {
        listOf(makeMoons(200), makeCircles(200), makeXorClusters(200)).forEach { dataset ->
            assertEquals(200, dataset.inputs.size)
            assertTrue(dataset.inputs.all { it.size == 2 })
            assertEquals(100, dataset.targets.count { it == 0 })
            assertEquals(100, dataset.targets.count { it == 1 })
        }
    }

    @Test
    fun `standardized features have mean zero and unit standard deviation`() {
        val standardized = irisDataset().standardized()
        (0 until 4).forEach { column ->
            val values = standardized.inputs.map { it[column] }
            val mean = values.average()
            val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
            assertEquals(0.0, mean, 1e-9)
            assertEquals(1.0, variance, 1e-9)
        }
    }

    private fun makeTwoClusters() = makeBlobs(
        40, 2, 2, 0.2,
        centers = listOf(doubleArrayOf(-1.0, 0.0), doubleArrayOf(1.0, 0.0))
    )
}
