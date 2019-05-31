package org.simbrain.util.widgets;

import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * This class consists of a graphical lock that when clicked changes to an open
 * lock and back to a locked lock when clicked again.
 *
 * @author Zoë
 */
public class LinkIcon extends JPanel implements MouseListener {

    /**
     * The locked lock icon.
     */
    private ImageIcon linkedImIc = ResourceManager.getImageIcon("chainIcon.png");

    /**
     * The unlocked lock icon.
     */
    private ImageIcon unlinkedImIc = ResourceManager.getImageIcon("brokenChainIcon.png");

    /**
     * The image assigned either the locked or unlocked ImageIcon.
     */
    private Image link;

    /**
     * Whether or not the icon is in the locked or unlocked state.
     */
    private boolean linked;

    /**
     * Create a lock icon either in the locked or unlocked state.
     *
     * @param linked the starting state
     */
    public LinkIcon(boolean linked) {

        this.linked = linked;
        link = linked ? linkedImIc.getImage() : unlinkedImIc.getImage();
        addMouseListener(this);

        Dimension dim = new Dimension((int) link.getWidth(null), link.getHeight(null));
        setPreferredSize(dim);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setSize(dim);
        setLayout(null);
        repaint();

    }

    /**
     * Changes the lock icon's image to whatever it currently is not. Also
     * changes the locked flag accordingly.
     */
    public void changeState() {
        // Whatever it is make it not that: if locked -> unlock,
        // if unlocked -> lock
        linked = !linked;
        // Change the image to reflect the new state.
        link = linked ? linkedImIc.getImage() : unlinkedImIc.getImage();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void paintComponent(Graphics g) {
        removeAll();
        super.paintComponent(g);

        int offset = 0;
        if (unlinkedImIc.getIconWidth() > linkedImIc.getIconWidth() && linked) {
            offset = (unlinkedImIc.getIconWidth() - linkedImIc.getIconWidth()) / 2;
        }

        g.drawImage(link, offset, 0, null);
    }

    /**
     * Sets the state of the link icon, linked or unlinked
     *
     * @param linked
     */
    public void setState(boolean linked) {
        this.linked = linked;
        link = linked ? linkedImIc.getImage() : unlinkedImIc.getImage();
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        changeState();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    public boolean isLinked() {
        return linked;
    }

}
