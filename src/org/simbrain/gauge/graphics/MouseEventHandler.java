/*
 * Created on Apr 2, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.gauge.graphics;

import org.simbrain.gauge.core.Utils;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * <b>MouseEventHandler</b> handles mouse events, which in HiSee are relatively limited.  In addition to
 * panning and zooming the dataset (which is handled separately by Piccolo, and not this class), users may
 * click on datapoints to print out their coordinates.
 */
public class MouseEventHandler extends PDragSequenceEventHandler {

	GaugePanel gp;
	
	final PCamera camera;
	final PText tooltipNode = new PText("Test");

	public MouseEventHandler(GaugePanel gauge) {
		gp = gauge;
		
		camera = gp.getCamera();

		tooltipNode.setBounds(10,10,20,20);
		gp.getLayer().addChild(tooltipNode);
		
		
	}

	public void mousePressed(PInputEvent e) {
		super.mousePressed(e);
		PNode theNode = e.getPickedNode();
		if (theNode instanceof PNodeDatapoint) {
			int i = ((PNodeDatapoint) theNode).getIndex();
			System.out.println(Utils.doubleArrayToString(gp.getGauge().getUpstairs().getPoint(i)));
		}
	}

	public void mouseMoved(PInputEvent event) {
		updateToolTip(event);
	}

	public void mouseDragged(PInputEvent event) {
		updateToolTip(event);
	}

	public void updateToolTip(PInputEvent event) {
		PNode theNode = event.getInputManager().getMouseOver().getPickedNode();
		if (theNode instanceof PNodeDatapoint) {
							
		}		
	}
}
