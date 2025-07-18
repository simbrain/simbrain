/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.subnetworks

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.point
import smile.math.matrix.Matrix
import java.awt.geom.Point2D
import kotlin.math.ceil
import kotlin.math.min

/**
 * Backprop network.
 *
 * @author Jeff Yoshimi
 */
class BackpropNetwork : FeedForward, SupervisedNetwork {

    @delegate:Transient
    override val layers: LinkedHashSet<Layer> by lazy {
        computeOrderedUpdatePath(setOf(inputLayer), outputLayer)
    }

    constructor(nodesPerLayer: IntArray, initialPosition: Point2D? = point(0,0)): super(nodesPerLayer, initialPosition) {
        layerList.forEach { it.updateRule = LinearRule() }
        inputLayer.isClamped = true
        hiddenLayers().forEach {
            it.updateRule = SigmoidalRule().apply {
                type = SigmoidFunctionEnum.LOGISTIC
            }
        }
        // Good default for regression tasks
        outputLayer.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.LOGISTIC
        }
        val nin = nodesPerLayer.first()
        val nout = nodesPerLayer.last()
        trainingSet = createDiagonalDataset(nin, nout, min(nin,nout))
        testingSet = MatrixDataset(
            inputs = Matrix(ceil(trainingSet.inputs.nrow() * 0.2).toInt(), trainingSet.inputs.ncol()),
            targets = Matrix(ceil(trainingSet.targets.nrow() * 0.2).toInt(), trainingSet.targets.ncol())
        )
    }

    @XStreamConstructor()
    private constructor() : super()

    override lateinit var trainingSet: MatrixDataset

    override lateinit var testingSet: MatrixDataset

    override var trainerConfig = SupervisedTrainerConfig(lossFunctionProvider = ::possibleLossFunctions)

    override fun initWeights() {
        wmList.forEach { wm -> trainerConfig.weightInitializationStrategy.initializeWeights(wm) }
    }

    override fun initBiases() {
        (layerList - inputLayer).forEach {
            it.clear()
            it.randomizeBiases()
        }
    }

    context(Network)
    override fun forwardPass() {
        layers.forwardPass(listOf(inputLayer.activations), listOf(inputLayer))
    }

    override fun copy(): BackpropNetwork {
        // Create a new instance with same structure
        val nodesPerLayer = layerList.map { it.size }.toIntArray()
        val copy = BackpropNetwork(nodesPerLayer)

        // Copy weights from original network
        for (i in wmList.indices) {
            copy.wmList[i].copyFrom(wmList[i])
        }

        // Copy training related properties
        copy.trainingSet = MatrixDataset(
            inputs = trainingSet.inputs.clone(),
            targets = trainingSet.targets.clone()
        )
        copy.testingSet = MatrixDataset(
            inputs = testingSet.inputs.clone(),
            targets = testingSet.targets.clone()
        )
        copy.trainerConfig = SupervisedTrainerConfig().copy()

        return copy
    }

}
