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
package org.simbrain.network.gui.dialogs.neuron;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>AbstractNeuronPanel</b> is the parent class for all panels used to set
 * parameters of specific neuron rule types.
 */
public abstract class AbstractNeuronPanel extends JPanel {

    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Parent network. */
    protected final Network parentNet;

    /** Main panel. */
    private final LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** The neuron update rules whose values are being modified. */
    protected ArrayList<NeuronUpdateRule> ruleList;

    /**
     * This method is the default constructor.
     */
    public AbstractNeuronPanel(Network parentNetwork) {
        this.parentNet = parentNetwork;
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Adds a new item.
     *
     * @param text Text to add
     * @param comp SimbrainComponent to add
     */
    public void addItem(final String text, final JComponent comp) {
        mainPanel.addItem(text, comp);
    }

    /**
     * Adds a new item label.
     *
     * @param text Text to add
     * @param comp Component to add.
     */
    public void addItemLabel(final JLabel text, final JComponent comp) {
        mainPanel.addItemLabel(text, comp);
    }

    /**
     * Populate fields with current data.
     */
    public abstract void fillFieldValues();

    /**
     * Populate fields with default data.
     */
    public abstract void fillDefaultValues();

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public abstract void commitChanges();

    /**
     * @return Returns the neuron_list.
     */
    public ArrayList<NeuronUpdateRule> getRuleList() {
        return ruleList;
    }

    /**
     * @param ruleList The neuron_list to set.
     */
    public void setRuleList(final ArrayList<NeuronUpdateRule> ruleList) {
        this.ruleList = ruleList;
    }

    /**
     * Override to add custom notes or other text to bottom of panel. Can be
     * html formatted.
     *
     * @param text Text to be added
     */
    public void addBottomText(final String text) {
        JPanel labelPanel = new JPanel();
        JLabel theLabel = new JLabel(text);
        labelPanel.add(theLabel);
        this.add(labelPanel, BorderLayout.SOUTH);
    }

    /**
     * @return the parentNet
     */
    public Network getParentNetwork() {
        return parentNet;
    }
}
