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
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.Executors;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;
import org.simbrain.plot.ChartListener;
import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.projection.ProjectionMethod;
import org.simbrain.util.projection.Projector;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Display a Scatter Plot.
 */
public class ProjectionGui extends GuiComponent<ProjectionComponent> implements ActionListener {
    
    /** Projector on/off checkbox. */
    private JCheckBox onOffBox = new JCheckBox(ResourceManager.getImageIcon("GaugeOn.png"));

    /** Open button. */
    private JButton openBtn = new JButton(ResourceManager.getImageIcon("Open.png"));

    /** Save button. */
    private JButton saveBtn = new JButton(ResourceManager.getImageIcon("Save.png"));

    /** Iterate once. */
    protected JButton iterateBtn = new JButton(ResourceManager.getImageIcon("Step.png"));

    /** Play button. */
    private JButton playBtn = new JButton(ResourceManager.getImageIcon("Play.png"));

    /** Preferences button. */
    private JButton prefsBtn = new JButton(ResourceManager.getImageIcon("Prefs.gif"));

    /** Clear button. */
    private JButton clearBtn = new JButton(ResourceManager.getImageIcon("Eraser.png"));

    /** Random button. */
    private JButton randomBtn = new JButton(ResourceManager.getImageIcon("Rand.png"));

    /** List of projector types. */
    private JComboBox projectionList = new JComboBox(Projector.getProjectorList());

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
 
    /** Plot Action Manager. */
    private PlotActionManager actionManager;
    
    /** The JFreeChart chart. */
    private JFreeChart chart;
    
    /** The dot renderer. */
    private XYDotRenderer renderer;

    /** The JFreeChart panel specialized for displaying JFreeCharts. */
    private ChartPanel panel;
    
    /**
     * Subclass of dot renderer which colors the "current" data item differently.
     */
    private class CustomRenderer extends XYDotRenderer {

		/* (non-Javadoc)
		 * @see org.jfree.chart.renderer.xy.XYDotRenderer#drawItem(java.awt.Graphics2D, org.jfree.chart.renderer.xy.XYItemRendererState, java.awt.geom.Rectangle2D, org.jfree.chart.plot.PlotRenderingInfo, org.jfree.chart.plot.XYPlot, org.jfree.chart.axis.ValueAxis, org.jfree.chart.axis.ValueAxis, org.jfree.data.xy.XYDataset, int, int, org.jfree.chart.plot.CrosshairState, int)
		 */
		@Override
		public void drawItem(Graphics2D graphics, XYItemRendererState arg1,
				Rectangle2D arg2, PlotRenderingInfo arg3, XYPlot arg4,
				ValueAxis arg5, ValueAxis arg6, XYDataset arg7, int series,
				int item, CrosshairState state, int pass) {

			//System.out.println("item = " + item + "   currentIndex = " + getWorkspaceComponent().getProjectionModel().currentItemIndex);

			/**
			 * TODO: 
			 * 	(1) Use non-deprecated methods
			 * 	(2) Make it possible to turn this off
			 *  (3) Optimize (do all these setPaints slow things down?) 
			 */
			
			if (item == getWorkspaceComponent().getProjectionModel().getCurrentItemIndex() - 1) {
				this.setPaint(Color.red, false);
			} else {
				this.setPaint(Color.green.brighter(), false);
			}
			super.drawItem(graphics, arg1, arg2, arg3, arg4, arg5, arg6, arg7, series, item,
					state, pass);
		}
    	
    }

    /**
     * Construct the ScatterPlot.
     */
    public ProjectionGui (final GenericFrame frame, final ProjectionComponent component) {
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
        renderer = new CustomRenderer();
        renderer.setDotWidth(5);
        renderer.setDotHeight(5);

        // Tooltip implementation that needs work...
        //
        //        renderer = new XYLineAndShapeRenderer(false, true);
        //        renderer.setBaseToolTipGenerator(new XYToolTipGenerator() {
        //            public String generateToolTip(XYDataset dataset, int series, int item) {
        //                return org.simbrain.util.Utils.doubleArrayToString(getWorkspaceComponent().getGauge().getUpstairs().getPoint(item));
        //             }
        //        });

        chart.getXYPlot().setRenderer(renderer);
        panel = new ChartPanel(chart);
        
        // Toolbar
        onOffBox.setToolTipText("Turn gauge on or off");
        openBtn.setToolTipText("Open high-dimensional data");
        saveBtn.setToolTipText("Save data");
        playBtn.setToolTipText("Iterate projection algorithm");
        iterateBtn.setToolTipText("Step projection algorithm");
        clearBtn.setToolTipText("Clear current data");
        projectionList.setMaximumSize(new java.awt.Dimension(200, 100));
        projectionList.addActionListener(this);
        projectionList.setSelectedIndex(1); // TODO: Hack!
        onOffBox.addActionListener(this);
        openBtn.addActionListener(this);
        saveBtn.addActionListener(this);
        iterateBtn.addActionListener(this);
        clearBtn.addActionListener(this);
        playBtn.addActionListener(this);
        prefsBtn.addActionListener(this);
        randomBtn.addActionListener(this);
        //theToolBar.add(onOffBox);
        theToolBar.add(projectionList);
        theToolBar.add(playBtn);
        theToolBar.add(iterateBtn);
        theToolBar.add(clearBtn);
        theToolBar.add(randomBtn);

        // Add/Delete Buttons
        JButton deleteButton = new JButton("Delete");
        deleteButton.setActionCommand("Delete");
        deleteButton.addActionListener(this);
        JButton addButton = new JButton("Add");
        addButton.setActionCommand("Add");
        addButton.addActionListener(this);
        
        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(deleteButton);
        buttonPanel.add(addButton);

        // Setup Menu Bar
        createAttachMenuBar();
        
        // Status Bar
        statusBar.add(pointsLabel);
        statusBar.add(dimsLabel);
        errorBar.add(errorLabel);

        // Bottom panel
        bottomPanel.add("North", buttonPanel);
        JPanel southPanel = new JPanel();
        southPanel.add(errorBar);
        southPanel.add(statusBar);
        bottomPanel.add("South", southPanel);
        
        // Put all panels together
        add("North", theToolBar);
        add("Center", panel);
        add("South", bottomPanel);
        
        //Add listener
        getWorkspaceComponent().getProjectionModel().addListener(new ChartListener() {

        	/**
        	 * Update bottom stats when a data source is added.
        	 */
			public void dataSourceAdded(int index) {
				update();
			}

        	/**
        	 * Update bottom stats when a data source is removed
        	 */
			public void dataSourceRemoved(int index) {
				update();
			}
        	
        });
        
        // Initializes labels
        update();
    }
    
    /**
     * Initializes frame.
     */
    @Override
    public void postAddInit() {
    }

    /**
     * Creates the menu bar.
     * @return menu bar
     */
    private void createAttachMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        for (Action action : actionManager.getOpenSavePlotActions()) {
            fileMenu.add(action);
        }

        JMenu editMenu = new JMenu("Edit");
        JMenuItem preferences = new JMenuItem("Preferences...");
        preferences.addActionListener(this);
        preferences.setActionCommand("dialog");
        editMenu.add(preferences);
        bar.add(fileMenu);
        bar.add(editMenu);
        getParentFrame().setJMenuBar(bar);
    }

    @Override
    public void closing() {
    }

    /* (non-Javadoc)
     * @see org.simbrain.workspace.gui.GuiComponent#update()
     */
    @Override
    protected void update() {
        super.update();
    	chart.fireChartChanged();
        // Update labels, etc.
        dimsLabel.setText("     Dimensions: " + getWorkspaceComponent().getGauge().getUpstairs().getDimensions());
        pointsLabel.setText("  Datapoints: " + getWorkspaceComponent().getGauge().getDownstairs().getNumPoints());
        if (getWorkspaceComponent().getGauge().getCurrentProjectionMethod().isIterable()) {
            errorLabel.setText(" Error:" + getWorkspaceComponent().getGauge().getError());
        }
    }
  
    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        Object e1 = e.getSource();

        // Handle drop down list; Change current projection algorithm
        if (e1 instanceof JComboBox) {
            String selectedGauge = ((JComboBox) e1).getSelectedItem().toString();
            getWorkspaceComponent().getGauge().setCurrentProjectionMethod(selectedGauge);
            getWorkspaceComponent().changeProjection();
            
            ProjectionMethod proj = getWorkspaceComponent().getGauge().getCurrentProjectionMethod();
            if (proj == null) {
                return;
            }
            if ((proj.isIterable()) && (showError)) {
                errorBar.setVisible(true);
            } else {
                errorBar.setVisible(false);
            }
            proj.checkDatasets();
            setToolbarIterable(proj.isIterable());
        }

        // Handle Button Presses
        if (e1 instanceof JButton) {
            JButton btemp = (JButton) e.getSource();

            if (btemp == iterateBtn) {
                getWorkspaceComponent().getGauge().iterate(1);
                getWorkspaceComponent().resetChartDataset();
                update();
            } else if (btemp == clearBtn) {
                getWorkspaceComponent().clearData();
            } else if (btemp == playBtn) {
                if (getWorkspaceComponent().getProjectionModel().isRunning()) {
                    playBtn.setIcon(ResourceManager.getImageIcon("Stop.png"));
                    playBtn.setToolTipText("Stop iterating projection algorithm");
                    getWorkspaceComponent().getProjectionModel().setRunning(false);
                    Executors.newSingleThreadExecutor().execute(new ProjectionUpdater(getWorkspaceComponent()));                    
                } else {
                    playBtn.setIcon(ResourceManager.getImageIcon("Play.png"));
                    playBtn.setToolTipText("Start iterating projection algorithm");
                    getWorkspaceComponent().getProjectionModel().setRunning(true);
                }
            } else if (btemp == randomBtn) {
               getWorkspaceComponent().getGauge().getDownstairs().randomize(100);
               getWorkspaceComponent().resetChartDataset();
            }
        }

        if (e.getActionCommand().equalsIgnoreCase("Add")) {
            getWorkspaceComponent().getProjectionModel().addSource();
        }
        if (e.getActionCommand().equalsIgnoreCase("Delete")) {
            getWorkspaceComponent().getProjectionModel().removeSource();
        }
               
   }
    
    /**
     * Enable or disable buttons depending on whether the current projection algorithm allows for iterations or not.
     *
     * @param b whether the current projection algorithm can be iterated or not
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
