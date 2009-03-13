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
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

public class ThreadViewerPanel extends JPanel {

    public ThreadViewerPanel(final Workspace workspace) {
        super(new BorderLayout());
        final JPanel threadViewer = new JPanel(new BorderLayout());
        final JList list = new JList();

        final ThreadListModel<ListItem> listModel = new ThreadListModel<ListItem>();

        // TODO: Is there a way to get the actual threads?
        for (int i = 1; i <= workspace.getWorkspaceUpdator().getNumThreads(); i++) {
            ListItem label = new ListItem("Thread " + i);
            listModel.add(label);
        }
        list.setModel(listModel);
        JScrollPane scrollPane = new JScrollPane(list);
        threadViewer.add(scrollPane);
        this.add(threadViewer);

        workspace.getWorkspaceUpdator().addListener(
                new org.simbrain.workspace.updator.WorkspaceUpdatorListener() {

                    public void finishedComponentUpdate(
                            WorkspaceComponent<?> component, int update,
                            int thread) {
                        listModel.getElementAt(thread - 1).setText(
                                "Thread " + thread + ": finished updating "
                                        + component.getName());
                        threadViewer.repaint();
                    }

                    public void startingComponentUpdate(
                            WorkspaceComponent<?> component, int update,
                            int thread) {
                        listModel.getElementAt(thread - 1).setText(
                                "Thread " + thread + ": starting to update"
                                        + component.getName());
                        threadViewer.repaint();
                    }

                    public void updatedCouplings(int update) {
                        listModel.getElementAt(update - 1).setText(
                                "Thread " + update + ": updating couplings");
                        threadViewer.repaint();
                    }

                });
    }

    /**
     * Simple holder for list items, to display thread state.
     */
    private class ListItem {

        /** Item text. */
        String text;

        public ListItem(String arg) {
            this.text = arg;
        }

        public String toString() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    /**
     * Simple List Model
     *
     * @param <ListItem>
     */
    private class ThreadListModel<ListItem> extends AbstractListModel {

        /* List items. */
        ArrayList<ListItem> list = new ArrayList<ListItem>();

        public void add(ListItem item) {
            list.add(item);
        }

        public ListItem getElementAt(int index) {
            return list.get(index);
        }

        public int getSize() {
            return list.size();
        }

    }

}
