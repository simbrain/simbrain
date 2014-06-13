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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;

import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.SwitchableChangeListener;
import org.simbrain.util.SwitchablePropertyChangeListener;
import org.simbrain.util.Utils;
import org.simbrain.util.randomizer.PolarizedRandomizer;
import org.simbrain.util.randomizer.gui.RandomizerPanel;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;

/**
 * Display preferences for regarding the ratio of excitatory to inhibitory
 * connections.
 * 
 * @author ztosi
 * @author jyoshimi
 * 
 */
@SuppressWarnings("serial")
public class ExcitatoryInhibitoryRatioPanel extends JPanel {

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
    private JFormattedTextField iRatio =
        new JFormattedTextField(1 - RATIO_INIT);

    private EditableRandomizerPanel excitatoryRandomizerPanel;

    private EditableRandomizerPanel inhibitoryRandomizerPanel;

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

    private PolarizedRandomizer exRandomizer;

    private PolarizedRandomizer inRandomizer;

    /** Whether or not this panel is being used to create or edit synapses. */
    private boolean creationPanel;

    /**
     * The synapse group this panel is operating on, null if operating on loose
     * synapses.
     */
    private final SynapseGroup synapseGroup;

    /** The apply button associated with the polarity slider for editing. */
    private JButton sliderApply = new JButton("Apply");

    private final Window parent;

    /**
     * Constructs the excitatory/inhibitory ratio sub-panel with default values
     * for the creation of some set of synapses grouped or otherwise.
     */
    public ExcitatoryInhibitoryRatioPanel(Window parent) {
        this.parent = parent;
        creationPanel = false;
        synapseGroup = null;
        excitatoryRandomizerPanel = new EditableRandomizerPanel(parent,
            Polarity.EXCITATORY);
        inhibitoryRandomizerPanel = new EditableRandomizerPanel(parent,
            Polarity.INHIBITORY);
        initializeContent();
        initializeLayout();
        fillDefaultValues();
    }

    /**
     * Constructs the excitatory/inhibitory ratio sub-panel, around an extant
     * synapse group.
     * 
     * @param synGrp
     */
    public ExcitatoryInhibitoryRatioPanel(Window parent, SynapseGroup synGrp) {
        this.parent = parent;
        this.synapseGroup = synGrp;
        if (synGrp.isEmpty()) {
            fillDefaultValues();
            creationPanel = true;
            excitatoryRandomizerPanel = new EditableRandomizerPanel(parent,
                Polarity.EXCITATORY);
            inhibitoryRandomizerPanel = new EditableRandomizerPanel(parent,
                Polarity.INHIBITORY);
        } else {
            fillFieldValues(synGrp);
            creationPanel = false;
            if (synGrp.getExcitatoryRandomizer() == null) {
                excitatoryRandomizerPanel = new EditableRandomizerPanel(parent,
                    Polarity.EXCITATORY);
            } else {
                excitatoryRandomizerPanel = new EditableRandomizerPanel(parent,
                    synGrp.getExcitatoryRandomizer());
            }
            if (synGrp.getInhibitoryRandomizer() == null) {
                inhibitoryRandomizerPanel = new EditableRandomizerPanel(parent,
                    Polarity.INHIBITORY);
            } else {
                inhibitoryRandomizerPanel = new EditableRandomizerPanel(parent,
                    synGrp.getInhibitoryRandomizer());
            }

        }
        excitatoryRandomizerPanel.initListeners();
        inhibitoryRandomizerPanel.initListeners();
        initializeContent();
        initializeLayout();
    }

    /**
     * Initializes the ratio field, sliders, change listeners, action listeners,
     * and random buttons/checkboxes.
     */
    private void initializeContent() {
        initializeRatioSlider();
        initializeChangeListeners();
    }

    /**
     * Initializes the panel layout.
     */
    private void initializeLayout() {

        JPanel sliderPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 5;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 5, 5);
        sliderPanel.add(ratioSlider, gbc);

        gbc.insets = new Insets(5, 5, 0, 5);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        JPanel inTfPanel = new JPanel(new FlowLayout());
        Dimension iRatioSize = iRatio.getPreferredSize();
        iRatioSize.width = 40;
        iRatio.setPreferredSize(iRatioSize);
        inTfPanel.add(new JLabel("% Inhibitory"));
        inTfPanel.add(iRatio);
        sliderPanel.add(inTfPanel, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 1;
        JPanel blank = new JPanel();
        blank.setPreferredSize(new Dimension(60, 10));
        blank.setMinimumSize(new Dimension(60, 10));
        sliderPanel.add(blank, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 2;
        JPanel exTfPanel = new JPanel(new FlowLayout());
        Dimension eRatioSize = eRatio.getPreferredSize();
        eRatioSize.width = 40;
        eRatio.setPreferredSize(eRatioSize);
        exTfPanel.add(new JLabel("% Excitatory"));
        exTfPanel.add(eRatio);
        sliderPanel.add(exTfPanel, gbc);

        if (!creationPanel) {
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.gridx = 4;
            gbc.gridy = 2;
            gbc.insets = new Insets(10, 5, 5, 10);
            sliderPanel.add(sliderApply, gbc);
        }
        Border sliderBorder = BorderFactory.createTitledBorder(
            "Inhibitory/Excitatory Ratio");
        sliderPanel.setBorder(sliderBorder);
        this.setLayout(new BorderLayout());
        this.add(sliderPanel, BorderLayout.NORTH);

        // buffer
        this.add(Box.createVerticalStrut(15), BorderLayout.CENTER);

        JPanel dualRandomizerPanel = new JPanel();
        dualRandomizerPanel.setLayout(new BoxLayout(dualRandomizerPanel,
            BoxLayout.X_AXIS));
        Box inBox = Box.createVerticalBox();
        Box exBox = Box.createVerticalBox();
        inBox.setAlignmentY(Component.TOP_ALIGNMENT);
        inBox.add(inhibitoryRandomizerPanel);
        inBox.add(Box.createVerticalGlue());
        inBox.add(new JPanel());
        exBox.setAlignmentY(Component.TOP_ALIGNMENT);
        exBox.add(excitatoryRandomizerPanel);
        exBox.add(Box.createVerticalGlue());
        exBox.add(new JPanel());
        dualRandomizerPanel.add(inBox);
        dualRandomizerPanel.add(Box.createHorizontalStrut(20));
        dualRandomizerPanel.add(exBox);
        this.add(dualRandomizerPanel, BorderLayout.SOUTH);

    }

    /**
     * Initializes the values of the GUI ratio slider.
     */
    private void initializeRatioSlider() {
        ratioSlider.setMajorTickSpacing(10);
        ratioSlider.setMinorTickSpacing(2);
        ratioSlider.setPaintTicks(true);

        Hashtable<Integer, JLabel> labelTable =
            new Hashtable<Integer, JLabel>();
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

        sliderApply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                double percentExcitatory = Utils.doubleParsable(eRatio) / 100;
                if (!Double.isNaN(percentExcitatory)) {
                    synapseGroup.setPercentExcitatory(percentExcitatory);
                }
            }
        });

        ratioSlider.addChangeListener(sliderListener);

        eRatio.addPropertyChangeListener(exTfListener);

        iRatio.addPropertyChangeListener(inTfListener);

    }

    /**
     * Fills the field values for this sub-panel based on the values of an
     * already existing connection object.
     * 
     * @param connection
     *            the connection being used to determine the field values
     */
    public void fillFieldValues(SynapseGroup synGrp) {
        double exRatio = synGrp.getPercentExcitatory();
        eRatio.setValue((int) (exRatio * 100));
        iRatio.setValue((int) ((1 - exRatio) * 100));
        ratioSlider.setValue((int) (exRatio * 100));
    }

    /**
     * Fills the fields based on certain default values
     */
    public void fillDefaultValues() {
        double exRatio = SynapseGroup.DEFAULT_PERCENT_EXCITATORY;
        eRatio.setValue((int) (exRatio * 100));
        iRatio.setValue((int) ((1 - exRatio) * 100));
        ratioSlider.setValue((int) (exRatio * 100));
    }

    /**
     * Commits changes to a synapse group.
     * 
     * @param synapseGroup
     */
    public void commitChanges(SynapseGroup synapseGroup) {
        double percentExcitatory = Utils.doubleParsable(eRatio) / 100;
        if (!Double.isNaN(percentExcitatory))
            synapseGroup.setPercentExcitatory(percentExcitatory);
        excitatoryRandomizerPanel.commitChanges();
        inhibitoryRandomizerPanel.commitChanges();
        synapseGroup.setExcitatoryRandomizer(exRandomizer);
        synapseGroup.setInhibitoryRandomizer(inRandomizer);
    }

    /**
     * For loose neurons just commits changes to the randomizer panels. The
     * percent excitatory can be retrieved later by calling
     * {@link #getPercentExcitatory()}.
     */
    public void commitChanges() {
        excitatoryRandomizerPanel.commitChanges();
        inhibitoryRandomizerPanel.commitChanges();
    }

    /**
     * Returns the desired percent excitatory. Used for loose connections
     * instead of committing changes.
     * 
     * @return the user selected percent of excitatory synapses
     */
    public double getPercentExcitatory() {
        double percentExcitatory = Utils.doubleParsable(eRatio);
        return percentExcitatory / 100;
    }

    /**
     * 
     * @return
     */
    public EditableRandomizerPanel getExcitatoryRandomizerPanel() {
        return excitatoryRandomizerPanel;
    }

    /**
     * 
     * @return
     */
    public EditableRandomizerPanel getInhibitoryRandomizerPanel() {
        return inhibitoryRandomizerPanel;
    }

    /**
     * Adds an additional action listener to the apply button associated with
     * the slider, so that external panels can perform some action.
     * 
     * @param al
     */
    public void addSliderApplyActionListener(ActionListener al) {
        sliderApply.addActionListener(al);
    }

    public PolarizedRandomizer getExRandomizer() {
        return exRandomizer;
    }

    public PolarizedRandomizer getInRandomizer() {
        return inRandomizer;
    }

    /**
     * 
     * 
     * @author Zach Tosi
     */
    public class EditableRandomizerPanel extends JPanel {

        private final Polarity polarity;

        private final PolarizedRandomizer randomizer;

        private final RandomizerPanel randomizerPanel =
            new RandomizerPanel(parent);

        private final DropDownTriangle enableStatusTriangle;

        private final JButton applyButton = new JButton("Apply");

        public EditableRandomizerPanel(Window parent,
            PolarizedRandomizer randomizer) {
            this.randomizer = randomizer;
            polarity = randomizer.getPolarity();
            randomizerPanel.fillFieldValues(randomizer);
            enableStatusTriangle =
                new DropDownTriangle(UpDirection.LEFT, true,
                    "Disabled", "Enabled", parent);
            enableStatusTriangle.setUpLabelColor(new Color(200, 0, 0));
            enableStatusTriangle.setDownLabelColor(new Color(0, 160, 0));
            init();
        }

        public EditableRandomizerPanel(Window parent, Polarity polarity) {
            this.polarity = polarity;
            randomizer = new PolarizedRandomizer(polarity);
            enableStatusTriangle =
                new DropDownTriangle(UpDirection.LEFT, !creationPanel,
                    "Disabled", "Enabled", parent);
            enableStatusTriangle.setUpLabelColor(new Color(200, 0, 0));
            enableStatusTriangle.setDownLabelColor(new Color(0, 160, 0));
            init();
        }

        /**
         * Initializes the layout of the panel
         */
        private void init() {
            randomizerPanel.fillFieldValues(randomizer);

            Box topPanel = Box.createHorizontalBox();
            topPanel.add(new JLabel("Weight Randomizer"));
            topPanel.add(Box.createHorizontalGlue());
            topPanel.add(enableStatusTriangle);
            topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            enableStatusTriangle.setVisible(creationPanel);

            Border colorBorder = BorderFactory.createLineBorder(Polarity
                .EXCITATORY.equals(polarity) ? Color.red : Color.blue);
            this.setLayout(new BorderLayout());
            Dimension rd = new Dimension(randomizerPanel.getPreferredSize()
                .width - 40, topPanel.getPreferredSize().height);
            topPanel.setPreferredSize(rd);
            this.add(topPanel, BorderLayout.NORTH);
            randomizerPanel.setVisible(enableStatusTriangle.isDown());
            this.add(randomizerPanel, BorderLayout.CENTER);
            if (!creationPanel) {
                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout
                    .RIGHT));
                bottomPanel.add(applyButton);
                bottomPanel.setPreferredSize(new Dimension(randomizerPanel
                    .getPreferredSize().width,
                    bottomPanel.getPreferredSize().height));
                this.add(bottomPanel, BorderLayout.SOUTH);
            }
            this.setBorder(BorderFactory.createTitledBorder(colorBorder,
                polarity.title()));
        }

        /**
         * Initializes the listener on the apply button, allowing the values in
         * the randomizer to be committed, and if editing, immediately causing
         * the synapses in question to undergo randomization.
         */
        public void initListeners() {

            applyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (enableStatusTriangle.isDown()) {
                        randomizerPanel.commitRandom(randomizer);
                        if (Polarity.EXCITATORY.equals(polarity)) {
                            exRandomizer = randomizer;
                            if (synapseGroup != null) {
                                synapseGroup.setExcitatoryRandomizer(
                                    randomizer);
                                if (!creationPanel) {
                                    synapseGroup
                                        .randomizeExcitatoryConnections();
                                }
                            }
                        } else {
                            inRandomizer = randomizer;
                            if (synapseGroup != null) {
                                synapseGroup.setInhibitoryRandomizer(
                                    randomizer);
                                if (!creationPanel) {
                                    synapseGroup
                                        .randomizeInhibitoryConnections();
                                }
                            }
                        }
                    }
                }
            });

            enableStatusTriangle.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent arg0) {
                    randomizerPanel.setVisible(enableStatusTriangle.isDown());
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

        }

        /**
         * Adds an additional listener to the apply button so that other panels
         * can perform other actions if the button is pressed.
         * 
         * @param al
         */
        public void addApplyActionListener(ActionListener al) {
            applyButton.addActionListener(al);
        }

        /**
         * Applies the settings in the randomizer panel to the randomizer given
         * to or created by this panel if the display triangle displays
         * "Enabled"
         */
        public void commitChanges() {
            if (enableStatusTriangle.isDown()) {
                randomizerPanel.commitRandom(randomizer);
                if (Polarity.EXCITATORY.equals(polarity)) {
                    exRandomizer = randomizer;
                } else {
                    inRandomizer = randomizer;
                }
            }
        }

    }

}