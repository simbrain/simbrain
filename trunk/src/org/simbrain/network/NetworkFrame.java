/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.actions.AddGaugeAction;
import org.simbrain.workspace.Workspace;

/**
 * Network frame.
 */
public final class NetworkFrame
    extends JInternalFrame implements MenuListener {

    /** Network panel. */
    private final NetworkPanel networkPanel;

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

    /** Container for toolbars. */
    private JPanel toolbars = new JPanel();

    /**
     * Create a new network frame.
     */
    public NetworkFrame() {

        super(DEFAULT_TITLE, RESIZEABLE, CLOSEABLE, MAXIMIZEABLE, ICONIFIABLE);

        networkPanel = new NetworkPanel();

        // place networkPanel in a buffer so that toolbars don't get in the way of canvas elements
        JPanel buffer = new JPanel();
        buffer.setLayout(new BorderLayout());

        // Construct toolbar pane
        FlowLayout flow = new FlowLayout(FlowLayout.LEFT);
        flow.setHgap(0);
        flow.setVgap(0);
        toolbars.setLayout(flow);
        toolbars.add(networkPanel.getMainToolBar());
        toolbars.add(networkPanel.getEditToolBar());
        toolbars.add(networkPanel.getClampToolBar());

        // Put it all together
        buffer.add("North", toolbars);
        buffer.add("Center", networkPanel);
        setContentPane(buffer);

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
        menuBar.add(networkPanel.createInsertMenu());
        menuBar.add(networkPanel.createViewMenu());
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
     * Return the path for this network frame.  Used in persistence.
     *
     * @return the path for this network frame
     */
    public String getPath() {
        return path;
    }

    /**
     * Return the platform-specific path for this network frame.  Used in persistence.
     *
     * @return the platform-specific path for this network frame
     */
    // note method name doesn't match doc
    public String getGenericPath() {
        String ret = path;

        if (path == null) {
            return null;
        }

        ret.replace('/', System.getProperty("file.separator").charAt(0));

        return ret;
    }

    /**
     * Sets a path to this network in a manner independent of OS.  Used in persistence.
     *
     * @param path the path for this network frame
     */
    public void setPath(final String path) {
        String thePath = path;

        if (thePath.length() > 2) {
            if (thePath.charAt(2) == '.') {
                thePath = path.substring(2, path.length());
            }
        }

        thePath = thePath.replace(System.getProperty("file.separator").charAt(0), '/');
        this.path = thePath;
    }

    /**
     * Return the "theHeight" for this network frame.  Persistence
     * requires the dimensions of this component's bounds to be accessible
     * individually.
     *
     * @return the "theHeight" for this network frame
     */
    public int getTheHeight() {
        return theHeight;
    }

    /**
     * Set the "theHeight" for this network frame to <code>theHeight</code>.
     * Persistence requires the dimensions of this component's bounds to be
     * accessible individually.
     *
     * @param theHeight the "theHeight" for this network frame
     */
    public void setTheHeight(final int theHeight) {
        this.theHeight = theHeight;
    }

    /**
     * Return the "theWidth" for this network frame.  Persistence
     * requires the dimensions of this component's bounds to be accessible
     * individually.
     *
     * @return the "theWidth" for this network frame
     */
    public int getTheWidth() {
        return theWidth;
    }

    /**
     * Set the "theWidth" for this network frame to <code>theHeight</code>.
     * Persistence requires the dimensions of this component's bounds to be
     * accessible individually.
     *
     * @param theWidth the "theWidth" for this network frame
     */
    public void setTheWidth(final int theWidth) {
        this.theWidth = theWidth;
    }

    /**
     * Return the "xpos" for this network frame.  Persistence
     * requires the dimensions of this component's bounds to be accessible
     * individually.
     *
     * @return the "xpos" for this network frame
     */
    public int getXpos() {
        return xpos;
    }

    /**
     * Set the "xpos" for this network frame to <code>xpos</code>.
     * Persistence requires the dimensions of this component's bounds to be
     * accessible individually.
     *
     * @param xpos the "xpos" for this network frame
     */
    public void setXpos(final int xpos) {
        this.xpos = xpos;
    }

    /**
     * Return the "ypos" for this network frame.  Persistence
     * requires the dimensions of this component's bounds to be accessible
     * individually.
     *
     * @return the "ypos" for this network frame
     */
    public int getYpos() {
        return ypos;
    }

    /**
     * Set the "ypos" for this network frame to <code>ypos</code>.
     * Persistence requires the dimensions of this component's bounds to be
     * accessible individually.
     *
     * @param ypos the "ypos" for this network frame
     */
    public void setYpos(final int ypos) {
        this.ypos = ypos;
    }

    /**
     * Initialize individual dimension properties from this component's
     * bounds.  The bounds are split into properties <code>xpos</code>,
     * <code>ypos</code>, <code>theWidth</code>, and <code>theHeight</code>
     * for <code>(x, y, w, h)</code> respectively.
     */
    public void initBounds() {
        setXpos(getX());
        setYpos(getY());
        setTheWidth(getBounds().width);
        setTheHeight(getBounds().height);
    }

    /** @see MenuListener */
    public void menuSelected(final MenuEvent me) {
        // This is here mainly to handle adding gauge menus
        // This should be refactored when the global workspace interactions are.
        if (me.getSource() instanceof JMenu) {
            if (getWorkspace().getGaugeList().size() > 0) {
                JMenu gaugeSubMenu = getWorkspace().getGaugeMenu(networkPanel);
                JMenu gaugeMenu = (JMenu) me.getSource();
                gaugeMenu.removeAll();
                gaugeMenu.add(new AddGaugeAction(networkPanel));
                gaugeMenu.add(gaugeSubMenu);
            }
        }
    }

    /** @see MenuListener */
    public void menuDeselected(final MenuEvent arg0) {
        // empty
    }

    /** @see MenuListener */
    public void menuCanceled(final MenuEvent arg0) {
        // empty
    }

    /**
     * Network frame listener.
     */
    private class NetworkFrameListener extends InternalFrameAdapter {

        /** @see InternalFrameAdapter */
        public void internalFrameClosing(final InternalFrameEvent e) {
            Workspace workspace = getWorkspace();
            workspace.getNetworkList().remove(NetworkFrame.this);

            // Reset gauge if one is attached.
            GaugeFrame gauge = getWorkspace().getGaugeAssociatedWithNetwork(getTitle());
            if (gauge != null) {
                gauge.reset();
            }

            // Perform network close operations.
            getNetworkPanel().closeNetwork();

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
}