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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.updator.WorkspaceUpdatorListener;

/**
 * Display updator and thread information.
 * 
 * @author jyoshimi
 * 
 */
public class ThreadViewerPanel extends JPanel {

    /** Thread viewer panel. */
	private JPanel threadViewer = new JPanel(new BorderLayout());

    /** Thread viewer panel. */
	private JToolBar topStatsPanel= new JToolBar();

    /** List. */
    private JList list = new JList();
    
    /** List model. */
    private ThreadListModel<ListItem> listModel = new ThreadListModel<ListItem>();
    
    /** Label for update method name. */
    private JLabel updateName = new JLabel();
    
    /** Thread viewer scroll pane. */
    JScrollPane scrollPane = null;
    
    /** Reference to parent workspace. */
    private Workspace workspace;
    
    /** Number of update threads. */
    private JTextField updatorNumThreads = new JTextField();

    /**
     * Constructor for viewer panel.
     *
     * @param workspace reference to parent workspace.
     */
    public ThreadViewerPanel(final Workspace workspace) {

        super(new BorderLayout());
        this.workspace = workspace;

        // Set up thread viewer
        updateList();
        scrollPane = new JScrollPane(list);
        threadViewer.add(scrollPane);

        // Set up top statistics panel
        topStatsPanel.add(updateName);
        topStatsPanel.addSeparator(new Dimension(50,10));
        topStatsPanel.add(new JLabel("Number of Threads: "));
        updatorNumThreads.setMaximumSize(new Dimension(100,100));
        topStatsPanel.add(updatorNumThreads);        
        JButton setThreadsButton = new JButton("Set");
        setThreadsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				workspace.getWorkspaceUpdator().setNumThreads(Integer.parseInt(updatorNumThreads.getText()));
			}
        	
        });
        topStatsPanel.add(setThreadsButton);
        topStatsPanel.addSeparator(new Dimension(50,10));
        topStatsPanel.add(new JLabel("Number of Processors: " + Runtime.getRuntime().availableProcessors()));
        updateStats();

        // Add main components to panel
        this.add("North", topStatsPanel);
        this.add("Center", threadViewer);

        // Add workspace updator listener
        workspace.getWorkspaceUpdator().addListener(
                new WorkspaceUpdatorListener() {

                	/**
                	 * {@inheritDoc}
                	 */
                    public void finishedComponentUpdate(
                            WorkspaceComponent component, int update,
                            int thread) {
                        listModel.getElementAt(thread - 1).setText(
                                "Thread " + thread + ": finished updating "
                                        + component.getName());
                        threadViewer.repaint();
                    }

                	/**
                	 * {@inheritDoc}
                	 */
                    public void startingComponentUpdate(
                            WorkspaceComponent component, int update,
                            int thread) {
                        listModel.getElementAt(thread - 1).setText(
                                "Thread " + thread + ": starting to update"
                                        + component.getName());
                        threadViewer.repaint();
                    }

                	/**
                	 * {@inheritDoc}
                	 */
                    public void updatedCouplings(int update) {
                        listModel.getElementAt(update - 1).setText(
                                "Thread " + update + ": updating couplings");
                        threadViewer.repaint();
                    }

                	/**
                	 * {@inheritDoc}
                	 */
					public void changedUpdateController() {
						updateStats();
					}

                	/**
                	 * {@inheritDoc}
                	 */
					public void changeNumThreads() {
						updateList();
					}

                    /**
                     * {@inheritDoc}
                     */
                    public void updatedWorkspace() {
                        // TODO Auto-generated method stub
                    }

                });
    }

    /**
     * Update thread viewer list. 
     */
    private void updateList() {
    	listModel = new ThreadListModel<ListItem>();
    	for (int i = 1; i <= workspace.getWorkspaceUpdator().getNumThreads(); i++) {
            ListItem label = new ListItem("Thread " + i);
            listModel.add(label);
        }
        list.setModel(listModel);
        updatorNumThreads.setText("" + workspace.getWorkspaceUpdator().getNumThreads());
    }
    
	/**
	 * Update the labels on the thread viewer panel, of which there is currently
	 * just one.
	 */
    private void updateStats() {
        updateName.setText("Current updator: "
                + workspace.getWorkspaceUpdator().getCurrentUpdatorName());    	
        updatorNumThreads.setText("" + workspace.getWorkspaceUpdator().getNumThreads());
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
     * Simple List Model.
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
