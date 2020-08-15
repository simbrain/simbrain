// /*
//  * Part of Simbrain--a java-based neural network kit
//  * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
//  *
//  * This program is free software; you can redistribute it and/or modify
//  * it under the terms of the GNU General Public License as published by
//  * the Free Software Foundation; either version 2 of the License, or
//  * (at your option) any later version.
//  *
//  * This program is distributed in the hope that it will be useful,
//  * but WITHOUT ANY WARRANTY; without even the implied warranty of
//  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  * GNU General Public License for more details.
//  *
//  * You should have received a copy of the GNU General Public License
//  * along with this program; if not, write to the Free Software
//  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//  */
// package org.simbrain.network.gui.dialogs.network;
//
// import org.simbrain.network.gui.NetworkPanel;
// import org.simbrain.network.gui.trainer.IterativeControlsPanel;
// import org.simbrain.network.gui.trainer.subnetworkTrainingPanels.LMSOfflineControlPanel;
// import org.simbrain.network.subnetworks.LMSNetwork;
// import org.simbrain.network.trainers.LMSIterative;
// import org.simbrain.network.trainers.LMSOffline;
//
// import javax.swing.*;
// import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// import java.awt.event.MouseAdapter;
// import java.awt.event.MouseEvent;
//
// /**
//  * <b>LMSEditorDialog</b> is a dialog box for editing a LMS network.
//  */
// public class LMSEditorDialog extends SupervisedTrainingDialog {
//
//     /**
//      * Reference to the lms network being edited.
//      */
//     private LMSNetwork lms;
//
//     /**
//      * Reference to iterative trainer.
//      */
//     private LMSIterative trainer;
//
//     /**
//      * Default constructor.
//      *
//      * @param np  parent panel
//      * @param lms edited network
//      */
//     public LMSEditorDialog(final NetworkPanel np, final LMSNetwork lms) {
//         super(np, lms);
//         this.lms = lms;
//         init();
//         initDefaultTabs();
//     }
//
//     /**
//      * This method initializes the components on the panel.
//      */
//     private void init() {
//         setTitle("Edit LMS Network");
//
//         // Set up combo box
//         String[] lmsTypes = {"Iterative", "Offline"};
//         final JComboBox<String> selectType = new JComboBox<String>(lmsTypes);
//         Box cbHolder = Box.createHorizontalBox();
//         cbHolder.add(Box.createHorizontalGlue());
//         cbHolder.add(new JLabel("Select training type: "));
//         cbHolder.add(selectType);
//         cbHolder.add(Box.createHorizontalGlue());
//
//         // Main vertical box
//         Box trainerPanel = Box.createVerticalBox();
//         trainerPanel.setOpaque(true);
//         trainerPanel.add(Box.createVerticalStrut(5));
//         trainerPanel.add(cbHolder);
//         trainerPanel.add(Box.createVerticalStrut(5));
//         final JPanel trainerContainer = new JPanel();
//         trainerContainer.setLayout(new BorderLayout());
//         JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
//         trainerPanel.add(separator);
//         trainerPanel.add(trainerContainer);
//
//         // Add to tabbed pane
//         addTab("Train", trainerPanel);
//         updateComboBox(selectType, trainerContainer);
//
//         selectType.addActionListener(new ActionListener() {
//
//             @Override
//             public void actionPerformed(ActionEvent e) {
//                 updateComboBox(selectType, trainerContainer);
//             }
//
//         });
//
//     }
//
//     /**
//      * Update trainer panel based on the combo box.
//      *
//      * @param selectType       the combo box
//      * @param trainerContainer the container
//      */
//     private void updateComboBox(JComboBox<String> selectType, final JPanel trainerContainer) {
//         trainerContainer.removeAll();
//         if (selectType.getSelectedIndex() == 0) {
//             trainer = new LMSIterative(lms);
//             IterativeControlsPanel iterativeControls = new IterativeControlsPanel(trainer);
//             trainerContainer.add(iterativeControls, BorderLayout.CENTER);
//             trainerContainer.setPreferredSize(iterativeControls.getPreferredSize());
//         } else {
//             LMSOffline trainer = new LMSOffline(lms);
//             final LMSOfflineControlPanel offlineControls = new LMSOfflineControlPanel(trainer, this);
//             offlineControls.addMouseListenerToTriangle(new MouseAdapter() {
//                 public void mouseClicked(MouseEvent me) {
//                     trainerContainer.setPreferredSize(offlineControls.getPreferredSize());
//                 }
//             });
//             offlineControls.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
//             trainerContainer.add(offlineControls, BorderLayout.CENTER);
//             trainerContainer.setPreferredSize(offlineControls.getPreferredSize());
//
//         }
//         trainerContainer.revalidate();
//         trainerContainer.repaint();
//         repaint();
//         pack();
//         setLocationRelativeTo(null);
//     }
//
//     @Override
//     protected void stopTrainer() {
//         if (trainer != null) {
//             trainer.setUpdateCompleted(true);
//         }
//     }
// }
