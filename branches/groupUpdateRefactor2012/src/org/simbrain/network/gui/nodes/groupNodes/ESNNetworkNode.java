package org.simbrain.network.gui.nodes.groupNodes;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.ESNTrainingPanel;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.trainer.DataViewer.DataHolder;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.subnetworks.EchoStateNetwork;
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
        menu.add(new JMenuItem(trainESNAction));
        menu.addSeparator();

        final EchoStateNetwork esn = (EchoStateNetwork)getGroup();
        
        // Reference to input data in the ESN
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
		menu.add(new JMenuItem(TrainerGuiActions.getEditDataAction(
				getNetworkPanel(), esn.getInputLayer().getNeuronList(),
				inputData, "Input Data")));
		
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
		menu.add(new JMenuItem(TrainerGuiActions.getEditDataAction(
				getNetworkPanel(), esn.getOutputLayer().getNeuronList(),
				trainingData, "Training Data")));
		menu.addSeparator();
		menu.add(new JMenuItem(ReflectivePropertyEditor
				.getPropertiesDialogAction((EchoStateNetwork) getGroup())));
        setConextMenu(menu);
    }
    
    /**
     * Action to train ESNs.
     */
	Action trainESNAction = new AbstractAction() {

		// Initialize
		{
			putValue(SMALL_ICON, ResourceManager.getImageIcon("Trainer.png"));
			putValue(NAME, "Train esn...");
			putValue(SHORT_DESCRIPTION, "Train esn...");
		}

		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed(ActionEvent arg0) {
			ESNTrainingPanel esnPanel = new ESNTrainingPanel((EchoStateNetwork) getGroup());
            getNetworkPanel().displayPanel(esnPanel, "ESN Trainer");
		}
	};
    

}
