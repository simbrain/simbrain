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
package org.simbrain.workspace.gui;

import org.simbrain.util.ResourceManager;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.CouplingEvents;
import org.simbrain.workspace.gui.couplingmanager.DesktopCouplingManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Displays a list of the current couplings in the network.
 */
public class CouplingListPanel extends JPanel {

    /**
     * List of network couplings.
     */
    private JList couplings = new JList();

    /**
     * Instance of parent frame.
     */
    private JFrame couplingFrame = new JFrame();

    /**
     * Simbrain desktop reference.
     */
    private SimbrainDesktop desktop;

    /**
     * List of couplings.
     */
    private Collection<Coupling> couplingList;

    /**
     * Action which deletes current couplings.
     */
    Action deleteCouplingsAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Eraser.png"));
            putValue(NAME, "Delete couplings");
            putValue(SHORT_DESCRIPTION, "Delete selected couplings");
            CouplingListPanel.this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), this);
            CouplingListPanel.this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), this);
            CouplingListPanel.this.getActionMap().put(this, this);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            desktop.getWorkspace().getCouplingManager().removeCouplings(getSelectedCouplings());
        }
    };

    /**
     * Creates a new coupling list panel using the applicable desktop and
     * coupling lists.
     *
     * @param desktop      Reference to simbrain desktop
     * @param couplingList list of couplings to be shown in window
     */
    public CouplingListPanel(SimbrainDesktop desktop, Collection<Coupling> couplingList) {
        super(new BorderLayout());

        // Reference to the simbrain desktop
        this.desktop = desktop;
        this.couplingList = couplingList;

        // Listens for frame closing for removal of listener.
        // couplingFrame.addWindowListener(new WindowAdapter() {
        //     public void windowClosing(final WindowEvent w) {
        //         desktop.getWorkspace().getCouplingManager().removeCouplingListener(CouplingListPanel.this);
        //     }
        // });
        // desktop.getWorkspace().getCouplingManager().addCouplingListener(this);

        // Populates the coupling list with data.
        couplings.setListData(this.couplingList.toArray());
        couplings.setCellRenderer(new CouplingCellRenderer());

        // Scroll pane for showing lists larger than viewing window and setting
        // maximum size
        JScrollPane listScroll = new JScrollPane(couplings);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Allows the user to delete couplings within the list frame.
        JPanel buttonPanel = new JPanel();
        JButton deleteCouplingButton = new JButton(deleteCouplingsAction);
        buttonPanel.add(deleteCouplingButton);

        // Add scroll pane to JPanel
        add(listScroll, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        couplingFrame.setContentPane(this);

        CouplingEvents events = desktop.getWorkspace().getCouplingManager().getEvents();

        events.onCouplingAdded(c -> updateCouplingsList());
        events.onCouplingRemoved(c -> updateCouplingsList());
        events.onCouplingsRemoved(cl -> updateCouplingsList());


    }

    /**
     * Updates the list of couplings when new couplings are made.
     */
    private void updateCouplingsList() {
        couplingList = desktop.getWorkspace().getCouplings();
        couplings.setListData(desktop.getWorkspace().getCouplings().toArray());
    }

    /**
     * Returns consumers selected in consumer list.
     *
     * @return selected consumers.
     */
    private ArrayList<Coupling> getSelectedCouplings() {
        ArrayList<Coupling> ret = new ArrayList<>();
        for (Object object : couplings.getSelectedValuesList()) {
            ret.add((Coupling) object);
        }
        return ret;
    }

    /**
     * Custom attribute renderer for JList.
     */
    private class CouplingCellRenderer extends DefaultListCellRenderer {
        public java.awt.Component getListCellRendererComponent(final JList list, final Object object, final int index, final boolean isSelected, final boolean cellHasFocus) {
            DefaultListCellRenderer renderer = (DefaultListCellRenderer) super.getListCellRendererComponent(list, object, index, isSelected, cellHasFocus);
            Coupling coupling = (Coupling) object;

            // Set text color based on data type
            renderer.setForeground(DesktopCouplingManager.getColor(coupling.getType()));
            return renderer;
        }
    }

}
