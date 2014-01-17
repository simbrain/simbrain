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
package org.simbrain.network.gui.nodes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;

import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PPaintContext;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;

/**
 * PNode representation of a group of synapses, where the synapses themselves
 * are not visible.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class SynapseGroupNodeSimple extends SynapseGroupNode {

	private static final float DEFAULT_ARROW_DISTANCE = 30;

	private static final float DEFAULT_ARROW_THICKNESS = 30;

	private static final float DEFAULT_ARROW_ANGLE = (float) (Math.PI/6);
	
	private static final Color DEFAULT_COLOR = Color.GREEN;
	
	private AffineTransform transform = new AffineTransform();

//	private ArrowHead arrowHead = new ArrowHead();
	
	private float arrowRotation;
	
	/** Line connecting nodes. */
	private PPath.Float curve;

	/** Arrow at end of curve. */
	private PPath.Float arrow;

	private final SynapseGroup group;

	/**
	 * Create a Synapse Group PNode.
	 *
	 * @param networkPanel parent panel
	 * @param group the synapse group
	 */
	public SynapseGroupNodeSimple(final NetworkPanel networkPanel,
			final SynapseGroup group) {
		super(networkPanel, group);
		this.group = group;
		curve = new PPath.Float();
		arrow = new PPath.Float();
		this.addChild(curve);
		this.addChild(arrow);
		if (group.isRecurrent()) {
			curve.setStroke(new BasicStroke(DEFAULT_ARROW_THICKNESS, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER));
		} else {
			curve.setStroke(new BasicStroke(DEFAULT_ARROW_THICKNESS, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_MITER));
		}
		curve.setTransparency(0.5f);
		curve.setStrokePaint(DEFAULT_COLOR);
		curve.raiseToTop();
		arrow.setPaint(DEFAULT_COLOR);
		arrow.setStroke(null);
		arrow.raiseToTop();
	}

	
//	
//	@Override
//	protected void paint(PPaintContext arg0) {
//		super.paint(arg0);
//		arrowHead.drawArrowHead(arg0.getGraphics());
//		
//	}



	/**
	 * Override PNode layoutChildren method in order to properly set the
	 * positions of children nodes.
	 */
	@Override
	public void layoutChildren() {
		if ((curve == null) || (arrow == null)) {
			return;
		}
		if (this.getSynapseGroup().isRecurrent()) {
			curve.reset();
			createArc(group);
		} else {

			float srcX = (float) synapseGroup.getSourceNeuronGroup().getCenterX();
			float srcY = (float) synapseGroup.getSourceNeuronGroup().getCenterY();
			float tarX = (float) synapseGroup.getTargetNeuronGroup().getCenterX();
			float tarY = (float) synapseGroup.getTargetNeuronGroup().getCenterY();

			float diffX = tarX - srcX;
			float diffY = tarY - srcY;

			int numSides = 3;
			int[] triPtx = new int[numSides];
			int[] triPty = new int[numSides];

			double theta = Math.atan(diffY / diffX);
			double phi = Math.PI / 6;

			int negCont = diffX < 0 ? -1 : 1;

			triPtx[0] = (int) (tarX - negCont
					* (DEFAULT_ARROW_DISTANCE * Math.cos(theta)));
			triPty[0] = (int) (tarY - negCont
					* (DEFAULT_ARROW_DISTANCE * Math.sin(theta)));

			triPtx[1] = (int) (tarX - negCont
					* (2 * DEFAULT_ARROW_DISTANCE * Math.cos(theta + phi)));
			triPty[1] = (int) (tarY - negCont
					* (2 * DEFAULT_ARROW_DISTANCE * Math.sin(theta + phi)));

			triPtx[2] = (int) (tarX - negCont
					* (2 * DEFAULT_ARROW_DISTANCE * Math.cos(theta - phi)));
			triPty[2] = (int) (tarY - negCont
					* (2 * DEFAULT_ARROW_DISTANCE * Math.sin(theta - phi)));

			float endX = (float) (tarX - negCont
					* (Math.sqrt(3) * DEFAULT_ARROW_DISTANCE * Math.cos(theta)));
			float endY = (float) (tarY - negCont
					* (Math.sqrt(3) * DEFAULT_ARROW_DISTANCE * Math.sin(theta)));

			float b2X = (float) (tarX - negCont
					* (3 * DEFAULT_ARROW_DISTANCE * Math.cos(theta)));
			float b2Y = (float) (tarY - negCont
					* (3 * DEFAULT_ARROW_DISTANCE * Math.sin(theta)));

			float x = (srcX + endX) / 2;
			float y = (srcY + endY) / 2;
			float slope = (endY - srcY) / (endX - srcX);
			float distanceToMidpoint = (float) Point2D.distance(srcX, srcY, x, y);
			float bez_x = (float) Math.sqrt(Math.pow(distanceToMidpoint, 2)
					/ (1 + Math.pow(1 / slope, 2)));

			float bez_y = bez_x * 1 / slope;
			if (srcX <= tarX) {
				bez_x = -bez_x;
			}
			if (srcX >= tarX) {
				bez_y = -bez_y;
			}
			CubicCurve2D.Float theCurve = new CubicCurve2D.Float(srcX, srcY, x
					+ bez_x, y + bez_y, b2X, b2Y, endX, endY);
			curve.reset();
			curve.append(theCurve, false);

			Polygon polyArrow = new Polygon(triPtx, triPty, numSides);
			arrow.reset();
			arrow.append(polyArrow, false);


			interactionBox.setOffset(x + bez_x / 2 - interactionBox.getWidth() / 2,
					y - interactionBox.getHeight() / 2);
			interactionBox.raiseToTop();
		}

		
		
	}

	private void createArc(SynapseGroup group) {
		if (!group.isRecurrent()) {
			throw new IllegalArgumentException("Using a recurrent synapse node"
					+ " for a non-recurrent synapse group.");
		}
		NeuronGroup ng = group.getSourceNeuronGroup();
		float halfSizeX = (float) (ng.getMaxX() - ng.getMinX()) / 2;
		float halfSizeY = (float) (ng.getMaxY() - ng.getMinX()) / 2;
		float halfSize = halfSizeX < halfSizeY ? halfSizeX : halfSizeY;
		Arc2D.Float recArc = new Arc2D.Float((float) ng.getMinX()
				+ halfSize/2, (float) ng.getMinY() + halfSize/2,
				halfSize, halfSize, 30, 300, Arc2D.OPEN);
//		float arrowHeight = (float) ((2 * DEFAULT_ARROW_DISTANCE
//				* Math.cos(DEFAULT_ARROW_ANGLE)) - DEFAULT_ARROW_DISTANCE);
//		float theta = (float)Math.tan(arrowHeight/(halfSize/2));
//		float phi = (float) Math.PI/6 - theta;
//		
//		float z = (float) Math.sqrt(Math.pow(arrowHeight, 2) + Math.pow(halfSize/2, 2));
//
//		float x_displacement = (float) (z * Math.cos(phi));
//		float y_displacement = (float) (z * Math.sin(phi));

//		arrowRotation = (float)-Math.PI/6;
//		
		
		curve.append(recArc, false);
		interactionBox.setOffset(ng.getCenterX() - interactionBox.getWidth()/2,
				ng.getCenterY() - interactionBox.getHeight()/2);
		interactionBox.raiseToTop();
		
//		arrowHead.setX_displacement(x_displacement + (float) recArc.getCenterX());
//		arrowHead.setY_displacement(y_displacement + (float) recArc.getCenterY());
//		arrowHead.setRotation(arrowRotation);
//		
	}

	private class ArrowHead   {
		
		private int[] default_pts_X = new int[] {0,
				(int) (2 * DEFAULT_ARROW_DISTANCE
						* Math.sin(DEFAULT_ARROW_ANGLE)),
						(int) (4 * DEFAULT_ARROW_DISTANCE
								* Math.sin(DEFAULT_ARROW_ANGLE))};
		
		private int[] default_pts_Y = new int[] {0, (int) ((2
				* DEFAULT_ARROW_DISTANCE * Math.cos(DEFAULT_ARROW_ANGLE))
				- DEFAULT_ARROW_DISTANCE), 0};
		
		private Polygon arrowTriangle;
		
		private AffineTransform transform;
		
		private float x_displacement;
		
		private float y_displacement;
		
		private float rotation;
		
		public ArrowHead() {
			arrowTriangle = new Polygon(default_pts_X, default_pts_Y, default_pts_X.length);
		}
		
		public void drawArrowHead(Graphics2D g2d) {
			transform = new AffineTransform();
			transform.setToIdentity();
			transform.translate(x_displacement, y_displacement);
			transform.rotate(rotation - Math.PI/2);
			Graphics2D g = (Graphics2D) g2d.create();
			g.setTransform(transform);
			g.setPaint(Color.RED);
			g.fill(arrowTriangle);
			g.dispose();
		}
		
		public float getX_displacement() {
			return x_displacement;
		}

		public void setX_displacement(float x_displacement) {
			this.x_displacement = x_displacement;
		}

		public float getY_displacement() {
			return y_displacement;
		}

		public void setY_displacement(float y_displacement) {
			this.y_displacement = y_displacement;
		}

		public float getRotation() {
			return rotation;
		}

		public void setRotation(float rotation) {
			this.rotation = rotation;
		}
		
	}
	
	// TODO: Not sure why below is needed.  Without the explicit null sets
	//  a fatal error occurs in the JRE.
	@Override
	public void removeFromParent() {
		curve = null;
		arrow = null;
		super.removeFromParent();
	}

}
