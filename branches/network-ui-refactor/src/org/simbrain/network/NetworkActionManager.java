
package org.simbrain.network;

import java.util.List;
import java.util.Arrays;

import javax.swing.Action;

import org.simbrain.network.actions.*;

/**
 * Network action manager.
 */
final class NetworkActionManager {

    /** Pan edit mode action. */
    private final Action panEditModeAction;

    /** Zoom in edit mode action. */
    private final Action zoomInEditModeAction;

    /** Zoom out edit mode action. */
    private final Action zoomOutEditModeAction;

    /** Build edit mode action. */
    private final Action buildEditModeAction;

    /** Selection edit mode action. */
    private final Action selectionEditModeAction;

    /** Network to world interaction mode action. */
    private final Action networkToWorldInteractionModeAction;

    /** World to network interaction mode action. */
    private final Action worldToNetworkInteractionModeAction;

    /** Neither way interaction mode action. */
    private final Action neitherWayInteractionModeAction;

    /** Both ways interaction mode action. */
    private final Action bothWaysInteractionModeAction;

    /** Incremenet objects up. */
    private final Action incremenetUpAction;

    /** Incremenet objects down. */
    private final Action incremenetDownAction;

    /** New neuron action. */
    private final Action newNeuronAction;

    /** Delete neurons action. */
    private final Action deleteNeuronsAction;

    /** Clear neurons action. */
    private final Action clearNeuronsAction;

    /** Randomize objects action. */
    private final Action randomizeObjectsAction;

    /** Select all action. */
    private final Action selectAllAction;

    /** Clear selection action. */
    private final Action clearSelectionAction;

    /** Iterate network action. */
    private final Action iterateNetworkAction;

    /** Run network action. */
    private final Action runNetworkAction;

    /** Stop network action. */
    private final Action stopNetworkAction;

    /** Show help action. */
    private final Action showHelpAction;    

    /**
     * Create a new network action manager for the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    NetworkActionManager(final NetworkPanel networkPanel) {

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        panEditModeAction = new PanEditModeAction(networkPanel);
        zoomInEditModeAction = new ZoomInEditModeAction(networkPanel);
        zoomOutEditModeAction = new ZoomOutEditModeAction(networkPanel);
        buildEditModeAction = new BuildEditModeAction(networkPanel);
        selectionEditModeAction = new SelectionEditModeAction(networkPanel);

        networkToWorldInteractionModeAction = new NetworkToWorldInteractionModeAction(networkPanel);
        worldToNetworkInteractionModeAction = new WorldToNetworkInteractionModeAction(networkPanel);
        neitherWayInteractionModeAction = new NeitherWayInteractionModeAction(networkPanel);
        bothWaysInteractionModeAction = new BothWaysInteractionModeAction(networkPanel);

        incremenetUpAction = new IncremenetUpAction(networkPanel);
        incremenetDownAction = new IncremenetDownAction(networkPanel);

        newNeuronAction = new NewNeuronAction(networkPanel);
        deleteNeuronsAction = new DeleteNeuronsAction(networkPanel);
        clearNeuronsAction = new ClearNeuronsAction(networkPanel);
        randomizeObjectsAction = new RandomizeObjectsAction(networkPanel);

        selectAllAction = new SelectAllAction(networkPanel);
        clearSelectionAction = new ClearSelectionAction(networkPanel);

        iterateNetworkAction = new IterateNetworkAction(networkPanel);
        runNetworkAction = new RunNetworkAction(networkPanel);
        stopNetworkAction = new StopNetworkAction(networkPanel);

        showHelpAction = new ShowHelpAction();
    }


    /**
     * Return the pan edit mode action.
     *
     * @return the pan edit mode action
     */
    public Action getPanEditModeAction() {
        return panEditModeAction;
    }

    /**
     * Return the zoom in edit mode action.
     *
     * @return the zoom in edit mode action
     */
    public Action getZoomInEditModeAction() {
        return zoomInEditModeAction;
    }

    /**
     * Return the zoom out edit mode action.
     *
     * @return the zoom out edit mode action
     */
    public Action getZoomOutEditModeAction() {
        return zoomOutEditModeAction;
    }

    /**
     * Return the build edit mode action.
     *
     * @return the build edit mode action
     */
    public Action getBuildEditModeAction() {
        return buildEditModeAction;
    }

    /**
     * Return the selection edit mode action.
     *
     * @return the selection edit mode action
     */
    public Action getSelectionEditModeAction() {
        return selectionEditModeAction;
    }

    /**
     * Return the network to world interaction mode action.
     *
     * @return the network to world interaction mode action
     */
    public Action getNetworkToWorldInteractionModeAction() {
        return networkToWorldInteractionModeAction;
    }

    /**
     * Return the world to network interaction mode action.
     *
     * @return the world to network interaction mode action
     */
    public Action getWorldToNetworkInteractionModeAction() {
        return worldToNetworkInteractionModeAction;
    }

    /**
     * Return the neither way interaction mode action.
     *
     * @return the neither way interaction mode action
     */
    public Action getNeitherWayInteractionModeAction() {
        return neitherWayInteractionModeAction;
    }

    /**
     * Return the both ways interaction mode action.
     *
     * @return the both ways interaction mode action
     */
    public Action getBothWaysInteractionModeAction() {
        return bothWaysInteractionModeAction;
    }

    /**
     * Return a list of interaction mode actions.
     *
     * @return a list of interaction mode actions
     */
    public List getInteractionModeActions() {
        return Arrays.asList(new Action[]  {bothWaysInteractionModeAction,
                                            networkToWorldInteractionModeAction,
                                            worldToNetworkInteractionModeAction,
                                            neitherWayInteractionModeAction });
    }

    /**
     * Return a list of network mode actions.
     *
     * @return a list of network mode actions
     */
    public List getNetworkModeActions() {
        return Arrays.asList(new Action[]  {zoomInEditModeAction,
                                            zoomOutEditModeAction,
                                            panEditModeAction,
                                            buildEditModeAction,
                                            selectionEditModeAction});
    }

    /**
     * Return a list of network control actions.
     *
     * @return a list of network control actions
     */
    public List getNetworkControlActions() {
        return Arrays.asList(new Action[] { runNetworkAction,
                                            stopNetworkAction});
    }

    /**
     * Return a list of network editing actions.
     *
     * @return a list of network editing actions
     */
    public List getNetworkEditingActions() {
        return Arrays.asList(new Action[] { newNeuronAction,
                                            deleteNeuronsAction});
    }

    /**
     * Return the new neuron action.
     *
     * @return the new neuron action
     */
    public Action getNewNeuronAction() {
        return newNeuronAction;
    }

    /**
     * Return the delete neurons action.
     *
     * @return the delete neurons action
     */
    public Action getDeleteNeuronsAction() {
        return deleteNeuronsAction;
    }

    /**
     * Return the clear neurons action.
     *
     * @return the clear neurons action
     */
    public Action getClearNeuronsAction() {
        return clearNeuronsAction;
    }

    /**
     * Return the randomize objects action.
     *
     * @return the randomize objects action
     */
    public Action getRandomizeObjectsAction() {
        return randomizeObjectsAction;
    }

    /**
     * Return the select all action.
     *
     * @return the select all action
     */
    public Action getSelectAllAction() {
        return selectAllAction;
    }

    /**
     * Return the clear selection action.
     *
     * @return the clear selection action
     */
    public Action getClearSelectionAction() {
        return clearSelectionAction;
    }

    /**
     * Return the iterate network action.
     *
     * @return the iterate network action
     */
    public Action getIterateNetworkAction() {
        return iterateNetworkAction;
    }
    
    /**
     * Return the run network action.
     *
     * @return the run network action
     */
    public Action getRunNetworkAction() {
        return runNetworkAction;
    }
    
    /**
     * Return the stop network action.
     *
     * @return the stop network action
     */
    public Action getStopNetworkAction() {
        return stopNetworkAction;
    }

    /**
     * Return the show help action.
     *
     * @return the show help action
     */
    public Action getShowHelpAction() {
        return showHelpAction;
    }
}