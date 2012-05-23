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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.simbrain.network.connections.Radial;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;

/**
 * <b>SparsePanel</b> creates a dialog for setting preferences of Sparse neuron
 * connections.
 */
public class RadialPanel extends AbstractConnectionPanel {

    /** Excitatory Probability. */
    private JTextField tfExciteProbability = new JTextField();

    /** Excitatory Radius. */
    private JTextField tfExciteRadius = new JTextField();

    /** Inhibitory Probability. */
    private JTextField tfInhibitProbability = new JTextField();

    /** Inhibitory Radius. */
    private JTextField tfInhibitRadius = new JTextField();

    /** Allow self connections check box. */
    private JCheckBox allowSelfConnect = new JCheckBox();

    /** Set the inhibitory synapse type. */
    private JButton setInhibitorySynapseType = new JButton();

    /** Set the excitatory synapse type. */
    private JButton setExcitatorySynapseType = new JButton();


    /**
     * This method is the default constructor.
     *
     * @param connection type
     */
    public RadialPanel(final Radial connection, final NetworkPanel panel) {
        super(connection, panel);

        setExcitatorySynapseType.setText(connection.getBaseExcitatorySynapse()
                .getType());
        this.addItem("Excitatory Radius:", tfExciteRadius);
        this.addItem("Excitatory Probability:", tfExciteProbability);
        this.addItem("Excitatory Synapse Type:", setExcitatorySynapseType);

        setExcitatorySynapseType.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ArrayList<Synapse> excitatoryList = new ArrayList<Synapse>();
                excitatoryList.add(connection.getBaseExcitatorySynapse());
                SynapseDialog dialog = new SynapseDialog(excitatoryList);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                Synapse excitatorySynapse = dialog.getSynapseList().get(0);
                connection.setBaseExcitatorySynapse(excitatorySynapse);
                setExcitatorySynapseType.setText(excitatorySynapse.getType());
            }

        });

        setInhibitorySynapseType.setText(connection.getBaseInhibitorySynapse()
                .getType());
        this.addItem("Inhibitory Radius:", tfInhibitRadius);
        this.addItem("Inhibitory Probability:", tfInhibitProbability);
        this.addItem("Inhibitory Synapse Type:", setInhibitorySynapseType);

        setInhibitorySynapseType.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ArrayList<Synapse> inhibitoryList = new ArrayList<Synapse>();
                inhibitoryList.add(connection.getBaseInhibitorySynapse());
                SynapseDialog dialog = new SynapseDialog(inhibitoryList);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                Synapse inhibitorySynapse = dialog.getSynapseList().get(0);
                connection.setBaseInhibitorySynapse(inhibitorySynapse);
                setInhibitorySynapseType.setText(inhibitorySynapse.getType());
            }

        });

        this.addItem("Allow Self Connections:", allowSelfConnect);
    }

    /**
     * {@inheritDoc}
     */
    public void commitChanges() {
        ((Radial) connection).setExcitatoryProbability(Double
                .parseDouble(tfExciteProbability.getText()));
        ((Radial) connection).setExcitatoryRadius(Double
                .parseDouble(tfExciteRadius.getText()));
        ((Radial) connection).setInhibitoryProbability(Double
                .parseDouble(tfInhibitProbability.getText()));
        ((Radial) connection).setInhibitoryRadius(Double
                .parseDouble(tfInhibitRadius.getText()));
        ((Radial) connection).setAllowSelfConnections(allowSelfConnect
                .isSelected());
    }

    /**
     * {@inheritDoc}
     */
    public void fillFieldValues() {
        tfExciteProbability.setText(Double.toString(((Radial) connection)
                .getExcitatoryProbability()));
        tfInhibitProbability.setText(Double.toString(((Radial) connection)
                .getInhibitoryProbability()));
        tfExciteRadius.setText(Double.toString(((Radial) connection)
                .getExcitatoryRadius()));
        tfInhibitProbability.setText(Double.toString(((Radial) connection)
                .getInhibitoryProbability()));
        tfInhibitRadius.setText(Double.toString(((Radial) connection)
                .getInhibitoryRadius()));
        allowSelfConnect.setSelected(((Radial) connection)
                .isAllowSelfConnections());
    }

}
