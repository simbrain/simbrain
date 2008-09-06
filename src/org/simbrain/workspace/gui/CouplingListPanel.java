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
package org.simbrain.workspace.gui;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.simbrain.workspace.Coupling;

/**
 * Displays a list of the current couplings in the network.
 *
 */
public class CouplingListPanel extends JPanel {

    /** List of network couplings. */
    private JList couplings = new JList();

    /** Simbrain desktop reference. */
    private final SimbrainDesktop desktop;

    /** Vertical screen resolution. */
    private final double screenHeight = java.awt.Toolkit.getDefaultToolkit().
            getScreenSize().getHeight();

    /** Height of an individual cell in pixels. */
    private final int cellHeight = 17;

    /** Percentage of screen for window to utilize. */
    private final double windowPercentage = 0.86;

    /** Maximum number of cells to be visible for a given screen resolution. */
    private final double maxCellsVisible = screenHeight / cellHeight * windowPercentage;

    /**
     * Creates a new coupling list panel using the applicable desktop and coupling lists.
     * @param desktop Reference to simbrain desktop
     * @param couplingList list of couplings to be shown in window
     */
    public CouplingListPanel(final SimbrainDesktop desktop, final Vector<Coupling> couplingList) {
        //Layout manager for the JPanel.
        super(new BorderLayout());

        // Reference to the simbrain desktop
        this.desktop = desktop;

        //Populates the coupling list with data.
        couplings.setListData(couplingList);

        // Sets the height of the cells.
        couplings.setFixedCellHeight(cellHeight);
        // Dynamically sets the number of rows that are visible.
        couplings.setVisibleRowCount(Math.min(couplingList.size(), (int) maxCellsVisible));

        //Scroll pane for showing lists larger than viewing window and setting maximum size
        JScrollPane listScroll = new JScrollPane(couplings);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Add scroll pane to JPanel
        add(listScroll, BorderLayout.CENTER);
        
    }

}
