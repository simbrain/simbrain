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
package org.simbrain.network.gui.dialogs.connect;

import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.connections.Sparse;

/**
 * The <b>SparsityAdjustmentPanel</b> is a sub-panel for other connection panels
 * which adjusts the sparsity of the connection. This panel includes a text
 * field for setting the sparsity as well as a slider and a text field
 * displaying how many efferents per source neuron there would be if efferents
 * are equalized. All of these are kept in sync.
 *
 * @author ztosi
 *
 */
public class SparsityAdjustmentPanel extends JPanel {

    /** A slider for setting the sparsity of the connections. */
    private JSlider sparsitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);

    /** A text field for setting the sparsity of the connections. */
    private JFormattedTextField sparsity = new JFormattedTextField(
            NumberFormat.getNumberInstance());

    /**
     * A text field allowing the user to specify the number of outgoing synapses
     * per source neuron.
     */
    private JFormattedTextField synsPerSource = new JFormattedTextField(
            NumberFormat.getNumberInstance());

    /**
     * A check box for determining if the number of outgoing synapses per source
     * neuron should be equalized.
     */
    private JCheckBox sparseSpecific = new JCheckBox();

    /**
     * The sparse connection object to which changes to this panel will be
     * committed.
     */
    private Sparse connection;

    /**
     * The number of target neurons being connected to. Used for determining the
     * number of efferents per source neuron when efferents are equalized.
     */
    private int numTargs;

    private boolean allowSelfConnect = false;

    /**
     * Constructs a gui panel for adjusting the sparsity of a sparse connect
     * neurons object, and initializes all appropriate listeners.
     *
     * @param connection the connection object this panel will act on
     * @param numTargs the number of target neurons being connected to
     */
    public SparsityAdjustmentPanel(Sparse connection, int numTargs) {
        this.connection = connection;
        this.numTargs = numTargs;
        fillFieldValues();
        initializeSparseSlider();
        addChangeListeners();
        addActionListeners();
        initializeLayout();
    }

    /**
     * Initializes the panel's layout
     */
    private void initializeLayout() {
        this.setLayout((new GridBagLayout()));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        this.add(sparsitySlider, gbc);

        JPanel sparseTextPanel = new JPanel(new FlowLayout());
        sparseTextPanel.add(new JLabel("Sparsity: "));
        Dimension sSize = sparsity.getPreferredSize();
        sSize.width = 40;
        sparsity.setPreferredSize(sSize);
        sparseTextPanel.add(sparsity);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        this.add(sparseTextPanel, gbc);

        JPanel equalizerPanel = new JPanel(new FlowLayout());
        equalizerPanel.add(new JLabel("Equalize Efferents:"));
        equalizerPanel.add(sparseSpecific);
        sSize = synsPerSource.getPreferredSize();
        sSize.width = 40;
        synsPerSource.setPreferredSize(sSize);
        gbc.gridx = 2;
        equalizerPanel.add(synsPerSource);
        this.add(equalizerPanel, gbc);

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
                if (source == sparsitySlider) {
                    if (!consistentValues()) {
                        int nt = allowSelfConnect ? numTargs : numTargs - 1;
                        double val = (double) (sparsitySlider.getValue()) / 100;
                        sparsity.setValue(new Double(val));
                        int sps = (int) (val * nt);
                        synsPerSource.setValue(new Integer(sps));

                    }
                }
            }

        });

        synsPerSource.addPropertyChangeListener("value",
                new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent arg0) {
                        if (arg0.getSource() == synsPerSource
                                && sparseSpecific.isSelected()) {
                            double sparse;
                            if (!consistentValues()) {
                                int nt = allowSelfConnect ? numTargs
                                        : numTargs - 1;
                                if (synsPerSource != null) {
                                    sparse = ((Number) synsPerSource.getValue())
                                            .doubleValue() / nt;
                                    sparsity.setValue(new Double(sparse));
                                    int sVal = (int) (sparse * 100);
                                    sparsitySlider.setValue(new Integer(sVal));
                                }
                            }
                        }
                    }
                });

        sparsity.addPropertyChangeListener("value",
                new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getSource() == sparsity) {
                            int sps;
                            if (!consistentValues()) {
                                int nt = allowSelfConnect ? numTargs
                                        : numTargs - 1;
                                if (sparsity.getValue() != null) {
                                    sps = (int) (((Number) sparsity.getValue())
                                            .doubleValue() * nt);
                                    synsPerSource.setValue(new Integer(sps));
                                    int sVal = (int) (((Number) sparsity
                                            .getValue()).doubleValue() * 100);
                                    sparsitySlider.setValue(new Integer(sVal));
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Checks if all values in the text fields and sliders are consistent. Used
     * to prevent infinite listener loops. TODO: Replace with switchable
     * (Simbrain Utils) listeners/ investigate event source issue
     *
     * @return true or false: are the values in the fields consistent?
     */
    private boolean consistentValues() {
        int spVal = (int) (Double.parseDouble(sparsity.getText()) * 100);
        if (spVal > 100) {
            sparsity.setText("1.0");
            spVal = 100;
        }
        if (spVal < 0) {
            sparsity.setText("0");
            spVal = 0;
        }
        int spsVal = (int) (100 * Double.parseDouble(sparsity.getText()) / numTargs);

        int slideVal = sparsitySlider.getValue();

        return slideVal == spsVal && spsVal == spVal;
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
                        synsPerSource.setEnabled(true);
                    } else {
                        synsPerSource.setEnabled(false);
                    }
                }
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    public void fillFieldValues() {
        if (connection == null || !(connection instanceof Sparse)) {
            sparsity.setValue(new Double(Sparse.getDEFAULT_SPARSITY()));
            synsPerSource.setValue(new Integer(
                    (int) (numTargs * ((Number) Sparse.getDEFAULT_SPARSITY())
                            .doubleValue())));
            sparsitySlider.setValue((int) (Sparse.getDEFAULT_SPARSITY() * 100));
        } else {
            sparsity.setValue(connection.getSparsity());
            synsPerSource.setValue(new Integer((int) (numTargs * connection
                    .getSparsity())));
            sparsitySlider.setValue((int) (connection.getSparsity() * 100));
        }

        synsPerSource.setEnabled(false);
    }

    /**
     * Fills the field values for this panel based on an already existing
     * connection tied to a synapse group. This method also sets the underlying
     * connect neurons object to the connection parameter.
     *
     * @param connection the connection object used to determine field values
     *            for this panel.
     */
    public void fillFieldValues(Sparse connection) {
        this.connection = connection;
        double connectivity = connection.getSparsity();
        sparsity.setValue(connectivity);
        sparseSpecific.setSelected(connection.isAllowSelfConnection());
        numTargs = connection.getTargetNeurons().size();
        synsPerSource.setValue((int) (connectivity * numTargs));
        sparsitySlider.setValue((int) (connectivity * 100));
    }

    /**
     * Commits the values set in this panels fields to the model connection
     * object
     */
    public void commitChanges() {
        ((Sparse) connection).setSparseSpecific(sparseSpecific.isSelected());
        ((Sparse) connection).setSparsity(((Number) sparsity.getValue())
                .doubleValue());
    }

    /**
     *
     * @return
     */
    public boolean isAllowSelfConnect() {
        return allowSelfConnect;
    }

    /**
     *
     * @param allowSelfConnect
     */
    public void setAllowSelfConnect(boolean allowSelfConnect) {
        this.allowSelfConnect = allowSelfConnect;
        int nt = allowSelfConnect ? numTargs : numTargs - 1;
        int sps = (int) (((Number) sparsity.getValue()).doubleValue() * nt);
        synsPerSource.setValue(new Integer(sps));
    }

}
