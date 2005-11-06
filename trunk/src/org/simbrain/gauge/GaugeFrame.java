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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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


/**
 * <b>GaugeFrame</b> wraps a Gauge object in a Simbrain workspace frame, which also stores  information about the
 * variables the Gauge is representing.
 */
public class GaugeFrame extends JInternalFrame implements InternalFrameListener, ActionListener, MenuListener {
    public static final String FS = "/"; // System.getProperty("file.separator");Separator();
    private Workspace workspace;
    private GaugePanel theGaugePanel;
    private String name = null;

    //Defaault directory
    private String localDir = new String();
    private String default_directory = GaugePreferences.getCurrentDirectory();

    // For workspace persistence
    private String path = null;
    private int xpos;
    private int ypos;
    private int the_width;
    private int the_height;
    private boolean changedSinceLastSave = false;

    // Menu stuff
    private JMenuBar mb = new JMenuBar();
    private JMenu fileMenu = new JMenu("File  ");
    private JMenuItem open = new JMenuItem("Open");
    private JMenuItem save = new JMenuItem("Save");
    private JMenuItem saveAs = new JMenuItem("Save As");
    private JMenu fileOpsMenu = new JMenu("Import / Export");
    private JMenuItem importCSV = new JMenuItem("Import CSV");
    private JMenuItem exportLow = new JMenuItem("Export Low-Dimensional CSV");
    private JMenuItem exportHigh = new JMenuItem("Export High-Dimensional CSV");
    private JMenuItem close = new JMenuItem("Close");
    private JMenu prefsMenu = new JMenu("Preferences");
    private JMenuItem projectionPrefs = new JMenuItem("Projection Preferences");
    private JMenuItem graphicsPrefs = new JMenuItem("Graphics /GUI Preferences");
    private JMenuItem generalPrefs = new JMenuItem("General Preferences");
    private JMenuItem setAutozoom = new JCheckBoxMenuItem("Autoscale", true);
    private JMenu helpMenu = new JMenu("Help");
    private JMenuItem helpItem = new JMenuItem("Help");

    public GaugeFrame() {
    }

    public GaugeFrame(final Workspace ws) {
        workspace = ws;
        init();
    }

    public void init() {
        theGaugePanel = new GaugePanel();
        getContentPane().add(theGaugePanel);

        this.addInternalFrameListener(this);
        this.setResizable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);

        setUpMenus();
    }

    private void setUpMenus() {
        setJMenuBar(mb);

        mb.add(getFileMenu());
        mb.add(getPrefsMenu());
        mb.add(getHelpMenu());

        getFileMenu().addMenuListener(this);

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

        getOpen().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        getSaveAs().setAccelerator(KeyStroke.getKeyStroke(
                                                     KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        getClose().setAccelerator(KeyStroke.getKeyStroke(
                                                    KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

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

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
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
                theGaugePanel.handlePreferenceDialogs();
            } else if (jmi == getGraphicsPrefs()) {
                theGaugePanel.handleGraphicsDialog();
            } else if (jmi == getGeneralPrefs()) {
                theGaugePanel.handleGeneralDialog();
            } else if (jmi == getSetAutozoom()) {
                theGaugePanel.setAutoZoom(getSetAutozoom().isSelected());
                theGaugePanel.repaint();
            } else if (jmi == getClose()) {
                if (isChangedSinceLastSave()) {
                    hasChanged();
                } else {
                    dispose();
                }
            } else if (jmi == getHelpItem()) {
                Utils.showQuickRef(this);
            }
        }
    }

    public void open() {
        SFileChooser chooser = new SFileChooser(default_directory, "xml");
        File theFile = chooser.showOpenDialog();

        if (theFile != null) {
            readGauge(theFile);
            default_directory = chooser.getCurrentLocation();
        }

        String localDir = new String(System.getProperty("user.dir"));

        if (theGaugePanel.getCurrentFile() != null) {
            this.setPath(Utils.getRelativePath(localDir, theGaugePanel.getCurrentFile().getAbsolutePath()));
            setName(theGaugePanel.getCurrentFile().getName());
        }
    }

    public void save() {
        if (theGaugePanel.getCurrentFile() != null) {
            writeGauge(theGaugePanel.getCurrentFile());
        } else {
            saveAs();
        }
    }

    public void saveAs() {
        SFileChooser chooser = new SFileChooser(default_directory, "xml");
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            writeGauge(theFile);
            default_directory = (chooser.getCurrentLocation());
        }
    }

    /**
     * Saves network information to the specified file
     */
    public void writeGauge(final File theFile) {
        theGaugePanel.setCurrentFile(theFile);

        try {
            LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "true");

            FileWriter writer = new FileWriter(theFile);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "gauge_mapping.xml");

            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(map);

            // marshaller.setDebug(true);
            theGaugePanel.getGauge().getCurrentProjector().getUpstairs().initPersistentData();
            theGaugePanel.getGauge().getCurrentProjector().getDownstairs().initPersistentData();
            marshaller.marshal(theGaugePanel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, theGaugePanel.getCurrentFile().getAbsolutePath()));
        setName(theFile.getName());
        this.setChangedSinceLastSave(false);
    }

    public void readGauge(final File f) {
        try {
            Reader reader = new FileReader(f);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "gauge_mapping.xml");

            Unmarshaller unmarshaller = new Unmarshaller(theGaugePanel);
            unmarshaller.setMapping(map);

            //unmarshaller.setDebug(true);
            theGaugePanel = (GaugePanel) unmarshaller.unmarshal(reader);
            theGaugePanel.initCastor();

            NetworkFrame net = getWorkspace().getNetwork(theGaugePanel.getGauge().getGaugedVars().getNetworkName());
            theGaugePanel.getGauge().getGaugedVars().initCastor(net);
            theGaugePanel.getGauge().getGaugedVars().setParent(theGaugePanel.getGauge());

            //Set Path; used in workspace persistence
            String localDir = new String(System.getProperty("user.dir"));
            theGaugePanel.setCurrentFile(f);
            setPath(Utils.getRelativePath(localDir, theGaugePanel.getCurrentFile().getAbsolutePath()));
            setName(theGaugePanel.getCurrentFile().getName());
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
     * Import data from csv (comma-separated-values) file
     */
    public void importCSV() {
        theGaugePanel.resetGauge();

        SFileChooser chooser = new SFileChooser(default_directory, "csv");
        File theFile = chooser.showOpenDialog();

        if (theFile != null) {
            Dataset data = new Dataset();
            data.readData(theFile);
            theGaugePanel.getGauge().getCurrentProjector().init(data, null);
            theGaugePanel.getGauge().getCurrentProjector().project();
            update();
            theGaugePanel.centerCamera();
            default_directory = chooser.getCurrentLocation();
        }
    }

    /**
     * Export high dimensional data to csv (comma-separated-values)
     */
    public void exportHigh() {
        SFileChooser chooser = new SFileChooser(default_directory, "csv");
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            theGaugePanel.getGauge().getUpstairs().saveData(theFile);
            default_directory = chooser.getCurrentLocation();
        }
    }

    /**
     * Export low-dimensional data to csv (comma-separated-values)
     */
    public void exportLow() {
        SFileChooser chooser = new SFileChooser(default_directory, "csv");
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            theGaugePanel.getGauge().getDownstairs().saveData(theFile);
            default_directory = chooser.getCurrentLocation();
        }
    }

    public GaugedVariables getGaugedVars() {
        return theGaugePanel.getGauge().getGaugedVars();
    }

    /**
     * Send state information to gauge
     */
    public void update() {
        changedSinceLastSave = true;

        double[] state = theGaugePanel.getGauge().getGaugedVars().getState();
        theGaugePanel.getGauge().addDatapoint(state);
        theGaugePanel.update();
        theGaugePanel.setHotPoint(theGaugePanel.getGauge().getUpstairs().getClosestIndex(state));
    }

    public void internalFrameOpened(final InternalFrameEvent e) {
    }

    public void internalFrameClosing(final InternalFrameEvent e) {
        if (isChangedSinceLastSave()) {
            hasChanged();
        } else {
            dispose();
        }
    }

    public void internalFrameClosed(final InternalFrameEvent e) {
        this.getWorkspace().getGaugeList().remove(this);
        GaugePreferences.setCurrentDirectory(default_directory);
    }

    public void internalFrameIconified(final InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(final InternalFrameEvent e) {
    }

    public void internalFrameActivated(final InternalFrameEvent e) {
    }

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
        the_width = this.getBounds().width;
        the_height = this.getBounds().height;
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
    public int getThe_height() {
        return the_height;
    }

    /**
     * @param the_height The the_height to set.
     */
    public void setThe_height(final int the_height) {
        this.the_height = the_height;
    }

    /**
     * @return Returns the the_width.
     */
    public int getThe_width() {
        return the_width;
    }

    /**
     * @param the_width The the_width to set.
     */
    public void setThe_width(final int the_width) {
        this.the_width = the_width;
    }

    /**
     * @return Returns the theGaugePanel.
     */
    public GaugePanel getGaugePanel() {
        return theGaugePanel;
    }

    /**
     * @param theGaugePanel The theGaugePanel to set.
     */
    public void setGaugePanel(final GaugePanel theGaugePanel) {
        this.theGaugePanel = theGaugePanel;
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
     * Checks to see if anything has changed and then offers to save if true
     */
    public void hasChanged() {
        Object[] options = {"Save", "Don't Save", "Cancel" };
        int s = JOptionPane.showInternalOptionDialog(
                                                     this,
                                                     "Gauge " + this.getName()
                                                     + " has changed since last save,\nwould you like to save these changes?",
                                                     "Gauge Has Changed", JOptionPane.YES_NO_OPTION,
                                                     JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (s == 0) {
            //    saveCombined();
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

    public boolean isChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    public void setChangedSinceLastSave(final boolean changedSinceLastSave) {
        this.changedSinceLastSave = changedSinceLastSave;
    }

    public void menuCanceled(final MenuEvent arg0) {
    }

    public void menuDeselected(final MenuEvent arg0) {
    }

    public void menuSelected(final MenuEvent arg0) {
        if (arg0.getSource().equals(getFileMenu())) {
            if (this.isChangedSinceLastSave()) {
                getSave().setEnabled(true);
            } else if (!this.isChangedSinceLastSave()) {
                getSave().setEnabled(false);
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
}
