/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.layout;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.util.StandardDialog;

/**
 * <b>LayoutPanel</b> allows the user to define the layout of a network.
 */
public class LayoutDialog extends StandardDialog {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The default initial layout. */
    private static final Layout DEFAULT_LAYOUT = new GridLayout();

    /** Main panel. */
    private MainLayoutPanel mainPanel;

    /** The network panel where layout will occur. */
    private final NetworkPanel networkPanel;

    /**
     * Constructor for creating dialog.
     *
     * @param networkPanel the networkPanel where layout will occur
     */
    public LayoutDialog(final NetworkPanel networkPanel) {
        this(DEFAULT_LAYOUT, networkPanel);
    }

    /**
     * Constructor for creating independent dialog.
     *
     * @param layout the layout to show
     * @param networkPanel the networkPanel where layout will occur
     */
    public LayoutDialog(final Layout layout, final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        mainPanel = new MainLayoutPanel(layout.getDescription(), false, this);
        setContentPane(mainPanel);
    }

    /**
     * {@inheritDoc}
     */
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
        mainPanel.getCurrentLayout().setInitialLocation(
                networkPanel.getLastClickedPosition()); //TODO: Think
        mainPanel.getCurrentLayout().layoutNeurons(
                networkPanel.getSelectedModelNeurons());
        networkPanel.repaint();
    }

    /** @see AbstractLayoutPanel */
    public void commitChanges() {
        mainPanel.commitChanges();
    }

}
