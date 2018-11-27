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

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.plot.histogram.HistogramModel;
import org.simbrain.plot.histogram.HistogramPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Panel for editing collections of synapses. TODO: In need of some
 * optimizations eventually... possibly after 3.0. Suggestions for this: Don't
 * allow polarity shifts and keep separate lists of excitatory/inhibitory
 * synapses, also allow to preview and keep values in numerical array and THEN
 * change synapse strengths.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public class SynapseAdjustmentPanel extends JPanel {

    /**
     * A reference to the parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Random source for randomizing inhibitory synapses.
     */
    private ProbabilityDistribution inhibitoryRandomizer =
            UniformDistribution.builder()
                    .polarity(Polarity.INHIBITORY)
                    .build();

    /**
     * Random source for randomizing excitatory synapses.
     */
    private ProbabilityDistribution excitatoryRandomizer =
            UniformDistribution.builder()
                    .polarity(Polarity.EXCITATORY)
                    .build();

    /**
     * Random source for randomizing all synapses.
     */
    private ProbabilityDistribution.Randomizer allRandomizer = new ProbabilityDistribution.Randomizer();

    /**
     * Random source for perturbing.
     */
    private ProbabilityDistribution perturber = UniformDistribution.create();

    /**
     * Current Mean label.
     */
    private JLabel meanLabel = new JLabel();

    /**
     * Current Median label.
     */
    private JLabel medianLabel = new JLabel();

    /**
     * Current Standard Deviation label.
     */
    private JLabel sdLabel = new JLabel();

    /**
     * The total number of synapses.
     */
    private JLabel numSynsLabel = new JLabel();

    /**
     * The number of excitatory synapses.
     */
    private JLabel numExSynsLabel = new JLabel();

    /**
     * The number of inhibitory synapses.
     */
    private JLabel numInSynsLabel = new JLabel();

    public enum SynapseView {
        ALL {
            @Override
            public String toString() {
                return "All";
            }

            @Override
            public boolean synapseIsAdjustable(Synapse s) {
                return true;
            }
        }, OVERLAY {
            @Override
            public String toString() {
                return "Overlay";
            }

            @Override
            public boolean synapseIsAdjustable(Synapse s) {
                return true;
            }
        }, EXCITATORY {
            @Override
            public String toString() {
                return "Excitatory";
            }

            @Override
            public boolean synapseIsAdjustable(Synapse s) {
                return s.getStrength() >= 0;
            }
        }, INHIBITORY {
            @Override
            public String toString() {
                return "Inhibitory";
            }

            @Override
            public boolean synapseIsAdjustable(Synapse s) {
                return s.getStrength() < 0;
            }
        };

        public abstract boolean synapseIsAdjustable(Synapse s);
    }

    /**
     * A combo box for selecting which kind of synapses should have their stats
     * displayed and/or what kind of display.
     */
    private JComboBox<SynapseView> synTypeSelector = new JComboBox<SynapseView>(SynapseView.values());

    /**
     * Calculates some basic statistics about the private final StatisticsBlock
     */
    StatisticsBlock statCalculator = new StatisticsBlock();

    /**
     * A histogram plotting the strength of synapses over given intervals (bins)
     * against their frequency.
     */
    private final HistogramPanel histogramPanel = new HistogramPanel(new HistogramModel(2));

    /**
     * The histogram axis names.
     */ {
        histogramPanel.setxAxisName("Synapse Strength");
        histogramPanel.setyAxisName("# of Synapses");
    }

    /**
     * A panel displaying basic statistics about the synapses, including: number
     * of synapses, number of inhibitory and excitatory synapses, and mean,
     * median, and standard deviation of the strengths of selected type of
     * synapses.
     */
    private JPanel statsPanel = new JPanel();

    /**
     * A random panel for randomizing the synapse strengths.
     */
    private AnnotatedPropertyEditor randomPanel = new AnnotatedPropertyEditor(allRandomizer);

    private ProbabilityDistribution.Randomizer perturberRandomizer = new ProbabilityDistribution.Randomizer();

    /**
     * A random panel for randomizing perturbations to synapse strengths.
     */
    private AnnotatedPropertyEditor perturberPanel = new AnnotatedPropertyEditor(perturberRandomizer);

    /**
     * Fills the fields of the random panels to default values.
     */ {
//        randomPanel.fillFieldValues(allRandomizer);

        perturberRandomizer.setProbabilityDistribution(perturber);
        inhibitoryRandomizer.setUpperBound(0);
        excitatoryRandomizer.setLowerBound(0);
    }

    /**
     * A button for committing random changes.
     */
    private JButton randomizeButton = new JButton("Apply");

    /**
     * A button for committing perturbation changes.
     */
    private JButton perturbButton = new JButton("Apply");

    /**
     * A collection of the selected synaptic weights, such that the first row
     * represents excitatory weights and the 2nd row represents inhibitory
     * weights. All inhibitory weights are stored as their absolute value. Note
     * that this array is only used internally, to display stats and the
     * histogram.
     */
    private double[][] weights = new double[2][];

    private final List<Synapse> synapses;

    public static SynapseAdjustmentPanel createSynapseAdjustmentPanel(final NetworkPanel networkPanel, final List<Synapse> synapses) {
        SynapseAdjustmentPanel sap = new SynapseAdjustmentPanel(networkPanel, synapses);

        // Update the stats in the stats panel.
        sap.updateStats();

        // Update the histogram
        sap.updateHistogram();

        // Add all action listeners for buttons unique to this panel.
        sap.addActionListeners();
        //
        // // Add network listener
        // networkPanel.getNetwork().addNetworkListener(networkListener);
        return sap;
    }

    /**
     * Create a synapse adjustment panel with a specified list of synapses.
     *
     * @param networkPanel parent network panel
     * @param synapses     synapses to represent in this panel
     */
    private SynapseAdjustmentPanel(final NetworkPanel networkPanel, final List<Synapse> synapses) {

        // Establish the parent panel.
        this.networkPanel = networkPanel;

        this.synapses = synapses;

        // Don't open if no synapses! */
        if (synapses.size() == 0) {
            return;
        }

        // Set Layout
        setLayout(new GridBagLayout());

        // Extract weight values in usable form by internal methods
        extractWeightValues(synapses);

        // Layout the panel.
        initializeLayout();
    }

    /**
     * Initializes the layout of the panel.
     */
    private void initializeLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel synTypePanel = new JPanel();
        synTypePanel.setBorder(BorderFactory.createTitledBorder("Synapse" + " Stats"));
        synTypePanel.setLayout(new GridLayout(3, 2));
        synTypePanel.add(numSynsLabel);
        synTypePanel.add(meanLabel);
        synTypePanel.add(numExSynsLabel);
        synTypePanel.add(medianLabel);
        synTypePanel.add(numInSynsLabel);
        synTypePanel.add(sdLabel);

        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = HistogramPanel.GRID_WIDTH - 1;
        gbc.gridheight = 1;

        this.add(synTypePanel, gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = HistogramPanel.GRID_WIDTH - 1;

        this.add(synTypeSelector, gbc);

        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = HistogramPanel.GRID_WIDTH;
        gbc.gridheight = HistogramPanel.GRID_HEIGHT;
        gbc.gridy = 1;
        gbc.gridx = 0;

        this.add(histogramPanel, gbc);

        gbc.gridy += HistogramPanel.GRID_HEIGHT;
        gbc.gridheight = 1;

        JTabbedPane bottomPanel = new JTabbedPane();
        JPanel randTab = new JPanel();
        JPanel perturbTab = new JPanel();
        JPanel prunerTab = new JPanel();
        JPanel scalerTab = new JPanel();

        randTab.setLayout(new GridBagLayout());
        perturbTab.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;

        randTab.add(randomPanel, c);
        perturbTab.add(perturberPanel, c);
        scalerTab.add(new ScalerPanel(networkPanel), c);
        prunerTab.add(new PrunerPanel(networkPanel), c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new Insets(5, 0, 5, 10);

        randTab.add(randomizeButton, c);
        perturbTab.add(perturbButton, c);

        bottomPanel.addTab("Randomizer", randTab);
        bottomPanel.addTab("Perturber", perturbTab);
        bottomPanel.addTab("Pruner", prunerTab);
        bottomPanel.addTab("Scaler", scalerTab);

        this.add(bottomPanel, gbc);

    }

    /**
     * Adds all the action listeners to the panel. Currently includes listeners
     * for: The perturb button, randomize button, and the synapse kind selector
     * combo box.
     */
    private void addActionListeners() {

        perturbButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                randomPanel.commitChanges();
                SynapseView view = (SynapseView) synTypeSelector.getSelectedItem();
                for (Synapse synapse : synapses) {
                    if (view.synapseIsAdjustable(synapse)) {
                        synapse.forceSetStrength(synapse.getStrength() + perturber.getRandom());
                    }
                }
                fullUpdate();
                networkPanel.getNetwork().fireSynapsesUpdated(synapses);
            }
        });

        randomizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                randomPanel.commitChanges();
                SynapseView view = (SynapseView) synTypeSelector.getSelectedItem();
                // Commit appropriate randomizer to panel
                switch (view) {
//                    case ALL:
//                        // TODO: Deal with changes in polarity...
//                        randomPanel.commitRandom(allRandomizer);
//                        break;
//                    case OVERLAY:
//                        randomPanel.commitRandom(excitatoryRandomizer);
//                        randomPanel.commitRandom(inhibitoryRandomizer);
//                        break;
//                    case INHIBITORY:
//                        randomPanel.commitRandom(inhibitoryRandomizer);
//                        break;
//                    case EXCITATORY:
//                        randomPanel.commitRandom(excitatoryRandomizer);
//                        break;
                }
                // Randomize synapses appropriately
                for (Synapse synapse : synapses) {
                    if (view.synapseIsAdjustable(synapse)) {
                        switch (view) {
                            case ALL:
                                synapse.forceSetStrength(allRandomizer.getRandom());
                                break;
                            case OVERLAY:
                                if (SynapseView.INHIBITORY.synapseIsAdjustable(synapse)) {
                                    synapse.forceSetStrength(inhibitoryRandomizer.getRandom());
                                }
                                if (SynapseView.EXCITATORY.synapseIsAdjustable(synapse)) {
                                    synapse.forceSetStrength(excitatoryRandomizer.getRandom());

                                }
                                break;
                            case EXCITATORY:
                                synapse.forceSetStrength(excitatoryRandomizer.getRandom());
                                break;
                            case INHIBITORY:
                                synapse.forceSetStrength(inhibitoryRandomizer.getRandom());
                        }
                    }
                }
                fullUpdate();
                networkPanel.getNetwork().fireSynapsesUpdated(synapses);
            }
        });

        synTypeSelector.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Show stats and histogram only for selected type(s)...
                updateHistogram();
                updateStats();
                getParent().revalidate();
                getParent().repaint();
                SynapseView view = (SynapseView) synTypeSelector.getSelectedItem();
                switch (view) {
//                    case ALL:
//                        randomPanel.fillFieldValues(allRandomizer);
//                    case OVERLAY:
//                        randomPanel.fillFieldValues(allRandomizer);
//                    case INHIBITORY:
//                        randomPanel.fillFieldValues(inhibitoryRandomizer);
//                    case EXCITATORY:
//                        randomPanel.fillFieldValues(excitatoryRandomizer);
                }
            }
        });

    }

    /**
     * Extracts weight values and organizes them by synapse type (inhibitory or
     * excitatory). Inhibitory values are represented by their absolute value.
     */
    private void extractWeightValues(List<Synapse> synapses) {

        int exWeights = 0;
        int inWeights = 0;

        // TODO: Get rid of this... replace with separate arraylists that
        // are preallocated. It should actually be more efficient.

        // Inefficient but necessary due to lack of support for collections of
        // primitive types.
        for (Synapse s : synapses) {
            double w = s.getStrength();
            if (w > 0) {
                exWeights++;
            } else {
                inWeights++;
            }
        }

        weights[0] = new double[exWeights];
        weights[1] = new double[inWeights];
        exWeights = 0;
        inWeights = 0;

        if (weights[0].length != 0) {
            // Inefficient but necessary due to lack of support for collections
            // of
            // primitive types.
            for (Synapse s : synapses) {
                double w = s.getStrength();
                if (w > 0) {
                    weights[0][exWeights++] = w;
                } else {
                    weights[1][inWeights++] = w;
                }
            }

        }

    }

    /**
     * Fully updates the histogram based on the status of the synapses in
     * question.
     */
    private void fullUpdate() {
        extractWeightValues(synapses);
        updateHistogram();
        updateStats();
        getParent().revalidate();
        getParent().repaint();
    }

    /**
     * Updates the histogram based on the selected synapses and selected
     * options. Can plot combined excitatory and absolute inhibitory, overlaid
     * excitatory/absolute inhibitory, only excitatory, or only inhibitory.
     * Histogram must be initialized prior to invocation. Red is used to
     * represent excitatory values, blue is used for inhibitory.
     */
    private void updateHistogram() {

        List<double[]> data = new ArrayList<double[]>();
        List<String> names = new ArrayList<String>();

        switch ((SynapseView) synTypeSelector.getSelectedItem()) {

            // The absolute value of all the weights are combined into a
            // single row.
            case ALL: {
                // Send the histogram the excitatory and absolute inhibitory
                // synapse values as separate data series.
                double[] hist1 = weights[0];
                double[] hist2 = weights[1];
                // The names of both series
                names.add(SynapseView.EXCITATORY.toString());
                names.add(SynapseView.INHIBITORY.toString());
                data.add(hist1);
                data.add(hist2);
            }
            ;
            break;

            // The weights as they are stored is appropriate except that the
            // inhibitory values must be converted into non-negative values
            case OVERLAY: {
                // Send the histogram the excitatory and absolute inhibitory
                // synapse values as separate data series.
                double[] hist1 = weights[0];
                double[] hist2 = new double[weights[1].length];
                for (int i = 0, n = hist2.length; i < n; i++) {
                    hist2[i] = Math.abs(weights[1][i]);
                }
                // The names of both series
                names.add(SynapseView.EXCITATORY.toString());
                names.add(SynapseView.INHIBITORY.toString());
                data.add(hist1);
                data.add(hist2);
            }
            ;
            break;

            // Data is a single row copy of first row of weights
            case EXCITATORY: {
                // Send the histogram only excitatory weights as a single series
                double[] hist = weights[0];
                // Name the series
                names.add(SynapseView.EXCITATORY.toString());
                data.add(hist);
            }
            ;
            break;

            // Data is a single row copy of second row of weights, negative
            // values are allowed here.
            case INHIBITORY: {
                // Send the histogram only inhibitory weights as a single series
                double[] hist = weights[1];
                // Name the series
                names.add(SynapseView.INHIBITORY.toString());
                data.add(hist);
            }
            ;
            break;

            default: {
                throw new IllegalArgumentException("Invalid Synapse" + " Selection.");
            }
        }

        // Send the histogram the new data and re-draw it.
        histogramPanel.getModel().resetData(data, names);
        histogramPanel.getModel().setSeriesColor(SynapseView.ALL.toString(), HistogramPanel.getDefault_Pallet()[0]);
        histogramPanel.getModel().setSeriesColor(SynapseView.EXCITATORY.toString(), HistogramPanel.getDefault_Pallet()[0]);
        histogramPanel.getModel().setSeriesColor(SynapseView.INHIBITORY.toString(), HistogramPanel.getDefault_Pallet()[1]);
        histogramPanel.reRender();

    }

    /**
     * Updates the values in the stats panel (number of synapses, excitatory
     * synapses, inhibitory synapses, and mean, median and standard deviation of
     * selected synapses. Extract data should be used prior to this.
     */
    private void updateStats() {

        statCalculator.calcStats();

        meanLabel.setText("Mean: " + SimbrainMath.roundDouble(statCalculator.getMean(), 5));
        medianLabel.setText("Median: " + SimbrainMath.roundDouble(statCalculator.getMedian(), 5));
        sdLabel.setText("Std. Dev: " + SimbrainMath.roundDouble(statCalculator.getStdDev(), 5));

        int tot = weights[0].length + weights[1].length;
        numSynsLabel.setText("Synapses: " + Integer.toString(tot));
        numExSynsLabel.setText("Excitatory : " + Integer.toString(weights[0].length));
        numInSynsLabel.setText("Inhibitory: " + Integer.toString(weights[1].length));

        statsPanel.revalidate();
        statsPanel.repaint();

    }

    /**
     * @author Zoë
     */
    public final class StatisticsBlock {

        private double mean;

        private double median;

        private double stdDev;

        /**
         * Gets the basic statistics: mean, median, and standard deviation of
         * the synapse weights based on which group of synapses is selected.
         *
         * @return an An array where the first element is the mean, the 2nd
         * element is the median, and the 3rd element is the standard
         * deviation.
         */
        private void calcStats() {
            double[] data = null;
            int tot = 0;
            SynapseView type = (SynapseView) synTypeSelector.getSelectedItem();
            double runningVal = 0;

            if (weights[0].length == 0 && weights[1].length == 0) {
                return;
            }

            // Determine selected type(s) and collect data accordingly...
            if (type.equals(SynapseView.ALL)) {
                tot = weights[0].length + weights[1].length;
                data = new double[tot];
                int c = 0;
                for (int i = 0; i < 2; i++) {
                    for (int j = 0, m = weights[i].length; j < m; j++) {
                        double val = weights[i][j];
                        runningVal += val;
                        data[c] = val;
                        c++;
                    }
                }
            } else if (type.equals(SynapseView.OVERLAY)) {
                tot = weights[0].length + weights[1].length;
                data = new double[tot];
                int c = 0;
                for (int i = 0; i < 2; i++) {
                    for (int j = 0, m = weights[i].length; j < m; j++) {
                        double val = Math.abs(weights[i][j]);
                        runningVal += val;
                        data[c] = val;
                        c++;
                    }
                }
            } else if (type.equals(SynapseView.EXCITATORY) && weights[0].length != 0) {
                tot = weights[0].length;
                data = new double[tot];
                for (int j = 0; j < tot; j++) {
                    double val = Math.abs(weights[0][j]);
                    runningVal += val;
                    data[j] = val;
                }

            } else if (type.equals(SynapseView.INHIBITORY) && weights[1].length != 0) {
                tot = weights[1].length;
                data = new double[tot];
                for (int j = 0; j < tot; j++) {
                    double val = weights[1][j];
                    runningVal += val;
                    data[j] = val;
                }
            }

            if (data != null) {
                mean = runningVal / tot;
                Arrays.sort(data);
                if (tot % 2 == 0) {
                    median = (data[tot / 2] + data[(tot / 2) - 1]) / 2;
                } else {
                    median = data[(int) Math.floor(tot / 2)];
                }
                runningVal = 0;
                for (int i = 0; i < tot; i++) {
                    runningVal += Math.pow((mean - data[i]), 2);
                }
                runningVal = runningVal / tot;
                stdDev = Math.sqrt(runningVal);
            }
        }

        public double getMean() {
            return mean;
        }

        public double getMedian() {
            return median;
        }

        public double getStdDev() {
            return stdDev;
        }

    }

    /**
     * Panel for scaling synapses.
     */
    @SuppressWarnings("serial")
    public class ScalerPanel extends LabelledItemPanel {

        /**
         * Percentage to increase or decrease indicated synapses.
         */
        private JTextField tfIncreaseDecrease = new JTextField(".1");

        /**
         * Button for increasing synapse strengths.
         */
        private JButton increaseButton = new JButton("Increase");

        /**
         * Button for decreasing synapse strengths.
         */
        private JButton decreaseButton = new JButton("Decrease");

        /**
         * Construct the scaler panel.
         *
         * @param networkPanel parent network panel
         */
        public ScalerPanel(final NetworkPanel networkPanel) {
            addItem("Percent to change", tfIncreaseDecrease);
            addItem("Increase", increaseButton);
            increaseButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double amount = Double.parseDouble(tfIncreaseDecrease.getText());
                    SynapseView view = (SynapseView) synTypeSelector.getSelectedItem();
                    for (Synapse synapse : synapses) {
                        if (view.synapseIsAdjustable(synapse)) {
                            synapse.forceSetStrength(synapse.getStrength() + synapse.getStrength() * amount);
                        }
                    }
                    fullUpdate();
                    networkPanel.getNetwork().fireSynapsesUpdated(synapses);
                }
            });

            addItem("Decrease", decreaseButton);
            decreaseButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    double amount = Double.parseDouble(tfIncreaseDecrease.getText());
                    SynapseView view = (SynapseView) synTypeSelector.getSelectedItem();
                    for (final Synapse synapse : synapses) {
                        if (view.synapseIsAdjustable(synapse)) {
                            synapse.forceSetStrength(synapse.getStrength() - synapse.getStrength() * amount);
                        }
                    }
                    fullUpdate();
                    networkPanel.getNetwork().fireSynapsesUpdated(synapses);
                }
            });
        }
    }

    /**
     * Panel for pruning synapses.
     */
    @SuppressWarnings("serial")
    public class PrunerPanel extends LabelledItemPanel {

        /**
         * Threshold. If synapse strength above absolute value of this value
         * prune the synapse when the prune button is pressed.
         */
        private final JTextField tfThreshold = new JTextField(".1");

        /**
         * Construct the panel.
         *
         * @param networkPanel reference to parent network panel.
         */
        public PrunerPanel(final NetworkPanel networkPanel) {
            JButton pruneButton = new JButton("Prune");
            addItem("Prune", pruneButton);
            addItem("Threshold", tfThreshold);

            pruneButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double threshold = Double.parseDouble(tfThreshold.getText());
                    SynapseView view = (SynapseView) synTypeSelector.getSelectedItem();
                    for (Synapse synapse : synapses) {
                        if (view.synapseIsAdjustable(synapse)) {
                            if (Math.abs(synapse.getStrength()) < threshold) {
                                networkPanel.getNetwork().removeSynapse(synapse);
                            }
                        }
                    }
                    fullUpdate();
                    networkPanel.getNetwork().fireSynapsesUpdated(synapses);
                }
            });
        }
    }

}