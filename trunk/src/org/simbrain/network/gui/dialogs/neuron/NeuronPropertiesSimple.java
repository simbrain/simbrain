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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.EditablePanel;

/**
 * Panel showing activation and label properties of a neuron.
 *
 * @author ztosi
 * @author jyoshimi
 *
 */
@SuppressWarnings("serial")
public class NeuronPropertiesSimple extends JPanel implements EditablePanel {

    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Activation field. */
    private JTextField tfActivation = new JTextField();

    /** Label Field. */
    private final JTextField tfNeuronLabel = new JTextField();

    /** The neuron Id. */
    private final JLabel idLabel = new JLabel();

    /**
     * The extra data panel. Includes: increment, upper bound, lower bound, and
     * priority.
     */
    private final NeuronPropertiesExtended extraDataPanel;

    /** The neurons being modified. */
    private final List<Neuron> neuronList;

    /**
     * A triangle that switches between an up (left) and a down state Used for
     * showing/hiding extra neuron data.
     */
    private final DropDownTriangle detailTriangle;

    /**
     * A reference to the parent window for resizing.
     */
    private final Window parent;

    /**
     * If true, displays ID info and other fields that would only make sense if
     * multiple neurons are being edited. This value is set automatically unless
     * otherwise specified at construction.
     */
    private boolean displayIDInfo;

    /**
     * Creates a basic neuron info panel. Here whether or not to display ID info
     * is automatically set based on the state of the neuron list.
     *
     * @param neuronList the neurons whose information is being displayed/made
     *            available to edit on this panel
     * @param parent the parent window for dynamic resizing.
     * @return A basic neuron info panel with the specified parameters
     */
    public static NeuronPropertiesSimple createBasicNeuronInfoPanel(
        final List<Neuron> neuronList, final Window parent) {
        return createBasicNeuronInfoPanel(neuronList, parent,
            !(neuronList == null || neuronList.size() != 1));
    }

    /**
     * Creates a basic neuron info panel. Here the whether or not ID info is
     * displayed is manually set. This is the case when the number of neurons
     * (such as when adding multiple neurons) is unknown at the time of display.
     * In fact this is probably the only reason to use this factory method over
     * {@link #createBasicNeuronInfoPanel(List, Window)}.
     *
     * @param neuronList the neurons whose information is being displayed/made
     *            available to edit on this panel
     * @param parent the parent window for dynamic resizing
     * @param displayIDInfo whether or not to display ID info
     * @return A basic neuron info panel with the specified parameters
     */
    public static NeuronPropertiesSimple createBasicNeuronInfoPanel(
        final List<Neuron> neuronList, final Window parent,
        final boolean displayIDInfo) {
        NeuronPropertiesSimple bnip = new NeuronPropertiesSimple(neuronList,
            parent, displayIDInfo);
        bnip.addListeners();
        return bnip;
    }

    /**
     *
     * @param neuronList
     * @param parent
     * @param displayIDInfo
     */
    private NeuronPropertiesSimple(final List<Neuron> neuronList,
        final Window parent, final boolean displayIDInfo) {
        this.neuronList = neuronList;
        this.parent = parent;
        this.displayIDInfo = displayIDInfo;
        detailTriangle = new DropDownTriangle(UpDirection.LEFT, true, "More",
            "Less", parent);
        extraDataPanel = new NeuronPropertiesExtended(this.neuronList, parent);
        initializeLayout();
        fillFieldValues();
    }

    /**
     * Initialize the basic info panel (generic neuron parameters)
     */
    private void initializeLayout() {

        setLayout(new BorderLayout());

        JPanel basicStatsPanel = new JPanel();
        basicStatsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridLayout gL = new GridLayout(0, 2);
        gL.setVgap(2);
        basicStatsPanel.setLayout(gL);
        if (displayIDInfo) {
            basicStatsPanel.add(new JLabel("Neuron Id:"));
            basicStatsPanel.add(idLabel);
        }
        basicStatsPanel.add(new JLabel("Activation:"));
        basicStatsPanel.add(tfActivation);
        // if (!multiFlag) {
        // TODO: Visible or not if multiple or no neurons are being edited?
        basicStatsPanel.add(new JLabel("Label:"));
        basicStatsPanel.add(tfNeuronLabel);
        // }

        JPanel ddTrianglePanel = new JPanel();
        ddTrianglePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        ddTrianglePanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        ddTrianglePanel.add(detailTriangle);

        this.add(basicStatsPanel, BorderLayout.NORTH);

        this.add(ddTrianglePanel, BorderLayout.CENTER);

        extraDataPanel.setVisible(detailTriangle.isDown());

        this.add(extraDataPanel, BorderLayout.SOUTH);

        TitledBorder tb = BorderFactory.createTitledBorder("Basic Data");
        this.setBorder(tb);

    }

    /**
     * Called Externally to repaint this panel based on whether or not extra
     * data is displayed.
     */
    public void repaintPanel() {
        extraDataPanel.setVisible(detailTriangle.isDown());
        repaint();
    }

    /**
     * A method for adding all internal listeners.
     */
    private void addListeners() {

        // Add a listener to display/hide extra editable neuron data
        detailTriangle.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Repaint to show/hide extra data
                repaintPanel();
                // Alert the panel/dialog/frame this is embedded in to
                // resize itself accordingly
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

        Neuron neuronRef = neuronList.get(0);
        if (neuronList.size() == 1) {
            idLabel.setText(neuronRef.getId());
        } else {
            idLabel.setText(NULL_STRING);
        }

        // (Below) Handle consistency of multiple selections

        // Handle Activation
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
            "getActivation")) {
            tfActivation.setText(NULL_STRING);
        } else {
            tfActivation.setText(Double.toString(neuronRef.getActivation()));
        }

        // Handle Label
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getLabel")) {
            tfNeuronLabel.setText(NULL_STRING);
        } else {
            tfNeuronLabel.setText(neuronRef.getLabel());
        }

    }

    @Override
    public boolean commitChanges() {

        boolean success = true;

        // Activation
        double act = Utils.doubleParsable(tfActivation);
        if (!Double.isNaN(act)) {
            for (int i = 0; i < neuronList.size(); i++) {
                neuronList.get(i).forceSetActivation(
                    Double.parseDouble(tfActivation.getText()));
            }
        } else {
            // Only successful if the field can't be parsed because
            // it is a NULL_STRING standing in for multiple values
            success &= tfActivation.getText().matches(NULL_STRING);
        }

        // Label
        if (!tfNeuronLabel.getText().equals(NULL_STRING)) {
            for (int i = 0; i < neuronList.size(); i++) {
                neuronList.get(i).setLabel(tfNeuronLabel.getText());
            }
        }
        if (!neuronList.isEmpty()) {
            neuronList.get(0).getNetwork().fireNetworkChanged();
        }

        success &= extraDataPanel.commitChanges();

        return success;

    }

    /**
     * @return the extraDataPanel
     */
    public NeuronPropertiesExtended getExtraDataPanel() {
        return extraDataPanel;
    }

    /**
     * @return the detailTriangle
     */
    public DropDownTriangle getDetailTriangle() {
        return detailTriangle;
    }

    /**
     * @return {@link #displayIDInfo}
     */
    public boolean isMultiFlag() {
        return displayIDInfo;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

}
