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

import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.trainers.BackpropTrainer
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedNetwork
import org.simbrain.network.trainers.createDiagonalDataset
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.point
import java.awt.geom.Point2D
import kotlin.math.min

/**
 * Backprop network.
 *
 * @author Jeff Yoshimi
 */
open class BackpropNetwork : FeedForward, SupervisedNetwork {

    constructor(nodesPerLayer: IntArray, initialPosition: Point2D? = point(0,0)): super(nodesPerLayer, initialPosition) {
        layerList.forEach { it.updateRule = LinearRule() }
        inputLayer.isClamped = true
        // hiddenLayers().forEach{(it.updateRule as LinearRule).clippingType = LinearRule.ClippingType.Relu}
        hiddenLayers().forEach {
            it.updateRule = SigmoidalRule().apply {
                type = SigmoidFunctionEnum.LOGISTIC
            }
        }
        outputLayer.updateRule = LinearRule().apply {
            clippingType = LinearRule.ClippingType.NoClipping
        }
        val nin = nodesPerLayer.first()
        val nout = nodesPerLayer.last()
        trainingSet = createDiagonalDataset(nin, nout, min(nin,nout))
        label = "Backprop"
    }

    @XStreamConstructor()
    private constructor() : super()

    override lateinit var trainingSet: MatrixDataset

    override var trainer: BackpropTrainer = BackpropTrainer()

}