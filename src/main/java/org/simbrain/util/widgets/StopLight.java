package org.simbrain.util.widgets;

import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * A custom status light component for use in GUI dialogs. StopLight couples to
 * some other component and can be used to indicate to the user whether or
 * whatever that component represents is ready. Can also be used for internal
 * checks.
 * <p>
 * TODO: implement null and/or yellow warning status. Change name to status
 * light and rename greenlight?
 *
 * @author ztosi
 */
public class StopLight extends JPanel implements ActionListener {

    /**
     * A List of images, designed to be a singleton list containing the status
     * image.
     */
    private final ArrayList<Image> imgs = new ArrayList<Image>();

    /**
     * Default greenlight image.
     */
    private final ImageIcon go = ResourceManager.getSmallIcon("menu_icons/GreenCheck.png");

    /**
     * Default redlight image.
     */
    private final ImageIcon stop = ResourceManager.getSmallIcon("menu_icons/RedX.png");

    /**
     * The current image.
     */
    private Image img;

    /**
     * A boolean tied to the current status.
     */
    private boolean greenlight;

    /**
     * Default Constructor, by default current status is redlight: image is RedX
     * and greenlight is false. Also fully paints and prepares stoplight as a
     * JPanel for integration into another panel/dialog/etc.
     */
    public StopLight() {
        img = stop.getImage();
        imgs.add(img);
        Dimension dim = new Dimension(img.getWidth(null), img.getHeight(null));
        setPreferredSize(dim);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setSize(dim);
        setLayout(null);
        repaint();
    }

    /**
     * Constructor which fully paints and prepares stoplight as a JPanel for
     * integration into another panel/dialog/etc. Also allows the user to set
     * the starting status.
     *
     * @param greenlight boolean representing whether or not the initial status
     *                   of the stoplight is green.
     */
    public StopLight(boolean greenlight) {
        this.greenlight = greenlight;
        if (greenlight) {
            img = go.getImage();
        } else {
            img = stop.getImage();
        }
        imgs.add(img);
        Dimension dim = new Dimension(img.getWidth(null), img.getHeight(null));
        setPreferredSize(dim);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setSize(dim);
        setLayout(null);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void paintComponent(Graphics g) {
        removeAll();
        super.paintComponent(g);
        g.drawImage(img, 0, 0, null);
    }

    /**
     * Quick method which flips the state of the stoplight to green if the
     * current state is red and red if the current state is green. Again the
     * image is always coupled to the greenlight boolean.
     */
    public void flip() {
        if (greenlight) {
            img = null;
            repaint();
            img = stop.getImage();
        } else {
            img = null;
            repaint();
            img = go.getImage();
        }
        greenlight = !greenlight;
        firePropertyChange("State", !greenlight, greenlight);
    }

    /**
     * Set the current state of the stoplight.
     *
     * @param greenlight the desired state of the stop light: green -&#62; true
     *                   false -&#62; red
     */
    public void setState(boolean greenlight) {
        this.greenlight = greenlight;
        if (greenlight) {
            img = null;
            repaint();
            img = go.getImage();
        } else {
            img = null;
            repaint();
            img = stop.getImage();
        }
        firePropertyChange("State", !greenlight, greenlight);
    }

    /**
     * @return the current state.
     */
    public boolean getState() {
        return greenlight;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        setState(true);
        repaint();
    }
}
