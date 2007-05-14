package org.simbrain.network.dialog.network;

import javax.swing.JTextField;

import org.simbrain.network.NetworkPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.layouts.LayersLayout;
import org.simnet.networks.actorcritic.ActorCritic;;

/**
 * <b>ActorCriticDialog</b> is a dialog box for creating actor-critic networks.
 */
public class ActorCriticDialog extends StandardDialog {

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Number of state units. */
    private JTextField numberOfStateUnits = new JTextField();

    /** Number of actor units. */
    private JTextField numberOfActorUnits = new JTextField();

    /** Reference to network panel. */
    private NetworkPanel networkPanel;

    /**
     * Default constructor.
     *
     * @param np Network panel.
     */
    public ActorCriticDialog(final NetworkPanel np) {
        init();
        networkPanel = np;
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        //Initialize Dialog
        setTitle("New Actor-Critic Network");

        fillFieldValues();

        numberOfStateUnits.setColumns(3);

        //Set up grapics panel
        mainPanel.addItem("Number of State Units", numberOfStateUnits);
        mainPanel.addItem("Number of Actor Units", numberOfActorUnits);

        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      LayersLayout layout = new LayersLayout(40, 40, LayersLayout.HORIZONTAL);
      layout.setInitialLocation(networkPanel.getLastClickedPosition());
      int state = Integer.parseInt(numberOfStateUnits.getText());
      int actions = Integer.parseInt(numberOfActorUnits.getText());
      ActorCritic ac = new ActorCritic(networkPanel.getRootNetwork(), state, actions, layout);
      networkPanel.getRootNetwork().addNetwork(ac);
      networkPanel.repaint();
      super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    private void fillFieldValues() {
        ActorCritic ac = new ActorCritic();
        numberOfStateUnits.setText(Integer.toString(ac.getStateUnits()));
        numberOfActorUnits.setText(Integer.toString(ac.getActorUnits()));
    }

}
