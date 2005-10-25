/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.gauge.graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.gauge.GaugePreferences;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.ProjectCoordinate;
import org.simbrain.gauge.core.ProjectPCA;
import org.simbrain.gauge.core.ProjectSammon;
import org.simbrain.gauge.core.Projector;
import org.simbrain.gauge.core.Settings;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.StandardDialog;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;


/**
 * <b>GaugePanel</b> is the main panel in which data are displayed.  Menu and toolbar handling code are also provided
 * here.
 */
public class GaugePanel extends PCanvas implements ActionListener {
    protected String name = "";

    // CHANGE HERE if adding projection algorithm
    private GaugeThread theThread;
    private File currentFile = null;
    protected static File current_file = null;
    private JCheckBox onOffBox = new JCheckBox(ResourceManager.getImageIcon("GaugeOn.gif"));
    private JButton openBtn = new JButton(ResourceManager.getImageIcon("Open.gif"));
    private JButton saveBtn = new JButton(ResourceManager.getImageIcon("Save.gif"));
    protected JButton iterateBtn = new JButton(ResourceManager.getImageIcon("Step.gif"));
    private JButton playBtn = new JButton(ResourceManager.getImageIcon("Play.gif"));
    private JButton prefsBtn = new JButton(ResourceManager.getImageIcon("Prefs.gif"));
    private JButton clearBtn = new JButton(ResourceManager.getImageIcon("Eraser.gif"));
    private JButton randomBtn = new JButton(ResourceManager.getImageIcon("Rand.gif"));
    private JComboBox projectionList = new JComboBox(Gauge.getProjectorList());
    private JPanel bottomPanel = new JPanel();
    private JToolBar theToolBar = new JToolBar();
    private JToolBar statusBar = new JToolBar();
    private JToolBar errorBar = new JToolBar();
    private JLabel pointsLabel = new JLabel();
    private JLabel dimsLabel = new JLabel();
    private JLabel errorLabel = new JLabel();
    public ArrayList node_list = new ArrayList();
    private Gauge theGauge;
    private double minx;
    private double maxx;
    private double miny;
    private double maxy;
    private KeyEventHandler keyEventHandler;
    private MouseEventHandler mouseHandler;
    private boolean autoZoom = true;
    private static final int CLEARED = -1;

    // Application parameters
    private boolean update_completed = false;
    private boolean colorMode = GaugePreferences.getColorDataPoints();
    private int numIterationsBetweenUpdate = GaugePreferences.getIterationsBetweenUpdates();
    private boolean showError = GaugePreferences.getShowError();
    private boolean showStatus = GaugePreferences.getShowStatusBar();
    private double pointSize = GaugePreferences.getPointSize();

    //Piccolo stuff
    private PCamera cam;
    private PPath pb;

    // "Hot" points 
    private int hotPoint = 0;
    public Color hotColor = new Color(GaugePreferences.getHotColor());
    public Color defaultColor = new Color(GaugePreferences.getDefaultColor());
    public Color backgroundColor = new Color(GaugePreferences.getBackgroundColor());

    public GaugePanel() {
        theGauge = new Gauge();
        init();
    }

    public void initCastor() {
        getGauge().getCurrentProjector().getUpstairs().initCastor();
        getGauge().getCurrentProjector().getDownstairs().initCastor();
        update();
        updateProjectionMenu();
    }

    public void init() {
        cam = this.getCamera();
        setLayout(new BorderLayout());
        setBackground(backgroundColor);

        onOffBox.setToolTipText("Turn gauge on or off");
        openBtn.setToolTipText("Open high-dimensional data");
        saveBtn.setToolTipText("Save data");
        playBtn.setToolTipText("Iterate projection algorithm");
        iterateBtn.setToolTipText("Step projection algorithm");
        clearBtn.setToolTipText("Clear current data");

        projectionList.setMaximumSize(new java.awt.Dimension(200, 100));

        keyEventHandler = new KeyEventHandler(this);
        addInputEventListener(keyEventHandler);
        getRoot().getDefaultInputManager().setKeyboardFocus(keyEventHandler);

        mouseHandler = new MouseEventHandler(this);
        addInputEventListener(mouseHandler);

        theToolBar.add(onOffBox);
        theToolBar.add(projectionList);
        theToolBar.add(playBtn);
        theToolBar.add(iterateBtn);
        theToolBar.add(clearBtn);
        theToolBar.add(randomBtn);

        statusBar.add(pointsLabel);
        statusBar.add(dimsLabel);
        setShowStatus(showStatus);

        errorBar.add(errorLabel);
        setShowError(showError);
        this.updateProjectionMenu();

        projectionList.addActionListener(this);

        onOffBox.addActionListener(this);
        openBtn.addActionListener(this);
        saveBtn.addActionListener(this);
        iterateBtn.addActionListener(this);
        clearBtn.addActionListener(this);
        playBtn.addActionListener(this);
        prefsBtn.addActionListener(this);
        randomBtn.addActionListener(this);

        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add("South", statusBar);
        bottomPanel.add("North", errorBar);
        add("North", theToolBar);
        add("South", bottomPanel);

        repaint();
    }

    /**
     * Open preference dialog based on which projector is currently selected
     */
    public void handlePreferenceDialogs() {
        if ((theGauge.getUpstairs() == null) || (theGauge.getDownstairs() == null)) {
            return;
        }

        if (theGauge.getCurrentProjector() instanceof ProjectSammon) {
            DialogSammon dialog = new DialogSammon(theGauge);
            showProjectorDialog((StandardDialog) dialog);
        } else if (theGauge.getCurrentProjector() instanceof ProjectCoordinate) {
            DialogCoordinate dialog = new DialogCoordinate(theGauge);
            showProjectorDialog((StandardDialog) dialog);
            theGauge.getCurrentProjector().project();
            update();
        }
    }

    /**
     * Show graphics dialog
     */
    public void handleGraphicsDialog() {
        DialogGraphics dialog = new DialogGraphics(this);
        dialog.pack();
        dialog.setVisible(true);

        if (!dialog.hasUserCancelled()) {
            dialog.commit();
            dialog.setAsDefault();
        } else {
            dialog.returnToCurrentPrefs();
        }
    }

    /**
     * Show genneral prefs dialog
     */
    public void handleGeneralDialog() {
        DialogGeneral dialog = new DialogGeneral(this);
        dialog.pack();
        dialog.setVisible(true);

        if (!dialog.hasUserCancelled()) {
            dialog.commit();
            dialog.setAsDefault();
        } else {
            dialog.returnToCurrentPrefs();
        }
    }

    /**
     * Show projector dialog
     *
     * @param dialog
     */
    private void showProjectorDialog(StandardDialog dialog) {
        dialog.pack();
        dialog.setVisible(true);

        if (!dialog.hasUserCancelled()) {
            dialog.commit();
            dialog.setAsDefault();
        } else {
            dialog.returnToCurrentPrefs();
        }
    }

    /**
     * Update node list, labels, etc.
     */
    public void update() {
        if (node_list.size() != theGauge.getDownstairs().getNumPoints()) {
            //A new node has been added
            hotPoint = CLEARED;
        }

        node_list.clear();
        this.getLayer().removeAllChildren();

        double[] tempPoint;

        for (int i = 0; i < theGauge.getDownstairs().getNumPoints(); i++) {
            tempPoint = theGauge.getDownstairs().getPoint(i);

            PNode theNode = new PNodeDatapoint(tempPoint, i, pointSize);
            node_list.add(theNode);
            this.getLayer().addChild(theNode);
        }

        dimsLabel.setText("     Dimensions: " + theGauge.getUpstairs().getDimensions());
        pointsLabel.setText("  Datapoints: " + theGauge.getDownstairs().getNumPoints());

        if (theGauge.getCurrentProjector().isIterable() == true) {
            errorLabel.setText(" Error:" + theGauge.getError());
        }

        updateColors(this.isColorMode());
        repaint();
        setUpdateCompleted(true);
    }

    /**
     * Reset the gauge, removing all PNodes and references to datapoints
     */
    public void resetGauge() {
        this.getLayer().removeAllChildren();
        node_list.clear();
        hotPoint = CLEARED;
        update();
    }

    /**
     * Manually set the currently selected projection algorithm.  Used when the projection method is changed
     * independently of the user
     */
    public void updateProjectionMenu() {
        Projector proj = theGauge.getCurrentProjector();

        if (proj instanceof ProjectCoordinate) {
            projectionList.setSelectedIndex(2);
        } else if (proj instanceof ProjectPCA) {
            projectionList.setSelectedIndex(1);
        } else if (proj instanceof ProjectSammon) {
            projectionList.setSelectedIndex(0);
        }

        setToolbarIterable(proj.isIterable());
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        Object e1 = e.getSource();

        // Handle drop down list; Change current projection algorithm
        if (e1 instanceof JComboBox) {
            stopThread();

            String selectedGauge = ((JComboBox) e1).getSelectedItem().toString();

            //The setCurrentProjector will wipe out the hot-point, so store it and reset it after the init
            int temp_hot_point = hotPoint;
            theGauge.setCurrentProjector(selectedGauge);

            Projector proj = theGauge.getCurrentProjector();

            if (proj == null) {
                return;
            }

            if ((proj.isIterable() == true) && (showError == true)) {
                errorBar.setVisible(true);
            } else {
                errorBar.setVisible(false);
            }

            proj.checkDatasets();
            setToolbarIterable(proj.isIterable());
            this.updateColors(this.isColorMode());
            update();
            setHotPoint(temp_hot_point);
        }

        // Handle Check boxes
        if (e1 instanceof JCheckBox) {
            if (e1 == onOffBox) {
                if (theGauge.isOn() == true) {
                    theGauge.setOn(false);
                    onOffBox.setIcon(ResourceManager.getImageIcon("GaugeOff.gif"));
                    onOffBox.setToolTipText("Turn gauge on");
                } else {
                    theGauge.setOn(true);
                    onOffBox.setIcon(ResourceManager.getImageIcon("GaugeOn.gif"));
                    onOffBox.setToolTipText("Turn gauge off");
                }
            }
        }

        // Handle Button Presses 
        if (e1 instanceof JButton) {
            JButton btemp = (JButton) e.getSource();

            if (btemp == iterateBtn) {
                iterate();
                update();
            } else if (btemp == clearBtn) {
                theGauge.init(theGauge.getUpstairs().getDimensions());
                resetGauge();
            } else if (btemp == playBtn) {
                if (theThread == null) {
                    theThread = new GaugeThread(this);
                }

                if (theThread.isRunning() == false) {
                    startThread();
                } else {
                    stopThread();
                }
            } else if (btemp == randomBtn) {
                theGauge.getDownstairs().randomize(100);
                update();
            } else if (btemp == prefsBtn) {
            }
        }
    }

    //////////////////////////
    // THREAD METHODS		//
    //////////////////////////
    private void stopThread() {
        playBtn.setIcon(ResourceManager.getImageIcon("Play.gif"));
        playBtn.setToolTipText("Start iterating projection algorithm");

        if (theThread == null) {
            return;
        }

        theThread.setRunning(false);
        theThread = null;
    }

    private void startThread() {
        if (theThread == null) {
            theThread = new GaugeThread(this);
        }

        playBtn.setIcon(ResourceManager.getImageIcon("Stop.gif"));
        playBtn.setToolTipText("Stop iterating projection algorithm");
        theThread.setRunning(true);
        theThread.start();
    }

    /**
     * Forward command to gauge; iterate the gauge one time
     */
    public void iterate() {
        theGauge.iterate(numIterationsBetweenUpdate);
    }

    //////////////////////////
    // GRAPHICS	 METHODS	//
    //////////////////////////

    /**
     * Scale the data so that it fits on the screen Assumes 2-d data.
     */
    public void centerCamera() {
        PCamera cam = this.getCamera();
        PBounds pb = getLayer().getGlobalFullBounds();
        pb = new PBounds(pb.x - 5, pb.y - 5, pb.width + 10, pb.height + 10);
        cam.animateViewToCenterBounds(pb, true, 0);
    }

    /**
     * Color every seventh point a different color; allows tracking of order
     */
    public void colorPoints() {
        // Use different colors for the points
        if (node_list.size() == 0) {
            return;
        }

        for (int i = 0; i < node_list.size(); i++) {
            PNodeDatapoint pn = (PNodeDatapoint) node_list.get(i);

            if ((i % 7) == 0) {
                pn.setColor(java.awt.Color.red);
            }

            if ((i % 7) == 1) {
                pn.setColor(java.awt.Color.orange);
            }

            if ((i % 7) == 2) {
                pn.setColor(java.awt.Color.yellow);
            }

            if ((i % 7) == 3) {
                pn.setColor(java.awt.Color.green);
            }

            if ((i % 7) == 4) {
                pn.setColor(java.awt.Color.cyan);
            }

            if ((i % 7) == 5) {
                pn.setColor(java.awt.Color.blue);
            }

            if ((i % 7) == 6) {
                pn.setColor(java.awt.Color.magenta);
            }
        }
    }

    /**
     * Color all datapoints a specified color
     *
     * @param c new color
     */
    public void setColor(Color c) {
        if (node_list.size() == 0) {
            return;
        }

        for (int i = 0; i < node_list.size(); i++) {
            PNodeDatapoint pn = (PNodeDatapoint) node_list.get(i);
            pn.setColor(c);
        }
    }

    /**
     * Set a unique datapoint to "hot" mode, which just means it is shown in a different color, to indicate (e.g.) that
     * it is the current point in a set of points.
     *
     * @param i index of datapoint to designate as "hot"
     */
    public void setHotPoint(int i) {
        if (i == CLEARED) {
            return;
        }

        if (i >= node_list.size()) {
            System.err.println("ERROR (setHotPoint): the designated point (" + i
                               + ") is outside the dataset bounds (dataset size = " + node_list.size() + ")");

            return;
        }

        if (hotPoint >= node_list.size()) {
            System.err.println("ERROR (setHotPoint): the designated hot-point (" + hotPoint
                               + ") is outside the dataset bounds (dataset size = " + node_list.size() + ")");

            return;
        }

        //New hot point to hot color
        hotPoint = i;
        ((PNodeDatapoint) node_list.get(hotPoint)).setColor(hotColor);
        ((PNode) node_list.get(hotPoint)).moveToFront();
    }

    /**
     * @return "current" point in use by another component.
     */
    public int getHotPoint() {
        return hotPoint;
    }

    public void repaint() {
        super.repaint();

        if (autoZoom == true) {
            centerCamera();
        }
    }

    /**
     * @return a reference to the gauge
     */
    public Gauge getGauge() {
        return theGauge;
    }

    public void setGauge(Gauge gauge) {
        theGauge = gauge;
    }

    /**
     * Used by the thread to be sure an iteration is complete before it iterates again
     *
     * @return true if update is completed, false otherwise
     */
    public boolean isUpdateCompleted() {
        return update_completed;
    }

    /**
     * Used by the thread to be sure an iteration is complete before it iterates again
     *
     * @param b true if update is completed, false otherwise
     */
    public void setUpdateCompleted(boolean b) {
        update_completed = b;
    }

    /**
     * Enable or disable buttons depending on whether the current projection algorithm allows for iterations or not
     *
     * @param b whether the current projection algorithm can be iterated or not
     */
    private void setToolbarIterable(boolean b) {
        if (b == true) {
            playBtn.setEnabled(true);
            iterateBtn.setEnabled(true);
        } else {
            playBtn.setEnabled(false);
            iterateBtn.setEnabled(false);
        }
    }

    /**
     * @return true if the gauge is in color mode (colors the datapoints), false otehrwise
     */
    public boolean isColorMode() {
        return colorMode;
    }

    /**
     * @param b true if the gauge is in color mode (colors the datapoints), false otehrwise
     */
    public void updateColors(boolean b) {
        colorMode = b;

        if (colorMode == true) {
            colorPoints();
        } else {
            setColor(defaultColor);
        }
    }

    /**
     * @return number of iterations the projection algorithm takes between graphics updates
     */
    public int getNumIterationsBetweenUpdate() {
        return numIterationsBetweenUpdate;
    }

    /**
     * @param i number of iterations the projection algorithm takes between graphics updates
     */
    public void setNumIterationsBetweenUpdate(int i) {
        numIterationsBetweenUpdate = i;
    }

    /**
     * Used to programatically set the projector
     */
    public void setProjector(String projector) {
    }

    /**
     * @return true if error information should be shown, false otherwise
     */
    public boolean isShowError() {
        return showError;
    }

    /**
     * @return true if status information (dimensions and number of datapoints) should be shown, false otherwise
     */
    public boolean isShowStatus() {
        return showStatus;
    }

    /**
     * @param b true if status information (dimensions and number of datapoints) should be shown, false otherwise
     */
    public void setShowStatus(boolean b) {
        statusBar.setVisible(b);
        showStatus = b;
    }

    /**
     * @param b true if error information should be shown, false otherwise
     */
    public void setShowError(boolean b) {
        errorBar.setVisible(b);
        showError = b;
    }

    /**
     * @return the minimum size which all datapoints must be
     */
    public double getPointSize() {
        return pointSize;
    }

    /**
     * @param d the minimum size which all datapoints must be
     */
    public void setPointSize(double d) {
        pointSize = d;
    }

    /**
     * You can turn autozoom off if you want to zoom in on data.
     *
     * @return true if autozoom (which automatically scales the dataset to the screen) is on, false otherwise.
     */
    public boolean isAutoZoom() {
        return autoZoom;
    }

    /**
     * @param b true if autozoom (which automatically scales the dataset to the screen) is on, false otherwise.
     */
    public void setAutoZoom(boolean b) {
        autoZoom = b;
    }

    public JCheckBox getOnOffBox() {
        return onOffBox;
    }

    /**
     * @return Returns the currentFile.
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @param currentFile The currentFile to set.
     */
    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }

    /**
     * @return Returns the defaultColor.
     */
    public int getDefaultColor() {
        return defaultColor.getRGB();
    }

    /**
     * @param defaultColor The defaultColor to set.
     */
    public void setDefaultColor(int rgb) {
        defaultColor = new Color(rgb);
        repaint();
    }

    /**
     * @return Returns the hotColor.
     */
    public int getHotColor() {
        return hotColor.getRGB();
    }

    /**
     * @param hotColor The hotColor to set.
     */
    public void setHotColor(int rgb) {
        hotColor = new Color(rgb);
        repaint();
    }

    public void setBackgroundColor(int rgb) {
        backgroundColor = new Color(rgb);
        this.setBackground(backgroundColor);
        repaint();
    }

    public int getBackgroundColor() {
        return backgroundColor.getRGB();
    }

    public Settings getSettings() {
        return theGauge.getCurrentProjector().getTheSettings();
    }
}
