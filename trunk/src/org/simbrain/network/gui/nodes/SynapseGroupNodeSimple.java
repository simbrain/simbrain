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

	private final AffineTransform transform = new AffineTransform();

	//	private ArrowHead arrowHead = new ArrowHead();

	private float arrowRotation;

	/** Line connecting nodes. */
	private PPath.Float curve;

	/** Arrow at end of curve. */
	private PPath.Float arrow;

//	private final PPath.Float dbLine;

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
//		dbLine = new PPath.Float();
		this.addChild(curve);
		this.addChild(arrow);
//		this.addChild(dbLine);
		if (group.isRecurrent()) {
			curve.setStroke(new BasicStroke(DEFAULT_ARROW_THICKNESS,
					BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER));
		} else {
			curve.setStroke(new BasicStroke(DEFAULT_ARROW_THICKNESS,
					BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_MITER));
		}
		curve.setTransparency(0.5f);
		curve.setPaint(null); 	// Prevents white background semi-circle from
		//appearing underneath the curve
		curve.setStrokePaint(DEFAULT_COLOR);
		curve.raiseToTop();
		arrow.setPaint(DEFAULT_COLOR);
		arrow.setStroke(null);
		arrow.raiseToTop();
//		dbLine.setStroke(new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
//		dbLine.setStrokePaint(Color.BLACK);
//		dbLine.setPaint(Color.BLACK);
//		dbLine.raiseToTop();
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
			NeuronGroup source = synapseGroup.getSourceNeuronGroup();
			NeuronGroup target = synapseGroup.getTargetNeuronGroup();

			float srcX;
			float srcY;
			float tarX;
			float tarY;


			float centerXSrc = (float) source.getCenterX();
			float centerYSrc = (float) source.getCenterY();
			float centerXTar = (float) target.getCenterX();
			float centerYTar = (float) target.getCenterY();
			int numSides = 3;
			int[] triPtx = new int[numSides];
			int[] triPty = new int[numSides];

			float distance = (float) Point2D.distance(centerXSrc,
					centerYSrc, centerXTar, centerYTar);

			float zoneModifier = distance / 3;//(1 + distance/source.getMaxDim()));

			/*/**********************************
			 * 	   \ Ia | I | Ib /
			 * 		\   |   |   /
			 * 	 IVa \  |   |  / IIa
			 * 	______\ |   | /_______
			 *  _IV____  SRC  ___II___
			 *        / |   | \
			 * 	 IVb /  |   |  \ IIb
			 *      /   |   |   \
			 * 	   /IIIa|III|IIIb\
			 ************************************/

			// Determine what quadrant the starting point will be in.
			if (Math.abs(centerYTar - centerYSrc)
					> Math.abs(centerXTar - centerXSrc)) {// Start point is in
														  //either I or III
				srcX = centerXSrc; // In any case source X is centered...

				if (centerYTar < centerYSrc) {// Start point is in III
					srcY = (float) source.getMinY() - DEFAULT_ARROW_DISTANCE;
				} else { // Start point is in I
					srcY = (float) source.getMaxY() + DEFAULT_ARROW_DISTANCE;
				}
				// Target is either offset or "directly above/below" target
				boolean left = target.getMaxX() < (source.getMinX() - zoneModifier);
				boolean right = target.getMinX() > (source.getMaxX() + zoneModifier);
				if (left || right) {
					tarY = centerYTar;
					if (left) { // Offset left (I/IIIa)
						tarX = (float) target.getMaxX() + DEFAULT_ARROW_DISTANCE/2;
					} else { // Offset right (I/IIIb)
						tarX = (float) target.getMinX() - DEFAULT_ARROW_DISTANCE/2;
					}
				} else { // Not offset I/III
					tarX = centerXTar;
					if (centerYTar < centerYSrc) {
						tarY = (float) target.getMaxY() + DEFAULT_ARROW_DISTANCE/2;
					} else {
						tarY = (float) target.getMinY() - DEFAULT_ARROW_DISTANCE/2;
					}
				}
			} else { // Start point is in either II or IV
				srcY = centerYSrc; // In any case Y is centered...
				if (centerXTar < centerXSrc) { // Start point is in IV
					srcX = (float) source.getMinX() - DEFAULT_ARROW_DISTANCE;
				} else { // Start point is in II
					srcX = (float) source.getMaxX() + DEFAULT_ARROW_DISTANCE;
				}
				boolean above = target.getMinY() > source.getMaxY() + zoneModifier;
				boolean below = target.getMaxY() < source.getMinY() - zoneModifier;
				if (above || below) {
					tarX = centerXTar;
					if (above) {
						tarY = (float) target.getMinY() - DEFAULT_ARROW_DISTANCE/2;
					} else {
						tarY = (float) target.getMaxY() + DEFAULT_ARROW_DISTANCE/2;
					}
				} else {
					tarY = centerYTar;
					if (centerXTar < centerXSrc) {
						tarX = (float) target.getMaxX() + DEFAULT_ARROW_DISTANCE/2;
					} else {
						tarX = (float) target.getMinX() - DEFAULT_ARROW_DISTANCE/2;
					}
				}
			}

			double theta =  0;
			if (tarY == centerYTar) {
				theta = tarX > centerXTar ? Math.PI : 0;
			} else {
				theta = tarY > centerYTar ? -Math.PI/2 : Math.PI/2;
			}

			double phi = Math.PI / 6;
			double neg = (tarX < srcX) ? 1 : 1;

			triPtx[0] = (int) (tarX - neg
					* (DEFAULT_ARROW_DISTANCE/2 * Math.cos(theta)));
			triPty[0] = (int) (tarY - neg
					* (DEFAULT_ARROW_DISTANCE/2 * Math.sin(theta)));

			triPtx[1] = (int) (tarX - neg
					* (2 * DEFAULT_ARROW_DISTANCE * Math.cos(theta + phi)));
			triPty[1] = (int) (tarY - neg
					* (2 * DEFAULT_ARROW_DISTANCE * Math.sin(theta + phi)));

			triPtx[2] = (int) (tarX - neg
					* (2 * DEFAULT_ARROW_DISTANCE * Math.cos(theta - phi)));
			triPty[2] = (int) (tarY - neg
					* (2 * DEFAULT_ARROW_DISTANCE * Math.sin(theta - phi)));

			float endX = (float) (tarX - neg
					* (Math.sqrt(3) * DEFAULT_ARROW_DISTANCE * Math.cos(theta)));
			float endY = (float) (tarY - neg
					* (Math.sqrt(3) * DEFAULT_ARROW_DISTANCE * Math.sin(theta)));

			float b2X = (float) (tarX - neg
					* (4 * DEFAULT_ARROW_DISTANCE * Math.cos(theta)));
			float b2Y = (float) (tarY - neg
					* (4 * DEFAULT_ARROW_DISTANCE * Math.sin(theta)));

			float x = (srcX + endX) / 2;
			float y = (srcY + endY) / 2;
			float distanceToMidpoint = (float) Point2D.distance(srcX,
					srcY, x, y);
			float bez_x = 0;
			float bez_y = 0;
			if (Math.abs(centerYTar - centerYSrc)
					> Math.abs(centerXTar - centerXSrc)) {
				bez_x = centerXSrc;
				if (centerYTar < centerYSrc) {
					bez_y = (float) (source.getMinY() - distanceToMidpoint);
				} else {
					bez_y = (float) (source.getMaxY() + distanceToMidpoint);
				}
			} else {
				bez_y = centerYSrc;
				if (centerXTar < centerXSrc) {
					bez_x = (float) (source.getMinX() - distanceToMidpoint);
				} else {
					bez_x = (float) (source.getMaxX() + distanceToMidpoint);
				}
			}
			CubicCurve2D.Float theCurve = new CubicCurve2D.Float(srcX, srcY,
					bez_x, bez_y, b2X, b2Y, endX, endY);
			curve.reset();
			curve.append(theCurve, false);

			Polygon polyArrow = new Polygon(triPtx, triPty, numSides);
			arrow.reset();
			arrow.append(polyArrow, false);

			//For debugging... displays a line between the two bezier control pts.
//			Line2D.Float line = new Line2D.Float(bez_x, bez_y, b2X, b2Y);
//			dbLine.reset();
//			dbLine.append(line, false);

			float midpointX = (bez_x + ((tarX+srcX)/2)) / 2;
			float midpointY = (bez_y + ((tarY+srcY)/2)) / 2;



			interactionBox.setOffset(midpointX - interactionBox.getWidth() / 2,
					midpointY - interactionBox.getHeight() / 2);
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

		private final int[] default_pts_X = new int[] {0,
				(int) (2 * DEFAULT_ARROW_DISTANCE
						* Math.sin(DEFAULT_ARROW_ANGLE)),
						(int) (4 * DEFAULT_ARROW_DISTANCE
								* Math.sin(DEFAULT_ARROW_ANGLE))};

		private final int[] default_pts_Y = new int[] {0, (int) ((2
				* DEFAULT_ARROW_DISTANCE * Math.cos(DEFAULT_ARROW_ANGLE))
				- DEFAULT_ARROW_DISTANCE), 0};

		private final Polygon arrowTriangle;

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
