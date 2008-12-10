/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.workspace.actions;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.gui.AttributePanel;
import org.simbrain.workspace.gui.CouplingListPanel;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * Opens and displays a list of the current couplings.
 */
public final class OpenCouplingListAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /** Reference to Simbrain desktop. */
    private SimbrainDesktop desktop;
    

    /**
     * Create a coupling list with the specified
     * workspace.
     *
     * @param desktop reference to simbrain desktop.
     */
    public OpenCouplingListAction(final SimbrainDesktop desktop) {
        super("Open coupling list");
        this.desktop = desktop;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("BothWays.png"));
        putValue(SHORT_DESCRIPTION, "Open coupling list");
    }

    /**
     * @see AbstractAction
     * @param event Action event
     */
    public void actionPerformed(final ActionEvent event) {
        final JFrame frame = new JFrame("Coupling List");
        JComponent cl = new CouplingListPanel(desktop,
                new Vector(desktop.getWorkspace().getCouplingManager().getCouplings()));
//        JComponent cl = new AttributePanel(desktop);
        MyGlassPane pane = new MyGlassPane();
        frame.setContentPane(cl);
        frame.setGlassPane(pane);
        frame.pack();
        pane.repaint();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }

    /**
     * Class for creating a glass pane. Used for when no data is available.
     */
    class MyGlassPane extends JComponent {
        public void paint(Graphics g) {
            g.setColor(Color.red);
            g.fillRect(0, 0, 100, 100);
            System.out.println("Inside Paint Method");
        }
    }
}