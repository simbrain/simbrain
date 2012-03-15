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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>LayoutPanel</b> allows the user to define the layout of a network.
 */
public class LayoutDialog extends StandardDialog {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The current layout panel. */
    private AbstractLayoutPanel layoutPanel;

    /** Top panel. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** Main panel. */
    private JPanel mainPanel = new JPanel();

    /** Array of layout panels available to a given network type. */
    private Layout[] layouts = { new LineLayout(), new GridLayout(),
            new HexagonalGridLayout() };

    /** Layouts combo box. */
    private JComboBox cbLayouts;

    /** The current Layout. */
    private static Layout currentLayout = new GridLayout();

    /** The network panel where layout will occur. */
    private final NetworkPanel networkPanel;

    /**
     * Constructor for creating dialog.
     *
     * @param networkPanel the networkPanel where layout will occur
     */
    public LayoutDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        initPanel();
    }

    /**
     * Constructor for creating independent dialog.
     *
     * @param layout the layout to show
     * @param networkPanel the networkPanel where layout will occur
     */
    public LayoutDialog(final Layout layout, final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        currentLayout = layout;
        initPanel();
    }

    /**
     * Initialize the layout panel based upon the current layout type.
     */
    private void initPanel() {
        JPanel panel = new JPanel();
        setTitle("Set Layout Properties");
        panel.setLayout(new BorderLayout());
        cbLayouts = new JComboBox(layouts);
        if (currentLayout instanceof LineLayout) {
            cbLayouts.setSelectedIndex(0);
        } else if (currentLayout instanceof GridLayout) {
            cbLayouts.setSelectedIndex(1);
        } else if (currentLayout instanceof HexagonalGridLayout) {
            cbLayouts.setSelectedIndex(2);
        }
        cbLayouts.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                initLayoutTypePanel();
            }
        });
        topPanel.addItem("Layout Style", cbLayouts);
        panel.add("North", topPanel);
        panel.add("Center", mainPanel);
        initLayoutTypePanel();

        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Initialize the layout type panel.
     */
    private void initLayoutTypePanel() {
        if (layoutPanel != null) {
            mainPanel.remove(layoutPanel);
        }
        currentLayout = (Layout) cbLayouts.getSelectedItem();
        if (currentLayout instanceof LineLayout) {
            layoutPanel = new LineLayoutPanel((LineLayout) currentLayout);
            layoutPanel.fillFieldValues();
            mainPanel.add(layoutPanel);
        } else if (currentLayout instanceof GridLayout) {
            layoutPanel = new GridLayoutPanel((GridLayout) currentLayout);
            layoutPanel.fillFieldValues();
            mainPanel.add(layoutPanel);
        } else if (currentLayout instanceof HexagonalGridLayout) {
            layoutPanel = new HexagonalGridLayoutPanel(
                    (HexagonalGridLayout) currentLayout);
            layoutPanel.fillFieldValues();
            mainPanel.add(layoutPanel);
        }
        pack();
    }

    /** @see StandardDialog */
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
        currentLayout.setInitialLocation(networkPanel.getLastClickedPosition());
        currentLayout.layoutNeurons(networkPanel.getSelectedModelNeurons());
        networkPanel.repaint();
    }

    /** @see AbstractLayoutPanel */
    public void commitChanges() {
        layoutPanel.commitChanges();
    }

    /**
     * Get the current layout.
     *
     * @return the current layout
     */
    public static Layout getCurrentLayout() {
        return currentLayout;
    }

    /**
     * Set the current layout.
     *
     * @param layout the new layout
     */
    public static void setCurrentLayout(Layout layout) {
        currentLayout = layout;
    }

}
