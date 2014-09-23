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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;

/**
 * Panel to display spike responder settings.
 *
 * @author ztosi
 */
public class SpikeResponderSettingsPanel extends JPanel {

    /**
     * The default display state of the synapse panel. Currently, True, that is,
     * by default, the synapse panel corresponding to the rule in the combo box
     * is visible.
     */
    private static final boolean DEFAULT_SP_DISPLAY_STATE = true;

    /** Spike responder type combo box. */
    private final JComboBox<String> cbResponderType = new JComboBox<String>(
            AbstractSpikeResponsePanel.getResponderList());

    /** The synapses being modified. */
    private final Collection<Synapse> synapseList;

    /** The currently displayed spike responder panel. */
    private AbstractSpikeResponsePanel spikeResponderPanel;

    /** For showing/hiding the synapse panel. */
    private final DropDownTriangle displaySPTriangle;

    /**
     * The originally displayed abstract spike response panel. If the currently
     * displayed panel is not the same as the starting panel, then we can be
     * sure that we are not editing spike responders, but rather are replacing
     * them.
     */
    private final AbstractSpikeResponsePanel startingPanel;

    /**
     * The parent window containing this panel, access to which is required for
     * resizing purposes.
     */
    private final Window parent;

    /**
     * A constructor that sets up the panel in its default display state.
     *
     * @param synapseList
     *            the list of synapses, the spike responders of which will be
     *            displayed for edit
     * @param parent
     *            the parent window
     */
    public SpikeResponderSettingsPanel(final Collection<Synapse> synapseList,
            final Window parent) {
        this(synapseList, DEFAULT_SP_DISPLAY_STATE, parent);
    }

    /**
     * A constructor that sets up the panel with the spike response panel either
     * hidden or displayed.
     *
     * @param synapseList
     *            the list of synapses, the spike responders of which will be
     *            displayed for edit
     * @param startingState
     *            whether or not the spike response panel will start out hidden
     *            or visible
     * @param parent
     *            the parent window
     */
    public SpikeResponderSettingsPanel(final Collection<Synapse> synapseList,
            final boolean startingState, final Window parent) {
        this.synapseList = synapseList;
        this.parent = parent;
        displaySPTriangle = new DropDownTriangle(UpDirection.LEFT,
                startingState, "Settings", "Settings", parent);
        // Find out what panel we're starting with and fill it as necessary
        initSpikeResponderType();
        // After the starting spike responder panel has been initialized we can
        // now assign it to the final variable startingPanel...
        startingPanel = spikeResponderPanel;
        initializeLayout();
        addListeners();
    }

    /**
     * Lays out this panel.
     */
    private void initializeLayout() {

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        JPanel tPanel = new JPanel();
        tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.X_AXIS));
        tPanel.add(cbResponderType);
        tPanel.add(Box.createHorizontalGlue());
        tPanel.add(displaySPTriangle);

        tPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tPanel.setBorder(padding);
        this.add(tPanel);

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        spikeResponderPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        spikeResponderPanel.setBorder(padding);
        spikeResponderPanel.setVisible(displaySPTriangle.isDown());
        this.add(spikeResponderPanel);

        TitledBorder tb2 = BorderFactory.createTitledBorder("Spike Responder");
        this.setBorder(tb2);

    }

    /**
     * Adds the listeners to this dialog.
     */
    private void addListeners() {
        displaySPTriangle.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent arg0) {

                spikeResponderPanel.setVisible(displaySPTriangle.isDown());
                repaint();
                parent.pack();

            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
            }

        });

        cbResponderType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                spikeResponderPanel = AbstractSpikeResponsePanel.RESPONDER_MAP
                        .get(cbResponderType.getSelectedItem()).deepCopy();

                // Is the current panel different from the starting panel?
                boolean replace = spikeResponderPanel != startingPanel;

                if (replace) {
                    // If so we have to fill the new panel with default values
                    spikeResponderPanel.fillDefaultValues();
                }

                // Tell the new panel whether it will have to replace
                // synapse update rules or edit them upon commit.
                spikeResponderPanel.setReplace(replace);

                repaintPanel();
                parent.pack();

            }

        });

    }

    /**
     * Called to repaint the panel based on changes in the to the selected
     * synapse type.
     */
    public void repaintPanel() {
        removeAll();
        initializeLayout();
        repaint();
    }

    /**
     * Initialize the main synapse panel based on the type of the selected
     * synapses.
     */
    private void initSpikeResponderType() {
        List<SpikeResponder> srList = SpikeResponder
                .getResponderList(synapseList);
        boolean consistent = srList.size() == synapseList.size();
        if (!consistent
                || !NetworkUtils.isConsistent(srList, SpikeResponder.class,
                        "getType")) {
            cbResponderType.addItem(SimbrainConstants.NULL_STRING);
            cbResponderType
                    .setSelectedIndex(cbResponderType.getItemCount() - 1);
            spikeResponderPanel = new EmptySpikeResponsePanel();
        } else {
            String spikeResponderName = srList.get(0).getDescription();
            spikeResponderPanel = AbstractSpikeResponsePanel.RESPONDER_MAP
                    .get(spikeResponderName).deepCopy();
            spikeResponderPanel.fillFieldValues(srList);
            cbResponderType.setSelectedItem(spikeResponderName);
        }

    }

    /**
     * @return the name of the selected synapse update rule
     */
    public JComboBox<String> getCbSynapseType() {
        return cbResponderType;
    }

    /**
     * @return the currently displayed synapse panel
     */
    public AbstractSpikeResponsePanel getSpikeResponsePanel() {
        return spikeResponderPanel;
    }

    /**
     * @param spikeResponderPanel
     *            set the currently displayed spike responder panel to the
     *            specified panel
     */
    public void setSynapsePanel(AbstractSpikeResponsePanel spikeResponderPanel) {
        this.spikeResponderPanel = spikeResponderPanel;
    }

    /**
     *
     * @author zach
     *
     */
    private class EmptySpikeResponsePanel extends AbstractSpikeResponsePanel {

        @Override
        public void fillFieldValues(List<SpikeResponder> spikeResponderList) {
        }

        @Override
        public void fillDefaultValues() {
        }

        @Override
        public void commitChanges(Synapse synapse) {
        }

        @Override
        public void commitChanges(Collection<Synapse> synapses) {
        }

        @Override
        public void writeValuesToRules(Collection<Synapse> synapses) {
        }

        @Override
        public SpikeResponder getPrototypeResponder() {
            return null;
        }

		@Override
		public AbstractSpikeResponsePanel deepCopy() {
			return null;
		}

    }

    /**
     * Commit changes to the panel.
     *
     * @return success or not, but does nothing now.
     */
    public boolean commitChanges() {
        spikeResponderPanel.commitChanges(synapseList);
        return true; // TODO;
    }

}
