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
import java.text.NumberFormat;

import javax.swing.*;

import org.simbrain.util.StandardDialog;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.updater.*;

/**
 * Display updater and thread information.
 *
 * @author jyoshimi
 *
 */
public class PerformanceMonitorPanel extends JPanel {

    private static class UpdateActionTimer {

        private static NumberFormat format = NumberFormat.getNumberInstance();

        static {
            format.setMaximumFractionDigits(2);
        }

        private UpdateAction action;
        private long startNs;
        private long endNs;
        private double durationMs;
        private double maximumMs;
        private double averageMs;

        UpdateActionTimer(UpdateAction action) {
            this.action = action;
        }

        public boolean isTimingAction(UpdateAction action) {
            return this.action == action;
        }

        public void setStart(long value) {
            startNs = value;
        }

        public void setEnd(long value) {
            endNs = value;
            durationMs = (endNs - startNs) / 1.0e6;
            maximumMs = Math.max(durationMs, maximumMs);
            averageMs = (0.99 * averageMs) + (0.01 * durationMs);
        }

        @Override
        public String toString() {
            return String.format("%s:      %10sms      Max: %10sms      Ave: %10sms", action.getDescription(),
                    format.format(durationMs), format.format(maximumMs), format.format(averageMs));
        }
    }

    private JPanel contentPanel = new JPanel();

    private DefaultListModel<UpdateActionTimer> timersModel = new DefaultListModel<>();

    private DefaultListModel<String> threadsModel = new DefaultListModel<>();

    /** Reference to parent workspace. */
    private Workspace workspace;

    /** Number of update threads. */
    private JTextField updaterNumThreads = new JTextField();

    /**
     * Constructor for viewer panel.
     *
     * @param workspace reference to parent workspace.
     */
    public PerformanceMonitorPanel(Workspace workspace) {
        super(new BorderLayout());
        this.workspace = workspace;
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

        updateList();

        JList<UpdateActionTimer> timersList = new JList<>();
        timersList.setModel(timersModel);
        JScrollPane updateActionPane = new JScrollPane(timersList);
        contentPanel.add(updateActionPane);

        JList<String> threadsList = new JList<>();
        threadsList.setModel(threadsModel);
        JScrollPane threadPane = new JScrollPane(threadsList);
        contentPanel.add(threadPane);

        JButton showUpdateManager = new JButton("Update Manager");
        showUpdateManager.addActionListener(evt -> {
            StandardDialog dialog = new StandardDialog();
            dialog.setContentPane(new WorkspaceUpdateManagerPanel(workspace, dialog));
            dialog.setModal(true);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });

        JToolBar toolBar = new JToolBar();
        toolBar.add(showUpdateManager);

        toolBar.addSeparator();
        toolBar.add(new JLabel("Number of Threads: "));
        updaterNumThreads.setMaximumSize(new Dimension(100, 100));
        toolBar.add(updaterNumThreads);
        JButton setThreadsButton = new JButton("Set");
        setThreadsButton.addActionListener(evt ->
                workspace.getUpdater().setNumThreads(Integer.parseInt(updaterNumThreads.getText())));
        toolBar.add(setThreadsButton);
        toolBar.addSeparator();
        toolBar.add(new JLabel("Number of Processors: " + Runtime.getRuntime().availableProcessors()));
        updateStats();

        // Add main components to panel
        this.add("North", toolBar);
        this.add("Center", contentPanel);

        // Add updater component listener
        workspace.getUpdater().addComponentListener(
            new UpdateEventListener() {
                @Override
                public void beforeUpdateAction(UpdateAction action, long nanoTime) {
                    for (int i = 0; i < timersModel.getSize(); ++i) {
                        if (timersModel.get(i).isTimingAction(action)) {
                            timersModel.get(i).setStart(nanoTime);
                        }
                    }
                }

                @Override
                public void afterUpdateAction(UpdateAction action, long nanoTime) {
                    for (int i = 0; i < timersModel.getSize(); ++i) {
                        if (timersModel.get(i).isTimingAction(action)) {
                            timersModel.get(i).setEnd(nanoTime);
                        }
                    }
                }

                @Override
                public void beforeComponentUpdate(WorkspaceComponent component, int update, int thread, long nanoTime) {
                    String text = String.format("Thread %s: Started updating %s", thread, component.getName());
                    threadsModel.setElementAt(text, thread - 1);
                    contentPanel.repaint();
                }

                @Override
                public void afterComponentUpdate(WorkspaceComponent component, int update, int thread, long nanoTime) {
                    String text = String.format("Thread %s: Finished updating %s", thread, component.getName());
                    threadsModel.setElementAt(text, thread - 1);
                    contentPanel.repaint();
                }
            });

        // Add updater listener
        workspace.getUpdater().addUpdaterListener(
            new WorkspaceUpdaterListener() {
                public void updatedCouplings(int update) {
                    String text = String.format("Thread %s: Updated couplings", update);
                    threadsModel.setElementAt(text, update - 1);
                    contentPanel.repaint();
                }

                public void changedUpdateController() {
                    updateStats();
                }

                public void changeNumThreads() {
                    updateList();
                }

                // TODO: Should be some useful graphic thing to do when update begins and ends...
                public void updatingStarted() {
                }

                public void updatingFinished() {
                }

                public void workspaceUpdated() {
                    contentPanel.repaint();
                }
            });

        workspace.getUpdater().getUpdateManager().addListener(new UpdateActionManager.UpdateManagerListener() {
            @Override
            public void actionAdded(UpdateAction action) {
                timersModel.add(timersModel.getSize(), new UpdateActionTimer(action));
                actionOrderChanged();
            }

            @Override
            public void actionRemoved(UpdateAction action) {
                for (int i = 0; i < timersModel.getSize(); ++i) {
                    if (timersModel.get(i).isTimingAction(action)) {
                        timersModel.remove(i);
                        break;
                    }
                }
                actionOrderChanged();
            }

            @Override
            public void actionOrderChanged() {
                // TODO: Do something here
            }
        });
    }

    /**
     * Update thread viewer list.
     */
    private void updateList() {
        timersModel.clear();
        for (UpdateAction action : workspace.getUpdater().getUpdateManager().getActionList()) {
            timersModel.add(timersModel.getSize(), new UpdateActionTimer(action));
        }

        threadsModel.clear();
        for (int i = 1; i <= workspace.getUpdater().getNumThreads(); i++) {
            String text = "Thread " + i;
            threadsModel.add(threadsModel.getSize(), text);
        }
        updaterNumThreads.setText("" + workspace.getUpdater().getNumThreads());
    }

    /**
     * Update various labels and components reflecting update stats.
     */
    private void updateStats() {
        updaterNumThreads.setText("" + workspace.getUpdater().getNumThreads());
    }

}
