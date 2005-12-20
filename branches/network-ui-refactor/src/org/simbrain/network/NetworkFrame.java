
package org.simbrain.network;

import java.util.ArrayList;

import javax.swing.JMenuBar;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.simbrain.gauge.GaugeFrame;
import org.simbrain.workspace.Workspace;

/**
 * Network frame.
 */
public final class NetworkFrame
    extends JInternalFrame implements InternalFrameListener{

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

        this.addInternalFrameListener(this);
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

    public void internalFrameClosed(final InternalFrameEvent e) {
        //networkPanel.resetNetwork();
        this.getWorkspace().getNetworkList().remove(this);

        // To prevent currently linked gauges from being updated
        ArrayList gauges = this.getWorkspace().getGauges(this);

        for (int i = 0; i < gauges.size(); i++) {
            ((GaugeFrame) gauges.get(i)).getGaugedVars().clear();
        }

        //resentCommandTargets
        NetworkFrame net = workspace.getLastNetwork();

        if (net != null) {
            net.grabFocus();
            getWorkspace().repaint();
        }

       // NetworkPreferences.setCurrentDirectory(netPanel.getSerializer().getCurrentDirectory());
    }

    public void internalFrameOpened(final InternalFrameEvent e) {
    }

    public void internalFrameClosing(final InternalFrameEvent e) {
        
        dispose();
        //        if (isChangedSinceLastSave()) {
        //            hasChanged();
        //        } else {
        //            dispose();
        //        }
    }

    public void internalFrameIconified(final InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(final InternalFrameEvent e) {
    }

    public void internalFrameActivated(final InternalFrameEvent e) {
    }

    public void internalFrameDeactivated(final InternalFrameEvent e) {
    }
}