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
package org.simbrain.network.gui.dialogs.network.layout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.simbrain.network.layouts.Layout;
import org.simbrain.util.LabelledItemPanel;


/**
 * <b>LayoutPanel</b> allows the user to define the layout of a network.
 */
public class LayoutPanel extends JPanel implements ActionListener {

    /** Layouts combo box. */
    private JComboBox cbLayouts = new JComboBox();

    /** The current layout panel. */
    private AbstractLayoutPanel layoutPanel;

    /** Top panel. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** Main panel. */
    private JPanel mainPanel = new JPanel();

    /** Array of layout panels available to a given network type.*/
    private AbstractLayoutPanel[] layouts;

    /** Parent dialog pane. */
    private JDialog parentDialog;

    /**
     * Constructor.
     *
     * @param parentDialog Dialog calling layout panel
     * @param layouts list of layouts available to a network type.
     */
    public LayoutPanel(final JDialog parentDialog, final AbstractLayoutPanel[] layouts) {
        this.parentDialog = parentDialog;
        this.setLayout(new BorderLayout());
        this.layouts = layouts;

        for (int i = 0; i < layouts.length; i++) {
            AbstractLayoutPanel layout = layouts[i];
            cbLayouts.addItem(layout.getNeuronLayout().getLayoutName());
        }
        cbLayouts.addActionListener(this);
        topPanel.addItem("Layout Style", cbLayouts);
        this.add("North", topPanel);
        layoutPanel = layouts[0];
        mainPanel.add(layoutPanel);
        this.add("Center", mainPanel);
    }

    /** @see ActionListener */
    public void actionPerformed(final ActionEvent arg0) {

        for (int i = 0; i < layouts.length; i++) {
            AbstractLayoutPanel layout = layouts[i];
            if (cbLayouts.getSelectedItem().equals(layout.getNeuronLayout().getLayoutName())) {
                mainPanel.remove(layoutPanel);
                layoutPanel  = layout;
                mainPanel.add(layoutPanel);
                break;
            }
        }
        parentDialog.pack();
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
