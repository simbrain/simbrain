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
package org.simbrain.network.gui.dialogs.synapse;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.dialogs.synapse.spike_responders.ConvolvedJumpAndDecayPanel;
import org.simbrain.network.gui.dialogs.synapse.spike_responders.JumpAndDecayPanel;
import org.simbrain.network.gui.dialogs.synapse.spike_responders.ProbabilisticSpikeResponderPanel;
import org.simbrain.network.gui.dialogs.synapse.spike_responders.RiseAndDecayPanel;
import org.simbrain.network.gui.dialogs.synapse.spike_responders.StepSpikerPanel;
import org.simbrain.network.synapse_update_rules.spikeresponders.ConvolvedJumpAndDecay;
import org.simbrain.network.synapse_update_rules.spikeresponders.JumpAndDecay;
import org.simbrain.network.synapse_update_rules.spikeresponders.ProbabilisticResponder;
import org.simbrain.network.synapse_update_rules.spikeresponders.RiseAndDecay;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.network.synapse_update_rules.spikeresponders.Step;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>AbstractSpikeResponsePanel</b>.
 */
public abstract class AbstractSpikeResponsePanel extends JPanel {

    /**
     * A mapping of available spike responders to their respective panels. Used
     * as a reference (especially for combo-boxes) by GUI classes.
     */
    public static final HashMap<String, AbstractSpikeResponsePanel> RESPONDER_MAP = new HashMap<String, AbstractSpikeResponsePanel>();

    static {
        RESPONDER_MAP.put(new JumpAndDecay().getDescription(),
                new JumpAndDecayPanel());
        RESPONDER_MAP.put(new ConvolvedJumpAndDecay().getDescription(),
        		new ConvolvedJumpAndDecayPanel());
        RESPONDER_MAP.put(new ProbabilisticResponder().getDescription(),
                new ProbabilisticSpikeResponderPanel());
        RESPONDER_MAP.put(new RiseAndDecay().getDescription(),
                new RiseAndDecayPanel());
        RESPONDER_MAP.put(new Step().getDescription(), new StepSpikerPanel());
    }

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /**
     * A flag used to indicate whether this panel will be replacing spike
     * responders or simply writing to them. In cases where the panel represents
     * the same responder as the responder of each of the synapses (i.e. Step
     * panel &#38; Step spike responder) the synapses' update rules are edited, not
     * replaced. However, if the panel does not correspond to the spike
     * responder of the synapses being edited, then new SpikeResponder objects
     * are created, and replace the old rule. This optimization prevents
     * multiple redundant "instanceof" checks.
     */
    private boolean replacing = true;

    public abstract AbstractSpikeResponsePanel deepCopy();
    
    /**
     * Adds an item.
     *
     * @param text
     *            label of item to add
     * @param comp
     *            component to add
     */
    public void addItem(final String text, final JComponent comp) {
        mainPanel.addItem(text, comp);
    }

    /**
     * Adds an item label.
     *
     * @param text
     *            label to add
     * @param comp
     *            component to label
     */
    public void addItemLabel(final JLabel text, final JComponent comp) {
        mainPanel.addItemLabel(text, comp);
    }

    /**
     * This method is the default constructor.
     */
    public AbstractSpikeResponsePanel() {
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Populate fields with current data.
     *
     * @param spikeResponderList
     *            the list of spike responders being used to ascertain which
     *            values should fill their respective fields.
     */
    public abstract void fillFieldValues(
            final List<SpikeResponder> spikeResponderList);

    /**
     * Populate fields with default data.
     */
    public abstract void fillDefaultValues();

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     * Specifically, this version is typically used to commit changes to a
     * synapse which will be used as a template and copied, since it only takes
     * in one synapse. This typically occurs during the creation of multiple
     * synapses.
     *
     * @param synapse
     *            the synapse to which spike responder changes will be committed
     */
    public abstract void commitChanges(final Synapse synapse);

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     * Specifically, this version is used in the editing process (rather than
     * creation), as it takes in a whole list of synapses and commits changes to
     * the spike responders of all of them in accordance with the values entered
     * into the panel's fields. This method is meant to ensure that each synapse
     * has the correct kind of spike responder, then pass the list to
     * {@link #writeValuesToRules(List)} to make the actual changes.
     *
     * @param synapses
     *            the synapses to which spike responder changes will be
     *            committed
     */
    public abstract void commitChanges(final Collection<Synapse> synapses);

    /**
     * Used internally to actually write all changes to the spike responders of
     * a given list of synapses. Prior to using this method it must be the case
     * that all synapses in the lists' spike responders are of the appropriate
     * type, otherwise a ClassCastException will be thrown.
     *
     * @param synapses
     *            the synapses whose spike responders will be written to based
     *            on the values in their respective fields.
     */
    protected abstract void writeValuesToRules(
            final Collection<Synapse> synapses);

    /**
     * Tells this panel whether it is going to be editing spike responders, or
     * creating new ones and replacing the spike responders of each of the
     * synapses being edited.
     *
     * @param replace
     *            tell the panel if it's replacing responders or editing them
     */
    protected void setReplace(boolean replace) {
        this.replacing = replace;
    }

    /**
     * Are we replacing rules or editing them? Replacing happens when
     * {@link #commitChanges(List)} is called on a synapse panel whose rule is
     * different from the rules of the synapses being edited.
     *
     * @return replacing or editing
     */
    protected boolean isReplace() {
        return replacing;
    }

    /**
     * @return the list of the names of the spike responders for combo-boxes
     */
    public static String[] getResponderList() {
        return RESPONDER_MAP.keySet().toArray(new String[RESPONDER_MAP.size()]);
    }

    /**
     * @return the prototypical model rule represented by this panel.
     */
    public abstract SpikeResponder getPrototypeResponder();

    /**
     * Add notes or other text to bottom of panel. Can be html formatted..
     *
     * @param text
     *            Text to be used for bottom of panel
     */
    public void addBottomText(final String text) {
        JPanel labelPanel = new JPanel();
        JLabel theLabel = new JLabel(text);
        labelPanel.add(theLabel);
        this.add(labelPanel, BorderLayout.SOUTH);
    }

    /**
     * @return the mainPanel
     */
    public LabelledItemPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * @param mainPanel
     *            the mainPanel to set
     */
    public void setMainPanel(LabelledItemPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

}