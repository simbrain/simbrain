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
package org.simbrain.network.gui.dialogs.connect.connector_panels;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.AbstractConnectionPanel;
import org.simbrain.util.SwitchableChangeListener;
import org.simbrain.util.SwitchablePropertyChangeListener;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.EditablePanel;

/**
 * The <b>SparsityAdjustmentPanel</b> is a sub-panel for other connection panels
 * which adjusts the sparsity of the connection. This panel includes a text
 * field for setting the sparsity as well as a slider and a text field
 * displaying how many efferents per source neuron there would be if efferents
 * are equalized. All of these are kept in sync.
 *
 * @author Zach Tosi
 *
 */
@SuppressWarnings("serial")
public class SparseConnectionPanel extends AbstractConnectionPanel implements
    EditablePanel {

    /** A slider for setting the sparsity of the connections. */
    private JSlider connectionDensitySlider = new JSlider(JSlider.HORIZONTAL,
        0, 100, 10);

    /** A text field for setting the sparsity of the connections. */
    private JFormattedTextField densityTf = new JFormattedTextField(
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
    private JCheckBox equalizeEfferentsChkBx = new JCheckBox();

    /**
     * A check box for determining whether or not self connections are allowed.
     */
    private JCheckBox allowSelfConnectChkBx = new JCheckBox();

    /** Panel for self connection check box. */
    private JPanel allowSelfConnectPanel = new JPanel();

    /**
     * The sparse connection object to which changes to this panel will be
     * committed.
     */
    private Sparse connection;

    /**
     * A property change listener for the density text field which can be
     * switched off to prevent infinite loops.
     */
    private SwitchablePropertyChangeListener densityTfListener;

    /**
     * A property change listener for the synapses per source neuron text field
     * which can be switched off to prevent infinite loops.
     */
    private SwitchablePropertyChangeListener synsPerSourceListener;

    /**
     * A change listener for the slider which can be switched off to prevent
     * infinite loops.
     */
    private SwitchableChangeListener sliderListener;

    /**
     * The number of target neurons being connected to. Used for determining the
     * number of efferents per source neuron when efferents are equalized.
     */
    private int numTargs;

    /** Whether to allow self connections. */
    private boolean allowSelfConnect = false;

    /** Whether to allow recurrent connections. */
    private boolean recurrentConnection = false;

    /**
     * If true then this sparse connection object is being edited, as opposed to
     * created.
     */
    private final boolean editing;

    /**
     * Create the sparsity based connection panel.
     *
     * @param connection the connection object to adjust
     * @param networkPanel the parent panel
     * @return the constructed panel
     */
    public static SparseConnectionPanel createSparsityAdjustmentPanel(
        Sparse connection, NetworkPanel networkPanel) {
        SparseConnectionPanel sap =
            new SparseConnectionPanel(connection, networkPanel);
        sap.fillFieldValues();
        sap.initializeSparseSlider();
        sap.addChangeListeners();
        sap.addActionListeners();
        sap.initializeLayout();
        return sap;
    }
    
    /**
     * Constructs a gui panel for adjusting the sparsity of a sparse connect
     * neurons object, and initializes all appropriate listeners.
     *
     * @param connection
     *            the connection object this panel will act on
     * @param numTargs
     *            the number of target neurons being connected to
     */
    private SparseConnectionPanel(Sparse connection,
            NetworkPanel networkPanel) {
        super();
        editing = connection.getSynapseGroup() != null;
        this.connection = connection;
        if (editing) {
            numTargs = connection.getSynapseGroup().getTargetNeuronGroup()
                    .size();
            setRecurrent(connection.getSynapseGroup().isRecurrent());
            allowSelfConnect = connection.isSelfConnectionAllowed();
        } else {
            // Assumes only one source and one target group are selected if any
            // are
            try {
                if (networkPanel.getSelectedModelNeuronGroups().size() > 0) {

                    NeuronGroup source = networkPanel.getSourceModelGroups()
                            .get(0);
                    NeuronGroup target =
                            networkPanel.getSelectedModelNeuronGroups().get(0);
                    numTargs = target.size();
                    setRecurrent(source.equals(target));

                } else {
                    // Called when Quick Properties (see QuickConnectDialog) is used
                    Set<Neuron> sources =
                            new HashSet<Neuron>(networkPanel
                                    .getSourceModelNeurons());
                    List<Neuron> targets = networkPanel
                            .getSelectedModelNeurons();
                    numTargs = targets.size();
                    int sourcesSize = sources.size();
                    sources.retainAll(targets);
                    int newSize = sources.size();
                    // Counts as recurrent iff all the source neurons are the
                    // same
                    // as all the target neurons.
                    setRecurrent(sourcesSize == newSize);
                }
                synsPerSource.setVisible(numTargs != 0);
            } catch (IndexOutOfBoundsException e) {
                setRecurrent(true);
                numTargs = (int) 1E4;
                synsPerSource.setVisible(false);
            }
        }
        
        
    }

    /**
     * Initializes the panel's layout
     */
    private void initializeLayout() {
        densityTfListener.disable();
        synsPerSourceListener.disable();
        sliderListener.disable();
        // Create label/text field combo panels for:
        // Connection Density
        // Equalize Efferents
        // Allow Self-Connection
        JPanel sparseTextPanel = new JPanel(new FlowLayout());
        sparseTextPanel.add(new JLabel("Density: "));
        Dimension sSize = densityTf.getPreferredSize();
        sSize.width = 80;
        densityTf.setPreferredSize(sSize);
        sparseTextPanel.add(densityTf);

        JPanel equalizerPanel = new JPanel(new FlowLayout());
        equalizerPanel.add(new JLabel("Equalize Efferents:"));
        equalizerPanel.add(equalizeEfferentsChkBx);
        sSize = synsPerSource.getPreferredSize();
        sSize.width = 80;
        synsPerSource.setPreferredSize(sSize);
        equalizerPanel.add(synsPerSource);

        allowSelfConnectPanel = new JPanel(new FlowLayout());
        allowSelfConnectPanel.add(new JLabel("Self Connections: "));
        allowSelfConnectPanel.add(allowSelfConnectChkBx);

        // Begin laying out components
        this.setLayout((new GridBagLayout()));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Connection density label/textfield
        // (0, 0) w 2
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        this.add(sparseTextPanel, gbc);

        // Connection density slider
        // (0, 1) w 4
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        this.add(connectionDensitySlider, gbc);

        // Equalize Efferents label/TextField
        // (0, 2) w 2
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = 2;
        this.add(equalizerPanel, gbc);

        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        if (recurrentConnection) {
            this.add(allowSelfConnectPanel, gbc);
        } else {
            this.add(Box.createRigidArea(allowSelfConnectPanel
                .getPreferredSize()), gbc);
        }

        densityTfListener.enable();
        synsPerSourceListener.enable();
        sliderListener.enable();
    }

    /**
     * Initializes the sparse slider.
     */
    private void initializeSparseSlider() {

        connectionDensitySlider.setMajorTickSpacing(10);
        connectionDensitySlider.setMinorTickSpacing(2);
        connectionDensitySlider.setPaintTicks(true);

        Hashtable<Integer, JLabel> labelTable2 =
            new Hashtable<Integer, JLabel>();
        labelTable2.put(new Integer(0), new JLabel("0%"));
        labelTable2.put(new Integer(100), new JLabel("100%"));
        connectionDensitySlider.setLabelTable(labelTable2);
        connectionDensitySlider.setPaintLabels(true);

    }

    /**
     * Adds change listeners specific to sparse panel: Sparsity slider, sparsity
     * text field, and syns/source field.
     *
     * Since every field listens to every field, loops are prevented through the
     * use of so-called "switchable listeners", which have fields allowing them
     * to be turned off, and focus listeners. Any field not being interacted
     * with by the user has its listener disabled, and any field being
     * interacted with (which is "in focus") is enabled. This allows all fields
     * to be changed accordingly in response to user input without changes to
     * the out of focus fields themselves firing off events.
     *
     */
    private void addChangeListeners() {

        // *********************************************************************
        // Slider
        sliderListener = new SwitchableChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                if (source == connectionDensitySlider && isEnabled()) {
                    densityTfListener.disable();
                    synsPerSourceListener.disable();
                    int nt = allowSelfConnect || !recurrentConnection
                        ? numTargs : numTargs - 1;
                    double val =
                        (double) (connectionDensitySlider.getValue()) / 100;
                    densityTf.setValue(new Double(val));
                    int sps = (int) (val * nt);
                    synsPerSource.setValue(new Integer(sps));
                }
            }
        };
        connectionDensitySlider.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Enables the listener associated with the density slider
                // field while it is in focus.
                sliderListener.enable();
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Disables the listener associated with the density slider
                // field when it is not in focus.
                sliderListener.disable();
            }
        });
        connectionDensitySlider.addChangeListener(sliderListener);

        // *********************************************************************
        // Equalized efferent number (Synapses per source)
        synsPerSourceListener = new SwitchablePropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (isEnabled() && evt.getSource() == synsPerSource) {
                    densityTfListener.disable();
                    sliderListener.disable();
                    int nt =
                        allowSelfConnect
                            || !recurrentConnection ? numTargs
                            : numTargs - 1;
                    Integer sps = Utils.parseInteger(synsPerSource);
                    if (sps != null) {
                        densityTf.setValue(new Double((double) sps / nt));
                        int sVal =
                            (int) (((Number) densityTf
                                .getValue()).doubleValue() * 100);
                        connectionDensitySlider
                            .setValue(new Integer(sVal));
                    }
                }
            }
        };
        synsPerSource.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
                // Enables the listener associated with the synapse per source
                // field while it is in focus.
                synsPerSourceListener.enable();

            }

            @Override
            public void focusLost(FocusEvent arg0) {
                // Disables the listener associated with the synapse per source
                // field while it is not in focus.
                synsPerSourceListener.disable();
            }
        });

        synsPerSource.addPropertyChangeListener(synsPerSourceListener);

        // *********************************************************************
        // Overall density
        densityTfListener = new SwitchablePropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == densityTf && isEnabled()) {
                    sliderListener.disable();
                    synsPerSourceListener.disable();
                    int sps;
                    int nt =
                        allowSelfConnect ? numTargs
                            : numTargs - 1;
                    if (densityTf.getValue() != null) {
                        sps =
                            (int) (((Number) densityTf
                                .getValue()).doubleValue() * nt);
                        synsPerSource.setValue(new Integer(sps));
                        int sVal =
                            (int) (((Number) densityTf
                                .getValue()).doubleValue() * 100);
                        connectionDensitySlider
                            .setValue(new Integer(sVal));
                    }
                }
            }
        };
        densityTf.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Enables the listener associated with the density
                // field while it is in focus.
                densityTfListener.enable();
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Disables the listener associated with the density
                // field while it is not in focus.
                densityTfListener.disable();
            }
        });
        densityTf.addPropertyChangeListener(densityTfListener);

    }

    /**
     * Adds action listeners specific to sparse panel: sparse specific check
     * box.
     */
    private void addActionListeners() {

        equalizeEfferentsChkBx.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (arg0.getSource() == equalizeEfferentsChkBx) {
                    if (equalizeEfferentsChkBx.isSelected()) {
                        synsPerSource.setEnabled(true);
                    } else {
                        synsPerSource.setEnabled(false);
                    }
                }
            }
        });

        allowSelfConnectChkBx.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                allowSelfConnect = allowSelfConnectChkBx.isSelected();

                densityTfListener.disable();
                sliderListener.disable();
                synsPerSourceListener.disable();

                int nt =
                    allowSelfConnect
                        || !recurrentConnection ? numTargs
                        : numTargs - 1;
                double sparsity = Double.parseDouble(densityTf.getText());
                synsPerSource.setValue(new Integer((int) (sparsity * nt)));

            }
        });

    }

    /**
     * 
     */
    @Override
    public void fillFieldValues() {
        double connectivity = connection.getConnectionDensity();
        equalizeEfferentsChkBx
            .setSelected(connection.isEqualizeEfferents());
        allowSelfConnectChkBx.setSelected(connection.isSelfConnectionAllowed());
        synsPerSource.setValue((int) (connectivity * numTargs));
        connectionDensitySlider.setValue((int) (connectivity * 100));
        densityTf.setValue(connectivity);
        synsPerSource.setEnabled(!editing);
        equalizeEfferentsChkBx.setEnabled(!editing);
        allowSelfConnectChkBx.setEnabled(!editing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean commitChanges() {
        if (equalizeEfferentsChkBx.isEnabled()) {
            // Should always be disabled if the connection is AllToAll
            ((Sparse) connection)
                .setEqualizeEfferents(equalizeEfferentsChkBx
                    .isSelected());
        }
        double connectivity = Utils.doubleParsable(densityTf);
        if (!Double.isNaN(connectivity)) {
            if (!editing)
                // Just set the density
                connection.setConnectionDensity(connectivity);
            else {
                // Tell the sparse connection to change its density
                if (connectivity > connection.getConnectionDensity()) {
                    connection.addToSparsity(connectivity);
                } else if (connectivity < connection.getConnectionDensity()) {
                    connection.removeToSparsity(connectivity);
                }
            }
        }
        if (allowSelfConnectChkBx.isEnabled()) {
            connection.setSelfConnectionAllowed(allowSelfConnectChkBx
                .isSelected());
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Synapse> applyConnection(List<Neuron> source,
        List<Neuron> target) {
        double density = Utils.doubleParsable(densityTf);
        if (!Double.isNaN(density)) {
            if (density == 1.0) {
                return AllToAll.connectAllToAll(source, target,
                    Utils.intersects(source, target), allowSelfConnect, true);
            } else {
                return Sparse.connectSparse(source, target, density,
                    allowSelfConnect, equalizeEfferentsChkBx.isSelected(),
                    true);
            }
        }
        return null;
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
        int sps = (int) (((Number) densityTf.getValue()).doubleValue() * nt);
        synsPerSource.setValue(new Integer(sps));
    }

    public boolean isRecurrentConnection() {
        return recurrentConnection;
    }

    public void setRecurrent(boolean recurrent) {
        recurrentConnection = recurrent;
        if (densityTfListener != null && synsPerSourceListener != null
            && sliderListener != null) {
            refresh();
        }
    }

    /**
     * @return the number of target neurons.
     */
    public int getNumTargs() {
        return numTargs;
    }

    /**
     *
     * @param numTargs
     */
    public void setNumTargs(int numTargs) {
        this.numTargs = numTargs;
        int nt = allowSelfConnect ? numTargs : numTargs - 1;
        int sps = (int) (((Number) densityTf.getValue()).doubleValue() * nt);
        synsPerSource.setValue(new Integer(sps));
    }

    public void setEnabled(boolean enabled) {
    	super.setEnabled(enabled);
    	connectionDensitySlider.setEnabled(enabled);
    	densityTf.setEnabled(enabled);
    	synsPerSource.setEnabled(enabled);
    	equalizeEfferentsChkBx.setEnabled(enabled);
    	allowSelfConnectChkBx.setEnabled(enabled);
    }
    
    /**
     *
     * @param density
     */
    public void setDensity(double density) {
        densityTf.setText(Double.toString(density));
        sliderListener.enable();
        connectionDensitySlider.setValue((int) (density * 100));
        sliderListener.disable();
    }

    /**
     * Refreshes the panel's view.
     */
    public void refresh() {
        densityTfListener.disable();
        double density = Utils.doubleParsable(densityTf);
        densityTf.setText(Double.toString(density));
        densityTfListener.enable();
        this.removeAll();
        initializeLayout();
        this.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectNeurons getConnection() {
        return connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getPanel() {
        return this;
    }

}
