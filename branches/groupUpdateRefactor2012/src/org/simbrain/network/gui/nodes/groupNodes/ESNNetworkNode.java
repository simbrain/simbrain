package org.simbrain.network.gui.nodes.groupNodes;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.ESNTrainingPanel;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.trainer.DataViewer.DataHolder;
import org.simbrain.network.gui.trainer.LMSOfflinePanel;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;

/**
 * PNode representation of an Echo State Network.
 */
public class ESNNetworkNode extends SubnetGroupNode {
	
	
	/**
     * Create an ESN network
     *
     * @param networkPanel parent panel
     * @param group the ESN
     */
    public ESNNetworkNode(NetworkPanel networkPanel, EchoStateNetwork group) {
        super(networkPanel, group);
        setInteractionBox(new ESNInteractionBox(networkPanel));
        setContextMenu();
    }
    
    /**
     * Custom interaction box for ESN group node.
     */
    private class ESNInteractionBox extends InteractionBox {
        public ESNInteractionBox(NetworkPanel net) {
            super(net, ESNNetworkNode.this);
        }

        @Override
        protected JDialog getPropertyDialog() {
            ReflectivePropertyEditor editor = new ReflectivePropertyEditor();
            editor.setUseSuperclass(false);
            editor.setObject((EchoStateNetwork) getGroup());
            JDialog dialog = editor.getDialog();
            return dialog;
        }
        
      @Override
      protected boolean hasPropertyDialog() {
          return true;
      }

        @Override
        protected String getToolTipText() {
            return "ESN...";
        }

        @Override
        protected boolean hasToolTipText() {
            return true;
        }

    };

    /**
     * Sets custom menu.
     */
    private void setContextMenu() {
    	JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        menu.add(new JMenuItem(trainOfflineAction));
        menu.addSeparator();
        
        final EchoStateNetwork esn = (EchoStateNetwork) getGroup();
        
		// Reference to the input data in the ESN
		DataHolder inputData = new DataHolder() {
			@Override
			public void setData(double[][] data) {
				esn.setInputData(data);
			}

			@Override
			public double[][] getData() {
				return esn.getInputData();
			}

		};
		// Reference to the training data in the ESN
		DataHolder trainingData = new DataHolder() {
			@Override
			public void setData(double[][] data) {
				esn.setTrainingData(data);
			}

			@Override
			public double[][] getData() {
				return esn.getTrainingData();
			}

		};
//		menu.add(TrainerGuiActions.getEditCombinedDataAction(getNetworkPanel(),
//				(Trainable) getGroup(), inputData, trainingData));
		menu.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(),
				esn.getInputLayer().getNeuronList(), inputData, "Input"));
		menu.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(),
				esn.getOutputLayer().getNeuronList(), trainingData, "Training"));
        setContextMenu(menu);
    }
    
    /**
     * Action to train ESN Offline
     */
	Action trainOfflineAction = new AbstractAction() {

		// Initialize
		{
			putValue(SMALL_ICON, ResourceManager.getImageIcon("Trainer.png"));
			putValue(NAME, "Train offline...");
			putValue(SHORT_DESCRIPTION, "Train offline...");
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			try{ 
				EchoStateNetwork network = (EchoStateNetwork) getGroup();
	        	ESNTrainingPanel trainingPanel = new ESNTrainingPanel(network);
	            trainingPanel.setGenericParent(getNetworkPanel()
	            		.displayPanel(trainingPanel, "Trainer"));
	            trainingPanel.init();
			} catch (NullPointerException npe) {				
				JOptionPane.showMessageDialog(new JFrame(), 
						"Input and training data must\nbe entered prior to" +
						" training.", "Warning", JOptionPane.WARNING_MESSAGE);
				npe.printStackTrace();
			}
		}
	};
    

}
