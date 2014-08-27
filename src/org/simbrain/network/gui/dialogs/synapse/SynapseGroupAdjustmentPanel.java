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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.dialogs.connect.SynapsePolarityAndRandomizerPanel;
import org.simbrain.network.gui.dialogs.synapse.SynapseAdjustmentPanel.SynapseView;
import org.simbrain.plot.histogram.HistogramModel;
import org.simbrain.plot.histogram.HistogramPanel;
import org.simbrain.util.math.SimbrainMath;

/**
 * A panel for adjusting the polarity and weights of a synapse group as well as
 * its weight randomizers, and displaying the results in a histogram.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 *
 */
public class SynapseGroupAdjustmentPanel extends JPanel {

    /**
     * A combo box for selecting which kind of synapses should have their stats
     * displayed and/or what kind of display.
     */
    private JComboBox<SynapseView> synTypeSelector =
        new JComboBox<SynapseView>(
            SynapseView.values());

    /** The synapses being viewed in the histogram. */
    private SynapseView synapseView = SynapseView.values()[0];

    /** A panel displaying basic statistics about the synapses. */
    private Stats statPanel = new Stats();

    /**
     * A histogram plotting the strength of synapses over given intervals (bins)
     * against their frequency.
     */
    private final HistogramPanel histogramPanel = new HistogramPanel(
        new HistogramModel(2));

    /**
     * The panel governing the percent excitatory connections and the randomizer
     * associated with each polarity.
     */
    private final SynapsePolarityAndRandomizerPanel excitatoryPercentPanel;

    /** The synapse group being displayed/edited. */
    private final SynapseGroup synapseGroup;

    /** Whether or not this is being used for creation. */
    private boolean creationPanel;

    /**
     * Create the synapse group adjustment panel.
     *
     * @param parent the parent window
     * @param synapseGroup the group to adjust
     * @return the constructed panel
     */
    public static SynapseGroupAdjustmentPanel
        createSynapseGroupAdjustmentPanel(
            Window parent, SynapseGroup synapseGroup, boolean isCreation) {
        SynapseGroupAdjustmentPanel sgap = new SynapseGroupAdjustmentPanel(
            parent, synapseGroup, isCreation);
        sgap.addListeners();
        return sgap;
    }

    /**
     * Private constructor used by factory method.
     *
     * @param parent parent window
     * @param synapseGroup group to adjust
     */
    private SynapseGroupAdjustmentPanel(Window parent,
        SynapseGroup synapseGroup, boolean isCreation) {
        this.synapseGroup = synapseGroup;
        this.creationPanel = isCreation;
        synTypeSelector.setVisible(!creationPanel);
        histogramPanel.setVisible(!creationPanel);
        statPanel.setVisible(!creationPanel);
        statPanel.update();
        excitatoryPercentPanel = SynapsePolarityAndRandomizerPanel
            .createPolarityRatioPanel(parent, synapseGroup);
        init();
    }

    /**
     * Lays out the panel.
     */
    private void init() {
        GridBagConstraints gbc = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = HistogramPanel.GRID_WIDTH - 1;
        gbc.gridheight = 1;

        this.add(statPanel, gbc);

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

        // Set initial size of histogram panel based on screen size
        int height =
            (int) (.33 * Toolkit.getDefaultToolkit().getScreenSize().height);
        int width = this.getPreferredSize().width;
        histogramPanel.setPreferredSize(new Dimension(width, height));

        this.add(histogramPanel, gbc);

        gbc.gridy += HistogramPanel.GRID_HEIGHT;
        gbc.gridheight = 1;

        this.add(excitatoryPercentPanel, gbc);
        fullUpdate();
    }

    /**
     * Adds listeners to the panel, particularly to the randomizers and
     * excitatory/inhibitory slider, so that the histogram and stats panel
     * change upon application of their new settings.
     */
    private void addListeners() {

        synTypeSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    synapseView = (SynapseView) synTypeSelector
                        .getSelectedItem();
                    fullUpdate();
                }
            }
        });

        excitatoryPercentPanel
            .addSliderApplyActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            fullUpdate();
                        }
                    });
                }
            });

        excitatoryPercentPanel.getExcitatoryRandomizerPanel()
            .addApplyActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            fullUpdate();
                        }
                    });
                }
            });

        excitatoryPercentPanel.getInhibitoryRandomizerPanel()
            .addApplyActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            fullUpdate();
                        }
                    });
                }
            });
    }

    /**
     * Updates the entire panel by changing the histogram to reflect and the
     * statistics panel to reflect the current values.
     */
    public void fullUpdate() {
        if (!creationPanel) {
            updateHistogram();
            statPanel.update();
        }
        revalidate();
        repaint();
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
                double[] hist1 = synapseGroup.getExcitatoryStrengths();
                double[] hist2 = synapseGroup.getInhibitoryStrengths();
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
                double[] hist1 = synapseGroup.getExcitatoryStrengths();
                double[] hist2 = synapseGroup.getInhibitoryStrengths();
                for (int i = 0, n = hist2.length; i < n; i++) {
                    hist2[i] = Math.abs(hist2[i]);
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
                double[] hist = synapseGroup.getExcitatoryStrengths();
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
                double[] hist = synapseGroup.getInhibitoryStrengths();
                // Name the series
                names.add(SynapseView.INHIBITORY.toString());
                data.add(hist);
            }
                ;
                break;

            default: {
                throw new IllegalArgumentException("Invalid Synapse"
                    + " Selection.");
            }
        }

        // Send the histogram the new data and re-draw it.
        histogramPanel.getModel().resetData(data, names);
        histogramPanel.getModel().setSeriesColor(SynapseView.ALL.toString(),
            HistogramPanel.getDefault_Pallet()[0]);
        histogramPanel.getModel().setSeriesColor(
            SynapseView.EXCITATORY.toString(),
            HistogramPanel.getDefault_Pallet()[0]);
        histogramPanel.getModel().setSeriesColor(
            SynapseView.INHIBITORY.toString(),
            HistogramPanel.getDefault_Pallet()[1]);
        histogramPanel.reRender();

    }

    /**
     *
     */
    public void commitChanges() {
        excitatoryPercentPanel.commitChanges(synapseGroup);
    }

    /**
     *
     * @return
     */
    public Collection<Synapse> getCurrentlyEditableSynapses() {
        switch (synapseView) {
            case ALL:
                return synapseGroup.getAllSynapses();
            case OVERLAY:
                return synapseGroup.getAllSynapses();
            case EXCITATORY:
                return synapseGroup.getExcitatorySynapses();
            case INHIBITORY:
                return synapseGroup.getInhibitorySynapses();
            default:
                throw new IllegalArgumentException("No such synapse view.");
        }
    }

    /**
     *
     * A panel for displaying basic statistics about the synapse group
     *
     * @author Zach Tosi
     *
     */
    public class Stats extends JPanel {

        double mean;

        double median;

        double stdDev;

        private JLabel numSynapses = new JLabel();

        private JLabel exSynapses = new JLabel();

        private JLabel inSynapses = new JLabel();

        private JLabel meanLabel = new JLabel();

        private JLabel medianLabel = new JLabel();

        private JLabel stdDevLabel = new JLabel();

        /**
         *
         */
        public Stats() {
            super();
            layoutPanel();
        }

        /**
         *
         */
        public void update() {
            calcStats();
            numSynapses.setText(Integer.toString(synapseGroup.size()));
            exSynapses.setText(Integer.toString(synapseGroup
                .getExcitatorySynapses().size()));
            inSynapses.setText(Integer.toString(synapseGroup
                .getInhibitorySynapses().size()));
            meanLabel
                .setText(Double.toString(SimbrainMath.roundDouble(mean, 4)));
            medianLabel.setText(Double.toString(SimbrainMath.roundDouble(
                median, 4)));
            stdDevLabel.setText(Double.toString(SimbrainMath.roundDouble(
                stdDev, 4)));
            revalidate();
            repaint();
        }

        /**
         *
         */
        public void layoutPanel() {
            this.setLayout(new GridLayout(3, 4));
            add(new JLabel("Synapses:"));
            add(numSynapses);
            add(new JLabel("Mean:"));
            add(meanLabel);
            add(new JLabel("Excitatory:"));
            add(exSynapses);
            add(new JLabel("Median:"));
            add(medianLabel);
            add(new JLabel("Inhibitory:"));
            add(inSynapses);
            add(new JLabel("Std. Dev.:"));
            add(stdDevLabel);

        }

        /**
         *
         */
        public void calcStats() {
            Collection<Synapse> synapses = getCurrentlyEditableSynapses();
            mean = getMean(synapses);
            median = getMedian(synapses);
            stdDev = getStdDev(synapses);
        }

        /**
         *
         * @param synapses
         * @return
         */
        public double getMean(Collection<Synapse> synapses) {
            double tot = 0;
            if (synapses.size() == 0) {
                return 0;
            }
            if (SynapseView.OVERLAY.equals(synapseView)) {
                for (Synapse s : synapses) {
                    tot += Math.abs(s.getStrength());
                }
            } else {
                for (Synapse s : synapses) {
                    tot += s.getStrength();
                }
            }
            return tot / synapses.size();
        }

        /**
         *
         * @param synapses
         * @return
         */
        public double getMedian(Collection<Synapse> synapses) {
            double[] vals = new double[synapses.size()];
            int index = 0;
            if (synapses.size() == 0) {
                return 0;
            }
            if (SynapseView.OVERLAY.equals(synapseView)) {
                for (Synapse s : synapses) {
                    vals[index++] = Math.abs(s.getStrength());
                }
            } else {
                for (Synapse s : synapses) {
                    vals[index++] = s.getStrength();
                }
            }
            Arrays.sort(vals);
            if (synapses.size() % 2 == 0) {
                return (vals[vals.length / 2 - 1] + vals[vals.length / 2]) / 2;
            } else {
                return vals[vals.length / 2];
            }
        }

        /**
         *
         * @param synapses
         * @return
         */
        public double getStdDev(Collection<Synapse> synapses) {
            double tot = 0;
            if (synapses.isEmpty()) {
                return 0;
            }
            if (SynapseView.OVERLAY.equals(synapseView)) {
                for (Synapse s : synapses) {
                    tot += (mean - Math.abs(s.getStrength()))
                        * (mean - Math.abs(s.getStrength()));
                }
            } else {
                for (Synapse s : synapses) {
                    tot += (mean - s.getStrength()) * (mean - s.getStrength());
                }
            }
            return Math.sqrt(tot / synapses.size());
        }

    }
}
