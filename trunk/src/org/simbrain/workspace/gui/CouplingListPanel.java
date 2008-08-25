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

    /** Panel for displaying couplingins. */
    private JPanel couplingPanel;

    /** List of network couplings. */
    private JList couplingList = new JList();

    /** Simbrain desktop reference. */
    private final SimbrainDesktop desktop;


    /**
     * Creates a new coupling list panel using the applicable desktop and coupling lists.
     * @param desktop
     * @param couplingsList
     */
    public CouplingListPanel(final SimbrainDesktop desktop, final Vector<Coupling> couplingsList) {
        this.desktop = desktop;
        couplingList.setListData(couplingsList);
        init();
    }

    /**
     * Initializes all relevant data needed for creation of frame.
     */
    private void init() {

        createFrame();

    }

    /**
     * Creates the frame for the display of couplings.
     */
    private void createFrame() {
        couplingPanel = new JPanel(new BorderLayout());
        JScrollPane listScroll = new JScrollPane(couplingList);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        couplingPanel.add("Center", listScroll);
        this.add(couplingPanel);
        
    }
}
