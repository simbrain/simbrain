// package org.simbrain.network.gui.dialogs.dl4j;
//
// import org.simbrain.network.dl4j.MultiLayerNet;
// import org.simbrain.network.gui.NetworkPanel;
// import org.simbrain.util.StandardDialog;
// import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
//
// import javax.swing.*;
//
// public class MultiLayerNetCreationDialog extends StandardDialog {
//
//
//     public MultiLayerNetCreationDialog(NetworkPanel np) {
//
//         super();
//
//         JPanel mainPanel = new JPanel();
//         mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
//         this.setContentPane(mainPanel);
//
//         // Create main network
//         MultiLayerNet.CreationTemplate creationTemplate = new MultiLayerNet.CreationTemplate();
//         AnnotatedPropertyEditor netCreator = new AnnotatedPropertyEditor(creationTemplate);
//         mainPanel.add(netCreator);
//         mainPanel.add(new JSeparator(JSeparator.HORIZONTAL));
//
//         // Hidden layer template
//         MultiLayerNet.LayerCreationTemplate lct = new MultiLayerNet.LayerCreationTemplate();
//         AnnotatedPropertyEditor layerCreator = new AnnotatedPropertyEditor(lct);
//         mainPanel.add(layerCreator);
//         mainPanel.add(new JSeparator(JSeparator.HORIZONTAL));
//
//         // Output layer template
//         MultiLayerNet.OutputLayerCreationTemplate oct = new MultiLayerNet.OutputLayerCreationTemplate();
//         AnnotatedPropertyEditor outputCreator = new AnnotatedPropertyEditor(oct);
//         mainPanel.add(outputCreator);
//
//         // Handle closing
//         this.addClosingTask( () -> SwingUtilities.invokeLater(() -> {
//             netCreator.commitChanges();
//             layerCreator.commitChanges();
//             outputCreator.commitChanges();
//             MultiLayerNet multiLayerNetwork = creationTemplate.create(np.getNetwork(), lct, oct);
//             np.getPlacementManager().addNewModelObject(multiLayerNetwork);
//             np.getNetwork().addDL4JMultiLayerNetwork(multiLayerNetwork);
//         }));
//     }
// }
