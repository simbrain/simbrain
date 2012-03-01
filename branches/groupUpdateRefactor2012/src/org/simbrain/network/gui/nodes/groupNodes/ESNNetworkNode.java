package org.simbrain.network.gui.nodes.groupNodes;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.groups.subnetworks.EchoStateNetwork;
import org.simbrain.network.groups.subnetworks.LMSNetwork;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.gui.trainer.TrainerPanel;
import org.simbrain.network.gui.trainer.TrainerPanel.TrainerDataType;
import org.simbrain.network.trainers.LMSIterative;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.util.ClassDescriptionPair;
import org.simbrain.util.genericframe.GenericFrame;

public class ESNNetworkNode extends SubnetGroupNode {
	
	
	/**
     * Create a layered network
     *
     * @param networkPanel parent panel
     * @param group the layered network
     */
    public ESNNetworkNode(NetworkPanel networkPanel, EchoStateNetwork group) {
        super(networkPanel, group);
        setInteractionBox(new ESNInteractionBox(networkPanel));
        setContextMenu();
    }
    
    private Trainer getTrainer() {
        return ((EchoStateNetwork) getGroup()).getTrainer();
    }
    
    /**
     * Custom interaction box for LMS group node.
     */
    private class ESNInteractionBox extends InteractionBox {
        public ESNInteractionBox(NetworkPanel net) {
            super(net, ESNNetworkNode.this);
        }

//        @Override
//        protected JDialog getPropertyDialog() {
//            TrainerPanel panel = new TrainerPanel(getNetworkPanel(),
//                    getTrainer());
//            JDialog dialog = new JDialog();
//            dialog.setContentPane(panel);
//            return dialog;
//        }
//        
//      @Override
//      protected boolean hasPropertyDialog() {
//          return true;
//      }

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
        Action trainNet = new AbstractAction("Show Training Controls...") {
            public void actionPerformed(final ActionEvent event) {
                ClassDescriptionPair[] rules = {
                        new ClassDescriptionPair(LMSOffline.class, "LMS-Offline") };
                TrainerPanel trainerPanel = new TrainerPanel(getNetworkPanel(),
                        getTrainer(), rules);
                GenericFrame frame = getNetworkPanel().displayPanel(
                        trainerPanel, "Trainer");
                trainerPanel.setFrame(frame);
            }
        };
        menu.add(new JMenuItem(trainNet));
        menu.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(), getTrainer(),
                TrainerDataType.Input));
        menu.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(), getTrainer(), 
                TrainerDataType.Trainer));
        menu.add(TrainerGuiActions.getShowPlotAction(getNetworkPanel(), getTrainer()));
        setConextMenu(menu);
    }

}
