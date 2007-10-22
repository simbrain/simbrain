package org.simbrain.gauge;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.log4j.Logger;
import org.simbrain.gauge.core.Dataset;
import org.simbrain.gauge.core.Projector;
import org.simbrain.gauge.graphics.GaugePanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.CouplingContainer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceListener;
import org.simbrain.workspace.gui.CouplingMenuItem;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class GaugeDesktopComponent extends DesktopComponent implements ActionListener, MenuListener {

    private static final long serialVersionUID = 1L;

    /** Logger. */
    private Logger logger = Logger.getLogger(GaugeComponent.class);

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
    
    private WorkspaceComponentListener listener = new WorkspaceComponentListener() {
        public void componentUpdated() {
            gaugePanel.updateGraphics();
        }
    };

    /**
     * Default constructor.
     */
    public GaugeDesktopComponent(GaugeComponent component) {
        super(component);
        component.addListener(listener);
        this.setCurrentDirectory(GaugePreferences.getCurrentDirectory());
        this.setPreferredSize(new Dimension(300,300));
        gaugePanel = new GaugePanel(component.getGauge());

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

        setCouplingMenu();

        helpMenu.add(helpItem);
    }

    /**
     * Set up the coupling menu.
     */
    private void setCouplingMenu() {
        prefsMenu.removeAll();
        producerListItem = SimbrainDesktop.getInstance().getProducerListMenu(this);
        prefsMenu.add(producerListItem);
        prefsMenu.addSeparator();
        prefsMenu.add(projectionPrefs);
        prefsMenu.add(graphicsPrefs);
        prefsMenu.add(generalPrefs);
        prefsMenu.addSeparator();
        prefsMenu.add(setAutozoom);
    }

    /**
     * Responds to actions performed.
     * @param e Action event
     */
    // TODO make sure types match in coupling
    @SuppressWarnings("unchecked")
    public void actionPerformed(final ActionEvent e) {
        logger.debug("coupling menu item selected");
        // Handle Coupling wireup
        if (e.getSource() instanceof CouplingMenuItem) {
            
            int oldDims = gaugePanel.getGauge().getDimensions();
            
            Collection<? extends Producer> producers = ((CouplingMenuItem) e.getSource()).getWorkspaceComponent().getProducers();
            int newDims = producers.size();
            gaugePanel.getGauge().resetCouplings(newDims);
            Iterator<? extends Producer> producerIterator = producers.iterator();
            for (Consumer consumer : this.getGaugePanel().getGauge().getConsumers()) {
                if (producerIterator.hasNext()) {
                    Coupling<?> coupling = new Coupling(producerIterator.next().getDefaultProducingAttribute(), consumer.getDefaultConsumingAttribute());
                    SimbrainDesktop.getInstance().getWorkspace().addCoupling(coupling);
                    
                    this.getGaugePanel().getGauge().getCouplings().add(coupling);
                }
            }

            // If the new data is consistent with the old, don't reset the gauge
            if (oldDims != newDims){
                gaugePanel.getGauge().init(newDims);
                gaugePanel.resetGauge();
            }
        }

        if ((e.getSource().getClass() == JMenuItem.class) || (e.getSource().getClass() == JCheckBoxMenuItem.class)) {

            JMenuItem jmi = (JMenuItem) e.getSource();

            if (jmi == open) {
                showOpenFileDialog();
            } else if (jmi == saveAs) {
                showSaveFileDialog();
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
                    this.dispose();
                } else if (jmi == helpItem) {
                    Utils.showQuickRef("Gauge.html");
                }
            }
        }
    }

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(Projector.class, "logger");
        xstream.omitField(Dataset.class, "logger");
        xstream.omitField(Dataset.class, "distances");
        xstream.omitField(Dataset.class, "dataset");
        return xstream;
    }

    /**
     * Saves network information to the specified file.
     * @param theFile File to write
     */
    public void open(final File theFile) {
        setCurrentFile(theFile);
        FileReader reader;
        try {
            reader = new FileReader(theFile);
            Projector projector = (Projector) getXStream().fromXML(reader);
            projector.postOpenInit();
            this.getGaugePanel().getGauge().setCurrentProjector(projector);
            this.getGaugePanel().updateGraphics();
            this.getGaugePanel().getGauge().resetCouplings(projector.getUpstairs().getDimensions());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        setStringReference(theFile);
        setChangedSinceLastSave(false);
    }

    /**
     * Reads gauge files.
     * @param f Gauge file to read
     */
    public void save(final File f) {
        setCurrentFile(f);
        this.getGaugePanel().getGauge().getCurrentProjector().preSaveInit();
        String xml = getXStream().toXML(this.getGaugePanel().getGauge().getCurrentProjector());
        try {
            FileWriter writer  = new FileWriter(f);
            writer.write(xml);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setStringReference(f);
        setChangedSinceLastSave(false);
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
            gaugePanel.updateGraphics();
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
     * Responds to internal frame closed.
     *
     * @param e Internal frame event
     */
    public void close() {
        getGaugePanel().stopThread();
        GaugePreferences.setCurrentDirectory(getCurrentDirectory());
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
    public void showHasChangedDialog() {
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
            setCouplingMenu();
            if (gaugePanel.getGauge().getCurrentProjector().hasDialog()) {
                projectionPrefs.setEnabled(true);
            } else {
                projectionPrefs.setEnabled(false);
            }
        }
    }

    @Override
    public String getFileExtension() {
        return "gdf";
    }

    @Override
    public void setCurrentDirectory(final String currentDirectory) {        
        super.setCurrentDirectory(currentDirectory);
        GaugePreferences.setCurrentDirectory(currentDirectory);
    }

    @Override
    public String getCurrentDirectory() {
        return GaugePreferences.getCurrentDirectory();
    }
}
