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

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.log4j.Logger;
import org.simbrain.gauge.core.Dataset;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.Projector;
import org.simbrain.gauge.graphics.GaugePanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Represents and interacts with a GaugeComponent in the SimbrainDesktop environment.
 * 
 * @author Matt Watson
 */
public class GaugeDesktopComponent extends GuiComponent<GaugeComponent> {

    /** The default static version Id for this class. */
    private static final long serialVersionUID = 1L;

    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(GaugeComponent.class);

    /** The initial height for the internal frame. */
    private static final int INITIAL_HEIGHT = 300;

    /** The initial width for the internal frame. */
    private static final int INITIAL_WIDTH = 300;
    
    /** The gauge that is the underlying model. */
    private final Gauge gauge;
    
    /** Gauge panel. */
    private final GaugePanel gaugePanel;

    /** The parent component. */
    private GaugeComponent component;
    
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
    private JMenuItem producerListItem;

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
    
    /** Listener for gaugeComponent events. */
    private final GaugeComponentListener listener = new GaugeComponentListener() {
        /**
         * {@inheritDoc}
         */
        public void componentUpdated() {
            gaugePanel.updateGraphics();
        }

        /**
         * {@inheritDoc}
         */
        public void dimensionsChanged(final int newDimensions) {
            gaugePanel.resetGauge();
        }

        public void setName(String name) {
        }
    };
    
    /**
     * Creates a new instance.
     * 
     * @param component The component this object will wrap.
     */
    public GaugeDesktopComponent(GenericFrame frame, final GaugeComponent component) {
        super(frame, component);
        this.component = component;
        component.addListener(listener);
        component.setCurrentDirectory(GaugePreferences.getCurrentDirectory());
        gauge = component.getGauge();
        gaugePanel = new GaugePanel(gauge); //TODO: Shouldn't this be the "Gaugepanel?"
        gaugePanel.setPreferredSize(new Dimension(INITIAL_HEIGHT, INITIAL_WIDTH));
        setLayout(new BorderLayout());
        add("North", gaugePanel.getTheToolBar());
        add(gaugePanel);
        add("South", gaugePanel.getBottomPanel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postAddInit() {
        setUpMenus();
        gauge.getCurrentProjector().postOpenInit(); // Ugh...
        gauge.setCurrentProjector(gauge.getCurrentProjector());
        gaugePanel.updateGraphics();
        component.resetCouplings(gauge.getCurrentProjector().getUpstairs().getDimensions());

    }
    
    /**
     * Sets up gauge frame menus.
     */
    private void setUpMenus() {
        getParentFrame().setJMenuBar(mb);

        mb.add(fileMenu);
        mb.add(prefsMenu);
        mb.add(helpMenu);

        helpMenu.addMenuListener(menuListener);
        prefsMenu.addMenuListener(menuListener);

        importCSV.addActionListener(menuItemListener);
        open.addActionListener(menuItemListener);
        exportHigh.addActionListener(menuItemListener);
        exportLow.addActionListener(menuItemListener);
        save.addActionListener(menuItemListener);
        saveAs.addActionListener(menuItemListener);
        projectionPrefs.addActionListener(menuItemListener);
        graphicsPrefs.addActionListener(menuItemListener);
        generalPrefs.addActionListener(menuItemListener);
        setAutozoom.addActionListener(menuItemListener);
        close.addActionListener(menuItemListener);
        helpItem.addActionListener(menuItemListener);

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
        producerListItem = CouplingMenus.getProducerListMenu(this.getWorkspaceComponent().getWorkspace(), couplingMenuItemListener);
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
    private final ActionListener menuItemListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            JMenuItem jmi = (JMenuItem) e.getSource();

            if (jmi == open) {
                showOpenFileDialog();
            } else if (jmi == saveAs) {
                showSaveFileDialog();
            } else if (jmi == save) {
                save();
            } else if (jmi == importCSV) {
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
                getParentFrame().dispose();
            } else if (jmi == helpItem) {
                Utils.showQuickRef("Gauge.html");
            }
        }
    };

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
     * Import data from csv (comma-separated-values) file.
     */
    public void importCSV() {
        gaugePanel.resetGauge();

        SFileChooser chooser = new SFileChooser(component.getCurrentDirectory(), "csv");
        File theFile = chooser.showOpenDialog();

        if (theFile != null) {
            Dataset data = new Dataset(theFile);
            gauge.getCurrentProjector().init(data, null);
            gauge.getCurrentProjector().project();
            gaugePanel.centerCamera();
            gaugePanel.updateGraphics();
            component.setCurrentDirectory(chooser.getCurrentLocation());
        }
    }

    /**
     * Export high dimensional data to csv (comma-separated-values).
     */
    public void exportHigh() {
        SFileChooser chooser = new SFileChooser(component.getCurrentDirectory(), "csv");
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            gauge.getUpstairs().saveData(theFile);
            component.setCurrentDirectory(chooser.getCurrentLocation());
        }
    }

    /**
     * Export low-dimensional data to csv (comma-separated-values).
     */
    public void exportLow() {
        SFileChooser chooser = new SFileChooser(component.getCurrentDirectory(), "csv");
        File theFile = chooser.showSaveDialog();

        if (theFile != null) {
            gauge.getDownstairs().saveData(theFile);
            component.setCurrentDirectory(chooser.getCurrentLocation());
        }
    }

    /**
     * Cleans-up and closes the Gauge panel.
     */
    public void closing() {
        gaugePanel.stopThread();
        GaugePreferences.setCurrentDirectory(component.getCurrentDirectory());
    }

    /**  Menu Listener. */
    private final MenuListener menuListener = new MenuListener() {
        public void menuCanceled(final MenuEvent arg0) {
        }

        public void menuDeselected(final MenuEvent arg0) {
        }

        /**
         * Menu selected.
         * @param arg0 Menu event
         */
        public void menuSelected(final MenuEvent arg0) {
            if (arg0.getSource().equals(fileMenu)) {
                if (component.hasChangedSinceLastSave()) {
                    save.setEnabled(true);
                } else if (!component.hasChangedSinceLastSave()) {
                    save.setEnabled(false);
                }
            } else if (arg0.getSource().equals(prefsMenu)) {
                setCouplingMenu();
                if (gauge.getCurrentProjector().hasDialog()) {
                    projectionPrefs.setEnabled(true);
                } else {
                    projectionPrefs.setEnabled(false);
                }
            }
        }
    };

    /** ActionListener for coupling menu events. */
    private final ActionListener couplingMenuItemListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            LOGGER.debug("coupling menu item selected");
            System.out.println("here");
            
            CouplingMenuItem menuItem = (CouplingMenuItem) e.getSource();
            Collection<? extends Producer> producers
                = menuItem.getWorkspaceComponent().getProducers();
            
            component.wireCouplings(producers);
        }
    };
    
}
