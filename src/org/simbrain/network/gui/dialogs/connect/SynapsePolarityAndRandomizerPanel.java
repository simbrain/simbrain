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

import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.SwitchableChangeListener;
import org.simbrain.util.SwitchablePropertyChangeListener;
import org.simbrain.util.Utils;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.util.Hashtable;

/**
 * Display preferences for regarding the ratio of excitatory to inhibitory
 * connections.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class SynapsePolarityAndRandomizerPanel extends JPanel {


    public enum RandBehavior {
        FORCE_ON, DEFAULT, FORCE_OFF;
    }

    private final RandBehavior randomizerState;

    /**
     * Max ratio of excitatory/inhibitory connections.
     */
    private static final int RATIO_MAX = 100;

    /**
     * Min ratio of excitatory/inhibitory connections.
     */
    private static final int RATIO_MIN = 0;

    /**
     * Default starting ratio of excitatory/inhibitory.
     */
    private static final int RATIO_INIT = 50;

    /**
     * A slider for setting the ratio of inhibitory to excitatory connections.
     */
    private JSlider ratioSlider = new JSlider(JSlider.HORIZONTAL, RATIO_MIN, RATIO_MAX, RATIO_INIT);

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

    private EditableRandomizerPanel excitatoryRandomizerPanel;

    private EditableRandomizerPanel inhibitoryRandomizerPanel;

    /**
     * A switchable listener.
     *
     * @see SwitchablePropertyChangeListener listenting to changes to the
     * excitatory ratio text field.
     */
    private SwitchablePropertyChangeListener exTfListener;

    /**
     * A switchable listener.
     *
     * @see SwitchablePropertyChangeListener listenting to changes to the
     * inhibitory ratio text field.
     */
    private SwitchablePropertyChangeListener inTfListener;

    /**
     * A switchable listener.
     *
     * @see SwitchableChangeListener listenting to changes to the
     * excitatory/inhibitory ratio slider.
     */
    private SwitchableChangeListener sliderListener;

    /**
     * Whether or not this panel is being used to create or edit synapses.
     */
    private boolean creationPanel;

    /**
     * The synapse group this panel is operating on, null if operating on loose
     * synapses.
     */
    private final SynapseGroup synapseGroup;

    /**
     * The apply button associated with the polarity slider for editing.
     */
    private JButton sliderApply = new JButton("Apply");

    private final Window parent;

    private final JLabel warning = new JLabel(ResourceManager.getImageIcon("WarningGray.png"));

    private static final double ERROR_TOLERANCE = 0.05;

    {
        warning.setToolTipText("Failed to apply ratio within error tolerance." + " Source neurons might be polarized.");
    }

    public static SynapsePolarityAndRandomizerPanel createPolarityRatioPanel(ConnectionStrategy connection, Window parentFrame) {
        SynapsePolarityAndRandomizerPanel prPanel = new SynapsePolarityAndRandomizerPanel(parentFrame, RandBehavior.DEFAULT);
        prPanel.fillDefaultValues();
        prPanel.excitatoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parentFrame,
                connection.getExRandomizer(), connection.isUseExcitatoryRandomization());
        prPanel.inhibitoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parentFrame,
                connection.getInRandomizer(), connection.isUseInhibitoryRandomization());
        prPanel.excitatoryRandomizerPanel.initListeners();
        prPanel.inhibitoryRandomizerPanel.initListeners();
        prPanel.initializeContent();
        prPanel.initializeLayout();
        prPanel.setExcitatoryRatio(connection.getExcitatoryRatio());
        return prPanel;

    }


    /**
     * @param parent
     * @param randState
     * @return
     */
    public static SynapsePolarityAndRandomizerPanel createPolarityRatioPanel(final Window parent, final RandBehavior randState) {
        SynapsePolarityAndRandomizerPanel prPanel = new SynapsePolarityAndRandomizerPanel(parent, randState);
        prPanel.excitatoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parent, Polarity.EXCITATORY);
        prPanel.inhibitoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parent, Polarity.INHIBITORY);
        prPanel.excitatoryRandomizerPanel.initListeners();
        prPanel.inhibitoryRandomizerPanel.initListeners();
        prPanel.initializeContent();
        prPanel.initializeLayout();
        prPanel.fillDefaultValues();
        return prPanel;
    }

    /**
     * @param parent
     * @param randState
     * @param synGrp
     * @return
     */
    public static SynapsePolarityAndRandomizerPanel createPolarityRatioPanel(final Window parent, final RandBehavior randState, final SynapseGroup synGrp) {
        SynapsePolarityAndRandomizerPanel prPanel = new SynapsePolarityAndRandomizerPanel(parent, synGrp, randState);
        if (synGrp.isEmpty()) {
            prPanel.fillDefaultValues();
            prPanel.creationPanel = true;
            prPanel.excitatoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parent, Polarity.EXCITATORY);
            prPanel.inhibitoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parent, Polarity.INHIBITORY);
        } else {
            prPanel.creationPanel = false;
            if (synGrp.getExcitatoryRandomizer() == null) {
                prPanel.excitatoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parent, Polarity.EXCITATORY);
                synGrp.setExcitatoryRandomizer(prPanel.getExcitatoryRandomizerPanel().getProbDist());
            } else {
                prPanel.excitatoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parent, synGrp.getExcitatoryRandomizer());
            }
            if (synGrp.getInhibitoryRandomizer() == null) {
                prPanel.inhibitoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parent, Polarity.INHIBITORY);
                synGrp.setInhibitoryRandomizer(prPanel.getInhibitoryRandomizerPanel().getProbDist());
            } else {
                prPanel.inhibitoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parent, synGrp.getInhibitoryRandomizer());
            }
            prPanel.fillFieldValues(synGrp);
        }
        prPanel.excitatoryRandomizerPanel.initListeners();
        prPanel.inhibitoryRandomizerPanel.initListeners();
        prPanel.initializeContent();
        prPanel.initializeLayout();
        return prPanel;
    }

    public static SynapsePolarityAndRandomizerPanel createPolarityRatioPanel(
            final Window parent,
            ProbabilityDistribution exRandomizer2,
            ProbabilityDistribution inRandomizer2,
            boolean useExcitatoryRandomization,
            boolean useInhibitoryRandomization
    ) {
        SynapsePolarityAndRandomizerPanel prPanel = new SynapsePolarityAndRandomizerPanel(parent, RandBehavior.DEFAULT);
        prPanel.fillDefaultValues();
        prPanel.excitatoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parent, exRandomizer2, useExcitatoryRandomization);
        prPanel.inhibitoryRandomizerPanel = prPanel.new EditableRandomizerPanel(parent, inRandomizer2, useInhibitoryRandomization);
        prPanel.excitatoryRandomizerPanel.initListeners();
        prPanel.inhibitoryRandomizerPanel.initListeners();
        prPanel.initializeContent();
        prPanel.initializeLayout();
        return prPanel;
    }

    /**
     * @param parent
     * @return
     */
    public static SynapsePolarityAndRandomizerPanel createPolarityRatioPanel(final Window parent) {
        return SynapsePolarityAndRandomizerPanel.createPolarityRatioPanel(parent, RandBehavior.DEFAULT);
    }

    /**
     * @param parent
     * @param synGrp
     * @return
     */
    public static SynapsePolarityAndRandomizerPanel createPolarityRatioPanel(final Window parent, final SynapseGroup synGrp) {
        return SynapsePolarityAndRandomizerPanel.createPolarityRatioPanel(parent, RandBehavior.DEFAULT, synGrp);
    }

    /**
     * Constructs the excitatory/inhibitory ratio sub-panel with default values
     * for the creation of some set of synapses grouped or otherwise.
     */
    private SynapsePolarityAndRandomizerPanel(final Window parent, final RandBehavior randState) {
        this.parent = parent;
        this.randomizerState = randState;
        creationPanel = true;
        synapseGroup = null;

    }

    /**
     * Constructs the excitatory/inhibitory ratio sub-panel, around an extant
     * synapse group.
     *
     * @param synGrp
     */
    private SynapsePolarityAndRandomizerPanel(final Window parent, final SynapseGroup synGrp, final RandBehavior randState) {
        this.parent = parent;
        this.synapseGroup = synGrp;
        this.randomizerState = randState;
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
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
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
        gbc.weightx = 0;
        JPanel blank = new JPanel();
        blank.setPreferredSize(new Dimension(60, 10));
        blank.setMinimumSize(new Dimension(60, 10));
        sliderPanel.add(blank, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        JPanel exTfPanel = new JPanel(new FlowLayout());
        Dimension eRatioSize = eRatio.getPreferredSize();
        eRatioSize.width = 40;
        eRatio.setPreferredSize(eRatioSize);
        exTfPanel.add(new JLabel("% Excitatory"));
        exTfPanel.add(eRatio);
        sliderPanel.add(exTfPanel, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        sliderPanel.add(warning, gbc);
        warning.setVisible(false);

        if (!creationPanel) {
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.gridx = 4;
            gbc.gridy = 2;
            gbc.insets = new Insets(10, 5, 5, 10);
            sliderPanel.add(sliderApply, gbc);
        }
        Border sliderBorder = BorderFactory.createTitledBorder("Inhibitory/Excitatory Ratio");
        sliderPanel.setBorder(sliderBorder);
        this.setLayout(new BorderLayout());
        this.add(sliderPanel, BorderLayout.NORTH);

        if (RandBehavior.FORCE_OFF != randomizerState) {
            // buffer
            this.add(Box.createVerticalStrut(10), BorderLayout.CENTER);

            JPanel dualRandomizerPanel = new JPanel();
            dualRandomizerPanel.setLayout(new BoxLayout(dualRandomizerPanel, BoxLayout.X_AXIS));
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
            dualRandomizerPanel.add(Box.createHorizontalStrut(5));
            dualRandomizerPanel.add(exBox);
            this.add(dualRandomizerPanel, BorderLayout.SOUTH);
        }

    }

    /**
     * Initializes the values of the GUI ratio slider.
     */
    private void initializeRatioSlider() {
        ratioSlider.setMajorTickSpacing(10);
        ratioSlider.setMinorTickSpacing(2);
        ratioSlider.setPaintTicks(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(0, new JLabel("0/100"));
        labelTable.put(25, new JLabel("25/75"));
        labelTable.put(50,  new JLabel("50/50"));
        labelTable.put(75, new JLabel("75/25"));
        labelTable.put(100, new JLabel("100/0"));
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
                    eRatio.setValue(ratioSlider.getValue());
                    iRatio.setValue(RATIO_MAX - ratioSlider.getValue());
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
                    ratioSlider.setValue(((Number) eRatio.getValue()).intValue());
                    iRatio.setValue(RATIO_MAX - ((Number) eRatio.getValue()).intValue());
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
                    ratioSlider.setValue(RATIO_MAX - ((Number) iRatio.getValue()).intValue());
                    eRatio.setValue(RATIO_MAX - ((Number) iRatio.getValue()).intValue());
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
                    synapseGroup.setExcitatoryRatio(percentExcitatory);
                    if (Math.abs(percentExcitatory - synapseGroup.getExcitatoryRatioPrecise()) > ERROR_TOLERANCE) {
                        warning.setVisible(true);
                    } else {
                        warning.setVisible(false);
                    }
                    // In case some or all source neurons have polarity, reset
                    // the slider and other field values to represent the result
                    // of synapseGroup's attempt to match the desired excitatory
                    // ratio.
                    ratioSlider.setValue((int) (100 * synapseGroup.getExcitatoryRatioPrecise()));
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
     * @param synGrp the connection being used to determine the field values
     */
    public void fillFieldValues(SynapseGroup synGrp) {
        double exRatio = synGrp.getExcitatoryRatioParameter();
        setExcitatoryRatio(exRatio);
        excitatoryRandomizerPanel.randomizer.setProbabilityDistribution(
                synGrp.getExcitatoryRandomizer());
        excitatoryRandomizerPanel.randomizerPanel.fillFieldValues();
        inhibitoryRandomizerPanel.randomizer.setProbabilityDistribution(
                synGrp.getInhibitoryRandomizer());
        inhibitoryRandomizerPanel.randomizerPanel.fillFieldValues();
    }

    /**
     * Set the ration of excitatory to inhibitory neurons, a value between 0 (no
     * excitatory) and 1 (all excitatory).
     *
     * @param ratio the ratio to set
     */
    public void setExcitatoryRatio(double ratio) {
        eRatio.setValue((int) (ratio * 100));
        iRatio.setValue((int) ((1 - ratio) * 100));
        ratioSlider.setValue((int) (ratio * 100));

    }

    /**
     * Fills the fields based on certain default values.
     */
    public void fillDefaultValues() {
        double exRatio = SynapseGroup.DEFAULT_EXCITATORY_RATIO;
        setExcitatoryRatio(exRatio);
    }

    /**
     * Commits changes to a synapse group.
     *
     * @param synapseGroup
     */
    public void commitChanges(SynapseGroup synapseGroup) {
        double percentExcitatory = Utils.doubleParsable(eRatio) / 100;
        if (!Double.isNaN(percentExcitatory))
            synapseGroup.setExcitatoryRatio(percentExcitatory);
        excitatoryRandomizerPanel.commitChanges();
        inhibitoryRandomizerPanel.commitChanges();
        synapseGroup.setInhibitoryRandomizer(inhibitoryRandomizerPanel.getProbDist());
        synapseGroup.setExcitatoryRandomizer(excitatoryRandomizerPanel.getProbDist());
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
     * @param percentExcitatory
     */
    public void setPercentExcitatory(double percentExcitatory) {
        ratioSlider.setValue((int) (100 * percentExcitatory));
    }

    /**
     * @return
     */
    public EditableRandomizerPanel getExcitatoryRandomizerPanel() {
        return excitatoryRandomizerPanel;
    }

    /**
     * @return
     */
    public EditableRandomizerPanel getInhibitoryRandomizerPanel() {
        return inhibitoryRandomizerPanel;
    }

    public ProbabilityDistribution getExRandomizer() {
        return excitatoryRandomizerPanel.getProbDist();
    }

    public ProbabilityDistribution getInRandomizer() {
        return inhibitoryRandomizerPanel.getProbDist();
    }
    /**
     * @return whether or not excitatory randomization is enabled.
     */
    public boolean exRandomizerEnabled() {
        return excitatoryRandomizerPanel.enableStatusTriangle.isDown();
    }

    /**
     * @return whether or not inhibitory randomization is enabled.
     */
    public boolean inRandomizerEnabled() {
        return inhibitoryRandomizerPanel.enableStatusTriangle.isDown();
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

    /**
     * @author Zoë Tosi
     */
    public class EditableRandomizerPanel extends JPanel {

        /**
         * The Polarity Associated with the panel (inhibitory -&#62; only
         * negative values allowed; exciatory -&#62; only positive values
         * allowed).
         */
        private final Polarity polarity;

        /**
         * The PolaraizedRandomizer this panel will either use to fill field
         * values and edit, or which this panel will create and then edit.
         */
        private final ProbabilityDistribution.Randomizer randomizer = new ProbabilityDistribution.Randomizer();

        /**
         * The randomizer panel used as a basis for this panel.
         */
        private AnnotatedPropertyEditor randomizerPanel;

        /**
         * A DropDownTriangle used to show or hide {@link #randomizerPanel}. The
         * state of the triangle is used on creation to decide whether or not
         * any randomization at all occurs. Only visible on creation.
         */
        public final DropDownTriangle enableStatusTriangle;

        /**
         * The apply button used to apply changes after editing.
         */
        private final JButton applyButton = new JButton("Apply");

        /**
         * Construct the synapse randomizer panel.
         *
         * @param parent     for resizing
         * @param dist  initial probabiliity distrubtion
         * @param enabled    start out enabled (w/ {@link #randomizerPanel}
         *                   visible)
         */
        public EditableRandomizerPanel(Window parent, ProbabilityDistribution dist, boolean enabled) {
            this.randomizer.setProbabilityDistribution(dist);
            polarity = randomizer.getProbabilityDistribution().getPolarity();
            enableStatusTriangle = new DropDownTriangle(UpDirection.LEFT, enabled, "Disabled", "Enabled", parent);
            enableStatusTriangle.setUpLabelColor(new Color(200, 0, 0));
            enableStatusTriangle.setDownLabelColor(new Color(0, 160, 0));
            init();
        }

        /**
         * Construct the synapse randomizer panel.
         *
         * @param parent     for resizing
         * @param dist  initial probabiliity distrubtion
         */
        public EditableRandomizerPanel(Window parent, ProbabilityDistribution dist) {
            this(parent, dist, true);
        }

        /**
         * Initialize with a polarity.
         *
         * @param parent parent window
         * @param polarity initial polarity
         */
        public EditableRandomizerPanel(Window parent, Polarity polarity) {

                this.polarity = polarity;

                randomizer.setProbabilityDistribution(
                        UniformDistribution.builder()
                                .polarity(polarity)
                                .build());

                enableStatusTriangle = new DropDownTriangle(UpDirection.LEFT, !creationPanel, "Disabled", "Enabled", parent);
                enableStatusTriangle.setUpLabelColor(new Color(200, 0, 0));
                enableStatusTriangle.setDownLabelColor(new Color(0, 160, 0));
                init();
        }

        /**
         * Initializes the layout of the panel
         */
        private void init() {
            randomizerPanel = new AnnotatedPropertyEditor(randomizer);
            setLayout(new GridBagLayout());
            Border colorBorder = BorderFactory.createLineBorder(Polarity.EXCITATORY.equals(polarity) ? Color.red : Color.blue);
            this.setBorder(BorderFactory.createTitledBorder(colorBorder, polarity.title()));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;
            gbc.weightx = 1.0;
            gbc.gridx = 0;
            gbc.gridy = 0;

            Box topPanel = Box.createHorizontalBox();
            if (RandBehavior.FORCE_ON != randomizerState && creationPanel) {
                topPanel.add(new JLabel("Weight Randomizer"));
                topPanel.add(Box.createHorizontalStrut(15));
                topPanel.add(Box.createHorizontalGlue());
                topPanel.add(enableStatusTriangle);
                topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                this.add(topPanel, gbc);
                gbc.gridy += 1;
            }

            randomizerPanel.setVisible(enableStatusTriangle.isDown() || RandBehavior.FORCE_ON == randomizerState);
            this.add(randomizerPanel, gbc);

            gbc.gridy += 1;
            gbc.fill = GridBagConstraints.VERTICAL;
            gbc.weighty = 1.0;
            this.add(new JPanel(), gbc);

            gbc.anchor = GridBagConstraints.SOUTHEAST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weighty = 0.0;
            gbc.gridy += 1;
            if (!creationPanel) {
                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                bottomPanel.add(applyButton);
                bottomPanel.setPreferredSize(new Dimension(randomizerPanel.getPreferredSize().width, bottomPanel.getPreferredSize().height));
                this.add(bottomPanel, gbc);
            }

        }

        public ProbabilityDistribution getProbDist() {
            return randomizer.getProbabilityDistribution();
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
                        randomizerPanel.commitChanges();
                        if (Polarity.EXCITATORY.equals(polarity)) {
                            if (synapseGroup != null) {
                                synapseGroup.setExcitatoryRandomizer(randomizer.getProbabilityDistribution());
                                if (!creationPanel) {
                                    synapseGroup.randomizeExcitatoryConnections();
                                }
                            }
                        } else {
                            if (synapseGroup != null) {
                                synapseGroup.setInhibitoryRandomizer(randomizer.getProbabilityDistribution());
                                if (!creationPanel) {
                                    synapseGroup.randomizeInhibitoryConnections();
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
         * "Enabled".
         */
        public void commitChanges() {
            if (enableStatusTriangle.isDown() || randomizerState == RandBehavior.FORCE_ON) {
                randomizerPanel.commitChanges();
                randomizer.getProbabilityDistribution().setPolarity(polarity);
            }
        }

    }

    public void commitChanges(ConnectionStrategy connection) {
        excitatoryRandomizerPanel.commitChanges();
        inhibitoryRandomizerPanel.commitChanges();
        connection.setExcitatoryRatio((double) ratioSlider.getValue()/100);
        connection.setUseExcitatoryRandomization(this.exRandomizerEnabled());
        connection.setUseInhibitoryRandomization(this.inRandomizerEnabled());
        connection.setExRandomizer(getExRandomizer());
        connection.setInRandomizer(getInRandomizer());
    }

}