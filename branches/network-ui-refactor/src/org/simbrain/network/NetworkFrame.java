
package org.simbrain.network;

import javax.swing.JMenuBar;
import javax.swing.JInternalFrame;

import org.simbrain.workspace.Workspace;

/**
 * Network frame.
 */
public final class NetworkFrame
    extends JInternalFrame {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Workspace. */
    private final Workspace workspace;

    /** Resizeable flag. */
    private static final boolean RESIZEABLE = true;

    /** Closeable flag. */
    private static final boolean CLOSEABLE = true;

    /** Maximizeable flag. */
    private static final boolean MAXIMIZEABLE = true;

    /** Iconifiable flag. */
    private static final boolean ICONIFIABLE = true;

    /** Default title. */
    private static final String DEFAULT_TITLE = "Title";


    /**
     * Create a new network frame.
     */
    public NetworkFrame(final Workspace workspace) {

        super(DEFAULT_TITLE, RESIZEABLE, CLOSEABLE, MAXIMIZEABLE, ICONIFIABLE);

        this.workspace = workspace;
        networkPanel = new NetworkPanel();

        setContentPane(networkPanel);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        createAndAttachMenus();
    }


    /**
     * Create and attach the menus for this network frame.
     */
    private void createAndAttachMenus() {

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(networkPanel.createFileMenu());
        menuBar.add(networkPanel.createEditMenu());
        menuBar.add(networkPanel.createGaugeMenu());
        menuBar.add(networkPanel.createHelpMenu());
        setJMenuBar(menuBar);
    }

    /**
     * Return the network panel for this network frame.
     *
     * @return the network panel for this network frame
     */
    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    /**
     * Return the workspace for this network frame.
     *
     * @return the workspace for this network frame
     */
    public Workspace getWorkspace() {
        return workspace;
    }

}