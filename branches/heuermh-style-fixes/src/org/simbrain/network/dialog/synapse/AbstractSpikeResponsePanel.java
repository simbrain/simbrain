/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.dialog.synapse;

import org.simbrain.util.LabelledItemPanel;

import java.awt.BorderLayout;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * <b>AbstractSpikeResponsePanel</b>
 */
public abstract class AbstractSpikeResponsePanel extends JPanel {
    public static final String NULL_STRING = "...";
    protected LabelledItemPanel mainPanel = new LabelledItemPanel();
    protected ArrayList spikeResponderList; // The neurons being modified
    protected org.simnet.interfaces.Network parentNet = null;

    public void addItem(String text, JComponent comp) {
        mainPanel.addItem(text, comp);
    }

    public void addItemLabel(JLabel text, JComponent comp) {
        mainPanel.addItemLabel(text, comp);
    }

    public AbstractSpikeResponsePanel() {
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Populate fields with current data
     */
    public abstract void fillFieldValues();

    /**
     * Populate fields with default data
     */
    public abstract void fillDefaultValues();

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public abstract void commitChanges();

    /**
     * @return Returns the spiker_list.
     */
    public ArrayList getSpikeResponderList() {
        return spikeResponderList;
    }

    /**
     * @param spiker_list The spiker_list to set.
     */
    public void setSpikeResponderList(ArrayList spiker_list) {
        this.spikeResponderList = spiker_list;
    }

    /**
     * Add notes or other text to bottom of panel.  Can be html formatted.
     */
    public void addBottomText(String text) {
        JPanel labelPanel = new JPanel();
        JLabel theLabel = new JLabel(text);
        labelPanel.add(theLabel);
        this.add(labelPanel, BorderLayout.SOUTH);
    }
}
