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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.util.widgets.YesNoNull;

/**
 * Panel showing basic properties of a set of synapses, e.g. id, strength, and
 * whether it is enabled or not.
 */
public class SynapsePropertiesSimple extends JPanel implements EditablePanel {

    /** Id Label. */
    private final JLabel idLabel = new JLabel();

    /** Strength field. */
    private final JTextField tfStrength = new JTextField(10);

    /**
     * A switch for determining whether or not the synapse will send a weighted
     * input.
     */
    private final YesNoNull synapseEnabled = new YesNoNull(
        "Enabled", "Disabled");

    /**
     * A triangle that switches between an up (left) and a down state Used for
     * showing/hiding extra synapse data.
     */
    private final DropDownTriangle detailTriangle;

    /**
     * The extra data panel. Includes: increment, upper bound, lower bound, and
     * priority.
     */
    private final SynapsePropertiesExtended extraDataPanel;

    /**
     * A reference to the parent window, for resizing after panel content
     * changes.
     */
    private final Window parent;

    /** The synapses being modified. */
    private final Collection<Synapse> synapseList;

    /**
     * If true, displays ID info and other fields that would only make sense if
     * multiple synapses are being edited. This value is set automatically
     * unless otherwise specified at construction.
     */
    private boolean displayIDInfo;

    /**
     * Creates a basic synapse info panel. Here whether or not to display ID
     * info is automatically set based on the state of the synapse list.
     *
     * @param synapses
     *            the synapses whose information is being displayed/made
     *            available to edit on this panel
     * @param parent
     *            the parent window for dynamic resizing.
     * @return A basic synapse info panel with the specified parameters
     */
    public static SynapsePropertiesSimple createBasicSynapseInfoPanel(
        final Collection<Synapse> synapses, final Window parent) {
        boolean displayIDInfo = synapses != null && synapses.size() == 1;
        if (displayIDInfo) {
            Iterator<Synapse> synIter = synapses.iterator();
            displayIDInfo = synIter.next().getSource() != null;
        }
        return createBasicSynapseInfoPanel(synapses, parent, displayIDInfo);
    }

    /**
     * Creates a basic synapse info panel. Here the whether or not ID info is
     * displayed is manually set. This is the case when the number of synapses
     * (such as when adding multiple synapses) is unknown at the time of
     * display. In fact this is probably the only reason to use this factory
     * method over {@link #createBasicSynapseInfoPanel(Collection, Window)}.
     *
     * @param synapses
     *            the synapses whose information is being displayed/made
     *            available to edit on this panel
     * @param parent
     *            the parent window for dynamic resizing
     * @param displayIDInfo
     *            whether or not to display ID info
     * @return A basic synapse info panel with the specified parameters
     */
    public static SynapsePropertiesSimple createBasicSynapseInfoPanel(
        final Collection<Synapse> synapses, final Window parent,
        final boolean displayIDInfo) {
        SynapsePropertiesSimple panel = new SynapsePropertiesSimple(synapses,
            parent, displayIDInfo);
        panel.fillFieldValues();
        panel.addListeners();
        return panel;
    }

    public static SynapsePropertiesSimple createBlankSynapseInfoPanel(
    		final Collection<Synapse> synapses, final Window parent,
    		final boolean displayIDInfo) {
        SynapsePropertiesSimple panel = new SynapsePropertiesSimple(synapses,
                parent, displayIDInfo);
        panel.addListeners();
        return panel;
    }
    
    /**
     * Construct the panel.
     *
     * @param synapseList
     *            the synapse list
     * @param parent
     *            the parent window
     * @param displayIDInfo
     *            whether to display ids
     */
    private SynapsePropertiesSimple(final Collection<Synapse> synapseList,
        final Window parent, final boolean displayIDInfo) {
        this.synapseList = synapseList;
        this.parent = parent;
        this.displayIDInfo = displayIDInfo;
        detailTriangle = new DropDownTriangle(UpDirection.LEFT, false, "More",
            "Less", parent);
        extraDataPanel = new SynapsePropertiesExtended(synapseList);
        initializeLayout();
    }

    /**
     * Initialize the basic info panel (generic synapse parameters)
     *
     */
    private void initializeLayout() {

        setLayout(new BorderLayout());

        JPanel basicsPanel = new JPanel(new GridBagLayout());
        basicsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        if (displayIDInfo) {
            gbc.weightx = 0.8;
            gbc.gridwidth = 1;
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(5, 0, 0, 0);
            basicsPanel.add(new JLabel("Synapse Id:"), gbc);

            gbc.gridwidth = 2;
            gbc.gridx = 1;
            basicsPanel.add(idLabel, gbc);
        }

        gbc.weightx = 0.8;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        basicsPanel.add(new JLabel("Strength:"), gbc);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(5, 3, 0, 0);
        gbc.gridwidth = 2;
        gbc.weightx = 0.2;
        gbc.gridx = 1;
        basicsPanel.add(tfStrength, gbc);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.gridwidth = 1;
        gbc.weightx = 0.8;
        gbc.gridx = 0;
        gbc.gridy++;
        basicsPanel.add(new JLabel("Status: "), gbc);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(5, 3, 0, 0);
        gbc.gridwidth = 2;
        gbc.weightx = 0.2;
        gbc.gridx = 1;
        basicsPanel.add(synapseEnabled, gbc);

        gbc.gridwidth = 1;
        int lgap = detailTriangle.isDown() ? 5 : 0;
        gbc.insets = new Insets(10, 5, lgap, 5);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.weightx = 0.2;
        basicsPanel.add(detailTriangle, gbc);

        this.add(basicsPanel, BorderLayout.NORTH);

        extraDataPanel.setVisible(detailTriangle.isDown());

        this.add(extraDataPanel, BorderLayout.SOUTH);

        TitledBorder tb = BorderFactory.createTitledBorder("Basic Data");
        this.setBorder(tb);

    }

    /**
     * A method for adding all internal listeners.
     */
    private void addListeners() {

        // Add a listener to display/hide extra editable synapse data
        detailTriangle.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // Repaint to show/hide extra data
                extraDataPanel.setVisible(detailTriangle.isDown());
                // Resize the parent window accordingly...
                parent.pack();
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

        });
    }

    @Override
    public void fillFieldValues() {
        Synapse synapseRef = synapseList.iterator().next();
        if (synapseList.size() == 1) {
            idLabel.setText(synapseRef.getId());
        } else {
            idLabel.setText(SimbrainConstants.NULL_STRING);
        }

        // (Below) Handle consistency of multiple selections

        // Handle Strength
        if (!NetworkUtils.isConsistent(synapseList, Synapse.class,
            "getStrength")) {
            tfStrength.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfStrength.setText(Double.toString(synapseRef.getStrength()));
        }

        // Handle Enabled
        if (!NetworkUtils.isConsistent(synapseList, Synapse.class,
            "isEnabled")) {
            synapseEnabled.setNull();
        } else {
            synapseEnabled
                .setSelectedIndex(synapseRef.isEnabled()
                    ? YesNoNull
                        .getTRUE() : YesNoNull.getFALSE());
        }

    }

    /**
     * Fills field values based on a given synapse group's values and which
     * synapse polarity it should use to get those values.
     * @param synapseGroup
     * @param polarity
     */
    public void fillFieldValues(SynapseGroup synapseGroup, Polarity polarity) {

        Set<Synapse> synSet = null;
        if (Polarity.EXCITATORY == polarity) {
            if (synapseGroup.hasExcitatory()) {
            	synSet = synapseGroup.getExcitatorySynapses();
            } else {
            	if (synapseList.isEmpty()) {
            		synapseList.add(synapseGroup.getExcitatoryPrototype());
            	}
            	fillFieldValues();
            	return;
            }
        } else if (Polarity.INHIBITORY == polarity) {
            if (synapseGroup.hasInhibitory()) {
            	synSet = synapseGroup.getInhibitorySynapses();
            } else {
            	if (synapseList.isEmpty()) {
            		synapseList.add(synapseGroup.getInhibitoryPrototype());
            	}
            	fillFieldValues();
            	return;
            }
        } else {
            if (!synapseGroup.isEmpty()) {
            	synSet = new HashSet<Synapse>();
            	synSet.addAll(synapseGroup.getExcitatorySynapses());
            	synSet.addAll(synapseGroup.getInhibitorySynapses());
            } else {
            	if (synapseList.isEmpty()) {
            		synapseList.add(synapseGroup.getExcitatoryPrototype());
            		synapseList.add(synapseGroup.getInhibitoryPrototype());
            	}
            	fillFieldValues();
            	return;
            }
        }
        synapseEnabled.setSelected(synapseGroup.isEnabled(polarity));
        Iterator<Synapse> synIter = synSet.iterator();
        double first = synIter.next().getStrength();
        boolean consistent = true;
        while (synIter.hasNext()) {
            consistent = first == synIter.next().getStrength();
            if (!consistent) {
                break;
            }
        }
        tfStrength.setText(consistent ? Double.toString(first)
            : SimbrainConstants.NULL_STRING);
        extraDataPanel.fillFieldValues(synapseGroup, polarity);
    }

    @Override
    public boolean commitChanges() {

        // Strength
        double strength = Utils.doubleParsable(tfStrength);
        if (!Double.isNaN(strength)) {
            for (Synapse s : synapseList) {
                s.setStrength(strength);
            }
        }

        // Enabled?
        boolean enabled = synapseEnabled.getSelectedIndex() == YesNoNull
            .getTRUE();
        if (synapseEnabled.getSelectedIndex() != YesNoNull.getNULL()) {
            for (Synapse s : synapseList) {
                s.setEnabled(enabled);
            }
        }

        extraDataPanel.commitChanges(synapseList);

        return true; // TODO
    }

    /**
     * @return The triangle widget used to view/hide extra data (spec. the extra
     *         data panel {@link #extraDataPanel}).
     */
    public DropDownTriangle getDetailTriangle() {
        return detailTriangle;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    /**
     * @return the extraDataPanel
     */
    public SynapsePropertiesExtended getExtraDataPanel() {
        return extraDataPanel;
    }

    public double getStrength() {
        return Utils.doubleParsable(tfStrength);
    }

}
