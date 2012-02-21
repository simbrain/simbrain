/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.connect;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Hashtable;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.connections.Sparse;
import org.simbrain.network.gui.NetworkPanel;

/**
 * <b>SparsePanel</b> creates a dialog for setting preferences of Sparse neuron
 * connections.
 *
 * @author ztosi
 */
public class SparsePanel extends AbstractConnectionPanel {

    private final ExcitatoryInhibitoryPropertiesPanel eipPanel;

    /** A slider for setting the sparsity of the connections. */
    private JSlider sparsitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);

    /** A text field for setting the sparsity of the connections. */
    private JFormattedTextField tfSparsity = new JFormattedTextField(
            NumberFormat.getNumberInstance());

    /**
     * A text field allowing the user to specify the number of outgoing synapses
     * per source neuron.
     */
    private JFormattedTextField synapsesPerSource = new JFormattedTextField(
            NumberFormat.getNumberInstance());

    /**
     * A check box for determining if the number of outgoing synapses per source
     * neuron should be equalized.
     */
    private JCheckBox sparseSpecific = new JCheckBox();

    /** A check box for determining if self-connections are to be allowed. */
    private JCheckBox allowSelfConnect = new JCheckBox();

    /** The number of target neurons */
    private final int numTargs;

    /**
     * A flag determining if an action was initiated by a user (useful for
     * reciprocal action listeners).
     */
    private boolean userFlag = true;

    /**
     * This method is the default constructor.
     *
     * @param connection type
     */
    public SparsePanel(final Sparse connection, final NetworkPanel networkPanel) {
        super(connection);
        eipPanel = new ExcitatoryInhibitoryPropertiesPanel(connection);
        numTargs = networkPanel.getSelectedModelNeurons().size();
        fillFieldValues();
        initializeSparseSlider();
        addChangeListeners();
        addActionListeners();
        initializeLayout();

    }

    /**
     * Initializes the custom layout of the sparse panel.
     */
    private void initializeLayout() {

        JPanel sparseContainer = new JPanel();
        this.add(sparseContainer);
        JScrollPane pScroller = new JScrollPane(sparseContainer);
        this.add(pScroller, BorderLayout.CENTER);

        sparseContainer.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.gridheight = 4;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        sparseContainer.add(initializeSparseSubPanel(), gbc);

        gbc.insets = new Insets(10, 10, 0, 10);
        gbc.gridy = 4;
        gbc.gridheight = 1;
        sparseContainer.add(new JSeparator(), gbc);

        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy = 5;
        gbc.gridheight = 9;
        sparseContainer.add(eipPanel, gbc);

        gbc.insets = new Insets(10, 10, 0, 10);
        gbc.gridy = 14;
        gbc.gridheight = 1;
        sparseContainer.add(new JSeparator(), gbc);

        gbc.gridy = 15;
        gbc.gridwidth = 1;
        sparseContainer.add(new JLabel("Allow Self-Connections: "), gbc);

        gbc.gridx = 2;
        sparseContainer.add(allowSelfConnect, gbc);

    }

    private JPanel initializeSparseSubPanel() {
        JPanel ssp = new JPanel();
        ssp.setLayout((new GridBagLayout()));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 10, 0, 10);
        ssp.add(new JLabel("No Connections"), gbc);
        gbc.gridx = 2;
        ssp.add(new JLabel("Full Connectivity"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        ssp.add(sparsitySlider, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JPanel connectivityPanel = new JPanel();
        connectivityPanel.add(new JLabel("% Connectivity: "));
        Dimension sSize = tfSparsity.getPreferredSize();
        sSize.width = 40;
        tfSparsity.setPreferredSize(sSize);
        connectivityPanel.add(tfSparsity);
        ssp.add(connectivityPanel, gbc);


        gbc.gridx = 0;
        gbc.gridy = 3;
        ssp.add(new JLabel(" Connectivity is the same for each neuron's fanout:"), gbc);
        gbc.gridx = 1;
        ssp.add(sparseSpecific, gbc);
        gbc.gridx = 2;
        sSize = synapsesPerSource.getPreferredSize();
        sSize.width = 40;
        synapsesPerSource.setPreferredSize(sSize);
        synapsesPerSource.setToolTipText("Number of outgoing synapses to be added to each source neuron.");
        ssp.add(synapsesPerSource, gbc);

        return ssp;
    }

    /**
     * Initializes the sparse slider.
     */
    private void initializeSparseSlider() {

        sparsitySlider.setMajorTickSpacing(10);
        sparsitySlider.setMinorTickSpacing(2);
        sparsitySlider.setPaintTicks(true);

        Hashtable<Integer, JLabel> labelTable2 = new Hashtable<Integer, JLabel>();
        labelTable2.put(new Integer(0), new JLabel("0%"));
        labelTable2.put(new Integer(100), new JLabel("100%"));
        sparsitySlider.setLabelTable(labelTable2);
        sparsitySlider.setPaintLabels(true);
    }

    /**
     * Adds change listeners specific to sparse panel: Sparsity slider, sparsity
     * text field, and syns/source field.
     */
    private void addChangeListeners() {

        sparsitySlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                if (!source.getValueIsAdjusting() && source == sparsitySlider) {
                    if (userFlag) {
                        userFlag = false;
                        double val = (double) (sparsitySlider.getValue());
                        tfSparsity.setValue(new Double(val));
                        int sps = (int) ((val / 100) * numTargs);
                        synapsesPerSource.setValue(new Integer(sps));

                    } else {
                        userFlag = true;
                    }

                }
            }

        });

        synapsesPerSource.addPropertyChangeListener("value",
                new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent arg0) {
                        if (arg0.getSource() == synapsesPerSource
                                && sparseSpecific.isSelected()) {
                            double sparse;
                            if (userFlag) {
                                userFlag = false;
                                if (synapsesPerSource != null) {
                                    sparse = ((Number) synapsesPerSource.getValue())
                                            .doubleValue() / numTargs;
                                    tfSparsity.setValue(new Double(sparse));
                                    int sVal = (int) (sparse * 100);
                                    sparsitySlider.setValue(new Integer(sVal));
                                }
                            } else {
                                userFlag = true;
                            }
                        }
                    }
                });

        tfSparsity.addPropertyChangeListener("value",
                new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getSource() == tfSparsity) {
                            int sps;
                            if (userFlag) {
                                userFlag = false;
                                if (tfSparsity.getValue() != null) {
                                    sps = (int) ((((Number) tfSparsity.getValue())
                                            .doubleValue() /100) * numTargs);
                                    synapsesPerSource.setValue(new Integer(sps));
                                    int sVal = (int) (((Number) tfSparsity
                                            .getValue()).doubleValue() * 100);
                                    sparsitySlider.setValue(new Integer(sVal));
                                }
                            } else {
                                userFlag = true;
                            }
                        }
                    }
                });

    }

    /**
     * Adds action listeners specific to sparse panel: sparse specific check
     * box.
     */
    private void addActionListeners() {

        sparseSpecific.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (arg0.getSource() == sparseSpecific) {
                    if (sparseSpecific.isSelected()) {
                        synapsesPerSource.setEnabled(true);
                    } else {
                        synapsesPerSource.setEnabled(false);
                    }
                }
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    public void commitChanges() {
        ((Sparse) connection).setSparseSpecific(sparseSpecific.isSelected());
        ((Sparse) connection).setSparsity(((Number) tfSparsity.getValue())
                .doubleValue()/100);
        ((Sparse) connection).setAllowSelfConnection(allowSelfConnect
                .isSelected());
        eipPanel.commitChanges();
    }

    /**
     * {@inheritDoc}
     */
    public void fillFieldValues() {
        tfSparsity.setValue(new Double(((Sparse) connection).getSparsity()) * 100);
        synapsesPerSource.setValue(new Integer((int) (numTargs * ((Number) tfSparsity
                .getValue()).doubleValue()/100)));
        synapsesPerSource.setEnabled(false);
    }

}
