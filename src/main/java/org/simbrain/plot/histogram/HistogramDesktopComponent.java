package org.simbrain.plot.histogram;

import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.*;

/**
 * Display a Histogram in the Simbrain Desktop.
 */
public class HistogramDesktopComponent extends DesktopComponent<HistogramComponent> {

    /**
     * Plot action manager.
     */
    private final PlotActionManager actionManager;

    /**
     * Preferred frame size.
     */
    private static final Dimension PREFERRED_SIZE = new Dimension(500, 400);

    /**
     * The histogram panel. This panel contains most of the GUI code.
     */
    private final HistogramPanel cPanel;

    /**
     * Construct the GUI.
     *
     * @param frame     Generic Frame
     * @param component Histogram component
     */
    public HistogramDesktopComponent(final GenericFrame frame, final HistogramComponent component) {
        super(frame, component);
        setPreferredSize(PREFERRED_SIZE);
        actionManager = new PlotActionManager(this);
        setLayout(new BorderLayout());
        createAttachMenuBar();
        cPanel = new HistogramPanel(this.getModel());
        add("Center", cPanel);

    }

    /**
     * Creates the menu bar.
     */
    private void createAttachMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        for (Action action : actionManager.getOpenSavePlotActions()) {
            fileMenu.add(action);
        }
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createRenameAction(this));
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createCloseAction(this));

        // Not currently used
        JMenu editMenu = new JMenu("Edit");
        JMenuItem preferences = new JMenuItem("Preferences...");
        editMenu.add(preferences);

        JMenu helpMenu = new JMenu("Help");
        ShowHelpAction helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/plots/barChart.html");
        JMenuItem helpItem = new JMenuItem(helpAction);
        helpMenu.add(helpItem);

        bar.add(fileMenu);
        // bar.add(editMenu);
        bar.add(helpMenu);

        getParentFrame().setJMenuBar(bar);
    }

    /**
     * Return a reference to the underlying data.
     *
     * @return the histogram model.
     */
    public HistogramModel getModel() {
        return getWorkspaceComponent().getModel();
    }

}
