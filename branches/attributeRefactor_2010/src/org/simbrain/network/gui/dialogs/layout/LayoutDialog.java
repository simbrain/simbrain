/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.network.gui.dialogs.layout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


/**
 * <b>LayoutPanel</b> allows the user to define the layout of a network.
 */
public class LayoutDialog extends StandardDialog implements ActionListener {

    /** The current layout panel. */
    private AbstractLayoutPanel layoutPanel;

    /** Top panel. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** Main panel. */
    private JPanel mainPanel = new JPanel();

    /** Array of layout panels available to a given network type.*/
    private Layout[] layouts = new Layout[]{new LineLayout(), new GridLayout(),
            new HexagonalGridLayout()};

    /** Layouts combo box. */
    private JComboBox cbLayouts = new JComboBox(layouts);


    /**
     * Constructor for creating independent dialog.
     */
    public LayoutDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        cbLayouts.addActionListener(this);
        topPanel.addItem("Layout Style", cbLayouts);
        panel.add("North", topPanel);
        initPanel();
//        layoutPanel = layouts[0];
        mainPanel.add(layoutPanel);
        panel.add("Center", mainPanel);
        setContentPane(panel);
    }

    /**
     * Initialize the layout panel based upon the current layout type.
     */
    private void initPanel() {
        Layout currentLayout = (Layout) cbLayouts.getSelectedItem();
        if (currentLayout instanceof LineLayout) {
            clearOptionsPanel();
            layoutPanel = new LineLayoutPanel();
            layoutPanel.fillFieldValues();
            mainPanel.add(layoutPanel);
        } else if (currentLayout instanceof GridLayout) {
            clearOptionsPanel();
            layoutPanel = new GridLayoutPanel();
            layoutPanel.fillFieldValues();
            mainPanel.add(layoutPanel);
        } else if (currentLayout instanceof HexagonalGridLayout) {
            clearOptionsPanel();
            layoutPanel = new HexagonalGridLayoutPanel();
            layoutPanel.fillFieldValues();
            mainPanel.add(layoutPanel);
        }
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Remove current panel, if any.
     */
    private void clearOptionsPanel() {
        if (layoutPanel != null) {
            mainPanel.remove(layoutPanel);
        }
    }

    /** @see StandardDialog */
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /** @see AbstractLayoutPanel */
    public void commitChanges() {
        layoutPanel.commitChanges();
        
    }

    /** @see ActionListener */
    public void actionPerformed(final ActionEvent arg0) {
        initPanel();
    }

    /**
     * Returns the layout that was built using this panel.
     *
     * @return the new layout
     */
    public Layout getNeuronLayout() {
     return layoutPanel.getNeuronLayout();
    }

}
