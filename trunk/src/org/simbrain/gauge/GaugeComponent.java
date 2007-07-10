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
package org.simbrain.gauge;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.gauge.core.Dataset;
import org.simbrain.gauge.graphics.GaugePanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.CouplingContainer;
import org.simbrain.workspace.CouplingMenuItem;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * <b>GaugeComponent</b> wraps a Gauge object in a Simbrain workspace frame, which also stores information about the
 * variables the Gauge is representing.
 */
public class GaugeComponent extends WorkspaceComponent implements ActionListener, MenuListener {

    /** Current workspace. */
    private Workspace workspace;

    /** Gauge panel. */
    private GaugePanel gaugePanel;

    /** Menu bar. */
    private JMenuBar mb = new JMenuBar();

    /** File menu. */
    private JMenu fileMenu = new JMenu("File  ");

    /** Open menu item. */
    private JMenuItem open = new JMenuItem("Open");

    /** Save menu item. */
    private JMenuItem save = new JMenuItem("Save");

    /** Save as menu item. */
    private JMenuItem saveAs = new JMenuItem("Save As");

    /** Import/export menu. */
    private JMenu fileOpsMenu = new JMenu("Import / Export");

    /** Import CSV file. */
    private JMenuItem importCSV = new JMenuItem("Import CSV");

    /** Export low dimensional CSV. */
    private JMenuItem exportLow = new JMenuItem("Export Low-Dimensional CSV");

    /** Export hi dimensional CSV. */
    private JMenuItem exportHigh = new JMenuItem("Export High-Dimensional CSV");

    /** Close gauge menu item. */
    private JMenuItem close = new JMenuItem("Close");

    /** Preferences menu. */
    private JMenu prefsMenu = new JMenu("Preferences");

    /** Projection preferences menu item. */
    private JMenuItem projectionPrefs = new JMenuItem("Projection Preferences");

    /** Coupling menu item. Must be reset every time.  */
    JMenuItem producerListItem;

    /** Graphics preferences menu item. */
    private JMenuItem graphicsPrefs = new JMenuItem("Graphics /GUI Preferences");

    /** General preferences menu item. */
    private JMenuItem generalPrefs = new JMenuItem("General Preferences");

    /** Set gauge auto zoom menu item. */
    private JMenuItem setAutozoom = new JCheckBoxMenuItem("Autoscale", true);

    /** Help menu. */
    private JMenu helpMenu = new JMenu("Help");

    /** Help menu item. */
    private JMenuItem helpItem = new JMenuItem("Help");

    /**
     * Default constructor.
     */
    public GaugeComponent() {
        super();
        this.setCurrentDirectory(GaugePreferences.getCurrentDirectory());
        gaugePanel = new GaugePanel();

        JPanel buffer = new JPanel();
        buffer.setLayout(new BorderLayout());
        buffer.add("North", gaugePanel.getTheToolBar());
        buffer.add(gaugePanel);
        buffer.add("South", gaugePanel.getBottomPanel());
        setContentPane(buffer);

        setUpMenus();
    }

    /**
     * Sets up gauge frame menus.
     */
    private void setUpMenus() {
        setJMenuBar(mb);

        mb.add(fileMenu);
        mb.add(prefsMenu);
        mb.add(helpMenu);

        helpMenu.addMenuListener(this);
        prefsMenu.addMenuListener(this);

        importCSV.addActionListener(this);
        open.addActionListener(this);
        exportHigh.addActionListener(this);
        exportLow.addActionListener(this);
        save.addActionListener(this);
        saveAs.addActionListener(this);
        projectionPrefs.addActionListener(this);
        graphicsPrefs.addActionListener(this);
        generalPrefs.addActionListener(this);
        setAutozoom.addActionListener(this);
        close.addActionListener(this);
        helpItem.addActionListener(this);

        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        fileMenu.add(open);
        fileMenu.add(save);
        fileMenu.add(saveAs);
        fileMenu.addSeparator();
        fileMenu.add(fileOpsMenu);
        fileOpsMenu.add(importCSV);
        fileOpsMenu.add(exportHigh);
        fileOpsMenu.add(exportLow);
        fileMenu.addSeparator();
        fileMenu.add(close);

        setCouplingMenuItem();
        prefsMenu.add(producerListItem);
        prefsMenu.addSeparator();
        prefsMenu.add(projectionPrefs);
        prefsMenu.add(graphicsPrefs);
        prefsMenu.add(generalPrefs);
        prefsMenu.addSeparator();
        prefsMenu.add(setAutozoom);

        helpMenu.add(helpItem);
    }

    /**
     * Set up the coupling menu.
     */
    private void setCouplingMenuItem() {
        producerListItem = Workspace.getInstance().getProducerListMenu(this);
        producerListItem.setText("Set gauge source");
    }

    /**
     * Responds to actions performed.
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {

        // Handle Coupling wireup
        if (e.getSource() instanceof CouplingMenuItem) {
            CouplingMenuItem m = (CouplingMenuItem) e.getSource();
            gaugePanel.getGauge().init(m.getCouplingContainer().getProducers().size());
            Iterator producerIterator = m.getCouplingContainer().getProducers().iterator();
            for (Consumer consumer : this.getGaugePanel().getGauge().getConsumers()) {
                if (producerIterator.hasNext()) {
                    Coupling coupling = new Coupling(((Producer) producerIterator.next()).getDefaultProducingAttribute(), consumer.getDefaultConsumingAttribute());
                    this.getGaugePanel().getGauge().getCouplings().add(coupling);
                }
            }
            gaugePanel.resetGauge();

        }

        if ((e.getSource().getClass() == JMenuItem.class) || (e.getSource().getClass() == JCheckBoxMenuItem.class)) {

            JMenuItem jmi = (JMenuItem) e.getSource();

            if (jmi == open) {
                open();
            } else if (jmi == saveAs) {
                saveAs();
            } else if (jmi == save) {
                save();
            } else {
                JMenuItem importCSV2 = importCSV;
                if (jmi == importCSV2) {
                    importCSV();
                } else if (jmi == exportLow) {
                    exportLow();
                } else if (jmi == exportHigh) {
                    exportHigh();
                } else if (jmi == projectionPrefs) {
                    gaugePanel.handlePreferenceDialogs();
                } else if (jmi == graphicsPrefs) {
                    gaugePanel.showGraphicsDialog();
                } else if (jmi == generalPrefs) {
                    gaugePanel.showGeneralDialog();
                } else if (jmi == setAutozoom) {
                    gaugePanel.setAutoZoom(setAutozoom.isSelected());
                    gaugePanel.repaint();
                } else if (jmi == close) {
                    if (isChangedSinceLastSave()) {
                        hasChanged();
                    } else {
                        try {
                            this.setClosed(true);
                        } catch (PropertyVetoException e1) {
                            e1.printStackTrace();
                        }
                    }
                } else if (jmi == helpItem) {
                    Utils.showQuickRef("Gauge.html");
                }
            }
        }
    }

    /**
     * Shows open file dialog.
     *
     * @return true if directory exisits
     */
    public boolean open() {
        SFileChooser chooser = new SFileChooser(this.getCurrentDirectory(), this.getFileExtension());
        File theFile = chooser.showOpenDialog();

        if (theFile != null) {
            readGauge(theFile);
            this.setCurrentDirectory(chooser.getCurrentLocation());
            return true;
        }

        String localDir = new String(System.getProperty("user.dir"));

        if (gaugePanel.getCurrentFile() != null) {
            this.setPath(Utils.getRelativePath(localDir, gaugePanel.getCurrentFile().getAbsolutePath()));
            setName(gaugePanel.getCurrentFile().getName());
        }
        return false;
    }

    /**
     * Saves current gauge. Calls saveAs if file has not been saved.
     */
    public void save() {
        if (gaugePanel.getCurrentFile() != null) {
            writeGauge(gaugePanel.getCurrentFile());
        } else {
            saveAs();
        }
    }

    /**
     * Opens save file dialog.
     */
    public void saveAs() {
        SFileChooser chooser = new SFileChooser(getCurrentDirectory(), getFileExtension());
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            writeGauge(theFile);
            setCurrentDirectory(chooser.getCurrentLocation());
        }
    }

    /**
     * Saves network information to the specified file.
     * @param theFile File to write
     */
    public void writeGauge(final File theFile) {
        gaugePanel.setCurrentFile(theFile);

        try {
//            LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "true");
//
//            FileWriter writer = new FileWriter(theFile);
//            Mapping map = new Mapping();
//            map.loadMapping("." + FS + "lib" + FS + "gauge_mapping.xml");
//
//            Marshaller marshaller = new Marshaller(writer);
//            marshaller.setMapping(map);
//
//            // marshaller.setDebug(true);
//            gaugePanel.getGauge().getCurrentProjector().getUpstairs().initPersistentData();
//            gaugePanel.getGauge().getCurrentProjector().getDownstairs().initPersistentData();
//            marshaller.marshal(gaugePanel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, gaugePanel.getCurrentFile().getAbsolutePath()));
        setName(theFile.getName());
        this.setChangedSinceLastSave(false);
    }

    /**
     * Reads gauge files.
     * @param f Gauge file to read
     */
    public void readGauge(final File f) {
 //       try {

//            Reader reader = new FileReader(f);
//            Mapping map = new Mapping();
//            map.loadMapping("." + FS + "lib" + FS + "gauge_mapping.xml");
//
//            // If theGaugePanel is not properly initialized at this point, nothing will show up on it
//            Unmarshaller unmarshaller = new Unmarshaller(gaugePanel);
//            unmarshaller.setMapping(map);
//
//            //unmarshaller.setDebug(true);
//            gaugePanel = (GaugePanel) unmarshaller.unmarshal(reader);
//            gaugePanel.initCastor();

//            // Initialize gauged variables, if any
//            NetworkComponent net = getWorkspace().getNetwork(gaugePanel.getGauge().getGaugedVars().getNetworkName());
//            if (net != null) {
//                gaugePanel.getGauge().getGaugedVars().initCastor(net);
//                net.getNetworkPanel().getRootNetwork().addNetworkListener(this);
//            }

//            //Set Path; used in workspace persistence
//            String localDir = new String(System.getProperty("user.dir"));
//            gaugePanel.setCurrentFile(f);
//            setPath(Utils.getRelativePath(localDir, gaugePanel.getCurrentFile().getAbsolutePath()));
//            setName(gaugePanel.getCurrentFile().getName());
//        } catch (java.io.FileNotFoundException e) {
//            JOptionPane.showMessageDialog(null, "Could not find the file \n" + f, "Warning", JOptionPane.ERROR_MESSAGE);
//
//            return;
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(
//                                          null, "There was a problem opening the file \n" + f, "Warning",
//                                          JOptionPane.ERROR_MESSAGE);
//            e.printStackTrace();
//
//            return;
//        }
    }

    /**
     * Import data from csv (comma-separated-values) file.
     */
    public void importCSV() {
        gaugePanel.resetGauge();

        SFileChooser chooser = new SFileChooser(getCurrentDirectory(), "csv");
        File theFile = chooser.showOpenDialog();

        if (theFile != null) {
            Dataset data = new Dataset(theFile);
            gaugePanel.getGauge().getCurrentProjector().init(data, null);
            gaugePanel.getGauge().getCurrentProjector().project();
            gaugePanel.centerCamera();
            setCurrentDirectory(chooser.getCurrentLocation());
        }
    }

    /**
     * Export high dimensional data to csv (comma-separated-values).
     */
    public void exportHigh() {
        SFileChooser chooser = new SFileChooser(getCurrentDirectory(), "csv");
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            gaugePanel.getGauge().getUpstairs().saveData(theFile);
            setCurrentDirectory(chooser.getCurrentLocation());
        }
    }

    /**
     * Export low-dimensional data to csv (comma-separated-values).
     */
    public void exportLow() {
        SFileChooser chooser = new SFileChooser(getCurrentDirectory(), "csv");
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            gaugePanel.getGauge().getDownstairs().saveData(theFile);
            setCurrentDirectory(chooser.getCurrentLocation());
        }
    }

    /**
     * Returns reference to gauge model which contains couplings.
     */
    public CouplingContainer getCouplingContainer() {
        return this.getGaugePanel().getGauge();
    }

    /**
     * Send state information to gauge.
     */
    public void updateComponent() {
        super.updateComponent();
        this.setChangedSinceLastSave(true);
        gaugePanel.update();
    }

    /**
     * Responds to internal frame closed.
     *
     * @param e Internal frame event
     */
    public void close() {
        getGaugePanel().stopThread();
        GaugePreferences.setCurrentDirectory(getCurrentDirectory());
    }


    /**
     * @return Returns the parent.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * @param parent The parent to set.
     */
    public void setWorkspace(final Workspace parent) {
        this.workspace = parent;
    }

    /**
     * @return Returns the theGaugePanel.
     */
    public GaugePanel getGaugePanel() {
        return gaugePanel;
    }

    /**
     * @param theGaugePanel The theGaugePanel to set.
     */
    public void setGaugePanel(final GaugePanel theGaugePanel) {
        this.gaugePanel = theGaugePanel;
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     */
    public void hasChanged() {
        Object[] options = {"Save", "Don't Save", "Cancel"};
        int s = JOptionPane
                .showInternalOptionDialog(
                        this,
                        "Gauge "
                                + this.getName()
                                + " has changed since last save,\nwould you like to save these changes?",
                        "Gauge Has Changed", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (s == 0) {
            // saveCombined();
            dispose();
        } else if (s == 1) {
            dispose();
        } else {
            return;
        }
    }

    /**
     * Menu cancelled.
     * @param arg0 Menu event
     */
    public void menuCanceled(final MenuEvent arg0) {
    }

    /**
     * Menu Deslected.
     * @param arg0 Menu event
     */
    public void menuDeselected(final MenuEvent arg0) {
    }

    /**
     * Menu selected.
     * @param arg0 Menu event
     */
    public void menuSelected(final MenuEvent arg0) {
        if (arg0.getSource().equals(fileMenu)) {
            if (this.isChangedSinceLastSave()) {
                save.setEnabled(true);
            } else if (!this.isChangedSinceLastSave()) {
                save.setEnabled(false);
            }
        } else if (arg0.getSource().equals(prefsMenu)) {
            setCouplingMenuItem();
            if (gaugePanel.getGauge().getCurrentProjector().hasDialog()) {
                projectionPrefs.setEnabled(true);
            } else {
                projectionPrefs.setEnabled(false);
            }
        }
    }

    @Override
    public int getDefaultHeight() {
        return 300;
    }

    @Override
    public int getDefaultWidth() {
        return 300;
    }


    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void save(File saveFile) {
        // TODO Auto-generated method stub
        
    }

    public List<Consumer> getConsumers() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Coupling> getCouplings() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Producer> getProducers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getWindowIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

}
