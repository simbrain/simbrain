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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Ellipse2D;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.simbrain.plot.ChartListener;
import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.SimbrainPreferences.PropertyNotFoundException;
import org.simbrain.util.Utils;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.projection.DataPoint;
import org.simbrain.util.projection.DataPointColored;
import org.simbrain.util.projection.IterableProjectionMethod;
import org.simbrain.util.projection.ProjectCoordinate;
import org.simbrain.util.projection.ProjectSammon;
import org.simbrain.util.projection.ProjectionMethod;
import org.simbrain.util.projection.Projector;
import org.simbrain.util.projection.ProjectorListener;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Gui Component to display a high dimensional projection object.
 */
public class ProjectionGui extends GuiComponent<ProjectionComponent> {

    /** Open button. */
    private JButton openBtn = new JButton(
            ResourceManager.getImageIcon("Open.png"));

    /** Save button. */
    private JButton saveBtn = new JButton(
            ResourceManager.getImageIcon("Save.png"));

    /** Iterate once. */
    protected JButton iterateBtn = new JButton(
            ResourceManager.getImageIcon("Step.png"));

    /** Play button. */
    private JButton playBtn = new JButton(
            ResourceManager.getImageIcon("Play.png"));

    /** Preferences button. */
    private JButton prefsBtn = new JButton(
            ResourceManager.getImageIcon("Prefs.gif"));

    /** Clear button. */
    private JButton clearBtn = new JButton(
            ResourceManager.getImageIcon("Eraser.png"));

    /** Random button. */
    private JButton randomBtn = new JButton(
            ResourceManager.getImageIcon("Rand.png"));

    /** List of projector types. */
    private JComboBox<String> projectionList = new JComboBox<String>();

    /** Bottom panel. */
    private Box bottomPanel = Box.createVerticalBox();

    /** Toolbar for bottom panel. */
    private JToolBar theToolBar = new JToolBar();

    /** Status toolbar. */
    private JToolBar statusBar = new JToolBar();

    /** Error bar. */
    private JToolBar errorBar = new JToolBar();

    /** Points indicator. */
    private JLabel pointsLabel = new JLabel();

    /** Dimension indicator. */
    private JLabel dimsLabel = new JLabel(" Dimensions:");

    /** Error indicator. */
    private JLabel errorLabel = new JLabel();

    /** Show error option. */
    private boolean showError = true;

    /** Help action used in menu and button. */
    private ShowHelpAction helpAction = new ShowHelpAction(
            "Pages/Plot/projection.html");

    /** Warning label. */
    private JLabel warningLabel = new JLabel(
            ResourceManager.getImageIcon("Warning.png"));

    /** Panel for showing Sammon step size and label, both with tooltip. */
    private Box sammonStepSizePanel = Box.createHorizontalBox();

    /** Shows the step size for Sammon map. */
    private JTextField sammonStepSize;

    /** Combo box for first dimension of coordinate projection. */
    private JComboBox<Integer> adjustDimension1 = new JComboBox<Integer>();

    /** Model for adjustDimension1. */
    private DefaultComboBoxModel<Integer> adjustDimension1Model = new DefaultComboBoxModel<Integer>();

    /** Combo box for first dimension of coordinate projection. */
    private JComboBox<Integer> adjustDimension2 = new JComboBox<Integer>();

    /** Model for adjustDimension2. */
    private DefaultComboBoxModel<Integer> adjustDimension2Model = new DefaultComboBoxModel<Integer>();

    /** Plot Action Manager. */
    private PlotActionManager actionManager;

    /** The JFreeChart chart. */
    private JFreeChart chart;

    /** The JFreeChart panel specialized for displaying JFreeCharts. */
    private ChartPanel panel;

    /**
     * Custom rendering of scatter plot points
     */
    private class CustomRenderer extends XYLineAndShapeRenderer {

        @Override
        public Paint getItemPaint(int row, int column) {
            Projector projector = getWorkspaceComponent().getProjectionModel()
                    .getProjector();
            DataPointColored point = ((DataPointColored) projector
                    .getUpstairs().getPoint(column));
            if (point != null) {
                return point.getColor();
            } else {
                return Color.green;
            }
        }

    }

    /**
     * Datapoints return a tooltip showing the high dimensional point being
     * Represented by a given point in the plot.
     */
    private class CustomToolTipGenerator extends CustomXYToolTipGenerator {
        @Override
        public String generateToolTip(XYDataset data, int series, int item) {
            DataPoint point = getWorkspaceComponent()
                    .getProjector().getUpstairs().getPoint(item);
            if (point != null) {
                return Utils.doubleArrayToString(point.getVector());
            } else {
                return "null";
            }
        }
    }

    /**
     * Construct the Projection GUI.
     * @param frame
     * @param component
     */
    public ProjectionGui(final GenericFrame frame,
            final ProjectionComponent component) {
        super(frame, component);
        setPreferredSize(new Dimension(500, 400));
        actionManager = new PlotActionManager(this);
        setLayout(new BorderLayout());

        // Generate the graph
        chart = ChartFactory.createScatterPlot("High Dimensional Projection",
                "Projection X", "Projection Y", getWorkspaceComponent()
                        .getProjectionModel().getDataset(),
                PlotOrientation.VERTICAL, false, true, false);
        // chart.getXYPlot().getDomainAxis().setRange(-100, 100);
        // chart.getXYPlot().getRangeAxis().setRange(-100, 100);
        chart.getXYPlot().setBackgroundPaint(Color.white);
        chart.getXYPlot().setDomainGridlinePaint(Color.gray);
        chart.getXYPlot().setRangeGridlinePaint(Color.gray);
        chart.getXYPlot().getDomainAxis().setAutoRange(true);
        chart.getXYPlot().getRangeAxis().setAutoRange(true);
        panel = new ChartPanel(chart);

        // Custom render points as dots (not squares) and use custom tooltips
        // that show high-d point
        CustomRenderer renderer = new CustomRenderer();
        chart.getXYPlot().setRenderer(renderer);
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-5, -5, 5, 5));
        CustomToolTipGenerator generator = new CustomToolTipGenerator();
        renderer.setSeriesToolTipGenerator(0, generator);

        // Toolbar
        openBtn.setToolTipText("Open high-dimensional data");
        saveBtn.setToolTipText("Save data");
        projectionList.setMaximumSize(new java.awt.Dimension(200, 100));
        iterateBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getWorkspaceComponent().getProjector().iterate();
                update();
            }
        });
        clearBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getWorkspaceComponent().getWorkspace().stop();
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        getWorkspaceComponent().clearData();
                    }
                });
            }
        });
        playBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (getWorkspaceComponent().getProjectionModel().isRunning()) {
                    playBtn.setIcon(ResourceManager.getImageIcon("Stop.png"));
                    playBtn.setToolTipText("Stop iterating projection algorithm");
                    getWorkspaceComponent().getProjectionModel().setRunning(
                            false);
                    Executors.newSingleThreadExecutor().execute(
                            new ProjectionUpdater(getWorkspaceComponent()));
                } else {
                    playBtn.setIcon(ResourceManager.getImageIcon("Play.png"));
                    playBtn.setToolTipText("Start iterating projection algorithm");
                    getWorkspaceComponent().getProjectionModel().setRunning(
                            true);
                }
            }
        });
        prefsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO (Still working out overall dialog structure).
            }
        });
        randomBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getWorkspaceComponent().getProjector().randomize(100);
            }
        });
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
        warningLabel.setToolTipText("This method works best with more "
                + "datapoints already added");
        theToolBar.add(warningLabel);
        String stepSizeToolTip = "Scales the amount points are moved on each iteration";
        JLabel stepSizeLabel = new JLabel("Step Size");
        stepSizeLabel.setToolTipText(stepSizeToolTip);
        sammonStepSizePanel.add(stepSizeLabel);
        try {
            sammonStepSize = new JTextField(""
                    + SimbrainPreferences.getDouble("projectorSammonEpsilon"));
        } catch (PropertyNotFoundException e1) {
            e1.printStackTrace();
        }
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
        helpButton.setAction(helpAction);

        // Setup Menu Bar
        createAttachMenuBar();

        // Status Bar
        statusBar.add(pointsLabel);
        statusBar.add(dimsLabel);
        errorBar.add(errorLabel);

        // Bottom panel
        JPanel southPanel = new JPanel();
        southPanel.add(errorBar);
        southPanel.add(statusBar);
        bottomPanel.add("South", southPanel);

        // Put all panels together
        add("North", theToolBar);
        add("Center", panel);
        add("South", bottomPanel);

        // Other initialization
        initializeComboBoxes();
        addListeners();
        updateToolBar();
        update();

    }

    /**
     * Initialize all the combo boxes.
     */
    private void initializeComboBoxes() {

        // Populate projection list combo box
        for (Entry<Class<?>, String> projMethod : getWorkspaceComponent()
                .getProjector().getProjectionMethods().entrySet()) {
            projectionList.addItem(projMethod.getValue());
        }
        projectionList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedMethod = (String) projectionList
                        .getSelectedItem();
                getWorkspaceComponent().getProjector().setProjectionMethod(
                        selectedMethod);
                updateToolBar();
            }
        });
        projectionList.getModel()
                .setSelectedItem(
                        getWorkspaceComponent().getProjector()
                                .getCurrentMethodString());

        // Init the adjust dimension combo boxes
        updateCoordinateProjectionComboBoxes();
        adjustDimension1.setModel(adjustDimension1Model);
        adjustDimension1.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ProjectionMethod proj = getWorkspaceComponent().getProjector()
                        .getProjectionMethod();
                if (proj != null) {
                    if (proj instanceof ProjectCoordinate) {
                        ((ProjectCoordinate) proj).setHiD1(adjustDimension1
                                .getSelectedIndex());
                        ((ProjectCoordinate) proj).project();
                        getWorkspaceComponent().getProjector()
                                .fireProjectorDataChanged();
                    }
                }
            }

        });
        adjustDimension2.setModel(adjustDimension2Model);
        adjustDimension2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ProjectionMethod proj = getWorkspaceComponent().getProjector()
                        .getProjectionMethod();
                if (proj != null) {
                    if (proj instanceof ProjectCoordinate) {
                        ((ProjectCoordinate) proj).setHiD2(adjustDimension2
                                .getSelectedIndex());
                        ((ProjectCoordinate) proj).project();
                        getWorkspaceComponent().getProjector()
                                .fireProjectorDataChanged();
                    }
                }
            }

        });
    }

    /**
     * Update the toolbar based on current projection method.
     */
    private void updateToolBar() {
        ProjectionMethod proj = getWorkspaceComponent().getProjector()
                .getProjectionMethod();
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
            adjustDimension1.setSelectedIndex(((ProjectCoordinate) proj)
                    .getHiD1());
            adjustDimension2.setSelectedIndex(((ProjectCoordinate) proj)
                    .getHiD2());
        }

        // Handle error bar
        if ((proj.isIterable()) && (showError)) {
            errorBar.setVisible(true);
        } else {
            errorBar.setVisible(false);
        }

        // Handle warning
        if (getWorkspaceComponent().getProjector().getNumPoints() < proj
                .suggestedMinPoints()) {
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
     * Add listeners. The chart listener mainly concerns workspace and gui level
     * stuff. The projector listener concerns the underlying projection model.
     */
    private void addListeners() {
        getWorkspaceComponent().getProjectionModel().addListener(
                new ChartListener() {

                    /**
                     * Update bottom stats when a data source is added.
                     */
                    public void dataSourceAdded(int index) {
                        update();
                        updateCoordinateProjectionComboBoxes();
                    }

                    /**
                     * Update bottom stats when a data source is removed
                     */
                    public void dataSourceRemoved(int index) {
                        update();
                        updateCoordinateProjectionComboBoxes();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void chartInitialized(int numSources) {
                        update();
                    }

                });

        // Listen to events from the underlying projector model.
        // Currently the main action is to just update the labels at the bottom.
        getWorkspaceComponent().getProjectionModel().getProjector()
                .addListener(new ProjectorListener() {

                    @Override
                    public void projectionMethodChanged() {
                        // System.out.println("ProjectionGui: In method changed");
                        update();
                    }

                    @Override
                    public void projectorDataChanged() {
                        // System.out.println("ProjectionGui: In data changed");
                        update();
                    }

                    @Override
                    public void datapointAdded() {
                        // System.out.println("ProjectionGui: In data added");
                    }

                    @Override
                    public void projectorColorsChanged() {
                        // System.out.println("ProjectionGui: In colors changed");
                        getWorkspaceComponent().getProjectionModel()
                                .getProjector().resetColors();
                        update();
                    }

                });

        // Epsilon field should update the model whenever a user clicks out of
        // it.
        sammonStepSize.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                ProjectionMethod proj = getWorkspaceComponent().getProjector()
                        .getProjectionMethod();
                if (proj != null) {
                    if (proj instanceof ProjectSammon) {
                        ((ProjectSammon) proj).setEpsilon(Double
                                .parseDouble(sammonStepSize.getText()));
                    }
                }
            }
        });

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
     * Initializes frame.
     */
    @Override
    public void postAddInit() {
    }

    /**
     * Creates the menu bar.
     */
    private void createAttachMenuBar() {

        final JMenuBar bar = new JMenuBar();
        final JMenu fileMenu = new JMenu("File");

        for (Action action : actionManager.getOpenSavePlotActions()) {
            fileMenu.add(action);
        }
        fileMenu.addSeparator();
        final JMenu exportImport = new JMenu("Export/Import...");
        fileMenu.add(exportImport);
        exportImport.add(ProjectionPlotActions
                .getImportData(getWorkspaceComponent().getProjectionModel()));
        exportImport.addSeparator();
        exportImport.add(ProjectionPlotActions
                .getExportDataHi(getWorkspaceComponent().getProjectionModel()));
        exportImport
                .add(ProjectionPlotActions
                        .getExportDataLow(getWorkspaceComponent()
                                .getProjectionModel()));
        fileMenu.addSeparator();
        fileMenu.add(new CloseAction(this.getWorkspaceComponent()));

        final JMenu editMenu = new JMenu("Edit");
        final JMenuItem preferencesGeneral = new JMenuItem(
                "Preferences...");
        preferencesGeneral.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ProjectionPreferencesDialog dialog = new ProjectionPreferencesDialog(
                        getWorkspaceComponent().getProjectionModel()
                                .getProjector());
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }

        });
        editMenu.add(preferencesGeneral);

        final JMenuItem setDimensions = new JMenuItem(
                "Set Dimensions...");
        setDimensions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String dimsString = JOptionPane.showInputDialog("Dimensions:",
                        getWorkspaceComponent().getProjectionModel()
                        .getProjector().getDimensions());
                int dims = Integer.parseInt(dimsString); //todo; Catch exception
                getWorkspaceComponent().getProjectionModel().init(dims);
                getWorkspaceComponent().initializeConsumers();
            }

        });
        editMenu.add(setDimensions);

        final JMenuItem colorPrefs = new JMenuItem("Datapoint Coloring...");
        colorPrefs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                DataPointColoringDialog dialog = new DataPointColoringDialog(
                        getWorkspaceComponent().getProjectionModel());
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);

            }

        });
        editMenu.add(colorPrefs);

        final JMenuItem dims = new JMenuItem("Set dimensions...");
        dims.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String dimensions = JOptionPane.showInputDialog("Dimensions:");
                if (dimensions != null) {
                    getWorkspaceComponent().getProjectionModel().getProjector()
                            .init(Integer.parseInt(dimensions));
                }

            }

        });
        // editMenu.add(dims);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem(helpAction);
        helpMenu.add(helpItem);

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(helpMenu);

        getParentFrame().setJMenuBar(bar);
    }

    @Override
    public void closing() {
    }

    /**
     * Update labels at bottom of component.
     */
    @Override
    protected void update() {
        super.update();
        chart.fireChartChanged();
        updateToolBar();
        dimsLabel.setText("     Dimensions: "
                + getWorkspaceComponent().getProjector().getUpstairs()
                        .getDimensions());
        pointsLabel.setText("  Datapoints: "
                + getWorkspaceComponent().getProjector().getDownstairs()
                        .getNumPoints());
        if (getWorkspaceComponent().getProjector().getProjectionMethod()
                .isIterable()) {
            errorLabel.setText(" Error:"
                    + ((IterableProjectionMethod) getWorkspaceComponent()
                            .getProjector().getProjectionMethod()).getError());
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
}
