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
package org.simbrain.plot.projection;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.Utils;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.projection.*;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.gui.DesktopComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Ellipse2D;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gui Component to display a high dimensional projection object.
 */
public class ProjectionDesktopComponent extends DesktopComponent<ProjectionComponent> {

    /**
     * The JFreeChart chart.
     */
    private JFreeChart chart;

    /**
     * JChart representation of the data.
     */
    private XYSeriesCollection xyCollection;

    /**
     * Executor service for running iterable projection methods.
     */
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * List of projector types.
     */
    private JComboBox<String> projectionList = new JComboBox<String>();

    /**
     * Iterate once.
     */
    protected JButton iterateBtn = new JButton(ResourceManager.getImageIcon("menu_icons/Step.png"));

    /**
     * Play button.
     */
    private JButton playBtn = new JButton(ResourceManager.getImageIcon("menu_icons/Play.png"));

    /**
     * Points indicator.
     */
    private JLabel pointsLabel = new JLabel();

    /**
     * Dimension indicator.
     */
    private JLabel dimsLabel = new JLabel(" Dimensions:");

    /**
     * Error indicator.
     */
    private JLabel errorLabel = new JLabel();

    /**
     * Error bar.
     */
    private JToolBar errorBar = new JToolBar();

    /**
     * Show error option.
     */
    private boolean showError = true;

    /**
     * Warning label.
     */
    private JLabel warningLabel = new JLabel(ResourceManager.getImageIcon("menu_icons/Warning.png"));

    /**
     * Combo box for first dimension of coordinate projection.
     */
    private JComboBox<Integer> adjustDimension1 = new JComboBox<>();

    /**
     * Model for adjustDimension1.
     */
    private DefaultComboBoxModel<Integer> adjustDimension1Model = new DefaultComboBoxModel<Integer>();

    /**
     * Combo box for first dimension of coordinate projection.
     */
    private JComboBox<Integer> adjustDimension2 = new JComboBox<Integer>();

    /**
     * Model for adjustDimension2.
     */
    private DefaultComboBoxModel<Integer> adjustDimension2Model = new DefaultComboBoxModel<Integer>();

    /**
     * Panel for showing Sammon step size and label, both with tooltip.
     */
    private Box sammonStepSizePanel = Box.createHorizontalBox();

    /**
     * Construct the Projection GUI.
     */
    public ProjectionDesktopComponent(final GenericFrame frame, final ProjectionComponent component) {
        super(frame, component);
        setPreferredSize(new Dimension(500, 400));
        setLayout(new BorderLayout());

        Projector proj = getWorkspaceComponent().getProjector();

        // Generate the graph
        xyCollection = new XYSeriesCollection();
        xyCollection.addSeries(new XYSeries("Data", false, true));
        chart = ChartFactory.createScatterPlot("", "Projection X", "Projection Y", xyCollection, PlotOrientation.VERTICAL, false, true, false);
        // chart.getXYPlot().getDomainAxis().setRange(-100, 100);
        // chart.getXYPlot().getRangeAxis().setRange(-100, 100);
        chart.getXYPlot().setBackgroundPaint(Color.white);
        chart.getXYPlot().setDomainGridlinePaint(Color.gray);
        chart.getXYPlot().setRangeGridlinePaint(Color.gray);
        chart.getXYPlot().getDomainAxis().setAutoRange(true);
        chart.getXYPlot().getRangeAxis().setAutoRange(true);
        chart.getXYPlot().setForegroundAlpha(.5f); // TODO: Make this settable

        ChartPanel chartPanel = new ChartPanel(chart);

        // Custom render points as dots (not squares) and use custom tooltips
        // that show high-d point
        CustomRenderer renderer = new CustomRenderer();
        chart.getXYPlot().setRenderer(renderer);
        // TODO: Make the visibility of series lines and point size adjustible
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-7, -7, 7, 7));
        CustomToolTipGenerator generator = new CustomToolTipGenerator();
        renderer.setSeriesToolTipGenerator(0, generator);

        // TODO
        // Set up custom label generator, used with some couplings
        // renderer.setBaseItemLabelsVisible(true);
        // renderer.setBaseItemLabelGenerator(new LegendXYItemLabelGenerator());

        // Toolbar
        JButton openBtn = new JButton(ResourceManager.getImageIcon("menu_icons/Open.png"));
        openBtn.setToolTipText("Open high-dimensional data");
        JButton saveBtn = new JButton(ResourceManager.getImageIcon("menu_icons/Save.png"));
        saveBtn.setToolTipText("Save data");
        projectionList.setMaximumSize(new java.awt.Dimension(200, 100));

        iterateBtn = new JButton(ResourceManager.getImageIcon("menu_icons/Step.png"));
        iterateBtn.addActionListener(e -> {
            proj.iterate();
            update();
        });

        playBtn = new JButton(ResourceManager.getImageIcon("menu_icons/Play.png"));
        playBtn.addActionListener(e -> {
            if (proj.isRunning()) {
                // If already running, image is stop. Click and it should stop the algorithm and
                // change the image to play.
                playBtn.setToolTipText("Start iterating projection algorithm");
                playBtn.setIcon(ResourceManager.getImageIcon("menu_icons/Play.png"));
                proj.setRunning(false);
            } else {
                // If not running, image is play. Click and it should run the algorithm
                // and change the image to stop.
                playBtn.setIcon(ResourceManager.getImageIcon("menu_icons/Stop.png"));
                playBtn.setToolTipText("Stop iterating projection algorithm");
                proj.setRunning(true);
                executor.execute(() -> {
                    while (proj.isRunning()) {
                        proj.iterate();
                        proj.getEvents().fireDataChanged();
                    }
                });
            }
        });

        JButton clearBtn = new JButton(ResourceManager.getImageIcon("menu_icons/Eraser.png"));
        clearBtn.addActionListener(e ->
                SwingUtilities.invokeLater(() -> {
                    getWorkspaceComponent().clearData();
                    xyCollection.getSeries(0).clear();
                }));

        JButton prefsBtn = new JButton(ResourceManager.getImageIcon("menu_icons/Prefs.gif"));
        prefsBtn.addActionListener(e -> {
            // TODO (Still working out overall dialog structure).
        });

        JButton randomBtn = new JButton(ResourceManager.getImageIcon("menu_icons/Rand.png"));
        randomBtn.addActionListener(e -> getWorkspaceComponent().getProjector().randomize(100));

        JToolBar theToolBar = new JToolBar();
        theToolBar.add(projectionList);
        playBtn.setToolTipText("Iterate projection algorithm");
        theToolBar.add(playBtn);
        iterateBtn.setToolTipText("Step projection algorithm");
        theToolBar.add(iterateBtn);
        clearBtn.setToolTipText("Clear current data");
        theToolBar.add(clearBtn);
        randomBtn.setToolTipText("Randomize datapoints");
        theToolBar.add(randomBtn);
        theToolBar.addSeparator();
        warningLabel.setPreferredSize(new Dimension(16, 16));
        warningLabel.setToolTipText("This method works best with more datapoints already added");
        theToolBar.add(warningLabel);
        String stepSizeToolTip = "Scales the amount points are moved on each iteration";
        JLabel stepSizeLabel = new JLabel("Step Size");
        stepSizeLabel.setToolTipText(stepSizeToolTip);

        sammonStepSizePanel.add(stepSizeLabel);
        JTextField sammonStepSize = new JFormattedTextField("" + SimbrainPreferences.getDouble(
                "projectorSammonEpsilon"));
        sammonStepSize.setColumns(3);
        sammonStepSize.setToolTipText(stepSizeToolTip);
        sammonStepSizePanel.add(sammonStepSize);
        theToolBar.add(sammonStepSizePanel);

        adjustDimension1.setToolTipText("Dimension 1");
        adjustDimension2.setToolTipText("Dimension 2");
        theToolBar.add(adjustDimension1);
        theToolBar.add(adjustDimension2);

        // Help button
        JButton helpButton = new JButton();
        helpButton.setAction(new ShowHelpAction("Pages/Plot/projection.html"));

        // Setup Menu Bar
        createAttachMenuBar();

        JToolBar statusBar = new JToolBar();
        statusBar.add(pointsLabel);
        statusBar.add(dimsLabel);

        JToolBar errorBar = new JToolBar();
        errorBar.add(errorLabel);

        JPanel southPanel = new JPanel();
        southPanel.add(errorBar);
        southPanel.add(statusBar);

        Box bottomPanel = Box.createVerticalBox();
        bottomPanel.add("South", southPanel);

        // Put all panels together
        add("North", theToolBar);
        add("Center", chartPanel);
        add("South", bottomPanel);

        // Other initialization
        initializeComboBoxes();

        proj.getEvents().onPointFound(p -> update());
        proj.getEvents().onDataChanged(() -> {
            resetData();
            update();
        });
        proj.getEvents().onColorsChanged(() -> {
            proj.resetColors();
            update();
        });

        // Epsilon field should update the model whenever a user clicks out of
        // it.
        sammonStepSize.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                ProjectionMethod projMethod = getWorkspaceComponent().getProjector().getProjectionMethod();
                if (projMethod != null) {
                    if (projMethod instanceof ProjectSammon) {
                        ((ProjectSammon) projMethod).setEpsilon(Utils.doubleParsable(sammonStepSize.getText()));
                    }
                }
            }
        });
        resetData();
        update();
        updateToolBar();
    }

    /**
     * Initialize all the combo boxes.
     */
    private void initializeComboBoxes() {

        // Populate projection list combo box
        for (Entry<Class<?>, String> projMethod : getWorkspaceComponent().getProjector().getProjectionMethods().entrySet()) {
            projectionList.addItem(projMethod.getValue());
        }
        projectionList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedMethod = (String) projectionList.getSelectedItem();
                getWorkspaceComponent().getProjector().setProjectionMethod(selectedMethod);
                updateToolBar();
            }
        });
        projectionList.getModel().setSelectedItem(getWorkspaceComponent().getProjector().getCurrentMethodString());

        // Init the adjust dimension combo boxes
        updateCoordinateProjectionComboBoxes();
        adjustDimension1.setModel(adjustDimension1Model);
        adjustDimension1.addActionListener(e -> {
            ProjectionMethod projMethod = getWorkspaceComponent().getProjector().getProjectionMethod();
            if (projMethod != null) {
                if (projMethod instanceof ProjectCoordinate) {
                    ((ProjectCoordinate) projMethod).setHiD1(adjustDimension1.getSelectedIndex());
                    projMethod.project();
                    getWorkspaceComponent().getProjector().getEvents().fireDataChanged();
                }
            }
        });
        adjustDimension2.setModel(adjustDimension2Model);
        adjustDimension2.addActionListener(e -> {
            ProjectionMethod projecMethod = getWorkspaceComponent().getProjector().getProjectionMethod();
            if (projecMethod != null) {
                if (projecMethod instanceof ProjectCoordinate) {
                    ((ProjectCoordinate) projecMethod).setHiD2(adjustDimension2.getSelectedIndex());
                    projecMethod.project();
                    getWorkspaceComponent().getProjector().getEvents().fireDataChanged();
                }
            }
        });
    }

    /**
     * Update the toolbar based on current projection method.
     */
    private void updateToolBar() {
        ProjectionMethod proj = getWorkspaceComponent().getProjector().getProjectionMethod();
        if (proj == null) {
            return;
        }

        // Clear unused toolbar items
        if (!(proj instanceof ProjectSammon)) {
            sammonStepSizePanel.setVisible(false);
        }
        if (!(proj instanceof ProjectCoordinate)) {
            adjustDimension1.setVisible(false);
            adjustDimension2.setVisible(false);
        } else {
            // Set correct dimensions
            adjustDimension1.setSelectedIndex(((ProjectCoordinate) proj).getHiD1());
            adjustDimension2.setSelectedIndex(((ProjectCoordinate) proj).getHiD2());
        }

        // Handle error bar
        if ((proj.isIterable()) && (showError)) {
            errorBar.setVisible(true);
        } else {
            errorBar.setVisible(false);
        }

        // Handle warning
        if (getWorkspaceComponent().getProjector().getNumPoints() < proj.suggestedMinPoints()) {
            warningLabel.setVisible(true);
        } else {
            warningLabel.setVisible(false);
        }

        // Handle new toolbar items
        if (proj instanceof ProjectSammon) {
            sammonStepSizePanel.setVisible(true);
        } else if (proj instanceof ProjectCoordinate) {
            adjustDimension1.setVisible(true);
            adjustDimension2.setVisible(true);
        }

        // Handle iterable
        setToolbarIterable(proj.isIterable());

    }

    /**
     * Update the Coordinate projection combo boxes.
     */
    private void updateCoordinateProjectionComboBoxes() {

        adjustDimension1Model.removeAllElements();
        adjustDimension2Model.removeAllElements();
        int dims = getWorkspaceComponent().getProjector().getDimensions();
        for (int i = 0; i < dims; i++) {
            adjustDimension1Model.addElement(i + 1);
            adjustDimension2Model.addElement(i + 1);
        }
    }

    /**
     * Creates the menu bar.
     */
    private void createAttachMenuBar() {

        final JMenuBar bar = new JMenuBar();
        final JMenu fileMenu = new JMenu("File");

        PlotActionManager actionManager = new PlotActionManager(this);
        for (Action action : actionManager.getOpenSavePlotActions()) {
            fileMenu.add(action);
        }
        fileMenu.addSeparator();
        final JMenu exportImport = new JMenu("Export/Import...");
        fileMenu.add(exportImport);
        exportImport.add(ProjectionPlotActions.getImportData(getWorkspaceComponent().getProjector()));
        exportImport.addSeparator();
        exportImport.add(ProjectionPlotActions.getExportDataHi(getWorkspaceComponent().getProjector()));
        exportImport.add(ProjectionPlotActions.getExportDataLow(getWorkspaceComponent().getProjector()));
        fileMenu.addSeparator();
        fileMenu.add(new CloseAction(this.getWorkspaceComponent()));

        final JMenu editMenu = new JMenu("Edit");
        final JMenuItem preferencesGeneral = new JMenuItem("Preferences...");
        preferencesGeneral.addActionListener(e -> {
            ProjectionPreferencesDialog dialog = new ProjectionPreferencesDialog(getWorkspaceComponent().getProjector());
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
        editMenu.add(preferencesGeneral);


        final JMenuItem colorPrefs = new JMenuItem("Datapoint Coloring...");
        colorPrefs.addActionListener(e -> {
            DataPointColoringDialog dialog = new DataPointColoringDialog(getWorkspaceComponent().getProjector());
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
        editMenu.add(colorPrefs);

        final JMenuItem dims = new JMenuItem("Set dimensions...");
        dims.addActionListener(e -> {
            String dimensions = JOptionPane.showInputDialog("Dimensions:");
            if (dimensions != null) {
                getWorkspaceComponent().getProjector().init(Integer.parseInt(dimensions));
            }

        });
        // editMenu.add(dims);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem(new ShowHelpAction("Pages/Plot/projection.html"));
        helpMenu.add(helpItem);

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(helpMenu);

        getParentFrame().setJMenuBar(bar);
    }

    /**
     * Update labels at bottom of component.
     */
    protected void update() {
        chart.fireChartChanged();
        updateToolBar();
        dimsLabel.setText("     Dimensions: " + getWorkspaceComponent().getProjector().getUpstairs().getDimensions());
        pointsLabel.setText("  Datapoints: " + getWorkspaceComponent().getProjector().getDownstairs().getNumPoints());
        if (getWorkspaceComponent().getProjector().getProjectionMethod().isIterable()) {
            errorLabel.setText(" Error:" + ((IterableProjectionMethod) getWorkspaceComponent().getProjector().getProjectionMethod()).getError());
        }
        repaint();
    }

    /**
     * Enable or disable buttons depending on whether the current projection
     * algorithm allows for iterations or not.
     *
     * @param b whether the current projection algorithm can be iterated
     */
    private void setToolbarIterable(final boolean b) {
        if (b) {
            playBtn.setEnabled(true);
            iterateBtn.setEnabled(true);
        } else {
            playBtn.setEnabled(false);
            iterateBtn.setEnabled(false);
        }
    }

    /**
     * Custom rendering of scatter plot points
     */
    private class CustomRenderer extends XYLineAndShapeRenderer {

        @Override
        public Paint getItemPaint(int row, int column) {
            Projector projector = getWorkspaceComponent().getProjector();
            if (column >= projector.getNumPoints()) {
                System.out.println("getItemPaint:" + column + ">" + projector.getNumPoints());
                return Color.green;
            }
            DataPointColored point = ((DataPointColored) projector.getUpstairs().getPoint(column));
            if (point != null) {
                return point.getColor();
            } else {
                return Color.green;
            }
        }

    }

    /**
     * Custom label generator that renders a point's label, if any.
     */
    public class LegendXYItemLabelGenerator extends StandardXYItemLabelGenerator
            implements XYItemLabelGenerator {

        @Override
        public String generateLabel(XYDataset dataset, int series, int item) {
            Projector projector = getWorkspaceComponent().getProjector();
            if (item >= projector.getNumPoints()) {
                System.out.println("generateLabel:" + item + ">" + projector.getNumPoints());
                return null;
            }
            DataPointColored point = ((DataPointColored) projector.getUpstairs().getPoint(item));
            if (point != null) {
                return point.getLabel();
            }
            return null;
        }
    }

    /**
     * Datapoints return a tooltip showing the high dimensional point being
     * Represented by a given point in the plot.
     */
    private class CustomToolTipGenerator extends CustomXYToolTipGenerator {
        @Override
        public String generateToolTip(XYDataset data, int series, int item) {
            Projector projector = getWorkspaceComponent().getProjector();
            if (item >= projector.getNumPoints()) {
                System.out.println("generateToolTip:" + item + ">" + projector.getNumPoints());
                return null;
            }
            DataPoint point = projector.getUpstairs().getPoint(item);
            if (point != null) {
                return Utils.doubleArrayToString(point.getVector());
            } else {
                return "null";
            }
        }
    }

    public void resetData() {
        EventQueue.invokeLater(() -> {
            xyCollection.getSeries(0).clear();
            int size = getWorkspaceComponent().getProjector().getNumPoints();
            for (int i = 0; i < size; i++) {
                DataPoint point = getWorkspaceComponent().getProjector().getDownstairs().getPoint(i);
                xyCollection.getSeries(0).add(point.get(0), point.get(1));
            }
        });

    }

}
