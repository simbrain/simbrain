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

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.util.LocalConfiguration;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.simbrain.gauge.core.Dataset;
import org.simbrain.gauge.graphics.GaugePanel;
import org.simbrain.network.NetworkFrame;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;
import org.simnet.interfaces.NetworkEvent;
import org.simnet.interfaces.NetworkListener;

/**
 * <b>GaugeFrame</b> wraps a Gauge object in a Simbrain workspace frame, which also stores information about the
 * variables the Gauge is representing.
 */
public class GaugeFrame extends JInternalFrame
    implements NetworkListener, InternalFrameListener, ActionListener, MenuListener {

    /** File system seperator. */
    public static final String FS = System.getProperty("file.separator");

    /** Current workspace. */
    private Workspace workspace;

    /** Gauge panel. */
    private GaugePanel gaugePanel;

    /** Name of gauge. */
    private String name = null;

    /** Default directory. */
    private String defaultDirectory = GaugePreferences.getCurrentDirectory();

    /** Path.  Used by Castor. */
    private String path = null;

    /** Current x position. Used by Castor so as not to conflict with Component's getX(). */
    private int xpos;

    /** Current y postion. Used by Castor so as not to conflict with Component's getY(). */
    private int ypos;

    /** Width of frame. Used by Castor. */
    private int theWidth;

    /** Height of frame. Used by Castor.ee */
    private int theHeight;

    /** Has gauge changed since last save. */
    private boolean changedSinceLastSave = false;

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
    public GaugeFrame() {
    }

    /**
     * Instance of gauge frame.
     * @param ws Current workspace
     */
    public GaugeFrame(final Workspace ws) {
        workspace = ws;
        init();
    }

    /**
     * Initializes gauge frame.
     */
    public void init() {
        gaugePanel = new GaugePanel();
        gaugePanel.getGauge().getGaugedVars().setParent(gaugePanel.getGauge());

        JPanel buffer = new JPanel();
        buffer.setLayout(new BorderLayout());
        buffer.add("North", gaugePanel.getTheToolBar());
        buffer.add(gaugePanel);
        buffer.add("South", gaugePanel.getBottomPanel());
        setContentPane(buffer);

        this.addInternalFrameListener(this);
        this.setResizable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);

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
                gaugePanel.handleGraphicsDialog();
            } else if (jmi == getGeneralPrefs()) {
                gaugePanel.handleGeneralDialog();
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
        SFileChooser chooser = new SFileChooser(defaultDirectory, "gdf");
        File theFile = chooser.showOpenDialog();

        if (theFile != null) {
            readGauge(theFile);
            defaultDirectory = chooser.getCurrentLocation();
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
        SFileChooser chooser = new SFileChooser(defaultDirectory, "gdf");
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            writeGauge(theFile);
            defaultDirectory = (chooser.getCurrentLocation());
        }
    }

    /**
     * Reset the gauge; used when the object it is gauging is removed.
     *
     * TODO: Make network open, close, save events that are listened to?
     *
     */
    public void reset() {
        this.getGaugedVars().clear();
    }

    /**
     * Initialize this gauge to a specified set of variables and a network name.
     *
     * @param gaugedVars The gaugedVars to set.
     * @param networkName the name of the network whose variables these are
     */
    public void setVariables(final Collection gaugedVars, final String networkName) {
        getGaugedVars().setVariables(gaugedVars);
        getGaugedVars().setNetworkName(networkName);
        NetworkFrame net = getWorkspace().getNetwork(networkName);
        if (net != null) {
            net.getNetworkPanel().getNetwork().addNetworkListener(this);
        }
    }

    /**
     * Saves network information to the specified file.
     * @param theFile File to write
     */
    public void writeGauge(final File theFile) {
        gaugePanel.setCurrentFile(theFile);
        getGaugedVars().prepareToSave();

        try {
            LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "true");

            FileWriter writer = new FileWriter(theFile);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "gauge_mapping.xml");

            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(map);

            // marshaller.setDebug(true);
            gaugePanel.getGauge().getCurrentProjector().getUpstairs().initPersistentData();
            gaugePanel.getGauge().getCurrentProjector().getDownstairs().initPersistentData();
            marshaller.marshal(gaugePanel);
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
        try {

            Reader reader = new FileReader(f);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "gauge_mapping.xml");

            // If theGaugePanel is not properly initialized at this point, nothing will show up on it
            Unmarshaller unmarshaller = new Unmarshaller(gaugePanel);
            unmarshaller.setMapping(map);

            //unmarshaller.setDebug(true);
            gaugePanel = (GaugePanel) unmarshaller.unmarshal(reader);
            gaugePanel.initCastor();

            // Initialize gauged variables, if any
            NetworkFrame net = getWorkspace().getNetwork(gaugePanel.getGauge().getGaugedVars().getNetworkName());
            if (net != null) {
                gaugePanel.getGauge().getGaugedVars().initCastor(net);
                net.getNetworkPanel().getNetwork().addNetworkListener(this);
            }

            //Set Path; used in workspace persistence
            String localDir = new String(System.getProperty("user.dir"));
            gaugePanel.setCurrentFile(f);
            setPath(Utils.getRelativePath(localDir, gaugePanel.getCurrentFile().getAbsolutePath()));
            setName(gaugePanel.getCurrentFile().getName());
        } catch (java.io.FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Could not find the file \n" + f, "Warning", JOptionPane.ERROR_MESSAGE);

            return;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                                          null, "There was a problem opening the file \n" + f, "Warning",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            return;
        }
    }

    /**
     * Import data from csv (comma-separated-values) file.
     */
    public void importCSV() {
        gaugePanel.resetGauge();

        SFileChooser chooser = new SFileChooser(defaultDirectory, "csv");
        File theFile = chooser.showOpenDialog();

        if (theFile != null) {
            Dataset data = new Dataset();
            data.readData(theFile);
            gaugePanel.getGauge().getCurrentProjector().init(data, null);
            gaugePanel.getGauge().getCurrentProjector().project();
            update();
            gaugePanel.centerCamera();
            defaultDirectory = chooser.getCurrentLocation();
        }
    }

    /**
     * Export high dimensional data to csv (comma-separated-values).
     */
    public void exportHigh() {
        SFileChooser chooser = new SFileChooser(defaultDirectory, "csv");
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            gaugePanel.getGauge().getUpstairs().saveData(theFile);
            defaultDirectory = chooser.getCurrentLocation();
        }
    }

    /**
     * Export low-dimensional data to csv (comma-separated-values).
     */
    public void exportLow() {
        SFileChooser chooser = new SFileChooser(defaultDirectory, "csv");
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            gaugePanel.getGauge().getDownstairs().saveData(theFile);
            defaultDirectory = chooser.getCurrentLocation();
        }
    }

    /**
     * @return Set of gauged variables.
     */
    public GaugedVariables getGaugedVars() {
        return gaugePanel.getGauge().getGaugedVars();
    }

    /**
     * Send state information to gauge.
     */
    public void update() {
        changedSinceLastSave = true;
        double[] state = gaugePanel.getGauge().getGaugedVars().getState();
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
    public void internalFrameClosed(final InternalFrameEvent e) {
        NetworkFrame net = getWorkspace().getNetwork(gaugePanel.getGauge().getGaugedVars().getNetworkName());
        if (net != null) {
            net.getNetworkPanel().getNetwork().removeNetworkListener(this);
        }

        getGaugePanel().stopThread();

        this.getWorkspace().getGaugeList().remove(this);
        GaugePreferences.setCurrentDirectory(defaultDirectory);
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
     * @return Returns the path.  Used in persistence.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return platform-specific path.  Used in persistence.
     */
    public String getGenericPath() {
        String ret = path;

        if (path == null) {
            return null;
        }

        ret.replace('/', System.getProperty("file.separator").charAt(0));

        return ret;
    }

    /**
     * @param path The path to set.  Used in persistence.
     */
    public void setPath(final String path) {
        String thePath = path;

        if (thePath.charAt(2) == '.') {
            thePath = path.substring(2, path.length());
        }

        thePath = thePath.replace(System.getProperty("file.separator").charAt(0), '/');
        this.path = thePath;
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
     * For Castor.  Turn Component bounds into separate variables.
     */
    public void initBounds() {
        xpos = this.getX();
        ypos = this.getY();
        theWidth = this.getBounds().width;
        theHeight = this.getBounds().height;
    }

    /**
     * @return Returns the xpos.
     */
    public int getXpos() {
        return xpos;
    }

    /**
     * @param xpos The xpos to set.
     */
    public void setXpos(final int xpos) {
        this.xpos = xpos;
    }

    /**
     * @return Returns the ypos.
     */
    public int getYpos() {
        return ypos;
    }

    /**
     * @param ypos The ypos to set.
     */
    public void setYpos(final int ypos) {
        this.ypos = ypos;
    }

    /**
     * @return Returns the the_height.
     */
    public int getTheHeight() {
        return theHeight;
    }

    /**
     * @param theHeight The the_height to set.
     */
    public void setTheHeight(final int theHeight) {
        this.theHeight = theHeight;
    }

    /**
     * @return Returns the the_width.
     */
    public int getTheWidth() {
        return theWidth;
    }

    /**
     * @param theWidth The the_width to set.
     */
    public void setTheWidth(final int theWidth) {
        this.theWidth = theWidth;
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
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(final String name) {
        this.name = name;
        setTitle(name);
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
     * @return Has gauge changed since last save.
     */
    public boolean isChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    /**
     * @param changedSinceLastSave Has gauge changed since last save.
     */
    public void setChangedSinceLastSave(final boolean changedSinceLastSave) {
        this.changedSinceLastSave = changedSinceLastSave;
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
            if (gaugePanel.checkProjector()) {
                getProjectionPrefs().setEnabled(false);
            } else {
                getProjectionPrefs().setEnabled(true);
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

    /** @see NetworkListener */
    public void networkChanged() {
        if (getGaugedVars().getNumVariables() > 0) {
            update();
        }
    }

    /** @see NetworkListener */
    public void neuronChanged(final NetworkEvent e) {
        getGaugedVars().changeVariable(e.getOldNeuron(), e.getNeuron());
    }

    /** @see NetworkListener */
    public void neuronAdded(final NetworkEvent e) {
        if (getGaugedVars().getNumVariables() == 0) {
            getGaugedVars().addVariable(e.getNeuron());
            gaugePanel.resetGauge();
        }
    }

    /** @see NetworkListener */
    public void neuronRemoved(final NetworkEvent e) {
        this.getGaugedVars().removeVariable(e.getNeuron());
        gaugePanel.resetGauge();
    }

    /** @see NetworkListener */
    public void synapseRemoved(final NetworkEvent e) {
        getGaugedVars().removeVariable(e.getSynapse());
        gaugePanel.resetGauge();
    }

    /** @see NetworkListener */
    public void synapseAdded(final NetworkEvent e) {
    }

    /** @see NetworkListener */
    public void synapseChanged(final NetworkEvent e) {
        getGaugedVars().changeVariable(e.getOldSynapse(), e.getSynapse());
    }

    /** @see NetworkListener */
    public void couplingChanged(final NetworkEvent e) {
    }

    /** @see NetworkListener */
    public void subnetAdded(final NetworkEvent e) {
    }

    /** @see NetworkListener */
    public void subnetRemoved(final NetworkEvent e) {
    }

    /** @see NetworkListener */
    public void clampChanged() {
    }

}
