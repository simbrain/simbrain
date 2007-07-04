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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.Collection;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.gauge.core.Dataset;
import org.simbrain.gauge.graphics.GaugePanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simnet.interfaces.NetworkEvent;
import org.simnet.interfaces.NetworkListener;

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

        mb.add(getFileMenu());
        mb.add(getPrefsMenu());
        mb.add(getHelpMenu());

        getFileMenu().addMenuListener(this);
        getPrefsMenu().addMenuListener(this);

        getImportCSV().addActionListener(this);
        getOpen().addActionListener(this);
        getExportHigh().addActionListener(this);
        getExportLow().addActionListener(this);
        getSave().addActionListener(this);
        getSaveAs().addActionListener(this);
        getProjectionPrefs().addActionListener(this);
        getGraphicsPrefs().addActionListener(this);
        getGeneralPrefs().addActionListener(this);
        getSetAutozoom().addActionListener(this);
        getClose().addActionListener(this);
        getHelpItem().addActionListener(this);

        getOpen().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        getSaveAs().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        getClose().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        getFileMenu().add(getOpen());
        getFileMenu().add(getSave());
        getFileMenu().add(getSaveAs());
        getFileMenu().addSeparator();
        getFileMenu().add(getFileOpsMenu());
        getFileOpsMenu().add(getImportCSV());
        getFileOpsMenu().add(getExportHigh());
        getFileOpsMenu().add(getExportLow());
        getFileMenu().addSeparator();
        getFileMenu().add(getClose());

        getPrefsMenu().add(getProjectionPrefs());
        getPrefsMenu().add(getGraphicsPrefs());
        getPrefsMenu().add(getGeneralPrefs());
        getPrefsMenu().addSeparator();
        getPrefsMenu().add(getSetAutozoom());

        getHelpMenu().add(getHelpItem());
    }

    /**
     * Responds to actions performed.
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        if ((e.getSource().getClass() == JMenuItem.class) || (e.getSource().getClass() == JCheckBoxMenuItem.class)) {
            JMenuItem jmi = (JMenuItem) e.getSource();

            if (jmi == getOpen()) {
                open();
            } else if (jmi == getSaveAs()) {
                saveAs();
            } else if (jmi == getSave()) {
                save();
            } else if (jmi == getImportCSV()) {
                importCSV();
            } else if (jmi == getExportLow()) {
                exportLow();
            } else if (jmi == getExportHigh()) {
                exportHigh();
            } else if (jmi == getProjectionPrefs()) {
                gaugePanel.handlePreferenceDialogs();
            } else if (jmi == getGraphicsPrefs()) {
                gaugePanel.showGraphicsDialog();
            } else if (jmi == getGeneralPrefs()) {
                gaugePanel.showGeneralDialog();
            } else if (jmi == getSetAutozoom()) {
                gaugePanel.setAutoZoom(getSetAutozoom().isSelected());
                gaugePanel.repaint();
            } else if (jmi == getClose()) {
                if (isChangedSinceLastSave()) {
                    hasChanged();
                } else {
                    try {
                        this.setClosed(true);
                    } catch (PropertyVetoException e1) {
                        e1.printStackTrace();
                    }
                }
            } else if (jmi == getHelpItem()) {
                Utils.showQuickRef("Gauge.html");
            }
        }
    }

    /**
     * Shows open file dialog.
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

    
    public double[] getState() {
        return null;
    }
    
    /**
     * Send state information to gauge.
     */
    public void updateComponent() {
        super.updateComponent();
        this.setChangedSinceLastSave(true);
        double[] state = getState();
        gaugePanel.getGauge().addDatapoint(state);
        gaugePanel.update();
        gaugePanel.setHotPoint(gaugePanel.getGauge().getUpstairs().getClosestIndex(state));
    }

    /**
     * Responds to internal frame opened.
     * @param e Internal frame event
     */
    public void internalFrameOpened(final InternalFrameEvent e) {
    }

    /**
     * Responds to internal frame closing.
     * @param e Internal frame event
     */
    public void internalFrameClosing(final InternalFrameEvent e) {
        if (isChangedSinceLastSave()) {
            hasChanged();
        } else {
            dispose();
        }
    }

    /**
     * Responds to internal frame closed.
     * @param e Internal frame event
     */
    public void close() {
        getGaugePanel().stopThread();
        GaugePreferences.setCurrentDirectory(getCurrentDirectory());
    }

    /**
     * Responds to internal frame iconified.
     * @param e Internal frame event
     */
    public void internalFrameIconified(final InternalFrameEvent e) {
    }

    /**
     * Resonds to internal frame deiconified.
     * @param e Internal frame event
     */
    public void internalFrameDeiconified(final InternalFrameEvent e) {
    }

    /**
     * Resonds to internal frame activated.
     * @param e Internal frame event
     */
    public void internalFrameActivated(final InternalFrameEvent e) {
    }

    /**
     * Resonds to internal frame deactivated.
     * @param e Internal frame event
     */
    public void internalFrameDeactivated(final InternalFrameEvent e) {
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
     * @param mb The mb to set.
     */
    void setMb(final JMenuBar mb) {
        this.mb = mb;
    }

    /**
     * @return Returns the mb.
     */
    JMenuBar getMb() {
        return mb;
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
        if (arg0.getSource().equals(getFileMenu())) {
            if (this.isChangedSinceLastSave()) {
                getSave().setEnabled(true);
            } else if (!this.isChangedSinceLastSave()) {
                getSave().setEnabled(false);
            }
        } else if (arg0.getSource().equals(getPrefsMenu())) {
            if (gaugePanel.getGauge().getCurrentProjector().hasDialog()) {
                getProjectionPrefs().setEnabled(true);
            } else {
                getProjectionPrefs().setEnabled(false);
            }
        }
    }

    /**
     * @param fileMenu The fileMenu to set.
     */
    void setFileMenu(final JMenu fileMenu) {
        this.fileMenu = fileMenu;
    }

    /**
     * @return Returns the fileMenu.
     */
    JMenu getFileMenu() {
        return fileMenu;
    }

    /**
     * @param open The open to set.
     */
    void setOpen(final JMenuItem open) {
        this.open = open;
    }

    /**
     * @return Returns the open.
     */
    JMenuItem getOpen() {
        return open;
    }

    /**
     * @param save The save to set.
     */
    void setSave(final JMenuItem save) {
        this.save = save;
    }

    /**
     * @return Returns the save.
     */
    JMenuItem getSave() {
        return save;
    }

    /**
     * @param saveAs The saveAs to set.
     */
    void setSaveAs(final JMenuItem saveAs) {
        this.saveAs = saveAs;
    }

    /**
     * @return Returns the saveAs.
     */
    JMenuItem getSaveAs() {
        return saveAs;
    }

    /**
     * @param fileOpsMenu The fileOpsMenu to set.
     */
    void setFileOpsMenu(final JMenu fileOpsMenu) {
        this.fileOpsMenu = fileOpsMenu;
    }

    /**
     * @return Returns the fileOpsMenu.
     */
    JMenu getFileOpsMenu() {
        return fileOpsMenu;
    }

    /**
     * @param importCSV The importCSV to set.
     */
    void setImportCSV(final JMenuItem importCSV) {
        this.importCSV = importCSV;
    }

    /**
     * @return Returns the importCSV.
     */
    JMenuItem getImportCSV() {
        return importCSV;
    }

    /**
     * @param exportLow The exportLow to set.
     */
    void setExportLow(final JMenuItem exportLow) {
        this.exportLow = exportLow;
    }

    /**
     * @return Returns the exportLow.
     */
    JMenuItem getExportLow() {
        return exportLow;
    }

    /**
     * @param exportHigh The exportHigh to set.
     */
    void setExportHigh(final JMenuItem exportHigh) {
        this.exportHigh = exportHigh;
    }

    /**
     * @return Returns the exportHigh.
     */
    JMenuItem getExportHigh() {
        return exportHigh;
    }

    /**
     * @param close The close to set.
     */
    void setClose(final JMenuItem close) {
        this.close = close;
    }

    /**
     * @return Returns the close.
     */
    JMenuItem getClose() {
        return close;
    }

    /**
     * @param prefsMenu The prefsMenu to set.
     */
    void setPrefsMenu(final JMenu prefsMenu) {
        this.prefsMenu = prefsMenu;
    }

    /**
     * @return Returns the prefsMenu.
     */
    JMenu getPrefsMenu() {
        return prefsMenu;
    }

    /**
     * @param projectionPrefs The projectionPrefs to set.
     */
    void setProjectionPrefs(final JMenuItem projectionPrefs) {
        this.projectionPrefs = projectionPrefs;
    }

    /**
     * @return Returns the projectionPrefs.
     */
    JMenuItem getProjectionPrefs() {
        return projectionPrefs;
    }

    /**
     * @param graphicsPrefs The graphicsPrefs to set.
     */
    void setGraphicsPrefs(final JMenuItem graphicsPrefs) {
        this.graphicsPrefs = graphicsPrefs;
    }

    /**
     * @return Returns the graphicsPrefs.
     */
    JMenuItem getGraphicsPrefs() {
        return graphicsPrefs;
    }

    /**
     * @param generalPrefs The generalPrefs to set.
     */
    void setGeneralPrefs(final JMenuItem generalPrefs) {
        this.generalPrefs = generalPrefs;
    }

    /**
     * @return Returns the generalPrefs.
     */
    JMenuItem getGeneralPrefs() {
        return generalPrefs;
    }

    /**
     * @param setAutozoom The setAutozoom to set.
     */
    void setSetAutozoom(final JMenuItem setAutozoom) {
        this.setAutozoom = setAutozoom;
    }

    /**
     * @return Returns the setAutozoom.
     */
    JMenuItem getSetAutozoom() {
        return setAutozoom;
    }

    /**
     * @param helpMenu The helpMenu to set.
     */
    void setHelpMenu(final JMenu helpMenu) {
        this.helpMenu = helpMenu;
    }

    /**
     * @return Returns the helpMenu.
     */
    JMenu getHelpMenu() {
        return helpMenu;
    }

    /**
     * @param helpItem The helpItem to set.
     */
    void setHelpItem(final JMenuItem helpItem) {
        this.helpItem = helpItem;
    }

    /**
     * @return Returns the helpItem.
     */
    JMenuItem getHelpItem() {
        return helpItem;
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
    public int getDefaultLocationX() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getDefaultLocationY() {
        // TODO Auto-generated method stub
        return 0;
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
