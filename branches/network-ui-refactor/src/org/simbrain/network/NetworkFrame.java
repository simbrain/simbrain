
package org.simbrain.network;

import javax.swing.JMenuBar;
import javax.swing.JInternalFrame;

import org.simbrain.workspace.Workspace;

/**
 * Network frame.
 */
public final class NetworkFrame
    extends JInternalFrame {

    /** Workspace. */
    private Workspace workspace;

    /** Network panel. */
    private NetworkPanel networkPanel;

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
    public NetworkFrame() {

        super(DEFAULT_TITLE, RESIZEABLE, CLOSEABLE, MAXIMIZEABLE, ICONIFIABLE);

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
     * @return Returns the networkPanel.
     */
    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }


    /**
     * @param networkPanel The networkPanel to set.
     */
    public void setNetworkPanel(NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
    }


    /**
     * @return Returns the workspace.
     */
    public Workspace getWorkspace() {
        return workspace;
    }


    /**
     * @param workspace The workspace to set.
     */
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }
}