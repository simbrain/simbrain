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
package org.simbrain.workspace.gui;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Displays a list of all currently open workspace components.
 *
 */
public class WorkspaceComponentListPanel extends JPanel {

    /** List of open components. */
    private JList componentList = new JList();

    /**
     * Workspace component list panel constructor.
     * @param desktop reference.
     */
    public WorkspaceComponentListPanel(final SimbrainDesktop desktop) {
        super(new BorderLayout());

        componentList.setListData(new Vector(desktop.getWorkspace().getComponentList()));

        JScrollPane scrollPane = new JScrollPane(componentList);

        add(scrollPane);
    }
}
