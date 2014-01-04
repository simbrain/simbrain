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
import java.beans.PropertyChangeEvent;
import java.util.Hashtable;

import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.Sparse;
import org.simbrain.util.SwitchableChangeListener;
import org.simbrain.util.SwitchablePropertyChangeListener;

/**
 * Display preferences for regarding the ratio of excitatory to inhibitory
 * connections.
 *
 * @author ztosi
 * @author jyoshimi
 *
 */
public class ExcitatoryInhibitoryRatioPanel extends JPanel {

    /** The connection object. */
    private ConnectNeurons connection;

    /** Max ratio of excitatory/inhibitory connections. */
    private static final int RATIO_MAX = 100;

    /** Min ratio of excitatory/inhibitory connections. */
    private static final int RATIO_MIN = 0;

    /** Default starting ratio of excitatory/inhibitory. */
    private static final int RATIO_INIT = 50;

    /** A slider for setting the ratio of inhibitory to excitatory connections. */
    private JSlider ratioSlider = new JSlider(JSlider.HORIZONTAL, RATIO_MIN,
            RATIO_MAX, RATIO_INIT);

    /**
     * A text field for setting the ratio of excitatory to inhibitory
     * connections.
     */
    private JFormattedTextField eRatio = new JFormattedTextField(RATIO_INIT);

    /**
     * A text field for setting the ratio of inhibitory to excitatory
     * connections.
     */
    private JFormattedTextField iRatio = new JFormattedTextField(1 - RATIO_INIT);

    /**
     * A switchable listener
     *
     * @see org.simbrain.util.SwitchablePropertyChangeListener.java listenting
     *      to changes to the excitatory ratio text field.
     */
    private SwitchablePropertyChangeListener exTfListener;

    /**
     * A switchable listener
     *
     * @see org.simbrain.util.SwitchablePropertyChangeListener.java listenting
     *      to changes to the inhibitory ratio text field.
     */
    private SwitchablePropertyChangeListener inTfListener;

    /**
     * A switchable listener
     *
     * @see org.simbrain.util.SwitchableChangeListener.java listenting to
     *      changes to the excitatory/inhibitory ratio slider.
     */
    private SwitchableChangeListener sliderListener;

    /**
     * Constructs the excitatory/inhibitory ratio sub-panel.
     *
     * @param connection the connection object which will be edited by
     *            committing changes to this panel.
     */
    public ExcitatoryInhibitoryRatioPanel(final ConnectNeurons connection) {
        this.connection = connection;
        initializeContent();
        initializeLayout();
    }

    /**
     * Initializes the ratio field, sliders, change listeners, action listeners,
     * and random buttons/checkboxes.
     */
    private void initializeContent() {
        eRatio.setValue(((Number) (ConnectNeurons.getDefaultRatio() * 100))
                .intValue());
        initializeRatioSlider();
        initializeChangeListeners();
    }

    /**
     * Initializes the panel layout.
     */
    private void initializeLayout() {

        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 5;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 5, 5);
        this.add(ratioSlider, gbc);

        gbc.insets = new Insets(5, 5, 0, 5);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        JPanel exTfPanel = new JPanel(new FlowLayout());
        Dimension eRatioSize = eRatio.getPreferredSize();
        eRatioSize.width = 40;
        eRatio.setPreferredSize(eRatioSize);
        exTfPanel.add(new JLabel("% Excitatory"));
        exTfPanel.add(eRatio);
        this.add(exTfPanel, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 1;
        JPanel blank = new JPanel();
        blank.setPreferredSize(new Dimension(60, 10));
        blank.setMinimumSize(new Dimension(60, 10));
        this.add(blank, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 2;
        JPanel inTfPanel = new JPanel(new FlowLayout());
        Dimension iRatioSize = iRatio.getPreferredSize();
        iRatioSize.width = 40;
        iRatio.setPreferredSize(iRatioSize);
        inTfPanel.add(new JLabel("% Inhibitory"));
        inTfPanel.add(iRatio);

        this.add(inTfPanel, gbc);

    }

    /**
     * Initializes the values of the GUI ratio slider.
     */
    private void initializeRatioSlider() {
        ratioSlider.setMajorTickSpacing(10);
        ratioSlider.setMinorTickSpacing(2);
        ratioSlider.setPaintTicks(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(new Integer(0), new JLabel("0/100"));
        labelTable.put(new Integer(25), new JLabel("25/75"));
        labelTable.put(new Integer(50), new JLabel("50/50"));
        labelTable.put(new Integer(75), new JLabel("75/25"));
        labelTable.put(new Integer(100), new JLabel("100/0"));
        ratioSlider.setLabelTable(labelTable);
        ratioSlider.setPaintLabels(true);
    }

    /**
     * Initializes the change listeners relating to the ratio text field and
     * ratio slider (reciprocal listeners).
     */
    private void initializeChangeListeners() {

        sliderListener = new SwitchableChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                if (source == ratioSlider && isEnabled()) {
                    exTfListener.disable();
                    inTfListener.disable();
                    eRatio.setValue(new Integer(ratioSlider.getValue()));
                    iRatio.setValue(RATIO_MAX
                            - new Integer(ratioSlider.getValue()));
                    exTfListener.enable();
                    inTfListener.enable();
                }
            }

        };

        exTfListener = new SwitchablePropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == eRatio && isEnabled()) {
                    sliderListener.disable();
                    inTfListener.disable();
                    ratioSlider.setValue(((Number) eRatio.getValue())
                            .intValue());
                    iRatio.setValue(RATIO_MAX
                            - ((Number) eRatio.getValue()).intValue());
                    sliderListener.enable();
                    inTfListener.enable();
                }

            }
        };

        inTfListener = new SwitchablePropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == iRatio && isEnabled()) {
                    sliderListener.disable();
                    exTfListener.disable();
                    ratioSlider.setValue(RATIO_MAX
                            - ((Number) iRatio.getValue()).intValue());
                    eRatio.setValue(RATIO_MAX
                            - ((Number) iRatio.getValue()).intValue());
                    sliderListener.enable();
                    exTfListener.enable();
                }
            }

        };

        ratioSlider.addChangeListener(sliderListener);

        eRatio.addPropertyChangeListener(exTfListener);

        iRatio.addPropertyChangeListener(inTfListener);

    }

    /**
     * Commit changes on closing.
     */
    public void commitChanges() {
        connection.setPercentExcitatory(((Number) eRatio.getValue())
                .doubleValue());
    }

    /**
     * Fills the field values for this sub-panel based on the values of an
     * already existing connection object.
     *
     * @param connection the connection being used to determine the field values
     */
    public void fillFieldValues(ConnectNeurons connection) {
        this.connection = connection;
        double exRatio = connection.getExcitatoryRatio();

        eRatio.setValue((int) (exRatio * 100));
        iRatio.setValue((int) ((1 - exRatio) * 100));
        ratioSlider.setValue((int) (exRatio * 100));
    }

    /**
     * TEST MAIN: For prototyping design quickly.
     *
     * @param args
     */
    public static void main(String[] args) {
        ExcitatoryInhibitoryRatioPanel eir = new ExcitatoryInhibitoryRatioPanel(
                new Sparse());

        JFrame f = new JFrame();
        f.setContentPane(eir);
        f.pack();
        f.setVisible(true);
        f.setLocationRelativeTo(null);
    }
}