package org.simbrain.network.gui.nodes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Arc2D;
import java.util.concurrent.atomic.AtomicBoolean;

import org.piccolo2d.nodes.PPath;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronGroupNode;

public class SynapseGroupNodeRecurrent extends SynapseGroupNode {

    private PPath arrowHead;
    
    private PPath arcCurve;
    
    private float strokeWidth;
    
    private AtomicBoolean halt = new AtomicBoolean(false);
    
    public SynapseGroupNodeRecurrent(NetworkPanel networkPanel,
            SynapseGroup group) {
        super(networkPanel, group);
        if (!group.isRecurrent()) {
            throw new IllegalArgumentException("Using a recurrent synapse node"
                    + " for a non-recurrent synapse group.");
        }
        arrowHead = new PPath.Float();
        arcCurve = new PPath.Float();
        arrowHead.setStroke(null);
        arrowHead.setPaint(Color.green);
        strokeWidth = (float) (group.getSourceNeuronGroup().getMaxDim() / 6);
        arcCurve.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        arcCurve.setStrokePaint(Color.green);
        arcCurve.setTransparency(0.5f);
        arcCurve.setPaint(null);
        this.addChild(arcCurve);
        this.addChild(arrowHead);
        
        ((NeuronGroupNode) networkPanel.getObjectNodeMap().get(group.getTargetNeuronGroup())).addChild(this);
//        ((NeuronGroupNode) networkPanel.getObjectNodeMap().get(group.getTargetNeuronGroup())).addPropertyChangeListener(this);
//        createArc(getSynapseGroup());
        
    }

    @Override
    public void layoutChildren() {
        if (halt.get())
            return;
        NeuronGroup ng = getSynapseGroup().getSourceNeuronGroup();
        float quarterSizeX = (float) Math.abs((ng.getMaxX() - ng.getMinX())) / 4;
        float quarterSizeY = (float) Math.abs((ng.getMaxY() - ng.getMinY())) / 4;
        float quarterSize = quarterSizeX < quarterSizeY ? quarterSizeX : quarterSizeY;
        Arc2D.Float recArc = new Arc2D.Float((float) ng.getMinX()
                + quarterSize/2, (float) ng.getMinY() + quarterSize/2,
                quarterSize * 3, quarterSize * 3, 30, 300, Arc2D.OPEN);
        float r = (float) (((quarterSize * 3.0) / 2.0));// - strokeWidth/2.0);
        arcCurve.reset();
        arcCurve.append(recArc, false);
        arrowHead.reset();
        double endAng = -(11.0 * Math.PI / 6.0);
        double ptAng = endAng - 3.1*Math.PI/6.0;
        arrowHead.append(traceArrowHead(endAng - 3.1*Math.PI/6.0, (ng.getCenterX() + r * Math.cos(endAng)) + 2*strokeWidth * Math.cos(ptAng),
                (ng.getCenterY() + r * Math.sin(endAng)) + 2*strokeWidth * Math.sin(ptAng)), false);
        interactionBox.setOffset(ng.getCenterX() - interactionBox.getWidth()/2,
                ng.getCenterY() - interactionBox.getHeight()/2);
        interactionBox.raiseToTop();
    }
    
//    private void createArc(SynapseGroup group) {
//
//        NeuronGroup ng = group.getSourceNeuronGroup();
//        float quarterSizeX = (float) (ng.getMaxX() - ng.getMinX()) / 4;
//        float quarterSizeY = (float) (ng.getMaxY() - ng.getMinX()) / 2;
//        float quarterSize = quarterSizeX < quarterSizeY ? quarterSizeX : quarterSizeY;
//        Arc2D.Float recArc = new Arc2D.Float((float) ng.getMinX()
//                + quarterSize/2, (float) ng.getMinY() + quarterSize/2,
//                quarterSize * 3, quarterSize * 3, 30, 300, Arc2D.OPEN);
//
////              float arrowHeight = (float) ((2 * 30
////                      * Math.cos(Math.PI/3)) - 30);
////              float theta = (float)Math.tan(arrowHeight/(halfSize/2));
////              float phi = (float) Math.PI/6 - theta;
////              
////              float z = (float) Math.sqrt(Math.pow(arrowHeight, 2) + Math.pow(halfSize/2, 2));
////        
////              float x_displacement = (float) (z * Math.cos(phi));
////              float y_displacement = (float) (z * Math.sin(phi));
////
////              arrowRotation = (float)-Math.PI/6;
//              
//
//
////              arrowHead.setX_displacement(x_displacement + (float) recArc.getCenterX());
////              arrowHead.setY_displacement(y_displacement + (float) recArc.getCenterY());
////              arrowHead.setRotation(arrowRotation);
//              
//    }
    
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
    
    @Override
    public synchronized void removeFromParent() {
        halt.getAndSet(true);
        arcCurve = null;
        super.removeFromParent();
    }
}
