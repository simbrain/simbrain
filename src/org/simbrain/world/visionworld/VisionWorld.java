package org.simbrain.world.visionworld;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JPanel;

import org.simbrain.coupling.CouplingMenuItem;
import org.simbrain.coupling.SensoryCoupling;
import org.simbrain.network.NetworkPanel;
import org.simbrain.world.Agent;
import org.simbrain.world.World;

/**
 *  < b>VisionWorld < /b> provides visual input to a neural network in the form of a grid of pixels that can be
 * turned on or off by the user.
 *
 * @author RJB
 *
 */
public class VisionWorld extends JPanel implements World, Agent, MouseListener, MouseMotionListener, ComponentListener {

	private final static int INITDIMENSIONS = 10;
    private int numPixelsRow = INITDIMENSIONS;
    private int numPixelsColumn = INITDIMENSIONS;
    private int pixelSize = INITDIMENSIONS;
    private ArrayList commandTargets = new ArrayList();

    private Pixel[][] pixels  =  new Pixel[numPixelsColumn][numPixelsRow];
    private VisionWorldFrame parentFrame;
    private String name;
    private boolean mouseInTheHouse  =  false;
    private boolean currentState  =  true; // Used when dragging the mouse, so that dragging does not toggle


    // Stub constructor
    public VisionWorld() {
        redimension(numPixelsRow, numPixelsColumn);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addComponentListener(this);
    }

    // Construct world with specific numPixelsColumn and numPixelsRow
    public VisionWorld(final int w, final int h) {
        redimension(w, h);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addComponentListener(this);
    }

    public VisionWorld(final VisionWorldFrame pf) {
        redimension(numPixelsRow, numPixelsColumn);
        parentFrame  =  pf;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addComponentListener(this);
    }


    /**
     *  Changes the dimensions of the world.
     * @param h numPixelsColumn in pixels
     * @param w numPixelsRow in pixels
     */
    public void redimension(final int w, final int h) {
        numPixelsRow  =  w;
        numPixelsColumn  =  h;
        pixels  =  new Pixel[numPixelsRow][numPixelsColumn];
        for (int i = 0; i < numPixelsRow; i++) {
            for (int j = 0; j < numPixelsColumn; j++) {
                pixels[i][j]  =  new Pixel();
            }
        }
    }


    public Pixel getPixel(final int x, final int y) {

        return pixels[x][y];

    }


    public Pixel getSelectedPixel(final MouseEvent e) {
        if (pixelSize != 0) {
            if (e.getX() / pixelSize < numPixelsRow && e.getY() / pixelSize < numPixelsColumn) {
                return getPixel(e.getX() / pixelSize, e.getY() / pixelSize);
            }
        }
        return null;
    }

    public String getSelectedPixelToolTip(MouseEvent e) {
        if (pixelSize != 0) {
            if (e.getX() / pixelSize < numPixelsRow && e.getY() / pixelSize < numPixelsColumn) {
                return "" + (e.getX() / pixelSize + 1) + "," + (e.getY() / pixelSize + 1);
            }
        }
        return null;
    }

    public void paint(final Graphics g) {
        this.setBackground(Color.RED);
        super.paint(g);

        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[i].length; j++) {
                pixels[i][j].show(g);
            }
        }
    }


    public void rebuild() {
        pixelSize = this.getWidth() / numPixelsRow;
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[i].length; j++) {
                pixels[i][j].setSize(new Dimension(pixelSize, pixelSize));
                pixels[i][j].setLocation(i * pixelSize, j * pixelSize);
            }
        }
        this.setPreferredSize(new Dimension(numPixelsColumn * pixelSize, numPixelsRow * pixelSize));
    }

    public World getParentWorld() {
        return this;
    }

    /**
     * Accepts a string in the form "x","y".
     * @return stimulus for attached node
     * @param sensorID is the sensor identification array
     */
    public double getStimulus(final String[] sensorID) {
        boolean state  =  getPixel(Integer.parseInt(sensorID[0]), Integer.parseInt(sensorID[1])).getState();
        if (state  ==  Pixel.OFF) {
            return 0;
        } else {
            return 1;
        }
    }


    public void setMotorCommand(final String[] commandList, final double value) {

    }

    public String getType() {
        return "VisionWorld";
    }

    public ArrayList getAgentList() {
        ArrayList ret  =  new ArrayList();
        ret.add(this);
        return ret;
    }

    public JMenu getMotorCommandMenu(final ActionListener al) {
        // TODO Auto-generated method stub
        return null;
    }

    public JMenu getSensorIdMenu(final ActionListener al) {
        JMenu ret  =  new JMenu(this.getName());
        for (int i = 1; i < pixels.length; i++) {
            JMenu row  =  new JMenu("Row " + i);
            for (int j = 1; j < pixels[i].length; j++) {
                CouplingMenuItem cmi  =  new CouplingMenuItem("Column "
                        + j, new SensoryCoupling(this, new String[]{"" + (i - 1), "" + (j - 1)}));
                cmi.addActionListener(al);
                row.add(cmi);
            }
            ret.add(row);
        }

        return ret;
    }


    public void addCommandTarget(final NetworkPanel net) {
        if (!commandTargets.contains(net)) {
            commandTargets.add(net);
        }
    }

    public void removeCommandTarget(final NetworkPanel net) {
        commandTargets.remove(net);
    }

    public ArrayList getCommandTargets() {
        return commandTargets;
    }

    public VisionWorldFrame getParentFrame() {
        return parentFrame;
    }

    public void setParentFrame(final VisionWorldFrame parentFrame) {
        this.parentFrame  =  parentFrame;
    }

    public void setName(final String name) {
        this.getParentFrame().setTitle(name);
        this.name  =  name;
    }

    public String getName() {
        return this.name;
    }

    public void mouseClicked(final MouseEvent e) {
    }

    public void mousePressed(final MouseEvent e) {
        Pixel p  =  getSelectedPixel(e);
        if (p !=  null) {
            p.switchState();
            currentState  =  p.getState();
        }
        repaint();
    }

    public void mouseDragged(final MouseEvent e) {
        Pixel p  =  getSelectedPixel(e);
        if (p !=  null) {
            p.setState(currentState);
            repaint();
        }
    }
    public void mouseReleased(final MouseEvent arg0) {
    }

    public void mouseEntered(final MouseEvent arg0) {
        this.mouseInTheHouse  =  true;
    }

    public void mouseExited(final MouseEvent arg0) {
    }

    public void componentResized(final ComponentEvent arg0) {
    }

    public void componentMoved(final ComponentEvent arg0) {
    }

    public void componentShown(final ComponentEvent arg0) {
    }

    public void componentHidden(final ComponentEvent arg0) {
    }

    public void mouseMoved(final MouseEvent arg0) {
        if (this.mouseInTheHouse) {
            this.setToolTipText(this.getSelectedPixelToolTip(arg0));
        }
    }

    public int getNumPixelsColumn() {
        return numPixelsColumn;
    }

    public void setNumPixelsColumn(final int numPixelsColumn) {
        this.numPixelsColumn = numPixelsColumn;
    }

    public int getNumPixelsRow() {
        return numPixelsRow;
    }

    public void setNumPixelsRow(final int numPixelsRow) {
        this.numPixelsRow = numPixelsRow;
    }

}
