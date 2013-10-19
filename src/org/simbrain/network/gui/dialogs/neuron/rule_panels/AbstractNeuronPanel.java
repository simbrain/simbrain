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
package org.simbrain.network.gui.dialogs.neuron.rule_panels;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>AbstractNeuronPanel</b> is the parent class for all panels used to set
 * parameters of specific neuron rule types.
 * 
 * Optimization has been emphasized for methods intended for neuron creation
 * rather than editing on the assumption that the former will be far more 
 * common for large numbers of neurons. 
 */
public abstract class AbstractNeuronPanel extends JPanel {

	/** Null string. */
	public static final String NULL_STRING = "...";

	/** Main panel. */
	private final LabelledItemPanel mainPanel = new LabelledItemPanel();

	/** 
	 * Is this dialog replacing the update rules of the neurons or just
	 * writing to them? Used as an optimization to act as a flag for
	 * situations where costly "instanceof" checks, text parsing,
	 * and equality checks can be minimized. As the name suggests, these 
	 * optimizations can be performed when we are certain that rules are
	 * being overwritten rather than written to.
	 */
	private boolean replacing = true;
	
	/**
	 * This method is the default constructor.
	 */
	public AbstractNeuronPanel() {
		this.setLayout(new BorderLayout());
		this.add(mainPanel, BorderLayout.CENTER);
	}

	/**
	 * Adds a new item.
	 * 
	 * @param text
	 *            Text to add
	 * @param comp
	 *            SimbrainComponent to add
	 */
	public void addItem(final String text, final JComponent comp) {
		mainPanel.addItem(text, comp);
	}

	/**
	 * Adds a new item label.
	 * 
	 * @param text
	 *            Text to add
	 * @param comp
	 *            Component to add.
	 */
	public void addItemLabel(final JLabel text, final JComponent comp) {
		mainPanel.addItemLabel(text, comp);
	}

	/**
	 * Populate fields with current data.
	 */
	public abstract void fillFieldValues(List<NeuronUpdateRule> ruleList);

	/**
	 * Populate fields with default data.
	 */
	public abstract void fillDefaultValues();

	/**
	 * Called to commit changes to a single neuron. Usually this is a template
	 * neuron intended to be copied for the purpose of creating many new
	 * neurons. Using this method to commit changes to many neurons is not
	 * recommended. Instead pass a list of the neurons to be changed into
	 * {@link #commitChanges(List) commitChanges}.
	 */
	public abstract void commitChanges(Neuron neuron);

	/**
	 * Called externally when the dialog is closed, to commit any changes made
	 * to many neurons simultaneously. This method by default overwrites the
	 * neurons' update rules. To change this behavior set {@link #replacing} to
	 * <b> false </b>, indicating to the panel that it is editing rather than
	 * changing/replacing existing neuron update rules.
	 */
	public abstract void commitChanges(List<Neuron> neuron);

	/**
	 * Writes values from this panel's fields to the model update rule.
	 * @param rule the rule being edited.
	 */
	protected abstract void writeValuesToRule(NeuronUpdateRule rule);
	
	/**
	 * Override to add custom notes or other text to bottom of panel. Can be
	 * html formatted.
	 * 
	 * @param text
	 *            Text to be added
	 */
	public void addBottomText(final String text) {
		JPanel labelPanel = new JPanel();
		JLabel theLabel = new JLabel(text);
		labelPanel.add(theLabel);
		this.add(labelPanel, BorderLayout.SOUTH);
	}

	/**
	 * @return the neuron update rule being edited.
	 */
	public abstract NeuronUpdateRule getPrototypeRule();

	public boolean isReplace() {
		return replacing;
	}

	public void setReplace(boolean replace) {
		this.replacing = replace;
	}

}
