//TODO: When APE's updated with conditional logic handling migrate relevant logic from
// here to there. Then delete this class.
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
// package org.simbrain.network.gui.dialogs.synapse;
//
// import org.simbrain.network.core.Synapse;
// import org.simbrain.network.synapse_update_rules.spikeresponders.*;
// import org.simbrain.util.SimbrainConstants;
// import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
// import org.simbrain.util.propertyeditor2.EditableObject;
// import org.simbrain.util.widgets.DropDownTriangle;
// import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
// import org.simbrain.util.widgets.EditablePanel;
//
// import javax.swing.*;
// import javax.swing.border.Border;
// import javax.swing.border.TitledBorder;
// import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// import java.awt.event.MouseAdapter;
// import java.awt.event.MouseEvent;
// import java.util.*;
// import java.util.List;
// import java.util.stream.Collectors;
//
// /**
//  * Panel to display spike responder settings.
//  *
//  * @author Zoë Tosi
//  */
// public class SpikeResponderSettingsPanel extends JPanel {
//
//     /**
//      * The default display state of the synapse panel. Currently, True, that is,
//      * by default, the synapse panel corresponding to the rule in the combo box
//      * is visible.
//      */
//     private static final boolean DEFAULT_SP_DISPLAY_STATE = true;
//
//     /**
//      * Spike responder type combo box.
//      */
//     private final JComboBox<String> cbResponderType = new JComboBox<String>(RESPONDER_MAP.keySet()
//         .toArray(new String[RESPONDER_MAP.size()]));
//
//     /**
//      * The synapses being modified.
//      */
//     private final Collection<Synapse> synapseList;
//
//     /**
//      * Panel for editing spike responders.
//      */
//     private AnnotatedPropertyEditor spikeResponderPanel;
//
//     /**
//      * For showing/hiding the synapse panel.
//      */
//     private final DropDownTriangle displaySPTriangle;
//
//     /**
//      * The originally displayed abstract spike response panel. If the currently
//      * displayed panel is not the same as the starting panel, then we can be
//      * sure that we are not editing spike responders, but rather are replacing
//      * them.
//      */
//     private final AnnotatedPropertyEditor startingPanel;
//
//     /**
//      * The parent window containing this panel, access to which is required for
//      * resizing purposes.
//      */
//     private final Window parent;
//
//     /**
//      * A mapping of available spike responders to their respective panels. Used
//      * as a reference (especially for combo-boxes) by GUI classes.
//      */
//     public static final LinkedHashMap<String, AnnotatedPropertyEditor> RESPONDER_MAP = new LinkedHashMap<>();
//
//     static {
//         RESPONDER_MAP.put(new UDF().getDescription(), new AnnotatedPropertyEditor(new UDF()));
//         RESPONDER_MAP.put(new JumpAndDecay().getDescription(), new AnnotatedPropertyEditor(new JumpAndDecay()));
//         RESPONDER_MAP.put(new ConvolvedJumpAndDecay().getDescription(),
//             new AnnotatedPropertyEditor(new ConvolvedJumpAndDecay()));
//         RESPONDER_MAP.put(new ProbabilisticResponder().getDescription(),
//             new AnnotatedPropertyEditor(new ProbabilisticResponder()));
//         RESPONDER_MAP.put(new RiseAndDecay().getDescription(), new AnnotatedPropertyEditor(new RiseAndDecay()));
//         RESPONDER_MAP.put(new Step().getDescription(), new AnnotatedPropertyEditor(new Step()));
//         RESPONDER_MAP.put(SimbrainConstants.NONE_STRING , new AnnotatedPropertyEditor(Collections.EMPTY_LIST));
//     }
//
//     /**
//      * A constructor that sets up the panel in its default display state.
//      *
//      * @param synapseList the list of synapses, the spike responders of which will be
//      *                    displayed for edit
//      * @param parent      the parent window
//      */
//     public SpikeResponderSettingsPanel(final Collection<Synapse> synapseList, final Window parent) {
//         this(synapseList, DEFAULT_SP_DISPLAY_STATE, parent);
//     }
//
//     /**
//      * A constructor that sets up the panel with the spike response panel either
//      * hidden or displayed.
//      *
//      * @param synapseList   the list of synapses, the spike responders of which will be
//      *                      displayed for edit
//      * @param startingState whether or not the dropdown showing spike responder parameters
//      *                      should be visible by default.
//      * @param parent        the parent window
//      */
//     public SpikeResponderSettingsPanel(final Collection<Synapse> synapseList, final boolean startingState,
//                                        final Window parent) {
//         this.synapseList = synapseList;
//         this.parent = parent;
//         displaySPTriangle = new DropDownTriangle(UpDirection.LEFT, startingState, "Settings", "Settings", parent);
//
//         // Find out what panel we're starting with and fill it as necessary
//         initSpikeResponderType();
//         // After the starting spike responder panel has been initialized we can
//         // now assign it to the final variable startingPanel...
//         startingPanel = spikeResponderPanel;
//         initializeLayout();
//         addListeners();
//     }
//
//     /**
//      * Lays out this panel.
//      */
//     private void initializeLayout() {
//
//         this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//
//         Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);
//
//         JPanel topPanel = new JPanel();
//         topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
//         topPanel.add(cbResponderType);
//         topPanel.add(Box.createHorizontalGlue());
//         topPanel.add(displaySPTriangle);
//         topPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
//         topPanel.setBorder(padding);
//         this.add(topPanel);
//         this.add(Box.createRigidArea(new Dimension(0, 5)));
//
//         // The drop-down spike responder panel
//         spikeResponderPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
//         spikeResponderPanel.setBorder(padding);
//         spikeResponderPanel.setVisible(displaySPTriangle.isDown());
//         this.add(spikeResponderPanel);
//
//         // Border
//         TitledBorder tb2 = BorderFactory.createTitledBorder("Spike Responder");
//         this.setBorder(tb2);
//
//     }
//
//     /**
//      * Initialize the main synapse panel based on the type of the selected
//      * synapses.
//      */
//     private void initSpikeResponderType() {
//         Iterator<Synapse> synIter = synapseList.iterator();
//         Synapse firstSynapse = synIter.next();
//
//         boolean srcSpiking = synapseList.stream().anyMatch(s->{
//             if(s.getSource() != null) {
//                 return s.getSource().getUpdateRule().isSpikingNeuron();
//             }
//             return true;
//         });
//
//         if(!srcSpiking) {
//             cbResponderType.setSelectedItem(SimbrainConstants.NONE_STRING);
//             spikeResponderPanel = new AnnotatedPropertyEditor(Collections.EMPTY_LIST);
//             cbResponderType.setEnabled(false);
//             spikeResponderPanel.setEnabled(false);
//             return;
//         }
//
//         Class synRClass = firstSynapse.getSpikeResponder() == null ? null : firstSynapse.getSpikeResponder().getClass();
//         boolean discrepancy = !synapseList.stream().allMatch(s ->  {
//             if(s.getSpikeResponder() == null) {
//                 return synRClass==null;
//             } else {
//                 return s.getSpikeResponder().getClass().equals(synRClass);
//             }
//         } );
//
//         // If they are different types, display combo box as null
//         if (discrepancy) {
//             cbResponderType.addItem(SimbrainConstants.NULL_STRING);
//             cbResponderType.setSelectedIndex(cbResponderType.getItemCount() - 1);
//             // Simply to serve as an empty panel
//             spikeResponderPanel = new AnnotatedPropertyEditor(Collections.EMPTY_LIST);
//         } else {
//             if(firstSynapse.getSpikeResponder() == null) {
//                 cbResponderType.setSelectedItem(SimbrainConstants.NONE_STRING);
//                 spikeResponderPanel = new AnnotatedPropertyEditor(Collections.EMPTY_LIST);
//             } else {
//                 String responderName = firstSynapse.getSpikeResponder().getDescription();
//                 spikeResponderPanel = RESPONDER_MAP.get(responderName);
//                 List<EditableObject> responderList = synapseList.stream()
//                         .map(Synapse::getSpikeResponder)
//                         .collect(Collectors.toList());
//                 spikeResponderPanel.fillFieldValues(responderList);
//                 cbResponderType.setSelectedItem(responderName);
//             }
//         }
//     }
//
//     /**
//      * Adds the listeners to this dialog.
//      */
//     private void addListeners() {
//
//         // Respond to triangle drop down clicks
//         displaySPTriangle.addMouseListener(new MouseAdapter() {
//             @Override
//             public void mouseClicked(MouseEvent arg0) {
//                 spikeResponderPanel.setVisible(displaySPTriangle.isDown());
//                 repaint();
//                 parent.pack();
//             }
//         });
//
//         cbResponderType.addActionListener(new ActionListener() {
//
//             /**
//              * Update the spike responder's property panel.  If it's the panel
//              * that was started with then don't overwrite those values with
//              * default values.
//              */
//             @Override
//             public void actionPerformed(ActionEvent arg0) {
//                 spikeResponderPanel = RESPONDER_MAP.get(cbResponderType.getSelectedItem());
//
//                 // Is the current panel different from the starting panel?
//                 boolean replace = spikeResponderPanel != startingPanel;
//
//                 if (replace) {
//                     // If so we have to fill the new panel with default values
//                     spikeResponderPanel.fillDefaultValues();
//                 }
//
//                 // Tell the new panel whether it will have to replace
//                 // synapse update rules or edit them upon commit.
//                 // TODO: synapsePanel.setReplace(replace);
//
//                 repaintPanel();
//                 parent.pack();
//             }
//         });
//     }
//
//     /**
//      * Called to repaint the panel based on changes in the selected
//      * synapse type.
//      */
//     public void repaintPanel() {
//         removeAll();
//         initializeLayout();
//         repaint();
//     }
//
//
// //    /**
// //     * {@inheritDoc}
// //     * Also enables/disables all UI sub-components
// //     */
// //    public void setEnabled(boolean enabled) {
// //        super.setEnabled(enabled);
// //        cbResponderType.setEnabled(enabled);
// //        spikeResponderPanel.setEnabled(enabled);
// //        startingPanel.setEnabled(enabled);
// //    }
//
//     /**
//      * @return the name of the selected synapse update rule
//      */
//     public JComboBox<String> getCbSynapseType() {
//         return cbResponderType;
//     }
//
//
//     /**
//      * Commit changes to the panel.
//      *
//      * @return success or not, but does nothing now.
//      */
//     public boolean commitChanges() {
//
//         SpikeResponder selectedResponder = (SpikeResponder) spikeResponderPanel.getEditedObject();
//
//         if (selectedResponder == null && cbResponderType.getSelectedItem() != SimbrainConstants.NONE_STRING) {
//             return true;
//         }
//
//         if(cbResponderType.getSelectedItem() == SimbrainConstants.NONE_STRING) {
//             synapseList.stream().forEach(s->s.setSpikeResponder(null));
//             return true;
//         }
//
//         synapseList.stream().filter(s->{
//             if(s.getSpikeResponder() == null) {
//                 return  true;
//             } else {
//                 return !selectedResponder.getClass().equals(s.getSpikeResponder().getClass());
//             }
//         }).forEach(s->s.setSpikeResponder(selectedResponder.deepCopy()));
//
//
//         List<EditableObject> responderList = synapseList.stream()
//             .map(Synapse::getSpikeResponder)
//             .collect(Collectors.toList());
//         spikeResponderPanel.commitChanges(responderList);
//
//         return true;
//     }
//
// }
