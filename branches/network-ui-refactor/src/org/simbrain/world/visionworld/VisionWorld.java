package org.simbrain.world.visionworld;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JPanel;

import org.simnet.coupling.CouplingMenuItem;
import org.simnet.coupling.SensoryCoupling;
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
public class VisionWorld extends World implements Agent, MouseListener, MouseMotionListener {

    /**
    * The initial dimension constant (prevents use of "magic numbers").
    */
    private final int initDimensions = 10;

    /**
    * The number of pixels in the row.
    */
    private int numPixelsRow = initDimensions;

    /**
    * The number of pixels in the column.
    */
    private int numPixelsColumn = initDimensions;

    /**
    * The size of an individual pixel.
    */
    private int pixelSize = initDimensions;

    /**
    * The array of networks associated with this world.
    */
    private ArrayList commandTargets = new ArrayList();

    /**
    * The array of <b>Pixel</b> objects representing the world.
    */
    private Pixel[][] pixels  =  new Pixel[numPixelsColumn][numPixelsRow];

    /**
    * The parent frame containing this world.
    */
    private VisionWorldFrame parentFrame;

    /**
    * The name of this world (also the title of the parent frame.
    */
    private String name;

    /**
    * A boolean that represents whether or not a mouse motion is inside of the pixel field.
    */
    private boolean mouseInTheHouse  =  false;

    /**
    * Used when dragging the mouse, so that dragging does not toggle.
    */
    private boolean currentState  =  true;


    /**
     * The default constructor, creates the world, and registers the necessary listeners.
     */
    public VisionWorld() {
        super();
        redimension(numPixelsRow, numPixelsColumn);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    /**
     * A constructor for a height and width.
     * @param w the width of the desired world (in pixels)
     * @param h the height of the desired world (in pixels)
     */
    public VisionWorld(final int w, final int h) {
        redimension(w, h);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    /**
     * A constructor for the default size, built on a parent frame.
     * @param pf the parent frame calling the constructor
     */
    public VisionWorld(final VisionWorldFrame pf) {
        redimension(numPixelsRow, numPixelsColumn);
        parentFrame  =  pf;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
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


    /**
     * @return the pixel that is represented by the parameters
     * @param x the x location of the pixel (in pixels)
     * @param y the y location of the pixel (in pixels)
     */
    public Pixel getPixel(final int x, final int y) {

        return pixels[x][y];

    }


    /**
     * @return the pixel that is underneath the mouse producing the event
     * @param e the event for which a pixel is needed
     */
    public Pixel getSelectedPixel(final MouseEvent e) {
        if (pixelSize != 0) {
            if (e.getX() / pixelSize < numPixelsRow && e.getY() / pixelSize < numPixelsColumn) {
                return getPixel(e.getX() / pixelSize, e.getY() / pixelSize);
            }
        }
        return null;
    }

    /**
     * @return the string to set as the toolTip for a given pixel
     * @param e the mouseEvent for which the toolTip is needed
     */
    public String getSelectedPixelToolTip(final MouseEvent e) {
        if (pixelSize != 0) {
            if (e.getX() / pixelSize < numPixelsRow && e.getY() / pixelSize < numPixelsColumn) {
                return "" + (e.getX() / pixelSize + 1) + "," + (e.getY() / pixelSize + 1);
            }
        }
        return null;
    }

    /**
     * Overrides the paint method of the JPanel component, providing special qualifiers for pixels.
     * @param g the Graphics object for this world
     */
    public void paint(final Graphics g) {
        this.setBackground(Color.RED);
        super.paint(g);

        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[i].length; j++) {
                pixels[i][j].show(g);
            }
        }
    }


    /**
     * Rebuilds the world to an appropriate form.
     */
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

    /**
     * @return this world
     */
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


    /**
     * Not yet implemented.
     * @param commandList the list of command strings
     * @param value the value to set on the given commanded item
     */
    public void setMotorCommand(final String[] commandList, final double value) {

    }

    /**
     * @return the Type of this world
     */
    public String getType() {
        return "VisionWorld";
    }

    /**
     * @return an arraylist representing this world/agent
     */
    public ArrayList getAgentList() {
        ArrayList ret  =  new ArrayList();
        ret.add(this);
        return ret;
    }

    /**
     * Not yet implemented.
     * @return the menu of available motor commands
     * @param al the action listener to be attached to these menu items
     */
    public JMenu getMotorCommandMenu(final ActionListener al) {
        return null;
    }

    /**
     * @return the menu of available sensors
     * @param al the action listener to attach to the menu items
     */
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


    /**
     * @param net the network to add as a command target
     */
    public void addCommandTarget(final NetworkPanel net) {
        if (!commandTargets.contains(net)) {
            commandTargets.add(net);
        }
    }

    /**
     * @param net the network to remove as a command target
     */
    public void removeCommandTarget(final NetworkPanel net) {
        commandTargets.remove(net);
    }

    /**
     * @return commandTargets
     */
    public ArrayList getCommandTargets() {
        return commandTargets;
    }

    /**
     * @return the parent frame
     */
    public VisionWorldFrame getParentFrame() {
        return parentFrame;
    }

    /**
     * @param parentFrame the parent frame to be set
     */
    public void setParentFrame(final VisionWorldFrame parentFrame) {
        this.parentFrame  =  parentFrame;
    }

    /**
     * @param name the name to be set for this world, also, the title for the parent frame
     */
    public void setName(final String name) {
        this.getParentFrame().setTitle(name);
        this.name  =  name;
    }

    /**
     * @return the name of this world
     */
    public String getWorldName() {
        return this.name;
    }

    /**
     * @param e the MouseEvent triggering the method
     */
    public void mouseClicked(final MouseEvent e) {
    }

    /**
     * Flips a pixel, or turns on pixel dragging.
     * @param e the MouseEvent triggering the method
     */
    public void mousePressed(final MouseEvent e) {
        Pixel p  =  getSelectedPixel(e);
        if (p !=  null) {
            p.switchState();
            currentState  =  p.getState();
        }
        repaint();
    }

    /**
     * Drag-Flips a series of pixels.
     * @param e the MouseEvent triggering the method
     */
    public void mouseDragged(final MouseEvent e) {
        Pixel p  =  getSelectedPixel(e);
        if (p !=  null) {
            p.setState(currentState);
            repaint();
        }
    }

    /**
     * @param e the MouseEvent triggering the method
     */
    public void mouseReleased(final MouseEvent e) {
    }

    /**
     * @param e the MouseEvent triggering the method
     */
    public void mouseEntered(final MouseEvent e) {
        this.mouseInTheHouse  =  true;
    }

    /**
     * @param e the MouseEvent triggering the method
     */
    public void mouseExited(final MouseEvent e) {
        this.mouseInTheHouse = false;
    }

    /**
     * Sets the tooltip text of the mouse to the hovered-over pixel.
     * @param e the MouseEvent triggering the method
     */
    public void mouseMoved(final MouseEvent e) {
        if (this.mouseInTheHouse) {
            this.setToolTipText(this.getSelectedPixelToolTip(e));
        }
    }

    /**
     * @return numPixelsColumn
     */
    public int getNumPixelsColumn() {
        return numPixelsColumn;
    }

    /**
     * @param numPixelsColumn the numPixelsColumn to set
     */
    public void setNumPixelsColumn(final int numPixelsColumn) {
        this.numPixelsColumn = numPixelsColumn;
    }

    /**
     * @return numPixelsRow
     */
    public int getNumPixelsRow() {
        return numPixelsRow;
    }

    /**
     * @param numPixelsRow the numPixelsRow to set
     */
    public void setNumPixelsRow(final int numPixelsRow) {
        this.numPixelsRow = numPixelsRow;
    }

}
