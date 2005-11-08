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

import org.simbrain.util.Utils;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PText;


/**
 * <b>MouseEventHandler</b> handles mouse events, which in HiSee are relatively limited.  In addition to panning and
 * zooming the dataset (which is handled separately by Piccolo, and not this class), users may click on datapoints to
 * print out their coordinates.
 */
public class MouseEventHandler extends PDragSequenceEventHandler {
    private GaugePanel gp;
    final PCamera camera;
    final PText tooltipNode = new PText("Test");

    public MouseEventHandler(final GaugePanel gauge) {
        gp = gauge;

        camera = gp.getCamera();
    }

    public void mousePressed(final PInputEvent e) {
        super.mousePressed(e);

        PNode theNode = e.getPickedNode();

        if (theNode instanceof PNodeDatapoint) {
            int i = ((PNodeDatapoint) theNode).getIndex();
            System.out.println(Utils.doubleArrayToString(gp.getGauge().getUpstairs().getPoint(i)));
        }
    }

    public void mouseMoved(final PInputEvent event) {
        updateToolTip(event);
    }

    public void mouseDragged(final PInputEvent event) {
        updateToolTip(event);
    }

    public void updateToolTip(final PInputEvent event) {
        PNode theNode = event.getInputManager().getMouseOver().getPickedNode();

        if (theNode instanceof PNodeDatapoint) {
        }
    }
    public void setGp(final GaugePanel gp) {
        this.gp = gp;
    }

    public GaugePanel getGp() {
        return this.gp;
    }
}
