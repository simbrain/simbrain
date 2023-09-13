/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Polygon
import java.awt.geom.Arc2D
import javax.swing.SwingUtilities

/**
 * PNode representation of a recurrent "green arrow" (representing a group of synapses) from a
 * [AbstractNeuronCollectionNode] to itself.
 *
 * @author Zoë
 */
class SynapseGroup2NodeRecurrent(group: SynapseGroup2Node) : PNode(), SynapseGroup2Node.Arrow {
    // TODO: If RecurrentArrow.kt is further developed, update this
    
    private val parent: SynapseGroup2Node = group
    private val arrowHead: PPath
    private var arcCurve: PPath
    private var strokeWidth: Float

    init {
        require(group.synapseGroup.isRecurrent()) { "Using a recurrent synapse node" + " for a non-recurrent synapse " +
                "group." }
        arrowHead = PPath.Float()
        arcCurve = PPath.Float()
        arrowHead.setStroke(null)
        // TODO: Below may look a bit better.   But then overlap is visible.   Need to find a way to nicely join the arc and head.
        //arrowHead.setTransparency(0.5f); 
        arrowHead.setPaint(Color.green)
        strokeWidth = (group.synapseGroup.source.maxDim / 6).toFloat()
        arcCurve.setStroke(BasicStroke(strokeWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER))
        arcCurve.setStrokePaint(Color.green)
        arcCurve.setTransparency(0.5f)
        arcCurve.setPaint(null)
        arcCurve.setVisible(true)
        arrowHead.setVisible(true)
        addChild(arcCurve)
        addChild(arrowHead)
        visible = true
    }

    @Synchronized
    override fun layoutChildren() {
        val ng = parent.synapseGroup.source
        val quarterSizeX = Math.abs(ng.maxX - ng.minX).toFloat() / 4
        val quarterSizeY = Math.abs(ng.maxY - ng.minY).toFloat() / 4
        var quarterSize = if (quarterSizeX < quarterSizeY) quarterSizeX else quarterSizeY
        var qRatio: Float
        if (quarterSize == 0f) { // LineLayout
            quarterSize = ng.maxDim.toFloat() / 15.0f
        } else if (quarterSize < 30.0f) {
            quarterSize = 30.0f
        }
        qRatio = quarterSize / (quarterSizeX + quarterSizeY)
        if (java.lang.Float.isNaN(qRatio)) {
            qRatio = 1f
        }
        val recArc = Arc2D.Float(
            ng.centerX.toFloat() - 3 * quarterSize / 2,
            ng.centerY.toFloat() - 3 * quarterSize / 2,
            quarterSize * 3,
            quarterSize * 3,
            30.0f,
            300.0f,
            Arc2D.OPEN
        )
        strokeWidth = (qRatio * (ng.width + ng.height) / 6).toFloat()
        arcCurve.stroke = BasicStroke(strokeWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER)
        arcCurve.reset()
        arcCurve.append(recArc, false)
        arrowHead.reset()
        val endAng = -(11.0 * Math.PI / 6.0)
        arrowHead.append(
            traceArrowHead(
                endAng - 3.1 * Math.PI / 6.0,
                recArc.endPoint.x + 0.9 * strokeWidth,
                recArc.endPoint.y - 0.9 * 2 * strokeWidth
            ), false
        )
        SwingUtilities.invokeLater { parent.interactionBox.centerFullBoundsOnPoint(recArc.centerX, recArc.centerY) }
    }

    private fun traceArrowHead(theta: Double, tarX: Double, tarY: Double): Polygon {
        val numSides = 3
        val triPtx = IntArray(numSides)
        val triPty = IntArray(numSides)
        val phi = Math.PI / 6
        triPtx[0] = (tarX - strokeWidth / 2 * Math.cos(theta)).toInt()
        triPty[0] = (tarY - strokeWidth / 2 * Math.sin(theta)).toInt()
        triPtx[1] = (tarX - 2 * strokeWidth * Math.cos(theta + phi)).toInt()
        triPty[1] = (tarY - 2 * strokeWidth * Math.sin(theta + phi)).toInt()
        triPtx[2] = (tarX - 2 * strokeWidth * Math.cos(theta - phi)).toInt()
        triPty[2] = (tarY - 2 * strokeWidth * Math.sin(theta - phi)).toInt()
        return Polygon(triPtx, triPty, numSides)
    }

    @Synchronized
    override fun removeFromParent() {
        super.removeFromParent()
    }
}