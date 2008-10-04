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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.Projector;
import org.simbrain.gauge.graphics.GaugeThread;
import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Display a Scatter Plot.
 */
public class ProjectionGui extends GuiComponent<ProjectionComponent> implements WorkspaceComponentListener, ActionListener {

    /** The underlying plot component. */
    private final ProjectionComponent component;
    
    /** Gauge on/off checkbox. */
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
    private JComboBox projectionList = new JComboBox(Gauge.getProjectorList());

    /** Bottom panel. */
    private JPanel bottomPanel = new JPanel();

    /** Toolbar for bottom panel. */
    private JToolBar theToolBar = new JToolBar();

    /** Status toolbar. */
    private JToolBar statusBar = new JToolBar();

    /** Error bar. */
    private JToolBar errorBar = new JToolBar();

    /** Points indicator. */
    private JLabel pointsLabel = new JLabel();

    /** Dimension indicator. */
    private JLabel dimsLabel = new JLabel();

    /** Error indicator. */
    private JLabel errorLabel = new JLabel();

    /** Show error option. */
    private boolean showError = true; 

    /**
     * Construct the ScatterPlot.
     */
    public ProjectionGui (final GenericFrame frame, final ProjectionComponent component) {
        super(frame, component);
        this.component = component;
        setPreferredSize(new Dimension(500, 400));        
        component.addListener(this);
 
    }

    /**
     * Initializes frame.
     */
    @Override
    public void postAddInit() {
        setLayout(new BorderLayout());
        
        // Generate the graph
        JFreeChart chart = ChartFactory.createScatterPlot("High Dimensional Projection",
                "Projection X", "Projection Y", component.getDataset(), PlotOrientation.VERTICAL, true, false, false);
        //
        //        // Use below to make this stuff settable
        //        chart.getXYPlot().getDomainAxis().setRange(0, 100);
        //        chart.getXYPlot().getRangeAxis().setRange(0, 100);
        //        chart.getXYPlot().getDomainAxis().setAutoRange(false);
        //        chart.getXYPlot().getRangeAxis().setAutoRange(false);
        XYDotRenderer renderer = new XYDotRenderer();
        renderer.setDotWidth(3);
        renderer.setDotHeight(3);
        chart.getXYPlot().setRenderer(renderer);
        ChartPanel panel = new ChartPanel(chart);
        
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
        
        // Status Bar
        statusBar.add(pointsLabel);
        statusBar.add(dimsLabel);
        errorBar.add(errorLabel);
        
        add("North", theToolBar);
        add("Center", panel);
        JPanel southPanel = new JPanel();
        southPanel.add(errorBar);
        southPanel.add(statusBar);
        add("South", southPanel);
    }

    @Override
    public void closing() {
    }

    /**
     * {@inheritDoc}
     */
    public void componentUpdated() {
        // Update labels, etc.
        dimsLabel.setText("     Dimensions: " + component.getGauge().getUpstairs().getDimensions());
        pointsLabel.setText("  Datapoints: " + component.getGauge().getDownstairs().getNumPoints());
        if (component.getGauge().getCurrentProjector().isIterable()) {
            errorLabel.setText(" Error:" + component.getGauge().getError());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        Object e1 = e.getSource();

        // Handle drop down list; Change current projection algorithm
        if (e1 instanceof JComboBox) {
            //stopThread();
            String selectedGauge = ((JComboBox) e1).getSelectedItem().toString();
            component.getGauge().setCurrentProjector(selectedGauge);
            component.changeProjection();
            
            Projector proj = component.getGauge().getCurrentProjector();
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
//            this.updateColors(this.isColorMode());
//            updateGraphics();
        }
        // Handle Check boxes
//        if (e1 instanceof JCheckBox) {
//            if (e1 == onOffBox) {
//                if (theGauge.isOn()) {
//                    theGauge.setOn(false);
//                    onOffBox.setIcon(ResourceManager.getImageIcon("GaugeOff.png"));
//                    onOffBox.setToolTipText("Turn gauge on");
//                } else {
//                    theGauge.setOn(true);
//                    onOffBox.setIcon(ResourceManager.getImageIcon("GaugeOn.png"));
//                    onOffBox.setToolTipText("Turn gauge off");
//                }
//            }
//        }

        // Handle Button Presses
        if (e1 instanceof JButton) {
            JButton btemp = (JButton) e.getSource();

            if (btemp == iterateBtn) {
                component.getGauge().iterate(100);
                component.resetChartDataset();
                componentUpdated();
            } else if (btemp == clearBtn) {
                component.clearData();
            } else if (btemp == playBtn) {
//                if (iterateThread == null) {
//                    iterateThread = new Thread(component);
//                }
//                if (component.isSuspended()) {
//                    playBtn.setIcon(ResourceManager.getImageIcon("Stop.png"));
//                    playBtn.setToolTipText("Stop iterating projection algorithm");
//                    iterateThread.start();
//                    component.setSuspended(false);
//                } else {
//                    playBtn.setIcon(ResourceManager.getImageIcon("Play.png"));
//                    playBtn.setToolTipText("Start iterating projection algorithm");
//                    component.setSuspended(true);
//                }
            } else if (btemp == randomBtn) {
               component.getGauge().getDownstairs().randomize(100);
               component.resetChartDataset();
            }
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
