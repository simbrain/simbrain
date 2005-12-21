
package org.simbrain.network;

import java.util.Iterator;

import javax.swing.JMenuBar;
import javax.swing.JInternalFrame;

import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameAdapter;

import org.simbrain.gauge.GaugeFrame;

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

        addInternalFrameListener(new NetworkFrameListener());

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


    /**
     * Network frame listener.
     */
    private class NetworkFrameListener
        extends InternalFrameAdapter
    {

        /** @see InternalFrameAdapter */
        public void internalFrameClosed(final InternalFrameEvent e) {

            Workspace workspace = getWorkspace();

            workspace.getNetworkList().remove(NetworkFrame.this);

            // To prevent currently linked gauges from being updated
            for (Iterator i = workspace.getGauges(NetworkFrame.this).iterator(); i.hasNext(); ) {
                GaugeFrame gaugeFrame = (GaugeFrame) i.next();
                gaugeFrame.getGaugedVars().clear();
            }

            // reset CommandTargets
            NetworkFrame lastNetworkFrame = workspace.getLastNetwork();

            if (lastNetworkFrame != null) {
                lastNetworkFrame.grabFocus();
                workspace.repaint();
            }

            // networkPanel.resetNetwork();
            // NetworkPreferences.setCurrentDirectory(netPanel.getSerializer().getCurrentDirectory());
        }

        /** @see InternalFrameAdapter */
        public void internalFrameClosing(final InternalFrameEvent e) {
        
            dispose();
            //        if (isChangedSinceLastSave()) {
            //            hasChanged();
            //        } else {
            //            dispose();
            //        }
        }
    }
}