
package org.simbrain.network;

import java.util.ArrayList;
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

    /** Whether this network has changed since the last save. */
    private boolean changedSinceLastSave = false;

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

    /** Path to this network; used in persistence. */
    private String path = null;

    /** x coordinate of this network frame; used in persistence. */
    private int xpos;

    /** y coordinate of this network frame; used in persistence. */
    private int ypos;

    /** width  of this network frame; used in persistence. */
    private int theWidth;

    /** height of this network frame; used in persistence. */
    private int theHeight;

    /**
     * Create a new network frame.
     */
    public NetworkFrame() {

        super(DEFAULT_TITLE, RESIZEABLE, CLOSEABLE, MAXIMIZEABLE, ICONIFIABLE);

        networkPanel = new NetworkPanel();

        setContentPane(networkPanel);

        addInternalFrameListener(new NetworkFrameListener());
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
        return networkPanel.getWorkspace();
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
         * Network frame listener.
         */
        private class NetworkFrameListener
            extends InternalFrameAdapter
        {

            /** @see InternalFrameAdapter */
            public void internalFrameClosed(final InternalFrameEvent e) {
            }

            /** @see InternalFrameAdapter */
            public void internalFrameClosing(final InternalFrameEvent e) {
                Workspace workspace = getWorkspace();
                workspace.getNetworkList().remove(NetworkFrame.this);

                // Reset gauge if one is attached.
                GaugeFrame gauge = getWorkspace().getGaugeAssociatedWithNetwork(getTitle());
                if (gauge != null) {
                    gauge.reset();
                }

                NetworkFrame lastNetworkFrame = workspace.getLastNetwork();
                if (lastNetworkFrame != null) {
                    lastNetworkFrame.grabFocus();
                    workspace.repaint();
                }

                NetworkPreferences.setCurrentDirectory(getNetworkPanel().getCurrentDirectory());
                //        if (isChangedSinceLastSave()) {
                //            hasChanged();
                //        } else {
                //            dispose();
                //        }
                dispose();
            }
        }

    /**
     * Sets a path to this network in a manner which independent of OS.
     *
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
     * @return Returns the theHeight.
     */
    public int getTheHeight() {
        return theHeight;
    }


    /**
     * @param theHeight The theHeight to set.
     */
    public void setTheHeight(final int theHeight) {
        this.theHeight = theHeight;
    }


    /**
     * @return Returns the theWidth.
     */
    public int getTheWidth() {
        return theWidth;
    }


    /**
     * @param theWidth The theWidth to set.
     */
    public void setTheWidth(final int theWidth) {
        this.theWidth = theWidth;
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
     * For Castor.  Turn Component bounds into separate variables.
     */
    public void initBounds() {
        setXpos(this.getX());
        setYpos(this.getY());
        setTheWidth(this.getBounds().width);
        setTheHeight(this.getBounds().height);
    }

    /**
     * @return Returns the changedSinceLastSave.
     */
    public boolean isChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    /**
     * @param changedSinceLastSave The changedSinceLastSave to set.
     */
    public void setChangedSinceLastSave(final boolean changedSinceLastSave) {
        this.changedSinceLastSave = changedSinceLastSave;
    }
}