/**
 * Wrapper for Smile's random forest, which trains on a data frame. The wrapper converts arrays to a data frame for
 * fitting and adapts the fitted forest to the array-based [Classifier] interface that [SimbrainClassifier] expects.
 */
package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.SimbrainClassifier
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.util.UserParameter
import smile.base.cart.DecisionNode
import smile.base.cart.Node
import smile.base.cart.OrdinalNode
import smile.base.cart.SplitRule
import smile.classification.Classifier
import smile.classification.RandomForest
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.formula.Formula
import smile.data.type.StructType
import smile.data.vector.IntVector
import smile.validation.metric.Accuracy
import java.lang.reflect.Field
import kotlin.math.sqrt

class RandomForestClassifier(
    dataset: ClassificationDataset = createClassificationDataset(
        inputs = mutableListOf<MutableList<Double>>(),
        targets = mutableListOf<Int>()
    ),
    splitRatio: Double = 0.8
) : SimbrainClassifier(dataset, splitRatio) {

    @UserParameter(label = "Number of trees", minimumValue = 1.0, order = 10)
    var numTrees = 100

    @UserParameter(
        label = "Max depth",
        description = "Maximum number of splits from the root of each tree to a leaf. Shallow trees give simpler, " +
                "blockier decision regions.",
        minimumValue = 1.0,
        order = 20
    )
    var maxDepth = 20

    @UserParameter(
        label = "Min samples per leaf",
        description = "Nodes with fewer samples than this are not split further.",
        minimumValue = 1.0,
        order = 30
    )
    var nodeSize = 1

    @UserParameter(label = "Split rule", order = 40)
    var splitRule = SplitRule.GINI

    @UserParameter(
        label = "Random seed",
        description = "Seed for the bootstrap samples and feature choices, so retraining gives the same forest.",
        order = 50
    )
    var seed = 42L

    override var model: Classifier<DoubleArray>? = null

    override val name = "Random Forest"

    /**
     * Number of trees in the trained forest, which can differ from [numTrees] if that was edited since training.
     */
    val trainedTreeCount get() = (model as? ForestModel)?.forest?.trees()?.size ?: 0

    /**
     * The [maxDepth] the current forest was trained with.
     */
    var trainedMaxDepth = maxDepth
        private set

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        val numFeatures = inputs.first().size
        val featureNames = Array(numFeatures) { "x$it" }
        val data = DataFrame.of(inputs, *featureNames).merge(IntVector.of("label", targets))
        val forest = RandomForest.fit(
            Formula.lhs("label"),
            data,
            numTrees,
            sqrt(numFeatures.toDouble()).toInt().coerceAtLeast(1),
            splitRule,
            // Smile counts the root as depth 1
            maxDepth + 1,
            inputs.size,
            nodeSize,
            1.0,
            null,
            java.util.stream.LongStream.range(seed, seed + numTrees)
        )
        model = ForestModel(forest, data.schema())
        trainedMaxDepth = maxDepth
        setTrainingStats(Accuracy.of(targets, model?.predict(inputs)))
        setTestingStats(Accuracy.of(testingData.targetArray, model?.predict(testingData.inputArrays)))
    }

    override fun predict(input: DoubleArray): Int {
        return model?.predict(input) ?: -1
    }

    /**
     * The rules of one tree of the trained forest, or null before training.
     */
    fun tree(index: Int): TreeRule? {
        val forest = (model as? ForestModel)?.forest ?: return null
        val tree = forest.trees().getOrNull(index) ?: return null
        // Trees in a forest share the forest's label encoding; leaf outputs index into it
        val labels = forest.classes()
        fun convert(node: Node): TreeRule = when (node) {
            is OrdinalNode -> TreeRule.Split(
                node.feature(),
                ordinalThreshold.getDouble(node),
                convert(node.trueChild()),
                convert(node.falseChild())
            )
            is DecisionNode -> node.count().let { TreeRule.Leaf(labels[node.output()], it[node.output()], it.sum()) }
            else -> throw IllegalStateException("Unexpected tree node $node")
        }
        return convert(tree.root())
    }

    /**
     * One decision tree as nested rules.
     */
    sealed interface TreeRule {
        /**
         * Follow [ifTrue] when input [feature] is at most [threshold], otherwise [ifFalse].
         */
        class Split(val feature: Int, val threshold: Double, val ifTrue: TreeRule, val ifFalse: TreeRule) : TreeRule

        /**
         * Predict [label]. Of the [total] points in the tree's resample of the training data that reach this leaf,
         * [count] have that label.
         */
        class Leaf(val label: Int, val count: Int, val total: Int) : TreeRule
    }

    override fun copy(): SimbrainClassifier {
        return RandomForestClassifier(dataset.copy()).also {
            it.numTrees = numTrees
            it.maxDepth = maxDepth
            it.nodeSize = nodeSize
            it.splitRule = splitRule
            it.seed = seed
        }
    }

    /**
     * Presents the forest as an array classifier. Smile's formula binds the response column even when predicting, so
     * each input is wrapped in a tuple of the full training schema with a placeholder label.
     */
    private class ForestModel(val forest: RandomForest, val schema: StructType) : Classifier<DoubleArray> {
        override fun numClasses() = forest.numClasses()
        override fun classes(): IntArray = forest.classes()
        override fun predict(x: DoubleArray): Int {
            val row = arrayOfNulls<Any>(x.size + 1)
            x.forEachIndexed { i, value -> row[i] = value }
            row[x.size] = 0
            return forest.predict(Tuple.of(row, schema))
        }
    }

    private companion object {
        /**
         * Smile keeps a split's threshold in a package-private field.
         */
        val ordinalThreshold: Field = OrdinalNode::class.java.getDeclaredField("value").apply { isAccessible = true }
    }

}
