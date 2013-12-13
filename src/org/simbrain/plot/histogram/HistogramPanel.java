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
package org.simbrain.plot.histogram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.ui.RefineryUtilities;

/**
 * Panel to display histogram. Used both for the plot component and as a
 * resuable component (e.g. in synapse adjustment panel).
 *
 * Supports multiple simultaneous data sets represented by different colors,
 * partially transparent to make overlap visible. The histogram takes the form
 * of a chart supported by a panel which is then placed within the final panel.
 * This panel supports dynamically changing the number of histogram bins
 * graphically, but only logically supports the altering of data series, relying
 * on other classes for graphical representations of such.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class HistogramPanel extends JPanel {

    /** The default preferred height. */
    private static final int DEFAULT_PREF_HEIGHT = 200;

    /** The default preferred width. */
    private static final int DEFAULT_PREF_WIDTH = 350;

    /** The preferred dimensions of the histogram. */
    private Dimension dimPref = new Dimension(DEFAULT_PREF_WIDTH,
            DEFAULT_PREF_HEIGHT);

    /** The grid width of this panel, for use by possible parent panels. */
    public static final int GRID_WIDTH = 3;

    /** The grid height of this panel, for use by possible parent panels. */
    public static final int GRID_HEIGHT = 3;

    /** Constant Alpha value governing transparency of histogram colors. */
    public static final byte DEFAULT_ALPHA = -0x50;

    /**
     * The default (and maximum, unless otherwise changed) number of colors
     * available to different data sets. Implicitly this is the default maximum
     * number of supported data sets.
     **/
    private static final int DEFAULT_NUM_DATASETS = 4;

    /** The color pallete, initialized to the default number of data sets. */
    public static final Color[] DEFAULT_PALLET = new Color[DEFAULT_NUM_DATASETS];

    /**
     * The standard color pallete, Red, Blue, Green, Yellow. Colors are
     * represented as a single integer where bits 0-7 consist of the blue
     * component, bits 8-15 consist of the green component, bits 16-23 consist
     * of the red component, and bits 24-31 consist of the alpha component. Here
     * bits are shifted to remove the default alpha value, then shifted back and
     * replaced with ALPHA.
     */
    {
        DEFAULT_PALLET[0] = new Color((Color.RED.getRGB() << 8) >>> 8
                | DEFAULT_ALPHA << 24, true);
        DEFAULT_PALLET[1] = new Color((Color.BLUE.getRGB() << 8) >>> 8
                | DEFAULT_ALPHA << 24, true);
        DEFAULT_PALLET[2] = new Color((Color.GREEN.getRGB() << 8) >>> 8
                | DEFAULT_ALPHA << 24, true);
        DEFAULT_PALLET[3] = new Color((Color.YELLOW.getRGB() << 8) >>> 8
                | DEFAULT_ALPHA << 24, true);
    }

    /** The color pallet, initialized to the default number of data sets. */
    private Color[] colorPallet = Arrays.copyOf(DEFAULT_PALLET,
            DEFAULT_NUM_DATASETS);

    /** The main panel supporting the histogram chart. */
    private JPanel mainPanel;

    /** The core chart supporting the actual histogram. */
    private JFreeChart mainChart;

    /** X axis label. */
    private String xAxisName = "";

    /** Y axis label. */
    private String yAxisName = "";

    /** The title of the histogram. */
    private String title = "";

    /** A button for updating the histogram for different numbers of bins. */
    private JButton binButton = new JButton("Set bins");

    /** Number of bins label. */
    private JLabel numBinLabel = new JLabel("# of Bins: ");

    /** A text field for specifying the number of bins. */
    private JTextField numBins = new JTextField(6);

    /** Reference to the histogram data. */
    private final HistogramModel model;

    /**
     * Construct a new histogram panel.
     *
     * @param model reference to underlying data
     */
    public HistogramPanel(final HistogramModel model) {
        this.model = model;
        this.setLayout(new BorderLayout());

        setPreferredSize(dimPref);
        createHistogram();

        JPanel buttonPanel = new JPanel();
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.resetData();
            }
        });
        buttonPanel.add(clearButton);
        buttonPanel.add(binButton);
        buttonPanel.add(numBinLabel);
        numBins.setText("" + model.getBins());
        buttonPanel.add(numBins);

        binButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                HistogramModel model = HistogramPanel.this.getModel();
                try {
                    model.setBins(Integer.parseInt(numBins.getText()));
                    model.redraw();
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                    JOptionPane.showMessageDialog(getParent(),
                            "Non-Integer number of bins.", "Error",
                            JOptionPane.ERROR);
                }
            }

        });

        this.add("South", buttonPanel);
        this.add("Center", mainPanel);

    }

    /**
     * Create the histogram panel based on the data.
     */
    public void createHistogram() {
        try {
            if (this.getModel().getData() != null) {
                mainChart = ChartFactory.createHistogram(title, xAxisName,
                        yAxisName, model.getDataSet(),
                        PlotOrientation.VERTICAL, true, true, false);
                mainChart.setBackgroundPaint(UIManager
                        .getColor("this.Background"));
                XYPlot plot = (XYPlot) mainChart.getPlot();
                plot.setForegroundAlpha(0.75F);
                XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
                renderer.setDrawBarOutline(false);
                renderer.setShadowVisible(false);

                for (int i = 0; i < model.getData().size(); i++) {
                    if (i < colorPallet.length) {
                        renderer.setSeriesPaint(i, colorPallet[i], true);
                    }
                }

            } else {

                mainChart = ChartFactory.createHistogram(title, xAxisName,
                        yAxisName, model.getDataSet(),
                        PlotOrientation.VERTICAL, true, true, false);
                mainChart.setBackgroundPaint(UIManager
                        .getColor("this.Background"));

            }

        } catch (IllegalArgumentException iaEx) {
            iaEx.printStackTrace();
            JOptionPane.showMessageDialog(null, iaEx.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IllegalStateException isEx) {
            isEx.printStackTrace();
            JOptionPane.showMessageDialog(null, isEx.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        mainPanel = new ChartPanel(mainChart);

    }

    /**
     * Initializes all listeners. At present this includes only the plot button
     * which redraws the histogram.
     */
    private void initializeListeners() {

        binButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                HistogramModel model = HistogramPanel.this.getModel();
                try {
                    model.setBins(Integer.parseInt(numBins.getText()));
                    // reDraw();
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                    JOptionPane.showMessageDialog(getParent(),
                            "Non-Integer number of bins.", "Error",
                            JOptionPane.ERROR);
                }

            }

        });

    }

    /**
     * Set a custom color pallete for this histogram to allow more data series
     * to be represented or to alter the default colors. Colors specified in the
     * pallete should be interpretable as a bitwise representation. That is:
     * Colors ought to be represented as a single integer where bits 0-7 consist
     * of the blue component, bits 8-15 consist of the green component, bits
     * 16-23 consist of the red component, and bits 24-31 consist of the alpha
     * component. Specified colors must have an alpha component, and should use
     * the default ALPHA value specified here.
     *
     * @param colorPallet the custom color pallete.
     */
    public void setColorPallet(Color[] colorPallet) {
        this.colorPallet = colorPallet;
    }

    /**
     * Main method for testing.
     */
    public static void main(String[] args) {

        List<double[]> histograms = new ArrayList<double[]>();
        Random random = new Random(3141592L);
        double[] r = new double[1000];
        for (int i = 0; i < 1000; i++) {
            r[i] = random.nextGaussian() + 100;
        }
        histograms.add(r);
        double[] r2 = new double[1000];
        for (int i = 0; i < 1000; i++) {
            r2[i] = random.nextGaussian() + 102;
        }
        histograms.add(r2);

        List<String> names = Arrays.asList("Joe", "Jane");

        JFrame bob = new JFrame();

        HistogramModel h = new HistogramModel(histograms, names, 50);

        bob.setContentPane(new HistogramPanel(h));
        bob.pack();
        RefineryUtilities.centerFrameOnScreen(bob);
        bob.setVisible(true);
    }

    /**
     * Return reference to underlying data.
     *
     * @return the data
     */
    public HistogramModel getModel() {
        return model;
    }

    /**
     * Returns default number of datasets.
     *
     * @return default num datasets.
     */
    public static int getDefaultNumDatasets() {
        return DEFAULT_NUM_DATASETS;
    }

    /**
     * @return the xAxisName
     */
    public String getxAxisName() {
        return xAxisName;
    }

    /**
     * @param xAxisName the xAxisName to set
     */
    public void setxAxisName(String xAxisName) {
        this.xAxisName = xAxisName;
    }

    /**
     * @return the yAxisName
     */
    public String getyAxisName() {
        return yAxisName;
    }

    /**
     * @param yAxisName the yAxisName to set
     */
    public void setyAxisName(String yAxisName) {
        this.yAxisName = yAxisName;
    }

}
