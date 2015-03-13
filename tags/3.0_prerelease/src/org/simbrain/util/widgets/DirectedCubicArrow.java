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
package org.simbrain.util.widgets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.simbrain.network.gui.nodes.NeuronGroupNode.Port;

/**
 *
 * @author Zach Tosi
 *
 */
@SuppressWarnings("serial")
public class DirectedCubicArrow extends PNode {

    public enum BezierTemplate {
        DIRECTED {

            @Override
            public Point2D.Float getBez1(Point2D src, Point2D tar, Port port) {
                float bez_y = 0;
                float bez_x = 0;
                switch (port) {
                    case NORTH:	bez_y = (float) (src.getY()
                            + Math.abs(src.getY() - tar.getY())/2);
                    bez_x = (float) src.getX();
                    break;
                    case SOUTH:	bez_y = (float) (src.getY()
                            - Math.abs(src.getY() - tar.getY())/2);
                    bez_x = (float) src.getX();
                    break;
                    case EAST:	bez_x = (float) (src.getX()
                            + Math.abs(src.getX() - tar.getX())/2);
                    bez_y = (float) src.getY();
                    break;
                    case WEST:	bez_x = (float) (src.getX()
                            - Math.abs(src.getX() - tar.getX())/2);
                    bez_y = (float) src.getY();
                    break;
                }
                return new Point2D.Float(bez_x, bez_y);
            }

            @Override
            public Point2D.Float getBez2(Point2D src, Point2D tar, Port port) {
                double theta =  getTheta(port);
                float distanceToMidpoint = BezierTemplate
                        .distanceToMidpoints(src, tar);
                float dist = distanceToMidpoint/2;
                if (dist < 120)  dist = 120;
                float b2X = (float) (tar.getX() - (dist * Math.cos(theta)));
                float b2Y = (float) (tar.getY() - (dist * Math.sin(theta)));
                return new Point2D.Float(b2X, b2Y);
            }
        },

        BIDIRECTIONAL {
            @Override
            public Point2D.Float getBez1(Point2D src, Point2D tar, Port port) {
                double theta = getTheta(port);
                float distanceToMidpoint = BezierTemplate.distanceToMidpoints(src, tar);
                float dist = distanceToMidpoint/1.25f;
                float b1X = (float) (src.getX() - (dist * Math.cos(theta)));
                float b1Y = (float) (src.getY() - (dist * Math.sin(theta)));
                return new Point2D.Float(b1X, b1Y);
            }

            @Override
            public Point2D.Float getBez2(Point2D src, Point2D tar, Port port) {
                double theta =  getTheta(port);
                float distanceToMidpoint = BezierTemplate.distanceToMidpoints(src, tar);
                float dist = distanceToMidpoint/1.25f;
                float b2X = (float) (tar.getX() - (dist * Math.cos(theta)));
                float b2Y = (float) (tar.getY() - (dist * Math.sin(theta)));
                return new Point2D.Float(b2X, b2Y);
            }
        };

        public abstract Point2D.Float getBez1(Point2D src, Point2D tar, Port port);

        public abstract Point2D.Float getBez2(Point2D src, Point2D tar, Port port);

        public static float distanceToMidpoints(Point2D src, Point2D tar) {
            return (float) src.distance(tar) / 2;
        }
    }

    private static final Color DEFAULT_COLOR = Color.BLACK;

    private static final float DEFAULT_TRANSPARENCY = 1.0f;

    private static final float DEFAULT_WIDTH = 30;

    private static final BezierTemplate DEFAULT_TEMPLATE = BezierTemplate.DIRECTED;

    private PPath.Float curve;

    private PPath.Float arrow;

    private BezierTemplate template;

    private Color color;

    private float transparency;

    private float strokeWidth;

    private Point2D.Float startPt;

    private Point2D.Float endPt;

    private Point2D.Float bezier_1;

    private Point2D.Float bezier_2;

    private float theta;

    //	private DirectedArrowShape arrShape;

    public DirectedCubicArrow() {
        this(DEFAULT_TEMPLATE);
    }

    public DirectedCubicArrow(BezierTemplate template){
        this(template, DEFAULT_COLOR, DEFAULT_TRANSPARENCY, DEFAULT_WIDTH);
    }

    public DirectedCubicArrow(BezierTemplate template, Color color,
            float transparency, float strokeWidth){
        this.setTemplate(template);
        this.color = color;
        this.transparency = transparency;
        this.strokeWidth = strokeWidth;
        curve = new PPath.Float();
        arrow = new PPath.Float();
        this.addChild(curve);
        this.addChild(arrow);
        curve.setStroke(new BasicStroke(strokeWidth,
                BasicStroke.CAP_SQUARE,
                BasicStroke.JOIN_MITER));
        curve.setTransparency(0.5f);
        curve.setPaint(null);
        curve.setStrokePaint(color);
        curve.raiseToTop();

        arrow.setPaint(color);
        arrow.setStrokePaint(null);
        arrow.raiseToTop();

    }

    public void layoutChildren(Point2D src, Port srcPort,
            Point2D tar, Port tarPort) {
        this.bezier_1 = template.getBez1(src, tar, srcPort);
        this.bezier_2 = template.getBez2(src, tar, tarPort);

        int endX = (int) (tar.getX() - (strokeWidth * Math.sqrt(3)
                * Math.cos(getTheta(tarPort))));
        int endY = (int) (tar.getY() - (strokeWidth * Math.sqrt(3)
                * Math.sin(getTheta(tarPort))));

        int startX = (int) (src.getX() - (strokeWidth * Math.sqrt(3)
                * Math.cos(getTheta(srcPort))));
        int startY = (int) (src.getY() - (strokeWidth * Math.sqrt(3)
                * Math.sin(getTheta(srcPort))));

        CubicCurve2D.Double theCurve = new CubicCurve2D.Double(startX, startY,
                bezier_1.getX(), bezier_1.getY(), bezier_2.getX(),
                bezier_2.getY(), endX, endY);
        curve.reset();
        curve.append(theCurve, false);

        arrow.reset();
        arrow.append(traceArrowHead(getTheta(tarPort), tar.getX(), tar.getY()),
                false);

    }

    private Polygon traceArrowHead(double theta, double tarX, double tarY) {
        int numSides = 3;
        int[] triPtx = new int[numSides];
        int[] triPty = new int[numSides];
        double phi = Math.PI / 6;

        triPtx[0] = (int) (tarX - (strokeWidth/2 * Math.cos(theta)));
        triPty[0] = (int) (tarY - (strokeWidth/2 * Math.sin(theta)));

        triPtx[1] = (int) (tarX - (2 * strokeWidth * Math.cos(theta + phi)));
        triPty[1] = (int) (tarY - (2 * strokeWidth * Math.sin(theta + phi)));

        triPtx[2] = (int) (tarX - (2 * strokeWidth * Math.cos(theta - phi)));
        triPty[2] = (int) (tarY - (2 * strokeWidth * Math.sin(theta - phi)));

        return new Polygon(triPtx, triPty, numSides);
    }

    public static float getTheta(Port port) {
        double theta = 0;
        switch(port) {
            case NORTH: theta = -Math.PI/2;	break;
            case SOUTH: theta = Math.PI/2;	break;
            case EAST:	theta = Math.PI; 	break;
            case WEST: 	theta = 0;			break;
        }
        return (float) theta;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        curve.setStrokePaint(color);
    }

    @Override
    public float getTransparency() {
        return transparency;
    }

    @Override
    public void setTransparency(float transparency) {
        this.transparency = transparency;
        curve.setTransparency(transparency);
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
    }

    public Point2D.Float getStartPt() {
        return startPt;
    }

    public void setStartPt(Point2D.Float startPt) {
        this.startPt = startPt;
    }

    public Point2D.Float getEndPt() {
        return endPt;
    }

    public void setEndPt(Point2D.Float endPt) {
        this.endPt = endPt;
    }

    public Point2D.Float getBezier_1() {
        return bezier_1;
    }

    public void setBezier_1(Point2D.Float bezier_1) {
        this.bezier_1 = bezier_1;
    }

    public Point2D.Float getBezier_2() {
        return bezier_2;
    }

    public void setBezier_2(Point2D.Float bezier_2) {
        this.bezier_2 = bezier_2;
    }

    public float getTheta() {
        return theta;
    }

    public void setTheta(float theta) {
        this.theta = theta;
    }

    public BezierTemplate getTemplate() {
        return template;
    }

    public void setTemplate(BezierTemplate template) {
        this.template = template;
    }

    //    /**
    //     *
    //     * Uggg... Terrible... It's nearly perfect... but needs smoothing. High
    //     * aspirations, and should be picked back up later, but there just isn't
    //     * enough time...
    //     *
    //     *
    //     * @author zach
    //     *
    //     */
    //    private class DirectedArrowShape extends Polygon {
    //
    //
    //        private float srcTheta;
    //
    //        private float endTheta;
    //
    //        private Point2D.Float src;
    //
    //        private Point2D.Float tar;
    //
    //        private Point2D.Float bez1;
    //
    //        private Point2D.Float bez2;
    //
    //        private float width;
    //
    //        private float percentFull;
    //
    //        /**
    //         *
    //         * @param srcTheta
    //         * @param endTheta
    //         * @param src
    //         * @param tar
    //         * @param bez1
    //         * @param bez2
    //         * @param width
    //         * @param percentFull
    //         */
    //        public DirectedArrowShape(float srcTheta, float endTheta,
    //                Point2D.Float src, Point2D.Float tar, Point2D.Float bez1,
    //                Point2D.Float bez2, float width, float percentFull) {
    //            super();
    //            this.srcTheta = srcTheta;
    //            this.endTheta = endTheta;
    //            this.src = src;
    //            this.tar = tar;
    //            this.bez1 = bez1;
    //            this.bez2 = bez2;
    //            this.width = width;
    //            this.percentFull = percentFull;
    //        }
    //
    //
    //        //		/**
    //        //		 *
    //        //		 * @param arg0
    //        //		 */
    //        //		public void paint(Graphics g) {
    //        //			Graphics2D g2d = (Graphics2D) g;
    //        //
    //        //			GeneralPath arrow = drawArrow();
    //        //
    //        //			BasicStroke stroke = new BasicStroke((1 - percentFull)
    //        //					* strokeWidth / 2,
    //        //					BasicStroke.CAP_SQUARE,
    //        //					BasicStroke.JOIN_MITER);
    //        //
    //        //			g2d.setPaint(color);
    //        //
    //        //			g2d.setStroke(stroke);
    //        //
    //        //			g2d.draw(arrow);
    //        //
    //        ////			int bitmask = 127 << 24;
    //        ////			Color fillC  = new Color((color.getRGB() << 8 >> 8) | bitmask);
    //        ////
    //        ////			g2d.setPaint(fillC);
    //        //
    //        //			g2d.fill(arrow);
    //        //
    //        //		}
    //
    //        /**
    //         *
    //         * @param start
    //         * @param end
    //         * @param mag
    //         * @return
    //         */
    //        public double [] perpendicular(Point2D.Double start, Point2D.Double end, double mag) {
    //            double [] vec = {(mag)*(end.getX() - start.getX())/(start.distance(end)), (mag)*(end.getY() - start.getY())/(start.distance(end))};
    //            double [] shift = {-vec[1], vec[0]}; //rotation
    //            return shift;
    //        }
    //
    //        /**
    //         *
    //         * @return
    //         */
    //        public GeneralPath drawArrow () {
    //            Point2D.Float startP;
    //            Point2D.Float arrPtSide1;
    //            Point2D.Float arrPtCenter;
    //            Point2D.Float arrPtSide2;
    //            Point2D.Float endP;
    //
    //            float lineFill = 1-percentFull;
    //            float offset1 = width * lineFill / 4;
    //            float offset2 = (percentFull * width / 2) + offset1;
    //            float hyp = (float) Math.sqrt(Math.pow(offset1, 2)
    //                    + Math.pow(offset2, 2));
    //            float offsetAngle = (float) Math.atan(offset2/offset1);
    //            float phi = srcTheta - offsetAngle;
    //            float offXst = (float) (hyp * Math.cos(phi + Math.PI));
    //            float offYst = (float) (hyp * Math.sin(phi + Math.PI));
    //
    //            startP = new Point2D.Float((float) (src.getX() + offXst),
    //                    (float) (src.getY() + offYst));
    //            endP = new Point2D.Float((float) (src.getX() - offXst),
    //                    (float)(src.getY() - offYst));
    //
    //            int numSides = 3;
    //            int[] triPtx = new int[numSides];
    //            int[] triPty = new int[numSides];
    //            phi = (float) Math.PI / 6;
    //
    //            triPtx[0] = (int) (tar.getX() - (DEFAULT_ARROW_SIZE/2 * Math.cos(endTheta)));
    //            triPty[0] = (int) (tar.getY() - (DEFAULT_ARROW_SIZE/2 * Math.sin(endTheta)));
    //
    //            triPtx[1] = (int) (tar.getX() - (2 * DEFAULT_ARROW_SIZE * Math.cos(endTheta + phi)));
    //            triPty[1] = (int) (tar.getY() - (2 * DEFAULT_ARROW_SIZE * Math.sin(endTheta + phi)));
    //
    //            triPtx[2] = (int) (tar.getX() - (2 * DEFAULT_ARROW_SIZE * Math.cos(endTheta - phi)));
    //            triPty[2] = (int) (tar.getY() - (2 * DEFAULT_ARROW_SIZE * Math.sin(endTheta - phi)));
    //
    //            int endX = (int) (tar.getX() - (DEFAULT_ARROW_SIZE * Math.sqrt(3) * Math.cos(endTheta)));
    //            int endY = (int) (tar.getY() - (DEFAULT_ARROW_SIZE * Math.sqrt(3) * Math.sin(endTheta)));
    //
    //            arrPtCenter = new Point2D.Float(triPtx[0], triPty[0]);
    //            arrPtSide1 = new Point2D.Float(triPtx[1], triPty[1]);
    //            arrPtSide2 = new Point2D.Float(triPtx[2], triPty[2]);
    //
    //            CubicCurve2D.Double theCurve = new CubicCurve2D.Double(src.getX(),
    //                    src.getY(), bez1.getX(), bez1.getY(),
    //                    bez2.getX(), bez2.getY(), endX,
    //                    endY);
    //            PathIterator pi = theCurve.getPathIterator(null, 0.1);
    //            double [] coords = new double [6];
    //            double wi = percentFull * width / 2;
    //            GeneralPath side1 = new GeneralPath(PathIterator.WIND_NON_ZERO);
    //            GeneralPath side2 = new GeneralPath(PathIterator.WIND_NON_ZERO);
    //            pi.currentSegment(coords);
    //            int count = 0;
    //            do {
    //
    //                int seg = pi.currentSegment(coords);
    //                Point2D.Double start = new Point2D.Double(coords[0], coords[1]);
    //                Point2D.Double ctrl1 = new Point2D.Double(coords[2], coords[3]);
    //                Point2D.Double ctrl2 = new Point2D.Double(coords[4], coords[4]);
    //                pi.next();
    //                if(pi.isDone()) break;
    //                pi.currentSegment(coords);
    //                Point2D.Double end = new Point2D.Double(coords[0], coords[1]);
    //                double [] shift = perpendicular(start, end, wi);
    //                if (count == 0) {
    //                    count++;
    //                    side1.moveTo(start.getX() + shift[0], start.getY() + shift[1]);
    //                    side2.moveTo(start.getX() - shift[0], start.getY() - shift[1]);
    //                } else if (seg == PathIterator.SEG_LINETO) {
    //                    side1.lineTo(start.getX() + shift[0], start.getY() + shift[1]);
    //                    side2.lineTo(start.getX() - shift[0], start.getY() - shift[1]);
    //                } else if (seg == PathIterator.SEG_QUADTO) {
    //                    side1.quadTo(start.getX() + shift[0], start.getY() + shift[1],
    //                            ctrl1.getX() + shift[0], ctrl1.getY() + shift[1]);
    //                    side2.quadTo(start.getX() - shift[0], start.getY() - shift[1],
    //                            ctrl1.getX() - shift[0], ctrl1.getY() - shift[1]);
    //                } else if (seg == PathIterator.SEG_CUBICTO) {
    //                    side1.curveTo(start.getX() + shift[0], start.getY() + shift[1],
    //                            ctrl1.getX() + shift[0], ctrl1.getY() + shift[1], ctrl2.getX()
    //                            + shift[0], ctrl2.getY() + shift[1]);
    //                    side2.curveTo(start.getX() - shift[0], start.getY() - shift[1],
    //                            ctrl1.getX() - shift[0], ctrl1.getY() - shift[1], ctrl2.getX()
    //                            - shift[0], ctrl2.getY() - shift[1]);
    //                }
    //
    //            } while (!pi.isDone());
    //
    //            //			theCurve = null;
    //
    //            GeneralPath arrow = new GeneralPath(PathIterator.WIND_NON_ZERO);
    //            arrow.moveTo(startP.getX(), startP.getY());
    //            arrow.append(side2, true);
    //            arrow.lineTo(arrPtSide1.getX(), arrPtSide1.getY());
    //            arrow.lineTo(arrPtCenter.getX(), arrPtCenter.getY());
    //            arrow.lineTo(arrPtSide2.getX(), arrPtSide2.getY());
    //            arrow.moveTo(endP.getX(), endP.getY());
    //            arrow.append(side1, true);
    //            arrow.lineTo(arrPtSide2.getX(), arrPtSide2.getY());
    //            arrow.lineTo(arrPtCenter.getX(), arrPtCenter.getY());
    //            arrow.moveTo(endP.getX(), endP.getY());
    //            arrow.lineTo(startP.getX(), startP.getY());
    //            arrow.closePath();
    //            return arrow;
    //
    //        }
    //    }
}
