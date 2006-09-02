/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.gauge.graphics;

import java.awt.Color;
import java.awt.geom.Ellipse2D;

import org.simbrain.util.Utils;

import edu.umd.cs.piccolo.nodes.PPath;


/**
 * <b>PNodeDatapoint</b> is a Piccolo PNode representing a (projected) point in the dataset.
 */
public class PNodeDatapoint extends PPath {

    /** Index. */
    private int index = 0;

    /** Reference to parent panel. */
    private GaugePanel gaugePanel;

    /**
     * Piccolo node data points. (Currently only handles 2-d points)
     * @param point current point
     * @param i index
     * @param size size of datapoint
     * @param gp reference to gauge panel
     */
    public PNodeDatapoint(final GaugePanel gp, final double[] point, final int i, final double size) {
        super(new Ellipse2D.Float((float) point[0], (float) -point[1], (float) size, (float) size), null);
        index = i;
        gaugePanel = gp;

        addInputEventListener(new ToolTipTextUpdater() {
           protected String getUpstairsText() {
               return PNodeDatapoint.this.toString();
            }
           protected String getDownstairsText() {
               return PNodeDatapoint.this.toStringDownstairs();
            }
        });
    }

    /**
     * @param c Color to be set.
     */
    public void setColor(final Color c) {
        this.setPaint(c);
    }

    /**
     * @return index of this datapoint in the associated dataset.
     */
    public int getIndex() {
        return index;
    }

    /** @see Object */
    public String toString() {
        return Utils.doubleArrayToString(gaugePanel.getGauge().getUpstairs().getPoint(index));
    }

    /**
     * Returns string representation of the low dimensional coordinates of this point.
     * High dimensional cooridnates are returned by default using toString()
     *
     * @return the low dimensnional coordinates
     */
    protected String toStringDownstairs() {
        return Utils.doubleArrayToString(gaugePanel.getGauge().getDownstairs().getPoint(index));
    }
}
